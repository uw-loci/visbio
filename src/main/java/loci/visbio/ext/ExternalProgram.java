/*
 * #%L
 * VisBio application for visualization of multidimensional biological
 * image data.
 * %%
 * Copyright (C) 2002 - 2014 Board of Regents of the University of
 * Wisconsin-Madison.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */

package loci.visbio.ext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.Vector;

import javax.swing.JOptionPane;

import loci.visbio.data.DataManager;
import loci.visbio.data.DataTransform;
import loci.visbio.data.ImageTransform;
import visad.FlatField;
import visad.Linear2DSet;
import visad.SampledSet;
import visad.Set;
import visad.VisADException;

/**
 * ExternalProgram is a transform that uses system calls to an external program
 * to evaluate a function, using stdin and stdout for binary data transfer.
 */
public class ExternalProgram extends ExternalFunction {

	// -- Static fields --

	/** Runtime object for running JVM. */
	protected static Runtime rt = Runtime.getRuntime();

	// -- Constructors --

	/** Creates an uninitialized external program transform. */
	public ExternalProgram() {
		super();
	}

	/** Creates an external program transform from the given transform. */
	public ExternalProgram(final DataTransform parent, final String name,
		final String function)
	{
		super(parent, name, function);
	}

	// -- ExternalFunction API methods --

	/**
	 * Predicts the width, height and number of output planes, given the width,
	 * height and number of input planes, and parameter values.
	 * 
	 * @return An int[3] array representing output width, height and num values.
	 */
	@Override
	public int[] predict(final int width, final int height, final int num,
		final String[] params)
	{
		final String[] args = new String[8 + params.length];
		args[0] = function;
		args[1] = "--predict";
		args[2] = "--width";
		args[3] = "" + width;
		args[4] = "--height";
		args[5] = "" + height;
		args[6] = "--planes";
		args[7] = "" + num;
		System.arraycopy(params, 0, args, 8, params.length);
		try {
			final Process p = rt.exec(args);
			final BufferedReader stdout =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
			final String w = stdout.readLine();
			final String h = stdout.readLine();
			final String n = stdout.readLine();
			try {
				p.waitFor();
			}
			catch (final InterruptedException exc) {
				exc.printStackTrace();
			}
			final int rval = p.exitValue();
			if (rval != 0) {}
			return new int[] { Integer.parseInt(w), Integer.parseInt(h),
				Integer.parseInt(n) };
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
		return null;
	}

	/** Gets the input parameter names and corresponding default values. */
	@Override
	public FunctionParam[] params() {
		FunctionParam[] plist = null;
		final String[] args = { function, "--params" };
		try {
			final Process p = rt.exec(args);
			final BufferedReader stdout =
				new BufferedReader(new InputStreamReader(p.getInputStream()));
			final Vector v = new Vector();
			while (true) {
				final String pname = stdout.readLine();
				if (pname == null) break;
				final String value = stdout.readLine();
				if (value == null) break;
				v.addElement(new FunctionParam(pname, value));
			}
			plist = new FunctionParam[v.size()];
			v.copyInto(plist);
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
		return plist;
	}

	/** Evaluates the function for the given input data and parameter values. */
	@Override
	public FlatField evaluate(final FlatField input, final String[] params) {
		final ImageTransform it = (ImageTransform) parent;
		final int width = it.getImageWidth();
		final int height = it.getImageHeight();
		final int num = it.getRangeCount();

		final String[] args = new String[7 + params.length];
		args[0] = function;
		args[1] = "--width";
		args[2] = "" + width;
		args[3] = "--height";
		args[4] = "" + height;
		args[5] = "--planes";
		args[6] = "" + num;
		System.arraycopy(params, 0, args, 7, params.length);
		try {
			final Process p = rt.exec(args);

			// read messages from stderr using a separate thread
			final BufferedReader stderr =
				new BufferedReader(new InputStreamReader(p.getErrorStream()));
			new Thread("VisBio-" + name + "-stderr") {

				@Override
				public void run() {
					try {
						while (true) {
							final String line = stderr.readLine();
							if (line == null) break;
							System.err.println(line);
						}
					}
					catch (final IOException exc) {
						exc.printStackTrace();
					}
				}
			}.start();

			// write input pixels to stdin using a separate thread
			final OutputStream stdin = p.getOutputStream();
			final float[][] inVals = input.getFloats(false);
			new Thread("VisBio-" + name + "-stdin") {

				@Override
				public void run() {
					try {
						// convert floats to byte array
						final int size = width * height;
						final byte[] bytes = new byte[4 * num * size];
						int c = 0;
						for (int n = 0; n < num; n++) {
							for (int i = 0; i < size; i++) {
								final int bits = Float.floatToRawIntBits(inVals[n][i]);
								bytes[c++] = (byte) (bits & 0xff);
								bytes[c++] = (byte) ((bits >> 8) & 0xff);
								bytes[c++] = (byte) ((bits >> 16) & 0xff);
								bytes[c++] = (byte) ((bits >> 24) & 0xff);
							}
						}
						stdin.write(bytes);
						stdin.flush();
					}
					catch (final IOException exc) {
						exc.printStackTrace();
					}
				}
			}.start();

			// read results from stdout using a separate thread
			final Object stdoutLock = new Object();
			final InputStream stdout = p.getInputStream();
			final byte[] results = new byte[4 * numRange * resY * resX];
			final Thread stdoutThread = new Thread("VisBio-" + name + "-stdout") {

				@Override
				public void run() {
					synchronized (stdoutLock) {
						try {
							int ndx = 0;
							while (ndx < results.length) {
								ndx += stdout.read(results, ndx, results.length - ndx);
							}
						}
						catch (final IOException exc) {
							exc.printStackTrace();
						}
						stdoutLock.notifyAll();
					}
				}
			};

			// wait for stdout results to finish
			synchronized (stdoutLock) {
				stdoutThread.start();
				try {
					stdoutLock.wait();
				}
				catch (final InterruptedException exc) {
					exc.printStackTrace();
				}
			}

			// wait for process to finish
			// try { p.waitFor(); }
			// catch (InterruptedException exc) { exc.printStackTrace(); }
			// int rval = p.exitValue();

			// convert results to floats
			final float[][] outVals = new float[numRange][resY * resX];
			for (int n = 0; n < numRange; n++) {
				final int base = n * resY * resX;
				for (int y = 0; y < resY; y++) {
					for (int x = 0; x < resX; x++) {
						final int ndx = 4 * (base + y * resX + x);
						int b0 = results[ndx];
						if (b0 < 0) b0 += 256;
						int b1 = results[ndx + 1];
						if (b1 < 0) b1 += 256;
						int b2 = results[ndx + 2];
						if (b2 < 0) b2 += 256;
						int b3 = results[ndx + 3];
						if (b3 < 0) b3 += 256;
						final int val = b0 | (b1 << 8) | (b2 << 16) | (b3 << 24);
						outVals[n][ndx / 4] = Float.intBitsToFloat(val);
					}
				}
			}

			// wrap results in a flat field
			Set fset = input.getDomainSet();
			if (fset instanceof SampledSet) {
				// SampledSet ss = (SampledSet) fset;
				final float[] lo = { 0, 0 }; // ss.getLow();
				final float[] hi = { width - 1, height - 1 }; // ss.getHi();
				fset =
					new Linear2DSet(fset.getType(), lo[0], hi[0], resX, hi[1], lo[1],
						resY, fset.getCoordinateSystem(), fset.getSetUnits(), fset
							.getSetErrors());
			}
			final FlatField ff = new FlatField(getType(), fset);
			ff.setSamples(outVals, false);
			return ff;
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		return null;
	}

	// -- Static DataTransform API methods --

	/** Creates a new external program, with user interaction. */
	public static DataTransform makeTransform(final DataManager dm) {
		final DataTransform data = dm.getSelectedData();
		if (!isValidParent(data)) return null;

		final String func =
			(String) JOptionPane.showInputDialog(dm.getControls(),
				"External program to call:", "Create external program transform",
				JOptionPane.INFORMATION_MESSAGE, null, null, "");
		if (func == null) return null;

		final String n =
			(String) JOptionPane.showInputDialog(dm.getControls(), "Transform name:",
				"Create external program transform", JOptionPane.INFORMATION_MESSAGE,
				null, null, data.getName() + " " + func + " external");
		if (n == null) return null;

		return new ExternalProgram(data, n, func);
	}

	/**
	 * Indicates whether this transform type would accept the given transform as
	 * its parent transform.
	 */
	public static boolean isValidParent(final DataTransform data) {
		return data != null && data instanceof ImageTransform;
	}

	/** Indicates whether this transform type requires a parent transform. */
	public static boolean isParentRequired() {
		return true;
	}

}

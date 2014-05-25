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

package loci.visbio;

import java.awt.Color;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import loci.visbio.util.InstanceServer;
import loci.visbio.util.SplashScreen;

/**
 * VisBio is a biological visualization tool designed for easy visualization and
 * analysis of multidimensional image data. This class is the main gateway into
 * the application. It creates and displays a VisBioFrame via reflection, so
 * that the splash screen appears as quickly as possible, before the class
 * loader gets too far along.
 */
public final class VisBio extends Thread {

	// -- Constants --

	/** Application title. */
	public static final String TITLE = "VisBio";

	/** Application version (of the form "###"). */
	private static final String V = "@visbio.version@";

	/** Application version (of the form "#.##"). */
	public static final String VERSION = V.equals("@visbio" + ".version@")
		? "(internal build)" : (V.substring(0, 1) + "." + V.substring(1));

	/** Application authors. */
	public static final String AUTHOR = "Curtis Rueden and Abraham Sorber, LOCI";

	/** Application build date. */
	public static final String DATE = "@date@";

	/** Port to use for communicating between application instances. */
	public static final int INSTANCE_PORT = 0xabcd;

	// -- Constructor --

	/** Ensure this class can't be externally instantiated. */
	private VisBio() {}

	// -- VisBio API methods --

	/** Launches the VisBio GUI with no arguments, in a separate thread. */
	public static void startProgram() {
		new VisBio().start();
	}

	/** Launches VisBio, returning the newly constructed VisBioFrame object. */
	public static Object launch(final String[] args)
		throws ClassNotFoundException, IllegalAccessException,
		InstantiationException, InvocationTargetException, NoSuchMethodException
	{
		// check whether VisBio is already running
		boolean isRunning = true;
		try {
			InstanceServer.sendArguments(args, INSTANCE_PORT);
		}
		catch (final IOException exc) {
			isRunning = false;
		}
		if (isRunning) return null;

		// display splash screen
		final String[] msg =
			{ TITLE + " " + VERSION + " - " + AUTHOR, "VisBio is starting up..." };
		final SplashScreen ss =
			new SplashScreen(VisBio.class.getResource("visbio-logo.png"), msg,
				new Color(255, 255, 220), new Color(255, 50, 50));
		ss.setVisible(true);

		// toggle window decoration mode via reflection
		final boolean b =
			"true".equals(System.getProperty("visbio.decorateWindows"));
		final Class<?> jf = Class.forName("javax.swing.JFrame");
		final Method m =
			jf.getMethod("setDefaultLookAndFeelDecorated",
				new Class<?>[] { boolean.class });
		m.invoke(null, new Object[] { new Boolean(b) });

		// construct VisBio interface via reflection
		final Class<?> vb = Class.forName("loci.visbio.VisBioFrame");
		final Constructor<?> con =
			vb.getConstructor(new Class[] { ss.getClass(), String[].class });
		return con.newInstance(new Object[] { ss, args });
	}

	// -- Thread API methods --

	/** Launches the VisBio GUI with no arguments. */
	@Override
	public void run() {
		try {
			launch(new String[0]);
		}
		catch (final Exception exc) {
			exc.printStackTrace();
		}
	}

	// -- Main --

	/** Launches the VisBio GUI. */
	public static void main(final String[] args) throws ClassNotFoundException,
		IllegalAccessException, InstantiationException, InvocationTargetException,
		NoSuchMethodException
	{
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		final Object o = launch(args);
		if (o == null) System.out.println("VisBio is already running.");
	}

}

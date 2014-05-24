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

package loci.visbio.data;

import java.rmi.RemoteException;

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import loci.visbio.state.Dynamic;
import loci.visbio.state.SaveException;
import loci.visbio.util.DataUtil;
import loci.visbio.util.XMLUtil;
import loci.visbio.view.TransformLink;

import org.w3c.dom.Element;

import visad.Data;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded3DSet;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.Tuple;
import visad.Unit;
import visad.VisADException;

/**
 * A transform for slicing a stack of images in 3D.
 */
public class ArbitrarySlice extends DataTransform implements TransformListener {

	// -- Constants --

	private static final double RADIUS3 = Math.sqrt(3);
	private static final double RADIUS6 = Math.sqrt(6);
	private static final double THETA1 = 0;
	private static final double THETA2 = Math.PI / 2;
	private static final double THETA3 = 3 * Math.PI / 2;
	private static final double THETA4 = Math.PI;
	private static final float T1COS = (float) (RADIUS6 * Math.cos(THETA1));
	private static final float T1SIN = (float) (RADIUS6 * Math.sin(THETA1));
	private static final float T2COS = (float) (RADIUS6 * Math.cos(THETA2));
	private static final float T2SIN = (float) (RADIUS6 * Math.sin(THETA2));
	private static final float T3COS = (float) (RADIUS6 * Math.cos(THETA3));
	private static final float T3SIN = (float) (RADIUS6 * Math.sin(THETA3));
	private static final float T4COS = (float) (RADIUS6 * Math.cos(THETA4));
	private static final float T4SIN = (float) (RADIUS6 * Math.sin(THETA4));

	// -- Fields --

	/** Dimensional axis to slice through. */
	protected int axis;

	/** Horizontal rotational angle of slicing line. */
	protected float yaw;

	/** Vertical rotatational angle of slicing line. */
	protected float pitch;

	/** Arbitrary slice's location along slicing line. */
	protected float loc;

	/** Resolution of arbitrary slice. */
	protected int res;

	/** Flag indicating whether slicing line should be shown. */
	protected boolean showLine;

	/** Flag indicating whether arbitrary slice should actually be computed. */
	protected boolean compute;

	/** Controls for the arbitrary slice. */
	protected SliceWidget controls;

	// -- Constructor --

	/** Creates an uninitialized arbitrary slice. */
	public ArbitrarySlice() {}

	/** Creates an overlay object for the given transform. */
	public ArbitrarySlice(final DataTransform parent, final String name) {
		super(parent, name);
		initState(null);
		parent.addTransformListener(this);
	}

	// -- ArbitrarySlice API methods --

	/**
	 * Sets the parameters for the arbitrary slice, recomputing the slice if the
	 * compute flag is set.
	 */
	public synchronized void setParameters(final int axis, final float yaw,
		final float pitch, final float loc, final int res, final boolean showLine,
		final boolean compute)
	{
		if (this.axis != axis) {
			this.axis = axis;
			computeLengths();
		}
		if (this.yaw == yaw && this.pitch == pitch && this.loc == loc &&
			this.res == res && this.showLine == showLine && this.compute == compute)
		{
			return;
		}
		this.yaw = yaw;
		this.pitch = pitch;
		this.loc = loc;
		this.res = res;
		this.showLine = showLine;
		this.compute = compute;
		controls.refreshWidget();
		notifyListeners(new TransformEvent(this));
	}

	/** Sets the axis through which to slice. */
	public void setAxis(final int axis) {
		setParameters(axis, yaw, pitch, loc, res, showLine, compute);
	}

	/** Sets the yaw for the slicing line. */
	public void setYaw(final float yaw) {
		setParameters(axis, yaw, pitch, loc, res, showLine, compute);
	}

	/** Sets the pitch for the slicing line. */
	public void setPitch(final float pitch) {
		setParameters(axis, yaw, pitch, loc, res, showLine, compute);
	}

	/** Sets the location for the arbitrary slice. */
	public void setLocation(final float loc) {
		setParameters(axis, yaw, pitch, loc, res, showLine, compute);
	}

	/** Sets the resolution for the slicing line. */
	public void setResolution(final int res) {
		setParameters(axis, yaw, pitch, loc, res, showLine, compute);
	}

	/** Sets whether white line is shown. */
	public void setLineVisible(final boolean showLine) {
		setParameters(axis, yaw, pitch, loc, res, showLine, compute);
	}

	/** Sets whether arbitrary slice is computed. */
	public void setSliceComputed(final boolean compute) {
		setParameters(axis, yaw, pitch, loc, res, showLine, compute);
	}

	/** Gets the axis through which to slice. */
	public int getAxis() {
		return axis;
	}

	/** Gets the yaw for the slicing line. */
	public float getYaw() {
		return yaw;
	}

	/** Gets the pitch for the slicing line. */
	public float getPitch() {
		return pitch;
	}

	/** Gets the location for the arbitrary slice. */
	public float getLocation() {
		return loc;
	}

	/** Gets the resolution for the slicing line. */
	public int getResolution() {
		return res;
	}

	/** Gets whether slicing line is shown. */
	public boolean isLineVisible() {
		return showLine;
	}

	/** Gets whether arbitary slice is computed. */
	public boolean isSliceComputed() {
		return compute;
	}

	// -- Static DataTransform API methods --

	/** Creates a new set of overlays, with user interaction. */
	public static DataTransform makeTransform(final DataManager dm) {
		final DataTransform dt = dm.getSelectedData();
		if (!isValidParent(dt)) return null;
		final String n =
			(String) JOptionPane.showInputDialog(dm.getControls(), "Title of slice:",
				"Create arbitrary slice", JOptionPane.INFORMATION_MESSAGE, null, null,
				dt.getName() + " slice");
		if (n == null) return null;
		return new ArbitrarySlice(dt, n);
	}

	/**
	 * Indicates whether this transform type would accept the given transform as
	 * its parent transform.
	 */
	public static boolean isValidParent(final DataTransform data) {
		return data != null && data instanceof ImageTransform &&
			data.getLengths().length > 0;
	}

	/** Indicates whether this transform type requires a parent transform. */
	public static boolean isParentRequired() {
		return true;
	}

	// -- DataTransform API methods --

	/**
	 * Retrieves the data corresponding to the given dimensional position, for the
	 * given display dimensionality.
	 * 
	 * @return null if the transform does not provide data of that dimensionality
	 */
	@Override
	public synchronized Data getData(final TransformLink link, final int[] pos,
		final int dim, final DataCache cache)
	{
		if (dim != 3) {
			System.err.println(name + ": invalid dimensionality (" + dim + ")");
			return null;
		}

		// get some info from the parent transform
		final ImageTransform it = (ImageTransform) parent;
		final int w = it.getImageWidth();
		final int h = it.getImageHeight();
		final int n = parent.getLengths()[axis];
		final RealType xType = it.getXType();
		final RealType yType = it.getYType();
		final RealType zType = it.getZType();
		final RealType[] range = it.getRangeTypes();
		final FunctionType imageType = it.getType();
		final Unit[] imageUnits = it.getImageUnits();
		final Unit zUnit = it.getZUnit(axis);
		final Unit[] xyzUnits = { imageUnits[0], imageUnits[1], zUnit };
		RealTupleType xy = null, xyz = null;
		try {
			xy = new RealTupleType(xType, yType);
			xyz = new RealTupleType(xType, yType, zType);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}

		// convert spherical polar coordinates to cartesian coordinates for line
		final double yawRadians = Math.PI * yaw / 180;
		final double yawCos = Math.cos(yawRadians);
		final double yawSin = Math.sin(yawRadians);
		final double pitchRadians = Math.PI * (90 - pitch) / 180;
		final double pitchCos = Math.cos(pitchRadians);
		final double pitchSin = Math.sin(pitchRadians);

		// Let P1 = (x, y, z), P2 = -P1
		final float x = (float) (RADIUS3 * pitchSin * yawCos);
		final float y = (float) (RADIUS3 * pitchSin * yawSin);
		final float z = (float) (RADIUS3 * pitchCos);

		// compute location along P1-P2 line
		final float q = (loc - 50) / 50;
		final float lx = q * x;
		final float ly = q * y;
		final float lz = q * z;

		// choose a point P which doesn't lie on the line through P1 and P2
		final float px = 1;
		float py = 1, pz = 1;
		final float ax = x < 0 ? -x : x;
		final float ay = y < 0 ? -y : y;
		final float az = z < 0 ? -z : z;
		if (ax - ay < 0.1) py = -1;
		if (ax - az < 0.1) pz = -1;

		// calculate the vector R as the cross product between P - P1 and P2 - P1
		final float pp1x = px - x, pp1y = py - y, pp1z = pz - z;
		final float p2p1x = -x - x, p2p1y = -y - y, p2p1z = -z - z;
		float rz = pp1x * p2p1y - pp1y * p2p1x;
		float rx = pp1y * p2p1z - pp1z * p2p1y;
		float ry = pp1z * p2p1x - pp1x * p2p1z;
		// R is now perpendicular to P2 - P1

		// calculate the vector S as the cross product between R and P2 - P1
		float sz = rx * p2p1y - ry * p2p1x;
		float sx = ry * p2p1z - rz * p2p1y;
		float sy = rz * p2p1x - rx * p2p1z;
		// S is now perpendicular to both R and the P2 - P1

		// normalize R and S
		final float rlen = (float) Math.sqrt(rx * rx + ry * ry + rz * rz);
		rx /= rlen;
		ry /= rlen;
		rz /= rlen;
		final float slen = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
		sx /= slen;
		sy /= slen;
		sz /= slen;
		// now R and S are an orthonormal basis for the plane

		Gridded3DSet line = null;
		if (showLine) {
			// convert x=[-1,1] y=[-1,1] z=[-1,1] to x=[0,w] y=[0,h] z=[0,n]
			final float[][] lineSamples =
				{ { w * (x + 1) / 2, w * (-x + 1) / 2 },
					{ h * (y + 1) / 2, h * (-y + 1) / 2 },
					{ n * (z + 1) / 2, n * (-z + 1) / 2 } };

			// construct line data object
			try {
				line =
					new Gridded3DSet(xyz, lineSamples, 2, null, xyzUnits, null, false);
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
		}

		// compute slice data
		Data slice = null;
		if (compute) { // interpolate from parent data
			// compute plane corners from orthonormal basis
			final float q1x = w * (lx + T1COS * rx + T1SIN * sx + 1) / 2;
			final float q1y = h * (ly + T1COS * ry + T1SIN * sy + 1) / 2;
			final float q1z = n * (lz + T1COS * rz + T1SIN * sz + 1) / 2;
			final float q2x = w * (lx + T2COS * rx + T2SIN * sx + 1) / 2;
			final float q2y = h * (ly + T2COS * ry + T2SIN * sy + 1) / 2;
			final float q2z = n * (lz + T2COS * rz + T2SIN * sz + 1) / 2;
			final float q3x = w * (lx + T3COS * rx + T3SIN * sx + 1) / 2;
			final float q3y = h * (ly + T3COS * ry + T3SIN * sy + 1) / 2;
			final float q3z = n * (lz + T3COS * rz + T3SIN * sz + 1) / 2;
			final float q4x = w * (lx + T4COS * rx + T4SIN * sx + 1) / 2;
			final float q4y = h * (ly + T4COS * ry + T4SIN * sy + 1) / 2;
			final float q4z = n * (lz + T4COS * rz + T4SIN * sz + 1) / 2;

			// retrieve parent data from data cache
			final int[] npos = getParentPos(pos);
			final FlatField[] fields = new FlatField[n];
			for (int i = 0; i < n; i++) {
				npos[axis] = i;
				final Data data = parent.getData(link, npos, 2, cache);
				if (data == null || !(data instanceof FlatField)) {
					System.err.println(name + ": parent image plane #" + (i + 1) +
						" is not valid");
					return null;
				}
				fields[i] = (FlatField) data;
			}
			try {
				// use image transform's recommended MathType and Units
				for (int i = 0; i < n; i++) {
					fields[i] = DataUtil.switchType(fields[i], imageType, imageUnits);
				}
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
			catch (final RemoteException exc) {
				exc.printStackTrace();
			}

			// generate planar domain samples and corresponding interpolated values
			final int res1 = res - 1;
			final float[][] planeSamples = new float[3][res * res];
			final float[][] planeValues = new float[range.length][res * res];
			for (int r = 0; r < res; r++) {
				final float rr = (float) r / res1;
				final float xmin = (1 - rr) * q1x + rr * q3x;
				final float ymin = (1 - rr) * q1y + rr * q3y;
				final float zmin = (1 - rr) * q1z + rr * q3z;
				final float xmax = (1 - rr) * q2x + rr * q4x;
				final float ymax = (1 - rr) * q2y + rr * q4y;
				final float zmax = (1 - rr) * q2z + rr * q4z;
				for (int c = 0; c < res; c++) {
					final float cc = (float) c / res1;
					final int ndx = r * res + c;
					final float xs = planeSamples[0][ndx] = (1 - cc) * xmin + cc * xmax;
					float ys = planeSamples[1][ndx] = (1 - cc) * ymin + cc * ymax;
					ys = h - ys; // lines are flipped
					final float zs = planeSamples[2][ndx] = (1 - cc) * zmin + cc * zmax;
					if (xs < 0 || ys < 0 || zs < 0 || xs > w - 1 || ys > h - 1 ||
						zs > n - 1)
					{
						// this pixel is outside the range of the data (missing)
						for (int k = 0; k < planeValues.length; k++) {
							planeValues[k][ndx] = Float.NaN;
						}
					}
					else {
						// interpolate the value of this pixel for each range component
						final int xx = (int) xs, yy = (int) ys, zz = (int) zs;
						final float wx = xs - xx, wy = ys - yy, wz = zs - zz;

						final int ndx00 = w * yy + xx;
						final int ndx10 = w * yy + xx + 1;
						final int ndx01 = w * (yy + 1) + xx;
						final int ndx11 = w * (yy + 1) + xx + 1;

						FlatField field0, field1;
						if (wz == 0) {
							// interpolate from a single field (z0 == z1)
							field0 = field1 = fields[zz];
						}
						else {
							// interpolate between two fields
							field0 = fields[zz];
							field1 = fields[zz + 1];
						}

						double[] v000 = null, v100 = null, v010 = null, v110 = null;
						double[] v001 = null, v101 = null, v011 = null, v111 = null;
						try {
							v000 = ((RealTuple) field0.getSample(ndx00)).getValues();
							v100 = ((RealTuple) field0.getSample(ndx10)).getValues();
							v010 = ((RealTuple) field0.getSample(ndx01)).getValues();
							v110 = ((RealTuple) field0.getSample(ndx11)).getValues();
							v001 = ((RealTuple) field1.getSample(ndx00)).getValues();
							v101 = ((RealTuple) field1.getSample(ndx10)).getValues();
							v011 = ((RealTuple) field1.getSample(ndx01)).getValues();
							v111 = ((RealTuple) field1.getSample(ndx11)).getValues();
						}
						catch (final VisADException exc) {
							exc.printStackTrace();
						}
						catch (final RemoteException exc) {
							exc.printStackTrace();
						}

						for (int k = 0; k < range.length; k++) {
							// tri-linear interpolation (x, then y, then z)
							final float vx00 = (float) ((1 - wx) * v000[k] + wx * v100[k]);
							final float vx10 = (float) ((1 - wx) * v010[k] + wx * v110[k]);
							final float vx01 = (float) ((1 - wx) * v001[k] + wx * v101[k]);
							final float vx11 = (float) ((1 - wx) * v011[k] + wx * v111[k]);
							final float vxy0 = (1 - wy) * vx00 + wy * vx10;
							final float vxy1 = (1 - wy) * vx01 + wy * vx11;
							final float vxyz = (1 - wz) * vxy0 + wz * vxy1;
							planeValues[k][ndx] = vxyz;
						}
					}
				}
			}
			try {
				final FunctionType planeType =
					new FunctionType(xyz, imageType.getRange());
				// set must be gridded, not linear, because ManifoldDimension is 2
				final Gridded3DSet planeSet =
					new Gridded3DSet(xyz, planeSamples, res, res, null, xyzUnits, null,
						false);
				final FlatField ff = new FlatField(planeType, planeSet);
				ff.setSamples(planeValues, false);
				slice = ff;
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
			catch (final RemoteException exc) {
				exc.printStackTrace();
			}
		}
		else { // construct bounding circle
			// compute circle coordinates from orthonormal basis
			final int num = 32;
			final float[][] samples = new float[3][num];
			for (int i = 0; i < num; i++) {
				final double theta = i * 2 * Math.PI / num;
				final float tcos = (float) (RADIUS3 * Math.cos(theta));
				final float tsin = (float) (RADIUS3 * Math.sin(theta));
				final float qx = lx + tcos * rx + tsin * sx;
				final float qy = ly + tcos * ry + tsin * sy;
				final float qz = lz + tcos * rz + tsin * sz;
				final int ndx = (i < num / 2) ? i : (3 * num / 2 - i - 1);
				samples[0][ndx] = w * (qx + 1) / 2;
				samples[1][ndx] = h * (qy + 1) / 2;
				samples[2][ndx] = n * (qz + 1) / 2;
			}

			// construct bounding circle data object
			try {
				slice =
					new Gridded3DSet(xyz, samples, num / 2, 2, null, xyzUnits, null,
						false);
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
		}

		final Data[] data =
			showLine ? new Data[] { slice, line } : new Data[] { slice };
		try {
			return new Tuple(data, false);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		return null;
	}

	/** Gets whether this transform provides data of the given dimensionality. */
	@Override
	public boolean isValidDimension(final int dim) {
		return dim == 3;
	}

	/** Retrieves a set of mappings for displaying this transform effectively. */
	@Override
	public ScalarMap[] getSuggestedMaps() {
		// Slice XYZ types piggyback on the parent, so that two sets of XYZ
		// coordinates don't show up during cursor probes (and so that the slice
		// is placed properly without an extra set of setRange calls).
		return parent.getSuggestedMaps();
	}

	/**
	 * Gets a string id uniquely describing this data transform at the given
	 * dimensional position, for the purposes of thumbnail caching. If global flag
	 * is true, the id is suitable for use in the default, global cache file.
	 */
	@Override
	public String getCacheId(final int[] pos, final boolean global) {
		return null;
	}

	/**
	 * Arbitrary slices are rendered immediately, due to frequent user
	 * interaction.
	 */
	@Override
	public boolean isImmediate() {
		return true;
	}

	/** Gets associated GUI controls for this transform. */
	@Override
	public JComponent getControls() {
		return controls;
	}

	// -- Dynamic API methods --

	/** Tests whether two dynamic objects are equivalent. */
	@Override
	public boolean matches(final Dynamic dyn) {
		if (!super.matches(dyn) || !isCompatible(dyn)) return false;
		final ArbitrarySlice data = (ArbitrarySlice) dyn;

		// CTR TODO return true iff data matches this object
		return false;
	}

	/**
	 * Tests whether the given dynamic object can be used as an argument to
	 * initState, for initializing this dynamic object.
	 */
	@Override
	public boolean isCompatible(final Dynamic dyn) {
		return dyn instanceof ArbitrarySlice;
	}

	/**
	 * Modifies this object's state to match that of the given object. If the
	 * argument is null, the object is initialized according to its current state
	 * instead.
	 */
	@Override
	public void initState(final Dynamic dyn) {
		if (dyn != null && !isCompatible(dyn)) return;
		super.initState(dyn);
		final ArbitrarySlice data = (ArbitrarySlice) dyn;

		if (data == null) {
			axis = -1;
			yaw = 0;
			pitch = 45;
			loc = 50;
			res = 64;
			showLine = true;
			compute = true;
		}
		else {
			axis = data.axis;
			yaw = data.yaw;
			pitch = data.pitch;
			loc = data.loc;
			res = data.res;
			showLine = data.showLine;
			compute = data.compute;
		}

		computeLengths();
		controls = new SliceWidget(this);
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("DataTransforms"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "ArbitrarySlice");
		super.saveState(child);
		child.setAttribute("axis", "" + axis);
		child.setAttribute("yaw", "" + yaw);
		child.setAttribute("pitch", "" + pitch);
		child.setAttribute("location", "" + loc);
		child.setAttribute("resolution", "" + res);
		child.setAttribute("showLine", "" + showLine);
		child.setAttribute("onTheFly", "" + compute);
	}

	/**
	 * Restores the current state from the given DOM element ("ArbitrarySlice").
	 */
	@Override
	public void restoreState(final Element el) throws SaveException {
		super.restoreState(el);
		axis = Integer.parseInt(el.getAttribute("axis"));
		yaw = Float.parseFloat(el.getAttribute("yaw"));
		pitch = Float.parseFloat(el.getAttribute("pitch"));
		loc = Float.parseFloat(el.getAttribute("location"));
		res = Integer.parseInt(el.getAttribute("resolution"));
		showLine = "true".equals(el.getAttribute("showLine"));
		compute = "true".equals(el.getAttribute("onTheFly"));
	}

	// -- TransformListener API methods --

	/** Called when parent data transform's parameters are updated. */
	@Override
	public void transformChanged(final TransformEvent e) {
		final int id = e.getId();
		if (id == TransformEvent.DATA_CHANGED) {
			initState(null);
			notifyListeners(new TransformEvent(this));
		}
	}

	// -- Helper methods --

	/** Computes lengths and dims based on dimensional axis to be sliced. */
	private void computeLengths() {
		final int[] plens = parent.getLengths();
		final String[] pdims = parent.getDimTypes();

		if (axis < 0) {
			axis = 0;
			for (int i = 0; i < pdims.length; i++) {
				if (pdims[i].equals("Slice")) {
					axis = i;
					break;
				}
			}
		}

		lengths = new int[plens.length - 1];
		System.arraycopy(plens, 0, lengths, 0, axis);
		System.arraycopy(plens, axis + 1, lengths, axis, lengths.length - axis);

		dims = new String[pdims.length - 1];
		System.arraycopy(pdims, 0, dims, 0, axis);
		System.arraycopy(pdims, axis + 1, dims, axis, dims.length - axis);

		makeLabels();
	}

	/** Gets dimensional position for parent transform. */
	private int[] getParentPos(final int[] pos) {
		final int[] npos = new int[pos.length + 1];
		System.arraycopy(pos, 0, npos, 0, axis);
		System.arraycopy(pos, axis, npos, axis + 1, pos.length - axis);
		return npos;
	}

}

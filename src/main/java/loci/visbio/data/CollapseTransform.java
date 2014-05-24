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
import loci.visbio.util.XMLUtil;
import loci.visbio.view.TransformLink;

import org.w3c.dom.Element;

import visad.Data;
import visad.FlatField;
import visad.FunctionType;
import visad.MathType;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;

/**
 * CollapseTransform converts a dimensional axis into a series of range
 * components.
 */
public class CollapseTransform extends ImageTransform {

	// -- Fields --

	/** Dimensional axis to collapse. */
	protected int axis;

	/** Range display mappings. */
	protected RealType[] range;

	/** Controls for this dimensional collapse. */
	protected CollapseWidget controls;

	// -- Constructors --

	/** Creates an uninitialized dimensional collapse. */
	public CollapseTransform() {
		super();
	}

	/** Creates a dimensional collapse from the given transform. */
	public CollapseTransform(final DataTransform parent, final String name,
		final int axis)
	{
		super(parent, name);
		this.axis = axis;
		initState(null);
	}

	// -- CollapseTransform API methods --

	/** Assigns the parameters for this dimensional collapse. */
	public void setParameters(final int axis) {
		if (axis < 0 || axis >= parent.lengths.length) return;
		this.axis = axis;
		computeLengths();

		// signal parameter change to listeners
		notifyListeners(new TransformEvent(this));
	}

	// -- ImageTransform API methods --

	/** Gets width of each image. */
	@Override
	public int getImageWidth() {
		return ((ImageTransform) parent).getImageWidth();
	}

	/** Gets height of each image. */
	@Override
	public int getImageHeight() {
		return ((ImageTransform) parent).getImageHeight();
	}

	/** Gets number of range components at each pixel. */
	@Override
	public int getRangeCount() {
		return range.length;
	}

	// -- Static DataTransform API methods --

	/** Creates a new dimensional collapse, with user interaction. */
	public static DataTransform makeTransform(final DataManager dm) {
		final DataTransform data = dm.getSelectedData();
		if (!isValidParent(data)) return null;
		final String n =
			(String) JOptionPane.showInputDialog(dm.getControls(), "Collapse name:",
				"Create dimensional collapse", JOptionPane.INFORMATION_MESSAGE, null,
				null, data.getName() + " collapse");
		if (n == null) return null;

		// guess at some reasonable defaults
		final int collapseAxis = 0;

		return new CollapseTransform(data, n, collapseAxis);
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
	public Data getData(final TransformLink link, final int[] pos, final int dim,
		final DataCache cache)
	{
		if (dim != 2) return null;

		final int len = parent.getLengths()[axis];
		final FlatField[] fields = new FlatField[len];
		final int[] npos = getParentPos(pos);
		for (int i = 0; i < len; i++) {
			npos[axis] = i;
			final Data data = parent.getData(link, npos, dim, cache);
			if (data == null || !(data instanceof FlatField)) return null;
			fields[i] = (FlatField) data;
		}
		return collapse(fields, range);
	}

	/** Gets whether this transform provides data of the given dimensionality. */
	@Override
	public boolean isValidDimension(final int dim) {
		return dim == 2 && parent.isValidDimension(dim);
	}

	/**
	 * Gets a string id uniquely describing this data transform at the given
	 * dimensional position, for the purposes of thumbnail caching. If global flag
	 * is true, the id is suitable for use in the default, global cache file.
	 */
	@Override
	public String getCacheId(final int[] pos, final boolean global) {
		final int[] npos = getParentPos(pos);
		final StringBuffer sb = new StringBuffer(parent.getCacheId(npos, global));
		sb.append("{");
		if (global) sb.append("collapse=");
		sb.append(axis);
		sb.append("}");
		return sb.toString();
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
		final CollapseTransform data = (CollapseTransform) dyn;

		return axis == data.axis;
	}

	/**
	 * Tests whether the given dynamic object can be used as an argument to
	 * initState, for initializing this dynamic object.
	 */
	@Override
	public boolean isCompatible(final Dynamic dyn) {
		return dyn instanceof CollapseTransform;
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
		final CollapseTransform data = (CollapseTransform) dyn;

		if (data != null) {
			axis = data.axis;
		}

		computeLengths();

		controls = new CollapseWidget(this);
		thumbs = new ThumbnailHandler(this, getCacheFilename());
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("DataTransforms"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "DimensionalCollapse");
		super.saveState(child);
		child.setAttribute("axis", "" + axis);
	}

	/**
	 * Restores the current state from the given DOM element
	 * ("DimensionalCollapse").
	 */
	@Override
	public void restoreState(final Element el) throws SaveException {
		super.restoreState(el);
		axis = Integer.parseInt(el.getAttribute("axis"));
	}

	// -- Utility methods --

	/** Collapses the given fields. */
	public static FlatField collapse(final FlatField[] fields,
		final RealType[] types)
	{
		if (fields == null || types == null || fields.length == 0) return null;

		FlatField ff = null;
		try {
			final FunctionType ftype = (FunctionType) fields[0].getType();
			final RealTupleType domain = ftype.getDomain();
			final MathType rtype =
				types.length == 1 ? (MathType) types[0] : (MathType) new RealTupleType(
					types);
			ff =
				new FlatField(new FunctionType(domain, rtype), fields[0].getDomainSet());

			final float[][] samples = new float[types.length][];
			int ndx = 0;

			for (int i = 0; i < fields.length; i++) {
				final float[][] samps = fields[i].getFloats(false);
				if (samps.length + ndx > types.length) return null;
				for (int j = 0; j < samps.length; j++)
					samples[ndx++] = samps[j];
			}
			if (ndx != types.length) return null;

			ff.setSamples(samples, false);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		return ff;
	}

	// -- Helper methods --

	/** Chooses filename for thumbnail cache based on parent cache's name. */
	private String getCacheFilename() {
		ThumbnailCache cache = null;
		final ThumbnailHandler th = parent.getThumbHandler();
		if (th != null) cache = th.getCache();
		if (cache == null) return "dimensional_collapse.visbio";
		String s = cache.getCacheFile().getAbsolutePath();
		final int dot = s.lastIndexOf(".");
		String suffix;
		if (dot < 0) suffix = "";
		else {
			suffix = s.substring(dot);
			s = s.substring(0, dot);
		}
		return s + "_collapse" + suffix;
	}

	/**
	 * Computes lengths, dims and range arrays based on dimensional axis to be
	 * collapsed.
	 */
	private void computeLengths() {
		final int[] plens = parent.getLengths();
		lengths = new int[plens.length - 1];
		System.arraycopy(plens, 0, lengths, 0, axis);
		System.arraycopy(plens, axis + 1, lengths, axis, lengths.length - axis);

		final String[] pdims = parent.getDimTypes();
		dims = new String[pdims.length - 1];
		System.arraycopy(pdims, 0, dims, 0, axis);
		System.arraycopy(pdims, axis + 1, dims, axis, dims.length - axis);

		makeLabels();

		final int len = plens[axis];
		final char letter =
			pdims[axis].equals("Slice") ? 'Z' : pdims[axis].charAt(0);
		final RealType[] prange = ((ImageTransform) parent).getRangeTypes();
		range = new RealType[prange.length * len];
		for (int i = 0; i < prange.length; i++) {
			final String rname = prange[i].getName();
			for (int j = 0; j < len; j++) {
				final int ndx = j * prange.length + i;
				range[ndx] = RealType.getRealType(rname + "_" + letter + (j + 1));
			}
		}
	}

	/** Gets dimensional position for parent transform. */
	private int[] getParentPos(final int[] pos) {
		final int[] npos = new int[pos.length + 1];
		System.arraycopy(pos, 0, npos, 0, axis);
		System.arraycopy(pos, axis, npos, axis + 1, pos.length - axis);
		return npos;
	}

}

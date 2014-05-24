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
import visad.RealType;
import visad.VisADException;

/**
 * ProjectionTransform performs a maximum intensity projection across a given
 * dimensional axis.
 */
public class ProjectionTransform extends ImageTransform {

	// -- Fields --

	/** Dimensional axis to project. */
	protected int axis;

	/** Range display mappings. */
	protected RealType[] range;

	/** Controls for this maximum intensity projection. */
	protected ProjectionWidget controls;

	// -- Constructors --

	/** Creates an uninitialized maximum intensity projection. */
	public ProjectionTransform() {
		super();
	}

	/** Creates a maximum intensity projection from the given transform. */
	public ProjectionTransform(final DataTransform parent, final String name,
		final int axis)
	{
		super(parent, name);
		this.axis = axis;
		initState(null);
	}

	// -- ProjectionTransform API methods --

	/** Assigns the parameters for this maximum intensity projection. */
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
		return ((ImageTransform) parent).getRangeCount();
	}

	// -- Static DataTransform API methods --

	/** Creates a new maximum intensity projection, with user interaction. */
	public static DataTransform makeTransform(final DataManager dm) {
		final DataTransform data = dm.getSelectedData();
		if (!isValidParent(data)) return null;
		final String n =
			(String) JOptionPane.showInputDialog(dm.getControls(),
				"Projection name:", "Create maximum intensity projection",
				JOptionPane.INFORMATION_MESSAGE, null, null, data.getName() +
					" projection");
		if (n == null) return null;

		// guess at some reasonable defaults
		final int projectAxis = 0;

		return new ProjectionTransform(data, n, projectAxis);
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
		return project(fields);
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
		if (global) sb.append("project=");
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
		final ProjectionTransform data = (ProjectionTransform) dyn;

		return axis == data.axis;
	}

	/**
	 * Tests whether the given dynamic object can be used as an argument to
	 * initState, for initializing this dynamic object.
	 */
	@Override
	public boolean isCompatible(final Dynamic dyn) {
		return dyn instanceof ProjectionTransform;
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
		final ProjectionTransform data = (ProjectionTransform) dyn;

		if (data != null) {
			axis = data.axis;
		}

		computeLengths();

		controls = new ProjectionWidget(this);
		thumbs = new ThumbnailHandler(this, getCacheFilename());
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("DataTransforms"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "MaximumIntensityProjection");
		super.saveState(child);
		child.setAttribute("axis", "" + axis);
	}

	/**
	 * Restores the current state from the given DOM element
	 * ("MaximumIntensityProjection").
	 */
	@Override
	public void restoreState(final Element el) throws SaveException {
		super.restoreState(el);
		axis = Integer.parseInt(el.getAttribute("axis"));
	}

	// -- Utility methods --

	/** Performs a maximum intensity projection across the given fields. */
	public static FlatField project(final FlatField[] fields) {
		if (fields == null || fields.length == 0) return null;

		FlatField ff = fields[0];
		try {
			for (int i = 1; i < fields.length; i++)
				ff = (FlatField) ff.max(fields[i]);
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
		if (cache == null) return "intensity_projection.visbio";
		String s = cache.getCacheFile().getAbsolutePath();
		final int dot = s.lastIndexOf(".");
		String suffix;
		if (dot < 0) suffix = "";
		else {
			suffix = s.substring(dot);
			s = s.substring(0, dot);
		}
		return s + "_projection" + suffix;
	}

	/**
	 * Computes lengths, dims and range arrays based on dimensional axis to be
	 * projected.
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
	}

	/** Gets dimensional position for parent transform. */
	private int[] getParentPos(final int[] pos) {
		final int[] npos = new int[pos.length + 1];
		System.arraycopy(pos, 0, npos, 0, axis);
		System.arraycopy(pos, axis, npos, axis + 1, pos.length - axis);
		return npos;
	}

}

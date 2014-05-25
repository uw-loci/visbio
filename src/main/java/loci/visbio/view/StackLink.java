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

package loci.visbio.view;

import java.rmi.RemoteException;
import java.util.Vector;

import loci.visbio.data.DataCache;
import loci.visbio.data.DataTransform;
import loci.visbio.data.ImageTransform;
import loci.visbio.data.ThumbnailHandler;
import loci.visbio.state.Dynamic;
import loci.visbio.state.SaveException;
import loci.visbio.util.ColorUtil;
import loci.visbio.util.DataUtil;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

import visad.ConstantMap;
import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.DataRenderer;
import visad.Display;
import visad.DisplayImpl;
import visad.DisplayRenderer;
import visad.FlatField;
import visad.FunctionImpl;
import visad.FunctionType;
import visad.Gridded3DSet;
import visad.GriddedSet;
import visad.Real;
import visad.RealTupleType;
import visad.RealType;
import visad.Unit;
import visad.VisADException;
import visad.util.CursorUtil;

/**
 * Represents a link between a data transform and a display that produces an
 * image stack.
 */
public class StackLink extends TransformLink {

	// -- Constants --

	/** Dummy data to avoid "Data is null" message for volume reference. */
	protected static final Data DUMMY = new Real(0);

	// -- Fields --

	/** Data references linking data to the display. */
	protected Vector<DataReference> references;

	/** Data renderers for toggling data's visibility and other parameters. */
	protected Vector<DataRenderer> renderers;

	/** Dimensional axis to use for image stacks. */
	protected int stackAxis;

	/**
	 * Flag indicating whether stackAxis is valid. If it is not, it will be
	 * autodetected when initState is called. Also, if it is valid, then
	 * setStackAxis will not rebuild display references unless the new axis value
	 * is different from the old one.
	 */
	protected boolean axisValid;

	/** Last known dimensional position of the link. */
	protected int[] lastPos;

	/** Data reference for volume rendered cube. */
	protected DataReferenceImpl volumeRef;

	/** Whether volume rendering is currently enabled. */
	protected boolean volume;

	/** Resolution of rendered volumes. */
	protected int volumeRes = StackHandler.DEFAULT_VOLUME_RESOLUTION;

	// -- Fields - initial state --

	/** Data transform's current slice. */
	protected int currentSlice;

	/**
	 * Flags indicating visibility of each slice of the stack. Used for both
	 * initial state and temporary storage while volume rendering.
	 */
	protected boolean[] visSlices;

	// -- Constructor --

	/** Constructs an uninitialized stack link. */
	public StackLink(final StackHandler h) {
		super(h);
	}

	/**
	 * Creates a link between the given data transform and the specified stack
	 * handler's display.
	 */
	public StackLink(final StackHandler h, final DataTransform t) {
		super(h, t);
	}

	// -- StackLink API methods --

	/** Gets references corresponding to each plane in the image stack. */
	public DataReferenceImpl[] getReferences() {
		final DataReferenceImpl[] refs = new DataReferenceImpl[references.size()];
		references.copyInto(refs);
		return refs;
	}

	/** Gets renderers corresponding to each plane in the image stack. */
	public DataRenderer[] getRenderers() {
		final DataRenderer[] rends = new DataRenderer[renderers.size()];
		renderers.copyInto(rends);
		return rends;
	}

	/** Assigns the given axis as the stack axis. */
	public void setStackAxis(final int axis) {
		if (axisValid && axis == stackAxis) return; // no change
		stackAxis = axis;
		references.removeAllElements();
		renderers.removeAllElements();
		lastPos = null;

		// convert slider axis to stack axis for this transform
		final int[] lengths = trans.getLengths();
		final int len = axis >= 0 && axis < lengths.length ? lengths[axis] : 1;

		// build reference/renderer pairs
		final String name = trans.getName();
		final DisplayRenderer dr =
			handler.getWindow().getDisplay().getDisplayRenderer();
		try {
			for (int i = 0; i < len; i++) {
				references.add(new DataReferenceImpl(name + i));
			}
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		axisValid = true;
	}

	/** Gets the currently assigned stack axis. */
	public int getStackAxis() {
		return stackAxis;
	}

	/** Gets the number of slices, assuming the currently assigned stack axis. */
	public int getSliceCount() {
		return references.size();
	}

	/**
	 * Gets the current slice index, assuming the currently assigned stack axis.
	 */
	public int getCurrentSlice() {
		if (handler == null) return currentSlice;
		final int[] pos = handler.getPos(trans);
		return stackAxis >= 0 && stackAxis < pos.length ? pos[stackAxis] : -1;
	}

	/** Enables or disables visibility at the specified slice index. */
	public void setSliceVisible(final int slice, final boolean vis) {
		if (slice < 0 || slice >= references.size()) return;
		renderers.elementAt(slice).toggle(vis);
	}

	/** Gets visibility at the specified slice index. */
	public boolean isSliceVisible(final int slice) {
		if (renderers == null) {
			return visSlices != null && slice >= 0 && slice < visSlices.length
				? visSlices[slice] : false;
		}
		if (slice < 0 || slice >= renderers.size()) return false;
		return renderers.elementAt(slice).getEnabled();
	}

	/** Enables or disables visibility of the yellow bounding box. */
	public void setBoundingBoxVisible(final boolean vis) {
		super.setVisible(vis);
	}

	/** Gets visibility of the given data transform's yellow bounding box. */
	public boolean isBoundingBoxVisible() {
		return super.isVisible();
	}

	/** Enables or disables volume rendering. */
	public void setVolumeRendered(final boolean volume) {
		if (this.volume == volume) return;
		this.volume = volume;
		if (volume) {
			if (colorHandler.getOpacityModel() != ColorUtil.CURVED_ALPHA ||
				colorHandler.getOpacityValue() == 255)
			{
				// guess at good default opacity settings
				final int opacity = 100 - volumeRes / 4;
				colorHandler.setOpacity(opacity, ColorUtil.CURVED_ALPHA, true);
			}

			// save slice visibility settings and hide slice data
			final int len = renderers.size();
			visSlices = new boolean[len];
			for (int s = 0; s < len; s++) {
				final DataRenderer sliceRend = renderers.elementAt(s);
				visSlices[s] = sliceRend.getEnabled();
				sliceRend.toggle(false);
			}
		}
		else {
			// restore slice visibility settings
			final int len = renderers.size();
			final boolean all = visSlices == null || visSlices.length != len;
			for (int s = 0; s < len; s++) {
				final DataRenderer sliceRend = renderers.elementAt(s);
				sliceRend.toggle(all ? true : visSlices[s]);
			}
		}
		doTransform(TransformHandler.MINIMUM_BURN_DELAY, true);
	}

	/** Gets status of volume rendering. */
	public boolean isVolumeRendered() {
		return volume;
	}

	/** Sets maximum resolution per axis of rendered volumes. */
	public void setVolumeResolution(final int res) {
		if (volumeRes == res) return;
		volumeRes = res;
		if (volume) doTransform(TransformHandler.MINIMUM_BURN_DELAY, true);
	}

	/** Gets maximum resolution per axis of rendered volumes. */
	public int getVolumeResolution() {
		return volumeRes;
	}

	// -- TransformLink API methods --

	/** Links this stack into the display. */
	@Override
	public void link() {
		final int len = references.size();
		final DisplayImpl display = handler.getWindow().getDisplay();

		// rebuild renderers if necessary
		final int numMaps = colorHandler.getMaps().length;
		final String imageRenderer = "visad.bom.ImageRendererJ3D";
		final boolean isImageRend =
			renderers.size() > 0 &&
				renderers.firstElement().getClass().getName().equals(imageRenderer);
		final boolean[] vis = new boolean[renderers.size()];
		for (int i = 0; i < vis.length; i++) {
			vis[i] = renderers.elementAt(i).getEnabled();
		}
		if (numMaps == 1 && !isImageRend) {
			// use ImageRendererJ3D when possible
			renderers.removeAllElements();
			try {
				final Class<?> c = Class.forName(imageRenderer);
				for (int i = 0; i < len; i++) {
					final DataRenderer dr = (DataRenderer) c.newInstance();
					renderers.addElement(dr);
					if (vis.length > i) dr.toggle(vis[i]);
				}
			}
			catch (final NoClassDefFoundError err) {}
			catch (final ClassNotFoundException exc) {
				exc.printStackTrace();
			}
			catch (final IllegalAccessException exc) {
				exc.printStackTrace();
			}
			catch (final InstantiationException exc) {
				exc.printStackTrace();
			}
			catch (final RuntimeException exc) {
				// HACK: workaround for bug in Apache Axis2
				final String msg = exc.getMessage();
				if (msg != null && msg.indexOf("ClassNotFound") < 0) throw exc;
				exc.printStackTrace();
			}
		}
		else if (numMaps > 1 && isImageRend) {
			// ImageRendererJ3D does not allow multiple mappings to Display.RGBA
			renderers.removeAllElements();
		}
		if (renderers.size() == 0) {
			for (int i = 0; i < len; i++) {
				final DataRenderer dr =
					display.getDisplayRenderer().makeDefaultRenderer();
				renderers.addElement(dr);
				if (vis.length > i) dr.toggle(vis[i]);
			}
		}
		if (rend == null) {
			rend = display.getDisplayRenderer().makeDefaultRenderer();
			rend.toggle(visible);
		}

		// link in the transform
		try {
			// add image slices
			if (stackAxis < 0) {
				display.addReferences(renderers.elementAt(0), references.elementAt(0));
			}
			else {
				for (int i = 0; i < len; i++) {
					final DataRenderer dataRend = renderers.elementAt(i);
					final DataReference dataRef = references.elementAt(i);
					display.addReferences(dataRend, dataRef);
					setZLevel(dataRend, i, len);
				}
			}

			// add yellow bounding box
			display.addReferences(rend, ref,
				new ConstantMap[] { new ConstantMap(1, Display.Red),
					new ConstantMap(1, Display.Green), new ConstantMap(0, Display.Blue),
					new ConstantMap(3, Display.LineWidth) });

			// add volume
			display.addReference(volumeRef);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

	/** Unlinks this stack from the display. */
	@Override
	public void unlink() {
		try {
			// remove image slices
			final int len = stackAxis < 0 ? 1 : references.size();
			final DisplayImpl display = handler.getWindow().getDisplay();
			for (int i = 0; i < len; i++) {
				display.removeReference(references.elementAt(i));
			}

			// remove yellow bounding box
			display.removeReference(ref);

			// remove volume reference
			display.removeReference(volumeRef);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

	/** Toggles visibility of the transform. */
	@Override
	public void setVisible(final boolean vis) {
		for (int i = 0; i < renderers.size(); i++) {
			renderers.elementAt(i).toggle(vis);
		}
	}

	/** Gets visibility of the transform. */
	@Override
	public boolean isVisible() {
		boolean vis = false;
		for (int i = 0; i < renderers.size(); i++) {
			if (renderers.elementAt(i).getEnabled()) {
				vis = true;
				break;
			}
		}
		return vis;
	}

	// -- Dynamic API methods --

	/** Tests whether two dynamic objects are equivalent. */
	@Override
	public boolean matches(final Dynamic dyn) {
		if (!isCompatible(dyn)) return false;
		final StackLink link = (StackLink) dyn;

		final int num = getSliceCount();
		if (num != link.getSliceCount()) return false;
		for (int i = 0; i < num; i++) {
			if (isSliceVisible(i) != link.isSliceVisible(i)) return false;
		}

		return super.matches(link) && getStackAxis() == link.getStackAxis() &&
			getCurrentSlice() == link.getCurrentSlice() &&
			isVolumeRendered() == link.isVolumeRendered() &&
			getVolumeResolution() == link.getVolumeResolution();
	}

	/**
	 * Tests whether the given dynamic object can be used as an argument to
	 * initState, for initializing this dynamic object.
	 */
	@Override
	public boolean isCompatible(final Dynamic dyn) {
		return dyn instanceof StackLink;
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

		references = new Vector<DataReference>();
		renderers = new Vector<DataRenderer>();
		try {
			volumeRef = new DataReferenceImpl(trans.getName() + "_volume");
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}

		if (!axisValid) {
			// auto-detect stack axis
			stackAxis = -1;
			final String[] dims = trans.getDimTypes();
			if (dims.length > 0) stackAxis = 0;
			for (int i = 0; i < dims.length; i++) {
				if (dims[i].equals("Slice")) {
					stackAxis = i;
					break;
				}
			}
		}
		axisValid = false; // make sure display references get rebuilt
		setStackAxis(stackAxis);
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("LinkedData"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "StackLink");
		child.setAttribute("id", "" + trans.getTransformId());
		if (colorHandler != null) colorHandler.saveState(child);
		child.setAttribute("visible", "" + isVisible());

		// save stack parameters
		child.setAttribute("axis", "" + stackAxis);
		child.setAttribute("slice", "" + getCurrentSlice());
		final int sliceCount = getSliceCount();
		final StringBuffer sb = new StringBuffer(sliceCount);
		for (int i = 0; i < sliceCount; i++)
			sb.append(isSliceVisible(i) ? "y" : "n");
		child.setAttribute("visibleSlices", sb.toString());
		child.setAttribute("isVolume", "" + isVolumeRendered());
		child.setAttribute("volumeResolution", "" + getVolumeResolution());
	}

	/** Restores the current state from the given DOM element ("StackLink"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		super.restoreState(el);

		// restore stack parameters
		stackAxis = Integer.parseInt(el.getAttribute("axis"));
		axisValid = true; // do not detect axis if this link is later initialized
		currentSlice = Integer.parseInt(el.getAttribute("slice"));
		final String s = el.getAttribute("visibleSlices");
		visSlices = new boolean[s.length()];
		for (int i = 0; i < visSlices.length; i++)
			visSlices[i] = s.charAt(i) == 'y';
		volume = el.getAttribute("isVolume").equalsIgnoreCase("true");
		volumeRes = Integer.parseInt(el.getAttribute("volumeResolution"));
	}

	// -- Internal StackLink API methods

	/**
	 * Assigns the given data object to the specified data reference, switching to
	 * the proper types if the flag is set.
	 */
	protected void setData(Data d, final DataReference dataRef,
		final DataRenderer dataRend, final boolean autoSwitch, final double zval)
	{
		if (autoSwitch && d instanceof FlatField && trans instanceof ImageTransform)
		{
			// special case: use ImageTransform's suggested MathType instead
			final FlatField ff = (FlatField) d;
			final ImageTransform it = (ImageTransform) trans;
			final FunctionType ftype = it.getType();
			final Unit[] imageUnits = it.getImageUnits();
			try {
				d = DataUtil.switchType(ff, ftype, imageUnits);
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
			catch (final RemoteException exc) {
				exc.printStackTrace();
			}
		}
		try {
			dataRef.setData(d);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

	/**
	 * Assigns the given value to the specified data renderer's ConstantMap to the
	 * Z axis.
	 */
	protected void setZLevel(final DataRenderer rend, double zval, final int len)
	{
		zval = 2 * zval / (len - 1) - 1;
		try {
			final ConstantMap zmap = new ConstantMap(zval, Display.ZAxis);
			rend.getLinks()[0].setConstantMaps(new ConstantMap[] { zmap });
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

	// -- Internal TransformLink API methods --

	/** Updates the currently displayed data for the given transform. */
	@Override
	protected void doTransform() {
		boolean doBox = false, doRefs = false;

		// get dimensional position of this transform
		final int[] pos = handler.getPos(trans);

		// check whether dimensional position has changed
		if (lastPos == null || lastPos.length != pos.length) doBox = doRefs = true;
		else {
			for (int i = 0; i < pos.length; i++) {
				if (lastPos[i] != pos[i]) {
					if (i == stackAxis) doBox = true;
					else doRefs = true;
				}
			}
		}
		lastPos = pos;

		final int len = references.size();
		if (doBox) {
			// compute yellow bounding box
			final ImageTransform it = (ImageTransform) trans;
			final int xres = it.getImageWidth();
			final int yres = it.getImageHeight();
			final float zval = stackAxis < 0 ? 0.0f : (float) pos[stackAxis];
			final float[][] samps =
				{ { 0, xres - 1, xres - 1, 0, 0 }, { 0, 0, yres - 1, yres - 1, 0 },
					{ zval, zval, zval, zval, zval } };
			final RealType xbox = it.getXType();
			final RealType ybox = it.getYType();
			final RealType zbox = it.getZType();
			final Unit[] imageUnits = it.getImageUnits();
			final Unit[] xyzUnits =
				{ imageUnits[0], imageUnits[1], it.getZUnit(stackAxis) };
			try {
				final RealTupleType xyz = new RealTupleType(xbox, ybox, zbox);
				setData(new Gridded3DSet(xyz, samps, 5, null, xyzUnits, null, false));
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
		}

		if (doRefs) super.doTransform();
	}

	/**
	 * Computes the reference data at the current position, utilizing thumbnails
	 * as appropriate.
	 */
	@Override
	protected synchronized void computeData(final boolean thumbs) {
		final int[] pos = handler.getPos(trans);
		final ThumbnailHandler th = trans.getThumbHandler();
		final int len = references.size();

		// check whether dimensional position has changed
		final DataCache cache = handler.getCache();
		if (!thumbs) {
			if (cachedPos != null && cachedPos.length == pos.length) {
				for (int i = 0; i < pos.length; i++) {
					if (cachedPos[i] != pos[i] && i != stackAxis) {
						// for now, simply dump old full-resolution data
						for (int s = 0; s < len; s++) {
							if (stackAxis >= 0) cachedPos[stackAxis] = s;
							cache.dump(trans, cachedPos, null);
						}
						// also dump old collapsed image stack
						cache.dump(trans, cachedPos, "collapse");
						break;
					}
				}
			}
			cachedPos = pos;
		}

		// retrieve collapsed image stack from data cache
		FlatField collapse = (FlatField) cache.getData(trans, pos, "collapse", 3);

		// compute image data at each slice
		final DisplayImpl display = handler.getWindow().getDisplay();
		DisplayUtil.setDisplayDisabled(display, true);
		final FlatField[] slices = new FlatField[len];
		for (int s = 0; s < len; s++) {
			if (stackAxis >= 0) pos[stackAxis] = s;

			final Data thumb = th == null ? null : th.getThumb(pos);
			final DataReferenceImpl sliceRef =
				(DataReferenceImpl) references.elementAt(s);
			final DataRenderer sliceRend = renderers.elementAt(s);
			if (thumbs) setData(thumb, sliceRef, sliceRend, true, s);
			else {
				if (!volume || collapse == null) {
					// load data from disk, unless collapsed volume is current
					setMessage("loading full-resolution data (" + (s + 1) + "/" + len +
						")");
					slices[s] = (FlatField) getImageData(pos);
					if (th != null && thumb == null && !volume) {
						// fill in missing thumbnail
						th.setThumb(pos, th.makeThumb(slices[s]));
					}
				}
				if (!volume) setData(slices[s], sliceRef, sliceRend, true, s);
			}
		}
		if (stackAxis >= 0) pos[stackAxis] = 0;

		// compute volume data
		if (thumbs) setData(DUMMY, volumeRef, false);
		else {
			if (volume) {
				// render slices as a volume
				final String res = volumeRes + "x" + volumeRes + "x" + volumeRes;
				setMessage("constructing " + res + " volume");
				final ImageTransform it = (ImageTransform) trans;
				final RealType zType = it.getZType();
				final FunctionType imageType = it.getType();
				final Unit[] imageUnits = it.getImageUnits();
				final Unit zUnit = it.getZUnit(stackAxis);
				try {
					if (collapse == null) {
						// use image transform's recommended MathType
						for (int i = 0; i < len; i++) {
							slices[i] = DataUtil.switchType(slices[i], imageType, imageUnits);
						}
						// compile slices into a single volume and collapse
						collapse =
							DataUtil.collapse(DataUtil.makeField(slices, zType, 0, len - 1,
								zUnit));
						cache.putData(trans, pos, "collapse", collapse);
					}
					// resample volume
					setData(DataUtil.makeCube(collapse, volumeRes), volumeRef, false);
					setMessage("rendering " + res + " volume");
				}
				catch (final VisADException exc) {
					exc.printStackTrace();
				}
				catch (final RemoteException exc) {
					exc.printStackTrace();
				}
			}
			else {
				// slice data is already set; just display burn-in message
				setData(DUMMY, volumeRef, false);
				setMessage("burning in full-resolution data");
			}
			clearWhenDone = true;
		}
		DisplayUtil.setDisplayDisabled(display, false);
	}

	/** Gets 2D data from the specified data transform. */
	@Override
	protected Data getImageData(final int[] pos) {
		final Data d = super.getImageData(pos);
		if (!(d instanceof FlatField)) return d;
		final FlatField ff = (FlatField) d;

		final GriddedSet set = (GriddedSet) ff.getDomainSet();
		final int[] len = set.getLengths();
		final int[] res = new int[len.length];
		final int[] maxRes = handler.getWindow().getManager().getStackResolution();
		for (int i = 0; i < len.length; i++) {
			if (len[i] > maxRes[i]) res[i] = maxRes[i];
			else res[i] = len[i];
		}
		try {
			return DataUtil.resample(ff, res, null);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		return null;
	}

	/** Computes range values at the current cursor location. */
	@Override
	protected void computeCursor() {
		// check for active cursor
		cursor = null;
		final DisplayImpl display = handler.getWindow().getDisplay();
		final DisplayRenderer dr = display.getDisplayRenderer();
		final Vector<?> cursorStringVector = dr.getCursorStringVector();
		if (cursorStringVector == null || cursorStringVector.size() == 0) return;

		// get cursor value
		final double[] cur = dr.getCursor();
		if (cur == null || cur.length == 0 || cur[0] != cur[0]) return;

		// get range values at the given cursor location
		if (!(trans instanceof ImageTransform)) return;
		final ImageTransform it = (ImageTransform) trans;

		// get cursor's domain coordinates
		final RealType xType = it.getXType();
		final RealType yType = it.getYType();
		final RealType zType = it.getZType();
		final double[] domain =
			CursorUtil.cursorToDomain(display,
				new RealType[] { xType, yType, zType }, cur);

		// determine which slice to probe
		int index = -1;
		final int len = references.size();
		double step = it.getMicronStep(stackAxis);
		if (step != step) step = 1;
		final double zpos = Math.round(domain[2] / step);
		if (zpos >= 0 && zpos < len) index = (int) zpos;
		if (index < 0) return;

		final DataReferenceImpl sliceRef =
			(DataReferenceImpl) references.elementAt(index);

		// get data at appropriate slice
		final Data data = sliceRef.getData();
		if (!(data instanceof FunctionImpl)) return;
		final FunctionImpl func = (FunctionImpl) data;

		// evaluate function at the cursor location
		double[] rangeValues = null;
		try {
			rangeValues = CursorUtil.evaluate(func, domain);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}

		// compile range value messages
		if (rangeValues == null) return;
		final RealType[] range = it.getRangeTypes();
		if (range.length < rangeValues.length) return;

		final int num = stackAxis < 0 ? rangeValues.length : rangeValues.length + 1;
		cursor = new VisADException[num];
		final String prefix = trans.getName() + ": ";
		for (int i = 0; i < rangeValues.length; i++) {
			cursor[i] =
				new VisADException(prefix + range[i].getName() + " = " + rangeValues[i]);
		}
		if (stackAxis >= 0) {
			final String dimType = trans.getDimTypes()[stackAxis];
			cursor[rangeValues.length] =
				new VisADException(prefix + "<" + (stackAxis + 1) + "> " + dimType +
					" = " + trans.getLabels()[stackAxis][index]);
		}
	}

}

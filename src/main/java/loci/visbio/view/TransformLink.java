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

import java.awt.Font;
import java.rmi.RemoteException;
import java.util.Vector;

import loci.visbio.VisBioFrame;
import loci.visbio.data.DataManager;
import loci.visbio.data.DataTransform;
import loci.visbio.data.ImageTransform;
import loci.visbio.data.ThumbnailHandler;
import loci.visbio.data.TransformEvent;
import loci.visbio.data.TransformListener;
import loci.visbio.state.Dynamic;
import loci.visbio.state.SaveException;
import loci.visbio.state.Saveable;
import loci.visbio.util.DataUtil;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.ObjectUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

import visad.Data;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.DataRenderer;
import visad.Display;
import visad.DisplayEvent;
import visad.DisplayImpl;
import visad.DisplayListener;
import visad.DisplayRenderer;
import visad.FlatField;
import visad.FunctionImpl;
import visad.FunctionType;
import visad.RealType;
import visad.ScalarMap;
import visad.TextControl;
import visad.Unit;
import visad.VisADException;
import visad.util.CursorUtil;
import visad.util.Util;

/**
 * Represents a link between a data transform and a display that produces a
 * single image.
 */
public class TransformLink implements DisplayListener, Dynamic, Runnable,
	Saveable, TransformListener
{

	// -- Fields --

	/** Associated transform handler. */
	protected TransformHandler handler;

	/** Data transform linked to the display. */
	protected DataTransform trans;

	/** Associated handler for managing this link's color settings. */
	protected ColorHandler colorHandler;

	/** Data reference linking data to the display. */
	protected DataReferenceImpl ref;

	/** Data renderer for toggling data's visibility and other parameters. */
	protected DataRenderer rend;

	/** Separate thread for managing full-resolution burn-in. */
	protected Thread burnThread;

	/** Whether a full-resolution burn-in should occur immediately. */
	protected boolean burnNow;

	/** Next clock time a full-resolution burn-in should occur. */
	protected long burnTime;

	/** Whether this link is still active. */
	protected boolean alive = true;

	/** Status message, to be displayed in bottom left corner. */
	protected VisADException status;

	/**
	 * Range values for current cursor location, to be displayed in bottom left
	 * corner.
	 */
	protected VisADException[] cursor;

	/** Whether a TRANSFORM_DONE event should clear the status message. */
	protected boolean clearWhenDone;

	/** Last cached dimensional position of the link. */
	protected int[] cachedPos;

	// -- Fields - initial state --

	/** Whether data transform is visible onscreen. */
	protected boolean visible;

	// -- Constructor --

	/** Constructs an uninitialized transform link. */
	public TransformLink(final TransformHandler h) {
		handler = h;
	}

	/**
	 * Creates a link between the given data transform and the specified transform
	 * handler's display.
	 */
	public TransformLink(final TransformHandler h, final DataTransform t) {
		handler = h;
		trans = t;
		if (trans instanceof ImageTransform) colorHandler = new ColorHandler(this);
		visible = true;
		initState(null);
	}

	// -- TransformLink API methods --

	/** Gets the link's transform handler. */
	public TransformHandler getHandler() {
		return handler;
	}

	/** Gets the link's data transform. */
	public DataTransform getTransform() {
		return trans;
	}

	/** Gets the link's color handler. */
	public ColorHandler getColorHandler() {
		return colorHandler;
	}

	/** Gets the link's reference. */
	public DataReferenceImpl getReference() {
		return ref;
	}

	/** Gets the link's renderer. */
	public DataRenderer getRenderer() {
		return rend;
	}

	/** Links this transform into the display. */
	public void link() {
		// rebuild renderer if necessary
		final int numMaps =
			colorHandler == null ? -1 : colorHandler.getMaps().length;
		final String imageRenderer = "visad.bom.ImageRendererJ3D";
		final boolean isImageRend =
			rend != null && rend.getClass().getName().equals(imageRenderer);
		if (numMaps == 1 && !isImageRend) {
			// use ImageRendererJ3D when possible
			rend = null;
			try {
				final Class c = Class.forName(imageRenderer);
				rend = (DataRenderer) c.newInstance();
				rend.toggle(visible);
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
			rend = null;
		}
		if (rend == null) {
			final DisplayImpl display = handler.getWindow().getDisplay();
			rend = display.getDisplayRenderer().makeDefaultRenderer();
			rend.toggle(visible);
		}

		// link in the transform
		try {
			handler.getWindow().getDisplay().addReferences(rend, ref);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

	/** Unlinks this transform from the display. */
	public void unlink() {
		try {
			handler.getWindow().getDisplay().removeReference(ref);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

	/** Frees resources being consumed by this transform link. */
	public void destroy() {
		alive = false;
	}

	/** Toggles visibility of the transform. */
	public void setVisible(final boolean vis) {
		rend.toggle(vis);
	}

	/** Gets visibility of the transform. */
	public boolean isVisible() {
		return rend == null ? visible : rend.getEnabled();
	}

	/** Sets status messages displayed in display's bottom left-hand corner. */
	public void setMessage(final String msg) {
		if (trans.isImmediate()) return; // no messages in immediate mode
		status =
			msg == null ? null : new VisADException(trans.getName() + ": " + msg);
		doMessages(false);
	}

	// -- DisplayListener API methods --

	/** Handles VisAD display events. */
	@Override
	public void displayChanged(final DisplayEvent e) {
		// ensure status messages stay visible
		final int id = e.getId();
		if (id == DisplayEvent.FRAME_DONE) {
			computeCursor();
			doMessages(true);
		}
		else if (e.getId() == DisplayEvent.TRANSFORM_DONE) {
			if (clearWhenDone) {
				setMessage(null);
				clearWhenDone = false;
			}
			else doMessages(false);
		}

		// pass along DisplayEvents to linked transform
		trans.displayChanged(e);
	}

	// -- Dynamic API methods --

	/** Tests whether two dynamic objects are equivalent. */
	@Override
	public boolean matches(final Dynamic dyn) {
		if (!isCompatible(dyn)) return false;
		final TransformLink link = (TransformLink) dyn;
		return getTransform().matches(link.getTransform()) &&
			isVisible() == link.isVisible();
	}

	/**
	 * Tests whether the given dynamic object can be used as an argument to
	 * initState, for initializing this dynamic object.
	 */
	@Override
	public boolean isCompatible(final Dynamic dyn) {
		return dyn instanceof TransformLink;
	}

	/**
	 * Modifies this object's state to match that of the given object. If the
	 * argument is null, the object is initialized according to its current state
	 * instead.
	 */
	@Override
	public void initState(final Dynamic dyn) {
		if (dyn != null && !isCompatible(dyn)) return;

		if (burnThread != null) {
			alive = false;
			try {
				burnThread.join();
			}
			catch (final InterruptedException exc) {}
			alive = true;
		}

		final TransformLink link = (TransformLink) dyn;
		if (link != null) {
			if (trans != null) trans.removeTransformListener(this);
			trans = link.getTransform();
			if (trans instanceof ImageTransform && colorHandler == null) {
				colorHandler = new ColorHandler(this);
			}
			if (colorHandler != null) colorHandler.initState(link.getColorHandler());
			visible = link.isVisible();
		}
		else if (colorHandler != null) colorHandler.initState(null);

		// remove old data reference
		final DisplayImpl display = handler.getWindow().getDisplay();
		if (ref != null) {
			try {
				display.removeReference(ref);
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
			catch (final RemoteException exc) {
				exc.printStackTrace();
			}
		}

		// build data reference
		try {
			ref = new DataReferenceImpl(trans.getName());
			display.addDisplayListener(this);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}

		// listen for changes to this transform
		trans.addTransformListener(this);

		// initialize thread for handling full-resolution burn-in operations
		if (!trans.isImmediate()) {
			burnThread =
				new Thread(this, "VisBio-BurnThread-" + handler.getWindow().getName() +
					":" + trans.getName());
			burnThread.start();
		}
	}

	/**
	 * Called when this object is being discarded in favor of another object with
	 * a matching state.
	 */
	@Override
	public void discard() {
		destroy();
	}

	// -- Runnable API methods --

	/** Executes full-resolution burn-in operations. */
	@Override
	public void run() {
		while (true) {
			// wait until a new burn-in is requested
			if (!alive) break;
			while (System.currentTimeMillis() > burnTime && !burnNow) {
				try {
					Thread.sleep(50);
				}
				catch (final InterruptedException exc) {}
			}
			burnNow = false;

			// wait until appointed burn-in time (which could change during the wait)
			long time;
			while ((time = System.currentTimeMillis()) < burnTime) {
				if (!alive) break;
				final long wait = burnTime - time;
				if (wait >= 1000) {
					final long seconds = wait / 1000;
					setMessage(seconds + " second" + (seconds == 1 ? "" : "s") +
						" until burn in");
					try {
						Thread.sleep(1000);
					}
					catch (final InterruptedException exc) {}
				}
				else {
					try {
						Thread.sleep(wait);
					}
					catch (final InterruptedException exc) {}
				}
			}

			// burn-in full resolution data
			if (!alive) break;
			computeData(false);
		}
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("LinkedData"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "TransformLink");
		child.setAttribute("id", "" + trans.getTransformId());
		if (colorHandler != null) colorHandler.saveState(child);
		child.setAttribute("visible", "" + isVisible());
	}

	/**
	 * Restores the current state from the given DOM element ("TransformLink").
	 */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final VisBioFrame bio = handler.getWindow().getVisBio();
		final DataManager dm = (DataManager) bio.getManager(DataManager.class);
		trans = dm.getDataById(Integer.parseInt(el.getAttribute("id")));
		if (trans instanceof ImageTransform && colorHandler == null) {
			colorHandler = new ColorHandler(this);
		}
		if (colorHandler != null) colorHandler.restoreState(el);
		visible = el.getAttribute("visible").equalsIgnoreCase("true");
	}

	// -- TransformListener API methods --

	/** Called when a data transform's parameters are updated. */
	@Override
	public void transformChanged(final TransformEvent e) {
		final int id = e.getId();
		if (id == TransformEvent.DATA_CHANGED) {
			doTransform(TransformHandler.MINIMUM_BURN_DELAY);
		}
		else if (id == TransformEvent.FONT_CHANGED) {
			Font font = trans.getFont();

			// HACK - always use font size 8, since it renders faster
			// and has virtually no effect on rendering size
			if (font != null) font = new Font(font.getName(), font.getStyle(), 8);

			// compile list of potential maps to Display.Text
			final Vector textMaps = new Vector();
			final ScalarMap[] maps = trans.getSuggestedMaps();
			for (int i = 0; i < maps.length; i++) {
				if (maps[i].getDisplayScalar().equals(Display.Text)) {
					textMaps.add(maps[i]);
				}
			}

			// search display for matching text maps
			final DisplayImpl display = handler.getWindow().getDisplay();
			final Vector mapVector = display.getMapVector();
			for (int i = 0; i < mapVector.size(); i++) {
				final ScalarMap map = (ScalarMap) mapVector.elementAt(i);
				for (int j = 0; j < textMaps.size(); j++) {
					final ScalarMap textMap = (ScalarMap) textMaps.elementAt(j);
					if (map.equals(textMap)) {
						// update font for matching text map
						final TextControl textControl = (TextControl) map.getControl();
						if (textControl != null) {
							try {
								textControl.setFont(font);
							}
							catch (final VisADException exc) {
								exc.printStackTrace();
							}
							catch (final RemoteException exc) {
								exc.printStackTrace();
							}
						}
					}
				}
			}
		}
	}

	// -- Internal TransformLink API methods --

	/** Updates displayed data based on current dimensional position. */
	protected void doTransform() {
		doTransform(handler.getBurnDelay());
	}

	/** Updates displayed data based on current dimensional position. */
	protected void doTransform(final long delay) {
		doTransform(delay, false);
	}

	/** Updates displayed data based on current dimensional position. */
	protected void doTransform(final long delay, final boolean now) {
		final String append = handler.getWindow().getName() + ":" + trans.getName();
		final boolean immediate = now || trans.isImmediate();
		final long burnDelay = delay;
		new Thread("VisBio-ComputeDataThread-" + append) {

			@Override
			public void run() {
				final int[] pos = handler.getPos(trans);
				if (handler.getCache().hasData(trans, pos, null)) {
					// cache hit; burn in immediately
					burnTime = System.currentTimeMillis();
					burnNow = true;
				}
				else if (immediate) computeData(false);
				else {
					computeData(true);
					// request a new burn-in in delay milliseconds
					burnTime = System.currentTimeMillis() + burnDelay;
					if (burnDelay < 100) burnNow = true;
				}
			}
		}.start();
	}

	/**
	 * Computes the reference data at the current position, utilizing thumbnails
	 * as appropriate.
	 */
	protected synchronized void computeData(final boolean thumbs) {
		int dim = handler.getWindow().is3D() ? 3 : 2;
		if (dim == 3 && !trans.isValidDimension(3)) dim = 2;
		if (!trans.isValidDimension(dim)) {
			System.err.println("Warning: display \"" + handler.getWindow().getName() +
				"\" is incapable of showing data \"" + trans.getName() + "\"");
			return;
		}

		final int[] pos = handler.getPos(trans);
		// need to change pos[stackAxis] = -1
		// pos[stackAxis] = -1;
		final ThumbnailHandler th = trans.getThumbHandler();
		final Data thumb = th == null ? null : th.getThumb(pos);
		if (thumbs) setData(thumb);
		else {
			setMessage("loading full-resolution data");
			if (!ObjectUtil.arraysEqual(pos, cachedPos)) {
				// for now, simply dump old full-resolution data
				handler.getCache().dump(trans, cachedPos, null);
			}
			cachedPos = pos;
			final Data d =
				dim == 3 ? trans.getData(this, pos, 3, handler.getCache())
					: getImageData(pos);
			if (th != null && thumb == null) {
				// fill in missing thumbnail
				th.setThumb(pos, th.makeThumb(d));
			}
			setMessage("burning in full-resolution data");
			clearWhenDone = true;
			setData(d);
			if (colorHandler != null) colorHandler.reAutoScale();
		}
	}

	/** Gets the transform's data at the given dimensional position. */
	protected Data getImageData(final int[] pos) {
		return trans.getData(this, pos, 2, handler.getCache());
	}

	/** Assigns the given data object to the data reference. */
	protected void setData(final Data d) {
		setData(d, ref, true);
	}

	/**
	 * Assigns the given data object to the data reference, switching to the
	 * proper types if the flag is set.
	 */
	protected void setData(final Data d, final boolean autoSwitch) {
		setData(d, ref, autoSwitch);
	}

	/** Assigns the given data object to the given data reference. */
	protected void setData(final Data d, final DataReference dataRef) {
		setData(d, dataRef, true);
	}

	/**
	 * Assigns the given data object to the given data reference, switching to the
	 * proper types if the flag is set.
	 */
	protected void setData(Data d, final DataReference dataRef,
		final boolean autoSwitch)
	{
		if (autoSwitch && d instanceof FlatField && trans instanceof ImageTransform)
		{
			// special case: use ImageTransform's suggested MathType instead
			final FlatField ff = (FlatField) d;
			final ImageTransform it = (ImageTransform) trans;
			final FunctionType ftype = it.getType();
			final Unit[] units = it.getImageUnits();
			try {
				d = DataUtil.switchType(ff, ftype, units);
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

	/** Computes range values at the current cursor location. */
	protected void computeCursor() {
		// check for active cursor
		cursor = null;
		final DisplayImpl display = handler.getWindow().getDisplay();
		final DisplayRenderer dr = display.getDisplayRenderer();
		final Vector cursorStringVector = dr.getCursorStringVector();
		if (cursorStringVector == null || cursorStringVector.size() == 0) return;

		// get cursor value
		final double[] cur = dr.getCursor();
		if (cur == null || cur.length == 0 || cur[0] != cur[0]) return;

		// get range values at the given cursor location
		if (!(trans instanceof ImageTransform)) return;
		final ImageTransform it = (ImageTransform) trans;

		// retrieve data object to be probed
		final Data data = ref.getData();
		if (!(data instanceof FunctionImpl)) return;
		final FunctionImpl func = (FunctionImpl) data;

		// get cursor's domain coordinates
		final double[] domain =
			CursorUtil.cursorToDomain(display, new RealType[] { it.getXType(),
				it.getYType(), null }, cur);

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

		cursor = new VisADException[rangeValues.length];
		final String prefix = trans.getName() + ": ";
		for (int i = 0; i < rangeValues.length; i++) {
			cursor[i] =
				new VisADException(prefix + range[i].getName() + " = " + rangeValues[i]);
		}
	}

	// -- Helper methods --

	/**
	 * Assigns the current status and cursor messages to the data renderer and
	 * redraws the display, optionally using the Swing event thread.
	 */
	private void doMessages(final boolean swing) {
		if (rend == null) return;
		final Vector oldList = rend.getExceptionVector();
		final Vector newList = new Vector();
		if (cursor != null) {
			for (int i = 0; i < cursor.length; i++)
				newList.add(cursor[i]);
		}
		if (status != null) newList.add(status);

		boolean equal = true;
		if (oldList == null) equal = false;
		else {
			final int len = oldList.size();
			if (newList.size() != len) equal = false;
			else {
				for (int i = 0; i < len; i++) {
					final VisADException oldExc = (VisADException) oldList.elementAt(i);
					final VisADException newExc = (VisADException) newList.elementAt(i);
					if (!oldExc.getMessage().equals(newExc.getMessage())) {
						equal = false;
						break;
					}
				}
			}
		}
		if (equal) return; // no change;

		rend.clearExceptions();
		final int len = newList.size();
		for (int i = 0; i < len; i++) {
			rend.addException((VisADException) newList.elementAt(i));
		}

		if (swing) {
			Util.invoke(false, new Runnable() {

				@Override
				public void run() {
					DisplayUtil.redrawMessages(handler.getWindow().getDisplay());
				}
			});
		}
		else DisplayUtil.redrawMessages(handler.getWindow().getDisplay());
	}

}

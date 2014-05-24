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

import loci.visbio.VisBioFrame;
import loci.visbio.data.DataTransform;
import loci.visbio.data.ImageTransform;
import loci.visbio.state.NumericOption;
import loci.visbio.state.OptionManager;
import loci.visbio.state.SaveException;
import loci.visbio.state.Saveable;
import loci.visbio.util.DisplayUtil;
import loci.visbio.util.ObjectUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

import visad.DisplayImpl;
import visad.DisplayRenderer;
import visad.GraphicsModeControl;
import visad.ProjectionControl;
import visad.VisADException;
import visad.java2d.DisplayImplJ2D;

/**
 * Provides logic for controlling a VisAD display's view.
 */
public class ViewHandler implements Saveable {

	// -- Constants --

	/** Default zoom factor for 2D displays. */
	public static final double DEFAULT_ZOOM_2D = 0.90;

	/** Default zoom factor for 3D displays. */
	public static final double DEFAULT_ZOOM_3D = 0.5;

	/** Default rotation factor for 3D displays. */
	public static final double DEFAULT_ROTATION = 80;

	/** How far display zooms in or out each time. */
	public static final double ZOOM_AMOUNT = 1.5;

	/** How far display rotates each time. */
	public static final double ROTATION_AMOUNT = 15;

	/** How far display pans each time. */
	public static final double PAN_AMOUNT = 0.25;

	// -- Fields --

	/** Associated display window. */
	protected DisplayWindow window;

	/** Aspect ratio of the display. */
	protected double xasp, yasp, zasp;

	/** Whether scale is visible. */
	protected boolean showScale;

	/** Whether bounding box is visible. */
	protected boolean boundingBox;

	/** Whether display is in parallel projection mode. */
	protected boolean parallel;

	// -- Fields - initial state --

	/** Matrix representing current display projection. */
	protected double[] matrix;

	// -- GUI components --

	/** GUI controls for view handler. */
	protected ViewPanel panel;

	// -- Constructor --

	/** Creates a display view handler. */
	public ViewHandler(final DisplayWindow dw) {
		window = dw;

		// default view settings
		xasp = yasp = zasp = 1.0;
		showScale = false;
		boundingBox = true;
		parallel = false;
	}

	// -- ViewHandler API methods --

	/** Gets associated display window. */
	public DisplayWindow getWindow() {
		return window;
	}

	/** Gets GUI controls for this view handler. */
	public ViewPanel getPanel() {
		return panel;
	}

	/** Zooms in on the display. */
	public void zoomIn() {
		zoom(ZOOM_AMOUNT);
	}

	/** Zooms out on the display. */
	public void zoomOut() {
		zoom(1 / ZOOM_AMOUNT);
	}

	/** Rotates the display clockwise (2D and 3D). */
	public void rotateClockwise() {
		rotate(0, 0, ROTATION_AMOUNT);
	}

	/** Rotates the display counterclockwise (2D and 3D). */
	public void rotateCounterclockwise() {
		rotate(0, 0, -ROTATION_AMOUNT);
	}

	/** Rotates the display to the left (3D only). */
	public void rotateLeft() {
		rotate(0, ROTATION_AMOUNT, 0);
	}

	/** Rotates the display to the right (3D only). */
	public void rotateRight() {
		rotate(0, -ROTATION_AMOUNT, 0);
	}

	/** Rotates the display upward (3D only). */
	public void rotateUp() {
		rotate(ROTATION_AMOUNT, 0, 0);
	}

	/** Rotates the display downward (3D only). */
	public void rotateDown() {
		rotate(-ROTATION_AMOUNT, 0, 0);
	}

	/** Slides the display to the left. */
	public void panLeft() {
		pan(-PAN_AMOUNT, 0, 0);
	}

	/** Slides the display to the right. */
	public void panRight() {
		pan(PAN_AMOUNT, 0, 0);
	}

	/** Slides the display upward. */
	public void panUp() {
		pan(0, PAN_AMOUNT, 0);
	}

	/** Slides the display downward. */
	public void panDown() {
		pan(0, -PAN_AMOUNT, 0);
	}

	/** Zooms the display by the given amount. */
	public void zoom(final double scale) {
		final double[] zoom =
			window.getDisplay().make_matrix(0, 0, 0, scale, 0, 0, 0);
		applyMatrix(zoom);
	}

	/** Rotates the display in the given direction. */
	public void rotate(final double rotx, final double roty, final double rotz) {
		final double[] rotate =
			window.getDisplay().make_matrix(rotx, roty, rotz, 1, 0, 0, 0);
		applyMatrix(rotate);
	}

	/** Pans the display in the given direction. */
	public void pan(final double panx, final double pany, final double panz) {
		final double[] pan =
			window.getDisplay().make_matrix(0, 0, 0, 1, panx, pany, panz);
		applyMatrix(pan);
	}

	/** Applies the given matrix transform to the display. */
	public void applyMatrix(final double[] m) {
		final DisplayImpl display = window.getDisplay();
		try {
			final ProjectionControl control = display.getProjectionControl();
			final double[] oldMatrix = control.getMatrix();
			final double[] newMatrix = display.multiply_matrix(m, oldMatrix);
			control.setMatrix(newMatrix);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		final VisBioFrame bio = window.getVisBio();
		bio.generateEvent(bio.getManager(DisplayManager.class),
			"adjust orientation for " + window.getName(), true);
	}

	/** Gets the display's current matrix transform. */
	public double[] getMatrix() {
		final DisplayImpl display = window.getDisplay();
		return display == null ? matrix : display.getProjectionControl()
			.getMatrix();
	}

	/** Restores the display's zoom and orientation to the original values. */
	public void reset() {
		final DisplayImpl display = window.getDisplay();
		if (display instanceof DisplayImplJ2D) {
			try {
				display.getProjectionControl().resetProjection();
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
			catch (final RemoteException exc) {
				exc.printStackTrace();
			}
			doAspect(xasp, yasp, zasp);
		}
		else setMatrix(makeDefaultMatrix());
		final VisBioFrame bio = window.getVisBio();
		bio.generateEvent(bio.getManager(DisplayManager.class),
			"reset orientation for " + window.getName(), true);
	}

	/** Guesses aspect ratio based on first linked data transform. */
	public void guessAspect() {
		final TransformHandler transformHandler = window.getTransformHandler();
		final DataTransform[] trans = transformHandler.getTransforms();
		double x = 1, y = 1, z = 1;
		for (int i = 0; i < trans.length; i++) {
			if (!(trans[i] instanceof ImageTransform)) continue;
			final ImageTransform it = (ImageTransform) trans[i];
			final int w = it.getImageWidth();
			final int h = it.getImageHeight();
			final TransformLink link = transformHandler.getLink(it);
			final int axis =
				link instanceof StackLink ? ((StackLink) link).getStackAxis() : -1;
			final int numSlices = axis < 0 ? 1 : it.getLengths()[axis];
			final double mw = it.getMicronWidth();
			final double mh = it.getMicronHeight();
			final double ms = it.getMicronStep(axis);
			x = mw == mw ? mw : w;
			y = mh == mh ? mh : h;
			z = ms == ms ? (ms * (numSlices - 1)) : (x < y ? x : y);
			break;
		}
		panel.setAspect(x, y, z);
	}

	/** Adjusts the aspect ratio. */
	public void setAspect(double x, double y, final double z) {
		if (x != x || x <= 0) x = 1;
		if (y != y || y <= 0) y = 1;
		final double d = x > y ? x : y;
		final double xx = x / d;
		final double yy = y / d;
		final double zz = (z == z && z > 0) ? z / d : 1.0;
		doAspect(xx, yy, zz);
		xasp = xx;
		yasp = yy;
		zasp = zz;
		final VisBioFrame bio = window.getVisBio();
		bio.generateEvent(bio.getManager(DisplayManager.class),
			"adjust aspect ratio for " + window.getName(), true);
	}

	/** Gets aspect ratio X component. */
	public double getAspectX() {
		return xasp;
	}

	/** Gets aspect ratio Y component. */
	public double getAspectY() {
		return yasp;
	}

	/** Gets aspect ratio Z component. */
	public double getAspectZ() {
		return zasp;
	}

	/** Toggles visibility of scale. */
	public void toggleScale(final boolean value) {
		showScale = value;

		final GraphicsModeControl gmc =
			window.getDisplay().getGraphicsModeControl();
		try {
			gmc.setScaleEnable(value);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		final VisBioFrame bio = window.getVisBio();
		final String endis = value ? "enable" : "disable";
		bio.generateEvent(bio.getManager(DisplayManager.class), endis +
			" scale for " + window.getName(), true);
	}

	/** Gets visibility of scale. */
	public boolean isScale() {
		return showScale;
	}

	/** Toggles visibility of the display's bounding box. */
	public void toggleBoundingBox(final boolean value) {
		boundingBox = value;

		final DisplayRenderer dr = window.getDisplay().getDisplayRenderer();
		try {
			dr.setBoxOn(value);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		final VisBioFrame bio = window.getVisBio();
		final String endis = value ? "enable" : "disable";
		bio.generateEvent(bio.getManager(DisplayManager.class), endis +
			" bounding box for " + window.getName(), true);
	}

	/** Gets visibility of the display's bounding box. */
	public boolean isBoundingBox() {
		return boundingBox;
	}

	/** Toggles whether 3D display uses a parallel projection. */
	public void toggleParallel(final boolean value) {
		if (!window.is3D()) return;
		parallel = value;

		DisplayUtil.setParallelProjection(window.getDisplay(), parallel);
		final VisBioFrame bio = window.getVisBio();
		final String endis = value ? "enable" : "disable";
		bio.generateEvent(bio.getManager(DisplayManager.class), endis +
			" parallel projection mode for " + window.getName(), true);
	}

	/** Gets whether 3D display uses a parallel projection. */
	public boolean isParallel() {
		return parallel;
	}

	// -- ViewHandler API methods - state logic --

	/** Tests whether two objects are in equivalent states. */
	public boolean matches(final ViewHandler handler) {
		return ObjectUtil.arraysEqual(getMatrix(), handler.getMatrix()) &&
			xasp == handler.xasp && yasp == handler.yasp && zasp == handler.zasp &&
			showScale == handler.showScale && boundingBox == handler.boundingBox &&
			parallel == handler.parallel;
	}

	/**
	 * Modifies this object's state to match that of the given object. If the
	 * argument is null, the object is initialized according to its current state
	 * instead.
	 */
	public void initState(final ViewHandler handler) {
		if (handler != null) {
			matrix = handler.getMatrix();
			xasp = handler.xasp;
			yasp = handler.yasp;
			zasp = handler.zasp;
			showScale = handler.showScale;
			boundingBox = handler.boundingBox;
			parallel = handler.parallel;
		}

		if (matrix == null) matrix = makeDefaultMatrix();

		setMatrix(matrix);
		toggleScale(showScale);
		toggleBoundingBox(boundingBox);
		toggleParallel(parallel);

		// configure eye separation distance
		final OptionManager om =
			(OptionManager) window.getVisBio().getManager(OptionManager.class);
		final NumericOption eye =
			(NumericOption) om.getOption(DisplayManager.EYE_DISTANCE);
		final double position = eye.getFloatingValue();
		DisplayUtil.setEyeSeparation(window.getDisplay(), position);

		if (panel == null) panel = new ViewPanel(this);
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("Display"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "Appearance");
		child.setAttribute("matrix", ObjectUtil.arrayToString(getMatrix()));
		child.setAttribute("aspectX", "" + xasp);
		child.setAttribute("aspectY", "" + yasp);
		child.setAttribute("aspectZ", "" + zasp);
		child.setAttribute("showScale", "" + showScale);
		child.setAttribute("boundingBox", "" + boundingBox);
		child.setAttribute("parallel", "" + parallel);
	}

	/** Restores the current state from the given DOM element ("Display"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final Element child = XMLUtil.getFirstChild(el, "Appearance");
		matrix = ObjectUtil.stringToDoubleArray(child.getAttribute("matrix"));
		xasp = ObjectUtil.stringToDouble(child.getAttribute("aspectX"));
		yasp = ObjectUtil.stringToDouble(child.getAttribute("aspectY"));
		zasp = ObjectUtil.stringToDouble(child.getAttribute("aspectZ"));
		showScale = child.getAttribute("showScale").equalsIgnoreCase("true");
		boundingBox = child.getAttribute("boundingBox").equalsIgnoreCase("true");
		parallel = child.getAttribute("parallel").equalsIgnoreCase("true");
	}

	// -- Helper methods --

	/** Adjusts the aspect ratio. */
	protected void doAspect(final double x, final double y, final double z) {
		final DisplayImpl display = window.getDisplay();
		final ProjectionControl pc = display.getProjectionControl();
		if (display instanceof DisplayImplJ2D) {
			try {
				pc.setAspect(new double[] { x, y });
			}
			catch (final VisADException exc) {
				exc.printStackTrace();
			}
			catch (final RemoteException exc) {
				exc.printStackTrace();
			}
			return;
		}

		// get old projection matrix
		final double[] oldMatrix = pc.getMatrix();

		// clear old aspect ratio from projection matrix
		final double[] undoOldAspect =
			{ 1 / xasp, 0, 0, 0, 0, 1 / yasp, 0, 0, 0, 0, 1 / zasp, 0, 0, 0, 0, 1 };
		double[] newMatrix = display.multiply_matrix(oldMatrix, undoOldAspect);

		// compute new aspect ratio matrix
		final double[] newAspect =
			{ x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1 };
		newMatrix = display.multiply_matrix(newMatrix, newAspect);

		// apply new projection matrix
		try {
			pc.setMatrix(newMatrix);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
	}

	/** Creates a matrix at the default orientation for the display. */
	protected double[] makeDefaultMatrix() {
		final DisplayImpl display = window.getDisplay();
		if (display == null || display instanceof DisplayImplJ2D) return null;

		final double scale = window.is3D() ? DEFAULT_ZOOM_3D : DEFAULT_ZOOM_2D;
		final double rotate = window.is3D() ? DEFAULT_ROTATION : 0;
		final double[] newMatrix =
			display.make_matrix(rotate, 0, 0, scale, 0, 0, 0);
		final double[] aspect =
			new double[] { xasp, 0, 0, 0, 0, yasp, 0, 0, 0, 0, zasp, 0, 0, 0, 0, 1 };
		return display.multiply_matrix(newMatrix, aspect);
	}

	/** Sets the display's projection matrix to match the given one. */
	protected void setMatrix(final double[] m) {
		final DisplayImpl display = window.getDisplay();
		if (display != null) {
			try {
				display.getProjectionControl().setMatrix(m);
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

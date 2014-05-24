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

package loci.visbio.overlays;

import java.awt.event.InputEvent;
import java.util.Vector;

import loci.visbio.data.TransformEvent;
import loci.visbio.util.MathUtil;
import visad.DisplayEvent;
import visad.DisplayImpl;
import visad.util.CursorUtil;

/**
 * FreeformTool is the tool for creating freeform objects.
 */
public class FreeformTool extends OverlayTool {

	// -- Constants --

	/**
	 * When drawing or editing, how far mouse must be dragged before new node is
	 * added.
	 */
	protected static final double DRAW_THRESH = OverlayNumericStrategy
		.getDrawThreshold();

	/**
	 * How close a mouseDrag event must be to a node in order to erase it.
	 */
	protected static final double ERASE_THRESH = OverlayNumericStrategy
		.getEraseThreshold();

	/** Threshhold within which click must occur to invoke edit mode. */
	protected static final double EDIT_THRESH = OverlayNumericStrategy
		.getEditThreshold();

	/**
	 * Threshhold within which click must occur to invoke extend mode or reconnect
	 * a freeformExtension.
	 */
	protected static final double RECONNECT_THRESH = OverlayNumericStrategy
		.getReconnectThreshold();

	/** How close mouse must be to end node to resume drawing. */
	protected static final double RESUME_THRESH = OverlayNumericStrategy
		.getResumeThreshold();

	/** Smoothing factor for "single exponential smoothing". */
	protected static final float S = (float) OverlayNumericStrategy
		.getSmoothingFactor();

	/** Constant for "erase" mode. */
	protected static final int ERASE = -1;

	/** Constant for "wait" mode. */
	protected static final int CHILL = 0; // could call this WAIT...

	/** Consant for "draw" mode. */
	protected static final int DRAW = 1;

	/** Constant for "edit" mode. */
	protected static final int EDIT = 2;

	/** Constant for "init" mode. */
	protected static final int INIT = 3;

	// -- Fields --

	/** Curve currently being drawn or modified. */
	protected OverlayFreeform freeform;

	/** Other freeforms on the canvas. */
	protected Vector otherFreefs;

	/** FreeformExtension wraps info about an edit to a curve. */
	protected FreeformExtension freeformExtension;

	/** Stores the current mode (INIT, ERASE, CHILL, DRAW, EDIT). */
	protected int mode;

	/** Point at which mouseDown occurs. */
	protected float downX, downY;

	// -- Constructor --

	/** Constructs a freeform creation tool. */
	public FreeformTool(final OverlayTransform overlay) {
		super(overlay, "Freeform", "Freeform", "freeform.png");
		freeformExtension = null;
		otherFreefs = new Vector();
		setMode(CHILL);
	}

	// -- OverlayTool API methods --

	/** Instructs this tool to respond to a mouse press. */
	@Override
	public void mouseDown(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{
		final boolean ctl = (mods & InputEvent.CTRL_MASK) != 0;
		final DisplayImpl display = (DisplayImpl) e.getDisplay();

		final double dpx = px;
		final double dpy = py;

		// if ctl, erase
		// else if near edit

		if (ctl) {
			setMode(ERASE);
			freeform = null;
		}
		else {
			// Logic of this section:
			// if close to freeform
			// if near end node, resume drawing
			// else begin editing
			// else
			// record initial click points

			// find closest freeform
			final double maxThresh = ERASE_THRESH;
			final DistanceQuery distanceQuery =
				getClosestFreeform(display, dpx, dpy, maxThresh);
			final OverlayFreeform target = distanceQuery.freeform;

			// operate on closest freeform
			if (target != null) {
				// editing operations on closest freeform
				freeform = target;

				final double dist = distanceQuery.dist;
				final int seg = distanceQuery.seg;
				final double weight = distanceQuery.wt;

				if (distanceQuery.isNearEndNode() && dist < RESUME_THRESH) {
					// resume drawing
					if (seg == 0) freeform.reverseNodes();
					setMode(DRAW);
				}
				else {
					// near an interior node
					if (dist < EDIT_THRESH) {
						// begin editing
						// project click onto curve and insert a new node there,
						// unless nearest point on curve is a node itself
						//
						// note: because of the way getDistSegWt() works, weight can
						// never be 0.0 except if seg = 0.
						if (weight == 1.0) { // nearest point on seg is the end node
							final float[] a = freeform.getNodeCoords(seg + 1);
							// TODO move these inserts inside FreeformExtension
							freeform.insertNode(seg + 2, a[0], a[1], true);
							freeformExtension =
								new FreeformExtension(freeform, seg + 1, seg + 2, true);
						}
						else {
							// determine projection on seg.
							// insert a pair of nodes there, starting a freeformExtension
							final float[] a = freeform.getNodeCoords(seg);
							final float[] b = freeform.getNodeCoords(seg + 1);
							final float[] newXY =
								MathUtil.computePtOnSegment(a, b, (float) weight);
							// TODO move thes inserts inside FreeformExtension
							freeform.insertNode(seg + 1, newXY[0], newXY[1], true);
							freeform.insertNode(seg + 1, newXY[0], newXY[1], true);
							freeformExtension =
								new FreeformExtension(freeform, seg + 1, seg + 2, false);
						}
						setMode(EDIT);
					}
				}
			}
			else { // no freeform was sufficiently close
				// drawing operations on new freeform
				if (!ctl) {
					// record initial coordinates of freeform
					downX = dx;
					downY = dy;
					setMode(INIT);
				}
			}
		}

		// stuff you have to do if there's been a change
		overlay.notifyListeners(new TransformEvent(overlay));
		((OverlayWidget) overlay.getControls()).refreshListSelection();
	} // end mouseDown

	/** Instructs this tool to respond to a mouse drag. */
	@Override
	public void mouseDrag(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{
		final boolean shift = (mods & InputEvent.SHIFT_MASK) != 0;
		final boolean ctl = (mods & InputEvent.CTRL_MASK) != 0;

		final DisplayImpl display = (DisplayImpl) e.getDisplay();

		final double dpx = px;
		final double dpy = py;

		if (mode == INIT) {
			freeform = new OverlayFreeform(overlay, downX, downY, downX, downY);
			overlay.addObject(freeform, pos);
			setMode(DRAW);
		}

		if (ctl && mode == DRAW) {
			freeform.truncateNodeArray();
			setMode(ERASE);
		}

		if (mode == DRAW) {
			// compute distance to endpoints of nearby freeforms
			int index = -1;
			boolean closerToHead = false;
			double minDist = Double.MAX_VALUE;

			for (int i = 0; i < otherFreefs.size(); i++) {
				final OverlayFreeform f = (OverlayFreeform) otherFreefs.get(i);
				final float[] head = f.getNodeCoords(0);
				final float[] tail = f.getNodeCoords(f.getNumNodes() - 1);

				final int[] drag = new int[] { px, py };
				final double hdist = getPixelDistanceToClick(display, head, drag);
				final double tdist = getPixelDistanceToClick(display, tail, drag);

				final boolean isHead = hdist < tdist ? true : false;
				final double dist = isHead ? hdist : tdist;

				if (dist < minDist) {
					minDist = dist;
					index = i;
					closerToHead = isHead;
				}
			}

			// if close, connect freeforms together
			if (minDist < DRAW_THRESH) {
				connectFreeforms(freeform, (OverlayFreeform) otherFreefs.get(index),
					closerToHead);
				setMode(CHILL);
			}
			else {
				// compute distance from last node
				// DISTANCE COMPUTATION: compare floats and ints
				final float[] last =
					{ freeform.getLastNodeX(), freeform.getLastNodeY() };
				final double distPxl =
					getPixelDistanceToClick(display, last, new int[] { px, py });

				if (distPxl > DRAW_THRESH) {
					final float[] s = OverlayUtil.smooth(new float[] { dx, dy }, last, S);
					freeform.setNextNode(s);
					final double len = freeform.getCurveLength();
					final double delta = MathUtil.getDistance(s, last);
					freeform.setCurveLength(len + delta);
					// I debated whether to call setBoundaries with every mouseDrag.
					// This is an efficient
					// method, but the corresponding update when erasing
					// requires an O(n)
					// scan of the nodes many times when a node is deleted.
					freeform.setBoundaries(s[0], s[1]);
				}
				// mode remains DRAW
			}
		}
		else if (mode == ERASE) {
			if (ctl) {
				final float thresh =
					(float) ERASE_THRESH * OverlayUtil.getMultiplier(display);
				eraseNearestNodeWithin(thresh, dx, dy);
			}
			else {
				setMode(DRAW);
			}
		}
		else if (mode == EDIT) {
			// extend freeformExtension and or reconnect
			// get coords of prev node
			final boolean reconnected =
				freeformExtension.extendOrReconnect(display, dx, dy, px, py, shift);
			if (reconnected) {
				freeform.computeLength();
				setMode(CHILL);
			}
		}
		overlay.notifyListeners(new TransformEvent(overlay));
	} // end mouseDrag

	/** Instructs this tool to respond to a mouse release. */
	@Override
	public void mouseUp(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{
		if (mode == DRAW) {
			freeform.truncateNodeArray();
		}
		else if (mode == EDIT) {
			// save coords of freeformExtension root
			final float[] c = freeform.getNodeCoords(freeformExtension.start);
			freeform.deleteBetween(freeformExtension.start - 1,
				freeformExtension.stop + 1);
			if (freeformExtension.nodal) {
				freeform.insertNode(freeformExtension.start, c[0], c[1], false);
			}
		}

		if (mode != CHILL) setMode(CHILL);
		overlay.notifyListeners(new TransformEvent(overlay));
		((OverlayWidget) overlay.getControls()).refreshListSelection();
	}

	// -- Helper methods for mouse methods

	/** Compiles a list of other freeforms at the current dimensional position. */
	protected Vector getFreeforms(final boolean includeThis) {
		final OverlayObject[] objs = overlay.getObjects();
		otherFreefs = new Vector(10);
		for (int i = 0; i < objs.length; i++) {
			if (objs[i] instanceof OverlayFreeform &&
				(includeThis || objs[i] != freeform))
			{
				otherFreefs.add(objs[i]);
			}
		}
		return otherFreefs;
	}

	/**
	 * Changes the edit mode. Depending on the new mode, some properties may
	 * change: e.g., whether a freeform is selected in the list of overlays.
	 */
	private void setMode(final int newMode) {
		mode = newMode;

		if (mode == INIT) {}
		else if (mode == DRAW) {
			deselectAll();
			freeform.setDrawing(true);
			freeform.setSelected(true);

			// create list of freeforms
			otherFreefs = getFreeforms(false);
		}
		else if (mode == EDIT) {
			deselectAll();
			freeform.setDrawing(true);
			freeform.setSelected(true);
			otherFreefs.removeAllElements();
		}
		else if (mode == ERASE) {
			deselectAll();
		}
		else if (mode == CHILL) {
			otherFreefs.removeAllElements();
			if (freeform != null) {
				freeform.setDrawing(false);
				if (!freeform.hasData()) {
					overlay.removeObject(freeform);
				}
				else {
					freeform.computeLength();
					freeform.updateBoundingBox();
					freeform.setSelected(true);
					freeform = null;
				}
			}
		}
	}

	/**
	 * Connects a pair of freeforms.
	 * 
	 * @param f1 the freeform being drawn.
	 * @param f2 the freeform to be appended to f2.
	 * @param head whether the tail of f1 should be connected to the head (or if
	 *          not, the tail) of f2.
	 */
	private void connectFreeforms(final OverlayFreeform f1,
		final OverlayFreeform f2, final boolean head)
	{
		// This method combines freeforms f1 and f2 to make a new freeform f3.
		if (!head) f2.reverseNodes();
		final OverlayFreeform f3 = f1.connectTo(f2);
		overlay.removeObject(f1);
		overlay.removeObject(f2);
		overlay.addObject(f3);
		freeform = f3; // store the new freeform
	}

	/** Erases the nearest node of freeforms nearby. */
	protected void eraseNearestNodeWithin(final float thresh, final float x,
		final float y)
	{
		final Vector freefs = getFreeforms(true); // collect all other freeforms,
		// including the current one tracked by this tool
		if (freefs.size() == 0) return;
		// compute closest node of closest freeform w/ respect to threshold
		double minDist = Double.POSITIVE_INFINITY;
		int minIndex = -1;
		OverlayFreeform closest = null;
		for (int i = 0; i < freefs.size(); i++) {
			final OverlayFreeform current = (OverlayFreeform) freefs.get(i);
			final double[] distIndex = current.getNearestNode(x, y);
			final double dist = distIndex[0];
			final int index = (int) distIndex[1];
			if (dist < thresh && dist < minDist) {
				minDist = dist;
				minIndex = index;
				closest = current;
			}
		}

		// remove that node if it exists, possibly splitting the freeform
		// in two.
		if (closest != null) {
			final OverlayFreeform[] children = closest.removeNode(minIndex);
			// remove freeforms with 1 or 0 nodes that result
			if (closest.getNumNodes() <= 1) overlay.removeObject(closest);
			removeEmptyFreeforms(children);
		}
	}

	/** Removes any freeforms with one or fewer nodes. */
	protected void removeEmptyFreeforms(final OverlayFreeform[] objs) {
		for (int i = 0; i < objs.length; i++) {
			if (objs[i] == null) continue;
			else if (objs[i].getNumNodes() <= 1) overlay.removeObject(objs[i]);
		}
	}

	/**
	 * Returns the closest (subject to a threshhold) OverlayFreeform object to the
	 * given point.
	 */
	protected DistanceQuery getClosestFreeform(final DisplayImpl display,
		final double dpx, final double dpy, final double thresh)
	{
		// returns only objects at the current dimensional position
		final OverlayObject[] objects = overlay.getObjects();
		// Q: Hey, are all of these OverlayFreeforms?
		// A: No, it returns OverlayObjects of all types

		OverlayFreeform closestFreeform = null;
		double[] min = { Double.MAX_VALUE, 0.0, 0.0 };
		if (objects != null) {
			for (int i = 0; i < objects.length; i++) {
				final OverlayObject currentObject = objects[i];
				if (currentObject instanceof OverlayFreeform) {
					final OverlayFreeform currentFreeform =
						(OverlayFreeform) currentObject;
					// performance opportunity:
					// check distance to bounding box first.
					// Should the getDistance method return a distance in
					// pixel coordinates?
					final double[][] nodesDbl =
						OverlayUtil.floatsToPixelDoubles(display, currentFreeform
							.getNodes());
					final double[] distSegWt =
					// DISTANCE COMPUTATION
					// compare float[][] with ints
						MathUtil.getDistSegWt(nodesDbl, dpx, dpy);
					if (distSegWt[0] < thresh && distSegWt[0] < min[0]) {
						min = distSegWt;
						closestFreeform = currentFreeform;
					}
				} // end if
			} // end for
		} // end if
		return new DistanceQuery(min, closestFreeform);
	}

	/** Gets distance in pixels between a click and a point in domain coords. */
	private double getPixelDistanceToClick(final DisplayImpl display,
		final float[] p, final int[] click)
	{
		// TODO Make this work for N-D arguments (not just 2D)
		final double[] pDbl = new double[] { p[0], p[1] };
		final int[] pPxl = CursorUtil.domainToPixel(display, pDbl);
		final double[] pPxlDbl = new double[] { pPxl[0], pPxl[1] };
		final double[] clickDbl = new double[] { click[0], click[1] };
		return MathUtil.getDistance(clickDbl, pPxlDbl);
	}

	/** Prints node array of current freeform; for debugging. */
	private void printNodes(final float[][] nodes) {
		System.out.println("Printing nodes...");
		for (int i = 0; i < nodes[0].length; i++) {
			System.out.println(i + ":(" + nodes[0][i] + "," + nodes[1][i] + ")");
		}
	}

	/** Prints node array of current freeform; for debugging. */
	private void printNodes() {
		if (freeform != null) {
			final float[][] nodes = freeform.getNodes();
			printNodes(nodes);
		}
	}

	/** Prints a message for debugging. */
	public void print(final String methodName, final String message) {
		final boolean toggle = true;
		if (toggle) {
			final String header = "FreeformTool.";
			final String out = header + methodName + ": " + message;
			System.out.println(out);
		}
	}

	// -- Inner Class --

	/**
	 * Wraps information about the distance from a point to a freeform object. The
	 * nearest point is expressed in terms of its location on the freeform using
	 * two variables: <code>seg</code> the segment on which the closest point
	 * lies, between nodes seg and seg+1 <code>wt</code> the relative distance
	 * along the segment where the closest point lies, between 0.0 and 1.0
	 */
	protected class DistanceQuery {

		// -- Fields --

		/** The index of the first node of the closest segment. */
		public int seg;

		/**
		 * The weight, between 0.0 and 1.0, representing the relative distance along
		 * the segment (seg, seg+1) that must be traveled from the node at index seg
		 * to reach the closest point on the curve.
		 */
		public double wt;

		/** The distance to the freeform. */
		public double dist;

		/** The nearest freeform itself. */
		public OverlayFreeform freeform;

		// -- Constructor --

		/** Constructs a DistanceQuery object. */
		public DistanceQuery(final double[] distSegWt, final OverlayFreeform f) {
			this.freeform = f;
			this.dist = distSegWt[0];
			this.seg = (int) distSegWt[1];
			this.wt = distSegWt[2];
		}

		// -- Methods --

		/** Whether the nearest node is an end node. */
		public boolean isNearEndNode() {
			return (seg == 0 && wt == 0.0) ||
				(seg == freeform.getNumNodes() - 2 && wt == 1.0);
		}
	}
} // end class FreeformTool

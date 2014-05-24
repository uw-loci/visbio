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

import loci.visbio.util.MathUtil;
import visad.DisplayImpl;
import visad.util.CursorUtil;

/**
 * The FreeformExtension class wraps information for the temporary section of a
 * freeform, which appears like a tendril on screen, created during an edit of
 * an existing freeform. The FreeformTool uses this information to modify the
 * freeform if the edit is successful, or to remove the freeform extension if
 * the edit is aborted. The section of the freeform represented by a freeform
 * extension is actually a loop, but it's thin (both sides overlap exactly) so
 * it appears like a single line. The loop has two parts, between start and tip
 * and between tip and stop. If an edit is completed, half of the freeform
 * extension is deleted along with the subtended section of the original
 * freeform. As a result, the tendril appears to be spliced into the old
 * freeform.
 */
public class FreeformExtension {

	// -- Static Fields --

	/** Smoothing factor for "single exponential smoothing". */
	protected static final float SMOOTHING_FACTOR =
		(float) OverlayNumericStrategy.getSmoothingFactor();

	/**
	 * When drawing or editing, how far mouse must be dragged before new node is
	 * added.
	 */
	protected static final double DRAW_THRESH = OverlayNumericStrategy
		.getDrawThreshold();

	/**
	 * Threshhold within which click must occur to invoke extend mode or reconnect
	 * a freeformExtension.
	 */
	protected static final double RECONNECT_THRESH = OverlayNumericStrategy
		.getReconnectThreshold();

	// -- Fields --

	/** The freeform to which this extension is attached. */
	protected OverlayFreeform freeform;

	/** The index of the last node of the freeform extension. */
	protected int start;

	/** The index of the last node of the freeform extension. */
	protected int stop;

	/** The index of the tip node of the freeform extension. */
	protected int tip;

	/** Chunks of curve before and after the extension. */
	protected float[][] pre, post;

	/**
	 * Whether this freeform extension began on a node or in the middle of a
	 * segment.
	 */
	protected boolean nodal;

	// -- Constructor --

	/** Constructs a new freeform extension. */
	public FreeformExtension(final OverlayFreeform freeform, final int start,
		final int stop, final boolean nodal)
	{
		this.freeform = freeform;
		this.start = start;
		this.stop = stop;
		this.nodal = nodal;
		// initial value of tip is nonsensical. Use as a check.
		this.tip = -1;

		if (nodal) splitNodes(freeform, start - 1, stop + 1);
		else splitNodes(freeform, start, stop + 1);
	}

	// -- Object API Methods --

	/** Whether this extension has begun or remains in the initial state. */
	public boolean hasBegun() {
		return (tip >= 0);
	}

	/**
	 * Extends the extension, reconnecting it with the curve proper if
	 * appropriate.
	 */
	public boolean extendOrReconnect(final DisplayImpl display, final float dx,
		final float dy, final int px, final int py, final boolean shift)
	{
		boolean reconnected = false;

		final double dpx = px;
		final double dpy = py;
		final double[] distSegWtPre = getDistanceToSection(pre, display, dpx, dpy);
		final double[] distSegWtPost =
			getDistanceToSection(post, display, dpx, dpy);

		boolean equalsCase = false;
		if (distSegWtPre[0] == distSegWtPost[0]) equalsCase = true;
		// Drag is equally close to both segments, wait for next drag.
		// This case is extremely unlikely.

		final boolean closerToPost = (distSegWtPre[0] > distSegWtPost[0]);
		final double[] distSegWt = (closerToPost) ? distSegWtPost : distSegWtPre;

		final double minDist = distSegWt[0];
		final int seg = (int) distSegWt[1];
		final double weight = distSegWt[2];

		final double dragDist = computeDragDist(display, px, py);

		// if not close to curve insert node, else reconnect
		if (minDist > RECONNECT_THRESH) {
			// insert a node at the drag point if drag went far enough
			if (dragDist > DRAW_THRESH) {
				final float[] prev = getTipCoords();
				final float[] s =
					OverlayUtil.smooth(new float[] { dx, dy }, prev, SMOOTHING_FACTOR);
				extend(s);
			}
		}
		else if (!shift && !equalsCase) {
			// reconnect with curve and delete nodes;
			if (hasBegun()) {
				// if (dragDist > DRAW_THRESH) {
				// insert a node first at drag point, then reconnect
				// skip this case: There's the possibility that this drag point
				// is across the curve from the previous drag point, which might
				// result in a unintended zig-zag.
				// }

				reconnect(seg, weight, closerToPost);
				reconnected = true;
			}
		} // end reconnect logic
		return reconnected;
	}

	/** Computes distance between a point and a section of the freeform. */
	public double[] getDistanceToSection(final float[][] section,
		final DisplayImpl display, final double dpx, final double dpy)
	{
		final double[][] dbl = OverlayUtil.floatsToPixelDoubles(display, section);
		final double[] distSegWt = MathUtil.getDistSegWt(dbl, dpx, dpy);
		return distSegWt;
	}

	/** Computes distance between mouse drag and tip of extension. **/
	public double computeDragDist(final DisplayImpl display, final int px,
		final int py)
	{
		// coords of this mouseDrag event
		final float[] prvCrdsFlt = getTipCoords();
		final double[] prvCrdsDbl = { prvCrdsFlt[0], prvCrdsFlt[1] };
		final int[] prvCrdsPxl = CursorUtil.domainToPixel(display, prvCrdsDbl);
		final double[] prvCrdsPxlDbl = { prvCrdsPxl[0], prvCrdsPxl[1] };

		final double[] drag = { px, py };
		final double dragDist = MathUtil.getDistance(drag, prvCrdsPxlDbl);
		return dragDist;
	}

	/** Extends the tip of the freeform extension by "one" node. */
	public void extend(final float[] c) {
		if (tip < 0) {
			freeform.insertNode(stop, c[0], c[1], true);
			stop++;
			tip = start + 1;
		}
		else { // later drags
			final float[] prev = getTipCoords();
			freeform.insertNode(tip + 1, prev[0], prev[1], true);
			freeform.insertNode(tip + 1, c[0], c[1], true);
			tip++;
			stop += 2;
		}
	}

	/** Reconnects the extension with the freeform. */
	public void reconnect(final int seg, final double weight,
		final boolean closerToPost)
	{
		// insert node at nearest point
		// offset points to either post[0] or pre[0]
		final int offset = closerToPost ? (stop + 1) : 0;

		int endIndex;
		if (weight == 0.0) {
			endIndex = seg + offset;
		}
		else if (weight == 1.0) {
			endIndex = seg + 1 + offset;
		}
		else {
			// determine projection on seg.
			final float[] a = freeform.getNodeCoords(seg + offset);
			final float[] b = freeform.getNodeCoords(seg + 1 + offset);
			final float[] newXY = MathUtil.computePtOnSegment(a, b, (float) weight);
			final int insertIndex = seg + 1 + offset;
			freeform.insertNode(insertIndex, newXY[0], newXY[1], true);
			if (!closerToPost) incrementAll();
			endIndex = insertIndex;
		}

		if (tip < endIndex) {
			freeform.deleteBetween(tip, endIndex);
		}
		else {
			freeform.deleteBetween(endIndex, tip);
		}
	}

	/** Returns the coordinates of the extension's tip or start node. */
	public float[] getTipCoords() {
		float[] prev;
		if (tip >= 0) {
			// previous node is tendril.tip
			prev = freeform.getNodeCoords(tip);
		}
		else {
			// previous node is tendril.start
			prev = freeform.getNodeCoords(start);
		}
		return prev;
	}

	/**
	 * Simultaneously increments all extension pointers into node array, to adjust
	 * for the insertion of a node before the base of the extension.
	 */
	public void incrementAll() {
		tip++;
		start++;
		stop++;
	}

	/**
	 * Splits the node array into two parts. The first part goes from a[0] to
	 * a[index-1], the second from a[index2] to a[a.length -1].
	 */
	private void splitNodes(final OverlayFreeform f, final int index,
		final int index2)
	{
		// splits the array a into two (before the index specified)
		final float[][] a = f.getNodes();
		// print these guys
		final int depth = a.length;
		final int len = a[0].length; // assumes non-ragged array
		pre = new float[depth][index];
		post = new float[depth][len - index2];
		for (int i = 0; i < depth; i++) {
			System.arraycopy(a[i], 0, pre[i], 0, index);
			System.arraycopy(a[i], index2, post[i], 0, len - index2);
		}
	}
}

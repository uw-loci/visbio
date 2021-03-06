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

import java.awt.Color;
import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Vector;

import loci.visbio.util.MathUtil;
import loci.visbio.view.TransformLink;
import visad.DataImpl;
import visad.DisplayImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.GriddedSet;
import visad.RealTupleType;
import visad.SampledSet;
import visad.TupleType;
import visad.UnionSet;
import visad.VisADException;
import visad.util.CursorUtil;

/**
 * Utility methods for Overlays.
 */
public final class OverlayUtil {

	// -- Constants --

	/** Width of the selection layer beyond the object's boundaries. */
	protected static final float GLOW_WIDTH = 5.0f; // in pixels

	/** Alpha of the selection layer. */
	protected static final float GLOW_ALPHA = 0.15f;

	/** Color of the selection layer. */
	protected static final Color GLOW_COLOR = Color.yellow;

	/** Color of the highlight layer for noded objects. */
	protected static final Color HLT_COLOR = Color.green;

	/** Alpha of the highlight layer for noded object. */
	protected static final float HLT_ALPHA = 0.5f;

	/** Color of the outline layer. */
	protected static final Color OUTLINE_COLOR = Color.cyan;

	/** Computed (X, Y) pairs for top 1/2 of a unit circle. */
	protected static final float[][] ARC = arc();

	/** Computes the top 1/2 of a unit circle. */
	private static float[][] arc() {
		final int res = 16; // resolution for 1/8 of circle
		final float[][] arc = new float[2][4 * res];
		for (int i = 0; i < res; i++) {
			final float t = 0.5f * (i + 0.5f) / res;
			final float x = (float) Math.sqrt(t);
			final float y = (float) Math.sqrt(1 - t);

			arc[0][i] = -y;
			arc[1][i] = x;

			final int i1 = 2 * res - i - 1;
			arc[0][i1] = -x;
			arc[1][i1] = y;

			final int i2 = 2 * res + i;
			arc[0][i2] = x;
			arc[1][i2] = y;

			final int i3 = 4 * res - i - 1;
			arc[0][i3] = y;
			arc[1][i3] = x;
		}
		return arc;
	}

	/** List of all OverlayObject subclasses. */
	public static final String[] OVERLAY_TYPES = { "Line", "Freeform", "Marker",
		"Text", "Oval", "Box", "Arrow", "Polyline" };

	// -- Constructor --
	private OverlayUtil() {}

	// -- Statistics Utility Methods --

	/** Get list of all OverlayObject subclasses. */
	public static String[] getOverlayTypes() {
		return OVERLAY_TYPES;
	}

	/**
	 * Returns statistic names for a particular class.
	 */
	protected static String[] getStatTypes(final String overlayType) {
		String[] statTypes = null;
		if (overlayType.equals("Arrow")) statTypes = OverlayArrow.getStatTypes();
		else if (overlayType.equals("Box")) statTypes = OverlayBox.getStatTypes();
		else if (overlayType.equals("Freeform")) statTypes =
			OverlayNodedObject.getStatTypes();
		else if (overlayType.equals("Line")) statTypes = OverlayLine.getStatTypes();
		else if (overlayType.equals("Marker")) statTypes =
			OverlayMarker.getStatTypes();
		else if (overlayType.equals("Oval")) statTypes = OverlayOval.getStatTypes();
		else if (overlayType.equals("Polyline")) statTypes =
			OverlayNodedObject.getStatTypes();
		else if (overlayType.equals("Text")) statTypes = OverlayText.getStatTypes();
		return statTypes;
	}

	// -- Selection Layer Utility Methods --

	/** Computes a type-specific selection layer for the given OverlayObject. */
	public static DataImpl getSelectionLayer(final OverlayObject obj,
		final TransformLink link, final boolean outline)
	{
		DataImpl layer = null;
		if (!obj.hasData()) layer = null;
		else if (outline) {
			layer = getOutlineLayer(obj, link);
		}
		else {
			if (obj instanceof OverlayArrow) layer = getArrowLayer(obj, link);
			if (obj instanceof OverlayBox) layer = getBoxLayer(obj, link);
			if (obj instanceof OverlayLine) layer = getLineLayer(obj, link);
			if (obj instanceof OverlayMarker) layer = getMarkerLayer(obj, link);
			if (obj instanceof OverlayNodedObject) layer = getNodedLayer(obj, link);
			if (obj instanceof OverlayOval) layer = getOvalLayer(obj, link);
			if (obj instanceof OverlayText) layer = getTextLayer(obj, link);
		}
		return layer;
	}

	/** Computes selection layer for OverlayArrow objects. */
	public static DataImpl getArrowLayer(final OverlayObject obj,
		final TransformLink link)
	{
		final OverlayTransform overlay = (OverlayTransform) link.getTransform();
		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		final float x1 = obj.getX();
		final float x2 = obj.getX2();
		final float y1 = obj.getY();
		final float y2 = obj.getY2();

		// compute corners of arrow tail
		// (this paragraph copied straight from OverlayArrow)
		final double xx = x2 - x1;
		final double yy = y2 - y1;
		final double dist = Math.sqrt(xx * xx + yy * yy);
		final double mult = 0.1; // something like aspect ratio
		final float qx = (float) (mult * xx);
		final float qy = (float) (mult * yy);

		// determine internal coordinate sys. basis vectors
		// assuming arrow is oriented like this:
		//
		// (x1, y1) <--------{{{ (x2, y2)
		//
		//
		final double[] x = { xx / dist, yy / dist };
		final double[] y = { -yy / dist, xx / dist };

		// arrow lengths:
		double a, b, c;
		a = Math.sqrt(qx * qx + qy * qy); // 1/2 width of tail
		b = dist; // length from tail to tip
		c = Math.sqrt(a * a + b * b); // length of side/hypotenuse

		final double d = GLOW_WIDTH * getMultiplier(link);

		// compute four corners of highlighted zone, a trapezoidal area around the
		// arrow, always a perpendicular distance d from the arrow's edge.
		// (Formulas come from pen-and-paper geometry using similar triangles).
		final float c1x = (float) (x1 - x[0] * d + y[0] * (d * c) / b);
		final float c1y = (float) (y1 - x[1] * d + y[1] * (d * c) / b);
		final float c2x = (float) (x2 + x[0] * d + y[0] * (a + d * (a + c) / b));
		final float c2y = (float) (y2 + x[1] * d + y[1] * (a + d * (a + c) / b));
		final float c3x = (float) (x2 + x[0] * d - y[0] * (a + d * (a + c) / b));
		final float c3y = (float) (y2 + x[1] * d - y[1] * (a + d * (a + c) / b));
		final float c4x = (float) (x1 - x[0] * d - y[0] * (d * c) / b);
		final float c4y = (float) (y1 - x[1] * d - y[1] * (d * c) / b);

		// order and combine the coordinates for a Gridded2DSet [2 2]
		final float[][] setSamples =
			{ { c1x, c2x, c4x, c3x }, { c1y, c2y, c4y, c3y } };

		// construct range samples
		final float r = GLOW_COLOR.getRed() / 255f;
		final float g = GLOW_COLOR.getGreen() / 255f;
		final float bl = GLOW_COLOR.getBlue() / 255f;

		final float[][] rangeSamples = new float[4][setSamples[0].length];
		Arrays.fill(rangeSamples[0], r);
		Arrays.fill(rangeSamples[1], g);
		Arrays.fill(rangeSamples[2], bl);
		Arrays.fill(rangeSamples[3], GLOW_ALPHA);

		// construct Field
		Gridded2DSet domainSet = null;
		FlatField field = null;
		try {
			domainSet =
				new Gridded2DSet(domain, setSamples, 2, 2, null, null, null, false);
			final FunctionType fieldType = new FunctionType(domain, range);
			field = new FlatField(fieldType, domainSet);
			field.setSamples(rangeSamples);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		return field;
	}

	/** Computes selection layer for OverlayBox objects. */
	public static DataImpl getBoxLayer(final OverlayObject obj,
		final TransformLink link)
	{
		final OverlayTransform overlay = (OverlayTransform) link.getTransform();
		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		final float x1 = obj.getX();
		final float x2 = obj.getX2();
		final float y1 = obj.getY();
		final float y2 = obj.getY2();

		final float delta = GLOW_WIDTH * getMultiplier(link);

		// Determine orientation of (x1, y1) relative to (x2, y2)
		// and flip if need be.
		// I've set up the code in this section based on the
		// supposition that the box is oriented like this:
		//
		// p1 +--------+
		// | |
		// | |
		// +--------+ p2
		//
		// which means x1 is supposed to be _less_ than x2, but inconsistently,
		// y1 is supposed to be _greater_ than y2.

		final boolean flipX = x2 > x1;
		final float xx1 = flipX ? x1 : x2;
		final float xx2 = flipX ? x2 : x1;
		final boolean flipY = y2 < y1;
		final float yy1 = flipY ? y1 : y2;
		final float yy2 = flipY ? y2 : y1;

		// just throw down a translucent rectangle over the box
		final float[][] setSamples =
			{ { xx1 - delta, xx2 + delta, xx1 - delta, xx2 + delta },
				{ yy1 + delta, yy1 + delta, yy2 - delta, yy2 - delta } };

		// construct range samples
		final float[][] rangeSamples = new float[4][4];
		final float r = GLOW_COLOR.getRed() / 255f;
		final float g = GLOW_COLOR.getGreen() / 255f;
		final float b = GLOW_COLOR.getBlue() / 255f;
		Arrays.fill(rangeSamples[0], r);
		Arrays.fill(rangeSamples[1], g);
		Arrays.fill(rangeSamples[2], b);
		Arrays.fill(rangeSamples[3], GLOW_ALPHA);

		// construct field
		Gridded2DSet domainSet = null;
		FlatField field = null;
		try {
			domainSet =
				new Gridded2DSet(domain, setSamples, 2, 2, null, null, null, false);
			final FunctionType fieldType = new FunctionType(domain, range);
			field = new FlatField(fieldType, domainSet);
			field.setSamples(rangeSamples);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}

		return field;
	}

	/** Returns a selection layer for OverlayLine objects. */
	public static DataImpl getLineLayer(final OverlayObject obj,
		final TransformLink link)
	{
		final OverlayTransform overlay = (OverlayTransform) link.getTransform();
		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		final float x1 = obj.getX();
		final float x2 = obj.getX2();
		final float y1 = obj.getY();
		final float y2 = obj.getY2();

		final float delta = GLOW_WIDTH * getMultiplier(link);

		// compute locations of grid points
		// (uses similar triangles instead of raw trig fcns)
		final float x = x2 - x1;
		final float y = y2 - y1;
		final float hyp = (float) Math.sqrt(x * x + y * y);
		final float ratio = delta / hyp;
		// offsets from endpoints of line segments
		final float dx1 = ratio * y;
		final float dy1 = ratio * x;
		final float dx2 = ratio * x;
		final float dy2 = ratio * y;

		final float[] c1 = { x1 - dx1 - dx2, y1 + dy1 - dy2 };
		final float[] c2 = { x2 - dx1 + dx2, y2 + dy1 + dy2 };
		final float[] c3 = { x1 + dx1 - dx2, y1 - dy1 - dy2 };
		final float[] c4 = { x2 + dx1 + dx2, y2 - dy1 + dy2 };

		final float[][] setSamples =
			{ { c1[0], c2[0], c3[0], c4[0] }, { c1[1], c2[1], c3[1], c4[1] } };

		// construct range samples;
		final Color col = GLOW_COLOR;

		final float r = col.getRed() / 255f;
		final float g = col.getGreen() / 255f;
		final float b = col.getBlue() / 255f;

		final float[][] rangeSamples = new float[4][4];
		Arrays.fill(rangeSamples[0], r);
		Arrays.fill(rangeSamples[1], g);
		Arrays.fill(rangeSamples[2], b);
		Arrays.fill(rangeSamples[3], GLOW_ALPHA);

		Gridded2DSet domainSet = null;
		FlatField field = null;
		try {
			domainSet =
				new Gridded2DSet(domain, setSamples, 2, 2, null, null, null, false);
			final FunctionType fieldType = new FunctionType(domain, range);
			field = new FlatField(fieldType, domainSet);
			field.setSamples(rangeSamples);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}

		return field;
	}

	/** Returns a selection layer for OverlayMarker objects. */
	public static DataImpl getMarkerLayer(final OverlayObject obj,
		final TransformLink link)
	{
		final OverlayTransform overlay = (OverlayTransform) link.getTransform();
		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		final float x1 = obj.getX();
		final float x2 = obj.getX2();
		final float y1 = obj.getY();
		final float y2 = obj.getY2();

		final float size = ((OverlayMarker) obj).getWidth();
		final float delta = GLOW_WIDTH * getMultiplier(link);

		final float xx1 = x1 - size - delta;
		final float xx2 = x1 + size + delta;
		final float yy1 = y1 + size + delta;
		final float yy2 = y1 - size - delta;

		// construct a cross shape, or if the marker is really small compared
		// to the desired width (delta) of the selection layer, a box instead
		SampledSet domainSet = null;
		int samplesLength = 4;
		if (2 * delta > size) {
			// return box
			float[][] setSamples;
			setSamples =
				new float[][] { { xx1, xx2, xx1, xx2 }, { yy1, yy1, yy2, yy2 } };

			try {
				domainSet =
					new Gridded2DSet(domain, setSamples, setSamples[0].length / 2, 2,
						null, null, null, false);
			}
			catch (final VisADException ex) {
				ex.printStackTrace();
			}
		}
		else {
			// return cross shape
			// using a UnionSet for now--couldn't get a single
			// Gridded2D set to appear as a cross

			// left branch
			final float[][] setSamples1 =
				{ { xx1, x1 - delta, xx1, x1 - delta },
					{ y1 + delta, y1 + delta, y1 - delta, y1 - delta } };

			// vertical part
			final float[][] setSamples2 =
				{ { x1 - delta, x1 + delta, x1 - delta, x1 + delta },
					{ yy1, yy1, yy2, yy2 } };

			// right branch
			final float[][] setSamples3 =
				{ { x1 + delta, xx2, x1 + delta, xx2 },
					{ y1 + delta, y1 + delta, y1 - delta, y1 - delta } };

			final float[][][] setSamples = { setSamples1, setSamples2, setSamples3 };

			samplesLength = 12;

			final Gridded2DSet[] sets = new Gridded2DSet[3];
			try {
				for (int j = 0; j < 3; j++) {
					sets[j] =
						new Gridded2DSet(domain, setSamples[j], 2, 2, null, null, null,
							false);
				}
				domainSet = new UnionSet(domain, sets);
			}
			catch (final VisADException ex) {
				ex.printStackTrace();
			}

			/*
			float dx = 0.0001;
			// here's the code for creating a cross-shaped Gridded2DSet,
			// which doesn't quite work
			setSamples = new float[][]{
			  {xx1, x1 - delta, x1 - delta + dx, x1 + delta - dx, x1 + delta, xx2,
			    xx1, x1 - delta, x1 - delta + dx, x1 + delta - dx, x1 + delta, xx2},
			  {y1 + delta, y1 + delta, yy1, yy1, y1 + delta, y1 + delta,
			    y1 - delta, y1 - delta, yy2, yy2, y1 - delta, y1 - delta}
			};*/

			// Start with this:
			/*
			setSamples = new float[][] {
			  {xx1, x1 - delta,
			    xx1, x1 - delta,},
			  {y1 + delta, y1 + delta,
			    y1 - delta, y1 - delta,}
			};
			*/

			// then try this:
			/*
			setSamples = new float[][] {
			  {xx1, x1 - delta, x1 - delta + dx,
			    xx1, x1 - delta, x1 -delta + dx},
			  {y1 + delta, y1 + delta, y1 + size + delta,
			    y1 - delta, y1 - delta, y1 - size - delta}
			};
			*/
			// I would expect the second one to look like a sideways 'T', but
			// it looks like a triangle instead
		}

		// construct range samples
		final float r = GLOW_COLOR.getRed() / 255f;
		final float g = GLOW_COLOR.getGreen() / 255f;
		final float b = GLOW_COLOR.getBlue() / 255f;

		final float[][] rangeSamples = new float[4][samplesLength];
		Arrays.fill(rangeSamples[0], r);
		Arrays.fill(rangeSamples[1], g);
		Arrays.fill(rangeSamples[2], b);
		Arrays.fill(rangeSamples[3], GLOW_ALPHA);

		// construct field
		FlatField field = null;
		try {
			final FunctionType fieldType = new FunctionType(domain, range);
			field = new FlatField(fieldType, domainSet);
			field.setSamples(rangeSamples);
		}
		catch (final VisADException ex) {
			ex.printStackTrace();
		}
		catch (final RemoteException ex) {
			ex.printStackTrace();
		}
		return field;
	}

	/** Computes selection layer for OverlayNodedObject objects. */
	public static DataImpl getNodedLayer(final OverlayObject obj,
		final TransformLink link)
	{
		final OverlayTransform overlay = (OverlayTransform) link.getTransform();
		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();
		final OverlayNodedObject ono = (OverlayNodedObject) obj;

		// This method builds a UnionSet of Gridded2DSets to highlight an Overlay
		// Freeform or OverlayPolyline, plus (sometimes) a translucent circle over
		// one of the nodes of the freeform or polyline to indicate that the mouse
		// pointer is nearby. It constructs a separate Gridded2DSet for
		// each segment of the freeform or polyline, and an additional Gridded2DSet
		// for the circle.

		final float delta = GLOW_WIDTH * getMultiplier(link);
		final float[][] nodes = ono.getNodes();
		final int numNodes = ono.getNumNodes();
		final boolean hlt = ono.isHighlightNode();
		int hltIndex = 0;
		if (hlt) hltIndex = ono.getHighlightedNodeIndex();
		final float[] c = ono.getNodeCoords(hltIndex);

		// arc and width params
		final int arcLen = ARC[0].length;
		final int len = 2 * arcLen;

		// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
		// Build nodes sets
		// ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

		int samples = 0;

		final Vector sets = buildNodesSets(domain, nodes, delta);
		samples = sets.size() * 4;

		final int hlen = hlt ? len : 0;
		final float[][] rangeSamples = new float[4][samples + hlen];

		// fill nodes range samples;
		Color col = GLOW_COLOR;
		float r = col.getRed() / 255f;
		float g = col.getGreen() / 255f;
		float b = col.getBlue() / 255f;

		// ADJUST THIS to account for circles later
		Arrays.fill(rangeSamples[0], 0, samples, r);
		Arrays.fill(rangeSamples[1], 0, samples, g);
		Arrays.fill(rangeSamples[2], 0, samples, b);
		Arrays.fill(rangeSamples[3], 0, samples, GLOW_ALPHA);

		// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
		// Build circle and circle samples
		// ,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,,

		/*
		System.out.println("2: isHighlightNode = " + hlt);
		System.out.println("Thread.currentThread()" + Thread.currentThread());
		*/
		Gridded2DSet hltSet = null;
		if (hlt) {
			final float rad = 2 * delta;

			// assemble a small circle
			final float[][] highlightSetSamples = new float[2][len];

			// top half of circle
			for (int i = 0; i < arcLen; i++) {
				highlightSetSamples[0][i] = c[0] + rad * ARC[0][i];
				highlightSetSamples[1][i] = c[1] + rad * ARC[1][i];
			}

			// bottom half of circle
			for (int i = 0; i < arcLen; i++) {
				final int ndx = arcLen + i;
				highlightSetSamples[0][ndx] = c[0] + rad * ARC[0][i];
				highlightSetSamples[1][ndx] = c[1] - rad * ARC[1][i];
			}

			try {
				// build highlight set
				hltSet =
					new Gridded2DSet(domain, highlightSetSamples, arcLen, 2, null, null,
						null, false);
			}
			catch (final VisADException ex) {
				ex.printStackTrace();
			}

			// fill highlight range samples
			col = HLT_COLOR;
			r = col.getRed() / 255f;
			g = col.getGreen() / 255f;
			b = col.getBlue() / 255f;

			Arrays.fill(rangeSamples[0], samples, samples + hlen, r);
			Arrays.fill(rangeSamples[1], samples, samples + hlen, g);
			Arrays.fill(rangeSamples[2], samples, samples + hlen, b);
			Arrays.fill(rangeSamples[3], samples, samples + hlen, HLT_ALPHA);

			sets.add(hltSet);
		}

		// convert vector to an array
		final Gridded2DSet[] trueSets = new Gridded2DSet[sets.size()];
		final Object[] rubbish = sets.toArray(trueSets);

		// form a union set and then a flat field
		FlatField field = null;
		UnionSet fieldSet = null;
		try {
			fieldSet = new UnionSet(domain, trueSets);
			final FunctionType fieldType = new FunctionType(domain, range);
			field = new FlatField(fieldType, fieldSet);
			field.setSamples(rangeSamples);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}

		return field;
	}

	/** Computes a selection layer for OverlayOval objects. */
	public static DataImpl getOvalLayer(final OverlayObject obj,
		final TransformLink link)
	{
		final OverlayTransform overlay = (OverlayTransform) link.getTransform();
		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		final float x1 = obj.getX();
		final float x2 = obj.getX2();
		final float y1 = obj.getY();
		final float y2 = obj.getY2();

		final float cx = (x1 + x2) / 2;
		final float cy = (y1 + y2) / 2;
		final float rrx = cx > x1 ? cx - x1 : cx - x2;
		final float rry = cy > y1 ? cy - y1 : cy - y2;

		final float scl = getMultiplier(link); // for now
		final float rx = rrx + GLOW_WIDTH * scl;
		final float ry = rry + GLOW_WIDTH * scl;

		final int arcLen = ARC[0].length;
		final int len = 2 * arcLen;
		final float[][] setSamples = new float[2][len];

		// top half of circle
		for (int i = 0; i < arcLen; i++) {
			setSamples[0][i] = cx + rx * ARC[0][i];
			setSamples[1][i] = cy + ry * ARC[1][i];
		}

		// bottom half of circle
		for (int i = 0; i < arcLen; i++) {
			final int ndx = arcLen + i;
			setSamples[0][ndx] = cx + rx * ARC[0][i];
			setSamples[1][ndx] = cy - ry * ARC[1][i];
		}

		// construct range samples
		final float r = GLOW_COLOR.getRed() / 255f;
		final float g = GLOW_COLOR.getGreen() / 255f;
		final float b = GLOW_COLOR.getBlue() / 255f;

		final float[][] rangeSamples = new float[4][setSamples[0].length];
		Arrays.fill(rangeSamples[0], r);
		Arrays.fill(rangeSamples[1], g);
		Arrays.fill(rangeSamples[2], b);
		Arrays.fill(rangeSamples[3], GLOW_ALPHA);

		GriddedSet fieldSet = null;
		FlatField field = null;
		try {
			fieldSet =
				new Gridded2DSet(domain, setSamples, arcLen, 2, null, null, null, false);

			final FunctionType fieldType = new FunctionType(domain, range);
			field = new FlatField(fieldType, fieldSet);
			field.setSamples(rangeSamples);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		return field;
	}

	/** Computes a selection layer for OverlayText objects. */
	public static DataImpl getTextLayer(final OverlayObject obj,
		final TransformLink link)
	{
		final OverlayTransform overlay = (OverlayTransform) link.getTransform();
		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		((OverlayText) obj).computeTextBounds();
		final float[][] corners = computeOutline(obj, link);

		final float[][] setSamples =
			{ { corners[0][0], corners[1][0], corners[0][0], corners[1][0] },
				{ corners[0][1], corners[0][1], corners[1][1], corners[1][1] }, };

		// construct range samples
		final float r = GLOW_COLOR.getRed() / 255f;
		final float g = GLOW_COLOR.getGreen() / 255f;
		final float b = GLOW_COLOR.getBlue() / 255f;

		final float[][] rangeSamples = new float[4][setSamples[0].length];
		Arrays.fill(rangeSamples[0], r);
		Arrays.fill(rangeSamples[1], g);
		Arrays.fill(rangeSamples[2], b);
		Arrays.fill(rangeSamples[3], GLOW_ALPHA);

		FlatField field = null;
		try {
			final Gridded2DSet domainSet =
				new Gridded2DSet(domain, setSamples, 2, 2, null, null, null, false);
			final FunctionType fieldType = new FunctionType(domain, range);
			field = new FlatField(fieldType, domainSet);
			field.setSamples(rangeSamples);
		}
		catch (final VisADException ex) {
			ex.printStackTrace();
		}
		catch (final RemoteException ex) {
			ex.printStackTrace();
		}
		return field;
	}

	/** Computes a standard outline layer for an OverlayObject. */
	public static DataImpl getOutlineLayer(final OverlayObject obj,
		final TransformLink link)
	{
		final OverlayTransform overlay = (OverlayTransform) link.getTransform();
		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		final float x1 = obj.getX();
		final float x2 = obj.getX2();
		final float y1 = obj.getY();
		final float y2 = obj.getY2();

		final float[][] cnrs = computeOutline(obj, link);

		float[][] setSamples = null;
		GriddedSet fieldSet = null;
		try {
			setSamples =
				new float[][] {
					{ cnrs[0][0], cnrs[1][0], cnrs[1][0], cnrs[0][0], cnrs[0][0] },
					{ cnrs[0][1], cnrs[0][1], cnrs[1][1], cnrs[1][1], cnrs[0][1] } };
			fieldSet =
				new Gridded2DSet(domain, setSamples, setSamples[0].length, null, null,
					null, false);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}

		final Color col = OUTLINE_COLOR;
		final float r = col.getRed() / 255f;
		final float g = col.getGreen() / 255f;
		final float b = col.getBlue() / 255f;

		final float[][] rangeSamples = new float[4][setSamples[0].length];
		Arrays.fill(rangeSamples[0], r);
		Arrays.fill(rangeSamples[1], g);
		Arrays.fill(rangeSamples[2], b);
		Arrays.fill(rangeSamples[3], 1.0f);

		FlatField field = null;
		try {
			final FunctionType fieldType = new FunctionType(domain, range);
			field = new FlatField(fieldType, fieldSet);
			field.setSamples(rangeSamples);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		return field;
	}

	/**
	 * Returns a multiplier suitable for scaling distances to pixel coordinates.
	 * Useful when the location of an event are unimportant, just the properties
	 * of the display.
	 */
	public static float getMultiplier(final TransformLink link) {
		final DisplayImpl display = link.getHandler().getWindow().getDisplay();
		return getMultiplier(display);
	}

	/**
	 * Returns a multiplier suitable for scaling distances to pixel coordinates.
	 * Useful when the location of an event are unimportant, just the properties
	 * of the display.
	 */
	public static float getMultiplier(final DisplayImpl display) {
		// NB: This method may be a bit naive,
		// obtaining the multiplier from only one measurement.
		final int[] p1 = { 0, 0 };
		final int[] p2 = { 0, 1000 };
		final double[] d1 = CursorUtil.pixelToDomain(display, p1[0], p1[1]);
		final double[] d2 = CursorUtil.pixelToDomain(display, p2[0], p2[1]);
		final int px = p2[0] - p1[0];
		final int py = p2[1] - p1[1];
		final double dx = d2[0] - d1[0];
		final double dy = d2[1] - d1[1];
		final double pp = Math.sqrt(px * px + py * py);
		final double dd = Math.sqrt(dx * dx + dy * dy);
		final float mult = (float) (dd / pp);
		return mult == mult ? mult : 1;
	}

	// -- Helper Methods --

	// Note: This method is basically the old
	// OverlayObject.computeGridParameters()
	// method from OverlayBox.
	/** Computes corners of an OverlayObject's outline. */
	private static float[][] computeOutline(final OverlayObject obj,
		final TransformLink link)
	{
		final DisplayImpl display = link.getHandler().getWindow().getDisplay();
		final float x1 = obj.getX();
		final float x2 = obj.getX2();
		final float y1 = obj.getY();
		final float y2 = obj.getY2();

		float scl = 0;
		if (obj instanceof OverlayText) scl = .25f;
		final float padding = GLOW_WIDTH * (scl + getMultiplier(link));
		final boolean flipX = x2 < x1;
		final float xx1 = flipX ? (x1 + padding) : (x1 - padding);
		final float xx2 = flipX ? (x2 - padding) : (x2 + padding);
		final boolean flipY = y2 < y1;
		final float yy1 = flipY ? (y1 + padding) : (y1 - padding);
		final float yy2 = flipY ? (y2 - padding) : (y2 + padding);

		return new float[][] { { xx1, yy1 }, { xx2, yy2 } };
	}

	/** Computes outline of a text object. */
	private static float[][] computeTextOutline(final OverlayObject obj,
		final OverlayTransform overlay)
	{
		if (obj instanceof OverlayText) ((OverlayText) obj).computeTextBounds();
		final float x1 = obj.getX();
		final float x2 = obj.getX2();
		final float y1 = obj.getY();
		final float y2 = obj.getY2();

		final int sx = overlay.getScalingValueX();
		final int sy = overlay.getScalingValueY();

		final float padx = 0.02f * sx;
		final float pady = 0.02f * sy;
		final float xx1 = x1 - padx;
		final float xx2 = x2 + padx;
		final float yy1 = y1 - pady;
		final float yy2 = y2 + pady;

		return new float[][] { { xx1, yy1 }, { xx2, yy2 } };
	}

	/** Converts float coordinates to a pixel coordinateis (as floats) */
	private static float[]
		domainToPixel(final TransformLink link, final float[] d)
	{
		final DisplayImpl display = link.getHandler().getWindow().getDisplay();
		final double[] dDbl = new double[d.length]; // domain coordinates as doubles
		for (int i = 0; i < d.length; i++)
			dDbl[i] = d[i];
		final int[] p = CursorUtil.domainToPixel(display, dDbl);
		final float[] pfloat = new float[p.length]; // pixel coordinates as floats
		for (int i = 0; i < p.length; i++)
			pfloat[i] = p[i];
		return pfloat;
	}

	/** Converts pixel coordinateis (as floats) to domain coordinates */
	private static float[]
		pixelToDomain(final TransformLink link, final float[] p)
	{
		final DisplayImpl display = link.getHandler().getWindow().getDisplay();
		// pixel coords cast down to ints here:
		final double[] d =
			CursorUtil.pixelToDomain(display, (int) (p[0] + 1), (int) (p[1] + 1));
		final float[] dfloat = new float[d.length]; // domain coordinates as floats
		for (int i = 0; i < d.length; i++)
			dfloat[i] = (float) d[i];
		return dfloat;
	}

	/** Converts an array of pixels as floats to domain. */
	private static float[][] pixelToDomain(final TransformLink link,
		final float[][] pixelSamples)
	{
		final DisplayImpl display = link.getHandler().getWindow().getDisplay();
		final float[][] domainSamples =
			new float[pixelSamples.length][pixelSamples[0].length];
		for (int i = 0; i < pixelSamples[0].length; i++) {
			// cast down to int here
			final double[] d =
				CursorUtil.pixelToDomain(display, (int) (pixelSamples[0][i] + 1),
					(int) (pixelSamples[1][i] + 1));
			domainSamples[0][i] = (float) d[0];
			domainSamples[1][i] = (float) d[1];
		}
		return domainSamples;
	}

	/** Connects a pair of VisAD-style 2D arrays of points. */
	public static float[][] adjoin(final float[][] a, final float[][] b) {
		final int alen = a[0].length;
		final int blen = b[0].length;
		final float[][] result = new float[a.length][alen + blen];
		for (int j = 0; j < 2; j++) {
			System.arraycopy(a[j], 0, result[j], 0, alen);
			System.arraycopy(b[j], 0, result[j], alen, blen);
		}
		return result;
	}

	/**
	 * Given a set of nodes, creates a UnionSet of Gridded2DSets to highlight the
	 * nodes.
	 */
	public static Vector buildNodesSets(final RealTupleType domain,
		final float[][] nodes, final float width)
	{
		final int len = nodes[0].length;

		// Create two arrays to store the gridpoints: one to the 'right'
		// of the curve and another to the 'left' (supposing the curve is oriented
		// in order of increasing node indices, i.e., the first node is nodes[][0],
		// the last is nodes[][nodes.length-1]).
		final float[][] right = new float[2][len]; // store the gridpts
		final float[][] left = new float[2][len];

		if (len <= 1) return null;

		// The senses of left and right remain the same except when the curve
		// doubles back on itself, i.e., when two consecutive segments form an
		// angle of 180 degrees. Then the flag 'orientationChanged' switches.
		boolean orientationChanged = false; // if this is true, right and left
		// must be switched

		// This loop computes the points to use in the Gridded2DSets. The sets
		// are constructed separately since each sets corner points are shared
		// by two other sets (the front points of one set are the rear points of the
		// next, etc.)
		for (int i = 0; i < len; i++) {
			// Each iteration of this loop computes a new pair of points
			// rigthPt and leftPt. The points are computed differently
			// depending on whether the index i points to
			// 1) the first node
			// 2) the last node
			// 3) some interior node
			float[] rightPt = new float[2];
			float[] leftPt = new float[2];

			// System.out.println("index = " + i);
			if (i == 0) {
				// Case 1: the first node
				// Just compute points a distance 'width' from the first node,
				// in the direction perpendicular to the first segment

				final float[] p1 = new float[] { nodes[0][0], nodes[1][0] }; // first
																																			// node
				final float[] p2 = new float[] { nodes[0][1], nodes[1][1] }; // second
																																			// node
				// get a perpendicular vector to the right of p2-p1
				final float[] vPerp = MathUtil.getRightPerpendicularVector2D(p2, p1);
				rightPt = MathUtil.add(p1, MathUtil.scalarMultiply(vPerp, width));
				leftPt = MathUtil.add(p1, MathUtil.scalarMultiply(vPerp, -1f * width));
				// System.out.print("First node: ");
				// System.out.print("p1: "); print(p1);
				// System.out.print("p2: "); print(p2);
				// System.out.print("vPerp: "); print(vPerp);
				// System.out.print("right pt: "); print(rightPt);
				// System.out.print("left pt: "); print(leftPt);
			}
			else if (i == len - 1) {
				// Case 2: the last node
				// Just compute points a distance 'width' from the last node,
				// in the direction perpendicular to the last segment

				final float[] p1 = new float[] { nodes[0][i - 1], nodes[1][i - 1] }; // penultimate
				// node
				final float[] p2 = new float[] { nodes[0][i], nodes[1][i] }; // last
																																			// node

				// Test if the last segment doubles back on the second to last segment
				boolean anti = false; // for 'antiparallel'
				if (len >= 3) {
					final float[] p0 = new float[] { nodes[0][i - 2], nodes[1][i - 2] }; // 3rd
																																								// 2
																																								// last
					final float[] v1 = MathUtil.unit(MathUtil.vector(p1, p0));
					final float[] v2 = MathUtil.unit(MathUtil.vector(p2, p1));
					if (MathUtil.areOpposite(v1, v2)) {
						anti = true;
						orientationChanged = orientationChanged ? false : true; // toggle
					}
				}

				float[] vPerp;
				if (anti) vPerp = MathUtil.getRightPerpendicularVector2D(p1, p2);
				// p1 and p2 above have been switched above to obtain a reflection
				else vPerp = MathUtil.getRightPerpendicularVector2D(p2, p1);

				// get a perpendicular vector to the right of p2-p1
				// add a multiple of this vector to the point p2, the last node in
				// the curve
				final float[] a =
					MathUtil.add(p2, MathUtil.scalarMultiply(vPerp, width));
				final float[] b =
					MathUtil.add(p2, MathUtil.scalarMultiply(vPerp, -1f * width));

				rightPt = orientationChanged ? b : a;
				leftPt = orientationChanged ? a : b;

				// System.out.print("\nLast node:");
				// System.out.print("p1: "); print(p1);
				// System.out.print("p2: "); print(p2);
				// System.out.print("vPerp: "); print(vPerp);
				// System.out.print("right pt: "); print(rightPt);
				// System.out.print("left pt: "); print(leftPt);
				// System.out.println("orientation changed = " + orientationChanged);
			}
			else {
				// Case 3: all interior nodes
				// Compute points a perpendicular distance 'width' away from the curve
				// as in cases 1 and 2. This time, however, the points lie on the
				// the line bisecting the angle formed by adjacent segments.

				final float[] p1 = { nodes[0][i - 1], nodes[1][i - 1] };
				final float[] p2 = { nodes[0][i], nodes[1][i] };
				final float[] p3 = { nodes[0][i + 1], nodes[1][i + 1] };

				// System.out.print("\nNode index " + i + ":");
				// System.out.print("p1: "); print(p1);
				// System.out.print("p2: "); print(p2);
				// System.out.print("p3: "); print(p3);

				// Test if the line doubles back on itself
				// check if p2-p3 and p3-p2 are antiparallel
				final float[] v1 = MathUtil.unit(MathUtil.vector(p2, p1));
				final float[] v2 = MathUtil.unit(MathUtil.vector(p3, p2));
				if (MathUtil.areOpposite(v1, v2)) {
					// vectors are antiparallel. just use v1 to calculate right and left
					final float[] vPerp = MathUtil.getRightPerpendicularVector2D(p2, p1);
					final float[] a =
						MathUtil.add(p2, MathUtil.scalarMultiply(vPerp, width));
					final float[] b =
						MathUtil.add(p2, MathUtil.scalarMultiply(vPerp, -1f * width));

					rightPt = orientationChanged ? b : a;
					leftPt = orientationChanged ? a : b;
					// System.out.print("vPerp: "); print(vPerp);
					orientationChanged = orientationChanged ? false : true; // toggle
				}
				else {
					// obtain unit vectors bisecting p2-p1 and p3-p2
					final float[] bisector =
						MathUtil.getRightBisectorVector2D(p1, p2, p3);
					final float[] bisectorReflected =
						MathUtil.scalarMultiply(bisector, -1f);

					// compute angle between the p2-p1 and bisector
					float sin =
						Math.abs(MathUtil.cross2D(bisector, MathUtil.unit(MathUtil.vector(
							p2, p1))));
					if (sin < 0.1f) sin = 0.1f; // keep a lower bound on this value to
					// prevent 1/sin from becoming too large

					// compute offset distance from curve
					final float offset = width / sin;

					final float[] a =
						MathUtil.add(p2, MathUtil.scalarMultiply(bisector, offset));
					final float[] b =
						MathUtil
							.add(p2, MathUtil.scalarMultiply(bisectorReflected, offset));

					rightPt = orientationChanged ? b : a;
					leftPt = orientationChanged ? a : b;

					// System.out.print("bisector: "); print(bisector);
					// System.out.print("bisectorReflected: "); print(bisectorReflected);
					// System.out.print("sin = " + sin);
					// System.out.print("offset = " + offset);
				}
				// System.out.print("right pt: "); print(rightPt);
				// System.out.print("left pt: "); print(leftPt);
				// System.out.println("orientation changed = " + orientationChanged);
			} // end else

			// copy calculated values to storage arrays
			right[0][i] = rightPt[0];
			right[1][i] = rightPt[1];
			left[0][i] = leftPt[0];
			left[1][i] = leftPt[1];
		} // end for

		// assemble an array of gridded sets representing the highlighting
		return makeGridded2DSets(domain, nodes, right, left, width);
	}

	/**
	 * Makes valid Gridded2DSets from the arrays supplied.
	 * 
	 * @param nodes The nodes of the noded object
	 * @param right The points on the right side of the noded object (supposing
	 *          node indices increase from left to right across the screen)
	 * @param left The points on the left side of the noded object (again
	 *          supposing node indices increase from left to right across the
	 *          screen).
	 */
	public static Vector makeGridded2DSets(final RealTupleType domain,
		final float[][] nodes, final float[][] right, final float[][] left,
		final float width)
	{

		// Note:
		// This method should probably determine whether Gridded2DSets will
		// be invalid itself, but instead it relies on VisAD's Gridded2DSet
		// constructor to do so. If the constructor throws an exception, this
		// method tries to construct the troublesome set differently.

		final int len = left[0].length;
		final Vector sets = new Vector(100);
		for (int i = 0; i < len - 1; i++) {
			// This loop constructs Gridded2D sets, one for each segment of a noded
			// object (implied here by the arrays right and left).
			// For each segment (between indices 'i' and 'i+1'),
			// try to construct a trapezoidal set:
			float[][] setSamples =
				{ { right[0][i], right[0][i + 1], left[0][i], left[0][i + 1] },
					{ right[1][i], right[1][i + 1], left[1][i], left[1][i + 1] } };

			try {
				final Gridded2DSet set =
					new Gridded2DSet(domain, setSamples, 2, 2, null, null, null, false);
				sets.add(set);
			}
			catch (final VisADException ex) {
				// If samples form an invalid set, the grid is most likely
				// bow-tie shaped.
				// "Uncross" the box just by switching the order of the two
				// left points.

				// System.out.println("formed 1 invalid set");
				setSamples =
					new float[][] {
						{ right[0][i], right[0][i + 1], left[0][i + 1], left[0][i] },
						{ right[1][i], right[1][i + 1], left[1][i + 1], left[1][i] } };

				try {
					final Gridded2DSet set =
						new Gridded2DSet(domain, setSamples, 2, 2, null, null, null, false);
					sets.add(set);
				}
				catch (final VisADException ex2) {
					// If the uncrossed samples still form an invalid set, give up
					// trying to make a trapezoidal set.
					// Make a plain rectangular set, disregarding left and right
					// arrays. Instead use the node array and calculate the corners of the
					// set using perpendiculars to the line segment under consideration.
					// System.out.println("formed 2 invalid sets");
					final float[] p1 = { nodes[0][i], nodes[1][i] };
					final float[] p2 = { nodes[0][i + 1], nodes[1][i + 1] };

					final float[] vPerp = MathUtil.getRightPerpendicularVector2D(p2, p1);
					final float[] vPerpReflected = MathUtil.scalarMultiply(vPerp, -1f);
					final float[] s1 = MathUtil.add(p1, vPerp);
					final float[] s2 = MathUtil.add(p2, vPerp);
					final float[] s3 = MathUtil.add(p1, vPerpReflected);
					final float[] s4 = MathUtil.add(p2, vPerpReflected);

					setSamples =
						new float[][] { { s1[0], s2[0], s3[0], s4[0] },
							{ s1[1], s2[1], s3[1], s4[1] } };

					try {
						final Gridded2DSet set =
							new Gridded2DSet(domain, setSamples, 2, 2, null, null, null,
								false);
						sets.add(set);
					}
					catch (final VisADException ex3) {
						System.out.println("OverlayUtil: error making Gridded2DSets: " +
							"all three tries produced invalid sets (index " + i + ").");
						System.out.println("left points");
						print(left);
						System.out.println("\nright points");
						print(right);
						ex3.printStackTrace();
					}
				}
			}
		} // end for

		return sets;
	}

	// -- Math Methods --

	/**
	 * Calculates smoothed coordinates using "single exponential smoothing" as
	 * described in Littlewood and Inman, _Computer-assisted DNA length
	 * measurements..._. Nucleic Acids Research, V 10 No. 5. (1982) p. 1694
	 */
	public static float[] smooth(final float[] un, final float[] cn1,
		final float S)
	{
		final float[] cn = new float[2];
		for (int i = 0; i < 2; i++) {
			cn[i] = S * un[i] + (1 - S) * cn1[i];
		}
		return cn;
	}

	/**
	 * Casts and converts an array of floats in domain coordinates to doubles in
	 * pixel coordinates.
	 */
	public static double[][] floatsToPixelDoubles(final DisplayImpl d,
		final float[][] nodes)
	{
		final double[][] nodesDbl = new double[nodes.length][nodes[0].length];
		for (int j = 0; j < nodes[0].length; j++) {
			final int[] c =
				CursorUtil.domainToPixel(d, new double[] { nodes[0][j], nodes[1][j] });
			nodesDbl[0][j] = c[0];
			nodesDbl[1][j] = c[1];
		}
		return nodesDbl;
	}

	/** Prints a VisAD style group of points. */
	public static void print(final float[][] points) {
		for (int i = 0; i < points[0].length; i++) {
			print(points[0][i], points[1][i]);
		}
	}

	/** Prints a point. */
	public static void print(final float x, final float y) {
		System.out.println("[" + x + "," + y + "]");
	}

	/** Prints a point. */
	public static void print(final float[] p) {
		print(p[0], p[1]);
	}
}

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

import visad.DataImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.GriddedSet;
import visad.RealTupleType;
import visad.TupleType;
import visad.VisADException;

/**
 * OverlayOval is a bounding oval overlay.
 */
public class OverlayOval extends OverlayObject {

	// -- Static Fields --

	/** The names of the statistics this object reports. */
	public static final String COORDS = "Coordinates";
	public static final String CTR = "Center";
	public static final String RAD = "Radius";
	public static final String MAJ = "Major Axis Length";
	public static final String MIN = "Minor Axis Length";
	public static final String AREA = "Area";
	public static final String ECC = "Eccentricity";
	public static final String CIRC = "Circumference (approximate)";
	protected static final String[] STAT_TYPES = { COORDS, CTR, RAD, MAJ, MIN,
		AREA, ECC, CIRC };

	// -- Constants --

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

	// -- Constructors --

	/** Constructs an uninitialized bounding oval. */
	public OverlayOval(final OverlayTransform overlay) {
		super(overlay);
	}

	/** Constructs an bounding oval. */
	public OverlayOval(final OverlayTransform overlay, final float x1,
		final float y1, final float x2, final float y2)
	{
		super(overlay);
		this.x1 = x1;
		this.y1 = y1;
		this.x2 = x2;
		this.y2 = y2;
	}

	// -- Static methods --

	/** Returns the names of the statistics this object reports. */
	public static String[] getStatTypes() {
		return STAT_TYPES;
	}

	// -- OverlayObject API methods --

	/**
	 * Returns whether this object is drawable, i.e., is of nonzero size, area,
	 * length, etc.
	 */
	@Override
	public boolean hasData() {
		return (x1 != x2 && y1 != y2);
	}

	/** Gets VisAD data object representing this overlay. */
	@Override
	public DataImpl getData() {
		if (!hasData()) return null;

		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		final float cx = (x1 + x2) / 2;
		final float cy = (y1 + y2) / 2;
		final float rx = cx > x1 ? cx - x1 : cx - x2;
		final float ry = cy > y1 ? cy - y1 : cy - y2;

		final int arcLen = ARC[0].length;
		final int len = 2 * arcLen;
		final float[][] setSamples = new float[2][filled ? len : len + 1];

		// top half of circle
		for (int i = 0; i < arcLen; i++) {
			setSamples[0][i] = cx + rx * ARC[0][i];
			setSamples[1][i] = cy + ry * ARC[1][i];
		}

		// bottom half of circle
		for (int i = 0; i < arcLen; i++) {
			final int ndx = filled ? arcLen + i : len - i - 1;
			setSamples[0][ndx] = cx + rx * ARC[0][i];
			setSamples[1][ndx] = cy - ry * ARC[1][i];
		}

		GriddedSet fieldSet = null;
		try {
			if (filled) {
				fieldSet =
					new Gridded2DSet(domain, setSamples, arcLen, 2, null, null, null,
						false);
			}
			else {
				setSamples[0][len] = setSamples[0][0];
				setSamples[1][len] = setSamples[1][0];
				fieldSet =
					new Gridded2DSet(domain, setSamples, setSamples[0].length, null,
						null, null, false);
			}
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}

		final Color col = selected ? GLOW_COLOR : color;
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

	/** Computes the shortest distance from this object to the given point. */
	@Override
	public double getDistance(final double x, final double y) {
		double xdist = 0;
		if (x < x1 && x < x2) xdist = Math.min(x1, x2) - x;
		else if (x > x1 && x > x2) xdist = x - Math.max(x1, x2);
		double ydist = 0;
		if (y < y1 && y < y2) ydist = Math.min(y1, y2) - y;
		else if (y > y1 && y > y2) ydist = y - Math.max(y1, y2);
		return Math.sqrt(xdist * xdist + ydist * ydist);
	}

	/** Returns a specific statistic of this object. */
	@Override
	public String getStat(final String name) {
		final float xx = x2 - x1;
		final float yy = y2 - y1;
		final float centerX = x1 + xx / 2;
		final float centerY = y1 + yy / 2;
		final float radiusX = (xx < 0 ? -xx : xx) / 2;
		final float radiusY = (yy < 0 ? -yy : yy) / 2;
		float major, minor;
		if (radiusX > radiusY) {
			major = radiusX;
			minor = radiusY;
		}
		else {
			major = radiusY;
			minor = radiusX;
		}
		final float eccen =
			(float) Math.sqrt(1 - (minor * minor) / (major * major));
		final float area = (float) (Math.PI * major * minor);

		// ellipse circumference approximation algorithm due to Ramanujan found at
		// http://mathforum.org/dr.math/faq/formulas/faq.ellipse.circumference.html
		final float mm = (major - minor) / (major + minor);
		final float q = 3 * mm * mm;
		final float circum =
			(float) (Math.PI * (major + minor) * (1 + q / (10 + Math.sqrt(4 - q))));

		if (name.equals(COORDS)) {
			return "(" + x1 + ", " + y1 + ")-(" + x2 + ", " + y2 + ")";
		}
		else if (name.equals(CTR)) {
			return "(" + centerX + ", " + centerY + ")";
		}
		else if (name.equals(RAD)) {
			return "(" + radiusX + ", " + radiusY + ")";
		}
		else if (name.equals(MAJ)) {
			return "" + major;
		}
		else if (name.equals(MIN)) {
			return "" + minor;
		}
		else if (name.equals(AREA)) {
			return "" + area;
		}
		else if (name.equals(ECC)) {
			return "" + eccen;
		}
		else if (name.equals(CIRC)) {
			return "" + circum;
		}
		else return "No such statistic for this overlay type";
	}

	/** Retrieves useful statistics about this overlay. */
	@Override
	public String getStatistics() {
		final float xx = x2 - x1;
		final float yy = y2 - y1;
		final float centerX = x1 + xx / 2;
		final float centerY = y1 + yy / 2;
		final float radiusX = (xx < 0 ? -xx : xx) / 2;
		final float radiusY = (yy < 0 ? -yy : yy) / 2;
		float major, minor;
		if (radiusX > radiusY) {
			major = radiusX;
			minor = radiusY;
		}
		else {
			major = radiusY;
			minor = radiusX;
		}
		final float eccen =
			(float) Math.sqrt(1 - (minor * minor) / (major * major));
		final float area = (float) (Math.PI * major * minor);

		// ellipse circumference approximation algorithm due to Ramanujan found at
		// http://mathforum.org/dr.math/faq/formulas/faq.ellipse.circumference.html
		final float mm = (major - minor) / (major + minor);
		final float q = 3 * mm * mm;
		final float circum =
			(float) (Math.PI * (major + minor) * (1 + q / (10 + Math.sqrt(4 - q))));

		return "Oval " + COORDS + " = (" + x1 + ", " + y1 + ")\n" + CTR + " = (" +
			centerX + ", " + centerY + ")\n" + RAD + " = (" + radiusX + ", " +
			radiusY + ")\n" + MAJ + " = " + major + "; " + MIN + " = " + minor +
			"\n" + AREA + " = " + area + "; " + ECC + " = " + eccen + "\n" + CIRC +
			" = " + circum;
	}

	/** True iff this overlay has an endpoint coordinate pair. */
	@Override
	public boolean hasEndpoint() {
		return true;
	}

	/** True iff this overlay has a second endpoint coordinate pair. */
	@Override
	public boolean hasEndpoint2() {
		return true;
	}

	/** True iff this overlay supports the filled parameter. */
	@Override
	public boolean canBeFilled() {
		return true;
	}

	// -- Object API methods --

	/** Gets a short string representation of this overlay oval. */
	@Override
	public String toString() {
		return "Oval";
	}

}

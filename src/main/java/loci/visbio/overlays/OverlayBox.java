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
 * OverlayBox is a rectangle overlay.
 */
public class OverlayBox extends OverlayObject {

	// -- Static Fields --

	/** The names of the statistics this object reports. */
	protected static final String COORDS = "Coordinates";
	protected static final String CTR = "Center";
	protected static final String WD = "Width";
	protected static final String HT = "Height";
	protected static final String AREA = "Area";
	protected static final String PERIM = "Perimeter";
	protected static final String[] STAT_TYPES = { COORDS, CTR, WD, HT, AREA,
		PERIM };

	// -- Constructors --

	/** Constructs an uninitialized bounding rectangle. */
	public OverlayBox(final OverlayTransform overlay) {
		super(overlay);
	}

	/** Constructs a bounding rectangle. */
	public OverlayBox(final OverlayTransform overlay, final float x1,
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
		// don't try to render a zero-area box
		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		float[][] setSamples = null;
		GriddedSet fieldSet = null;

		try {
			if (filled) {
				setSamples = new float[][] { { x1, x2, x1, x2 }, { y1, y1, y2, y2 } };
				fieldSet =
					new Gridded2DSet(domain, setSamples, 2, 2, null, null, null, false);
			}
			else {
				setSamples =
					new float[][] { { x1, x2, x2, x1, x1 }, { y1, y1, y2, y2, y1 } };
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
		final float width = xx < 0 ? -xx : xx;
		final float height = yy < 0 ? -yy : yy;
		final float centerX = x1 + xx / 2;
		final float centerY = y1 + yy / 2;
		final float area = width * height;
		final float perim = width + width + height + height;

		if (name.equals(COORDS)) {
			return "(" + x1 + ", " + y1 + ")-(" + x2 + ", " + y2 + ")";
		}
		else if (name.equals(CTR)) {
			return "(" + centerX + ", " + centerY + ")";
		}
		else if (name.equals(WD)) {
			return "" + width;
		}
		else if (name.equals(HT)) {
			return "" + height;
		}
		else if (name.equals(AREA)) {
			return "" + area;
		}
		else if (name.equals(PERIM)) {
			return "" + perim;
		}
		else return "No such statistic for this overlay type";
	}

	/** Retrieves useful statistics about this overlay. */
	@Override
	public String getStatistics() {
		final float xx = x2 - x1;
		final float yy = y2 - y1;
		final float width = xx < 0 ? -xx : xx;
		final float height = yy < 0 ? -yy : yy;
		final float centerX = x1 + xx / 2;
		final float centerY = y1 + yy / 2;
		final float area = width * height;
		final float perim = width + width + height + height;

		return "Box " + COORDS + " = (" + x1 + ", " + y1 + ")-(" + x2 + ", " + y2 +
			")\n" + CTR + " = (" + centerX + ", " + centerY + ")\n" + WD + " = " +
			width + "; " + HT + " = " + height + "\n" + AREA + " = " + area + "; " +
			PERIM + " = " + perim;
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

	/** Gets a short string representation of this overlay box. */
	@Override
	public String toString() {
		return "Box";
	}
}

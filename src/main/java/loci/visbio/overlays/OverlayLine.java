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

import loci.visbio.util.MathUtil;
import visad.DataImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.GriddedSet;
import visad.RealTupleType;
import visad.TupleType;
import visad.VisADException;

/**
 * OverlayLine is a measurement line overlay.
 */
public class OverlayLine extends OverlayObject {

	// -- Static Fields --

	/** The names of the statistics this object reports. */
	protected static final String COORDS = "Coordinates";
	protected static final String LENGTH = "Length";
	protected static final String[] STAT_TYPES = { COORDS, LENGTH };

	// -- Constructors --

	/** Constructs an uninitialized measurement line. */
	public OverlayLine(final OverlayTransform overlay) {
		super(overlay);
	}

	/** Constructs a measurement line. */
	public OverlayLine(final OverlayTransform overlay, final float x1,
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
		return (x1 != x2 || y1 != y2);
	}

	/** Gets VisAD data object representing this overlay. */
	@Override
	public DataImpl getData() {
		if (!hasData()) return null;

		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		final float[][] setSamples = { { x1, x2 }, { y1, y2 } };

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
			final GriddedSet fieldSet =
				new Gridded2DSet(domain, setSamples, setSamples[0].length, null, null,
					null, false);
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
		return MathUtil.getDistance(new double[] { x1, y1 },
			new double[] { x2, y2 }, new double[] { x, y }, true);
	}

	/** Returns a specific statistic of this object. */
	@Override
	public String getStat(final String name) {
		if (name.equals(COORDS)) {
			return "(" + x1 + ", " + y1 + ")-(" + x2 + ", " + y2 + ")";
		}
		else if (name.equals(LENGTH)) {
			final float xx = x2 - x1;
			final float yy = y2 - y1;
			final float length = (float) Math.sqrt(xx * xx + yy * yy);
			return "" + length;
		}
		else return "No such statistic for this overlay type";
	}

	/** Retrieves useful statistics about this overlay. */
	@Override
	public String getStatistics() {
		final float xx = x2 - x1;
		final float yy = y2 - y1;
		final float length = (float) Math.sqrt(xx * xx + yy * yy);

		return "Line " + COORDS + " = (" + x1 + ", " + y1 + ")-(" + x2 + ", " + y2 +
			")\n" + LENGTH + " = " + length;
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

	// -- Object API methods --

	/** Gets a short string representation of this measurement line. */
	@Override
	public String toString() {
		return "Line";
	}

	// -- Helper methods --

	// ACS -- keep this method around in case since figuring out the geometry
	// is a bother.

	/**
	 * Helper method for computing coordinates of two corner points of the
	 * rectangular selection grid for a line or arrow overlay.
	 */
	protected static float[]
		computeCorners(final float x1, final float y1, final float x2,
			final float y2, final float padding, final float multiplier)
	{
		// multiplier is used to widen the distance between corner points
		// appropriately for the "wide" end of the arrow overlay; for lines,
		// multiplier is 1 (no widening required)

		final double xx = x2 - x1;
		final double yy = y2 - y1;
		final double dist = Math.sqrt(xx * xx + yy * yy);
		final double mult = padding / dist;
		final float qx = (float) (mult * xx);
		final float qy = (float) (mult * yy);

		return new float[] { x2 + qx + multiplier * qy, y2 + qy - multiplier * qx,
			x2 + qx - multiplier * qy, y2 + qy + multiplier * qx };
	}
}

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
 * OverlayMarker is a marker crosshairs overlay.
 */
public class OverlayMarker extends OverlayObject {

	// -- Static Fields --

	/** The names of the statistics this object reports. */
	protected static final String COORDS = "Coordinates";
	protected static final String[] STAT_TYPES = { COORDS };

	/** The default width of the marker. */
	protected float width;

	// -- Constructors --

	/** Constructs an uninitialized measurement marker. */
	public OverlayMarker(final OverlayTransform overlay) {
		super(overlay);
	}

	/** Constructs a measurement marker. */
	public OverlayMarker(final OverlayTransform overlay, final float x,
		final float y)
	{
		super(overlay);
		x1 = x;
		y1 = y;
		width = getDefaultWidth();
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
		return true;
	}

	/** Gets VisAD data object representing this overlay. */
	@Override
	public DataImpl getData() {
		if (!hasData()) return null;
		final RealTupleType domain = overlay.getDomainType();
		final TupleType range = overlay.getRangeType();

		final float[][] setSamples =
			{ { x1, x1, x1, x1 + width, x1 - width },
				{ y1 + width, y1 - width, y1, y1, y1 } };
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
		final double xx = x1 - x;
		final double yy = y1 - y;
		return Math.sqrt(xx * xx + yy * yy);
	}

	/** Returns a specific statistic of this object. */
	@Override
	public String getStat(final String name) {
		if (name.equals(COORDS)) {
			return "(" + x1 + ", " + y1 + ")";
		}
		return "No such statistic for this overlay type";
	}

	/** Retrieves useful statistics about this overlay. */
	@Override
	public String getStatistics() {
		return "Marker " + COORDS + " = (" + x1 + ", " + y1 + ")";
	}

	/** True iff this overlay has an endpoint coordinate pair. */
	@Override
	public boolean hasEndpoint() {
		return true;
	}

	/** OverlayMarker's are scalable--returns true. */
	@Override
	public boolean isScalable() {
		return true;
	}

	/** Rescales an OverlayMarker. */
	@Override
	public void rescale(final float multiplier) {
		// width = getDefaultWidth() * multiplier;
	}

	// -- Overlay Marker API methods --

	/** Returns the defualt width of this marker. */
	protected float getDefaultWidth() {
		return 0.02f * overlay.getScalingValue();
	}

	/** Returns the width of this marker. */
	protected float getWidth() {
		return width;
	}

	// -- Object API methods --

	/** Gets a short string representation of this measurement marker. */
	@Override
	public String toString() {
		return "Marker";
	}

}

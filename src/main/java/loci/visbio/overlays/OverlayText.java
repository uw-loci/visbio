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
import java.awt.FontMetrics;
import java.rmi.RemoteException;

import visad.DataImpl;
import visad.FieldImpl;
import visad.FunctionType;
import visad.RealTuple;
import visad.RealTupleType;
import visad.Set;
import visad.SingletonSet;
import visad.TupleType;
import visad.VisADException;

/**
 * OverlayText is a text string overlay.
 */
public class OverlayText extends OverlayObject {

	// -- Static Fields --

	/** The names of the statistics this object reports. */
	protected static final String COORDS = "Coordinates";
	protected static final String[] STAT_TYPES = { COORDS };

	// -- Constructors --

	/** Constructs an uninitialized text string overlay. */
	public OverlayText(final OverlayTransform overlay) {
		super(overlay);
	}

	/** Constructs a text string overlay. */
	public OverlayText(final OverlayTransform overlay, final float x,
		final float y, final String text)
	{
		super(overlay);
		x1 = x;
		y1 = y;
		x2 = x;
		y2 = y;
		this.text = text;
		computeTextBounds();
		// System.out.println("New text object created with text [" + text + "]");
		// System.out.println("text bounds: [" + x1 + "," + y1 + "] [" + x2 + "," +
		// y2 + "]");
	}

	// -- Static methods --

	/** Returns the names of the statistics this object reports. */
	public static String[] getStatTypes() {
		return STAT_TYPES;
	}

	// -- OverlayText API methods --

	public void computeTextBounds() {
		// Computing the grid for text overlays is difficult because the size of
		// the overlay depends on the font metrics, which are obtained from an AWT
		// component (in this case, window.getDisplay().getComponent() for a
		// display window), but data transforms have no knowledge of which display
		// windows are currently displaying them.

		// HACK - for now, use this harebrained scheme to estimate the bounds
		final int sx = overlay.getScalingValueX();
		final int sy = overlay.getScalingValueY();
		final float mw = sx / 318f, mh = sy / 640f; // obtained through
																								// experimentation
		final FontMetrics fm = overlay.getFontMetrics();
		x2 = x1 + mw * fm.stringWidth(text);
		y2 = y1 + mh * fm.getHeight();
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
		final TupleType range = overlay.getTextRangeType();

		final Color col = selected ? GLOW_COLOR : color;
		final float r = col.getRed() / 255f;
		final float g = col.getGreen() / 255f;
		final float b = col.getBlue() / 255f;

		FieldImpl field = null;
		try {
			final FunctionType fieldType = new FunctionType(domain, range);
			final Set fieldSet =
				new SingletonSet(new RealTuple(domain, new double[] { x1, y1 }));
			field = new FieldImpl(fieldType, fieldSet);
			field.setSample(0, overlay.getTextRangeValue(text, r, g, b, 1), false);
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
		if (name.equals(COORDS)) {
			return "(" + x1 + ", " + y1 + ")";
		}
		return "No such statistic for this overlay type";
	}

	/** Retrieves useful statistics about this overlay. */
	@Override
	public String getStatistics() {
		return "Text " + COORDS + " = (" + x1 + ", " + y1 + ")";
	}

	/** True iff this overlay has an endpoint coordinate pair. */
	@Override
	public boolean hasEndpoint() {
		return true;
	}

	/** True iff this overlay object returns text to render. */
	@Override
	public boolean hasText() {
		return true;
	}

	// -- Object API methods --

	/** Gets a short string representation of this overlay text. */
	@Override
	public String toString() {
		return "Text";
	}

}

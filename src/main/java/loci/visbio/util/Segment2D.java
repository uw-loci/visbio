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

package loci.visbio.util;

import java.awt.geom.Point2D.Float;

/**
 * A 2D line segment.
 */
public class Segment2D {

	/** Endpoints. */
	protected Float a, b;

	/** Constructor. */
	public Segment2D(final float[] a, final float[] b) {
		this.a = new Float(a[0], a[1]);
		this.b = new Float(b[0], b[1]);
	}

	public Float getPtA() {
		return a;
	}

	public Float getPtB() {
		return b;
	}

	/** Whether another segment intersects this one. */
	public boolean intersects(final Segment2D y) {
		// first check bounding box, then straddle
		return (this.boundingBoxOverlapsBoundingBoxOf(y) && this.straddles(y) && y
			.straddles(this));
	}

	/**
	 * Whether another segment meets the bounding box of this segment. True if
	 * either endpt of the supplied segment is in/on the bounding box.
	 */
	public boolean boundingBoxOverlapsBoundingBoxOf(final Segment2D y) {
		// collect all parameters
		final float[] xa = { (float) this.a.getX(), (float) this.a.getY() };
		final float[] xb = { (float) this.b.getX(), (float) this.b.getY() };
		final float[] ya = { (float) y.getPtA().getX(), (float) y.getPtA().getY() };
		final float[] yb = { (float) y.getPtB().getX(), (float) y.getPtB().getY() };

		// inside is not strict--a point on the boundary is considered inside
		boolean meets = false;
		if ((MathUtil.inside(xa, ya, yb) || MathUtil.inside(xb, ya, yb)) ||
			(MathUtil.inside(ya, xa, xb) || MathUtil.inside(yb, xa, xb))) meets =
			true;
		return meets;
	}

	/** Whether another segment crosses the line defined by this segment. */
	public boolean straddles(final Segment2D y) {
		// collect all parameters
		final double ax = this.a.getX();
		final double ay = this.a.getY();
		final double bx = this.a.getX();
		final double by = this.b.getY();
		final double yax = y.a.getX();
		final double yay = y.a.getY();
		final double ybx = y.b.getX();
		final double yby = y.b.getY();

		final float[] v1 = { (float) (ax - yax), (float) (ay - yay) };
		final float[] v2 = { (float) (ybx - yax), (float) (yby - yay) };
		final float[] v3 = { (float) (bx - yax), (float) (by - yay) };

		final float cross1 = MathUtil.cross2D(v1, v2);
		final float cross2 = MathUtil.cross2D(v3, v2);

		// return true if cross products have opposite signs
		return ((cross1 < 0 && cross2 > 0) || (cross1 > 0 && cross2 < 0) ||
			cross1 == 0 || cross2 == 0);
	}

	@Override
	public String toString() {
		return "[" + a.getX() + "," + a.getY() + "]->[" + b.getX() + "," +
			b.getY() + "]";
	}
}

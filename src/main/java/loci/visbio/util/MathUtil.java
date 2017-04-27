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

import java.math.BigInteger;

/**
 * MathUtil contains useful mathematical functions.
 */
public final class MathUtil {

	// -- Constructor --

	private MathUtil() {}

	// -- Utility methods --

	/**
	 * Gets the distance between the points p and q, using the given conversion
	 * values between pixels and microns.
	 * 
	 * @param p Coordinates of the first endpoint
	 * @param q Coordinates of the second endpoint
	 * @param m Conversion values between microns and pixels
	 */
	public static double getDistance(final double[] p, final double[] q,
		final double[] m)
	{
		final int len = p.length;
		double sum = 0;
		for (int i = 0; i < len; i++) {
			final double dist = m[i] * (q[i] - p[i]);
			sum += dist * dist;
		}
		return Math.sqrt(sum);
	}

	/**
	 * Computes the minimum distance between the point v and the line a-b.
	 * 
	 * @param a Coordinates of the line's first endpoint
	 * @param b Coordinates of the line's second endpoint
	 * @param v Coordinates of the standalone point
	 * @param segment Whether distance computation should be constrained to the
	 *          given line segment
	 */
	public static double getDistance(final double[] a, final double[] b,
		final double[] v, final boolean segment)
	{
		final double[] p = getProjection(a, b, v, segment);
		return getDistance(p, v);
	}

	/**
	 * Computes the minimum distance between the point p and the point v.
	 * 
	 * @param p Coordinates of the first point
	 * @param v Coordinates of the second point
	 */
	public static double getDistance(final double[] p, final double[] v) {
		final int len = p.length; // redundant with getProjection
		double sum = 0;
		for (int i = 0; i < len; i++) {
			final double dist = p[i] - v[i];
			sum += dist * dist;
		}
		return Math.sqrt(sum);
	}

	/**
	 * Computes the minimum distance between the point p and the point v in double
	 * precision.
	 * 
	 * @param p Coordinates of the first point
	 * @param v Coordinates of the second point
	 */
	public static double getDistance(final float[] p, final float[] v) {
		final double[] pp = new double[p.length];
		final double[] vv = new double[v.length];
		for (int i = 0; i < p.length; i++)
			pp[i] = p[i];
		for (int i = 0; i < v.length; i++)
			vv[i] = v[i];
		return getDistance(pp, vv);
	}

	/**
	 * Computes the projection of the point v onto the line segment a-b.
	 * 
	 * @param a Coordinates of the segment's first endpoint
	 * @param b Coordinates of the segment's second endpoint
	 * @param v Coordinates of the point to be projected
	 * @param segment Whether the projection should be constrained to the given
	 *          line segment
	 */
	public static double[] getProjection(final double[] a, final double[] b,
		final double[] v, final boolean segment)
	{
		final int len = a.length;
		// vectors
		final double[] ab = new double[len];
		final double[] va = new double[len];
		for (int i = 0; i < len; i++) {
			ab[i] = a[i] - b[i];
			va[i] = v[i] - a[i];
		}

		// project v onto (a, b)
		double numer = 0;
		double denom = 0;
		for (int i = 0; i < len; i++) {
			numer += va[i] * ab[i];
			denom += ab[i] * ab[i];
		}
		final double c = numer / denom;
		final double[] p = new double[len];
		for (int i = 0; i < len; i++)
			p[i] = c * ab[i] + a[i];

		int flag = 0;
		if (segment) {
			for (int i = 0; i < len; i++) {
				if (p[i] > a[i] && p[i] > b[i]) flag = a[i] > b[i] ? 1 : 2;
				else if (p[i] < a[i] && p[i] < b[i]) flag = a[i] < b[i] ? 1 : 2;
				else continue;
				break;
			}
		}

		if (flag == 0) return p;
		else if (flag == 1) return a;
		else return b;
	}

	/**
	 * Gets distance to a curve of points connected by straight line segments.
	 * Also returns a segment index and wieght describing the location of the
	 * nearest point on the curve (see below).
	 * 
	 * @param x x coordinate of point in question
	 * @param y y coordinate of point in question
	 * @return an array double[3] with element 0 the distance to the nearest point
	 *         on the curve, element 1 the node index i of one end of closest line
	 *         segment (the other end being i+1), and element 2 the weight
	 *         (between zero and one) for determining the location of the closest
	 *         point by interpolation along the segment (i, i+1).
	 */
	public static double[] getDistSegWt(final double[][] nodes, final double x,
		final double y)
	{
		// assumes a non-ragged array of float[2][numNodes]
		double minDist = Double.MAX_VALUE;
		int seg = 0;
		double weight = 0;

		final int numNodes = nodes[0].length;

		// toss out the trivial case
		if (numNodes == 1) {
			final double xdist = x - nodes[0][0];
			final double ydist = y - nodes[1][0];
			minDist = Math.sqrt(xdist * xdist + ydist * ydist);
		}
		else {
			for (int i = 0; i < numNodes - 1; i++) {
				final double[] a = { nodes[0][i], nodes[1][i] };
				final double[] b = { nodes[0][i + 1], nodes[1][i + 1] };
				final double[] p = { x, y };

				final double[] proj = getProjection(a, b, p, true);
				final double dist = getDistance(p, proj);

				if (dist < minDist) {
					minDist = dist;
					seg = i;
					final double segDist = getDistance(a, b);
					final double fracDist = getDistance(a, proj);
					weight = fracDist / segDist;
				}
			}
		}

		// 'seg' is always an integer;
		// cast down to int in functions using this method.
		final double[] retvals = { minDist, seg, weight };
		return retvals;
	}

	/**
	 * Gets distance to a curve of points connected by straight line segments.
	 * Also returns a segment index and wieght describing the location of the
	 * nearest point on the curve (see below). All math takes place in double
	 * precision (floats upcast).
	 * 
	 * @param x x coordinate of point in question
	 * @param y y coordinate of point in question
	 * @return an array double[3] with element 0 the distance to the nearest point
	 *         on the curve, element 1 the node index i of one end of closest line
	 *         segment (the other end being i+1), and element 2 the weight
	 *         (between zero and one) for determining the location of the closest
	 *         point by interpolation along the segment (i, i+1).
	 */
	public static double[] getDistSegWt(final float[][] nodes, final float x,
		final float y)
	{
		// assumes a non-ragged array of float[2][numNodes]
		double minDist = Double.MAX_VALUE;
		int seg = 0;
		double weight = 0;

		final int numNodes = nodes[0].length;

		// toss out the trivial case
		if (numNodes == 1) {
			final double xdist = x - nodes[0][0];
			final double ydist = y - nodes[1][0];
			minDist = Math.sqrt(xdist * xdist + ydist * ydist);
		}
		else {
			for (int i = 0; i < numNodes - 1; i++) {
				final double[] a = { nodes[0][i], nodes[1][i] };
				final double[] b = { nodes[0][i + 1], nodes[1][i + 1] };
				final double[] p = { x, y };

				final double[] proj = getProjection(a, b, p, true);
				final double dist = getDistance(p, proj);

				if (dist < minDist) {
					minDist = dist;
					seg = i;
					final double segDist = getDistance(a, b);
					final double fracDist = getDistance(a, proj);
					weight = fracDist / segDist;
				}
			}
		}
		// 'seg' is always an integer;
		// cast down to int in functions using this method.
		final double[] retvals = { minDist, seg, weight };
		return retvals;
	}

	/**
	 * Computes a point along the line segment a[]-b[] based on the parameter
	 * weight.
	 */
	public static float[] computePtOnSegment(final float[] a, final float[] b,
		final float weight)
	{
		if (a.length != b.length) return null;
		final int len = a.length;
		final float[] p = new float[len];
		for (int i = 0; i < len; i++) {
			final float d = b[i] - a[i];
			p[i] = a[i] + d * weight;
		}
		return p;
	}

	/** Rounds the value to nearest value along the given progression. */
	public static int getNearest(final double val, final int min, final int max,
		final int step)
	{
		final int lo = (int) ((val - min) / step);
		final int hi = lo + step;
		int v = (val - lo < hi - val) ? lo : hi;
		if (v < min) v = min;
		else if (v > max) {
			final int q = (max - min) / step;
			v = min + q * step;
		}
		return v;
	}

	/** Units for use with getProductWithUnit method. */
	private static final String[] UNITS = { "", "kilo", "mega", "giga", "tera",
		"peta", "exa", "zetta", "yotta", "xona", "weka", "vunda", "uda", "treda",
		"sorta", "rinta", "quexa", "pepta", "ocha", "nena", "minga", "luma" };

	/**
	 * Returns the given number as a string complete with unit (e.g., kilo, mega,
	 * etc.), to the specififed number of decimal places.
	 */
	public static String getValueWithUnit(BigInteger value, final int places) {
		if (value == null) return null;
		String s = "1";
		for (int i = 0; i < places; i++)
			s += "0";
		final BigInteger scale = new BigInteger(s);
		value = value.multiply(scale);

		int unit = 0;
		final BigInteger kilo = new BigInteger("1024");
		final BigInteger half = new BigInteger("512");
		final BigInteger stop = kilo.multiply(scale);
		while (value.compareTo(stop) >= 0) {
			final BigInteger[] bi = value.divideAndRemainder(kilo);
			value = bi[0];
			if (bi[1].compareTo(half) >= 0) value = value.add(BigInteger.ONE);
			unit++;
		}
		final BigInteger[] bi = value.divideAndRemainder(scale);
		String dec = bi[1].toString();
		while (dec.length() < places)
			dec = "0" + dec;
		final String u =
			unit < UNITS.length ? UNITS[unit] : ("(2^" + (3 * unit) + ")");
		String result = bi[0].toString();
		if (places > 0) result += "." + dec;
		result += " " + u;
		return result;
	}

	// -- Vector Math Methods --

	/** Computes the cross product of two 2D vectors. */
	public static float cross2D(final float[] v1, final float[] v2) {
		return v1[0] * v2[1] - v1[1] * v2[0];
	}

	/** Computes the N-D unit vector in the direction of the vector supplied. */
	public static float[] unit(final float[] v) {
		final float mag = mag(v);
		final float[] vHat = new float[v.length];
		for (int i = 0; i < v.length; i++) {
			vHat[i] = v[i] / mag;
		}
		return vHat;
	}

	/** Computes the dot product of two N-D vectors. */
	public static float dot(final float[] a, final float[] b) {
		float sum = 0f;
		for (int i = 0; i < a.length; i++) {
			sum += a[i] * b[i];
		}
		return sum;
	}

	/** Computes the magnitude of an N-D vector. */
	public static float mag(final float[] a) {
		return (float) Math.sqrt(dot(a, a));
	}

	/** Creates the vector p2-(minus) p1. */
	public static float[] vector(final float[] p2, final float[] p1) {
		// assumes p1, p2 have same lengths
		if (p2.length != p1.length) return null;
		final int len = p2.length;
		final float[] v = new float[len];
		for (int i = 0; i < len; i++) {
			v[i] = p2[i] - p1[i];
		}
		return v;
	}

	/** Adds two N-D vectors. */
	public static float[] add(final float[] v1, final float[] v2) {
		// v1 and v2 should have same lengths
		if (v1.length != v2.length) return null;
		final float[] r = new float[v1.length];
		for (int i = 0; i < v1.length; i++) {
			r[i] = v1[i] + v2[i];
		}
		return r;
	}

	/** Multiplies an N-D vector by a scalar. */
	public static float[] scalarMultiply(final float[] v, final float s) {
		final int len = v.length;
		final float[] r = new float[len];
		for (int i = 0; i < len; i++) {
			r[i] = v[i] * s;
		}
		return r;
	}

	/** Tests whether two vectors are coordinate-wise opposite. */
	public static boolean areOpposite(final float[] v1, final float[] v2) {
		/*    if (v1.length != v2.length) return false;
		 boolean opposite = true;
		 for (int i=0; i<v1.length; i++) {
		   if (v1[i] != -v2[i]) {
		     opposite = false;
		     break;
		   }
		 }
		 return opposite;
		 */
		return areSame(v1, scalarMultiply(v2, -1f));
	}

	/** Tests whether two arrays of floats are identical. */
	public static boolean areSame(final float[] a, final float[] b) {
		boolean same = true;
		if (a.length == b.length) {
			for (int i = 0; i < a.length; i++) {
				if (a[i] != b[i]) {
					same = false;
					break;
				}
			}
		}
		else same = false;
		return same;
	}

	/** Tests whether two arrays of floats are different. */
	public static boolean areDifferent(final float[] a, final float[] b) {
		return !areSame(a, b);
	}

	// -- Computational Geometry Methods --

	/**
	 * Whether the point a is inside the N-D box implied by points b2 and b2
	 * (i.e., in 2D, whether a is inside the box with diagonal b1-b2; in 3D,
	 * whether a is inside the cube with diagonal b1-b2).
	 */
	public static boolean inside(final float[] a, final float[] b1,
		final float[] b2)
	{
		// assumes a, b1, b2 have same lengths
		boolean between = true;
		for (int i = 0; i < a.length; i++) {
			final boolean flip = b1[i] < b2[i] ? false : true;
			final float lo = flip ? b2[i] : b1[i];
			final float hi = flip ? b1[i] : b2[i];
			if (a[i] < lo || a[i] > hi) {
				between = false;
				break;
			}
		}
		return between;
	}

	/**
	 * Obtains the z-coordinate of the cross product of the 2D vectors
	 * {@code p2-p1} and {@code p3-p2}, useful for determining whether the curve
	 * {@code p1->p2->p3} is curving to the right or left.
	 */
	public static float orient2D(final float[] p1, final float[] p2,
		final float[] p3)
	{
		final float x1 = p1[0];
		final float y1 = p1[1];
		final float x2 = p2[0];
		final float y2 = p2[1];
		final float x3 = p3[0];
		final float y3 = p3[1];
		// z coord. of cross product of p2-(minus)p1 and p3-p2
		final float z = x1 * (y2 - y3) + x2 * (y3 - y1) + x3 * (y1 - y2);
		return z;
	}

	/**
	 * Gets a vector perpendicular to the vector {@code p2-p1}, pointing to the
	 * right with respect to the direction of {@code p2-p1}.
	 */
	public static float[] getRightPerpendicularVector2D(final float[] p2,
		final float[] p1)
	{
		final float[] v = vector(p2, p1);
		final float[] vPerp = { v[1], -v[0] };
		return unit(vPerp);
	}

	/**
	 * Gets a unit vector which bisects {@code (p1 - p2)} and {@code (p3 - p2)}.
	 * This vector points to the 'right' side of the line defined by
	 * {@code p1->p2->p3}.
	 */
	public static float[] getRightBisectorVector2D(final float[] p1,
		final float[] p2, final float[] p3)
	{
		// System.out.println("entering getBisectorVector2D ..."); //TEMP

		// Always retrieves the bisector vector on the right (as opposed to left)
		// side of the angle made by the two vectors.

		// z coord. of cross product of p2-(minus)p1 and p3-p2
		final float z = orient2D(p1, p2, p3);

		final float[] v1 = vector(p1, p2);
		final float[] v2 = vector(p3, p2);
		final float[] v1Hat = unit(v1);
		final float[] v2Hat = unit(v2);
		final float[] vAvg =
			{ (v1Hat[0] + v2Hat[0]) / 2f, (v1Hat[1] + v2Hat[1]) / 2f };

		float[] aBisector = null; // ... says what?
		if ((vAvg[0] == 0 && vAvg[1] == 0) || z == 0) {
			// Sometimes, z can have a very small nonzero value even when
			// the points have the same x=coordinate
			// (Apparently due to floating point arithmetic?)
			// To handle that case, test for parallel vectors without referring to z,
			// by comparing vAvg to <0, 0>.
			final float[] v = MathUtil.vector(p2, p1);
			aBisector = new float[] { v[1], -v[0] };
		}
		else if (z < 0) {
			// the curve is curving to the right--vAvg points right
			aBisector = vAvg;
		}
		else if (z > 0) {
			// the curve is curving to the left--vAvg points left
			aBisector = new float[] { -vAvg[0], -vAvg[1] };
		}

		final float[] bisector = unit(aBisector);

		return bisector;
	}

}

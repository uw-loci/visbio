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

package loci.visbio.utests.util;

import junit.framework.TestCase;
import loci.visbio.util.MathUtil;

/**
 * Unit tests for the MathUtil class.
 */
public class MathUtilTest extends TestCase {

	// -- Constants --

	public static final float DELTA = 0f;

	// -- Admin. Methods --

	/** Sets up the test fixture. */
	@Override
	protected void setUp() {
		// nothing to do.
	}

	/** Tears down the test fixture. */
	@Override
	protected void tearDown() {
		// nothing to do.
	}

	// -- Vector Math Tests --

	/** Tests MathUtil.mag(). */
	public void testMag() {
		final float[] v1 = new float[1000];
		float magSquared = 0f;
		for (int i = 0; i < v1.length; i++) {
			v1[i] = 5000f * (float) Math.random();
			magSquared += v1[i] * v1[i];
		}
		final float magExpected = (float) Math.sqrt(magSquared);

		assertEquals(magExpected, MathUtil.mag(v1), DELTA);
	}

	/** Tests MathUtil.dot(). */
	public void testDot() {
		final float[] v1 = { -5, 10f };
		final float[] v2 = { 10f, 12f };
		final float dotExpected = 70f;

		assertEquals(dotExpected, MathUtil.dot(v1, v2), DELTA);
	}

	/** Tests MathUtil.cross2D(). */
	public void testCross2D() {
		final float[] v1 = { 0, 10f };
		final float[] v2 = { 10f, 0 };
		final float crossExpected = -100f;

		assertEquals(crossExpected, MathUtil.cross2D(v1, v2), DELTA);
	}

	/** Tests MathUtil.unit(). */
	public void testUnit() {
		// ----------------------------------------------------------------------
		// vector 1
		// ----------------------------------------------------------------------
		final float[] v1 = { 0f, 10f };
		final float mag1 = (float) Math.sqrt(v1[0] * v1[0] + v1[1] * v1[1]);
		final float[] v1hatExpected = { v1[0] / mag1, v1[1] / mag1 };
		final float[] v1hat = MathUtil.unit(v1);
		for (int i = 0; i < v1.length; i++) {
			assertEquals(v1hatExpected[i], v1hat[i], DELTA);
		}

		// ----------------------------------------------------------------------
		// vector 2
		// ----------------------------------------------------------------------
		final float[] v2 = { 5.67f, 25.123f };
		final float mag2 = (float) Math.sqrt(v2[0] * v2[0] + v2[1] * v2[1]);
		final float[] v2hatExpected = { v2[0] / mag2, v2[1] / mag2 };
		final float[] v2hat = MathUtil.unit(v2);
		for (int i = 0; i < v2.length; i++) {
			assertEquals(v2hatExpected[i], v2hat[i], DELTA);
		}

		// ----------------------------------------------------------------------
		// vector 3 (large, random)
		// ----------------------------------------------------------------------
		final float[] v3 = new float[100];
		final float size = (float) Math.random() * 100f;
		final float sign = size < 50f ? -1f : 1f;
		// populate v3
		for (int i = 0; i < v3.length; i++) {
			v3[i] = (float) Math.random() * size * sign;
		}

		// calculate magnitude of v3
		float magV3Squared = 0f;
		for (int i = 0; i < v3.length; i++) {
			magV3Squared += v3[i] * v3[i];
		}
		final float magV3 = (float) Math.sqrt(magV3Squared);

		// populate v3Expected
		final float[] v3hatExpected = new float[v3.length];
		for (int i = 0; i < v3.length; i++) {
			v3hatExpected[i] = v3[i] / magV3;
		}

		// calculate v3Hat
		final float[] v3hat = MathUtil.unit(v3);

		// assert
		for (int i = 0; i < v3.length; i++) {
			assertEquals(v3hatExpected[i], v3hat[i], DELTA);
		}
	}

	/** Tests the MathUtil.vector() method. */
	public void testVector() {
		final float[] p1 = { 3f, 4f, 0f };
		final float[] p2 = { -1f, 5f, 6f };
		final float[] v1Expected = { p1[0] - p2[0], p1[1] - p2[1], p1[2] - p2[2] };

		final float[] v1 = MathUtil.vector(p1, p2);

		assertEquals(v1.length, v1Expected.length);
		for (int i = 0; i < v1.length; i++) {
			assertEquals(v1Expected[i], v1[i], DELTA);
		}
	}

	/** Tests the MathUtil.add() method. */
	public void testAdd() {
		final float[] v1 = { 0f, -1f, 2f };
		float[] v2 = { 1f, 3f, 1f };
		final float[] re = { 1f, 2f, 3f };
		float[] r = MathUtil.add(v1, v2);

		assertEquals(re.length, r.length);
		for (int i = 0; i < re.length; i++) {
			assertEquals(re[i], r[i], DELTA);
		}

		v2 = new float[] { 0f, -1f };

		r = MathUtil.add(v1, v2);
		assertNull(r);
	}

	/** Tests MathUtil.scalarMultiply(). */
	public void testScalarMultiply() {
		final float[] v = new float[100];
		final float[] vs = new float[100];
		final float s = 20.3f;
		// a vector
		for (int i = 0; i < 100; i++) {
			v[i] = (float) Math.random();
		}
		// vector times scalar
		for (int i = 0; i < 100; i++) {
			vs[i] = v[i] * s;
		}

		final float[] ve = MathUtil.scalarMultiply(v, s);
		for (int i = 0; i < 100; i++) {
			assertEquals(ve[i], vs[i], DELTA);
		}
	}

	/** Tests the same, different, and opposite methods. */
	public void testSameEtc() {
		final float[] v1 = { 0f, 3f, 4f, 5f, -24.65f, 0f, 45f };
		final float[] v2 = MathUtil.scalarMultiply(v1, -1f);
		final float[] v3 = { v2[0] };

		assertEquals(true, MathUtil.areSame(v1, v1));
		assertEquals(false, MathUtil.areSame(v1, v2));
		assertEquals(true, MathUtil.areDifferent(v1, v2));
		assertEquals(false, MathUtil.areDifferent(v1, v1));
		assertEquals(true, MathUtil.areOpposite(v1, v2));
		assertEquals(false, MathUtil.areOpposite(v1, v3));
	}

	// -- Computational Geometry Tests --

	/** Tests the MathUtil.inside() method. */
	public void testInside() {
		final float[] a = { 1f, 2f };
		final float[] b1 = { 0f, 0f };
		final float[] b2 = { 3f, 3f };
		final float[] c = { 1f, -1f };

		assertEquals("a should be between b1 and b2", true, MathUtil.inside(a, b1,
			b2));

		assertEquals("a should be inside b2 and b1", true, MathUtil.inside(a, b2,
			b1));

		assertEquals("c should not be inside b1 and b2", false, MathUtil.inside(c,
			b1, b2));

		assertEquals("c should not be inside b2 and b1", false, MathUtil.inside(c,
			b2, b1));

		assertEquals("b1 should be inside b1 and b2", true, MathUtil.inside(b1, b1,
			b2));
	}

	/** Tests the computePointOnSegment method. */
	public void testSegment() {
		final float[] a = { 2f, 2f };
		final float[] b = { 3f, 4f };

		final float[] p1 = MathUtil.computePtOnSegment(a, b, 0f);
		final float[] p2 = MathUtil.computePtOnSegment(a, b, 1f);
		final float[] p3 = MathUtil.computePtOnSegment(a, b, .5f);

		final float[] p3e = { 2.5f, 3f };

		compareFloats(a, p1, DELTA);
		compareFloats(b, p2, DELTA);
		compareFloats(p3, p3e, DELTA);
	}

	/** Tests the MathUtil.bisect() method. */
	public void testBisect() {
		final float[] p1 = { 0f, 1f };
		final float[] p2 = { 0f, 0f };
		final float[] p3 = { 1f, 0f };

		final float[] p4Expected = MathUtil.unit(new float[] { -1f, -1f });
		final float[] p4 = MathUtil.getRightBisectorVector2D(p1, p2, p3);

		assertEquals(p4Expected.length, p4.length);
		for (int i = 0; i < p4.length; i++) {
			assertEquals(p4Expected[i], p4[i], DELTA);
		}
	}

	/** Tests the MathUtil.bisect() method. */
	public void testRightBisectFlatCase() {
		final float[] p1 = { 0f, 0f };
		final float[] p2 = { 0f, 1f };
		final float[] p3 = { 0f, 2f };

		final float[] vExpected = { 1f, 0f };
		float[] v = MathUtil.getRightBisectorVector2D(p1, p2, p3);

		assertEquals(vExpected.length, v.length);
		compareFloats(vExpected, v, DELTA);

		final float[] p4 = { 1f, .2000000f };
		final float[] p5 = { 1f, .2000001f };
		final float[] p6 = { 1f, .20000015f };
		final float[] vE2 = { 1f, 0f };
		v = MathUtil.getRightBisectorVector2D(p4, p5, p6);
		assertEquals(vE2.length, v.length);
		compareFloats(vE2, v, DELTA);
	}

	/** Tests the getRightPerpendicularVector2D() method. */
	public void testGetRightPerp() {
		final float[] p1 = { 0f, 0f };
		final float[] p2 = { 0f, 1f };
		final float[] p3 = { 1f, 0f };
		final float[] p4 = { 1f, 1f };

		final float[] v1e = { 1f, 0f };
		final float[] v2e = { 0f, -1f };
		final float[] v3e = MathUtil.unit(new float[] { 1f, -1f });
		final float[] v4e = MathUtil.unit(new float[] { -1f, 1f });

		final float[] v1 = MathUtil.getRightPerpendicularVector2D(p2, p1);
		final float[] v2 = MathUtil.getRightPerpendicularVector2D(p3, p1);
		final float[] v3 = MathUtil.getRightPerpendicularVector2D(p4, p1);
		final float[] v4 = MathUtil.getRightPerpendicularVector2D(p1, p4);

		compareFloats(v1e, v1, DELTA);
		compareFloats(v2e, v2, DELTA);
		compareFloats(v3e, v3, DELTA);
		compareFloats(v4e, v4, DELTA);
	}

	/** Tests the orient2D method. */
	public void testOrient() {
		final float[] p1 = { 0f, 0f };
		final float[] p2 = { 0f, 1f };
		final float[] p3 = { 1f, 0f };
		final float[] p4 = { 1f, 1f };

		final float z1e = -1f;
		final float z2e = -1f;
		final float z1 = MathUtil.orient2D(p1, p2, p3);
		final float z2 = MathUtil.orient2D(p1, p2, p4);

		assertEquals(z1e, z1, DELTA);
		assertEquals(z2e, z2, DELTA);
	}

	// -- Helper Methods --

	/** Compares two arrays of floats, item-wise. */
	public void compareFloats(final float[] expected, final float[] actual,
		final float delta)
	{
		for (int i = 0; i < expected.length; i++) {
			assertEquals(expected[i], actual[i], delta);
		}
	}

	/** Prints an array of floats. */
	public void print(final float[] a) {
		for (int i = 0; i < a.length; i++) {
			System.out.print(a[i] + " ");
		}
		System.out.println();
	}
}

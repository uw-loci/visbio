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

package loci.visbio.utests.overlays;

import junit.framework.TestCase;
import loci.visbio.overlays.OverlayFreeform;
import loci.visbio.overlays.OverlayTransform;
import loci.visbio.overlays.OverlayUtil;

/**
 * Test some critical methods of the OverlayNodedObject class.
 */
public class OverlayNodedObjectTest extends TestCase {

	/** Tolerance for comparing floats. */
	public static final float DELTA = 0f;

	/** Overlay transform for test overlays. */
	protected OverlayTransform ot;

	/** Different testing OverlayNodedObjects. */
	protected OverlayFreeform f, g, h;

	/** A set of nodes. */
	protected static final float[][] NODES = { { 0f, 1f, 2f }, { 0f, 0f, 0f } };

	/** Another set of nodes. */
	protected static final float[][] NODES2 = { { 0f, 1f, 2f, 1f, 0f },
		{ 0f, 0f, 0f, 0f, 0f } };

	/** This method runs before each test. */
	@Override
	public void setUp() {
		final DummyImageTransform it = new DummyImageTransform(null, "image");
		ot = new DummyOverlayTransform(it, "Howdy");
		f = new OverlayFreeform(ot, NODES);
		g = new OverlayFreeform(ot, new float[][] { { 0f }, { 0f } });
		h = new OverlayFreeform(ot, NODES2);
	}

	/** This method runs after each test. */
	@Override
	public void tearDown() {}

	// -- Insertion Tests --

	// GLOSSARY: ONO = OverlayNodedObject

	/** Tests inserting a node somewhere in the middle of an ONO's node array. */
	public void testInsertInterior() {
		final float[] c = { 5f, 5f };
		final float[][] nodesExpected = { { 0f, 5f, 1f, 2f }, { 0f, 5f, 0f, 0f } };
		f.insertNode(1, c[0], c[1], false);
		final float[][] nodes = f.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	/**
	 * Tests inserting a node somewhere in the middle of an ONO's node array that
	 * is colocational with the next node in the array. The insert method should
	 * detect this and fail to insert the node.
	 */
	public void testInsertCoInterior() {
		// insert should fail since nodes are colocational
		final float[] c = { 1f, 0f };
		f.insertNode(1, c[0], c[1], false);
		final float[][] nodes = f.getNodes();
		compareNodes(NODES, nodes);
	}

	/**
	 * Tests inserting a node somewhere in the middle of an ONO's node array that
	 * is colocational with the previous node in the array. The insert method
	 * should detect this and fail to insert the node.
	 */
	public void testInsertCoInterior2() {
		// insert should fail since nodes are colocational
		final float[] c = { 0f, 0f };
		f.insertNode(1, c[0], c[1], false);
		final float[][] nodes = f.getNodes();
		compareNodes(NODES, nodes);
	}

	/**
	 * Tests inserting a node just before the last node of an ONO's node array. In
	 * this case, the node is colocational with the previous node in the array.
	 * Basically redundant with previous two test cases. Insertion should fail.
	 */
	public void testInsertCoPenultimate() {
		final float[] c = { 1f, 0f };
		f.insertNode(2, c[0], c[1], false);
		final float[][] nodes = f.getNodes();
		compareNodes(NODES, nodes);
	}

	/** Tests inserting a node a the beginning of an ONO's node array. */
	public void testInsertFront() {
		final float[] c = { 5f, 5f };
		final float[][] nodesExpected = { { 5f, 0f, 1f, 2f }, { 5f, 0f, 0f, 0f } };
		f.insertNode(0, c[0], c[1], false);
		final float[][] nodes = f.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	/**
	 * Tests inserting a node at the beginning of an ONO's node array,
	 * colocational with the array's first node. The insertion method should
	 * detect this and fail to insert the node.
	 */
	public void testInsertCoFront() {
		final float[] c = { 0f, 0f };
		f.insertNode(0, c[0], c[1], false);
		final float[][] nodes = f.getNodes();
		compareNodes(NODES, nodes);
	}

	/**
	 * Tests inserting a node at the back of an ONO's node array. This operation,
	 * as currently implemented, should fail. There is a separate "setNextNode"
	 * method for this task.
	 */
	public void testInsertBack() {
		// should fail--can't insert past last node
		final float[] c = { 5f, 5f };
		f.insertNode(3, c[0], c[1], false);
		final float[][] nodes = f.getNodes();
		compareNodes(NODES, nodes);
	}

	/**
	 * Tests inserting a node just before the last node of an ONO's node array.
	 * (This operation should succeed.)
	 */
	public void testInsertPenultimate() {
		final float[] c = { 5f, 5f };
		final float[][] nodesExpected = { { 0f, 1f, 5f, 2f }, { 0f, 0f, 5f, 0f } };
		f.insertNode(2, c[0], c[1], false);
		final float[][] nodes = f.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	/**
	 * Tests inserting a node before the first node of a one-node node array.
	 * (This operation should succeed.)
	 */
	public void testInsertOne() {
		final float[] c = { 5f, 5f };
		final float[][] nodesExpected = { { 5f, 0f }, { 5f, 0f } };
		g.insertNode(0, c[0], c[1], false);
		final float[][] nodes = g.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	// -- Deletion tests --

	/** Tests deleting the first node of an ONO's node array. */
	public void testDeleteFirst() {
		final float[][] nodesExpected = { { 1f, 2f }, { 0f, 0f } };
		f.deleteNode(0);
		final float[][] nodes = f.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	/** Tests deleting the last node of an ONO's node array. */
	public void testDeleteLast() {
		final float[][] nodesExpected = { { 0f, 1f }, { 0f, 0f } };
		f.deleteNode(2);
		final float[][] nodes = f.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	/** Tests deleting a node somewhere in the middle of an ONO's node array. */
	public void testDeleteInterior() {
		final float[][] nodesExpected = { { 0f, 2f }, { 0f, 0f } };
		f.deleteNode(1);
		final float[][] nodes = f.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	/**
	 * Tests deleting a node somewhere in the middle of an ONO's node array such
	 * that two colocational nodes become adjacent. The deletion method should
	 * detect this and cull one of the colocational nodes.
	 */
	public void testDeleteCoInterior() {
		final float[][] nodesExpected = { { 0f, 1f, 0f }, { 0f, 0f, 0f } };
		h.deleteNode(2);
		final float[][] nodes = h.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	/**
	 * Tests deleting a range of nodes in an ONO's node array.
	 */
	public void testDeleteBetween() {
		final float[][] nodesExpected = { { 0f, 2f }, { 0f, 0f } };
		f.deleteBetween(0, 2);
		final float[][] nodes = f.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	/**
	 * Tests deleting an adjacent pair of nodes in an ONO's node array. This
	 * operation should not change the node array.
	 */
	public void testDeleteBetweenAdjacent() {
		f.deleteBetween(0, 1);
		final float[][] nodes = f.getNodes();
		compareNodes(NODES, nodes);
	}

	/**
	 * Tests deleting between a range of nodes in an ONO's node array such that
	 * two colocational nodes become adjacent. The deletion method should detect
	 * this an cull one of the colocational nodes.
	 */
	public void testDeleteBetweenCo() {
		final float[][] nodesExpected = { { 0f, 1f, 0f }, { 0f, 0f, 0f } };
		h.deleteBetween(1, 3);
		final float[][] nodes = h.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	// -- Connect Freeforms Tests --

	/** Tests whether two freeforms connect correctly. */
	public void testConnect() {
		final OverlayFreeform f3 = f.connectTo(h);
		final float[][] nodesExpected = OverlayUtil.adjoin(NODES, NODES2);
		final float[][] nodes = f3.getNodes();
		compareNodes(nodesExpected, nodes);
	}

	/**
	 * Tests whether two freeforms connect correctly when the head of the first
	 * freefom and the tail of the second freeform are located at the same point.
	 * The connectTo method should detect this and prune one of these colocational
	 * points from the node array of the resulting freeform.
	 */
	public void testConnectFreeformsWithColocationalTailAndHead() {
		final float[][] nodes1 = { { 0f, 1f }, { 0f, 1f } };
		final float[][] nodes2 = { { 1f, 2f }, { 1f, 2f } };
		final float[][] nodesExpected = { { 0f, 1f, 2f }, { 0f, 1f, 2f } };
		final OverlayFreeform f1 = new OverlayFreeform(ot, nodes1);
		final OverlayFreeform f2 = new OverlayFreeform(ot, nodes2);
		final OverlayFreeform f3 = f1.connectTo(f2);
		final float[][] nodes3 = f3.getNodes();
		compareNodes(nodesExpected, nodes3);
	}

	// -- Helpers --

	/**
	 * Compares values of floats in two arrays of points subject to the static
	 * member DELTA of this class (probably equal to 0.0f).
	 */
	public void compareNodes(final float[][] e, final float[][] a) {
		compareNodes(e, a, DELTA);
	}

	/**
	 * Compares values of floats in two arrays of points. Asserts equality of
	 * floats at each 2-D index of the arrays. Also asserts that the arrays have
	 * the same dimensions.
	 * 
	 * @param e the expected array of floats
	 * @param a the actual array of floats
	 * @param delta the tolerance to pass to the assertEquals method. Presumably
	 *          floats that differ by an amount less than this tolerance are
	 *          considered equal.
	 */
	public void compareNodes(final float[][] e, final float[][] a,
		final float delta)
	{
		assertEquals(e.length, a.length);
		for (int i = 0; i < e.length; i++) {
			assertEquals(e[0].length, a[0].length);
		}
		for (int i = 0; i < e.length; i++) {
			for (int j = 0; j < e[0].length; j++) {
				assertEquals(e[i][j], a[i][j], delta);
			}
		}
	}

	/** Prints an array of points--useful for debugging. */
	public void printNodes(final float[][] n) {
		for (int j = 0; j < n[0].length; j++) {
			System.out.println(j + ": [" + n[0][j] + "," + n[1][j] + "]");
		}
	}

}

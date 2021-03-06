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

package loci.visbio.view;

import loci.visbio.state.Dynamic;
import loci.visbio.state.SaveException;
import loci.visbio.state.Saveable;
import loci.visbio.util.ObjectUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

/**
 * DisplayPosition represents an orientation of VisAD display.
 */
public class DisplayPosition implements Dynamic, Saveable {

	// -- Fields --

	/** Name of the group. */
	private String name;

	/** Matrix representing the position. */
	private double[] matrix;

	// -- Constructor --

	/** Constructs an uninitialized display position. */
	public DisplayPosition() {}

	/** Constructs a display position. */
	public DisplayPosition(final String name, final double[] matrix) {
		this.name = name;
		this.matrix = matrix;
	}

	// -- DisplayPosition API methods --

	/** Gets the position's string representation (name). */
	@Override
	public String toString() {
		return name;
	}

	/** Gets the positions's name. */
	public String getName() {
		return name;
	}

	/** Gets the position's description. */
	public double[] getMatrix() {
		return matrix;
	}

	// -- Dynamic API methods --

	/** Tests whether two dynamic objects have matching states. */
	@Override
	public boolean matches(final Dynamic dyn) {
		if (!isCompatible(dyn)) return false;
		final DisplayPosition position = (DisplayPosition) dyn;

		return ObjectUtil.objectsEqual(name, position.name) &&
			ObjectUtil.arraysEqual(matrix, position.matrix);
	}

	/**
	 * Tests whether the given dynamic object can be used as an argument to
	 * initState, for initializing this dynamic object.
	 */
	@Override
	public boolean isCompatible(final Dynamic dyn) {
		return dyn instanceof DisplayPosition;
	}

	/** Modifies this object's state to match that of the given object. */
	@Override
	public void initState(final Dynamic dyn) {
		if (dyn != null && !isCompatible(dyn)) return;
		final DisplayPosition position = (DisplayPosition) dyn;

		if (position != null) {
			name = position.name;
			matrix = position.matrix;
		}
	}

	/**
	 * Called when this object is being discarded in favor of another object with
	 * a matching state.
	 */
	@Override
	public void discard() {}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("Capture"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "DisplayPosition");
		child.setAttribute("name", name);
		child.setAttribute("matrix", ObjectUtil.arrayToString(matrix));
	}

	/**
	 * Restores the current state from the given DOM element ("DisplayPosition").
	 */
	@Override
	public void restoreState(final Element el) throws SaveException {
		name = el.getAttribute("name");
		matrix = ObjectUtil.stringToDoubleArray(el.getAttribute("matrix"));
	}

}

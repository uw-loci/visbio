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

package loci.visbio.state;

import java.awt.Component;

import org.w3c.dom.Element;

/**
 * BioOption represents an option in the VisBio Options dialog.
 */
public abstract class BioOption implements Saveable {

	// -- Fields --

	/** String identifying this option. */
	protected String text;

	// -- Constructor --

	/** Constructs a new option. */
	public BioOption(final String text) {
		this.text = text;
	}

	// -- BioOption API methods --

	/** Gets text identifying this option. */
	public String getText() {
		return text;
	}

	/** Gets a GUI component representing this option. */
	public abstract Component getComponent();

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("Options"). */
	@Override
	public void saveState(final Element el) throws SaveException {}

	/** Restores the current state from the given DOM element ("Options"). */
	@Override
	public void restoreState(final Element el) throws SaveException {}

}

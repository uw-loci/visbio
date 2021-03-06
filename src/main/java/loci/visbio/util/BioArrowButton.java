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

import java.awt.Dimension;

import javax.swing.plaf.basic.BasicArrowButton;

/**
 * BioArrowButton is a simple arrow button of fixed size.
 */
public class BioArrowButton extends BasicArrowButton {

	// -- Constants --

	/** Default size for arrow buttons. */
	protected static final int BUTTON_SIZE = 24;

	// -- Fields --

	/** Size of the arrow button in pixels. */
	protected Dimension size;

	// -- Constructors --

	/** Constructs a new arrow button of default size. */
	public BioArrowButton(final int dir) {
		this(dir, BUTTON_SIZE);
	}

	/** Constructs a new slider widget. */
	public BioArrowButton(final int dir, final int size) {
		super(dir);
		this.size = new Dimension(size, size);
		setMinimumSize(this.size);
		setPreferredSize(this.size);
		setMaximumSize(this.size);
	}

}

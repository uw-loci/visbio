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

package loci.visbio.data;

import java.util.EventObject;

/**
 * TransformEvent is the event generated when a data transform's parameters are
 * updated.
 */
public class TransformEvent extends EventObject {

	// -- Constants --

	/** Transform event indicating transform's data has changed. */
	public static final int DATA_CHANGED = 1;

	/** Transform event indicating transform's font has changed. */
	public static final int FONT_CHANGED = 2;

	/** Transform event indicating transform is being deleted. */
	public static final int DATA_REMOVED = 3;

	// -- Fields --

	/** The type of transform event. */
	protected int id;

	// -- Constructors --

	/** Constructs a new transform event indicating data has changed. */
	public TransformEvent(final Object source) {
		this(source, DATA_CHANGED);
	}

	/** Constructs a new data event. */
	public TransformEvent(final Object source, final int id) {
		super(source);
		this.id = id;
	}

	// -- TransformEvent API methods --

	/**
	 * Gets the type of transform event. Possibilities include: <li>
	 * TransformEvent.DATA_CHANGED <li>TransformEvent.FONT_CHANGED
	 */
	public int getId() {
		return id;
	}

}

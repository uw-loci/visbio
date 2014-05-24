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

package loci.visbio.help;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * HelpTopic is a tree node representing a help topic.
 */
public class HelpTopic extends DefaultMutableTreeNode {

	// -- Fields --

	/** Content source for this help topic. */
	private final String source;

	// -- Constructor --

	/** Creates a VisBio help topic. */
	public HelpTopic(final String name, final String source) {
		super(name);
		this.source = source;
	}

	// -- HelpWindow API methods --

	/** Gets the name associated with this help topic. */
	public String getName() {
		return (String) getUserObject();
	}

	/** Gets the content source for this help topic. */
	public String getSource() {
		return source;
	}

}

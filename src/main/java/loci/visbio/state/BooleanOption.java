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

import javax.swing.JCheckBox;

import loci.visbio.util.LAFUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

/**
 * BooleanOption is a true-or-false option in the VisBio Options dialog.
 */
public class BooleanOption extends BioOption {

	// -- Fields --

	/** Check box GUI component. */
	private final JCheckBox box;

	// -- Constructor --

	/** Constructs a new option. */
	public BooleanOption(final String text, final char mnemonic,
		final String tip, final boolean value)
	{
		super(text);
		box = new JCheckBox(text, value);
		if (!LAFUtil.isMacLookAndFeel()) box.setMnemonic(mnemonic);
		box.setToolTipText(tip);
	}

	// -- BooleanOption API methods --

	/** Gets this option's current setting. */
	public boolean getValue() {
		return box.isSelected();
	}

	// -- BioOption API methods --

	/** Gets a GUI component representing this option. */
	@Override
	public Component getComponent() {
		return box;
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("Options"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element e = XMLUtil.createChild(el, "Boolean");
		e.setAttribute("name", text);
		e.setAttribute("value", box.isSelected() ? "true" : "false");
	}

	/** Restores the current state from the given DOM element ("Options"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final Element[] e = XMLUtil.getChildren(el, "Boolean");
		for (int i = 0; i < e.length; i++) {
			final String name = e[i].getAttribute("name");
			if (!name.equals(text)) continue;
			final boolean value = e[i].getAttribute("value").equalsIgnoreCase("true");
			box.setSelected(value);
			break;
		}
	}

}

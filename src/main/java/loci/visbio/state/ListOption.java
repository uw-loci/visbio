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

import javax.swing.JPanel;

import loci.visbio.util.BioComboBox;
import loci.visbio.util.FormsUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

/**
 * ListOption is an option from a list in the VisBio Options dialog.
 */
public class ListOption extends BioOption {

	// -- Fields --

	/** Panel containing GUI components. */
	private final JPanel panel;

	/** Text box GUI component. */
	private final BioComboBox box;

	// -- Constructor --

	/** Constructs a new option. */
	public ListOption(final String text, final String tip, final String[] choices)
	{
		super(text);

		// combo box
		box = new BioComboBox(choices);
		box.setToolTipText(tip);
		box.setSelectedIndex(0);

		// lay out components
		panel = FormsUtil.makeRow(text, box);
	}

	// -- ListOption API methods --

	/** Gets this option's current setting. */
	public String getValue() {
		return (String) box.getSelectedItem();
	}

	// -- BioOption API methods --

	/** Gets a GUI component representing this option. */
	@Override
	public Component getComponent() {
		return panel;
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("Options"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element e = XMLUtil.createChild(el, "List");
		e.setAttribute("name", text);
		e.setAttribute("value", getValue());
	}

	/** Restores the current state from the given DOM element ("Options"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final Element[] e = XMLUtil.getChildren(el, "List");
		for (int i = 0; i < e.length; i++) {
			final String name = e[i].getAttribute("name");
			if (!name.equals(text)) continue;
			final String value = e[i].getAttribute("value");
			box.setSelectedItem(value);
			break;
		}
	}

}

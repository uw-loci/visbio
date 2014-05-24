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

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import loci.visbio.util.FormsUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

/**
 * ResolutionOption is an option for changing image resolution (two positive
 * integers) in the VisBio Options dialog.
 */
public class ResolutionOption extends BioOption {

	// -- Fields --

	/** Panel containing GUI components. */
	private final JPanel panel;

	/** X resolution text field GUI component. */
	private final JTextField resX;

	/** Y resolution text field GUI component. */
	private final JTextField resY;

	// -- Constructor --

	/** Constructs a new option. */
	public ResolutionOption(final String text, final String tip,
		final int valueX, final int valueY)
	{
		super(text);

		// X resolution text field
		resX = new JTextField(4);
		resX.setText("" + valueX);
		resX.setToolTipText(tip);

		// Y resolution text field
		resY = new JTextField(4);
		resY.setText("" + valueY);
		resY.setToolTipText(tip);

		// lay out components
		panel =
			FormsUtil
				.makeRow(new Object[] { text + ":", resX, new JLabel("x"), resY });
	}

	// -- ResolutionOption API methods --

	/** Gets this option's current X resolution. */
	public int getValueX() {
		int valueX;
		try {
			valueX = Integer.parseInt(resX.getText());
		}
		catch (final NumberFormatException exc) {
			valueX = -1;
		}
		return valueX;
	}

	/** Gets this option's current Y resolution. */
	public int getValueY() {
		int valueY;
		try {
			valueY = Integer.parseInt(resY.getText());
		}
		catch (final NumberFormatException exc) {
			valueY = -1;
		}
		return valueY;
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
		final Element e = XMLUtil.createChild(el, "Resolution");
		e.setAttribute("name", text);
		e.setAttribute("resX", "" + getValueX());
		e.setAttribute("resY", "" + getValueY());
	}

	/** Restores the current state from the given DOM element ("Options"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final Element[] e = XMLUtil.getChildren(el, "Resolution");
		for (int i = 0; i < e.length; i++) {
			final String name = e[i].getAttribute("name");
			if (!name.equals(text)) continue;
			final String valueX = e[i].getAttribute("resX");
			resX.setText(valueX);
			final String valueY = e[i].getAttribute("resY");
			resY.setText(valueY);
			break;
		}
	}

}

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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextField;

import loci.visbio.overlays.SpreadsheetLaunchException;
import loci.visbio.overlays.SpreadsheetLauncher;
import loci.visbio.util.LAFUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

/**
 * Option to toggle whether spreadsheet automatically launches when overlays are
 * exported. Also allows user to specify path to spreadsheet application.
 */
public class SpreadsheetLaunchOption extends BioOption implements
	ActionListener
{

	// -- Fields --

	/** Text Field containing user's text. */
	private final JTextField textField;

	/** Combined text field and label component. */
	private final JPanel component;

	/** Should the spreadsheet launch automatcially? */
	private final JCheckBox box;

	/** Button to restore default path. */
	private final JButton button;

	// -- Constructor --

	/** Constructs a new option. */
	public SpreadsheetLaunchOption(final char mnemonic, final String textValue,
		final boolean boxValue)
	{
		super(SpreadsheetOptionStrategy.getText());

		box = new JCheckBox(SpreadsheetOptionStrategy.getText(), boxValue);
		if (!LAFUtil.isMacLookAndFeel()) box.setMnemonic(mnemonic);
		box.setToolTipText(SpreadsheetOptionStrategy.getBoxTip());
		box.addActionListener(this);
		box.setActionCommand("setSelected");

		textField = new JTextField(textValue, Math.max(textValue.length(), 25));
		textField.setToolTipText(SpreadsheetOptionStrategy.getTextTip());
		textField.setEnabled(box.isSelected());

		button = new JButton("Restore default path");
		button.setToolTipText(SpreadsheetOptionStrategy.getButtonTip());
		button.addActionListener(this);
		button.setActionCommand("restoreDefaultPath");
		button.setEnabled(box.isSelected());

		component =
			makePanelFrom(SpreadsheetOptionStrategy.getLabel(), textField, box);
	}

	// -- SpreadsheetLaunchOption API methods --

	/** Gets this option's current setting. */
	public String getValue() {
		return textField.getText();
	}

	/** Gets whether the checkbox is selected. */
	public boolean getSelected() {
		return box.isSelected();
	}

	// -- BioOption API methods --

	/** Gets a GUI component representing this option. */
	@Override
	public Component getComponent() {
		return component;
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("Options"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element e = XMLUtil.createChild(el, "String");
		e.setAttribute("name", text);
		e.setAttribute("value", textField.getText());
		e.setAttribute("selected", box.isSelected() ? "true" : "false");
	}

	/** Restores the current state from the given DOM element ("Options"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final Element[] e = XMLUtil.getChildren(el, "String");
		for (int i = 0; i < e.length; i++) {
			final String name = e[i].getAttribute("name");
			if (!name.equals(text)) continue;
			final String value = e[i].getAttribute("value");
			final boolean selected =
				e[i].getAttribute("selected").equalsIgnoreCase("true");
			textField.setText(value);
			box.setSelected(selected);
			break;
		}
	}

	// -- ActionListener Interface methods --

	/** Responds to changes in checkbox state. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getActionCommand().equals("setSelected")) {
			textField.setEnabled(box.isSelected());
			button.setEnabled(box.isSelected());
		}
		else if (e.getActionCommand().equals("restoreDefaultPath")) {
			String s = "";
			try {
				s = SpreadsheetLauncher.getDefaultApplicationPath();
			}
			catch (final SpreadsheetLaunchException ex) {}
			textField.setText(s);
		}
	}

	// -- Helper Methods --

	/** Constructs a JPanel containing a label and a JTextField */
	private JPanel makePanelFrom(final String label, final JTextField field,
		final JCheckBox bx)
	{
		final FormLayout fl =
			new FormLayout("pref, 3dlu, pref ",
				"3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu");

		final PanelBuilder builder = new PanelBuilder(fl);
		final CellConstraints cc = new CellConstraints();

		builder.add(bx, cc.xyw(1, 2, 3));
		builder.addLabel(label, cc.xy(1, 4));
		builder.add(field, cc.xy(3, 4));
		builder.add(button, cc.xy(1, 6));

		return builder.getPanel();
	}
}

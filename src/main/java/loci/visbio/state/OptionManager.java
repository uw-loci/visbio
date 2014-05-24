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

import com.jgoodies.looks.LookUtils;

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Vector;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import loci.visbio.ExitManager;
import loci.visbio.LogicManager;
import loci.visbio.VisBioEvent;
import loci.visbio.VisBioFrame;
import loci.visbio.util.DialogPane;
import loci.visbio.util.MessagePane;
import loci.visbio.util.WarningPane;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * OptionManager is the manager encapsulating VisBio's options.
 */
public class OptionManager extends LogicManager {

	// -- Constants --

	/** Configuration file for storing VisBio options. */
	private static final File CONFIG_FILE = new File("visbio.ini");

	// -- Fields --

	/** Option pane. */
	private final OptionPane options;

	/** List of options. */
	private final Vector list;

	// -- Constructor --

	/** Constructs an options manager. */
	public OptionManager(final VisBioFrame bio) {
		super(bio);
		options = new OptionPane(this);
		list = new Vector();

		// HACK - ensure tabs appear in the right order
		options.addTab("General");
		options.addTab("Thumbnails");
		options.addTab("Warnings");
		options.addTab("Debug");
	}

	// -- OptionManager API methods --

	/** Adds an option allowing the user to toggle a check box. */
	public BooleanOption addBooleanOption(final String tab, final String text,
		final char mnemonic, final String tip, final boolean value)
	{
		final BooleanOption option = new BooleanOption(text, mnemonic, tip, value);
		addOption(tab, option);
		return option;
	}

	/** Adds an option allowing the user to enter a numerical value. */
	public NumericOption addNumericOption(final String tab, final String text,
		final String unit, final String tip, final int value)
	{
		final NumericOption option = new NumericOption(text, unit, tip, value);
		addOption(tab, option);
		return option;
	}

	/** Adds an option allowing the user to enter a numerical value. */
	public NumericOption addNumericOption(final String tab, final String text,
		final String unit, final String tip, final double value)
	{
		final NumericOption option = new NumericOption(text, unit, tip, value);
		addOption(tab, option);
		return option;
	}

	/** Adds an option allowing the user to enter a string. */
	public StringOption addStringOption(final String tab, final String text,
		final String tip, final String value, final String label)
	{
		final StringOption option = new StringOption(text, tip, value, label);
		addOption(tab, option);
		return option;
	}

	/** Adds an option allowing the user to select from a dropdown list. */
	public ListOption addListOption(final String tab, final String text,
		final String tip, final String[] choices)
	{
		final ListOption option = new ListOption(text, tip, choices);
		addOption(tab, option);
		return option;
	}

	/**
	 * Adds a custom GUI component to VisBio's options dialog. Such options will
	 * not be saved in the INI file automatically.
	 */
	public CustomOption addCustomOption(final String tab, final Component c) {
		final CustomOption option = new CustomOption(c);
		addOption(tab, option);
		return option;
	}

	/** Adds an option to VisBio's options dialog. */
	public void addOption(final String tab, final BioOption option) {
		// HACK - do not show Overlays-related options in option dialog
		if (!tab.equals("Overlays")) options.addOption(tab, option);
		list.add(option);
		bio.generateEvent(this, "add option", false);
	}

	/** Gets the VisBio option with the given text. */
	public BioOption getOption(final String text) {
		for (int i = 0; i < list.size(); i++) {
			final BioOption option = (BioOption) list.elementAt(i);
			if (option.getText().equals(text)) return option;
		}
		return null;
	}

	/** Reads in configuration from configuration file. */
	public void readIni() {
		if (!CONFIG_FILE.exists()) return;
		try {
			final Document doc = XMLUtil.parseXML(CONFIG_FILE);
			final Element el = doc == null ? null : doc.getDocumentElement();
			if (el != null) restoreState(el);
			bio.generateEvent(this, "read ini file", false);
		}
		catch (final SaveException exc) {
			exc.printStackTrace();
		}
	}

	/** Writes out configuration to configuration file. */
	public void writeIni() {
		final Document doc = XMLUtil.createDocument("VisBio");
		try {
			saveState(doc.getDocumentElement());
		}
		catch (final SaveException exc) {
			exc.printStackTrace();
		}
		XMLUtil.writeXML(CONFIG_FILE, doc);
	}

	/**
	 * Checks whether to display a message using the given panel, and does so if
	 * necessary. Message is displayed if the option corresponding to the
	 * specified text (opt) is enabled.
	 */
	public boolean checkMessage(final Component parent, final String opt,
		final boolean allowCancel, final JPanel panel, final String title)
	{
		return checkMessage(parent, opt, new MessagePane(title, panel, allowCancel));
	}

	/**
	 * Checks whether to display a warning with the given text, and does so if
	 * necessary. Warning is displayed if the option corresponding to the
	 * specified text (opt) is enabled.
	 */
	public boolean checkWarning(final Component parent, final String opt,
		final boolean allowCancel, final String text)
	{
		return checkMessage(parent, opt, new WarningPane(text, allowCancel));
	}

	/**
	 * Checks whether to display a message using the given message pane, and does
	 * so if necessary. Message is displayed if the option corresponding to the
	 * specified text (opt) is enabled.
	 */
	public boolean checkMessage(final Component parent, final String opt,
		final MessagePane pane)
	{
		final BioOption option = getOption(opt);
		if (!(option instanceof BooleanOption)) return true;
		final BooleanOption alwaysOption = (BooleanOption) option;
		if (!alwaysOption.getValue()) return true;
		final JCheckBox alwaysBox = (JCheckBox) alwaysOption.getComponent();

		final boolean success =
			pane.showDialog(parent) == DialogPane.APPROVE_OPTION;

		final boolean always = pane.isAlwaysDisplayed();
		if (alwaysBox.isSelected() != always) {
			alwaysBox.setSelected(always);
			writeIni();
		}

		return success;
	}

	// -- LogicManager API methods --

	/** Called to notify the logic manager of a VisBio event. */
	@Override
	public void doEvent(final VisBioEvent evt) {
		final int eventType = evt.getEventType();
		if (eventType == VisBioEvent.LOGIC_ADDED) {
			final Object src = evt.getSource();
			if (src == this) doGUI();
			else if (src instanceof ExitManager) {
				// HACK - make options menu item appear in the proper location

				if (!LookUtils.IS_OS_MAC) {
					// file menu
					bio.addMenuSeparator("File");
					bio.addMenuItem("File", "Options...",
						"loci.visbio.state.OptionManager.fileOptions", 'o');
					bio.setMenuShortcut("File", "Options...", KeyEvent.VK_P);
				}
			}
		}
	}

	/** Gets the number of tasks required to initialize this logic manager. */
	@Override
	public int getTasks() {
		return 1;
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("VisBio"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element optionsElement = XMLUtil.createChild(el, "Options");
		for (int i = 0; i < list.size(); i++) {
			final BioOption option = (BioOption) list.elementAt(i);
			option.saveState(optionsElement);
		}
	}

	/** Restores the current state from the given DOM element ("VisBio"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final Element optionsElement = XMLUtil.getFirstChild(el, "Options");
		if (optionsElement != null) {
			for (int i = 0; i < list.size(); i++) {
				final BioOption option = (BioOption) list.elementAt(i);
				option.restoreState(optionsElement);
			}
		}
	}

	// -- Helper methods --

	/** Adds options-related GUI components to VisBio. */
	private void doGUI() {
		// options menu
		bio.setSplashStatus("Initializing options logic");
	}

	// -- Menu commands --

	/** Brings up the options dialog box. */
	public void fileOptions() {
		readIni();
		final int rval = options.showDialog(bio);
		if (rval == DialogPane.APPROVE_OPTION) {
			writeIni();
			bio.generateEvent(this, "tweak options", true);
		}
		else readIni();
	}

}

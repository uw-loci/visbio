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

package loci.visbio.overlays;

import com.jgoodies.forms.builder.ButtonBarBuilder;
import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import loci.visbio.VisBioFrame;
import loci.visbio.state.BioOption;
import loci.visbio.state.OptionManager;

/**
 * A tabbed pane full of checkboxes.
 */
public class StatsOptionsPane extends JPanel implements ActionListener {

	// -- Constants --

	/** Action command for 'Toggle Current Tab' button. */
	private static final String CURRENT = "current";

	/** Action command for 'Toggle All Tabs' button. */
	private static final String ALL = "all";

	// -- Fields --

	/** All of the check boxes contained in all tabs. */
	protected JCheckBox[][] checkBoxes;

	/** The tabbed pane contained in this object. */
	protected JTabbedPane tabs;

	// -- Constructor --

	/** Creates an StatsOptionsPane object. */
	public StatsOptionsPane() {
		tabs = makeTabs();
		tabs.setPreferredSize(new Dimension(190, 230)); // trial and error
		final JPanel buttons = makeButtons();
		final JPanel pane = makePane(tabs, buttons);
		this.add(pane);
	}

	// -- StatsOptionsPane API methods --

	/** Saves current pane selections to OptionManager. */
	public void saveSettings() {
		final OptionManager om =
			(OptionManager) VisBioFrame.getVisBio().getManager(OptionManager.class);

		final String[] overlayTypes = OverlayUtil.getOverlayTypes();

		for (int type = 0; type < overlayTypes.length; type++) {
			final String[] statTypes = OverlayUtil.getStatTypes(overlayTypes[type]);
			for (int i = 0; i < statTypes.length; i++) {
				final String name = overlayTypes[type] + "." + statTypes[i];
				final BioOption opt = om.getOption(name);
				final JCheckBox optBox = (JCheckBox) opt.getComponent();
				final JCheckBox proxyBox = checkBoxes[type][i];
				optBox.setSelected(proxyBox.isSelected());
			}
		}
	}

	/** Loads current pane selections from OptionManager. */
	public void loadSettings() {
		final OptionManager om =
			(OptionManager) VisBioFrame.getVisBio().getManager(OptionManager.class);
		final String[] overlayTypes = OverlayUtil.getOverlayTypes();

		for (int type = 0; type < overlayTypes.length; type++) {
			final String[] statTypes = OverlayUtil.getStatTypes(overlayTypes[type]);
			for (int i = 0; i < statTypes.length; i++) {
				final String name = overlayTypes[type] + "." + statTypes[i];
				final BioOption opt = om.getOption(name);
				final JCheckBox optBox = (JCheckBox) opt.getComponent();
				checkBoxes[type][i].setSelected(optBox.isSelected());
			}
		}
	}

	// -- ActionListener interface methods --

	/** Change selection state of check boxes depending on button pressed. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		if (ALL.equals(e.getActionCommand())) toggleAllTabs();
		else if (CURRENT.equals(e.getActionCommand())) toggleCurrentTab();
	}

	// -- Helper Methods --

	/** Makes the tabs of this ExportOptionsPane object. */
	private JTabbedPane makeTabs() {
		final OptionManager om =
			(OptionManager) VisBioFrame.getVisBio().getManager(OptionManager.class);

		final String[] overlayTypes = OverlayUtil.getOverlayTypes();
		checkBoxes = new JCheckBox[overlayTypes.length][];

		// populate checkbox array
		for (int type = 0; type < overlayTypes.length; type++) {
			final String[] statTypes = OverlayUtil.getStatTypes(overlayTypes[type]);
			checkBoxes[type] = new JCheckBox[statTypes.length];
			for (int i = 0; i < statTypes.length; i++) {
				checkBoxes[type][i] = new JCheckBox(statTypes[i]);
			}
		}

		this.loadSettings();

		final JTabbedPane jtp = new JTabbedPane();
		// make a tab for each overlay type
		for (int type = 0; type < overlayTypes.length; type++) {
			// build a string representing the row heights for this tab
			String rowString = "3dlu, ";
			for (int i = 0; i < checkBoxes[type].length - 1; i++) {
				rowString += "pref, 3dlu, ";
			}
			rowString += "pref, 3dlu, pref";

			// initialize JGoodies stuff
			final FormLayout fl = new FormLayout("15dlu, pref, 15dlu", rowString);

			final PanelBuilder builder = new PanelBuilder(fl);
			final CellConstraints cc = new CellConstraints();

			// populate panel with the appropriate checkboxes
			for (int i = 0; i < checkBoxes[type].length; i++) {
				builder.add(checkBoxes[type][i], cc.xy(2, 2 * (i + 1)));
			}

			// add a tab to the instance
			final JPanel panel = builder.getPanel();
			jtp.addTab(overlayTypes[type], null, panel, overlayTypes[type] +
				" statistics");
		}

		return jtp;
	}

	/** Makes a button bar with 2 buttons to toggle options. */
	private JPanel makeButtons() {
		final JButton toggleCurrent = new JButton("Toggle Current Tab");
		toggleCurrent.setActionCommand(CURRENT);
		toggleCurrent.addActionListener(this);
		toggleCurrent.setMnemonic('c');
		final JButton toggleAll = new JButton("Toggle All Tabs");
		toggleAll.setActionCommand(ALL);
		toggleAll.addActionListener(this);
		toggleAll.setMnemonic('a');

		final ButtonBarBuilder builder = new ButtonBarBuilder();
		builder.addGridded(toggleCurrent);
		builder.addRelatedGap();
		builder.addGlue();
		builder.addGridded(toggleAll);
		final JPanel panel = builder.getPanel();
		return panel;
	}

	private JPanel makePane(final JTabbedPane jtp, final JPanel buttons) {
		final FormLayout fl =
			new FormLayout("3dlu, pref, 3dlu", "pref, 3dlu, pref, 3dlu, pref");
		final PanelBuilder builder = new PanelBuilder(fl);
		final CellConstraints cc = new CellConstraints();

		builder.addLabel("Select statistics to save/export:", cc.xy(2, 1));
		builder.add(jtp, cc.xy(2, 3));
		builder.add(buttons, cc.xy(2, 5));
		final JPanel panel = builder.getPanel();
		return panel;
	}

	/**
	 * Selects or deselects all checkboxes in current tab. Guesses which way to
	 * toggle based on number of selected check boxes in current tab--aims to
	 * toggle as many check boxes as possible
	 */
	protected void toggleCurrentTab() {
		final int ndx = tabs.getSelectedIndex();
		final JCheckBox[] currentTabCheckBoxes = checkBoxes[ndx];
		int netSelected = 0;
		for (int i = 0; i < currentTabCheckBoxes.length; i++) {
			final JCheckBox box = currentTabCheckBoxes[i];
			if (box.isSelected()) netSelected++;
			else netSelected--;
		}

		final boolean moreSelected = netSelected < 0 ? false : true;

		for (int i = 0; i < currentTabCheckBoxes.length; i++) {
			final JCheckBox box = currentTabCheckBoxes[i];
			box.setSelected(!moreSelected);
		}
	}

	/**
	 * Selects or deselects all checkboxes in current tab. Guesses which way to
	 * toggle based on number of selected check boxes in _current_ tab only,
	 * aiming to toggle as many check boxes as possible in this tab
	 */
	protected void toggleAllTabs() {
		final int ndx = tabs.getSelectedIndex();
		final JCheckBox[] currentTabCheckBoxes = checkBoxes[ndx];
		int netSelected = 0;

		// poll current tab only
		for (int i = 0; i < currentTabCheckBoxes.length; i++) {
			final JCheckBox box = currentTabCheckBoxes[i];
			if (box.isSelected()) netSelected++;
			else netSelected--;
		}

		final boolean moreSelected = netSelected < 0 ? false : true;

		// loop thru all check boxes and toggle.
		for (int type = 0; type < checkBoxes.length; type++) {
			for (int i = 0; i < checkBoxes[type].length; i++) {
				final JCheckBox box = checkBoxes[type][i];
				box.setSelected(!moreSelected);
			}
		}
	}
}

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

import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.EmptyBorder;

import loci.visbio.util.DialogPane;

/**
 * OptionPane provides a dialog box for adjusting VisBio's options.
 */
public class OptionPane extends DialogPane {

	// -- Constants --

	/** Flag indicating options should be divided into separate tabs. */
	protected static final boolean USE_TABS = true;

	// -- Fields --

	/** Options manager. */
	protected OptionManager om;

	/** Tabbed pane. */
	protected JTabbedPane tabs;

	/** Non-tabbed pane. */
	protected JPanel noTabs;

	// -- Constructor --

	/** Creates a new options pane. */
	public OptionPane(final OptionManager om) {
		super("VisBio Options");
		this.om = om;
		if (USE_TABS) {
			tabs = new JTabbedPane();
			add(tabs);
		}
		else {
			noTabs = new JPanel();
			noTabs.setLayout(new BoxLayout(noTabs, BoxLayout.Y_AXIS));
			add(noTabs);
		}
	}

	// -- OptionPane API methods --

	/** Adds an option with a boolean value to the specified tab. */
	public void addOption(final String tab, final BioOption option) {
		final JPanel tabPanel = USE_TABS ? getTab(tab) : noTabs;
		final Component c = option.getComponent();
		if (c instanceof JComponent) {
			final JComponent jc = (JComponent) c;
			jc.setAlignmentX(Component.LEFT_ALIGNMENT);
		}
		tabPanel.add(c);
	}

	/** Initializes a tab with the given name. */
	public void addTab(final String tab) {
		if (USE_TABS) getTab(tab);
	}

	// -- DialogPane API methods --

	/** Resets the dialog pane's components to their default states. */
	@Override
	public void resetComponents() {
		if (USE_TABS) tabs.setSelectedIndex(0);
	}

	// -- Helper methods --

	/** Gets the tab with the given name, initializing it if necessary. */
	protected JPanel getTab(final String tab) {
		int ndx = tabs.indexOfTab(tab);
		if (ndx < 0) {
			final JPanel p = new JPanel();
			p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
			p.setBorder(new EmptyBorder(5, 5, 5, 5));
			tabs.addTab(tab, p);
			ndx = tabs.getTabCount() - 1;
		}
		return (JPanel) tabs.getComponentAt(ndx);
	}
}

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

import com.jgoodies.forms.factories.ButtonBarFactory;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JPanel;

/**
 * WizardPane provides an extensible interface for creating multi-page wizard
 * dialogs with Cancel, Back, Next and Finish buttons.
 */
public class WizardPane extends DialogPane {

	// -- GUI components --

	/** Panels representing the wizard's pages. */
	protected JPanel[] pages;

	/** Wizard's next button. */
	protected JButton next;

	/** Wizard's back button. */
	protected JButton back;

	// -- Other fields --

	/** The current page. */
	protected int page;

	// -- Constructor --

	/** Creates a file series chooser dialog. */
	public WizardPane(final String title) {
		super(title);
		page = -1;
	}

	// -- WizardPane API methods --

	/** Sets wizard's pages to match the given panels. */
	public void setPages(final JPanel[] pages) {
		this.pages = pages;
		for (int i = 0; i < pages.length; i++) {
			add(pages[i]);
			pages[i].setVisible(false);
		}
		setPage(0);
	}

	/** Sets the current page to the given value. */
	public void setPage(final int page) {
		if (this.page == page) return;
		if (this.page >= 0) pages[this.page].setVisible(false);
		try {
			Thread.sleep(500);
		}
		catch (final Exception exc) {}
		pages[page].setVisible(true);
		this.page = page;
		enableButtons();
		repack();
	}

	/** Resizes and centers dialog based on its contents. */
	public void repack() {
		if (dialog == null) return;
		final Point loc = dialog.getLocation();
		final Dimension size = dialog.getSize();
		final Dimension pref = dialog.getPreferredSize();
		int x = loc.x + (size.width - pref.width) / 2;
		int y = loc.y + (size.height - pref.height) / 2;
		if (x < 0) x = 0;
		if (y < 0) y = 0;
		dialog.setBounds(new Rectangle(x, y, pref.width, pref.height));
		dialog.validate();
	}

	/** Enables wizard's buttons. */
	public void enableButtons() {
		back.setEnabled(page > 0);
		final int last = pages.length - 1;
		next.setEnabled(page < last);
		ok.setEnabled(page == last);
		cancel.setEnabled(true);
	}

	/** Disables wizard's buttons. */
	public void disableButtons() {
		back.setEnabled(false);
		next.setEnabled(false);
		ok.setEnabled(false);
		cancel.setEnabled(false);
	}

	// -- DialogPane API methods --

	/** Resets the wizard pane's components to their default states. */
	@Override
	public void resetComponents() {
		setPage(0);
	}

	/** Internal method for creating dialog buttons. */
	@Override
	protected void makeButtons(final boolean doCancel) {
		super.makeButtons(doCancel);

		// back button
		back = new JButton("< Back");
		if (!LAFUtil.isMacLookAndFeel()) back.setMnemonic('b');
		back.setActionCommand("back");
		back.addActionListener(this);
		back.setEnabled(false);

		// next button
		next = new JButton("Next >");
		if (!LAFUtil.isMacLookAndFeel()) next.setMnemonic('n');
		next.setActionCommand("next");
		next.addActionListener(this);
		next.setEnabled(false);

		// finish button
		ok.setText("Finish");
		if (!LAFUtil.isMacLookAndFeel()) ok.setMnemonic('f');
	}

	/** Internal method for doing button layout. */
	@Override
	protected JPanel doButtonLayout() {
		return ButtonBarFactory.buildWizardBar(back, next, ok, cancel);
	}

	// -- ActionListener API methods --

	/** Handles button press events. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final String command = e.getActionCommand();
		if (command.equals("back")) setPage(page - 1);
		else if (command.equals("next")) setPage(page + 1);
		else super.actionPerformed(e);
	}

}

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

package loci.visbio;

import com.jgoodies.looks.LookUtils;

import java.awt.event.KeyEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.WindowConstants;

/**
 * ExitManager is the manager encapsulating VisBio's shutdown logic.
 */
public class ExitManager extends LogicManager implements WindowListener {

	// -- Fields --

	/** Flag indicating shutdown process may continue. */
	private boolean exitOk;

	// -- Constructor --

	/** Constructs an exit manager. */
	public ExitManager(final VisBioFrame bio) {
		super(bio);
	}

	// -- ExitManager API methods --

	/** Trip flag indicating shutdown process should not continue. */
	public void cancelShutdown() {
		exitOk = false;
	}

	// -- LogicManager API methods --

	/** Called to notify the logic manager of a VisBio event. */
	@Override
	public void doEvent(final VisBioEvent evt) {
		final int eventType = evt.getEventType();
		if (eventType == VisBioEvent.LOGIC_ADDED) {
			final LogicManager lm = (LogicManager) evt.getSource();
			if (lm == this) doGUI();
		}
	}

	/** Gets the number of tasks required to initialize this logic manager. */
	@Override
	public int getTasks() {
		return 2;
	}

	// -- Helper methods --

	/** Adds shutdown-related GUI components to VisBio. */
	private void doGUI() {
		// close action
		bio.setSplashStatus("Initializing shutdown logic");
		bio.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		bio.addWindowListener(this);

		// file menu
		bio.setSplashStatus(null);
		if (!LookUtils.IS_OS_MAC) {
			bio.addMenuSeparator("File");
			bio.addMenuItem("File", "Exit", "loci.visbio.ExitManager.fileExit", 'x');
			bio.setMenuShortcut("File", "Exit", KeyEvent.VK_Q);
		}
	}

	// -- WindowListener API methods --

	/** Called when something tries to shut down VisBio. */
	@Override
	public void windowClosing(final WindowEvent e) {
		fileExit();
	}

	/** Unused WindowListener method. */
	@Override
	public void windowActivated(final WindowEvent e) {}

	/** Unused WindowListener method. */
	@Override
	public void windowClosed(final WindowEvent e) {}

	/** Unused WindowListener method. */
	@Override
	public void windowDeactivated(final WindowEvent e) {}

	/** Unused WindowListener method. */
	@Override
	public void windowDeiconified(final WindowEvent e) {}

	/** Unused WindowListener method. */
	@Override
	public void windowIconified(final WindowEvent e) {}

	/** Unused WindowListener method. */
	@Override
	public void windowOpened(final WindowEvent e) {}

	// -- Menu commands --

	/**
	 * Exits the application, allowing all logic managers to take appropriate
	 * actions before doing so.
	 */
	public void fileExit() {
		exitOk = true;
		bio.generateEvent(this, "shutdown request", false);
		if (!exitOk) return;
		bio.generateEvent(this, "shutdown", false);
		bio.destroy();

		// HACK - don't force exit if VisBio was launched from within MATLAB
		final boolean force = loci.visbio.ext.MatlabUtil.getMatlabVersion() == null;
		if (force) System.exit(0);
	}

}

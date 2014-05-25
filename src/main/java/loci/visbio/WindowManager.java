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

import java.awt.Frame;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JFrame;
import javax.swing.JMenuBar;

import loci.visbio.state.BooleanOption;
import loci.visbio.state.OptionManager;
import loci.visbio.state.SaveException;
import loci.visbio.util.Docker;
import loci.visbio.util.SwingUtil;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

/**
 * WindowManager is the manager encapsulating VisBio's window logic, including
 * docking, resizing, minimization and cursor control.
 */
public class WindowManager extends LogicManager implements WindowListener {

	// -- Constants --

	/** String for window docking option. */
	public static final String DOCKING = "Window docking (buggy)";

	/** String for global minimization option. */
	public static final String HIDE_ALL =
		"Hide all windows when main window is minimized";

	// -- Fields --

	/** Table for keeping track of registered windows. */
	protected Hashtable<Window, WindowInfo> windows =
		new Hashtable<Window, WindowInfo>();

	/** List of windows that were visible before VisBio was minimized. */
	protected Vector<Window> visible = new Vector<Window>();

	/** Number of queued wait cursors. */
	protected int waiting = 0;

	/** Object enabling docking between windows. */
	protected Docker docker;

	/** Whether window docking features are enabled. */
	protected boolean docking = false;

	/** Whether minimizing main VisBio window hides all other windows. */
	protected boolean hideAll = false;

	/** Whether to distribute the menu bar across all registered frames. */
	protected boolean distributed;

	// -- Fields - initial state --

	/** Table of window states read during state restore. */
	protected Hashtable<String, WindowState> windowStates =
		new Hashtable<String, WindowState>();

	// -- Constructor --

	/** Constructs a window manager. */
	public WindowManager(final VisBioFrame bio) {
		super(bio);
	}

	// -- WindowManager API methods --

	/** Registers a window with the window manager. */
	public void addWindow(final Window w) {
		addWindow(w, true);
	}

	/**
	 * Registers a window with the window manager. The pack flag indicates that
	 * the window should be packed prior to being shown for the first time.
	 */
	public void addWindow(final Window w, final boolean pack) {
		if (w instanceof Frame) ((Frame) w).setIconImage(bio.getIcon());
		final WindowInfo winfo = new WindowInfo(w, pack);
		final String wname = SwingUtil.getWindowTitle(w);
		final WindowState ws = windowStates.get(wname);
		if (ws != null) {
			winfo.setState(ws);
			windowStates.remove(wname);
		}
		windows.put(w, winfo);
		if (distributed && w instanceof JFrame) {
			final JFrame f = (JFrame) w;
			if (f.getJMenuBar() == null) {
				f.setJMenuBar(SwingUtil.cloneMenuBar(bio.getJMenuBar()));
			}
		}
		docker.addWindow(w);
	}

	/** Removes the window from the window manager and disposes of it. */
	public void disposeWindow(final Window w) {
		if (w == null) return;
		w.setVisible(false);
		final String wname = SwingUtil.getWindowTitle(w);
		windowStates.remove(wname);
		windows.remove(w);
		docker.removeWindow(w);
		w.dispose();
	}

	/** Gets a list of windows being handled by the window manager. */
	public Window[] getWindows() {
		final Enumeration<Window> e = windows.keys();
		final Window[] w = new Window[windows.size()];
		for (int i = 0; i < w.length; i++) {
			w[i] = e.nextElement();
		}
		return w;
	}

	/** Toggles the cursor between hourglass and normal pointer mode. */
	public void setWaitCursor(final boolean wait) {
		boolean doCursor = false;
		if (wait) {
			// set wait cursor
			if (waiting == 0) doCursor = true;
			waiting++;
		}
		else {
			waiting--;
			// set normal cursor
			if (waiting == 0) doCursor = true;
		}
		if (doCursor) {
			// apply cursor to all windows
			final Enumeration<Window> en = windows.keys();
			while (en.hasMoreElements()) {
				final Window w = en.nextElement();
				SwingUtil.setWaitCursor(w, wait);
			}
		}
	}

	/**
	 * Shows the given window. If this is the first time the window has been
	 * shown, or the current window position is off the screen, it is packed and
	 * placed in a cascading position.
	 */
	public void showWindow(final Window w) {
		final WindowInfo winfo = windows.get(w);
		if (winfo == null) return;
		winfo.showWindow();
	}

	/** Hides all windows. */
	public void hideWindows() {
		final Enumeration<Window> en = windows.keys();
		while (en.hasMoreElements()) {
			final Window w = en.nextElement();
			if (w.isVisible() && w != bio) {
				visible.add(w);
				w.setVisible(false);
			}
		}
	}

	/** Restores all previously hidden windows. */
	public void restoreWindows() {
		for (int i = 0; i < visible.size(); i++) {
			final Window w = visible.elementAt(i);
			w.setVisible(true);
		}
		visible.removeAllElements();
		bio.toFront();
	}

	/** Disposes all windows, prior to program exit. */
	public void disposeWindows() {
		final Enumeration<Window> en = windows.keys();
		while (en.hasMoreElements()) {
			final Window w = en.nextElement();
			w.dispose();
		}
	}

	/**
	 * Sets whether all registered frames should have a duplicate of the main
	 * VisBio menu bar. This feature is only here to support the Macintosh screen
	 * menu bar.
	 */
	public void setDistributedMenus(final boolean dist) {
		if (distributed == dist) return;
		distributed = dist;
		doMenuBars(dist ? bio.getJMenuBar() : null);
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
		else if (eventType == VisBioEvent.STATE_CHANGED) {
			final Object src = evt.getSource();
			if (src instanceof OptionManager) {
				final OptionManager om = (OptionManager) src;
// Window docking is too buggy; disable it
//        BooleanOption option = (BooleanOption) om.getOption(DOCKING);
//        if (option != null) setDocking(option.getValue());
				final BooleanOption option = (BooleanOption) om.getOption(HIDE_ALL);
				if (option != null) setHideAll(option.getValue());
			}
		}
	}

	/** Gets the number of tasks required to initialize this logic manager. */
	@Override
	public int getTasks() {
		return 2;
	}

	// -- WindowListener API methods --

	@Override
	public void windowDeiconified(final WindowEvent e) {
		if (hideAll) {
			// program has been restored; show all previously visible windows
			restoreWindows();
		}
	}

	@Override
	public void windowIconified(final WindowEvent e) {
		if (hideAll) {
			// program has been minimized; hide currently visible windows
			hideWindows();
		}
	}

	@Override
	public void windowActivated(final WindowEvent e) {}

	@Override
	public void windowClosed(final WindowEvent e) {}

	@Override
	public void windowClosing(final WindowEvent e) {}

	@Override
	public void windowDeactivated(final WindowEvent e) {}

	@Override
	public void windowOpened(final WindowEvent e) {}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("VisBio"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element container = XMLUtil.createChild(el, "Windows");
		final Enumeration<Window> en = windows.keys();
		while (en.hasMoreElements()) {
			final Window w = en.nextElement();
			final String name = SwingUtil.getWindowTitle(w);
			final Element e = XMLUtil.createChild(container, "Window");
			e.setAttribute("name", name);
			e.setAttribute("visible", "" + w.isVisible());
			final Rectangle r = w.getBounds();
			e.setAttribute("x", "" + r.x);
			e.setAttribute("y", "" + r.y);
			e.setAttribute("width", "" + r.width);
			e.setAttribute("height", "" + r.height);
		}
	}

	/** Restores the current state from the given DOM element ("VisBio"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final Element container = XMLUtil.getFirstChild(el, "Windows");
		windowStates.clear();
		final Element[] e = XMLUtil.getChildren(container, "Window");
		for (int i = 0; i < e.length; i++) {
			final String name = e[i].getAttribute("name");
			final String vis = e[i].getAttribute("visible");
			final int x = Integer.parseInt(e[i].getAttribute("x"));
			final int y = Integer.parseInt(e[i].getAttribute("y"));
			final int w = Integer.parseInt(e[i].getAttribute("width"));
			final int h = Integer.parseInt(e[i].getAttribute("height"));
			final WindowState ws =
				new WindowState(name, vis.equalsIgnoreCase("true"), x, y, w, h);
			final WindowInfo winfo = getWindowByTitle(name);
			if (winfo == null) windowStates.put(name, ws); // remember position
			else winfo.setState(ws); // window already exists; set position
		}
	}

	// -- Helper methods --

	/** Adds window-related GUI components to VisBio. */
	protected void doGUI() {
		// window listener
		bio.setSplashStatus("Initializing windowing logic");
		docker = new Docker();
		docker.setEnabled(docking);
		addWindow(bio);
		bio.addWindowListener(this);

		// options menu
		bio.setSplashStatus(null);
		final OptionManager om =
			(OptionManager) bio.getManager(OptionManager.class);
		// window docking is too buggy; disable it
		// om.addBooleanOption("General", DOCKING, 'd',
		// "Toggles whether window docking features are enabled", docking);
		om.addBooleanOption("General", HIDE_ALL, 'h', "Toggles whether all "
			+ "VisBio windows disappear when main window is minimized", hideAll);
	}

	/** Sets whether window docking features are enabled. */
	protected void setDocking(final boolean docking) {
		if (this.docking == docking) return;
		this.docking = docking;
		docker.setEnabled(docking);
	}

	/**
	 * Sets whether minimizing the main VisBio window hides the other VisBio
	 * windows.
	 */
	protected void setHideAll(final boolean hideAll) {
		if (this.hideAll == hideAll) return;
		this.hideAll = hideAll;
	}

	/** Propagates the given menu bar across all registered frames. */
	protected void doMenuBars(final JMenuBar master) {
		final Enumeration<Window> en = windows.keys();
		while (en.hasMoreElements()) {
			final Window w = en.nextElement();
			if (!(w instanceof JFrame)) continue;
			final JFrame f = (JFrame) w;
			if (f.getJMenuBar() != master) {
				f.setJMenuBar(SwingUtil.cloneMenuBar(master));
			}
		}
	}

	/**
	 * Gets window information about the first window matching the specified
	 * window title.
	 */
	protected WindowInfo getWindowByTitle(final String name) {
		final Enumeration<WindowInfo> en = windows.elements();
		while (en.hasMoreElements()) {
			final WindowInfo winfo = en.nextElement();
			final Window w = winfo.getWindow();
			if (name.equals(SwingUtil.getWindowTitle(w))) return winfo;
		}
		return null;
	}

}

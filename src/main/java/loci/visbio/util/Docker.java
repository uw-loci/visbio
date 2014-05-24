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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFrame;

/**
 * This class is used to specify a group of windows able to be docked to one
 * another. When a window is moved close enough to another window, it snaps next
 * to it, becoming docked. When the nearby window is subsequently moved, it
 * drags all docked windows along with it. Docker also enables windows to snap
 * to the edges of the screen, if desired.
 */
public class Docker implements ComponentListener {

	// -- Constants --

	/** Default window docking priority. */
	public static final int DEFAULT_PRIORITY = 5;

	/** A docking priority lower than the default. */
	public static final int LOW_PRIORITY = 0;

	/** A docking priority higher than the default. */
	public static final int HIGH_PRIORITY = 10;

	// -- Fields --

	/** List of windows able to be docked to one another. */
	protected Vector windows = new Vector();

	/** List of window locations. */
	protected Vector locations = new Vector();

	/** For each window, the set of windows docked to it. */
	protected Vector docked = new Vector();

	/** List of window docking priorities. */
	protected Vector priorities = new Vector();

	/** Whether to perform docking operations at all. */
	protected boolean enabled = true;

	/** Pixel threshold for windows to snap next to one another. */
	protected int thresh = 10;

	/** Whether to snap to the edges of the screen. */
	protected boolean edges = true;

	// -- Docker API methods --

	/** Adds a window to the dockable group. */
	public void addWindow(final Window w) {
		addWindow(w, DEFAULT_PRIORITY);
	}

	/** Adds a window to the dockable group, at the given docking priority. */
	public void addWindow(final Window w, final int priority) {
		w.addComponentListener(this);
		windows.add(w);
		locations.add(w.getLocation());
		docked.add(new HashSet());
		priorities.add(new Integer(priority));
	}

	/** Removes a window from the dockable group. */
	public void removeWindow(final Window w) {
		final int ndx = windows.indexOf(w);
		if (ndx < 0) return;

		// remove window from lists
		windows.removeElementAt(ndx);
		locations.removeElementAt(ndx);
		docked.removeElementAt(ndx);
		priorities.removeElementAt(ndx);

		// undock window from remaining windows
		for (int i = 0; i < windows.size(); i++) {
			final HashSet set = (HashSet) docked.elementAt(i);
			set.remove(w);
		}
	}

	/** Removes all windows from the dockable group. */
	public void removeAllWindows() {
		windows.removeAllElements();
		locations.removeAllElements();
		docked.removeAllElements();
		priorities.removeAllElements();
	}

	/** Sets whether docker should perform docking operations. */
	public void setEnabled(final boolean enabled) {
		this.enabled = enabled;
	}

	/** Sets the docking threshold in pixels. */
	public void setSnapThreshold(final int pixels) {
		thresh = pixels;
	}

	/** Sets whether windows should snap to the screen edges when close. */
	public void setSnapToScreenEdges(final boolean snap) {
		edges = snap;
	}

	// -- ComponentListener API methods --

	/** Called when a window is resized. */
	@Override
	public void componentResized(final ComponentEvent e) {}

	/** Called when a window is moved. */
	@Override
	public void componentMoved(final ComponentEvent e) {
		final Window w = (Window) e.getSource();
		final int ndx = getIndex(w);

		// build docked group
		final Vector group = new Vector();
		buildGroup(ndx, group);

		// build list of windows not in docked group
		final Vector non = new Vector();
		for (int i = 0; i < windows.size(); i++) {
			final Window wi = getWindow(i);
			if (wi.isVisible() && group.indexOf(wi) < 0) non.add(wi);
		}

		// undock all windows in the docked group from windows not in the group
		for (int i = 0; i < group.size(); i++) {
			final Window wi = (Window) group.elementAt(i);
			for (int j = 0; j < non.size(); j++)
				undock(wi, (Window) non.elementAt(j));
		}

		// compute docked group's X and Y snapping distance
		final Point p = computeSnap(w, non);

		// compute distance moved and record new location
		final Point oloc = getLocation(ndx);
		final Point nloc = w.getLocation();
		nloc.x += p.x;
		nloc.y += p.y;
		final Point shift = new Point(nloc.x - oloc.x, nloc.y - oloc.y);

		// snap all windows in docked group to appropriate location
		for (int i = 0; i < group.size(); i++) {
			final Window wi = (Window) group.elementAt(i);
			if (wi == w) shiftLocation(wi, p.x, p.y);
			else shiftLocation(wi, shift.x, shift.y);
		}
		// if (p.x == 0 && p.y == 0) return;

		// dock newly adjoining windows to appropriate windows within the group
		for (int i = 0; i < group.size(); i++) {
			final Window wi = (Window) group.elementAt(i);
			for (int j = 0; j < non.size(); j++) {
				final Window wj = (Window) non.elementAt(j);
				final Point pij = computeDistance(wi, wj);
				if (pij.x == 0 || pij.y == 0) {
					// windows are adjacent; dock them
					final int pi = getPriority(wi);
					final int pj = getPriority(wj);
					if (pi > pj) dock(wj, wi);
					else dock(wi, wj);
				}
			}
		}
	}

	/** Called when a window is shown. */
	@Override
	public void componentShown(final ComponentEvent e) {}

	/** Called when a window is hidden. */
	@Override
	public void componentHidden(final ComponentEvent e) {
		final Window w = (Window) e.getSource();
		final int ndx = getIndex(w);

		// undock all other windows from hidden window
		getDockSet(ndx).clear();

		// undock hidden window from all other windows
		for (int i = 0; i < windows.size(); i++)
			getDockSet(i).remove(w);
	}

	// -- Internal methods - get window characteristics from an index --

	/** Gets the window at the specified index. */
	protected Window getWindow(final int ndx) {
		return (Window) windows.elementAt(ndx);
	}

	/** Gets the last recorded location of the window at the specified index. */
	protected Point getLocation(final int ndx) {
		return (Point) locations.elementAt(ndx);
	}

	/** Gets the set of docked windows for the window at the specified index. */
	protected HashSet getDockSet(final int ndx) {
		return (HashSet) docked.elementAt(ndx);
	}

	/** Gets the docking priority for the window at the specified index. */
	protected int getPriority(final int ndx) {
		return ((Integer) priorities.elementAt(ndx)).intValue();
	}

	// -- Internal methods - get window characteristics from a window --

	/** Gets the given window's index. */
	protected int getIndex(final Window w) {
		return windows.indexOf(w);
	}

	/** Gets the given window's last recorded location. */
	protected Point getLocation(final Window w) {
		return getLocation(getIndex(w));
	}

	/** Gets the set of docked windows for the given window. */
	protected HashSet getDockSet(final Window w) {
		return getDockSet(getIndex(w));
	}

	/** Gets the docking priority for the given window. */
	protected int getPriority(final Window w) {
		return getPriority(getIndex(w));
	}

	// -- Internal methods - dock/undock windows to/from each other --

	/** Docks window A to window B. */
	protected void dock(final Window a, final Window b) {
		getDockSet(b).add(a);
	}

	/** Undocks window A from window B. */
	protected void undock(final Window a, final Window b) {
		getDockSet(b).remove(a);
	}

	// -- Internal methods - edge comparison/snapping --

	/** Integer absolute value function. */
	protected static int abs(final int value) {
		return value < 0 ? -value : value;
	}

	/** Computes the distance rectangle A must snap to adjoin rectangle B. */
	protected Point computeDistance(final Rectangle a, final Rectangle b) {
		final Point p = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);

		// compute horizontal distance
		if (b.y <= a.y + a.height + thresh && a.y <= b.y + b.height + thresh) {
			final int ll = b.x - a.x;
			if (abs(ll) < abs(p.x)) p.x = ll;
			final int lr = b.x - (a.x + a.width);
			if (abs(lr) < abs(p.x)) p.x = lr;
			final int rl = b.x + b.width - a.x;
			if (abs(rl) < abs(p.x)) p.x = rl;
			final int rr = b.x + b.width - (a.x + a.width);
			if (abs(rr) < abs(p.x)) p.x = rr;
		}

		// compute vertical distance
		if (b.x <= a.x + a.width + thresh && a.x <= b.x + b.width + thresh) {
			final int uu = b.y - a.y;
			if (abs(uu) < abs(p.y)) p.y = uu;
			final int ud = b.y - (a.y + a.height);
			if (abs(ud) < abs(p.y)) p.y = ud;
			final int du = b.y + b.height - a.y;
			if (abs(du) < abs(p.y)) p.y = du;
			final int dd = b.y + b.height - (a.y + a.height);
			if (abs(dd) < abs(p.y)) p.y = dd;
		}

		return p;
	}

	/** Computes the distance window A must snap to adjoin window B. */
	protected Point computeDistance(final Window a, final Window b) {
		return computeDistance(a.getBounds(), b.getBounds());
	}

	/** Computes the X and Y distances the docked group should snap. */
	protected Point computeSnap(final Window w, final Vector non) {
		final Point p = new Point(Integer.MAX_VALUE, Integer.MAX_VALUE);
		final Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
		final Rectangle screen = new Rectangle(0, 0, ss.width, ss.height);

		final Rectangle r = w.getBounds();
		for (int i = 0; i < non.size(); i++) {
			final Rectangle ri = ((Window) non.elementAt(i)).getBounds();
			final Point pi = computeDistance(r, ri);
			if (abs(pi.x) < abs(p.x)) p.x = pi.x;
			if (abs(pi.y) < abs(p.y)) p.y = pi.y;
		}
		if (edges) {
			final Point ps = computeDistance(r, screen);
			if (abs(ps.x) < abs(p.x)) p.x = ps.x;
			if (abs(ps.y) < abs(p.y)) p.y = ps.y;
		}

		if (abs(p.x) > thresh) p.x = 0;
		if (abs(p.y) > thresh) p.y = 0;
		return p;
	}

	/**
	 * Shifts the given window's location by the given amount, without generating
	 * a ComponentEvent.
	 */
	protected void shiftLocation(final Window w, final int px, final int py) {
		if (enabled) {
			w.removeComponentListener(this);
			final Point l = w.getLocation();
			w.setLocation(l.x + px, l.y + py);
		}
		locations.setElementAt(w.getLocation(), getIndex(w));
		if (enabled) w.addComponentListener(this);
	}

	// -- Helper methods --

	/** Recursively builds a list of windows docked together into a group. */
	protected void buildGroup(final int ndx, final Vector group) {
		group.add(windows.elementAt(ndx));
		final HashSet set = (HashSet) docked.elementAt(ndx);
		final Iterator it = set.iterator();
		while (it.hasNext()) {
			final Window w = (Window) it.next();
			if (group.indexOf(w) < 0) buildGroup(getIndex(w), group);
		}
	}

	// -- Main method --

	/** Tests the Docker class. */
	public static void main(final String[] args) {
		final Docker docker = new Docker();
		for (int i = 0; i < 5; i++) {
			final JFrame frame = new JFrame(i == 0 ? "Main" : "Frame" + i);
			frame.getContentPane().add(new JButton(i == 0 ? "Main" : "Button" + i));
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.pack();
			frame.setBounds(300, 80 * i + 200, 200, 60);
			if (i == 0) docker.addWindow(frame, HIGH_PRIORITY);
			else docker.addWindow(frame);
			frame.setVisible(true);
		}
	}

}

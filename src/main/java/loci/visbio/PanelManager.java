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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Dimension;
import java.awt.Point;
import java.util.Arrays;
import java.util.Vector;

import javax.swing.JPanel;

import loci.visbio.help.HelpManager;

/**
 * PanelManager is the manager encapsulating VisBio's control panel logic.
 */
public class PanelManager extends LogicManager {

	// -- GUI components --

	/** Control panels. */
	private final Vector panels = new Vector();

	/** (X, Y) coordinates for each control panel. */
	private final Vector coords = new Vector();

	/** Width and height for each control panel. */
	private final Vector sizes = new Vector();

	/** Column string for each control panel. */
	private final Vector colStrings = new Vector();

	/** Row string for each control panel. */
	private final Vector rowStrings = new Vector();

	/** Number of rows in panel layout. */
	private int numRows = 0;

	/** Number of columns in panel layout. */
	private int numCols = 0;

	/** Pane containing control panels. */
	private JPanel pane;

	// -- Constructor --

	/** Constructs a panel manager. */
	public PanelManager(final VisBioFrame bio) {
		super(bio);
	}

	// -- PanelManager API methods --

	/** Adds a new control panel. */
	public void addPanel(final ControlPanel cpl, final int x, final int y,
		final int w, final int h)
	{
		addPanel(cpl, x, y, w, h, null, null);
	}

	/** Adds a new control panel. */
	public void addPanel(final ControlPanel cpl, final int x, final int y,
		final int w, final int h, final String colString, final String rowString)
	{
		panels.add(cpl);
		final int xx = 2 * x + 1;
		final int yy = 4 * y + 1;
		final int ww = w;
		final int hh = 4 * h - 3;
		if (xx + ww - 1 > numCols) numCols = xx + ww - 1;
		if (yy + hh + 1 > numRows) numRows = yy + hh + 1;
		coords.add(new Point(xx, yy));
		sizes.add(new Dimension(ww, hh));
		colStrings.add(colString == null ? "pref:grow" : colString);
		rowStrings.add(rowString == null ? "pref:grow" : rowString);
		bio.generateEvent(this, "add " + cpl.getName() + " panel", false);
	}

	/** Gets the control panel with the given name. */
	public ControlPanel getPanel(final String name) {
		for (int i = 0; i < panels.size(); i++) {
			final ControlPanel cpl = (ControlPanel) panels.elementAt(i);
			if (cpl.getName().equals(name)) return cpl;
		}
		return null;
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
				// HACK - finalize control panel layout
				bio.setSplashStatus("Initializing control panels");

				// lay out control panels
				final String pad = "9dlu";
				final String[] cols = new String[numCols];
				final String[] rows = new String[numRows];
				Arrays.fill(cols, pad);
				Arrays.fill(rows, pad);
				for (int i = 0; i < panels.size(); i++) {
					final ControlPanel cpl = (ControlPanel) panels.elementAt(i);
					final Point p = (Point) coords.elementAt(i);
					final Dimension d = (Dimension) sizes.elementAt(i);
					final int xx = p.x;
					final int yy = p.y;
					final int ww = d.width;
					final int hh = d.height;
					final String colString = (String) colStrings.elementAt(i);
					final String rowString = (String) rowStrings.elementAt(i);
					cols[xx - 1] = colString;
					rows[yy - 1] = "pref";
					rows[yy] = "3dlu";
					rows[yy + 1] = rowString;
				}
				final StringBuffer cbuf = new StringBuffer(cols[0]);
				for (int i = 1; i < cols.length; i++) {
					cbuf.append(",");
					cbuf.append(cols[i]);
				}
				final StringBuffer rbuf = new StringBuffer(rows[0]);
				for (int i = 1; i < rows.length; i++) {
					rbuf.append(",");
					rbuf.append(rows[i]);
				}
				final PanelBuilder builder =
					new PanelBuilder(new FormLayout(cbuf.toString(), rbuf.toString()));
				builder.setDefaultDialogBorder();
				final CellConstraints cc = new CellConstraints();
				for (int i = 0; i < panels.size(); i++) {
					final ControlPanel cpl = (ControlPanel) panels.elementAt(i);
					final Point p = (Point) coords.elementAt(i);
					final Dimension d = (Dimension) sizes.elementAt(i);
					final int xx = p.x;
					final int yy = p.y;
					final int ww = d.width;
					final int hh = d.height;
					builder.addSeparator(cpl.getName(), cc.xyw(xx, yy, ww));
					builder.add(cpl, cc.xywh(xx, yy + 2, ww, hh, "fill,fill"));
				}
				bio.setContentPane(builder.getPanel());
			}
		}
	}

	/** Gets the number of tasks required to initialize this logic manager. */
	@Override
	public int getTasks() {
		return 2;
	}

	// -- Helper methods --

	/** Adds base control panel GUI components to VisBio. */
	private void doGUI() {
		// help window
		bio.setSplashStatus(null);
		final HelpManager hm = (HelpManager) bio.getManager(HelpManager.class);
		hm.addHelpTopic("Control panels", "control_panels.html");
	}

}

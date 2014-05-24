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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

/**
 * BioTask is a panel representing a particular VisBio task.
 */
public class BioTask extends JPanel implements ActionListener {

	// -- Fields --

	/** Task manager associated with the task. */
	protected TaskManager tm;

	/** Whether the task has been stopped. */
	protected boolean stopped = false;

	// -- GUI components --

	/** Label displaying name of the task. */
	protected JLabel title;

	/** Label displaying current status message for the task. */
	protected JLabel status;

	/** Progress bar displaying the task's progress. */
	protected JProgressBar progress;

	/** Button for halting and resuming the task. */
	protected JButton stop;

	// -- Constructor --

	/** Constructs a new VisBio task. */
	public BioTask(final TaskManager taskMan, final String name) {
		tm = taskMan;
		title = new JLabel(name);
		title.setFont(title.getFont().deriveFont(Font.BOLD));
		status = new JLabel() {

			@Override
			public Dimension getPreferredSize() {
				// HACK - limit label width to viewport
				final Dimension pref = super.getPreferredSize();
				int width =
					tm.getControls().getPreferredTaskWidth() -
						title.getPreferredSize().width - stop.getPreferredSize().width - 25;
				if (pref.width < width) width = pref.width;
				return new Dimension(width, pref.height);
			}
		};
		progress = new JProgressBar();
		progress.setIndeterminate(true);
		stop = new JButton("Stop");
		stop.addActionListener(this);
		stop.setEnabled(false);
		setLayout(new BorderLayout());
		setBorder(new EmptyBorder(0, 0, 5, 0));
		final PanelBuilder builder =
			new PanelBuilder(new FormLayout("pref:grow, 3dlu, pref:grow, 3dlu, pref",
				"pref, pref"));
		final CellConstraints cc = new CellConstraints();
		builder.add(title, cc.xy(1, 1, "left,bottom"));
		builder.add(status, cc.xy(3, 1, "right,bottom"));
		builder.add(progress, cc.xyw(1, 2, 3, "fill,top"));
		builder.add(stop, cc.xywh(5, 1, 1, 2, "center,center"));
		add(builder.getPanel());
	}

	// -- BioTask API methods --

	/** Updates the status of this task. */
	public void setStatus(final int value, final int maximum) {
		setStatus(value, maximum, null);
	}

	/** Updates the status of this task. */
	public void setStatus(final String message) {
		setStatus(-1, -1, message);
	}

	/** Updates the status of this task. */
	public void
		setStatus(final int value, final int maximum, final String message)
	{
		final int val = value;
		final int max = maximum;
		final String msg = message;
		SwingUtilities.invokeLater(new Runnable() {

			@Override
			public void run() {
				if (val >= 0 && max >= 0) {
					progress.setIndeterminate(false);
					progress.setMaximum(max);
					progress.setValue(val);
				}
				if (msg != null) status.setText(msg);
			}
		});
	}

	/** Marks a task as completed. */
	public void setCompleted() {
		tm.setCompleted(this);
	}

	/** Toggles whether the task can be stopped midway. */
	public void setStoppable(final boolean stoppable) {
		stop.setEnabled(stoppable);
	}

	/** Gets whether the task has been stopped. */
	public boolean isStopped() {
		return stopped;
	}

	// -- Component API methods --

	@Override
	public Dimension getMaximumSize() {
		final Dimension max = super.getMaximumSize();
		final Dimension pref = super.getPreferredSize();
		return new Dimension(max.width, pref.height);
	}

	// -- ActionListener methods --

	/** Toggles the stop button. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		progress.setIndeterminate(false);
		stop.setEnabled(false);
		stopped = true;
	}

}

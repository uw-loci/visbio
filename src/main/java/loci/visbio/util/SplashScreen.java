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

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.Color;
import java.net.URL;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.border.LineBorder;

/**
 * An application splash screen.
 */
public class SplashScreen extends JWindow {

	// -- Fields --

	/** Label containing (bottom line of) the splash message. */
	private JLabel msgLabel;

	/** Progress bar. */
	private final JProgressBar bar;

	/** Current task number. */
	private int task = 0;

	/** Total number of tasks. */
	private int tasks = 0;

	// -- Constructor --

	/**
	 * Constructs a splash screen using the given logo image, informative messages
	 * and message background color.
	 */
	public SplashScreen(final URL logo, final String[] msg, final Color bgColor) {
		this(logo, msg, bgColor, null);
	}

	/**
	 * Constructs a splash screen using the given logo image, informative
	 * messages, message background color and progress bar color.
	 */
	public SplashScreen(final URL logo, final String[] msg, final Color bgColor,
		final Color pbColor)
	{
		final JLabel logoImage = new JLabel(new ImageIcon(logo));
		bar = new JProgressBar();
		if (pbColor != null) bar.setForeground(pbColor);

		// lay out components
		final StringBuffer sb = new StringBuffer();
		sb.append("pref, pref, 2dlu");
		for (int i = 0; i < msg.length; i++)
			sb.append(", pref");
		sb.append(", 2dlu");
		final PanelBuilder builder =
			new PanelBuilder(new FormLayout("pref", sb.toString()));
		final CellConstraints cc = new CellConstraints();
		builder.add(logoImage, cc.xy(1, 1));
		builder.add(bar, cc.xy(1, 2));
		for (int i = 0; i < msg.length; i++) {
			msgLabel = builder.addLabel(msg[i], cc.xy(1, i + 4));
			msgLabel.setHorizontalAlignment(SwingConstants.CENTER);
		}
		final JPanel pane = builder.getPanel();
		pane.setBorder(new LineBorder(Color.black, 2));
		if (bgColor != null) pane.setBackground(bgColor);
		setContentPane(pane);
		pack();
		SwingUtil.centerWindow(this);
	}

	// -- SplashScreen API methods --

	/** Sets the number of tasks for splash screen progress bar. */
	public void setTaskCount(final int taskCount) {
		tasks = taskCount;
	}

	/** Changes the currently displayed text. */
	public void setText(final String msg) {
		msgLabel.setText(msg);
	}

	/** Advances task counter. */
	public void nextTask() {
		if (tasks > 0) {
			task++;
			final int p = 100 * task / tasks;
			bar.setValue(p);
		}
	}

	/** Gets current task number. */
	public int getTask() {
		return task;
	}

	/** Gets number of tasks for splash screen progress bar. */
	public int getTaskCount() {
		return tasks;
	}

}

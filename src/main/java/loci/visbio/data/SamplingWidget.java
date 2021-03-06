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

package loci.visbio.data;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import loci.visbio.util.LAFUtil;

/**
 * SamplingWidget is a set of GUI controls for a DataSampling transform.
 */
public class SamplingWidget extends JPanel implements ActionListener {

	// -- Fields --

	/** Associated data sampling. */
	protected DataSampling sampling;

	/** Text field for image width. */
	protected JTextField widthField;

	/** Text field for image height. */
	protected JTextField heightField;

	/** Text fields for minimum dimensional values. */
	protected JTextField[] minFields;

	/** Text fields for maximum dimensional values. */
	protected JTextField[] maxFields;

	/** Text fields for dimensional step values. */
	protected JTextField[] stepFields;

	/** Check boxes for included range components. */
	protected JCheckBox[] rangeBoxes;

	// -- Constructor --

	public SamplingWidget(final DataSampling sampling) {
		super();
		this.sampling = sampling;

		final int[] lengths = sampling.getLengths();
		final String[] dims = sampling.getDimTypes();
		final int[] min = sampling.getMin();
		final int[] max = sampling.getMax();
		final int[] step = sampling.getStep();
		final int resX = sampling.getImageWidth();
		final int resY = sampling.getImageHeight();
		final boolean[] range = sampling.getRange();

		widthField = new JTextField("" + resX, 4);
		heightField = new JTextField("" + resY, 4);

		minFields = new JTextField[min.length];
		maxFields = new JTextField[max.length];
		stepFields = new JTextField[step.length];
		for (int i = 0; i < lengths.length; i++) {
			minFields[i] = new JTextField("" + min[i], 4);
			maxFields[i] = new JTextField("" + max[i], 4);
			stepFields[i] = new JTextField("" + step[i], 4);
		}

		final JPanel rangePanel = new JPanel();
		rangePanel.add(new JLabel("Range components"));
		rangeBoxes = new JCheckBox[range.length];
		for (int i = 0; i < range.length; i++) {
			rangeBoxes[i] = new JCheckBox("" + (i + 1), range[i]);
			if (i < 9 && !LAFUtil.isMacLookAndFeel()) {
				rangeBoxes[i].setMnemonic('1' + i);
			}
			rangePanel.add(rangeBoxes[i]);
		}

		final JButton apply = new JButton("Apply");
		if (!LAFUtil.isMacLookAndFeel()) apply.setMnemonic('a');
		apply.addActionListener(this);

		// lay out components
		final StringBuffer sb = new StringBuffer("pref, 3dlu, ");
		for (int i = 0; i < lengths.length; i++)
			sb.append("pref, 3dlu, ");
		sb.append("pref, 3dlu, pref");
		final FormLayout layout =
			new FormLayout(
				"pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref", sb
					.toString());
		final PanelBuilder builder = new PanelBuilder(layout);
		final CellConstraints cc = new CellConstraints();
		builder.addLabel("Image &resolution", cc.xyw(1, 1, 5));
		builder.add(widthField, cc.xy(7, 1));
		builder.addLabel("&by", cc.xy(9, 1));
		builder.add(heightField, cc.xy(11, 1));
		for (int i = 0; i < lengths.length; i++) {
			final int row = 2 * i + 3;
			builder.addLabel("<" + (i + 1) + "> " + dims[i], cc.xy(1, row));
			builder.add(minFields[i], cc.xy(3, row));
			builder.addLabel("to", cc.xy(5, row));
			builder.add(maxFields[i], cc.xy(7, row));
			builder.addLabel("step", cc.xy(9, row));
			builder.add(stepFields[i], cc.xy(11, row));
		}
		builder.add(rangePanel, cc.xyw(1, 2 * lengths.length + 3, 11));
		builder.add(ButtonBarFactory.buildCenteredBar(apply), cc.xyw(1,
			2 * lengths.length + 5, 11));

		setLayout(new BorderLayout());
		add(builder.getPanel());
	}

	// -- ActionListener API methods --

	/** Applies changes to this data sampling's parameters. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		String msg = null;
		int resX = -1, resY = -1;
		int[] min = null, max = null, step = null;
		try {
			resX = Integer.parseInt(widthField.getText());
			resY = Integer.parseInt(heightField.getText());
			min = parseInts(minFields);
			max = parseInts(maxFields);
			step = parseInts(stepFields);
		}
		catch (final NumberFormatException exc) {}
		final boolean[] range = new boolean[rangeBoxes.length];
		int numRange = 0;
		for (int i = 0; i < rangeBoxes.length; i++) {
			range[i] = rangeBoxes[i].isSelected();
			if (range[i]) numRange++;
		}

		// check parameters for validity
		if (resX <= 0 || resY <= 0) msg = "Invalid image resolution.";
		else {
			final DataTransform parent = sampling.getParent();
			final int[] len = parent.getLengths();
			final String[] types = parent.getDimTypes();
			for (int i = 0; i < minFields.length; i++) {
				final String dim = "<" + (i + 1) + "> " + types[i];
				if (min[i] < 1 || min[i] > len[i]) msg = "Invalid minimum for " + dim;
				else if (max[i] < 1 || max[i] > len[i]) {
					msg = "Invalid maximum for " + dim;
				}
				else if (step[i] < 1) msg = "Invalid step size for " + dim;
				else if (min[i] > max[i]) {
					msg = "Minimum cannot be greater than maximum for " + dim;
				}
				if (msg != null) break;
			}
		}
		if (numRange == 0) {
			msg = "Sampling must include at least one range component.";
		}

		if (msg != null) {
			JOptionPane.showMessageDialog(this, msg, "VisBio",
				JOptionPane.ERROR_MESSAGE);
			return;
		}

		sampling.setParameters(min, max, step, resX, resY, range);
	}

	// -- Helper methods --

	/** Parses integer values from the given list of text fields. */
	private int[] parseInts(final JTextField[] fields) {
		final int[] vals = new int[fields.length];
		for (int i = 0; i < fields.length; i++) {
			vals[i] = Integer.parseInt(fields[i].getText());
		}
		return vals;
	}

}

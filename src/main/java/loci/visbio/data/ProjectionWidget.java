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

import com.jgoodies.forms.factories.ButtonBarFactory;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;

import loci.visbio.util.BioComboBox;
import loci.visbio.util.FormsUtil;
import loci.visbio.util.LAFUtil;

/**
 * ProjectionWidget is a set of GUI controls for a maximum intensity projection
 * transform.
 */
public class ProjectionWidget extends JPanel implements ActionListener {

	// -- Fields --

	/** Associated maximum intensity projection transform. */
	protected ProjectionTransform projection;

	/** Dropdown combo box listing available dimensions for projection. */
	protected BioComboBox axes;

	// -- Constructor --

	/** Creates a new maximum intensity projection widget. */
	public ProjectionWidget(final ProjectionTransform projection) {
		super();
		this.projection = projection;

		final DataTransform parent = projection.getParent();
		final String[] types = parent.getDimTypes();

		// create combo box for selecting which axis to project
		final String[] names = new String[types.length];
		for (int i = 0; i < names.length; i++)
			names[i] = (i + 1) + ": " + types[i];
		axes = new BioComboBox(names);

		// apply button
		final JButton apply = new JButton("Apply");
		if (!LAFUtil.isMacLookAndFeel()) apply.setMnemonic('a');
		apply.addActionListener(this);

		// lay out components
		final JPanel row1 =
			FormsUtil.makeRow(new Object[] { "&Dimension to project", axes },
				new boolean[] { false, true });
		final JPanel row2 = ButtonBarFactory.buildCenteredBar(apply);

		setLayout(new BorderLayout());
		add(FormsUtil.makeColumn(row1, row2));
	}

	// -- ActionListener API methods --

	/** Applies changes to this projection's parameters. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final int index = axes.getSelectedIndex();
		projection.setParameters(index);
	}

}

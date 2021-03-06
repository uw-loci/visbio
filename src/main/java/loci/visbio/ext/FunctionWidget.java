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

package loci.visbio.ext;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;

import loci.visbio.util.LAFUtil;

/**
 * FunctionWidget is a set of GUI controls for an ExternalFunction transform.
 */
public class FunctionWidget extends JPanel implements ActionListener {

	// -- Fields --

	/** Associated external function transform. */
	protected ExternalFunction function;

	/** Fields corresponding to function parameters. */
	protected JTextField[] paramFields;

	// -- Constructor --

	public FunctionWidget(final ExternalFunction function) {
		super();
		this.function = function;

		final String[] names = function.getParameterNames();
		final String[] params = function.getParameters();

		// create text fields
		paramFields = new JTextField[params.length];
		for (int i = 0; i < params.length; i++) {
			paramFields[i] = new JTextField(8);
			paramFields[i].setText(params[i]);
		}

		// create apply button
		final JButton apply = new JButton("Apply");
		if (!LAFUtil.isMacLookAndFeel()) apply.setMnemonic('a');
		apply.addActionListener(this);

		// lay out components
		final StringBuffer sb = new StringBuffer("pref");
		for (int i = 1; i < paramFields.length; i++)
			sb.append(", 3dlu, pref");
		sb.append(", 9dlu, pref");
		final PanelBuilder builder =
			new PanelBuilder(new FormLayout("pref, 3dlu, pref:grow", sb.toString()));
		final CellConstraints cc = new CellConstraints();
		for (int i = 0; i < paramFields.length; i++) {
			final int row = 2 * i + 1;
			builder.addLabel(names[i], cc.xy(1, row));
			builder.add(paramFields[i], cc.xy(3, row));
		}
		builder.add(ButtonBarFactory.buildCenteredBar(apply), cc.xyw(1,
			2 * paramFields.length + 1, 3));

		setLayout(new BorderLayout());
		add(builder.getPanel());
	}

	// -- ActionListener API methods --

	/** Applies changes to this data sampling's parameters. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final String[] params = new String[paramFields.length];
		for (int i = 0; i < params.length; i++)
			params[i] = paramFields[i].getText();
		function.setParameters(params);
	}

}

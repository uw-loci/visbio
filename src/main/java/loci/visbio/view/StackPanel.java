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

package loci.visbio.view;

import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;

import loci.visbio.data.DataTransform;
import loci.visbio.util.BioComboBox;
import loci.visbio.util.DialogPane;
import loci.visbio.util.FormsUtil;
import loci.visbio.util.LAFUtil;

/**
 * Provides GUI controls for a display stack handler.
 */
public class StackPanel extends TransformPanel {

	// -- Fields --

	/** Label for stack axis combo box. */
	protected JLabel stackLabel;

	/** List of axes for stacking. */
	protected BioComboBox stackBox;

	/** Dialog box for toggling individual slices. */
	protected SliceToggler sliceToggler;

	/** Button for bringing up slice toggler dialog box. */
	protected JButton toggleSlices;

	/** Checkbox for choosing whether current slice is visible. */
	protected JCheckBox sliceVisible;

	/** Checkbox for choosing whether current slice is highlighted. */
	protected JCheckBox highlight;

	/** Checkbox indicating whether volume rendering is enabled. */
	protected JCheckBox render;

	/** Spinner for volume rendering resolution. */
	protected JSpinner renderRes;

	// -- Constructor --

	/** Creates a panel containing view handler GUI controls. */
	public StackPanel(final StackHandler h) {
		super(h);
	}

	// -- TransformPanel API methods --

	/** Updates controls to reflect current handler status. */
	@Override
	public void updateControls() {
		super.updateControls();

		final DataTransform trans =
			(DataTransform) transformList.getSelectedValue();
		final TransformLink tlink = trans == null ? null : handler.getLink(trans);
		final boolean isStack = tlink instanceof StackLink;
		final StackLink slink = isStack ? (StackLink) tlink : null;
		final boolean hasAxis = isStack && slink.getStackAxis() >= 0;
		final boolean isRendered = isStack && slink.isVolumeRendered();
		if (visible.isEnabled()) visible.setEnabled(!isRendered);
		stackLabel.setEnabled(isStack && !isRendered);
		stackBox.setEnabled(isStack && !isRendered);
		toggleSlices.setEnabled(isStack && hasAxis && !isRendered);
		sliceVisible.setEnabled(isStack && hasAxis && !isRendered);
		highlight.setEnabled(isStack);
		render.setEnabled(isStack && hasAxis);
		renderRes.setEnabled(isStack && hasAxis);
		if (isStack) {
			// update "stack axis" combo box
			final String[] dims = trans.getDimTypes();
			stackBox.removeActionListener(this);
			final String[] items = new String[dims.length + 1];
			items[0] = "None";
			for (int i = 0; i < dims.length; i++) {
				items[i + 1] = "<" + (i + 1) + "> " + dims[i];
			}
			boolean same = items.length == stackBox.getItemCount();
			for (int i = 0; i < items.length && same; i++) {
				same = items[i].equals(stackBox.getItemAt(i));
			}
			if (!same) {
				stackBox.removeAllItems();
				for (int i = 0; i < items.length; i++)
					stackBox.addItem(items[i]);
			}
			final int stackAxis = slink.getStackAxis();
			stackBox.setSelectedIndex(stackAxis + 1);
			stackBox.addActionListener(this);

			// update "current slice visible" checkbox
			final int slice = slink.getCurrentSlice();
			sliceVisible.setSelected(slink.isSliceVisible(slice));

			// update "highlight current slice" checkbox
			highlight.setSelected(slink.isBoundingBoxVisible());

			// update "render as a volume" checkbox and spinner
			render.setSelected(slink.isVolumeRendered());
			renderRes.setValue(new Integer(slink.getVolumeResolution()));
		}
	}

	// -- ActionListener API methods --

	/** Handles button presses and combo box selections. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final String cmd = e.getActionCommand();
		if (cmd.equals("stackBox")) {
			final DataTransform trans =
				(DataTransform) transformList.getSelectedValue();
			final StackLink link = (StackLink) handler.getLink(trans);
			link.setStackAxis(stackBox.getSelectedIndex() - 1);
			handler.rebuild(false);
		}
		else if (cmd.equals("toggleSlices")) {
			final DisplayWindow window = handler.getWindow();
			final DataTransform trans =
				(DataTransform) transformList.getSelectedValue();
			sliceToggler.setTransform(trans);
			final int rval = sliceToggler.showDialog(window.getControls());
			if (rval != DialogPane.APPROVE_OPTION) return;
			updateControls();
		}
		else if (cmd.equals("sliceVisible")) {
			final DataTransform trans =
				(DataTransform) transformList.getSelectedValue();
			final StackLink link = (StackLink) handler.getLink(trans);
			link.setSliceVisible(link.getCurrentSlice(), sliceVisible.isSelected());
			updateControls();
		}
		else if (cmd.equals("highlight")) {
			final DataTransform trans =
				(DataTransform) transformList.getSelectedValue();
			final StackLink link = (StackLink) handler.getLink(trans);
			link.setBoundingBoxVisible(highlight.isSelected());
		}
		else if (cmd.equals("render")) {
			final DataTransform trans =
				(DataTransform) transformList.getSelectedValue();
			final StackLink link = (StackLink) handler.getLink(trans);
			link.setVolumeRendered(render.isSelected());
			updateControls();
		}
		else super.actionPerformed(e);
	}

	// -- ChangeListener API methods --

	/** Handles spinner changes. */
	@Override
	public void stateChanged(final ChangeEvent e) {
		final Object src = e.getSource();
		if (src == renderRes) {
			final DataTransform trans =
				(DataTransform) transformList.getSelectedValue();
			final StackLink link = (StackLink) handler.getLink(trans);
			link.setVolumeResolution(((Integer) renderRes.getValue()).intValue());
		}
		else super.stateChanged(e);
	}

	// -- Helper methods --

	/** Creates a panel for controls pertaining to the selected data object. */
	@Override
	protected JPanel doDataProperties() {
		// stack axis combo box
		stackBox = new BioComboBox(new String[] { "None" });
		stackBox.setToolTipText("The axis over which to stack images in 3D");
		stackBox.setActionCommand("stackBox");
		stackBox.addActionListener(this);
		stackBox.setEnabled(false);

		// stack axis label
		stackLabel = new JLabel("Stack axis");
		if (!LAFUtil.isMacLookAndFeel()) stackLabel.setDisplayedMnemonic('k');
		stackLabel.setLabelFor(stackBox);
		stackLabel.setEnabled(false);

		// slice toggler dialog box
		sliceToggler = new SliceToggler((StackHandler) handler);

		// toggle slices button
		toggleSlices = new JButton("Toggle slices");
		if (!LAFUtil.isMacLookAndFeel()) toggleSlices.setMnemonic('g');
		toggleSlices
			.setToolTipText("Provides options to toggle slices in the stack");
		toggleSlices.setActionCommand("toggleSlices");
		toggleSlices.addActionListener(this);
		toggleSlices.setEnabled(false);

		// current slice visible checkbox
		sliceVisible = new JCheckBox("Current slice visible");
		if (!LAFUtil.isMacLookAndFeel()) sliceVisible.setMnemonic('l');
		sliceVisible.setToolTipText("Toggles visibility of the current slice");
		sliceVisible.setActionCommand("sliceVisible");
		sliceVisible.addActionListener(this);
		sliceVisible.setEnabled(false);

		// highlight current slice checkbox
		highlight = new JCheckBox("Highlight current slice");
		if (!LAFUtil.isMacLookAndFeel()) highlight.setMnemonic('h');
		highlight.setToolTipText("Toggles yellow highlight around current slice");
		highlight.setActionCommand("highlight");
		highlight.addActionListener(this);
		highlight.setEnabled(false);

		// checkbox for toggling volume rendering
		render = new JCheckBox("Render as a volume");
		render.setActionCommand("render");
		render.addActionListener(this);
		render.setMnemonic('r');

		// slider for adjusting volume resolution
		final SpinnerNumberModel renderModel =
			new SpinnerNumberModel(StackHandler.DEFAULT_VOLUME_RESOLUTION,
				StackHandler.MIN_VOLUME_RESOLUTION, StackHandler.MAX_VOLUME_RESOLUTION,
				16);
		renderRes = new JSpinner(renderModel);
		renderRes.setToolTipText("Adjusts the resolution of the rendering.");
		renderRes.setEnabled(false);
		renderRes.addChangeListener(this);

		// lay out components
		return FormsUtil.makeColumn(new Object[] {
			FormsUtil.makeRow(stackLabel, stackBox),
			FormsUtil.makeRow(visible, toggleSlices), sliceVisible, highlight,
			FormsUtil.makeRow(render, renderRes) });
	}

}

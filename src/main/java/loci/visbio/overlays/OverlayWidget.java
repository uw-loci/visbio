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

package loci.visbio.overlays;

import com.jgoodies.forms.builder.PanelBuilder;
import com.jgoodies.forms.factories.ButtonBarFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Vector;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Timer;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.Document;

import loci.formats.gui.ExtensionFileFilter;
import loci.poi.hssf.usermodel.HSSFWorkbook;
import loci.visbio.VisBioFrame;
import loci.visbio.data.TransformEvent;
import loci.visbio.data.TransformListener;
import loci.visbio.state.OptionManager;
import loci.visbio.state.SpreadsheetLaunchOption;
import loci.visbio.state.SpreadsheetOptionStrategy;
import loci.visbio.util.BioComboBox;
import loci.visbio.util.DialogPane;
import loci.visbio.util.FontChooserPane;
import loci.visbio.util.FormsUtil;
import loci.visbio.util.LAFUtil;
import loci.visbio.util.ObjectUtil;
import loci.visbio.util.SwingUtil;
import visad.util.Util;

/**
 * OverlayWidget is a set of GUI controls for an overlay transform.
 */
public class OverlayWidget extends JPanel implements ActionListener,
	DocumentListener, ListSelectionListener, Runnable, TransformListener
{

	// -- Fields --

	/** Associated overlay object. */
	protected OverlayTransform overlay;

	/** Button group ensuring only one tool can be selected at a time. */
	protected ButtonGroup buttonGroup;

	/** List of overlay tools. */
	protected Vector tools;

	/** List of buttons for each overlay tool. */
	protected Vector buttons;

	/** Timer for refreshing widget components 5 times per second. */
	protected Timer refreshTimer;

	/** Flag indicating widget components need to be refreshed. */
	protected boolean needRefresh;

	/**
	 * Flag indicating AWT/Swing events should be ignored, used for programmatic
	 * update of GUI components.
	 */
	protected boolean ignoreEvents;

	// -- GUI components - global --

	/** File chooser for saving overlays. */
	protected JFileChooser overlaySaveBox;

	/** File chooser for loading overlays. */
	protected JFileChooser overlayLoadBox;

	/** File chooser for exporting overlays to .xls format. */
	protected JFileChooser overlayXLSBox;

	/** Text field indicating current font. */
	protected JTextField currentFont;

	/** Button for bringing up font chooser. */
	protected JButton chooseFont;

	/** List of overlays. */
	protected JList overlayList;

	/** List model for overlay list. */
	protected DefaultListModel overlayListModel;

	/** Button for removing selected overlays. */
	protected JButton remove;

	/** Button for copying selected overlays to the clipboard. */
	protected JButton copy;

	/** Button for pasting copied overlays onto the current position. */
	protected JButton paste;

	/**
	 * Button for distributing an overlay across all positions between the copied
	 * position and the one selected.
	 */
	protected JButton dist;

	/** Button for loading overlays from disk. */
	protected JButton load;

	/** Button for saving overlays to disk. */
	protected JButton save;

	/** Button for exporting overlays to .xls on disk. */
	protected JButton export;

	// -- GUI components - overlay-specific --

	/** Text fields for (X, Y) coordinate pairs. */
	protected JTextField x1, y1, x2, y2;

	/** Text field for text labels. */
	protected JTextField text;

	/** Button for choosing overlay color. */
	protected JButton color;

	/** Check box indicating whether overlay should be filled or hollow. */
	protected JCheckBox filled;

	/** Combo box for selecting an overlay group. */
	protected BioComboBox groupList;

	/** Button for creating a new overlay group. */
	protected JButton newGroup;

	/** Text field for miscellaneous notes. */
	protected JTextField notes;

	/** Text area for overlay statistics. */
	protected JTextArea stats;

	// -- Constructor --

	/** Creates overlay GUI controls. */
	public OverlayWidget(final OverlayTransform overlay) {
		super();
		this.overlay = overlay;
		buttonGroup = new ButtonGroup();

		// list of tools
		final OverlayTool[] toolList =
			{ new PointerTool(overlay), new LineTool(overlay),
				new FreeformTool(overlay), new MarkerTool(overlay),
				new TextTool(overlay), new OvalTool(overlay), new BoxTool(overlay),
				new ArrowTool(overlay), new PolylineTool(overlay), };
		tools = new Vector(toolList.length);

		// tool buttons
		buttons = new Vector(toolList.length);
		for (int i = 0; i < toolList.length; i++)
			addTool(toolList[i]);
		final Object[] buttonList = new Object[buttons.size()];
		buttons.copyInto(buttonList);
		final JPanel toolsRow = FormsUtil.makeRow(buttonList);

		// file chooser for loading overlays
		overlayLoadBox = new JFileChooser();
		overlayLoadBox.addChoosableFileFilter(new ExtensionFileFilter(
			new String[] { "txt" }, "Overlay text files"));

		// file chooser for saving overlays
		overlaySaveBox = new JFileChooser();
		overlaySaveBox.addChoosableFileFilter(new ExtensionFileFilter(
			new String[] { "txt" }, "Overlay text files"));
		overlaySaveBox.setAccessory(new StatsOptionsPane());

		overlayXLSBox = new JFileChooser();
		overlayXLSBox.addChoosableFileFilter(new ExtensionFileFilter(
			new String[] { "xls" }, "Overlay spreadsheet files"));
		overlayXLSBox.setAccessory(new StatsOptionsPane());

		// current font text field
		currentFont = new JTextField();
		currentFont.setEditable(false);
		currentFont.setToolTipText("Font used for text overlays");
		refreshCurrentFont();

		// font chooser button
		chooseFont = new JButton("Change...");
		chooseFont.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) chooseFont.setMnemonic('h');
		chooseFont.setToolTipText("Configures font used for text overlays");
		final JPanel fontRow =
			FormsUtil.makeRow(new Object[] { "Font", currentFont, chooseFont },
				new boolean[] { false, true, false });

		// overlay list
		overlayListModel = new DefaultListModel();
		overlayList = new JList(overlayListModel);
		overlayList.addListSelectionListener(this);
		overlayList.setToolTipText("Overlays at the current dimensional position");
		final JScrollPane overlayScroll = new JScrollPane(overlayList);
		SwingUtil.configureScrollPane(overlayScroll);
		overlayScroll.setPreferredSize(new Dimension(120, 0));

		// text fields for (X, Y) coordinate pairs
		final int textWidth = 12;
		x1 = new JTextField(textWidth);
		y1 = new JTextField(textWidth);
		x2 = new JTextField(textWidth);
		y2 = new JTextField(textWidth);
		x1.setEnabled(false);
		y1.setEnabled(false);
		x2.setEnabled(false);
		y2.setEnabled(false);
		x1.getDocument().addDocumentListener(this);
		y1.getDocument().addDocumentListener(this);
		x2.getDocument().addDocumentListener(this);
		y2.getDocument().addDocumentListener(this);
		x1.setToolTipText("First X coordinate of selected overlays");
		y1.setToolTipText("First Y coordinate of selected overlays");
		x2.setToolTipText("Second X coordinate of selected overlays");
		y2.setToolTipText("Second Y coordinate of selected overlays");

		// text text field ;-)
		text = new JTextField(2 * textWidth);
		text.getDocument().addDocumentListener(this);
		text.setToolTipText("Text displayed for the selected text overlays");

		// color chooser
		color = new JButton();
		color.addActionListener(this);
		color.setBackground(Color.white);
		color.setToolTipText("The color of the selected overlays");

		// filled checkbox
		filled = new JCheckBox("Filled");
		filled.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) filled.setMnemonic('f');
		filled
			.setToolTipText("Whether the selected overlays are filled in or outlined");

		// group selector
		groupList = new BioComboBox();
		groupList.addItem("None");
		groupList.addActionListener(this);
		groupList
			.setToolTipText("The overlay group to which the selected overlays belong");

		// new group button
		newGroup = new JButton("New...");
		newGroup.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) newGroup.setMnemonic('n');
		newGroup.setToolTipText("Creates a new overlay group");

		// notes text field
		notes = new JTextField(2 * textWidth);
		notes.getDocument().addDocumentListener(this);
		notes
			.setToolTipText("Miscellaneous notes associated with the selected overlays");

		// stats text area
		stats = new JTextArea(7, 3 * textWidth);
		stats.setEditable(false);
		stats.setBorder(new EtchedBorder());
		stats.setToolTipText("Statistics for the selected overlay");

		// overlay removal button
		remove = new JButton("Remove");
		remove.setEnabled(false);
		remove.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) remove.setMnemonic('r');
		remove.setToolTipText("Deletes the selected overlays");

		// overlay copy button
		copy = new JButton("Copy");
		copy.setEnabled(false);
		copy.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) copy.setMnemonic('c');
		copy.setToolTipText("Copies selected overlays to the clipboard");

		// overlay paste button
		paste = new JButton("Paste");
		paste.setEnabled(false);
		paste.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) paste.setMnemonic('p');
		paste.setToolTipText("Pastes overlays from the clipboard");

		// overlay distribution button
		dist = new JButton("Distribute");
		dist.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) dist.setMnemonic('d');
		dist.setToolTipText("Distributes an overlay evenly between "
			+ "copied location and selected location");

		// overlay loading button
		load = new JButton("Load overlays...");
		load.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) load.setMnemonic('l');
		load.setToolTipText("Loads overlays from a text file on disk");

		// overlay saving button
		save = new JButton("Save overlays...");
		save.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) save.setMnemonic('s');
		save.setToolTipText("Saves overlays to a text file on disk");

		// overlay export button
		export = new JButton("Export overlays...");
		export.addActionListener(this);
		if (!LAFUtil.isMacLookAndFeel()) export.setMnemonic('e');
		export.setToolTipText("Exports overlays to a .xls file on disk");

		// lay out components
		setLayout(new BorderLayout());
		final FormLayout layout =
			new FormLayout(
				"pref, 5dlu, pref, 3dlu, pref:grow, 5dlu, pref, 3dlu, pref:grow",
				"pref, 3dlu, pref, 9dlu, pref, 3dlu, pref, 9dlu, pref, 3dlu, pref, "
					+ "3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref, "
					+ "3dlu, pref, 9dlu, pref, 3dlu, fill:pref:grow");
		final PanelBuilder builder = new PanelBuilder(layout);
		final CellConstraints cc = new CellConstraints();

		int row = 1;
		builder.addSeparator("Tools", cc.xyw(1, row, 9));
		row += 2;
		builder.add(toolsRow, cc.xyw(1, row, 9));
		row += 2;
		builder.addSeparator("Overlays", cc.xyw(1, row, 9));
		row += 2;
		builder.add(fontRow, cc.xyw(1, row, 9));
		row += 2;
		builder.add(overlayScroll, cc.xywh(1, row, 1, 11));
		builder.addLabel("X1", cc.xy(3, row));
		builder.add(x1, cc.xy(5, row));
		builder.addLabel("Y1", cc.xy(7, row));
		builder.add(y1, cc.xy(9, row));
		row += 2;
		builder.addLabel("X2", cc.xy(3, row));
		builder.add(x2, cc.xy(5, row));
		builder.addLabel("Y2", cc.xy(7, row));
		builder.add(y2, cc.xy(9, row));
		row += 2;
		builder.addLabel("Text", cc.xy(3, row));
		builder.add(text, cc.xyw(5, row, 5));
		row += 2;
		builder.addLabel("Color", cc.xy(3, row));
		builder.add(color, cc.xy(5, row, "fill, fill"));
		builder.add(filled, cc.xyw(7, row, 3));
		row += 2;
		builder.addLabel("Group", cc.xy(3, row));
		builder.add(groupList, cc.xy(5, row));
		builder.add(newGroup, cc.xyw(7, row, 3, "left, center"));
		row += 2;
		builder.addLabel("Notes", cc.xy(3, row));
		builder.add(notes, cc.xyw(5, row, 5));
		row += 2;
		builder.add(ButtonBarFactory.buildCenteredBar(new JButton[] { remove, copy,
			paste, dist }), cc.xyw(1, row, 9, "center, center"));
		row += 2;
		builder.add(ButtonBarFactory.buildCenteredBar(new JButton[] { load, save,
			export }), cc.xyw(1, row, 9, "center, center"));
		row += 2;
		builder.addSeparator("Statistics", cc.xyw(1, row, 9));
		row += 2;
		builder.add(stats, cc.xyw(1, row, 9));

		layout.setColumnGroups(new int[][] { { 5, 9 } });
		add(builder.getPanel());

		overlay.addTransformListener(this);

		// widget refresh timer
		refreshTimer = new Timer(200, this);
		refreshTimer.start();
	}

	// -- OverlayWidget API methods --

	/** Adds the given overlay tool to the overlay widget. */
	public void addTool(final OverlayTool tool) {
		final JToggleButton b =
			SwingUtil.makeToggleButton(this, tool.getIcon(), tool.getName(), 6, 6);
		b.setToolTipText(tool.getTip());
		buttonGroup.add(b);
		buttons.add(b);
		tools.add(tool);
	}

	/** Gets the currently selected overlay tool. */
	public OverlayTool getActiveTool() {
		final int size = buttons.size();
		for (int i = 0; i < size; i++) {
			final JToggleButton b = (JToggleButton) buttons.elementAt(i);
			if (b.isSelected()) return (OverlayTool) tools.elementAt(i);
		}
		return null;
	}

	/** Gets X1 coordinate value for current overlay. */
	public float getX1() {
		return Float.parseFloat(x1.getText());
	}

	/** Gets Y1 coordinate value for current overlay. */
	public float getY1() {
		return Float.parseFloat(y1.getText());
	}

	/** Gets X2 coordinate value for current overlay. */
	public float getX2() {
		return Float.parseFloat(x2.getText());
	}

	/** Gets Y2 coordinate value for current overlay. */
	public float getY2() {
		return Float.parseFloat(y2.getText());
	}

	/** Gets text string for current overlay. */
	public String getText() {
		return text.getText();
	}

	/** Sets currently active overlay color. */
	public void setActiveColor(final Color c) {
		color.setForeground(c);
		color.setBackground(c);
		// user updated Color button
		final Object[] sel = overlayList.getSelectedValues();
		for (int i = 0; i < sel.length; i++)
			((OverlayObject) sel[i]).setColor(c);
		if (sel.length > 0) notifyListeners(false);
	}

	/** Gets currently active overlay color. */
	public Color getActiveColor() {
		return color.getBackground();
	}

	/** Sets whether current overlay is filled. */
	public void setFilled(final boolean fill) {
		filled.setSelected(fill);
	}

	/** Gets whether current overlay is filled. */
	public boolean isFilled() {
		return filled.isSelected();
	}

	/** Sets currently active overlay group. */
	public void setActiveGroup(final String group) {
		final DefaultComboBoxModel model =
			(DefaultComboBoxModel) groupList.getModel();
		if (model.getIndexOf(group) < 0) groupList.addItem(group);
		groupList.setSelectedItem(group);
	}

	/** Gets currently active overlay group. */
	public String getActiveGroup() {
		return (String) groupList.getSelectedItem();
	}

	/** Sets notes for current overlay. */
	public void setNotes(final String newText) {
		notes.setText(newText);
	}

	/** Gets notes for current overlay. */
	public String getNotes() {
		return notes.getText();
	}

	/** Sets statistics for current overlay. */
	public void setStatistics(final String newText) {
		stats.setText(newText);
	}

	/** Gets statistics for current overlay. */
	public String getStatistics() {
		return stats.getText();
	}

	/** Updates items on overlay list based on current transform state. */
	public void refreshListObjects() {
		Util.invoke(false, this);
	}

	/** Updates overlay list's selection based on current transform state. */
	public void refreshListSelection() {
		final OverlayObject[] obj = overlay.getObjects();
		int sel = 0;
		if (obj != null) {
			for (int i = 0; i < obj.length; i++) {
				if (obj[i].isSelected()) sel++;
			}
		}
		final int[] indices = new int[sel];
		int c = 0;
		for (int i = 0; c < sel && i < obj.length; i++) {
			if (obj[i].isSelected()) indices[c++] = i;
		}
		ignoreEvents = true;
		overlayList.setSelectedIndices(indices);
		final boolean hasSelection = sel > 0;
		remove.setEnabled(hasSelection);
		copy.setEnabled(hasSelection);
		ignoreEvents = false;
		needRefresh = true;
	}

	/**
	 * Updates right-hand widget components to display data relevant to currently
	 * selected overlays.
	 */
	public void refreshWidgetComponents() {
		Object[] sel = overlayList.getSelectedValues();
		int numDraw = 0;
		for (int i = 0; i < sel.length; i++) {
			final OverlayObject obj = (OverlayObject) sel[i];
			if (obj.isDrawing()) numDraw++;
		}
		if (numDraw > 0) {
			// if any overlays are still being drawn, concentrate on those only
			final Object[] draw = new Object[numDraw];
			int c = 0;
			for (int i = 0; i < sel.length && c < numDraw; i++) {
				final OverlayObject obj = (OverlayObject) sel[i];
				if (obj.isDrawing()) draw[c++] = obj;
			}
			sel = draw;
		}
		boolean enableXY1 = false, enableXY2 = false;
		boolean editXY1 = false, editXY2 = false;
		String xval1 = "", yval1 = "", xval2 = "", yval2 = "";
		String words = "";
		boolean fill = true;
		boolean enableFill = true;
		boolean enableText = true;
		Color col = null;
		String grp = null;
		String note = null;
		String stat = "";
		for (int i = 0; i < sel.length; i++) {
			final OverlayObject obj = (OverlayObject) sel[i];

			// if any selected overlay is not filled, clear filled checkbox
			if (!obj.isFilled()) fill = false;

			// if any selected overlay cannot be filled, disable filled checkbox
			if (!obj.canBeFilled()) enableFill = false;

			// if any selected overlay is not a text object, disable text box
			if (!obj.hasText()) enableText = false;

			if (i == 0) {
				// fill in values based on parameters of first selected overlay
				enableXY1 = obj.hasEndpoint();
				enableXY2 = obj.hasEndpoint2();
				editXY1 = obj.areBoundsEditable();
				editXY2 = obj.areBoundsEditable();
				if (enableXY1) xval1 = "" + obj.getX();
				if (enableXY1) yval1 = "" + obj.getY();
				if (enableXY2) xval2 = "" + obj.getX2();
				if (enableXY2) yval2 = "" + obj.getY2();
				if (obj.hasText()) words = obj.getText();
				col = obj.getColor();
				grp = obj.getGroup();
				note = obj.getNotes();
				stat = obj.getStatistics();
			}
			else {
				// multiple overlays selected; disable coordinate boxes
				enableXY1 = enableXY2 = false;
				editXY1 = editXY2 = false;

				xval1 = yval1 = xval2 = yval2 = "";

				// if parameters do not match, reset to defaults
				if (!ObjectUtil.objectsEqual(obj.getText(), words)) words = "";
				if (!ObjectUtil.objectsEqual(obj.getColor(), col)) col = Color.white;
				if (!ObjectUtil.objectsEqual(obj.getGroup(), grp)) grp = "None";
				if (!ObjectUtil.objectsEqual(obj.getNotes(), note)) note = "";

				stat = "Multiple overlays selected";
			}
		}

		// update GUI components based on computed values
		ignoreEvents = true;
		x1.setEnabled(enableXY1);
		y1.setEnabled(enableXY1);
		x2.setEnabled(enableXY2);
		y2.setEnabled(enableXY2);
		x1.setEditable(editXY1);
		y1.setEditable(editXY1);
		x2.setEditable(editXY2);
		y2.setEditable(editXY2);
		x1.setText(xval1);
		y1.setText(yval1);
		x2.setText(xval2);
		y2.setText(yval2);
		filled.setEnabled(enableFill);
		text.setEnabled(enableText);
		text.setEditable(enableText);
		text.setText(words);

		if (sel.length > 0) {
			// leave GUI components alone if nothing is selected
			filled.setSelected(fill);
			color.setBackground(col);
			groupList.setSelectedItem(grp);
			notes.setText(note);
		}
		stats.setText(stat);
		ignoreEvents = false;
	}

	/**
	 * Updates copy and paste widget components to display data relevant to
	 * currently selected overlays.
	 */
	public void refreshPasteComponent(final boolean enabled) {
		paste.setEnabled(enabled);
	}

	// -- ActionListener API methods --

	/** Handles button presses. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		if (ignoreEvents) return;
		final Object src = e.getSource();
		if (src == refreshTimer) {
			if (needRefresh) {
				refreshWidgetComponents();
				needRefresh = false;
			}
		}
		else if (src == chooseFont) {
			final FontChooserPane fcp = new FontChooserPane(overlay.getFont());
			final int rval = fcp.showDialog(this);
			if (rval == DialogPane.APPROVE_OPTION) {
				final Font font = fcp.getSelectedFont();
				if (font != null) overlay.setFont(font);
			}
		}
		else if (src == color) {
			Color c = getActiveColor();
			c = JColorChooser.showDialog(this, "Select a color", c);
			if (c != null) setActiveColor(c);
		}
		else if (src == filled) {
			// user updated Filled checkbox
			final boolean f = filled.isSelected();
			final Object[] sel = overlayList.getSelectedValues();
			for (int i = 0; i < sel.length; i++)
				((OverlayObject) sel[i]).setFilled(f);
			if (sel.length > 0) notifyListeners(false);
		}
		else if (src == groupList) {
			// user updated Group combo box
			final String g = (String) groupList.getSelectedItem();
			final Object[] sel = overlayList.getSelectedValues();
			for (int i = 0; i < sel.length; i++)
				((OverlayObject) sel[i]).setGroup(g);
			if (sel.length > 0) notifyListeners(false);
		}
		else if (src == newGroup) {
			final String nextGroup = "group" + groupList.getItemCount();
			final String group =
				(String) JOptionPane.showInputDialog(this, "Group name:",
					"Create overlay group", JOptionPane.INFORMATION_MESSAGE, null, null,
					nextGroup);
			if (group != null) setActiveGroup(group);
		}
		else if (src == remove) overlay.removeSelectedObjects();
		else if (src == copy) overlay.copySelectedObjects();
		else if (src == paste) overlay.pasteObjects();
		else if (src == dist) {
			final String err = overlay.distributeObjects();
			if (err != null) {
				JOptionPane.showMessageDialog(this, err, "Cannot distribute overlays",
					JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (src == load) {
			final int rval = overlayLoadBox.showOpenDialog(this);
			if (rval != JFileChooser.APPROVE_OPTION) return;
			final File file = overlayLoadBox.getSelectedFile();
			try {
				final BufferedReader fin = new BufferedReader(new FileReader(file));
				overlay.loadOverlays(fin);
				fin.close();
			}
			catch (final IOException exc) {
				JOptionPane.showMessageDialog(this, "Error loading overlay file " +
					file + "): " + exc.getMessage(), "Cannot load overlays",
					JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (src == save) {
			final StatsOptionsPane statsPane =
				(StatsOptionsPane) overlaySaveBox.getAccessory();
			statsPane.loadSettings();

			// set default filename to a reasonable default
			String defaultName = overlay.getParent().getName();
			defaultName = defaultName.replaceAll("[^\\w]", ""); // sanitize name
			overlaySaveBox.setSelectedFile(new File(defaultName + ".txt"));

			final int rval = overlaySaveBox.showSaveDialog(this);
			if (rval != JFileChooser.APPROVE_OPTION) return;
			final File file = overlaySaveBox.getSelectedFile();
			statsPane.saveSettings();
			try {
				final PrintWriter fout = new PrintWriter(new FileWriter(file));
				overlay.saveOverlays(fout);
				fout.close();
			}
			catch (final IOException exc) {
				JOptionPane.showMessageDialog(this, "Error saving overlay file " +
					file + "): " + exc.getMessage(), "Cannot save overlays",
					JOptionPane.ERROR_MESSAGE);
			}
		}
		else if (src == export) {
			final StatsOptionsPane statsPane =
				(StatsOptionsPane) overlayXLSBox.getAccessory();
			statsPane.loadSettings();

			// set default filename to a reasonable default
			String defaultName = overlay.getParent().getName();
			defaultName = defaultName.replaceAll("[^\\w]", ""); // sanitize name
			overlayXLSBox.setSelectedFile(new File(defaultName + ".xls"));

			final int rval = overlayXLSBox.showDialog(this, "Export");
			if (rval != JFileChooser.APPROVE_OPTION) return;
			final File file = overlayXLSBox.getSelectedFile();
			statsPane.saveSettings();
			try {
				final FileOutputStream fout = new FileOutputStream(file);
				final HSSFWorkbook wb = overlay.exportOverlays();
				wb.write(fout); // TODO use a task here... this can be slow for
				// really big overlay sets
				fout.close();
				launchSpreadsheet(file);
			}
			catch (final IOException exc) {
				// TODO add error message or something more sophisticated?
				exc.printStackTrace();
			}
		}
	}

	// -- DocumentListener API methods --

	@Override
	public void changedUpdate(final DocumentEvent e) {
		documentUpdate(e);
	}

	@Override
	public void insertUpdate(final DocumentEvent e) {
		documentUpdate(e);
	}

	@Override
	public void removeUpdate(final DocumentEvent e) {
		documentUpdate(e);
	}

	/** Handles text field changes. */
	public void documentUpdate(final DocumentEvent e) {
		if (ignoreEvents) return;
		final Document src = e.getDocument();
		if (src == x1.getDocument()) {
			// user updated X1 text field
			float v = Float.NaN;
			try {
				v = Float.parseFloat(x1.getText());
			}
			catch (final NumberFormatException exc) {}
			if (Float.isNaN(v)) return;
			final Object[] sel = overlayList.getSelectedValues();
			for (int i = 0; i < sel.length; i++)
				((OverlayObject) sel[i]).setX(v);
			if (sel.length > 0) notifyListeners(false);
		}
		else if (src == y1.getDocument()) {
			// user updated Y1 text field
			float v = Float.NaN;
			try {
				v = Float.parseFloat(y1.getText());
			}
			catch (final NumberFormatException exc) {}
			if (Float.isNaN(v)) return;
			final Object[] sel = overlayList.getSelectedValues();
			for (int i = 0; i < sel.length; i++)
				((OverlayObject) sel[i]).setY(v);
			if (sel.length > 0) notifyListeners(false);
		}
		else if (src == x2.getDocument()) {
			// user updated X2 text field
			float v = Float.NaN;
			try {
				v = Float.parseFloat(x2.getText());
			}
			catch (final NumberFormatException exc) {}
			if (Float.isNaN(v)) return;
			final Object[] sel = overlayList.getSelectedValues();
			for (int i = 0; i < sel.length; i++)
				((OverlayObject) sel[i]).setX2(v);
			if (sel.length > 0) notifyListeners(false);
		}
		else if (src == y2.getDocument()) {
			// user updated Y2 text field
			float v = Float.NaN;
			try {
				v = Float.parseFloat(y2.getText());
			}
			catch (final NumberFormatException exc) {}
			if (Float.isNaN(v)) return;
			final Object[] sel = overlayList.getSelectedValues();
			for (int i = 0; i < sel.length; i++)
				((OverlayObject) sel[i]).setY2(v);
			if (sel.length > 0) notifyListeners(false);
		}
		else if (src == text.getDocument()) {
			// user updated Text text field
			final String t = text.getText();
			final Object[] sel = overlayList.getSelectedValues();
			for (int i = 0; i < sel.length; i++)
				((OverlayObject) sel[i]).setText(t);
			if (sel.length > 0) notifyListeners(false);
		}
		else if (src == notes.getDocument()) {
			// user updated Notes text field
			final String n = notes.getText();
			final Object[] sel = overlayList.getSelectedValues();
			for (int i = 0; i < sel.length; i++)
				((OverlayObject) sel[i]).setNotes(n);
			if (sel.length > 0) notifyListeners(false);
		}
	}

	// -- ListSelectionListener API methods --

	/** Handles list selection changes. */
	@Override
	public void valueChanged(final ListSelectionEvent e) {
		if (ignoreEvents) return;
		final OverlayObject[] obj = overlay.getObjects();
		boolean[] selected = null;
		if (obj != null) {
			selected = new boolean[obj.length];

			// deselect all previously selected overlays
			for (int i = 0; i < obj.length; i++) {
				selected[i] = obj[i].isSelected();
				obj[i].setSelected(false);
			}
		}

		// select highlighted overlays
		final Object[] sel = overlayList.getSelectedValues();
		for (int i = 0; i < sel.length; i++) {
			((OverlayObject) sel[i]).setSelected(true);
		}

		boolean changed = false;
		if (obj != null) {
			for (int i = 0; i < obj.length; i++) {
				if (selected[i] != obj[i].isSelected()) {
					changed = true;
					break;
				}
			}
		}
		if (changed) {
			needRefresh = true;
			remove.setEnabled(sel.length > 0);
			notifyListeners(true);
		}
	}

	// -- Runnable API methods --

	/** Refreshes the list selection. */
	@Override
	public void run() {
		final OverlayObject[] obj = overlay.getObjects();
		ignoreEvents = true;
		overlayListModel.clear();
		if (obj != null) {
			overlayListModel.ensureCapacity(obj.length);
			for (int i = 0; i < obj.length; i++)
				overlayListModel.addElement(obj[i]);
		}
		ignoreEvents = false;
		refreshListSelection();
	}

	// -- TransformListener API methods --

	/** Handles font changes. */
	@Override
	public void transformChanged(final TransformEvent e) {
		final int id = e.getId();
		if (id == TransformEvent.DATA_CHANGED) {
			if (!ignoreEvents) needRefresh = true;
		}
		else if (id == TransformEvent.FONT_CHANGED) refreshCurrentFont();
	}

	// -- Helper methods --

	/** Sets the font text field to reflect the currently chosen font. */
	protected void refreshCurrentFont() {
		final Font font = overlay.getFont();
		String size = "" + font.getSize2D();
		if (size.endsWith(".0")) size = size.substring(0, size.length() - 2);
		String s = font.getFamily() + " " + size;
		if (font.isBold()) s += " Bold";
		if (font.isItalic()) s += " Italic";
		currentFont.setText(s);
	}

	/** Sends a TransformEvent to the overlay transform's listeners. */
	protected void notifyListeners(final boolean updateGUI) {
		if (!updateGUI) ignoreEvents = true;
		overlay.notifyListeners(new TransformEvent(overlay));
		if (!updateGUI) ignoreEvents = false;
	}

	/** Launches the spreadsheet. */
	protected void launchSpreadsheet(final File file) {
		final OptionManager om =
			(OptionManager) VisBioFrame.getVisBio().getManager(OptionManager.class);
		final SpreadsheetLaunchOption opt =
			(SpreadsheetLaunchOption) om.getOption(SpreadsheetOptionStrategy
				.getText());
		if (opt.getSelected()) {
			try {
				final SpreadsheetLauncher launcher = new SpreadsheetLauncher();
				launcher.launchSpreadsheet(file, opt.getValue());
			}
			catch (final SpreadsheetLaunchException ex) {
				displayErrorMessage(ex.getMessage());
			}
		}
	}

	/** Displays an error message. */
	protected void displayErrorMessage(final String message) {
		JOptionPane.showMessageDialog(this, message,
			"Could not launch spreadsheet", JOptionPane.ERROR_MESSAGE);
	}
}

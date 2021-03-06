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
import com.jgoodies.looks.LookUtils;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * FontChooser is a dialog for graphical font selection. (Java comes with a file
 * chooser and a color chooser, but no font chooser... why not?)
 */
public class FontChooserPane extends DialogPane implements ChangeListener {

	// -- Constants -

	/** Default font. */
	public static final Font DEFAULT_FONT = new Font("Default", Font.PLAIN, 11);

	/** Default text used in font preview pane. */
	protected static final String DEFAULT_PREVIEW_TEXT =
		"\"The most common of all follies is to believe passionately in the "
			+ "palpably not true. It is the chief occupation of mankind.\"\n"
			+ "--H. L. Mencken";

	// -- Fields --

	/** Dropdown combo box containing available font names. */
	protected BioComboBox fontName;

	/** Checkbox for indicating font should be in bold face. */
	protected JCheckBox fontBold;

	/** Checkbox for indicating font should be in italics. */
	protected JCheckBox fontItalic;

	/** Spinner for selecting font size. */
	protected JSpinner fontSize;

	/** Text area containing preview text with the chosen font settings. */
	protected JTextArea previewText;

	// -- Constructors --

	/** Creates a new font chooser pane. */
	public FontChooserPane() {
		this(DEFAULT_FONT, DEFAULT_PREVIEW_TEXT);
	}

	/** Creates a new font chooser pane with the given initial font. */
	public FontChooserPane(final Font font) {
		this(font, DEFAULT_PREVIEW_TEXT);
	}

	/** Creates a new font chooser pane with the given preview text. */
	public FontChooserPane(final String preview) {
		this(DEFAULT_FONT, preview);
	}

	/**
	 * Creates a new font chooser apne with the given initial font and preview
	 * text.
	 */
	public FontChooserPane(final Font font, final String preview) {
		super("Font Chooser", true);

		final GraphicsEnvironment e =
			GraphicsEnvironment.getLocalGraphicsEnvironment();
		String[] fontNames = e.getAvailableFontFamilyNames();

		// HACK - do not display "L" style fonts, because
		// selecting them crashes the JVM, at least on some systems
		if (LookUtils.IS_OS_LINUX) {
			int count = 0;
			for (int i = 0; i < fontNames.length; i++) {
				if (!fontNames[i].endsWith(" L")) count++;
			}
			int c = 0;
			final String[] filteredNames = new String[count];
			for (int i = 0; i < fontNames.length && c < count; i++) {
				if (!fontNames[i].endsWith(" L")) filteredNames[c++] = fontNames[i];
			}
			fontNames = filteredNames;
		}

		int ndx = -1;
		final String family = font.getFamily();
		for (int i = 0; i < fontNames.length; i++) {
			if (fontNames[i].equals(family)) {
				ndx = i;
				break;
			}
		}
		fontName = new BioComboBox(fontNames);
		fontName.setSelectedIndex(ndx);
		fontName.addActionListener(this);

		fontBold = new JCheckBox("Bold", font.isBold());
		if (!LAFUtil.isMacLookAndFeel()) fontBold.setMnemonic('b');
		fontBold.addActionListener(this);

		fontItalic = new JCheckBox("Italic", font.isItalic());
		if (!LAFUtil.isMacLookAndFeel()) fontItalic.setMnemonic('i');
		fontItalic.addActionListener(this);

		final SpinnerNumberModel fontSizeModel =
			new SpinnerNumberModel(font.getSize2D(), 1, 150, 0.5);
		fontSize = new JSpinner(fontSizeModel);
		fontSize.addChangeListener(this);

		previewText = new JTextArea(preview, 3, 2);
		previewText.setFont(font);
		previewText.setWrapStyleWord(true);
		previewText.setLineWrap(true);

		// HACK - workaround for problem where JTextArea's
		// preferred size changes when line wrapping is on
		previewText.setMinimumSize(new Dimension(100, 32));

		final JScrollPane previewScroll = new JScrollPane(previewText);
		SwingUtil.configureScrollPane(previewScroll);

		// lay out components
		final PanelBuilder builder =
			new PanelBuilder(
				new FormLayout(
					"pref, 3dlu, pref:grow, 3dlu, pref, 3dlu, pref, 3dlu, pref, 3dlu, pref",
					"pref, 9dlu, pref, 3dlu, fill:70:grow"));
		final CellConstraints cc = new CellConstraints();
		final JLabel fontNameLabel = builder.addLabel("Name", cc.xy(1, 1));
		fontNameLabel.setLabelFor(fontName);
		if (!LAFUtil.isMacLookAndFeel()) fontNameLabel.setDisplayedMnemonic('n');
		builder.add(fontName, cc.xy(3, 1));
		final JLabel fontSizeLabel = builder.addLabel("Size", cc.xy(5, 1));
		fontSizeLabel.setLabelFor(fontSize);
		if (!LAFUtil.isMacLookAndFeel()) fontSizeLabel.setDisplayedMnemonic('s');
		builder.add(fontSize, cc.xy(7, 1));
		builder.add(fontBold, cc.xy(9, 1));
		builder.add(fontItalic, cc.xy(11, 1));
		builder.addSeparator("Preview", cc.xyw(1, 3, 11));
		builder.add(previewScroll, cc.xyw(1, 5, 11));
		add(builder.getPanel());
	}

	// -- FontChooserPane API methods --

	/** Gets the font indicated by the font chooser. */
	public Font getSelectedFont() {
		return previewText.getFont();
	}

	// -- ActionListener API methods --

	/** Handles GUI events. */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final Object o = e.getSource();
		if (o == fontName || o == fontBold || o == fontItalic) refreshFont();
		super.actionPerformed(e);
	}

	// -- ChangeListener API methods --

	/** Handles GUI events. */
	@Override
	public void stateChanged(final ChangeEvent e) {
		refreshFont();
	}

	// -- Helper methods --

	/** Refreshes font to match latest GUI state. */
	protected void refreshFont() {
		final String name = (String) fontName.getSelectedItem();

		final boolean bold = fontBold.isSelected();
		final boolean italic = fontItalic.isSelected();
		int style;
		if (bold && italic) style = Font.BOLD | Font.ITALIC;
		else if (bold) style = Font.BOLD;
		else if (italic) style = Font.ITALIC;
		else style = Font.PLAIN;

		final float size = ((Double) fontSize.getValue()).floatValue();

		final Font font = new Font(name, style, 1).deriveFont(size);
		previewText.setFont(font);
	}

}

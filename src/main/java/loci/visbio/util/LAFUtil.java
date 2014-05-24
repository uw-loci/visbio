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

import com.jgoodies.looks.LookUtils;
import com.jgoodies.looks.Options;

import java.awt.Dimension;
import java.util.Vector;

import javax.swing.LookAndFeel;
import javax.swing.UIManager;

/**
 * LAFUtil contains useful functions relating to Look and Feel.
 */
public final class LAFUtil {

	// -- Constructor --

	private LAFUtil() {}

	// -- Utility methods --

	/** Initializes some look and feel parameters. */
	public static void initLookAndFeel() {
		if (System.getProperty("swing.defaultlaf") == null) {
			// use JGoodies Plastic 3D as default if no Look & Feel is set
			try {
				UIManager
					.setLookAndFeel("com.jgoodies.looks.plastic.Plastic3DLookAndFeel");
			}
			catch (final Exception exc) {
				exc.printStackTrace();
			}
		}

		UIManager.put(Options.USE_SYSTEM_FONTS_APP_KEY, Boolean.TRUE);
		Options.setDefaultIconSize(new Dimension(18, 18));

		if (!isMacLookAndFeel()) {
			System.setProperty("apple.laf.useScreenMenuBar", "false");
		}
		UIManager.installLookAndFeel("JGoodies Windows",
			"com.jgoodies.plaf.windows.ExtWindowsLookAndFeel");
		UIManager.installLookAndFeel("JGoodies Plastic",
			"com.jgoodies.plaf.plastic.PlasticLookAndFeel");
		UIManager.installLookAndFeel("JGoodies Plastic 3D",
			"com.jgoodies.plaf.plastic.Plastic3DLookAndFeel");
		UIManager.installLookAndFeel("JGoodies Plastic XP",
			"com.jgoodies.plaf.plastic.PlasticXPLookAndFeel");
	}

	/** Gets the name and class of the current look and feel. */
	public static String[] getLookAndFeel() {
		final LookAndFeel laf = UIManager.getLookAndFeel();
		return new String[] { laf.getName(), laf.getClass().getName() };
	}

	/** Gets whether the current look and feel is Mac OS X native. */
	public static boolean isMacLookAndFeel() {
		final LookAndFeel laf = UIManager.getLookAndFeel();
		return laf.getClass().getName().equals("apple.laf.AquaLookAndFeel");
	}

	/** Gets whether the current look and feel is Windows native. */
	public static boolean isWindowsLookAndFeel() {
		final LookAndFeel laf = UIManager.getLookAndFeel();
		return laf.getClass().getName().indexOf("Windows") >= 0;
	}

	/** Gets whether the current look and feel is GTK+. */
	public static boolean isGTKLookAndFeel() {
		final LookAndFeel laf = UIManager.getLookAndFeel();
		return laf.getClass().getName().indexOf("GTK") >= 0;
	}

	/**
	 * Gets whether the current look and feel is one of the JGoodies Plastic
	 * series.
	 */
	public static boolean isPlasticLookAndFeel() {
		final LookAndFeel laf = UIManager.getLookAndFeel();
		return laf.getClass().getName().startsWith(
			"com.jgoodies.plaf.plastic.Plastic");
	}

	/**
	 * Gets list of available look and feels, taking some OS-specific look and
	 * feels into account.
	 * 
	 * @return An array dimensioned String[2][*], with String[0] being the L&F
	 *         names, and String[1] being the fully qualified L&F class names.
	 */
	public static String[][] getAvailableLookAndFeels() {
		final UIManager.LookAndFeelInfo[] lafs =
			UIManager.getInstalledLookAndFeels();
		final Vector v = new Vector(lafs.length);
		for (int i = 0; i < lafs.length; i++) {
			final String cname = lafs[i].getClassName();
			if (cname.indexOf("WindowsLookAndFeel") < 0 || LookUtils.IS_OS_WINDOWS) {
				v.add(lafs[i]);
			}
		}

		final int size = v.size();
		final String[][] s = new String[2][size];
		for (int i = 0; i < size; i++) {
			final UIManager.LookAndFeelInfo info =
				(UIManager.LookAndFeelInfo) v.elementAt(i);
			s[0][i] = info.getName();
			s[1][i] = info.getClassName();
		}
		return s;
	}

}

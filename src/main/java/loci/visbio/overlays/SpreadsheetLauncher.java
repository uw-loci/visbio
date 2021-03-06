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

import com.jgoodies.looks.LookUtils;

import java.io.File;
import java.io.IOException;

/**
 * Launches an external spreadsheet application.
 */
public class SpreadsheetLauncher {

	// -- Constants --

	/** Default path to spreadsheet (Excel) on Windows computers. */
	protected static final String WIN_PATH =
		"C:\\Program Files\\Microsoft Office\\excel.exe";

	/** Default path to spreadsheet (OpenOffice) on Linux computers. */
	protected static final String LIN_PATH = "/usr/bin/oocalc";

	/** Default path to spreadsheet (Excel) on Macintosh computers. */
	protected static final String MAC_PATH =
		"/Applications/Microsoft Office 2004/Microsoft Excel";

	// -- Fields --

	/** Path to spreadsheet executable. */
	protected final String path;

	// -- Constructor --

	/** Constructs a spreadsheet launcher. */
	public SpreadsheetLauncher() throws SpreadsheetLaunchException {
		path = getDefaultApplicationPath();
	}

	// -- Static SpreadsheetLauncher API methods

	/** Returns the default spreadsheet application path for the current OS. */
	public static String getDefaultApplicationPath()
		throws SpreadsheetLaunchException
	{
		String def = "";
		if (isWindows()) def = WIN_PATH;
		else if (isLinux()) def = LIN_PATH;
		else if (isMac()) def = MAC_PATH;
		else {
			throw new SpreadsheetLaunchException(makeCantIdentifyOSMessage());
		}
		return def;
	}

	// -- SpreadsheetLauncher API methods --

	/** Tries to launch the appropriate spreadsheet application. */
	public void launchSpreadsheet(final File file)
		throws SpreadsheetLaunchException
	{
		launchSpreadsheet(file, path);
	}

	/** Tries to launch the appropriate spreadsheet application. */
	public void launchSpreadsheet(final File file, final String appPath)
		throws SpreadsheetLaunchException
	{
		if (file.exists()) {
			final String cmd = appPath;
			final String filePath = file.getAbsolutePath();
			final String[] command = { cmd, filePath };
			try {
				Runtime.getRuntime().exec(command);
			}
			catch (final IOException ex) {
				throw new SpreadsheetLaunchException(makeCommandErrorMessage(cmd));
			}
		}
		else {
			throw new SpreadsheetLaunchException(makeFileDoesNotExistMessage(file));
		}
	}

	// -- Helper methods --

	/** Whether the OS is windows. */
	protected static boolean isWindows() {
		return LookUtils.IS_OS_WINDOWS_MODERN;
	}

	/** Whether the OS is mac. */
	protected static boolean isMac() {
		return LookUtils.IS_OS_MAC;
	}

	/** Whether OS is Linux. */
	protected static boolean isLinux() {
		return LookUtils.IS_OS_LINUX;
	}

	/** Makes an error message from the given command. */
	protected String makeCommandErrorMessage(final String command) {
		final String msg =
			"Could not launch spreadsheet using the following command:\n\t" +
				command + "\nYou may wish to change the spreadsheet application path" +
				" in the 'File > Options...' menu.";
		return msg;
	}

	/** Makes an error message from the given file. */
	protected String makeFileDoesNotExistMessage(final File file) {
		return "Could not launch spreadsheet.  File does not exist:\n\t" +
			file.getAbsolutePath();
	}

	/** Returns an error message indicating the OS could not be identified. */
	protected static String makeCantIdentifyOSMessage() {
		return "Could not launch spreadsheet: could not identify OS.";
	}
}

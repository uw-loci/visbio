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

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;

import loci.visbio.data.DataManager;
import loci.visbio.data.DataTransform;
import loci.visbio.data.Dataset;
import loci.visbio.state.StateManager;

/**
 * BioDropHandler provides drag and drop support for handling file drops into
 * VisBio.
 */
public class BioDropHandler extends TransferHandler {

	// -- Fields --

	/** Associated VisBio frame. */
	private final VisBioFrame bio;

	// -- Constructor --

	/** Constructs a new transfer handler for VisBio file drops. */
	public BioDropHandler(final VisBioFrame bio) {
		super();
		this.bio = bio;
	}

	// -- TransferHandler API methods --

	/** Determines whether a drop operation is legal with the given flavors. */
	@Override
	public boolean canImport(final JComponent comp, final DataFlavor[] flavors) {
		bio.toFront(); // be aggressive!
		for (int i = 0; i < flavors.length; i++) {
			if (flavors[i].isFlavorJavaFileListType()) return true;
		}
		return false;
	}

	/** Performs a drop operation with the given transferable component. */
	@Override
	public boolean importData(final JComponent comp, final Transferable t) {
		if (!t.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false;

		List files;
		try {
			files = (List) t.getTransferData(DataFlavor.javaFileListFlavor);
		}
		catch (final UnsupportedFlavorException exc) {
			return false;
		}
		catch (final IOException exc) {
			return false;
		}
		if (files.size() < 1) return false;

		final File file = (File) files.get(0);
		if (file.getPath().toLowerCase().endsWith(".txt")) {
			// assume file is a VisBio state file
			final StateManager sm = (StateManager) bio.getManager(StateManager.class);
			if (sm == null) return false;
			sm.restoreState(file);
		}
		else {
			// assume file is part of a dataset
			final DataManager dm = (DataManager) bio.getManager(DataManager.class);
			if (dm == null) return false;
			final DataTransform data = Dataset.makeTransform(dm, file, bio);
			if (data != null) dm.addData(data);
		}
		return true;
	}

}

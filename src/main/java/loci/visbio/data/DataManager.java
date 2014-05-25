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

import java.awt.Component;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.tree.DefaultMutableTreeNode;

import loci.common.StatusEvent;
import loci.common.StatusListener;
import loci.formats.FilePattern;
import loci.visbio.BioTask;
import loci.visbio.LogicManager;
import loci.visbio.PanelManager;
import loci.visbio.TaskManager;
import loci.visbio.VisBioEvent;
import loci.visbio.VisBioFrame;
import loci.visbio.help.HelpManager;
import loci.visbio.state.BooleanOption;
import loci.visbio.state.OptionManager;
import loci.visbio.state.ResolutionOption;
import loci.visbio.state.SaveException;
import loci.visbio.state.StateManager;
import loci.visbio.util.XMLUtil;

import org.w3c.dom.Element;

/**
 * DataManager is the manager encapsulating VisBio's data transform logic.
 */
public class DataManager extends LogicManager {

	// -- Constants --

	/** URL prefix for sample datasets. */
	protected static final String SAMPLE_PREFIX =
		"http://www.loci.wisc.edu/files/software/data/";

	/** Default resolution for low-resolution thumbnails. */
	protected static final int DEFAULT_THUMBNAIL_RESOLUTION = 96;

	/** String for thumbnail auto-generation option. */
	public static final String AUTO_THUMBS = "Automatically generate thumbnails";

	/** String for thumbnail resolution option. */
	public static final String THUMB_RES = "Thumbnail resolution";

	// -- Control panel --

	/** Datasets control panel. */
	protected DataControls dataControls;

	// -- Other fields --

	/** List of registered data transform type classes. */
	protected Vector<Class<?>> transformTypes;

	/** List of registered data transform type labels. */
	protected Vector<String> transformLabels;

	// -- Constructor --

	/** Constructs a dataset manager. */
	public DataManager(final VisBioFrame bio) {
		super(bio);
	}

	// -- DataManager API methods --

	/** Adds a data object to the list. */
	public void addData(final DataTransform data) {
		dataControls.addData(data);
		final ThumbnailHandler thumbHandler = data.getThumbHandler();
		if (thumbHandler != null) {
			final TaskManager tm = (TaskManager) bio.getManager(TaskManager.class);
			thumbHandler.setTaskManager(tm);
		}
		bio.generateEvent(this, "add data", true);
	}

	/** Removes a data object from the list. */
	public void removeData(final DataTransform data) {
		removeData(data, false);
	}

	/** Removes a data object from the list. */
	public void removeData(final DataTransform data, final boolean confirm) {
		final boolean success = dataControls.removeData(data, confirm);
		if (success) bio.generateEvent(this, "remove data", true);
	}

	/** Gets the root node of the data object tree. */
	public DefaultMutableTreeNode getDataRoot() {
		return dataControls.getDataRoot();
	}

	/** Gets the currently selected data object. */
	public DataTransform getSelectedData() {
		return dataControls.getSelectedData();
	}

	/** Shows dialog containing controls for the given data object. */
	public void showControls(final DataTransform data) {
		dataControls.showControls(data);
	}

	/**
	 * Registers the given subclass of DataTransform with the data manager, using
	 * the given label as a description.
	 */
	public void registerDataType(final Class<?> c, final String label) {
		transformTypes.add(c);
		transformLabels.add(label);
	}

	/** Gets list of registered data transform types. */
	public Class<?>[] getRegisteredDataTypes() {
		final Class<?>[] types = new Class[transformTypes.size()];
		transformTypes.copyInto(types);
		return types;
	}

	/** Gets list of regitered data transform labels. */
	public String[] getRegisteredDataLabels() {
		final String[] labels = new String[transformLabels.size()];
		transformLabels.copyInto(labels);
		return labels;
	}

	/** Gets a list of data transforms present in the tree. */
	public Vector<DataTransform> getDataList() {
		final Vector<DataTransform> v = new Vector<DataTransform>();
		buildDataList(dataControls.getDataRoot(), v);
		return v;
	}

	/** Gets the data transform with the associated ID. */
	public DataTransform getDataById(final int id) {
		return getDataById(dataControls.getDataRoot(), id);
	}

	/** Imports a dataset. */
	public void importData() {
		importData(bio);
	}

	/** Imports a dataset, using the given parent component for user dialogs. */
	public void importData(final Component parent) {
		final DataTransform dt = Dataset.makeTransform(this, null, parent);
		if (dt != null) addData(dt);
	}

	/** Exports the selected data object to disk. */
	public void exportData() {
		final DataTransform data = dataControls.getSelectedData();
		if (data instanceof ImageTransform) exportData((ImageTransform) data);
	}

	/** Exports the given data object to disk. */
	public void exportData(final ImageTransform data) {
		dataControls.exportData(data);
	}

	/** Sends the selected data object to an instance of ImageJ. */
	public void sendDataToImageJ() {
		final DataTransform data = dataControls.getSelectedData();
		if (data instanceof ImageTransform) {
			sendDataToImageJ((ImageTransform) data);
		}
	}

	/** Sends the given data object to an instance of ImageJ. */
	public void sendDataToImageJ(final ImageTransform data) {
		dataControls.sendDataToImageJ(data);
	}

	/**
	 * Loads the given sample dataset. If one with the given name already exists
	 * in the samples cache, it is used. Otherwise, it is downloaded from the
	 * VisBio site and stored in the cache first.
	 */
	public void openSampleData(final String name) {
		final String dirName = name;
		final String zipName = name + ".zip";
		final String location = SAMPLE_PREFIX + zipName;
		final TaskManager tm = (TaskManager) bio.getManager(TaskManager.class);
		final BioTask task = tm.createTask(name);
		task.setStoppable(true);
		new Thread() {

			@Override
			public void run() {
				// create samples folder if it does not already exist
				final File samplesDir = new File("samples");
				if (!samplesDir.exists()) samplesDir.mkdir();

				// create samples subdirectory and download data if not already cached
				final File dir = new File(samplesDir, dirName);
				if (!dir.exists()) {
					dir.mkdir();
					try {
						task.setStatus("Downloading " + zipName);
						final URL url = new URL(location);
						final ZipInputStream in = new ZipInputStream(url.openStream());
						final byte[] buf = new byte[8192];
						while (true) {
							if (task.isStopped()) break;
							final ZipEntry entry = in.getNextEntry();
							if (entry == null) break; // eof
							final String entryName = entry.getName();
							task.setStatus("Extracting " + entryName);
							final FileOutputStream out =
								new FileOutputStream(new File(dir, entryName));
							while (true) {
								final int r = in.read(buf);
								if (r == -1) break; // end of entry
								out.write(buf, 0, r);
							}
							out.close();
							in.closeEntry();
						}
						in.close();
					}
					catch (final IOException exc) {
						System.err.println("Cannot download sample data from " + location);
						exc.printStackTrace();
					}
				}
				if (task.isStopped()) {
					task.setCompleted();
					return;
				}

				// create dataset object
				task.setStatus("Organizing data");
				final File[] files = dir.listFiles();
				String pattern = null;
				for (int i = 0; i < files.length; i++) {
					if (task.isStopped()) break;
					if (files[i].getName().endsWith(".visbio")) continue;
					pattern = FilePattern.findPattern(files[i]);
					if (pattern == null) pattern = files[i].getAbsolutePath();
					break;
				}
				if (task.isStopped()) {
					task.setCompleted();
					return;
				}
				if (pattern == null) {
					System.err.println("Error: no files for sample dataset " + dirName);
					return;
				}

				final FilePattern fp = new FilePattern(pattern);

				if (task.isStopped()) {
					task.setCompleted();
					return;
				}
				createDataset(dirName, pattern, task);
			}
		}.start();
	}

	/**
	 * Creates a dataset, updating the given task object as things progress. If no
	 * task object is given, a new one is created to use.
	 */
	public void createDataset(final String name, final String pattern,
		BioTask bioTask)
	{
		if (bioTask == null) {
			final TaskManager tm = (TaskManager) bio.getManager(TaskManager.class);
			bioTask = tm.createTask(name);
		}
		final BioTask task = bioTask;
		task.setStoppable(false);
		task.setStatus("Creating dataset");
		final StatusListener sl = new StatusListener() {

			@Override
			public void statusUpdated(final StatusEvent e) {
				final int val = e.getProgressValue();
				final int max = e.getProgressMaximum();
				final String msg = e.getStatusMessage();
				task.setStatus(val, max, msg);
			}
		};
		final Dataset dataset = new Dataset(name, pattern, sl);
		task.setCompleted();
		addData(dataset);
	}

	/**
	 * Gets whether low-resolution thumbnails should be automatically generated
	 * from VisBio options.
	 */
	public boolean getAutoThumbGen() {
		final OptionManager om =
			(OptionManager) bio.getManager(OptionManager.class);
		final BooleanOption opt = (BooleanOption) om.getOption(AUTO_THUMBS);
		return opt.getValue();
	}

	/** Gets resolution of low-resolution thumbnails from VisBio options. */
	public int[] getThumbnailResolution() {
		final OptionManager om =
			(OptionManager) bio.getManager(OptionManager.class);
		final ResolutionOption opt = (ResolutionOption) om.getOption(THUMB_RES);
		return new int[] { opt.getValueX(), opt.getValueY() };
	}

	/** Gets associated control panel. */
	public DataControls getControls() {
		return dataControls;
	}

	// -- LogicManager API methods --

	/** Called to notify the logic manager of a VisBio event. */
	@Override
	public void doEvent(final VisBioEvent evt) {
		final int eventType = evt.getEventType();
		if (eventType == VisBioEvent.LOGIC_ADDED) {
			final LogicManager lm = (LogicManager) evt.getSource();
			if (lm == this) doGUI();
		}
	}

	/** Gets the number of tasks required to initialize this logic manager. */
	@Override
	public int getTasks() {
		return 5;
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("VisBio"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Vector<DataTransform> v = getDataList();
		final int len = v.size();
		final Element child = XMLUtil.createChild(el, "DataTransforms");
		for (int i = 0; i < len; i++) {
			final DataTransform data = v.elementAt(i);
			data.saveState(child);
		}
	}

	/** Restores the current state from the given DOM element ("VisBio"). */
	@Override
	public void restoreState(final Element el) throws SaveException {
		final Element child = XMLUtil.getFirstChild(el, "DataTransforms");
		final Element[] els = XMLUtil.getChildren(child, null);
		final Vector<DataTransform> vn = new Vector<DataTransform>();
		for (int i = 0; i < els.length; i++) {
			// read transform class name
			final String className = els[i].getAttribute("class");
			if (className == null) {
				System.err.println("Failed to read transform #" + i + " class");
				continue;
			}

			// locate transform class
			Class<?> c = null;
			try {
				c = Class.forName(className);
			}
			catch (final ClassNotFoundException exc) {}
			catch (final RuntimeException exc) {
				// HACK: workaround for bug in Apache Axis2
				final String msg = exc.getMessage();
				if (msg != null && msg.indexOf("ClassNotFound") < 0) throw exc;
			}
			if (c == null) {
				System.err.println("Failed to identify transform #" + i + " class: " +
					className);
				continue;
			}

			// construct transform
			Object o = null;
			try {
				o = c.newInstance();
			}
			catch (final IllegalAccessException exc) {}
			catch (final InstantiationException exc) {}
			if (o == null) {
				System.err.println("Failed to instantiate transform #" + i);
				continue;
			}
			if (!(o instanceof DataTransform)) {
				System.err.println("Transform #" + i + " is not valid (" +
					o.getClass().getName() + ")");
				continue;
			}

			// restore transform state
			final DataTransform data = (DataTransform) o;
			data.restoreState(els[i]);
			vn.add(data);
		}

		// restore parent transform references
		final int nlen = vn.size();
		for (int i = 0; i < nlen; i++) {
			final DataTransform data = vn.elementAt(i);
			final String parentId = els[i].getAttribute("parent");
			if (parentId == null || parentId.equals("")) data.parent = null;
			else {
				int pid = -1;
				try {
					pid = Integer.parseInt(parentId);
				}
				catch (final NumberFormatException exc) {
					exc.printStackTrace();
				}
				// search for transform with matching ID
				for (int j = 0; j < nlen; j++) {
					final DataTransform dt = vn.elementAt(j);
					if (dt.getTransformId() == pid) data.parent = dt;
				}
				if (data.parent == null) {
					System.err.println("Invalid parent id (" + parentId +
						") for transform #" + i);
				}
			}
		}

		// merge old and new transform lists
		final Vector<DataTransform> vo = getDataList();
		StateManager.mergeStates(vo, vn);

		// add new transforms to tree structure
		for (int i = 0; i < nlen; i++) {
			final DataTransform data = vn.elementAt(i);
			if (!vo.contains(data)) addData(data);
		}

		// purge old transforms from tree structure
		final int olen = vo.size();
		for (int i = 0; i < olen; i++) {
			final DataTransform data = vo.elementAt(i);
			if (!vn.contains(data)) removeData(data);
		}
	}

	// -- Helper methods --

	/** Adds data-related GUI components to VisBio. */
	private void doGUI() {
		// control panel
		bio.setSplashStatus("Initializing data logic");
		dataControls = new DataControls(this);
		final PanelManager pm = (PanelManager) bio.getManager(PanelManager.class);
		pm.addPanel(dataControls, 0, 0, 1, 2, "350:grow", null);

		// data transform registration
		bio.setSplashStatus(null);
		transformTypes = new Vector<Class<?>>();
		transformLabels = new Vector<String>();
		registerDataType(Dataset.class, "Dataset");
		registerDataType(DataSampling.class, "Subsampling");
		registerDataType(ProjectionTransform.class, "Maximum intensity projection");
		registerDataType(CollapseTransform.class, "Dimensional collapse");
		registerDataType(SpectralTransform.class, "Spectral mapping");
		registerDataType(ArbitrarySlice.class, "Arbitrary slice");

		// menu items
		bio.setSplashStatus(null);
		bio.addMenuItem("File", "Import data...",
			"loci.visbio.data.DataManager.importData", 'i');
		bio.setMenuShortcut("File", "Import data...", KeyEvent.VK_O);
		bio.addMenuItem("File", "Export data...",
			"loci.visbio.data.DataManager.exportData", 'e').setEnabled(false);
		bio.setMenuShortcut("File", "Export data...", KeyEvent.VK_X);
		bio.addSubMenu("File", "Sample datasets", 'd');
		bio.addMenuItem("Sample datasets", "sdub",
			"loci.visbio.data.DataManager.openSampleData(sdub)", 's');
		bio.addMenuItem("Sample datasets", "TAABA",
			"loci.visbio.data.DataManager.openSampleData(TAABA)", 't');

		// options
		bio.setSplashStatus(null);
		final OptionManager om =
			(OptionManager) bio.getManager(OptionManager.class);
		om.addBooleanOption("Thumbnails", AUTO_THUMBS, 'a',
			"Toggles whether thumbnails are automatically generated", true);
		final int thumbRes = DEFAULT_THUMBNAIL_RESOLUTION;
		om.addOption("Thumbnails", new ResolutionOption(THUMB_RES,
			"Adjusts resolution of low-resolution thumbnails", thumbRes, thumbRes));

		// help window
		bio.setSplashStatus(null);
		final HelpManager hm = (HelpManager) bio.getManager(HelpManager.class);
		hm.addHelpTopic("File formats", "formats.html");
		String s = "Data transforms";
		hm.addHelpTopic(s, "data_transforms.html");
		hm.addHelpTopic(s + "/Datasets", "dataset.html");
		hm.addHelpTopic(s + "/Subsamplings", "subsampling.html");
		hm.addHelpTopic(s + "/Maximum intensity projections", "max_intensity.html");
		hm.addHelpTopic(s + "/Dimensional collapse transforms", "collapse.html");
		hm.addHelpTopic(s + "/Spectral mappings", "spectral.html");
		hm.addHelpTopic(s + "/Arbitrary slices", "arbitrary_slice.html");
		s = "Control panels/Data panel";
		hm.addHelpTopic(s, "data_panel.html");
		hm.addHelpTopic(s + "/Importing a dataset from disk", "import_data.html");
		hm.addHelpTopic(s + "/Adding a data object", "add_data.html");
		hm.addHelpTopic(s + "/Generating thumbnails", "thumbnails.html");
		hm.addHelpTopic(s + "/Displaying a data object", "display_data.html");
		hm.addHelpTopic(s + "/Exporting data to disk", "export_data.html");
		hm.addHelpTopic(s + "/Exporting data directly to ImageJ",
			"export_imagej.html");
		hm.addHelpTopic(s + "/Uploading to an OME database", "upload_ome.html");
	}

	/** Recursively creates a list of data transforms below the given node. */
	private void buildDataList(final DefaultMutableTreeNode node,
		final Vector<DataTransform> v)
	{
		final Object o = node.getUserObject();
		if (o instanceof DataTransform) v.add((DataTransform) o);

		final int count = node.getChildCount();
		for (int i = 0; i < count; i++) {
			final DefaultMutableTreeNode child =
				(DefaultMutableTreeNode) node.getChildAt(i);
			buildDataList(child, v);
		}
	}

	/** Recursively searches data tree for a transform with the given ID. */
	private DataTransform getDataById(final DefaultMutableTreeNode node,
		final int id)
	{
		final Object o = node.getUserObject();
		if (o instanceof DataTransform) {
			final DataTransform data = (DataTransform) o;
			if (data.getTransformId() == id) return data;
		}
		final int count = node.getChildCount();
		for (int i = 0; i < count; i++) {
			final DefaultMutableTreeNode child =
				(DefaultMutableTreeNode) node.getChildAt(i);
			final DataTransform data = getDataById(child, id);
			if (data != null) return data;
		}
		return null;
	}

}

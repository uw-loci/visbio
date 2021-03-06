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

import loci.visbio.help.HelpManager;

/**
 * TaskManager is the manager encapsulating VisBio's multitasking logic.
 */
public class TaskManager extends LogicManager {

	// -- Control panel --

	/** Tasks control panel. */
	protected TaskControls taskControls;

	// -- Constructor --

	/** Constructs a tasks manager. */
	public TaskManager(final VisBioFrame bio) {
		super(bio);
	}

	// -- TaskManager API methods --

	/** Creates a new task with the given name. */
	public BioTask createTask(final String name) {
		final BioTask task = new BioTask(this, name);
		taskControls.addTask(task);
		return task;
	}

	/** Removes the given task from the task list. */
	public void setCompleted(final BioTask task) {
		taskControls.removeTask(task);
	}

	/** Gets associated control panel. */
	public TaskControls getControls() {
		return taskControls;
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
		return 2;
	}

	// -- Helper methods --

	/** Adds data-related GUI components to VisBio. */
	private void doGUI() {
		// control panel
		bio.setSplashStatus("Initializing task management");
		taskControls = new TaskControls(this);
		final PanelManager pm = (PanelManager) bio.getManager(PanelManager.class);
		pm.addPanel(taskControls, 1, 1, 1, 1, "350:grow", "200:grow");

		// help topics
		bio.setSplashStatus(null);
		final HelpManager hm = (HelpManager) bio.getManager(HelpManager.class);
		hm.addHelpTopic("Control panels/Tasks panel", "tasks_panel.html");
	}

}

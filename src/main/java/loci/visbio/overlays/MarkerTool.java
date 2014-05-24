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

import loci.visbio.data.TransformEvent;
import visad.DisplayEvent;

/**
 * MarkerTool is the tool for creating measurement markers.
 */
public class MarkerTool extends OverlayTool {

	// -- Fields --

	/** Marker currently being drawn. */
	protected OverlayMarker marker;

	// -- Constructor --

	/** Constructs a measurement marker creation tool. */
	public MarkerTool(final OverlayTransform overlay) {
		super(overlay, "Marker", "Marker", "marker.png");
	}

	// -- OverlayTool API methods --

	/** Instructs this tool to respond to a mouse press. */
	@Override
	public void mouseDown(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{
		deselectAll();
		marker = new OverlayMarker(overlay, dx, dy);
		overlay.addObject(marker, pos);
	}

	/** Instructs this tool to respond to a mouse release. */
	@Override
	public void mouseUp(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{
		if (marker == null) return;
		marker.setDrawing(false);
		marker = null;
		overlay.notifyListeners(new TransformEvent(overlay));
	}

	/** Instructs this tool to respond to a mouse drag. */
	@Override
	public void mouseDrag(final DisplayEvent e, final int px, final int py,
		final float dx, final float dy, final int[] pos, final int mods)
	{
		if (marker == null) return;
		marker.setCoords(dx, dy);
		overlay.notifyListeners(new TransformEvent(overlay));
	}

}

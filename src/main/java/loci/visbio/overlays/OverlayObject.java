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

import java.awt.Color;

import visad.DataImpl;

/**
 * OverlayObject is the superclass of all overlay objects.
 */
public abstract class OverlayObject {

	// -- Constants --

	protected static final float GLOW_WIDTH = 1.0f;

	protected static final float GLOW_ALPHA = 0.15f;

	protected static final Color GLOW_COLOR = Color.YELLOW;

	// -- Fields --

	/** Associated overlay transform. */
	protected OverlayTransform overlay;

	/** Endpoint coordinates. */
	protected float x1, y1, x2, y2;

	/** Text string to render. */
	protected String text;

	/** Color of this overlay. */
	protected Color color;

	/** Flag indicating overlay is solid. */
	protected boolean filled;

	/** Group to which this overlay belongs. */
	protected String group;

	/** Notes associated with this overlay. */
	protected String notes;

	/** Flag indicating this overlay is currently selected. */
	protected boolean selected = true;

	/** Flag indicating this overlay is still being initially drawn. */
	protected boolean drawing = true;

	/** Top-left endpoint of selection grid rectangle. */
	protected float xGrid1, yGrid1;

	/** Top-right endpoint of selection grid rectangle. */
	protected float xGrid2, yGrid2;

	/** Bottom-left endpoint of selection grid rectangle. */
	protected float xGrid3, yGrid3;

	/** Bottom-right endpoint of selection grid rectangle. */
	protected float xGrid4, yGrid4;

	/** Number of horizontal and vertical dividing lines for selection grid. */
	protected int horizGridCount, vertGridCount;

	// -- Constructor --

	/** Constructs an overlay. */
	public OverlayObject(final OverlayTransform overlay) {
		this.overlay = overlay;
		overlay.setTextDrawn(false);
	}

	// -- OverlayObject API methods --

	/**
	 * Returns whether this object is drawable, i.e., is of nonzero size, area,
	 * length, etc.
	 */
	public abstract boolean hasData();

	/** Gets VisAD data object representing this overlay. */
	public abstract DataImpl getData();

	/** Computes the shortest distance from this overlay to the given point. */
	public abstract double getDistance(double x, double y);

	/** Gets a specific overlay statistic. */
	public abstract String getStat(String name);

	/** Retrieves useful statistics about this overlay. */
	public String getStatistics() {
		String name = getClass().getName();
		final String pack = getClass().getPackage().getName();
		if (name.startsWith(pack)) name = name.substring(pack.length() + 1);
		return "No statistics for " + name;
	}

	/** True iff this overlay has an endpoint coordinate pair. */
	public boolean hasEndpoint() {
		return false;
	}

	/** True iff this overlay has a second endpoint coordinate pair. */
	public boolean hasEndpoint2() {
		return false;
	}

	/** True iff this overlay supports the filled parameter. */
	public boolean canBeFilled() {
		return false;
	}

	/** True iff this overlay can be resized using X1, X2, Y1, Y2 entry boxes. */
	public boolean areBoundsEditable() {
		return true;
	}

	// currently, only non-noded objects can be resized this way.
	// (Actually could perform some rad scaling on all nodes)

	/** True iff this overlay returns text to render. */
	public boolean hasText() {
		return false;
	}

	/** Changes X coordinate of the overlay's first endpoint. */
	public void setX(final float x1) {
		if (!hasEndpoint()) return;
		this.x1 = x1;
	}

	/** Changes Y coordinate of the overlay's first endpoint. */
	public void setY(final float y1) {
		if (!hasEndpoint()) return;
		this.y1 = y1;
	}

	/** Changes coordinates of the overlay's first endpoint. */
	public void setCoords(final float x1, final float y1) {
		if (!hasEndpoint()) return;
		this.x1 = x1;
		this.y1 = y1;
	}

	/** Gets X coordinate of the overlay's first endpoint. */
	public float getX() {
		return x1;
	}

	/** Gets Y coordinate of the overlay's first endpoint. */
	public float getY() {
		return y1;
	}

	/** Changes X coordinate of the overlay's second endpoint. */
	public void setX2(final float x2) {
		if (!hasEndpoint2()) return;
		this.x2 = x2;
	}

	/** Changes Y coordinate of the overlay's second endpoint. */
	public void setY2(final float y2) {
		if (!hasEndpoint2()) return;
		this.y2 = y2;
	}

	/** Changes coordinates of the overlay's second endpoint. */
	public void setCoords2(final float x2, final float y2) {
		if (!hasEndpoint2()) return;
		this.x2 = x2;
		this.y2 = y2;
	}

	/** Gets X coordinate of the overlay's second endpoint. */
	public float getX2() {
		return x2;
	}

	/** Gets Y coordinate of the overlay's second endpoint. */
	public float getY2() {
		return y2;
	}

	/** Changes text to render. */
	public void setText(final String text) {
		if (!hasText()) return;
		this.text = text;
	}

	/** Gets text to render. */
	public String getText() {
		return text;
	}

	/** Sets color of this overlay. */
	public void setColor(final Color c) {
		color = c;
	}

	/** Gets color of this overlay. */
	public Color getColor() {
		return color;
	}

	/** Sets whether overlay is solid. */
	public void setFilled(final boolean filled) {
		if (canBeFilled()) this.filled = filled;
	}

	/** Gets whether overlay is solid. */
	public boolean isFilled() {
		return filled;
	}

	/** Gets whether overlay is scalable. */
	public boolean isScalable() {
		return false;
	}

	/** Rescales this overlay object. */
	public void rescale(final float multiplier) {}

	/** Sets group to which this overlay belongs. */
	public void setGroup(final String group) {
		this.group = group;
	}

	/** Gets group to which this overlay belongs. */
	public String getGroup() {
		return group;
	}

	/** Sets notes for this overlay. */
	public void setNotes(final String text) {
		notes = text;
	}

	/** Gets notes for this overlay. */
	public String getNotes() {
		return notes;
	}

	/** Sets whether this overlay is currently selected. */
	public void setSelected(final boolean selected) {
		this.selected = selected;
	}

	/** Gets whether this overlay is currently selected. */
	public boolean isSelected() {
		return selected;
	}

	/** Sets whether this overlay is still being initially drawn. */
	public void setDrawing(final boolean drawing) {
		this.drawing = drawing;
		overlay.setTextDrawn(!drawing);
	}

	/** Gets whether this overlay is still being initially drawn. */
	public boolean isDrawing() {
		return drawing;
	}

	// -- Internal OverlayObject API methods --

	/** Sets value of largest and smallest x, y values. */
	protected void setBoundaries(final float x, final float y) {
		x1 = Math.min(x1, x);
		x2 = Math.max(x2, x);
		y1 = Math.min(y1, y);
		y2 = Math.max(y2, y);
	}
}// end class

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

/**
 * OverlayFreeform is a freeform overlay.
 */

public class OverlayPolyline extends OverlayNodedObject {

  // -- Fields --

  // -- Constructors --

  /** Constructs an uninitialized freeform. */
  public OverlayPolyline(OverlayTransform overlay) { super(overlay); }

  /** Constructs a freeform. */
  public OverlayPolyline(OverlayTransform overlay,
    float x1, float y1, float x2, float y2)
  {
    super(overlay, x1, y1, x2, y2);
  }

  /** Constructs a freeform from an array of nodes. */
  public OverlayPolyline(OverlayTransform overlay, float[][] nodes) {
    super(overlay, nodes);
  }

  // -- Internal OverlayObject API methods --

  // -- Object API methods --

  // -- OverlayObject API methods --
  /** Gets a short string representation of this freeform. */
  public String toString() { return "Polyline"; }

}

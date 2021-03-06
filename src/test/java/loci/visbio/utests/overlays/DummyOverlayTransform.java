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

package loci.visbio.utests.overlays;

import java.util.Vector;

import loci.formats.FormatTools;
import loci.visbio.data.DataTransform;
import loci.visbio.data.TransformListener;
import loci.visbio.overlays.OverlayTransform;
import loci.visbio.state.Dynamic;

/**
 * A fake overlay transform without widget or controls used for unit testing on
 * OverlayObjects.
 */
public class DummyOverlayTransform extends OverlayTransform implements
	TransformListener
{

	// -- Constants --

	// -- Fields --

	// -- Constructor --

	/** Creates an overlay object for the given transform. */
	public DummyOverlayTransform(final DataTransform parent, final String name) {
		super(parent, name);
	}

	// -- OverlayTransform API methods --

	/**
	 * From OverlayTransform: "Modifies this object's state to match that of the
	 * given object. If the argument is null, the object is initialized according
	 * to its current state instead." This implementation is practically the same,
	 * just skips calling the superclasses' initState methods and skips creating a
	 * widget and controls.
	 */
	@Override
	public void initState(final Dynamic dyn) {
		if (dyn != null && !isCompatible(dyn)) return;
		// super.initState(dyn)
		final DummyOverlayTransform data = (DummyOverlayTransform) dyn;

		if (data != null) {
			// CTR TODO synchronize data with this object
		}

		lengths = parent.getLengths();
		dims = parent.getDimTypes();
		makeLabels();

		final int len = FormatTools.getRasterLength(lengths);
		final Vector[] v = new Vector[len];
		int minLen = 0;
		if (overlays != null) {
			// CTR - This logic is simplistic and will result in erroneous behavior
			// should a transform with multiple dimensional axes suffer a length
			// alteration along its axes. That is, the rasterization will probably be
			// shifted so that a particular position such as (3, 5) will no longer be
			// (3, 5), even if (3, 5) is still a valid dimensional position. But we
			// cannot guarantee much in the general case, because the number of
			// dimensional positions could also have shifted. What we should do is
			// sniff out exactly how a transform has changed by examining the old and
			// new lengths arrays, and act appropriately, but for now we simply
			// preserve as many overlays as possible. If the dimensional axes have
			// been significantly altered, too bad.
			minLen = overlays.length < len ? overlays.length : len;
			System.arraycopy(overlays, 0, v, 0, minLen);
		}
		for (int i = minLen; i < len; i++)
			v[i] = new Vector();
		overlays = v;
		pos = new int[lengths.length];

		// controls = new OverlayWidget(this);
		// fontMetrics = controls.getFontMetrics(font);
		controls = null;
		fontMetrics = null;
	}
}

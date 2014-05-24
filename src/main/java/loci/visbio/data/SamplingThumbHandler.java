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

import java.rmi.RemoteException;

import loci.visbio.util.DataUtil;
import visad.FlatField;
import visad.VisADException;

/**
 * Provides logic for handling data sampling thumbnails.
 */
public class SamplingThumbHandler extends ThumbnailHandler {

	// -- Constructor --

	/** Creates a thumbnail handler. */
	public SamplingThumbHandler(final DataTransform data, final String filename) {
		super(data, filename);
	}

	// -- Internal ThumbnailHandler API methods --

	/**
	 * Computes a thumbnail for the given dimensional position. This method is
	 * intelligent enough to check for parent thumbnails before going to disk, and
	 * resample from them if they are available.
	 */
	@Override
	protected FlatField computeThumb(final int[] pos) {
		final DataSampling samp = (DataSampling) data;
		final ThumbnailHandler th = samp.getParent().getThumbHandler();
		if (th == null) return super.computeThumb(pos);

		final int[] min = samp.getMin();
		final int[] step = samp.getStep();
		final boolean[] range = samp.getRange();

		final int[] p = new int[pos.length];
		for (int i = 0; i < p.length; i++)
			p[i] = min[i] + step[i] * pos[i] - 1;
		final FlatField ff = th.getThumb(p);
		if (ff == null) return super.computeThumb(pos);

		final float[] smin = { 0, 0 };
		final float[] smax =
			{ samp.getImageWidth() - 1, samp.getImageHeight() - 1 };
		try {
			return DataUtil.resample(ff, resolution, range, smin, smax);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		return null;
	}

}

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

import javax.swing.JComponent;
import javax.swing.JOptionPane;

import loci.visbio.state.Dynamic;
import loci.visbio.state.SaveException;
import loci.visbio.util.ObjectUtil;
import loci.visbio.util.XMLUtil;
import loci.visbio.view.TransformLink;

import org.w3c.dom.Element;

import visad.Data;
import visad.FlatField;
import visad.FunctionType;
import visad.RealTupleType;
import visad.RealType;
import visad.VisADException;

/**
 * SpectralTransform is a mapping of spectral bands onto an RGB or grayscale
 * color space, as specified by the user with weighted sliders.
 */
public class SpectralTransform extends ImageTransform implements
	TransformListener
{

	// -- Fields --

	/** Output range components. */
	protected RealType[] range;

	/** Spectral channel weights. */
	protected double[][] weights;

	/** Controls for this spectral mapping. */
	protected SpectralWidget controls;

	// -- Constructors --

	/** Creates an uninitialized spectral mapping. */
	public SpectralTransform() {
		super();
	}

	/** Creates a spectral mapping from the given transform. */
	public SpectralTransform(final DataTransform parent, final String name,
		final int numRange)
	{
		super(parent, name);

		// weights default to evenly distributed 1s
		final ImageTransform it = (ImageTransform) parent;
		final int in = it.getRangeCount();
		weights = new double[numRange][in];

		final int div = in / numRange;
		final int rem = in % numRange;
		int count = 0;
		for (int i = 0; i < numRange; i++) {
			final int num = i < rem ? div + 1 : div;
			for (int j = count; j < count + num; j++)
				weights[i][j] = 1.0;
			count += num;
		}

		initState(null);
		parent.addTransformListener(this);
	}

	// -- SpectralTransform API methods --

	/** Assigns the parameters for this spectral mapping. */
	public void setParameters(final double[][] weights) {
		if (weights.length != range.length) return;

		this.weights = new double[weights.length][];
		for (int i = 0; i < weights.length; i++) {
			final int len = weights[i].length;
			this.weights[i] = new double[len];
			System.arraycopy(weights[i], 0, this.weights[i], 0, len);
		}

		// signal parameter change to listeners
		notifyListeners(new TransformEvent(this));
	}

	/** Gets spectral channel weights. */
	public double[][] getWeights() {
		return weights;
	}

	// -- ImageTransform API methods --

	/** Gets width of each image. */
	@Override
	public int getImageWidth() {
		return ((ImageTransform) parent).getImageWidth();
	}

	/** Gets height of each image. */
	@Override
	public int getImageHeight() {
		return ((ImageTransform) parent).getImageHeight();
	}

	/** Gets number of range components at each pixel. */
	@Override
	public int getRangeCount() {
		return range.length;
	}

	// -- Static DataTransform API methods --

	/** Creates a new spectral mapping, with user interaction. */
	public static DataTransform makeTransform(final DataManager dm) {
		final DataTransform data = dm.getSelectedData();
		if (!isValidParent(data)) return null;
		final String n =
			(String) JOptionPane.showInputDialog(dm.getControls(), "Mapping name:",
				"Create spectral mapping", JOptionPane.INFORMATION_MESSAGE, null, null,
				data.getName() + " mapping");
		if (n == null) return null;

		// guess at some reasonable defaults
		final int rangeCount = 3;

		return new SpectralTransform(data, n, rangeCount);
	}

	/**
	 * Indicates whether this transform type would accept the given transform as
	 * its parent transform.
	 */
	public static boolean isValidParent(final DataTransform data) {
		return data != null && data instanceof ImageTransform;
	}

	/** Indicates whether this transform type requires a parent transform. */
	public static boolean isParentRequired() {
		return true;
	}

	// -- DataTransform API methods --

	/**
	 * Retrieves the data corresponding to the given dimensional position, for the
	 * given display dimensionality.
	 * 
	 * @return null if the transform does not provide data of that dimensionality
	 */
	@Override
	public Data getData(final TransformLink link, final int[] pos, final int dim,
		final DataCache cache)
	{
		if (dim != 2) return null;

		final Data data = parent.getData(link, pos, dim, cache);
		if (data == null || !(data instanceof FlatField)) return null;

		return doWeightedMapping((FlatField) data, range, weights);
	}

	/** Gets whether this transform provides data of the given dimensionality. */
	@Override
	public boolean isValidDimension(final int dim) {
		return dim == 2 && parent.isValidDimension(dim);
	}

	/**
	 * Gets a string id uniquely describing this data transform at the given
	 * dimensional position, for the purposes of thumbnail caching. If global flag
	 * is true, the id is suitable for use in the default, global cache file.
	 */
	@Override
	public String getCacheId(final int[] pos, final boolean global) {
		final StringBuffer sb = new StringBuffer(parent.getCacheId(pos, global));
		sb.append("{");
		appendArray(sb, "w0", weights[0], true);
		appendArray(sb, "w1", weights[1], true);
		appendArray(sb, "w2", weights[2], false);
		sb.append("}");
		return sb.toString();
	}

	/** Gets associated GUI controls for this transform. */
	@Override
	public JComponent getControls() {
		return controls;
	}

	// -- Dynamic API methods --

	/** Tests whether two dynamic objects are equivalent. */
	@Override
	public boolean matches(final Dynamic dyn) {
		if (!super.matches(dyn) || !isCompatible(dyn)) return false;
		final SpectralTransform data = (SpectralTransform) dyn;

		return ObjectUtil.arraysEqual(weights, data.weights);
	}

	/**
	 * Tests whether the given dynamic object can be used as an argument to
	 * initState, for initializing this dynamic object.
	 */
	@Override
	public boolean isCompatible(final Dynamic dyn) {
		return dyn instanceof SpectralTransform;
	}

	/**
	 * Modifies this object's state to match that of the given object. If the
	 * argument is null, the object is initialized according to its current state
	 * instead.
	 */
	@Override
	public void initState(final Dynamic dyn) {
		if (dyn != null && !isCompatible(dyn)) return;
		super.initState(dyn);
		final SpectralTransform data = (SpectralTransform) dyn;

		if (data != null) {
			weights = new double[data.weights.length][];
			for (int i = 0; i < weights.length; i++) {
				weights[i] = ObjectUtil.copy(data.weights[i]);
			}
		}

		lengths = parent.getLengths();
		dims = parent.getDimTypes();
		makeLabels();

		final int numRange = weights.length;
		final String[] rangeLabels = new String[numRange];
		range = new RealType[numRange];
		for (int i = 0; i < numRange; i++) {
			rangeLabels[i] = "value" + (i + 1);
			range[i] = RealType.getRealType(rangeLabels[i]);
		}

		controls = new SpectralWidget(this, rangeLabels);
		thumbs = new ThumbnailHandler(this, getCacheFilename());
	}

	// -- Saveable API methods --

	/** Writes the current state to the given DOM element ("DataTransforms"). */
	@Override
	public void saveState(final Element el) throws SaveException {
		final Element child = XMLUtil.createChild(el, "SpectralMapping");
		super.saveState(child);
		for (int i = 0; i < weights.length; i++) {
			final Element wel = XMLUtil.createChild(child, "Weights");
			XMLUtil.createText(wel, ObjectUtil.arrayToString(weights[i]));
		}
	}

	/**
	 * Restores the current state from the given DOM element ("SpectralMapping").
	 */
	@Override
	public void restoreState(final Element el) throws SaveException {
		super.restoreState(el);
		final Element[] els = XMLUtil.getChildren(el, "Weights");
		weights = new double[els.length][];
		for (int i = 0; i < weights.length; i++) {
			weights[i] = ObjectUtil.stringToDoubleArray(XMLUtil.getText(els[i]));
		}
	}

	// -- TransformListener API methods --

	/** Called when parent data transform's parameters are updated. */
	@Override
	public void transformChanged(final TransformEvent e) {
		final int id = e.getId();
		if (id == TransformEvent.DATA_CHANGED) {
			// CTR TODO transformChanged DATA_CHANGED
			// depending on how parent changed, recompute weights array size
			// if it changes size, need to redo spectral widget layout
			// but always need to notify listeners of spectral transform change
			notifyListeners(new TransformEvent(this));
		}
	}

	// -- Utility methods --

	/**
	 * Applies a spectral mapping to the given field based on the given weights
	 * and range types.
	 */
	public static FlatField doWeightedMapping(final FlatField field,
		final RealType[] types, final double[][] weights)
	{
		if (types.length != weights.length) return null;

		try {
			final float[][] samples = field.getFloats(false);
			final int count = samples[0].length;

			final float[][] nsamps = new float[types.length][count];
			for (int r = 0; r < types.length; r++) {
				final int wlen = weights[r].length;
				for (int c = 0; c < count; c++) {
					float sum = 0;
					for (int w = 0; w < wlen; w++)
						sum += weights[r][w] * samples[w][c];
					nsamps[r][c] = sum / wlen;
				}
			}

			final RealTupleType domain = ((FunctionType) field.getType()).getDomain();
			final FunctionType ftype =
				new FunctionType(domain, new RealTupleType(types));

			final FlatField ff = new FlatField(ftype, field.getDomainSet());
			ff.setSamples(nsamps, false);
			return ff;
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		catch (final RemoteException exc) {
			exc.printStackTrace();
		}
		return null;
	}

	// -- Helper methods --

	/** Chooses filename for thumbnail cache based on parent cache's name. */
	private String getCacheFilename() {
		ThumbnailCache cache = null;
		final ThumbnailHandler th = parent.getThumbHandler();
		if (th != null) cache = th.getCache();
		if (cache == null) return "spectral_mapping.visbio";
		String s = cache.getCacheFile().getAbsolutePath();
		final int dot = s.lastIndexOf(".");
		String suffix;
		if (dot < 0) suffix = "";
		else {
			suffix = s.substring(dot);
			s = s.substring(0, dot);
		}
		return s + "_mapping" + suffix;
	}

	/** Appends the given array to the specified string buffer. */
	private void appendArray(final StringBuffer sb, final String name,
		final double[] array, final boolean semicolon)
	{
		sb.append(name);
		sb.append("=[");
		for (int i = 0; i < array.length; i++) {
			if (i > 0) sb.append(",");
			sb.append(array[i]);
		}
		sb.append("]");
		if (semicolon) sb.append(";");
	}

}

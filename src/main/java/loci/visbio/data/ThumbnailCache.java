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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Vector;

import visad.FlatField;
import visad.VisADException;
import visad.data.visad.BinaryReader;
import visad.data.visad.BinaryWriter;

//TODO: rewrite thumbnailing logic to save .visbio cache more efficiently
//maybe in JPEG format, but definitely in 8-bit
//put "default" cache files in "cache" subfolder of visbio, instead of just
//one huge "cache.visbio" file

/**
 * Disk cache for thumbnails generated from datasets.
 */
public class ThumbnailCache {

	// -- Constants --

	/** Default cache file to use if supplied cache file is not available. */
	private static final File DEFAULT_CACHE = new File("cache.visbio");

	// -- Fields --

	/** File containing thumbnail cache data. */
	protected File file;

	/** Thumbnail id strings. */
	protected Vector ids;

	/** Thumbnail byte offsets. */
	protected Vector offsets;

	/** Last retrieved thumbnail index. */
	protected int last;

	// -- Constructor --

	/** Constructs a thumbnail cache that uses the given disk file. */
	public ThumbnailCache(final String filename) {
		file = new File(filename);
		ids = new Vector();
		offsets = new Vector();

		// read in existing id/offset pairs
		try {
			if (!file.exists()) {
				boolean success = true;
				try {
					file.createNewFile();
				}
				catch (final IOException exc) {
					success = false;
				}
				catch (final SecurityException exc) {
					success = false;
				}
				if (!success) {
					// supplied file is unavailable; use default cache file instead
					file = DEFAULT_CACHE;
					if (!file.exists()) file.createNewFile();
				}
			}
			final RandomAccessFile raf = new RandomAccessFile(file, "r");
			long offset = 0;
			while (true) {
				try {
					final int idLen = raf.readInt();
					final byte[] buf = new byte[idLen];
					raf.readFully(buf);
					final String id = new String(buf);
					final int size = raf.readInt();
					ids.add(id);
					offsets.add(new Long(offset));
					offset += idLen + size + 8;
					raf.seek(offset);
				}
				catch (final EOFException exc) {
					break;
				}
				catch (final Exception exc) {
					// something went horribly wrong; assume cache is corrupt & purge it
					System.err.println("Purging corrupt cache file " + file);
					try {
						raf.close();
					}
					catch (final IOException exc2) {
						exc2.printStackTrace();
					}
					clear();
					break;
				}
			}
			raf.close();
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
	}

	// -- API methods --

	/** Retrieves the thumbnail with the given id string from the disk cache. */
	public FlatField retrieve(final String id) {
		final long offset = getOffset(id);
		if (offset < 0) return null;
		try {
			return load(offset);
		}
		catch (final IOException exc) {
			exc.printStackTrace();
			return null;
		}
	}

	/** Stores the given thumbnail in the disk cache. */
	public void store(final String id, final FlatField thumb) {
		// append thumbnail to the data file
		try {
			save(id, thumb);
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
	}

	/** Wipes the thumbnail disk cache. */
	public void clear() {
		try {
			file.delete();
			file.createNewFile();
		}
		catch (final IOException exc) {
			exc.printStackTrace();
		}
	}

	/** Gets thumbnail cache disk usage in bytes. */
	public long getUsage() {
		return file.length();
	}

	/** Gets the number of thumbnails in the disk cache. */
	public int getThumbCount() {
		return ids.size();
	}

	/** Gets the disk cache file. */
	public File getCacheFile() {
		return file;
	}

	/**
	 * Gets whether the default cache file was used because the one supplied to
	 * the constructor was unavailable.
	 */
	public boolean isDefault() {
		return file.equals(DEFAULT_CACHE);
	}

	// -- Helper methods --

	/** Gets the offset corresponding to the given id string. */
	protected long getOffset(final String id) {
		final int size = ids.size();
		for (int i = 0; i < size; i++) {
			final String s = (String) ids.elementAt(i);
			if (s.equals(id)) return ((Long) offsets.elementAt(i)).longValue();
		}
		return -1;
	}

	/** Saves the given data object to the end of the cache file. */
	protected void save(final String id, final FlatField thumb)
		throws IOException
	{
		final byte[] idBytes = id.getBytes();
		final long offset = file.length();

		// convert image data into byte array
		final ByteArrayOutputStream bout = new ByteArrayOutputStream();
		final BinaryWriter fout = new BinaryWriter(bout);
		try {
			fout.save(thumb);
		}
		catch (final VisADException exc) {
			exc.printStackTrace();
		}
		fout.close();
		final byte[] image = bout.toByteArray();

		// add id/offset pair to the list
		ids.add(id);
		offsets.add(new Long(offset));

		// write data to cache file
		final RandomAccessFile raf = new RandomAccessFile(file, "rw");
		raf.seek(offset);
		raf.writeInt(idBytes.length);
		raf.write(idBytes);
		raf.writeInt(image.length);
		raf.write(image);
		raf.close();
	}

	/** Loads the data object at the given byte offset of the cache file. */
	protected FlatField load(final long offset) throws IOException {
		final RandomAccessFile raf = new RandomAccessFile(file, "r");
		raf.seek(offset);
		final int idLen = raf.readInt();
		raf.skipBytes(idLen); // skip id string

		// read in image bytes
		final int length = raf.readInt();
		final byte[] bytes = new byte[length];
		raf.readFully(bytes);
		raf.close();

		// convert image bytes to FlatField object
		final BinaryReader fin = new BinaryReader(new ByteArrayInputStream(bytes));
		FlatField thumb;
		try {
			thumb = (FlatField) fin.getData();
		}
		catch (final ClassCastException exc) {
			thumb = null;
		}
		catch (final VisADException exc) {
			thumb = null;
		}
		fin.close();

		return thumb;
	}

}

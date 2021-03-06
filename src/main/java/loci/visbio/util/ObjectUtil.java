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

package loci.visbio.util;

import java.awt.Color;
import java.lang.reflect.Array;
import java.util.StringTokenizer;

/**
 * ObjectUtil contains object and array manipulation functions.
 */
public final class ObjectUtil {

	// -- Constructor --

	private ObjectUtil() {}

	// -- Equality testing --

	/**
	 * Determines whether the two Object arrays are component-wise equal. Also
	 * works with multidimensional arrays of Objects or primitives.
	 */
	public static boolean arraysEqual(final Object[] a1, final Object[] a2) {
		if (a1 == null || a2 == null) return a1 == null && a2 == null;
		if (a1.length != a2.length) return false;
		if (!a1.getClass().equals(a2.getClass())) return false;
		if (a1.length == 0) return true;

		final String cname = a1[0].getClass().getName();
		final boolean array = cname.startsWith("[");
		final char atype = cname.length() < 1 ? '\0' : cname.charAt(1);
		for (int i = 0; i < a1.length; i++) {
			if (array) {
				if (atype == '[' || atype == 'L') { // array of arrays/Objects
					if (!arraysEqual((Object[]) a1[i], (Object[]) a2[i])) return false;
				}
				else if (atype == 'I') { // array of ints
					if (!arraysEqual((int[]) a1[i], (int[]) a2[i])) return false;
				}
				else if (atype == 'C') { // array of chars
					if (!arraysEqual((char[]) a1[i], (char[]) a2[i])) return false;
				}
				else if (atype == 'F') { // array of floats
					if (!arraysEqual((float[]) a1[i], (float[]) a2[i])) return false;
				}
				else if (atype == 'D') { // array of doubles
					if (!arraysEqual((double[]) a1[i], (double[]) a2[i])) return false;
				}
				else if (atype == 'J') { // array of longs
					if (!arraysEqual((long[]) a1[i], (long[]) a2[i])) return false;
				}
				else if (atype == 'S') { // array of shorts
					if (!arraysEqual((short[]) a1[i], (short[]) a2[i])) return false;
				}
				else if (atype == 'B') { // array of bytes
					if (!arraysEqual((byte[]) a1[i], (byte[]) a2[i])) return false;
				}
				else if (atype == 'Z') { // array of booleans
					if (!arraysEqual((boolean[]) a1[i], (boolean[]) a2[i])) return false;
				}
			}
			else if (!objectsEqual(a1[i], a2[i])) return false;
		}
		return true;
	}

	/** Determines whether the two boolean arrays are component-wise equal. */
	public static boolean arraysEqual(final boolean[] a1, final boolean[] a2) {
		if (a1 == null || a2 == null) return a1 == null && a2 == null;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i]) return false;
		}
		return true;
	}

	/** Determines whether the two byte arrays are component-wise equal. */
	public static boolean arraysEqual(final byte[] a1, final byte[] a2) {
		if (a1 == null || a2 == null) return a1 == null && a2 == null;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i]) return false;
		}
		return true;
	}

	/** Determines whether the two char arrays are component-wise equal. */
	public static boolean arraysEqual(final char[] a1, final char[] a2) {
		if (a1 == null || a2 == null) return a1 == null && a2 == null;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i]) return false;
		}
		return true;
	}

	/** Determines whether the two double arrays are component-wise equal. */
	public static boolean arraysEqual(final double[] a1, final double[] a2) {
		if (a1 == null || a2 == null) return a1 == null && a2 == null;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i]) return false;
		}
		return true;
	}

	/** Determines whether the two float arrays are component-wise equal. */
	public static boolean arraysEqual(final float[] a1, final float[] a2) {
		if (a1 == null || a2 == null) return a1 == null && a2 == null;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i]) return false;
		}
		return true;
	}

	/** Determines whether the two int arrays are component-wise equal. */
	public static boolean arraysEqual(final int[] a1, final int[] a2) {
		if (a1 == null || a2 == null) return a1 == null && a2 == null;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i]) return false;
		}
		return true;
	}

	/** Determines whether the two long arrays are component-wise equal. */
	public static boolean arraysEqual(final long[] a1, final long[] a2) {
		if (a1 == null || a2 == null) return a1 == null && a2 == null;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i]) return false;
		}
		return true;
	}

	/** Determines whether the two short arrays are component-wise equal. */
	public static boolean arraysEqual(final short[] a1, final short[] a2) {
		if (a1 == null || a2 == null) return a1 == null && a2 == null;
		if (a1.length != a2.length) return false;
		for (int i = 0; i < a1.length; i++) {
			if (a1[i] != a2[i]) return false;
		}
		return true;
	}

	/**
	 * Determines whether the two objects are equal. In particular, the case where
	 * one or both objects are null is handled properly.
	 */
	public static boolean objectsEqual(final Object o1, final Object o2) {
		if (o1 == null && o2 == null) return true;
		if (o1 == null || o2 == null) return false;
		return o1.equals(o2);
	}

	// -- String-to-array conversions --

	/** Converts the given String into an array of Strings. */
	public static String[] stringToStringArray(final String s) {
		if (s == null || s.equals("null")) return null;
		final StringTokenizer st = new StringTokenizer(s, ",");
		final int len = st.countTokens();
		final String[] a = new String[len];
		for (int i = 0; i < len; i++)
			a[i] = st.nextToken();
		return a;
	}

	/** Converts the given String into an array of booleans. */
	public static boolean[] stringToBooleanArray(final String s) {
		if (s == null || s.equals("null")) return null;
		final StringTokenizer st = new StringTokenizer(s, ",");
		final int len = st.countTokens();
		final boolean[] a = new boolean[len];
		for (int i = 0; i < len; i++)
			a[i] = st.nextToken().equalsIgnoreCase("true");
		return a;
	}

	/** Converts the given String into an array of bytes. */
	public static byte[] stringToByteArray(final String s) {
		if (s == null || s.equals("null")) return null;
		final StringTokenizer st = new StringTokenizer(s, ",");
		final int len = st.countTokens();
		final byte[] a = new byte[len];
		for (int i = 0; i < len; i++)
			a[i] = Byte.parseByte(st.nextToken());
		return a;
	}

	/** Converts the given String into an array of chars. */
	public static char[] stringToCharArray(final String s) {
		if (s == null || s.equals("null")) return null;
		final StringTokenizer st = new StringTokenizer(s, ",");
		final int len = st.countTokens();
		final char[] a = new char[len];
		for (int i = 0; i < len; i++)
			a[i] = st.nextToken().charAt(0);
		return a;
	}

	/** Converts the given String into an array of doubles. */
	public static double[] stringToDoubleArray(final String s) {
		if (s == null || s.equals("null")) return null;
		final StringTokenizer st = new StringTokenizer(s, ",");
		final int len = st.countTokens();
		final double[] a = new double[len];
		for (int i = 0; i < len; i++)
			a[i] = stringToDouble(st.nextToken());
		return a;
	}

	/** Converts the given String into an array of floats. */
	public static float[] stringToFloatArray(final String s) {
		if (s == null || s.equals("null")) return null;
		final StringTokenizer st = new StringTokenizer(s, ",");
		final int len = st.countTokens();
		final float[] a = new float[len];
		for (int i = 0; i < len; i++)
			a[i] = stringToFloat(st.nextToken());
		return a;
	}

	/** Converts the given String into an array of ints. */
	public static int[] stringToIntArray(final String s) {
		if (s == null || s.equals("null")) return null;
		final StringTokenizer st = new StringTokenizer(s, ",");
		final int len = st.countTokens();
		final int[] a = new int[len];
		for (int i = 0; i < len; i++)
			a[i] = Integer.parseInt(st.nextToken());
		return a;
	}

	/** Converts the given String into an array of longs. */
	public static long[] stringToLongArray(final String s) {
		if (s == null || s.equals("null")) return null;
		final StringTokenizer st = new StringTokenizer(s, ",");
		final int len = st.countTokens();
		final long[] a = new long[len];
		for (int i = 0; i < len; i++)
			a[i] = Long.parseLong(st.nextToken());
		return a;
	}

	/** Converts the given String into an array of shorts. */
	public static short[] stringToShortArray(final String s) {
		if (s == null || s.equals("null")) return null;
		final StringTokenizer st = new StringTokenizer(s, ",");
		final int len = st.countTokens();
		final short[] a = new short[len];
		for (int i = 0; i < len; i++)
			a[i] = Short.parseShort(st.nextToken());
		return a;
	}

	// -- String-to-object conversions --

	/** Converts the given string into a double value. */
	public static double stringToDouble(final String s) {
		double d;
		if (s == null || s.equals("NaN")) d = Double.NaN;
		else d = Double.parseDouble(s);
		return d;
	}

	/** Converts the given string into a float value. */
	public static float stringToFloat(final String s) {
		float f;
		if (s == null || s.equals("NaN")) f = Float.NaN;
		else f = Float.parseFloat(s);
		return f;
	}

	/** Converts the given string into a Color object. */
	public static Color stringToColor(final String s) {
		final int[] a = s == null ? null : stringToIntArray(s);
		if (a == null || a.length < 3) return null;
		if (a.length == 3) return new Color(a[0], a[1], a[2]);
		return new Color(a[0], a[1], a[2], a[3]);
	}

	// -- Array-to-string conversions --

	/** Converts the given array of Objects into a String. */
	public static String arrayToString(final Object[] a) {
		if (a == null) return "null";
		if (a.length == 0) return "";
		final StringBuffer sb = new StringBuffer();
		sb.append(a[0].toString());
		for (int i = 1; i < a.length; i++) {
			sb.append(",");
			sb.append(a[i].toString());
		}
		return sb.toString();
	}

	/** Converts the given array of booleans into a String. */
	public static String arrayToString(final boolean[] a) {
		if (a == null) return "null";
		if (a.length == 0) return "";
		final StringBuffer sb = new StringBuffer();
		sb.append(a[0]);
		for (int i = 1; i < a.length; i++) {
			sb.append(",");
			sb.append(a[i]);
		}
		return sb.toString();
	}

	/** Converts the given array of bytes into a String. */
	public static String arrayToString(final byte[] a) {
		if (a == null) return "null";
		if (a.length == 0) return "";
		final StringBuffer sb = new StringBuffer();
		sb.append(a[0]);
		for (int i = 1; i < a.length; i++) {
			sb.append(",");
			sb.append(a[i]);
		}
		return sb.toString();
	}

	/** Converts the given array of chars into a String. */
	public static String arrayToString(final char[] a) {
		if (a == null) return "null";
		if (a.length == 0) return "";
		final StringBuffer sb = new StringBuffer();
		sb.append(a[0]);
		for (int i = 1; i < a.length; i++) {
			sb.append(",");
			sb.append(a[i]);
		}
		return sb.toString();
	}

	/** Converts the given array of doubles into a String. */
	public static String arrayToString(final double[] a) {
		if (a == null) return "null";
		if (a.length == 0) return "";
		final StringBuffer sb = new StringBuffer();
		sb.append(a[0]);
		for (int i = 1; i < a.length; i++) {
			sb.append(",");
			sb.append(a[i]);
		}
		return sb.toString();
	}

	/** Converts the given array of floats into a String. */
	public static String arrayToString(final float[] a) {
		if (a == null) return "null";
		if (a.length == 0) return "";
		final StringBuffer sb = new StringBuffer();
		sb.append(a[0]);
		for (int i = 1; i < a.length; i++) {
			sb.append(",");
			sb.append(a[i]);
		}
		return sb.toString();
	}

	/** Converts the given array of ints into a String. */
	public static String arrayToString(final int[] a) {
		if (a == null) return "null";
		if (a.length == 0) return "";
		final StringBuffer sb = new StringBuffer();
		sb.append(a[0]);
		for (int i = 1; i < a.length; i++) {
			sb.append(",");
			sb.append(a[i]);
		}
		return sb.toString();
	}

	/** Converts the given array of longs into a String. */
	public static String arrayToString(final long[] a) {
		if (a == null) return "null";
		if (a.length == 0) return "";
		final StringBuffer sb = new StringBuffer();
		sb.append(a[0]);
		for (int i = 1; i < a.length; i++) {
			sb.append(",");
			sb.append(a[i]);
		}
		return sb.toString();
	}

	/** Converts the given array of shorts into a String. */
	public static String arrayToString(final short[] a) {
		if (a == null) return "null";
		if (a.length == 0) return "";
		final StringBuffer sb = new StringBuffer();
		sb.append(a[0]);
		for (int i = 1; i < a.length; i++) {
			sb.append(",");
			sb.append(a[i]);
		}
		return sb.toString();
	}

	// -- Object-to-string conversions --

	/** Writes the given color object to the given output stream. */
	public static String colorToString(final Color color) {
		if (color == null) return "null";
		final StringBuffer sb = new StringBuffer();
		sb.append(color.getRed());
		sb.append(",");
		sb.append(color.getGreen());
		sb.append(",");
		sb.append(color.getBlue());
		final int alpha = color.getAlpha();
		if (alpha < 255) {
			sb.append(",");
			sb.append(color.getAlpha());
		}
		return sb.toString();
	}

	// -- Array copying --

	/** Creates a copy of the given boolean array. */
	public static boolean[] copy(final boolean[] a) {
		if (a == null) return null;
		return (boolean[]) copy(a, new boolean[a.length]);
	}

	/** Creates a copy of the given byte array. */
	public static byte[] copy(final byte[] a) {
		if (a == null) return null;
		return (byte[]) copy(a, new byte[a.length]);
	}

	/** Creates a copy of the given char array. */
	public static char[] copy(final char[] a) {
		if (a == null) return null;
		return (char[]) copy(a, new char[a.length]);
	}

	/** Creates a copy of the given double array. */
	public static double[] copy(final double[] a) {
		if (a == null) return null;
		return (double[]) copy(a, new double[a.length]);
	}

	/** Creates a copy of the given float array. */
	public static float[] copy(final float[] a) {
		if (a == null) return null;
		return (float[]) copy(a, new float[a.length]);
	}

	/** Creates a copy of the given int array. */
	public static int[] copy(final int[] a) {
		if (a == null) return null;
		return (int[]) copy(a, new int[a.length]);
	}

	/** Creates a copy of the given long array. */
	public static long[] copy(final long[] a) {
		if (a == null) return null;
		return (long[]) copy(a, new long[a.length]);
	}

	/** Creates a copy of the given short array. */
	public static short[] copy(final short[] a) {
		if (a == null) return null;
		return (short[]) copy(a, new short[a.length]);
	}

	/** Creates a copy of the given array of objects. */
	public static Object[] copy(final Object[] a) {
		if (a == null || a.length == 0) return a;
		return (Object[]) copy(a, Array.newInstance(a[0].getClass(), a.length));
	}

	/** Copies source array into destination array, returning destination. */
	public static Object copy(final Object src, final Object dest) {
		System.arraycopy(src, 0, dest, 0, Array.getLength(src));
		return dest;
	}

}

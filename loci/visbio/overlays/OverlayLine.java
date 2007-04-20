//
// OverlayLine.java
//

/*
VisBio application for visualization of multidimensional
biological image data. Copyright (C) 2002-@year@ Curtis Rueden.

This program is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package loci.visbio.overlays;

import java.awt.Color;
import java.rmi.RemoteException;
import java.util.Arrays;
import loci.visbio.util.MathUtil;
import visad.*;

/** OverlayLine is a measurement line overlay. */
public class OverlayLine extends OverlayObject {

  // -- Static Fields -- 
  
  /** The names of the statistics this object reports */
  protected static String[] statTypes =  {"Coordinates", "Length"};

  // -- Constructors --

  /** Constructs an uninitialized measurement line. */
  public OverlayLine(OverlayTransform overlay) { super(overlay); }

  /** Constructs a measurement line. */
  public OverlayLine(OverlayTransform overlay,
    float x1, float y1, float x2, float y2)
  {
    super(overlay);
    this.x1 = x1;
    this.y1 = y1;
    this.x2 = x2;
    this.y2 = y2;
    computeGridParameters();
  }

  // -- Static methods --

  /** Returns the names of the statistics this object reports */
  public static String[] getStatTypes() {return statTypes;}

  // -- OverlayObject API methods --

  /** Returns whether this object is drawable, i.e., is of nonzero 
  *  size, area, length, etc. 
  */
  public boolean hasData() { return (x1 != x2 || y1 != y2); }

  /** Gets VisAD data object representing this overlay. */
  public DataImpl getData() {
    if (!hasData()) return null;

    RealTupleType domain = overlay.getDomainType();
    TupleType range = overlay.getRangeType();

    float[][] setSamples = {{x1, x2}, {y1, y2}};

    Color col = selected ? GLOW_COLOR : color; 
    float r = col.getRed() / 255f;
    float g = col.getGreen() / 255f;
    float b = col.getBlue() / 255f;

    float[][] rangeSamples = new float[4][setSamples[0].length];
    Arrays.fill(rangeSamples[0], r);
    Arrays.fill(rangeSamples[1], g);
    Arrays.fill(rangeSamples[2], b);
    Arrays.fill(rangeSamples[3], 1.0f);

    FlatField field = null;
    try {
      GriddedSet fieldSet = new Gridded2DSet(domain,
        setSamples, setSamples[0].length, null, null, null, false);
      FunctionType fieldType = new FunctionType(domain, range);
      field = new FlatField(fieldType, fieldSet);
      field.setSamples(rangeSamples);
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }
    return field;
  }

  /** Computes the shortest distance from this object to the given point. */
  public double getDistance(double x, double y) {
    return MathUtil.getDistance(new double[] {x1, y1},
      new double[] {x2, y2}, new double[] {x, y}, true);
  }

  /** Returns a specific statistic of this object */
  public String getStat(String name) {
    if (name.equals("Coordinates")) {
      return "(" + x1 + ", " + y1 + ")-(" + x2 + ", " + y2 + ")";
    } 
    else if (name.equals("Length")) {
      float xx = x2 - x1;
      float yy = y2 - y1;
      float length = (float) Math.sqrt(xx * xx + yy * yy);
      return "" + length;
    } 
    else return "No such statistic for this overlay type";
  }
 
  /** Retrieves useful statistics about this overlay. */
  public String getStatistics() {
    float xx = x2 - x1;
    float yy = y2 - y1;
    float length = (float) Math.sqrt(xx * xx + yy * yy);

    return "Line coordinates = (" + x1 + ", " + y1 +
      ")-(" + x2 + ", " + y2 + ")\n" +
      "Length = " + length;
  }

  /** True iff this overlay has an endpoint coordinate pair. */
  public boolean hasEndpoint() { return true; }

  /** True iff this overlay has a second endpoint coordinate pair. */
  public boolean hasEndpoint2() { return true; }

  // -- Internal OverlayObject API methods --

  /** Computes parameters needed for selection grid computation. */
  protected void computeGridParameters() {
    float padding = 0.02f * overlay.getScalingValue();
    float[] corners1 = computeCorners(x1, y1, x2, y2, padding, 1);
    float[] corners2 = computeCorners(x2, y2, x1, y1, padding, 1);

    xGrid1 = corners1[0]; yGrid1 = corners1[1];
    xGrid2 = corners1[2]; yGrid2 = corners1[3];
    xGrid3 = corners2[2]; yGrid3 = corners2[3];
    xGrid4 = corners2[0]; yGrid4 = corners2[1];
    horizGridCount = 3; vertGridCount = 2;
  }

  // -- Object API methods --

  /** Gets a short string representation of this measurement line. */
  public String toString() { return "Line"; }

  // -- Helper methods --

  /**
   * Helper method for computing coordinates of two corner points of the
   * rectangular selection grid for a line or arrow overlay.
   */
  protected static float[] computeCorners(float x1, float y1,
    float x2, float y2, float padding, float multiplier)
  {
    // multiplier is used to widen the distance between corner points
    // appropriately for the "wide" end of the arrow overlay; for lines,
    // multiplier is 1 (no widening required)

    double xx = x2 - x1;
    double yy = y2 - y1;
    double dist = Math.sqrt(xx * xx + yy * yy);
    double mult = padding / dist;
    float qx = (float) (mult * xx);
    float qy = (float) (mult * yy);

    return new float[] {
      x2 + qx + multiplier * qy, y2 + qy - multiplier * qx,
      x2 + qx - multiplier * qy, y2 + qy + multiplier * qx
    };
  }

  public DataImpl getSelectionGrid() { return getSelectionGrid(false); }

  public DataImpl getSelectionGrid(boolean outline) {
    if (!hasData()) return null;
    else if (outline) return super.getSelectionGrid(outline);

    RealTupleType domain = overlay.getDomainType();
    TupleType range = overlay.getRangeType();

    float delta = GLOW_WIDTH;

    // compute locations of grid points
    // (uses similar triangles instead of raw trig fcns)
    float x = x2 - x1;
    float y = y2 - y1;
    float hyp = (float) Math.sqrt(x * x + y * y);
    float ratio = delta / hyp;
    // offsets from endpoints of line segments 
    float dx1 = ratio * y; 
    float dy1 = ratio * x;
    float dx2 = ratio * x;
    float dy2 = ratio * y;

    float[] p1 = {x1 - dx1 - dx2, y1 + dy1 - dy2};
    float[] p2 = {x2 - dx1 + dx2, y2 + dy1 + dy2};
    float[] p3 = {x1 + dx1 - dx2, y1 - dy1 - dy2};
    float[] p4 = {x2 + dx1 + dx2, y2 - dy1 + dy2};

    float[][] setSamples = {{p1[0], p2[0], p3[0], p4[0]},
                            {p1[1], p2[1], p3[1], p4[1]}};

    // construct range samples;
    Color col = GLOW_COLOR;

    float r = col.getRed() / 255f;
    float g = col.getGreen() / 255f;
    float b = col.getBlue() / 255f;

    float[][] rangeSamples = new float[4][4];
    Arrays.fill(rangeSamples[0], r);
    Arrays.fill(rangeSamples[1], g);
    Arrays.fill(rangeSamples[2], b);
    Arrays.fill(rangeSamples[3], GLOW_ALPHA);

    Gridded2DSet domainSet = null;
    FlatField field = null;
    try {
      domainSet = new Gridded2DSet(domain, setSamples,
            2, 2, null, null, null, false);
      FunctionType fieldType = new FunctionType(domain, range);
      field = new FlatField(fieldType, domainSet);
      field.setSamples(rangeSamples);
    }
    catch (VisADException exc) { exc.printStackTrace(); }
    catch (RemoteException exc) { exc.printStackTrace(); }

    return field;
  }

}

package org.locationtech.jts.noding;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.util.LinearComponentExtracter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Utility methods for processing {@link SegmentString}s.
 * 
 * @author Martin Davis
 *
 */
public class SegmentStringUtil 
{
  /**
   * Extracts all linear components from a given {@link Geometry}
   * to {@link SegmentString}s.
   * The SegmentString data item is set to be the source Geometry.
   * 
   * @param geom the geometry to extract from
   * @return a List of SegmentStrings
   */
  public static List extractSegmentStrings(Geometry geom)
  {
    List segStr = new ArrayList();
    List lines = LinearComponentExtracter.getLines(geom);
    for (Iterator i = lines.iterator(); i.hasNext(); ) {
      LineString line = (LineString) i.next();
      Coordinate[] pts = line.getCoordinates();
      segStr.add(new NodedSegmentString(pts, geom));
    }
    return segStr;
  }

}

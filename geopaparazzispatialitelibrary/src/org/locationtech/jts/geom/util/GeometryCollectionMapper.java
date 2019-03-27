package org.locationtech.jts.geom.util;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Maps the members of a {@link GeometryCollection}
 * into another <tt>GeometryCollection</tt> via a defined
 * mapping function.
 * 
 * @author Martin Davis
 *
 */
public class GeometryCollectionMapper 
{
  public static GeometryCollection map(GeometryCollection gc, GeometryMapper.MapOp op)
  {
    GeometryCollectionMapper mapper = new GeometryCollectionMapper(op);
    return mapper.map(gc);
  }
  
  private GeometryMapper.MapOp mapOp = null;
  
  public GeometryCollectionMapper(GeometryMapper.MapOp mapOp) {
    this.mapOp = mapOp;
  }

  public GeometryCollection map(GeometryCollection gc)
  {
    List mapped = new ArrayList();
    for (int i = 0; i < gc.getNumGeometries(); i++) {
      Geometry g = mapOp.map(gc.getGeometryN(i));
      if (!g.isEmpty())
        mapped.add(g);
    }
    return gc.getFactory().createGeometryCollection(
        GeometryFactory.toGeometryArray(mapped));
  }
}

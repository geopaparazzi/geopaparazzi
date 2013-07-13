package eu.geopaparazzi.spatialite.database.spatial.core;

import java.util.List;

import jsqlite.Exception;
import android.graphics.Paint;

public interface ISpatialDatabaseHandler {

    /**
     * Get the spatial tables from the database.
     * 
     * @param forceRead force a clean read from the db instead of using cached.
     * @return the list of {@link SpatialVectorTable}s.
     * @throws Exception
     */
    public abstract List<SpatialVectorTable> getSpatialVectorTables( boolean forceRead ) throws Exception;

    public abstract List<SpatialRasterTable> getSpatialRasterTables( boolean forceRead ) throws Exception;

    public abstract byte[] getRasterTile( String query );

    /**
     * Get the table's bounds.
     * 
     * @param spatialTable the table to use.
     * @param destSrid the srid to which to project to.
     * @return the bounds as [n,s,e,w].
     * @throws Exception 
     */
    public float[] getTableBounds( SpatialVectorTable spatialTable, String destSrid ) throws Exception;

    /**
     * Get the {@link GeometryIterator} of a table in a given bound.
     * 
     * @param destSrid the srid to which to transform to.
     * @param table the table to use.
     * @param n north bound.
     * @param s south bound.
     * @param e east bound.
     * @param w west bound.
     * @return the geometries iterator.
     */
    public abstract GeometryIterator getGeometryIteratorInBounds( String destSrid, SpatialVectorTable table, double n, double s,
            double e, double w );

    /**
     * Get the stroke {@link Paint} for a given style.
     * 
     * <p>Paints are cached and reused.</p>
     * 
     * @param style the {@link Style} to use.
     * @return the paint.
     */
    public abstract Paint getStrokePaint4Style( Style style );

    /**
     * Get the fill {@link Paint} for a given style.
     * 
     * <p>Paints are cached and reused.</p>
     * 
     * @param style the {@link Style} to use.
     * @return the paint.
     */
    public abstract Paint getFillPaint4Style( Style style );

    public void close() throws Exception;

    public void updateStyle( Style style ) throws Exception;

    public void intersectionToStringBBOX( String boundsSrid, SpatialVectorTable spatialTable, double n, double s, double e,
            double w, StringBuilder sb, String indentStr ) throws Exception;

    public void intersectionToString4Polygon( String boundsSrid, SpatialVectorTable spatialTable, double n, double e,
            StringBuilder sb, String indentStr ) throws Exception;

}
package eu.geopaparazzi.mapsforge.mapsdirmanager.maps.tiles;

import org.mapsforge.android.maps.mapgenerator.MapGenerator;
import org.mapsforge.android.maps.mapgenerator.databaserenderer.DatabaseRenderer;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.MapnikTileDownloader;
import org.mapsforge.android.maps.mapgenerator.tiledownloader.OpenCycleMapTileDownloader;

public enum MapGeneratorInternal {
    /**
     * Map tiles are downloaded from the Mapnik server.
     *
     * @see <a href="http://wiki.openstreetmap.org/wiki/Mapnik">Mapnik</a>
     */
    mapnik,

    /**
     * Map tiles are downloaded from the OpenCycleMap server.
     *
     * @see <a href="http://opencyclemap.org/">OpenCycleMap</a>
     */
    opencyclemap;

    public static MapGenerator createMapGenerator( MapGeneratorInternal mapGeneratorInternal ) {
        switch( mapGeneratorInternal ) {
        case mapnik:
            return new MapnikTileDownloader();
        case opencyclemap:
            return new OpenCycleMapTileDownloader();
        default:
            return new DatabaseRenderer();
        }
    }
}

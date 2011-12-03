package eu.hydrologis.geopaparazzi.osm;

public class OsmImageCache {

    private static OsmImageCache osmImageCache = null;

    private OsmImageCache() {
    }

    public static OsmImageCache getInstance() {
        if (osmImageCache == null) {
            osmImageCache = new OsmImageCache();
        }
        return osmImageCache;
    }
    
    
    
}

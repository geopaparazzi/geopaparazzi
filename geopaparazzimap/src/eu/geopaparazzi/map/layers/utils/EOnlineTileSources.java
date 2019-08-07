package eu.geopaparazzi.map.layers.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Collection of online tile sources and configurations.
 *
 * @author Antonello Andrea (www.hydrologis.com)
 */
@SuppressWarnings("ALL")
public enum EOnlineTileSources {
    //    Google_Maps("Google Maps", "", "", "Google", "https://mt1.google.com/vt/lyrs=m&x={X}&y={Y}&z={Z}", "", "19", "0"), //       
//    Google_Satellite("Google Satellite", "", "", "Google", "https://mt1.google.com/vt/lyrs=s&x={X}&y={Y}&z={Z}", "", "19", "0"), //       
//    Google_Terrain("Google Terrain", "", "", "Google", "https://mt1.google.com/vt/lyrs=t&x={X}&y={Y}&z={Z}", "", "19", "0"), //       
//    Google_Terrain_Hybrid("Google Terrain Hybrid", "", "", "Google", "https://mt1.google.com/vt/lyrs=p&x={X}&y={Y}&z={Z}", "",     
//            "19", "0"), //  
//    Google_Satellite_Hybrid("Google Satellite Hybrid", "", "", "Google", "https://mt1.google.com/vt/lyrs=y&x={X}&y={Y}&z={Z}", "",     
//            "19", "0"), //  
    Stamen_Terrain("Stamen Terrain", "", "", "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under ODbL",
            "https://tile.stamen.com/terrain", "/{Z}/{X}/{Y}.png", "20", "0"), //   
    Stamen_Toner("Stamen Toner", "", "", "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under ODbL",
            "https://tile.stamen.com/toner", "/{Z}/{X}/{Y}.png", "20", "0"), //   
    Stamen_Toner_Light("Stamen Toner Light", "", "",   //$NON-NLS-3$
            "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under ODbL",
            "https://tile.stamen.com/toner-lite", "/{Z}/{X}/{Y}.png", "20", "0"), //   
    Stamen_Watercolor("Stamen Watercolor", "", "",   //$NON-NLS-3$
            "Map tiles by Stamen Design, under CC BY 3.0. Data by OpenStreetMap, under ODbL",
            "https://tile.stamen.com/watercolor", "/{Z}/{X}/{Y}.jpg", "18", "0"), //   
    Wikimedia_Map("Wikimedia Map", "", "", "OpenStreetMap contributors, under ODbL",
            "https://maps.wikimedia.org/osm-intl", "/{Z}/{X}/{Y}.png", "20", "1"), //   
    Wikimedia_Hike_Bike_Map("Wikimedia Hike Bike Map", "", "", "OpenStreetMap contributors, under ODbL",
            "https://tiles.wmflabs.org/hikebike", "/{Z}/{X}/{Y}.png", "17", "1"), //   
    Esri_Boundaries_Places("Esri Boundaries Places", "", "", "Esri",
            "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile", "/{Z}/{Y}/{X}",
            "20", "0"), //   //$NON-NLS-3$
    Esri_Gray_dark("Esri Gray (dark)", "", "", "Esri",
            "https://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Dark_Gray_Base/MapServer/tile", "/{Z}/{Y}/{X}",
            "20", "0"), //  
    Esri_Gray_light("Esri Gray (light)", "", "", "Esri",
            "https://services.arcgisonline.com/ArcGIS/rest/services/Canvas/World_Light_Gray_Base/MapServer/tile", "/{Z}/{Y}/{X}",
            "20", "0"), //  
    Esri_National_Geographic("Esri National Geographic", "", "", "Esri",
            "https://services.arcgisonline.com/ArcGIS/rest/services/NatGeo_World_Map/MapServer/tile", "/{Z}/{Y}/{X}", "20", "0"), //   
    Esri_Ocean("Esri Ocean", "", "", "Esri",
            "https://services.arcgisonline.com/ArcGIS/rest/services/Ocean/World_Ocean_Base/MapServer/tile", "/{Z}/{Y}/{X}", "20",
            "0"), // 
    Esri_Satellite("Esri Satellite", "", "", "Esri",
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile", "/{Z}/{Y}/{X}", "19", "0"), //   
    Esri_Standard("Esri Standard", "", "", "Esri",
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Street_Map/MapServer/tile", "/{Z}/{Y}/{X}", "20", "0"), //   
    Esri_Terrain("Esri Terrain", "", "", "Esri",
            "https://server.arcgisonline.com/ArcGIS/rest/services/World_Terrain_Base/MapServer/tile", "/{Z}/{Y}/{X}", "20", "0"), //   
    Esri_Transportation("Esri Transportation", "", "", "Esri",
            "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Transportation/MapServer/tile", "/{Z}/{Y}/{X}",
            "20", "0"), //  
    Esri_World_Imagery("Esri World Imagery", "", "https://wiki.openstreetmap.org/wiki/Esri", "Esri",
            "https://services.arcgisonline.com/arcgis/rest/services/World_Imagery/MapServer/tile", "/{Z}/{Y}/{X}", "22", "0"), //   
    Esri_Topo_World("Esri Topo World", "", "", "Esri",
            "https://services.arcgisonline.com/ArcGIS/rest/services/World_Topo_Map/MapServer/tile", "/{Z}/{Y}/{X}", "20", "0"), //   
    Open_Street_Map_Standard("Open Street Map", "", "", "OpenStreetMap contributors, CC-BY-SA",
            "https://tile.openstreetmap.org", "/{Z}/{X}/{Y}.png", "19", "0"), //   
    Open_Stree_Map_Cicle("Open Cicle Map", "", "", "OpenStreetMap contributors, CC-BY-SA",
            "https://tile.opencyclemap.org/cycle", "/{Z}/{X}/{Y}.png", "19", "0"), //
    Open_Street_Map_HOT("Open Street Map H.O.T.", "", "", "OpenStreetMap contributors, CC-BY-SA",
            "https://tile.openstreetmap.fr/hot", "/{Z}/{X}/{Y}.png", "19", "0"), //   
    Open_Street_Map_Monochrome("Open Street Map Monochrome", "", "", "OpenStreetMap contributors, CC-BY-SA",
            "https://tiles.wmflabs.org/bw-mapnik", "/{Z}/{X}/{Y}.png", "19", "0"), //   
    //    Strava_All("Strava All", "", "", "OpenStreetMap contributors, CC-BY-SA",   
//            "https://heatmap-external-b.strava.com/tiles/all/bluered","/{Z}/{X}/{Y}.png",  "15", "0"), //   
//    Strava_Run("Strava Run", "", "", "OpenStreetMap contributors, CC-BY-SA",   
//            "https://heatmap-external-b.strava.com/tiles/run/bluered","/{Z}/{X}/{Y}.png?v=19",  "15", "0"), //   
//    Open_Weather_Map_Temperature("Open Weather Map Temperature", "", "", "Map tiles by OpenWeatherMap, under CC BY-SA 4.0",   
//            "https://tile.openweathermap.org/map/temp_new/{Z}/{X}/{Y}.png?APPID=1c3e4ef8e25596946ee1f3846b53218a", "", "19", "0"), //   
//    Open_Weather_Map_Clouds("Open Weather Map Clouds", "", "", "Map tiles by OpenWeatherMap, under CC BY-SA 4.0",   
//            "https://tile.openweathermap.org/map/clouds_new/{Z}/{X}/{Y}.png?APPID=ef3c5137f6c31db50c4c6f1ce4e7e9dd", "", "19",   //$NON-NLS-3$
//            "0"), // 
//    Open_Weather_Map_Wind_Speed("Open Weather Map Wind Speed", "", "", "Map tiles by OpenWeatherMap, under CC BY-SA 4.0",   
//            "https://tile.openweathermap.org/map/wind_new/{Z}/{X}/{Y}.png?APPID=f9d0069aa69438d52276ae25c1ee9893", "", "19", "0"), //   
    CartoDb_Dark_Matter("CartoDb Dark Matter", "", "",   //$NON-NLS-3$
            "Map tiles by CartoDB, under CC BY 3.0. Data by OpenStreetMap, under ODbL.",
            "https://basemaps.cartocdn.com/dark_all", "/{Z}/{X}/{Y}.png", "20", "0"), //   
    CartoDb_Positron("CartoDb Positron", "", "", " Map tiles by CartoDB, under CC BY 3.0. Data by OpenStreetMap, under ODbL.",
            "https://basemaps.cartocdn.com/light_all", "/{Z}/{X}/{Y}.png", "20", "0");

    private String _name;
    private String _attribution;
    private String _url;
    private String _tilePath;
    private String _maxZoom;

    private EOnlineTileSources(String name, String arg1, String arg2, String attribution, String url, String tilePath,
                               String maxZoom, String minZoom) {
        _name = name;
        _attribution = attribution;
        _url = url;
        _tilePath = tilePath;
        _maxZoom = maxZoom;
    }

    public String getName() {
        return _name;
    }

    public String getUrl() {
        return _url;
    }

    public String getTilePath() {
        return _tilePath;
    }

    public String getAttribution() {
        return _attribution;
    }

    public int getMaxZoom() {
        return Integer.parseInt(_maxZoom);
    }

    /**
     * Get a source by its name.
     *
     * @param name the source name.
     * @return the source or OSM if it doesn't exist.
     */
    public static EOnlineTileSources getByName(String name) {
        for (EOnlineTileSources source : values()) {
            if (name.equals(source.getName())) {
                return source;
            }
        }
        return Open_Street_Map_Standard;
    }

    public static List<String> getNames() {
        List<String> names = new ArrayList<>();
        for (EOnlineTileSources source : values()) {
            names.add(source.getName());
        }
        return names;
    }
}

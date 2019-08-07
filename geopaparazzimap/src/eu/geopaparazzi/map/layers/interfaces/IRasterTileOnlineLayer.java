package eu.geopaparazzi.map.layers.interfaces;

import org.oscim.tiling.source.bitmap.BitmapTileSource;

@SuppressWarnings("ALL")
public interface IRasterTileOnlineLayer extends IGpLayer {
//    public String name;
//
//    public String path;
//
//    public Integer alpha;
//
//    public String eraseColor;


    BitmapTileSource.Builder<?> OPENSTREETMAP = BitmapTileSource.builder()
            .url("https://tile.openstreetmap.org")
            .zoomMax(19);

    BitmapTileSource.Builder<?> OPENCYCLEMAP = BitmapTileSource.builder()
            .url("http://tile.opencyclemap.org/cycle")
            .zoomMax(19);

    BitmapTileSource.Builder<?> STAMEN_TONER = BitmapTileSource.builder()
            .url("https://stamen-tiles.a.ssl.fastly.net/toner")
            .zoomMax(19);

    BitmapTileSource.Builder<?> STAMEN_WATERCOLOR = BitmapTileSource.builder()
            .url("https://stamen-tiles.a.ssl.fastly.net/watercolor")
            .tilePath("/{Z}/{X}/{Y}.jpg")
            .zoomMax(19);
}


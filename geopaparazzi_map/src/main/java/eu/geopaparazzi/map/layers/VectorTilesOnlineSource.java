package eu.geopaparazzi.map.layers;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.OverzoomTileDataSource;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;
import org.oscim.tiling.source.mvt.TileDecoder;

@SuppressWarnings("ALL")
public class VectorTilesOnlineSource extends UrlTileSource {

    private static final String DEFAULT_URL = "https://api.maptiler.com/tiles/v3";//NON-NLS
    private static final String DEFAULT_PATH = "/{Z}/{X}/{Y}.pbf";//NON-NLS

    public static class Builder<T extends Builder<T>> extends UrlTileSource.Builder<T> {
        private String locale = "";

        public Builder() {
            super(DEFAULT_URL, DEFAULT_PATH);
            overZoom(20);
        }

        public T locale(String locale) {
            this.locale = locale;
            return self();
        }

        public T url(String url) {
            this.url = url;
            return self();
        }

        public T tilePath(String tilePath) {
            this.tilePath = tilePath;
            return self();
        }

        @Override
        public VectorTilesOnlineSource build() {
            return new VectorTilesOnlineSource(this);
        }
    }

    @SuppressWarnings("rawtypes")
    public static Builder<?> builder() {
        return new Builder();
    }

    private final String locale;

    public VectorTilesOnlineSource(Builder<?> builder) {
        super(builder);
        this.locale = builder.locale;
    }

    public VectorTilesOnlineSource() {
        this(builder());
    }

    public VectorTilesOnlineSource(String urlString) {
        this(builder().url(urlString));
    }

    @Override
    public ITileDataSource getDataSource() {
        return new OverzoomTileDataSource(new UrlTileDataSource(this, new TileDecoder(locale), getHttpEngine()), mOverZoom);
    }
}

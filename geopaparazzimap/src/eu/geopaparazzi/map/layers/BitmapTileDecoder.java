package eu.geopaparazzi.map.layers;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.Tile;
import org.oscim.tiling.ITileDataSink;
import org.oscim.tiling.source.ITileDecoder;

import java.io.IOException;
import java.io.InputStream;

public class BitmapTileDecoder implements ITileDecoder {

    @Override
    public boolean decode(Tile tile, ITileDataSink sink, InputStream is)
            throws IOException {

        Bitmap bitmap = CanvasAdapter.decodeBitmap(is);
        if (!bitmap.isValid()) {
            return false;
        }

        sink.setTileImage(bitmap);
        return true;
    }
}
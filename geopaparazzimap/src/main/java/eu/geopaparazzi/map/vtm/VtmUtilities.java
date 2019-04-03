package eu.geopaparazzi.map.vtm;

import android.content.Context;

import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;

public class VtmUtilities {


    public static Bitmap getBitmapFromResource(Context context, int resourceId) throws IOException {
        InputStream is = null;
        Bitmap bmp = null;
        try {
            is = context.getResources().openRawResource(resourceId);
            float scale = CanvasAdapter.getScale();
            bmp = CanvasAdapter.decodeSvgBitmap(is, (int) (60 * scale), (int) (60 * scale), 100);
            return bmp;
        } finally {
            IOUtils.closeQuietly(is);
        }
    }
}

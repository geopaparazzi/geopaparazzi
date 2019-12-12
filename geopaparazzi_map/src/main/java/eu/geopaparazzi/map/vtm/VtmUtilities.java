package eu.geopaparazzi.map.vtm;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import eu.geopaparazzi.library.util.Compat;

public class VtmUtilities {


    public static Bitmap getBitmapFromResource(Context context, int resourceId) throws IOException {
        Drawable activeGpsMarker = Compat.getDrawable(context, resourceId);
        return AndroidGraphics.drawableToBitmap(activeGpsMarker);
    }


    public static Bitmap decodeBitmap(byte[] imageBytes) throws IOException {
        Bitmap bitmap = CanvasAdapter.decodeBitmap(new ByteArrayInputStream(imageBytes));
        return bitmap;
    }
}

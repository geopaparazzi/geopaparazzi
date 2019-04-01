//package eu.geopaparazzi.mapsforge.utils;
//
//import android.graphics.Color;
//import android.graphics.Rect;
//
//import org.mapsforge.core.graphics.Canvas;
//import org.mapsforge.core.graphics.Paint;
//import org.mapsforge.core.graphics.Path;
//import org.mapsforge.core.model.BoundingBox;
//import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
//import org.mapsforge.map.android.util.AndroidUtil;
//
//import java.util.HashMap;
//
//import eu.geopaparazzi.library.style.ColorUtilities;
//import eu.geopaparazzi.library.style.ColorUtilitiesCompat;
//
//public class MapsforgeUtils {
//    private static HashMap<String, Integer> colorMap = new HashMap<>();
//
//
//    public static void drawRect(Canvas canvas, int x, int y, int width, int height, Paint paint) {
//        Path path = AndroidGraphicFactory.INSTANCE.createPath();
//        path.moveTo(x, y);
//        path.moveTo(x, y + height);
//        path.moveTo(x + width, y + height);
//        path.moveTo(x + width, y);
//        path.close();
//        canvas.drawPath(path, paint);
//    }
//
//    public static void drawRect(Canvas canvas, Rect rect, Paint paint) {
//        Path path = AndroidGraphicFactory.INSTANCE.createPath();
//        int x1 = rect.left;
//        int x2 = rect.right;
//        int y1 = rect.bottom;
//        int y2 = rect.top;
//        path.moveTo(x1, y1);
//        path.moveTo(x1, y2);
//        path.moveTo(x2, y2);
//        path.moveTo(x2, y1);
//        path.close();
//        canvas.drawPath(path, paint);
//    }
//
//    /**
//     * Returns the corresponding color int.
//     *
//     * @param nameOrHex the name of the color as supported in this class, or the hex value.
//     * @return the int color.
//     */
//    public static int toColor(String nameOrHex, int alpha) {
//        if (alpha < 0) alpha = 255;
//        nameOrHex = nameOrHex.trim();
//        if (nameOrHex.startsWith("#")) {
//            int[] rgb = hex2Rgb(nameOrHex);
//
//            int color = AndroidGraphicFactory.INSTANCE.createColor(alpha, rgb[0], rgb[1], rgb[2]);
//            return color;
//        }
//        Integer color = colorMap.get(nameOrHex);
//        if (color == null) {
//            ColorUtilities[] values = ColorUtilities.values();
//            for (ColorUtilities colorUtil : values) {
//                if (colorUtil.name().equalsIgnoreCase(nameOrHex)) {
//                    String hex = colorUtil.getHex();
//
//                    int[] rgb = hex2Rgb(hex);
//                    color = AndroidGraphicFactory.INSTANCE.createColor(alpha, rgb[0], rgb[1], rgb[2]);
//
//                    colorMap.put(nameOrHex, color);
//                    return color;
//                }
//            }
//        }
//        if (color == null) {
//            String hex = ColorUtilitiesCompat.getHex(nameOrHex);
//            if (hex != null) {
//                return toColor(hex, alpha);
//            }
//        }
//        if (color == null) {
//            int[] rgb = hex2Rgb("#42a6ff");
//            color = AndroidGraphicFactory.INSTANCE.createColor(alpha, rgb[0], rgb[1], rgb[2]);
//        }
//        return color;
//    }
//
//    public static int[] hex2Rgb(String colorStr) {
//        return new int[]{
//                Integer.valueOf(colorStr.substring(1, 3), 16),
//                Integer.valueOf(colorStr.substring(3, 5), 16),
//                Integer.valueOf(colorStr.substring(5, 7), 16)
//        };
//    }
//
//    public static boolean contains(BoundingBox containing, BoundingBox toCheck) {
//        return containing.minLongitude < toCheck.minLongitude &&
//                containing.minLatitude < toCheck.minLatitude &&
//                containing.maxLongitude > toCheck.maxLongitude &&
//                containing.maxLatitude > toCheck.maxLatitude;
//    }
//}
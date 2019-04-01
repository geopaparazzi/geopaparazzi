//package eu.geopaparazzi.mapsforge.core.layers;
//
//import android.content.Context;
//import android.graphics.drawable.Drawable;
//
//import org.mapsforge.core.graphics.Bitmap;
//import org.mapsforge.core.graphics.Canvas;
//import org.mapsforge.core.graphics.Color;
//import org.mapsforge.core.graphics.FontFamily;
//import org.mapsforge.core.graphics.FontStyle;
//import org.mapsforge.core.graphics.Paint;
//import org.mapsforge.core.graphics.Style;
//import org.mapsforge.core.model.BoundingBox;
//import org.mapsforge.core.model.Point;
//import org.mapsforge.core.model.Rectangle;
//import org.mapsforge.core.util.MercatorProjection;
//import org.mapsforge.map.android.graphics.AndroidGraphicFactory;
//import org.mapsforge.map.layer.Layer;
//
//import eu.geopaparazzi.library.gps.GpsServiceStatus;
//import eu.geopaparazzi.library.util.Compat;
//import eu.geopaparazzi.mapsforge.utils.MapsforgeUtils;
//import eu.geopaparazzi.spatialite.ISpatialiteTableAndFieldsNames;
//
//import static eu.geopaparazzi.library.util.LibraryConstants.COORDINATE_FORMATTER;
//import static eu.geopaparazzi.library.util.LibraryConstants.DECIMAL_FORMATTER_1;
//
//public class GpsPositionLayer extends Layer implements ISpatialiteTableAndFieldsNames {
//    private Bitmap activeBitmap;
//    private Bitmap staleBitmap;
//    private Bitmap movingBitmap;
//    private int horizontalOffset;
//    private int verticalOffset;
//    private GpsServiceStatus lastGpsServiceStatus = GpsServiceStatus.GPS_OFF;
//    private float[] lastGpsPositionExtras;
//    private int[] lastGpsStatusExtras;
//    private double[] lastGpsPosition;
//
//    private final Paint activeTextPaint;
//    private final Paint staleTextPaint;
//    private boolean showInfo = false;
//
//    public GpsPositionLayer(Context context) {
//        super();
//
//        Drawable activeGpsMarker = Compat.getDrawable(context, eu.geopaparazzi.library.R.drawable.ic_my_location_black_24dp);
//        org.mapsforge.core.graphics.Bitmap activeGpsBitmap = AndroidGraphicFactory.convertToBitmap(activeGpsMarker);
//        Drawable staleGpsMarker = Compat.getDrawable(context, eu.geopaparazzi.library.R.drawable.ic_my_location_grey_24dp);
//        org.mapsforge.core.graphics.Bitmap staleGpsBitmap = AndroidGraphicFactory.convertToBitmap(staleGpsMarker);
//        Drawable movingGpsMarker = Compat.getDrawable(context, eu.geopaparazzi.library.R.drawable.ic_my_location_moving_24dp);
//        org.mapsforge.core.graphics.Bitmap movingGpsBitmap = AndroidGraphicFactory.convertToBitmap(movingGpsMarker);
//
//        this.activeBitmap = activeGpsBitmap;
//        this.staleBitmap = staleGpsBitmap;
//        this.movingBitmap = movingGpsBitmap;
//        this.horizontalOffset = 0;
//        this.verticalOffset = 0;
//
//        activeTextPaint = AndroidGraphicFactory.INSTANCE.createPaint();
//        activeTextPaint.setColor(AndroidGraphicFactory.INSTANCE.createColor(Color.BLACK));
//        activeTextPaint.setStrokeWidth(2);
//        activeTextPaint.setStyle(Style.FILL);
//        activeTextPaint.setTextSize(60f);
//        activeTextPaint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
//
//        int staleColor = MapsforgeUtils.toColor("#898989", -1);
//        staleTextPaint = AndroidGraphicFactory.INSTANCE.createPaint();
//        staleTextPaint.setColor(staleColor);
//        staleTextPaint.setStrokeWidth(2);
//        staleTextPaint.setStyle(Style.FILL);
//        staleTextPaint.setTextSize(60f);
//        staleTextPaint.setTypeface(FontFamily.DEFAULT, FontStyle.BOLD);
//
//    }
//
//    public void toggleInfo(boolean showInfo) {
//        this.showInfo = showInfo;
//    }
//
//
//    @Override
//    public synchronized void draw(BoundingBox boundingBox, byte zoomLevel, Canvas canvas, Point topLeftPoint) {
//
//        Bitmap current = null;
//        Paint textPaint = null;
//        if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
//            if (lastGpsPositionExtras != null && lastGpsPositionExtras[2] != 0) {
//                current = movingBitmap;
//            } else {
//                current = activeBitmap;
//            }
//            textPaint = activeTextPaint;
//        } else if (lastGpsServiceStatus == GpsServiceStatus.GPS_LISTENING__NO_FIX) {
//            current = staleBitmap;
//            textPaint = staleTextPaint;
//        } else {
//            return;
//        }
//
//        if (current != null && !current.isDestroyed() && lastGpsPosition != null) {
//            long mapSize = MercatorProjection.getMapSize(zoomLevel, this.displayModel.getTileSize());
//            double pixelX = MercatorProjection.longitudeToPixelX(lastGpsPosition[0], mapSize);
//            double pixelY = MercatorProjection.latitudeToPixelY(lastGpsPosition[1], mapSize);
//
//            int width = current.getWidth();
//            int height = current.getHeight();
//            int halfBitmapWidth = width / 2;
//            int halfBitmapHeight = height / 2;
//
//            int left = (int) (pixelX - topLeftPoint.x - halfBitmapWidth + this.horizontalOffset);
//            int top = (int) (pixelY - topLeftPoint.y - halfBitmapHeight + this.verticalOffset);
//            int right = left + width;
//            int bottom = top + height;
//
//            Rectangle bitmapRectangle = new Rectangle(left, top, right, bottom);
//            Rectangle canvasRectangle = new Rectangle(0, 0, canvas.getWidth(), canvas.getHeight());
//            if (!canvasRectangle.intersects(bitmapRectangle)) {
//                return;
//            }
//            canvas.drawBitmap(current, left, top);
//
//            if (showInfo) {
//                String string = "lon: " + COORDINATE_FORMATTER.format(lastGpsPosition[0]);
//                int x = right + 5;
//                int y = bottom;
//                canvas.drawText(string, x, y, textPaint);
//                string = "lat: " + COORDINATE_FORMATTER.format(lastGpsPosition[1]);
//                int delta = textPaint.getTextHeight(string);
//                y = (int) (y + delta * 1.4);
//                canvas.drawText(string, x, y, textPaint);
//                string = "elev: " + (int) lastGpsPosition[2] + " m";
//                y = (int) (y + delta * 1.4);
//                canvas.drawText(string, x, y, textPaint);
//
//                if (lastGpsPositionExtras != null) {
//                    //accuracy, speed, bearing.
//                    string = "accuracy: " + DECIMAL_FORMATTER_1.format(lastGpsPositionExtras[0]) + " m";
//                    y = (int) (y + delta * 1.4);
//                    canvas.drawText(string, x, y, textPaint);
//                    string = "speed: " + DECIMAL_FORMATTER_1.format(lastGpsPositionExtras[1]) + " m/s";
//                    y = (int) (y + delta * 1.4);
//                    canvas.drawText(string, x, y, textPaint);
//                    string = "bearing: " + (int) lastGpsPositionExtras[2];
//                    y = (int) (y + delta * 1.4);
//                    canvas.drawText(string, x, y, textPaint);
//                }
//                if (lastGpsStatusExtras != null) {
//                    //maxSatellites, satCount, satUsedInFixCount.
//                    string = "sats: " + lastGpsStatusExtras[1];
//                    y = (int) (y + delta * 1.4);
//                    canvas.drawText(string, x, y, textPaint);
//                    string = "fixsats: " + lastGpsStatusExtras[2];
//                    y = (int) (y + delta * 1.4);
//                    canvas.drawText(string, x, y, textPaint);
//                }
//
//            }
//        }
//    }
//
//
//    @Override
//    public synchronized void onDestroy() {
//        if (this.activeBitmap != null) {
//            this.activeBitmap.decrementRefCount();
//        }
//        if (this.staleBitmap != null) {
//            this.staleBitmap.decrementRefCount();
//        }
//    }
//
//    /**
//     * @param lastGpsServiceStatus
//     * @param lastGpsPosition       lon, lat, elev
//     * @param lastGpsPositionExtras accuracy, speed, bearing.
//     * @param lastGpsStatusExtras   maxSatellites, satCount, satUsedInFixCount.
//     */
//    public void setGpsStatus(GpsServiceStatus lastGpsServiceStatus, double[] lastGpsPosition, float[] lastGpsPositionExtras, int[] lastGpsStatusExtras) {
//        this.lastGpsServiceStatus = lastGpsServiceStatus;
//        this.lastGpsPositionExtras = lastGpsPositionExtras;
//        this.lastGpsStatusExtras = lastGpsStatusExtras;
//        if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
//            this.lastGpsPosition = lastGpsPosition;
//        }
//    }
//
//}

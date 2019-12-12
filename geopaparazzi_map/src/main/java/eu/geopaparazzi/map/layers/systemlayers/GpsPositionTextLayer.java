package eu.geopaparazzi.map.layers.systemlayers;

import android.content.Context;
import android.graphics.drawable.Drawable;

import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.CanvasAdapter;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.backend.canvas.Paint;
import org.oscim.core.GeoPoint;
import org.oscim.layers.marker.ItemizedLayer;
import org.oscim.layers.marker.MarkerItem;
import org.oscim.layers.marker.MarkerSymbol;
import org.oscim.map.Layers;
import org.oscim.map.Map;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.gps.GpsLoggingStatus;
import eu.geopaparazzi.library.gps.GpsServiceStatus;
import eu.geopaparazzi.library.util.Compat;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.map.GPMapView;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.layers.LayerGroups;
import eu.geopaparazzi.map.layers.interfaces.IPositionLayer;
import eu.geopaparazzi.map.layers.interfaces.ISystemLayer;

import static eu.geopaparazzi.library.util.LibraryConstants.COORDINATE_FORMATTER;
import static eu.geopaparazzi.library.util.LibraryConstants.DECIMAL_FORMATTER_1;

public class GpsPositionTextLayer extends ItemizedLayer<MarkerItem> implements ItemizedLayer.OnItemGestureListener<MarkerItem>, IPositionLayer, ISystemLayer {
    private static final int FG_COLOR = 0xFF000000; // 100 percent black. AARRGGBB
    private static final int BG_COLOR = 0x80FF69B4; // 50 percent pink. AARRGGBB
    private static final int TRANSP_WHITE = 0x80FFFFFF; // 50 percent white. AARRGGBB
    public static String NAME = null;
    private static Bitmap notesBitmap;
    private GPMapView mapView;

    private GpsServiceStatus lastGpsServiceStatus = GpsServiceStatus.GPS_OFF;
    private float[] lastGpsPositionExtras;
    private int[] lastGpsStatusExtras;
    private GpsLoggingStatus lastGpsLoggingStatus;
    private double[] lastGpsPosition;

    public GpsPositionTextLayer(GPMapView mapView) {
        super(mapView.map(), getMarkerSymbol(mapView));
        this.mapView = mapView;
        getName(mapView.getContext());
        setOnItemGestureListener(this);

    }

    public static String getName(Context context) {
        if (NAME == null) {
            NAME = context.getString(R.string.layername_gpsinfo);
        }
        return NAME;
    }

    private static MarkerSymbol getMarkerSymbol(GPMapView mapView) {
        Drawable notesDrawable = Compat.getDrawable(mapView.getContext(), eu.geopaparazzi.library.R.drawable.ic_place_accent_24dp);
        notesBitmap = AndroidGraphics.drawableToBitmap(notesDrawable);

        return new MarkerSymbol(notesBitmap, MarkerSymbol.HotspotPlace.CENTER, false);
    }

    public GpsPositionTextLayer(Map map, MarkerSymbol defaultMarker) {
        super(map, defaultMarker);
    }

    public void disable() {
        setEnabled(false);
    }


    public void enable() {
        setEnabled(true);
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        if (item != null) {
            String description = item.getSnippet();
            GPDialogs.infoDialog(mapView.getContext(), description, null);
        }
        return false;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        return false;
    }


    private MarkerSymbol createAdvancedSymbol() {
        final Paint textPainter = CanvasAdapter.newPaint();
        textPainter.setStyle(Paint.Style.FILL);
        textPainter.setColor(Color.BLACK);
        textPainter.setTextSize(70);
        textPainter.setTypeface(Paint.FontFamily.DEFAULT, Paint.FontStyle.NORMAL);


        List<String> lines = new ArrayList<>();
        lines.add("lon: " + COORDINATE_FORMATTER.format(lastGpsPosition[0]));//NON-NLS
        lines.add("lat: " + COORDINATE_FORMATTER.format(lastGpsPosition[1]));//NON-NLS
        lines.add("elev: " + (int) lastGpsPosition[2] + " m");//NON-NLS

        if (lastGpsPositionExtras != null) {
            lines.add("accuracy: " + DECIMAL_FORMATTER_1.format(lastGpsPositionExtras[0]) + " m");//NON-NLS
            lines.add("speed: " + DECIMAL_FORMATTER_1.format(lastGpsPositionExtras[1]) + " m/s");//NON-NLS
            lines.add("bearing: " + (int) lastGpsPositionExtras[2]);//NON-NLS
        }
        if (lastGpsStatusExtras != null) {
            lines.add("sats: " + lastGpsStatusExtras[1]);//NON-NLS
            lines.add("fixsats: " + lastGpsStatusExtras[2]);//NON-NLS
        }
        if (lastGpsLoggingStatus != null) {
            String msg = lastGpsLoggingStatus == GpsLoggingStatus.GPS_DATABASELOGGING_ON ? "on" : "off";//NON-NLS //NON-NLS
            lines.add("logging is " + msg);//NON-NLS
        }

        float refHeight = textPainter.getTextHeight(lines.get(0)) * 1.5f;
        float interline = 1.4f;
        float heightSum = 0;
        float maxWidth = 0;
        for (String line : lines) {
            heightSum += textPainter.getTextHeight(line) + interline;
            maxWidth = Math.max(maxWidth, textPainter.getTextWidth(line));
        }

        int margin = 6;
        int imageWidth = (int) (maxWidth + margin * 3);
        int imageHeight = (int) (lines.size() * refHeight + refHeight);
        Bitmap titleBitmap = CanvasAdapter.newBitmap(imageWidth, imageHeight + margin, 0);
        org.oscim.backend.canvas.Canvas titleCanvas = CanvasAdapter.newCanvas();
        titleCanvas.setBitmap(titleBitmap);

        titleCanvas.fillRectangle(0, 0, imageWidth, imageHeight, TRANSP_WHITE);


        int x = margin * 2;
        int count = 1;
        for (String line : lines) {
            float deltaY = refHeight * count;
            int y = (int) (margin + deltaY);
            titleCanvas.drawText(line, x, y, textPainter);
            count++;
        }
        return (new MarkerSymbol(titleBitmap, MarkerSymbol.HotspotPlace.LOWER_LEFT_CORNER, true));
    }


    /**
     * @param lastGpsServiceStatus
     * @param lastGpsPosition       lon, lat, elev
     * @param lastGpsPositionExtras accuracy, speed, bearing.
     * @param lastGpsStatusExtras   maxSatellites, satCount, satUsedInFixCount.
     * @param lastGpsLoggingStatus
     */
    public void setGpsStatus(GpsServiceStatus lastGpsServiceStatus, double[] lastGpsPosition, float[] lastGpsPositionExtras, int[] lastGpsStatusExtras, GpsLoggingStatus lastGpsLoggingStatus) {
        this.lastGpsServiceStatus = lastGpsServiceStatus;
        this.lastGpsPositionExtras = lastGpsPositionExtras;
        this.lastGpsStatusExtras = lastGpsStatusExtras;
        this.lastGpsLoggingStatus = lastGpsLoggingStatus;
        if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
            this.lastGpsPosition = lastGpsPosition;
        }

        removeAllItems();

        float bearing = 0;
        if (lastGpsPositionExtras != null) {
            bearing = lastGpsPositionExtras[2];
        }


        if (lastGpsServiceStatus == GpsServiceStatus.GPS_FIX) {
            double delta = 0.0001;
            if (lastGpsPositionExtras != null && lastGpsPositionExtras[2] != 0) {
                MarkerItem item = new MarkerItem("", "", new GeoPoint(lastGpsPosition[1] + delta, lastGpsPosition[0] + delta));
                MarkerSymbol sym = createAdvancedSymbol();
                item.setMarker(sym);
                addItem(item);
            } else {
                MarkerItem item = new MarkerItem("", "", new GeoPoint(lastGpsPosition[1] + delta, lastGpsPosition[0] + delta));
                MarkerSymbol sym = createAdvancedSymbol();
                item.setMarker(sym);
                addItem(item);
            }
        } else if (lastGpsServiceStatus == GpsServiceStatus.GPS_LISTENING__NO_FIX) {
            // just remove marker
        }

    }

    @Override
    public String getId() {
        return getName();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public GPMapView getMapView() {
        return mapView;
    }

    @Override
    public void load() {
        Layers layers = map().layers();
        layers.add(this, LayerGroups.GROUP_SYSTEM_TOP.getGroupId());
    }

    @Override
    public void reloadData() {

    }

    @Override
    public JSONObject toJson() throws JSONException {
        return toDefaultJson();
    }

    @Override
    public void dispose() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }
}

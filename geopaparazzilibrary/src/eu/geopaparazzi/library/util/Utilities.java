/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2010  HydroloGIS (www.hydrologis.com)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.geopaparazzi.library.util;

import java.io.File;
import java.util.Date;
import java.util.HashMap;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.Drawable;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Looper;
import android.os.StatFs;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import eu.geopaparazzi.library.database.GPLog;

/**
 * Utilities class.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Utilities {

    private static double originShift = 2 * Math.PI * 6378137 / 2.0;
    private static final double METER_TO_FEET_CONVERSION_FACTOR = 3.2808399;

    /**
     * get unique device id.
     * 
     * @param context  the context to use.
     * @return the unique id.
     */
    public static String getUniqueDeviceId( Context context ) {
        // try to go for the imei
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String id = tm.getDeviceId();
        if (id == null) {
            // try the android id
            id = android.provider.Settings.Secure.getString(context.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID);
        }
        return id;
    }

    /**
     * Checks if we are on the UI thread.
     *
     * @return <code>true</code> if we are on the UI thread.
     */
    public static boolean isInUiThread() {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            // UI Thread
            return true;
        }
        return false;
    }

    /**
     * Convert decimal degrees to exif format.
     *
     * @param decimalDegree the angle in decimal format.
     * @return the exif format string.
     */
    @SuppressWarnings("nls")
    public static String degreeDecimal2ExifFormat( double decimalDegree ) {
        StringBuilder sb = new StringBuilder();
        sb.append((int) decimalDegree);
        sb.append("/1,");
        decimalDegree = (decimalDegree - (int) decimalDegree) * 60;
        sb.append((int) decimalDegree);
        sb.append("/1,");
        decimalDegree = (decimalDegree - (int) decimalDegree) * 60000;
        sb.append((int) decimalDegree);
        sb.append("/1000");
        if (GPLog.LOG) {
            GPLog.addLogEntry("UTILITIES", sb.toString());
        }
        return sb.toString();
    }

    /**
     * Convert exif format to decimal degree.
     *
     * @param exifFormat the exif string of the gps position.
     * @return the decimal degree.
     */
    @SuppressWarnings("nls")
    public static double exifFormat2degreeDecimal( String exifFormat ) {
        // latitude=44/1,10/1,28110/1000
        String[] exifSplit = exifFormat.trim().split(",");

        String[] value = exifSplit[0].split("/");

        double tmp1 = Double.parseDouble(value[0]);
        double tmp2 = Double.parseDouble(value[1]);
        double degree = tmp1 / tmp2;

        value = exifSplit[1].split("/");
        tmp1 = Double.parseDouble(value[0]);
        tmp2 = Double.parseDouble(value[1]);
        double minutes = tmp1 / tmp2;

        value = exifSplit[2].split("/");
        tmp1 = Double.parseDouble(value[0]);
        tmp2 = Double.parseDouble(value[1]);
        double seconds = tmp1 / tmp2;

        double result = degree + (minutes / 60.0) + (seconds / 3600.0);
        return result;
    }

    /**
     * Calculates the hypothenuse as of the Pythagorean theorem.
     *
     * @param d1 the length of the first leg.
     * @param d2 the length of the second leg.
     * @return the length of the hypothenuse.
     */
    public static double pythagoras( double d1, double d2 ) {
        return Math.sqrt(Math.pow(d1, 2.0) + Math.pow(d2, 2.0));
    }

    /**
     * Tries to adapt a value to the supplied type.
     *
     * @param value the value to adapt.
     * @param adaptee the class to adapt to.
     * @return the adapted object or <code>null</code>, if it fails.
     */
    public static <T> T adapt( Object value, Class<T> adaptee ) {
        if (value instanceof Number) {
            Number num = (Number) value;
            if (adaptee.isAssignableFrom(Double.class)) {
                return adaptee.cast(num.doubleValue());
            } else if (adaptee.isAssignableFrom(Float.class)) {
                return adaptee.cast(num.floatValue());
            } else if (adaptee.isAssignableFrom(Integer.class)) {
                return adaptee.cast(num.intValue());
            } else if (adaptee.isAssignableFrom(Long.class)) {
                return adaptee.cast(num.longValue());
            } else if (adaptee.isAssignableFrom(String.class)) {
                return adaptee.cast(num.toString());
            } else {
                throw new IllegalArgumentException();
            }
        } else if (value instanceof String) {
            if (adaptee.isAssignableFrom(Double.class)) {
                try {
                    Double parsed = Double.parseDouble((String) value);
                    return adaptee.cast(parsed);
                } catch (Exception e) {
                    return null;
                }
            } else if (adaptee.isAssignableFrom(Float.class)) {
                try {
                    Float parsed = Float.parseFloat((String) value);
                    return adaptee.cast(parsed);
                } catch (Exception e) {
                    return null;
                }
            } else if (adaptee.isAssignableFrom(Integer.class)) {
                try {
                    Integer parsed = Integer.parseInt((String) value);
                    return adaptee.cast(parsed);
                } catch (Exception e) {
                    return null;
                }
            } else if (adaptee.isAssignableFrom(String.class)) {
                return adaptee.cast(value);
            } else {
                throw new IllegalArgumentException();
            }
        } else {
            throw new IllegalArgumentException("Can't adapt attribute of type: " + value.getClass().getCanonicalName()); //$NON-NLS-1$
        }
    }

    /**
     * Execute a message dialog in an {@link AsyncTask}.
     *
     * @param context the {@link Context} to use.
     * @param msg the message to show.
     * @param okRunnable optional {@link Runnable} to trigger after ok was pressed.
     */
    public static void messageDialog( final Context context, final String msg, final Runnable okRunnable ) {
        new AsyncTask<String, Void, String>(){
            protected String doInBackground( String... params ) {
                return ""; //$NON-NLS-1$
            }

            protected void onPostExecute( String response ) {
                final Dialog dialog = new Dialog(context);
                dialog.setContentView(eu.geopaparazzi.library.R.layout.simpledialog);
                dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                TextView text = (TextView) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogtext);
                text.setText(msg);
                try {
                    Button dialogButton = (Button) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogButtonOK);
                    dialogButton.setOnClickListener(new View.OnClickListener(){
                        public void onClick( View v ) {
                            dialog.dismiss();
                            if (okRunnable != null) {
                                new Thread(okRunnable).start();
                            }
                        }
                    });
                    dialog.show();
                } catch (Exception e) {
                    GPLog.error("UTILITIES", "Error in messageDialog#inPostExecute", e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }.execute((String) null);
    }

    /**
     * A custom dialog.
     */
    public class CustomDialog extends Dialog {
        /**
         * @param context  the context to use.
         * @param view parent view.
         */
        public CustomDialog( Context context, View view ) {
            super(context);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            setContentView(view);
            Drawable drawable = context.getResources().getDrawable(eu.geopaparazzi.library.R.drawable.dialog_background);
            getWindow().getDecorView().setBackgroundDrawable(drawable);
        }
    }

    /**
     * Execute a generic error dialog in an {@link AsyncTask}.
     *
     * @param context the {@link Context} to use.
     * @param t the exception.
     * @param okRunnable optional {@link Runnable} to trigger after ok was pressed.
     */
    public static void errorDialog( final Context context, final Throwable t, final Runnable okRunnable ) {

        new AsyncTask<String, Void, String>(){
            protected String doInBackground( String... params ) {
                return ""; //$NON-NLS-1$
            }

            protected void onPostExecute( String response ) {
                try {
                    AlertDialog.Builder builder = new AlertDialog.Builder(context);
                    builder //
                    .setTitle(t.getLocalizedMessage()).setMessage(Log.getStackTraceString(t))
                            .setIcon(android.R.drawable.ic_dialog_alert).setCancelable(false)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
                                public void onClick( DialogInterface dialog, int id ) {
                                    if (okRunnable != null) {
                                        new Thread(okRunnable).start();
                                    }
                                }
                            });
                    AlertDialog alertDialog = builder.create();
                    alertDialog.show();
                } catch (Exception e) {
                    GPLog.error("UTILITIES", "Error in errorDialog#inPostExecute", e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }.execute((String) null);
    }

    /**
     * Execute a message dialog in an {@link AsyncTask}.
     *
     * @param context the {@link Context} to use.
     * @param msg the message to show.
     * @param yesRunnable optional {@link Runnable} to trigger after yes was pressed.
     * @param noRunnable optional {@link Runnable} to trigger after no was pressed.
     */
    public static void yesNoMessageDialog( final Context context, final String msg, final Runnable yesRunnable,
            final Runnable noRunnable ) {
        new AsyncTask<String, Void, String>(){
            protected String doInBackground( String... params ) {
                return ""; //$NON-NLS-1$
            }

            protected void onPostExecute( String response ) {
                try {
                    final Dialog dialog = new Dialog(context);
                    dialog.setContentView(eu.geopaparazzi.library.R.layout.yesnodialog);
                    dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
                    dialog.setCanceledOnTouchOutside(false);
                    dialog.setCancelable(false);
                    TextView text = (TextView) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogtext);
                    text.setText(msg);
                    Button yesButton = (Button) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogButtonOK);
                    yesButton.setOnClickListener(new View.OnClickListener(){
                        public void onClick( View v ) {
                            dialog.dismiss();
                            if (yesRunnable != null) {
                                new Thread(yesRunnable).start();
                            }
                        }
                    });
                    Button noButton = (Button) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogButtonCancel);
                    noButton.setOnClickListener(new View.OnClickListener(){
                        public void onClick( View v ) {
                            dialog.dismiss();
                            if (noRunnable != null) {
                                new Thread(noRunnable).start();
                            }
                        }
                    });
                    dialog.show();
                } catch (Exception e) {
                    GPLog.error("UTILITIES", "Error in yesNoMessageDialog#inPostExecute", e); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
        }.execute((String) null);
    }

    /**
     * Execute a message dialog in an {@link AsyncTask}.
     *
     * @param context the {@link Context} to use.
     * @param msgId the id of the message to show.
     * @param okRunnable optional {@link Runnable} to trigger after ok was pressed.
     */
    public static void messageDialog( final Context context, final int msgId, final Runnable okRunnable ) {
        String msg = context.getString(msgId);
        messageDialog(context, msg, okRunnable);
    }

    /**
     * A warning dialog.
     *
     * <b>NOT IMPLEMENTED YET, FOR NOW JUST CALLS {@link #messageDialog}</b>
     *
     * @param context  the context to use.
     * @param msg the message.
     * @param okRunnable optional {@link Runnable} to trigger after ok was pressed.
     */
    public static void warningDialog( final Context context, final String msg, final Runnable okRunnable ) {
        messageDialog(context, msg, okRunnable);
    }
    /**
     * A warning dialog.
     *
     * <b>NOT IMPLEMENTED YET, FOR NOW JUST CALLS {@link #messageDialog}</b>
     *
     * @param context  the context to use.
     * @param msgId msg id.
     * @param okRunnable optional {@link Runnable} to trigger after ok was pressed.
     */
    public static void warningDialog( final Context context, final int msgId, final Runnable okRunnable ) {
        messageDialog(context, msgId, okRunnable);
    }

    /**
     * Execute a toast in an {@link AsyncTask}.
     *
     * @param context the {@link Context} to use.
     * @param msg the message to show.
     * @param length toast length.
     */
    public static void toast( final Context context, final String msg, final int length ) {
        new AsyncTask<String, Void, String>(){
            protected String doInBackground( String... params ) {
                return ""; //$NON-NLS-1$
            }

            protected void onPostExecute( String response ) {
                Toast.makeText(context, msg, length).show();
            }
        }.execute((String) null);
    }

    /**
     * Execute a toast in an {@link AsyncTask}.
     *
     * @param context the {@link Context} to use.
     * @param msgId the id of the message to show.
     * @param length toast length.
     */
    public static void toast( final Context context, final int msgId, final int length ) {
        String msg = context.getString(msgId);
        toast(context, msg, length);
    }

    /**
     * Execute a message dialog in an {@link AsyncTask}.
     *
     * @param context the {@link Context} to use.
     * @param title a title for the input dialog.
     * @param message a message to show.
     * @param defaultText a default text to fill in.
     * @param textRunnable optional {@link TextRunnable} to trigger after ok was pressed.
     */
    public static void inputMessageDialog( final Context context, final String title, final String message,
            final String defaultText, final TextRunnable textRunnable ) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(eu.geopaparazzi.library.R.layout.inputdialog);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        TextView text = (TextView) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogtext);
        text.setText(message);
        final EditText editText = (EditText) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogEdittext);
        editText.setText(defaultText);
        Button yesButton = (Button) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogButtonOK);
        yesButton.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                Editable value = editText.getText();
                String newText = value.toString();
                if (newText == null || newText.length() < 1) {
                    newText = defaultText;
                }
                dialog.dismiss();
                if (textRunnable != null) {
                    textRunnable.setText(newText);
                    new Thread(textRunnable).start();
                }
            }
        });
        Button cancelButton = (Button) dialog.findViewById(eu.geopaparazzi.library.R.id.dialogButtonCancel);
        cancelButton.setOnClickListener(new View.OnClickListener(){
            public void onClick( View v ) {
                dialog.dismiss();
            }
        });
        dialog.show();

    }

    /**
     * Ring action.
     * 
     * @param context  if something goes wrong.
     */
    public static void ring( Context context ) {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(context, notification);
        r.play();

    }

    /**
     * Converts TMS tile coordinates to Google Tile coordinates.
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param tx the x tile number.
     * @param ty the y tile number.
     * @param zoom the current zoom level.
     * @return the converted values.
     */
    public static int[] tmsTile2GoogleTile( int tx, int ty, int zoom ) {
        return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
    }

    /**
     * Converts Google tile coordinates to TMS Tile coordinates.
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param tx the x tile number.
     * @param ty the y tile number.
     * @param zoom the current zoom level.
     * @return the converted values.
     */
    public static int[] googleTile2TmsTile( int tx, int ty, int zoom ) {
        return new int[]{tx, (int) ((Math.pow(2, zoom) - 1) - ty)};
    }

    /**
     * Converts TMS tile coordinates to Microsoft QuadTree.
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     * 
     * @param tx tile x.
     * @param ty tile y.
     * @param zoom zoomlevel.
     *
     * @return the quadtree key.
     */
    public static String quadTree( int tx, int ty, int zoom ) {
        String quadKey = ""; //$NON-NLS-1$
        ty = (int) ((Math.pow(2, zoom) - 1) - ty);
        for( int i = zoom; i < 0; i-- ) {
            int digit = 0;
            int mask = 1 << (i - 1);
            if ((tx & mask) != 0) {
                digit += 1;
            }
            if ((ty & mask) != 0) {
                digit += 2;
            }
            quadKey += (digit + ""); //$NON-NLS-1$
        }
        return quadKey;
    }

    /**
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param tx tile x.
     * @param ty tile y.
     * @param zoom zoomlevel.
     * @param tileSize tile size.
     * @return [minx, miny, maxx, maxy]
     */
    public static double[] tileLatLonBounds( int tx, int ty, int zoom, int tileSize ) {
        double[] bounds = tileBounds(tx, ty, zoom, tileSize);
        double[] mins = metersToLatLon(bounds[0], bounds[1]);
        double[] maxs = metersToLatLon(bounds[2], bounds[3]);
        return new double[]{mins[1], maxs[0], maxs[1], mins[0]};
    }

    /**
     * Returns bounds of the given tile in EPSG:900913 coordinates
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     *
     * @param tx tile x.
     * @param ty tile y.
     * @param zoom zoomlevel.
     * @param tileSize tile size.
     * @return [minx, miny, maxx, maxy]
     */
    public static double[] tileBounds( int tx, int ty, int zoom, int tileSize ) {
        double[] min = pixelsToMeters(tx * tileSize, ty * tileSize, zoom, tileSize);
        double minx = min[0], miny = min[1];
        double[] max = pixelsToMeters((tx + 1) * tileSize, (ty + 1) * tileSize, zoom, tileSize);
        double maxx = max[0], maxy = max[1];
        return new double[]{minx, miny, maxx, maxy};
    }

    /**
     * Converts XY point from Spherical Mercator EPSG:900913 to lat/lon in WGS84
     * Datum
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     * 
     * @param mx x 
     * @param my y
     * @return lat long 
     */
    public static double[] metersToLatLon( double mx, double my ) {

        double lon = (mx / originShift) * 180.0;
        double lat = (my / originShift) * 180.0;

        lat = 180 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return new double[]{-lat, lon};
    }
    /**
    * Equatorial radius of earth is required for distance computation.
    */
    public static final double EQUATORIALRADIUS = 6378137.0;
    /**
     * Convert a longitude coordinate (in degrees) to a horizontal distance in meters from the
     * zero meridian
     *
     * @param longitude
     *            in degrees
     * @return longitude in meters in spherical mercator projection
     */
    public static double longitudeToMetersX( double longitude ) {
        return EQUATORIALRADIUS * java.lang.Math.toRadians(longitude);
    }

    /**
     * Convert a meter measure to a longitude
     *
     * @param x
     *            in meters
     * @return longitude in degrees in spherical mercator projection
     */
    public static double metersXToLongitude( double x ) {
        return java.lang.Math.toDegrees(x / EQUATORIALRADIUS);
    }

    /**
     * Convert a meter measure to a latitude
     *
     * @param y
     *            in meters
     * @return latitude in degrees in spherical mercator projection
     */
    public static double metersYToLatitude( double y ) {
        return java.lang.Math.toDegrees(java.lang.Math.atan(java.lang.Math.sinh(y / EQUATORIALRADIUS)));
    }

    /**
     * Convert a latitude coordinate (in degrees) to a vertical distance in meters from the
     * equator
     *
     * @param latitude
     *            in degrees
     * @return latitude in meters in spherical mercator projection
     */
    public static double latitudeToMetersY( double latitude ) {
        return EQUATORIALRADIUS
                * java.lang.Math.log(java.lang.Math.tan(java.lang.Math.PI / 4 + 0.5 * java.lang.Math.toRadians(latitude)));
    }
    /**
    * Convert a east-longitude,west-longitude coordinate (in degrees) to distance in meters
    *
    * @param east_longitude longitude in degrees
    * @param west_longitude longitude in degrees
    * @return meters in spherical mercator projection
    */
    public static double longitudeToMeters( double east_longitude, double west_longitude ) {
        return longitudeToMetersX(east_longitude) - longitudeToMetersX(west_longitude);
    }
    /**
    * Convert a north-latitude,south-latitude coordinate (in degrees) to distance in meters
    *
    * @param north_latitude latitude in degrees
    * @param south_latitude latitude in degrees
    * @return meters in spherical mercator projection
    */
    public static double latitudeToMeters( double north_latitude, double south_latitude ) {
        return latitudeToMetersY(north_latitude) - latitudeToMetersY(south_latitude);
    }
    /**
     * Converts pixel coordinates in given zoom level of pyramid to EPSG:900913
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     * @param px pixel x.
     * @param py  pixel y.
     * @param zoom zoomlevel.
     * @param tileSize tile size.
     * @return converted coordinate.
     */
    public static double[] pixelsToMeters( double px, double py, int zoom, int tileSize ) {
        double res = getResolution(zoom, tileSize);
        double mx = px * res - originShift;
        double my = py * res - originShift;
        return new double[]{mx, my};
    }

    /**
     * Resolution (meters/pixel) for given zoom level (measured at Equator)
     *
     * <p>Code copied from: http://code.google.com/p/gmap-tile-generator/</p>
     * 
     * @param zoom zoomlevel.
     * @param tileSize tile size.
     * @return resolution.
     */
    public static double getResolution( int zoom, int tileSize ) {
        // return (2 * Math.PI * 6378137) / (this.tileSize * 2**zoom)
        double initialResolution = 2 * Math.PI * 6378137 / tileSize;
        return initialResolution / Math.pow(2, zoom);
    }

    /**
     * Convert unsafe chars.
     * 
     * @param string text to check.
     * @return safe text.
     */
    @SuppressWarnings("nls")
    public static String makeXmlSafe( String string ) {
        if (string == null)
            return "";
        string = string.replaceAll("&", "&amp;");
        return string;
    }

    /**
     * A string formatter.
     *
     * <p>This method exists, because the method to use is not definitive yet.</p>
     * <p>
     * Currently the format of the substitutes in the message are:
     * <ul>
     *   <li>%1$d = for numeric</li>
     *   <li>%1$s = for strings</li>
     * </ul>
     * The %1, %2, etc refer to the number of the args.
     * </p>
     *
     * @param msg the message.
     * @param args the args to substitute.
     * @return the formatted string.
     */
    public static String format( String msg, String... args ) {
        String msgFormat = String.format(msg, (Object[]) args);
        return msgFormat;
    }

    /**
     * Convert bytes to hex string.
     *
     * @param b the bytes array to convert.
     * @param size the size of the array to consider.
     * @return the hex string.
     */
    public static String getHexString( byte[] b, int size ) {
        if (size < 1) {
            size = b.length;
        }
        StringBuilder sb = new StringBuilder();
        for( int i = size - 1; i >= 0; i-- ) {
            if (i >= 0)
                sb.append(Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    /**
     * Get the megabytes available in the filesystem at 'file'.
     *
     * @param file the filesystem's path.
     * @return the available space in mb.
     */
    public static float getAvailableMegabytes( File file ) {
        StatFs stat = new StatFs(file.getPath());
        long bytesAvailable = (long) stat.getBlockSize() * (long) stat.getAvailableBlocks();
        return bytesAvailable / (1024.f * 1024.f);
    }

    /**
     * Get the size in megabytes of the filesystem at 'file'.
     *
     * @param file the filesystem's path.
     * @return the size in mb.
     */
    public static float getFilesystemMegabytes( File file ) {
        StatFs stat = new StatFs(file.getPath());
        long bytes = (long) stat.getBlockSize() * (long) stat.getBlockCount();
        return bytes / (1024.f * 1024.f);
    }

    /**
     * Convert meters to feet.
      *
      * @param meters the value in meters to convert to feet.
      * @return meters converted to feet.
      */
    public static double toFeet( final double meters ) {
        return meters * METER_TO_FEET_CONVERSION_FACTOR;
    }

    /**
     * Create an OSM url from coordinates.
     *
     * @param lat lat
     * @param lon lon
     * @param withMarker if <code>true</code>, marker is added.
     * @param withGeosmsParam if <code>true</code>, geosms params are added.
     * @return url string.
     */
    @SuppressWarnings("nls")
    public static String osmUrlFromLatLong( float lat, float lon, boolean withMarker, boolean withGeosmsParam ) {
        StringBuilder sB = new StringBuilder();
        sB.append("http://www.osm.org/?lat=");
        sB.append(lat);
        sB.append("&lon=");
        sB.append(lon);
        sB.append("&zoom=14");
        if (withMarker) {
            sB.append("&layers=M&mlat=");
            sB.append(lat);
            sB.append("&mlon=");
            sB.append(lon);
        }
        if (withGeosmsParam) {
            sB.append("&GeoSMS");
        }
        return sB.toString();
    }

    /**
     * Method to help define file names that need to be hidden.
     *
     * <p>Currently ones that start with _ are hidden.</p>
     *
     * @param name the name to check.
     * @return <code>true</code> if the name defines a file to hide.
     */
    public static boolean isNameFromHiddenFile( String name ) {
        return name.startsWith("_"); //$NON-NLS-1$
    }

    /**
     * Dismiss {@link ProgressDialog} with check in one line.
     * 
     * @param progressDialog the dialog to dismiss.
     */
    public static void dismissProgressDialog( ProgressDialog progressDialog ) {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }

    /**
     * Gets the data from a gmap url.
     * 
     * @param url the url to parse.
     * @return an array with [lat, lon, text] or <code>null</code>.
     */
    @SuppressWarnings("nls")
    public static String[] getLatLonTextFromGmapUrl( String url ) {
        String googleMapsUrl = "http://maps.google.com/maps?q=";
        if (url.startsWith(googleMapsUrl)) {
            // google maps url
            String relativePath = url.substring(googleMapsUrl.length());

            // check if there is a dash for adding text
            String textStr = new Date().toLocaleString();
            int lastDashIndex = relativePath.lastIndexOf('#');
            if (lastDashIndex != -1) {
                // everything after a dash is taken as text
                textStr = relativePath.substring(lastDashIndex + 1);
                relativePath = relativePath.substring(0, lastDashIndex);
            }

            int indexOfAmp = relativePath.indexOf('&');
            String coordsStr = null;
            if (indexOfAmp == -1) {
                // no other &
                coordsStr = relativePath;
            } else {
                coordsStr = relativePath.substring(0, indexOfAmp);
            }
            String[] split = coordsStr.split(",");
            if (split.length == 2) {
                try {
                    double lat = Double.parseDouble(split[0]);
                    double lon = Double.parseDouble(split[1]);

                    return new String[]{String.valueOf(lat), String.valueOf(lon), textStr};
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        } else {
            return null;
        }
        return null;
    }

    /**
     * Gets the data from a osm url.
     * 
     * @param urlString the url to parse.
     * @return an array with [lat, lon, text, zoom] or <code>null</code>.
     */
    @SuppressWarnings("nls")
    public static String[] getLatLonTextFromOsmUrl( String urlString ) {
        // http://www.openstreetmap.org/?mlat=42.082&mlon=9.822#map=6/42.082/9.822&layers=N
        String osmMapsUrl = "http://www.openstreetmap.org";
        if (urlString.startsWith(osmMapsUrl)) {
            String[] urlSplit = urlString.split("#|&|\\?");
            HashMap<String, String> paramsMap = new HashMap<String, String>();
            for( String string : urlSplit ) {
                if (string.indexOf('=') != -1) {
                    String[] keyValue = string.split("=");
                    if (keyValue.length == 2) {
                        paramsMap.put(keyValue[0].toLowerCase(), keyValue[1]);
                    }
                }
            }

            // check if there is a dash for adding text
            String textStr = new Date().toLocaleString();
            int lastDashIndex = urlString.lastIndexOf('#');
            if (lastDashIndex != -1) {
                // everything after a dash is taken as text
                String tmpTextStr = urlString.substring(lastDashIndex + 1);
                if (!tmpTextStr.startsWith("map=")) {
                    textStr = tmpTextStr;
                }
            }

            String coordsStr = paramsMap.get("map");
            if (coordsStr != null) {
                String[] split = coordsStr.split("/");
                if (split.length == 3) {
                    try {
                        double lat = Double.parseDouble(split[1]);
                        double lon = Double.parseDouble(split[2]);
                        int zoom = (int) Double.parseDouble(split[0]);
                        return new String[]{String.valueOf(lat), String.valueOf(lon), textStr, String.valueOf(zoom)};
                    } catch (NumberFormatException e) {
                        return null;
                    }
                }
            }
        } else {
            return null;
        }
        return null;
    }
}

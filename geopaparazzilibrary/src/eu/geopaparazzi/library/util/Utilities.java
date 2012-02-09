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

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.text.Editable;
import android.widget.EditText;
import android.widget.Toast;
import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.debug.Debug;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * Utilities class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Utilities {

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
        if (Debug.D) {
            Logger.i("UTILITIES", sb.toString());
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
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setMessage(msg).setCancelable(false)
                        .setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener(){
                            public void onClick( DialogInterface dialog, int id ) {
                                if (okRunnable != null) {
                                    new Thread(okRunnable).start();
                                }
                            }
                        });
                AlertDialog alertDialog = builder.create();
                alertDialog.show();
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
     * Execute a toast in an {@link AsyncTask}.
     * 
     * @param context the {@link Context} to use.
     * @param msg the message to show.
     * @param okRunnable optional {@link Runnable} to trigger after ok was pressed. 
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
     * @param okRunnable optional {@link Runnable} to trigger after ok was pressed. 
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
        final EditText input = new EditText(context);
        input.setText(defaultText);
        Builder builder = new AlertDialog.Builder(context).setTitle(title);
        builder.setMessage(message);
        builder.setView(input);
        builder.setIcon(android.R.drawable.ic_dialog_alert)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                    }
                }).setPositiveButton(R.string.ok, new DialogInterface.OnClickListener(){
                    public void onClick( DialogInterface dialog, int whichButton ) {
                        Editable value = input.getText();
                        String newText = value.toString();
                        if (newText == null || newText.length() < 1) {
                            newText = defaultText;
                        }
                        if (textRunnable != null) {
                            textRunnable.setText(newText);
                            new Thread(textRunnable).start();
                        }
                    }
                }).setCancelable(false).show();
    }
}

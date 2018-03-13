/*
 * Geopaparazzi - Digital field mapping on Android based devices
 * Copyright (C) 2016  HydroloGIS (www.hydrologis.com)
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Looper;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v4.content.FileProvider;
import android.telephony.TelephonyManager;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;

/**
 * Utilities class.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Utilities {

    public static final String GEOPAPARAZZI_LIBRARY_FILEPROVIDER_PLUS = ".library.fileprovider";

    public static String getLastFilePath(Context context) throws Exception {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getString(LibraryConstants.PREFS_KEY_LASTPATH, ResourcesManager.getInstance(context).getMainStorageDir().getAbsolutePath());
    }

    public static void setLastFilePath(Context context, String lastPath) throws Exception {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        File file = new File(lastPath);
        if (file.exists()) {
            if (!file.isDirectory()) {
                file = file.getParentFile();
            }
        }
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(LibraryConstants.PREFS_KEY_LASTPATH, file.getAbsolutePath());
        editor.apply();
    }

    public static Uri getFileUriInApplicationFolder(Context context, File imageFile) throws Exception {
        String packageName = ResourcesManager.getInstance(context).getPackageName();

        Uri outputFileUri = FileProvider.getUriForFile(context, packageName + GEOPAPARAZZI_LIBRARY_FILEPROVIDER_PLUS, imageFile);
        return outputFileUri;
    }


    /**
     * get unique device id.
     *
     * @param context the context to use.
     * @return the unique id.
     */
    public static String getUniqueDeviceId(Context context) {
        String id = null;


        try {
            // try to go for the imei
            TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
            id = tm.getDeviceId();
            if (id != null) {
                return id;
            }
        } catch (Exception e) {
            // ignore and try next
        }


        try {
            id = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ANDROID_ID);
            if (id != null) {
                return id;
            }
        } catch (Exception e) {
            // ignore and go on
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
    public static String degreeDecimal2ExifFormat(double decimalDegree) {
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
    public static double exifFormat2degreeDecimal(String exifFormat) {
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
    public static double pythagoras(double d1, double d2) {
        return Math.sqrt(Math.pow(d1, 2.0) + Math.pow(d2, 2.0));
    }

    /**
     * Tries to adapt a value to the supplied type.
     *
     * @param value   the value to adapt.
     * @param adaptee the class to adapt to.
     * @return the adapted object or <code>null</code>, if it fails.
     */
    public static <T> T adapt(Object value, Class<T> adaptee) {
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
     * Ring action.
     *
     * @param context if something goes wrong.
     */
    public static void ring(Context context) {
        Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(context, notification);
        r.play();

    }


    /**
     * Convert unsafe chars.
     *
     * @param string text to check.
     * @return safe text.
     */
    @SuppressWarnings("nls")
    public static String makeXmlSafe(String string) {
        if (string == null)
            return "";
        string = string.replaceAll("&", "&amp;");
        return string;
    }

    /**
     * A string formatter.
     * <p/>
     * <p>This method exists, because the method to use is not definitive yet.</p>
     * <p>
     * Currently the format of the substitutes in the message are:
     * <ul>
     * <li>%1$d = for numeric</li>
     * <li>%1$s = for strings</li>
     * </ul>
     * The %1, %2, etc refer to the number of the args.
     * </p>
     *
     * @param msg  the message.
     * @param args the args to substitute.
     * @return the formatted string.
     */
    public static String format(String msg, String... args) {
        String msgFormat = String.format(msg, (Object[]) args);
        return msgFormat;
    }

    /**
     * Convert bytes to hex string.
     *
     * @param b    the bytes array to convert.
     * @param size the size of the array to consider.
     * @return the hex string.
     */
    public static String getHexString(byte[] b, int size) {
        if (size < 1) {
            size = b.length;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = size - 1; i >= 0; i--) {
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
    public static float getAvailableMegabytes(File file) {
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
    public static float getFilesystemMegabytes(File file) {
        StatFs stat = new StatFs(file.getPath());
        long bytes = (long) stat.getBlockSize() * (long) stat.getBlockCount();
        return bytes / (1024.f * 1024.f);
    }


    /**
     * Method to help define file names that need to be hidden.
     * <p/>
     * <p>Currently ones that start with _ are hidden.</p>
     *
     * @param name the name to check.
     * @return <code>true</code> if the name defines a file to hide.
     */
    public static boolean isNameFromHiddenFile(String name) {
        return name.startsWith("_"); //$NON-NLS-1$
    }

    /**
     * Serialize an object.
     *
     * @param obj the object to serialize.
     * @return the byte array.
     * @throws IOException
     */
    public static byte[] serializeObject(Object obj) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream out = new ObjectOutputStream(bos);
        out.writeObject(obj);
        out.close();
        return bos.toByteArray();
    }

    /**
     * Deserialize an array of bytes.
     *
     * @param bytes the array to convert.
     * @param clazz the class contained in the array.
     * @return the converted object.
     * @throws Exception
     */
    public static <T> T deserializeObject(byte[] bytes, Class<T> clazz) throws Exception {
        ObjectInputStream in = null;
        try {
            in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            return clazz.cast(in.readObject());
        } finally {
            if (in != null)
                in.close();
        }
    }

}

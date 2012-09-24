package eu.hydrologis.geopaparazzi.util;

import static eu.hydrologis.geopaparazzi.util.Constants.DECIMATION_FACTOR;
import static eu.hydrologis.geopaparazzi.util.Constants.PANICKEY;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class GpUtilities {

    public static int getDecimationFactor( Context context ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String decimationFactorStr = preferences.getString(DECIMATION_FACTOR, "5"); //$NON-NLS-1$
        int decimationFactor = 5;
        try {
            decimationFactor = Integer.parseInt(decimationFactorStr);
        } catch (Exception e) {
            // use default
        }
        return decimationFactor;
    }

    /**
     * Gets the panic numbers from the preferences.
     * 
     * @param context the {@link Context} to use.
     * @return the array of numbers or null.
     */
    @SuppressWarnings("nls")
    public static String[] getPanicNumbers( Activity context ) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        final String panicNumbersString = preferences.getString(PANICKEY, "");
        // Make sure there's a valid return address.
        if (panicNumbersString == null || panicNumbersString.length() == 0 || panicNumbersString.matches(".*[A-Za-z].*")) {
            return null;
        } else {
            String[] numbers = panicNumbersString.split(";");
            return numbers;
        }
    }
}

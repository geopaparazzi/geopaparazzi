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
package eu.hydrologis.geopaparazzi.util;

import eu.hydrologis.geopaparazzi.R;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.Button;

/**
 * A toggle button for the log.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LogToggleButton extends Button {
    protected boolean isChecked = false;
    private Drawable gps;
    private Drawable gpsOn;
    private String startGpsStr;
    private String stopGpsStr;

    public LogToggleButton( Context context ) {
        super(context);
        init();
    }

    public LogToggleButton( Context context, AttributeSet attrs ) {
        super(context, attrs);
        init();
    }

    private void init() {
        gps = getResources().getDrawable(R.drawable.gps);
        gps.setBounds(0, 0, gps.getIntrinsicWidth(), gps.getIntrinsicHeight());
        gpsOn = getResources().getDrawable(R.drawable.gps_on);
        gpsOn.setBounds(0, 0, gpsOn.getIntrinsicWidth(), gpsOn.getIntrinsicHeight());
        startGpsStr = getResources().getString(R.string.text_start_gps_logging);
        stopGpsStr = getResources().getString(R.string.text_stop_gps_logging);
        setCompoundDrawables(null, gps, null, null);
        setText(startGpsStr);
    }

    public boolean isChecked() {
        return this.isChecked;
    }

    public boolean performClick() {
        this.isChecked = !this.isChecked;
        if (isChecked) {
            setCompoundDrawables(null, gpsOn, null, null);
            setText(stopGpsStr);
        } else {
            setCompoundDrawables(null, gps, null, null);
            setText(startGpsStr);
        }
        return super.performClick();
    }

}

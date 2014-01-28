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
package eu.geopaparazzi.library.forms;

import java.text.DecimalFormat;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.TextView;
import android.widget.TimePicker;

/**
 * A time picker fragment.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FormTimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

    private TextView timeView;
    private int hourOfDay;
    private int minute;
    private boolean is24;

    /**
     * Set attributes.
     *
     * @param hourOfDay hour
     * @param minute minute
     * @param is24 format
     * @param timeView view
     */
    public void setAttributes( int hourOfDay, int minute, boolean is24, TextView timeView ) {
        this.hourOfDay = hourOfDay;
        this.minute = minute;
        this.is24 = is24;
        this.timeView = timeView;
    }

    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState ) {
        return new TimePickerDialog(getActivity(), this, hourOfDay, minute, is24);
    }

    @Override
    public void onTimeSet( TimePicker arg0, int hourOfDay, int minute ) {
        DecimalFormat decimalFormatter = new DecimalFormat("00"); //$NON-NLS-1$

        StringBuilder sb = new StringBuilder();
        sb.append(decimalFormatter.format(hourOfDay));
        sb.append(":"); //$NON-NLS-1$
        sb.append(decimalFormatter.format(minute));
        String dateStr = sb.toString();
        timeView.setText(dateStr);
    }
}
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

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.widget.DatePicker;
import android.widget.TextView;

/**
 * A date picker fragment.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FormDatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

    private final int year;
    private final int month;
    private final int day;
    private final TextView dateView;

    /**
     * constructor
     * 
     * @param year the year as of Calendar.get(Calendar.YEAR).
     * @param month the month as of Calendar.get(Calendar.MONTH).
     * @param day the day as of Calendar.get(Calendar.DAY).
     * @param dateView the {@link TextView} to update.
     */
    public FormDatePickerFragment( int year, int month, int day, TextView dateView ) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.dateView = dateView;
    }

    @Override
    public Dialog onCreateDialog( Bundle savedInstanceState ) {
        return new DatePickerDialog(getActivity(), this, year, month, day);
    }

    public void onDateSet( DatePicker view, int year, int month, int day ) {
        DecimalFormat decimalFormatter = new DecimalFormat("00");; //$NON-NLS-1$

        StringBuilder sb = new StringBuilder();
        sb.append(year);
        sb.append("-"); //$NON-NLS-1$
        sb.append(decimalFormatter.format(month + 1));
        sb.append("-"); //$NON-NLS-1$
        sb.append(decimalFormatter.format(day));
        String dateStr = sb.toString();
        dateView.setText(dateStr);
    }
}
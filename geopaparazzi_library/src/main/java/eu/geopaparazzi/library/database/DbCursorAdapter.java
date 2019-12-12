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

package eu.geopaparazzi.library.database;

import android.content.Context;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.cursoradapter.widget.CursorAdapter;

import eu.geopaparazzi.library.R;
import eu.geopaparazzi.library.util.Compat;

/**
 * A cursor adapter generic to given field names.
 *
 * <p><b>Note that it ignores fields named _id.</b></p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DbCursorAdapter extends CursorAdapter {

    public DbCursorAdapter(Context context, Cursor c) {
        super(context, c, false);
    }

    public void bindView(View view, Context context, Cursor cursor) {
        StringBuilder sb = new StringBuilder();
        TextView textView = (TextView) view;
        for (int i = 0; i < cursor.getColumnCount(); i++) {
            String field = cursor.getColumnName(i);
            if (field.equals("_id")) {//NON-NLS
                continue;
            }
            String value = cursor.getString(i);
            sb.append("\n").append(field).append(" = ").append(value);
        }
        if (sb.length() > 1) {
            String text = sb.substring(1);
            textView.setText(text);
        } else {
            textView.setText(" - nv - ");//NON-NLS
        }
        textView.setTextColor(Compat.getColor(context, R.color.main_text_color));
        textView.setPadding(5, 5, 5, 5);
    }

    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Inflate your view here.
        TextView view = new TextView(context);
        return view;
    }
}
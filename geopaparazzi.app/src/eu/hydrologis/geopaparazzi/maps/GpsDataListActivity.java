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
package eu.hydrologis.geopaparazzi.maps;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ColorUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.util.Constants;

/**
 * Gpx listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsDataListActivity extends ListActivity {
    private static final int SELECTALL = 1;
    private static final int UNSELECTALL = 2;
    private static final int MERGE_SELECTED = 3;

    private static final int GPSDATAPROPERTIES_RETURN_CODE = 668;

    private LogMapItem[] gpslogItems;
    private Comparator<MapItem> mapItemSorter = new ItemComparators.MapItemIdComparator(true);

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.gpslogslist);

        handleNotes();

        refreshList(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList(true);
    }

    private void refreshList( boolean doReread ) {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "refreshing gps maps list"); //$NON-NLS-1$
        gpslogItems = new LogMapItem[0];
        try {
            if (doReread) {
                List<LogMapItem> logsList = DaoGpsLog.getGpslogs();
                Collections.sort(logsList, mapItemSorter);
                gpslogItems = logsList.toArray(new LogMapItem[0]);
            }
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        ArrayAdapter<MapItem> arrayAdapter = new ArrayAdapter<MapItem>(this, R.layout.gpslog_row, gpslogItems){
            @Override
            public View getView( int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.gpslog_row, null);

                TextView nameView = (TextView) rowView.findViewById(R.id.filename);
                CheckBox visibleView = (CheckBox) rowView.findViewById(R.id.visible);

                final MapItem item = gpslogItems[position];
                rowView.setBackgroundColor(ColorUtilities.toColor(item.getColor()));
                nameView.setText(item.getName());

                visibleView.setChecked(item.isVisible());
                visibleView.setOnCheckedChangeListener(new OnCheckedChangeListener(){
                    public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                        item.setVisible(isChecked);
                        item.setDirty(true);
                    }
                });

                return rowView;
            }

        };
        setListAdapter(arrayAdapter);
    }

    @Override
    protected void onListItemClick( ListView parent, View v, int position, long id ) {
        Intent intent = new Intent(this, GpsDataPropertiesActivity.class);
        intent.putExtra(Constants.PREFS_KEY_GPSLOG4PROPERTIES, gpslogItems[position]);
        startActivityForResult(intent, GPSDATAPROPERTIES_RETURN_CODE);
    }

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "Activity returned"); //$NON-NLS-1$
        super.onActivityResult(requestCode, resultCode, data);
        switch( requestCode ) {
        case (GPSDATAPROPERTIES_RETURN_CODE): {
            if (resultCode == Activity.RESULT_OK) {
                double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0d);
                double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0d);
                Intent intent = getIntent();
                intent.putExtra(LibraryConstants.LATITUDE, lat);
                intent.putExtra(LibraryConstants.LONGITUDE, lon);
                setResult(Activity.RESULT_OK, intent);
            }
            break;
        }
        }
    }

    public boolean onCreateOptionsMenu( Menu menu ) {
        super.onCreateOptionsMenu(menu);
        menu.add(Menu.NONE, MERGE_SELECTED, 1, R.string.mainmenu_merge).setIcon(android.R.drawable.ic_menu_add);
        menu.add(Menu.FIRST, SELECTALL, 2, R.string.select_all).setIcon(R.drawable.ic_menu_select);
        menu.add(Menu.FIRST, UNSELECTALL, 3, R.string.unselect_all).setIcon(R.drawable.ic_menu_unselect);
        return true;
    }

    public boolean onMenuItemSelected( int featureId, MenuItem item ) {
        switch( item.getItemId() ) {
        case MERGE_SELECTED:
            try {
                mergeSelected();
            } catch (IOException e) {
                GPLog.error(this, e.getLocalizedMessage(), e);
                e.printStackTrace();
            }
            return true;
        case SELECTALL:
            try {
                DaoGpsLog.setLogsVisibility(true);
                refreshList(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        case UNSELECTALL:
            try {
                DaoGpsLog.setLogsVisibility(false);
                refreshList(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void mergeSelected() throws IOException {
        final List<LogMapItem> selected = new ArrayList<LogMapItem>();
        for( LogMapItem mapItem : gpslogItems ) {
            if (mapItem.isVisible()) {
                selected.add(mapItem);
            }
        }

        if (selected.size() < 2) {
            return;
        }

        int logsNum = selected.size();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        String message = logsNum + getString(R.string.logs_will_be_merged);
        builder.setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener(){
            public void onClick( DialogInterface dialog, int ii ) {
                long mainId = selected.get(0).getId();
                for( int i = 1; i < selected.size(); i++ ) {
                    MapItem mapItem = selected.get(i);
                    long id = mapItem.getId();
                    try {
                        DaoGpsLog.mergeLogs(id, mainId);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                refreshList(true);
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener(){
            public void onClick( DialogInterface dialog, int id ) {
                // ignore
            }
        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();

    }

    @Override
    protected void onPause() {
        try {
            boolean oneVisible = false;
            for( MapItem item : gpslogItems ) {
                if (item.isDirty()) {
                    DaoGpsLog.updateLogProperties(item.getId(), item.getColor(), item.getWidth(), item.isVisible(), null);
                    item.setDirty(false);
                }
                if (!oneVisible && item.isVisible()) {
                    oneVisible = true;
                }
            }
            DataManager.getInstance().setLogsVisible(oneVisible);
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        super.onPause();
    }

    private void handleNotes() {
        // images selection
        CheckBox imagesView = (CheckBox) findViewById(R.id.imagesvisible);
        imagesView.setChecked(DataManager.getInstance().areImagesVisible());
        imagesView.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            public void onCheckedChanged( CompoundButton buttonView, boolean isChecked ) {
                DataManager.getInstance().setImagesVisible(isChecked);
            }
        });

        final Button notesPropertiesButton = (Button) findViewById(R.id.notesPropertiesButton);
        notesPropertiesButton.setOnClickListener(new Button.OnClickListener(){
            public void onClick( View v ) {
                Intent intent = new Intent(GpsDataListActivity.this, NotesPropertiesActivity.class);
                startActivity(intent);
            }
        });

    }

}

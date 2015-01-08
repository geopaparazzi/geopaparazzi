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
import android.app.PendingIntent;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
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
import eu.geopaparazzi.library.util.DynamicDoubleArray;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoGpsLog;
import eu.hydrologis.geopaparazzi.database.TableDescriptions;
import eu.hydrologis.geopaparazzi.util.Constants;
import eu.hydrologis.geopaparazzi.util.Line;

import static eu.hydrologis.geopaparazzi.database.TableDescriptions.TABLE_GPSLOGS;
import static eu.hydrologis.geopaparazzi.database.TableDescriptions.TABLE_GPSLOG_PROPERTIES;

/**
 * Gpx listing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsDataListActivity extends ListActivity implements
        NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {
    private static final int SELECTALL = 1;
    private static final int UNSELECTALL = 2;
    private static final int MERGE_SELECTED = 3;

    private static final int GPSDATAPROPERTIES_RETURN_CODE = 668;

    private NfcAdapter mNfcAdapter;

    private LogMapItem[] gpslogItems;
    private Comparator<MapItem> mapItemSorter = new ItemComparators.MapItemIdComparator(true);
    private String logSendingMimeType = "application/eu.geopaparazzi.gpsdatalog_msg";

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.gpslogslist);

        // Check for available NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter != null) {
            mNfcAdapter.setNdefPushMessageCallback(this, this);
            mNfcAdapter.setOnNdefPushCompleteCallback(this, this);
        }

        handleNotes();

    }

    @Override
    protected void onResume() {
        super.onResume();
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
//        nfcAdapter.enableForegroundDispatch(this, pendingIntent, null, null);
        // Check to see if a Beam launched this Activity
        String action = getIntent().getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            processNfcIntent(getIntent());
        }
        refreshList(true);
    }

    void processNfcIntent(Intent intent) {
        Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (rawMsgs == null) return;
        // only one message sent during the beam
        NdefMessage msg = (NdefMessage) rawMsgs[0];
        // record 0 contains the MIME type, record 1 is the AAR, if present
        NdefRecord ndefRecord = msg.getRecords()[0];

        byte[] type = ndefRecord.getType();
        if (type == null || !new String(type).equals(logSendingMimeType)) {
            return;
        }

        final byte[] nfcBytes = ndefRecord.getPayload();
        try {
            final SerializableLogs logs = Utilities.deserializeObject(nfcBytes, SerializableLogs.class);
            for (int i = 0; i < logs.getSize(); i++) {
                LogMapItem log = logs.getLogAt(i);
                Line logData = logs.getLogDataAt(i);

                DaoGpsLog daoGpsLog = new DaoGpsLog();
                long logId = daoGpsLog.addGpsLog(log.getStartTime(), log.getEndTime(), -1, log.getName(), log.getWidth(), log.getColor(), true);

                SQLiteDatabase sqliteDatabase = GeopaparazziApplication.getInstance().getDatabase();
                sqliteDatabase.beginTransaction();
                try {
                    List<String> dateList = logData.getDateList();
                    DynamicDoubleArray lonList = logData.getLonList();
                    DynamicDoubleArray latList = logData.getLatList();
                    DynamicDoubleArray altimList = logData.getAltimList();
                    int size = dateList.size();
                    for (int j = 0; j < size; j++) {
                        double lon = lonList.get(j);
                        double lat = latList.get(j);

                        double altim = altimList.get(j);
                        long time = Long.parseLong(dateList.get(j));
                        daoGpsLog.addGpsLogDataPoint(sqliteDatabase, logId, lon, lat, altim, time);
                    }
                    sqliteDatabase.setTransactionSuccessful();

                    intent.removeExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                } catch (Exception e) {
                    GPLog.error(GpsDataListActivity.this, e.getLocalizedMessage(), e);
                    throw new IOException(e.getLocalizedMessage());
                } finally {
                    sqliteDatabase.endTransaction();
                }

            }
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utilities.messageDialog(GpsDataListActivity.this, "Incoming logs loaded: " + logs.getSize(), null);
                }
            });
        } catch (Exception e) {
            GPLog.error(GpsDataListActivity.this, e.getLocalizedMessage(), e);
        }


    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {

        try {
            List<LogMapItem> logsList = DaoGpsLog.getGpslogs();
            SerializableLogs logs = new SerializableLogs();
            for (int i = 0; i < logsList.size(); i++) {
                LogMapItem logMapItem = logsList.get(i);
                if (logMapItem.isVisible()) {
                    Line line = DaoGpsLog.getGpslogAsLine(logMapItem.getLogID(), -1);
                    logs.addLog(logMapItem, line);
                }
            }
            byte[] logBytes = Utilities.serializeObject(logs);

            NdefMessage msg = new NdefMessage(NdefRecord.createMime(
                    logSendingMimeType, logBytes)
                    /**
                     * The Android Application Record (AAR) is commented out. When a device
                     * receives a push with an AAR in it, the application specified in the AAR
                     * is guaranteed to run. The AAR overrides the tag dispatch system.
                     * You can add it back in to guarantee that this
                     * activity starts when receiving a beamed message. For now, this code
                     * uses the tag dispatch system.
                     */
                    //,NdefRecord.createApplicationRecord("com.examples.nfcbeam")
            );
            return msg;
        } catch (IOException e) {
            GPLog.error(this, "Error in sending logs.", e);
        }
        return null;
    }

    @Override
    public void onNdefPushComplete(NfcEvent event) {
        //This callback happens on a binder thread, don't update
        // the UI directly from this method.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utilities.messageDialog(GpsDataListActivity.this, "Logs sent.", null);
            }
        });
    }

    @Override
    public void onNewIntent(Intent intent) {
        // onResume gets called after this to handle the intent
        setIntent(intent);
    }

    private void refreshList(boolean doReread) {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "refreshing gps maps list"); //$NON-NLS-1$
        gpslogItems = new LogMapItem[0];
        try {
            if (doReread) {
                List<LogMapItem> logsList = DaoGpsLog.getGpslogs();
                Collections.sort(logsList, mapItemSorter);
                gpslogItems = logsList.toArray(new LogMapItem[logsList.size()]);
            }
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
        }

        ArrayAdapter<MapItem> arrayAdapter = new ArrayAdapter<MapItem>(this, R.layout.gpslog_row, gpslogItems) {
            @Override
            public View getView(int position, View cView, ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.gpslog_row, null);

                TextView nameView = (TextView) rowView.findViewById(R.id.filename);
                CheckBox visibleView = (CheckBox) rowView.findViewById(R.id.visible);

                final MapItem item = gpslogItems[position];
                rowView.setBackgroundColor(ColorUtilities.toColor(item.getColor()));
                nameView.setText(item.getName());

                visibleView.setChecked(item.isVisible());
                visibleView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
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
    protected void onListItemClick(ListView parent, View v, int position, long id) {
        Intent intent = new Intent(this, GpsDataPropertiesActivity.class);
        intent.putExtra(Constants.PREFS_KEY_GPSLOG4PROPERTIES, gpslogItems[position]);
        startActivityForResult(intent, GPSDATAPROPERTIES_RETURN_CODE);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "Activity returned"); //$NON-NLS-1$
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (GPSDATAPROPERTIES_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    double lon = data.getDoubleExtra(LibraryConstants.LONGITUDE, 0d);
                    double lat = data.getDoubleExtra(LibraryConstants.LATITUDE, 0d);
//                    Intent intent = getIntent();
//                    intent.putExtra(LibraryConstants.LATITUDE, lat);
//                    intent.putExtra(LibraryConstants.LONGITUDE, lon);
//                    setResult(Activity.RESULT_OK, intent);

                    Intent intent = new Intent(this, MapsSupportService.class);
                    intent.putExtra(MapsSupportService.CENTER_ON_POSITION_REQUEST, true);
                    intent.putExtra(LibraryConstants.LONGITUDE, lon);
                    intent.putExtra(LibraryConstants.LATITUDE, lat);
                    startService(intent);
                    finish();
                }
            }
        }
    }

    public boolean onCreateOptionsMenu(Menu menu) {
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
            }
            return true;
        case SELECTALL:
            try {
                DaoGpsLog.setLogsVisibility(true);
                refreshList(true);
            } catch (IOException e) {
                GPLog.error(this, null, e); //$NON-NLS-1$
            }
            return true;
        case UNSELECTALL:
            try {
                DaoGpsLog.setLogsVisibility(false);
                refreshList(true);
            } catch (IOException e) {
                GPLog.error(this, null, e); //$NON-NLS-1$
            }
            return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private void mergeSelected() throws IOException {
        final List<LogMapItem> selected = new ArrayList<LogMapItem>();
        for (LogMapItem mapItem : gpslogItems) {
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
        builder.setMessage(message).setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int ii) {
                long mainId = selected.get(0).getId();
                for (int i = 1; i < selected.size(); i++) {
                    MapItem mapItem = selected.get(i);
                    long id = mapItem.getId();
                    try {
                        DaoGpsLog.mergeLogs(id, mainId);
                    } catch (IOException e) {
                        GPLog.error(this, null, e); //$NON-NLS-1$
                    }
                }
                refreshList(true);
            }
        }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
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
            for (MapItem item : gpslogItems) {
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
        imagesView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                DataManager.getInstance().setImagesVisible(isChecked);
            }
        });

        final Button notesPropertiesButton = (Button) findViewById(R.id.notesPropertiesButton);
        notesPropertiesButton.setOnClickListener(new Button.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(GpsDataListActivity.this, NotesPropertiesActivity.class);
                startActivity(intent);
            }
        });

    }

}

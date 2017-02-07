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
package eu.geopaparazzi.core.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.os.Bundle;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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
import eu.geopaparazzi.library.style.ColorUtilities;
import eu.geopaparazzi.library.util.DynamicDoubleArray;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoGpsLog;
import eu.geopaparazzi.core.database.objects.ItemComparators;
import eu.geopaparazzi.core.database.objects.Line;
import eu.geopaparazzi.core.database.objects.LogMapItem;
import eu.geopaparazzi.core.database.objects.MapItem;
import eu.geopaparazzi.core.database.objects.SerializableLogs;
import eu.geopaparazzi.core.mapview.MapsSupportService;
import eu.geopaparazzi.core.utilities.Constants;

/**
 * Gpx listing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GpsDataListActivity extends AppCompatActivity implements
        NfcAdapter.CreateNdefMessageCallback, NfcAdapter.OnNdefPushCompleteCallback {

    private static final int GPSDATAPROPERTIES_RETURN_CODE = 668;

    private NfcAdapter mNfcAdapter;

    private LogMapItem[] gpslogItems;
    private Comparator<MapItem> mapItemSorter = new ItemComparators.MapItemIdComparator(true);
    private String logSendingMimeType = "application/eu.geopaparazzi.gpsdatalog_msg";
    private SharedPreferences mPeferences;
    private ListView mListView;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_gpsdatalist);

        Toolbar toolbar = (Toolbar) findViewById(eu.geopaparazzi.mapsforge.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mListView = (ListView) findViewById(R.id.gpsdatalist);

        mPeferences = PreferenceManager.getDefaultSharedPreferences(this);

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
                    GPDialogs.infoDialog(GpsDataListActivity.this, getString(R.string.incoming_logs_added) + logs.getSize(), null);
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
                GPDialogs.infoDialog(GpsDataListActivity.this, getString(R.string.logs_sent), null);
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

        ArrayAdapter<MapItem> arrayAdapter = new ArrayAdapter<MapItem>(this, R.layout.activity_gpsdatalist_row, gpslogItems) {
            class ViewHolder {
                TextView nameView;
                CheckBox visibleView;
                Button colorView;
                Button propertiesButton;
            }

            @Override
            public View getView(final int position, View cView, ViewGroup parent) {

                ViewHolder holder;
                // Recycle existing view if passed as parameter
                View rowView = cView;
                if (rowView == null) {
                    LayoutInflater inflater = getLayoutInflater();
                    rowView = inflater.inflate(R.layout.activity_gpsdatalist_row, parent, false);
                    holder = new ViewHolder();
                    holder.nameView = (TextView) rowView.findViewById(R.id.filename);
                    holder.visibleView = (CheckBox) rowView.findViewById(R.id.visible);
                    holder.colorView = (Button) rowView.findViewById(R.id.colorButton);
                    holder.propertiesButton = (Button) rowView.findViewById(R.id.propertiesButton);
                    rowView.setTag(holder);
                } else {
                    holder = (ViewHolder) rowView.getTag();
                }


                final MapItem item = gpslogItems[position];

                Drawable background = holder.colorView.getBackground();
                if (background instanceof GradientDrawable) {
                    int color = ColorUtilities.toColor(item.getColor());
                    GradientDrawable gd = (GradientDrawable) background;
                    gd.setStroke(1, Color.BLACK);
                    gd.setColor(color);
                }
                holder.nameView.setText(item.getName());

                holder.visibleView.setChecked(item.isVisible());
                holder.visibleView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        item.setVisible(isChecked);
                        item.setDirty(true);
                    }
                });
                holder.propertiesButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(GpsDataListActivity.this, GpsLogPropertiesActivity.class);
                        intent.putExtra(Constants.PREFS_KEY_GPSLOG4PROPERTIES, gpslogItems[position]);
                        startActivityForResult(intent, GPSDATAPROPERTIES_RETURN_CODE);
                    }
                });

                return rowView;
            }

        };
        mListView.setAdapter(arrayAdapter);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_gpsdatalist, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_selectall) {
            try {
                DaoGpsLog.setLogsVisibility(true);
                refreshList(true);
            } catch (IOException e) {
                GPLog.error(this, null, e); //$NON-NLS-1$
            }
        } else if (item.getItemId() == R.id.action_unselectall) {
            try {
                DaoGpsLog.setLogsVisibility(false);
                refreshList(true);
            } catch (IOException e) {
                GPLog.error(this, null, e); //$NON-NLS-1$
            }
        } else if (item.getItemId() == R.id.action_merge) {
            try {
                mergeSelected();
            } catch (IOException e) {
                GPLog.error(this, e.getLocalizedMessage(), e);
            }
        } else if (item.getItemId() == R.id.action_notesproperties) {
            Intent intent = new Intent(GpsDataListActivity.this, NotesPropertiesActivity.class);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    private void mergeSelected() throws IOException {
        final List<LogMapItem> selected = new ArrayList<>();
        for (LogMapItem mapItem : gpslogItems) {
            if (mapItem.isVisible()) {
                selected.add(mapItem);
            }
        }

        if (selected.size() < 2) {
            return;
        }

        int logsNum = selected.size();
        String message = logsNum + " " + getString(R.string.logs_will_be_merged);
        GPDialogs.yesNoMessageDialog(this, message, new Runnable() {
            @Override
            public void run() {
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

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        refreshList(true);
                    }
                });
            }
        }, null);
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

            // TODO
//            DataManager.getInstance().setLogsVisible(oneVisible);
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        super.onPause();
    }

    private void handleNotes() {


    }

}

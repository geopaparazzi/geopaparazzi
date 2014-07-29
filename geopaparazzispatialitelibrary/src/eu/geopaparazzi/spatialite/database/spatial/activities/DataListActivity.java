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
package eu.geopaparazzi.spatialite.database.spatial.activities;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.spatialite.R;
import eu.geopaparazzi.spatialite.database.spatial.SpatialDatabasesManager;
import eu.geopaparazzi.spatialite.database.spatial.core.databasehandlers.AbstractSpatialDatabaseHandler;
import eu.geopaparazzi.spatialite.database.spatial.core.tables.SpatialVectorTable;
import eu.geopaparazzi.spatialite.database.spatial.util.comparators.OrderComparator;
import eu.geopaparazzi.spatialite.database.spatial.util.SpatialiteLibraryConstants;

/**
 * Data listing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class DataListActivity extends ListActivity implements View.OnClickListener {

    private List<SpatialVectorTable> spatialTables = new ArrayList<SpatialVectorTable>();
    private String mapsDirPath;

    private EditText filterText;
    private String textToFilter = "";
    private Button toggleTablesButton;
    private Button toggleViewsButton;

    private boolean showTables = true;
    private boolean showViews = true;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.data_list);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        filterText = (EditText) findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);

        toggleTablesButton = (Button) findViewById(R.id.toggleTablesButton);
        toggleTablesButton.setOnClickListener(this);
        toggleTablesButton.setText("Tables");
        toggleTablesButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable_selected));

        toggleViewsButton = (Button) findViewById(R.id.toggleViewsButton);
        toggleViewsButton.setOnClickListener(this);
        toggleViewsButton.setText("Views");
        toggleViewsButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable_selected));

        try {
            mapsDirPath = ResourcesManager.getInstance(this).getMapsDir().getPath();
        } catch (Exception e) {
            e.printStackTrace();
        }

        refreshList(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void refreshList(boolean doReread) {
        try {
            if (doReread)
                spatialTables = SpatialDatabasesManager.getInstance().getSpatialVectorTables(doReread);
        } catch (Exception e) {
            GPLog.error(this, null, e);
        }

        final List<SpatialVectorTable> filteredTables = new ArrayList<SpatialVectorTable>();
        for (SpatialVectorTable spatialTable : spatialTables) {
            boolean isView = spatialTable.isView();

            boolean doAdd = false;
            if (showTables && !isView) {
                doAdd = true;
            } else if (showViews && isView) {
                doAdd = true;
            }

            if (textToFilter.length() > 0) {
                // filter text
                String tableNameString = spatialTable.getTableName().toLowerCase();
                String dbName = spatialTable.getFileName().toLowerCase();
                String filterString = textToFilter.toLowerCase();
                if (!tableNameString.contains(filterString) && !dbName.contains(filterString)) {
                    doAdd = false;
                }
            }

            if (doAdd) {
                filteredTables.add(spatialTable);
            }

        }

        final boolean isUnFiltered = spatialTables.size() == filteredTables.size();

        ArrayAdapter<SpatialVectorTable> arrayAdapter = new ArrayAdapter<SpatialVectorTable>(this, R.layout.data_row,
                filteredTables) {
            @SuppressWarnings("nls")
            @Override
            public View getView(final int position, View cView, ViewGroup parent) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.data_row, null);
                final SpatialVectorTable item = filteredTables.get(position);
                AbstractSpatialDatabaseHandler tableHandler = null;
                try {
                    tableHandler = SpatialDatabasesManager.getInstance().getVectorHandler(item);
                } catch (jsqlite.Exception e1) {
                    GPLog.error(DataListActivity.this, null, e1);
                }

                TextView nameView = (TextView) rowView.findViewById(R.id.name);
                TextView descriptionView = (TextView) rowView.findViewById(R.id.description);

                CheckBox visibleView = (CheckBox) rowView.findViewById(R.id.visible);

                ImageButton listUpButton = (ImageButton) rowView.findViewById(R.id.upButton);
                if (isUnFiltered) {
                    listUpButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (position > 0) {
                                SpatialVectorTable before = spatialTables.get(position - 1);
                                int tmp1 = before.getStyle().order;
                                int tmp2 = item.getStyle().order;
                                item.getStyle().order = tmp1;
                                before.getStyle().order = tmp2;
                                Collections.sort(spatialTables, new OrderComparator());
                                refreshList(false);
                            }
                        }
                    });
                } else {
                    listUpButton.setVisibility(View.GONE);
                }

                ImageButton listDownButton = (ImageButton) rowView.findViewById(R.id.downButton);
                if (isUnFiltered) {
                    listDownButton.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            if (position < spatialTables.size() - 1) {
                                SpatialVectorTable after = spatialTables.get(position + 1);
                                int tmp1 = after.getStyle().order;
                                int tmp2 = item.getStyle().order;
                                item.getStyle().order = tmp1;
                                after.getStyle().order = tmp2;
                                Collections.sort(spatialTables, new OrderComparator());
                                refreshList(false);
                            }
                        }
                    });
                } else {
                    listDownButton.setVisibility(View.GONE);
                }

                ImageButton propertiesButton = (ImageButton) rowView.findViewById(R.id.propertiesButton);
                propertiesButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = null;
                        if (item.isLine()) {
                            intent = new Intent(DataListActivity.this, LinesDataPropertiesActivity.class);
                        } else if (item.isPolygon()) {
                            intent = new Intent(DataListActivity.this, PolygonsDataPropertiesActivity.class);
                        } else if ((item.isPoint()) || (item.isGeometryCollection())) {
                            intent = new Intent(DataListActivity.this, PointsDataPropertiesActivity.class);
                        } else {
                            return;
                        }
                        intent.putExtra(SpatialiteLibraryConstants.PREFS_KEY_TEXT, item.getUniqueNameBasedOnDbFilePath());
                        startActivity(intent);

                    }
                });

                ImageButton zoomtoButton = (ImageButton) rowView.findViewById(R.id.zoomtoButton);
                zoomtoButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        try {
                            float[] tableBounds = SpatialDatabasesManager.getInstance().getVectorHandler(item)
                                    .getTableBounds(item);
                            double lat = tableBounds[1] + (tableBounds[0] - tableBounds[1]) / 2.0;
                            double lon = tableBounds[3] + (tableBounds[2] - tableBounds[3]) / 2.0;

                            Intent intent = getIntent();
                            intent.putExtra(SpatialiteLibraryConstants.LATITUDE, lat);
                            intent.putExtra(SpatialiteLibraryConstants.LONGITUDE, lon);
                            setResult(Activity.RESULT_OK, intent);
                            finish();
                        } catch (jsqlite.Exception e) {
                            e.printStackTrace();
                        }
                    }
                });

                ImageButton labelsButton = (ImageButton) rowView.findViewById(R.id.labelsButton);
                labelsButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = new Intent(DataListActivity.this, LabelPropertiesActivity.class);
                        intent.putExtra(SpatialiteLibraryConstants.PREFS_KEY_TEXT, item.getUniqueNameBasedOnDbFilePath());
                        startActivity(intent);
                    }
                });

                // rowView.setBackgroundColor(ColorUtilities.toColor(item.getColor()));
                // mj10777: some tables may have more than one column, thus the column name will
                // also be shown item.getUniqueName()
                nameView.setText(item.getTableName());

                String dbName = item.getFileName();

                if (mapsDirPath != null && tableHandler != null) {
                    String databasePath = tableHandler.getFile().getAbsolutePath();
                    if (databasePath.startsWith(mapsDirPath)) {
                        dbName = databasePath.replaceFirst(mapsDirPath, "");
                        if (dbName.startsWith(File.separator)) {
                            dbName = dbName.substring(1);
                        }
                    }
                }

                descriptionView.setText(item.getGeomName() + ": " + item.getLayerTypeDescription() + ", db: " + dbName);

                visibleView.setChecked(item.getStyle().enabled != 0);
                visibleView.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        item.getStyle().enabled = isChecked ? 1 : 0;
                        try {
                            SpatialDatabasesManager.getInstance().updateStyle(item);
                        } catch (jsqlite.Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                return rowView;
            }

        };
        setListAdapter(arrayAdapter);

    }

    @Override
    protected void onPause() {
        try {
            for (SpatialVectorTable spatialTable : spatialTables) {
                SpatialDatabasesManager.getInstance().updateStyle(spatialTable);
            }
            spatialTables = SpatialDatabasesManager.getInstance().getSpatialVectorTables(true);
        } catch (Exception e) {
            // Logger.e(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        super.onPause();
    }


    protected void onDestroy() {
        if (filterText != null)
            filterText.removeTextChangedListener(filterTextWatcher);
        super.onDestroy();
    }


    private TextWatcher filterTextWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
            // ignore
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // ignore
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            textToFilter = s.toString();
            try {
                refreshList(false);
            } catch (Exception e) {
                GPLog.error(DataListActivity.this, "ERROR", e);
            }
        }
    };

    @Override
    public void onClick(View view) {
        if (view == toggleTablesButton) {
            if (!showTables) {
                toggleTablesButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable_selected));
            } else {
                toggleTablesButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable));
            }
            showTables = !showTables;
        }
        if (view == toggleViewsButton) {
            if (!showViews) {
                toggleViewsButton.setBackgroundDrawable(getResources().getDrawable(
                        R.drawable.button_background_drawable_selected));
            } else {
                toggleViewsButton.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_background_drawable));
            }
            showViews = !showViews;
        }
        try {
            refreshList(false);
        } catch (Exception e) {
            GPLog.error(DataListActivity.this, "ERROR", e);
        }
    }
}

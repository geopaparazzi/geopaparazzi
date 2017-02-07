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

import android.content.Intent;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.Set;

import eu.geopaparazzi.library.core.activities.DatabaseListActivity;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.core.R;

/**
 * A view for db queries.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SqlViewActivity extends AppCompatActivity {

    private HashMap<String, Query> queriesMap;
    private Spinner sqlSpinner;
    private EditText customQueryText;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sqlview);

        Toolbar toolbar = (Toolbar) findViewById(eu.geopaparazzi.core.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        sqlSpinner = (Spinner) findViewById(R.id.sqlQuerySpinner);
        try {
            List<String> queriesNames = createQueries();

            ArrayAdapter<String> queryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, queriesNames);
            queryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            sqlSpinner.setAdapter(queryAdapter);
            sqlSpinner.setSelection(0);
        } catch (Exception e) {
            GPLog.error(this, null, e); //$NON-NLS-1$
        }

        customQueryText = (EditText) findViewById(R.id.ownQueryEditText);
    }

    /**
     * Launch a query.
     *
     * @param view parent.
     * @throws Exception if something goes wrong.
     */
    public void launchQuery(View view) throws Exception {
        String sqlName = sqlSpinner.getSelectedItem().toString();
        Query query = queriesMap.get(sqlName);

        Intent dbViewIntent = new Intent(this, DatabaseListActivity.class);
        dbViewIntent.putExtra(LibraryConstants.PREFS_KEY_QUERY, query.query);
        startActivity(dbViewIntent);
    }

    /**
     * Launch custom query.
     *
     * @param view parent.
     * @throws Exception if something goes wrong.
     */
    public void launchOwnQuery(View view) throws Exception {
        String customQuery = customQueryText.getText().toString();

        Intent dbViewIntent = new Intent(this, DatabaseListActivity.class);
        dbViewIntent.putExtra(LibraryConstants.PREFS_KEY_QUERY, customQuery);
        startActivity(dbViewIntent);
    }

    private List<String> createQueries() throws Exception {
        queriesMap = new HashMap<>();

        AssetManager assetManager = getAssets();
        InputStream inputStream = assetManager.open("queries_select.txt");
        String viewQueriesString = new Scanner(inputStream).useDelimiter("\\A").next();
        String[] queriesSplit = viewQueriesString.split("\n");
        for (String queryLine : queriesSplit) {
            String[] lineSplit = queryLine.split(";");
            Query q = new Query();
            q.name = lineSplit[0].trim();
            String[] split = lineSplit[1].trim().split(",");
            String[] splitTrim = new String[split.length];
            for (int i = 0; i < splitTrim.length; i++) {
                splitTrim[i] = split[i].trim();
            }
            q.query = lineSplit[1].trim();
            queriesMap.put(q.name, q);
        }

        Set<String> keySet = queriesMap.keySet();
        List<String> queries = new ArrayList<>();
        for (String query : keySet) {
            queries.add(query);
        }
        Collections.sort(queries);
        return queries;
    }

    private static class Query {
        String name;
        String query;
    }
}

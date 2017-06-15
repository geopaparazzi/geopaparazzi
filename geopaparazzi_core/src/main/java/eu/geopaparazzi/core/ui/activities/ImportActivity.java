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

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import java.util.List;

import eu.geopaparazzi.library.plugin.ExtensionPoints;
import eu.geopaparazzi.library.plugin.PluginLoaderListener;
import eu.geopaparazzi.library.plugin.menu.IMenuLoader;
import eu.geopaparazzi.library.plugin.menu.MenuLoader;
import eu.geopaparazzi.library.plugin.style.StyleHelper;
import eu.geopaparazzi.library.plugin.types.IMenuEntry;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.core.R;

/**
 * Activity for export tasks.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ImportActivity extends AppCompatActivity implements IActivitySupporter {

    public static final int START_REQUEST_CODE = 666;

    private SparseArray<IMenuEntry> menuEntriesMap = new SparseArray<>();

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.activity_import);

        Toolbar toolbar = (Toolbar) findViewById(eu.geopaparazzi.core.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        MenuLoader menuLoader = new MenuLoader(this, ExtensionPoints.MENU_IMPORT_PROVIDER);
        menuLoader.addListener(new PluginLoaderListener<MenuLoader>() {
            @Override
            public void pluginLoaded(MenuLoader loader) {
                addMenuEntries(loader.getEntries());
            }
        });
        menuLoader.connect();
    }

    protected void addMenuEntries(List<IMenuEntry> entries) {
        menuEntriesMap.clear();
        int code = START_REQUEST_CODE + 1;
        for (final eu.geopaparazzi.library.plugin.types.IMenuEntry entry : entries) {
            final Context context = this;

            Button button = new Button(context);
            LinearLayout.LayoutParams lp = StyleHelper.styleButton(this, button);
            button.setText(entry.getLabel());
            entry.setRequestCode(code);
            menuEntriesMap.put(code, entry);
            code++;
            LinearLayout container = (LinearLayout) findViewById(R.id.scrollView);
            container.addView(button, lp);
            button.setOnClickListener(new Button.OnClickListener() {
                public void onClick(View v) {
                    entry.onClick(ImportActivity.this);
                }
            });
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IMenuEntry entry = menuEntriesMap.get(requestCode);
        if (entry != null) {
            entry.onActivityResultExecute(requestCode, resultCode, data);
        }
    }

    @Override
    public Context getContext() {
        return this;
    }


}

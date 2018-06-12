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
package eu.geopaparazzi.plugins.pdfexport;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import eu.geopaparazzi.core.GeopaparazziApplication;
import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoImages;
import eu.geopaparazzi.core.database.DaoMetadata;
import eu.geopaparazzi.core.database.DaoNotes;
import eu.geopaparazzi.core.database.objects.ItemComparators;
import eu.geopaparazzi.core.database.objects.Metadata;
import eu.geopaparazzi.core.database.objects.Note;
import eu.geopaparazzi.core.mapview.MapviewActivity;
import eu.geopaparazzi.core.ui.activities.NotesPropertiesActivity;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.ANote;
import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IImagesDbHelper;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.forms.FormActivity;
import eu.geopaparazzi.library.forms.FormInfoHolder;
import eu.geopaparazzi.library.forms.FormUtilities;
import eu.geopaparazzi.library.images.ImageUtilities;
import eu.geopaparazzi.library.share.ShareUtilities;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.PositionUtilities;
import eu.geopaparazzi.library.util.StringAsyncTask;
import eu.geopaparazzi.library.util.UrlUtilities;

/**
 * Notes listing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PdfExportNotesListActivity extends AppCompatActivity {
    private List<ANote> allNotesList = new ArrayList<>();
    private List<ANote> visibleNotesList = new ArrayList<>();

    private ArrayAdapter<ANote> arrayAdapter;
    private EditText filterText;

    private String selectAll;
    private String invertSelection;
    private ListView listView;

    private int currentComparatorIndex = 0;
    private SharedPreferences mPreferences;
    private StringAsyncTask deletionTask;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(eu.geopaparazzi.plugins.pdfexport.R.layout.activity_pdfexportnoteslist);

        Toolbar toolbar = findViewById(eu.geopaparazzi.mapsforge.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        listView = findViewById(R.id.notesList);

        selectAll = getString(R.string.select_all);
        invertSelection = getString(R.string.invert_selection);

        refreshList();

        filterText = findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);
    }


    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    protected void onDestroy() {
        filterText.removeTextChangedListener(filterTextWatcher);
        super.onDestroy();
    }

    private void refreshList() {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "refreshing notes list"); //$NON-NLS-1$
        try {
            visibleNotesList.clear();
            collectAllNotes();
            visibleNotesList.addAll(allNotesList);
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        redoAdapter();
    }

    private void collectAllNotes() throws IOException {
        allNotesList.clear();
        List<Note> tmpNotesList = DaoNotes.getNotesList(null, false);
        for (Note note : tmpNotesList) {
            if (note.getForm() != null)
                allNotesList.add(note);
        }
    }

    private void filterList(String filterText) {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "filter notes list"); //$NON-NLS-1$
        try {
            collectAllNotes();
            visibleNotesList.clear();
            filterText = filterText.toLowerCase();
            for (ANote note : allNotesList) {
                String name = note.getName();
                String nameLower = name.toLowerCase();
                if (nameLower.contains(filterText)) {
                    visibleNotesList.add(note);
                }
            }
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        redoAdapter();
    }

    private void redoAdapter() {


        arrayAdapter = new ArrayAdapter<ANote>(this, R.layout.activity_noteslist_row, visibleNotesList) {
            class ViewHolder {
                CheckBox checkButton;
                TextView notesText;
                ImageButton goButton;
                ImageButton moreButton;
            }

            @Override
            public View getView(int position, View rowView, ViewGroup parent) {

                ViewHolder holder;
                // Recycle existing view if passed as parameter
                if (rowView == null) {
                    LayoutInflater inflater = getLayoutInflater();
                    rowView = inflater.inflate(R.layout.activity_noteslist_row, parent, false);
                    holder = new ViewHolder();
                    holder.checkButton = rowView.findViewById(R.id.selectedCheckBox);
                    holder.notesText = rowView.findViewById(R.id.notesrowtext);
                    holder.goButton = rowView.findViewById(R.id.gobutton);
                    holder.moreButton = rowView.findViewById(R.id.morebutton);

                    rowView.setTag(holder);
                } else {
                    holder = (ViewHolder) rowView.getTag();
                }

                final ANote currentNote = visibleNotesList.get(position);

                final CheckBox checkBox = holder.checkButton;
                checkBox.setChecked(currentNote.isChecked());
                checkBox.setOnCheckedChangeListener(new CheckBox.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        checkBox.setChecked(isChecked);
                        currentNote.setChecked(isChecked);
                    }
                });


                holder.notesText.setText(currentNote.getName());

                holder.goButton.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        Intent intent = getIntent();
                        intent.putExtra(LibraryConstants.LATITUDE, currentNote.getLat());
                        intent.putExtra(LibraryConstants.LONGITUDE, currentNote.getLon());
                        intent.putExtra(LibraryConstants.ZOOMLEVEL, 16);
                        // if (getParent() == null) {
                        setResult(Activity.RESULT_OK, intent);
                        finish();
                    }
                });

                final ImageButton moreButton2 = holder.moreButton;
                moreButton2.setOnClickListener(new View.OnClickListener() {
                    public void onClick(View v) {
                        openMoreMenu(moreButton2, currentNote);
                    }
                });

                return rowView;
            }

        };

        listView.setAdapter(arrayAdapter);
    }

    private void openMoreMenu(ImageButton button, final ANote currentNote) {

        PopupMenu popup = new PopupMenu(this, button);
        popup.getMenu().add(selectAll);
        popup.getMenu().add(invertSelection);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                String actionName = item.getTitle().toString();
                if (actionName.equals(selectAll)) {
                    for (ANote aNote : visibleNotesList) {
                        aNote.setChecked(true);
                    }
                    arrayAdapter.notifyDataSetChanged();
                } else if (actionName.equals(invertSelection)) {
                    for (ANote aNote : visibleNotesList) {
                        aNote.setChecked(!aNote.isChecked());
                    }
                    arrayAdapter.notifyDataSetChanged();
                }
                return true;
            }


        });
        popup.show();
    }


    public void save(View view) {
        List<Long> exportIds = new ArrayList<>();
        for (ANote note : visibleNotesList) {
            if (note.isChecked()) {
                exportIds.add(note.getId());
            }
        }
        long[] ids = new long[exportIds.size()];
        for (int i = 0; i < ids.length; i++) {
            ids[i] = exportIds.get(i);
        }

        PdfExportDialogFragment pdfExportDialogFragment = PdfExportDialogFragment.newInstance(null, ids);
        pdfExportDialogFragment.show(getSupportFragmentManager(), "pdf export");

    }

    private TextWatcher filterTextWatcher = new TextWatcher() {

        public void afterTextChanged(Editable s) {
            // ignore
        }

        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            // ignore
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            // arrayAdapter.getFilter().filter(s);
            filterList(s.toString());
        }
    };

    public void clearFilter(View view) {
        filterText.setText("");
    }
}

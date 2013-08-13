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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.FormActivity;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.NoteType;
import eu.hydrologis.geopaparazzi.util.Note;

/**
 * Notes listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NotesListActivity extends ListActivity {
    private String[] notesNames;
    private Map<String, Note> notesMap = new HashMap<String, Note>();
    private Comparator<Note> notesSorter = new ItemComparators.NotesComparator(false);

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.noteslist);

        refreshList();

        filterText = (EditText) findViewById(R.id.search_box);
        filterText.addTextChangedListener(filterTextWatcher);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshList();
    }

    protected void onDestroy() {
        super.onDestroy();
        filterText.removeTextChangedListener(filterTextWatcher);
    }

    private void refreshList() {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "refreshing notes list"); //$NON-NLS-1$
        try {
            List<Note> notesList = DaoNotes.getNotesList();

            Collections.sort(notesList, notesSorter);
            notesNames = new String[notesList.size()];
            notesMap.clear();
            int index = 0;
            for( Note note : notesList ) {
                String name = note.getName();
                notesMap.put(name, note);
                notesNames[index] = name;
                index++;
            }
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        redoAdapter();
    }

    private void filterList( String filterText ) {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "filter notes list"); //$NON-NLS-1$
        try {
            List<Note> notesList = DaoNotes.getNotesList();
            Collections.sort(notesList, notesSorter);

            notesMap.clear();
            filterText = ".*" + filterText.toLowerCase() + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
            List<String> namesList = new ArrayList<String>();
            for( Note note : notesList ) {
                String name = note.getName();
                String nameLower = name.toLowerCase();
                if (nameLower.matches(filterText)) {
                    namesList.add(name);
                    notesMap.put(name, note);
                }
            }

            notesNames = namesList.toArray(new String[0]);
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        redoAdapter();
    }

    private void redoAdapter() {
        arrayAdapter = new ArrayAdapter<String>(this, R.layout.note_row, notesNames){
            @Override
            public View getView( int position, View cView, ViewGroup parent ) {
                LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View rowView = inflater.inflate(R.layout.note_row, null);

                final TextView notesText = (TextView) rowView.findViewById(R.id.bookmarkrowtext);
                notesText.setText(notesNames[position]);

                final ImageView editButton = (ImageView) rowView.findViewById(R.id.editbutton);
                editButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        final String name = notesText.getText().toString();
                        Note note = notesMap.get(name);
                        if (note.getForm() == null || note.getForm().length() == 0) {
                            // can't edit simple notes
                            Utilities.messageDialog(NotesListActivity.this,
                                    "Only complex notes can be edited. Simple notes can be simply replaced.", null);
                            return;
                        }

                        int type = note.getType();
                        double lat = note.getLat();
                        double lon = note.getLon();
                        String form = note.getForm();
                        if (form != null && form.length() > 0 && type != NoteType.OSM.getTypeNum()) {
                            double altim = note.getAltim();
                            Intent formIntent = new Intent(NotesListActivity.this, FormActivity.class);
                            formIntent.putExtra(LibraryConstants.PREFS_KEY_FORM_JSON, form);
                            formIntent.putExtra(LibraryConstants.PREFS_KEY_FORM_NAME, name);
                            formIntent.putExtra(LibraryConstants.LATITUDE, (double) lat);
                            formIntent.putExtra(LibraryConstants.LONGITUDE, (double) lon);
                            formIntent.putExtra(LibraryConstants.ELEVATION, (double) altim);
                            NotesListActivity.this.startActivityForResult(formIntent, MapsActivity.FORMUPDATE_RETURN_CODE);
                        }

                    }
                });

                final ImageView deleteButton = (ImageView) rowView.findViewById(R.id.deletebutton);
                deleteButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        final String name = notesText.getText().toString();
                        final Note note = notesMap.get(name);
                        Utilities.yesNoMessageDialog(NotesListActivity.this, getString(R.string.prompt_delete_note),
                                new Runnable(){
                                    public void run() {
                                        new AsyncTask<String, Void, String>(){
                                            protected String doInBackground( String... params ) {
                                                return "";
                                            }

                                            protected void onPostExecute( String response ) {
                                                try {
                                                    DaoNotes.deleteNote(note.getId());
                                                    refreshList();
                                                } catch (IOException e) {
                                                    GPLog.error(this, e.getLocalizedMessage(), e);
                                                    e.printStackTrace();
                                                    Toast.makeText(getApplicationContext(), e.getLocalizedMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        }.execute((String) null);

                                    }
                                }, null);

                    }
                });

                final ImageView goButton = (ImageView) rowView.findViewById(R.id.gobutton);
                goButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        Note note = notesMap.get(notesText.getText().toString());
                        if (note != null) {
                            Intent intent = getIntent();
                            intent.putExtra(LibraryConstants.LATITUDE, note.getLat());
                            intent.putExtra(LibraryConstants.LONGITUDE, note.getLon());
                            intent.putExtra(LibraryConstants.ZOOMLEVEL, 16);
                            // if (getParent() == null) {
                            setResult(Activity.RESULT_OK, intent);
                        }
                        finish();
                    }
                });

                return rowView;
            }

        };

        setListAdapter(arrayAdapter);
    }
    private TextWatcher filterTextWatcher = new TextWatcher(){

        public void afterTextChanged( Editable s ) {
        }

        public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
        }

        public void onTextChanged( CharSequence s, int start, int before, int count ) {
            // arrayAdapter.getFilter().filter(s);
            filterList(s.toString());
        }
    };
    private ArrayAdapter<String> arrayAdapter;
    private EditText filterText;

    protected void onActivityResult( int requestCode, int resultCode, Intent data ) {
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "Activity returned"); //$NON-NLS-1$
        super.onActivityResult(requestCode, resultCode, data);
        switch( requestCode ) {
        case (MapsActivity.FORMUPDATE_RETURN_CODE): {
            if (resultCode == Activity.RESULT_OK) {
                String[] formArray = data.getStringArrayExtra(LibraryConstants.PREFS_KEY_FORM);
                if (formArray != null) {
                    try {
                        double lon = Double.parseDouble(formArray[0]);
                        double lat = Double.parseDouble(formArray[1]);
                        String jsonStr = formArray[6];

                        float n = (float) (lat + 0.00001f);
                        float s = (float) (lat - 0.00001f);
                        float w = (float) (lon - 0.00001f);
                        float e = (float) (lon + 0.00001f);

                        List<Note> notesInWorldBounds = DaoNotes.getNotesInWorldBounds(n, s, w, e);
                        if (notesInWorldBounds.size() > 0) {
                            Note note = notesInWorldBounds.get(0);
                            long id = note.getId();
                            DaoNotes.updateForm(id, jsonStr);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                        Utilities.messageDialog(this, eu.geopaparazzi.library.R.string.notenonsaved, null);
                    }
                }
            }
            break;
        }
        }
    }
}
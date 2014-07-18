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
import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.forms.FormActivity;
import eu.geopaparazzi.library.forms.FormUtilities;
import eu.geopaparazzi.library.share.ShareUtilities;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.Utilities;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.NoteType;
import eu.hydrologis.geopaparazzi.util.INote;
import eu.hydrologis.geopaparazzi.util.Image;
import eu.hydrologis.geopaparazzi.util.Note;

/**
 * Notes listing activity.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NotesListActivity extends ListActivity {
    private static String SHARE_NOTE_WITH = "";
    private String[] notesNames;
    private Map<String, INote> notesMap = new HashMap<String, INote>();
    private Comparator<INote> notesSorter = new ItemComparators.NotesComparator(false);

    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);

        setContentView(R.layout.noteslist);

        SHARE_NOTE_WITH = getResources().getString(eu.geopaparazzi.library.R.string.share_note_with);

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

            List<INote> allNotesList = new ArrayList<INote>();
            List<Note> notesList = DaoNotes.getNotesList();
            allNotesList.addAll(notesList);
            List<Image> imagesList = DaoImages.getImagesList();
            allNotesList.addAll(imagesList);
            Collections.sort(allNotesList, notesSorter);

            notesNames = new String[allNotesList.size()];
            notesMap.clear();
            int index = 0;
            for( INote note : allNotesList ) {
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
            List<INote> allNotesList = new ArrayList<INote>();
            List<Note> notesList = DaoNotes.getNotesList();
            allNotesList.addAll(notesList);
            List<Image> imagesList = DaoImages.getImagesList();
            allNotesList.addAll(imagesList);
            Collections.sort(allNotesList, notesSorter);

            notesMap.clear();
            filterText = ".*" + filterText.toLowerCase() + ".*"; //$NON-NLS-1$ //$NON-NLS-2$
            List<String> namesList = new ArrayList<String>();
            for( INote note : allNotesList ) {
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

                final ImageView shareButton = (ImageView) rowView.findViewById(R.id.sharebutton);
                shareButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        final String name = notesText.getText().toString();
                        INote iNote = notesMap.get(name);
                        float lat = (float) iNote.getLat();
                        float lon = (float) iNote.getLon();
                        String osmUrl = Utilities.osmUrlFromLatLong(lat, lon, true, false);
                        if (iNote instanceof Note) {
                            Note note = (Note) iNote;
                            if (note.getForm() == null || note.getForm().length() == 0) {
                                // simple note
                                String text = note.getName();
                                text = text + "\n" + osmUrl;
                                ShareUtilities.shareText(NotesListActivity.this, SHARE_NOTE_WITH, text);
                            } else {
                                int type = note.getType();
                                String form = note.getForm();
                                try {
                                    String formText = FormUtilities.formToPlainText(form, false);
                                    formText = formText + "\n" + osmUrl;
                                    if (form != null && form.length() > 0 && type != NoteType.OSM.getTypeNum()) {
                                        // double altim = note.getAltim();
                                        List<String> imagePaths = note.getImagePaths();
                                        File imageFile = null;
                                        if (imagePaths.size() > 0) {
                                            String imagePath = imagePaths.get(0);
                                            imageFile = new File(imagePath);
                                            if (!imageFile.exists()) {
                                                imageFile = null;
                                            }
                                        }
                                        if (imageFile != null) {
                                            ShareUtilities.shareTextAndImage(NotesListActivity.this, SHARE_NOTE_WITH, formText,
                                                    imageFile);
                                        } else {
                                            ShareUtilities.shareText(NotesListActivity.this, SHARE_NOTE_WITH, formText);
                                        }
                                    }
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    Utilities.errorDialog(NotesListActivity.this, e, null);
                                }
                            }

                        } else if (iNote instanceof Image) {
                            Image image = (Image) iNote;
                            File imageFile = new File(image.getPath());
                            try {
                                if (!imageFile.exists()) {
                                    // try relative to media
                                    File mediaDir = ResourcesManager.getInstance(NotesListActivity.this).getMediaDir();
                                    imageFile = new File(mediaDir.getParentFile(), image.getPath());
                                }
                            } catch (java.lang.Exception e) {
                                e.printStackTrace();
                            }
                            if (imageFile.exists()) {
                                ShareUtilities.shareTextAndImage(NotesListActivity.this, SHARE_NOTE_WITH, osmUrl, imageFile);
                            } else {
                                Utilities.errorDialog(NotesListActivity.this, new IOException("The image is missing: "
                                        + imageFile), null);
                            }
                        }

                    }
                });

                final ImageView editButton = (ImageView) rowView.findViewById(R.id.editbutton);
                editButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        final String name = notesText.getText().toString();
                        INote iNote = notesMap.get(name);
                        if (iNote instanceof Note) {
                            Note note = (Note) iNote;
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
                        } else if (iNote instanceof Image) {
                            Image image = (Image) iNote;
                            Intent intent = new Intent();
                            intent.setAction(android.content.Intent.ACTION_VIEW);
                            File absolutePath = new File(image.getPath());
                            if (!absolutePath.exists()) {
                                // try relative to media
                                try {
                                    File mediaDir = ResourcesManager.getInstance(NotesListActivity.this).getMediaDir();
                                    absolutePath = new File(mediaDir.getParentFile(), image.getPath());
                                } catch (java.lang.Exception e) {
                                    e.printStackTrace();
                                }
                            }
                            intent.setDataAndType(Uri.fromFile(absolutePath), "image/*"); //$NON-NLS-1$
                            NotesListActivity.this.startActivity(intent);
                        }

                    }
                });

                final ImageView deleteButton = (ImageView) rowView.findViewById(R.id.deletebutton);
                deleteButton.setOnClickListener(new View.OnClickListener(){
                    public void onClick( View v ) {
                        final String name = notesText.getText().toString();
                        final INote note = notesMap.get(name);
                        Utilities.yesNoMessageDialog(NotesListActivity.this, getString(R.string.prompt_delete_note),
                                new Runnable(){
                                    public void run() {
                                        new AsyncTask<String, Void, String>(){
                                            protected String doInBackground( String... params ) {
                                                return "";
                                            }

                                            protected void onPostExecute( String response ) {
                                                try {
                                                    if (note instanceof Note) {
                                                        DaoNotes.deleteNote(note.getId());
                                                    } else if (note instanceof Image) {
                                                        DaoImages.deleteImage(note.getId());
                                                    }
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
                        INote note = notesMap.get(notesText.getText().toString());
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
            // ignore
        }

        public void beforeTextChanged( CharSequence s, int start, int count, int after ) {
            // ignore
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
                        String textStr = formArray[4];
                        String jsonStr = formArray[6];

                        float n = (float) (lat + 0.00001f);
                        float s = (float) (lat - 0.00001f);
                        float w = (float) (lon - 0.00001f);
                        float e = (float) (lon + 0.00001f);

                        List<Note> notesInWorldBounds = DaoNotes.getNotesInWorldBounds(n, s, w, e);
                        if (notesInWorldBounds.size() > 0) {
                            Note note = notesInWorldBounds.get(0);
                            long id = note.getId();
                            DaoNotes.updateForm(id, textStr, jsonStr);
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

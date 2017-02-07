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
package eu.hydrologis.geopaparazzi.ui.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
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
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
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
import eu.hydrologis.geopaparazzi.GeopaparazziApplication;
import eu.hydrologis.geopaparazzi.R;
import eu.hydrologis.geopaparazzi.database.DaoImages;
import eu.hydrologis.geopaparazzi.database.DaoNotes;
import eu.hydrologis.geopaparazzi.database.objects.ItemComparators;
import eu.hydrologis.geopaparazzi.database.objects.Note;
import eu.hydrologis.geopaparazzi.mapview.MapviewActivity;

/**
 * Notes listing activity.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NotesListActivity extends AppCompatActivity {
    public static final String CURRENT_NOTES_COMPARATOR_INDEX = "CURRENT_NOTES_COMPARATOR_INDEX";
    private String SHARE_NOTE_WITH = "";
    private List<ANote> allNotesList = new ArrayList<>();
    private List<ANote> visibleNotesList = new ArrayList<>();
//    private Comparator<ANote> notesSorter = new ItemComparators.NotesComparator(false);

    private ArrayAdapter<ANote> arrayAdapter;
    private EditText filterText;

    private String share;
    private String edit;
    private String delete;
    private String asSelection;
    private String allNotesSubmenu;
    private String selectAll;
    private String invertSelection;
    private String deleteSelected;
    private String view;
    private ListView listView;

    private String[] checkItems;
    private Comparator<ANote>[] comparators;
    private int currentComparatorIndex = 0;
    private SharedPreferences mPreferences;
    private StringAsyncTask deletionTask;

    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        setContentView(R.layout.activity_noteslist);

        Toolbar toolbar = (Toolbar) findViewById(eu.geopaparazzi.mapsforge.R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);


        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        double[] mapCenterFromPreferences = PositionUtilities.getMapCenterFromPreferences(mPreferences, true, true);
        double lon = mapCenterFromPreferences[0];
        double lat = mapCenterFromPreferences[1];

        currentComparatorIndex = mPreferences.getInt(CURRENT_NOTES_COMPARATOR_INDEX, 0);

        checkItems = new String[]{//
                getString(R.string.sortby_none),//
                getString(R.string.sortby_name),//
                getString(R.string.sortby_name_inverse),//
                getString(R.string.sortby_mapcenterdist), //
                getString(R.string.sortby_mapcenterdist_inverse)//
        };
        comparators = new Comparator[]{//
                null, //
                new ItemComparators.NotesComparator(),//
                new ItemComparators.NotesComparator(true),//
                new ItemComparators.NotesDistanceFromPointComparator(lon, lat),//
                new ItemComparators.NotesDistanceFromPointComparator(lon, lat, true),//
        };

        listView = (ListView) findViewById(R.id.notesList);

        SHARE_NOTE_WITH = getString(eu.geopaparazzi.library.R.string.share_note_with);
        share = getString(R.string.share);
        edit = getString(R.string.edit);
        view = getString(R.string.view);
        delete = getString(R.string.delete);
        asSelection = getString(R.string.use_as_selection);
        allNotesSubmenu = getString(R.string.all_notes_submenu);
        selectAll = getString(R.string.select_all);
        invertSelection = getString(R.string.invert_selection);
        deleteSelected = getString(R.string.delete_selected);

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
        if (deletionTask != null) deletionTask.dispose();
        filterText.removeTextChangedListener(filterTextWatcher);
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_noteslist, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_sort) {
            DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    currentComparatorIndex = which;

                    SharedPreferences.Editor editor = mPreferences.edit();
                    editor.putInt(CURRENT_NOTES_COMPARATOR_INDEX, which);
                    editor.apply();
                }
            };

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.select_sort_mode);
            builder.setSingleChoiceItems(checkItems, currentComparatorIndex, dialogListener);
            builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialogInterface) {
                    try {
                        refreshList();
                    } catch (Exception e) {
                        GPLog.error(this, null, e);
                    }
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
        }
        return super.onOptionsItemSelected(item);
    }

    private void refreshList() {
        if (GPLog.LOG_HEAVY)
            GPLog.addLogEntry(this, "refreshing notes list"); //$NON-NLS-1$
        try {
            visibleNotesList.clear();
            collectAllNotes();
            visibleNotesList.addAll(allNotesList);
            if (comparators[currentComparatorIndex] != null) {
                Collections.sort(visibleNotesList, comparators[currentComparatorIndex]);
            }
        } catch (IOException e) {
            GPLog.error(this, e.getLocalizedMessage(), e);
            e.printStackTrace();
        }

        redoAdapter();
    }

    private void collectAllNotes() throws IOException {
        allNotesList.clear();
        List<Note> tmpNotesList = DaoNotes.getNotesList(null, false);
        allNotesList.addAll(tmpNotesList);
        List<Image> imagesList = DaoImages.getImagesList(false, true);
        allNotesList.addAll(imagesList);
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
                Button goButton;
                Button moreButton;
            }

            @Override
            public View getView(int position, View rowView, ViewGroup parent) {

                ViewHolder holder;
                // Recycle existing view if passed as parameter
                if (rowView == null) {
                    LayoutInflater inflater = getLayoutInflater();
                    rowView = inflater.inflate(R.layout.activity_noteslist_row, parent, false);
                    holder = new ViewHolder();
                    holder.checkButton = (CheckBox) rowView.findViewById(R.id.selectedCheckBox);
                    holder.notesText = (TextView) rowView.findViewById(R.id.notesrowtext);
                    holder.goButton = (Button) rowView.findViewById(R.id.gobutton);
                    holder.moreButton = (Button) rowView.findViewById(R.id.morebutton);

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

                final Button moreButton2 = holder.moreButton;
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

    private void openMoreMenu(Button button, final ANote currentNote) {
        String editLabel = null;
        if (currentNote instanceof Note) {
            Note note = (Note) currentNote;
            if (note.getForm() == null || note.getForm().length() == 0) {
                // simple note
                editLabel = null;
            } else {
                editLabel = edit;
            }
        } else if (currentNote instanceof Image) {
            // image
            editLabel = view;
        }

        PopupMenu popup = new PopupMenu(this, button);
        if (editLabel != null)
            popup.getMenu().add(editLabel);
        popup.getMenu().add(share);
        popup.getMenu().add(delete);
        popup.getMenu().add(asSelection);
        SubMenu subMenu = popup.getMenu().addSubMenu(allNotesSubmenu);
        subMenu.add(selectAll);
        subMenu.add(invertSelection);
        subMenu.add(deleteSelected);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                String actionName = item.getTitle().toString();
                if (actionName.equals(share)) {
                    shareNote(currentNote);
                } else if (actionName.equals(edit) || actionName.equals(view)) {
                    editNote(currentNote);
                } else if (actionName.equals(delete)) {
                    deleteNote(currentNote);
                } else if (actionName.equals(asSelection)) {
                    String name = currentNote.getName();
                    filterText.setText(name);
                } else if (actionName.equals(selectAll)) {
                    for (ANote aNote : visibleNotesList) {
                        aNote.setChecked(true);
                    }
                    arrayAdapter.notifyDataSetChanged();
                } else if (actionName.equals(invertSelection)) {
                    for (ANote aNote : visibleNotesList) {
                        aNote.setChecked(!aNote.isChecked());
                    }
                    arrayAdapter.notifyDataSetChanged();
                } else if (actionName.equals(deleteSelected)) {
                    deleteSelectedNotes();
                }
                return true;
            }


        });
        popup.show();
    }

    private void shareNote(ANote currentNote) {
        float lat = (float) currentNote.getLat();
        float lon = (float) currentNote.getLon();
        String osmUrl = UrlUtilities.osmUrlFromLatLong(lat, lon, true, false);
        if (currentNote instanceof Note) {
            Note note = (Note) currentNote;
            if (note.getForm() == null || note.getForm().length() == 0) {
                // simple note
                String text = note.getName();
                text = text + "\n" + osmUrl;
                ShareUtilities.shareText(NotesListActivity.this, SHARE_NOTE_WITH, text);
            } else {
                String description = note.getDescription();
                String form = note.getForm();
                try {
                    String formText = FormUtilities.formToPlainText(form, false);
                    formText = formText + "\n" + osmUrl;
                    if (form.length() > 0 && !description.equals(LibraryConstants.OSM)) {
                        // double altim = note.getAltim();

                        IImagesDbHelper imageHelper = DefaultHelperClasses.getDefaulfImageHelper();
                        File tempDir = ResourcesManager.getInstance(GeopaparazziApplication.getInstance()).getTempDir();

                        // for now only one image is shared
                        List<String> imageIds = note.getImageIds();
                        File imageFile = null;
                        if (imageIds.size() > 0) {
                            String imageId = imageIds.get(0);

                            Image image = imageHelper.getImage(Long.parseLong(imageId));

                            String imageName = image.getName();
                            imageFile = new File(tempDir, imageName);

                            byte[] imageData = imageHelper.getImageDataById(image.getImageDataId(), null);
                            ImageUtilities.writeImageDataToFile(imageData, imageFile.getAbsolutePath());
                        }
                        if (imageFile != null) {
                            ShareUtilities.shareTextAndImage(NotesListActivity.this, SHARE_NOTE_WITH, formText,
                                    imageFile);
                        } else {
                            ShareUtilities.shareText(NotesListActivity.this, SHARE_NOTE_WITH, formText);
                        }
                    }
                } catch (Exception e) {
                    GPLog.error(this, null, e); //$NON-NLS-1$
                    GPDialogs.errorDialog(NotesListActivity.this, e, null);
                }
            }

        } else if (currentNote instanceof Image) {
            Image image = (Image) currentNote;
            try {
                File tempDir = ResourcesManager.getInstance(NotesListActivity.this).getTempDir();
                String ext = ".jpg";
                if (image.getName().endsWith(".png"))
                    ext = ".png";
                File imageFile = new File(tempDir, ImageUtilities.getTempImageName(ext));
                byte[] imageData = new DaoImages().getImageData(image.getId());
                ImageUtilities.writeImageDataToFile(imageData, imageFile.getAbsolutePath());
                if (imageFile.exists()) {
                    ShareUtilities.shareTextAndImage(NotesListActivity.this, SHARE_NOTE_WITH, osmUrl, imageFile);
                } else {
                    GPDialogs.errorDialog(NotesListActivity.this, new IOException("The image is missing: "
                            + imageFile), null);
                }
            } catch (Exception e) {
                GPLog.error(this, null, e); //$NON-NLS-1$
            }
        }
    }

    private void editNote(ANote currentNote) {
        if (currentNote instanceof Note) {
            Note note = (Note) currentNote;
            if (note.getForm() == null || note.getForm().length() == 0) {
                // can't edit simple notes
                GPDialogs.warningDialog(this,
                        getString(R.string.only_complex_notes_editing), null);
            } else {
                String description = note.getDescription();
                double lat = note.getLat();
                double lon = note.getLon();
                String form = note.getForm();
                if (form.length() > 0 && !description.equals(LibraryConstants.OSM)) {
                    double altim = note.getAltim();

                    Intent formIntent = new Intent(this, FormActivity.class);
                    FormInfoHolder formInfoHolder = new FormInfoHolder();
                    formInfoHolder.sectionName = currentNote.getName();
                    formInfoHolder.formName = null;
                    formInfoHolder.noteId = note.getId();
                    formInfoHolder.longitude = lon;
                    formInfoHolder.latitude = lat;
                    formInfoHolder.elevation = altim;
                    formInfoHolder.sectionObjectString = form;
                    formInfoHolder.objectExists = true;
                    formIntent.putExtra(FormInfoHolder.BUNDLE_KEY_INFOHOLDER, formInfoHolder);

                    this.startActivityForResult(formIntent, MapviewActivity.FORMUPDATE_RETURN_CODE);
                }
            }
        } else if (currentNote instanceof Image) {
            try {
                Image image = (Image) currentNote;
                byte[] imageData = new DaoImages().getImageData(image.getId());
                AppsUtilities.showImage(imageData, image.getName(), this);
            } catch (Exception e) {
                GPLog.error(this, null, e);
            }

        }
    }

    private void deleteNote(final ANote currentNote) {
        GPDialogs.yesNoMessageDialog(NotesListActivity.this, getString(R.string.prompt_delete_note),
                new Runnable() {
                    public void run() {
                        new AsyncTask<String, Void, String>() {
                            protected String doInBackground(String... params) {
                                return "";
                            }

                            protected void onPostExecute(String response) {
                                try {
                                    if (currentNote instanceof Note) {
                                        DaoNotes.deleteNote(currentNote.getId());
                                    } else if (currentNote instanceof Image) {
                                        DaoImages.deleteImages(currentNote.getId());
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
                }, null
        );
    }

    private void deleteSelectedNotes() {
        GPDialogs.yesNoMessageDialog(NotesListActivity.this, getString(R.string.prompt_delete_selected_notes),
                new Runnable() {
                    public void run() {

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                int total = 0;
                                for (ANote aNote : visibleNotesList) {
                                    if (aNote.isChecked()) {
                                        total++;
                                    }
                                }

                                deletionTask = new StringAsyncTask(NotesListActivity.this) {
                                    protected String doBackgroundWork() {
                                        try {
                                            int index = 0;
                                            for (ANote aNote : visibleNotesList) {
                                                if (aNote.isChecked()) {
                                                    if (aNote instanceof Note) {
                                                        DaoNotes.deleteNote(aNote.getId());
                                                    } else if (aNote instanceof Image) {
                                                        DaoImages.deleteImages(aNote.getId());
                                                    }
                                                    publishProgress(index);
                                                    index++;
                                                }
                                            }
                                        } catch (Exception e) {
                                            return getString(R.string.error_while_removing_notes) + e.getLocalizedMessage();
                                        }
                                        return "";
                                    }

                                    protected void doUiPostWork(String response) {
                                        dispose();
                                        if (response.length() != 0) {
                                            GPDialogs.warningDialog(NotesListActivity.this, response, null);
                                        } else {
                                            refreshList();
                                        }
                                    }
                                };
                                deletionTask.setProgressDialog(null, getString(R.string.removing_notes), false, total);
                                deletionTask.execute();
                            }
                        });
                    }
                }, null
        );
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


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (GPLog.LOG_ABSURD)
            GPLog.addLogEntry(this, "Activity returned"); //$NON-NLS-1$
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (MapviewActivity.FORMUPDATE_RETURN_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    FormInfoHolder formInfoHolder = (FormInfoHolder) data.getSerializableExtra(FormInfoHolder.BUNDLE_KEY_INFOHOLDER);
                    if (formInfoHolder != null) {
                        try {
                            long noteId = formInfoHolder.noteId;
                            String nameStr = formInfoHolder.renderingLabel;
                            String jsonStr = formInfoHolder.sectionObjectString;

                            DaoNotes.updateForm(noteId, nameStr, jsonStr);
                        } catch (Exception e) {
                            GPLog.error(this, null, e);
                            GPDialogs.warningDialog(this, getString(eu.geopaparazzi.library.R.string.notenonsaved), null);
                        }
                    }
                }
                break;
            }
        }
    }


    public void clearFilter(View view) {
        filterText.setText("");
    }
}

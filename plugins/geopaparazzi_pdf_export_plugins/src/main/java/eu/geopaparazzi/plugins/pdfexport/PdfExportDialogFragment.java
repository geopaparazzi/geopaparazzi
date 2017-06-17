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

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;

import com.itextpdf.text.Anchor;
import com.itextpdf.text.Chapter;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Date;
import java.util.List;

import eu.geopaparazzi.core.R;
import eu.geopaparazzi.core.database.DaoImages;
import eu.geopaparazzi.core.database.DaoNotes;
import eu.geopaparazzi.core.database.objects.Note;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.Image;
import eu.geopaparazzi.library.forms.FormUtilities;
import eu.geopaparazzi.library.forms.TagsManager;
import eu.geopaparazzi.library.util.TimeUtilities;
import eu.geopaparazzi.library.util.Utilities;


/**
 * Dialog for pdf files export.
 *
 * @author Andrea Antonello
 */
public class PdfExportDialogFragment extends DialogFragment {
    public static final String NODATA = "NODATA";
    public static final String PDF_PATH = "exportPath";
    public static final String INTERRUPTED = "INTERRUPTED";
    private ProgressBar progressBar;
    private String exportPath;

    private boolean isInterrupted = false;
    private AlertDialog alertDialog;
    private Button positiveButton;


    /**
     * Create a dialog instance.
     *
     * @param exportPath an optional path to which to export the kmz to. If null, a default path is chosen.
     * @return the instance.
     */
    public static PdfExportDialogFragment newInstance(String exportPath) {
        PdfExportDialogFragment f = new PdfExportDialogFragment();
        Bundle args = new Bundle();
        args.putString(PDF_PATH, exportPath);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        exportPath = getArguments().getString(PDF_PATH);
    }

    @Override
    public Dialog onCreateDialog(Bundle bundle) {

        AlertDialog.Builder builder =
                new AlertDialog.Builder(getActivity());
        View gpsinfoDialogView = getActivity().getLayoutInflater().inflate(
                R.layout.fragment_dialog_progressbar, null);
        builder.setView(gpsinfoDialogView);
        builder.setMessage(eu.geopaparazzi.plugins.pdfexport.R.string.exporting_data_to_pdf);

        progressBar = (ProgressBar) gpsinfoDialogView.findViewById(
                R.id.progressBar);

        builder.setNegativeButton(android.R.string.cancel,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        isInterrupted = true;
                    }
                }
        );
        builder.setPositiveButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                }
        );

        progressBar.setIndeterminate(true);

        alertDialog = builder.create();
        return alertDialog;
    }

    private void startExport() {
        final Context context = getContext();

        new AsyncTask<String, Void, String>() {
            protected String doInBackground(String... params) {
                File pdfOutputFile = null;
                try {
                    boolean hasAtLeastOne = false;
                    /*
                     * get notes
                     */
                    List<Note> notesList = DaoNotes.getNotesList(null, false);
                    if (notesList.size() == 0) {
                        return NODATA;
                    }
                    if (isInterrupted) return INTERRUPTED;

                    File pdfExportDir = ResourcesManager.getInstance(getActivity()).getSdcardDir();
                    String filename = ResourcesManager.getInstance(getActivity()).getApplicationName() + "_projectexport_" + TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.format(new Date()) + ".pdf";
                    pdfOutputFile = new File(pdfExportDir, filename);
                    if (exportPath != null) {
                        pdfOutputFile = new File(exportPath);
                    }

                    // TODO export list to pdf
                    Document document = new Document();
                    document.setMargins(36, 36, 36, 36);
                    PdfWriter.getInstance(document, new FileOutputStream(pdfOutputFile));
                    document.open();

                    document.addTitle("Geopaparazzi PDF Export");
                    document.addSubject("Geopaparazzi PDF Export");
                    document.addKeywords("geopaparazzi, export, notes");
                    document.addAuthor("Geopaparazzi User");
                    document.addCreator("Geopaparazzi - http://www.geopaparazzi.eu");

                    int index = 1;
                    for (Note note : notesList) {
                        processNote(document, note, index++);
                    }

                    document.close();

                    return pdfOutputFile.getAbsolutePath();
                } catch (Exception e) {
                    // cleanup as it might be inconsistent
                    if (pdfOutputFile != null && pdfOutputFile.exists()) {
                        pdfOutputFile.delete();
                    }
                    GPLog.error(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                    return ""; //$NON-NLS-1$
                }
            }

            protected void onPostExecute(String response) { // on UI thread!
                progressBar.setVisibility(View.GONE);

                if (response.equals(NODATA)) {
                    String msg = context.getString(R.string.no_data_found_in_project_to_export);
                    alertDialog.setMessage(msg);
                } else if (response.equals(INTERRUPTED)) {
                    alertDialog.setMessage(context.getString(R.string.interrupted_by_user));
                } else if (response.length() > 0) {
                    String msg = context.getString(R.string.datasaved) + " " + response;
                    alertDialog.setMessage(msg);
                } else {
                    String msg = context.getString(R.string.data_nonsaved);
                    alertDialog.setMessage(msg);
                }
                if (positiveButton != null) positiveButton.setEnabled(true);

            }
        }.execute((String) null);
    }

    public void processNote(Document document, Note note, int count) throws Exception {
        String name = Utilities.makeXmlSafe(note.getName());
        String form = note.getForm();

        DaoImages daoImages = new DaoImages();
        if (form != null && form.length() > 0) {
            JSONObject sectionObject = new JSONObject(form);
            if (!sectionObject.has(FormUtilities.ATTR_SECTIONNAME)) {
                return;
            }
            String sectionName = sectionObject.getString(FormUtilities.ATTR_SECTIONNAME);
            Anchor anchor = new Anchor(sectionName);
            anchor.setName(sectionName);
            Chapter currentChapter = new Chapter(new Paragraph(anchor), count);
            addEmptyLine(currentChapter, 3);

            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setHeaderRows(0);
            infoTable.setWidthPercentage(90);
            currentChapter.add(infoTable);

            addKeyValueToTableRow(infoTable, "Timestamp", new Date(note.getTimeStamp()).toString());
            addKeyValueToTableRow(infoTable, "Latitude", note.getLat() + "");
            addKeyValueToTableRow(infoTable, "Longitude", note.getLon() + "");

            addEmptyLine(currentChapter, 3);

            List<String> formsNames = TagsManager.getFormNames4Section(sectionObject);
            for (String formName : formsNames) {
                Paragraph section = new Paragraph(formName);
                currentChapter.addSection(section);
                addEmptyLine(currentChapter, 3);

                PdfPTable currentTable = new PdfPTable(2);
                currentTable.setHeaderRows(1);
                currentTable.setWidthPercentage(90);
                currentChapter.add(currentTable);

                JSONObject form4Name = TagsManager.getForm4Name(formName, sectionObject);
                JSONArray formItems = TagsManager.getFormItems(form4Name);
                for (int i = 0; i < formItems.length(); i++) {
                    JSONObject formItem = formItems.getJSONObject(i);
                    if (!formItem.has(FormUtilities.TAG_KEY)) {
                        continue;
                    }

                    String type = formItem.getString(FormUtilities.TAG_TYPE);
                    String key = formItem.getString(FormUtilities.TAG_KEY);
                    String value = formItem.getString(FormUtilities.TAG_VALUE);

                    String label = key;
                    if (formItem.has(FormUtilities.TAG_LABEL)) {
                        label = formItem.getString(FormUtilities.TAG_LABEL);
                    }

                    if (type.equals(FormUtilities.TYPE_PICTURES)) {
                        if (value.trim().length() == 0) {
                            continue;
                        }
                        String[] imageIdsSplit = value.split(Note.IMAGES_SEPARATOR);
                        for (String imageId : imageIdsSplit) {
                            Image image = daoImages.getImage(Long.parseLong(imageId));
                            String imgName = image.getName();
                            byte[] imageData = daoImages.getImageData(Long.parseLong(imageId));
                            com.itextpdf.text.Image itextImage = com.itextpdf.text.Image.getInstance(imageData);
                            Paragraph caption = new Paragraph(imgName);
                            caption.setAlignment(Element.ALIGN_CENTER);

                            PdfPCell keyCell = new PdfPCell(new Phrase(label));
                            keyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            keyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                            keyCell.setPadding(10);
                            currentTable.addCell(keyCell);
                            PdfPCell valueCell = new PdfPCell();
                            valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                            valueCell.setPadding(10);
                            valueCell.addElement(itextImage);
                            valueCell.addElement(caption);
                            currentTable.addCell(valueCell);
                        }
                    } else if (type.equals(FormUtilities.TYPE_MAP)) {
                        if (value.trim().length() == 0) {
                            continue;
                        }
                        String imageId = value.trim();
                        Image image = daoImages.getImage(Long.parseLong(imageId));
                        String imgName = image.getName();
                        byte[] imageData = daoImages.getImageData(Long.parseLong(imageId));
                        com.itextpdf.text.Image itextImage = com.itextpdf.text.Image.getInstance(imageData);
                        Paragraph caption = new Paragraph(imgName);
                        caption.setAlignment(Element.ALIGN_CENTER);

                        PdfPCell keyCell = new PdfPCell(new Phrase(label));
                        keyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        keyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        keyCell.setPadding(10);
                        currentTable.addCell(keyCell);
                        PdfPCell valueCell = new PdfPCell();
                        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        valueCell.setPadding(10);
                        valueCell.addElement(itextImage);
                        valueCell.addElement(caption);
                        currentTable.addCell(valueCell);
                    } else if (type.equals(FormUtilities.TYPE_SKETCH)) {
                        if (value.trim().length() == 0) {
                            continue;
                        }
                        String[] imageIdsSplit = value.split(Note.IMAGES_SEPARATOR);
                        for (String imageId : imageIdsSplit) {
                            Image image = daoImages.getImage(Long.parseLong(imageId));
                            String imgName = image.getName();
                            byte[] imageData = daoImages.getImageData(Long.parseLong(imageId));
                            com.itextpdf.text.Image itextImage = com.itextpdf.text.Image.getInstance(imageData);
                            Paragraph caption = new Paragraph(imgName);
                            caption.setAlignment(Element.ALIGN_CENTER);

                            PdfPCell keyCell = new PdfPCell(new Phrase(label));
                            keyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            keyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                            keyCell.setPadding(10);
                            currentTable.addCell(keyCell);
                            PdfPCell valueCell = new PdfPCell();
                            valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                            valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                            valueCell.setPadding(10);
                            valueCell.addElement(itextImage);
                            valueCell.addElement(caption);
                            currentTable.addCell(valueCell);
                        }
                    } else {
                        addKeyValueToTableRow(currentTable, label, value);
                    }
                }
            }

            document.add(currentChapter);
            document.newPage();

        }


    }

    private void addKeyValueToTableRow(PdfPTable table, String key, String value) {
        PdfPCell keyCell = new PdfPCell(new Phrase(key));
        keyCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        keyCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        keyCell.setPadding(10);
        table.addCell(keyCell);
        PdfPCell valueCell = new PdfPCell(new Phrase(value));
        valueCell.setHorizontalAlignment(Element.ALIGN_CENTER);
        valueCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        valueCell.setPadding(10);
        table.addCell(valueCell);
    }

    public void addEmptyLine(Chapter element, int number) throws DocumentException {
        for (int i = 0; i < number; i++) {
            Paragraph p = new Paragraph(" ");
            element.add(p);
        }
    }

    public void onStart() {
        super.onStart();
        AlertDialog d = (AlertDialog) getDialog();
        if (d != null) {
            positiveButton = d.getButton(Dialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(false);
        }
        startExport();
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

}

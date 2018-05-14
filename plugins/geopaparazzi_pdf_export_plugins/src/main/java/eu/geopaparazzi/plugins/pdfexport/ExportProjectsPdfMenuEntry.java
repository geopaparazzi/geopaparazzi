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

import android.content.Context;
import android.content.Intent;

import eu.geopaparazzi.library.plugin.types.MenuEntry;
import eu.geopaparazzi.library.util.IActivitySupporter;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ExportProjectsPdfMenuEntry extends MenuEntry {

    private final Context serviceContext;
    private IActivitySupporter clickActivityStarter;

    public ExportProjectsPdfMenuEntry(Context context) {
        this.serviceContext = context;
    }

    @Override
    public String getLabel() {
        return serviceContext.getString(eu.geopaparazzi.core.R.string.data_to_pdf_label);
    }

    @Override
    public int getOrder() {
        return 9999;
    }

    @Override
    public void onClick(final IActivitySupporter clickActivityStarter) {
        this.clickActivityStarter = clickActivityStarter;

        Intent preferencesIntent = new Intent(clickActivityStarter.getContext(), PdfExportNotesListActivity.class);
        clickActivityStarter.startActivity(preferencesIntent);

//        PdfExportDialogFragment pdfExportDialogFragment = PdfExportDialogFragment.newInstance(null, exportIds);
//        pdfExportDialogFragment.show(clickActivityStarter.getSupportFragmentManager(), "pdf export");
    }

}

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
package eu.geopaparazzi.library.kml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import eu.geopaparazzi.library.util.CompressionUtilities;
import eu.geopaparazzi.library.util.ResourcesManager;
import eu.geopaparazzi.library.util.debug.Logger;

/**
 * A kmz exporter for notes, logs and pics.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class KmlExport {

    private final File outputFile;
    private String name;

    public KmlExport( String name, File outputFile ) {
        this.name = name;
        this.outputFile = outputFile;
    }

    public void export( Context context, List<KmlRepresenter> kmlRepresenters, String folderName ) throws IOException {
        if (name == null) {
            name = "Geopaparazzi Export";
        }

        File applicationDir = ResourcesManager.getInstance(context).getApplicationDir();
        File outputDir = new File(applicationDir, "media");
        if (folderName != null) {
            outputDir = new File(applicationDir, folderName);
        }

        List<File> existingImages = new ArrayList<File>();

        /*
         * write the internal kml file
         */
        File kmlFile = new File(outputFile.getParentFile(), "kml.kml");
        BufferedWriter bW = null;
        try {
            bW = new BufferedWriter(new FileWriter(kmlFile));
            bW.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            bW.write("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\"\n");
            bW.write("xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
            bW.write("<Document>\n");
            bW.write("<name>");
            bW.write(name);
            bW.write("</name>\n");
            bW.write("<Style id=\"red-pushpin\">\n");
            bW.write("<IconStyle>\n");
            bW.write("<scale>1.1</scale>\n");
            bW.write("<Icon>\n");
            bW.write("<href>http://maps.google.com/mapfiles/kml/pushpin/red-pushpin.png\n");
            bW.write("</href>\n");
            bW.write("</Icon>\n");
            bW.write("<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\" />\n");
            bW.write("</IconStyle>\n");
            bW.write("<ListStyle>\n");
            bW.write("</ListStyle>\n");
            bW.write("</Style>\n");
            bW.write("<Style id=\"yellow-pushpin\">\n");
            bW.write("<IconStyle>\n");
            bW.write("<scale>1.1</scale>\n");
            bW.write("<Icon>\n");
            bW.write("<href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png\n");
            bW.write("</href>\n");
            bW.write("</Icon>\n");
            bW.write("<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\" />\n");
            bW.write("</IconStyle>\n");
            bW.write("<ListStyle>\n");
            bW.write("</ListStyle>\n");
            bW.write("</Style>\n");

            for( KmlRepresenter kmlRepresenter : kmlRepresenters ) {
                try {
                    bW.write(kmlRepresenter.toKmlString());

                    if (kmlRepresenter.hasImage()) {
                        String imagePath = kmlRepresenter.getImagePath();
                        File imageFile = new File(outputDir.getParentFile(), imagePath);
                        if (imageFile.exists()) {
                            existingImages.add(imageFile);
                        } else {
                            Logger.w(this, "Can't find image: " + imageFile.getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    Logger.e(this, e.getLocalizedMessage(), e);
                    e.printStackTrace();
                }
            }

            bW.write("</Document>\n");
            bW.write("</kml>\n");

        } finally {
            if (bW != null)
                bW.close();
        }
        /*
         * create the kmz file with the base kml and all the pictures
         */

        File[] files = new File[1 + existingImages.size()];
        files[0] = kmlFile;
        for( int i = 0; i < files.length - 1; i++ ) {
            files[i + 1] = existingImages.get(i);
        }
        CompressionUtilities.createZipFromFiles(outputFile, files);

        kmlFile.delete();
    }
}

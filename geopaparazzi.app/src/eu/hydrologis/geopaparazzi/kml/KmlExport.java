/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package eu.hydrologis.geopaparazzi.kml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import eu.hydrologis.geopaparazzi.util.CompressionUtilities;
import eu.hydrologis.geopaparazzi.util.Line;
import eu.hydrologis.geopaparazzi.util.Note;
import eu.hydrologis.geopaparazzi.util.Picture;

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

    public void export( List<Note> notes, HashMap<Long, Line> linesMap, List<Picture> picturesList ) {
        if (name == null) {
            name = "Geopaparazzi Export";
        }
        StringBuilder sB = new StringBuilder();
        sB.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sB.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\"\n");
        sB.append("xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        sB.append("<Document>\n");
        sB.append("<name>").append(name).append("</name>\n");
        sB.append("<Style id=\"red-pushpin\">\n");
        sB.append("<IconStyle>\n");
        sB.append("<scale>1.1</scale>\n");
        sB.append("<Icon>\n");
        sB.append("<href>http://maps.google.com/mapfiles/kml/pushpin/red-pushpin.png\n");
        sB.append("</href>\n");
        sB.append("</Icon>\n");
        sB.append("<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\" />\n");
        sB.append("</IconStyle>\n");
        sB.append("<ListStyle>\n");
        sB.append("</ListStyle>\n");
        sB.append("</Style>\n");
        sB.append("<Style id=\"yellow-pushpin\">\n");
        sB.append("<IconStyle>\n");
        sB.append("<scale>1.1</scale>\n");
        sB.append("<Icon>\n");
        sB.append("<href>http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png\n");
        sB.append("</href>\n");
        sB.append("</Icon>\n");
        sB.append("<hotSpot x=\"20\" y=\"2\" xunits=\"pixels\" yunits=\"pixels\" />\n");
        sB.append("</IconStyle>\n");
        sB.append("<ListStyle>\n");
        sB.append("</ListStyle>\n");
        sB.append("</Style>\n");

        for( Note note : notes ) {
            sB.append(note.toKmlString());
        }
        Collection<Line> lines = linesMap.values();
        for( Line line : lines ) {
            if (line.getLatList().size() > 1)
                sB.append(line.toKmlString());
        }
        for( Picture picture : picturesList ) {
            sB.append(picture.toKmlString());
        }

        sB.append("</Document>\n");
        sB.append("</kml>\n");

        /*
         * write the internal kml file
         */
        File kmlFile = new File(outputFile.getParentFile(), "kml.kml");
        try {
            BufferedWriter bW = new BufferedWriter(new FileWriter(kmlFile));
            bW.write(sB.toString());
            bW.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
         * create the kmz file with the base kml and all the pictures
         */
        File[] files = new File[1 + picturesList.size()];
        files[0] = kmlFile;
        for( int i = 0; i < files.length - 1; i++ ) {
            files[i + 1] = new File(picturesList.get(i).getPicturePath());
        }
        try {
            CompressionUtilities.createZipFromFiles(outputFile, files);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

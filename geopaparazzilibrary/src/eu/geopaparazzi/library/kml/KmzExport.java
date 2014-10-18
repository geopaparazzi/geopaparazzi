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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.content.Context;

import eu.geopaparazzi.library.database.DefaultHelperClasses;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.database.IImagesDbHelper;
import eu.geopaparazzi.library.database.Image;

/**
 * A kmz exporter for notes, logs and pics.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class KmzExport {

    private final File outputFile;
    private String name;

    /**
     * Constructor.
     *
     * @param name       a name for the kmz.
     * @param outputFile the path in which to create the kmz.
     */
    public KmzExport(String name, File outputFile) {
        this.name = name;
        this.outputFile = outputFile;
    }

    /**
     * Export.
     *
     * @param context         the context to use.
     * @param kmlRepresenters the list of data representers.
     * @throws Exception if something goes wrong.
     */
    public void export(Context context, List<KmlRepresenter> kmlRepresenters) throws Exception {
        if (name == null) {
            name = "Geopaparazzi Export";
        }

        /*
         * write the internal kml file
         */
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        stringBuilder.append("<kml xmlns=\"http://www.opengis.net/kml/2.2\" xmlns:gx=\"http://www.google.com/kml/ext/2.2\"\n");
        stringBuilder.append("xmlns:kml=\"http://www.opengis.net/kml/2.2\" xmlns:atom=\"http://www.w3.org/2005/Atom\">\n");
        stringBuilder.append("<Document>\n");
        stringBuilder.append("<name>");
        stringBuilder.append(name);
        stringBuilder.append("</name>\n");
        addMarker(stringBuilder, "red-pushpin", "http://maps.google.com/mapfiles/kml/pushpin/red-pushpin.png", 20, 2);
        addMarker(stringBuilder, "yellow-pushpin", "http://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png", 20, 2);
        addMarker(stringBuilder, "bookmark-icon", "http://maps.google.com/mapfiles/kml/pal4/icon39.png", 16, 16);
        addMarker(stringBuilder, "camera-icon", "http://maps.google.com/mapfiles/kml/pal4/icon38.png", 16, 16);
        addMarker(stringBuilder, "info-icon", "http://maps.google.com/mapfiles/kml/pal3/icon35.png", 16, 16);

        for (KmlRepresenter kmlRepresenter : kmlRepresenters) {
            try {
                stringBuilder.append(kmlRepresenter.toKmlString());
            } catch (Exception e) {
                GPLog.error(this, e.getLocalizedMessage(), e);
                e.printStackTrace();
            }
        }
        stringBuilder.append("</Document>\n");
        stringBuilder.append("</kml>\n");

        byte[] kmlBytes = stringBuilder.toString().getBytes(Charset.forName("UTF-8"));

        /*
         * start adding the kml part
         */
        FileOutputStream fos = new FileOutputStream(outputFile);
        ZipOutputStream zos = new ZipOutputStream(fos);
        CRC32 crc = new CRC32();
        String kmlName = "kml.kml";
        crc.update(kmlBytes);
        ZipEntry entry = new ZipEntry(kmlName);
        entry.setMethod(ZipEntry.STORED);
        entry.setCompressedSize(kmlBytes.length);
        entry.setSize(kmlBytes.length);
        entry.setCrc(crc.getValue());
        zos.putNextEntry(entry);
        zos.write(kmlBytes);

        /*
         * now add all images
         */
        Class<?> logHelper = Class.forName(DefaultHelperClasses.IMAGE_HELPER_CLASS);
        IImagesDbHelper imagesDbHelper = (IImagesDbHelper) logHelper.newInstance();
        for (KmlRepresenter kmlRepresenter : kmlRepresenters) {
            if (kmlRepresenter.hasImages()) {
                List<String> imageIds = kmlRepresenter.getImageIds();
                for (String imageId : imageIds) {
                    long id = Long.parseLong(imageId);
                    Image image = imagesDbHelper.getImage(id);
                    byte[] imageData = imagesDbHelper.getImageData(id);

                    crc.reset();
                    crc.update(imageData);
                    ZipEntry imageEntry = new ZipEntry(image.getName());
                    imageEntry.setMethod(ZipEntry.STORED);
                    imageEntry.setCompressedSize(imageData.length);
                    imageEntry.setSize(imageData.length);
                    imageEntry.setCrc(crc.getValue());
                    zos.putNextEntry(imageEntry);
                    zos.write(imageData);
                }
            }
        }
        zos.close();
    }

    private void addMarker(StringBuilder sb, String alias, String url, int x, int y) throws IOException {
        sb.append("<Style id=\"" + alias + "\">\n");
        sb.append("<IconStyle>\n");
        sb.append("<scale>1.1</scale>\n");
        sb.append("<Icon>\n");
        sb.append("<href>" + url + "\n");
        sb.append("</href>\n");
        sb.append("</Icon>\n");
        sb.append("<hotSpot x=\"" + x + "\" y=\"" + y + "\" xunits=\"pixels\" yunits=\"pixels\" />\n");
        sb.append("</IconStyle>\n");
        sb.append("<ListStyle>\n");
        sb.append("</ListStyle>\n");
        sb.append("</Style>\n");
    }
}

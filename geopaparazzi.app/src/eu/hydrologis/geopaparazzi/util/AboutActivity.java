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
package eu.hydrologis.geopaparazzi.util;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.text.Spanned;
import android.widget.TextView;
import eu.hydrologis.geopaparazzi.R;

/**
 * The about view.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AboutActivity extends Activity {
    @SuppressWarnings("nls")
    public void onCreate( Bundle icicle ) {
        super.onCreate(icicle);
        setContentView(R.layout.about);

        TextView aboutView = (TextView) findViewById(R.id.about);
        
        StringBuilder sB = new StringBuilder();
        sB.append("<h1>GeoPaparazzi</h1>");
        sB.append("written by Andrea Antonello<br>");
        sB.append("<br>");
        sB.append("is brought to you by ");
        sB.append("<a href=\"http://www.hydrologis.eu\">HydroloGIS (www.hydrologis.eu)</a>.<br>");
        sB.append("<br>");
        sB.append("The main website of the project is <a href=\"http://www.geopaparazzi.eu\">www.geopaparazzi.eu</a>.<br>");
        sB.append("<hr style=\"width: 100%; height: 2px;\"><br>");
        sB.append("GeoPaparazzi is the little brother of:<br>");
        sB.append("<br>");
        sB.append("<a href=\"http://www.jgrass.org\">JGrass (www.jgrass.org)</a><br>");
        sB.append("<br>");
        sB.append("<a href=\"http://www.beegis.org\">BeeGIS (www.beegis.org)</a><br>");
        sB.append("<br>");
        sB.append("and as such is able to load collected data into those free and open");
        sB.append("source GIS.<br>");
        sB.append("<br>");
        sB.append("The chart view is adapted from the GraphView by Arno den Hond.<br>");
        sB.append("<br>");
        sB.append("The free maps come from <a href=\"http://www.openstreetmap.org//\">");
        sB.append("OpenStreetMap (www.openstreetmap.org)</a>.");
        sB.append("The compass is the logo of the <a href=\"http://www.osgeo.org/\">Open");
        sB.append("Source Geospatial Foundation (www.osgeo.org)</a>.");
        
        Spanned fromHtml = Html.fromHtml(sB.toString());
        aboutView.setText(fromHtml);
        
    }
}

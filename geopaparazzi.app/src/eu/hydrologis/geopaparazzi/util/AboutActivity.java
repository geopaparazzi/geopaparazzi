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

        String[] aboutText = getResources().getString(R.string.abouttext).split("\\n");
        StringBuilder sB = new StringBuilder();
        boolean first = true;
        for( String line : aboutText ) {
            line = line.trim();
            
            if (first) {
                sB.append("<h1>");
                sB.append(line);
                sB.append("</h1>");
                
                first = false;
            }else{
                sB.append(line).append("<BR>");
            }
            
        }

        Spanned fromHtml = Html.fromHtml(sB.toString());
        aboutView.setText(fromHtml);

    }
}

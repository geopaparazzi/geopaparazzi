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
package eu.hydrologis.geopaparazzi.util;

import android.app.Activity;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
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

        String aboutString = getResources().getString(R.string.abouttext);

        String version = "";
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo("eu.hydrologis.geopaparazzi", PackageManager.GET_META_DATA);
            version = pInfo.versionName;
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        aboutString = aboutString.replaceFirst("VERSION", version);
        String[] aboutText = aboutString.split("\\n");
        StringBuilder sB = new StringBuilder();
        boolean first = true;
        for( String line : aboutText ) {
            line = line.trim();

            if (first) {
                sB.append("<h1>");
                sB.append(line);
                sB.append("</h1>");

                first = false;
            } else {
                sB.append(line).append("<BR>");
            }

        }

        Spanned fromHtml = Html.fromHtml(sB.toString());
        aboutView.setText(fromHtml);

    }

}

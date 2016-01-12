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

package eu.geopaparazzi.library.util.fragments;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import java.io.IOException;
import java.io.InputStream;
import java.util.Scanner;

import eu.geopaparazzi.library.util.LibraryConstants;

/**
 * The fragment_about view.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class AboutFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View v = inflater.inflate(eu.geopaparazzi.library.R.layout.frag, container, false);

        return v; // return the fragment's view for display
    }

    public void setData(Bundle savedInstanceState) {
        String packageName = null;
        if (savedInstanceState != null) {
            packageName = savedInstanceState.getString(LibraryConstants.PREFS_KEY_TEXT);
        }

        try {
            AssetManager assetManager = getActivity().getAssets();
            InputStream inputStream = assetManager.open("about.html");
            String htmlText = new Scanner(inputStream).useDelimiter("\\A").next();

            if (packageName != null) {
                String version = "";
                try {
                    PackageInfo pInfo = getActivity().getPackageManager().getPackageInfo(packageName, PackageManager.GET_META_DATA);
                    version = pInfo.versionName;
                } catch (NameNotFoundException e) {
                    e.printStackTrace();
                }

                htmlText = htmlText.replaceFirst("VERSION", version);
            }

            WebView aboutView = (WebView) view;
            aboutView.loadData(htmlText, "text/html", "utf-8");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

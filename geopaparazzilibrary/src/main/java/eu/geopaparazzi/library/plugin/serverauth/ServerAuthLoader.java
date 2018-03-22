package eu.geopaparazzi.library.plugin.serverauth;
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

import android.content.Context;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import eu.geopaparazzi.library.plugin.PluginLoader;
import eu.geopaparazzi.library.plugin.menu.IMenuLoader;
import eu.geopaparazzi.library.plugin.types.IMenuEntry;
import eu.geopaparazzi.library.plugin.types.IMenuEntryList;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class ServerAuthLoader extends PluginLoader {
    private ArrayList<IAuthProvider> authProviders = new ArrayList<IAuthProvider>();
    public ServerAuthLoader(Context context, String providerName) {
        super(context, providerName);
    }

    @Override
    protected void doProcessService(IBinder binder) {
        IAuthProvider provider = (IAuthProvider) binder;
        authProviders.add(provider);
    }

    public List<IAuthProvider> getEntries() {
        if (isLoadComplete()) {
            return authProviders;
        }
        else {
            throw new RuntimeException("Providers still not ready");
        }
    }

    @Override
    protected void onLoadComplete() {
        Collections.sort(authProviders, new Comparator<IAuthProvider>() {
            @Override
            public int compare(IAuthProvider t0, IAuthProvider t1) {
                // return natural order comparison, but consider that
                // - numbers<0 are bigger than numbers >=0
                // - numbers <0 have the same order as other numbers <0
                if (t0.getOrder()<0) {
                    if (t1.getOrder()<0) {
                        return 0;
                    }
                    else {
                        return 1;
                    }
                }
                else if (t1.getOrder()<0) {
                    return -1;
                }
                else if (t0.getOrder()<t1.getOrder()) {
                    return -1;
                }
                else if (t0.getOrder()==t1.getOrder()) {
                    return 0;
                }
                else {
                    return 1;
                }
            }
        });
    }

}

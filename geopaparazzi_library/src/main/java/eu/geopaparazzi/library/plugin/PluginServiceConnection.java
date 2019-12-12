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
package eu.geopaparazzi.library.plugin;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public abstract class PluginServiceConnection implements android.content.ServiceConnection {
    private boolean bound = false;

    @Override
    public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
        bound = true;
    }

    @Override
    public void onServiceDisconnected(ComponentName componentName) {
        bound = false;
    }

    public boolean isBound() {
        return bound;
    }

    public void disconnect(Context context) {
        if (bound) {
            bound = false;
            context.unbindService(this);
        }
    }

}

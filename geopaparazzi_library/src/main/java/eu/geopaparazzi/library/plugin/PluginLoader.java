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

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.List;

import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.plugin.types.IMenuEntryList;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public abstract class PluginLoader {

    protected final Context context;
    protected String serviceName = null;
    private int numberOfPlugins;
    protected PluginServiceConnection pluginServiceConn = new PluginServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            super.onServiceConnected(name, service);
            processService(service);
        }
    };
    private List<PluginLoaderListener> listeners = new ArrayList<PluginLoaderListener>();
    private boolean isLoadComplete = false;

    protected abstract void doProcessService(IBinder binder);

    public PluginLoader(Context context, String serviceName) {
        this.context = context;
        this.serviceName = serviceName;
    }

    public void addListener(PluginLoaderListener listener) {
        listeners.add(listener);
    }

    public void removeListener(PluginLoaderListener listener) {
        listeners.remove(listener);
    }

    public void connect() {
        PackageManager packageManager = context.getPackageManager();

        Intent intent = new Intent(this.serviceName);
        try {
            String packageName = ResourcesManager.getInstance(context).getPackageName();
            intent.setPackage(packageName);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<ResolveInfo> menuProviders = packageManager.queryIntentServices(intent, PackageManager.GET_RESOLVED_FILTER);
        numberOfPlugins = menuProviders.size();
        for (int i = 0; i < menuProviders.size(); ++i) {
            ResolveInfo info = menuProviders.get(i);
            ServiceInfo sinfo = info.serviceInfo;

            String packageName = sinfo.packageName;
            String className = sinfo.name;
            ComponentName component = new ComponentName(packageName, className);
            Intent explicitIntent = new Intent(intent);
            explicitIntent.setComponent(component);

            context.bindService(explicitIntent, pluginServiceConn, Service.BIND_AUTO_CREATE);
        }
    }

    private synchronized void processService(IBinder binder) {
        doProcessService(binder);
        numberOfPlugins = numberOfPlugins - 1;
        if (numberOfPlugins == 0) { // all the plugins have been processed
            if (pluginServiceConn.isBound()) {
                context.unbindService(pluginServiceConn);
            }
            onLoadComplete();
            isLoadComplete = true;
            for (PluginLoaderListener listener : listeners) {
                listener.pluginLoaded(this);
            }
        }
    }


    public void disconnect() {
        pluginServiceConn.disconnect(context);
    }

    public boolean isLoadComplete() {
        return isLoadComplete;
    }

    protected abstract void onLoadComplete();

}

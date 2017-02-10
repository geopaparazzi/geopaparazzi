package eu.geopaparazzi.library.plugin.menu;


import android.content.Context;
import android.os.IBinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import eu.geopaparazzi.library.plugin.PluginLoader;
import eu.geopaparazzi.library.plugin.types.IMenuEntry;
import eu.geopaparazzi.library.plugin.types.IMenuEntryList;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class MenuLoader extends PluginLoader implements IMenuLoader {
    private ArrayList<IMenuEntryList> menuGroups = new ArrayList<IMenuEntryList>();
    private ArrayList<IMenuEntry> menuEntries = new ArrayList<IMenuEntry>();
    public MenuLoader(Context context, String providerName) {
        super(context, providerName);
    }

    @Override
    protected void doProcessService(IBinder binder) {
        IMenuEntryList entryGroup = (IMenuEntryList) binder;
        menuGroups.add(entryGroup);
    }

    public List<IMenuEntry> getEntries() {
        if (isLoadComplete()) {
            return menuEntries;
        }
        else {
            throw new RuntimeException("Entries still not ready");
        }
    }

    @Override
    protected void onLoadComplete() {
        for (IMenuEntryList menuGroup: menuGroups) {
            for (IMenuEntry entry: menuGroup.getEntries()) {
                menuEntries.add(entry);
            }
        }
        Collections.sort(menuEntries, new Comparator<IMenuEntry>() {
            @Override
            public int compare(IMenuEntry t0, IMenuEntry t1) {
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

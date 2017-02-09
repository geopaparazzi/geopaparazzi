package eu.geopaparazzi.library.plugin.types;

import android.os.Binder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by cesar on 8/02/17.
 */

public class MenuEntryList extends Binder implements IMenuEntryList {
    private ArrayList<IMenuEntry> list = new ArrayList<IMenuEntry>();

    public void addEntry(IMenuEntry entry) {
        list.add(entry);
    }

    @Override
    public List<IMenuEntry> getEntries() {
        return list;
    }

    @Override
    public boolean shouldReplaceDefaults() {
        return false;
    }
}

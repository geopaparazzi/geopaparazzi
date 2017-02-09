package eu.geopaparazzi.library.plugin.types;

/**
 * Created by cesar on 8/02/17.
 */

import java.util.List;

public interface IMenuEntryList {

    List<IMenuEntry> getEntries();

    boolean shouldReplaceDefaults();
}

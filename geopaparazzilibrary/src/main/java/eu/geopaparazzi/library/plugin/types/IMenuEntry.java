package eu.geopaparazzi.library.plugin.types;


import android.content.Intent;

import eu.geopaparazzi.library.util.IActivitySupporter;

/**
 * MenuEntry extension type. An extension that returns a label and icon
 *
 * @author Cesar Martinez Izquierdo  (www.scolab.es)
 */
public interface IMenuEntry {
    /**
     * Returns the text to show in the menu entry
     */
    String getLabel();

    /**
     * Returns the icon
     */
    byte[] getIcon();

    /**
     * This is invoked when the entry is clicked, before the activity specified
     * by the action is started. The activity execution can be cancelled if this
     * method returns false
     */
    void onClick(IActivitySupporter clickActivityStarter);

    /**
     * Gets the order in which the entry should be placed. The application installing the
     * menu entries are free to use or ignore this proposed order.
     * Zero will be the top-most menu entry and 500 is the default value.
     *
     * @return An integer number >= 0, where 0 means the top most
     * item. Negative numbers means no particular order.
     */
    int getOrder();

    /**
     * Setter for a request code to be used.
     *
     * @param requestCode the code.
     */
    void setRequestCode(int requestCode);

    /**
     * If an activity result is necessary, this can be launched on it.
     */
    void onActivityResultExecute(int requestCode, int resultCode, Intent data);

}
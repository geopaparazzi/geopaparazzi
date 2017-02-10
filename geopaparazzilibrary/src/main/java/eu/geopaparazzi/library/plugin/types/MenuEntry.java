package eu.geopaparazzi.library.plugin.types;

import android.content.Context;

import eu.geopaparazzi.library.plugin.ActivityStarter;
import eu.geopaparazzi.library.util.IActivityStarter;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class MenuEntry implements IMenuEntry {
    private String label = null;
    private byte[] icon = null;
    private String action = null;
    private IActivityStarter starter;

    public MenuEntry() {
        starter = new ActivityStarter();
    }

    public MenuEntry(IActivityStarter starter) {
        this.starter = starter;
    }

    @Override
    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public byte[] getIcon() {
        return this.icon;
    }

    public void setIcon(byte[] icon) {
        this.icon = icon;
    }

    @Override
    public String getAction() {
        return this.action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    @Override
    public void onClick(Context context) {
        if (processOnClick(context)) {
            this.starter.start(context, getAction());
        }
    }

    /**
     * This method is invoked when the entry is clicked, before the activity specified
     * by the action is started. The activity execution can be cancelled if this
     * method returns false
     *
     * @return false to cancel the execution of the action, true otherwise
     */
    protected boolean processOnClick(Context context) {
        return true;
    }

    @Override
    public int getOrder() {
        return 500;
    }
}

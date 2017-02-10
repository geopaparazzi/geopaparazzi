package eu.geopaparazzi.library.plugin.types;

import android.content.Intent;

import eu.geopaparazzi.library.util.IActivitySupporter;

/**
 * @author Cesar Martinez Izquierdo (www.scolab.es)
 */
public class MenuEntry implements IMenuEntry {
    private String label = null;
    private byte[] icon = null;
    protected int requestCode = -1;

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
    public void onClick(IActivitySupporter clickActivityStarter) {
        if (processOnClick()) {
            start();
        }
    }

    /**
     * The start method called if the processOnCLick returns true.
     */
    protected void start(){};

    /**
     * This method is invoked when the entry is clicked, before the activity specified
     * by the action is started. The activity execution can be cancelled if this
     * method returns false
     *
     * @return false to cancel the execution of the action, true otherwise
     */
    protected boolean processOnClick() {
        return true;
    }

    @Override
    public int getOrder() {
        return 500;
    }

    public void onActivityResultExecute(int requestCode, int resultCode, Intent data){};

    public void setRequestCode(int requestCode){
        this.requestCode = requestCode;
    }

}

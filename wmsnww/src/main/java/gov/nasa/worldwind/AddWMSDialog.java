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
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerStyle;
import gov.nasa.worldwind.util.WWUtil;

import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.support.v4.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;

/**
 * @author Nicola Dorigatti
 */
public class AddWMSDialog extends DialogFragment {
    private static final String TAG = "AddWMSDialog";
    public static final String WMSURL_KEY = "WMSURL_KEY";
    private Thread downloadThread;
    LayerInfoAdapter mListViewAdapter = null;
    private ListView mListView;

    private OnWMSLayersAddedListener wmsLayersAddedListener;
    private String baseUrl;
    private String forcedWmsVersion;

    public interface OnWMSLayersAddedListener {
        void onWMSLayersAdded(String baseUrl, String forcedWmsVersion, List<LayerInfo> layersToAdd);
    }


    /**
     * Create a dialog instance with the necessary wms url.
     *
     * @param wmsurl the wms url to use.
     * @return the instance.
     */
    public static AddWMSDialog newInstance(String wmsurl) {
        AddWMSDialog f = new AddWMSDialog();
        if (wmsurl != null) {
            Bundle args = new Bundle();
            args.putString(WMSURL_KEY, wmsurl);
            f.setArguments(args);
        }
        return f;
    }

    // ------------LAYER INFO UTILS
    protected final TreeSet<LayerInfo> layerInfos = new TreeSet<LayerInfo>(new Comparator<LayerInfo>() {
        public int compare(LayerInfo infoA, LayerInfo infoB) {
            String nameA = infoA.getName();
            String nameB = infoB.getName();
            return nameA.compareTo(nameB);
        }
    });

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Bundle arguments = getArguments();

        String defaultWMSURL = "";
        if (arguments != null)
            defaultWMSURL = arguments.getString(WMSURL_KEY);

        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_wms_add, null);

        // Lookup views
        mListView = (ListView) view.findViewById(R.id.wms_layerslistview);
        mListViewAdapter = new LayerInfoAdapter(getActivity(), layerInfos.toArray(new LayerInfo[layerInfos.size()]));
        mListView.setAdapter(mListViewAdapter);

        final LinearLayout mainLayout = (LinearLayout) view.findViewById(R.id.main_add_wms_layout);
        mainLayout.setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);
        final EditText urlEditText = (EditText) view.findViewById(R.id.wms_url_et);
        urlEditText.setText(defaultWMSURL);
        final Button getCapabilitiesButton = (Button) view.findViewById(R.id.get_capabilities_btn);
        getCapabilitiesButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(urlEditText.getWindowToken(), 0);
                final String WMSURLtoUSE = urlEditText.getText().toString();
                // TODO Should Check url validity before starting to download WMS capabilities
                if (downloadThread == null) {
                    getCapabilitiesButton.setEnabled(false);
                    downloadThread = new Thread(new Runnable() {

                        @Override
                        public void run() {
                            downloadCapabilities(WMSURLtoUSE);
                            downloadThread = null;
                            AddWMSDialog.this.getActivity().runOnUiThread(new Runnable() {

                                @Override
                                public void run() {
                                    // update listVIew
                                    updateLayerInfoList(getActivity());
                                    getCapabilitiesButton.setEnabled(true);
                                }
                            });
                        }
                    });
                    downloadThread.start();
                }

            }
        });

        builder.setView(view);
        builder.setPositiveButton(getString(android.R.string.ok), new OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                ArrayList<LayerInfo> retval = new ArrayList<LayerInfo>();
                LayerInfo[] infos = mListViewAdapter.getLayerInfos();
                for (LayerInfo info : infos) {
                    if (info.selected) {
                        retval.add(info);
                    }
                }
                if (null != wmsLayersAddedListener) {
                    wmsLayersAddedListener.onWMSLayersAdded(baseUrl, forcedWmsVersion, retval);
                }
            }
        });
        // Create the AlertDialog object and return it
        return builder.create();
    }

    /*
     * (non-Javadoc)
     * @see android.app.Fragment#onViewCreated(android.view.View, android.os.Bundle)
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    public void setOnAddWMSLayersListener(OnWMSLayersAddedListener listener) {
        wmsLayersAddedListener = listener;
    }

    private void downloadCapabilities(String wmsURL) {
        try {
            layerInfos.clear();
            URI serverURI = new URI(wmsURL.trim());
            WMSCapabilities caps = WMSCapabilities.retrieve(serverURI);
            caps.parse();
            final List<WMSLayerCapabilities> namedLayerCaps = caps.getNamedLayers();
            if (namedLayerCaps == null)
                return;
            for (WMSLayerCapabilities lc : namedLayerCaps) {
                Set<WMSLayerStyle> styles = lc.getStyles();

                if (styles == null || styles.size() == 0) {
                    // Log.d(TAG, "Null or empty styles!");
                    LayerInfo layerInfo = createLayerInfo(caps, lc, null);
                    layerInfos.add(layerInfo);
                } else {
                    // Log.d(TAG, "Styles is not null and not empty");
                    for (WMSLayerStyle style : styles) {
                        LayerInfo layerInfo = createLayerInfo(caps, lc, style);
                        layerInfos.add(layerInfo);
                    }
                }
            }

            String[] urlSplit = wmsURL.split("\\?");
            if (urlSplit.length == 2) {
                baseUrl = urlSplit[0];
                String[] paramsSplit = urlSplit[1].split("&");
                for (String param : paramsSplit) {
                    if (param.toLowerCase().startsWith("version=")) {
                        forcedWmsVersion = param.split("=")[1];
                        break;
                    }
                }

            }

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private void updateLayerInfoList(Context context) {
        mListViewAdapter = new LayerInfoAdapter(getActivity(), layerInfos.toArray(new LayerInfo[layerInfos.size()]));
        mListView.setAdapter(mListViewAdapter);
        // mListViewAdapter.setLayerInfos(layerInfos.toArray(new LayerInfo[layerInfos.size()]));
        mListViewAdapter.notifyDataSetChanged();
        Log.i(TAG, "Updated listview");
    }


    protected LayerInfo createLayerInfo(WMSCapabilities caps, WMSLayerCapabilities layerCaps, WMSLayerStyle style) {
        // Create the layer info specified by the layer's capabilities entry and
        // the selected style.

        LayerInfo linfo = new LayerInfo();
        linfo.caps = caps;
        linfo.params = new AVListImpl();
        linfo.params.setValue(AVKey.LAYER_NAMES, layerCaps.getName());
        if (style != null)
            linfo.params.setValue(AVKey.STYLE_NAMES, style.getName());
        String abs = layerCaps.getLayerAbstract();
        if (!WWUtil.isEmpty(abs))
            linfo.params.setValue(AVKey.LAYER_ABSTRACT, abs);

        linfo.params.setValue(AVKey.DISPLAY_NAME, makeTitle(caps, linfo));

        return linfo;
    }

    protected static String makeTitle(WMSCapabilities caps, LayerInfo layerInfo) {
        String layerNames = layerInfo.params.getStringValue(AVKey.LAYER_NAMES);
        String styleNames = layerInfo.params.getStringValue(AVKey.STYLE_NAMES);
        String[] lNames = layerNames.split(",");
        String[] sNames = styleNames != null ? styleNames.split(",") : null;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lNames.length; i++) {
            if (sb.length() > 0)
                sb.append(", ");

            String layerName = lNames[i];
            WMSLayerCapabilities lc = caps.getLayerByName(layerName);
            String layerTitle = lc.getTitle();
            sb.append(layerTitle != null ? layerTitle : layerName);

            if (sNames == null || sNames.length <= i)
                continue;

            String styleName = sNames[i];
            WMSLayerStyle style = lc.getStyleByName(styleName);
            if (style == null)
                continue;

            sb.append(" : ");
            String styleTitle = style.getTitle();
            sb.append(styleTitle != null ? styleTitle : styleName);
        }

        return sb.toString();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof OnWMSLayersAddedListener) {
            wmsLayersAddedListener = (OnWMSLayersAddedListener) activity;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        wmsLayersAddedListener = null;
    }

    public static class LayerInfo {
        public WMSCapabilities caps;
        public AVListImpl params = new AVListImpl();
        public boolean selected = false;

        public String getTitle() {
            return params.getStringValue(AVKey.DISPLAY_NAME);
        }

        public String getName() {
            return params.getStringValue(AVKey.LAYER_NAMES);
        }

        public String getAbstract() {
            return params.getStringValue(AVKey.LAYER_ABSTRACT);
        }
    }

    private class LayerInfoAdapter extends ArrayAdapter<LayerInfo> {

        private LayerInfo[] types;

        public LayerInfoAdapter(Activity context, LayerInfo[] types) {
            super(context, android.R.layout.simple_list_item_1, types);
            this.types = types;
        }

        public LayerInfo[] getLayerInfos() {
            return types;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final LayerInfo layerType = types[position];
            View retval = convertView;
            if (retval == null) {
                retval = new CheckBox(getContext());
            }
            ((CheckBox) retval).setText(layerType.getTitle());
            ((CheckBox) retval).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    layerType.selected = buttonView.isChecked();
                }
            });
            ((CheckBox) retval).setChecked(layerType.selected);
            return retval;
        }
    }
}

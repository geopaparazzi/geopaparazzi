package eu.geopaparazzi.plugins.defaultimports;

import android.content.Context;
import android.widget.Button;

import java.io.File;
import java.util.List;
import java.util.Set;

import eu.geopaparazzi.core.ui.activities.ImportActivity;
import eu.geopaparazzi.library.core.ResourcesManager;
import eu.geopaparazzi.library.network.NetworkUtilities;
import eu.geopaparazzi.library.plugin.types.MenuEntry;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivityStupporter;
import eu.geopaparazzi.mapsforge.BaseMapSourcesManager;
import gov.nasa.worldwind.AddWMSDialog;
import gov.nasa.worldwind.ogc.OGCBoundingBox;
import gov.nasa.worldwind.ogc.wms.WMSCapabilityInformation;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;

public class ImportWmsMenuEntry extends MenuEntry implements AddWMSDialog.OnWMSLayersAddedListener {

    private final Context serviceContext;
    private IActivityStupporter clickActivityStarter;

    public ImportWmsMenuEntry(Context context) {
        this.serviceContext = context;
    }

    @Override
    public String getLabel() {
        return "WMS";//serviceContext.getString(eu.geopaparazzi.core.R.string.gpx);
    }

    @Override
    public void onClick(IActivityStupporter clickActivityStarter) {
        this.clickActivityStarter = clickActivityStarter;
        Context context = clickActivityStarter.getContext();
        if (!NetworkUtilities.isNetworkAvailable(context)) {
            GPDialogs.infoDialog(context, context.getString(eu.geopaparazzi.core.R.string.available_only_with_network), null);
            return;
        }
        AddWMSDialog addWMSDialog = AddWMSDialog.newInstance(null);

        addWMSDialog.setOnAddWMSLayersListener(this);

        addWMSDialog.show(clickActivityStarter.getSupportFragmentManager(), "wms import");
    }

    @Override
    public void onWMSLayersAdded(String baseurl, String forcedWmsVersion, List<AddWMSDialog.LayerInfo> layersToAdd) {
        for (AddWMSDialog.LayerInfo li : layersToAdd) {
            String layerName = li.getName();

            StringBuilder sb = new StringBuilder();
            String wmsversion = "1.1.1";

            if (forcedWmsVersion != null) {
                wmsversion = forcedWmsVersion;
            } else if (li.caps.getVersion() != null) {
                wmsversion = li.caps.getVersion();
            }
            WMSCapabilityInformation capabilityInformation = li.caps.getCapabilityInformation();
            //            for (String imageFormat : capabilityInformation.getImageFormats()) {
            //                if (imageFormat.toLowerCase().endsWith("png") || imageFormat.toLowerCase().endsWith("jpeg"))
            //                    sb.append("format=").append(imageFormat).append("\n");
            //                break;
            //            }


            List<WMSLayerCapabilities> layerCapabilities = capabilityInformation.getLayerCapabilities();


            for (WMSLayerCapabilities layerCapability : layerCapabilities) {
                String srs = null;
                Set<String> crsList = layerCapability.getCRS();
                if (crsList.size() == 0) {
                    crsList = layerCapability.getSRS();
                }
                for (String crs : crsList) {
                    if (crs.equals("CRS:84") || crs.equals("EPSG:4326")) {
                        srs = crs;

                        boolean doLonLat = false;
                        if (crs.equals("CRS:84")) {
                            doLonLat = true;
                        } else if (crs.equals("EPSG:4326") && !wmsversion.equals("1.3.0")) {
                            doLonLat = true;
                        }

                        String bboxStr;
                        if (doLonLat) {
                            bboxStr = "XXX,YYY,XXX,YYY";
                        } else {
                            bboxStr = "YYY,XXX,YYY,XXX";
                        }
                        String srsLabel = "SRS";
                        if (wmsversion.equals("1.3.0")) {
                            srsLabel = "CRS";
                        }
                        sb.append("url=" + baseurl.trim() + "?REQUEST=GetMap&SERVICE=WMS&VERSION=" + wmsversion //
                                + "&LAYERS=" + layerName + "&STYLES=&FORMAT=image/png&BGCOLOR=0xFFFFFF&TRANSPARENT=TRUE&" + srsLabel + "=" //
                                + srs + "&BBOX=" + bboxStr + "&WIDTH=256&HEIGHT=256\n");
                        sb.append("minzoom=1\n");
                        sb.append("maxzoom=22\n");
                        sb.append("defaultzoom=17\n");
                        sb.append("format=png\n");
                        sb.append("type=wms\n");
                        sb.append("description=").append(layerName).append("\n");


                        break;
                    }
                }

                if (srs == null) {
                    // TODO
                    return;
                }

                for (OGCBoundingBox bbox : layerCapability.getBoundingBoxes()) {
                    String crs = bbox.getCRS();
                    if (crs.equals("CRS:84") || crs.equals("EPSG:4326")) {
                        double centerX = bbox.getMinx() + (bbox.getMaxx() - bbox.getMinx()) / 2.0;
                        double centerY = bbox.getMiny() + (bbox.getMaxy() - bbox.getMiny()) / 2.0;
                        sb.append("center=");
                        sb.append(centerX).append(" ").append(centerY);
                        sb.append("\n");

                    }
                }

            }

            try {
                Context context = clickActivityStarter.getContext();
                File applicationSupporterDir = ResourcesManager.getInstance(context).getApplicationSupporterDir();
                File newMapurl = new File(applicationSupporterDir, layerName + ".mapurl");

                sb.append("mbtiles=defaulttiles/_" + newMapurl.getName() + ".mbtiles\n");

                String mapurlText = sb.toString();
                FileUtilities.writefile(mapurlText, newMapurl);


                BaseMapSourcesManager.INSTANCE.addBaseMapsFromFile(newMapurl);
                GPDialogs.infoDialog(context, context.getString(eu.geopaparazzi.core.R.string.wms_mapurl_added) + newMapurl.getName(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }

            break;
        }

    }
}

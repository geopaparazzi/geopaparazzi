package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.adapter;

import java.util.Set;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import eu.geopaparazzi.mapsforge.R;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.MapsDirTreeViewList;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.TreeNodeInfo;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.TreeStateManager;

final public class MapTypeTreeViewAdapter extends FileDirectoryTreeViewAdapter {
    public MapTypeTreeViewAdapter( final MapsDirTreeViewList activity, final Set<Long> selected_nodes,
            final TreeStateManager<Long> treeStateManager, final int numberOfLevels ) {
        super(activity, selected_nodes, treeStateManager, numberOfLevels);
    }

    @Override
    public LinearLayout updateView( final View view, final TreeNodeInfo<Long> treeNodeInfo ) {
        final LinearLayout viewLayout = (LinearLayout) view;
        final TextView descriptionView = (TextView) viewLayout.findViewById(R.id.mapsdir_treeview_item_fields_short_text);
        final TextView typeView = (TextView) viewLayout.findViewById(R.id.mapsdir_treeview_item_fields_type);
        descriptionView.setText(treeNodeInfo.getFileName());
        if (treeNodeInfo.isWithChildren()) {
            typeView.setText("");
        } else {
            typeView.setText("[" + treeNodeInfo.getTypeDescriptionText() + "]");
        }
        return viewLayout;
    }

}

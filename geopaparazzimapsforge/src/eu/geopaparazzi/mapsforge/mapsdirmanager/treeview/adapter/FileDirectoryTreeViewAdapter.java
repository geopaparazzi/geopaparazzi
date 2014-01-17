package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.adapter;

import java.util.Set;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import eu.geopaparazzi.mapsforge.R;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.MapsDirTreeViewList;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.TreeNodeInfo;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.TreeStateManager;

/**
 * This is a very simple adapter that provides very basic tree view with a
 * checkboxes and simple item description.
 */
public class FileDirectoryTreeViewAdapter extends AbstractTreeViewAdapter<Long> {

    public FileDirectoryTreeViewAdapter( final MapsDirTreeViewList treeViewListDemo, final Set<Long> selected_nodes,
            final TreeStateManager<Long> treeStateManager, final int numberOfLevels ) {
        super(treeViewListDemo, treeStateManager, numberOfLevels);
        selected_nodes.toString(); // id this param needed
    }

    @Override
    public View getNewChildView( final TreeNodeInfo<Long> node_info ) {
        final LinearLayout viewLayout = (LinearLayout) getActivity().getLayoutInflater().inflate(
                R.layout.mapsdir_treeview_item_fields, null);
        return updateView(viewLayout, node_info);
    }

    @Override
    public LinearLayout updateView( final View view, final TreeNodeInfo<Long> node_info ) {
        final LinearLayout viewLayout = (LinearLayout) view;
        final TextView descriptionView = (TextView) viewLayout.findViewById(R.id.mapsdir_treeview_item_fields_short_text);
        final TextView typeView = (TextView) viewLayout.findViewById(R.id.mapsdir_treeview_item_fields_type);
        descriptionView.setText(node_info.getShortText());
        if (node_info.isWithChildren()) {
            typeView.setText("");
        } else {
            typeView.setText("[" + node_info.getTypeText() + "]");
        }
        return viewLayout;
    }

    @Override
    public void handleItemClick( final View view, final Object id ) {
        final Long longId = (Long) id;
        final TreeNodeInfo<Long> node_info = getManager().getNodeInfo(longId);
        if (node_info.isWithChildren()) {
            super.handleItemClick(view, id);
        }
    }

    @Override
    public long getItemId( final int position ) {
        return getTreeId(position);
    }
}

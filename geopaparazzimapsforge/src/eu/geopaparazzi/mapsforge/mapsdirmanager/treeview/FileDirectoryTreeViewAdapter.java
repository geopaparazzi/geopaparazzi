package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;

import java.util.Set;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import eu.geopaparazzi.mapsforge.R;

/**
 * This is a very simple adapter that provides very basic tree view with a
 * checkboxes and simple item description.
 *
 */
class FileDirectoryTreeViewAdapter extends AbstractTreeViewAdapter<Long> {
    // private final Set<Long> selected_nodes;
    // private final OnCheckedChangeListener onCheckedChange = new OnCheckedChangeListener(){
    // @Override
    // public void onCheckedChanged( final CompoundButton buttonView, final boolean isChecked ) {
    // final Long id = (Long) buttonView.getTag();
    // changeSelected(isChecked, id);
    // }
    // };

    // private void changeSelected( final boolean isChecked, final Long id ) {
    // if (isChecked) {
    // selected_nodes.add(id);
    // } else {
    // selected_nodes.remove(id);
    // }
    // }

    public FileDirectoryTreeViewAdapter( final MapsDirTreeViewList treeViewListDemo, final Set<Long> selected_nodes,
            final TreeStateManager<Long> treeStateManager, final int numberOfLevels ) {
        super(treeViewListDemo, treeStateManager, numberOfLevels);
        // this.selected_nodes = selected_nodes;
        selected_nodes.toString(); // id this param needed
    }

    // private String getDescription(final long id)
    // {
    // final Integer[] hierarchy = getManager().getHierarchyDescription(id);
    // return "Node " + id + Arrays.asList(hierarchy);
    // }

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
        // descriptionView.setText(getDescription(treeNodeInfo.getId()));
        // typeView.setText(Integer.toString(treeNodeInfo.getLevel()));
        descriptionView.setText(node_info.getShortText());
        if (node_info.isWithChildren()) {
            typeView.setText("");
        } else {
            typeView.setText("[" + node_info.getTypeText() + "]");
        }
        // ImageView this_view = (ImageView)
        // viewLayout.findViewById(R.id.mapsdir_treeview_item_fields_options_button);
        return viewLayout;
    }

    @Override
    public void handleItemClick( final View view, final Object id ) {
        final Long longId = (Long) id;
        final TreeNodeInfo<Long> node_info = getManager().getNodeInfo(longId);
        if (node_info.isWithChildren()) {
            super.handleItemClick(view, id);
        } else {
            // final ViewGroup vg = (ViewGroup) view;
            /*
            final CheckBox cb = (CheckBox) vg.findViewById(R.id.mapsdir_treeview_item_fields_checkbox);
            cb.performClick();
            */
        }
    }

    @Override
    public long getItemId( final int position ) {
        return getTreeId(position);
    }
}

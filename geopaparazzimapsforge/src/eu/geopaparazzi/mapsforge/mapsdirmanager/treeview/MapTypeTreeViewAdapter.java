package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;

import java.util.Set;

import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import eu.geopaparazzi.mapsforge.R;

final class MapTypeTreeViewAdapter extends FileDirectoryTreeViewAdapter {
    public MapTypeTreeViewAdapter( final MapsDirTreeViewList activity, final Set<Long> selected_nodes,
            final TreeStateManager<Long> treeStateManager, final int numberOfLevels ) {
        super(activity, selected_nodes, treeStateManager, numberOfLevels);
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
        /*
        final LinearLayout viewLayout = super.updateView(view,node_info);
        final TextView descriptionView = (TextView) viewLayout.findViewById(R.id.mapsdir_treeview_item_fields_short_text);
        final TextView typeView = (TextView) viewLayout.findViewById(R.id.mapsdir_treeview_item_fields_type);
        descriptionView.setTextSize(20 - 2 * node_info.getLevel());
        typeView.setTextSize(20 - 2 * node_info.getLevel());
        return viewLayout;
        * */
    }
    /*
     @Override
     public Drawable getBackgroundDrawable(final TreeNodeInfo<Long> node_info)
     {
      switch (node_info.getLevel())
      {
       case 0:
        return new ColorDrawable(Color.WHITE);
       case 1:
        return new ColorDrawable(Color.GRAY);
       case 2:
        return new ColorDrawable(Color.YELLOW);
       default:
        return null;
      }
     }
     * */
}

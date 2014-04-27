package eu.geopaparazzi.mapsforge.mapsdirmanager.sourcesview;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ExpandableListView;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.mapsforge.R;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;

public class SourcesTreeListActivity extends Activity {

    SourcesExpandableListAdapter listAdapter;
    ExpandableListView expListView;

    @Override
    protected void onCreate( Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sources_list);

        // get the listview
        expListView = (ExpandableListView) findViewById(R.id.expandableSourceListView);

        try {
            getData();
        } catch (Exception e) {
            GPLog.error("SourcesTreeListActivity", "Problem getting sources.", e);
        }
    }

    private void getData() throws Exception {
        final LinkedHashMap<String, List<String[]>> fodler2TablesMap = MapsDirManager.getInstance().getFodler2TablesMap();
        listAdapter = new SourcesExpandableListAdapter(this, fodler2TablesMap);
        expListView.setAdapter(listAdapter);
        expListView.setClickable(true);
        expListView.setFocusable(true);
        expListView.setFocusableInTouchMode(true);
        expListView.setOnChildClickListener(new ExpandableListView.OnChildClickListener(){
            public boolean onChildClick( ExpandableListView parent, View v, int groupPosition, int childPosition, long id ) {
                int index = 0;
                String[] spatialTableDate = null;
                for( Entry<String, List<String[]>> entry : fodler2TablesMap.entrySet() ) {
                    if (groupPosition == index) {
                        List<String[]> value = entry.getValue();
                        spatialTableDate = value.get(childPosition);
                        break;
                    }
                    index++;
                }
                MapsDirManager.getInstance().setSelectedSpatialTable(SourcesTreeListActivity.this, spatialTableDate);
                finish();
                return false;
            }
        });
        int groupCount = listAdapter.getGroupCount();
        for( int i = 0; i < groupCount; i++ ) {
            expListView.expandGroup(i);
        }
    }
}
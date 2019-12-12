package eu.geopaparazzi.map.gui;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.woxthebox.draglistview.BoardView;
import com.woxthebox.draglistview.DragItem;
import com.woxthebox.draglistview.DragItemAdapter;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.ISpatialTableNames;
import org.hortonmachine.dbs.datatypes.EDataType;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.TileEntry;
import org.hortonmachine.dbs.geopackage.android.GPGeopackageDb;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.library.util.AppsUtilities;
import eu.geopaparazzi.library.util.FileUtilities;
import eu.geopaparazzi.library.util.GPDialogs;
import eu.geopaparazzi.library.util.IActivitySupporter;
import eu.geopaparazzi.library.util.LibraryConstants;
import eu.geopaparazzi.library.util.Utilities;
import eu.geopaparazzi.library.util.types.ESpatialDataSources;
import eu.geopaparazzi.map.R;
import eu.geopaparazzi.map.layers.ELayerTypes;
import eu.geopaparazzi.map.layers.LayerManager;
import eu.geopaparazzi.map.layers.interfaces.IGpLayer;
import eu.geopaparazzi.map.layers.utils.EOnlineTileSources;

public class MapLayerListFragment extends Fragment implements IActivitySupporter, View.OnClickListener {

    public static final int PICKFILE_REQUEST_CODE = 666;
    public static final int PICKFOLDER_REQUEST_CODE = 667;

    private static int sCreatedItems = 0;
    private BoardView mBoardView;

    private boolean isFabOpen = false;
    private FloatingActionButton toggleButton, addSourceButton, addSourceFolderButton, addDefaultTileSourcesButton;
    private Animation fab_open, fab_close, rotate_forward, rotate_backward;

    public static MapLayerListFragment newInstance() {
        return new MapLayerListFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.board_layout, container, false);

        mBoardView = view.findViewById(R.id.board_view);
        mBoardView.setSnapToColumnsWhenScrolling(true);
        mBoardView.setSnapToColumnWhenDragging(true);
        mBoardView.setSnapDragItemToTouch(true);
        mBoardView.setCustomDragItem(new MyDragItem(getActivity(), R.layout.column_item));
        mBoardView.setCustomColumnDragItem(new MyColumnDragItem(getActivity(), R.layout.column_drag_layout));
        mBoardView.setSnapToColumnInLandscape(false);
        mBoardView.setColumnSnapPosition(BoardView.ColumnSnapPosition.CENTER);
        mBoardView.setBoardListener(new BoardView.BoardListener() {
            @Override
            public void onItemDragStarted(int column, int row) {
                //Toast.makeText(getContext(), "Start - column: " + column + " row: " + row, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onItemDragEnded(int fromColumn, int fromRow, int toColumn, int toRow) {
                if (fromRow != toRow) {
                    LayerManager.INSTANCE.changeLayerPosition(fromColumn == 1, fromRow, toRow);
                }
            }

            @Override
            public void onItemChangedPosition(int oldColumn, int oldRow, int newColumn, int newRow) {

            }

            @Override
            public void onItemChangedColumn(int oldColumn, int newColumn) {
                TextView itemCount1 = mBoardView.getHeaderView(oldColumn).findViewById(R.id.item_count);
                itemCount1.setText(String.valueOf(mBoardView.getAdapter(oldColumn).getItemCount()));
                TextView itemCount2 = mBoardView.getHeaderView(newColumn).findViewById(R.id.item_count);
                itemCount2.setText(String.valueOf(mBoardView.getAdapter(newColumn).getItemCount()));
            }

            @Override
            public void onFocusedColumnChanged(int oldColumn, int newColumn) {
                if (newColumn == 1) {
                    addSourceButton.hide();
                    addSourceFolderButton.hide();
                    addDefaultTileSourcesButton.hide();
                    toggleButton.hide();
                } else {
                    addSourceButton.show();
                    addSourceFolderButton.show();
                    addDefaultTileSourcesButton.show();
                    toggleButton.show();
                }
                //Toast.makeText(getContext(), "Focused column changed from " + oldColumn + " to " + newColumn, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onColumnDragStarted(int position) {
                //Toast.makeText(getContext(), "Column drag started from " + position, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onColumnDragChangedPosition(int oldPosition, int newPosition) {
                //Toast.makeText(getContext(), "Column changed from " + oldPosition + " to " + newPosition, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onColumnDragEnded(int position) {
                //Toast.makeText(getContext(), "Column drag ended at " + position, Toast.LENGTH_SHORT).show();
            }
        });
        mBoardView.setBoardCallback(new BoardView.BoardCallback() {
            @Override
            public boolean canDragItemAtPosition(int column, int dragPosition) {
                // Add logic here to prevent an item to be dragged
                return true;
            }

            @Override
            public boolean canDropItemAtPosition(int oldColumn, int oldRow, int newColumn, int newRow) {
                // we drag and drop only on same column
                return oldColumn == newColumn;
            }
        });


        toggleButton = view.findViewById(R.id.mapToggleButton);
        toggleButton.setOnClickListener(this);
        addSourceButton = view.findViewById(R.id.addMapSourceButton);
        addSourceButton.setOnClickListener(this);
        addSourceFolderButton = view.findViewById(R.id.addMapSourceFolderButton);
        addSourceFolderButton.setOnClickListener(this);
        addDefaultTileSourcesButton = view.findViewById(R.id.addDefaultTileSourcesButton);
        addDefaultTileSourcesButton.setOnClickListener(this);

        fab_open = AnimationUtils.loadAnimation(getContext(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getContext(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getContext(), R.anim.rotate_backward);

        return view;
    }


    @Override
    public void onClick(View v) {
        if (v == toggleButton) {
            animateFAB();
        } else if (v == addSourceButton) {
            addMap();
        } else if (v == addSourceFolderButton) {
            addMapFolder();
        } else if (v == addDefaultTileSourcesButton) {
            addDefaultTileSource();
        }
    }

    public void animateFAB() {
        if (isFabOpen) {
            toggleButton.startAnimation(rotate_backward);
            addSourceButton.startAnimation(fab_close);
            addSourceFolderButton.startAnimation(fab_close);
            addDefaultTileSourcesButton.startAnimation(fab_close);
            addSourceButton.setClickable(false);
            addSourceFolderButton.setClickable(false);
            addDefaultTileSourcesButton.setClickable(false);
            isFabOpen = false;
        } else {
            toggleButton.startAnimation(rotate_forward);
            addSourceButton.startAnimation(fab_open);
            addSourceFolderButton.startAnimation(fab_open);
            addDefaultTileSourcesButton.startAnimation(fab_open);
            addSourceButton.setClickable(true);
            addSourceFolderButton.setClickable(true);
            addDefaultTileSourcesButton.setClickable(true);
            isFabOpen = true;
        }
    }

    public void addMap() {
        try {
            String title = getString(R.string.add_map);
            String[] supportedExtensions = ESpatialDataSources.getAllSupportedExtensions();
            AppsUtilities.pickFile(this, PICKFILE_REQUEST_CODE, title, supportedExtensions, null);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(getActivity(), e, null);
        }
    }

    public void addMapFolder() {
        try {
            String title = getString(R.string.add_maps_folder);
            String[] supportedExtensions = ESpatialDataSources.getAllSupportedExtensions();
            AppsUtilities.pickFolder(this, PICKFOLDER_REQUEST_CODE, title, null, supportedExtensions);
        } catch (Exception e) {
            GPLog.error(this, null, e);
            GPDialogs.errorDialog(getActivity(), e, null);
        }
    }

    private void addDefaultTileSource() {
        List<String> namesList = EOnlineTileSources.getNames();
        String[] names = namesList.toArray(new String[0]);
        boolean[] checked = new boolean[names.length];

        GPDialogs.singleOptionDialog(getActivity(), names, checked, () -> {
            FragmentActivity activity = getActivity();
            if (activity != null)
                activity.runOnUiThread(() -> {
                    for (int i = 0; i < checked.length; i++) {
                        if (checked[i]) {
                            EOnlineTileSources source = EOnlineTileSources.getByName(names[i]);
                            int index = 0;
                            try {
                                index = LayerManager.INSTANCE.addBitmapTileService(source.getName(), source.getUrl(), source.getTilePath(), source.getMaxZoom(), 1f, null);
                                MapLayerItem item = new MapLayerItem();
                                item.position = index;
                                item.name = source.getName();
                                item.url = source.getUrl();
                                item.path = source.getUrl();
                                item.enabled = true;
                                int focusedColumn = mBoardView.getFocusedColumn();
                                int itemCount = mBoardView.getItemCount(focusedColumn);
                                mBoardView.addItem(focusedColumn, itemCount, item, true);
                            } catch (Exception e) {
                                GPDialogs.warningDialog(getActivity(), e.getMessage(), null);
                            }

                        }
                    }
                });
        });

    }

    public void removeItemAtIndex(int col, int row) {
        mBoardView.removeItem(col, row);
    }


    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        ((AppCompatActivity) getActivity()).getSupportActionBar().setTitle("Board");

        try {
            addUserLayersColumn();
            addSystemLayersColumn();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addUserLayersColumn() throws Exception {
        final ArrayList<MapLayerItem> mItemArray = new ArrayList<>();

        List<JSONObject> layerDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
        int index = 0;
        for (JSONObject layerDefinition : layerDefinitions) {
            MapLayerItem layerItem = getMapLayerItem(index, layerDefinition);
            mItemArray.add(layerItem);
            index++;
        }

        final MapLayerAdapter listAdapter = new MapLayerAdapter(this, mItemArray, R.layout.column_item, R.id.item_layout, true);
        final View header = View.inflate(getActivity(), R.layout.column_header, null);
        ((TextView) header.findViewById(R.id.text)).setText(getString(R.string.map_layers));
        ((TextView) header.findViewById(R.id.item_count)).setText("");

        mBoardView.addColumn(listAdapter, header, header, false);
    }

    private void addSystemLayersColumn() throws Exception {
        final ArrayList<MapLayerItem> mItemArray = new ArrayList<>();

        List<JSONObject> layerDefinitions = LayerManager.INSTANCE.getSystemLayersDefinitions();
        int index = 0;
        for (JSONObject layerDefinition : layerDefinitions) {
            MapLayerItem layerItem = getMapLayerItem(index, layerDefinition);
            layerItem.isSystem = true;
            mItemArray.add(layerItem);
            index++;
        }

        final MapLayerAdapter listAdapter = new MapLayerAdapter(this, mItemArray, R.layout.column_item, R.id.item_layout, true);
        final View header = View.inflate(getActivity(), R.layout.column_header, null);
        ((TextView) header.findViewById(R.id.text)).setText(getString(R.string.project_layers));
        ((TextView) header.findViewById(R.id.item_count)).setText("");

        mBoardView.addColumn(listAdapter, header, header, false);
    }


    @NonNull
    private MapLayerItem getMapLayerItem(int index, JSONObject layerDefinition) throws JSONException {
        String name = layerDefinition.getString(IGpLayer.LAYERNAME_TAG);
        String type = layerDefinition.getString(IGpLayer.LAYERTYPE_TAG);
        boolean isEnabled = true;
        if (layerDefinition.has(IGpLayer.LAYERENABLED_TAG)) {
            isEnabled = layerDefinition.getBoolean(IGpLayer.LAYERENABLED_TAG);
        }
        boolean isEditing = false;
        if (layerDefinition.has(IGpLayer.LAYEREDITING_TAG)) {
            isEditing = layerDefinition.getBoolean(IGpLayer.LAYEREDITING_TAG);
        }
        MapLayerItem layerItem = new MapLayerItem();
        layerItem.position = index;
        layerItem.enabled = isEnabled;
        layerItem.isEditing = isEditing;
        if (layerDefinition.has(IGpLayer.LAYERURL_TAG))
            layerItem.url = layerDefinition.getString(IGpLayer.LAYERURL_TAG);
        if (layerDefinition.has(IGpLayer.LAYERPATH_TAG))
            layerItem.path = layerDefinition.getString(IGpLayer.LAYERPATH_TAG);
        layerItem.name = name;
        layerItem.type = type;
        return layerItem;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
//        inflater.inflate(R.menu.menu_board, menu);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
//        menu.findItem(R.id.action_disable_drag).setVisible(mBoardView.isDragEnabled());
//        menu.findItem(R.id.action_enable_drag).setVisible(!mBoardView.isDragEnabled());
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//            case R.id.action_disable_drag:
//                mBoardView.setDragEnabled(false);
//                getActivity().invalidateOptionsMenu();
//                return true;
//            case R.id.action_enable_drag:
//                mBoardView.setDragEnabled(true);
//                getActivity().invalidateOptionsMenu();
//                return true;
//            case R.id.action_add_column:
//                addColumn();
//                return true;
//            case R.id.action_remove_column:
//                mBoardView.removeColumn(0);
//                return true;
//            case R.id.action_clear_board:
//                mBoardView.clearBoard();
//                return true;
//        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public FragmentManager getSupportFragmentManager() {
        return getFragmentManager();
    }


    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case (PICKFILE_REQUEST_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String filePath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                        File file = new File(filePath);
                        if (file.exists()) {
                            int focusedColumn = mBoardView.getFocusedColumn();
                            int itemCount = mBoardView.getItemCount(focusedColumn);
                            Utilities.setLastFilePath(getActivity(), filePath);
                            final File finalFile = file;
                            ELayerTypes layerType = ELayerTypes.fromFileExt(finalFile.getName());
                            switch (layerType) {
                                case MAPSFORGE:
                                case MBTILES:
                                    int index = LayerManager.INSTANCE.addMapFile(finalFile, null);

                                    if (index >= 0) {
                                        MapLayerItem item = new MapLayerItem();
                                        item.type = layerType.getTilesType();
                                        item.position = index;
                                        item.name = FileUtilities.getNameWithoutExtention(finalFile);
                                        item.path = finalFile.getAbsolutePath();
                                        item.enabled = true;

                                        mBoardView.addItem(focusedColumn, itemCount, item, true);
                                    } else {
                                        // reload list to show changes in existing item
                                        DragItemAdapter adapter = mBoardView.getAdapter(focusedColumn);
                                        List<JSONObject> layerDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                                        int i = 0;
                                        List<MapLayerItem> mItemArray = new ArrayList<>();
                                        for (JSONObject layerDefinition : layerDefinitions) {
                                            MapLayerItem layerItem = getMapLayerItem(i, layerDefinition);
                                            mItemArray.add(layerItem);
                                            i++;
                                        }
                                        adapter.setItemList(mItemArray);
                                    }
                                    break;
                                case MAPURL: {
                                    String readfile = FileUtilities.readfile(finalFile);
                                    if (readfile.replace(" ", "").contains("type=wms")) {//NON-NLS
                                        GPDialogs.warningDialog(getActivity(), getString(R.string.wms_unsupported), null);
                                    } else {
                                        int index1 = LayerManager.INSTANCE.addMapurlBitmapTileService(finalFile, null);
                                        if (index1 >= 0) {
                                            MapLayerItem item = new MapLayerItem();
                                            item.type = layerType.getTilesType();
                                            item.position = index1;
                                            item.name = FileUtilities.getNameWithoutExtention(finalFile);
                                            item.path = finalFile.getAbsolutePath();
                                            item.enabled = true;

                                            mBoardView.addItem(focusedColumn, itemCount, item, true);
                                        } else {
                                            // reload list to show changes in existing item
                                            DragItemAdapter adapter = mBoardView.getAdapter(focusedColumn);
                                            List<JSONObject> layerDefinitions = LayerManager.INSTANCE.getUserLayersDefinitions();
                                            int i = 0;
                                            List<MapLayerItem> mItemArray = new ArrayList<>();
                                            for (JSONObject layerDefinition : layerDefinitions) {
                                                MapLayerItem layerItem = getMapLayerItem(i, layerDefinition);
                                                mItemArray.add(layerItem);
                                                i++;
                                            }
                                            adapter.setItemList(mItemArray);
                                        }
                                    }
                                    break;
                                }
                                case SPATIALITE: {
                                    // ask for table
                                    List<String> tableNames = null;
                                    try (ASpatialDb db = EDb.SPATIALITE4ANDROID.getSpatialDb()) {
                                        db.open(finalFile.getAbsolutePath());

                                        HashMap<String, List<String>> tablesMap = db.getTablesMap(true);
                                        List<String> tableNamesTmp = tablesMap.get(ISpatialTableNames.USERDATA);
                                        if (tableNamesTmp != null) {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                                tableNamesTmp.removeIf(tn -> {
                                                    // also remove tables that do not have a numeric primary key
                                                    try {
                                                        GeometryColumn gc = db.getGeometryColumnsForTable(tn);
                                                        if (gc == null)
                                                            return true;
                                                        List<String[]> tableColumns = db.getTableColumns(tn);
                                                        for (String[] tableColumn : tableColumns) {
                                                            if (tableColumn[2].equals("1")) {
                                                                EDataType type = EDataType.getType4Name(tableColumn[1]);
                                                                if (type == EDataType.INTEGER || type == EDataType.LONG) {
                                                                    return false;
                                                                }
                                                            }
                                                        }
                                                        return true;
                                                    } catch (Exception e) {
                                                        return true;
                                                    }
                                                });
                                            } else {
                                                // TODO remove this condition when minsdk gets 24
                                                Iterator<String> iterator = tableNamesTmp.iterator();
                                                while (iterator.hasNext()) {
                                                    String tn = iterator.next();
                                                    GeometryColumn gc = db.getGeometryColumnsForTable(tn);
                                                    if (gc == null) {
                                                        iterator.remove();
                                                        continue;
                                                    }
                                                    boolean toRemove = true;
                                                    List<String[]> tableColumns = db.getTableColumns(tn);
                                                    for (String[] tableColumn : tableColumns) {
                                                        if (tableColumn[2].equals("1")) {
                                                            EDataType type = EDataType.getType4Name(tableColumn[1]);
                                                            if (type == EDataType.INTEGER || type == EDataType.LONG) {
                                                                toRemove = false;
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    if (toRemove) {
                                                        iterator.remove();
                                                    }
                                                }
                                            }
                                            tableNames = tableNamesTmp;
                                        }
                                    }

                                    if (tableNames != null) {
                                        if (tableNames.size() == 1) {
                                            try {
                                                int index2 = LayerManager.INSTANCE.addSpatialiteTable(finalFile.getAbsoluteFile(), tableNames.get(0), null);
                                                int focusedColumn2 = mBoardView.getFocusedColumn();
                                                int itemCount2 = mBoardView.getItemCount(focusedColumn2);
                                                if (index2 >= 0) {
                                                    MapLayerItem item = new MapLayerItem();
                                                    item.type = layerType.getVectorType();
                                                    item.position = index2;
                                                    item.name = tableNames.get(0);
                                                    item.path = finalFile.getAbsolutePath();
                                                    item.enabled = true;
                                                    item.isEditing = false;

                                                    mBoardView.addItem(focusedColumn2, itemCount2, item, true);
                                                }
                                            } catch (Exception e) {
                                                GPLog.error(this, null, e);
                                            }
                                        } else if (tableNames.size() > 0) {
                                            String[] items = new String[tableNames.size()];
                                            boolean[] checkItems = new boolean[tableNames.size()];
                                            for (int i = 0; i < items.length; i++) {
                                                items[i] = tableNames.get(i);
                                            }
                                            GPDialogs.multiOptionDialog(getActivity(), items, checkItems, () -> {
                                                List<String> selTables = new ArrayList<>();
                                                for (int i = 0; i < checkItems.length; i++) {
                                                    if (checkItems[i]) {
                                                        selTables.add(items[i]);
                                                    }
                                                }
                                                if (selTables.size() > 0) {
                                                    for (String selTable : selTables) {
                                                        try {
                                                            int index2 = LayerManager.INSTANCE.addSpatialiteTable(finalFile.getAbsoluteFile(), selTable, null);
                                                            int focusedColumn2 = mBoardView.getFocusedColumn();
                                                            int itemCount2 = mBoardView.getItemCount(focusedColumn2);
                                                            if (index2 >= 0) {
                                                                MapLayerItem item = new MapLayerItem();
                                                                item.type = layerType.getVectorType();
                                                                item.position = index2;
                                                                item.name = selTable;
                                                                item.path = finalFile.getAbsolutePath();
                                                                item.enabled = true;
                                                                item.isEditing = false;

                                                                mBoardView.addItem(focusedColumn2, itemCount2, item, true);
                                                            }
                                                        } catch (Exception e) {
                                                            GPLog.error(this, null, e);
                                                        }
                                                    }

                                                }
                                            });
                                        }
                                    } else {
                                        GPDialogs.warningDialog(getActivity(), getString(R.string.unable_to_find_tables_in_db), null);
                                    }
                                    break;
                                }
                                case GEOPACKAGE: {
                                    // ask for table
                                    List<String> vectorTableNames = new ArrayList<>();
                                    List<String> tilesTableNames = new ArrayList<>();
                                    int ignoredVectorTables = 0;
                                    int ignoredTilesTables = 0;
                                    try (GPGeopackageDb db = new GPGeopackageDb()) {
                                        db.open(finalFile.getAbsolutePath());
                                        db.setForceMobileCompatibility(false); // we need to see what we ignore

                                        List<FeatureEntry> featuresList = db.features();
                                        for (FeatureEntry featureEntry : featuresList) {
                                            Integer srid = featureEntry.getSrid();
                                            if (srid != null && srid != GPGeopackageDb.WGS84LL_SRID) {
                                                ignoredVectorTables++;
                                                // only 4326 layers are supported
                                                continue;
                                            }
                                            String tableName = featureEntry.getTableName();
                                            vectorTableNames.add(tableName);
                                        }

                                        List<TileEntry> tiles = db.tiles();
                                        for (TileEntry tileEntry : tiles) {
                                            Integer srid = tileEntry.getSrid();
                                            if (srid != null && srid != GPGeopackageDb.MERCATOR_SRID) {
                                                ignoredTilesTables++;
                                                // only 3857 layers are supported
                                                continue;
                                            }
                                            String tableName = tileEntry.getTableName();
                                            tilesTableNames.add(tableName);
                                        }


                                    }

                                    if (ignoredVectorTables > 0 || ignoredTilesTables > 0) {
                                        String msg = getContext().getString(R.string.gpkg_ignore_vector_due_to_srid);
                                        GPDialogs.toast(getActivity(), String.format(msg, ignoredVectorTables, ignoredTilesTables), Toast.LENGTH_LONG);
                                    }

                                    List<String> allTables = new ArrayList<>();
                                    allTables.addAll(vectorTableNames);
                                    allTables.addAll(tilesTableNames);

                                    if (allTables.size() > 0) {
                                        if (allTables.size() == 1) {
                                            try {
                                                String tableName = allTables.get(0);
                                                String layerTypeStr = vectorTableNames.contains(tableName) ? layerType.getVectorType() : layerType.getTilesType();

                                                int index2 = LayerManager.INSTANCE.addGeopackageTable(finalFile.getAbsoluteFile(), tableName, null, layerTypeStr);
                                                int focusedColumn2 = mBoardView.getFocusedColumn();
                                                int itemCount2 = mBoardView.getItemCount(focusedColumn2);
                                                if (index2 >= 0) {
                                                    MapLayerItem item = new MapLayerItem();
                                                    item.type = layerTypeStr;
                                                    item.position = index2;
                                                    item.name = tableName;
                                                    item.path = finalFile.getAbsolutePath();
                                                    item.enabled = true;
                                                    item.isEditing = false;

                                                    mBoardView.addItem(focusedColumn2, itemCount2, item, true);
                                                }
                                            } catch (Exception e) {
                                                GPLog.error(this, null, e);
                                            }
                                        } else if (allTables.size() > 1) {
                                            String[] items = new String[allTables.size()];
                                            boolean[] checkItems = new boolean[allTables.size()];
                                            for (int i = 0; i < items.length; i++) {
                                                items[i] = allTables.get(i);
                                            }

                                            GPDialogs.multiOptionDialog(getActivity(), items, checkItems, () -> {
                                                List<String> selTables = new ArrayList<>();
                                                for (int i = 0; i < checkItems.length; i++) {
                                                    if (checkItems[i]) {
                                                        selTables.add(items[i]);
                                                    }
                                                }
                                                if (selTables.size() > 0) {
                                                    for (String selTable : selTables) {
                                                        try {
                                                            String layerTypeStr = vectorTableNames.contains(selTable) ? layerType.getVectorType() : layerType.getTilesType();


                                                            int index2 = LayerManager.INSTANCE.addGeopackageTable(finalFile.getAbsoluteFile(), selTable, null, layerTypeStr);
                                                            int focusedColumn2 = mBoardView.getFocusedColumn();
                                                            int itemCount2 = mBoardView.getItemCount(focusedColumn2);
                                                            if (index2 >= 0) {
                                                                MapLayerItem item = new MapLayerItem();
                                                                item.type = layerTypeStr;
                                                                item.position = index2;
                                                                item.name = selTable;
                                                                item.path = finalFile.getAbsolutePath();
                                                                item.enabled = true;
                                                                item.isEditing = false;

                                                                mBoardView.addItem(focusedColumn2, itemCount2, item, true);
                                                            }
                                                        } catch (Exception e) {
                                                            GPLog.error(this, null, e);
                                                        }
                                                    }

                                                }
                                            });
                                        }
                                    } else {
                                        GPDialogs.warningDialog(getActivity(), getString(R.string.unable_to_find_tables_in_db), null);
                                    }
                                    break;
                                }
                            }

                        }
                    } catch (Exception e) {
                        GPDialogs.errorDialog(getActivity(), e, null);
                    }
                }
                break;
            }
            case (PICKFOLDER_REQUEST_CODE): {
                if (resultCode == Activity.RESULT_OK) {
                    try {
                        String folderPath = data.getStringExtra(LibraryConstants.PREFS_KEY_PATH);
                        final File folder = new File(folderPath);
                        if (folder.exists()) {
                            Utilities.setLastFilePath(getContext(), folderPath);
                            final List<File> foundFiles = new ArrayList<>();
                            // get all supported files
                            String[] supportedExtensions = ESpatialDataSources.getSupportedTileSourcesExtensions();
                            FileUtilities.searchDirectoryRecursive(folder, supportedExtensions, foundFiles);
                            // add basemap to list and in mPreferences
                        }
                    } catch (Exception e) {
                        GPDialogs.errorDialog(getActivity(), e, null);
                    }
                }
                break;
            }
        }
    }


    //// DRAG AND DROP


    private static class MyColumnDragItem extends DragItem {

        MyColumnDragItem(Context context, int layoutId) {
            super(context, layoutId);
            setSnapToTouch(false);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            LinearLayout clickedLayout = (LinearLayout) clickedView;
            View clickedHeader = clickedLayout.getChildAt(0);
            RecyclerView clickedRecyclerView = (RecyclerView) clickedLayout.getChildAt(1);

            View dragHeader = dragView.findViewById(R.id.drag_header);
            ScrollView dragScrollView = dragView.findViewById(R.id.drag_scroll_view);
            LinearLayout dragLayout = dragView.findViewById(R.id.drag_list);
            dragLayout.removeAllViews();

            ((TextView) dragHeader.findViewById(R.id.text)).setText(((TextView) clickedHeader.findViewById(R.id.text)).getText());
            ((TextView) dragHeader.findViewById(R.id.item_count)).setText(((TextView) clickedHeader.findViewById(R.id.item_count)).getText());
            for (int i = 0; i < clickedRecyclerView.getChildCount(); i++) {
                View mapItemView = View.inflate(dragView.getContext(), R.layout.column_item, null);

                ((TextView) mapItemView.findViewById(R.id.text)).setText(((TextView) clickedRecyclerView.getChildAt(i).findViewById(R.id.text)).getText());
                dragLayout.addView(mapItemView);

                if (i == 0) {
                    dragScrollView.setScrollY(-clickedRecyclerView.getChildAt(i).getTop());
                }
            }

            dragView.setPivotY(0);
            dragView.setPivotX(clickedView.getMeasuredWidth() / 2);
        }

        @Override
        public void onStartDragAnimation(View dragView) {
            super.onStartDragAnimation(dragView);
            dragView.animate().scaleX(0.9f).scaleY(0.9f).start();
        }

        @Override
        public void onEndDragAnimation(View dragView) {
            super.onEndDragAnimation(dragView);
            dragView.animate().scaleX(1).scaleY(1).start();
        }
    }

    private static class MyDragItem extends DragItem {

        MyDragItem(Context context, int layoutId) {
            super(context, layoutId);
        }

        @Override
        public void onBindDragView(View clickedView, View dragView) {
            CharSequence name = ((TextView) clickedView.findViewById(R.id.nameView)).getText();
            ((TextView) dragView.findViewById(R.id.nameView)).setText(name);

            CharSequence path = ((TextView) clickedView.findViewById(R.id.pathView)).getText();
            ((TextView) dragView.findViewById(R.id.pathView)).setText(path);

            boolean enabled = ((CheckBox) clickedView.findViewById(R.id.enableCheckbox)).isChecked();
            ((CheckBox) dragView.findViewById(R.id.enableCheckbox)).setChecked(enabled);

            CardView dragCard = dragView.findViewById(R.id.card);
            CardView clickedCard = clickedView.findViewById(R.id.card);

            dragCard.setMaxCardElevation(40);
            dragCard.setCardElevation(clickedCard.getCardElevation());
            // I know the dragView is a FrameLayout and that is why I can use setForeground below api level 23
            dragCard.setForeground(clickedView.getResources().getDrawable(R.drawable.card_view_drag_foreground));
        }

        @Override
        public void onMeasureDragView(View clickedView, View dragView) {
            CardView dragCard = dragView.findViewById(R.id.card);
            CardView clickedCard = clickedView.findViewById(R.id.card);
            int widthDiff = dragCard.getPaddingLeft() - clickedCard.getPaddingLeft() + dragCard.getPaddingRight() -
                    clickedCard.getPaddingRight();
            int heightDiff = dragCard.getPaddingTop() - clickedCard.getPaddingTop() + dragCard.getPaddingBottom() -
                    clickedCard.getPaddingBottom();
            int width = clickedView.getMeasuredWidth() + widthDiff;
            int height = clickedView.getMeasuredHeight() + heightDiff;
            dragView.setLayoutParams(new FrameLayout.LayoutParams(width, height));

            int widthSpec = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY);
            int heightSpec = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY);
            dragView.measure(widthSpec, heightSpec);
        }

        @Override
        public void onStartDragAnimation(View dragView) {
            CardView dragCard = dragView.findViewById(R.id.card);
            ObjectAnimator anim = ObjectAnimator.ofFloat(dragCard, "CardElevation", dragCard.getCardElevation(), 40); //NON-NLS
            anim.setInterpolator(new DecelerateInterpolator());
            anim.setDuration(ANIMATION_DURATION);
            anim.start();
        }

        @Override
        public void onEndDragAnimation(View dragView) {
            CardView dragCard = dragView.findViewById(R.id.card);
            ObjectAnimator anim = ObjectAnimator.ofFloat(dragCard, "CardElevation", dragCard.getCardElevation(), 6); //NON-NLS
            anim.setInterpolator(new DecelerateInterpolator());
            anim.setDuration(ANIMATION_DURATION);
            anim.start();
        }
    }
}
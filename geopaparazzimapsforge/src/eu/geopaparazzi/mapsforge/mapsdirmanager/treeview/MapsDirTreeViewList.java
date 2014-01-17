package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;

import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.AdapterView.OnItemClickListener;
import eu.geopaparazzi.library.database.GPLog;
import eu.geopaparazzi.mapsforge.R;
import eu.geopaparazzi.mapsforge.mapsdirmanager.MapsDirManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.adapter.FileDirectoryTreeViewAdapter;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.adapter.MapTypeTreeViewAdapter;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.util.NodeSortParameter;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.util.TreeType;

/**
 * Demo activity showing how the tree view can be used.
 *
 */
@SuppressWarnings({"rawtypes", "unchecked", "nls"})
public class MapsDirTreeViewList extends Activity {
    private static final String SAVEDINSTANCE__USE_TREE_TYPE = "use_treeType";
    /**
     * The name of the extra data in the result {@link Intent}.
     */
    private static final String SELECTED_FILE = "selected_nodesFILE";
    private static final String SELECTED_TYPE = "selected_nodesTYPE";
    private static final String SELECTED_CLASSINFO = "selected_nodesCLASSINFO";

    private static List<TreeNode< ? >> sourceNodesList = null;
    private static List<TreeNode< ? >> directoryFileLevelsNodesList = null;
    private final Set<Long> selected_nodes = new HashSet<Long>();
    private static final String TAG = MapsDirTreeViewList.class.getSimpleName();
    private TreeViewList this_treeViewList;
    private static int MAPDIR_LEVEL_NUMBER = 0;
    private static int MAX_LEVEL_NUMBER = 0;
    private TreeStateManager<Long> treeStateManagerFiledirectory = null;
    private TreeStateManager<Long> treeStateManagerMaptype = null;

    private static MapTypeTreeViewAdapter treeview_maptype = null;
    private static FileDirectoryTreeViewAdapter treeview_filedirectory = null;

    private static TreeType currentTreeType = TreeType.FILEDIRECTORY;
    private boolean collapsible;

    /**
     * Flag to enable the delete file menu.
     */
    public static boolean ENABLE_MENU_DELETE_FILE = false;
    /**
     * Flag to enable the edit file menu.
     */
    public static boolean ENABLE_MENU_EDIT_FILE = false;
    /**
     * Flag to enable the file properties menu.
     */
    public static boolean ENABLE_MENU_PROPERTIES_FILE = true;

    private static TreeNode mapsdirParentTreeNode = null;

    // Sort the files, sorted by Directory-File
    private static Comparator<TreeNode< ? >> directoryFileComparator = TreeNode.getComparator(NodeSortParameter.SORT_DIRECTORY,
            NodeSortParameter.SORT_FILE_NAME);

    // Sort the files, sorted by typetext-Directory-File
    private static Comparator<TreeNode< ? >> typetextDirectoryFileComparator = TreeNode.getComparator(
            NodeSortParameter.SORT_TYPE_TEXT, NodeSortParameter.SORT_DIRECTORY, NodeSortParameter.SORT_FILE_NAME);

    private final int id_mapsdir_context_menu_load = 0;
    private final int id_mapsdir_context_menu_properties = 1;
    private final int id_mapsdir_context_menu_properties_edit = 2;
    private final int id_mapsdir_context_menu_properties_delete = 3;
    private final int id_mapsdir_treeview_filedirectory_menu = 4;
    private final int id_mapsdir_treeview_maptype_menu = 5;
    private final int id_mapsdir_treeview_collapsible_menu_disable = 6;
    private final int id_mapsdir_treeview_expand_all_menu = 7;
    private final int id_mapsdir_treeview_collapse_all_menu = 8;
    private final int id_mapsdir_treeview_item_fields_options_button = 9;
    private final int id_mapsdir_context_menu_collapse = 10;
    private final int id_mapsdir_context_menu_expand_all = 11;
    private final int id_mapsdir_context_menu_expand_item = 12;
    // -----------------------------------------------
    /**
      * Get the list of tile based {@link TreeNode}s.
      *
      * <ul>
      *  <li>the data has been collected by the different Map-Classes</li>
      *  <li>- this can be used to display by Map-Type</li>
      *  <li>- field 'long_text' is the absolut path to the file </li>
      *  <li>- filedirectory_classes : is sorted by the Absolut-Path</li>
      *  <li>- - this can be used to display by File-Diretory</li>
      *  <li>- both arrarys will have the same size</li>
      * </ul>
     * @param origSourceNodesList the source {@link TreeNode}s list.
     * @param mapsDirFile the maps folder file.
     * @return the list sorted as Directory-File with levels set.
     */
    public static List<TreeNode< ? >> getTileBasedNodesList( final List<TreeNode< ? >> origSourceNodesList, final File mapsDirFile ) {
        String mapsDirPath = mapsDirFile.getAbsolutePath();
        int fileDirectoryLevel = 0;
        mapsdirParentTreeNode = new TreeNode(0, null, mapsDirPath);
        mapsdirParentTreeNode.setLevel(fileDirectoryLevel);

        MAPDIR_LEVEL_NUMBER = (mapsDirPath.length() - mapsDirPath.replaceAll(File.separator, "").length());
        directoryFileLevelsNodesList = new LinkedList<TreeNode< ? >>();
        for( int i = 0; i < origSourceNodesList.size(); i++ ) {
            TreeNode< ? > sourceNode = origSourceNodesList.get(i);
            String filePath = sourceNode.getFilePath();
            fileDirectoryLevel = filePath.length() - filePath.replaceAll(File.separator, "").length() - MAPDIR_LEVEL_NUMBER;
            if (MAX_LEVEL_NUMBER < fileDirectoryLevel)
                MAX_LEVEL_NUMBER = fileDirectoryLevel;
            sourceNode.setLevel(fileDirectoryLevel);
            directoryFileLevelsNodesList.add(sourceNode);
        }
        MAX_LEVEL_NUMBER++; // add '/mnt/sdcard/maps' to the amount of levels

        sourceNodesList = origSourceNodesList;

        Collections.sort(sourceNodesList, typetextDirectoryFileComparator);
        Collections.sort(directoryFileLevelsNodesList, directoryFileComparator);
        return directoryFileLevelsNodesList;
    }

    // Goal :
    // - retain basic this_treeViewList ContentView reactions (Collap)
    // - http://code.google.com/p/tree-view-list-android/
    // -- R.layout.mapsdir_treeview
    // - add selection ContentView reactions as found in 'ringdroid' project
    // -- R.layout.mapsdir_treeview_item_fields
    // --- when a simple file is clicked/selected_nodes
    // ---- the result will end the activity and load the file
    // ----- directories will be ignored
    // --- when a simple file is pressed or the right combox selected_nodes
    // ---- a menu will show up with the selected_nodes 'file-name' and possible options
    // ----- directories at the moment not defined
    @Override
    public void onCreate( final Bundle savedInstanceState ) {
        super.onCreate(savedInstanceState);
        if (directoryFileLevelsNodesList == null) {
            // FIXME handle this better with a message
            GPLog.androidLog(4, TAG + "onCreate setMapTypeClasses [has not been called]");
            return;
        }

        boolean makeCollapsible;
        TreeType newTreeType;
        if (savedInstanceState == null) {
            treeStateManagerFiledirectory = new InMemoryTreeStateManager<Long>();
            treeStateManagerMaptype = new InMemoryTreeStateManager<Long>();
            final TreeBuilder<Long> treeBuilderFiledirectory = new TreeBuilder<Long>(treeStateManagerFiledirectory);
            final TreeBuilder<Long> treeBuilderMaptype = new TreeBuilder<Long>(treeStateManagerMaptype);
            TreeNode currentTreeNode = null;

            int filedirectoryLevel = 0;
            int maptypeLevel = 0;
            int filedirectoryParentDir = 0;
            int maptypeParentDir = 0;
            long filedirectoryPosition = 0;
            long maptypePosition = 0;

            newTreeType = currentTreeType;
            String s_directory_prev_filedirectory = "";
            String s_directory_prev_maptype = "";
            String previousMaptype = "";
            String s_directory_new = "";

            try {
                for( int i = 0; i < sourceNodesList.size(); i++ ) {
                    switch( newTreeType ) {
                    case MAPTYPE:
                        // the input has been sorted by 'TYPE_TEXT,DIRECTORY,FILE'
                    case FILEDIRECTORY:
                        // the input has been sorted by 'DIRECTORY,FILE'
                    default: {
                        // ---------------------------------------------
                        // Begin: 'TYPE_TEXT,DIRECTORY,FILE'
                        // ---------------------------------------------
                        currentTreeNode = sourceNodesList.get(i);

                        String newMapType = currentTreeNode.getTypeText() + " [" + mapsdirParentTreeNode.getFilePath() + "]";
                        String[] sa_string_prev;
                        String s_directory_prev = "";
                        if (!newMapType.equals(previousMaptype)) {
                            maptypeLevel = 0;
                            String filePath = mapsdirParentTreeNode.getFilePath();
                            TreeNode dir_classinfo = new TreeNode((maptypePosition + 10000), null, filePath);
                            dir_classinfo.setLevel(maptypeLevel);
                            previousMaptype = newMapType; // "mbtiles [/mnt/extSdCard/maps]";
                            maptypePosition = (long) (i + maptypeParentDir);
                            treeBuilderMaptype.sequentiallyAddNextNode(maptypePosition, maptypeLevel, dir_classinfo);
                            maptypeParentDir++;
                        }
                        s_directory_new = "";
                        if (currentTreeNode.getLevel() > 1) { // level[1] is a file in the
                                                              // root-map-directory
                            s_directory_new = currentTreeNode.getFile().getParent()
                                    .replaceAll(mapsdirParentTreeNode.getFilePath() + File.separator, "")
                                    + File.separator;
                        }
                        if (((currentTreeNode.getLevel()) != maptypeLevel) || (!s_directory_new.equals(s_directory_prev_maptype))) {
                            String s_directory_path = "";
                            if (!s_directory_new.equals("")) {
                                boolean b_directory_prev = s_directory_new.startsWith(s_directory_prev_maptype);
                                maptypeLevel = mapsdirParentTreeNode.getLevel();
                                String[] sa_string = s_directory_new.split(File.separator);
                                sa_string_prev = s_directory_prev_maptype.split(File.separator);
                                s_directory_path = mapsdirParentTreeNode.getFilePath() + File.separator;
                                for( int j = 0; j < sa_string.length; j++ ) {
                                    String s_directory = sa_string[j] + File.separator;
                                    s_directory_prev = "";
                                    maptypeLevel++;
                                    if (b_directory_prev) {
                                        if (j < sa_string_prev.length) {
                                            s_directory_prev = sa_string_prev[j] + File.separator;
                                        }
                                    }
                                    if (!s_directory.equals(s_directory_prev)) {
                                        // First parm must have an absolute path, -
                                        // - second parm 's_directory' : only this will be shown
                                        // 'getShortText()'
                                        TreeNode dir_classinfo = new TreeNode((maptypePosition + 10000), null, s_directory_path
                                                + s_directory);
                                        dir_classinfo.setLevel(maptypeLevel);
                                        // todo search if exists, add only if it does not
                                        maptypePosition = (long) (i + maptypeParentDir);
                                        treeBuilderMaptype.sequentiallyAddNextNode(maptypePosition, maptypeLevel, dir_classinfo);
                                        maptypeParentDir++;
                                    }
                                    s_directory_path += s_directory;
                                }
                            }
                            maptypeLevel = currentTreeNode.getLevel();
                            // GPLog.androidLog(-1,TAG+" onCreate prev[" + s_directory_prev_maptype+
                            // "] new[" + s_directory_new+
                            // "] level["+i_level_maptype+"] directory_path[" + s_directory_path+
                            // "]");
                            s_directory_prev_maptype = s_directory_new;
                        }
                        maptypePosition = (long) (i + maptypeParentDir);
                        // GPLog.androidLog(-1,TAG+" onCreate["+this_classinfo.getId()+"] TYPE_TEXT,DIRECTORY,FILE["
                        // + this_classinfo.getFileNamePath()+ "]");
                        treeBuilderMaptype.sequentiallyAddNextNode(maptypePosition, maptypeLevel, currentTreeNode);
                        // ---------------------------------------------
                        // End: 'TYPE_TEXT,DIRECTORY,FILE'
                        // ---------------------------------------------
                        // Begin: 'DIRECTORY,FILE'
                        // ---------------------------------------------
                        currentTreeNode = directoryFileLevelsNodesList.get(i);
                        if (filedirectoryLevel == 0) {
                            s_directory_prev_filedirectory = mapsdirParentTreeNode.getFilePath(); // "/mnt/extSdCard/maps";
                            treeBuilderFiledirectory.sequentiallyAddNextNode((long) filedirectoryParentDir++,
                                    mapsdirParentTreeNode.getLevel(), mapsdirParentTreeNode);
                            filedirectoryLevel = mapsdirParentTreeNode.getLevel();
                        }
                        // GPLog.androidLog(-1,TAG+" onCreate[" + this_classinfo.toString()+ "]");
                        // String
                        // s_directory_new=TreeNode.getSubDirectory(this_classinfo,i_level_filedirectory,mapsdir_classinfo.getFileNamePath(),s_directory_prev_filedirectory);
                        s_directory_new = "";
                        if (currentTreeNode.getLevel() > 1) { // level[1] is a file in the
                                                              // root-map-directory
                            s_directory_new = currentTreeNode.getFile().getParent()
                                    .replaceAll(mapsdirParentTreeNode.getFilePath() + File.separator, "")
                                    + File.separator;
                        }
                        if (((currentTreeNode.getLevel()) != filedirectoryLevel)
                                || (!s_directory_new.equals(s_directory_prev_filedirectory))) {
                            // GPLog.androidLog(-1,TAG+" onCreate[FILEDIRECTORY] level["+this_classinfo.getLevel()+"] prev["
                            // + s_directory_prev_filedirectory+ "] new[" + s_directory_new+ "] ");
                            String s_directory_path = "";
                            if (!s_directory_new.equals("")) {
                                boolean b_directory_prev = s_directory_new.startsWith(s_directory_prev_filedirectory);
                                filedirectoryLevel = mapsdirParentTreeNode.getLevel();
                                // int i_level_filedirectorys =
                                // (s_directory_new.length()-s_directory_new.replaceAll(File.separator,
                                // "").length());
                                String[] sa_string = s_directory_new.split(File.separator);
                                sa_string_prev = s_directory_prev_filedirectory.split(File.separator);
                                s_directory_path = mapsdirParentTreeNode.getFilePath() + File.separator;
                                for( int j = 0; j < sa_string.length; j++ ) {
                                    String s_directory = sa_string[j] + File.separator;
                                    filedirectoryLevel++;
                                    s_directory_prev = "";
                                    if (b_directory_prev) {
                                        if (j < sa_string_prev.length) {
                                            s_directory_prev = sa_string_prev[j] + File.separator;
                                        }
                                    }
                                    if (!s_directory.equals(s_directory_prev)) {
                                        // First parm must have an absolute path, -
                                        // - second parm 's_directory' : only this will be shown
                                        // 'getShortText()'
                                        TreeNode dir_classinfo = new TreeNode((filedirectoryPosition + 10000), null,
                                                s_directory_path + s_directory);
                                        dir_classinfo.setLevel(filedirectoryLevel);
                                        // todo search if exists, add only if it does not
                                        filedirectoryPosition = (long) (i + filedirectoryParentDir);
                                        treeBuilderFiledirectory.sequentiallyAddNextNode(filedirectoryPosition,
                                                filedirectoryLevel, dir_classinfo);
                                        filedirectoryParentDir++;
                                    }
                                    s_directory_path += s_directory;
                                }
                            }
                            filedirectoryLevel = currentTreeNode.getLevel();
                            // GPLog.androidLog(-1,TAG+" onCreate prev[" +
                            // s_directory_prev_filedirectory+ "] new[" + s_directory_new+
                            // "] level["+i_level_filedirectory+"] directory_path[" +
                            // s_directory_path+ "]");
                            s_directory_prev_filedirectory = s_directory_new;
                        }
                        filedirectoryPosition = (long) (i + filedirectoryParentDir);
                        treeBuilderFiledirectory.sequentiallyAddNextNode(filedirectoryPosition, filedirectoryLevel,
                                currentTreeNode);
                        // ---------------------------------------------
                        // End: 'DIRECTORY,FILE'
                        // ---------------------------------------------
                    }
                        break;
                    }
                }
            } catch (Exception e) {
                GPLog.androidLog(4, TAG + "onCreate creating [filling tree-nodes]", e);
            }
            // GPLog.androidLog(-1,TAG+" onCreate[" + tree_manager_filedirectory.toString()+ "]");
            // GPLog.androidLog(-1,TAG+" onCreate[filling ended]");
            makeCollapsible = true;
        } else {
            treeStateManagerMaptype = (TreeStateManager<Long>) savedInstanceState.getSerializable("treetree_manager_maptype");
            if (treeStateManagerMaptype == null) {
                treeStateManagerMaptype = new InMemoryTreeStateManager<Long>();
            }
            treeStateManagerFiledirectory = (TreeStateManager<Long>) savedInstanceState
                    .getSerializable("treetree_manager_filedirectory");
            if (treeStateManagerFiledirectory == null) {
                treeStateManagerFiledirectory = new InMemoryTreeStateManager<Long>();
            }
            newTreeType = (TreeType) savedInstanceState.getSerializable(SAVEDINSTANCE__USE_TREE_TYPE);
            if (newTreeType == null) {
                newTreeType = TreeType.FILEDIRECTORY;
            }
            makeCollapsible = savedInstanceState.getBoolean("collapsible");
        }
        try {
            setContentView(R.layout.mapsdir_treeview);
            this_treeViewList = (TreeViewList) findViewById(R.id.mapsdir_treeview);
            setTreeAdapter(newTreeType);
            setCollapsible(makeCollapsible);
            registerForContextMenu(this_treeViewList);
        } catch (Exception e) {
            GPLog.androidLog(4, TAG + "onCreate creating [this_treeViewList_row_Adapter]", e);
        }
        // -- end of Context logic from original 'tree-view-list-android' project
        // Normal click - select the file and exit
        // - when a simple file (directories are ignored)
        // -- the Activity will end returning the file information
        // --- no user interaction
        // [logic from 'ringdroid' project]
        // 20131107 mj10777: this is the same as done in setTreeAdapter()
        // - for some reason it does not take effect there the first time
        // -- when the Tree-Type change menu is called it works, but not here
        // --- thus we call it again here [only once]
        this_treeViewList.setOnItemClickListener(new OnItemClickListener(){
            public void onItemClick( AdapterView parent, View this_view, int position, long id_node ) {
                TreeNodeInfo<Long> node_info = null;
                switch( currentTreeType ) {
                case MAPTYPE:
                    node_info = treeStateManagerMaptype.getNodeInfo(id_node);
                    break;
                case FILEDIRECTORY:
                default:
                    node_info = treeStateManagerFiledirectory.getNodeInfo(id_node);
                    break;
                }
                GPLog.androidLog(-1, TAG + " onItemClick[" + currentTreeType + "] [" + node_info.getFilePath() + "] id_node["
                        + id_node + "]");
                if ((node_info != null) && (!node_info.isWithChildren())) {
                    loadNodeAndFinish(id_node);
                }
            }
        });
        // Long-press opens a context menu
        // setContentView(R.layout.mapsdir_treeview_item_fields);
        // ImageView this_view = (ImageView)
        // findViewById(R.id.mapsdir_treeview_item_fields_options_button);
        // registerForContextMenu(this_view);
    }
    @Override
    protected void onSaveInstanceState( final Bundle outState ) {
        outState.putSerializable("treetree_manager_maptype", treeStateManagerMaptype);
        outState.putSerializable("treetree_manager_filedirectory", treeStateManagerFiledirectory);
        outState.putSerializable(SAVEDINSTANCE__USE_TREE_TYPE, currentTreeType);
        outState.putBoolean("collapsible", this.collapsible);
        super.onSaveInstanceState(outState);
    }

    protected final void setTreeAdapter( final TreeType new_TreeType ) {
        currentTreeType = new_TreeType;
        switch( currentTreeType ) {
        case MAPTYPE: {
            if (treeview_maptype == null) {
                treeview_maptype = new MapTypeTreeViewAdapter(this, selected_nodes, treeStateManagerMaptype, MAX_LEVEL_NUMBER);
            }
            this_treeViewList.setAdapter(treeview_maptype);
        }
            break;
        case FILEDIRECTORY:
        default: {
            if (treeview_filedirectory == null) {
                treeview_filedirectory = new FileDirectoryTreeViewAdapter(this, selected_nodes, treeStateManagerFiledirectory,
                        MAX_LEVEL_NUMBER);
            }
            this_treeViewList.setAdapter(treeview_filedirectory);
        }
            break;
        }
        this_treeViewList.setOnItemClickListener(new OnItemClickListener(){
            public void onItemClick( AdapterView parent, View this_view, int position, long id_node ) {
                TreeNodeInfo<Long> node_info = null;
                switch( currentTreeType ) {
                case MAPTYPE:
                    node_info = treeStateManagerMaptype.getNodeInfo(id_node);
                    break;
                case FILEDIRECTORY:
                default:
                    node_info = treeStateManagerFiledirectory.getNodeInfo(id_node);
                    break;
                }
                // GPLog.androidLog(-1,TAG+" onItemClick["+use_treeType+"] [" +
                // node_info.getLongText()+ "] id_node["+id_node+"]");
                if ((node_info != null) && (!node_info.isWithChildren())) {
                    loadNodeAndFinish(id_node);
                }
            }
        });
    }
    protected final void setCollapsible( final boolean new_Collapsible ) {
        this.collapsible = new_Collapsible;
        this_treeViewList.setCollapsible(this.collapsible);
    }
    @Override
    public boolean onCreateOptionsMenu( final Menu menu ) {
        menu.add(0, id_mapsdir_treeview_filedirectory_menu, 0, R.string.mapsdir_treeview_filedirectory_menu);
        menu.add(0, id_mapsdir_treeview_maptype_menu, 0, R.string.mapsdir_treeview_maptype_menu);
        menu.add(0, id_mapsdir_treeview_expand_all_menu, 0, R.string.mapsdir_treeview_expand_all_menu);
        menu.add(0, id_mapsdir_treeview_collapse_all_menu, 0, R.string.mapsdir_treeview_collapse_all_menu);
        menu.add(0, id_mapsdir_treeview_collapsible_menu_disable, 0, R.string.mapsdir_treeview_collapsible_menu_disable);
        return true;
    }
    @Override
    public boolean onPrepareOptionsMenu( final Menu menu ) {
        final MenuItem collapsibleMenu = menu.findItem(id_mapsdir_treeview_collapsible_menu_disable);
        if (collapsible) {
            collapsibleMenu.setTitle(R.string.mapsdir_treeview_collapsible_menu_disable);
            collapsibleMenu.setTitleCondensed(getResources().getString(
                    R.string.mapsdir_treeview_collapsible_menu_disable_condensed));
        } else {
            collapsibleMenu.setTitle(R.string.mapsdir_treeview_collapsible_menu_enable);
            collapsibleMenu.setTitleCondensed(getResources().getString(R.string.mapsdir_treeview_collapsible_condensed_enable));
        }
        MenuItem mapsdirMenu = null;
        switch( currentTreeType ) {
        case MAPTYPE:
            // Show only the option to activate the other modus [hide the active modus]
            menu.findItem(id_mapsdir_treeview_filedirectory_menu).setVisible(true);
            menu.findItem(id_mapsdir_treeview_maptype_menu).setVisible(false);
            mapsdirMenu = menu.findItem(id_mapsdir_treeview_filedirectory_menu);
            mapsdirMenu.setTitle(R.string.mapsdir_treeview_filedirectory_menu);
            mapsdirMenu.setTitleCondensed(getResources().getString(R.string.mapsdir_treeview_filedirectory_menu_condensed));
            break;
        case FILEDIRECTORY:
        default:
            // Show only the option to activate the other modus [hide the active modus]
            menu.findItem(id_mapsdir_treeview_maptype_menu).setVisible(true);
            menu.findItem(id_mapsdir_treeview_filedirectory_menu).setVisible(false);
            mapsdirMenu = menu.findItem(id_mapsdir_treeview_maptype_menu);
            mapsdirMenu.setTitle(R.string.mapsdir_treeview_maptype_menu);
            mapsdirMenu.setTitleCondensed(getResources().getString(R.string.mapsdir_treeview_maptype_menu_condensed));
            break;
        }
        return super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected( final MenuItem item ) {
        switch( item.getItemId() ) {
        case id_mapsdir_treeview_filedirectory_menu:
            setTreeAdapter(TreeType.FILEDIRECTORY);
            break;
        case id_mapsdir_treeview_maptype_menu:
            setTreeAdapter(TreeType.MAPTYPE);
            break;
        case id_mapsdir_treeview_collapsible_menu_disable:
            setCollapsible(!this.collapsible);
            break;
        case id_mapsdir_treeview_expand_all_menu:
            switch( currentTreeType ) {
            case MAPTYPE: {
                treeStateManagerMaptype.expandEverythingBelow(null);
            }
                break;
            case FILEDIRECTORY: {
                treeStateManagerFiledirectory.expandEverythingBelow(null);
            }
                break;
            }
            break;
        case id_mapsdir_treeview_collapse_all_menu:
            switch( currentTreeType ) {
            case MAPTYPE: {
                treeStateManagerMaptype.collapseChildren(null);
            }
                break;
            case FILEDIRECTORY: {
                treeStateManagerFiledirectory.collapseChildren(null);
            }
                break;
            }
            break;
        default:
            return false;
        }
        return true;
    }
    @Override
    public void onCreateContextMenu( final ContextMenu menu, final View this_view, final ContextMenuInfo menuInfo ) {
        final AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
        long id_node = adapterInfo.id;
        TreeNodeInfo<Long> treeNode = null;
        switch( currentTreeType ) {
        case MAPTYPE:
            treeNode = treeStateManagerMaptype.getNodeInfo(id_node);
            break;
        case FILEDIRECTORY:
            treeNode = treeStateManagerFiledirectory.getNodeInfo(id_node);
            break;
        }
        menu.add(0, id_mapsdir_context_menu_expand_all, 0, R.string.mapsdir_treeview_expand_all_menu);
        menu.add(0, id_mapsdir_context_menu_expand_item, 0, R.string.mapsdir_treeview_expand_menu);
        menu.add(0, id_mapsdir_context_menu_collapse, 0, R.string.mapsdir_treeview_collapse_all_menu);
        if (treeNode.isWithChildren()) {
            if (treeNode.isExpanded()) {
                menu.findItem(id_mapsdir_context_menu_expand_item).setVisible(false);
                menu.findItem(id_mapsdir_context_menu_expand_all).setVisible(false);
            } else {
                menu.findItem(id_mapsdir_context_menu_collapse).setVisible(false);
            }
        } else {
            menu.findItem(id_mapsdir_context_menu_expand_item).setVisible(false);
            menu.findItem(id_mapsdir_context_menu_expand_all).setVisible(false);
            menu.findItem(id_mapsdir_context_menu_collapse).setVisible(false);
        }
        switch( this_view.getId() ) {
        case id_mapsdir_treeview_item_fields_options_button:
            break;
        default:
            // changing color does not work [ffff99,#EDDA74]
            // String
            // s_Title_Text="<font color='#EDDA74'><center> --> "+node_info.getShortText()+"</center></font>";
            // menu.setHeaderTitle(Html.fromHtml(s_Title_Text));
            String s_Title_Text = "[" + treeNode.getTypeDescriptionText() + "] " + treeNode.getFilePath();
            menu.setHeaderTitle(s_Title_Text);
            if (ENABLE_MENU_PROPERTIES_FILE) {
                menu.add(0, id_mapsdir_context_menu_properties, 0, R.string.mapsdir_treeview_properties_menu);
            }
            if (ENABLE_MENU_EDIT_FILE) {
                menu.add(0, id_mapsdir_context_menu_properties_edit, 0, R.string.mapsdir_treeview_properties_edit_menu);
            }
            if (ENABLE_MENU_DELETE_FILE) {
                menu.add(0, id_mapsdir_context_menu_properties_delete, 0, R.string.mapsdir_treeview_delete_menu);
            }
            menu.add(0, id_mapsdir_context_menu_load, 0, R.string.mapsdir_treeview_load_menu);
            // GPLog.androidLog(-1,TAG+" onCreateContextMenu ["+this_view.getId()+"] mapsdir_treeview_item_fields_*["
            // + node_info.getLongText()+
            // "] properties["+MapsDirTreeViewList.ENABLE_MENU_PROPERTIES_FILE+"] edit["+MapsDirTreeViewList.ENABLE_MENU_EDIT_FILE+"] delete["+MapsDirTreeViewList.ENABLE_MENU_DELETE_FILE+"]");
            break;
        }
        super.onCreateContextMenu(menu, this_view, menuInfo);
    }
    @Override
    public boolean onContextItemSelected( final MenuItem item ) {
        final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
        final long id_node = info.id;
        TreeNodeInfo<Long> node_info = null;
        switch( currentTreeType ) {
        case MAPTYPE: {
            node_info = treeStateManagerMaptype.getNodeInfo(id_node);
        }
            break;
        case FILEDIRECTORY: {
            node_info = treeStateManagerFiledirectory.getNodeInfo(id_node);
        }
            break;
        }
        node_info.toString(); // TODO check why this is never used.

        switch( item.getItemId() ) {
        case id_mapsdir_context_menu_collapse:
            // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_collapse[" +
            // item.getTitle()+ "]");
            switch( currentTreeType ) {
            case MAPTYPE: {
                treeStateManagerMaptype.collapseChildren(id_node);
            }
                break;
            case FILEDIRECTORY: {
                treeStateManagerFiledirectory.collapseChildren(id_node);
            }
                break;
            }
            break;
        case id_mapsdir_context_menu_expand_all:
            // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_expand_all[" +
            // item.getTitle()+ "]");
            switch( currentTreeType ) {
            case MAPTYPE: {
                treeStateManagerMaptype.expandEverythingBelow(id_node);
            }
                break;
            case FILEDIRECTORY: {
                treeStateManagerFiledirectory.expandEverythingBelow(id_node);
            }
                break;
            }
            break;
        case id_mapsdir_context_menu_expand_item:
            // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_expand_item[" +
            // item.getTitle()+ "]");
            switch( currentTreeType ) {
            case MAPTYPE: {
                treeStateManagerMaptype.expandDirectChildren(id_node);
            }
                break;
            case FILEDIRECTORY: {
                treeStateManagerFiledirectory.expandDirectChildren(id_node);
            }
                break;
            }
            break;
        case id_mapsdir_context_menu_load:
            // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_load[" +
            // item.getTitle()+ "] id["+id+"]");
            loadNodeAndFinish(id_node);
            break;
        case id_mapsdir_context_menu_properties:
            // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_properties[" +
            // item.getTitle()+ "] id["+id+"]");
            switch( currentTreeType ) {
            case MAPTYPE: {
                // tree_manager_maptype.removeNodeRecursively(id_node);
            }
                break;
            case FILEDIRECTORY: {
                // tree_manager_filedirectory.removeNodeRecursively(id_node);
            }
                break;
            }
            break;
        case id_mapsdir_context_menu_properties_edit:
            // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_properties[" +
            // item.getTitle()+ "] id["+id+"]");
            switch( currentTreeType ) {
            case MAPTYPE: {
                // tree_manager_maptype.removeNodeRecursively(id_node);
            }
                break;
            case FILEDIRECTORY: {
                // tree_manager_filedirectory.removeNodeRecursively(id_node);
            }
                break;
            }
            break;
        case id_mapsdir_context_menu_properties_delete:
            // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_delete[" +
            // item.getTitle()+ "]");
            switch( currentTreeType ) {
            case MAPTYPE: {
                treeStateManagerMaptype.removeNodeRecursively(id_node);
            }
                break;
            case FILEDIRECTORY: {
                treeStateManagerFiledirectory.removeNodeRecursively(id_node);
            }
                break;
            }
            break;
        default:
            // GPLog.androidLog(-1,TAG+" onContextItemSelected unknown["+item.getItemId()+"] [" +
            // item.getTitle()+ "]");
            return super.onContextItemSelected(item);
        }
        return true;
    }
    protected final void loadNodeAndFinish( final long id_node ) {
        TreeNodeInfo<Long> treeNodeInfo = null;
        switch( currentTreeType ) {
        case MAPTYPE: {
            treeNodeInfo = treeStateManagerMaptype.getNodeInfo(id_node);
        }
            break;
        case FILEDIRECTORY: {
            treeNodeInfo = treeStateManagerFiledirectory.getNodeInfo(id_node);
        }
            break;
        }
        if (treeNodeInfo != null) {
            Intent send_intent = new Intent();
            String filePath = treeNodeInfo.getFilePath();
            String fileType = treeNodeInfo.getTypeDescriptionText();
            send_intent.putExtra(SELECTED_FILE, filePath);
            send_intent.putExtra(SELECTED_TYPE, fileType);
            TreeNode< ? > selectedTreeNode = treeNodeInfo.getTreeNode();
            send_intent.putExtra(SELECTED_CLASSINFO, selectedTreeNode.toString());
            GPLog.androidLog(-1, TAG + " return_load_file mapsdir_context_menu_load selected_file[" + filePath + "] type["
                    + fileType + "]");

            MapsDirManager.getInstance().selectMapClassInfo(this, selectedTreeNode, null, null);

            setResult(RESULT_OK, send_intent);
        }
        finish();
    }
}

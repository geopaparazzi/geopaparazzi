package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;

import java.io.Serializable;
import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.LinkedList;

import eu.geopaparazzi.mapsforge.R;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.InMemoryTreeStateManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.TreeBuilder;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.TreeNodeInfo;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.TreeStateManager;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.TreeViewList;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.ClassNodeInfo;
import eu.geopaparazzi.library.database.GPLog;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.AdapterContextMenuInfo;
import android.widget.ImageView;
import android.widget.SimpleCursorAdapter;

/**
 * Demo activity showing how the tree view can be used.
 *
 */
public class MapsDirTreeViewList extends Activity
{
 /**
  * The name of the extra data in the result {@link Intent}.
  */
 public static final String SELECTED_FILE = "selected_nodesFILE";
 public static final String SELECTED_TYPE = "selected_nodesTYPE";
 public static final String SELECTED_CLASSINFO = "selected_nodesCLASSINFO";
 public enum TreeType implements Serializable
 {
  FILEDIRECTORY,
  MAPTYPE
 }
 private static List<ClassNodeInfo> maptype_classes=null;
 private static List<ClassNodeInfo> filedirectory_classes=null;
 private static File maps_dir=null;
 private final Set<Long> selected_nodes = new HashSet<Long>();
 private static final String TAG = MapsDirTreeViewList.class.getSimpleName();
 private TreeViewList this_treeViewList;
 private static int MAPDIR_LEVEL_NUMBER = 0;
 private static int MAX_LEVEL_NUMBER = 0;
 private TreeStateManager<Long> tree_manager_filedirectory = null;
 private TreeStateManager<Long> tree_manager_maptype = null;
 private static MapTypeTreeViewAdapter treeview_maptype=null;
 private static FileDirectoryTreeViewAdapter treeview_filedirectory=null;
 private String s_file_name="";
 private String s_file_type="map";
 public static TreeType use_treeType=TreeType.FILEDIRECTORY;
 private boolean collapsible;
 public static boolean b_delete_file=false;
 public static boolean b_edit_file=false;
 public static boolean b_properties_file=true;
 public static ClassNodeInfo selected_classinfo=null;
 public static ClassNodeInfo mapsdir_classinfo=null;
  // Sort the files, sorted by Directory-File
 private static Comparator<ClassNodeInfo> cp_directory_file = ClassNodeInfo.getComparator(ClassNodeInfo.SortParameter.SORT_DIRECTORY,ClassNodeInfo.SortParameter.SORT_FILE);
 // Sort the files, sorted by Meta-type-Directory-File
 private static Comparator<ClassNodeInfo> cp_meta_directory_file = ClassNodeInfo.getComparator(ClassNodeInfo.SortParameter.SORT_TYPE_TEXT,ClassNodeInfo.SortParameter.SORT_DIRECTORY,ClassNodeInfo.SortParameter.SORT_FILE);
 private SimpleCursorAdapter this_treeViewList_row_Adapter;
 // For popup Context-Menu [in a library 'case id_' cannot be used, there are not final]
 private final int id_mapsdir_context_menu_load=0;
 private final int id_mapsdir_context_menu_properties=1;
 private final int id_mapsdir_context_menu_properties_edit=2;
 private final int id_mapsdir_context_menu_properties_delete=3;
 private final int id_mapsdir_treeview_filedirectory_menu=4;
 private final int id_mapsdir_treeview_maptype_menu=5;
 private final int id_mapsdir_treeview_collapsible_menu_disable=6;
 private final int id_mapsdir_treeview_expand_all_menu=7;
 private final int id_mapsdir_treeview_collapse_all_menu=8;
 private final int id_mapsdir_treeview_item_fields_options_button=9;
 private final int id_mapsdir_context_menu_collapse=10;
 private final int id_mapsdir_context_menu_expand_all=11;
 private final int id_mapsdir_context_menu_expand_item=12;
 // -----------------------------------------------
 /**
   * setMapTypeClasses : source of Data
   *
   * <ul>
   *  <li>the data has been collected by the different Map-Classes</li>
   *  <li>- this can be used to display by Map-Type</li>
   *  <li>- field 'long_text' is the absolut path to the file </li>
   *  <li>- filedirectory_classes : is sorted by the Absolut-Path</li>
   *  <li>- - this can be used to display by File-Diretory</li>
   *  <li>- both arrarys will have the same size</li>
   * </ul>
   *
   * @param maptype_classes source Map-Files collect by Map-Type
   * @param maps_dir list the starting Directory
   * @return void
  */
 public static List<ClassNodeInfo> setMapTypeClasses(final List<ClassNodeInfo> maptype_classes_parm,final File maps_dir_parm)
 {
  maps_dir=maps_dir_parm;
  String s_map_file=maps_dir.getAbsolutePath();
  int i_level_filedirectory = 0;
  mapsdir_classinfo=new ClassNodeInfo(0,100,"","directory",s_map_file,s_map_file,s_map_file,s_map_file,s_map_file,
  "","","");
  mapsdir_classinfo.setLevel(i_level_filedirectory);
  // '/mnt/sdcard/maps' or '/mnt/extSdCard/maps' = level[0]
  MAPDIR_LEVEL_NUMBER = (s_map_file.length()-s_map_file.replaceAll(File.separator, "").length());
  ClassNodeInfo this_classinfo=null;
  filedirectory_classes=new LinkedList<ClassNodeInfo>();
  GPLog.androidLog(-1,TAG+" setMapTypeClasses["+maptype_classes_parm.size()+"] [" + MAPDIR_LEVEL_NUMBER+ "]");
  for (int i=0;i<maptype_classes_parm.size();i++)
  {
   this_classinfo=maptype_classes_parm.get(i);
   s_map_file=this_classinfo.getLongText();
   // '/mnt/sdcard/maps/opencycle.mapurl' = level[1] or '/mnt/extSdCard/maps/defaulttiles/_opencycle.mbtiles'  = level[2]
   // '+1' this file is also a level
   i_level_filedirectory = (((s_map_file.length()-s_map_file.replaceAll(File.separator, "").length())-MAPDIR_LEVEL_NUMBER));
   // GPLog.androidLog(-1,TAG+" setMapTypeClasses["+this_classinfo.getId()+"] [" + this_classinfo.getFileNamePath()+ "]");
   // GPLog.androidLog(1,"MapsDirTreeViewList ["+i+"] i_level_filedirectory[" + i_level_filedirectory+ "] map_file[" + s_map_file+"]");
   if (MAX_LEVEL_NUMBER < i_level_filedirectory)
    MAX_LEVEL_NUMBER=i_level_filedirectory;
   this_classinfo.setLevel(i_level_filedirectory);
   filedirectory_classes.add(this_classinfo);
  }
  MAX_LEVEL_NUMBER++; // add '/mnt/sdcard/maps' to the amount of levels
  maptype_classes=maptype_classes_parm;
  Collections.sort(maptype_classes,cp_meta_directory_file);
  Collections.sort(filedirectory_classes,cp_directory_file);
  // GPLog.androidLog(2,"MapsDirTreeViewList setMapTypeClasses MAPDIR_LEVEL_NUMBER[" + MAPDIR_LEVEL_NUMBER+ "] MAX_LEVEL_NUMBER[" + MAX_LEVEL_NUMBER+"]");
  return filedirectory_classes;
 }
 @SuppressWarnings("unchecked")
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
 public void onCreate(final Bundle savedInstanceState)
 {
  super.onCreate(savedInstanceState);
  if (filedirectory_classes == null)
  {
   GPLog.androidLog(4,TAG+"onCreate setMapTypeClasses [has not been called]");
   return;
  }
  TreeType new_TreeType = null;
  String s_short_text;
  String s_long_text;
  String s_type="map";
  boolean new_Collapsible;
  if (savedInstanceState == null)
  {
   tree_manager_filedirectory = new InMemoryTreeStateManager<Long>();
   tree_manager_maptype = new InMemoryTreeStateManager<Long>();
   final TreeBuilder<Long> tree_builder_filedirectory = new TreeBuilder<Long>(tree_manager_filedirectory);
   final TreeBuilder<Long> tree_builder_maptype = new TreeBuilder<Long>(tree_manager_maptype);
   ClassNodeInfo this_classinfo=null;
   int i_level_filedirectory=0;
   int i_level_maptype=0;
   int i_parent_dir_filedirectory=0;
   int i_parent_dir_maptype=0;
   long l_position_filedirectory=0;
   long l_position_maptype=0;
   // new_TreeType = TreeType.FILEDIRECTORY;
   new_TreeType = use_treeType;
   String s_directory_prev_filedirectory="";
   String s_directory_prev_maptype="";
   String s_maptype_prev="";
   String s_directory_new="";
   try
   {
    for (int i_class=0;i_class<this.maptype_classes.size();i_class++)
    {
     switch (new_TreeType)
     {
      case MAPTYPE:
      // the input has been sorted by 'TYPE_TEXT,DIRECTORY,FILE'
      case FILEDIRECTORY:
      // the input has been sorted by 'DIRECTORY,FILE'
      default:
      {
       //---------------------------------------------
       // Begin: 'TYPE_TEXT,DIRECTORY,FILE'
       //---------------------------------------------
       this_classinfo=maptype_classes.get(i_class);
       String s_maptype_new=this_classinfo.getTypeText()+" ["+mapsdir_classinfo.getFileNamePath()+"]";
       String[] sa_string_prev;
       String s_directory_prev="";
       if (!s_maptype_new.equals(s_maptype_prev))
       {
        i_level_maptype=0;
        ClassNodeInfo dir_classinfo=new ClassNodeInfo((l_position_maptype+10000),100,"","directory",mapsdir_classinfo.getFileNamePath(),s_maptype_new,"","","","","","");
        dir_classinfo.setLevel(i_level_maptype);
        s_maptype_prev=s_maptype_new; // "mbtiles [/mnt/extSdCard/maps]";
        l_position_maptype=(long)(i_class+i_parent_dir_maptype);
        tree_builder_maptype.sequentiallyAddNextNode(l_position_maptype,i_level_maptype,dir_classinfo);
        i_parent_dir_maptype++;
       }
       // GPLog.androidLog(-1,TAG+" onCreate[" + this_classinfo.toString()+ "]");
       // String s_directory_new=ClassNodeInfo.getSubDirectory(this_classinfo,i_level_filedirectory,mapsdir_classinfo.getFileNamePath(),s_directory_prev_filedirectory);
       s_directory_new="";
       if (this_classinfo.getLevel() > 1)
       { // level[1] is a file in the root-map-directory
        s_directory_new=this_classinfo.getFilePath().getParent().replaceAll(mapsdir_classinfo.getFileNamePath()+File.separator,"")+File.separator;
       }
       if (((this_classinfo.getLevel()) != i_level_maptype) || (!s_directory_new.equals(s_directory_prev_maptype)))
       {
        // GPLog.androidLog(-1,TAG+" onCreate[MAPTYPE] level["+this_classinfo.getLevel()+"] prev[" + s_directory_prev_maptype+ "] new[" + s_directory_new+ "] ");
        String s_directory_path="";
        if (!s_directory_new.equals(""))
        {
         boolean b_directory_prev= s_directory_new.startsWith(s_directory_prev_maptype);
         i_level_maptype=mapsdir_classinfo.getLevel();
         // int i_level_maptypes = (s_directory_new.length()-s_directory_new.replaceAll(File.separator, "").length());
         String[] sa_string=s_directory_new.split(File.separator);
         sa_string_prev=s_directory_prev_maptype.split(File.separator);
         s_directory_path=mapsdir_classinfo.getFileNamePath()+File.separator;
         for (int j=0;j<sa_string.length;j++)
         { // if diff == 1: '1861_World_Mercator/' ; diff==2: 'mbtiles/1861_World_Mercator'
          String s_directory=sa_string[j]+File.separator;
          s_directory_prev="";
          i_level_maptype++;
          if (b_directory_prev)
          {
           if (j<sa_string_prev.length)
           {
            s_directory_prev=sa_string_prev[j]+File.separator;
           }
          }
          if (!s_directory.equals(s_directory_prev))
          {
           // First parm must have an absolute path, -
           // - second parm 's_directory' : only this will be shown 'getShortText()'
           ClassNodeInfo dir_classinfo=new ClassNodeInfo((l_position_maptype+10000),100,"","directory",s_directory_path+s_directory,s_directory,"","","","","","");
           dir_classinfo.setLevel(i_level_maptype);
           // todo search if exists, add only if it does not
           l_position_maptype=(long)(i_class+i_parent_dir_maptype);
           tree_builder_maptype.sequentiallyAddNextNode(l_position_maptype,i_level_maptype,dir_classinfo);
           i_parent_dir_maptype++;
          }
          s_directory_path+=s_directory;
         }
        }
        i_level_maptype=this_classinfo.getLevel();
        // GPLog.androidLog(-1,TAG+" onCreate prev[" + s_directory_prev_maptype+ "] new[" + s_directory_new+ "] level["+i_level_maptype+"] directory_path[" + s_directory_path+ "]");
        s_directory_prev_maptype = s_directory_new;
       }
       l_position_maptype=(long)(i_class+i_parent_dir_maptype);
       // GPLog.androidLog(-1,TAG+" onCreate["+this_classinfo.getId()+"] TYPE_TEXT,DIRECTORY,FILE[" + this_classinfo.getFileNamePath()+ "]");
       tree_builder_maptype.sequentiallyAddNextNode(l_position_maptype,i_level_maptype,this_classinfo);
       //---------------------------------------------
       // End:  'TYPE_TEXT,DIRECTORY,FILE'
       //---------------------------------------------
       // Begin: 'DIRECTORY,FILE'
       //---------------------------------------------
       this_classinfo=filedirectory_classes.get(i_class);
       if (i_level_filedirectory == 0)
       {
        s_directory_prev_filedirectory=mapsdir_classinfo.getFileNamePath(); // "/mnt/extSdCard/maps";
        tree_builder_filedirectory.sequentiallyAddNextNode((long) i_parent_dir_filedirectory++,mapsdir_classinfo.getLevel(),mapsdir_classinfo);
        i_level_filedirectory=mapsdir_classinfo.getLevel();
       }
       // GPLog.androidLog(-1,TAG+" onCreate[" + this_classinfo.toString()+ "]");
       // String s_directory_new=ClassNodeInfo.getSubDirectory(this_classinfo,i_level_filedirectory,mapsdir_classinfo.getFileNamePath(),s_directory_prev_filedirectory);
       s_directory_new="";
       if (this_classinfo.getLevel() > 1)
       { // level[1] is a file in the root-map-directory
        s_directory_new=this_classinfo.getFilePath().getParent().replaceAll(mapsdir_classinfo.getFileNamePath()+File.separator,"")+File.separator;
       }
       if (((this_classinfo.getLevel()) != i_level_filedirectory) || (!s_directory_new.equals(s_directory_prev_filedirectory)))
       {
        // GPLog.androidLog(-1,TAG+" onCreate[FILEDIRECTORY] level["+this_classinfo.getLevel()+"] prev[" + s_directory_prev_filedirectory+ "] new[" + s_directory_new+ "] ");
        String s_directory_path="";
        if (!s_directory_new.equals(""))
        {
         boolean b_directory_prev= s_directory_new.startsWith(s_directory_prev_filedirectory);
         i_level_filedirectory=mapsdir_classinfo.getLevel();
         // int i_level_filedirectorys = (s_directory_new.length()-s_directory_new.replaceAll(File.separator, "").length());
         String[] sa_string=s_directory_new.split(File.separator);
         sa_string_prev=s_directory_prev_filedirectory.split(File.separator);
         s_directory_path=mapsdir_classinfo.getFileNamePath()+File.separator;
         for (int j=0;j<sa_string.length;j++)
         { // if diff == 1: '1861_World_Mercator/' ; diff==2: 'mbtiles/1861_World_Mercator'
          String s_directory=sa_string[j]+File.separator;
          i_level_filedirectory++;
          s_directory_prev="";
          if (b_directory_prev)
          {
           if (j<sa_string_prev.length)
           {
            s_directory_prev=sa_string_prev[j]+File.separator;
           }
          }
          if (!s_directory.equals(s_directory_prev))
          {
           // First parm must have an absolute path, -
           // - second parm 's_directory' : only this will be shown 'getShortText()'
           ClassNodeInfo dir_classinfo=new ClassNodeInfo((l_position_filedirectory+10000),100,"","directory",s_directory_path+s_directory,s_directory,"","","","","","");
           dir_classinfo.setLevel(i_level_filedirectory);
           // todo search if exists, add only if it does not
           l_position_filedirectory=(long)(i_class+i_parent_dir_filedirectory);
           tree_builder_filedirectory.sequentiallyAddNextNode(l_position_filedirectory,i_level_filedirectory,dir_classinfo);
           i_parent_dir_filedirectory++;
          }
          s_directory_path+=s_directory;
         }
        }
        i_level_filedirectory=this_classinfo.getLevel();
        // GPLog.androidLog(-1,TAG+" onCreate prev[" + s_directory_prev_filedirectory+ "] new[" + s_directory_new+ "] level["+i_level_filedirectory+"] directory_path[" + s_directory_path+ "]");
        s_directory_prev_filedirectory = s_directory_new;
       }
       l_position_filedirectory=(long)(i_class+i_parent_dir_filedirectory);
       tree_builder_filedirectory.sequentiallyAddNextNode(l_position_filedirectory,i_level_filedirectory,this_classinfo);
       //---------------------------------------------
       // End:  'DIRECTORY,FILE'
       //---------------------------------------------
      }
      break;
     }
    }
   }
   catch (Exception e)
   {
    GPLog.androidLog(4,TAG+"onCreate creating [filling tree-nodes]",e);
   }
   // GPLog.androidLog(-1,TAG+" onCreate[" + tree_manager_filedirectory.toString()+ "]");
   // GPLog.androidLog(-1,TAG+" onCreate[filling ended]");
   new_Collapsible = true;
  }
  else
  {
   tree_manager_maptype = (TreeStateManager<Long>) savedInstanceState.getSerializable("treetree_manager_maptype");
   if (tree_manager_maptype == null)
   {
    tree_manager_maptype = new InMemoryTreeStateManager<Long>();
   }
   tree_manager_filedirectory = (TreeStateManager<Long>) savedInstanceState.getSerializable("treetree_manager_filedirectory");
   if (tree_manager_filedirectory == null)
   {
    tree_manager_filedirectory = new InMemoryTreeStateManager<Long>();
   }
   new_TreeType = (TreeType) savedInstanceState.getSerializable("use_treeType");
   if (new_TreeType == null)
   {
    new_TreeType = TreeType.FILEDIRECTORY;
   }
   new_Collapsible = savedInstanceState.getBoolean("collapsible");
  }
  try
  {
   setContentView(R.layout.mapsdir_treeview);
   this_treeViewList = (TreeViewList) findViewById(R.id.mapsdir_treeview);
   setTreeAdapter(new_TreeType);
   setCollapsible(new_Collapsible);
   registerForContextMenu(this_treeViewList);
  }
  catch (Exception e)
  {
   GPLog.androidLog(4,TAG+"onCreate creating [this_treeViewList_row_Adapter]",e);
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
  this_treeViewList.setOnItemClickListener(new OnItemClickListener()
  {
   public void onItemClick(AdapterView parent,View this_view,int position,long id_node)
   {
    TreeNodeInfo<Long> node_info =null;
    switch (use_treeType)
    {
     case MAPTYPE:
      node_info = tree_manager_maptype.getNodeInfo(id_node);
     break;
     case FILEDIRECTORY:
     default:
      node_info = tree_manager_filedirectory.getNodeInfo(id_node);
     break;
    }
    GPLog.androidLog(-1,TAG+" onItemClick["+use_treeType+"] [" + node_info.getLongText()+ "] id_node["+id_node+"]");
    if ((node_info != null) && (!node_info.isWithChildren()))
    {
     return_load_file(id_node);
    }
   }
  });
  // Long-press opens a context menu
  // setContentView(R.layout.mapsdir_treeview_item_fields);
  // ImageView this_view = (ImageView) findViewById(R.id.mapsdir_treeview_item_fields_options_button);
  // registerForContextMenu(this_view);
 }
 @Override
 protected void onSaveInstanceState(final Bundle outState)
 {
  outState.putSerializable("treetree_manager_maptype", tree_manager_maptype);
  outState.putSerializable("treetree_manager_filedirectory", tree_manager_filedirectory);
  outState.putSerializable("use_treeType", use_treeType);
  outState.putBoolean("collapsible", this.collapsible);
  super.onSaveInstanceState(outState);
 }
 protected final void setTreeAdapter(final TreeType new_TreeType)
 {
  this.use_treeType = new_TreeType;
  switch (use_treeType)
  {
   case MAPTYPE:
   {
    if (treeview_maptype == null)
    {
     treeview_maptype = new MapTypeTreeViewAdapter(this,selected_nodes,tree_manager_maptype,MAX_LEVEL_NUMBER);
    }
    this_treeViewList.setAdapter(treeview_maptype);
   }
   break;
   case FILEDIRECTORY:
   default:
   {
    if (treeview_filedirectory == null)
    {
     treeview_filedirectory = new FileDirectoryTreeViewAdapter(this,selected_nodes,tree_manager_filedirectory,MAX_LEVEL_NUMBER);
    }
    this_treeViewList.setAdapter(treeview_filedirectory);
   }
   break;
  }
  this_treeViewList.setOnItemClickListener(new OnItemClickListener()
  {
   public void onItemClick(AdapterView parent,View this_view,int position,long id_node)
   {
    TreeNodeInfo<Long> node_info =null;
    switch (use_treeType)
    {
     case MAPTYPE:
      node_info = tree_manager_maptype.getNodeInfo(id_node);
     break;
     case FILEDIRECTORY:
     default:
      node_info = tree_manager_filedirectory.getNodeInfo(id_node);
     break;
    }
    // GPLog.androidLog(-1,TAG+" onItemClick["+use_treeType+"] [" + node_info.getLongText()+ "] id_node["+id_node+"]");
    if ((node_info != null) && (!node_info.isWithChildren()))
    {
     return_load_file(id_node);
    }
   }
  });
 }
 protected final void setCollapsible(final boolean new_Collapsible)
 {
  this.collapsible = new_Collapsible;
  this_treeViewList.setCollapsible(this.collapsible);
 }
 @Override
 public boolean onCreateOptionsMenu(final Menu menu)
 {
  menu.add(0,id_mapsdir_treeview_filedirectory_menu,0,R.string.mapsdir_treeview_filedirectory_menu);
  menu.add(0,id_mapsdir_treeview_maptype_menu,0,R.string.mapsdir_treeview_maptype_menu);
  menu.add(0,id_mapsdir_treeview_expand_all_menu,0,R.string.mapsdir_treeview_expand_all_menu);
  menu.add(0,id_mapsdir_treeview_collapse_all_menu,0,R.string.mapsdir_treeview_collapse_all_menu);
  menu.add(0,id_mapsdir_treeview_collapsible_menu_disable,0,R.string.mapsdir_treeview_collapsible_menu_disable);
  return true;
 }
 @Override
 public boolean onPrepareOptionsMenu(final Menu menu)
 {
  final MenuItem collapsibleMenu = menu.findItem(id_mapsdir_treeview_collapsible_menu_disable);
  if (collapsible)
  {
   collapsibleMenu.setTitle(R.string.mapsdir_treeview_collapsible_menu_disable);
   collapsibleMenu.setTitleCondensed(getResources().getString(R.string.mapsdir_treeview_collapsible_menu_disable_condensed));
  }
  else
  {
   collapsibleMenu.setTitle(R.string.mapsdir_treeview_collapsible_menu_enable);
   collapsibleMenu.setTitleCondensed(getResources().getString(R.string.mapsdir_treeview_collapsible_condensed_enable));
  }
  MenuItem mapsdirMenu=null;
  switch (use_treeType)
  {
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
 public boolean onOptionsItemSelected(final MenuItem item)
 {
  switch (item.getItemId())
  {
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
    switch (use_treeType)
    {
     case MAPTYPE:
     {
      tree_manager_maptype.expandEverythingBelow(null);
     }
     break;
     case FILEDIRECTORY:
     {
      tree_manager_filedirectory.expandEverythingBelow(null);
     }
     break;
    }
   break;
   case id_mapsdir_treeview_collapse_all_menu:
    switch (use_treeType)
    {
     case MAPTYPE:
     {
      tree_manager_maptype.collapseChildren(null);
     }
     break;
     case FILEDIRECTORY:
     {
      tree_manager_filedirectory.collapseChildren(null);
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
 public void onCreateContextMenu(final ContextMenu menu, final View this_view, final ContextMenuInfo menuInfo)
 {
  final AdapterContextMenuInfo adapterInfo = (AdapterContextMenuInfo) menuInfo;
  long id_node = adapterInfo.id;
  TreeNodeInfo<Long> node_info = null;
  switch (use_treeType)
  {
   case MAPTYPE:
   {
    node_info = tree_manager_maptype.getNodeInfo(id_node);
   }
   break;
   case FILEDIRECTORY:
   {
    node_info = tree_manager_filedirectory.getNodeInfo(id_node);
   }
   break;
  }
  menu.add(0,id_mapsdir_context_menu_expand_all,0,R.string.mapsdir_treeview_expand_all_menu);
  menu.add(0,id_mapsdir_context_menu_expand_item,0,R.string.mapsdir_treeview_expand_menu);
  menu.add(0,id_mapsdir_context_menu_collapse,0,R.string.mapsdir_treeview_collapse_all_menu);
  if (node_info.isWithChildren())
  {
   if (node_info.isExpanded())
   {
    menu.findItem(id_mapsdir_context_menu_expand_item).setVisible(false);
    menu.findItem(id_mapsdir_context_menu_expand_all).setVisible(false);
   }
   else
   {
    menu.findItem(id_mapsdir_context_menu_collapse).setVisible(false);
   }
  }
  else
  {
   menu.findItem(id_mapsdir_context_menu_expand_item).setVisible(false);
   menu.findItem(id_mapsdir_context_menu_expand_all).setVisible(false);
   menu.findItem(id_mapsdir_context_menu_collapse).setVisible(false);
  }
  switch (this_view.getId())
  {
   case id_mapsdir_treeview_item_fields_options_button:
   break;
   default:
    // changing color does not work [ffff99,#EDDA74]
    // String s_Title_Text="<font color='#EDDA74'><center> --> "+node_info.getShortText()+"</center></font>";
    // menu.setHeaderTitle(Html.fromHtml(s_Title_Text));
    String s_Title_Text="["+node_info.getTypeText()+"] "+node_info.getShortText();
    menu.setHeaderTitle(s_Title_Text);
    if (MapsDirTreeViewList.b_properties_file)
    {
     menu.add(0,id_mapsdir_context_menu_properties,0,R.string.mapsdir_treeview_properties_menu);
    }
    if (MapsDirTreeViewList.b_edit_file)
    {
     menu.add(0,id_mapsdir_context_menu_properties_edit,0,R.string.mapsdir_treeview_properties_edit_menu);
    }
    if (MapsDirTreeViewList.b_delete_file)
    {
     menu.add(0,id_mapsdir_context_menu_properties_delete,0,R.string.mapsdir_treeview_delete_menu);
    }
    menu.add(0,id_mapsdir_context_menu_load,0,R.string.mapsdir_treeview_load_menu);
    // GPLog.androidLog(-1,TAG+" onCreateContextMenu ["+this_view.getId()+"] mapsdir_treeview_item_fields_*[" + node_info.getLongText()+ "] properties["+MapsDirTreeViewList.b_properties_file+"] edit["+MapsDirTreeViewList.b_edit_file+"] delete["+MapsDirTreeViewList.b_delete_file+"]");
   break;
  }
  super.onCreateContextMenu(menu, this_view, menuInfo);
 }
 @Override
 public boolean onContextItemSelected(final MenuItem item)
 {
  final AdapterContextMenuInfo info = (AdapterContextMenuInfo) item.getMenuInfo();
  final long id_node = info.id;
  TreeNodeInfo<Long> node_info = null;
  switch (use_treeType)
  {
   case MAPTYPE:
   {
    node_info = tree_manager_maptype.getNodeInfo(id_node);
   }
   break;
   case FILEDIRECTORY:
   {
    node_info = tree_manager_filedirectory.getNodeInfo(id_node);
   }
   break;
  }
  switch (item.getItemId())
  {
   case id_mapsdir_context_menu_collapse:
    // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_collapse[" + item.getTitle()+ "]");
    switch (use_treeType)
    {
     case MAPTYPE:
     {
      tree_manager_maptype.collapseChildren(id_node);
     }
     break;
     case FILEDIRECTORY:
     {
      tree_manager_filedirectory.collapseChildren(id_node);
     }
     break;
    }
   break;
   case id_mapsdir_context_menu_expand_all:
    // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_expand_all[" + item.getTitle()+ "]");
    switch (use_treeType)
    {
     case MAPTYPE:
     {
      tree_manager_maptype.expandEverythingBelow(id_node);
     }
     break;
     case FILEDIRECTORY:
     {
      tree_manager_filedirectory.expandEverythingBelow(id_node);
     }
     break;
    }
   break;
   case id_mapsdir_context_menu_expand_item:
    // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_expand_item[" + item.getTitle()+ "]");
    switch (use_treeType)
    {
     case MAPTYPE:
     {
      tree_manager_maptype.expandDirectChildren(id_node);
     }
     break;
     case FILEDIRECTORY:
     {
      tree_manager_filedirectory.expandDirectChildren(id_node);
     }
     break;
    }
   break;
   case id_mapsdir_context_menu_load:
    // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_load[" + item.getTitle()+ "] id["+id+"]");
    return_load_file(id_node);
   break;
   case id_mapsdir_context_menu_properties:
    // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_properties[" + item.getTitle()+ "] id["+id+"]");
    switch (use_treeType)
    {
     case MAPTYPE:
     {
      // tree_manager_maptype.removeNodeRecursively(id_node);
     }
     break;
     case FILEDIRECTORY:
     {
      // tree_manager_filedirectory.removeNodeRecursively(id_node);
     }
     break;
    }
   break;
   case id_mapsdir_context_menu_properties_edit:
    // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_properties[" + item.getTitle()+ "] id["+id+"]");
    switch (use_treeType)
    {
     case MAPTYPE:
     {
      // tree_manager_maptype.removeNodeRecursively(id_node);
     }
     break;
     case FILEDIRECTORY:
     {
      // tree_manager_filedirectory.removeNodeRecursively(id_node);
     }
     break;
    }
   break;
   case id_mapsdir_context_menu_properties_delete:
    // GPLog.androidLog(-1,TAG+" onContextItemSelected mapsdir_context_menu_delete[" + item.getTitle()+ "]");
    switch (use_treeType)
    {
     case MAPTYPE:
     {
      tree_manager_maptype.removeNodeRecursively(id_node);
     }
     break;
     case FILEDIRECTORY:
     {
      tree_manager_filedirectory.removeNodeRecursively(id_node);
     }
     break;
    }
   break;
   default:
    // GPLog.androidLog(-1,TAG+" onContextItemSelected unknown["+item.getItemId()+"] [" + item.getTitle()+ "]");
    return super.onContextItemSelected(item);
  }
  return true;
 }
 protected final void return_load_file(final long id_node)
 {
  TreeNodeInfo<Long> node_info = null;
  switch (use_treeType)
  {
   case MAPTYPE:
   {
    node_info = tree_manager_maptype.getNodeInfo(id_node);
   }
   break;
   case FILEDIRECTORY:
   {
    node_info = tree_manager_filedirectory.getNodeInfo(id_node);
   }
   break;
  }
  if (node_info != null)
  {
   Intent send_intent=new Intent();
   s_file_name=node_info.getLongText();
   s_file_type=node_info.getTypeText();
   send_intent.putExtra(SELECTED_FILE,s_file_name);
   send_intent.putExtra(SELECTED_TYPE,s_file_type);
   selected_classinfo=node_info.getClassNodeInfo();
   send_intent.putExtra(SELECTED_CLASSINFO,selected_classinfo.toString());
   GPLog.androidLog(-1,TAG+" return_load_file mapsdir_context_menu_load selected_file[" + s_file_name+ "] type["+s_file_type+"]");
   setResult(RESULT_OK,send_intent);
  }
  finish();
 }
}

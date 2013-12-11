package eu.geopaparazzi.mapsforge.mapsdirmanager.treeview;
import java.io.File;
import java.util.Comparator;
import eu.geopaparazzi.mapsforge.mapsdirmanager.treeview.ClassNodeInfoSort;
import eu.geopaparazzi.library.database.GPLog;
/**
 * Information about the class using the node.
 *
 * @param <T>
 *            type of the id for the tree
 */
public class ClassNodeInfo<T>
{
 private final T id;
 private final int i_type;
 private final boolean b_file;
 private int i_level=0;
 private File file_path;
 private final String s_file_path;
 private final String s_type;
 private final String s_short_text;
 private final String s_long_text;
 private final String s_short_description;
 private final String s_long_description;
 private final String s_class_name;
 private final String s_bounds;
 private final String s_center;
 private final String s_zoom_levels;
 private double centerX=0.0; // wsg84
 private double centerY=0.0; // wsg84
 private double bounds_west=0.0; // wsg84
 private double bounds_east=0.0; // wsg84
 private double bounds_north=0.0; // wsg84
 private double bounds_south=0.0; // wsg84
 private int minZoom=0;
 private int maxZoom=22;
 private int i_enabled=0;
 private boolean b_valid_bounds=false;
 /**
  * Creates the class node information.
  *
  * @param id
  *            id of the node
  * @param type
  *            type of class
  * @param s_type
  *            type to be retrieved [as text : file.extention]
  * @param s_class_name
  *            short text to be shown
  * @param s_file_path [as file-name with path [vector + table and field-name]]
  *           unique text to be shown
  * @param s_short_text [as file-name without path]
  *            short text to be shown [vector: database-file without path]
  * @param s_long_text [as file-name with path]
  *            long text to be retrieved
  * @param s_short_description [as file-name without path]
  *            short discription to be shown
  * @param s_long_description [as file-name with path]
  *            long discription to be retrieved [vector-table=table-name]
  *
  */
 public ClassNodeInfo(final T id, int type,String s_type,String s_class_name,
         String s_file_path,String s_short_text,String s_long_text,
         String s_short_description,String s_long_description,
         String s_bounds,String s_center,String s_zoom_levels)
 {
  //   mapsdir_classinfo=new ClassNodeInfo(0,-1,"directory","",s_map_file,s_map_file,s_map_file,s_map_file,s_map_file);
  super();
  this.id = id;
  this.file_path=new File(s_file_path);
  if ((!this.file_path.isAbsolute()) && (this.file_path.exists()))
  { // Where possible use only absolute path
   s_file_path=this.file_path. getAbsolutePath();
   this.file_path=this.file_path.getAbsoluteFile();
  }
  this.s_file_path=s_file_path;
  if (this.file_path.exists())
  {
   this.b_file=this.file_path.isFile();
  }
  else
  { // something like '/ALBUM/ARTIST/something.mp3', which may be for a media listing - not a true file-structure
   this.b_file=this.file_path.getName().lastIndexOf('.') != -1;
  }
  // set default values if empty
  if (s_type.equals(""))
  {
   s_type="file";
   if (!this.b_file)
    s_type="directory";
  }
  if (type < 0)
  {
   type=1000; // unknown
   if (s_type.equals("directory"))
   {
    type=10;
   }
   if (s_type.equals("file"))
   {
    type=101;
   }
  }
  if (s_short_text.equals(""))
  {
   s_short_text=getName();
  }
  if (s_long_text.equals(""))
  {
   s_long_text=getFileNamePath();
  }
  if (s_short_description.equals(""))
  {
   s_short_description=getName();
  }
  if (s_long_description.equals(""))
  {
   s_long_description=getFileNamePath();
  }
  this.i_type = type;
  this.s_type=s_type;
  //  MediaStore.Audio.Media.ALBUM,
  //  MediaStore.Audio.Media.ARTIST,
  //  MediaStore.Audio.Media.COMPOSER,
  //  MediaStore.Audio.Media.TITLE,
  this.s_class_name= s_class_name;
  this.s_short_text=s_short_text;
  this.s_long_text=s_long_text;
  this.s_short_description=s_short_description;
  this.s_long_description=s_long_description;
  this.s_bounds=s_bounds;
  this.s_center=s_center;
  this.s_zoom_levels=s_zoom_levels;
  setPositionValues();
 }
  // -----------------------------------------------
 /**
   * Set Position Values
   * - strick checking is done, since anything could be sent
   * -- indended for use with SpatialVectorTable
   * @param bounds_zoom 5 values: west,south,east,north wsg84 values and zoom-level
   * @param i_check_enabled 0: return all ; 1= return only those that are enabled
   * @return i_rc 0: conditions not fullfilled ; 1=compleatly inside valid bounds ; 2=partially inside valid bounds ; -1 zoom invalid ; -2 bounds invalid
   */
 public int checkPositionValues(double[] bounds_zoom, int i_check_enabled)
 {
  int i_rc=0;
  if ((i_check_enabled == 1) && (getEnabled() != 1))
  { // this is not enabled, so return now [checking is desired]
   return i_rc;
  }
  if (bounds_zoom == null)
  { // no checking of any kind are done
   i_rc=10;
   return i_rc;
  }
  if (bounds_zoom.length == 5)
  { // we must have 5 values: west,south,east,north wsg84 values and zoom-level
   int i_zoom=(int)bounds_zoom[4];
   // String s_parms=bounds_zoom[0]+","+bounds_zoom[1]+","+bounds_zoom[2]+","+bounds_zoom[3]+";"+i_zoom;
   if ((i_zoom >= minZoom) && (i_zoom <= maxZoom))
   { // inside valid zoom-levels
    if (((bounds_zoom[0] >= bounds_west) && (bounds_zoom[2] <= bounds_east)) &&
         ((bounds_zoom[1] >= bounds_south) && (bounds_zoom[3] <= bounds_north)))
    { // compleatly inside valid bounds
     i_rc=1;
    }
    else
    {
     if (((bounds_zoom[0] >= bounds_west) && (bounds_zoom[0] <= bounds_east)) ||
          ((bounds_zoom[2] >= bounds_west) && (bounds_zoom[2] <= bounds_east)) ||
          ((bounds_zoom[1] >= bounds_south) && (bounds_zoom[1] <= bounds_north)) ||
          ((bounds_zoom[3] >= bounds_south) && (bounds_zoom[3] <= bounds_north)))
     { // partially inside valid bounds
      i_rc=2;
     }
     else
     {
      i_rc=-2;
     }
    }
   }
   else
   {
    i_rc=-1;
   }
   // GPLog.androidLog(-1, "ClassNodeInfo i_rc="+i_rc+" enabled["+getEnabled()+"] ["+s_parms+"] [" + toString() + "]");
  }
  return i_rc;
 }
 // -----------------------------------------------
 /**
   * Set Position Values
   * - strick checking is done, since anything could be sent
   * -- indended for use with SpatialVectorTable
   * @return nothing
   */
 private void setPositionValues()
 {
  String[] sa_string = null;
  if ((!this.s_bounds.equals("")) && (this.s_bounds.indexOf(",") != -1))
  {
   sa_string = this.s_bounds.split(",");
   if (sa_string.length == 4)
   {
    double[] bounds = new double[]{0.0, 0.0, 0.0, 0.0};
    try
    {
     bounds[0] = Double.parseDouble(sa_string[0]);
     bounds[1] = Double.parseDouble(sa_string[1]);
     bounds[2] = Double.parseDouble(sa_string[2]);
     bounds[3] = Double.parseDouble(sa_string[3]);
     if (((bounds[0] >= -180.0) && (bounds[0]) <= 180.0) && ((bounds[2] >= -180.0) && (bounds[2] <= 180.0)) &&
          ((bounds[1] >= -85.05113) && (bounds[1] <= 85.05113)) && ((bounds[3] >= -85.05113) && (bounds[3] <= 85.05113)))
     {
      bounds_west=bounds[0]; // wsg84
      bounds_east=bounds[2]; // wsg84
      bounds_north=bounds[3]; // wsg84
      bounds_south=bounds[1]; // wsg84
      b_valid_bounds=true;
      centerX = (bounds[0] + (bounds[2] - bounds[0]) / 2);
      centerY = (bounds[1] + (bounds[3] - bounds[1]) / 2);
     }
    }
    catch (NumberFormatException e)
    {
    }
   }
  }
  if ((!this.s_center.equals("")) && (this.s_center.indexOf(",") != -1))
  {
   sa_string = this.s_center.split(",");
   if (sa_string.length == 2)
   {
    double[] center = new double[]{0.0, 0.0};
    try
    {
     center[0] = Double.parseDouble(sa_string[0]);
     center[1] = Double.parseDouble(sa_string[1]);
     if (((center[0] >= -180.0) && (center[0] <= 180.0)) && ((center[1] >= -85.05113) && (center[1] <= 85.05113)))
     {
      centerX = center[0];
      centerY = center[1];
     }
    }
    catch (NumberFormatException e)
    {
    }
   }
  }
  if ((!this.s_zoom_levels.equals("")) && (this.s_zoom_levels.indexOf("-") != -1))
  {
   sa_string = this.s_zoom_levels.split("-");
   if (sa_string.length == 2)
   {
    int[] zoom = new int[]{0, 22};
    try
    {
     zoom[0] = Integer.parseInt(sa_string[0]);
     zoom[1] = Integer.parseInt(sa_string[1]);
     if (((zoom[0] >= minZoom) && (zoom[0] <= maxZoom)) && ((zoom[1] >= minZoom) && (zoom[1] <= maxZoom)))
     {
      minZoom=zoom[0];
      maxZoom=zoom[1];
     }
    }
    catch (NumberFormatException e)
    {
    }
   }
  }
 }
 public T getId()
 {
  return id;
 }
 public boolean exists()
 {
  return this.b_file;
 }
 public String getFileNamePath()
 { // vector: database-file with path + / + table-name +/ + / field-name
  if (this.file_path != null)
   return this.file_path.getAbsolutePath();
  else
   return s_file_path;
 }
 public String getFileName()
 {
  if (this.file_path != null)
   return this.file_path.getName();
  else
   return s_file_path;
 }
 public String getName()
 {
  if (this.b_file)
   return this.file_path.getName().substring(0,this.file_path.getName().lastIndexOf("."));
  else
   return this.file_path.getName();
 }
 public File getFilePath()
 {
  return this.file_path; // file_path.;
 }
 public String getClassName()
 {
  return s_class_name;
 }
 public String getShortText()
 { // vector: database-file without path
  return s_short_text;
 }
 public String getLongText()
 { // vector: table-name
  return s_long_text;
 }
 public String getShortDescription()
 { // vector: field-name
  return s_short_description;
 }
 public String getLongDescription()
 { // vector: table-name + / + field-name
  return s_long_description;
 }
 public String getTypeText()
 {
  return s_type;
 }
 public String getBounds()
 {
  return s_bounds;
 }
 public String getCenter()
 {
  return s_center;
 }
  public String getZoom_Levels()
 {
  return s_zoom_levels;
 }
 public int getType()
 {
  return i_type;
 }
 public int getEnabled()
 {
  return i_enabled;
 }
 public void setEnabled(int i_enabled)
 {
  this.i_enabled=i_enabled;
 }
 public int getLevel()
 {
  return i_level;
 }
 public void setLevel(int i_level)
 {
  this.i_level=i_level;
 }
 @Override
 public String toString()
 {
  return "ClassNodeInfo [id=" + id + ", type=" + i_type + ", level=" + i_level  + ", enabled=" + i_enabled
              + ", ClassName=" + s_class_name + " FileNamePath["+getFileNamePath()+"] "
              + ", type=" + s_type + " short_text["+s_short_text+"] long_text["+s_long_text+"]" + " short_description["+s_short_description+"] long_description["+s_long_description+"]"
              + ", bounds["+s_bounds+"]" + " center["+s_center+"] zoom_levels["+s_zoom_levels+"]";
 }
 public static Comparator<ClassNodeInfo> getComparator(SortParameter... sortParameters)
 {
  return new ClassNodeInfoSort(sortParameters);
 }
 public static  enum SortParameter
 {
  SORT_TYPE_TEXT,SORT_DIRECTORY,SORT_FILE,SORT_FILENAME_PATH
 }
}


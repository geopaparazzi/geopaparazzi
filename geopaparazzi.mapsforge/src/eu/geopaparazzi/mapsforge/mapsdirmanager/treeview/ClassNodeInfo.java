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
  * @param s_short_text [as file-name without path]
  *            short text to be shown
  * @param s_long_text [as file-name with path]
  *            long text to be retrieved
  * @param s_short_description [as file-name without path]
  *            short discription to be shown
  * @param s_long_description [as file-name with path]
  *            long discription to be retrieved
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
 }
 public T getId()
 {
  return id;
 }
 public String getFileNamePath()
 {
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
 {
  return s_short_text;
 }
 public String getLongText()
 {
  return s_long_text;
 }
 public String getShortDescription()
 {
  return s_short_description;
 }
 public String getLongDescription()
 {
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
  return "ClassNodeInfo [id=" + id + ", type=" + i_type + ", level=" + i_level
              + ", ClassName=" + s_class_name + " FileNamePath["+getFileNamePath()+"] PATH[" + getFilePath().getAbsolutePath()+"]"
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


.. index:: data preparation
.. _datapreparation:

Handling datasets from and for Geopaparazzi
===================================================

Several applications can be used to prepare and evaluate 
datasets for Geopaparazzi. The current ones known are:

* STAGE
* Spatialite GUI
* GDAL
* QGIS
* GRASS

.. index:: STAGE 0.7

STAGE 0.7
--------------

STAGE (Spatial Toolbox And Geoscripting Environment) is a standalone application 
dedicated to spatial analyses.

Related to Geopaparazzi, it contains functionalities to:

* Create **mbtiles** files from raster and vector sources.  
* Export geopaparazzi projects (version 3 or 4) to **shapefiles**, notes, profiles, and other details. 
* Convert geopaparazzi projects from version 3 to version 4.

Installation
++++++++++++++++++

STAGE can be `downloaded from the github releases page <http://git.io/stage_releases>`_

Steps:

* copy the zip file in a folder:

 - stage_0.7_win64.zip for 64 bits windows operating systems
 - stage_0.7_win32.zip for 32 bits windows operating systems
 - stage_0.7_lin64.zip for 64 bits linux operating systems

* unzip the file in a folder
* enter the unzipped folder
* run the Stage application (ex. stage.exe for windows)

.. figure:: 07_maps_handling/01_stage.png
   :align: center
   :width: 600px

The *mobile* section contains modules related to Geopaparazzi.

.. figure:: 07_maps_handling/02_stage.png
   :align: center
   :width: 600px

.. _stagembtiles:

MBTiles creation
++++++++++++++++++++++

For the creation of an MBTiles file from a set of GIS data, as shapefiles 
and tiff rasters, select the module: **GeopaparazziMapsCreator**

.. figure:: 07_maps_handling/03_stage.png
   :align: center
   :width: 600px

The user will be able to add:

* up to 2 raster tiff or asc maps
* up to 5 vector shapefiles
* define the name of the new mbtiles dataset
* define the minimum and maximum wanted zoomlevel (zoomlevels > 18 start to take long time
  to build because of the large number of tiles generated)
* the image type to use:
  - jpg: this should be used when photographic data are used (ex. aerial images)
  - png: this should be used when images are used (ex. technical maps)
* the output folder, inside which the database will be created

Once the parameters are set, the module can be launched by pushing the 
green **play button** in the top right toolbar.

In case of big data the user should consider to set the memory put 
available to the run module. This is done in the lower left combobox 
labeled **Memory [MB]**. The number to set is considered to be MegaBytes.
A safe value to use is a bit less than the amount of RAM available 
on the computer.

For windows 32bit machines it is not possible to use more than 1000 MB due
to technical limitations of Java itself.


Converstion of Geopaparazzi data to GIS data
++++++++++++++++++++++++++++++++++++++++++++++++

Through the module **Geopaparazzi4Converter** it is possible to export 
data from a Geopaparazzi project database.

.. figure:: 07_maps_handling/04_stage.png
   :align: center
   :width: 600px

The only parameter to set are the input Geopaparazzi database path and the 
output folder. It is also possible to toggle the creation of some of the data 
contained in the database.

To run the module, refer to the :ref:`mbtiles section <stagembtiles>`.

How are the data are exported from the Geopaparazzi database?
The following is created:

- point shapefiles for each type of note also complex forms
- point shapefiles with reference to pictures taken and sketches
- all pictures are exported to a media folder
- line and points shapefiles for log lines and points
- profile charts and csv of log data
- a simple text project metadata file

The exported data, viewed inside a GIS (in this case `uDig <http://www.udig.eu>`_)
look like the following:

.. figure:: 07_maps_handling/12_udig.png
   :align: center
   :width: 600px

.. index:: spatialite gui
.. _spatialitegui:

Spatialite GUI
---------------------

The Spatialite GUI can be used to create spatialite databases from shapefiles.

You can find the application on the spatialite homepage, at the time of writing 
a good version for windows is version 1.7.1 available in `this download 
area <http://www.gaia-gis.it/gaia-sins/windows-bin-amd64-prev/>`_.

Open it and find yourself with:

.. figure:: 07_maps_handling/01_spl.png
   :align: center
   :width: 600px


We now create a new empty database in which to load the shapefile data:

.. figure:: 07_maps_handling/02_spl.png
   :align: center
   :width: 600px

You will be asked to save the database somewhere on disk. Once done, 
you should find yourself with something like this, but with different path:

.. figure:: 07_maps_handling/03_spl.png
   :align: center
   :width: 600px

To then load a shapefil, locate the *Load Shapefile* icon:

.. figure:: 07_maps_handling/04_spl.png
   :align: center
   :width: 600px

In this example I will import a set of shapefiles from the `Natural 
Earth dataset <http://www.naturalearthdata.com/>`_, in particular the following ones:

.. figure:: 07_maps_handling/05_spl.png
   :align: center
   :width: 600px

that `can be found here <http://www.naturalearthdata.com/downloads/10m-cultural-vectors/>`_. 

The import dialog is the important one to fill the right way:

.. figure:: 07_maps_handling/06_spl.png
   :align: center
   :width: 600px

The really important things to take care of, are underlined in red:

* the SRID, i.e. the EPSG code of the data projection. If that one is not right, then you will not be able to see the data in geopaparazzi. Don't even hope in miracles!
* the Charset Encoding. Make sure to choose the right one. For example Japanese people might want to choose SHIFT_JIS if they want to see the labels rendered properly
* force the creation of the spatial index

If you then the push the ok button, you should find yourself with an ok message like this after the import:

.. figure:: 07_maps_handling/07_spl.png
   :align: center
   :width: 600px

You are almost there, one last step to go.

Right-click on the database name and select the **Update Layer Statistics** command. 

.. figure:: 07_maps_handling/08_spl.png
   :align: center
   :width: 600px

Depending on the amount of data it should keep your harddisk working for a bit. 
Don't think it finished unless you see a result like the following:

.. figure:: 07_maps_handling/09_spl.png
   :align: center
   :width: 600px

Once this result appears to you, you are good to go.

Move the spatialite database to your device, fire up geopaparazzi and 
go directly to the :ref:`spatialite data view <spatialitedatalist>`.

You should see something like:

.. figure:: 07_maps_handling/10_spl.png
   :align: center
   :width: 300px

Enable the countries and places layer, maybe also activate the labels
 for the places layer:

.. figure:: 07_maps_handling/11_spl.png
   :align: center
   :width: 300px

.. index:: gdal

GDAL
----------

Creation of local tiles datasets
++++++++++++++++++++++++++++++++++++++

Geopaparazzi does not support reprojecting raster data sources on-the-fly, so the file
must be warped to the proper projection before using it. To do it you can use
`gdalwarp <http://www.gdal.org/gdalwarp.html>`_ command.

The target projection must be Google Web Mercator (EPSG code 3857); you must know 
also the source projection of the raster you are converting. As an example, if 
you have a WGS 84 projected (EPSG code 4326) input file, you will run this kind of command::

    gdalwarp -s_srs EPSG:4326 -t_srs EPSG:3857 -r bilinear input.tif output.tif

To create the tiles you can use `gdal2tiles.py <http://www.gdal.org/gdal2tiles.html>`_ script, 
using as input your Google Web Mercator projected raster file::

    gdal2tiles.py output.tif

It generates directory with TMS tiles, that you can use in Geopaparazzi. In the 
root of the this directory you will find "tilemapresource.xml" file
which contains all the information to build the .mapurl file:

.. code-block:: xml
   :emphasize-lines: 6,7

    <?xml version="1.0" encoding="utf-8"?>
    <TileMap version="1.0.0" tilemapservice="http://tms.osgeo.org/1.0.0">
      <Title>temp3.vrt</Title>
      <Abstract></Abstract>
      <SRS>EPSG:900913</SRS>
      <BoundingBox minx="46.39742402665929" miny="11.28858223249814" maxx="46.45081836101696" maxy="11.37616698902041"/>
      <Origin x="46.39742402665929" y="11.28858223249814"/>
      <TileFormat width="256" height="256" mime-type="image/png" extension="png"/>
      <TileSets profile="mercator">
        <TileSet href="12" units-per-pixel="38.21851413574219" order="12"/>
        <TileSet href="13" units-per-pixel="19.10925706787109" order="13"/>
        <TileSet href="14" units-per-pixel="9.55462853393555" order="14"/>
        <TileSet href="15" units-per-pixel="4.77731426696777" order="15"/>
        <TileSet href="16" units-per-pixel="2.38865713348389" order="16"/>
        <TileSet href="17" units-per-pixel="1.19432856674194" order="17"/>
        <TileSet href="18" units-per-pixel="0.59716428337097" order="18"/>
      </TileSets>
    </TileMap>

Note that the *BoundingBox* and *Origin* values created by *gdal2tiles* 
are have **x** and **y** values switched against how we need them::

      minx="11.28858223249814" 
      miny="46.39742402665929" 
      maxx="11.37616698902041" 
      maxy="46.45081836101696"
      x="11.28858223249814" 
      y="46.39742402665929"


Creation of MBTiles databases
++++++++++++++++++++++++++++++++++

This section is looking for an author.

.. index:: QGIS

QGIS
----------

The Geopaparazzi plugin
++++++++++++++++++++++++++++++++++

This section is looking for an author.

.. index:: GRASS

GRASS
-----------

v.in.geopaparazzi
+++++++++++++++++++++

This section is looking for an author.

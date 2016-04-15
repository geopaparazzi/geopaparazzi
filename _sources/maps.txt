.. index:: maps
.. _maps:

Supported datasets
===================

Geopaparazzi's mapview supports several map-types: vector or raster, editable or static. Also,
maps can be pulled live from an Internet server, or be generated locally from a map data file for
offline operation. You can switch between these modes in settings.

There are two types of maps in Geopaparazzi: 

 * **basemaps**: the background map in the map view
 * **overlay maps**: vector maps that can be shown on top of the basemaps

If you add local/offline maps, the map data files need to be placed in the configured 
maps folder, which is a folder called *maps* in the **root of the sdcard** by default.
You can change this path in :ref:`settings <custommapsfolder>`.

The following sections will detail various map source types. In case you want to prepare 
tailored map datasets as map sources in one of below map formats, please refer to the 
:ref:`data preparation section <datapreparation>`.

Basemaps
-----------------

Basemaps can be of 4 different types:

* mapurls
* mbtiles
* mapsforge maps
* rasterlite2


Mapurls: Custom tiles based maps
++++++++++++++++++++++++++++++++++

Mapurl files are simple text files containing definitions of tile sources, either local or remote.
A mapurl file must have the file extention/suffix *.mapurl* to be recognised by Geopaparazzi.

Remote Tile sources
~~~~~~~~~~~~~~~~~~~~~~

By default, Geopaparazzi is configured to load map mapnik rendered tiles live from the Internet, from OpenStreetMap's tile server. Compare the contents of the included mapnik.mapurl file for how this is done. You can exchange the OSM tile server with any other tile server as long as it adheres to the Mapurl URL conventions. The basics are:

.. literalinclude:: /../../geopaparazzi-git/geopaparazzimapsforge/res/raw/mapnik.mapurl

The mandatory information is:

* the url of the tile server, having:

  - *ZZZ* instead of the zoom level
  - *XXX* instead of the tile column number
  - *YYY* instead of the tile row number
  
  This information can be tested also in a browser
  http://tile.openstreetmap.org/9/271/182.png has ZZZ=9, XXX=271 and YYY=182

* the minimum zoom level that is supported
* the maximum zoom level that is supported
* the center of the tile source
* the type fo tile server. Currently both `standard TMS <http://en.wikipedia.org/wiki/Tile_Map_Service>`_
  and google based numbering of the tiles is supported by the line:

  * type=tms
  * type=google

* the backup mbtiles path. This one is used to save downloaded tiles in the
  local mbtiles database

Also WMS works as remote source, as long as it can be accessed through an **EPSG:4326** projection. 

An example for the url part is::

    url=http://sdi.provincia.bz.it/geoserver/wms?LAYERS=inspire:OI.ORTHOIMAGECOVERAGE.2011&TRANSPARENT=true&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&STYLES=&EXCEPTIONS=application/vnd.ogc.se_inimage&SRS=EPSG:4326&BBOX=XXX,YYY,XXX,YYY&WIDTH=256&HEIGHT=256

Important here are:

* SRS=EPSG:4326
* BBOX=XXX,YYY,XXX,YYY

Geopaparazzi will cache/store downloaded Mapurl tiles in a local MBTiles SQLite file, so pre-fetched tiles will remain viewable when Geopaparazzi is operated disconnected from a remote Mapurl source (in offline operation).

Local Tile sources
~~~~~~~~~~~~~~~~~~~~~~

Just like your mapurl configuration may reference an online/remote tile
sources, your mapurl setting may reference a local tile source for offline
use. This way it's possible to load on any smarthphone complex maps as for 
example the following map that has a technical basemap with shapefiles 
overlayed in transparency

To be able to load such maps, one needs to prepare the tiles properly.
This can be done via in several ways as explained in the :ref:`data 
preparation section <datapreparation>`.

The tile folder have then to be loaded in the *maps* folder together
with the description of the tile source::

    url=mytilesfolder/ZZZ/XXX/YYY.png
    minzoom=12
    maxzoom=18
    center=11.40553 46.39478
    type=tms

Nothing changes against the description for the remote source apart of 
the url. The url in this case represents the relative path of the tiles 
folder starting from the *"maps"* folder.

**A note of warning:** Filesystems are know to have problems in handling
large amounts of very small files. This the exact case of local tile 
sources. If the dataset is large, it gets very hard to move the data 
from and to the device. Therefore this method, even if still supported, 
is flagged as deprecated. The **MBTiles** datasource, explained in the next 
section, should be used instead.
 

.. index:: mbtiles
.. _mbtiles:

MBTiles
+++++++++++++

MBTiles is a file format for storing map tiles in a single file. It is, technically, a SQLite database. 
See the `openstreetmap wiki <http://wiki.openstreetmap.org/wiki/MBTiles>`_ for more information.


.. index:: mapsforge maps
.. _mapsforgemaps:

Mapsforge maps
++++++++++++++++++

The mapsforge project provides free and open software for the rendering of maps based on OpenStreetMap. It developed an efficient binary format for storage of OSM map data (usually with file extension **.map**), and is offering country specific .map files for download. Geopaparazzi is able to render map tiles locally from .map files and will cache rendered tiles in a local MBTiles store.

Apart from Mapsforge itself, openandmaps is also offering .map files but with a different data bias and different render theme.

mapsforge
~~~~~~~~~~~~~

These are the standard maps generated, maintained and distributed by the `mapsforge <http://code.google.com/p/mapsforge/>`_
team and downloadable from `their server <http://download.mapsforge.org/>`_.

In their default style they kind of look like:

.. figure:: 06_maps/01_mapsforge_maps.png
   :align: center
   :width: 300px

openandmaps
~~~~~~~~~~~~~

`Openandromaps <http://www.openandromaps.org>`_ generates maps following the opencycle 
theme, with isolines and more hiking related stuff. The map files are larger but worth 
every byte. Their `download area is here <http://www.openandromaps.org/en/download.html>`_.

With the Oruxmaps theme that is available from the download area, the maps look like:

.. figure:: 06_maps/02_cycle_maps.png
   :align: center
   :width: 300px

Apply a render theme
~~~~~~~~~~~~~~~~~~~~~~~~

When rendereing mapforge tiles locally, Geopaparazzi applies render themes if they are
found on the disk. In order to be found, the render theme xml file needs to have the
same name as the map file. 
Ex, the above cycle map example has a::
    
    italy_cycle.map

and a::

    italy_cycle.xml

render theme file in the same folder as the map file itself.


.. index:: rasterlite2
.. _rasterlite2:

RasterLite2
+++++++++++++++

`RasterLite2 <https://www.gaia-gis.it/fossil/librasterlite2/wiki?name=librasterlite2>`_ is a raster 
format implemented in the `spatialite database <https://www.gaia-gis.it/fossil/libspatialite/index>`_.

At the time of writing it is released as development version and supported in Geopaparazzi for testing.


Overlay maps
-----------------

The only datasets that can be overlayed on top of basemaps are vector maps
coming from a `spatialite database <https://www.gaia-gis.it/fossil/libspatialite/index>`_.






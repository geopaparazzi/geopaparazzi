.. index:: main menu
.. _mainmenu:

The Main View Menu
====================

The main presents the following options:

* **Select tile source**: allows the selection of a map tile source to be used as base map
* **Create new project**: this will create a new project on the sdcard. A timestamp based name is proposed to the user
* **Load existing project**: gives the possibility to browse the filesystem to select an existing geopaparazzi project folder (the one with the database in it) to load it
* **Settings**: accesses the settings page
* **About**: visualises the about page, which contains infos, links and acknowledgements
* **Exit**: shuts down geopaparazzi

.. figure:: 04_mapviewmenu/01_menu.png
   :align: center
   :width: 300px

.. index:: select tile source
.. _selecttilesource:

Select tile source
-----------------------

From the main menu, when pushing the tile source button, the list of available tile sources are presented,
properly divided by path inside the maps folder on the device:

.. figure:: 04_mapviewmenu/02_select_tiles.png
   :align: center
   :width: 300px

Each map source has a name and a type description between brackets.

At the current time 4 types are supported:

* Map
* Mapurl
* MBTiles
* RasterLite

The :ref:`supported datasets <maps>` section lists and describes all supported 
map types.

Through the button at the top of the view the user can filter the source 
types to show.

.. figure:: 04_mapviewmenu/03_select_tiles.png
   :align: center
   :width: 300px

which makes searching simpler when a lot of sources are present on the device.

.. figure:: 04_mapviewmenu/04_select_tiles.png
   :align: center
   :width: 300px

A few map sources, as for example **mapnik.mapurl** and **opencycle.mapurl** are available by default.


To appear in the sources list, all the data sources need to be placed
in the **folder named "maps" in the root of the sdcard**. 
We will refer to it on this page as **maps** folder.

The user can change the location of the maps folder from the :ref:`settings`.

.. index:: create new project
.. _newproject:

Create a new project
---------------------

When a new project is created, the user is prompted to insert a name
for the new project file. This is the name that will be given to the 
database file that contains all data surveyed in Geopaparazzi.

.. figure:: 04_mapviewmenu/05_new_project.png
   :align: center
   :width: 300px

Once the name is defined, an new empty database is created and Geopaparazzi is 
restarted and opened loading the new created project.

.. index:: load project
.. _loadproject:

Load an existing project
--------------------------

Existing projects can be loaded through a simple file browser
from within Geoapaparazzi:

.. figure:: 04_mapviewmenu/06_load_project.png
   :align: center
   :width: 300px

The Geopaparazzi project are visualized with a different icon to
help the user to choose the proper files.


Settings & Preferences
------------------------

All settings and preferences are described in `the dedicated section <settings>`_.

About
-----------

The about page list information about the current version of 
Geopaparazzi, as well as information about the authors and 
contributors.

.. figure:: 04_mapviewmenu/07_about.png
   :align: center
   :width: 300px

Exit
--------------

The exit button closes Geopaparazzi and stoppes any ongoing logging and 
sensor activity.

This might seem obvious, but it is important to note that this is the 
**only way to properly close Geopaparazzi**. 

Pushing the home button of the device will not close Geopaparazzi, 
which will continue any activity started.

This is important, because it makes very long loggings possible even 
if interrupted by phone calls or other uses of the device.

Often users that ignore this, after pushing the 
home screen and thinking that Geopaparazzi has been closed, 
experience a faster battery drop, because of the active application 
in the background.


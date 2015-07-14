.. index:: mapview
.. _mapview:

The Map View
==============

The map view is the central view of the application. It can be used
to view the current gps position, several maps and datasets and
can be used to navigate.

.. figure:: 03_mapview/01_mapview.png
   :align: center
   :width: 300px

   The map view.

The map engine comes from the `mapsforge <http://code.google.com/p/mapsforge/>`_ project.

The :ref:`supported datasets <maps>` section lists and describes all supported 
map types. Just to cite a few, one can view:

* `OpenStreetMap <http://www.openstreetmap.org/>`_ maps both when online (downloading new tiles) 
  and offline (visualizing those that were cached during online mode)
* `mbtiles databases <http://www.mapbox.com/developers/mbtiles/>`_
* local and remote `TMS <http://wiki.osgeo.org/wiki/Tile_Map_Service_Specification>`_ tiles
* mapsforge vector files

.. index:: mapview tools
.. _mapviewtools:

Map view tools
--------------------

The map view has a set of tools that can be exploited to interact with the map. 

When first launched, the tools are already visible on the map. To have a 
cleaner mapview, the user can long-tap on the upper right menu button 
to toggle the visibility of the tools.

.. figure:: 03_mapview/02_mapview_tools_off.png
   :align: center
   :width: 300px
   
   The map view with disabled tools.
   

Available tools are:

* Left screen side

  - add note (the same that is accessed from the dashboard)
  - list all notes
  - add bookmark
  - list all bookmarks
  - activate the measure tool

* Right screen side

  - the context menu button
  - the zoom in and out buttons

* Lower center

  - battery status
  - center screen on GPS position and GPS status button
  - editing tools

.. figure:: 03_mapview/01_mapview.png
   :align: center
   :width: 300px

   The map view with all map tools visible.

Map navigation
------------------

The navigation of the map is probably the most basic and important part.

* **pan**: panning of the map, i.e. moving the map around is simply done
  by dragging the map with a finger. While panning, the current 
  longitude and latitude of the map center are displayed.
  
.. figure:: 03_mapview/29_pan_coords.png
   :align: center
   :width: 300px


* **zoom**: zoom in and out can be done in different ways. There are 
  the zoom in/out buttons at the lower right part of the map. It is also
  possible to zoom in by double-tapping on the map. Zoom in and out can
  also be achived through `pinch-zoom gestures <https://en.wikipedia.org/wiki/Multi-touch>`_.
* **center on gps**: through the lower center button it is possible 
  to center the map on the gps position. The button also shows the status of the GPS
  the same way as the icon :ref:`on the main view's action bar <actionbar>`.

Add Notes
------------------

.. figure:: 02_dashboard/03_notes.png
   :align: center
   :width: 300px

   The view from which notes are taken.

The add notes button opens the same notes view as the button on the dashboard.
There is only once difference. Since notes can be added both in the gps position
and in the middle of the map, to open the add note view from the dashboard, the GPS 
is required to have a fix.

Instead from the map view the user can open the add note view even without 
GPS signal, in which case he will be allowed to insert notes only in the 
map center position.

For further information about taking notes, visit the :ref:`notes section<notes>`.

.. index:: notes list
.. _noteslist:

The Notes List
------------------

The notes list shows all the available notes, both text and image notes.

.. figure:: 03_mapview/03_noteslist1.png
   :align: center
   :width: 300px

   The notes list view.


In the upper part there is a textbox that helps to filter out a particular note based on its name.

.. figure:: 03_mapview/04_noteslist_filter.png
   :align: center
   :width: 300px

   Notes can be filtered by text.

At the right side of the list 2 icon are available. 
The left one positions the map on the selected note's position.
The rigth one opens a menu:

.. figure:: 03_mapview/05_note_menu.png
   :align: center
   :width: 300px

   The note menu.

From the note menu the user is able to:

* share the note through social networks
* delete the note
* use the current note as a filter

.. figure:: 03_mapview/06_note_as_selection.png
   :align: center
   :width: 300px

   Example of using a note as filter for the list.

* access the submenu related to all notes

.. figure:: 03_mapview/07_notes_all_menu.png
   :align: center
   :width: 300px

   The submenu that considers all notes.

From the all-notes submenu the user can:
 
  - select all notes
  - invert the current selection
  - delete the selected notes

.. index:: add bookmarks
.. _addbookmarks:

Add Bookmark
------------------

Bookmarks are in a layer on their own, that contains saved settings of the current map view.
When a bookmark is added, the user is prompted to insert a name for the bookmark
or leave the generated name based on the current time and date.

.. figure:: 03_mapview/08_add_bookmark.png
   :align: center
   :width: 300px

   The add bookmark dialog.

When a bookmark is added , a small star is added on the map in the center of the screen.

.. figure:: 03_mapview/09_bookmark.png
   :align: center
   :width: 300px

   The bookmark star in the map.

Also the map bounds and zoom are saved. That way one can return to the scenario 
that the bookmark represents in any moment.

It is possible to tap on the bookmark to read its label.

.. figure:: 03_mapview/10_bookmark_open.png
   :align: center
   :width: 300px

   A tapped bookmark.


.. index:: bookmarks list
.. _bookmarkslist:

The Bookmarks List
------------------

The bookmarks list shows all the saved bookmarks.

In the upper part there is a textbox that helps to filter out a particular bookmark based on its name.

The user has 4 options, as the icons on each bookmark entry shows:

* go to the bookmark location
* add a proximity alert. In that case a dialog opens and asks for a radius in meters 
  to define the proximity area. Once the gps enters that area, the user will be notified
* rename the bookmark
* delete the bookmark

.. figure:: 03_mapview/11_bookmarks_list.png
   :align: center
   :width: 300px

   The list of bookmarks.

.. index:: measure tool
.. _measuretool:

Measure tool
------------------

Activating the measure tool puts the app in measure mode. This mode disables 
the ability to pan the map while enabling
the possibility to draw a line on the map and measure the line drawn. 

The *approximate* (the distance is calculated without considering
elevation deltas and with the coordinate picking precision of a finger 
on a screen) distance is shown in the upper part of the map view.

When active, the measure tool has a red colored icon, when inactive the icon is green instead.

.. figure:: 03_mapview/12_measure_tool.png
   :align: center
   :width: 300px

   An example of qualitative measurement on the map.

.. index:: gps log analysis tool
.. _gpsloganalysistool:

Gps log analysis tool
-------------------------

Apart of charting the log it is possible to analyze the various positions of 
a gps log. The tool to do so is hidden behind the editing tools button.

If no editing layer is enabled, only two tools appear in the editing tools
bar. The lower one is the one that can be used to analize the logs:

.. figure:: 03_mapview/28_gps_log_analysis1.png
   :align: center
   :width: 300px

Once enabled, one can simply touch the screen near a gps log and information about 
the nearest log point will be shown. Also the color of the text will be the one of 
the currently queried log:

.. figure:: 03_mapview/28_gps_log_analysis.png
   :align: center
   :width: 300px













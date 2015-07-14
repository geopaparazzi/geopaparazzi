.. index:: dashboard
.. _dashboard:

The Main View
==============

Once you launch Geopaparazzi, the dashboard appears.

All the features that need to be quickly accessed, such as toggling GPS on and off, 
creating a note or taking a picture, as well as visualizing the current position 
on a map, are accessible from the dashboard.

.. figure:: 02_dashboard/00_dashboard.png
   :align: center
   :width: 300px

   The dashboard of geopaparazzi.

From the dashboard these primary functions can be accessed:

* the action bar functions
* take notes
* view or modify the project metadata
* take a GPS Track
* switch to Map View
* import Data
* export Data

.. index:: actionbar
.. _actionbar:

Action Bar
-------------------------

The action bar, from right to left, presents the following functions:

* the main menu
* the info button
* the gps status application access button
* the gps status icon 

.. figure:: 02_dashboard/01_actionbar.png
   :align: center
   :width: 300px

   The actionbar of the main view.

The info button shows information about the used maps folder,
the current selected map and the status of the gps. If the
gps has acquired a fix, information about the position is given: 

.. figure:: 02_dashboard/10_info_button.png
   :align: center
   :width: 604px

   Information shown when tapping the info button.

The gps status application button opens the 
`GPS Status & Toolbox <https://play.google.com/store/apps/details?id=com.eclipsim.gpsstatus2&hl=en>`_
app, which gives a lot of information about the gps and is useful 
in cases in which one waits long for the gps to acquire the fix.

.. figure:: 02_dashboard/11_gps_status_app.png
   :align: center
   :width: 300px

   The free (but not open source) Gps Status & Toolbox app.

Last but not least, the gps status icon, shows the current status:

* RED: GPS is not switched on
* ORANGE: GPS is on but no fix was acquired
* GREEN: GPS is on, has fix, but no log is being recorded
* BLUE: GPS is logging

.. figure:: 02_dashboard/02_gps_states.png
   :align: center
   :width: 300px

   The different gps states available.

.. index:: panic button
.. _panicbutton:

The panic button
--------------------

At the opposite end of the actionbar the **Panic Bar** is visible.
Il can be dragged up and presents 2 big buttons:

* the *PANIC!* button
* the *send position* button

.. figure:: 02_dashboard/20_panic.png
   :align: center
   :width: 300px

   The panic panel.

The panic button sends a request for help sms with the last available GPS
position to a phone number that can be configured in the :ref:`settings` of geopaparazzi.

If no number is configured, the system sms dialog opens up with a precompiled message
and the user will have to select the contact to which to send the message:

.. figure:: 02_dashboard/21_panic_sms.png
   :align: center
   :width: 300px


The send position button opens up the sms dialog directly filling in the position without 
any request for help message. It is meant for quick sending the current position via sms
so that it can be opened from Geopaparazzi.


.. index:: notes
.. _notes:

Notes
-------------------------

Geopaparazzi supports 4 different types of notes:

* text notes
* picture notes
* sketch notes
* form-based notes

To access them you can tap on the first icon of the dashboard,
the *take notes* icon. Once you tap on it, the *take notes* dialog 
appears:

.. figure:: 02_dashboard/03_notes.png
   :align: center
   :width: 300px

   The view from which notes are taken.

The uppermost button gives the possibility to choose if the note
will be inserted in the **current gps position** (in case gps is on)
or in the the **center of the map** (aka, the crossfade).

Below the button, three quick note buttons are presented:

* The Quick text note button, which opens a simple dialog, 
  inside which the text note can be written. Want to save the note? 
  Tap on the *ok* button. Want to trash it? Tap on the *cancel* button.

  Remember that the position of the note is taken when the note view is 
  opened, not closed, so you have all the time you need to insert the text. 

.. figure:: 02_dashboard/04_textnotes.png
   :align: center
   :width: 300px

   The simple text note view.

* The Quick picture note button, which will launch the camera application 
  that comes with your android system. We decided to switch from our custom 
  camera application to the default one, because it gives many more 
  possibilities of customization of the images size, focus and so on. 
  This has one drawback, which is the fact that we are not able to pick 
  the azimuth of the camera shot at the exact moment it is taken. 
  The azimuth is therefore recorded at the moment the camera application is closed.
  That means that to have a realistic azimuth, you need to take the picture 
  and stay with the device in the same position of the snapshot until 
  you have closed the camera app.

* The Quick sketch note button, which allows you to draw on a small panel and 
  save your sketch. One can change stroke style, color, and width. 
  Other buttons such as undo, clear, save, and share are also available.
  
  Once the note has been saved, it can't be changed.

.. figure:: 02_dashboard/05_sketchnotes.png
   :align: center
   :width: 300px

   An example of taking sketch note.

Form based notes
+++++++++++++++++++++

Form based notes are complex notes that allow for better surveys.
Some examples are available in the base installation of geopaparazzi.

The **example** button in particular shows all the possible form widgets available:

.. figure:: 02_dashboard/06_form_based_notes.png
   :align: center
   :height: 300px

   An example of form based notes.

The notes can be saved and modified in a second moment.

To understand how to create forms, have a look at the 
:ref:`section dedicated to forms <forms>`.

.. index:: project information
.. _projectinformation:

Project Information
-------------------------

The project view shows information about the project database.

It shows:

* the database file name
* the project name
* the project description
* project notes
* creation and last closing date
* the user that created the project
* the user that last modified the project

Apart of the dates, that are set by the system, all data can be changed and 
saved through the save button.

.. figure:: 02_dashboard/07_project_info.png
   :align: center
   :width: 300px

   The project metadata view.

.. index:: gps logging
.. _gpslogging:

Gps Logging
-------------------------

To start logging, the user simply has to push the **logging** button.

Once it is tapped, the user is prompted to insert a name for the 
log or to accept the one generated based on the current date and time
( log_YYYYMMDD_HHMMSS ).

It is also possible to attach the new log to the last created log by 
checking the box: *Continue last log*. In that case the proposed name 
of the log (or any user inserted) is ignored, since no new log is created. 

.. figure:: 02_dashboard/08_start_logging.png
   :align: center
   :width: 300px

   The new log dialog. From here it is possible to continue the last log.

Once the logging has started, the logging icon will change and present a
red sign. Also, the gps status icon has turned blue.
 
.. figure:: 02_dashboard/09_logging_on.png
   :align: center
   :width: 300px

   The stop logging button with its red sign.

To stop logging, the same button is used. Once tapped, the user is prompted 
to verify the action. 


Map View
-------------------------

The map view presents a map and a set of tools that can be used to navigate 
the map, make measurements or edit datasets. The various tools are presented 
in the :ref:`section dedicated to the Map View <mapview>`.
 
.. figure:: 02_dashboard/12_map_view.png
   :align: center
   :width: 300px

   The map view.

.. index:: import
.. _import:

Import
-------------------------

.. figure:: 02_dashboard/13_import.png
   :align: center
   :width: 300px

   The import view.

Geopaparazzi supports the import of:

* mapurl configuration files for online tiles
* gpx datasets
* geopaparazzi cloud projects
* bookmarks
* default spatialite databases

.. index:: mapurls
.. _mapurls:

Mapurls
++++++++++

Since the creation of a mapurl configuration file for WMS services is complex,
a small service has been created, that automatically generates mapurls for known services.

Once chosen the services query view appears:

.. figure:: 02_dashboard/14_mapurls.png
   :align: center
   :width: 300px 

If wanted, the service will consider the device's position to gather 
only dataset in that area. Also some minor text filters can be added.

An example with the gps placed in Italy is the following:

.. figure:: 02_dashboard/15_mapurls.png
   :align: center
   :width: 300px


The service can then simply be downloaded. It will install the mapurl 
inside your system. The user is prompted for a custom name to name 
the service after, else the original name will be used. Since the 
original name could be duplicated in different services, the 
prefix *tanto\_* will be added in that case.

This service is in an experimental state right now, but it works fairly well. 
If you experience problems, please report them at 
`the homepage of the service <http://tanto.github.io/geopapurls/>`_.

The same link also contains instruction about how to suggest to suggest new WMS services.


.. index:: import gpx
.. _importgpx:

GPX
+++++

By tabbing on the *GPX* icon, the user is taken to a simple file browser. 

.. figure:: 02_dashboard/16_import_gpx.png
   :align: center
   :width: 300px

The browser only shows folders and files with gpx extensions. On selection, the 
file is imported.

Geopaparazzi cloud projects
++++++++++++++++++++++++++++++

**This section needs to be done...**


.. index:: import bookmarks
.. _importbookmarks:

Bookmarks
++++++++++++

Bookmarks can be imported from csv files that *must be placed in the root of the sdcard*
and the name of which has to start with the part **bookmarks** and to end with the 
extension **.csv**.

Geopaparazzi will let the user select the files to import if more than one are available
and load the bookmarks from there and import only those that do not exist already.

The format of the csv is: **NAME, LATITUDE, LONGITUDE**
as for example::

    Uscita Vicenza Est, 45.514237, 11.593432
    Le Bistrot, 46.070037, 11.220296
    Ciolda, 46.024756, 11.230184
    Hotel Trieste, 45.642043,13.780791
    Grassday Trieste,45.65844,13.79320

.. index:: default databases

Default databases
+++++++++++++++++++

When tapping the default database import button, the user is asked to name the new 
database to create. Let's use testdb for the sake of this example:

.. figure:: 02_dashboard/19_mapsforge.png
   :align: center
   :width: 300px

Now you will have to restart geopap, sadly that is still required. 
Once it is done, in the spatialite database view 3 new layer of the database 
**testdb.sqlite** will be visible:

.. figure:: 02_dashboard/19_mapsforge1.png
   :align: center
   :width: 300px

While lines and points won't be of much use in geopap yet, you will find 
the polygon layer interesting, since it is **editing ready**. Enable editing 
and edit right away. Since it is a template db, the attributes table have
been created as generic fields with names from **field1 to field10**. As 
said, very simple, but still of use when you have to quickly collect some 
polygon data with attributes.

.. figure:: 02_dashboard/19_mapsforge2.png
   :align: center
   :width: 300px


.. index:: export
.. _export:

Export
--------------

.. figure:: 02_dashboard/18_export.png
   :align: center
   :width: 300px

   The export view.

Geopaparazzi supports the export to the following formats:

* kmz
* gpx
* geopaparazzi cloud projects
* bookmarks
* images


.. index:: export gpx
.. _exportgpx:

GPX
+++++

The lines and notes data are exported to gpx, creating tracks and waypoints.


.. index:: kmz
.. _kmz:

KMZ
+++++

It is possible to export all collected data to kmz format.
KMZ is well known as it can be visualized in the 3D viewer `Google Earth <http://earth.google.com/>`_.

In the export:

* the notes are placed as red pins having the first letters of the text content as label
* the images are placed as yellow pins
* the gps logs are visualized as tracks

Geopaparazzi cloud projects
++++++++++++++++++++++++++++++++

Find more about geopaparazzi web project protocol in the dedicated page. 


.. index:: export bookmarks
.. _exportbookmarks:

Bookmarks
+++++++++++++++

Bookmarks can be exported to a csv file that has to be called *bookmarks.csv* and 
must be placed in the root of the sdcard.

Geopaparazzi will write to the file only those bookmarks that do not exist already in the csv. 


.. index:: export images
.. _exportimages:

Images
+++++++++++

Since images are kept inside the database, this export is handy if the user needs 
to use the images inside a different software. In this case all the images of the project
are exported inside a folder and a popup message shows the folder path.

.. figure:: 02_dashboard/17_export_img.png
   :align: center
   :width: 300px






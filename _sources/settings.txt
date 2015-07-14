.. index:: settings
.. _settings:

Settings & Preferences
==========================

The preferences page is divided in main categories: 

.. figure:: 05_settings/01_settings.png
   :align: center
   :width: 300px

.. index:: gps preferences

Gps Preferences
+++++++++++++++++++

.. figure:: 05_settings/02_gps.png
   :align: center
   :width: 300px

In the gps preferences it is possible to 

* define the time interval for taking a point when in logging mode
* define the minimum distance in meters, within the taken gps point 
  is not added to the gps log. This is useful for example in the 
  case in which the user stops during logging and the gps continues to 
  supply points (due to the inaccuracy), while instead the user 
  stands still in a point.
* have the mapview recentered on the gps position when the position 
  reaces the screen border (usefull while driving)
* use network based position instead of GPS position. This can be useful 
  for testing purposes. The network based position is by no means precise!

.. index:: sms preferences

Sms Preferences
++++++++++++++++++

.. figure:: 05_settings/03_sms.png
   :align: center
   :width: 300px

It is possible to activate an SMS catcher. If activated, the phone listens
to incoming short messages containing the word::

    GEOPAP

If such a message comes in, the phone answers with an SMS to the incoming
number by sending the last known position.

From here it is also possible to insert the panic numbers(s), i.e. the
numbers to which a status update is sent when the panic button is pushed.


.. index:: screen preferences

Screen Preferences
+++++++++++++++++++

.. figure:: 05_settings/04_screen.png
   :align: center
   :width: 300px

In the screen preferences it is possible to 

* change the map center cross properties (size, color, stroke width)
* change the mapsforge map text size factor (normal is 1. To make text bigger, increase the value)
* enable the always screen on mode
* toggle the use of metric/imperial units
* enable settings to optimize rendering for high density displays



Spatialite Preferences
++++++++++++++++++++++++

.. figure:: 05_settings/05_spl_recovery.png
   :align: center
   :width: 300px

In the Spatialite preferences it is possible to enable the **Spatialite Recovery Mode**            

When should **Spatialite Recovery Mode** be used?

When a **new** Database has been added **and** the geometry does **not** show up,
the preference needs to be activated and Geopaparazzi restarted.

On restart faulty entries in the databases will be **PERMANENTLY** corrected where possible.
Since the process might be time consuming, after a recovery, the settings is
switched off again.

.. index:: custom sdcard path

Custom sdcard path
++++++++++++++++++++++

This can be used for those devices that have more than one external storage recognized by the device.
Use it at your own risk. We use it regularly, but it needs to be done properly.

.. index:: custom maps folder
.. _custommapsfolder:

Custom maps folder
++++++++++++++++++++++++++++

If necessary the maps folder location can be modified here. Geopaparazzi needs
to be restarted for the setting to be applied.


.. index:: force locale

Force Locale
+++++++++++++++++++

The locale of Geopaparazzi can be changed regardless of the locale used for 
Android.

This setting opens a menu in which a locale can be chosen between the available ones:

.. figure:: 05_settings/06_locale.png
   :align: center
   :width: 300px

After that, each newly loaded view will be in the new locale, as for example here in Japanese:

.. figure:: 05_settings/07_locale.png
   :align: center
   :width: 300px



OSM Preferences
+++++++++++++++++++++++

To be done...

Geopap-cloud Preferences
++++++++++++++++++++++++++

To be done...

.. index:: routing api key

Routing api key
++++++++++++++++++++

Here one can set the api keys for MapQuest and Graphhopper to use 
their routing services as explained in the :ref:`go to section <goto>`.





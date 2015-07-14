.. index:: database
.. _database:

The Geopaparazzi Database
==========================

The Geopaparazzi project database is a plain sqlite database 
with the extension **.gpap**.

Database tables described
----------------------------

Metadata
++++++++++++

============    ====================================
key             key for the record
value           value for the record
============    ====================================


Point
++++++

notes
~~~~~~~~

============    ====================================
 _id            unique id               
lon             longitude of the note 
lat             latitude of the note 
altim           elevation 
ts              timestamp 
description     description of the note 
text            text of the note 
form            the form data
isdirty         is dirty field (0 = false, 1 = true)
style           style of the note
============    ====================================

style and isdirty are currently unused.

images
~~~~~~~~

============    =================================================================
 _id            unique id               
lon             longitude of the image 
lat             latitude of the image 
altim           elevation 
ts              timestamp 
azim            azimuth 
text            text of the note 
isdirty         is dirty field (0 = false, 1 = true)
note_id         an optional note id, to which it is bound to
imagedate_id    id of the connected image data  
============    =================================================================

imagedata
~~~~~~~~~~

============    =================================================================
 _id            unique id               
data            the image data
thumbnail       the image thumbnail data
============    =================================================================


bookmarks
~~~~~~~~~~~~

======   =================================================================
_id      unique id               
lon      longitude of the bookmark
lat      latitude of the bookmark
zoom     the zoom of the bookmark 
north    the north bound of the bookmark 
south    the south bound of the bookmark 
west     the west bound of the bookmark 
east     the east bound of the bookmark 
text     the name of the bookmark 
======   =================================================================

Gps
++++

gpslogs
~~~~~~~~~

=======   =================================================================
 _id      unique id               
startts   log start timestamp 
endts     log end timestamp 
lengthm   the length in meters
isdirty   is dirty field (0 = false, 1 = true)
text      name of the log 
=======   =================================================================

gpslog_data
~~~~~~~~~~~~

======   =================================================================
 _id     unique id               
lon      longitude of the log point 
lat      latitude of the log point 
altim    elevation of the log point 
ts       timestamp of the log point 
logid    id of the log of which the point is part 
======   =================================================================

gpslogsproperties
~~~~~~~~~~~~~~~~~~~~~

=======   =================================================================
 _id      unique id               
logid     id of the log the properties are part of 
color     the color to use to draw the log  
width     the width to use to draw the log 
visible   flag that defines if the log is visible at the time given 
=======   =================================================================




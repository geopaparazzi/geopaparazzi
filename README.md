# Geopaparazzi 

This repository contains the complete code of the android app geopaparazzi.

## Notes on the file structure

* **geopaparazzi.app** contains the app, but is only a very small wrapper that extends the real 
  application activity inside **geopaparazzi_core**. This is what you want to change to brand your own 
  geopaparazzi app. Just change the colors, icon and style.
* **geopaparazzi_core** contains the geopaparazzi application logic.
* **geopaparazzilibrary** contains the reusable generic geopaparazzi android code.
* **geopaparazzimapsforge** contains the main mapsforge parts used.
* **geopaparazzimarkerslib** contains the markers app used for the sketches.
* **geopaparazzispatialitelibrary** contains the spatialite support part.

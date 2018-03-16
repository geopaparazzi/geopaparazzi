# Geopaparazzi 

[![Join the chat at https://gitter.im/geopaparazzi/geopaparazzi](https://badges.gitter.im/geopaparazzi/geopaparazzi.svg)](https://gitter.im/geopaparazzi/geopaparazzi?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

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

## The WMS Nasa World Wind module

The WMS NWW module is kept in a a different repository and added to the build as a git submodule.

So the cloning of the repo to work with should be done through:

```
git clone --recursive https://github.com/geopaparazzi/geopaparazzi and then build the package.
```

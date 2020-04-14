# Geopaparazzi

[<img src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
    alt="Get it on Google Play"
    height="80">](https://play.google.com/store/apps/details?id=eu.hydrologis.geopaparazzi)

[![Join the chat at https://gitter.im/geopaparazzi/geopaparazzi](https://badges.gitter.im/geopaparazzi/geopaparazzi.svg)](https://gitter.im/geopaparazzi/geopaparazzi?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)
[![SourceSpy Dashboard](https://sourcespy.com/shield.svg)](https://sourcespy.com/github/geopaparazzigeopaparazzi/)

This repository contains the complete code of the Android app geopaparazzi.

## Notes on the file structure
* **geopaparazzi_acrylicpaint** contains the markers used for the sketches.
* **geopaparazzi_app** contains the app, but is only a very small wrapper that extends the real 
  application activity inside **geopaparazzi_core**. This is what you want to change to brand your own 
  geopaparazzi app. Just change the colors, icon, and style.
* **geopaparazzi_core** contains the geopaparazzi application logic.
* **geopaparazzi_library** contains the reusable generic geopaparazzi Android code.
* **geopaparazzi_map** contains the main mapsforge parts used.
* **plugins** contains the various plugins used for importing and exporting.

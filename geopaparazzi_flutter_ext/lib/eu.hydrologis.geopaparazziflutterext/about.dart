/*
 * Copyright (c) 2019-2020. Antonello Andrea (www.hydrologis.com). All rights reserved.
 * Use of this source code is governed by a GPL3 license that can be
 * found in the LICENSE file.
 */

import 'package:flutter/material.dart';

class AboutPage extends StatefulWidget {
  @override
  AboutPageState createState() {
    return AboutPageState();
  }
}

class AboutPageState extends State<AboutPage> {
  String _appName;
  String _version;
  String _buildNumber;

  Future<void> getVersion() async {
//    PackageInfo packageInfo = await PackageInfo.fromPlatform();

    _appName = null;///packageInfo.appName;
    if (_appName == null) {
      _appName = "SMASH";
    }
    _version = "1.0";// packageInfo.version;
    _buildNumber = "0";//packageInfo.buildNumber;
    if (_version == _buildNumber) {
      _buildNumber = "";
    }

    setState(() {});
  }

  @override
  void initState() {
    getVersion();
    super.initState();
  }

  @override
  Widget build(BuildContext context) {
    String version = _version;
    if (_buildNumber != null && _buildNumber.length != 0) {
      version += " build " + _buildNumber;
    }

    return _appName == null
        ? Text(
       "Loading information...",
    )
        : Scaffold(
      appBar: new AppBar(
        title: Text("ABOUT " + _appName),
      ),
      body: Container(
        padding: const EdgeInsets.all(8.0),
        child: ListView(
          children: <Widget>[
            ListTile(
              title: Text(_appName),
              subtitle: Text("Smart Mobile App for Surveyor's Happyness"),
            ),
            ListTile(
              title: Text("Application version"),
              subtitle: Text(version),
            ),
            ListTile(
              title: Text("License"),
              subtitle: Text(_appName +
                  " is available under the General Public License, version 3."),
            ),
//            ListTile(
//              title: Text("Source Code"),
//              subtitle:
//              Text("Tap here to visit the source code repository"),
//              onTap: () async {
//                if (await canLaunch("https://github.com/moovida/smash")) {
//                  await launch("https://github.com/moovida/smash");
//                }
//              },
//            ),
//            ListTile(
//              title: Text("Legal Information"),
//              subtitle: Text(
//                  "Copyright 2020, HydroloGIS S.r.l. -  some rights reserved. Tap to visit."),
//              onTap: () async {
//                if (await canLaunch("http://www.hydrologis.com")) {
//                  await launch("http://www.hydrologis.com");
//                }
//              },
//            ),
            ListTile(
              title: Text("Supported by"),
              subtitle: Text(
                  "Partially supported by the project Steep Stream of the University of Trento."),
            ),
//            ListTile(
//              title: Text("You might also be interested in"),
//              subtitle: Text(
//                  "Geopaparazzi is a complete and mature android-only digital field mapping application by the same authors of SMASH."),
//              onTap: () async {
//                if (await canLaunch("http://www.geopaparazzi.org")) {
//                  await launch("http://www.geopaparazzi.org");
//                }
//              },
//            ),
          ],
        ),
      ),
    );
  }
}

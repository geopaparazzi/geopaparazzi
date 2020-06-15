import 'package:flutter/material.dart';
import 'package:geopaparazziflutterext/eu.hydrologis.geopaparazziflutterext/about.dart';

void main() => runApp(MyApp());

class MyApp extends StatelessWidget {
  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      routes: {
        '/about': (context) => AboutPage(),
      },
      title: 'Flutter Demo',
//      theme: ThemeData(
//        primarySwatch: Colors.blue,
//      ),
    );
  }
}

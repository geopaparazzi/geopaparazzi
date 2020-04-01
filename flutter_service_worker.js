'use strict';
const CACHE_NAME = 'flutter-app-cache';
const RESOURCES = {
  "index.html": "4c551d32aaabdbd6b2cb3d499c7866fe",
"main.dart.js": "04d8684e14ce005f0696f0e48efa38da",
"assets/LICENSE": "dae86ac55ab0a8cd592eabcbd9ddfbb2",
"assets/AssetManifest.json": "0ae7fc18201ca69c4417732763656d07",
"assets/FontManifest.json": "40849f3e1b3bb567a55b7118d9b4dadf",
"assets/packages/material_design_icons_flutter/lib/fonts/materialdesignicons-webfont.ttf": "3ac50b5b36eb2f11b000dce1792d0bb0",
"assets/fonts/MaterialIcons-Regular.ttf": "56d3ffdef7a25659eab6a68a3fbfaf16"
};

self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys().then(function (cacheName) {
      return caches.delete(cacheName);
    }).then(function (_) {
      return caches.open(CACHE_NAME);
    }).then(function (cache) {
      return cache.addAll(Object.keys(RESOURCES));
    })
  );
});

self.addEventListener('fetch', function (event) {
  event.respondWith(
    caches.match(event.request)
      .then(function (response) {
        if (response) {
          return response;
        }
        return fetch(event.request);
      })
  );
});

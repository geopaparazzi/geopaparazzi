'use strict';
const CACHE_NAME = 'flutter-app-cache';
const RESOURCES = {
  "index.html": "4c551d32aaabdbd6b2cb3d499c7866fe",
"main.dart.js": "18a7b78fe617c21878a538ecb293f0e6",
"assets/LICENSE": "4917a4e81ecc6d490e317fe2687e6709",
"assets/AssetManifest.json": "931da6a0e67213c055a2606f2bcd4179",
"assets/FontManifest.json": "0621fb7723859a382fc19210904f6578",
"assets/packages/material_design_icons_flutter/lib/fonts/materialdesignicons-webfont.ttf": "3ac50b5b36eb2f11b000dce1792d0bb0",
"assets/packages/cupertino_icons/assets/CupertinoIcons.ttf": "115e937bb829a890521f72d2e664b632",
"assets/fonts/MaterialIcons-Regular.ttf": "56d3ffdef7a25659eab6a68a3fbfaf16",
"assets/assets/00.png": "275a9078061033bc20db4c84bc07937b",
"assets/assets/01.jpg": "306aefbfce33bcdbd311975c87f1752f",
"assets/assets/03.jpg": "7c0199f3a24875303664f87835e871c5",
"assets/assets/smash_icon.png": "5747fb7b8598d34c6f5144f0f1e703b7",
"assets/assets/02.jpg": "48985a12dea6a87f5e6f378db7a1297d",
"assets/assets/06.png": "c3d56eec9d7e164ffedb8d1f826181b3",
"assets/assets/07.png": "a3e8a28d81138ed682e42d7bc2cdbdb4",
"assets/assets/05.png": "8186769777ab0015df7e2be39833c8cb",
"assets/assets/04.jpg": "870c9c5fed4eb2a55d82193c5ea4d73b",
"assets/assets/09.png": "736d210fc0415f6b2f8a639e254c9f52",
"assets/assets/08.png": "7c3fe28011ccf9da9fcbe90e036ebfc2"
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

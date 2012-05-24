#Build script

#Clean up
rm -rf build
rm -rf dist

#create the needed directories
mkdir -m 770 -p dist
mkdir -m 770 -p build/classes

#Rmove the R.java file as will be created by aapt
rm ../../geopaparazzi.app/gen/eu/hydrologis/geopaparazzi/R.java 

#Now use aapt
echo Create the R.java file
aapt p -f -v -M ../../geopaparazzi.app/AndroidManifest.xml -F ./build/resources.res -I ~/system/classes/android.jar -S ../../geopaparazzi.app/res/ -J ../../geopaparazzi.app/src/eu/hydrologis/geopaparazzi 

#cd into the src dir
cd ../../geopaparazzi.app/src

#Now compile - note the use of a seperate lib (in non-dex format!)
echo Compile the java code
javac -cp ../libs/osmdroid-android-3.0.5.jar:../libs/osmdroid-third-party-3.0.5.jar:../libs/slf4j-android-1.5.8.jar -d ../../extras/terminalide/build/classes eu/hydrologis/geopaparazzi/GeoPaparazziActivity.java
#javac -verbose -cp ../libs/osmdroid-android-3.0.5.jar:../libs/osmdroid-third-party-3.0.5.jar:../libs/slf4j-android-1.5.8.jar -d ../build/classes eu/hydrologis/geopaparazzi/GeoPaparazziActivity.java

#Back out
cd ..

#Now into build dir
cd ../extras/terminalide/build/classes/

#Now convert to dex format (need --no-strict) (Notice demolib.jar at the end - non-dex format)
echo Now convert to dex format
dx --dex --verbose --no-strict --output=../geopaparazzi.dex eu ../../../../geopaparazzi.app/libs/osmdroid-android-3.0.5.jar ../../../../geopaparazzi.app/libs/osmdroid-third-party-3.0.5.jar ../../../../geopaparazzi.app/libs/slf4j-android-1.5.8.jar

echo Done convert to dex format

#Back out
cd ../..

#And finally - create the .apk
apkbuilder ./dist/geopaparazzi.apk -v -u -z ./build/resources.res -f ./build/geopaparazzi.dex 

#And now sign it
cd dist
signer geopaparazzi.apk geopaparazzi_signed.apk

cd ..


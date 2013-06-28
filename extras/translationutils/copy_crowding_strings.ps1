# change paths
$from="......\geopaparazzi_translations"
$toPap=".....\geopaparazzi-git\geopaparazzi.app\res"
$toLib=".....\geopaparazzi-git\geopaparazzilibrary\res"
$toSpa=".....\geopaparazzi-git\geopaparazzispatialitelibrary\res"

$languages = @("de", "es", "fi", "fr", "hu", "it", "ja")
foreach ($language in $languages) {
    cp $from\$language\strings.xml $toPap\values-$language\strings.xml	
    cp $from\$language\library\strings.xml $toLib\values-$language\strings.xml	
    cp $from\$language\spatialite\strings.xml $toSpa\values-$language\strings.xml	
}



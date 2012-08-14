#!/usr/bin/python

import json, sys
#try to import required library
try:
    import osgeo.ogr as ogr
    import osgeo.osr as osr
except ImportError:
    import ogr, osr
try:
    from OsmApi import OsmApi
except ImportError:
    import OsmApi

def getMapData(geom,api,buf=0.0002):
    """ Return OSM data inside the bounding box of feature's geometry
    geom = OGR geometry object
    api = pythonOsmApi object
    buf = buffer to downloads osm data; default  0.0002 == 20 meters radius
    """
    #create a buffer around the point of 30 m
    geomBuffer = geom.Buffer(buf)
    #get bounding box
    bbox = geomBuffer.GetEnvelope()
    #download data
    osmData = api.Map(bbox[0],bbox[2],bbox[1],bbox[3])
    return osmData

def getPointData(osmData):
    """ Return the list of point data
    osmData = variable created with getMapData()
    line = False if you want point data, true if you want nodes of line data
    """
    pointData = []
    for i in osmData:
        #if is not a node without tags
        if i[unicode('type')] == 'node' and i[unicode('data')][unicode('tag')] != {}:
            #if is not a node with only tag created_by
            if len(i[unicode('data')][unicode('tag')]) == 1 and i[unicode('data')][unicode('tag')].keys() == unicode('created_by'):
                #not done
                continue
            else:
                #add node
                pointData.append(i)
    return pointData

def checkDiversityTags(tagOSM,tagPoint):
    """ Check the similarity of tags
    tagOSM = tags of osm data
    tagPoint = tags of data to import
    """
    if len(tagOSM) >= len(tagPoint):
        tagMag = tagOSM
        tagMin = tagPoint
    else:
        tagMag = tagPoint
        tagMin = tagOSM
    allTags = 0.0
    tagsDifferent = 0.0    
    # for all tag in osm data
    for i in tagMin.keys():
        keyTag = i
        allTags += 1.0
        # if the tag's key of new point doesn't contain the tag in osm data 
        if tagMag.keys().count(i) == 0:
            tagsDifferent += 1.0
        # if the tag's key of new point is contained in osm data
        else:
            # if the value of tag is different
            if tagMin[i] != tagMag[keyTag]:
                tagsDifferent += 1.0
    # check the % of ugual tags
    percUgual = tagsDifferent * 100 / allTags
    #print "Percentuale : " + str(percUgual)
    # if number of tags is <=2 all tag must be ugual for upload data
    if allTags <= 2: 
        if percUgual != 100:
            return 0
        else:
            return 1
    # if number of tags is >2 and <=4 the percent must be >66 for upload data 
    elif allTags <= 4 and allTags > 2:
        if percUgual < 66:
            return 0
        else:
            return 1     
    # if number of tags is >4 the percentual must be >75 for upload data
    if allTags > 4 :
        if percUgual < 75:
            return 0
        else:
            return 1

def checkNewPoint(geom,tags,pointData):
    '''Function check the similarity of a point, check before inside a buffer and after in the tag
    geom = geometry of the feature
    tags = tags of the feature, return of tagsDef function
    pointData = point data present on osm db, return of getPointData function
    '''   
    #if pointData is null is possible add that feature
    if pointData == []:
        return 1
    else:
        # all point
        nPointData = len(pointData)
        # variable per different data
        nPointDifferent = 0
        for i in pointData:
            #print i[unicode('data')][unicode('tag')],tags
            #check if the tags are similar
            if checkDiversityTags(i[unicode('data')][unicode('tag')],tags):
                nPointDifferent += 1
        # if the number of all point and the different are the same is possible to upload data
        if nPointDifferent == nPointData:
            return 1
        else:
            return 0

def zoosmarazzi(conf,inputs,outputs):
    """ Function for ZOO for import vector data on OSM DB 
    All parameters are passed from ZOO request
    """

    #the json string
    inputName = inputs["inputvector"]["value"]
    #the username of openstreetmap
    usern = inputs["username"]["value"]
    #the password of user in openstreetmap
    passw = inputs["password"]["value"]
    #the comment of changset
    change = inputs["changeset"]["value"]
    # try to decode json
    try:
        js = json.loads(inputName)
    except:
        conf["lenv"]["message"] = "error_json"
        return 4
    # set the changeset comment
    if change:
        comment = 'auto import of features using geopaparazzi and geopaposm.\n %s' % change
    else:
        comment = 'auto import of features using geopaparazzi and geopaposm.'
    # connect to api
    osmapi = OsmApi(api="api06.dev.openstreetmap.org", username = unicode(usern),
                password = unicode(passw), appid = unicode('Z0OSM 0.2'),
                changesetauto = True, changesetautotags =
                {unicode('comment'): unicode(comment)})
    # set to zero the number of feature
    nfeatures = 0
    nfeaturesload = 0
    # temporary id for osmapi
    iD = -1
    # for each feature in json string create the node definition
    for j in js: 
        values=j['form']['formitems']
        tags = {}
        # set temporary id
        nodeDef = {unicode('id'):iD}
        # for each value put it to tags or coordinate
        for v in values:
	    
            if v['value'] != '':
		if v['type'] == 'boolean' and v['value'] == 'false':
		    continue
		elif v['type'] == 'boolean' and v['value'] == 'true':
		    tags[v['key']] = 'yes'
                elif v['key'] == 'LONGITUDE':
                    nodeDef[unicode('lon')] = v['value']
                elif v['key'] == 'LATITUDE':
                    nodeDef[unicode('lat')] = v['value']
                else:
                    tags[v['key']] = v['value']
        nodeDef[unicode('tag')] = tags
        if len(list(set([unicode('tag'), unicode('lat'),unicode('lon')]
                        ).intersection(set(nodeDef.keys())))) == 3:
                # create geometry point with ogr to check if it's present 
                #a similar point in a buffer
                geom = ogr.Geometry(ogr.wkbPoint)
                geom.AddPoint_2D(float(nodeDef[unicode('lon')]),float(nodeDef[unicode('lat')]))
                #get osm data near the feature (look function getMapData)
                osmData = getMapData(geom,osmapi)
                #from osm data extract only point
                pointData = getPointData(osmData)
                #made some control to search if feature already exist on osm db
                if checkNewPoint(geom,tags,pointData):
                    osmapi.NodeCreate(nodeDef)
                    nfeaturesload += 1
        nfeatures += 1
        iD -= 1
    # try to load data or return an error
    try:
        osmapi.flush()
    except:
        conf["lenv"]["message"] = "error_osm"
        return 4
    # return the string of the number of feature loaded
    npoints = abs(iD) - 1
    if nfeaturesload == nfeatures:
        output = 'features_imported'
    else:
        output = 'features_imported_%i_%i' % (nfeaturesload, nfeatures)
    outputs["output"]["value"]= output
    return 3


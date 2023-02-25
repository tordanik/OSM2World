#!/bin/bash

cd `dirname $0`

# retrieve VM parameters

vmparams="-Xmx2G --add-exports java.base/java.lang=ALL-UNNAMED --add-exports java.desktop/sun.awt=ALL-UNNAMED --add-exports java.desktop/sun.java2d=ALL-UNNAMED"

if [[ $1 == --vm-params=* ]]
  then
    vmparams=${1:12}
    shift
fi

# run OSM2World

java $vmparams -jar OSM2World.jar $@

#!/bin/bash

cd `dirname $0`

# retrieve VM parameters

vmparams="-Xmx2G"

if [[ $1 == --vm-params=* ]]
  then
    vmparams=${1:12}
    shift
fi

# run OSM2World

java $vmparams -jar OSM2World.jar $@

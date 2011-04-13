#!/bin/bash

MACHINE_TYPE=`uname -m`
KERNEL_NAME=`uname -s`

if [ ${KERNEL_NAME} == 'Darwin' ]; then
  java -Djava.library.path="lib/jogl/macosx-universal" -Xmx2G -jar OSM2World.jar $*
else
  if [ ${MACHINE_TYPE} == 'x86_64' ]; then
    java -Djava.library.path="lib/jogl/linux-amd64" -Xmx2G -jar OSM2World.jar $*
  else
    java -Djava.library.path="lib/jogl/linux-i586" -Xmx2G -jar OSM2World.jar $*
  fi
fi

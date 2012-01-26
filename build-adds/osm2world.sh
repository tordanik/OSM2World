#!/bin/bash

# choose path for the native JOGL libs depending on system and java version

MACHINE_TYPE=`uname -m`
KERNEL_NAME=`uname -s`

if [ ${KERNEL_NAME} == 'Darwin' ]; then
  joglpath="macosx-universal"
else  
  if [ `java -version 2>&1|grep -c 64-Bit` -gt 0 ]; then
    joglpath="lib/jogl/linux-amd64"
  else
    joglpath="lib/jogl/linux-i586"
  fi 
fi

# run OSM2World

java -Djava.library.path=$joglpath -Xmx2G -jar OSM2World.jar $*

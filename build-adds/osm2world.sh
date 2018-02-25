#!/bin/bash

# retrieve VM parameters

vmparams="-Xmx2G"

if [[ $1 == --vm-params=* ]]
  then
    vmparams=${1:12}
    shift
fi

# choose path for the native JOGL libs depending on system and java version
script_dir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

MACHINE_TYPE=`uname -m`
KERNEL_NAME=`uname -s`

if [ ${KERNEL_NAME} == 'Darwin' ]; then
lpsolvepath="lib/lp_solve/mac"
else
if [ `java -version 2>&1|grep -c 64-Bit` -gt 0 ]; then
lpsolvepath="lib/lp_solve/ux64"
  else
lpsolvepath="lib/lp_solve/ux32"
  fi
fi

# run OSM2World

export LD_LIBRARY_PATH=$script_dir/$lpsolvepath

java -Djava.library.path=$lpsolvepath $vmparams -jar $script_dir/OSM2World.jar --config texture_config.properties $@

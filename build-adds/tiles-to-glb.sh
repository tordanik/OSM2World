#!/usr/bin/env bash

# Usage ./tiles-to-glb.sh /path/to/tiles

BASE_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )/.." &> /dev/null && pwd )
BIN="java -jar $BASE_DIR/target/osm2world-0.4.0-SNAPSHOT.jar"

TILES_ROOT=${1%/}

echo "Convert tiles from $TILES_ROOT"
echo $BASE_DIR

mkdir -p "$TILES_ROOT/tiles"

for tile_p in $(find $TILES_ROOT -iname '*.osm'); do
    #cut prefix
    tile_sfx=${tile_p#"$TILES_ROOT/"}
    #cut suffix
    tile=${tile_sfx%".osm"}

    z=${tile%%/*}
    xy=${tile#*/}
    x=${xy%%/*}
    y=${xy#*/}

if [ -f "$TILES_ROOT/$z/$x/$y.osm" ]; then
    echo $tile $z $x $y

    cmd="$BIN --input $TILES_ROOT/$z/$x/$y.osm --tile $z/$x/$y --output $TILES_ROOT/tiles/${z}_${x}_${y}.glb"
    $cmd > /dev/null 2>&1
fi

done
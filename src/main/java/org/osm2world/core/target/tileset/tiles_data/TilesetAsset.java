package org.osm2world.core.target.tileset.tiles_data;

public class TilesetAsset {
    private String version = "1.0";

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public TilesetAsset() {

    }

    public TilesetAsset(String version) {
        this.version = version;
    }
    
}

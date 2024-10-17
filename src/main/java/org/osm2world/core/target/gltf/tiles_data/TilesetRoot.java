package org.osm2world.core.target.gltf.tiles_data;

public class TilesetRoot {
    private TilesetAsset asset;
    private TilesetParentEntry root;

    public TilesetRoot() {
    }

    public TilesetAsset getAsset() {
        return asset;
    }
    public void setAsset(TilesetAsset asset) {
        this.asset = asset;
    }
    
    public TilesetParentEntry getRoot() {
        return root;
    }
    public void setRoot(TilesetParentEntry root) {
        this.root = root;
    }
    
}

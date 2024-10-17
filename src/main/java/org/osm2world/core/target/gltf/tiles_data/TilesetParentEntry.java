package org.osm2world.core.target.gltf.tiles_data;

import java.util.ArrayList;
import java.util.List;

/**
 * // Example tileset
    "content": {
        "uri": "14_5298_5916_0.glb"
    },
    "refine": "ADD",
    "geometricError": 25,
    "boundingVolume": {
        "region": [-1.1098350999480917,0.7790694465970149,-1.1094516048185785,0.779342292568195,0.0,100]
    },
    "transform": [
        0.895540041198885,    0.4449809373551852,  0.0,                0.0,
        -0.31269461895546163, 0.6293090971636892,  0.7114718093525005, 0.0,
        0.3165913926274654,   -0.6371514934593837, 0.7027146394495273, 0.0,
        2022609.150078308,    -4070573.2078238726, 4459382.83869308,   1.0
    ],

    "children": [{
        "boundingVolume": {
            "region": [-1.1098350999480917,0.7790694465970149,-1.1094516048185785,0.779342292568195,0.0,97.49999999999997]
        },
        "geometricError": 0,
        "content": {
            "uri": "14_5298_5916.glb"
        }
    }]
 */
public class TilesetParentEntry extends TilesetEntry {

    private String refine = "ADD";
    private double[] transform;
    private List<TilesetEntry> children;

    public TilesetParentEntry() {
    }

    public String getRefine() {
        return refine;
    }
    public void setRefine(String refine) {
        this.refine = refine;
    }
    
    public double[] getTransform() {
        return transform;
    }
    public void setTransform(double[] transform) {
        this.transform = transform;
    }
    
    public List<TilesetEntry> getChildren() {
        return children;
    }
    public void setChildren(List<TilesetEntry> children) {
        this.children = children;
    }

    public void addChild(TilesetEntry chld) {
        if (this.children == null) {
            this.children = new ArrayList<>();
        }
        this.children.add(chld);
    }
    
    public void addChild(String contentUri) {
        this.addChild(new TilesetEntry(
            0,
            this.getBoundingVolume().getRegion(),
            contentUri
        ));
        
    }

}

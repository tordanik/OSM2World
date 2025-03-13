package org.osm2world.output.tileset.tiles_data;


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

    // This TilesetEntry covers children entries
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


public class TilesetEntry {

    public static final class Region {

        private double[] region = new double[6];

        public Region() {
        }

        public Region(double[] region) {
            this.region = region;
        }

        public double[] getRegion() {
            return region;
        }

        public void setRegion(double[] region) {
            this.region = region;
        }
    }

    public static final class TilesetContent {
        private String uri;

        public TilesetContent(String uri) {
            this.uri = uri;
        }

        public TilesetContent() {
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }
    }

    private Number geometricError = 0;
    private Region boundingVolume = null;
    private TilesetContent content = null;

    public TilesetEntry() {
    }

    public TilesetEntry(Number geometricError, double[] boundingVolume, String contentUri) {
        this.geometricError = geometricError;
        this.boundingVolume = new Region(boundingVolume);
        this.content = new TilesetContent(contentUri);
    }

    public Number getGeometricError() {
        return geometricError;
    }
    public void setGeometricError(Number geometricError) {
        this.geometricError = geometricError;
    }
    
    public Region getBoundingVolume() {
        return boundingVolume;
    }
    public void setBoundingVolume(Region boundingVolume) {
        this.boundingVolume = boundingVolume;
    }
    public void setBoundingVolume(double[] region) {
        this.boundingVolume = new Region(region);
    }
    
    public TilesetContent getContent() {
        return content;
    }
    public void setContent(TilesetContent content) {
        this.content = content;
    }
    public void setContent(String contentUri) {
        this.content = new TilesetContent(contentUri);
    }
}

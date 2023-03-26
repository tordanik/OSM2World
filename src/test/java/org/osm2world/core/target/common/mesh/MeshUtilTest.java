package org.osm2world.core.target.common.mesh;

import org.junit.Test;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.material.TextureDataDimensions;

import java.awt.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.osm2world.core.target.common.mesh.MeshUtil.createBox;

public class MeshUtilTest {

    @Test
    public void testCreateBox() {

        Geometry result = createBox(new VectorXYZ(0, 0, 0), new VectorXZ(0, -1), 5, 3, 2,
                Color.RED, List.of(new TextureDataDimensions(1, 1, null, null)));

        assertEquals(12, result.asTriangles().triangles.size());

    }

}
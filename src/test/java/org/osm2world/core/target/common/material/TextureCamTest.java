package org.osm2world.core.target.common.material;

import static java.awt.Color.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.osm2world.core.target.common.material.Materials.PLASTIC;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.TextureCam.ViewDirection;
import org.osm2world.core.target.common.material.TextureData.Wrap;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;

public class TextureCamTest {

	@Test
	public void test2Triangles() throws IOException {

		TriangleXYZ tFront = new TriangleXYZ(new VectorXYZ(1, 0, 1), new VectorXYZ(0, 1, 0), new VectorXYZ(0, 0, 1));
		TriangleXYZ tBack = new TriangleXYZ(new VectorXYZ(1, 0, 2), new VectorXYZ(1, 1, 2), new VectorXYZ(0, 0, 2));

		TriangleGeometry.Builder geometryBuilder = new TriangleGeometry.Builder(0, GRAY, Interpolation.FLAT);
		geometryBuilder.addTriangles(asList(tFront), emptyList(), asList(RED, RED, GREEN));
		geometryBuilder.addTriangles(tBack);

		List<Mesh> meshes = asList(new Mesh(geometryBuilder.build(), PLASTIC));

		TextureLayer result = TextureCam.renderTextures(meshes, ViewDirection.FROM_FRONT, "test", 1.0, 1.0, null, null,
				Wrap.CLAMP, new VectorXYZ(0.5, 0.5, 0));

		result.writeToFiles(new File("/tmp/texturecam-test_$INFIX.png"));

	}

}

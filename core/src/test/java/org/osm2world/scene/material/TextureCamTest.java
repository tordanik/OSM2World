package org.osm2world.scene.material;

import static java.lang.Math.sqrt;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.osm2world.scene.color.Color.*;
import static org.osm2world.scene.material.TextureCam.colorFromNormal;
import static org.osm2world.scene.material.TextureCam.normalFromColor;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.color.LColor;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.material.TextureCam.ViewDirection;
import org.osm2world.scene.material.TextureData.Wrap;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.TriangleGeometry;

public class TextureCamTest {

	@Test
	public void testNormalColorConversion() {

		assertAlmostEquals(new VectorXYZ(0, 0, 1), normalFromColor(new LColor(0.5f, 0.5f, 1f)));

		assertAlmostEquals(new LColor(0.5f, 0.5f, 1f), colorFromNormal(new VectorXYZ(0, 0, 1)));

		for (VectorXYZ normal : asList(new VectorXYZ(0.0, -sqrt(0.5), sqrt(0.5)))) {
			assertAlmostEquals(normal, normalFromColor(colorFromNormal(normal)));
		}

	}

	@Test
	public void test2Triangles() throws IOException {

		TriangleXYZ tFront = new TriangleXYZ(new VectorXYZ(1, 0, 1), new VectorXYZ(0, 1, 0), new VectorXYZ(0, 0, 1));
		TriangleXYZ tBack = new TriangleXYZ(new VectorXYZ(1, 0, 2), new VectorXYZ(1, 1, 2), new VectorXYZ(0, 0, 2));

		TriangleGeometry.Builder geometryBuilder = new TriangleGeometry.Builder(0, GRAY, Interpolation.FLAT);
		geometryBuilder.addTriangles(asList(tFront), emptyList(), asList(RED, RED, GREEN));
		geometryBuilder.addTriangles(tBack);

		List<Mesh> meshes = asList(new Mesh(geometryBuilder.build(), new ImmutableMaterial(Interpolation.FLAT, WHITE)));

		TextureLayer result = TextureCam.renderTextures(meshes, ViewDirection.FROM_FRONT, "test",
				new TextureDataDimensions(1.0, 1.0),
				Wrap.CLAMP, new VectorXYZ(0.5, 0.5, 0), 0.0);

		String tmpDir = System. getProperty("java.io.tmpdir");
		result.writeToFiles(new File(tmpDir, "texturecam-test_$INFIX.png"));

		/* check some pixels of the result */

		assertAlmostEquals(new LColor(0.5f, 0.5f, 0f),
				result.baseColorTexture.getColorAt(new VectorXZ(0.0, 0.5), Wrap.CLAMP));
		assertAlmostEquals(LColor.fromRGB(GRAY),
				result.baseColorTexture.getColorAt(new VectorXZ(1.0, 0.5), Wrap.CLAMP));

		assertAlmostEquals(new LColor(1f, 1f, 1f),
				result.displacementTexture.getColorAt(new VectorXZ(0.0, 0.01), Wrap.CLAMP));
		assertAlmostEquals(new LColor(0.5f, 0.5f, 0.5f),
				result.displacementTexture.getColorAt(new VectorXZ(0.5, 1.0), Wrap.CLAMP));
		assertAlmostEquals(new LColor(0f, 0f, 0f),
				result.displacementTexture.getColorAt(new VectorXZ(1.0, 0.5), Wrap.CLAMP));

		assertAlmostEquals(0, -sqrt(0.5), sqrt(0.5),
				normalFromColor(result.normalTexture.getColorAt(new VectorXZ(0.25, 0.5), Wrap.CLAMP)),
				0.02);
		assertAlmostEquals(0, 0, 1,
				normalFromColor(result.normalTexture.getColorAt(new VectorXZ(0.75, 0.5), Wrap.CLAMP)),
				0.02);

	}

}

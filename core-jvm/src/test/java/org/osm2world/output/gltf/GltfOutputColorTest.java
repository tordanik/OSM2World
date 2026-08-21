package org.osm2world.output.gltf;

import static org.junit.Assert.*;
import static org.osm2world.output.gltf.GltfOutputTestUtil.fileToString;
import static org.osm2world.output.gltf.GltfOutputTestUtil.writeGltf;
import static org.osm2world.scene.color.Color.BLUE;
import static org.osm2world.scene.color.Color.RED;
import static org.osm2world.scene.material.DefaultMaterials.STEEL;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.color.LColor;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.mesh.TriangleGeometry;
import org.osm2world.util.platform.image.ImageImplementationJvm;
import org.osm2world.util.platform.json.JsonImplementationJvm;

/**
 * tests that a vertex color which is the same for the entire mesh ends up in the material
 * instead of in a COLOR_0 attribute, see {@link GltfOutput}
 */
public class GltfOutputColorTest {

	static {
		JsonImplementationJvm.register();
		ImageImplementationJvm.register();
	}

	@Test
	public void testConstantColorGoesIntoMaterial() throws IOException {

		String json = writeAndReadGltf(List.of(Color.RED, Color.RED, Color.RED));

		assertFalse("no COLOR_0 attribute for a mesh with a single color", json.contains("COLOR_0"));

		/* the color is stored in the material instead, in linear color space */

		LColor expected = LColor.fromRGB(RED);
		assertTrue("baseColorFactor is present: " + json, json.contains("baseColorFactor"));
		assertTrue("baseColorFactor holds the red component: " + json,
				json.contains(Float.toString(expected.red)));
		assertTrue("baseColorFactor holds the green component: " + json,
				json.contains(Float.toString(expected.green)));

	}

	@Test
	public void testVaryingColorsStayInTheAttribute() throws IOException {

		String json = writeAndReadGltf(List.of(Color.RED, BLUE, Color.RED));

		assertTrue("COLOR_0 attribute for a mesh with more than one color", json.contains("COLOR_0"));
		assertFalse("no baseColorFactor if the colors vary", json.contains("baseColorFactor"));

	}

	@Test
	public void testNoVertexColors() throws IOException {

		String json = writeAndReadGltf((List<Color>) null);

		/* even without explicit vertex colors, the color of the material itself is moved to the vertices
		 * before the output is written. It is the same for all vertices, so it ends up in the material again. */

		assertFalse("no COLOR_0 attribute for the uniform color of a material", json.contains("COLOR_0"));

		LColor expected = LColor.fromRGB(STEEL.defaultAppearance().color());
		assertTrue("baseColorFactor holds the color of the material: " + json,
				json.contains(Float.toString(expected.red)));

	}

	@Test
	public void testMaterialSharedAcrossMeshes() throws IOException {

		String json = writeAndReadGltf(List.of(RED, RED, RED), List.of(RED, RED, RED));

		assertEquals("separate meshes with the same material and color share a glTF material",
				1, StringUtils.countMatches(json, "baseColorFactor"));

	}

	/**
	 * writes a glTF file containing one mesh per list of vertex colors, and returns its content
	 *
	 * @param colorLists  colors for the 3 vertices of the single triangle of each mesh, each may be null
	 */
	@SafeVarargs
	private static @Nonnull String writeAndReadGltf(List<Color>... colorLists) throws IOException {

		Material material = STEEL.defaultAppearance();

		List<Mesh> meshes = new ArrayList<>();

		for (List<Color> colors : colorLists) {

			var triangle = new TriangleXYZ(new VectorXYZ(0, 0, 0), new VectorXYZ(1, 0, 0), new VectorXYZ(0, 1, 0));

			var builder = new TriangleGeometry.Builder(material.textureLayers().size(), null,
					material.interpolation());
			builder.addTriangles(List.of(triangle), null, colors);

			meshes.add(new Mesh(builder.build(), material));

		}

		File tempFile = writeGltf(".gltf", meshes, Map.of("keepOsmElements", false));

		return fileToString(tempFile);

	}

}

package org.osm2world.scene.material;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.osm2world.scene.color.Color.BLACK;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.material.Material.Transparency;
import org.osm2world.scene.material.TextureData.Wrap;

public class MaterialTest {

	@Test
	public void testPlaceholders_map() {

		Material originalMat = createTextTestMaterial("test %{test}");

		TagSet tags = TagSet.of("test", "success");

		Material newMat = originalMat.withPlaceholdersFilledIn(emptyMap(), tags);

		TextTexture result = (TextTexture)newMat.getTextureLayers().get(0).baseColorTexture;
		assertEquals("test success", result.text);

	}

	@Test
	public void testPlaceholders_tags() {

		Material originalMat = createTextTestMaterial("%{test} %{test2}");

		Map<String, String> map = new HashMap<>();
		map.put("test", "success1");
		map.put("test2", "success2");

		Material newMat = originalMat.withPlaceholdersFilledIn(map, TagSet.of());

		TextTexture result = (TextTexture)newMat.getTextureLayers().get(0).baseColorTexture;
		assertEquals("success1 success2", result.text);

	}

	@Test
	public void testPlaceholders_noReplacement() {

		Material originalMat = createTextTestMaterial("");

		Map<String, String> map = new HashMap<>();
		map.put("foo", "bar");
		map.put("foo2", "baz");

		Material newMat = originalMat.withPlaceholdersFilledIn(map, TagSet.of());

		assertSame(originalMat, newMat);

	}

	@Test
	public void testPlaceholders_default() {

		Material originalMat = createTextTestMaterial("%{somekey, 30} t");

		Material newMat = originalMat.withPlaceholdersFilledIn(emptyMap(), TagSet.of());

		TextTexture result = (TextTexture)newMat.getTextureLayers().get(0).baseColorTexture;
		assertEquals("30 t", result.text);

	}

	/** creates a material with a single {@link TextTexture} */
	private static Material createTextTestMaterial(String text) {
		return new ImmutableMaterial(Interpolation.FLAT, Color.WHITE, Transparency.TRUE, asList(new TextureLayer(
				new TextTexture(text,
				null, new TextureDataDimensions(1.0, 1.0), 50.0, 50.0, BLACK, 50.0,
				Wrap.CLAMP, GLOBAL_X_Z), null, null, null, false)));
	}

}

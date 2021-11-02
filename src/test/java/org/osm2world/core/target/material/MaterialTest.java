package org.osm2world.core.target.material;

import static java.awt.Color.BLACK;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static org.junit.Assert.*;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.GLOBAL_X_Z;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.Material.Transparency;
import org.osm2world.core.target.common.material.TextTexture;
import org.osm2world.core.target.common.material.TextureData.Wrap;
import org.osm2world.core.target.common.material.TextureLayer;

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
				null, 1.0, 1.0, null, null, 50.0, 50.0, BLACK, 50.0,
				Wrap.CLAMP, GLOBAL_X_Z), null, null, null, false)));
	}

}

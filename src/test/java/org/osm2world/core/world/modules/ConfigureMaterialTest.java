package org.osm2world.core.world.modules;

import static org.junit.Assert.*;
import static java.awt.Color.BLACK;
import static java.util.Arrays.asList;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.osm2world.core.target.common.TextTextureData;
import org.osm2world.core.target.common.TextureData.Wrap;
import org.osm2world.core.target.common.material.ConfMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.openstreetmap.josm.plugins.graphview.core.data.EmptyTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.MapBasedTagGroup;
import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.openstreetmap.josm.plugins.graphview.core.data.TagGroup;

public class ConfigureMaterialTest {

	@Test
	public void emptyMapTest() {

		ConfMaterial originalMat = new ConfMaterial(Interpolation.FLAT, Color.white);

		/* add a text layer to texture data list */
		originalMat.setTextureDataList(asList( new TextTextureData("%{test}",
				null, 1.0, 1.0, 50.0, 50.0, BLACK, 50.0,
				Wrap.CLAMP, null, false, false)));

		TagGroup tags = new MapBasedTagGroup(new Tag("test", "success"));

		Material newMat = TrafficSignModule.configureMaterial(originalMat, Collections.emptyMap(), tags);

		assertTrue("new material's TextTextureData layer text should have been 'success'. It is "+
				((TextTextureData)newMat.getTextureDataList().get(0)).text+" instead.", ((TextTextureData)newMat.getTextureDataList().get(0)).text.equals("success"));
	}

	@Test
	public void nonEmptyMapTest() {

		ConfMaterial originalMat = new ConfMaterial(Interpolation.FLAT, Color.white);

		/* add a text layer to texture data list */
		originalMat.setTextureDataList(asList( new TextTextureData("%{test} %{test2}",
				null, 1.0, 1.0, 50.0, 50.0, BLACK, 50.0,
				Wrap.CLAMP, null, false, false)));

		Map<String, String> map = new HashMap<>();

		map.put("test", "success1");
		map.put("test2", "success2");

		Material newMat = TrafficSignModule.configureMaterial(originalMat, map, EmptyTagGroup.EMPTY_TAG_GROUP);

		assertTrue("new material's TextTextureData layer text should have been 'success1 success2'. It is "+
				((TextTextureData)newMat.getTextureDataList().get(0)).text+" instead.", ((TextTextureData)newMat.getTextureDataList().get(0)).text.equals("success1 success2"));
	}
}

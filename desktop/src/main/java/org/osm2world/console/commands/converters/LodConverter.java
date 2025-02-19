package org.osm2world.console.commands.converters;

import org.osm2world.scene.mesh.LevelOfDetail;

import picocli.CommandLine;

public class LodConverter implements CommandLine.ITypeConverter<LevelOfDetail> {

	@Override
	public LevelOfDetail convert(String value) throws Exception {

		try {
			value = value.toUpperCase();
			value = value.replace("LOD", "");
			int intValue = Integer.parseInt(value);
			var lod = LevelOfDetail.fromInt(intValue);
			if (lod != null) {
				return lod;
			}
		} catch (NumberFormatException ignored) {}

		throw new IllegalArgumentException("Not a valid LOD value");

	}

}

package org.osm2world.output.tileset;

import java.io.File;

import javax.annotation.Nullable;

import org.osm2world.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.output.common.MeshOutput;

/**
 * creates a tile (with tileset.json) according to the Cesium 3D Tiles specification
 * using a {@link TilesetCreator}.
 */
public class TilesetOutput extends MeshOutput {

	private final TilesetCreator creator;
	private final File outputFile;
	private final @Nullable SimpleClosedShapeXZ bounds;

	public TilesetOutput(TilesetCreator creator, File outputFile, @Nullable SimpleClosedShapeXZ bounds) {
		this.creator = creator;
		this.outputFile = outputFile;
		this.bounds = bounds;
	}

	@Override
	public String toString() {
		return "TilesetOutput(" + outputFile + ")";
	}

	@Override
	public void finish() {
		creator.createTileset(meshStore, outputFile, bounds);
	}

}

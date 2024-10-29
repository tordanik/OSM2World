package org.osm2world.core.target.tileset;

import java.io.File;

import javax.annotation.Nullable;

import org.osm2world.core.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.core.target.common.MeshTarget;

/**
 * creates a tile (with tileset.json) according to the Cesium 3D Tiles specification
 * using a {@link TilesetCreator}.
 */
public class TilesetTarget extends MeshTarget {

	private final TilesetCreator creator;
	private final File outputFile;
	private final @Nullable SimpleClosedShapeXZ bounds;

	public TilesetTarget(TilesetCreator creator, File outputFile, @Nullable SimpleClosedShapeXZ bounds) {
		this.creator = creator;
		this.outputFile = outputFile;
		this.bounds = bounds;
	}

	@Override
	public String toString() {
		return "TilesetTarget(" + outputFile + ")";
	}

	@Override
	public void finish() {
		creator.createTileset(meshStore, outputFile, bounds);
	}

}

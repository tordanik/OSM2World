package org.osm2world.output.tileset;

import static java.util.Objects.requireNonNullElse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import org.cesiumjs.WGS84Util;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.geo.LatLon;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.shapes.AxisAlignedBoundingBoxXYZ;
import org.osm2world.math.shapes.AxisAlignedRectangleXZ;
import org.osm2world.math.shapes.SimpleClosedShapeXZ;
import org.osm2world.output.common.MeshOutput;
import org.osm2world.output.common.compression.Compression;
import org.osm2world.output.gltf.GltfFlavor;
import org.osm2world.output.gltf.GltfOutput;
import org.osm2world.output.tileset.tiles_data.TilesetAsset;
import org.osm2world.output.tileset.tiles_data.TilesetEntry;
import org.osm2world.output.tileset.tiles_data.TilesetParentEntry;
import org.osm2world.output.tileset.tiles_data.TilesetRoot;
import org.osm2world.scene.mesh.MeshStore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * creates tiles according to the Cesium 3D Tiles specification.
 * Uses {@link GltfOutput} to generate the tile content,
 * and creates matching tileset.json files.
 */
public class TilesetOutput extends MeshOutput {

	//TODO: Make configurable
	private static final int NUM_MESHES_FOR_SUBDIVISION_TOP = 100;

	/**
	 * omit the file extension indicating a compressed file from the tileset JSON,
	 * relying on the webserver's rewrite rules
	 */
	private static final boolean TRANSPARENT_COMPRESSION = true;

	private final File outputFile;
	private final GltfFlavor gltfFlavor;
	private final Compression gltfCompression;
	private final org.osm2world.math.geo.MapProjection mapProjection;
	private final @Nullable SimpleClosedShapeXZ bounds;

	/**
	 * @param outputFile  the path for the tileset JSON
	 */
	public TilesetOutput(File outputFile,
			GltfFlavor gltfFlavor, Compression gltfCompression,
			MapProjection mapProjection, @Nullable SimpleClosedShapeXZ bounds) {
		this.outputFile = outputFile;
		this.gltfFlavor = gltfFlavor;
		this.gltfCompression = gltfCompression;
		this.mapProjection = mapProjection;
		this.bounds = bounds;
	}

	@Override
	public String toString() {
		return "TilesetOutput(" + outputFile + ")";
	}

	@Override
	public void finish() {
		createTileset();
	}

	/**
	 * creates both the tileset JSON and the glTF contents
	 */
	private void createTileset() {

		var dataBounds = new AxisAlignedBoundingBoxXYZ(
				meshStore.meshesWithMetadata().stream()
						.flatMap(m -> m.mesh().geometry.asTriangles().vertices().stream())
						.toList());

		SimpleClosedShapeXZ bounds = requireNonNullElse(this.bounds, dataBounds.xz());

		/* write one or more glTF files */

		List<File> tileContentFiles = new ArrayList<>();

		Path outputDir = outputFile.toPath().toAbsolutePath().getParent();
		String extension = gltfFlavor.extension() + gltfCompression.extension();
		String baseFileName = outputFile.getName();
		baseFileName = baseFileName.replaceAll("(?:\\.tileset)?\\.json$", "");

		if (config.getBoolean("subdivideTiles", false)) {

			List<MeshStore.MeshWithMetadata> meshes = meshStore.meshesWithMetadata();
			meshes.sort(new MeshHeightAndSizeComparator());

			File gltfFile0 = outputDir.resolve(baseFileName + "_0" + extension).toFile();
			List<MeshStore.MeshWithMetadata> topMeshes = meshes.subList(0, Math.min(meshes.size(), NUM_MESHES_FOR_SUBDIVISION_TOP));
			writeGltf(gltfFile0, topMeshes, bounds);
			tileContentFiles.add(gltfFile0);

			File gltfFile1 = outputDir.resolve(baseFileName + "_1" + extension).toFile();
			List<MeshStore.MeshWithMetadata> restMeshes = meshes.subList(Math.min(meshes.size(), 100), meshes.size());
			writeGltf(gltfFile1, restMeshes, bounds);
			tileContentFiles.add(gltfFile1);

		} else {

			File gltfFile = outputDir.resolve(baseFileName + extension).toFile();
			writeGltf(gltfFile, meshStore.meshesWithMetadata(), bounds);
			tileContentFiles.add(gltfFile);

		}

		/* write a tileset JSON referencing all written glTF files */

		writeTilesetJson(outputFile, tileContentFiles, mapProjection.getOrigin(), bounds, dataBounds.minY, dataBounds.maxY);

	}

	private void writeGltf(File gltfFile, List<MeshStore.MeshWithMetadata> meshesWithMetadata,
			SimpleClosedShapeXZ bounds) {

		GltfOutput gltfOutput = new GltfOutput(gltfFile, gltfFlavor, gltfCompression, bounds);
		gltfOutput.setConfiguration(config);

		gltfOutput.outputScene(meshesWithMetadata);

	}

	/*
	Working example for tileset
	{
		"asset" : {
			"version": "1.0"
		},
		"root": {
			"content": {
				"uri": "14_5298_5916_0.glb"
			},
			"refine": "ADD",
			"geometricError": 25,
			"boundingVolume": {
				"region": [-1.1098350999480917,0.7790694465970149,-1.1094516048185785,0.779342292568195,0.0,100]
			},
			"transform": [
				0.895540041198885,    0.4449809373551852,  0.0,                0.0,
				-0.31269461895546163, 0.6293090971636892,  0.7114718093525005, 0.0,
				0.3165913926274654,   -0.6371514934593837, 0.7027146394495273, 0.0,
				2022609.150078308,    -4070573.2078238726, 4459382.83869308,   1.0
			],
			"children": [{
				"boundingVolume": {
					"region": [-1.1098350999480917,0.7790694465970149,-1.1094516048185785,0.779342292568195,0.0,97.49999999999997]
				},
				"geometricError": 0,
				"content": {
					"uri": "14_5298_5916.glb"
				}
			}]
		}
	}*/
	private void writeTilesetJson(File outFile, List<File> tileContentFiles, LatLon origin, SimpleClosedShapeXZ bounds, double minY, double maxY) {

		if (TRANSPARENT_COMPRESSION) {
			tileContentFiles = tileContentFiles.stream()
					.map(f -> new File(f.getPath().replaceAll("\\.(?:gz|zip)$", "")))
					.toList();
		}

		VectorXYZ cartesianOrigin = WGS84Util.cartesianFromLatLon(origin, 0.0);
		double[] transform = WGS84Util.eastNorthUpToFixedFrame(cartesianOrigin);

		AxisAlignedRectangleXZ bbox = bounds.boundingBox();

		LatLon westSouth = mapProjection.toLatLon(bbox.bottomLeft());
		LatLon eastNorth = mapProjection.toLatLon(bbox.topRight());

		var boundingRegion = new TilesetEntry.Region(westSouth, eastNorth, minY, maxY);

		TilesetRoot tileset = new TilesetRoot();
		tileset.setAsset(new TilesetAsset());

		TilesetParentEntry root = new TilesetParentEntry();
		tileset.setRoot(root);

		root.setTransform(transform);
		root.setGeometricError(25);
		root.setBoundingVolume(boundingRegion);
		root.setContent(tileContentFiles.get(0).getName());

		if (tileContentFiles.size() > 1) {
			root.addChild(tileContentFiles.get(1).getName());
		}

		try (var writer = new PrintWriter(outFile)) {
			Gson gson = new GsonBuilder().create();
			gson.toJson(tileset, writer);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		}
	}

}

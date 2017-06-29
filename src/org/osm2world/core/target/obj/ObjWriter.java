package org.osm2world.core.target.obj;

import static com.google.common.base.Preconditions.checkArgument;
import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;
import org.jglue.fluentjson.JsonBuilder;
import org.jglue.fluentjson.JsonBuilderFactory;
import org.jglue.fluentjson.Mapper;
import org.osm2world.core.GlobalValues;
import org.osm2world.core.map_data.creation.LatLon;
import org.osm2world.core.map_data.creation.MapProjection;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.WGS84Util;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.TargetProvider;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.core.world.data.WorldObject;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

/**
 * utility class for creating an Wavefront OBJ file
 */
public final class ObjWriter {

	private static final class TiledObjTargetProvider implements TargetProvider<RenderableToObj> {
		private int zLevel;
		private PrintStream mtlStream;
		private File mtlFile;
		private File tilesFolder;
		private MapProjection mapProjection;

		private static final class TiledObjTarget extends ObjTarget {
			private final TargetAndStream result;
			private int objCounter = 0;
			
			public int getObjCounter() {
				return objCounter;
			}
			
			public void incrementObjCounter() {
				objCounter++;
			}
			
			private TiledObjTarget(PrintStream objStream, PrintStream mtlStream, TargetAndStream result) {
				super(objStream, mtlStream);
				this.result = result;
			}

			@Override
			public void drawFace(Material material, List<VectorXYZ> vs, List<VectorXYZ> normals,
				List<List<VectorXZ>> texCoordLists) {
				super.drawFace(material, vs, normals, texCoordLists);
				for (VectorXYZ v : vs) {
					result.maxY = Math.max(result.maxY, v.y);
					result.minY = Math.min(result.minY, v.y);
				}
			}
			
			@Override
			public void beginObject(WorldObject object) {
				super.beginObject(object);
				incrementObjCounter();
			}
			
		}

		private static final class DoubleJsonArrayMapper implements Mapper<Double> {
			@Override
			public JsonBuilder map(Double arg0) {
				return JsonBuilderFactory.buildPrimitive(arg0);
			}
		}

		private static final class TargetAndStream {
			TiledObjTarget target;
			int maxObjects = 10;
			int subLevel = 0;
			AxisAlignedBoundingBoxXZ boundingVolume;
			PrintStream pstream;
			double minY = 0;
			double maxY = 0;
		}

		private final Map<String, List<TargetAndStream>> tile2Target 
				= new HashMap<String, List<TargetAndStream>>();
		private String tilesetPath;
		private boolean generateMeta = false;

		
		private TiledObjTargetProvider(int zLevel, MapProjection mapProjection, 
				File tilesFolder, File mtlFile, PrintStream mtlStream, 
				String tilesetPath) {
			this.zLevel = zLevel;
			this.tilesFolder = tilesFolder.getParentFile();
			this.mtlFile = mtlFile;
			this.mtlStream = mtlStream;
			this.mapProjection = mapProjection;
			this.tilesetPath = tilesetPath;
			this.generateMeta  = (tilesetPath != null);
		}

		@Override
		public Target<RenderableToObj> getTarget(MapElement e){
			AxisAlignedBoundingBoxXZ bbox = e.getAxisAlignedBoundingBoxXZ();
			VectorXZ center = bbox.center();
			double lat = this.mapProjection.calcLat(center);
			double lon = this.mapProjection.calcLon(center);
			
			String tileNumber = getTileNumber(lat, lon, zLevel);
			TargetAndStream tuple = getOrCreateTarget(tileNumber);
			
			if (tuple.boundingVolume == null) {
				tuple.boundingVolume = bbox;
			}
			else {
				tuple.boundingVolume = 
						AxisAlignedBoundingBoxXZ.union(tuple.boundingVolume, bbox);
			}

			return tuple.target;
		}
		
		private TargetAndStream getOrCreateTarget(String tileNumber) {
			if(this.tile2Target.get(tileNumber) == null) {
				this.tile2Target.put(tileNumber, new ArrayList<TargetAndStream>());
				this.tile2Target.get(tileNumber).add(createTargetAndWriter(tileNumber, 0));
			}
			
			List<TargetAndStream> subtargets = this.tile2Target.get(tileNumber);
			
			TargetAndStream activeTarget = subtargets.get(subtargets.size() - 1);
			
			if (activeTarget.target.getObjCounter() > activeTarget.maxObjects) {
				int maxObjects = activeTarget.maxObjects * 4;
				int nextSubLevel = activeTarget.subLevel + 1;
				activeTarget = createTargetAndWriter(tileNumber, subtargets.size());
				activeTarget.maxObjects = maxObjects;
				activeTarget.subLevel = nextSubLevel;
				subtargets.add(activeTarget);
			}
			
			return activeTarget;
		}

		private TargetAndStream createTargetAndWriter(String tileNumber, int subLevel) {
			try {
				File objFile = new File(tilesFolder.getPath() + File.separator
						+ tileNumber + "_" + subLevel + ".obj");
				
				objFile.getParentFile().mkdirs();
				
				if (!objFile.exists()) {
					objFile.createNewFile();
				}
				
				PrintStream objStream = new PrintStream(objFile);
				
				writeObjHeader(objStream, mapProjection);
				
				objStream.println("mtllib " + "../../" + mtlFile.getName() + "\n");
				
				final TargetAndStream result = new TargetAndStream();
				
				result.target = new TiledObjTarget(objStream, mtlStream, result);
				result.pstream = objStream;
				
				return result;

			} catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		private static String getTileNumber(final double lat, final double lon, final int zoom) {
			int xtile = (int) Math.floor((lon + 180) / 360 * (1 << zoom));
			double radLat = Math.toRadians(lat);
			double y = 1 - Math.log(Math.tan(radLat) + 1 / Math.cos(radLat)) / Math.PI;

			int ytile = (int) Math.floor(y / 2 * (1 << zoom));
			if (xtile < 0)
				xtile = 0;
			if (xtile >= (1 << zoom))
				xtile = ((1 << zoom) - 1);
			if (ytile < 0)
				ytile = 0;
			if (ytile >= (1 << zoom))
				ytile = ((1 << zoom) - 1);
			
			return ("" + zoom + File.separator + xtile + File.separator + ytile);
		}
		
		AxisAlignedBoundingBoxXZ tile2boundingBox(final int x, final int y, final int zoom) {
			
			double north = tile2lat(y, zoom);
			double south = tile2lat(y + 1, zoom);
			double west = tile2lon(x, zoom);
			double east = tile2lon(x + 1, zoom);
			
			return new AxisAlignedBoundingBoxXZ(west, south, east, north);
		}

		static double tile2lon(int x, int z) {
			return x / Math.pow(2.0, z) * 360.0 - 180;
		}

		static double tile2lat(int y, int z) {
			double n = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
			return Math.toDegrees(Math.atan(Math.sinh(n)));
		}

		@Override
		public void close() {
			
			AxisAlignedBoundingBoxXZ fullBBOX = null;
			double minY = 0;
			double maxY = 0;
			
			JsonArray children = new JsonArray();
			
			for (Entry<String, List<TargetAndStream>> entry : this.tile2Target.entrySet()) {
				for (TargetAndStream t : entry.getValue()) {
					
					t.pstream.close();
					
					minY = Math.min(t.minY, minY);
					maxY = Math.max(t.maxY, maxY);
				}
				
				if (generateMeta) {
					fullBBOX = generateMetaInfoForTile(children, entry.getKey(), entry.getValue(), fullBBOX);
				}
			}
			
			if (generateMeta) {
				writeTileset(fullBBOX, minY, maxY, children);
			}
		}

		private void writeTileset(AxisAlignedBoundingBoxXZ fullBBOX, 
				double minY, double maxY, JsonArray children) {
			
			File tilesetFile = new File(this.tilesetPath);
			
			File metaFile = new File(this.tilesFolder.getPath() + 
					File.separator + this.tilesetPath);

			if (tilesetFile.isAbsolute()) {
				metaFile = tilesetFile;
			}
			
			LatLon origin = this.mapProjection.getOrigin();
			VectorXYZ cartesianOrigin = WGS84Util.cartesianFromLatLon(origin, 0.0);
			double[] transform = WGS84Util.eastNorthUpToFixedFrame(cartesianOrigin);

			List<Double> regionList = Arrays.asList(ArrayUtils.toObject(encodeRegion(minY, maxY, fullBBOX)));
			List<Double> transformList = Arrays.asList(ArrayUtils.toObject(transform));
			double extentDiagonal = maxY * 4;
			
			JsonObject tileset = JsonBuilderFactory.buildObject()
				.addObject("asset")
					.add("version", "0.0")
				.end()
				.addObject("root")
					.addObject("boundingVolume")
						.addArray("region").addAll(new DoubleJsonArrayMapper(), regionList)
						.end()
					.end()
					.addArray("transform").addAll(new DoubleJsonArrayMapper(), transformList)
					.end()
					.add("refine", "ADD")
					.add("geometricError", extentDiagonal)
				.end()
				.add("geometricError", extentDiagonal)
			.getJson();
			
			tileset.getAsJsonObject("root").add("children", children);
			
			try {
				PrintWriter metaWriter = new PrintWriter(new FileOutputStream(metaFile));
				metaWriter.println(tileset.toString());
				metaWriter.flush();
				metaWriter.close();
			}
			catch (FileNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		private AxisAlignedBoundingBoxXZ generateMetaInfoForTile(JsonArray children, 
				String tms, List<TargetAndStream> sublevels, AxisAlignedBoundingBoxXZ fullBBOX) {
			
			if (sublevels.size() == 1 && sublevels.get(0).maxY == 0.0) {
				// Don't write epty tiles
				return fullBBOX;
			}

			String[] tmsSplit = tms.split("/");
			
			Integer tileX = Integer.valueOf(tmsSplit[1]);
			Integer tileY = Integer.valueOf(tmsSplit[2]);
			Integer tileZ = Integer.valueOf(tmsSplit[0]);
			
			AxisAlignedBoundingBoxXZ tileBBOX = 
					tile2boundingBox(tileX, tileY, tileZ);
			
			// We'll write from the bigest to smallest sublevels
			// because upper levels includes down levels as children 
			Collections.reverse(sublevels);
			
			JsonObject prevSubtile = null;
			int sublevel = 0;
			for (TargetAndStream t : sublevels) {
				
				// Union content bbox with tile bbox, to ensure that content is fully within
				AxisAlignedBoundingBoxXZ union = AxisAlignedBoundingBoxXZ.union(t.boundingVolume, tileBBOX);
				double[] region = encodeRegion(t.minY, t.maxY, union);
				
				// Ensure that whole tileset bbox covers all the children bboxes and content
				fullBBOX = fullBBOX == null ? union : AxisAlignedBoundingBoxXZ.union(union, fullBBOX);
				
				// Tight bbox contains only content
				double[] contentRegion = encodeRegion(t.minY, t.maxY, t.boundingVolume);
				
				JsonObject tileBoundingVolume = getRegionAsBVJsonObject(region);
				JsonObject contentBoundingVolume = getRegionAsBVJsonObject(contentRegion);
				
				double geometricError = t.maxY * Double.valueOf(sublevel);
				
				JsonObject meta = new JsonObject();
				meta.add("boundingVolume", tileBoundingVolume);
				meta.add("geometricError", new JsonPrimitive(geometricError));
				
				JsonObject content = new JsonObject();
				content.add("url", new JsonPrimitive(tms + "_" + t.subLevel + ".b3dm"));
				content.add("objects", new JsonPrimitive(t.target.getObjCounter()));
				content.add("boundingVolume", contentBoundingVolume);
				
				meta.add("content", content);

				if (prevSubtile != null) {
					JsonArray ca = new JsonArray();
					ca.add(prevSubtile);
					meta.add("children", ca);
				}
				
				prevSubtile = meta;
				sublevel ++;
			}
			
			if (prevSubtile != null) {
				children.add(prevSubtile);
			}
			
			return fullBBOX;
		}

		private JsonObject getRegionAsBVJsonObject(double[] region) {
			List<Double> asList = Arrays.asList(ArrayUtils.toObject(region));
			
			return JsonBuilderFactory.buildObject()
				.addArray("region")
					.addAll(new DoubleJsonArrayMapper(), asList)
				.end()
				.getJson();
		}

		private double[] encodeRegion(double minY, double maxY, AxisAlignedBoundingBoxXZ union) {
			VectorXZ bottomRight = union.bottomRight();
			VectorXZ topLeft = union.topLeft();

			double west = this.mapProjection.calcLon(topLeft);
			double east = this.mapProjection.calcLon(bottomRight);
			double north = this.mapProjection.calcLat(topLeft);
			double south = this.mapProjection.calcLat(bottomRight);
			
			double[] region = new double[]{
				Math.toRadians(west), 
				Math.toRadians(south), 
				Math.toRadians(east), 
				Math.toRadians(north), 
				minY, maxY
			};
			return region;
		}
	}

	/** prevents instantiation */
	private ObjWriter() { }
	
	public static final void writeObjFile(
			File objFile, MapData mapData,
			MapProjection mapProjection,
			Camera camera, Projection projection, boolean underground)
			throws IOException {
		
		if (!objFile.exists()) {
			objFile.createNewFile();
		}
		
		File mtlFile = new File(objFile.getAbsoluteFile() + ".mtl");
		if (!mtlFile.exists()) {
			mtlFile.createNewFile();
		}
		
		PrintStream objStream = new PrintStream(objFile);
		PrintStream mtlStream = new PrintStream(mtlFile);
		
		/* write comments at the beginning of both files */
		
		writeObjHeader(objStream, mapProjection);
		
		writeMtlHeader(mtlStream);
				
		/* write path of mtl file to obj file */
		
		objStream.println("mtllib " + mtlFile.getName() + "\n");
		
		/* write actual file content */
		
		ObjTarget target = new ObjTarget(objStream, mtlStream);
                
		TargetUtil.renderWorldObjects(target, mapData, underground);
		
		objStream.close();
		mtlStream.close();
		
	}

	public static final void writeObjFileTiled(
			final File objDirectory, MapData mapData,
			final MapProjection mapProjection,
			Camera camera, Projection projection, 
			int tilesZoomLevel, String tilesetPath)
			throws IOException {
					
		if (!objDirectory.getParentFile().exists()) {
			objDirectory.getParentFile().mkdir();
		}
		
		String tilesetName = objDirectory.getName().replace(".obj", "");
		
		checkArgument(objDirectory.getParentFile().isDirectory());
		
		final File mtlFile = new File(objDirectory.getParentFile().getPath()
				+ File.separator + tilesetName + "_materials.mtl");
		if (!mtlFile.exists()) {
			mtlFile.createNewFile();
		}
		
		final PrintStream mtlStream = new PrintStream(mtlFile);
		
		writeMtlHeader(mtlStream);
		
		/* create iterator which creates and wraps .obj files as needed */
		TargetProvider<RenderableToObj> targetProvider = 
				new TiledObjTargetProvider(tilesZoomLevel, 
						mapProjection, objDirectory, mtlFile, mtlStream, tilesetPath);		
		
		/* write file content */
		
		TargetUtil.renderWorldObjectsTiles(targetProvider, mapData);
		
		mtlStream.close();
		targetProvider.close();
		
	}
	
	public static final void writeObjFiles(
			final File objDirectory, MapData mapData,
			final MapProjection mapProjection,
			Camera camera, Projection projection,
			int primitiveThresholdPerFile)
			throws IOException {
					
		if (!objDirectory.exists()) {
			objDirectory.mkdir();
		}
		
		checkArgument(objDirectory.isDirectory());
		
		final File mtlFile = new File(objDirectory.getPath()
				+ File.separator + "materials.mtl");
		if (!mtlFile.exists()) {
			mtlFile.createNewFile();
		}
		
		final PrintStream mtlStream = new PrintStream(mtlFile);
		
		writeMtlHeader(mtlStream);
		
		/* create iterator which creates and wraps .obj files as needed */
				
		Iterator<ObjTarget> objIterator = new Iterator<ObjTarget>() {

			private int fileCounter = 0;
			PrintStream objStream = null;

			@Override
			public boolean hasNext() {
				return true;
			}

			@Override
			public ObjTarget next() {

				try {
					
					if (objStream != null) {
						objStream.close();
						fileCounter ++;
					}
					
					File objFile = new File(objDirectory.getPath() + File.separator
							+ "part" + format("%04d", fileCounter) + ".obj");
					
					if (!objFile.exists()) {
						objFile.createNewFile();
					}
					
					objStream = new PrintStream(objFile);
					
					writeObjHeader(objStream, mapProjection);
	
					objStream.println("mtllib " + mtlFile.getName() + "\n");
					
					return new ObjTarget(objStream, mtlStream);
					
				} catch (FileNotFoundException e) {
					throw new RuntimeException(e);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
			
		};
		
		/* write file content */
		
		TargetUtil.renderWorldObjects(objIterator, mapData, primitiveThresholdPerFile);
		
		mtlStream.close();
		
	}

	private static final void writeObjHeader(PrintStream objStream,
			MapProjection mapProjection) {
		
		objStream.println("# This file was created by OSM2World "
				+ GlobalValues.VERSION_STRING + " - "
				+ GlobalValues.OSM2WORLD_URI + "\n");
		objStream.println("# Projection information:");
		objStream.println("# Coordinate origin (0,0,0): "
				+ "lat " + mapProjection.calcLat(VectorXZ.NULL_VECTOR) + ", "
				+ "lon " + mapProjection.calcLon(VectorXZ.NULL_VECTOR) + ", "
				+ "ele 0");
		objStream.println("# North direction: " + new VectorXYZ(
						mapProjection.getNorthUnit().x, 0,
						- mapProjection.getNorthUnit().z));
		objStream.println("# 1 coordinate unit corresponds to roughly "
				+ "1 m in reality\n");
		
	}

	private static final void writeMtlHeader(PrintStream mtlStream) {
		
		mtlStream.println("# This file was created by OSM2World "
				+ GlobalValues.VERSION_STRING + " - "
				+ GlobalValues.OSM2WORLD_URI + "\n\n");
		
	}
			
}

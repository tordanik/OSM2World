package org.osm2world;

import static java.lang.Math.abs;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static java.util.Comparator.comparingDouble;
import static java.util.Objects.requireNonNullElse;
import static org.osm2world.math.shapes.AxisAlignedRectangleXZ.bbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.annotation.Nullable;

import org.osm2world.conversion.ConfigUtil;
import org.osm2world.conversion.ConversionLog;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.conversion.ProgressListener;
import org.osm2world.map_data.creation.OSMToMapDataConverter;
import org.osm2world.map_data.data.MapData;
import org.osm2world.map_elevation.creation.*;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.datastructures.IndexGrid;
import org.osm2world.math.datastructures.SpatialIndex;
import org.osm2world.math.geo.GeoBounds;
import org.osm2world.math.geo.MapProjection;
import org.osm2world.math.geo.TileNumber;
import org.osm2world.math.shapes.FlatSimplePolygonShapeXYZ;
import org.osm2world.osm.creation.OSMDataReader;
import org.osm2world.osm.data.OSMData;
import org.osm2world.output.Output;
import org.osm2world.output.common.material.Materials;
import org.osm2world.output.common.model.Models;
import org.osm2world.scene.Scene;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.creation.WorldCreator;
import org.osm2world.world.creation.WorldModule;
import org.osm2world.world.data.WorldObject;
import org.osm2world.world.modules.*;
import org.osm2world.world.modules.building.BuildingModule;
import org.osm2world.world.modules.building.indoor.IndoorModule;
import org.osm2world.world.modules.traffic_sign.TrafficSignModule;

import com.google.common.collect.Streams;

import de.topobyte.osm4j.core.resolve.EntityNotFoundException;

/**
 * Implementation of {@link O2WConverter}.
 * Provides an easy way to call all steps of the conversion process in the correct order.
 */
class O2WConverterImpl {

	private final O2WConfig config;
	private final List<ProgressListener> listeners;

	O2WConverterImpl(@Nullable O2WConfig config, List<ProgressListener> listeners) {
		this.config = requireNonNullElse(config, new O2WConfig());
		this.listeners = new ArrayList<>(listeners);
	}

	/**
	 * implementation of {@link O2WConverter#convert(OSMDataReader, GeoBounds, MapProjection, Output...)}
	 */
	Scene convert(OSMDataReader osmDataReader, GeoBounds bounds, MapProjection mapProjection, Output[] outputs)
			throws IOException{

		/* load OSM data */

		updatePhase(ProgressListener.Phase.MAP_DATA);

		OSMData osmData;

		if (bounds instanceof TileNumber tile) {
			osmData = osmDataReader.getData(tile);
		} else if (bounds != null) {
			osmData = osmDataReader.getData(bounds.latLonBounds());
		} else {
			osmData = osmDataReader.getAllData();
		}

		/* create map data from OSM data */

		mapProjection = requireNonNullElse(mapProjection, config.mapProjection().apply(osmData.getCenter()));

		OSMToMapDataConverter converter = new OSMToMapDataConverter(mapProjection);

		try {

			MapData mapData = converter.createMapData(osmData, config);

			return this.convert(mapData, mapProjection, outputs);

		} catch (EntityNotFoundException e) {
			throw new IOException(e);
		}

	}

	/**
	 * implementation of {@link O2WConverter#convert(MapData, MapProjection, Output...)}
	 */
	Scene convert(MapData mapData, @Nullable MapProjection mapProjection, Output[] outputs) {

		outputs = requireNonNullElse(outputs, new Output[0]);

		/* apply world modules */
		updatePhase(ProgressListener.Phase.REPRESENTATION);

		ConfigUtil.parseFonts(config);
		Materials.configureMaterials(config);
		Models.configureModels(config);
			//this will cause problems if multiple conversions are run
			//at the same time, because global variables are being modified

		WorldCreator moduleManager = new WorldCreator(config, createModuleList(config));
		moduleManager.addRepresentationsTo(mapData);

		/* determine elevations */
		updatePhase(ProgressListener.Phase.ELEVATION);

		TerrainElevationData eleData = null;

		if (config.srtmDir() != null) {
			if (mapProjection == null) {
				throw new IllegalArgumentException("Using SRTM data requires a map projection");
			}
			eleData = new SRTMData(config.srtmDir(), mapProjection);
		}

		/* create terrain and attach connectors */
		updatePhase(ProgressListener.Phase.TERRAIN);

		calculateElevations(mapData, eleData, config);
		attachConnectors(mapData);

		/* convert 3d scene to target representation */
		updatePhase(ProgressListener.Phase.OUTPUT);

		var scene = new Scene(mapProjection, mapData);

		/* supply results to outputs */

		for (Output output : outputs) {
			output.setConfiguration(config);
			output.outputScene(scene);
		}

		updatePhase(ProgressListener.Phase.FINISHED);

		return scene;

	}

	/**
	 * generates the list of {@link WorldModule}s for the conversion
	 */
	private static List<WorldModule> createModuleList(O2WConfig config) {

		List<String> excludedModules = config.getList("excludeWorldModule")
				.stream().map(Object::toString).toList();

		return Stream.of((WorldModule)
						new RoadModule(),
						new RailwayModule(),
						new AerowayModule(),
						new BuildingModule(),
						new ParkingModule(),
						new TreeModule(),
						new StreetFurnitureModule(),
						new TrafficSignModule(),
						new BicycleParkingModule(),
						new WaterModule(),
						new PoolModule(),
						new GolfModule(),
						new SportsModule(),
						new CliffModule(),
						new BarrierModule(),
						new PowerModule(),
						new MastModule(),
						new BridgeModule(),
						new TunnelModule(),
						new SurfaceAreaModule(),
						new InvisibleModule(),
						new IndoorModule()
				)
				.filter(m -> !excludedModules.contains(m.getClass().getSimpleName()))
				.toList();

	}

	private void attachConnectors(MapData mapData) {

		/* collect the surfaces */

		SpatialIndex<AttachmentSurface> attachmentSurfaceIndex =
				new IndexGrid<>(mapData.getDataBoundary().pad(50), 100, 100);

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), object -> {
			if (object.getParent() == null) {
				object.getAttachmentSurfaces().forEach(attachmentSurfaceIndex::insert);
			}
		});

		/* attach connectors to the surfaces */

		for (WorldObject object : mapData.getWorldObjects()) {

			if (object.getParent() != null) continue;

			for (AttachmentConnector connector : object.getAttachmentConnectors()) {

				for (String surfaceType : connector.compatibleSurfaceTypes) {

					Iterable<AttachmentSurface> nearbySurfaces = attachmentSurfaceIndex.probe(
							bbox(singleton(connector.originalPos)).pad(connector.maxDistanceXZ()));

					Optional<AttachmentSurface> closestSurface = Streams.stream(nearbySurfaces)
							.filter(s -> s.getTypes().stream().anyMatch(t -> t.equals(surfaceType)))
							.filter(s -> s.getFaces().stream().anyMatch(f -> connector.isAcceptableNormal.test(f.getNormal())))
							.min(comparingDouble(s -> s.distanceTo(connector.originalPos)));

					if (closestSurface.isPresent()) {
						attachConnectorIfValid(connector, closestSurface.get());
						break;
					}
				}

			}

		}

	}

	static void attachConnectorIfValid(AttachmentConnector connector, AttachmentSurface surface) {

		double ele = surface.getBaseEleAt(connector.originalPos.xz()) + connector.preferredHeight;
		VectorXYZ posAtEle = connector.originalPos.y(ele);

		for (boolean requirePreferredHeight : asList(true, false)) {

			Predicate<FlatSimplePolygonShapeXYZ> matchesPreferredHeight = f -> {
				if (!requirePreferredHeight) {
					return true;
				} else {
					VectorXYZ closestPoint = f.closestPoint(posAtEle);
					double height = closestPoint.y - surface.getBaseEleAt(closestPoint.xz());
					return abs(height - connector.preferredHeight) < 0.001;
				}
			};

			Optional<? extends FlatSimplePolygonShapeXYZ> closestFace = surface.getFaces().stream()
					.filter(matchesPreferredHeight)
					.filter(f -> connector.isAcceptableNormal.test(f.getNormal()))
					.min(comparingDouble(f -> connector.changeXZ ? f.distanceTo(posAtEle) : f.distanceToXZ(posAtEle)));

			if (closestFace.isEmpty()) continue; // try again without enforcing the preferred height

			VectorXYZ closestPoint;

			if (!connector.changeXZ && closestFace.get().getNormal().y >= 0.001) {
				// no XZ movement is desired, obtain the face point directly above/below the connector
				VectorXYZ pointInFacePlane = posAtEle.y(closestFace.get().getYAt(posAtEle.xz()));
				closestPoint = closestFace.get().closestPoint(pointInFacePlane);
			} else {
				closestPoint = closestFace.get().closestPoint(posAtEle);
			}

			if (closestPoint.xz().distanceTo(connector.originalPos.xz()) > connector.maxDistanceXZ() + 0.001) {
				continue;
			}

			connector.attach(surface, closestPoint, closestFace.get().getNormal());
			break; // attached, don't try again

		}

	}

	/**
	 * uses OSM data and a terrain elevation data (usually from an external
	 * source) to calculate elevations for all {@link EleConnector}s of the
	 * {@link WorldObject}s
	 */
	private void calculateElevations(MapData mapData,
			TerrainElevationData eleData, O2WConfig config) {

		TerrainInterpolator interpolator = config.terrainInterpolator().get();

		if (eleData == null) {
			interpolator = new ZeroInterpolator();
		}

		/* provide known elevations from eleData to the interpolator */

		if (!(interpolator instanceof ZeroInterpolator)) {

			Collection<VectorXYZ> sites = emptyList();

			try {
				sites = eleData.getSites(mapData.getDataBoundary().pad(10));
			} catch (IOException e) {
				ConversionLog.error("Could not read elevation data: " + e.getMessage(), e);
			}

			if (!sites.isEmpty()) {
				interpolator.setKnownSites(sites);
			} else {
				ConversionLog.error("No sites with known elevation available");
				interpolator = new ZeroInterpolator();
			}

		}

		/* interpolate terrain elevation for each connector */

		final TerrainInterpolator finalInterpolator = interpolator;

		FaultTolerantIterationUtil.forEach(mapData.getWorldObjects(), (WorldObject worldObject) -> {
			for (EleConnector conn : worldObject.getEleConnectors()) {
				conn.setPosXYZ(finalInterpolator.interpolateEle(conn.pos));
			}
		});

		/* refine terrain-based elevation with information from map data */

		EleCalculator eleCalculator = config.eleCalculator().get();
		eleCalculator.calculateElevations(mapData);

	}

	private void updatePhase(ProgressListener.Phase newPhase) {
		for (ProgressListener listener : listeners) {
			double progress = newPhase.ordinal() * 1.0 / (ProgressListener.Phase.values().length - 1);
			listener.updateProgress(newPhase, progress);
		}
	}

}

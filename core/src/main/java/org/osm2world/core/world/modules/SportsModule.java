package org.osm2world.core.world.modules;

import static java.lang.Math.abs;
import static java.lang.Math.cos;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.osm2world.core.math.algorithms.GeometryUtil.interpolateBetween;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.STRIP_FIT_HEIGHT;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.triangleTexCoordLists;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.math.shapes.SimplePolygonXZ;
import org.osm2world.core.math.shapes.TriangleXYZ;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.TextureDataDimensions;
import org.osm2world.core.target.common.material.TextureLayer;
import org.osm2world.core.target.common.mesh.ExtrusionGeometry;
import org.osm2world.core.target.common.mesh.LevelOfDetail;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.common.model.ModelInstance;
import org.osm2world.core.target.common.texcoord.NamedTexCoordFunction;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;
import org.osm2world.core.target.common.texcoord.TexCoordUtil;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.ProceduralWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * adds pitches for various sports to the map
 */
public class SportsModule extends AbstractModule {

	@Override
	public void applyToArea(MapArea area) {

		if (area.getTags().contains("leisure", "pitch")) {

			String sport = area.getTags().getValue("sport");

			if ("soccer".equals(sport)) {
				area.addRepresentation(new SoccerPitch(area));
			} else if ("tennis".equals(sport)) {
				area.addRepresentation(new TennisPitch(area));
			} else if ("beachvolleyball".equals(sport)) {
				area.addRepresentation(new BeachVolleyballPitch(area));
			}

		}

	}

	/**
	 * a pitch with markings for any sport
	 */
	static abstract class Pitch extends AbstractAreaWorldObject
			implements ProceduralWorldObject {

		public Pitch(MapArea area) {

			super(area);

		}

		/** minimum length of the pitch's longer side, in meters */
		protected abstract double getMinLongSide();

		/** maximum length of the pitch's longer side, in meters */
		protected abstract double getMaxLongSide();

		/** minimum length of the pitch's shorter side, in meters */
		protected abstract double getMinShortSide();

		/** maximum length of the pitch's shorter side, in meters */
		protected abstract double getMaxShortSide();

		/** the regular material for the pitch */
		protected abstract Material getPitchMaterial();

		/**
		 * the fallback material to be used instead of {@link #getPitchMaterial()}
		 * if no legal pitch can be constructed.
		 * This is usually just the base material (such as grass) without the markings.
		 */
		protected abstract Material getFallbackPitchMaterial();

		@Override
		public Collection<PolygonShapeXZ> getRawGroundFootprint() {
			return List.of(getOutlinePolygonXZ());
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			List<TriangleXYZ> triangles = getTriangulation();

			List<TextureLayer> layers = getPitchMaterial().getTextureLayers();
			TexCoordFunction texFunction = configureTexFunction(area.getOuterPolygon(), layers.get(layers.size() - 1).baseColorTexture.dimensions());

			if (texFunction != null) {

				Material material = getPitchMaterial();

				target.drawTriangles(material, triangles,
						triangleTexCoordLists(triangles, material, t -> texFunction));

			} else {

				Material material = getFallbackPitchMaterial();

				target.drawTriangles(material, triangles,
						triangleTexCoordLists(triangles, material, NamedTexCoordFunction.GLOBAL_X_Z));

			}

		}

		/**
		 * calculates the parameters for a {@link PitchTexFunction}
		 * and calls the constructor
		 *
		 * @return  the texture coordinate function;
		 * null if it's not possible to construct a valid pitch
		 */
		protected PitchTexFunction configureTexFunction(SimplePolygonXZ polygon,
														TextureDataDimensions textureDimensions) {

			/* approximate a rectangular shape for the pitch */

			SimplePolygonXZ bbox = polygon.minimumRotatedBoundingBox();

			VectorXZ origin = bbox.getVertex(0);

			VectorXZ longSide, shortSide;

			if (bbox.getVertex(0).distanceTo(bbox.getVertex(1))
					> bbox.getVertex(1).distanceTo(bbox.getVertex(2))) {

				longSide = bbox.getVertex(1).subtract(bbox.getVertex(0));
				shortSide = bbox.getVertex(2).subtract(bbox.getVertex(1));

			} else {

				longSide = bbox.getVertex(2).subtract(bbox.getVertex(1));
				shortSide = bbox.getVertex(1).subtract(bbox.getVertex(0));

			}

			/* scale the pitch markings based on official regulations (TODO use values from config file) */

			double newLongSideLength = longSide.length() * 0.95;
			double newShortSideLength = shortSide.length() * 0.95;

			if (newLongSideLength < getMinLongSide()) {
				return null;
			} else if (newLongSideLength > getMaxLongSide()) {
				newLongSideLength = getMaxLongSide();
			}

			if (newShortSideLength < getMinShortSide()) {
				return null;
			} else if (newShortSideLength > getMaxShortSide()) {
				newShortSideLength = getMaxShortSide();
			}

			origin = origin
					.add(longSide.mult((longSide.length() - newLongSideLength) / 2 / longSide.length()))
					.add(shortSide.mult((shortSide.length() - newShortSideLength) / 2 / shortSide.length()));

			longSide = longSide.mult(newLongSideLength / longSide.length());
			shortSide = shortSide.mult(newShortSideLength / shortSide.length());

			/* build the result */

			return new PitchTexFunction(origin, longSide, shortSide, textureDimensions);

		}

		/**
		 * specialized texture coordinate calculation for pitch marking textures
		 */
		static class PitchTexFunction implements TexCoordFunction {

			private final VectorXZ origin;
			private final VectorXZ longSide;
			private final VectorXZ shortSide;
			private final TextureDataDimensions textureDimensions;

			PitchTexFunction(VectorXZ origin, VectorXZ longSide, VectorXZ shortSide,
							 TextureDataDimensions textureDimensions) {

				this.origin = origin;
				this.longSide = longSide;
				this.shortSide = shortSide;
				this.textureDimensions = textureDimensions;

			}

			public VectorXZ getLongSide() {
				return longSide;
			}

			public VectorXZ getShortSide() {
				return shortSide;
			}

			public VectorXZ getOrigin() {
				return origin;
			}

			@Override
			public List<VectorXZ> apply(List<VectorXYZ> vs) {

				List<VectorXZ> result = new ArrayList<VectorXZ>(vs.size());

				for (VectorXYZ vOriginal : vs) {

					VectorXZ v = vOriginal.xz().subtract(origin);

					double angleLong = VectorXZ.angleBetween(v, longSide);
					double longSideProjectedLength = v.length() * cos(angleLong);

					double angleShort = VectorXZ.angleBetween(v, shortSide);
					double shortSideProjectedLength = v.length() * cos(angleShort);

					VectorXZ rawTexCoord = new VectorXZ(
									shortSideProjectedLength / shortSide.length(),
									longSideProjectedLength / longSide.length());

					result.add(TexCoordUtil.applyPadding(rawTexCoord, textureDimensions));

				}

				return result;

			}

		}

	}

	/**
	 * a pitch with soccer markings
	 */
	class SoccerPitch extends Pitch {

		SoccerPitch(MapArea area) {
			super(area);
		}

		@Override
		protected double getMinLongSide() {
			return 90;

		}

		@Override
		protected double getMaxLongSide() {
			return 120;
		}

		@Override
		protected double getMinShortSide() {
			return 45;
		}

		@Override
		protected double getMaxShortSide() {
			return 90;
		}

		@Override
		protected Material getPitchMaterial() {
			return PITCH_SOCCER;
		}

		@Override
		protected Material getFallbackPitchMaterial() {
			return GRASS;
		}

	}

	/**
	 * a pitch with tennis markings
	 */
	class TennisPitch extends Pitch {

		private final double netHeightAtPosts = 1.07;
		private final double netHeightCenter = 0.91;
		private final double postRadius = 0.04;

		private final Model tennisNetPost = new Model() {
			@Override
			public List<Mesh> buildMeshes(InstanceParameters params) {
				return singletonList(new Mesh(ExtrusionGeometry.createColumn(
						null, params.position(), netHeightAtPosts, postRadius, postRadius, false, true,
						new Color(184, 184, 184), PLASTIC.getTextureDimensions()), PLASTIC,
						LevelOfDetail.LOD2, LevelOfDetail.LOD4));
			}
		};

		TennisPitch(MapArea area) {
			super(area);
		}

		@Override
		protected double getMinLongSide() {
			return 17;

		}

		@Override
		protected double getMaxLongSide() {
			return 23.77; //regulation size
		}

		@Override
		protected double getMinShortSide() {
			boolean singles = area.getTags().contains("tennis", "single");
			return singles ? 6 : 8;
		}

		@Override
		protected double getMaxShortSide() {
			boolean singles = area.getTags().contains("tennis", "single");
			return singles ? 8.23 : 10.97; //regulation size
		}

		@Override
		protected Material getPitchMaterial() {

			boolean singles = area.getTags().contains("tennis", "single");
			String surface = area.getTags().getValue("surface");

			if ("grass".equals(surface)) {
				return singles ? PITCH_TENNIS_SINGLES_GRASS : PITCH_TENNIS_GRASS;
			} else if ("asphalt".equals(surface)) {
				return singles ? PITCH_TENNIS_SINGLES_ASPHALT : PITCH_TENNIS_ASPHALT;
			} else {
				return singles ? PITCH_TENNIS_SINGLES_CLAY : PITCH_TENNIS_CLAY;
			}

		}

		@Override
		protected Material getFallbackPitchMaterial() {

			String surface = area.getTags().getValue("surface");

			if ("grass".equals(surface)) {
				return GRASS;
			} else if ("asphalt".equals(surface)) {
				return ASPHALT;
			} else {
				return EARTH;
			}

		}

		@Override
		public void buildMeshesAndModels(Target target) {

			/* let the supertype draw the pitch surface */

			super.buildMeshesAndModels(target);

			/* add a net with posts */

			List<TextureLayer> layers = getPitchMaterial().getTextureLayers();
			PitchTexFunction texFunction = configureTexFunction(area.getOuterPolygon(), layers.get(layers.size() - 1).baseColorTexture.dimensions());

			//TODO: support this feature when elevation is enabled

			boolean renderNet = true;

			for (EleConnector connector : getEleConnectors()) {
				if (abs(connector.getPosXYZ().y) > 0.01) {
					renderNet = false;
					break;
				}
			}

			double ele = 0;

			if (getConnectorIfAttached() != null) {
					ele = getConnectorIfAttached().getAttachedPos().getY();
					renderNet = true;
			}

			if (texFunction != null && renderNet) {

				VectorXYZ postPositionA = texFunction.getOrigin()
						.add(texFunction.getLongSide().mult(0.5)).xyz(ele);

				VectorXYZ postPositionB = postPositionA.add(texFunction.getShortSide());

				VectorXZ shortSideNormalized = texFunction.getShortSide().normalize();

				postPositionA = postPositionA.add(shortSideNormalized.invert());
				postPositionB = postPositionB.add(shortSideNormalized);

				/* add two posts */

				for (VectorXYZ postPosition : asList(postPositionA, postPositionB)) {
					target.addSubModel(new ModelInstance(tennisNetPost, new InstanceParameters(postPosition, 0)));
				}

				/* draw the net (with an approximated droop in the center) */

				target.setCurrentLodRange(LevelOfDetail.LOD2, LevelOfDetail.LOD4);

				final int numInterpolatedPoints = 20;

				List<VectorXYZ> netBottom = new ArrayList<VectorXYZ>();
				List<VectorXYZ> netTop = new ArrayList<VectorXYZ>();

				for (int i = 0; i < numInterpolatedPoints; i++) {

					double ratio = (double)i / (numInterpolatedPoints - 1);
					VectorXYZ v = interpolateBetween(postPositionA, postPositionB, ratio);

					double heightFactor = (2 * ratio - 1) * (2 * ratio - 1);
					double height = netHeightCenter + heightFactor * (netHeightAtPosts - netHeightCenter);

					netBottom.add(v);
					netTop.add(v.addY(height));

				}

				List<VectorXYZ> verticesNet = new ArrayList<VectorXYZ>();
				List<VectorXYZ> verticesNetBack = new ArrayList<VectorXYZ>();

				for (int i = 0; i < numInterpolatedPoints; i++) {

					verticesNet.add(netTop.get(i));
					verticesNet.add(netBottom.get(i));

					verticesNetBack.add(0, netBottom.get(i));
					verticesNetBack.add(0, netTop.get(i));

				}

				target.drawTriangleStrip(TENNIS_NET, verticesNet,
						texCoordLists(verticesNet, TENNIS_NET, STRIP_FIT_HEIGHT));

				if (!TENNIS_NET.isDoubleSided()) {
					target.drawTriangleStrip(TENNIS_NET, verticesNetBack,
							texCoordLists(verticesNetBack, TENNIS_NET, STRIP_FIT_HEIGHT));
				}

			}

		}

	}

	/**
	 * a pitch with beach volleyball markings
	 */
	class BeachVolleyballPitch extends Pitch {

		BeachVolleyballPitch(MapArea area) {
			super(area);
		}

		@Override
		protected double getMinLongSide() {
			return 12;
		}

		@Override
		protected double getMaxLongSide() {
			return 16; //regulation size
		}

		@Override
		protected double getMinShortSide() {
			return 6;
		}

		@Override
		protected double getMaxShortSide() {
			return 8; //regulation size
		}

		@Override
		protected Material getPitchMaterial() {
			return PITCH_BEACHVOLLEYBALL;
		}

		@Override
		protected Material getFallbackPitchMaterial() {
			return SAND;
		}

	}

}

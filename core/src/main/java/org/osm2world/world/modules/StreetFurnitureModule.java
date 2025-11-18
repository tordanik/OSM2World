package org.osm2world.world.modules;

import static java.lang.Math.*;
import static java.lang.Math.min;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.math.NumberUtils.max;
import static org.osm2world.math.VectorXYZ.Y_UNIT;
import static org.osm2world.math.VectorXYZ.Z_UNIT;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.math.algorithms.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.math.algorithms.TriangulationUtil.triangulate;
import static org.osm2world.math.shapes.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.output.common.ExtrudeOption.END_CAP;
import static org.osm2world.output.common.ExtrudeOption.START_CAP;
import static org.osm2world.scene.color.Color.*;
import static org.osm2world.scene.color.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.scene.material.Materials.*;
import static org.osm2world.scene.mesh.ExtrusionGeometry.createColumn;
import static org.osm2world.scene.mesh.LevelOfDetail.*;
import static org.osm2world.scene.mesh.MeshUtil.createBox;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.*;
import static org.osm2world.scene.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.util.ValueParseUtil.parseColor;
import static org.osm2world.util.ValueParseUtil.parseMeasure;
import static org.osm2world.world.attachment.AttachmentUtil.getCompatibleSurfaceTypes;
import static org.osm2world.world.attachment.AttachmentUtil.isAttachedToVerticalSurface;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.*;

import java.time.LocalTime;
import java.util.*;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.map_data.data.TagSet;
import org.osm2world.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.map_elevation.data.EleConnector;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.osm2world.output.CommonTarget;
import org.osm2world.output.common.ExtrudeOption;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.scene.material.MaterialOrRef;
import org.osm2world.scene.material.Materials;
import org.osm2world.scene.material.TextureLayer;
import org.osm2world.scene.mesh.ExtrusionGeometry;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.model.InstanceParameters;
import org.osm2world.scene.model.Model;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.scene.model.ProceduralModel;
import org.osm2world.scene.texcoord.MapBasedTexCoordFunction;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.data.NoOutlineNodeWorldObject;
import org.osm2world.world.data.NodeModelInstance;
import org.osm2world.world.data.NodeWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.common.AbstractModule;

/**
 * adds various types of street furniture to the world
 */
public class StreetFurnitureModule extends AbstractModule {

	@Override
	protected void applyToNode(MapNode node) {
		if (node.getTags().contains("playground", "swing")) {
			node.addRepresentation(new Swing(node));
		}
		if (node.getTags().contains("man_made", "pole")) {
			node.addRepresentation(new Pole(node));
		}
		if (node.getTags().contains("man_made", "flagpole")) {
			node.addRepresentation(new Flagpole(node));
		}
		if (node.getTags().contains("advertising", "column")) {
			node.addRepresentation(new AdvertisingColumn(node));
		}
		if (node.getTags().contains("advertising", "billboard")) {
			node.addRepresentation(new Billboard(node));
		}
		if (node.getTags().contains("amenity", "bench")) {
			node.addRepresentation(new Bench(node));
		}
		if (node.getTags().contains("amenity", "table")
				|| node.getTags().contains("leisure", "picnic_table")) {
			node.addRepresentation(new Table(node));
		}
		if (node.getTags().contains("highway", "bus_stop")
				|| node.getTags().contains("public_transport", "platform")
				&& node.getTags().contains("bus", "yes")) {
			if (!isInHighway(node)) {
				node.addRepresentation(new BusStop(node));
			}
		}
		if (node.getTags().contains("man_made", "cross")
				|| node.getTags().contains("summit:cross", "yes")
				|| node.getTags().contains("historic", "wayside_cross")) {
			node.addRepresentation(new Cross(node));
		}
		if (node.getTags().contains("amenity", "clock")
				&& node.getTags().contains("support", "wall")) {
			node.addRepresentation(new Clock(node));
		}
		if (node.getTags().contains("amenity", "waste_basket")) {
			node.addRepresentation(new WasteBasket(node));
		}
		if (node.getTags().contains("amenity", "grit_bin")) {
			node.addRepresentation(new GritBin(node));
		}
		if (node.getTags().contains("amenity", "post_box")
				&& node.getTags().containsAny(asList("operator", "brand"), null)) {
			node.addRepresentation(new PostBox(node));
		}
		if (node.getTags().contains("amenity", "telephone")
				&& node.getTags().containsAny(asList("operator", "brand"), null)) {
			node.addRepresentation(new Phone(node));
		}
		if (node.getTags().contains("amenity", "parcel_locker")
				|| (node.getTags().contains("amenity", "vending_machine") && (node.getTags().containsAny(
						asList("vending"), asList("parcel_pickup;parcel_mail_in", "parcel_mail_in"))))) {
			node.addRepresentation(new NodeModelInstance(node,
					new ParcelLocker(node.getTags()), parseDirection(node.getTags(), PI)));
		}
		if (node.getTags().contains("amenity", "vending_machine")
				&& (node.getTags().containsAny(asList("vending"), asList("bicycle_tube", "cigarettes", "condoms")))) {
			node.addRepresentation(new VendingMachine(node));
		}
		if (node.getTags().contains("amenity", "recycling")
				&& (node.getTags().contains("recycling_type", "container"))) {
			node.addRepresentation(new RecyclingContainer(node));
		}
		if (node.getTags().contains("emergency", "fire_hydrant")
				&& node.getTags().contains("fire_hydrant:type", "pillar")) {
			node.addRepresentation(new FireHydrant(node));
		}
		if (node.getTags().contains("highway", "street_lamp")) {
			node.addRepresentation(new StreetLamp(node));
		}
		if (node.getTags().contains("tourism", "information")
				&& node.getTags().contains("information", "board")) {
			node.addRepresentation(new Board(node));
		}
	}

	private static boolean isInWall(MapNode node) {
		if (node.getAdjacentAreas().size() > 0) {
			return true;
		} else {
			return false;
		}
	}

	private static boolean isInHighway(MapNode node) {
		if (node.getConnectedWaySegments().size() > 0) {
			for (MapWaySegment way : node.getConnectedWaySegments()) {
				if (way.getTags().containsKey("highway")
						&& !asList("path", "footway", "platform").contains(way.getTags().getValue("highway"))) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * a {@link NodeWorldObject} which can either be attached to a vertical surface,
	 * or stand on its own supports. In the latter case, the supports will be rendered as part of this model.
	 */
	private static abstract class NodeWorldObjectWithOptionalSupports extends NoOutlineNodeWorldObject
			implements ProceduralWorldObject {

		public NodeWorldObjectWithOptionalSupports(MapNode node) {
			super(node);
		}

		/** Returns the bottom position for the model itself, excluding the support. */
		public VectorXYZ getModelBase() {
			if (isAttachedToVerticalSurface(attachmentConnector)) {
				return getBase();
			} else {
				return getBase().addY(getPreferredVerticalAttachmentHeight());
			}
		}

		/**
		 * Returns the bottom position for the support,
		 * or null if the model is attached to a vertical surface and therefore needs no supports.
		 */
		public @Nullable VectorXYZ getSupportBase() {
			if (isAttachedToVerticalSurface(attachmentConnector)) {
				return null;
			} else {
				return getBase();
			}
		}

		/** Returns the facing direction. */
		public VectorXZ getDirection() {
			if (isAttachedToVerticalSurface(attachmentConnector)) {
				return attachmentConnector.getAttachedSurfaceNormal().xz().normalize();
			} else {
				return VectorXZ.fromAngle(parseDirection(node.getTags(), PI));
			}
		}

	}

	public static final class Pole extends NoOutlineNodeWorldObject {

		public Pole(MapNode node) {
			super(node);
		}

		@Override
		public List<Mesh> buildMeshes() {

			double height = parseMeasure(node.getTags().getValue("height"), 5.0);
			double radius = parseMeasure(node.getTags().getValue("width"), 0.2) / 2;

			Material material = getMaterial(node.getTags().getValue("material"), STEEL);
			Color color = parseColor(node.getTags().getValue("colour"));

			ExtrusionGeometry geometry = ExtrusionGeometry.createColumn(null, this.getBase(),
					height, radius, radius, false, true,
					color, material.textureDimensions());

			return singletonList(new Mesh(geometry, material, height >= 10 ? LOD2 : LOD3, LOD4));

		}

		@Override
		public Collection<AttachmentSurface> getAttachmentSurfaces() {
			return singleton(AttachmentSurface.fromMeshes("pole", buildMeshes()));
		}

	}

	public static final class Flagpole extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public Flagpole(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			/* draw the pole */

			double poleHeight = parseHeight(node.getTags(), 10.0);
			double poleRadius = 0.15;

			VectorXYZ poleBase = getBase();

			target.drawColumn(STEEL, null, poleBase,
					poleHeight, poleRadius, poleRadius, false, true);

			/* draw the flag (if any) */

			Flag flag = null;

			String wikidataId = node.getTags().getValue("flag:wikidata");
			if (wikidataId != null) {
				Material flagMaterial = buildTexturedFlagMaterial(wikidataId);
				if (flagMaterial != null) {
					flag = new TexturedFlag(flagMaterial);
				}
			}

			String countryCode = node.getTags().getValue("country");
			if (flag == null && node.getTags().contains("flag:type", "national") && countryCode != null) {
				Material countryFlagMaterial = buildTexturedFlagMaterial(countryCode);
				if (countryFlagMaterial != null) {
					flag = new TexturedFlag(countryFlagMaterial);
				} else {
					flag = NATIONAL_FLAGS.get(countryCode);
				}
			}

			if (flag == null && node.getTags().containsKey("flag:colour")) {
				Color color = parseColor(node.getTags().getValue("flag:colour"), CSS_COLORS);
				if (color != null) {
					flag = new TexturedFlag(2 / 3.0, FLAGCLOTH.get().withColor(color));
				}
			}

			if (flag != null) {

				double flagHeight = min(2.0, poleHeight / 4);

				VectorXYZ flagTop = poleBase.add(Y_UNIT.mult(poleHeight * 0.97));

				VectorXYZ flagBottom = flagTop.add(poleBase.subtract(flagTop).normalize().mult(flagHeight));
				VectorXYZ flagFrontNormal = Z_UNIT.invert();

				double flagWidth = flagHeight / flag.getHeightWidthRatio();

				VectorXYZ toOuterEnd = flagTop.subtract(flagBottom).crossNormalized(flagFrontNormal).invert();
				VectorXYZ outerBottom = flagBottom.add(toOuterEnd.mult(flagWidth));
				VectorXYZ outerTop = outerBottom.add(flagTop.subtract(flagBottom));

				flagTop = flagTop.add(toOuterEnd.mult(poleRadius));
				flagBottom = flagBottom.add(toOuterEnd.mult(poleRadius));
				outerTop = outerTop.add(toOuterEnd.mult(poleRadius));
				outerBottom = outerBottom.add(toOuterEnd.mult(poleRadius));

				flag.renderFlag(target, flagTop, flagHeight, flagWidth);

			}

		}

		/**
		 * Combines a flag image with {@link Materials#FLAGCLOTH} to create a textured flag material.
		 *
		 * @param flagId  the ID of the flag, e.g. a country code or Wikidata ID
		 */
		private @Nullable Material buildTexturedFlagMaterial(String flagId) {
			Material material = getMaterial("FLAG_" + flagId);
			Material flagcloth = FLAGCLOTH.get();
			if (material != null && material.textureLayers().size() > 0 && flagcloth.textureLayers().size() > 0) {
				List<TextureLayer> textureLayers = new ArrayList<>(flagcloth.textureLayers());
				TextureLayer flag0 = material.textureLayers().get(0);
				TextureLayer cloth0 = flagcloth.textureLayers().get(0);
				textureLayers.set(0, new TextureLayer(
						flag0.baseColorTexture,
						requireNonNullElse(flag0.normalTexture, cloth0.normalTexture),
						requireNonNullElse(flag0.ormTexture, cloth0.ormTexture),
						requireNonNullElse(flag0.displacementTexture, cloth0.displacementTexture),
						flag0.colorable));
				return new Material(Interpolation.SMOOTH, WHITE, true,
						flagcloth.transparency(), flagcloth.shadow(), flagcloth.ambientOcclusion(),
						textureLayers);
			} else {
				return material;
			}
		}

		static class FlagMesh {
			VectorXYZ[][] vertices;
			VectorXYZ[][] normals;
			Map<VectorXYZ, VectorXZ> texCoordMap;
			public FlagMesh(VectorXYZ[][] vertices, VectorXYZ[][] normals, Map<VectorXYZ, VectorXZ> texCoordMap) {
				this.vertices = vertices;
				this.normals = normals;
				this.texCoordMap = texCoordMap;
			}
		}

		static abstract class Flag {

			private final double heightWidthRatio;
			private final List<Material> stripeMaterials;
			private final boolean verticalStripes;

			/**
			 *
			 * @param heightWidthRatio  height / width
			 * @param verticalStripes   whether the material stripes provided by
			 *                          stripeMaterials are vertical.
			 * @param stripeMaterials   returns one or more materials for the flag.
			 *                          If there's more than one, the flag will be striped.
			 */
			protected Flag(double heightWidthRatio, List<Material> stripeMaterials, boolean verticalStripes) {
				this.heightWidthRatio = heightWidthRatio;
				this.stripeMaterials = stripeMaterials;
				this.verticalStripes = verticalStripes;
			}

			public double getHeightWidthRatio() {
				return heightWidthRatio;
			}

			/**
			 * renders the flag to any {@link Target}.
			 *
			 * @param origin  top point of the flag on the side connected to the pole)
			 * @param height  height of the flag in meters
			 * @param width  width of the flag in meters
			 */
			public void renderFlag(Target target, VectorXYZ origin, double height, double width) {

				FlagMesh flagMesh = createFlagMesh(origin, height, width);
				VectorXYZ[][] mesh = flagMesh.vertices;
				final Map<VectorXYZ, VectorXZ> texCoordMap = flagMesh.texCoordMap;

				/* define a function that looks the texture coordinate up in the map  */

				var texCoordFunction = new MapBasedTexCoordFunction(texCoordMap);

				/* flip the mesh array in case of vertically striped flags */

				if (verticalStripes) {

					VectorXYZ[][] flippedMesh = new VectorXYZ[mesh[0].length][mesh.length];

					for (int i = 0; i < mesh.length; i++) {
			            for (int j = 0; j < mesh[0].length; j++) {
			                flippedMesh[j][i] = mesh[i][j];
			            }
					}

			        mesh = flippedMesh;

				}

				/* draw the mesh */

				// TODO use the calculated normals

				for (int row = 0; row < mesh[0].length - 1; row ++) {

					List<VectorXYZ> vsFront = new ArrayList<>(mesh.length * 2);
					List<VectorXYZ> vsBack = new ArrayList<>(mesh.length * 2);

					for (int col = 0; col < mesh.length; col++) {

						VectorXYZ vA = mesh[col][row];
						VectorXYZ vB = mesh[col][row + 1];

						vsFront.add(vA);
						vsFront.add(vB);

						vsBack.add(vB);
						vsBack.add(vA);

					}

					// determine which stripe we're in and pick the right material
					int materialIndex = (int) floor(stripeMaterials.size() * (row + 0.5) / (mesh[0].length - 1));
					Material material = stripeMaterials.get(materialIndex);

					target.drawTriangleStrip(material, vsFront, texCoordLists(
							vsFront, material, t -> texCoordFunction));

					if (!material.doubleSided()) {
						target.drawTriangleStrip(material, vsBack, texCoordLists(
								vsBack, material, t -> texCoordFunction));
					}

				}

			}

			/**
			 * creates a grid of vertices as the geometry of the flag.
			 * The grid is deformed to give the appearance of cloth.
			 *
			 * @return the grid of vertices, normals and the matching texture coordinates
			 */
			private final FlagMesh createFlagMesh(VectorXYZ origin, double height, double width) {

				/* set the minimum columns and rows to achieve a reasonable "wavy cloth" appearance */

				int minCols = 6;
				int minRows = max(2, (int)round(minCols * height/width));

				/* set the actual number of columns and rows so that they're a multiple of the number of stripes */

				int stripeCols = verticalStripes ? stripeMaterials.size() : 1;
				int stripeRows = verticalStripes ? 1 : stripeMaterials.size();

				int numCols = stripeCols;
				while (numCols < minCols) {
					numCols += stripeCols;
				}

				int numRows = stripeRows;
				while (numRows < minRows) {
					numRows += stripeRows;
				}

				// number of vertices per row/column is 1 higher than number of rectangles
				numCols += 1;
				numRows += 1;

				/* create the vertices and texture coordinates */

				VectorXYZ[][] vertices = new VectorXYZ[numCols][numRows];
				Map<VectorXYZ, VectorXZ> texCoordMap = new HashMap<>(numCols * numRows);

				for (int x = 0; x < numCols; x++) {
					for (int y = 0; y < numRows; y++) {

						double xRatio = x / (double)(numCols - 1);
						double yRatio = y / (double)(numRows - 1);

						vertices[x][y] = new VectorXYZ(
								xRatio * width,
								yRatio * -height,
								0).add(origin);

						// use a sinus function for basic wavyness
						vertices[x][y] = vertices[x][y].add(0, 0, sin(6 * xRatio) * 0.1);

						// have the flag drop down the further it gets from the pole
						vertices[x][y] = vertices[x][y].add(0, height * -0.2 * xRatio * sqrt(xRatio), 0);

						// have the top of the flag drop backwards and down a bit
						double factor = sqrt(xRatio) * max(0.7 - yRatio, 0);
						vertices[x][y] = vertices[x][y].add(0,
								factor * factor * -0.25 * height,
								factor * factor * 0.35 * height);

						texCoordMap.put(vertices[x][y], new VectorXZ(xRatio, 1 - yRatio));

					}
				}

				/* create the normals based on the vertices */

				VectorXYZ[][] normals = new VectorXYZ[numCols][numRows];

				for (int x = 0; x < numCols; x++) {
					for (int y = 0; y < numRows; y++) {

						int colNext = min(x + 1, numCols - 1);
						int colPrev = max(x - 1, 0);
						int rowNext = min(y + 1, numRows - 1);
						int rowPrev = max(y - 1, 0);

						VectorXYZ colVec = vertices[x][rowNext].subtract(vertices[x][rowPrev]);
						VectorXYZ rowVec = vertices[colNext][y].subtract(vertices[colPrev][y]);

						normals[x][y] = colVec.cross(rowVec);

					}
				}

				return new FlagMesh(vertices, normals, texCoordMap);

			}

		}

		static class StripedFlag extends Flag {

			public StripedFlag(double heightWidthRatio, List<Color> colors, boolean verticalStripes) {

				super(heightWidthRatio, createStripeMaterials(colors), verticalStripes);

			}

			/**
			 * creates a material for each colored stripe
			 */
			private static List<Material> createStripeMaterials(List<Color> colors) {
				return colors.stream().map(FLAGCLOTH.get()::withColor).toList();
			}

		}

		/**
		 * flag entirely made of a single, usually textured, {@link Material}.
		 * Allows for untextured materials in addition to textured ones.
		 */
		static class TexturedFlag extends Flag {

			public TexturedFlag(double heightWidthRatio, Material material) {
				super(heightWidthRatio, singletonList(material), false);
			}

			/**
			 * Alternative constructor that uses the aspect ratio of the first texture layer's color texture
			 * instead of an explicit heightWidthRatio parameter. Only works for textured materials,
			 * otherwise an unreliable default is used.
			 */
			public TexturedFlag(Material material) {
				this(material.textureLayers().isEmpty() ? 3 / 5.0
						: 1.0 / material.textureLayers().get(0).baseColorTexture.getAspectRatio(),
						material);
			}

		}

		private static final Map<String, Flag> NATIONAL_FLAGS = new HashMap<String, Flag>();

		static {

			NATIONAL_FLAGS.put("AT", new StripedFlag(2 / 3.0, asList(new Color(240, 79, 93), WHITE, new Color(240, 79, 93)), false));
			NATIONAL_FLAGS.put("AM", new StripedFlag(1 / 2.0, asList(new Color(218, 0, 10), new Color(0, 48, 160), new Color(242, 170, 0)), false));
			NATIONAL_FLAGS.put("BE", new StripedFlag(13 / 15.0, asList(BLACK, new Color(245, 221, 63), new Color(231, 35, 53)), true));
			NATIONAL_FLAGS.put("BG", new StripedFlag(3 / 5.0, asList(WHITE, new Color(0, 151, 110), new Color(215, 33, 10)), false));
			NATIONAL_FLAGS.put("BO", new StripedFlag(2 / 3.0, asList(new Color(207, 38, 23), new Color(249, 228, 0), new Color(0, 122, 49)), false));
			NATIONAL_FLAGS.put("CI", new StripedFlag(2 / 3.0, asList(decode("#F77F00"), WHITE, decode("#009E60")), true));
			NATIONAL_FLAGS.put("CO", new StripedFlag(2 / 3.0, asList(decode("#FCD20E"), decode("#FCD20E"), decode("#003594"), decode("#CF0821")), false));
			NATIONAL_FLAGS.put("DE", new StripedFlag(3 / 5.0, asList(BLACK, new Color(222, 0, 0), new Color(255, 207, 0)), false));
			NATIONAL_FLAGS.put("EE", new StripedFlag(7 / 11.0, asList(decode("#0073CF"), BLACK, WHITE), false));
			NATIONAL_FLAGS.put("FR", new StripedFlag(2 / 3.0, asList(decode("#001E96"), WHITE, decode("#EE2436")), true));
			NATIONAL_FLAGS.put("GA", new StripedFlag(3 / 4.0, asList(decode("#009F60"), decode("#FCD20E"), decode("#3776C5")), false));
			NATIONAL_FLAGS.put("GN", new StripedFlag(2 / 3.0, asList(decode("#CF0821"), decode("#FCD20E"), decode("#009560")), true));
			NATIONAL_FLAGS.put("ID", new StripedFlag(2 / 3.0, asList(RED, WHITE), false));
			NATIONAL_FLAGS.put("IE", new StripedFlag(1 / 2.0, asList(decode("#0E9C62"), WHITE, decode("#FF893C")), true));
			NATIONAL_FLAGS.put("IT", new StripedFlag(2 / 3.0, asList(decode("#009344"), WHITE, decode("#CF2734")), true));
			NATIONAL_FLAGS.put("LT", new StripedFlag(3 / 5.0, asList(decode("#FDBA0B"), decode("#006A42"), decode("#C22229")), false));
			NATIONAL_FLAGS.put("LU", new StripedFlag(3 / 5.0, asList(decode("#EE2436"), WHITE, decode("#00A3DF")), false));
			NATIONAL_FLAGS.put("MC", new StripedFlag(4 / 5.0, asList(decode("#CF0821"), WHITE), false));
			NATIONAL_FLAGS.put("ML", new StripedFlag(2 / 3.0, asList(decode("#0CB637"), decode("#FCD20E"), decode("#CF0821")), true));
			NATIONAL_FLAGS.put("MU", new StripedFlag(2 / 3.0, asList(decode("#EA2205"), decode("#282F58"), decode("#F6B711"), decode("#008757")), false));
			NATIONAL_FLAGS.put("NG", new StripedFlag(1 / 2.0, asList(decode("#008850"), WHITE, decode("#008850")), true));
			NATIONAL_FLAGS.put("NL", new StripedFlag(2 / 3.0, asList(decode("#AD1622"), decode("#F9F9F9"), decode("#183B7A")), false));
			NATIONAL_FLAGS.put("PE", new StripedFlag(2 / 3.0, asList(decode("#DA081E"), WHITE, decode("#DA081E")), true));
			NATIONAL_FLAGS.put("PL", new StripedFlag(5 / 8.0, asList(WHITE, decode("#DD0C39")), false));
			NATIONAL_FLAGS.put("RO", new StripedFlag(2 / 3.0, asList(decode("#002780"), decode("#FAD00E"), decode("#C1071F")), true));
			NATIONAL_FLAGS.put("RU", new StripedFlag(2 / 3.0, asList(WHITE, decode("#0036A8"), decode("#D62718")), false));
			NATIONAL_FLAGS.put("SL", new StripedFlag(2 / 3.0, asList(decode("#17B637"), WHITE, decode("#0073C7")), false));
			NATIONAL_FLAGS.put("TD", new StripedFlag(2 / 3.0, asList(decode("#002664"), decode("#FECB00"), decode("#C60C30")), true));
			NATIONAL_FLAGS.put("UA", new StripedFlag(2 / 3.0, asList(decode("#005BBC"), decode("#FED500")), false));
			NATIONAL_FLAGS.put("HU", new StripedFlag(1 / 2.0, asList(decode("#CE253C"), WHITE, decode("#41704C")), false));
			NATIONAL_FLAGS.put("YE", new StripedFlag(2 / 3.0, asList(decode("#CF0821"), WHITE, BLACK), false));

		}

	}

	public static final class AdvertisingColumn extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public AdvertisingColumn(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			double height = parseHeight(node.getTags(), 3.0);

			/* draw socket, poster and cap */

			target.drawColumn(CONCRETE, null,
					getBase(),
					0.15 * height,
					0.5, 0.5, false, false);

			target.drawColumn(ADVERTISING_POSTER, null,
					getBase(),
					0.98 * height,
					0.48, 0.48, false, false);

			target.drawColumn(CONCRETE, null,
					getBase().add(0, 0.95 * height, 0),
					0.05 * height,
					0.5, 0.5, false, true);

		}

	}

	public static final class Billboard extends NodeWorldObjectWithOptionalSupports {

		/** @param trueHeight  the height of the billboard itself, i.e. height minus minHeight */
		private record BillboardDimensions(
			double width,
			double trueHeight,
			double minHeight
		) {
			public static BillboardDimensions fromTags(TagSet tags) {
				double width = parseWidth(tags, 4);
				double height = parseHeight(tags, 3.5f);
				double minHeight = height / 5;
				double trueHeight = height - minHeight;
				return new BillboardDimensions(width, trueHeight, minHeight);
			}
		}

		private final BillboardDimensions dimensions;

		public Billboard(MapNode node) {
			super(node);
			this.dimensions = BillboardDimensions.fromTags(node.getTags());
		}

		@Override
		protected double getPreferredVerticalAttachmentHeight() {
			var dimensions = BillboardDimensions.fromTags(node.getTags());
			return dimensions.minHeight;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			@Nullable VectorXYZ supportBase = getSupportBase();
			VectorXYZ boardBase = getModelBase();
			VectorXZ direction = getDirection();

			if (supportBase != null) {
				drawSupport(target, supportBase, direction);
			} else {
				// prevent the board from overlapping with the wall
				boardBase = boardBase.add(direction.mult(0.01));
			}

			drawBoard(target, boardBase, direction);

		}

		private void drawBoard(Target target, VectorXYZ boardBase, VectorXZ direction) {

			/* draw board */

			VectorXZ boardVector = direction.rightNormal();

			VectorXYZ[] vsPoster = {
					boardBase.add(boardVector.mult(dimensions.width / 2)).addY(dimensions.trueHeight),
					boardBase.add(boardVector.mult(dimensions.width / 2)),
					boardBase.add(boardVector.mult(-dimensions.width / 2)).addY(dimensions.trueHeight),
					boardBase.add(boardVector.mult(-dimensions.width / 2))
			};

			List<VectorXYZ> vsListPoster = asList(vsPoster);

			target.drawTriangleStrip(ADVERTISING_POSTER, vsListPoster,
					texCoordLists(vsListPoster, ADVERTISING_POSTER, STRIP_FIT));

			List<VectorXYZ> vsBoard = List.of(
					vsPoster[2],
					vsPoster[3],
					vsPoster[0],
					vsPoster[1]
			);

			if (node.getTags().contains("two_sided", "yes")) {

				Material backMaterial = ADVERTISING_POSTER.get();
				target.drawTriangleStrip(backMaterial, vsBoard,
						texCoordLists(vsBoard, backMaterial, STRIP_FIT));

			} else {

				Material backMaterial = CONCRETE.get();
				target.drawTriangleStrip(backMaterial, vsBoard,
						texCoordLists(vsBoard, backMaterial, STRIP_WALL));

			}

			/* draw frame */

			target.drawBox(CONCRETE, boardBase.addY(dimensions.trueHeight - 0.1),
					direction, 0.1, dimensions.width, 0.1);

			target.drawBox(CONCRETE, boardBase,
					direction, 0.1, dimensions.width, 0.1);

			target.drawBox(CONCRETE, boardBase.add(boardVector.mult(dimensions.width / 2)),
					direction, dimensions.trueHeight, 0.1, 0.1);

			target.drawBox(CONCRETE, boardBase.add(boardVector.mult(-dimensions.width / 2)),
					direction, dimensions.trueHeight, 0.1, 0.1);

		}


		private void drawSupport(Target target, VectorXYZ supportBase, VectorXZ direction) {

			/* draw two poles */

			VectorXZ boardVector = direction.rightNormal();

			VectorXZ[] poles = {
					node.getPos().add(boardVector.mult(-dimensions.width / 4)),
					node.getPos().add(boardVector.mult(+dimensions.width / 4))
			};

			for (VectorXZ pole : poles) {
				target.drawBox(CONCRETE, pole.xyz(supportBase.y),
						direction, dimensions.minHeight, 0.2, 0.1);
			}

		}

	}

	public static final class Swing extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public Swing(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			// determine width and height of the swing structure
			final double swingHeight = parseHeight(node.getTags(), 1.5);
			final double boxHeight = 0.05;

			final double defaultWidth = 0.5f * parseInt(node.getTags(), 4, "capacity");
			final double width = parseWidth(node.getTags(), defaultWidth);

			final int capacity = parseInt(node.getTags(), 4, "capacity");

			final double CENTRAL_BOX_WIDTH = width*0.8; //C_B_W

			final double placeForRopes = CENTRAL_BOX_WIDTH*0.8/2; /* use 80% of C_B_W's width and "normalize" it
			 														   to keep ropes (and seats) within C_B_W's limits */
			final double seatWidth = 0.18;

			final double ropesOffset = 0.07;

			// determine material and color

			Material material = null;

			if (node.getTags().containsKey("material")) {
				material = getMaterial(node.getTags().getValue("material").toUpperCase());
			}

			if (material == null) {
				material = WOOD.get();
			}

			material = material.withColor(parseColor(node.getTags().getValue("colour"), CSS_COLORS));

			// calculate vectors and corners

			double directionAngle = parseDirection(node.getTags(), PI);

			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			VectorXZ boardVector = faceVector.rightNormal();

			List<VectorXZ> cornerOffsets = asList(
					faceVector.mult(+0.25).add(boardVector.mult(+width / 2)),
					faceVector.mult(+0.25).add(boardVector.mult(-width / 2)),
					faceVector.mult(-0.25).add(boardVector.mult(+width / 2)),
					faceVector.mult(-0.25).add(boardVector.mult(-width / 2)));

			// boxes on top of poles
			target.drawBox(material, getBase().add(boardVector.mult(CENTRAL_BOX_WIDTH/2)).addY(swingHeight-boxHeight),
					boardVector, boxHeight, 0.48, boxHeight);
			target.drawBox(material, getBase().add(boardVector.mult(-CENTRAL_BOX_WIDTH/2)).addY(swingHeight-boxHeight),
					boardVector, boxHeight, 0.48, boxHeight);
			target.drawBox(material, getBase().addY(swingHeight-boxHeight),
					faceVector, boxHeight, CENTRAL_BOX_WIDTH, boxHeight);


			// draw poles
			for (VectorXZ cornerOffset : cornerOffsets) {

				VectorXZ polePos = node.getPos().add(cornerOffset.mult(0.8));

				target.drawBox(material, polePos.xyz(getBase().y),
						faceVector, swingHeight, 0.08, 0.08);
			}

			ShapeXZ shape = new LineSegmentXZ(new VectorXZ(0.01, 0), new VectorXZ(0, 0));

			List<List<VectorXYZ>> paths = new ArrayList<List<VectorXYZ>>();

			VectorXZ leftMost = getBase().add(boardVector.mult(placeForRopes)).xz();
			VectorXZ rightMost = getBase().add(boardVector.mult(-placeForRopes)).xz();

			double distance = leftMost.distanceTo(rightMost);

			//if capacity=1 "distribute" only 1 point
			List<VectorXZ> seatPositions = equallyDistributePointsAlong(
					distance/(capacity>1 ? capacity-1 : capacity),
					(capacity>1 ? true : false), leftMost, rightMost);

			for(VectorXZ vec : seatPositions) {

				//place ropes slightly off the center of the seat
				paths.add(asList(
						vec.xyz(swingHeight/3).add(boardVector.mult(-ropesOffset)),
						vec.xyz(swingHeight).add(boardVector.mult(-ropesOffset)) ));
				paths.add(asList(
						vec.xyz(swingHeight/3).add(boardVector.mult(ropesOffset)),
						vec.xyz(swingHeight).add(boardVector.mult(ropesOffset)) ));

				//draw seat
				target.drawBox(material, vec.xyz(swingHeight/3),
						faceVector, 0.02, seatWidth, 0.1);
			}

			//Draw 2 triangleStrips for each rope, to be visible from both front and back side
			for(List<VectorXYZ> path : paths) {
				target.drawExtrudedShape(STEEL, shape, path, nCopies(2,Z_UNIT.invert()), null, null);
				target.drawExtrudedShape(STEEL, shape, path, nCopies(2,Z_UNIT), null, null);
			}
		}
	}

	public static final class Bench extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public Bench(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			VectorXYZ position = getBase();

			/* determine the width of the bench */

			double defaultWidth = 0.5f * parseInt(node.getTags(), 4, "seats");

			double width = parseWidth(node.getTags(), defaultWidth);

			/* determine material and color */

			Material material = null;

			if (node.getTags().containsKey("material")) {
				material = getMaterial(node.getTags().getValue("material").toUpperCase());
			}

			if (material == null) {
				material = WOOD.get();
			}

			material = material.withColor(parseColor(node.getTags().getValue("colour"), CSS_COLORS));

			/* calculate vectors and corners */

			double directionAngle = parseDirection(node.getTags(), PI);

			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			VectorXZ boardVector = faceVector.rightNormal();

			List<VectorXZ> cornerOffsets = asList(
					faceVector.mult(+0.25).add(boardVector.mult(+width / 2)),
					faceVector.mult(+0.25).add(boardVector.mult(-width / 2)),
					faceVector.mult(-0.25).add(boardVector.mult(+width / 2)),
					faceVector.mult(-0.25).add(boardVector.mult(-width / 2)));

			/* draw seat and backrest */

			target.drawBox(material, position.addY(0.5),
					faceVector, 0.05, width, 0.5);

			if (!node.getTags().contains("backrest", "no")) {

				target.drawBox(material,
						position.add(faceVector.mult(-0.23)).addY(0.5),
						faceVector, 0.5, width, 0.04);

			}

			/* draw poles */

			for (VectorXZ cornerOffset : cornerOffsets) {
				VectorXZ polePos = node.getPos().add(cornerOffset.mult(0.8));
				target.drawBox(material, polePos.xyz(position.y),
						faceVector, 0.5, 0.08, 0.08);
			}

		}

	}


	public static final class Table extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		private final Material defaultMaterial;

		public Table(MapNode node) {
			super(node);

			if (node.getTags().contains("leisure", "picnic_table")) {
				defaultMaterial = WOOD.get();
			} else {
				defaultMaterial = STEEL.get();
			}
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			int seats = parseInt(node.getTags(), 4, "seats");

			// All default values are bound to the hight value. This allows to chose any table size.
			double height = parseHeight(node.getTags(), 0.75f);
			double width = parseWidth(node.getTags(), height * 1.2f);
			double length = parseLength(node.getTags(), ((seats + 1) / 2) * height / 1.25f);

			double seatHeight = height / 1.5;

			/* determine material */

			Material material = null;

			//TODO parse color

			if (material == null) {
				material = getSurfaceMaterial(
						node.getTags().getValue("material"));
			}

			if (material == null) {
				material = getSurfaceMaterial(
						node.getTags().getValue("surface"), defaultMaterial);
			}

			/* calculate vectors and corners */

			double directionAngle = parseDirection(node.getTags(), PI);

			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			VectorXZ boardVector = faceVector.rightNormal();

			double poleCenterOffset = height / 15.0;

			List<VectorXZ> cornerOffsets = new ArrayList<>(4);
			cornerOffsets.add(faceVector.mult(+length / 2 - poleCenterOffset).add(boardVector.mult(+width / 2 - poleCenterOffset)));
			cornerOffsets.add(faceVector.mult(+length / 2 - poleCenterOffset).add(boardVector.mult(-width / 2 + poleCenterOffset)));
			cornerOffsets.add(faceVector.mult(-length / 2 + poleCenterOffset).add(boardVector.mult(+width / 2 - poleCenterOffset)));
			cornerOffsets.add(faceVector.mult(-length / 2 + poleCenterOffset).add(boardVector.mult(-width / 2 + poleCenterOffset)));

			/* draw poles */

			double poleThickness = poleCenterOffset * 1.6;

			for (VectorXZ cornerOffset : cornerOffsets) {
				VectorXZ polePos = node.getPos().add(cornerOffset);
				target.drawBox(material, polePos.xyz(getBase().y + 0.001), faceVector, height, poleThickness, poleThickness);
			}

			/* draw table */

			target.drawBox(material, getBase().addY(height * 14f / 15f),
					faceVector, height / 15f, width, length);

			/* draw seats */

			int leftSeats = seats / 2;
			int rightSeats = (seats + 1) / 2;

			renderSeatSide(target, material, boardVector.mult(+width / 2 + seatHeight / 2.5f), length, leftSeats, seatHeight);
			renderSeatSide(target, material, boardVector.mult(-width / 2 - seatHeight / 2.5f), length, rightSeats, seatHeight);
		}

		private void renderSeatSide(Target target, Material material, VectorXZ rowPos, double length, int seats, double seatHeight) {
			VectorXZ boardVector = rowPos.rightNormal();
			VectorXZ faceVector = rowPos.normalize();

			double seatWidth = seatHeight / 1.25f;
			double seatLength = seatHeight / 1.25f;
			VectorXYZ seatBase = getBase().add(rowPos).addY(seatHeight * 0.94f);
			VectorXYZ backrestBase = getBase().add(rowPos).add(faceVector.mult(seatLength * 0.45f)).addY(seatHeight);

			for (int i = 0; i < seats; i++) {
				double seatBoardPos = length / seats * ((seats - 1) / 2.0f - i);

				VectorXZ seatPos = rowPos.add(boardVector.mult(seatBoardPos));

				List<VectorXZ> cornerOffsets = new ArrayList<VectorXZ>(4);
				cornerOffsets.add(seatPos.add(boardVector.mult(+seatWidth * 0.45f).add(faceVector.mult(+seatLength * 0.45f))));
				cornerOffsets.add(seatPos.add(boardVector.mult(+seatWidth * 0.45f).add(faceVector.mult(-seatLength * 0.45f))));
				cornerOffsets.add(seatPos.add(boardVector.mult(-seatWidth * 0.45f).add(faceVector.mult(-seatLength * 0.45f))));
				cornerOffsets.add(seatPos.add(boardVector.mult(-seatWidth * 0.45f).add(faceVector.mult(+seatLength * 0.45f))));

				/* draw poles */

				for (VectorXZ cornerOffset : cornerOffsets) {
					VectorXZ polePos = node.getPos().add(cornerOffset);
					target.drawBox(material, polePos.xyz(getBase().y + 0.001), faceVector, seatHeight, seatWidth / 10f, seatLength / 10f);
				}

				/* draw seat */


				target.drawBox(material,
						seatBase.add(boardVector.mult(seatBoardPos)),
						faceVector, seatHeight * 0.06f, seatWidth, seatLength);

				/* draw backrest */

				target.drawBox(material,
						backrestBase.add(boardVector.mult(seatBoardPos)),
						faceVector, seatHeight, seatWidth, seatLength / 10f);
			}
		}

	}

	/**
	 * a summit cross or wayside cross
	 */
	public static final class Cross extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public Cross(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			boolean summit = node.getTags().containsKey("summit:cross")
					|| node.getTags().contains("natural", "peak");

			double height = parseHeight(node.getTags(), summit ? 4f : 2f);
			double width = parseHeight(node.getTags(), height * 2 / 3);

			double thickness = min(height, width) / 8;

			/* determine material and direction */

			Material material = null;

			//TODO parse color

			if (material == null) {
				material = getSurfaceMaterial(
						node.getTags().getValue("material"));
			}

			if (material == null) {
				material = getSurfaceMaterial(
						node.getTags().getValue("surface"), WOOD);
			}

			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

			/* draw cross */

			target.drawBox(material, getBase(),
					faceVector, height, thickness, thickness);

			target.drawBox(material, getBase().addY(height - width / 2 - thickness / 2),
					faceVector, thickness, width, thickness);

		}

	}

	/**
	 * a clock. Currently only clocks attached to walls are supported.
	 */
	public static final class Clock implements NodeWorldObject, ProceduralWorldObject {

		private static final LocalTime TIME = LocalTime.parse("12:25");

		private final MapNode node;
		private final AttachmentConnector connector;

		public Clock(MapNode node) {

			this.node = node;

			double preferredHeight = parseHeight(node.getTags(), 10f);
			connector = new AttachmentConnector(asList("wall"), node.getPos().xyz(0), this, preferredHeight, true);

		}

		@Override
		public MapNode getPrimaryMapElement() {
			return node;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ATTACHED;
		}

		@Override
		public Iterable<EleConnector> getEleConnectors() {
			return emptyList();
		}

		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {}

		@Override
		public Iterable<AttachmentConnector> getAttachmentConnectors() {
			return singleton(connector);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			if (!connector.isAttached()) return;

			target.setCurrentLodRange(LOD2, LOD4);

			double diameter = parseWidth(node.getTags(), 1f);
			var instance = new ModelInstance(new ClockFace(TIME), new InstanceParameters(
					connector.getAttachedPos(),
					connector.getAttachedSurfaceNormal().xz().angle(),
					diameter));
			instance.render(target);

		}

		private static class ClockFace implements ProceduralModel {

			private final LocalTime time;

			public ClockFace(LocalTime currentTime) {
				this.time = currentTime;
			}

			@Override
			public void render(CommonTarget target, InstanceParameters params) {

				double diameter = params.height() != null ? params.height() : 1.0;
				double thickness = 0.08;

				VectorXZ faceNormal = VectorXZ.fromAngle(params.direction());

				VectorXYZ backCenter = params.position().add(faceNormal.mult(thickness * 0.7));
				VectorXYZ frontCenter = params.position().add(faceNormal.mult(thickness));

				CircleXZ outerCircle = new CircleXZ(NULL_VECTOR, diameter / 2);
				CircleXZ innerCircle = new CircleXZ(NULL_VECTOR, diameter / 2.2);

				PolygonWithHolesXZ ring = new PolygonWithHolesXZ(asSimplePolygon(outerCircle),
						asList(asSimplePolygon(innerCircle).reverse()));

				target.drawExtrudedShape(PLASTIC.get().withColor(BLACK), ring,
						asList(params.position(), frontCenter),
						nCopies(2, Y_UNIT), null, EnumSet.of(ExtrudeOption.END_CAP));

				target.drawShape(PLASTIC, innerCircle, backCenter, faceNormal.xyz(0), Y_UNIT, 1);

				drawHand(target, frontCenter, faceNormal, diameter / 20, diameter / 2.5, thickness / 5, angleMinuteHand(time));
				drawHand(target, frontCenter, faceNormal, diameter / 15, diameter / 4, thickness / 5, angleHourHand(time));

			}

			private final void drawHand(CommonTarget target, VectorXYZ origin, VectorXZ faceNormal,
					double width, double length, double thickness, double angleRad) {

				assert width < length;

				ShapeXZ handShape = new AxisAlignedRectangleXZ(-width/2, -width/2, width/2, length - width/2);
				handShape = handShape.rotatedCW(angleRad);

				target.drawExtrudedShape(PLASTIC.get().withColor(BLACK), handShape,
						asList(origin, origin.add(faceNormal.mult(thickness))),
						nCopies(2, Y_UNIT), null, EnumSet.of(ExtrudeOption.END_CAP));

			}

			/** returns the clockwise angle for the minute hand in radians */
			private static double angleMinuteHand(LocalTime time) {
				return time.getMinute() / 60.0 * 2 * PI;
			}

			/** returns the clockwise angle for the hour hand in radians */
			private static double angleHourHand(LocalTime time) {
				return ((time.getHour() % 12) * 60 + time.getMinute()) / (12 * 60.0) * 2 * PI;
			}

		}

	}

	public static final class RecyclingContainer extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		double directionAngle = parseDirection(node.getTags(), PI);
		VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

		public RecyclingContainer(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			float distanceX = 3f;
			float distanceZ = 1.6f;

			int n = -1;
			int m = 0;

			if (node.getTags().containsAny(asList("recycling:glass_bottles", "recycling:glass"), asList("yes"))) {
				n++;
			}
			if (node.getTags().contains("recycling:paper", "yes")) {
				n++;
			}
			if (node.getTags().contains("recycling:clothes", "yes")) {
				n++;
			}

			if (node.getTags().contains("recycling:paper", "yes")) {
				drawContainer(target, "paper", getBase().add(new VectorXYZ((distanceX * (-n / 2 + m)), 0f, (distanceZ / 2)).rotateY(faceVector.angle())));
				drawContainer(target, "paper", getBase().add(new VectorXYZ((distanceX * (-n / 2 + m)), 0f, -(distanceZ / 2)).rotateY(faceVector.angle())));
				m++;
			}
			if (node.getTags().containsAny(asList("recycling:glass_bottles", "recycling:glass"), asList("yes"))) {
				drawContainer(target, "white_glass", getBase().add(new VectorXYZ((distanceX * (-n / 2 + m)), 0f, (distanceZ / 2)).rotateY(faceVector.angle())));
				drawContainer(target, "coloured_glass", getBase().add(new VectorXYZ((distanceX * (-n / 2 + m)), 0f, -(distanceZ / 2)).rotateY(faceVector.angle())));
				m++;
			}
			if (node.getTags().contains("recycling:clothes", "yes")) {
				drawContainer(target, "clothes", getBase().add(new VectorXYZ((distanceX * (-n / 2 + m)), 0f, 0).rotateY(faceVector.angle())));
			}


		}

		private void drawContainer(Target target, String trash, VectorXYZ pos) {

			if ("clothes".equals(trash)) {
				target.drawBox(new Material(Interpolation.FLAT, new Color(0.82f, 0.784f, 0.75f)),
						pos,
						faceVector, 2, 1, 1);
			} else { // "paper" || "white_glass" || "coloured_glass"
				float width = 1.5f;
				float height = 1.6f;

				Material colourFront = null;
				Material colourBack = null;

				if ("paper".equals(trash)) {
					colourFront = new Material(Interpolation.FLAT, Color.BLUE);
					colourBack = new Material(Interpolation.FLAT, Color.BLUE);
				} else if ("white_glass".equals(trash)) {
					colourFront = new Material(Interpolation.FLAT, Color.WHITE);
					colourBack = new Material(Interpolation.FLAT, Color.WHITE);
				} else { // "coloured_glass"
					colourFront = new Material(Interpolation.FLAT, new Color(0.18f, 0.32f, 0.14f));
					colourBack = new Material(Interpolation.FLAT, new Color(0.39f, 0.15f, 0.11f));
				}

				target.drawBox(STEEL,
						pos,
						faceVector, height, width, width);
				target.drawBox(colourFront,
						pos.add(new VectorXYZ((width / 2 - 0.10), 0.1f, (width / 2 - 0.1)).rotateY(directionAngle)),
						faceVector, height - 0.2, 0.202, 0.202);
				target.drawBox(colourBack,
						pos.add(new VectorXYZ(-(width / 2 - 0.10), 0.1f, (width / 2 - 0.1)).rotateY(directionAngle)),
						faceVector, height - 0.2, 0.202, 0.202);
				target.drawBox(colourFront,
						pos.add(new VectorXYZ((width / 2 - 0.10), 0.1f, -(width / 2 - 0.1)).rotateY(directionAngle)),
						faceVector, height - 0.2, 0.202, 0.202);
				target.drawBox(colourBack,
						pos.add(new VectorXYZ(-(width / 2 - 0.10), 0.1f, -(width / 2 - 0.1)).rotateY(directionAngle)),
						faceVector, height - 0.2, 0.202, 0.202);
			}
		}


	}

	public static final class WasteBasket extends NodeWorldObjectWithOptionalSupports {

		public WasteBasket(MapNode node) {
			super(node);
		}

		@Override
		protected double getPreferredVerticalAttachmentHeight() {
			return 0.6;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			/* determine material */

			Material material = null;

			if (node.getTags().containsKey("material")) {
				material = getMaterial(node.getTags().getValue("material").toUpperCase());
			}

			if (material == null) {
				material = STEEL.get();
			}

			material = material.withColor(parseColor(node.getTags().getValue("colour"), CSS_COLORS));

			/* determine position and direction */

			@Nullable VectorXYZ supportBase = getSupportBase();
			VectorXYZ modelBase = getModelBase();
			VectorXZ direction = getDirection();

			/* render geometry */

			if (supportBase != null) {
				drawSupport(target, material, supportBase);
				modelBase = modelBase.add(direction.mult(0.05));
			}

			drawBasket(target, material, modelBase, direction);

		}

		private static void drawSupport(Target target, Material material, VectorXYZ supportBase) {
			target.drawColumn(material, null, supportBase, 1.2, 0.06, 0.06, false, true);
		}

		private void drawBasket(Target target, Material material, VectorXYZ modelBase, VectorXZ direction) {

			double height = 0.5;
			double radius = 0.2;

			VectorXYZ bottomCenter = modelBase.addY(-0.1).add(direction.mult(0.2));
			VectorXYZ innerBottomCenter = bottomCenter.addY(height * 0.05);
			VectorXYZ topCenter = bottomCenter.addY(height);

			SimplePolygonXZ outerRing = asSimplePolygon(new CircleXZ(NULL_VECTOR, radius));
			SimplePolygonXZ innerRing = asSimplePolygon(new CircleXZ(NULL_VECTOR, radius * 0.95));
			innerRing = innerRing.reverse();

			PolygonWithHolesXZ upperDonut = new PolygonWithHolesXZ(asSimplePolygon(outerRing), singletonList(innerRing));

			target.drawExtrudedShape(material, outerRing, asList(bottomCenter, topCenter),
					null, null, EnumSet.of(START_CAP));

			target.drawExtrudedShape(material, innerRing, asList(topCenter, innerBottomCenter),
					null, null, EnumSet.of(END_CAP));

			triangulate(upperDonut).forEach(it -> target.drawShape(material, it, topCenter, Y_UNIT, Z_UNIT, 1));

		}

	}

	public static final class GritBin extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public GritBin(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			double height = parseHeight(node.getTags(), 0.5f);
			double width = parseWidth(node.getTags(), 1);
			double depth = width / 2f;

			/* determine material */

			Color color = parseColor(node.getTags().getValue("colour"), CSS_COLORS);
			if (color == null) {
				color = new Color(0.3f, 0.5f, 0.4f);
			}

			Material material = getSurfaceMaterial(node.getTags().getValue("material"),
					getSurfaceMaterial(node.getTags().getValue("surface"), PLASTIC))
					.withColor(color);

			double directionAngle = parseDirection(node.getTags(), PI);

			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			VectorXZ boardVector = faceVector.rightNormal();

			/* draw box */
			target.drawBox(material, getBase(),
					faceVector, height, width, depth);

			/* draw lid */
			List<VectorXYZ> vs = List.of(
					getBase().addY(height + 0.2),
					getBase().add(boardVector.mult(width / 2)).add(faceVector.mult(depth / 2)).addY(height),
					getBase().add(boardVector.mult(-width / 2)).add(faceVector.mult(depth / 2)).addY(height),
					getBase().add(boardVector.mult(-width / 2)).add(faceVector.mult(-depth / 2)).addY(height),
					getBase().add(boardVector.mult(width / 2)).add(faceVector.mult(-depth / 2)).addY(height),
					getBase().add(boardVector.mult(width / 2)).add(faceVector.mult(depth / 2)).addY(height));

			Material lidMaterial = material;
			target.drawTriangleFan(lidMaterial, vs, texCoordLists(vs, lidMaterial, SLOPED_TRIANGLES));

		}

	}

	public static final class Phone extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		private static enum Type {WALL, PILLAR, CELL, HALFCELL}

		public Phone(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

			MaterialOrRef roofMaterial;
			MaterialOrRef poleMaterial;
			Type type;

			// get Type of Phone
			if (isInWall(node)) {
				type = Type.WALL;
			} else {
				type = Type.CELL;
			}

			// Phones differ widely in appearance, hence we draw them only for known operators or brands
			if (node.getTags().containsAny(asList("operator", "brand"), asList("Deutsche Telekom AG", "Deutsche Telekom", "Telekom"))) {
				roofMaterial = TELEKOM_MANGENTA;
				poleMaterial = STEEL;
			} else if (node.getTags().containsAny(asList("operator", "brand"), asList("British Telecom"))) {
				roofMaterial = POSTBOX_ROYALMAIL;
				poleMaterial = POSTBOX_ROYALMAIL;
			} else {
				//no rendering, unknown operator or brand
				return;
			}


			// default dimensions may differ depending on the phone type
			double height = 0f;
			double width = 0f;

			switch (type) {
				case WALL:

					break;
				case CELL:
					height = parseHeight(node.getTags(), 2.1f);
					width = parseWidth(node.getTags(), 0.8f);

					target.drawBox(GLASS,
							getBase(),
							faceVector, height - 0.2, width - 0.06, width - 0.06);
					target.drawBox(roofMaterial,
							getBase().addY(height - 0.2),
							faceVector, 0.2, width, width);
					target.drawBox(poleMaterial,
							getBase().add(new VectorXYZ((width / 2 - 0.05), 0, (width / 2 - 0.05)).rotateY(directionAngle)),
							faceVector, height - 0.2, 0.1, 0.1);
					target.drawBox(poleMaterial,
							getBase().add(new VectorXYZ(-(width / 2 - 0.05), 0, (width / 2 - 0.05)).rotateY(directionAngle)),
							faceVector, height - 0.2, 0.1, 0.1);
					target.drawBox(poleMaterial,
							getBase().add(new VectorXYZ(0, 0, -(width / 2 - 0.05)).rotateY(directionAngle)),
							faceVector, height - 0.2, width, 0.1);

					break;
				default:
					assert false : "unknown or unsupported phone type";
			}

		}

	}

	public static final class VendingMachine extends NodeWorldObjectWithOptionalSupports {

		public VendingMachine(MapNode node) {
			super(node);
		}

		@Override
		protected List<String> getAttachmentTypes() {
			List<String> attachmentTypes = getCompatibleSurfaceTypes(node);
			if (attachmentTypes.isEmpty() && isInWall(node)) {
				attachmentTypes = List.of("wall");
			}
			return attachmentTypes;
		}

		@Override
		protected double getPreferredVerticalAttachmentHeight() {
			return 0.83;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			Material machineMaterial;

			if (node.getTags().contains("vending", "bicycle_tube")
					&& node.getTags().containsAny(asList("operator"), asList("Continental", "continental"))) {
				machineMaterial = new Material(Interpolation.FLAT, Color.ORANGE);
			} else if (node.getTags().contains("vending", "bicycle_tube")) {
				machineMaterial = new Material(Interpolation.FLAT, Color.BLUE);
			} else if (node.getTags().contains("vending", "cigarettes")) {
				machineMaterial = new Material(Interpolation.FLAT, new Color(0.8f, 0.73f, 0.5f));
			} else if (node.getTags().contains("vending", "condoms")) {
				machineMaterial = new Material(Interpolation.FLAT, new Color(0.39f, 0.15f, 0.11f));
			} else {
				machineMaterial = new Material(Interpolation.FLAT, LIGHT_GRAY);
			}

			double height = parseHeight(node.getTags(), 1.8f);

			@Nullable VectorXYZ supportBase = getSupportBase();
			VectorXYZ boardBase = getModelBase();
			VectorXZ direction = getDirection();

			if (supportBase != null) {

				/* draw pole */
				target.drawBox(STEEL, supportBase, direction.xz(), height - 0.3, 0.1, 0.1);

				boardBase = boardBase.add(direction.mult(0.05));

			}

			target.drawBox(machineMaterial, boardBase.add(direction.mult(0.1)), direction.xz(), height - 0.8, 1, 0.2);

		}

	}

	public static final class PostBox extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		private static enum Type {WALL, PILLAR}

		public PostBox(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

			MaterialOrRef boxMaterial = null;
			MaterialOrRef poleMaterial = null;
			Type type = null;


			// post boxes differ widely in appearance, hence we draw them only for known operators or brands
			if (node.getTags().containsAny(asList("operator", "brand"), asList("Deutsche Post AG", "Deutsche Post"))) {
				boxMaterial = POSTBOX_DEUTSCHEPOST;
				poleMaterial = STEEL;
				type = Type.WALL;
			} else if (node.getTags().contains("operator", "Royal Mail")) {
				boxMaterial = POSTBOX_ROYALMAIL;
				type = Type.PILLAR;
			} else {
				//no rendering, unknown operator or brand for post box
				return;
			}

			assert (type != Type.WALL || poleMaterial != null) : "post box of type wall requires a pole material";

			// default dimensions will differ depending on the post box type
			double height = 0f;
			double width = 0f;

			switch (type) {
				case WALL:
					height = parseHeight(node.getTags(), 0.8f);
					width = parseWidth(node.getTags(), 0.3f);

					target.drawBox(poleMaterial,
							getBase(),
							faceVector, height, 0.08, 0.08);

					target.drawBox(boxMaterial,
							getBase().add(faceVector.mult(width / 2 - 0.08 / 2)).addY(height),
							faceVector, width, width, width);
					break;
				case PILLAR:
					height = parseHeight(node.getTags(), 2f);
					width = parseWidth(node.getTags(), 0.5f);

					target.drawColumn(boxMaterial, null,
							getBase(),
							height - 0.1, width, width, false, false);
					target.drawColumn(boxMaterial, null,
							getBase().addY(height - 0.1),
							0.1, width + 0.1, 0, true, true);
					break;
				default:
					assert false : "unknown post box type";
			}

		}

	}

	public static final class BusStop extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public BusStop(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			double height = parseHeight(node.getTags(), 3.0);
			double signHeight = 0.7;
			double signWidth = 0.4;

			/* draw pole */

			target.setCurrentAttachmentTypes("bus_stop", "pole");

			double directionAngle = parseDirection(node.getTags(), PI);

			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

			target.drawColumn(STEEL, null,
					getBase(),
					height - signHeight, 0.05, 0.05, false, true);

			target.setCurrentAttachmentTypes();

			/* draw sign */
			target.drawBox(BUS_STOP_SIGN,
					getBase().addY(height - signHeight),
					faceVector, signHeight, signWidth, 0.02);
			/*  draw timetable */
			target.drawBox(STEEL,
					getBase().addY(1.2f).add(new VectorXYZ(0.055f, 0, 0f).rotateY(directionAngle)),
					faceVector, 0.31, 0.01, 0.43);

		}

	}

	public static final class ParcelLocker implements Model {

		private final TagSet tags;

		public ParcelLocker(TagSet tags) {
			this.tags = tags;
		}

		@Override
		public List<Mesh> buildMeshes(InstanceParameters params) {

			Material boxMaterial = POSTBOX_DEUTSCHEPOST.get();
			Material otherMaterial = STEEL.get();

			VectorXZ faceVector = VectorXZ.fromAngle(params.direction());
			VectorXZ rightVector = faceVector.rightNormal();

			// shape depends on type
			if (tags.containsAny(asList("parcel_locker:type", "packstation_type", "type"),
					asList("Rondell", "circular"))) {

				double height = parseHeight(tags, 2.2f);
				double width = parseWidth(tags, 3f);
				double rondelWidth = width * 2 / 3;
				double boxWidth = width * 1 / 3;
				double roofOverhang = 0.3f;

				return asList(
						/* rondel */
						new Mesh(createColumn(null, params.position().add(rightVector.mult(-rondelWidth / 2)),
								height, rondelWidth / 2, rondelWidth / 2, false, true,
								boxMaterial.color(), boxMaterial.textureDimensions()), boxMaterial),
						/* box */
						new Mesh(createBox(params.position().add(rightVector.mult(boxWidth / 2)).add(faceVector.mult(-boxWidth / 2)),
								faceVector, height, boxWidth, boxWidth,
								boxMaterial.color(), boxMaterial.textureDimensions()), boxMaterial),
						/* roof */
						new Mesh(createColumn(null, params.position().addY(height),
								0.1, rondelWidth / 2 + roofOverhang / 2, rondelWidth / 2 + roofOverhang / 2, true, true,
								otherMaterial.color(), otherMaterial.textureDimensions()), otherMaterial));

			} else if (tags.containsAny(asList("parcel_locker:type", "packstation_type", "type"),
					asList("Paketbox", "parcel_box"))) {

				double height = parseHeight(tags, 1.5);
				double width = parseHeight(tags, 1.0);
				double depth = width;

				return asList(new Mesh(createBox(params.position(), faceVector, height, width * 2, depth * 2,
						boxMaterial.color(), boxMaterial.textureDimensions()), boxMaterial));

			} else { // type=Schrank or type=24/7 Station (they look roughly the same) or no type (fallback)

				double height = parseHeight(tags, 2.2);
				double width = parseWidth(tags, 3.5);
				double depth = width / 3;
				double roofOverhang = 0.3f;

				return asList(
						/* box */
						new Mesh(createBox(params.position(),
								faceVector, height, width, depth,
								boxMaterial.color(), boxMaterial.textureDimensions()), boxMaterial),
						/* small roof */
						new Mesh(createBox(params.position().add(faceVector.mult(roofOverhang)).addY(height),
								faceVector, 0.1, width, depth + roofOverhang * 2,
								boxMaterial.color(), boxMaterial.textureDimensions()), boxMaterial));

			}

		}

	}

	public static final class FireHydrant extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public FireHydrant(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			double height = parseHeight(node.getTags(), 1.0);

			/* draw main pole */
			target.drawColumn(FIREHYDRANT, null,
					getBase(),
					height,
					0.15, 0.15, false, true);

			/* draw two small and one large valve */
			VectorXYZ valveBaseVector = getBase().addY(height - 0.3);
			VectorXZ smallValveVector = VectorXZ.X_UNIT;
			VectorXZ largeValveVector = VectorXZ.Z_UNIT;

			target.drawBox(FIREHYDRANT,
					valveBaseVector,
					smallValveVector, 0.1f, 0.5f, 0.1f);
			target.drawBox(FIREHYDRANT,
					valveBaseVector.add(0.2f, -0.1f, 0f),
					largeValveVector, 0.15f, 0.15f, 0.15f);
		}

	}

	public static final class StreetLamp extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public StreetLamp(MapNode node) {
			super(node);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD2, LOD4);

			double lampHeight = 0.8;
			double lampHalfWidth = 0.4;
			double poleHeight = parseHeight(node.getTags(), 5.0) - lampHeight;

			/* determine material */

			Material material = null;

			if (material == null) {
				material = getSurfaceMaterial(
						node.getTags().getValue("material"));
			}

			if (material == null) {
				material = getSurfaceMaterial(
						node.getTags().getValue("surface"), STEEL);
			}

			/* draw pole */

			target.setCurrentAttachmentTypes("street_lamp");

			target.drawExtrudedShape(material, new CircleXZ(NULL_VECTOR, 1),
					asList(getBase(), getBase().addY(0.5), getBase().addY(0.5 + poleHeight)),
					null, asList(0.16, 0.08, 0.08), null);

			target.setCurrentAttachmentTypes();

			/* draw lamp */

			// lower part
			List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
			vs.add(getBase().addY(poleHeight));
			vs.add(getBase().addY(poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, lampHalfWidth));
			vs.add(getBase().addY(poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, -lampHalfWidth));
			vs.add(getBase().addY(poleHeight + lampHeight * 0.8).add(-lampHalfWidth, 0, -lampHalfWidth));
			vs.add(getBase().addY(poleHeight + lampHeight * 0.8).add(-lampHalfWidth, 0, lampHalfWidth));
			vs.add(getBase().addY(poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, lampHalfWidth));

			target.drawTriangleFan(material, vs, texCoordLists(vs, material, GLOBAL_X_Z));

			// upper part
			vs = new ArrayList<VectorXYZ>();
			vs.add(getBase().addY(poleHeight + lampHeight));
			vs.add(getBase().addY(poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, lampHalfWidth));
			vs.add(getBase().addY(poleHeight + lampHeight * 0.8).add(-lampHalfWidth, 0, lampHalfWidth));
			vs.add(getBase().addY(poleHeight + lampHeight * 0.8).add(-lampHalfWidth, 0, -lampHalfWidth));
			vs.add(getBase().addY(poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, -lampHalfWidth));
			vs.add(getBase().addY(poleHeight + lampHeight * 0.8).add(lampHalfWidth, 0, lampHalfWidth));

			target.drawTriangleFan(material, vs, texCoordLists(vs, material, GLOBAL_X_Z));
		}

	}

	public static final class Board extends NodeWorldObjectWithOptionalSupports {

		public Board(MapNode node) {
			super(node);
		}

		@Override
		protected double getPreferredVerticalAttachmentHeight() {
			return 1.2;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			target.setCurrentLodRange(LOD3, LOD4);

			@Nullable VectorXYZ supportBase = getSupportBase();
			VectorXYZ boardBase = getModelBase();
			VectorXZ direction = getDirection();

			if (supportBase != null) {
				drawSupport(target, supportBase, direction);
			}

			drawBoard(target, boardBase, direction);

		}

		private void drawBoard(Target target, VectorXYZ boardBase, VectorXZ faceVector) {
			target.drawBox(WOOD,
					boardBase,
					faceVector, 0.4, 0.4, 0.1);
		}

		private void drawSupport(Target target, VectorXYZ supportBase, VectorXZ faceVector) {
			target.drawColumn(WOOD, null,
					supportBase,
					1.5, 0.05, 0.05, false, true);
		}

	}

}

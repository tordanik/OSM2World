package org.osm2world.core.world.modules;

import static java.awt.Color.*;
import static java.lang.Math.min;
import static java.lang.Math.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.math.NumberUtils.max;
import static org.osm2world.core.math.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.core.math.SimplePolygonXZ.asSimplePolygon;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.math.VectorXYZ.Z_UNIT;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.math.algorithms.TriangulationUtil.triangulate;
import static org.osm2world.core.target.common.ExtrudeOption.END_CAP;
import static org.osm2world.core.target.common.ExtrudeOption.START_CAP;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.mesh.ExtrusionGeometry.createColumn;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.*;
import static org.osm2world.core.target.common.mesh.MeshUtil.createBox;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.*;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.core.util.ValueParseUtil.*;
import static org.osm2world.core.util.color.ColorNameDefinitions.CSS_COLORS;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseInt;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.awt.*;
import java.time.LocalTime;
import java.util.List;
import java.util.*;

import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.*;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.ExtrudeOption;
import org.osm2world.core.target.common.material.ConfMaterial;
import org.osm2world.core.target.common.material.ImmutableMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.mesh.ExtrusionGeometry;
import org.osm2world.core.target.common.mesh.LevelOfDetail;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.model.InstanceParameters;
import org.osm2world.core.target.common.model.LegacyModel;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.common.texcoord.TexCoordFunction;
import org.osm2world.core.world.attachment.AttachmentConnector;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.attachment.AttachmentSurface.Builder;
import org.osm2world.core.world.data.LegacyWorldObject;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.data.NodeModelInstance;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

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
			node.addRepresentation(new VendingMachineVice(node));
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

	public static final class Pole extends NoOutlineNodeWorldObject {

		public Pole(MapNode node) {
			super(node);
		}

		@Override
		public List<Mesh> buildMeshes() {

			double height = parseMeasure(node.getTags().getValue("height"), 5.0);
			double radius = parseMeasure(node.getTags().getValue("width"), 0.2) / 2;

			Material material = Materials.getMaterial(node.getTags().getValue("material"), STEEL);
			Color color = parseColor(node.getTags().getValue("colour"));

			ExtrusionGeometry geometry = ExtrusionGeometry.createColumn(null, this.getBase(),
					height, radius, radius, false, true,
					color, material.getTextureDimensions());

			return singletonList(new Mesh(geometry, material, height >= 10 ? LOD2 : LOD3, LOD4));

		}

		@Override
		public Collection<AttachmentSurface> getAttachmentSurfaces() {
			return singleton(AttachmentSurface.fromMeshes("pole", buildMeshes()));
		}

	}

	public static final class Flagpole extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		public Flagpole(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD2, LOD4);
		}

		@Override
		public void renderTo(Target target) {

			/* draw the pole */

			double poleHeight = parseHeight(node.getTags(), 10.0);
			double poleRadius = 0.15;

			VectorXYZ poleBase = getBase();

			target.drawColumn(STEEL, null, poleBase,
					poleHeight, poleRadius, poleRadius, false, true);

			/* draw the flag (if any) */

			Flag flag = null;

			if (node.getTags().contains("flag:type", "national")
					&& node.getTags().containsKey("country")) {
				flag = NATIONAL_FLAGS.get(node.getTags().getValue("country"));
			}

			if (flag == null && node.getTags().containsKey("flag:colour")) {
				Color color = parseColor(node.getTags().getValue("flag:colour"), CSS_COLORS);
				if (color != null) {
					flag = new TexturedFlag(2 / 3.0, FLAGCLOTH.withColor(color));
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

				TexCoordFunction texCoordFunction = (List<VectorXYZ> vs) ->
					vs.stream().map(texCoordMap::get).collect(toList());

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

					if (!material.isDoubleSided()) {
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

						texCoordMap.put(vertices[x][y], new VectorXZ(xRatio, yRatio));

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
				return colors.stream().map(FLAGCLOTH::withColor).collect(toList());
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
			 * alternative constructor that uses the aspect ratio of the first texture layer's color texture
			 * instead of an explicit heightWidthRatio parameter. Only works for textured materials.
			 */
			public TexturedFlag(Material material) {
				this(material.getTextureLayers().get(0).baseColorTexture.height
						/ material.getTextureLayers().get(0).baseColorTexture.width, material);
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

	public static final class AdvertisingColumn extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		public AdvertisingColumn(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

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

	public static final class Billboard extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		private final double width;
		/** the height of the billboard itself, i.e. height minus minHeight */
		private final double trueHeight;
		private final double minHeight;

		private final AttachmentConnector connector;

		public Billboard(MapNode node) {

			super(node);

			width = parseWidth(node.getTags(), 4);

			double height = parseHeight(node.getTags(), 3.5f);
			minHeight = height / 5;
			trueHeight = height - minHeight;

			if (node.getTags().contains("support", "wall") && node.getTags().containsKey("level")) {
				connector = new AttachmentConnector(asList("wall" + parseLevels(node.getTags().getValue("level")).get(0)),
						node.getPos().xyz(0), this, minHeight + trueHeight / 2, true);
			} else if (node.getTags().contains("support", "wall")) {
				connector = new AttachmentConnector(asList("wall"),
						node.getPos().xyz(0), this, minHeight + trueHeight / 2, true);
			} else {
				connector = null;
			}

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD2, LOD4);
		}

		@Override
		public Iterable<AttachmentConnector> getAttachmentConnectors() {
			if (connector == null) {
				return emptyList();
			} else {
				return singleton(connector);
			}
		}

		@Override
		public void renderTo(Target target) {

			VectorXZ faceVector;
			VectorXYZ bottomCenter;

			if (connector == null || !connector.isAttached()) {
				faceVector = VectorXZ.fromAngle(parseDirection(node.getTags(), PI));
				bottomCenter = getBase().addY(minHeight);
			} else {
				faceVector = connector.getAttachedSurfaceNormal().xz();
				bottomCenter = connector.getAttachedPos().addY(-trueHeight/2).add(faceVector.mult(0.01));
			}

			VectorXZ boardVector = faceVector.rightNormal();

			/* draw board */

			VectorXYZ[] vsPoster = {
					bottomCenter.add(boardVector.mult(width / 2)).addY(trueHeight),
					bottomCenter.add(boardVector.mult(width / 2)),
					bottomCenter.add(boardVector.mult(-width / 2)).addY(trueHeight),
					bottomCenter.add(boardVector.mult(-width / 2))
			};

			List<VectorXYZ> vsListPoster = asList(vsPoster);

			target.drawTriangleStrip(ADVERTISING_POSTER, vsListPoster,
					texCoordLists(vsListPoster, ADVERTISING_POSTER, STRIP_FIT));

			List<VectorXYZ> vsBoard = asList(
					vsPoster[2],
					vsPoster[3],
					vsPoster[0],
					vsPoster[1]
			);

			if (node.getTags().contains("two_sided", "yes")) {

				Material backMaterial = ADVERTISING_POSTER;
				target.drawTriangleStrip(backMaterial, vsBoard,
						texCoordLists(vsBoard, backMaterial, STRIP_FIT));

			} else {

				Material backMaterial = CONCRETE;
				target.drawTriangleStrip(backMaterial, vsBoard,
						texCoordLists(vsBoard, backMaterial, STRIP_WALL));

			}

			/* draw frame */

			target.drawBox(CONCRETE, bottomCenter.addY(trueHeight - 0.1),
					faceVector, 0.1, width, 0.1);

			target.drawBox(CONCRETE, bottomCenter,
					faceVector, 0.1, width, 0.1);

			target.drawBox(CONCRETE, bottomCenter.add(boardVector.mult(width / 2)),
					faceVector, trueHeight, 0.1, 0.1);

			target.drawBox(CONCRETE, bottomCenter.add(boardVector.mult(-width / 2)),
					faceVector, trueHeight, 0.1, 0.1);

			/* draw poles */

			if (connector == null) {

				VectorXZ[] poles = {
						node.getPos().add(boardVector.mult(-width / 4)),
						node.getPos().add(boardVector.mult(+width / 4))
				};

				for (VectorXZ pole : poles) {
					target.drawBox(CONCRETE, pole.xyz(getBase().y),
							faceVector, minHeight, 0.2, 0.1);
				}

			}

		}

	}

	public static final class Swing extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		public Swing(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

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
				material = Materials.getMaterial(node.getTags().getValue("material").toUpperCase());
			}

			if (material == null) {
				material = WOOD;
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
				target.drawExtrudedShape(STEEL, shape, path, nCopies(2,Z_UNIT.invert()), null, null, null);
				target.drawExtrudedShape(STEEL, shape, path, nCopies(2,Z_UNIT), null, null, null);
			}
		}
	}

	public static final class Bench extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		private final AttachmentConnector connector;

		public Bench(MapNode node) {
			super(node);

			if (node.getTags().containsKey("level")) {
				connector = new AttachmentConnector(singletonList("floor" +  parseLevels(node.getTags().getValue("level")).get(0).toString()),
						node.getPos().xyz(0), this, 0.6, false);
			} else {
				connector = null;
			}

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

			/* determine elevation from connector */

			VectorXYZ position = getBase();

			if (connector != null && connector.isAttached()) {
				position = position.xz().xyz(connector.getAttachedPos().getY());
			}

			/* determine the width of the bench */

			double defaultWidth = 0.5f * parseInt(node.getTags(), 4, "seats");

			double width = parseWidth(node.getTags(), defaultWidth);

			/* determine material and color */

			Material material = null;

			if (node.getTags().containsKey("material")) {
				material = Materials.getMaterial(node.getTags().getValue("material").toUpperCase());
			}

			if (material == null) {
				material = WOOD;
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

		@Override
		public Iterable<AttachmentConnector> getAttachmentConnectors() {
			if (connector == null) {
				return emptyList();
			} else {
				return singleton(connector);
			}
		}

	}


	public static final class Table extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		private final ConfMaterial defaultMaterial;

		public Table(MapNode node) {
			super(node);

			if (node.getTags().contains("leisure", "picnic_table")) {
				defaultMaterial = Materials.WOOD;
			} else {
				defaultMaterial = Materials.STEEL;
			}
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

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
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("material"));
			}

			if (material == null) {
				material = Materials.getSurfaceMaterial(
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
	public static final class Cross extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		public Cross(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD2, LOD4);
		}

		@Override
		public void renderTo(Target target) {

			boolean summit = node.getTags().containsKey("summit:cross")
					|| node.getTags().contains("natural", "peak");

			double height = parseHeight(node.getTags(), summit ? 4f : 2f);
			double width = parseHeight(node.getTags(), height * 2 / 3);

			double thickness = min(height, width) / 8;

			/* determine material and direction */

			Material material = null;

			//TODO parse color

			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("material"));
			}

			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("surface"), Materials.WOOD);
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
	public static final class Clock implements NodeWorldObject, LegacyWorldObject {

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
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD2, LOD4);
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
		public void renderTo(Target target) {

			if (!connector.isAttached()) return;

			double diameter = parseWidth(node.getTags(), 1f);
			new ClockFace(TIME).render(target, new InstanceParameters(connector.getAttachedPos(),
					connector.getAttachedSurfaceNormal().xz().angle(),
					null, diameter, null));

		}

		private static class ClockFace implements LegacyModel {

			private final LocalTime time;

			public ClockFace(LocalTime currentTime) {
				this.time = currentTime;
			}

			@Override
			public void render(Target target, InstanceParameters params) {

				double diameter = params.width() != null ? params.width() : 1.0;
				double thickness = params.length() != null ? params.length() : 0.08;

				VectorXZ faceNormal = VectorXZ.fromAngle(params.direction());

				VectorXYZ backCenter = params.position().add(faceNormal.mult(thickness * 0.7));
				VectorXYZ frontCenter = params.position().add(faceNormal.mult(thickness));

				CircleXZ outerCircle = new CircleXZ(NULL_VECTOR, diameter / 2);
				CircleXZ innerCircle = new CircleXZ(NULL_VECTOR, diameter / 2.2);

				PolygonWithHolesXZ ring = new PolygonWithHolesXZ(asSimplePolygon(outerCircle),
						asList(asSimplePolygon(innerCircle).reverse()));

				target.drawExtrudedShape(PLASTIC.withColor(BLACK), ring,
						asList(params.position(), frontCenter),
						nCopies(2, Y_UNIT), null, null, EnumSet.of(ExtrudeOption.END_CAP));

				target.drawShape(PLASTIC, innerCircle, backCenter, faceNormal.xyz(0), Y_UNIT, 1);

				drawHand(target, frontCenter, faceNormal, diameter / 20, diameter / 2.5, thickness / 5, angleMinuteHand(time));
				drawHand(target, frontCenter, faceNormal, diameter / 15, diameter / 4, thickness / 5, angleHourHand(time));

			}

			private final void drawHand(Target target, VectorXYZ origin, VectorXZ faceNormal,
					double width, double length, double thickness, double angleRad) {

				assert width < length;

				ShapeXZ handShape = new AxisAlignedRectangleXZ(-width/2, -width/2, width/2, length - width/2);
				handShape = handShape.rotatedCW(angleRad);

				target.drawExtrudedShape(PLASTIC.withColor(BLACK), handShape,
						asList(origin, origin.add(faceNormal.mult(thickness))),
						nCopies(2, Y_UNIT), null, null, EnumSet.of(ExtrudeOption.END_CAP));

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

	public static final class RecyclingContainer extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		double directionAngle = parseDirection(node.getTags(), PI);
		VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

		public RecyclingContainer(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD2, LOD4);
		}

		@Override
		public void renderTo(Target target) {

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
				target.drawBox(new ImmutableMaterial(Interpolation.FLAT, new Color(0.82f, 0.784f, 0.75f)),
						pos,
						faceVector, 2, 1, 1);
			} else { // "paper" || "white_glass" || "coloured_glass"
				float width = 1.5f;
				float height = 1.6f;

				Material colourFront = null;
				Material colourBack = null;

				if ("paper".equals(trash)) {
					colourFront = new ImmutableMaterial(Interpolation.FLAT, Color.BLUE);
					colourBack = new ImmutableMaterial(Interpolation.FLAT, Color.BLUE);
				} else if ("white_glass".equals(trash)) {
					colourFront = new ImmutableMaterial(Interpolation.FLAT, Color.WHITE);
					colourBack = new ImmutableMaterial(Interpolation.FLAT, Color.WHITE);
				} else { // "coloured_glass"
					colourFront = new ImmutableMaterial(Interpolation.FLAT, new Color(0.18f, 0.32f, 0.14f));
					colourBack = new ImmutableMaterial(Interpolation.FLAT, new Color(0.39f, 0.15f, 0.11f));
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

	public static final class WasteBasket extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		private final AttachmentConnector connector;

		public WasteBasket(MapNode node) {

			super(node);

			if (node.getTags().containsKey("support") &&
					!node.getTags().contains("support", "ground")) {
				connector = new AttachmentConnector(singletonList(node.getTags().getValue("support")),
						node.getPos().xyz(0), this, 0.6, true);
			} else {
				connector = null;
			}

		}

		@Override
		public GroundState getGroundState() {
			if (connector != null && connector.isAttached()) {
				return GroundState.ATTACHED;
			} else {
				return GroundState.ON;
			}
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public Iterable<AttachmentConnector> getAttachmentConnectors() {
			if (connector == null) {
				return emptyList();
			} else {
				return singleton(connector);
			}
		}

		@Override
		public void renderTo(Target target) {

			/* determine material */

			Material material = null;

			if (node.getTags().containsKey("material")) {
				material = Materials.getMaterial(node.getTags().getValue("material").toUpperCase());
			}

			if (material == null) {
				material = STEEL;
			}

			material = material.withColor(parseColor(node.getTags().getValue("colour"), CSS_COLORS));

			/* determine position */

			VectorXYZ pos;
			VectorXYZ direction = VectorXZ.fromAngle(parseDirection(node.getTags(), PI)).xyz(0);

			if (connector != null && connector.isAttached()) {

				pos = connector.getAttachedPos();
				direction = connector.getAttachedSurfaceNormal();

			} else {

				pos = getBase().addY(0.6).add(direction.mult(0.05));

				/* draw pole */
				target.drawColumn(material, null, getBase(), 1.2, 0.06, 0.06, false, true);

			}

			/* draw basket */
			renderBasket(target, material, pos, direction);

		}

		private void renderBasket(Target target, Material material, VectorXYZ pos, VectorXYZ direction) {

			double height = 0.5;
			double radius = 0.2;

			VectorXYZ bottomCenter = pos.addY(-0.1).add(direction.mult(0.2));
			VectorXYZ innerBottomCenter = bottomCenter.addY(height * 0.05);
			VectorXYZ topCenter = bottomCenter.addY(height);

			SimplePolygonXZ outerRing = asSimplePolygon(new CircleXZ(NULL_VECTOR, radius));
			SimplePolygonXZ innerRing = asSimplePolygon(new CircleXZ(NULL_VECTOR, radius * 0.95));
			innerRing = innerRing.reverse();

			PolygonWithHolesXZ upperDonut = new PolygonWithHolesXZ(asSimplePolygon(outerRing), singletonList(innerRing));

			target.drawExtrudedShape(material, outerRing, asList(bottomCenter, topCenter),
					null, null, null, EnumSet.of(START_CAP));

			target.drawExtrudedShape(material, innerRing, asList(topCenter, innerBottomCenter),
					null, null, null, EnumSet.of(END_CAP));

			triangulate(upperDonut).forEach(it -> target.drawShape(material, it, topCenter, Y_UNIT, Z_UNIT, 1));

		}

	}

	public static final class GritBin extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		public GritBin(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

			double height = parseHeight(node.getTags(), 0.5f);
			double width = parseWidth(node.getTags(), 1);
			double depth = width / 2f;

			/* determine material */

			Material material = null;

			//TODO parse color

			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("material"));
			}

			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("surface"), Materials.GRITBIN_DEFAULT);
			}

			double directionAngle = parseDirection(node.getTags(), PI);

			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			VectorXZ boardVector = faceVector.rightNormal();

			/* draw box */
			target.drawBox(material, getBase(),
					faceVector, height, width, depth);

			/* draw lid */
			List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
			vs.add(getBase().addY(height + 0.2));
			vs.add(getBase().add(boardVector.mult(width / 2)).add(faceVector.mult(depth / 2)).addY(height));
			vs.add(getBase().add(boardVector.mult(-width / 2)).add(faceVector.mult(depth / 2)).addY(height));
			vs.add(getBase().add(boardVector.mult(-width / 2)).add(faceVector.mult(-depth / 2)).addY(height));
			vs.add(getBase().add(boardVector.mult(width / 2)).add(faceVector.mult(-depth / 2)).addY(height));
			vs.add(getBase().add(boardVector.mult(width / 2)).add(faceVector.mult(depth / 2)).addY(height));

			Material lidMaterial = material.withColor(material.getColor().brighter());
			target.drawTriangleFan(lidMaterial, vs, texCoordLists(vs, lidMaterial, SLOPED_TRIANGLES));

		}

	}

	public static final class Phone extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		private static enum Type {WALL, PILLAR, CELL, HALFCELL}

		public Phone(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

			Material roofMaterial = null;
			Material poleMaterial = null;
			Type type = null;

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

	public static final class VendingMachineVice extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		private static enum Type {WALL, PILLAR}

		public VendingMachineVice(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

			Material machineMaterial = null;
			Material poleMaterial = STEEL;
			Type type = null;

			if (node.getTags().contains("vending", "bicycle_tube")
					&& node.getTags().containsAny(asList("operator"), asList("Continental", "continental"))) {
				machineMaterial = new ImmutableMaterial(Interpolation.FLAT, Color.ORANGE);
			} else if (node.getTags().contains("vending", "bicycle_tube")) {
				machineMaterial = new ImmutableMaterial(Interpolation.FLAT, Color.BLUE);
			} else if (node.getTags().contains("vending", "cigarettes")) {
				machineMaterial = new ImmutableMaterial(Interpolation.FLAT, new Color(0.8f, 0.73f, 0.5f));
			} else if (node.getTags().contains("vending", "condoms")) {
				machineMaterial = new ImmutableMaterial(Interpolation.FLAT, new Color(0.39f, 0.15f, 0.11f));
			}

			// get Type of vending machine
			if (isInWall(node)) {
				type = Type.WALL;
			} else {
				type = Type.PILLAR;
			}

			// default dimensions will differ depending on the post box type
			double height = 0f;

			switch (type) {
				case WALL:

					break;
				case PILLAR:
					height = parseHeight(node.getTags(), 1.8f);

					target.drawBox(poleMaterial,
							getBase().add(new VectorXYZ(0, 0, -0.05).rotateY(faceVector.angle())),
							faceVector, height - 0.3, 0.1, 0.1);
					target.drawBox(machineMaterial,
							getBase().addY(height - 1).add(new VectorXYZ(0, 0, 0.1).rotateY(directionAngle)),
							faceVector, 1, 1, 0.2);

					break;
				default:
					assert false : "unknown or unsupported Vending machine Type";
			}

		}

	}

	public static final class PostBox extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		private static enum Type {WALL, PILLAR}

		public PostBox(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

			Material boxMaterial = null;
			Material poleMaterial = null;
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

	public static final class BusStop extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		public BusStop(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

			double height = parseHeight(node.getTags(), 3.0);
			double signHeight = 0.7;
			double signWidth = 0.4;

			Material poleMaterial = STEEL;

			double directionAngle = parseDirection(node.getTags(), PI);

			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

			target.drawColumn(poleMaterial, null,
					getBase(),
					height - signHeight, 0.05, 0.05, false, true);

			if (target instanceof AttachmentSurface.Builder) return;

			/* draw sign */
			target.drawBox(BUS_STOP_SIGN,
					getBase().addY(height - signHeight),
					faceVector, signHeight, signWidth, 0.02);
			/*  draw timetable */
			target.drawBox(poleMaterial,
					getBase().addY(1.2f).add(new VectorXYZ(0.055f, 0, 0f).rotateY(directionAngle)),
					faceVector, 0.31, 0.01, 0.43);

		}

		@Override
		public Collection<AttachmentSurface> getAttachmentSurfaces() {
			Builder builder = new AttachmentSurface.Builder("bus_stop", "pole");
			this.renderTo(builder);
			return singleton(builder.build());
		}

	}

	public static final class ParcelLocker implements Model {

		private final TagSet tags;

		public ParcelLocker(TagSet tags) {
			this.tags = tags;
		}

		@Override
		public List<Mesh> buildMeshes(InstanceParameters params) {

			Material boxMaterial = POSTBOX_DEUTSCHEPOST;
			Material otherMaterial = STEEL;

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
								boxMaterial.getColor(), boxMaterial.getTextureDimensions()), boxMaterial),
						/* box */
						new Mesh(createBox(params.position().add(rightVector.mult(boxWidth / 2)).add(faceVector.mult(-boxWidth / 2)),
								faceVector, height, boxWidth, boxWidth,
								boxMaterial.getColor(), boxMaterial.getTextureDimensions()), boxMaterial),
						/* roof */
						new Mesh(createColumn(null, params.position().addY(height),
								0.1, rondelWidth / 2 + roofOverhang / 2, rondelWidth / 2 + roofOverhang / 2, true, true,
								otherMaterial.getColor(), otherMaterial.getTextureDimensions()), otherMaterial));

			} else if (tags.containsAny(asList("parcel_locker:type", "packstation_type", "type"),
					asList("Paketbox", "parcel_box"))) {

				double height = parseHeight(tags, 1.5);
				double width = parseHeight(tags, 1.0);
				double depth = width;

				return asList(new Mesh(createBox(params.position(), faceVector, height, width * 2, depth * 2,
						boxMaterial.getColor(), boxMaterial.getTextureDimensions()), boxMaterial));

			} else { // type=Schrank or type=24/7 Station (they look roughly the same) or no type (fallback)

				double height = parseHeight(tags, 2.2);
				double width = parseWidth(tags, 3.5);
				double depth = width / 3;
				double roofOverhang = 0.3f;

				return asList(
						/* box */
						new Mesh(createBox(params.position(),
								faceVector, height, width, depth,
								boxMaterial.getColor(), boxMaterial.getTextureDimensions()), boxMaterial),
						/* small roof */
						new Mesh(createBox(params.position().add(faceVector.mult(roofOverhang)).addY(height),
								faceVector, 0.1, width, depth + roofOverhang * 2,
								boxMaterial.getColor(), boxMaterial.getTextureDimensions()), boxMaterial));

			}

		}

	}

	public static final class FireHydrant extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		public FireHydrant(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

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

	public static final class StreetLamp extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		public StreetLamp(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD2, LOD4);
		}

		@Override
		public void renderTo(Target target) {

			double lampHeight = 0.8;
			double lampHalfWidth = 0.4;
			double poleHeight = parseHeight(node.getTags(), 5.0) - lampHeight;

			/* determine material */

			Material material = null;

			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("material"));
			}

			if (material == null) {
				material = Materials.getSurfaceMaterial(
						node.getTags().getValue("surface"), STEEL);
			}

			/* draw pole */

			target.drawExtrudedShape(material, new CircleXZ(NULL_VECTOR, 1),
					asList(getBase(), getBase().addY(0.5), getBase().addY(0.5 + poleHeight)),
					null, asList(0.16, 0.08, 0.08), null, null);

			if (target instanceof AttachmentSurface.Builder) return;

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

		@Override
		public Collection<AttachmentSurface> getAttachmentSurfaces() {
			Builder builder = new AttachmentSurface.Builder("street_lamp");
			this.renderTo(builder);
			return singleton(builder.build());
		}

	}

	public static final class Board extends NoOutlineNodeWorldObject implements LegacyWorldObject {

		public Board(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public Pair<LevelOfDetail, LevelOfDetail> getLodRange() {
			return Pair.of(LOD3, LOD4);
		}

		@Override
		public void renderTo(Target target) {

			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);
			target.drawColumn(WOOD, null,
					getBase(),
					1.5, 0.05, 0.05, false, true);
			target.drawBox(WOOD,
					getBase().addY(1.2),
					faceVector, 0.4, 0.4, 0.1);
		}

	}

}

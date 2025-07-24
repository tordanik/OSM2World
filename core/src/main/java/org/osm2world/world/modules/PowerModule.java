package org.osm2world.world.modules;

import static java.awt.Color.BLACK;
import static java.lang.Math.*;
import static java.lang.Math.max;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Collections.min;
import static java.util.Comparator.comparingDouble;
import static java.util.stream.Collectors.toList;
import static org.osm2world.map_elevation.data.GroundState.ON;
import static org.osm2world.math.VectorXYZ.*;
import static org.osm2world.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.math.VectorXZ.angleBetween;
import static org.osm2world.math.algorithms.GeometryUtil.equallyDistributePointsAlong;
import static org.osm2world.scene.material.Materials.PLASTIC;
import static org.osm2world.scene.material.Materials.SOLAR_PANEL;
import static org.osm2world.scene.texcoord.NamedTexCoordFunction.*;
import static org.osm2world.scene.texcoord.TexCoordUtil.texCoordLists;
import static org.osm2world.scene.texcoord.TexCoordUtil.triangleTexCoordLists;
import static org.osm2world.util.ValueParseUtil.parseMeasure;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.rotateShapeX;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.*;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapArea;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_data.data.MapWaySegment;
import org.osm2world.map_data.data.overlaps.MapOverlap;
import org.osm2world.map_elevation.data.EleConnectorGroup;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.Angle;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.*;
import org.osm2world.output.CommonTarget;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Materials;
import org.osm2world.scene.material.TextureDataDimensions;
import org.osm2world.scene.mesh.ExtrusionGeometry;
import org.osm2world.scene.mesh.LODRange;
import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.model.InstanceParameters;
import org.osm2world.scene.model.Model;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.scene.model.ProceduralModel;
import org.osm2world.scene.texcoord.TexCoordFunction;
import org.osm2world.util.FaultTolerantIterationUtil;
import org.osm2world.util.ValueParseUtil;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.data.AbstractAreaWorldObject;
import org.osm2world.world.data.NoOutlineNodeWorldObject;
import org.osm2world.world.data.NoOutlineWaySegmentWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.common.AbstractModule;

/**
 * module for power infrastructure
 */
public final class PowerModule extends AbstractModule {

	private static TowerConfig generateTowerConfig(MapNode node) {

		List<MapWaySegment> powerLines = new ArrayList<MapWaySegment>();
		for (MapWaySegment way : node.getConnectedWaySegments()) {
			if (way.getTags().contains("power", "line")) {
				powerLines.add(way);
			}
		}

		VectorXZ dir = VectorXZ.Z_UNIT;
		int cables = -1;
		int voltage = -1;

		if (!powerLines.isEmpty()) {
			dir = VectorXZ.NULL_VECTOR;
			for (MapWaySegment powerLine : powerLines) {
				dir = dir.add(powerLine.getDirection());

				try {
					cables = Integer.valueOf(powerLine.getTags().getValue("cables"));
				} catch (NumberFormatException e) {}
				try {
					voltage = Integer.valueOf(powerLine.getTags().getValue("voltage"));
				} catch (NumberFormatException e) {}
			}
			if (dir.length() < 0.001) {
				// can happen in rare cases, e.g. if two ways in opposite directions cancel each other out
				dir = powerLines.get(0).getDirection();
			}
			dir = dir.normalize();
		}

		return new TowerConfig(node, cables, voltage, dir);
	}

	@Override
	protected void applyToNode(MapNode node) {

		if (node.getTags().contains("power", "cable_distribution_cabinet")) {
			node.addRepresentation(new PowerCabinet(node));
		}

		if (node.getTags().contains("power", "pole")) {
			node.addRepresentation(new Powerpole(node));
		}
		if (node.getTags().contains("power", "generator")
			&& node.getTags().contains("generator:source", "wind")) {
			node.addRepresentation(new WindTurbine(node));
		}

		if (node.getTags().contains("power", "tower")) {

			TowerConfig config = generateTowerConfig(node);

			if (node.getPrimaryRepresentation() == null) {
				if (config.isHighVoltagePowerTower()) {
					node.addRepresentation(new HighVoltagePowerTower(node, config));
				} else {
					node.addRepresentation(new PowerTower(node, config));
				}
			}
		}
	}

	@Override
	protected void applyToWaySegment(MapWaySegment segment) {
		if (segment.getTags().contains("power", "minor_line")) {
			segment.addRepresentation(new PowerMinorLine(segment));
		}

		if (segment.getTags().contains("power", "line")) {
			segment.addRepresentation(new PowerLine(segment));
		}
	}

	@Override
	protected void applyToArea(MapArea area) {
		if (area.getTags().contains("power", "generator")
				&& (area.getTags().contains("generator:method", "photovoltaic")
						|| area.getTags().contains("generator:type", "solar_photovoltaic_panel"))) {
			if (area.getTags().contains("location", "roof")) {
				area.addRepresentation(new RooftopSolarPanels(area));
			} else {
				area.addRepresentation(new PhotovoltaicPlant(area));
			}
		}
	}

	private static final class PowerCabinet extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public PowerCabinet(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

			Material material = PLASTIC.withColor(new Color(184, 184, 184));
			target.drawBox(material, getBase(), faceVector, 1.5, 0.8, 0.3);

		}

	}

	private final static class TowerConfig {
		MapNode pos;
		int cables;
		int voltage;
		VectorXZ direction;

		public TowerConfig(MapNode pos, int cables, int voltage, VectorXZ direction) {
			super();
			this.pos = pos;
			this.cables = cables;
			this.voltage = voltage;
			this.direction = direction;
		}

		public boolean isHighVoltagePowerTower() {
			return voltage >= 50000 || cables >= 6;
		}
	}

	private static final class Powerpole extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		public Powerpole(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			/* determine material */

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

			target.drawColumn(material, null, getBase(),
					parseHeight(node.getTags(), 8f),
					0.15, 0.15, false, true);
		}

	}

	public static final class WindTurbine extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		/** model of a rotor with 1 m rotor diameter */
		public static final Model ROTOR = new ProceduralModel() {

			@Override
			public void render(CommonTarget target, InstanceParameters params) {

				double bladeLength = (params.height() == null ? 1 : params.height()) / 2;
				double bladeWidth = 0.1 * bladeLength;

				Material bladeMaterial = Materials.STEEL; // probably fibre, but color matches roughly :)

				// define first blade
				List<VectorXYZ> bladeFront = asList(
						params.position().add(-bladeWidth/5, 0, +bladeWidth/2),
						params.position().add(0, -bladeLength, 0),
						params.position().add(+bladeWidth/5, 0, -bladeWidth/2)
				);

				List<VectorXYZ> bladeBack = asList(bladeFront.get(0), bladeFront.get(2), bladeFront.get(1));

				// rotate and draw blades
				double rotCenterY = params.position().y;
				double rotCenterZ = params.position().z;

				bladeFront = rotateShapeX(bladeFront, 60, rotCenterY, rotCenterZ);
				bladeBack  = rotateShapeX(bladeBack, 60, rotCenterY, rotCenterZ);
				target.drawTriangleStrip(bladeMaterial, bladeFront,
						texCoordLists(bladeFront, bladeMaterial, GLOBAL_Z_Y));
				target.drawTriangleStrip(bladeMaterial, bladeBack,
						texCoordLists(bladeBack, bladeMaterial, GLOBAL_Z_Y));
				bladeFront = rotateShapeX(bladeFront, 120, rotCenterY, rotCenterZ);
				bladeBack  = rotateShapeX(bladeBack, 120, rotCenterY, rotCenterZ);
				target.drawTriangleStrip(bladeMaterial, bladeFront,
						texCoordLists(bladeFront, bladeMaterial, GLOBAL_Z_Y));
				target.drawTriangleStrip(bladeMaterial, bladeBack,
						texCoordLists(bladeBack, bladeMaterial, GLOBAL_Z_Y));
				bladeFront = rotateShapeX(bladeFront, 120, rotCenterY, rotCenterZ);
				bladeBack  = rotateShapeX(bladeBack, 120, rotCenterY, rotCenterZ);
				target.drawTriangleStrip(bladeMaterial, bladeFront,
						texCoordLists(bladeFront, bladeMaterial, GLOBAL_Z_Y));
				target.drawTriangleStrip(bladeMaterial, bladeBack,
						texCoordLists(bladeBack, bladeMaterial, GLOBAL_Z_Y));

			}

		};

		private final AttachmentConnector connector;

		public WindTurbine(MapNode node) {

			super(node);

			if (node.getTags().contains("location", "roof") || node.getTags().contains("location", "rooftop")) {
				connector = new AttachmentConnector(singletonList("roof"), node.getPos().xyz(0), this, 0, false);
			} else {
				connector = null;
			}

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			double poleHeight = parseHeight(node.getTags(),
					parseMeasure(node.getTags().getValue("height:hub"), 100.0));
			double poleRadiusBottom = parseWidth(node.getTags(), poleHeight / 20) / 2;
			double poleRadiusTop = poleRadiusBottom / 2;
			double nacelleHeight = poleHeight * 0.05;
			double nacelleDepth = poleHeight * 0.1;
			double rotorDiameter = parseMeasure(node.getTags().getValue("rotor:diameter"), poleHeight);


			/* determine material */

			Material poleMaterial = null;
			Material nacelleMaterial = Materials.STEEL;

			//TODO parse color

			if (poleMaterial == null) {
				poleMaterial = Materials.getSurfaceMaterial(
						node.getTags().getValue("material"));
			}

			if (poleMaterial == null) {
				poleMaterial = Materials.getSurfaceMaterial(
						node.getTags().getValue("surface"), Materials.STEEL);
			}

			/* determine position with elevation */
			VectorXYZ position = getBase();
			if (connector != null && connector.isAttached()) {
				position = position.xz().xyz(connector.getAttachedPos().getY());
			}

			/* draw pole */
			target.drawColumn(poleMaterial, null,
					position,
					poleHeight,
					poleRadiusBottom, poleRadiusTop, false, false);

			/* draw nacelle */
			VectorXZ nacelleVector = VectorXZ.X_UNIT;
			target.drawBox(nacelleMaterial,
					position.addY(poleHeight).add(nacelleDepth/2 - poleRadiusTop*2, 0f, 0f),
					nacelleVector, nacelleHeight, nacelleHeight, nacelleDepth);

			/* draw rotor blades */
			target.addSubModel(new ModelInstance(ROTOR, new InstanceParameters(
					position.addY(poleHeight).add(-poleRadiusTop*2.5, nacelleHeight/2, 0),
					0, rotorDiameter)));

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

	private static class PowerMinorLine extends NoOutlineWaySegmentWorldObject {

		private static final float DEFAULT_THICKN = 0.05f; // width and height
		private static final float DEFAULT_CLEARING_BL = 7.5f; // power pole height is 8

		public PowerMinorLine(MapWaySegment segment) {
			super(segment);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public List<Mesh> buildMeshes() {

			ShapeXZ powerlineShape = new PolylineXZ(
				new VectorXZ(-DEFAULT_THICKN/2, DEFAULT_CLEARING_BL),
				new VectorXZ(-DEFAULT_THICKN/2, DEFAULT_CLEARING_BL + DEFAULT_THICKN),
				new VectorXZ(+DEFAULT_THICKN/2, DEFAULT_CLEARING_BL + DEFAULT_THICKN),
				new VectorXZ(+DEFAULT_THICKN/2, DEFAULT_CLEARING_BL)
			);

			List<VectorXYZ> path = getBaseline();

			return singletonList(new Mesh(new ExtrusionGeometry(powerlineShape, getBaseline(),
					nCopies(path.size(), Y_UNIT), null, BLACK, null, PLASTIC.getTextureDimensions()),
					PLASTIC, LevelOfDetail.LOD3, LevelOfDetail.LOD4));

		}

	}

	private final static class PowerLine extends NoOutlineWaySegmentWorldObject {

		private static final float CABLE_THICKNESS = 0.05f;
		private static final Material CABLE_MATERIAL = PLASTIC;
		private static final double SLACK_SPAN = 6;
		private static final Map<LODRange, Double> INTERPOLATION_STEPS = Map.of(
				new LODRange(LevelOfDetail.LOD3), 3.0,
				new LODRange(LevelOfDetail.LOD4), 10.0);
		private static final ShapeXZ powerlineShape = new CircleXZ(NULL_VECTOR, CABLE_THICKNESS/2);

		private int cables = -1;
		private int voltage = -1;
		private TowerConfig start;
		private TowerConfig end;
		private List<VectorXYZ> startPos = null;
		private List<VectorXYZ> endPos = null;


		public PowerLine(MapWaySegment line) {
			super(line);
		}

		private void addPos(VectorXYZ baseStart, VectorXYZ baseEnd, double gotoRight, double up) {
			startPos.add(baseStart.add(start.direction.rightNormal().mult(gotoRight)).add(0, up, 0));
			endPos.add(baseEnd.add(end.direction.rightNormal().mult(gotoRight)).add(0, up, 0));
		}

		private void addPos(VectorXYZ baseStart, VectorXYZ baseEnd, double gotoRight, double upStart, double upEnd) {
			startPos.add(baseStart.add(start.direction.rightNormal().mult(gotoRight)).add(0, upStart, 0));
			endPos.add(baseEnd.add(end.direction.rightNormal().mult(gotoRight)).add(0, upEnd, 0));
		}

		private void setup() {

			startPos = new ArrayList<VectorXYZ>();
			endPos = new ArrayList<VectorXYZ>();

			// check number of power lines
			try {
				cables = Integer.valueOf(segment.getTags().getValue("cables"));
			} catch (NumberFormatException e) {}

			// check voltage
			try {
				voltage = Integer.valueOf(segment.getTags().getValue("voltage"));
			} catch (NumberFormatException e) {}

			if (cables <= 0) {
				return;
			}

			// get the tower configurations for start and end tower
			start = generateTowerConfig(segment.getStartNode());
			end = generateTowerConfig(segment.getEndNode());

			if (!start.isHighVoltagePowerTower() && !end.isHighVoltagePowerTower()) {

				// normal PowerTower...

				double startHeight = parseHeight(start.pos.getTags(), 14) + 0.25;
				double endHeight = parseHeight(end.pos.getTags(), 14) + 0.25;

				VectorXYZ baseStart = getStartXYZ().addY(startHeight - 0.5);
				VectorXYZ baseEnd = getEndXYZ().addY(endHeight - 0.5);

				// power lines at the top left and right
				addPos(baseStart, baseEnd, 2, 0.5);
				addPos(baseStart, baseEnd, -2, 0.5);

				if (cables >= 3) {
					// additional power line at the top center
					addPos(baseStart, baseEnd, 0, 0.5);
				}
				if (cables >= 5) {
					// further power lines at the left and right below the column
					addPos(baseStart, baseEnd, 1.5, -0.5);
					addPos(baseStart, baseEnd, -1.5, -0.5);
				}
			} else {

				// High voltage PowerTower ...

				float default_height = voltage > 150000 ? 40 : 30;
				float pole_width = voltage > 150000 ? 16 : 13;

				double startHeight = parseHeight(start.pos.getTags(), default_height);
				double endHeight = parseHeight(end.pos.getTags(), default_height);

				double heightS = 2.5 * (((int) (startHeight / 2.5)) / 5);
				double heightE = 2.5 * (((int) (endHeight / 2.5)) / 5);

				VectorXYZ baseStart = getStartXYZ().addY(-0.5);
				VectorXYZ baseEnd = getEndXYZ().addY(-0.5);

				// power line at the tower's top
				addPos(baseStart, baseEnd, 0, 5*heightS, 5*heightE);

				// power lines start a little bit below the tower's columns
				baseStart = baseStart.add(0, -0.2, 0);
				baseEnd = baseEnd.add(0, -0.2, 0);

				// power lines at the base column
				addPos(baseStart, baseEnd, 0.9*pole_width, startHeight/2, endHeight/2);
				addPos(baseStart, baseEnd, -0.9*pole_width, startHeight/2, endHeight/2);
				if ((cables > 3) && (cables <= 9)) {
					addPos(baseStart, baseEnd, 0.45*pole_width, startHeight/2, endHeight/2);
					addPos(baseStart, baseEnd, -0.45*pole_width, startHeight/2, endHeight/2);
				} else if (cables > 9) {
					addPos(baseStart, baseEnd, 0.6*pole_width, startHeight/2, endHeight/2);
					addPos(baseStart, baseEnd, -0.6*pole_width, startHeight/2, endHeight/2);
					addPos(baseStart, baseEnd, 0.3*pole_width, startHeight/2, endHeight/2);
					addPos(baseStart, baseEnd, -0.3*pole_width, startHeight/2, endHeight/2);
				}

				// additional power lines at the upper column
				if (cables >= 7) {
					addPos(baseStart, baseEnd, 0.9*0.6*pole_width, 4*heightS, 4*heightE);
					addPos(baseStart, baseEnd, -0.9*0.6*pole_width, 4*heightS, 4*heightE);
					if (cables >= 9) {
						addPos(baseStart, baseEnd, 0.45*0.6*pole_width, 4*heightS, 4*heightE);
						addPos(baseStart, baseEnd, -0.45*0.6*pole_width, 4*heightS, 4*heightE);
					}
				}
			}
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ABOVE;
		}

		@Override
		public List<Mesh> buildMeshes() {

			List<Mesh> result = new ArrayList<>();

			for (LODRange lodRange : INTERPOLATION_STEPS.keySet()) {

				double interpolationSteps = INTERPOLATION_STEPS.get(lodRange);

				// do initial setup for height and position calculation, if necessary
				if (startPos == null) {
					setup();
				}

				for (int i = 0; i < startPos.size(); i++) {

					VectorXYZ start = startPos.get(i);
					VectorXYZ end = endPos.get(i);

					double lenToEnd = end.distanceToXZ(start);
					double heightDiff = end.y - start.y;

					double stepSize = lenToEnd / interpolationSteps;
					VectorXZ dir = end.xz().subtract(start.xz()).normalize();

					List<VectorXYZ> path = new ArrayList<VectorXYZ>();
					for (int x = 0; x <= interpolationSteps; x++) {
						double ratio = x / interpolationSteps;

						// distance from start to position x
						double dx = stepSize * x;

						// use a simple parabola between two towers
						double height = (1 - Math.pow(2.0 * (ratio - 0.5), 2)) * -SLACK_SPAN;
						// add a linear function to account for different tower/terrain heights
						height += ratio * heightDiff;

						path.add(start.add(dir.mult(dx)).add(0, height, 0));
					}

					result.add(new Mesh(new ExtrusionGeometry(powerlineShape, path, nCopies(path.size(), Y_UNIT),
							null, BLACK, null, CABLE_MATERIAL.getTextureDimensions()),
							CABLE_MATERIAL, lodRange.min(), lodRange.max()));

				}

			}

			return result;

		}

	}


	private static final class PowerTower extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		private TowerConfig config;

		public PowerTower(MapNode node, TowerConfig config) {
			super(node);
			this.config = config;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		// TODO we're missing the ceramics to hold the power lines

		@Override
		public void buildMeshesAndModels(Target target) {

			VectorXYZ base = getBase().addY(-0.5);
			double height = parseHeight(node.getTags(), 14);

			Material material = Materials.getSurfaceMaterial(node.getTags().getValue("material"));
			if (material == null) {
				material = Materials.getSurfaceMaterial(node.getTags().getValue("surface"), Materials.STEEL);
			}

			// draw base column
			target.drawColumn(material, null, base, height, 0.5, 0.25, true, true);

			// draw cross "column"
			target.drawBox(material, base.add(0, height, 0), config.direction, 0.25, 5, 0.25);

			// draw pieces holding the power lines
			base = base.add(0, height + 0.25, 0);
			target.drawColumn(Materials.CONCRETE, null, base.add(config.direction.rightNormal().mult(2)), 0.5, 0.1, 0.1, true, true);
			target.drawColumn(Materials.CONCRETE, null, base.add(config.direction.rightNormal().mult(-2)), 0.5, 0.1, 0.1, true, true);
			if (config.cables >= 3) {
				target.drawColumn(Materials.CONCRETE, null, base, 0.5, 0.1, 0.1, true, true);
			}
			if (config.cables >= 5) {
				target.drawColumn(Materials.CONCRETE, null, base.add(config.direction.rightNormal().mult(1.5)), -0.5, 0.1, 0.1, true, true);
				target.drawColumn(Materials.CONCRETE, null, base.add(config.direction.rightNormal().mult(-1.5)), -0.5, 0.1, 0.1, true, true);
			}
		}
	}


	private static final class HighVoltagePowerTower extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		private TowerConfig config;
		private VectorXZ direction;

		public HighVoltagePowerTower(MapNode node, TowerConfig config) {
			super(node);
			this.config = config;
			this.direction = config.direction;
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		/**
		 * parse height tag or provide a reasonable default based on the tower's voltage
		 */
		private double getTowerHeight() {

			float default_height = config.voltage > 150000 ? 40 : 30;
			double height = parseHeight(node.getTags(), default_height);

			return height;
		}

		private VectorXZ[] getCorners(VectorXZ center, double diameter) {
			double half = diameter/2;
			VectorXZ ortho = direction.rightNormal();

			VectorXZ right = center.add(direction.mult(half));
			VectorXZ left = center.add(direction.mult(-half));

			return new VectorXZ[] {
					right.add(ortho.mult(-half)),
					right.add(ortho.mult(half)),
					left.add(ortho.mult(half)),
					left.add(ortho.mult(-half))
			};
		}

		private void drawSegment(Target target,
				VectorXZ[] low, VectorXZ[] high, double base, double height) {

			for (int a = 0; a < 4; a++) {

				List<VectorXYZ> vs = new ArrayList<>();
				List<VectorXZ> tex = new ArrayList<>();
				List<List<VectorXZ>> texList =
					nCopies(Materials.POWER_TOWER_VERTICAL.getNumTextureLayers(), tex);

				vs.add(high[a].xyz(height));
				tex.add(new VectorXZ(0, 1));

				vs.add(low[a].xyz(base));
				tex.add(new VectorXZ(0, 0));

				if (high[a].distanceTo(high[(a + 1) % 4]) > 0.001) {
					vs.add(high[(a + 1) % 4].xyz(height));
					tex.add(new VectorXZ(1, 1));
				}

				vs.add(low[(a + 1) % 4].xyz(base));
				tex.add(new VectorXZ(1, 0));

				target.drawTriangleStrip(Materials.POWER_TOWER_VERTICAL, vs, texList);
			}
		}

		private void drawHorizontalSegment(Target target,
				VectorXZ left, VectorXZ right, double base,
				double left_height, double right_height) {

			List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
			List<VectorXZ> tex = new ArrayList<VectorXZ>();
			List<List<VectorXZ>> texList =
					nCopies(Materials.POWER_TOWER_HORIZONTAL.getNumTextureLayers(), tex);

			vs.add(right.xyz(base));
			vs.add(left.xyz(base));
			vs.add(right.xyz(base+right_height));
			vs.add(left.xyz(base+left_height));

			tex.add(new VectorXZ(1, 1));
			tex.add(new VectorXZ(0, 1));
			tex.add(new VectorXZ(1, 0));
			tex.add(new VectorXZ(0, 0));

			target.drawTriangleStrip(Materials.POWER_TOWER_HORIZONTAL, vs, texList);
		}

		private void drawHorizontalTop(Target target, VectorXZ[] frontPoints, VectorXZ[] backPoints,
				double base, double border, double middle, double center) {

			double[] height = new double[]{border, middle, center, center, middle, border};

			int len = height.length;
			for (int a = 0; a < len-1; a++) {

				List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
				List<VectorXZ> tex = new ArrayList<VectorXZ>();
				List<List<VectorXZ>> texList =
						nCopies(Materials.POWER_TOWER_VERTICAL.getNumTextureLayers(), tex);

				for (int i = 0; i < 2; i++) {
					vs.add(frontPoints[a+i].xyz(base + height[a+i]));
					vs.add(backPoints[a+i].xyz(base + height[a+i]));
					tex.add(new VectorXZ(0, i));
					tex.add(new VectorXZ(1, i));
				}
				target.drawTriangleStrip(Materials.POWER_TOWER_VERTICAL, vs, texList);
			}
		}


		private double drawPart(Target target, double elevation,
				int nr_segments, double segment_height, double ground_size,
				double top_size) {

			for (int i = 0; i < nr_segments; i++) {

				double bottom = ground_size + i * (top_size - ground_size) / nr_segments;
				double top = ground_size + (i + 1) * (top_size - ground_size) / nr_segments;

				VectorXZ[] low = getCorners(node.getPos(), bottom);
				VectorXZ[] high = getCorners(node.getPos(), top);

				drawSegment(target, low, high, elevation, elevation + segment_height);

				elevation += segment_height;
			}

			return elevation;
		}

		private VectorXZ[] getPoleCoordinates(VectorXZ base,
				VectorXZ direction, double width, double size) {

			return new VectorXZ[] {
					base.add(direction.mult(-width - size)),
					base.add(direction.mult(-width / 2 - size)),
					base.add(direction.mult(-size)),
					base.add(direction.mult(size)),
					base.add(direction.mult(width / 2 + size)),
					base.add(direction.mult(width + size))
			};
		}

		private void drawHorizontalPole(Target target, double elevation,
				double diameter, double width) {

			double half = diameter / 2;
			VectorXZ ortho = direction.rightNormal().invert();

			VectorXZ[] frontPoints = getPoleCoordinates(node.getPos().add(direction.mult(half)), ortho, width, half);
			VectorXZ[] backPoints = getPoleCoordinates(node.getPos().add(direction.mult(-half)), ortho, width, half);

			for (VectorXZ[] points : asList(frontPoints, backPoints)) {
				drawHorizontalSegment(target, points[0], points[1], elevation, 0.1, diameter/2);
				drawHorizontalSegment(target, points[1], points[2], elevation, diameter/2, diameter);
				drawHorizontalSegment(target, points[2], points[3], elevation, diameter, diameter);
				drawHorizontalSegment(target, points[3], points[4], elevation, diameter, diameter/2);
				drawHorizontalSegment(target, points[4], points[5], elevation, diameter/2, 0.1);
			}

			drawHorizontalTop(target, frontPoints, backPoints, elevation, 0.1, diameter/2, diameter);
		}

		// TODO we're missing the ceramics to hold the power lines

		@Override
		public void buildMeshesAndModels(Target target) {

			float pole_width = config.voltage > 150000 ? 16 : 13;
			float[] tower_width = config.voltage > 150000 ? new float[]{11,6,4f,0} : new float[]{8,5,3,0};
			double height = getTowerHeight();

			double segment_height = 2.5;
			double base = getBase().y - 0.5;

			int parts = (int) (height / segment_height);
			int low_parts = parts / 5;

			// draw the tower itself
			double ele = drawPart(target, base, low_parts, segment_height, tower_width[0], tower_width[1]);
			ele = drawPart(target, ele, 3 * low_parts, segment_height, tower_width[1], tower_width[2]);
			drawPart(target, ele, low_parts, segment_height, tower_width[2], tower_width[3]);

			// draw the vertical poles
			drawHorizontalPole(target, base + height/2, 0.7*tower_width[1], pole_width);
			if (config.cables > 6) {
				drawHorizontalPole(target, ele, 0.55*tower_width[2], 0.6*pole_width);
			}
		}
	}

	private static final class PhotovoltaicPlant extends AbstractAreaWorldObject implements ProceduralWorldObject {

		/** compares vectors by x coordinate */
		private static final Comparator<VectorXZ> X_COMPARATOR = comparingDouble(v -> v.x);

		private final VectorXYZ panelUpVector;

		/** locations of the rows of panels */
		private final List<PolylineXZ> panelRows = new ArrayList<>();

		/** connectors for every point in of the {@link #panelRows} */
		private final EleConnectorGroup eleConnectors = new EleConnectorGroup();

		protected PhotovoltaicPlant(MapArea area) {

			super(area);

			/* construct panel geometry */

			double panelAngle = PI / 4;
			double panelHeight = 5;

			panelUpVector = Z_UNIT.mult(panelHeight).rotateX(-panelAngle);

			/* place and draw rows of panels */

			AxisAlignedRectangleXZ box = this.boundingBox();

			List<PolygonShapeXZ> obstacles = getGroundObstacles();

			double posZ = box.minZ;

			while (posZ + panelUpVector.z < box.maxZ) {

				LineSegmentXZ rowLine = new LineSegmentXZ(
						 new VectorXZ(box.minX - 10, posZ),
						 new VectorXZ(box.maxX + 10, posZ));

				// calculate start and end points (maybe more than one each)

				List<VectorXZ> intersections =
						area.getPolygon().intersectionPositions(rowLine);

				assert intersections.size() % 2 == 0;

				intersections.sort(X_COMPARATOR);

				// add more start/end points at ground-level obstacles

				for (PolygonShapeXZ obstacle : obstacles) {

					List<VectorXZ> obstacleIntersections =
							obstacle.intersectionPositions(rowLine);

					for (int i = 0; i + 1 < obstacleIntersections.size(); i += 2) {

						int insertionIndexA = -binarySearch(intersections,
								obstacleIntersections.get(i), X_COMPARATOR) - 1;
						int insertionIndexB = -binarySearch(intersections,
								obstacleIntersections.get(i + 1), X_COMPARATOR) - 1;

						obstacleIntersections.sort(X_COMPARATOR);

						if (insertionIndexA == insertionIndexB
								&& insertionIndexA >= 0
								&& insertionIndexA % 2 == 1) {

							intersections.add(insertionIndexA,
									obstacleIntersections.get(i+1));
							intersections.add(insertionIndexA,
									obstacleIntersections.get(i));

						}

					}

				}

				// define a row of panels between each start/end pair

				assert intersections.size() % 2 == 0;

				for (int i = 0; i + 1 < intersections.size(); i += 2) {
					VectorXZ start = intersections.get(i);
					VectorXZ end = intersections.get(i + 1);
					PolylineXZ polyline = new PolylineXZ(equallyDistributePointsAlong(20, true,start, end));
					panelRows.add(polyline);
					eleConnectors.addConnectorsFor(polyline.vertices(), this, ON);
				}

				posZ += panelUpVector.z * 1.5;

			}

		}

		@Override
		public void buildMeshesAndModels(Target target) {

			for (PolylineXZ panelRow : panelRows) {

				for (int i = 0; i + 1 < panelRow.vertices().size(); i++)

				renderPanelsTo(target,
						eleConnectors.getPosXYZ(panelRow.vertices().get(i)),
						eleConnectors.getPosXYZ(panelRow.vertices().get(i+1)),
						panelUpVector);

			}

		}

		@Override
		public EleConnectorGroup getEleConnectors() {
			return eleConnectors;
		}

		/**
		 * returns outlines from ground objects overlapping this area
		 */
		private List<PolygonShapeXZ> getGroundObstacles() {

			List<PolygonShapeXZ> obstacles = new ArrayList<>();

			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
				FaultTolerantIterationUtil.forEach(overlap.getOther(area).getRepresentations(), otherWO -> {

					if (otherWO.getGroundState() == GroundState.ON
							&& otherWO.getOutlinePolygonXZ() != null) {
						obstacles.add(otherWO.getOutlinePolygonXZ());
					}

				});
			}

			return obstacles;

		}

		private void renderPanelsTo(Target target, VectorXYZ bottomLeft,
				VectorXYZ bottomRight, VectorXYZ upVector) {

			/* draw front */

			List<VectorXYZ> vs = asList(
					bottomLeft.add(upVector),
					bottomLeft,
					bottomRight.add(upVector),
					bottomRight);

			target.drawTriangleStrip(Materials.SOLAR_PANEL, vs,
					texCoordLists(vs, Materials.SOLAR_PANEL, STRIP_FIT_HEIGHT));

			/* draw back */

			vs = asList(vs.get(2), vs.get(3), vs.get(0), vs.get(1));

			Material backMaterial = PLASTIC.withColor(new Color(184, 184, 184));
			target.drawTriangleStrip(backMaterial, vs,
					texCoordLists(vs, backMaterial, STRIP_WALL));

		}

	}

	static final class RooftopSolarPanels extends AbstractAreaWorldObject implements ProceduralWorldObject {

		private static final double DISTANCE_FROM_ROOF = 0.05;

		private final AttachmentConnector connector;

		public RooftopSolarPanels(MapArea area) {

			super(area);

			List<String> compatibleSurfaceTypes = asList("roof");
			if (area.getTags().containsKey("level")) {
				List<Integer> levels = ValueParseUtil.parseLevels(area.getTags().getValue("level"));
				if (levels != null) {
					compatibleSurfaceTypes = asList("roof" + levels.get(0), "roof");
				}
			}

			VectorXYZ attachmentPoint = area.getOuterPolygon().getPointInside().xyz(0);
			connector = new AttachmentConnector(compatibleSurfaceTypes, attachmentPoint, this, 0, false);

		}

		@Override
		public Iterable<AttachmentConnector> getAttachmentConnectors() {
			return singletonList(connector);
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			if (connector.isAttached()) {

				List<TriangleXZ> trianglesXZ = getTriangulationXZ();

				TriangleXYZ plane = planeFromPosAndNormal(
						connector.getAttachedPos().addY(DISTANCE_FROM_ROOF),
						connector.getAttachedSurfaceNormal());

				List<TriangleXYZ> triangles = trianglesXZ.stream()
						.map(it -> it.xyz(v -> pointToPlane(plane, v)))
						.collect(toList());

				Function<TextureDataDimensions, PanelTexCoordFunction> texCoordFunctionGenerator =
						placePanelTextures(area.getPolygon(), plane.getNormal());

				target.drawTriangles(SOLAR_PANEL, triangles,
						triangleTexCoordLists(triangles, SOLAR_PANEL, texCoordFunctionGenerator));

			}

		}

		private static final TriangleXYZ planeFromPosAndNormal(VectorXYZ pos, VectorXYZ normal) {
			if (normal.y < 0.001) {
				throw new IllegalArgumentException("does not work for vertical planes");
			}
			VectorXYZ cross1 = normal.crossNormalized(X_UNIT);
			VectorXYZ cross2 = normal.crossNormalized(Z_UNIT);
			return new TriangleXYZ(pos, pos.add(cross1), pos.add(cross2));
		}

		private static final VectorXYZ pointToPlane(TriangleXYZ plane, VectorXZ v) {
			return v.xyz(plane.getYAt(v));
		}

		/** attempts to place textures so that all points lie roughly on integer multiples of a panel's dimensions */
		private static final Function<TextureDataDimensions, PanelTexCoordFunction> placePanelTextures(
				PolygonShapeXZ polygon, VectorXYZ roofNormal) {

			SimplePolygonShapeXZ outerPolygon = polygon.getOuter();

			/* fit a rectangle around the panels */

			SimplePolygonXZ bbox = outerPolygon.minimumRotatedBoundingBox();

			VectorXZ downDirection;
			if (roofNormal.xz().lengthSquared() > 0.01) {
				downDirection = roofNormal.xz().normalize();
			} else {
				// roof normal is almost vertical, e.g. with flat roofs. Try to have panels face south.
				downDirection = new VectorXZ(0, -1);
			}

			/* determine the rotation of panels on the rectangle */

			LineSegmentXZ segment = bbox.reverse().getSegments().stream()
					.min(Comparator.comparingDouble(s -> angleBetween(s.getDirection(), downDirection))).get();

			VectorXZ origin;
			Angle zDirection;
			origin = segment.p1;
			zDirection = Angle.ofRadians(segment.getDirection().angle());

			/* determine width and height of the rectangle around the panels */

			AxisAlignedRectangleXZ xzBbox = bbox.rotatedCW(-zDirection.radians).boundingBox();

			double totalWidth = xzBbox.sizeX();
			double totalHeight = xzBbox.sizeZ();

			/* determine the number of panels to place */

			//TODO possibly run this twice; with all segments, and (as fallback) with segments of simplified polygon

			PolygonShapeXZ rotatedPolygon = polygon.rotatedCW(-zDirection.radians);

			List<Double> horizontalSegmentLengths = new ArrayList<>();
			List<Double> verticalSegmentLengths = new ArrayList<>();

			List<LineSegmentXZ> segments = rotatedPolygon.getRings().stream()
					.flatMap(r -> r.getSegments().stream())
					.collect(toList());

			for (LineSegmentXZ s : segments) {

				if (angleBetween(VectorXZ.X_UNIT, s.getDirection()) < PI / 18
						|| angleBetween(VectorXZ.X_UNIT.invert(), s.getDirection()) < PI / 18) {
					horizontalSegmentLengths.add(s.getLength());
				} else if (angleBetween(VectorXZ.Z_UNIT, s.getDirection()) < PI / 18
						|| angleBetween(VectorXZ.Z_UNIT.invert(), s.getDirection()) < PI / 18) {
					verticalSegmentLengths.add(s.getLength());
				}

			}

			// consider that the panel height in the XZ plane is smaller than the real panel height for small windows
			double heightFactor = cos(roofNormal.angleTo(Y_UNIT));

			@Nullable Integer panelsX = roughCommonDivisor(totalWidth, horizontalSegmentLengths, 1.0);
			@Nullable Integer panelsZ = roughCommonDivisor(totalHeight, verticalSegmentLengths, 1.66 * heightFactor);

			/* construct the result */

			return t -> new PanelTexCoordFunction(origin, zDirection, totalWidth, totalHeight, panelsX, panelsZ, t);

		}

		/**
		 * tries to find a length somewhat close to preferredLength
		 * that fits an integer number of times into the total length,
		 * and also fits an integer number of times into as many segment lengths as possible.
		 *
		 * @return  the number of times the resulting length fits into the total, or null if no good solution was found
		 */
		static Integer roughCommonDivisor(double total, List<Double> lengths, double preferredLength) {

			/* determine some lengths which are somewhat close to preferredLength and fit the total */

			Set<Double> candidateLengths = new HashSet<>();
			Map<Double, Double> maxDeviations = new HashMap<>();

			int iMin = max(1, (int)floor(total / (2.0 * preferredLength)));
			int iMax = max(1, (int)ceil(total / (0.5 * preferredLength)));

			for (int i = iMin; i <= iMax; i++) {
				double candidateLength = total / i;
				candidateLengths.add(candidateLength);
				maxDeviations.put(candidateLength, 0.0);
			}

			/* remove any candidate lengths which do not fit in one of the segment lengths */

			for (double segmentLength : lengths) {
				for (double candidate : new ArrayList<>(candidateLengths)) {
					// check if it's "almost" an integer multiple
					double deviation = abs((segmentLength / candidate) - round(segmentLength / candidate));
					if (deviation > 0.2) {
						candidateLengths.remove(candidate);
						maxDeviations.remove(candidate);
					} else if (deviation > maxDeviations.get(candidate)) {
						maxDeviations.put(candidate, deviation);
					}
				}

			}

			/* among the results which get close to the smallest deviation,
			 * select the one most similar to preferredLength */

			if (candidateLengths.isEmpty()) return null;

			double minDeviation = min(maxDeviations.values());

			double selectedLength = candidateLengths.stream()
					.filter(l -> maxDeviations.get(l) - minDeviation < 0.05)
					.min(Comparator.comparingDouble(l -> abs(preferredLength - l)))
					.get();

			return (int)round(total / selectedLength);

		}

		static class PanelTexCoordFunction implements TexCoordFunction {

			public final VectorXZ origin;
			public final Angle zDirection;
			public final double totalWidth;
			public final double totalHeight;
			public final @Nullable Integer panelsX;
			public final @Nullable Integer panelsZ;
			public final TextureDataDimensions texDim;

			public PanelTexCoordFunction(VectorXZ origin, Angle zDirection, double totalWidth, double totalHeight,
					@Nullable Integer panelsX, @Nullable Integer panelsZ, TextureDataDimensions textureData) {
				this.origin = origin;
				this.zDirection = zDirection;
				this.totalWidth = totalWidth;
				this.totalHeight = totalHeight;
				this.panelsX = panelsX;
				this.panelsZ = panelsZ;
				this.texDim = textureData;
			}

			@Override
			public List<VectorXZ> apply(List<VectorXYZ> vs) {
				return vs.stream().map(v -> apply(v)).collect(toList());
			}

			VectorXZ apply(VectorXYZ v) {

				double defaultEntityWidth = texDim.widthPerEntity() != null ? texDim.widthPerEntity() : texDim.width();
				double defaultEntityHeight = texDim.heightPerEntity() != null ? texDim.heightPerEntity() : texDim.height();

				double wFactor = defaultEntityWidth / texDim.width();
				double hFactor = defaultEntityHeight / texDim.height();

				double panelWidth, panelHeight;

				if (panelsX != null && panelsZ != null) {
					panelWidth = totalWidth / panelsX;
					panelHeight = totalHeight / panelsZ;
				} else {
					int panelsX = max(1, (int)round(totalWidth / defaultEntityWidth));
					int panelsZ = max(1, (int)round(totalHeight / defaultEntityHeight));
					panelWidth = totalWidth / panelsX;
					panelHeight = totalHeight / panelsZ;
				}

				VectorXZ metricTexCoord = v.xz().subtract(origin);
				metricTexCoord = metricTexCoord.rotate(-zDirection.radians);

				return new VectorXZ(
						wFactor * metricTexCoord.x / panelWidth,
						hFactor * metricTexCoord.z / panelHeight);

			}

		}

	}

}

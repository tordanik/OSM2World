package org.osm2world.core.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.Comparator.comparingDouble;
import static org.openstreetmap.josm.plugins.graphview.core.util.ValueStringParser.parseMeasure;
import static org.osm2world.core.math.VectorXYZ.*;
import static org.osm2world.core.math.VectorXZ.NULL_VECTOR;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.STRIP_WALL;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.rotateShapeX;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.shapes.CircleXZ;
import org.osm2world.core.math.shapes.PolylineXZ;
import org.osm2world.core.math.shapes.ShapeXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.frontend_pbf.ModelTarget;
import org.osm2world.core.target.frontend_pbf.RenderableToModelTarget;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.data.NoOutlineWaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.data.WorldObjectWithOutline;
import org.osm2world.core.world.modules.common.AbstractModule;

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

		VectorXZ dir = VectorXZ.NULL_VECTOR;
		int cables = -1;
		int voltage = -1;
		for (MapWaySegment powerLine : powerLines) {
			dir = dir.add(powerLine.getDirection());

			try {
				cables = Integer.valueOf(powerLine.getTags().getValue("cables"));
			} catch (NumberFormatException e) {}
			try {
				voltage = Integer.valueOf(powerLine.getTags().getValue("voltage"));
			} catch (NumberFormatException e) {}
		}
		dir = dir.mult(1.0/powerLines.size());

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
				&& area.getTags().contains("generator:method", "photovoltaic")) {
			area.addRepresentation(new PhotovoltaicPlant(area));
		}
	}

	private static final class PowerCabinet extends NoOutlineNodeWorldObject
	implements RenderableToAllTargets {

		public PowerCabinet(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target<?> target) {

			double directionAngle = parseDirection(node.getTags(), PI);
			VectorXZ faceVector = VectorXZ.fromAngle(directionAngle);

			target.drawBox(PLASTIC_GREY, getBase(),
					faceVector, 1.5, 0.8, 0.3);

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

	private static final class Powerpole extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {

		public Powerpole(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target<?> target) {

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

	public static final class WindTurbine extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets, RenderableToModelTarget {

		/** model of a rotor with 1 m rotor diameter */
		public static final Model ROTOR = new Model() {

			@Override
			public void render(Target<?> target, VectorXYZ position,
					double direction, Double height, Double width, Double length) {

				double bladeLength = (height == 0 ? 1 : height) / 2;
				double bladeWidth = 0.1 * bladeLength;

				Material bladeMaterial = Materials.STEEL; // probably fibre, but color matches roughly :)

				// define first blade
				List<VectorXYZ> bladeFront = asList(
						position.add(0, 0, +bladeWidth/2),
						position.add(0, -bladeLength, 0),
						position.add(0, 0, -bladeWidth/2)
				);

				List<VectorXYZ> bladeBack = asList(bladeFront.get(0), bladeFront.get(2), bladeFront.get(1));

				// rotate and draw blades
				double rotCenterY = position.y;
				double rotCenterZ = position.z;

				bladeFront = rotateShapeX(bladeFront, 60, rotCenterY, rotCenterZ);
				bladeBack  = rotateShapeX(bladeBack, 60, rotCenterY, rotCenterZ);
				target.drawTriangleStrip(bladeMaterial, bladeFront, null);
				target.drawTriangleStrip(bladeMaterial, bladeBack, null);
				bladeFront = rotateShapeX(bladeFront, 120, rotCenterY, rotCenterZ);
				bladeBack  = rotateShapeX(bladeBack, 120, rotCenterY, rotCenterZ);
				target.drawTriangleStrip(bladeMaterial, bladeFront, null);
				target.drawTriangleStrip(bladeMaterial, bladeBack, null);
				bladeFront = rotateShapeX(bladeFront, 120, rotCenterY, rotCenterZ);
				bladeBack  = rotateShapeX(bladeBack, 120, rotCenterY, rotCenterZ);
				target.drawTriangleStrip(bladeMaterial, bladeFront, null);
				target.drawTriangleStrip(bladeMaterial, bladeBack, null);

			}

		};

		public WindTurbine(MapNode node) {
			super(node);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target<?> target) {

			float poleHeight = parseHeight(node.getTags(), 100f);
			float poleRadiusBottom = parseWidth(node.getTags(), 5) / 2;
			float poleRadiusTop = poleRadiusBottom / 2;
			float nacelleHeight = poleHeight * 0.05f;
			float nacelleDepth = poleHeight * 0.1f;
			double rotorDiameter = poleHeight;

			if (node.getTags().containsKey("rotor:diameter")
					&& parseMeasure(node.getTags().getValue("rotor:diameter")) != null) {
				rotorDiameter = parseMeasure(node.getTags().getValue("rotor:diameter"));
			}

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

			/* draw pole */
			target.drawColumn(poleMaterial, null,
					getBase(),
					poleHeight,
					poleRadiusBottom, poleRadiusTop, false, false);

			/* draw nacelle */
			VectorXZ nacelleVector = VectorXZ.X_UNIT;
			target.drawBox(nacelleMaterial,
					getBase().addY(poleHeight).add(nacelleDepth/2 - poleRadiusTop*2, 0f, 0f),
					nacelleVector, nacelleHeight, nacelleHeight, nacelleDepth);

			/* draw rotor blades */

			if (target instanceof ModelTarget<?>) {

				((ModelTarget<?>) target).drawModel(ROTOR,
						getBase().addY(poleHeight).add(-poleRadiusTop*2, nacelleHeight/2, 0),
						0, rotorDiameter, rotorDiameter, rotorDiameter);

			} else {

				ROTOR.render(target,
						getBase().addY(poleHeight).add(-poleRadiusTop*2, nacelleHeight/2, 0),
						0, rotorDiameter, rotorDiameter, rotorDiameter);

			}

		}

		@Override
		public void renderTo(ModelTarget<?> target) {
			renderTo(target);
		}

	}

	private static class PowerMinorLine
		extends NoOutlineWaySegmentWorldObject
		implements RenderableToAllTargets {

		private static final float DEFAULT_THICKN = 0.05f; // width and height
		private static final float DEFAULT_CLEARING_BL = 7.5f; // power pole height is 8
		private static final Material material = PLASTIC_BLACK;

		public PowerMinorLine(MapWaySegment segment) {
			super(segment);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target<?> target) {

			ShapeXZ powerlineShape = new PolylineXZ(
				new VectorXZ(-DEFAULT_THICKN/2, DEFAULT_CLEARING_BL),
				new VectorXZ(-DEFAULT_THICKN/2, DEFAULT_CLEARING_BL + DEFAULT_THICKN),
				new VectorXZ(+DEFAULT_THICKN/2, DEFAULT_CLEARING_BL + DEFAULT_THICKN),
				new VectorXZ(+DEFAULT_THICKN/2, DEFAULT_CLEARING_BL)
			);

			List<VectorXYZ> path = getBaseline();

			target.drawExtrudedShape(material, powerlineShape, getBaseline(),
					nCopies(path.size(), Y_UNIT), null, null, null);

		}

	}

	private final static class PowerLine
		extends NoOutlineWaySegmentWorldObject
		implements RenderableToAllTargets {

		private static final float CABLE_THICKNESS = 0.05f;
		// TODO: we need black plastic for cable material
		private final static Material CABLE_MATERIAL = PLASTIC_BLACK;
		private static final double SLACK_SPAN = 6;
		private static final double INTERPOLATION_STEPS = 10;
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
		public void renderTo(Target<?> target) {

			// do initial setup for height and position calculation, if necessary
			if (startPos == null) {
				setup();
			}

			for (int i = 0; i < startPos.size(); i++) {

				VectorXYZ start = startPos.get(i);
				VectorXYZ end = endPos.get(i);

				double lenToEnd = end.distanceToXZ(start);
				double heightDiff = end.y - start.y;

				double stepSize = lenToEnd / INTERPOLATION_STEPS;
				VectorXZ dir = end.xz().subtract(start.xz()).normalize();

				List<VectorXYZ> path = new ArrayList<VectorXYZ>();
				for (int x = 0; x <= INTERPOLATION_STEPS; x++) {
					double ratio = x / INTERPOLATION_STEPS;

					// distance from start to position x
					double dx = stepSize * x;

					// use a simple parabola between two towers
					double height = (1 - Math.pow(2.0*(ratio - 0.5), 2)) * -SLACK_SPAN;
					// add a linear function to account for different tower/terrain heights
					height += ratio * heightDiff;

					path.add(start.add(dir.mult(dx)).add(0, height, 0));
				}

				target.drawExtrudedShape(CABLE_MATERIAL, powerlineShape, path,
						nCopies(path.size(), Y_UNIT), null, null, null);

			}
		}

	}


	private static final class PowerTower extends NoOutlineNodeWorldObject
		implements RenderableToAllTargets {

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
		public void renderTo(Target<?> target) {

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


	private static final class HighVoltagePowerTower extends NoOutlineNodeWorldObject
		implements RenderableToAllTargets {

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

		private VectorXZ[][] getCorners(VectorXZ center, double diameter) {
			double half = diameter/2;
			VectorXZ ortho = direction.rightNormal();

			VectorXZ right_in = center.add(direction.mult(half));
			VectorXZ left_in = center.add(direction.mult(-half));
			VectorXZ right_out = center.add(direction.mult(half));
			VectorXZ left_out = center.add(direction.mult(-half));

			// TODO: if we can switch off backface culling we'd only need one face here
			return new VectorXZ[][]{
					new VectorXZ[]{
							right_in.add(ortho.mult(-half)),
							right_in.add(ortho.mult(half)),
							left_in.add(ortho.mult(half)),
							left_in.add(ortho.mult(-half))
					},
					new VectorXZ[]{
							right_out.add(ortho.mult(half)),
							right_out.add(ortho.mult(-half)),
							left_out.add(ortho.mult(-half)),
							left_out.add(ortho.mult(half))
					}};
		}

		private void drawSegment(Target<?> target,
				VectorXZ[] low, VectorXZ[] high, double base, double height) {

			for (int a = 0; a < 4; a++) {

				List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
				List<VectorXZ> tex = new ArrayList<VectorXZ>();
				List<List<VectorXZ>> texList =
					nCopies(Materials.POWER_TOWER_VERTICAL.getNumTextureLayers(), tex);

				for (int i = 0; i < 2; i++) {
					int idx = (a+i)%4;
					vs.add(high[idx].xyz(height));
					vs.add(low[idx].xyz(base));
					tex.add(new VectorXZ(i, 1));
					tex.add(new VectorXZ(i, 0));
				}

				target.drawTriangleStrip(Materials.POWER_TOWER_VERTICAL, vs, texList);
			}
		}

		private void drawHorizontalSegment(Target<?> target,
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

		private void drawHorizontalTop(Target<?> target, VectorXZ[][] points,
				double base, double border, double middle, double center) {

			double[] height = new double[]{border, middle, center, center, middle, border};

			int len = height.length;
			for (int a = 0; a < len-1; a++) {

				List<VectorXYZ> vs = new ArrayList<VectorXYZ>();
				List<VectorXZ> tex = new ArrayList<VectorXZ>();
				List<List<VectorXZ>> texList =
						nCopies(Materials.POWER_TOWER_VERTICAL.getNumTextureLayers(), tex);

				for (int i = 0; i < 2; i++) {
					vs.add(points[1][a+i].xyz(base + height[a+i]));
					vs.add(points[2][a+i].xyz(base + height[a+i]));
					tex.add(new VectorXZ(0, i));
					tex.add(new VectorXZ(1, i));
				}
				target.drawTriangleStrip(Materials.POWER_TOWER_VERTICAL, vs, texList);
			}
		}


		private double drawPart(Target<?> target, double elevation,
				int nr_segments, double segment_height, double ground_size,
				double top_size) {

			for (int i = 0; i < nr_segments; i++) {

				double bottom = ground_size + i * (top_size - ground_size) / nr_segments;
				double top = ground_size + (i + 1) * (top_size - ground_size) / nr_segments;

				VectorXZ[][] low = getCorners(node.getPos(), bottom);
				VectorXZ[][] high = getCorners(node.getPos(), top);

				drawSegment(target, low[0], high[0], elevation, elevation + segment_height);
				drawSegment(target, low[1], high[1], elevation, elevation + segment_height);

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

		private void drawHorizontalPole(Target<?> target, double elevation,
				double diameter, double width) {

			double half = diameter / 2;
			VectorXZ ortho = direction.rightNormal();

			// TODO: if we can switch off backface culling we'd only need one face here
			VectorXZ[][] draw = new VectorXZ[][] {
					getPoleCoordinates(node.getPos().add(direction.mult(-half)), ortho, width, half),
					getPoleCoordinates(node.getPos().add(direction.mult(-half)), ortho.invert(), width, half),
					getPoleCoordinates(node.getPos().add(direction.mult(half)), ortho.invert(), width, half),
					getPoleCoordinates(node.getPos().add(direction.mult(half)), ortho, width, half)
			};

			for (int i = 0; i < 4; i++) {
				drawHorizontalSegment(target, draw[i][0], draw[i][1], elevation, 0.1, diameter/2);
				drawHorizontalSegment(target, draw[i][1], draw[i][2], elevation, diameter/2, diameter);
				drawHorizontalSegment(target, draw[i][2], draw[i][3], elevation, diameter, diameter);
				drawHorizontalSegment(target, draw[i][3], draw[i][4], elevation, diameter, diameter/2);
				drawHorizontalSegment(target, draw[i][4], draw[i][5], elevation, diameter/2, 0.1);
			}

			drawHorizontalTop(target, draw, elevation, 0.1, diameter/2, diameter);
		}

		// TODO we're missing the ceramics to hold the power lines

		@Override
		public void renderTo(Target<?> target) {

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

	private static final class PhotovoltaicPlant extends AbstractAreaWorldObject
		implements RenderableToAllTargets {

		//TODO create individual EleConnector for panels

		/** compares vectors by x coordinate */
		private static final Comparator<VectorXZ> X_COMPARATOR = comparingDouble(v -> v.x);

		protected PhotovoltaicPlant(MapArea area) {
			super(area);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target<?> target) {

			/* construct panel geometry */

			double panelAngle = PI / 4;
			double panelHeight = 5;

			VectorXYZ upVector = Z_UNIT.mult(panelHeight).rotateX(-panelAngle);

			/* place and draw rows of panels */

			AxisAlignedBoundingBoxXZ box = this.getAxisAlignedBoundingBoxXZ();

			List<SimplePolygonXZ> obstacles = getGroundObstacles();

			double posZ = box.minZ;

			while (posZ + upVector.z < box.maxZ) {

				LineSegmentXZ rowLine = new LineSegmentXZ(
						 new VectorXZ(box.minX - 10, posZ),
						 new VectorXZ(box.maxX + 10, posZ));

				// calculate start and end points (maybe more than one each)

				List<VectorXZ> intersections =
						area.getPolygon().intersectionPositions(rowLine);

				assert intersections.size() % 2 == 0;

				intersections.sort(X_COMPARATOR);

				// add more start/end points at ground-level obstacles

				for (SimplePolygonXZ obstacle : obstacles) {

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

				// draw row of panels between each start/end pair

				assert intersections.size() % 2 == 0;

				for (int i = 0; i + 1 < intersections.size(); i += 2) {

					// TODO: take elevation into account
					// Might necessitate individual panels or shorter strips.

//					renderPanelsTo(target,
//							eleProfile.getWithEle(intersections.get(i)),
//							eleProfile.getWithEle(intersections.get(i+1)),
//							upVector);

				}

				posZ += upVector.z * 1.5;

			}

		}

		/**
		 * returns outlines from ground objects overlapping this area
		 */
		private List<SimplePolygonXZ> getGroundObstacles() {

			List<SimplePolygonXZ> obstacles = new ArrayList<SimplePolygonXZ>();

			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
				for (WorldObject otherWO : overlap.getOther(area).getRepresentations()) {

					if (otherWO.getGroundState() == GroundState.ON
							&& otherWO instanceof WorldObjectWithOutline) {

						obstacles.add(((WorldObjectWithOutline)otherWO).getOutlinePolygonXZ());

					}

				}
			}

			return obstacles;

		}

		private void renderPanelsTo(Target<?> target, VectorXYZ bottomLeft,
				VectorXYZ bottomRight, VectorXYZ upVector) {

			/* draw front */

			List<VectorXYZ> vs = asList(
					bottomLeft.add(upVector),
					bottomLeft,
					bottomRight.add(upVector),
					bottomRight);

			target.drawTriangleStrip(Materials.SOLAR_PANEL, vs,
					texCoordLists(vs, Materials.SOLAR_PANEL, STRIP_WALL));

			/* draw back */

			vs = asList(vs.get(2), vs.get(3), vs.get(0), vs.get(1));

			target.drawTriangleStrip(Materials.PLASTIC_GREY, vs,
					texCoordLists(vs, Materials.PLASTIC_GREY, STRIP_WALL));

		}


	}

}

package org.osm2world.core.world.modules;

import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.filterWorldObjectCollisions;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.openstreetmap.josm.plugins.graphview.core.data.Tag;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_data.data.overlaps.MapOverlap;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.GeometryUtil;
import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.FaceTarget;
import org.osm2world.core.target.common.RenderableToFaceTarget;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.povray.POVRayTarget;
import org.osm2world.core.target.povray.RenderableToPOVRay;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.data.WaySegmentWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.common.ConfigurableWorldModule;
import org.osm2world.core.world.modules.common.WorldModuleBillboardUtil;

/**
 * adds trees, tree rows, tree groups and forests to the world
 */
public class TreeModule extends ConfigurableWorldModule {
	
	private boolean useBillboards = false;
	private double defaultTreeHeight = 10;
	private double defaultTreeHeightForest = 20;
	
	@Override
	public void setConfiguration(Configuration config) {
		super.setConfiguration(config);
		useBillboards = config.getBoolean("useBillboards", false);
		defaultTreeHeight = config.getDouble("defaultTreeHeight", 10);
		defaultTreeHeightForest = config.getDouble("defaultTreeHeightForest", 20);
	}
	
	@Override
	public final void applyTo(MapData mapData) {
		
		for (MapNode node : mapData.getMapNodes()) {
			
			if (node.getTags().contains("natural", "tree")) {
				node.addRepresentation(new Tree(node));
			}
			
		}

		for (MapWaySegment segment : mapData.getMapWaySegments()) {
			
			if (segment.getTags().contains(new Tag("natural", "tree_row"))) {
				segment.addRepresentation(new TreeRow(segment));
			}
			
		}

		for (MapArea area : mapData.getMapAreas()) {
			
			if (area.getTags().contains("natural", "wood")
					|| area.getTags().contains("landuse", "forest")
					|| area.getTags().containsKey("wood")
					|| area.getTags().contains("landuse", "orchard")) {
				area.addRepresentation(new Forest(area, mapData));
			}
			
		}
		
	}

	private static final float TREE_RADIUS_PER_HEIGHT = 0.2f;
	
	private void renderTree(Target<?> target,
			MapElement element,	VectorXYZ pos) {
		
		boolean fruit = isFruitTree(element, pos);
		boolean coniferous = isConiferousTree(element, pos);
		double height = getTreeHeight(element, coniferous, fruit);
		
		if (useBillboards) {
			
			//"random" decision based on x coord
			boolean mirrored = (long)(pos.getX()) % 2 == 0;
			
			Material material = fruit
					? Materials.TREE_BILLBOARD_BROAD_LEAVED_FRUIT
					: coniferous
					? Materials.TREE_BILLBOARD_CONIFEROUS
					: Materials.TREE_BILLBOARD_BROAD_LEAVED;
			
			WorldModuleBillboardUtil.renderCrosstree(target, material, pos,
					(fruit ? 1.0 : 0.5 ) * height, height, mirrored);
			
		} else {
			
			renderTreeGeometry(target, pos, coniferous, height);
			
		}
		
	}
	
	private static void renderTreeGeometry(Target<?> target,
			VectorXYZ posXYZ, boolean coniferous, double height) {
		
		double stemRatio = coniferous?0.3:0.5;
		double radius = height*TREE_RADIUS_PER_HEIGHT;
		
		target.drawColumn(Materials.TREE_TRUNK,
				null, posXYZ, height*stemRatio,
				radius / 4, radius / 5, false, true);
		
		target.drawColumn(Materials.TREE_CROWN,
				null, posXYZ.y(posXYZ.y+height*stemRatio),
				height*(1-stemRatio),
				radius,
				coniferous ? 0 : radius,
				true, true);
	}
		
	private static boolean isConiferousTree(MapElement element, Vector3D pos) {
		
		String typeValue = element.getTags().getValue("wood");
		
		if (typeValue == null) {
			typeValue = element.getTags().getValue("type");
		}
		
		if ("broad_leaved".equals(typeValue)
				|| "broad_leafed".equals(typeValue) // both values are common
				|| "deciduous".equals(typeValue)) {
			return false;
		} else if ("coniferous".equals(typeValue)
				|| "conifer".equals(typeValue)) {
			return true;
		} else { //mixed or undefined
			//"random" decision based on x coord
			return (long)(pos.getX()) % 2 == 0;
		}
		
	}
	
	private static boolean isFruitTree(MapElement element, Vector3D pos) {
		
		if (element.getTags().contains("landuse", "orchard")) {
			return true;
		}
		
		String species = element.getTags().getValue("species");
		
		return species != null &&
				species.contains("malus");
		
	}
	
	/**
	 * parse height (for forests, add some random factor)
	 */
	private double getTreeHeight(MapElement element,
			boolean isConiferousTree, boolean isFruitTree) {
		
		float heightFactor = 1;
		if (element instanceof MapArea) {
			heightFactor = 0.5f + 0.75f * (float)Math.random();
		}
		
		double defaultHeight = defaultTreeHeight;
		if (element instanceof MapArea && !isFruitTree) {
			defaultHeight = defaultTreeHeightForest;
		}
		
		return heightFactor *
				parseHeight(element.getTags(), (float)defaultHeight);
		
	}
	
	private POVRayTarget previousDeclarationTarget = null;
	
	private void addTreeDeclarationsTo(POVRayTarget target) {
		if (target != previousDeclarationTarget) {
		
			previousDeclarationTarget = target;
			
			target.append("#ifndef (broad_leaved_tree)\n");
			target.append("#declare broad_leaved_tree = object { union {\n");
			renderTreeGeometry(target, VectorXYZ.NULL_VECTOR, false, 1);
			target.append("} }\n#end\n\n");
			
			target.append("#ifndef (coniferous_tree)\n");
			target.append("#declare coniferous_tree = object { union {\n");
			renderTreeGeometry(target, VectorXYZ.NULL_VECTOR, true, 1);
			target.append("} }\n#end\n\n");
			
		}
	}

	private void renderTree(POVRayTarget target,
			MapElement element, VectorXYZ pos) {
		
		boolean isConiferousTree = isConiferousTree(element, pos);
		boolean isFruitTree = isFruitTree(element, pos);
		
		double height = getTreeHeight(element, isConiferousTree, isFruitTree);
		
		//rotate randomly for variation
		float yRotation = (float) Math.random() * 360;
		
		//add union of stem and leaves
		if (isConiferousTree) {
			target.append("object { coniferous_tree rotate ");
		} else {
			target.append("object { broad_leaved_tree rotate ");
		}
		
		target.append(Float.toString(yRotation));
		target.append("*y scale ");
		target.append(height);
		target.append(" translate ");
		target.appendVector(pos.x, 0, pos.z);
		target.append(" }\n");
		
	}
	
	private class Tree extends NoOutlineNodeWorldObject
		implements RenderableToAllTargets, RenderableToPOVRay {
		
		private final boolean isConiferous;
		private final boolean isFruitTree;
		
		public Tree(MapNode node) {
			super(node);
			this.isConiferous = isConiferousTree(node, node.getPos());
			this.isFruitTree = isFruitTree(node, node.getPos());
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public AxisAlignedBoundingBoxXZ getAxisAlignedBoundingBoxXZ() {
			return new AxisAlignedBoundingBoxXZ(Collections.singleton(node.getPos()));
		}
		
		@Override
		public void renderTo(Target<?> target) {
			renderTree(target, node, getBase());
		}
		
		@Override
		public void addDeclarationsTo(POVRayTarget target) {
			addTreeDeclarationsTo(target);
		}
		
		@Override
		public void renderTo(POVRayTarget target) {
			renderTree(target, node, getBase());
		}
		
	}

	private class TreeRow implements WaySegmentWorldObject,
		RenderableToPOVRay, RenderableToFaceTarget, RenderableToAllTargets {

		private final MapWaySegment segment;

		private final List<EleConnector> treeConnectors;
		
		public TreeRow(MapWaySegment segment) {
			
			this.segment = segment;
			
			//TODO: spread along a full way
			
			List<VectorXZ> treePositions = GeometryUtil.equallyDistributePointsAlong(
					4 /* TODO: derive from tree count */ ,
					false /* TODO: should be true once a full way is covered */,
					segment.getStartNode().getPos(), segment.getEndNode().getPos());
			
			treeConnectors = new ArrayList<EleConnector>(treePositions.size());
			
			for (VectorXZ treePosition : treePositions) {
				treeConnectors.add(new EleConnector(treePosition));
			}
			
		}
		
		@Override
		public MapWaySegment getPrimaryMapElement() {
			return segment;
		}
		
		@Override
		public Iterable<EleConnector> getEleConnectors() {
			// TODO Auto-generated method stub
			return null;
		}
		
		@Override
		public void addEleConstraints(EleConstraintEnforcer enforcer) {}
		
		@Override
		public VectorXZ getEndPosition() {
			return segment.getEndNode().getPos();
		}

		@Override
		public VectorXZ getStartPosition() {
			return segment.getStartNode().getPos();
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(POVRayTarget target) {
			for (EleConnector treeConnector : treeConnectors) {
				renderTree(target, segment, treeConnector.getPosXYZ());
			}
		}
		
		@Override
		public void addDeclarationsTo(POVRayTarget target) {
			addTreeDeclarationsTo(target);
		}
		
		@Override
		public void renderTo(FaceTarget<?> target) {
			for (EleConnector treeConnector : treeConnectors) {
				renderTree(target, segment, treeConnector.getPosXYZ());
				target.flushReconstructedFaces();
			}
		}
		
		@Override
		public void renderTo(Target<?> target) {
			for (EleConnector treeConnector : treeConnectors) {
				renderTree(target, segment, treeConnector.getPosXYZ());
			}
		}
		
		//TODO: there is significant code duplication with Forest...
		
	}
	

	private class Forest implements AreaWorldObject,
		RenderableToPOVRay, RenderableToFaceTarget, RenderableToAllTargets {

		private final MapArea area;
		private final MapData mapData;
		
		private Collection<EleConnector> treeConnectors = null;
		
		public Forest(MapArea area, MapData mapData) {
			
			this.area = area;
			this.mapData = mapData;
						
		}

		private void createTreeConnectors(double density) {
			
			/* collect other objects that the trees should not be placed on */
			
			Collection<WorldObject> avoidedObjects = new ArrayList<WorldObject>();
			
			for (MapOverlap<?, ?> overlap : area.getOverlaps()) {
				for (WorldObject otherRep : overlap.getOther(area).getRepresentations()) {
					
					if (otherRep.getGroundState() == GroundState.ON) {
						avoidedObjects.add(otherRep);
					}
				
				}
			}
			
			/* place the trees */
			
			List<VectorXZ> treePositions =
				GeometryUtil.distributePointsOn(area.getOsmObject().id,
						area.getPolygon(), mapData.getBoundary(),
						density, 0.3f);
			
			filterWorldObjectCollisions(treePositions, avoidedObjects);
			
			/* create a terrain connector for each tree */
			
			treeConnectors = new ArrayList<EleConnector>(treePositions.size());
			
			for (VectorXZ treePosition : treePositions) {
				treeConnectors.add(new EleConnector(treePosition));
			}
			
		}
		
		@Override
		public MapArea getPrimaryMapElement() {
			return area;
		}
		
		@Override
		public Iterable<EleConnector> getEleConnectors() {
			
			if (treeConnectors == null) {
				createTreeConnectors(config.getDouble("treesPerSquareMeter", 0.01f));
			}
			
			return treeConnectors;
			
		}

		@Override
		public void addEleConstraints(EleConstraintEnforcer enforcer) {}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(POVRayTarget target) {
			for (EleConnector treeConnector : treeConnectors) {
				renderTree(target, area, treeConnector.getPosXYZ());
			}
		}
		
		@Override
		public void addDeclarationsTo(POVRayTarget target) {
			addTreeDeclarationsTo(target);
		}
		
		@Override
		public void renderTo(FaceTarget<?> target) {
			for (EleConnector treeConnector : treeConnectors) {
				renderTree(target, area, treeConnector.getPosXYZ());
				target.flushReconstructedFaces();
			}
		}
		
		@Override
		public void renderTo(Target<?> target) {
			for (EleConnector treeConnector : treeConnectors) {
				renderTree(target, area, treeConnector.getPosXYZ());
			}
		}
		
	}
		
}

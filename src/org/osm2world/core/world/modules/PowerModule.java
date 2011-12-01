package org.osm2world.core.world.modules;

import static java.lang.Math.toRadians;
import static java.util.Collections.nCopies;
import static org.osm2world.core.world.modules.common.WorldModuleGeometryUtil.createShapeExtrusionAlong;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseHeight;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.parseWidth;

import java.util.List;

import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;
import org.osm2world.core.world.network.AbstractNetworkWaySegmentWorldObject;

/**
 * module for power infrastructure
 */
public class PowerModule extends AbstractModule {
	
	@Override
	protected void applyToNode(MapNode node) {
		if (node.getTags().contains("power", "pole")) {
			node.addRepresentation(new Powerpole(node));
		}
		if (node.getTags().contains("power", "generator")
			&& node.getTags().contains("generator:source", "wind")) {
			node.addRepresentation(new WindTurbine(node));
		}
	}
	
	@Override
	protected void applyToWaySegment(MapWaySegment segment) {
		if (segment.getTags().contains("power", "minor_line")) {
			segment.addRepresentation(new Powerline(segment));
		}
	}
	
	private static final class Powerpole extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		public Powerpole(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public MapElement getPrimaryMapElement() {
			return node;
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
			
			target.drawColumn(material, null,
					node.getElevationProfile().getWithEle(node.getPos()),
					parseHeight(node.getTags(), 8f),
					0.15, 0.15, false, true);
		}
		
	}
	
	private static final class WindTurbine extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {
		
		public WindTurbine(MapNode node) {
			super(node);
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public MapElement getPrimaryMapElement() {
			return node;
		}
		
		/** returns an array of vectors rotated by the given angle around the given Y and Z coordinates */
		public VectorXYZ[] rotateVectorsX(VectorXYZ[] vectors, double angle, double posY, double posZ) {
			for(int i = 0; i < vectors.length; ++i) {
				vectors[i] = vectors[i].add(0f, -posY, -posZ);
				vectors[i] = vectors[i].rotateX(toRadians(angle));
				vectors[i] = vectors[i].add(0f, posY, posZ);
			}

			return vectors;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			double ele = node.getElevationProfile().getEle();
			
			float poleHeight = parseHeight(node.getTags(), 100f);
			float poleRadiusBottom = parseWidth(node.getTags(), 5) / 2;
			float poleRadiusTop = poleRadiusBottom / 2;
			float nacelleHeight = poleHeight * 0.05f;
			float nacelleDepth = poleHeight * 0.1f;
			float bladeLength = poleHeight / 2;
			
			/* determine material */
			
			Material poleMaterial = null;
			Material nacelleMaterial = Materials.STEEL;
			Material bladeMaterial = Materials.STEEL; // probably fibre, but color matches roughly :)
			
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
					node.getElevationProfile().getWithEle(node.getPos()),
					poleHeight,
					poleRadiusBottom, poleRadiusTop, false, false);
			
			/* draw nacelle */
			VectorXZ nacelleVector = VectorXZ.X_UNIT;
			target.drawBox(nacelleMaterial,
					node.getPos().xyz(ele + poleHeight).add(nacelleDepth/2 - poleRadiusTop*2, 0f, 0f),
					nacelleVector, nacelleHeight, nacelleHeight, nacelleDepth);
			
			/* draw blades */
			
			// define first blade
			VectorXYZ[] bladeFront = {
				node.getPos().xyz(ele + poleHeight).add(-poleRadiusTop*2, nacelleHeight/2, +nacelleHeight/2),
				node.getPos().xyz(ele + poleHeight).add(-poleRadiusTop*2, nacelleHeight/2 - bladeLength, 0f),
				node.getPos().xyz(ele + poleHeight).add(-poleRadiusTop*2, nacelleHeight/2, -nacelleHeight/2),
			};
			VectorXYZ[] bladeBack = {
				bladeFront[0],
				bladeFront[2],
				bladeFront[1]
			};
			
			// rotate and draw blades
			double rotCenterY = ele + poleHeight + nacelleHeight/2;
			double rotCenterZ = node.getPos().getZ();
			
			bladeFront = rotateVectorsX(bladeFront, 60, rotCenterY, rotCenterZ);
			bladeBack  = rotateVectorsX(bladeBack, 60, rotCenterY, rotCenterZ);
			target.drawTriangleStrip(bladeMaterial, bladeFront);
			target.drawTriangleStrip(bladeMaterial, bladeBack);
			bladeFront = rotateVectorsX(bladeFront, 120, rotCenterY, rotCenterZ);
			bladeBack  = rotateVectorsX(bladeBack, 120, rotCenterY, rotCenterZ);
			target.drawTriangleStrip(bladeMaterial, bladeFront);
			target.drawTriangleStrip(bladeMaterial, bladeBack);
			bladeFront = rotateVectorsX(bladeFront, 120, rotCenterY, rotCenterZ);
			bladeBack  = rotateVectorsX(bladeBack, 120, rotCenterY, rotCenterZ);
			target.drawTriangleStrip(bladeMaterial, bladeFront);
			target.drawTriangleStrip(bladeMaterial, bladeBack);
			
		}
		
	}
	
	private static class Powerline
		extends AbstractNetworkWaySegmentWorldObject
		implements RenderableToAllTargets {
		
		private static final float DEFAULT_THICKN = 0.1f; // width and height
		private static final float DEFAULT_CLEARING_BL = 7.5f; // power pole height is 8
		private static final Material material = Materials.STEEL;
		
		public Powerline(MapWaySegment segment) {
			super(segment);
		}
		
		@Override
		public float getWidth() {
			return DEFAULT_THICKN;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public double getClearingAbove(VectorXZ pos) {
			return 0;
		}
		
		@Override
		public double getClearingBelow(VectorXZ pos) {
			//powerlines are currently treated as running on the ground for simplicity
			return 0;
		}
		
		@Override
		public void renderTo(Target<?> target) {
			
			VectorXYZ[] wallShape = {
				new VectorXYZ(-DEFAULT_THICKN/2, DEFAULT_CLEARING_BL, 0),
				new VectorXYZ(-DEFAULT_THICKN/2, DEFAULT_CLEARING_BL + DEFAULT_THICKN, 0),
				new VectorXYZ(+DEFAULT_THICKN/2, DEFAULT_CLEARING_BL + DEFAULT_THICKN, 0),
				new VectorXYZ(+DEFAULT_THICKN/2, DEFAULT_CLEARING_BL, 0)
			};
			
			List<VectorXYZ> path =
				line.getElevationProfile().getPointsWithEle();
			
			List<VectorXYZ[]> strips = createShapeExtrusionAlong(wallShape,
					path, nCopies(path.size(), VectorXYZ.Y_UNIT));
			
			for (VectorXYZ[] strip : strips) {
				target.drawTriangleStrip(material, strip);
			}
			
		}
		
	}
	
}

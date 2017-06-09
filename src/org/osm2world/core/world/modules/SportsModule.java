package org.osm2world.core.world.modules;

import static java.lang.Math.cos;
import static org.osm2world.core.target.common.material.Materials.PITCH_SOCCER;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.SimplePolygonXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.PolygonUtil;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.TexCoordFunction;
import org.osm2world.core.world.data.AbstractAreaWorldObject;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;
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
				area.addRepresentation(new Pitch(area));
			}
			
			
			
		}
		
	}
	
	static class Pitch extends AbstractAreaWorldObject
			implements RenderableToAllTargets, TerrainBoundaryWorldObject {
	
		public Pitch(MapArea area) {
			
			super(area);
			
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void renderTo(Target<?> target) {

			Material material = PITCH_SOCCER;
			
			Collection<TriangleXYZ> triangles = getTriangulation();
			
			target.drawTriangles(material, triangles,
					triangleTexCoordLists(triangles, material,
					configureTexFunction(area.getOuterPolygon())));
			
		}

		/**
		 * calculates the parameters for a {@link TexCoordFunction}
		 * and calls the constructor
		 */
		private static TexCoordFunction configureTexFunction(SimplePolygonXZ polygon) {
			
			/* approximate a rectangular shape for the pitch */
			
			SimplePolygonXZ bbox = PolygonUtil.minimumBoundingBox(polygon);

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
			
			double scaleFactorShortSide = 1;
			double scaleFactorLongSide = 1;
			
			/* build the result */
			
			return new PitchTexFunction(origin, longSide, shortSide, scaleFactorShortSide, scaleFactorLongSide);
			
		}

		/**
		 * specialized texture coordinate calculation for pitch marking textures
		 */
		static class PitchTexFunction implements TexCoordFunction {
			
			private final VectorXZ origin;
			private final VectorXZ longSide;
			private final VectorXZ shortSide;
			private final double scaleFactorShortSide;
			private final double scaleFactorLongSide;
			
			PitchTexFunction(VectorXZ origin, VectorXZ longSide, VectorXZ shortSide,
					double scaleFactorShortSide, double scaleFactorLongSide) {
				
				this.origin = origin;
				this.longSide = longSide;
				this.shortSide = shortSide;
				this.scaleFactorShortSide = scaleFactorShortSide;
				this.scaleFactorLongSide = scaleFactorLongSide;
				
			}

			@Override
			public List<VectorXZ> apply(List<VectorXYZ> vs, TextureData textureData) {
				
				//TODO implement scaling
				
				List<VectorXZ> result = new ArrayList<VectorXZ>(vs.size());
				
				for (VectorXYZ vOriginal : vs) {
					
					VectorXZ v = vOriginal.xz().subtract(origin);
					
					double angleLong = VectorXZ.angleBetween(v, longSide);
					double longSideProjectedLength = v.length() * cos(angleLong);
					
					double angleShort = VectorXZ.angleBetween(v, shortSide);
					double shortSideProjectedLength = v.length() * cos(angleShort);
					
					result.add(new VectorXZ(
							shortSideProjectedLength / shortSide.length(),
							longSideProjectedLength / longSide.length()));
					
				}
				
				return result;
				
			}
			
		}
		
	}
	
}

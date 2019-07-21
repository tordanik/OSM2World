package org.osm2world.core.world.modules;

import static java.lang.Math.PI;
import static java.util.Arrays.asList;
import static org.osm2world.core.math.VectorXYZ.X_UNIT;
import static org.osm2world.core.target.common.material.Materials.*;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.STRIP_FIT;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.util.ArrayList;
import java.util.List;

import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.data.NoOutlineNodeWorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;


/**
 * adds traffic signs to the world
 */
public class TrafficSignModule extends AbstractModule {

	@Override
	protected void applyToNode(MapNode node) {

		if (!node.getTags().containsKey("traffic_sign")) return;

		if (isInHighway(node)) return; //only exact positions (next to highway)

		/* split the traffic sign value into its components */

		String tagValue = node.getTags().getValue("traffic_sign");
		String[] countryAndSigns = tagValue.split(":", 2);

		if (countryAndSigns.length != 2) return;

		String country = countryAndSigns[0];

		String[] signs = countryAndSigns[1].split("[;,]");

		/* match each individual sign to the list of supported types */

		List<TrafficSignType> types = new ArrayList<TrafficSignType>(signs.length);

		for (String sign : signs) {

			sign = sign.trim();
			sign = sign.replace('-', '_');

			try {
				types.add(TrafficSignType.valueOf(country + '_' + sign));
			} catch (IllegalArgumentException e) {
				// not a supported traffic sign type
			}

		}

		/* create a visual representation for the traffic sign */

		if (types.size() > 0) {
			node.addRepresentation(new TrafficSign(node, types));
		}

	}

	private enum TrafficSignType {

		STOP(SIGN_DE_206, 1, 2),

		DE_206(SIGN_DE_206, 1, 2),
		DE_250(SIGN_DE_250, 1, 2),
		DE_625(SIGN_DE_625_11, 2, .80),
		DE_625_11(SIGN_DE_625_11, 2, .80),
		DE_625_21(SIGN_DE_625_21, 2, .80),

		DE_101(SIGN_DE_101, 1, 2),
		DE_101_10(SIGN_DE_101_10, 1, 2),
		DE_101_11(SIGN_DE_101_11, 1, 2),
		DE_101_12(SIGN_DE_101_12, 1, 2),
		DE_101_13(SIGN_DE_101_13, 1, 2),
		DE_101_14(SIGN_DE_101_14, 1, 2),
		DE_101_15(SIGN_DE_101_15, 1, 2),
		DE_101_20(SIGN_DE_101_20, 1, 2),
		DE_101_21(SIGN_DE_101_21, 1, 2),
		DE_101_22(SIGN_DE_101_22, 1, 2),
		DE_101_23(SIGN_DE_101_23, 1, 2),
		DE_101_24(SIGN_DE_101_24, 1, 2),
		DE_101_25(SIGN_DE_101_25, 1, 2),
		DE_101_51(SIGN_DE_101_51, 1, 2),
		DE_101_52(SIGN_DE_101_52, 1, 2),
		DE_101_53(SIGN_DE_101_53, 1, 2),
		DE_101_54(SIGN_DE_101_54, 1, 2),
		DE_101_55(SIGN_DE_101_55, 1, 2),
		DE_102(SIGN_DE_102, 1, 2),
		DE_103_10(SIGN_DE_103_10, 1, 2),
		DE_103_20(SIGN_DE_103_20, 1, 2),
		DE_105_10(SIGN_DE_105_10, 1, 2),
		DE_105_20(SIGN_DE_105_20, 1, 2),
		DE_108_10(SIGN_DE_108_10, 1, 2),
		DE_110_12(SIGN_DE_110_12, 1, 2),
		DE_112(SIGN_DE_112, 1, 2),
		DE_114(SIGN_DE_114, 1, 2),
		DE_117_10(SIGN_DE_117_10, 1, 2),
		DE_117_20(SIGN_DE_117_20, 1, 2),
		DE_120(SIGN_DE_120, 1, 2),
		DE_121_10(SIGN_DE_121_10, 1, 2),
		DE_121_20(SIGN_DE_121_20, 1, 2),
		DE_123(SIGN_DE_123, 1, 2),
		DE_124(SIGN_DE_124, 1, 2),
		DE_125(SIGN_DE_125, 1, 2),
		DE_131(SIGN_DE_131, 1, 2),
		DE_133_10(SIGN_DE_133_10, 1, 2),
		DE_133_20(SIGN_DE_133_20, 1, 2),
		DE_136_10(SIGN_DE_136_10, 1, 2),
		DE_136_20(SIGN_DE_136_20, 1, 2),
		DE_138_10(SIGN_DE_138_10, 1, 2),
		DE_138_20(SIGN_DE_138_20, 1, 2),
		DE_142_10(SIGN_DE_142_10, 1, 2),
		DE_142_20(SIGN_DE_142_20, 1, 2),
		DE_145(SIGN_DE_145, 1, 2),
		DE_151(SIGN_DE_151, 1, 2),
		DE_301(SIGN_DE_301, 1, 2);

		public final Material material;

		public final int numPosts;

		private final double defaultHeight;

		TrafficSignType(Material material, int numPosts, double defaultHeight) {
			this.material = material;
			this.numPosts = numPosts;
			this.defaultHeight = defaultHeight;
		}

	}

	private static boolean isInHighway(MapNode node){
		if (node.getConnectedWaySegments().size()>0){
			for(MapWaySegment way: node.getConnectedWaySegments()){
				if( way.getTags().containsKey("highway") && !way.getTags().containsAny("highway", asList("path", "footway", "platform") ) ){
					return true;
				}
			}
		}
		return false;
	}

	private static final class TrafficSign extends NoOutlineNodeWorldObject
			implements RenderableToAllTargets {

		private final List<TrafficSignType> types;

		public TrafficSign(MapNode node, List<TrafficSignType> types) {

			super(node);

			this.types = types;

		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void renderTo(Target<?> target) {

			/* get basic parameters */

			double height = parseHeight(node.getTags(), (float)types.get(0).defaultHeight);
			double postRadius = 0.05;

			double[] signHeights = new double[types.size()];
			double[] signWidths = new double[types.size()];

			for (int sign = 0; sign < types.size(); sign++) {

				TextureData textureData = null;

				if (types.get(sign).material.getNumTextureLayers() != 0) {
					textureData = types.get(sign).material.getTextureDataList().get(0);
				}

				if (textureData == null) {
					signHeights[sign] = 0.6;
					signWidths[sign] = 0.6;
				} else {
					signHeights[sign] = textureData.height;
					signWidths[sign] = textureData.width;
				}

			}

			/* position the post(s) */

			int numPosts = types.get(0).numPosts;

			List<VectorXYZ> positions = new ArrayList<VectorXYZ>(numPosts);

			for (int i = 0; i < numPosts; i++) {
				double relativePosition = 0.5 - (i+1)/(double)(numPosts+1);
				positions.add(getBase().add(X_UNIT.mult(relativePosition * signWidths[0])));
			}

			/* create the front and back side of the sign */

			List<List<VectorXYZ>> signGeometries = new ArrayList<List<VectorXYZ>>();

			double distanceBetweenSigns = 0.1;
			double upperHeight = height;

			for (int sign = 0; sign < types.size(); sign++) {

				double signHeight = signHeights[sign];
				double signWidth = signWidths[sign];

				List<VectorXYZ> vs = asList(
						getBase().add(+signWidth/2, upperHeight, postRadius),
						getBase().add(+signWidth/2, upperHeight-signHeight, postRadius),
						getBase().add(-signWidth/2, upperHeight, postRadius),
						getBase().add(-signWidth/2, upperHeight-signHeight, postRadius)
						);

				signGeometries.add(vs);

				upperHeight -= signHeight + distanceBetweenSigns;

			}

			/* rotate the sign around the base to match the direction tag */

			double direction = parseDirection(node.getTags(), PI);

			for (List<VectorXYZ> vs : signGeometries) {

				for (int i = 0; i < vs.size(); i++) {
					VectorXYZ v = vs.get(i);
					v = v.rotateVec(direction, getBase(), VectorXYZ.Y_UNIT);
					vs.set(i, v);
				}

			}

			if (positions.size() > 1) { // if 1, the post is exactly on the base
				for (int i = 0; i < positions.size(); i++) {
					VectorXYZ v = positions.get(i);
					v = v.rotateVec(direction, getBase(), VectorXYZ.Y_UNIT);
					positions.set(i, v);
				}
			}

			/* render the post(s) */

			for (VectorXYZ position : positions) {
				target.drawColumn(STEEL, null, position,
						height, postRadius, postRadius,
						false, true);
			}

			/* render the sign (front, then back) */

			for (int sign = 0; sign < types.size(); sign++) {

				TrafficSignType type = types.get(sign);
				List<VectorXYZ> vs = signGeometries.get(sign);

				target.drawTriangleStrip(type.material, vs,
						texCoordLists(vs, type.material, STRIP_FIT));

				vs = asList(vs.get(2), vs.get(3), vs.get(0), vs.get(1));

				target.drawTriangleStrip(STEEL, vs,
						texCoordLists(vs, STEEL, STRIP_FIT));

			}

		}

	}

}

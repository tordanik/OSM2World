package org.osm2world.core.world.modules.traffic_sign;

import static java.util.Arrays.asList;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.target.common.material.Materials.STEEL;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.STRIP_FIT;
import static org.osm2world.core.target.common.material.TexCoordUtil.texCoordLists;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.model.Model;

/**
 * 3D model of a single traffic sign. A {@link TrafficSignGroup} has one or more of these.
 */
public class TrafficSignModel implements Model {

	/**
	 * the material of the front of the sign.
	 * Will be based on the {@link TrafficSignType}'s material, but may be slightly modified for a particular instance
	 * of that sign type (e.g. with text placeholders filled in or colors modified).
	 */
	public final Material material;

	public final int numPosts;
	public final double defaultHeight;

	public TrafficSignModel(Material material, int numPosts, double height) {
		this.material = material;
		this.numPosts = numPosts;
		this.defaultHeight = height;
	}

	public static TrafficSignModel create(TrafficSignIdentifier signId, TagSet tagsOfElement, Configuration config) {

		/* prepare map with subtype and/or brackettext values */

		Map<String, String> map = new HashMap<>();

		if (signId.bracketText != null) {
			map.put("traffic_sign.brackettext", signId.bracketText);
		}
		if (signId.subType() != null) {
			map.put("traffic_sign.subtype", signId.subType());
		}

		/* load information about this type of sign from the configuration */

		TrafficSignType type = TrafficSignType.fromConfig(signId, config);
		if (type == null) { type = TrafficSignType.blankSign(); }

		/* build the model */

		return new TrafficSignModel(
				type.material.withPlaceholdersFilledIn(map, tagsOfElement),
				type.defaultNumPosts, type.defaultHeight);

	}

	public double getSignHeight() {
		if (material.getNumTextureLayers() > 0) {
			return material.getTextureLayers().get(0).baseColorTexture.height;
		} else {
			return 0.6;
		}
	}

	public double getSignWidth() {
		if (material.getNumTextureLayers() > 0) {
			return material.getTextureLayers().get(0).baseColorTexture.width;
		} else {
			return 0.6;
		}
	}

	@Override
	public void render(Target target, VectorXYZ position, double direction, Double height, Double width,
			Double length) {

		double signHeight = getSignHeight();
		double signWidth = getSignWidth();

		/* create the geometry of the sign */

		List<VectorXYZ> vsFront = asList(
					position.add(+signWidth / 2, +signHeight / 2, 0),
					position.add(+signWidth / 2, -signHeight / 2, 0),
					position.add(-signWidth / 2, +signHeight / 2, 0),
					position.add(-signWidth / 2, -signHeight / 2, 0));

		/* rotate the sign around the base to match the direction */

		for (int i = 0; i < vsFront.size(); i++) {
			VectorXYZ v = vsFront.get(i);
			v = v.rotateVec(direction, position, Y_UNIT);
			vsFront.set(i, v);
		}

		/* render the front of the sign */

		target.drawTriangleStrip(material, vsFront,
				texCoordLists(vsFront, material, STRIP_FIT));

		/* render the back of the sign */

		List<VectorXYZ> vsBack = asList(vsFront.get(2), vsFront.get(3), vsFront.get(0), vsFront.get(1));
		Material materialBack = STEEL;

		target.drawTriangleStrip(materialBack, vsBack,
				texCoordLists(vsBack, materialBack, STRIP_FIT));

	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(defaultHeight);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((material == null) ? 0 : material.hashCode());
		result = prime * result + numPosts;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TrafficSignModel other = (TrafficSignModel) obj;
		if (Double.doubleToLongBits(defaultHeight) != Double.doubleToLongBits(other.defaultHeight))
			return false;
		if (material == null) {
			if (other.material != null)
				return false;
		} else if (!material.equals(other.material))
			return false;
		if (numPosts != other.numPosts)
			return false;
		return true;
	}

}

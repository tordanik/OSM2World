package org.osm2world.core.world.modules.traffic_sign;

import static java.awt.Color.WHITE;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;
import static org.osm2world.core.target.common.material.Material.Interpolation.FLAT;
import static org.osm2world.core.target.common.material.Materials.STEEL;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.LOD3;
import static org.osm2world.core.target.common.mesh.LevelOfDetail.LOD4;
import static org.osm2world.core.target.common.texcoord.NamedTexCoordFunction.STRIP_FIT;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.mirroredHorizontally;
import static org.osm2world.core.target.common.texcoord.TexCoordUtil.texCoordFunctions;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.material.*;
import org.osm2world.core.target.common.material.CompositeTexture.CompositeMode;
import org.osm2world.core.target.common.material.Material.Transparency;
import org.osm2world.core.target.common.mesh.Mesh;
import org.osm2world.core.target.common.mesh.TriangleGeometry;
import org.osm2world.core.target.common.model.InstanceParameters;
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

	public final TrafficSignType type;

	public final int numPosts;
	public final double defaultHeight;

	/**
	 * the material of the back of a sign (uses the front material's alpha values to get the same shape)
	 *
	 * TODO: Use a hash of the alpha mask as the key to share back materials between sign types that have the same shape
	 */
	private static final Map<TrafficSignType, Material> backMaterials = new HashMap<>();

	/**
	 * the material of the front of a sign.
	 * Only used when combining multi-layer materials, single-layer materials are used without modification.
	 */
	private static final Map<Material, Material> frontMaterials = new HashMap<>();

	public TrafficSignModel(Material material, TrafficSignType type, int numPosts, double height) {
		this.material = material;
		this.type = type;
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
				type, type.defaultNumPosts, type.defaultHeight);

	}

	public double getSignHeight() {
		if (material.getNumTextureLayers() > 0) {
			return material.getTextureLayers().get(0).baseColorTexture.dimensions().height();
		} else {
			return 0.6;
		}
	}

	public double getSignWidth() {
		if (material.getNumTextureLayers() > 0) {
			return material.getTextureLayers().get(0).baseColorTexture.dimensions().width();
		} else {
			return 0.6;
		}
	}

	@Override
	public List<Mesh> buildMeshes(InstanceParameters params) {

		double signHeight = getSignHeight();
		double signWidth = getSignWidth();

		/* create the geometry of the sign */

		List<VectorXYZ> vsFront = asList(
				params.position().add(+signWidth / 2, +signHeight / 2, 0),
				params.position().add(+signWidth / 2, -signHeight / 2, 0),
				params.position().add(-signWidth / 2, +signHeight / 2, 0),
				params.position().add(-signWidth / 2, -signHeight / 2, 0));

		/* rotate the sign around the base to match the direction */

		for (int i = 0; i < vsFront.size(); i++) {
			VectorXYZ v = vsFront.get(i);
			v = v.rotateVec(params.direction(), params.position(), Y_UNIT);
			vsFront.set(i, v);
		}

		/* render the front of the sign */

		Material materialFront = getFrontMaterial(material);

		var frontBuilder = new TriangleGeometry.Builder(texCoordFunctions(materialFront, STRIP_FIT), null, FLAT);
		frontBuilder.addTriangleStrip(vsFront);
		var frontMesh = new Mesh(frontBuilder.build(), materialFront, LOD3, LOD4);

		/* render the back of the sign */

		List<VectorXYZ> vsBack = asList(vsFront.get(2), vsFront.get(3), vsFront.get(0), vsFront.get(1));

		Material materialBack = getBackMaterial(type);

		var backBuilder = new TriangleGeometry.Builder(texCoordFunctions(materialBack, mirroredHorizontally(STRIP_FIT)), null, FLAT);
		backBuilder.addTriangleStrip(vsBack);
		var backMesh = new Mesh(backBuilder.build(), materialBack, LOD3, LOD4);

		return List.of(frontMesh, backMesh);

	}

	private static Material getBackMaterial(TrafficSignType type) {

		if (type.material.getNumTextureLayers() == 0) {
			return STEEL;
		}

		if (!backMaterials.containsKey(type)) {

			// use the transparency information from the front of the sign to cut out the correct shape for the back
			return new ImmutableMaterial(
					FLAT, WHITE, Transparency.BINARY, asList(new TextureLayer(
						textureWithAlphaMask(type.material, STEEL.getTextureLayers().get(0).baseColorTexture),
						textureWithAlphaMask(type.material, STEEL.getTextureLayers().get(0).normalTexture),
						textureWithAlphaMask(type.material, STEEL.getTextureLayers().get(0).ormTexture),
						textureWithAlphaMask(type.material, STEEL.getTextureLayers().get(0).displacementTexture),
						false)));
		}

		return backMaterials.get(type);

	}

	private static Material getFrontMaterial(Material material) {

		if (material.getNumTextureLayers() <= 0) {
			return material;
		}

		if (!frontMaterials.containsKey(material)) {

			List<TextureData> baseColorTextures = material.getTextureLayers().stream().map(t -> t.baseColorTexture)
					.collect(toList());
			List<TextureData> normalTextures = material.getTextureLayers().stream().map(t -> t.normalTexture)
					.filter(t -> t != null).collect(toList());
			List<TextureData> ormTextures = material.getTextureLayers().stream().map(t -> t.ormTexture)
					.filter(t -> t != null).collect(toList());
			List<TextureData> displacementTextures = material.getTextureLayers().stream().map(t -> t.displacementTexture)
					.filter(t -> t != null).collect(toList());

			return new ImmutableMaterial(
					FLAT, WHITE, Transparency.BINARY, asList(new TextureLayer(
						CompositeTexture.stackOf(baseColorTextures),
						normalTextures.isEmpty() ? null : CompositeTexture.stackOf(normalTextures),
						ormTextures.isEmpty() ? null : CompositeTexture.stackOf(ormTextures),
						displacementTextures.isEmpty() ? null : CompositeTexture.stackOf(displacementTextures),
						false)));
		}

		return frontMaterials.get(material);

	}

	private static CompositeTexture textureWithAlphaMask(Material alphaMaterial, TextureData textureB) {
		return new CompositeTexture(CompositeMode.ALPHA_FROM_A, false,
				alphaMaterial.getTextureLayers().get(0).baseColorTexture, textureB);
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

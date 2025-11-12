package org.osm2world.world.modules.traffic_sign;

import static java.lang.Math.PI;
import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;
import static org.osm2world.map_elevation.data.GroundState.ATTACHED;
import static org.osm2world.map_elevation.data.GroundState.ON;
import static org.osm2world.math.VectorXYZ.X_UNIT;
import static org.osm2world.math.VectorXYZ.Y_UNIT;
import static org.osm2world.math.VectorXZ.angleBetween;
import static org.osm2world.scene.material.Materials.STEEL;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD3;
import static org.osm2world.scene.mesh.LevelOfDetail.LOD4;
import static org.osm2world.world.modules.common.WorldModuleParseUtil.parseHeight;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.osm2world.conversion.O2WConfig;
import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.scene.mesh.ExtrusionGeometry;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.scene.model.InstanceParameters;
import org.osm2world.scene.model.ModelInstance;
import org.osm2world.world.attachment.AttachmentConnector;
import org.osm2world.world.attachment.AttachmentUtil;
import org.osm2world.world.data.NoOutlineNodeWorldObject;

/**
 * a group of vertically stacked traffic signs, including their pole(s) or other means of support/attachment.
 */
public class TrafficSignGroup extends NoOutlineNodeWorldObject {

	/** The {@link TrafficSignModel}s from top to bottom */
	public List<TrafficSignModel> signs;

	/** location of this group of signs. Can be different from the node's position if the node is part of a way. */
	public VectorXZ position;

	/** The direction the signs are facing */
	public @Nullable Double direction;

	/** radius of the post. Will be ignored if the sign is attached to something else. */
	public final double postRadius;

	private AttachmentConnector connector = null;

	public TrafficSignGroup(MapNode node, O2WConfig config) {
		// TODO remove this constructor and make the class immutable
		this(node, null, null, null, config);
	}

	public TrafficSignGroup(MapNode node, List<TrafficSignModel> signs, VectorXZ position, @Nullable Double direction,
			O2WConfig config) {
		super(node);
		this.signs = signs;
		this.position = position;
		this.direction = direction;
		this.postRadius = config.getDouble("standardPoleRadius", 0.05);
	}

	@Override
	public GroundState getGroundState() {
		if (connector != null && connector.isAttached()) {
			return ATTACHED;
		} else {
			return ON;
		}
	}

	@Override
	public Iterable<AttachmentConnector> getAttachmentConnectors() {

		List<String> types = AttachmentUtil.getCompatibleSurfaceTypes(node);

		if (types.isEmpty()) {
			return emptyList();
		} else {

			if (connector == null) {

				if (position == null) throw new IllegalStateException();

				double height = parseHeight(node.getTags(), (float) signs.get(0).defaultHeight);

				Predicate<VectorXYZ> isAcceptableNormal = null;
				if (direction != null) {
					isAcceptableNormal = (v -> angleBetween(v.xz(), VectorXZ.fromAngle(direction)) < PI / 8);
				}

				connector = new AttachmentConnector(types, position.xyz(height), this, height, true, isAcceptableNormal);

			}

			return singleton(connector);
		}

	}

	@Override
	public List<Mesh> buildMeshes() {
		return buildMeshesAndSubModels().getLeft();
	}

	@Override
	public List<ModelInstance> getSubModels() {
		return buildMeshesAndSubModels().getRight();
	}

	private Pair<List<Mesh>, List<ModelInstance>> buildMeshesAndSubModels() {

		List<Mesh> resultMeshes = emptyList();
		List<ModelInstance> resultInstances = new ArrayList<>();

		double height = parseHeight(node.getTags(), (float) signs.get(0).defaultHeight);
		double width = signs.get(0).getSignWidth();

		VectorXYZ basePositionXYZ = position.xyz(getBase().y);

		VectorXYZ signPositionXYZ = basePositionXYZ.addY(height);
		double direction = this.direction != null ? this.direction : PI;
		boolean drawPosts = true;

		if (connector != null && connector.isAttached()) {
			signPositionXYZ = connector.getAttachedPos();
			direction = connector.getAttachedSurfaceNormal().xz().angle();
			drawPosts = false;
		}

		/* render the post(s) */

		if (drawPosts) {

			int numPosts = signs.get(0).numPosts;

			for (int i = 0; i < numPosts; i++) {

				double relativePosition = 0.5 - (i + 1) / (double) (numPosts + 1);
				VectorXYZ position = basePositionXYZ.add(X_UNIT.mult(relativePosition * width));

				if (numPosts > 1) {
					position = position.rotateVec(direction, basePositionXYZ, Y_UNIT);
				}

				ExtrusionGeometry geometry = ExtrusionGeometry.createColumn(null, position, height,
						postRadius, postRadius, false, true, null, STEEL.get().textureDimensions());
				resultMeshes = List.of(new Mesh(geometry, STEEL.get(), LOD3, LOD4));

			}

		}

		/* render the signs */

		double distanceFromPost = 0.01; // desirable to avoid clipping between the pole and sign
		double distanceBetweenSigns = 0.1;

		double upperHeight = 0;

		for (TrafficSignModel sign : signs) {

			double signHeight = sign.getSignHeight();

			VectorXYZ position = signPositionXYZ.addY(upperHeight - signHeight / 2);
			position = position.add(VectorXZ.fromAngle(direction).mult((drawPosts ? postRadius : 0) + distanceFromPost));

			resultInstances.add(new ModelInstance(sign, new InstanceParameters(position, direction)));

			upperHeight -= signHeight + distanceBetweenSigns;

		}

		return Pair.of(resultMeshes, resultInstances);

	}

}
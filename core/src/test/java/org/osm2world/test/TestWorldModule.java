package org.osm2world.test;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.osm2world.output.common.material.Materials.PLASTIC;

import java.util.List;

import javax.annotation.Nullable;

import org.osm2world.map_data.data.MapNode;
import org.osm2world.map_elevation.data.GroundState;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.TriangleXYZ;
import org.osm2world.output.common.mesh.Mesh;
import org.osm2world.output.frontend_pbf.FrontendPbf.WorldObject;
import org.osm2world.world.data.NoOutlineNodeWorldObject;
import org.osm2world.world.data.ProceduralWorldObject;
import org.osm2world.world.modules.common.AbstractModule;

/**
 * a world module for unit tests that produces simple and predictable {@link WorldObject}s
 */
public class TestWorldModule extends AbstractModule {

	@Override
	protected void applyToNode(MapNode node) {
		node.addRepresentation(new TestNodeWorldObject(node));
	}

	public static class TestNodeWorldObject extends NoOutlineNodeWorldObject implements ProceduralWorldObject {

		private final @Nullable Mesh mesh;

		public TestNodeWorldObject(MapNode node, @Nullable Mesh mesh) {
			super(node);
			this.mesh = mesh;
		}

		public TestNodeWorldObject(MapNode node) {
			this(node, null);
		}

		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}

		@Override
		public void buildMeshesAndModels(Target target) {

			VectorXYZ base = node.getPos().xyz(0);

			TriangleXYZ triangle = new TriangleXYZ(base, base.add(0, 1, 0), base.add(1, 1, 0));
			target.drawTriangles(PLASTIC, singletonList(triangle), emptyList());

		}

		@Override
		public List<Mesh> buildMeshes() {
			if (mesh == null) {
				return ProceduralWorldObject.super.buildMeshes();
			} else {
				return singletonList(mesh);
			}
		}
	}

}

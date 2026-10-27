package org.osm2world.world.connectors;

import static java.util.Collections.emptyList;
import static org.junit.Assert.*;
import static org.osm2world.math.algorithms.GeometryUtil.closeLoop;
import static org.osm2world.test.TestUtil.assertAlmostEquals;

import java.util.Collection;
import java.util.List;

import org.junit.Test;
import org.osm2world.map_data.data.MapElement;
import org.osm2world.math.VectorXYZ;
import org.osm2world.math.VectorXZ;
import org.osm2world.math.shapes.FaceXYZ;
import org.osm2world.scene.mesh.Mesh;
import org.osm2world.world.connectors.AttachmentSurface.AttachmentPoint;
import org.osm2world.world.connectors.Connector.AttachmentProperties;
import org.osm2world.world.data.WorldObject;

public class AttachmentSurfaceTest {

	/** a horizontal triangle at the given elevation */
	private static FaceXYZ horizontalFace(double y) {
		return new FaceXYZ(closeLoop(
				new VectorXYZ(-1, y, 0),
				new VectorXYZ(+1, y, 0),
				new VectorXYZ(0, y, 2)));
	}

	@Test
	public void testFindAttachmentPoint() {

		var surface = new AttachmentSurface(List.of("test"), null, List.of(horizontalFace(5)));
		var ap = new AttachmentProperties(List.of("test"), 0, false);

		AttachmentPoint point = surface.findAttachmentPoint(ap, new VectorXYZ(0, 2, 1));

		assertNotNull(point);
		assertAlmostEquals(0, 5, 1, point.pos());
		assertAlmostEquals(VectorXYZ.Y_UNIT, point.normal());

	}

	@Test
	public void testFindAttachmentPoint_tooFar() {

		var surface = new AttachmentSurface(List.of("test"), null, List.of(horizontalFace(5)));
		var ap = new AttachmentProperties(List.of("test"), 0, false);

		assertNull(surface.findAttachmentPoint(ap, new VectorXYZ(50, 2, 1)));

	}

	@Test
	public void testUpdateGeometryFrom() {

		var surface = new AttachmentSurface(List.of("test"), null, List.of(horizontalFace(5)));

		var connector = new Connector(new VectorXYZ(0, 2, 1), null, GroundState.ATTACHED,
				new AttachmentProperties(List.of("test"), 0, false), null);
		AttachmentPoint point = surface.findAttachmentPoint(connector.attachmentProperties, connector.originalPos);
		connector.attach(surface, point.pos(), point.normal());

		assertEquals(5, surface.getBaseEleAt(VectorXZ.NULL_VECTOR), 1e-6);

		surface.updateGeometryFrom(new AttachmentSurface(List.of("test"), null, List.of(horizontalFace(15))));

		assertEquals(15, surface.getBaseEleAt(VectorXZ.NULL_VECTOR), 1e-6);
		assertEquals(15, surface.getFaces().iterator().next().vertices().get(0).y, 1e-6);
		assertEquals(List.of(connector), List.copyOf(surface.getAttachedConnectors()));
		assertSame(surface, connector.getAttachedSurface());

	}

	@Test(expected = IllegalArgumentException.class)
	public void testUpdateGeometryFrom_differentTypes() {
		var surface = new AttachmentSurface(List.of("test"), null, List.of(horizontalFace(5)));
		surface.updateGeometryFrom(new AttachmentSurface(List.of("other"), null, List.of(horizontalFace(5))));
	}

	@Test
	public void testDefaultUpdateAttachmentSurfaces() {

		/* an object which creates a new surface on every call, at an elevation that can change */

		class MovableObject implements WorldObject {

			double ele = 0;

			@Override public List<Mesh> buildMeshes() { return emptyList(); }
			@Override public MapElement getPrimaryMapElement() { return null; }
			@Override public Iterable<Connector> getConnectors() { return emptyList(); }

			@Override
			public Collection<AttachmentSurface> getAttachmentSurfaces() {
				return List.of(new AttachmentSurface(List.of("test"), this, List.of(horizontalFace(ele))));
			}

		}

		var object = new MovableObject();
		List<AttachmentSurface> surfaces = List.copyOf(object.getAttachmentSurfaces());

		object.ele = 10;
		object.updateAttachmentSurfaces(surfaces);

		assertEquals(10, surfaces.get(0).getBaseEleAt(VectorXZ.NULL_VECTOR), 1e-6);

	}

}

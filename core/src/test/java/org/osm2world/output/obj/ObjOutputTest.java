package org.osm2world.output.obj;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.awt.*;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.osm2world.math.VectorXYZ;
import org.osm2world.output.common.material.ImmutableMaterial;
import org.osm2world.output.common.material.Material;
import org.osm2world.output.common.mesh.Mesh;
import org.osm2world.output.common.mesh.TriangleGeometry;

public class ObjOutputTest {

	@Test
	public void testSimpleGeometry() {

		var objWriter = new StringWriter();
		var mtlWriter = new StringWriter();

		var target = new ObjOutput(objWriter, mtlWriter, null, null);

		var geometryBuilder = new TriangleGeometry.Builder(0, null, Material.Interpolation.FLAT);
		geometryBuilder.addTriangleStrip(List.of(
				new VectorXYZ(0, 0, 0),
				new VectorXYZ(1, 0, 0),
				new VectorXYZ(0, 0, 1),
				new VectorXYZ(1, 0, 1),
				new VectorXYZ(0, 0, 2),
				new VectorXYZ(1, 0, 2)));

		var testMesh = new Mesh(geometryBuilder.build(), new ImmutableMaterial(Material.Interpolation.FLAT, Color.RED));

		target.drawMesh(testMesh);

		String obj = objWriter.toString();
		String mtl = mtlWriter.toString();

		assertEquals(1, obj.lines().filter(it -> it.startsWith("usemtl ")).count());
		assertEquals(6, obj.lines().filter(it -> it.startsWith("v ")).count());
		assertEquals(4, obj.lines().filter(it -> it.startsWith("f ")).count());

		assertTrue(obj.lines().anyMatch(it ->
				Arrays.equals(it.split("\\s+"), new String[] {"v", "1.0", "0.0", "-2.0"})));

		assertEquals(1, mtl.lines().filter(it -> it.startsWith("newmtl ")).count());

	}

}

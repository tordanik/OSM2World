package org.osm2world.core.target.common;

import static java.util.Collections.emptyList;
import static org.osm2world.core.math.GeometryUtil.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.Renderable;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.data.WorldObject;

/**
 * superclass for {@link Target} implementations that defines some
 * of the required methods using others. Extending it reduces the number of
 * methods that have to be provided by the implementation
 */
public abstract class AbstractTarget<R extends Renderable>
		implements Target<R> {
	
	protected Configuration config;
	
	@Override
	public void setConfiguration(Configuration config) {
		this.config = config;
	}
	
	@Override
	public void beginObject(WorldObject object) {}
	
	@Override
	public void drawBox(Material material, VectorXYZ frontLowerLeft,
			VectorXYZ rightVector, VectorXYZ upVector, VectorXYZ backVector) {
		
		final VectorXYZ frontLowerRight = frontLowerLeft.add(rightVector);
		final VectorXYZ frontUpperLeft  = frontLowerLeft.add(upVector);
		final VectorXYZ frontUpperRight = frontLowerRight.add(upVector);
		
		final VectorXYZ backLowerLeft   = frontLowerLeft.add(backVector);
		final VectorXYZ backLowerRight  = frontLowerRight.add(backVector);
		final VectorXYZ backUpperLeft   = frontUpperLeft.add(backVector);
		final VectorXYZ backUpperRight  = frontUpperRight.add(backVector);
		
		drawTriangleStrip(material,
				frontLowerLeft, frontLowerRight,
				frontUpperLeft, frontUpperRight,
				backUpperLeft, backUpperRight,
				backLowerLeft, backLowerRight);
		
		drawTriangleStrip(material,
				backUpperRight, frontUpperRight,
				backLowerRight, frontLowerRight,
				backLowerLeft, frontLowerLeft,
				backUpperLeft, frontUpperLeft);
		
	}
	
	@Override
	public void drawBox(Material material, VectorXYZ bottomCenter,
			VectorXZ backDirection, double height, double width, double depth) {
		
		VectorXYZ backVector = backDirection.mult(depth).xyz(0);
		VectorXYZ rightVector = backDirection.rightNormal().mult(width).xyz(0);
		VectorXYZ upVector = VectorXYZ.Y_UNIT.mult(height);
		
		drawBox(material,
				bottomCenter
					.add(rightVector.mult(-0.5))
					.add(backVector.mult(-0.5)),
				rightVector, upVector, backVector);
		
	}
	
	@Override
	public void drawBox(Material material,
			VectorXYZ frontLowerLeft, VectorXYZ frontLowerRight,
			VectorXYZ frontUpperLeft, VectorXYZ frontUpperRight,
			VectorXYZ backLowerLeft, VectorXYZ backLowerRight,
			VectorXYZ backUpperLeft, VectorXYZ backUpperRight) {
		
		drawTriangleStrip(material,
				frontLowerLeft, frontLowerRight,
				frontUpperLeft, frontUpperRight,
				backUpperLeft, backUpperRight,
				backLowerLeft, backLowerRight);
		
		drawTriangleStrip(material,
				backUpperRight, frontUpperRight,
				backLowerRight, frontLowerRight,
				backLowerLeft, frontLowerLeft,
				backUpperLeft, frontUpperLeft);
		
	}

	private static final int EDGES_FOR_CYLINDER = 16;
	
	@Override
	public void drawColumn(Material material, Integer corners, VectorXYZ base,
			double height, double radiusBottom, double radiusTop,
			boolean drawBottom, boolean drawTop) {
		
		if (corners == null) {
			corners = EDGES_FOR_CYLINDER;
			material = material.makeSmooth();
		}

		float angleInterval = (float) (2 * Math.PI / corners);

		/* prepare vector lists for the 3 primitives */
		
		List<VectorXYZ> bottomFan = new ArrayList<VectorXYZ>(corners + 2);
		List<VectorXYZ> topFan = new ArrayList<VectorXYZ>(corners + 2);
		List<VectorXYZ> mantleStrip = new ArrayList<VectorXYZ>(corners + 2);
		
		/* fill vectors into lists */
		
		bottomFan.add(base);
		topFan.add(base.add(0, height, 0));
		
		for (int i = 0; i <= corners; i++) {
			
			double angle = - i * angleInterval;
			double sin = Math.sin(angle);
			double cos = Math.cos(angle);
			
			VectorXYZ topV = base.add(
					radiusTop * sin, height, radiusTop * cos);
			VectorXYZ bottomV = base.add(
					radiusBottom * sin, 0, radiusBottom * cos);

			bottomFan.add(bottomV);
			topFan.add(topV);

			mantleStrip.add(topV);
			mantleStrip.add(bottomV);
			
		}
		
		Collections.reverse(bottomFan);
		
		/* draw the 3 primitives */

		if (drawBottom) { drawTriangleFan(material, bottomFan); }
		if (drawTop) { drawTriangleFan(material, topFan); }
		drawTriangleStrip(material, mantleStrip);
		
	}

	@Override
	public void drawTriangles(Material material,
			Collection<? extends TriangleXYZ> triangles) {
		drawTriangles(material, triangles,
				Collections.<List<VectorXZ>>emptyList());
	}
	
	@Override
	public void drawTriangleStrip(Material material, VectorXYZ... vectors) {
		drawTriangleStrip(material, Arrays.asList(vectors));
	}
	
	@Override
	public void drawTriangleStrip(Material material, List<? extends VectorXYZ> vs) {
		drawTriangleStrip(material, vs,
				Collections.<List<VectorXZ>>emptyList());
	}
	
	@Override
	public void drawTriangleStrip(Material material, List<? extends VectorXYZ> vs,
			List<List<VectorXZ>> textureCoordLists) {
		
		List<List<VectorXZ>> newTexCoordLists = emptyList();
		if (!textureCoordLists.isEmpty()) {
			newTexCoordLists = new ArrayList<List<VectorXZ>>(textureCoordLists.size());
			for (List<VectorXZ> texCoordList : textureCoordLists) {
				newTexCoordLists.add(
						triangleVertexListFromTriangleStrip(texCoordList));
			}
		}
				
		drawTriangles(material, trianglesFromTriangleStrip(vs), newTexCoordLists);
	}
	
	@Override
	public void drawTriangleFan(Material material, List<? extends VectorXYZ> vs) {
		drawTriangleFan(material, vs,
				Collections.<List<VectorXZ>>emptyList());
	}
	
	@Override
	public void drawTriangleFan(Material material, List<? extends VectorXYZ> vs,
			List<List<VectorXZ>> textureCoordLists) {
		
		List<List<VectorXZ>> newTexCoordLists = emptyList();
		if (!textureCoordLists.isEmpty()) {
			newTexCoordLists = new ArrayList<List<VectorXZ>>(textureCoordLists.size());
			for (List<VectorXZ> texCoordList : textureCoordLists) {
				newTexCoordLists.add(
						triangleVertexListFromTriangleFan(texCoordList));
			}
		}
		
		drawTriangles(material, trianglesFromTriangleFan(vs), newTexCoordLists);
		
	}
	
	@Override
	public void drawPolygon(Material material, VectorXYZ... vs) {
		drawTriangleFan(material, Arrays.asList(vs));
	}
	
	@Override
	public void finish() {}
	
}

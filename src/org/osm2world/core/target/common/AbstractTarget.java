package org.osm2world.core.target.common;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.osm2world.core.math.GeometryUtil.*;
import static org.osm2world.core.world.modules.common.WorldModuleTexturingUtil.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.configuration.Configuration;
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
	public void drawBox(Material material,
			VectorXYZ bottomCenter, VectorXZ faceDirection,
			double height, double width, double depth) {
		
		final VectorXYZ backVector = faceDirection.mult(-depth).xyz(0);
		final VectorXYZ rightVector = faceDirection.rightNormal().mult(-width).xyz(0);
		final VectorXYZ upVector = new VectorXYZ(0, height, 0);
		
		final VectorXYZ frontLowerLeft = bottomCenter
				.add(rightVector.mult(-0.5))
				.add(backVector.mult(-0.5));
		
		final VectorXYZ frontLowerRight = frontLowerLeft.add(rightVector);
		final VectorXYZ frontUpperLeft  = frontLowerLeft.add(upVector);
		final VectorXYZ frontUpperRight = frontLowerRight.add(upVector);
		
		final VectorXYZ backLowerLeft   = frontLowerLeft.add(backVector);
		final VectorXYZ backLowerRight  = frontLowerRight.add(backVector);
		final VectorXYZ backUpperLeft   = frontUpperLeft.add(backVector);
		final VectorXYZ backUpperRight  = frontUpperRight.add(backVector);
		
		List<VectorXYZ> vsStrip1 = asList(
				backLowerLeft, backLowerRight,
				frontLowerLeft, frontLowerRight,
				frontUpperLeft, frontUpperRight,
				backUpperLeft, backUpperRight
		);
		
		List<VectorXYZ> vsStrip2 = asList(
				frontUpperRight, frontLowerRight,
				backUpperRight, backLowerRight,
				backUpperLeft, backLowerLeft,
				frontUpperLeft, frontLowerLeft
		);
		
		List<List<VectorXZ>> texCoords1 = null, texCoords2 = null;
		
		if (material.getTextureDataList() != null) {
			texCoords1 = nCopies(material.getTextureDataList().size(), BOX_TEX_COORDS_1);
			texCoords2 = nCopies(material.getTextureDataList().size(), BOX_TEX_COORDS_2);
		}
		
		drawTriangleStrip(material, vsStrip1, texCoords1);
		drawTriangleStrip(material, vsStrip2, texCoords2);
		
	}
	
	protected static final List<VectorXZ> BOX_TEX_COORDS_1 = asList(
		new VectorXZ(0,     0), new VectorXZ(0.25,     0),
		new VectorXZ(0, 1.0/3), new VectorXZ(0.25, 1.0/3),
		new VectorXZ(0, 2.0/3), new VectorXZ(0.25, 2.0/3),
		new VectorXZ(0,     1), new VectorXZ(0.25,     1)
	);
	
	protected static final List<VectorXZ> BOX_TEX_COORDS_2 = asList(
		new VectorXZ(0.25, 2.0/3), new VectorXZ(0.25, 1.0/3),
		new VectorXZ(0.50, 2.0/3), new VectorXZ(0.50, 1.0/3),
		new VectorXZ(0.75, 2.0/3), new VectorXZ(0.75, 1.0/3),
		new VectorXZ(1.00, 2.0/3), new VectorXZ(1.00, 1.0/3)
	);

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

		if (drawBottom) { drawTriangleFan(material, bottomFan,
				globalTexCoordLists(bottomFan, material, false)); }
		
		if (drawTop) { drawTriangleFan(material, topFan,
				globalTexCoordLists(topFan, material, false)); }
		
		drawTriangleStrip(material, mantleStrip,
				wallTexCoordLists(mantleStrip, material));
		
	}
	
	@Override
	public void drawTriangleStrip(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {
		
		List<List<VectorXZ>> newTexCoordLists = emptyList();
		if (texCoordLists != null && !texCoordLists.isEmpty()) {
			newTexCoordLists = new ArrayList<List<VectorXZ>>(texCoordLists.size());
			for (List<VectorXZ> texCoordList : texCoordLists) {
				newTexCoordLists.add(
						triangleVertexListFromTriangleStrip(texCoordList));
			}
		}
				
		drawTriangles(material, trianglesFromTriangleStrip(vs), newTexCoordLists);
	}
	
	@Override
	public void drawTriangleFan(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {
		
		List<List<VectorXZ>> newTexCoordLists = emptyList();
		if (texCoordLists != null && !texCoordLists.isEmpty()) {
			newTexCoordLists = new ArrayList<List<VectorXZ>>(texCoordLists.size());
			for (List<VectorXZ> texCoordList : texCoordLists) {
				newTexCoordLists.add(
						triangleVertexListFromTriangleFan(texCoordList));
			}
		}
		
		drawTriangles(material, trianglesFromTriangleFan(vs), newTexCoordLists);
		
	}
	
	@Override
	public void drawConvexPolygon(Material material, List<VectorXYZ> vs,
			List<List<VectorXZ>> texCoordLists) {
		drawTriangleFan(material, vs, texCoordLists);
	}
	
	@Override
	public void finish() {}
	
}

package org.osm2world.core.target.frontend_pbf;

import static java.lang.Math.round;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.ConversionFacade;
import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.common.AbstractTarget;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Tile;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Triangle3d;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Vector3dBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.WorldObject;

public class FrontendPbfTarget extends AbstractTarget<RenderableToAllTargets> {

	private final OutputStream outputStream;

	private final List<VectorXYZ> vector3dBlock = new ArrayList<VectorXYZ>();
	private final List<WorldObject> objects = new ArrayList<FrontendPbf.WorldObject>();

	private WorldObject.Builder currentObjectBuilder = WorldObject.newBuilder();

	public FrontendPbfTarget(OutputStream outputStream) {
		this.outputStream = outputStream;
	}

	@Override
	public Class<RenderableToAllTargets> getRenderableType() {
		return RenderableToAllTargets.class;
	}

	@Override
	public void render(RenderableToAllTargets renderable) {
		renderable.renderTo(this);
	}

	@Override
	public void beginObject(org.osm2world.core.world.data.WorldObject object) {

		/* build the previous object */

		if (currentObjectBuilder.getTrianglesCount() > 0) {
			objects.add(currentObjectBuilder.build());
		}

		/* start a new object */

		currentObjectBuilder = WorldObject.newBuilder();

		/* find and use the actual OSM id of the new object, if possible */

		MapElement element = object.getPrimaryMapElement();

		if (element != null) {

			if (element instanceof MapArea) {
				currentObjectBuilder.setOsmId(((MapArea)element).getOsmObject().toString());
			} else if (element instanceof MapWaySegment) {
				currentObjectBuilder.setOsmId(((MapWaySegment)element).getOsmWay().toString());
			} else if (element instanceof MapNode) {
				currentObjectBuilder.setOsmId(((MapNode)element).getOsmNode().toString());
			}

		}

	}

	@Override
	public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists) {

		for (TriangleXYZ triangle : triangles) {

			Triangle3d.Builder triangleBuilder = Triangle3d.newBuilder();

			/* set the vertices */

			triangleBuilder.setV1(toIndex(triangle.v1));
			triangleBuilder.setV2(toIndex(triangle.v2));
			triangleBuilder.setV3(toIndex(triangle.v3));

			/* set material properties */

			triangleBuilder.setMaterial(convertMaterial(material));

			/* build the triangle */

			currentObjectBuilder.addTriangles(triangleBuilder.build());

		}

	}

	@Override
	public void drawTrianglesWithNormals(Material material, Collection<? extends TriangleXYZWithNormals> triangles,
			List<List<VectorXZ>> texCoordLists) {
		drawTriangles(material, triangles, texCoordLists);
	}

	private long toIndex(VectorXYZ v) {

		long index = vector3dBlock.indexOf(v);

		if (index < 0) {
			vector3dBlock.add(v);
			index = vector3dBlock.size() - 1;
		}

		return index;

	}

	private FrontendPbf.Material convertMaterial(Material material) {

		FrontendPbf.Material.Builder materialBuilder = FrontendPbf.Material.newBuilder();

		materialBuilder.setAmbientR(material.ambientColor().getRed());
		materialBuilder.setAmbientG(material.ambientColor().getGreen());
		materialBuilder.setAmbientB(material.ambientColor().getBlue());

		materialBuilder.setDiffuseR(material.diffuseColor().getRed());
		materialBuilder.setDiffuseG(material.diffuseColor().getGreen());
		materialBuilder.setDiffuseB(material.diffuseColor().getBlue());

		return materialBuilder.build();

	}

	@Override
	public void finish() {

		/* build the last object */

		if (currentObjectBuilder.getTrianglesCount() > 0) {
			objects.add(currentObjectBuilder.build());
		}

		/* build the tile */

		Vector3dBlock.Builder vector3dBlockBuilder = Vector3dBlock.newBuilder();

		for (VectorXYZ v : vector3dBlock) {
			vector3dBlockBuilder.addCoord(round(v.x*1000));
			vector3dBlockBuilder.addCoord(round(v.y*1000));
			vector3dBlockBuilder.addCoord(round(v.z*1000));
		}

		Tile.Builder tileBuilder = Tile.newBuilder();
		tileBuilder.setVector3DBlock(vector3dBlockBuilder);
		tileBuilder.addAllObjects(objects);

		/* write the protobuf */

		try {
			tileBuilder.build().writeTo(outputStream);
		} catch (IOException e) {
			//TODO proper error handling
			throw new Error(e);
		}

	}

	public static void main(String[] args) throws IOException {

		ConversionFacade facade = new ConversionFacade();

		File osmFile = new File("/home/tk/pbfTest.osm");
		FrontendPbfTarget target = new FrontendPbfTarget(new FileOutputStream("/home/tk/pbfTest.o2w.pbf"));

		List<FrontendPbfTarget> targets = singletonList(target);
		facade.createRepresentations(osmFile, null, null, targets);

	}

}

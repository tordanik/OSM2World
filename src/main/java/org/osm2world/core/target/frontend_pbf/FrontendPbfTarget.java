package org.osm2world.core.target.frontend_pbf;

import static java.lang.Math.round;
import static java.util.Collections.*;
import static org.osm2world.core.math.VectorXYZ.Y_UNIT;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapData;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.math.AxisAlignedBoundingBoxXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXYZWithNormals;
import org.osm2world.core.math.Vector3D;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.AbstractTarget;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Shadow;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Material.TextureLayer;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Material.TextureLayer.Wrap;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Material.Transparency;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.MaterialBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Tile;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.TriangleGeometry;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Vector2dBlock;
import org.osm2world.core.target.frontend_pbf.FrontendPbf.Vector3dBlock;
import org.osm2world.core.world.data.WorldObject;

import com.google.common.collect.ComparisonChain;

public class FrontendPbfTarget extends AbstractTarget<RenderableToAllTargets> {

	/**
	 * a block containing all elements of a particular type.
	 * Elements are then referenced by their index (i.e. position) in the block.
	 * This saves space in the resulting protobuf file
	 * because identical elements only need to be transmitted once.
	 */
	static interface Block<T> {

		public List<T> getElements();

		/** adds the element to the block if necessary, and returns its index */
		public int toIndex(T element);

	}

	/**
	 * simple implementation of {@link Block}.
	 * Works for any content type, but slow for large numbers of elements.
	 */
	static class SimpleBlock<T> implements Block<T> {

		List<T> elements = new ArrayList<T>();

		public List<T> getElements() {
			return elements;
		}

		/** adds the element to the block if necessary, and returns its index */
		public int toIndex(T element) {

			int index = elements.indexOf(element);

			if (index < 0) {
				elements.add(element);
				index = elements.size() - 1;
			}

			return index;

		}

	}

	/**
	 * implementation of {@link Block} that's optimized for {@link Vector3D} instances.
	 */
	static class VectorBlock<T extends Vector3D> implements Block<T> {

		private static class Entry<T extends Vector3D> implements Comparable<Entry<T>> {

			private final T element;
			private final int index;

			public Entry(T element, int index) {
				this.element = element;
				this.index = index;
			}

			@Override
			public int compareTo(Entry<T> o) {
				return ComparisonChain.start()
						.compare(this.element.getX(), o.element.getX())
						.compare(this.element.getY(), o.element.getY())
						.compare(this.element.getZ(), o.element.getZ())
						.result();
			}

		}

		List<T> elements = new ArrayList<T>();
		List<Entry<T>> sortedEntrys = new ArrayList<Entry<T>>();

		public List<T> getElements() {
			return elements;
		}

		/** adds the element to the block if necessary, and returns its index */
		public int toIndex(T element) {

			int index = binarySearch(sortedEntrys, new Entry<T>(element, -1));

			if (index >= 0) {
				return sortedEntrys.get(index).index;
			} else {

				elements.add(element);

				Entry<T> entry = new Entry<T>(element, elements.size() - 1);
				sortedEntrys.add(-(index + 1), entry);

				return entry.index;

			}

		}

	}

	private static class TriangleData {

		private final List<VectorXYZ> vertices;
		private final List<VectorXYZ> normals;
		private final List<List<VectorXZ>> texCoordLists;

		public TriangleData(int numTextureLayers) {

			vertices = new ArrayList<VectorXYZ>();
			normals = new ArrayList<VectorXYZ>();

			texCoordLists = new ArrayList<List<VectorXZ>>(numTextureLayers);

			for (int i = 0; i < numTextureLayers; i++) {
				texCoordLists.add(new ArrayList<VectorXZ>());
			}

		}

		public void add(List<VectorXYZ> vs,
				List<VectorXYZ> normals,
				List<List<VectorXZ>> texCoordLists) {

			assert (vs.size() == normals.size());
			assert (texCoordLists.size() == this.texCoordLists.size());

			this.vertices.addAll(vs);
			this.normals.addAll(normals);

			for (int i = 0; i < texCoordLists.size(); i++) {

				assert (vs.size() == texCoordLists.get(i).size());

				this.texCoordLists.get(i).addAll(texCoordLists.get(i));

			}

		}

		public List<VectorXYZ> getVertices() {
			return unmodifiableList(vertices);
		}

		public List<VectorXYZ> getNormals() {
			return unmodifiableList(normals);
		}

		public List<List<VectorXZ>> getTexCoordLists() {
			return unmodifiableList(texCoordLists);
		}

	}

	/** prefix for the URL of texture files */
	private static final String TEXTURE_BASE_URL = "textures/";

	/** factor applied to coordinate values before rounding to integers */
	private final int COORD_PRECISION_FACTOR = 1000;

	private final OutputStream outputStream;
	private final AxisAlignedBoundingBoxXZ bbox;

	private final Block<VectorXYZ> vector3dBlock = new VectorBlock<VectorXYZ>();
	private final Block<VectorXZ> vector2dBlock = new VectorBlock<VectorXZ>();
	private final Block<Material> materialBlock = new SimpleBlock<Material>();

	private final List<FrontendPbf.WorldObject> objects = new ArrayList<FrontendPbf.WorldObject>();

	private WorldObject currentObject = null;
	private Map<Material, TriangleData> currentTriangles = new HashMap<Material, TriangleData>();

	/**
	 *
	 * @param outputStream
	 * @param bbox  the desired bounding box for the output.
	 *              Objects are part of the output if their center is inside this box.
	 */
	public FrontendPbfTarget(OutputStream outputStream, AxisAlignedBoundingBoxXZ bbox) {

		this.outputStream = outputStream;
		this.bbox = bbox;

		/* initialize the vector blocks with frequently used values,
		 * to make sure these have low indices (= more compact varints) */

		//TODO implement, then check size differences

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
	public void beginObject(WorldObject object) {

		/* build the previous object (if it's inside the bounding box) */

		boolean isInsideBbox = true;

		if (currentObject != null && currentObject.getPrimaryMapElement() != null) {

			MapElement mapElement = currentObject.getPrimaryMapElement();

			VectorXZ center = null;

			if (mapElement instanceof MapNode) {
				center = ((MapNode) mapElement).getPos();
			} else if (mapElement instanceof MapWaySegment) {
				center = ((MapWaySegment) mapElement).getCenter();
			} else if (mapElement instanceof MapArea) {
				center = ((MapArea) mapElement).getOuterPolygon().getCenter();
			}

			isInsideBbox = bbox.contains(center);

		}

		if (!currentTriangles.isEmpty() && isInsideBbox) {
			objects.add(buildCurrentObject());
		}

		/* start a new object */

		currentObject = object;
		currentTriangles = new HashMap<Material, FrontendPbfTarget.TriangleData>();

	}

	@Override
	public void drawTriangles(Material material, Collection<? extends TriangleXYZ> triangles,
			List<List<VectorXZ>> texCoordLists) {

		TriangleData triangleData;

		if (currentTriangles.containsKey(material)) {
			triangleData = currentTriangles.get(material);
		} else {
			triangleData = new TriangleData(material.getTextureDataList().size());
			currentTriangles.put(material, triangleData);
		}

		List<VectorXYZ> vertices = new ArrayList<VectorXYZ>(triangles.size() * 3);

		for (TriangleXYZ t : triangles) {
			vertices.addAll(t.getVertices());
		}

		triangleData.add(vertices ,
				nCopies(vertices.size(), Y_UNIT), //TODO replace with actual normals by extending PrimitiveTarget
				texCoordLists);

	}

	@Override
	public void drawTrianglesWithNormals(Material material, Collection<? extends TriangleXYZWithNormals> triangles,
			List<List<VectorXZ>> texCoordLists) {
		drawTriangles(material, triangles, texCoordLists);
	}

	private FrontendPbf.Material convertMaterial(Material material) {

		FrontendPbf.Material.Builder materialBuilder = FrontendPbf.Material.newBuilder();

		materialBuilder.setAmbientR(material.ambientColor().getRed());
		materialBuilder.setAmbientG(material.ambientColor().getGreen());
		materialBuilder.setAmbientB(material.ambientColor().getBlue());

		materialBuilder.setDiffuseR(material.diffuseColor().getRed());
		materialBuilder.setDiffuseG(material.diffuseColor().getGreen());
		materialBuilder.setDiffuseB(material.diffuseColor().getBlue());

		Color specularColor = Material.multiplyColor(
				material.getColor(), material.getSpecularFactor());

		materialBuilder.setSpecularR(specularColor.getRed());
		materialBuilder.setSpecularG(specularColor.getGreen());
		materialBuilder.setSpecularB(specularColor.getBlue());

		materialBuilder.setShininess(material.getShininess());

		switch (material.getTransparency()) {
			case TRUE: materialBuilder.setTransparency(Transparency.TRUE); break;
			case BINARY: materialBuilder.setTransparency(Transparency.BINARY); break;
			case FALSE: break; //default value – not setting it saves bandwidth in proto2
			default: throw new Error("unsupported transparency: " + material.getTransparency());
		}

		if (material.getShadow() == Shadow.FALSE) {
			materialBuilder.setCastShadow(false);
		}

		for (TextureData textureData : material.getTextureDataList()) {
			materialBuilder.addTextureLayer(convertTextureLayer(textureData));
		}

		return materialBuilder.build();

	}

	private TextureLayer convertTextureLayer(TextureData textureData) {

		TextureLayer.Builder layerBuilder = TextureLayer.newBuilder();

		layerBuilder.setTextureURL(TEXTURE_BASE_URL + textureData.file.getName());

		switch (textureData.wrap) {
			case CLAMP:
			case CLAMP_TO_BORDER: layerBuilder.setWrap(Wrap.CLAMP); break;
			case REPEAT: break; //default value – not setting it saves bandwidth in proto2
			default: throw new Error("unsupported transparency: " + textureData.wrap);
		}

		layerBuilder.setColorable(textureData.colorable);

		return layerBuilder.build();

	}

	/**
	 * builds the current {@link FrontendPbf.WorldObject},
	 * based e.g. on {@link #currentTriangles}.
	 */
	private FrontendPbf.WorldObject buildCurrentObject() {

		/* build the object's geometries */

		List<TriangleGeometry> triangleGeometries = new ArrayList<TriangleGeometry>();

		for (Material material : currentTriangles.keySet()) {

			TriangleGeometry.Builder geometryBuilder = TriangleGeometry.newBuilder();

			geometryBuilder.setMaterial(materialBlock.toIndex(material));

			TriangleData triangleData = currentTriangles.get(material);

			/* write the vertices */

			for (VectorXYZ v : triangleData.getVertices()) {
				geometryBuilder.addVertices(vector3dBlock.toIndex(v));
			}

			/* write the texture coordinates */

			List<VectorXZ> texCoords = new ArrayList<VectorXZ>();

			for (List<VectorXZ> list : triangleData.getTexCoordLists()) {
				texCoords.addAll(list);
			}

			for (VectorXZ v : texCoords) {
				geometryBuilder.addTexCoords(vector2dBlock.toIndex(v));
			}

			/* build the geometry */

			triangleGeometries.add(geometryBuilder.build());

		}

		/* build the actual object */

		FrontendPbf.WorldObject.Builder objectBuilder = FrontendPbf.WorldObject.newBuilder();

		if (currentObject != null) {

			MapElement element = currentObject.getPrimaryMapElement();

			if (element != null) {

				if (element instanceof MapArea) {
					objectBuilder.setOsmId(((MapArea)element).getOsmObject().toString());
				} else if (element instanceof MapWaySegment) {
					objectBuilder.setOsmId(((MapWaySegment)element).getOsmWay().toString());
				} else if (element instanceof MapNode) {
					objectBuilder.setOsmId(((MapNode)element).getOsmNode().toString());
				}

			}

		}

		if (triangleGeometries.isEmpty()) {
			//TODO proper error handling
			throw new Error("a WorldObject needs geometry");
		}

		objectBuilder.addAllTriangleGeometries(triangleGeometries);

		return objectBuilder.build();

	}

	@Override
	public void finish() {

		/* build the last object */

		objects.add(buildCurrentObject());

		/* build the blocks */

		Vector3dBlock.Builder vector3dBlockBuilder = Vector3dBlock.newBuilder();

		for (VectorXYZ v : vector3dBlock.getElements()) {
			vector3dBlockBuilder.addCoords(round(v.x*COORD_PRECISION_FACTOR));
			vector3dBlockBuilder.addCoords(round(v.y*COORD_PRECISION_FACTOR));
			vector3dBlockBuilder.addCoords(round(v.z*COORD_PRECISION_FACTOR));
		}

		Vector2dBlock.Builder vector2dBlockBuilder = Vector2dBlock.newBuilder();

		for (VectorXZ v : vector2dBlock.getElements()) {
			vector2dBlockBuilder.addCoords(round(v.x*COORD_PRECISION_FACTOR));
			vector2dBlockBuilder.addCoords(round(v.z*COORD_PRECISION_FACTOR));
		}

		MaterialBlock.Builder materialBlockBuilder = MaterialBlock.newBuilder();

		for (Material m : materialBlock.getElements()) {
			materialBlockBuilder.addMaterials(convertMaterial(m));
		}

		/* build the tile */

		Tile.Builder tileBuilder = Tile.newBuilder();

		tileBuilder.setVector3DBlock(vector3dBlockBuilder);
		tileBuilder.setVector2DBlock(vector2dBlockBuilder);
		tileBuilder.setMaterialBlock(materialBlockBuilder);

		tileBuilder.addAllObjects(objects);

		/* write the protobuf */

		try {
			tileBuilder.build().writeTo(outputStream);
		} catch (IOException e) {
			//TODO proper error handling
			throw new Error(e);
		}

	}

	public static void writePbfFile(File outputFile, MapData mapData) throws IOException {

		FileOutputStream output = null;

		try {

			output = new FileOutputStream(outputFile);

			writePbfStream(output, mapData);

		} finally {
			if (output != null) {
				output.close();
			}
		}

	}

	public static void writePbfStream(OutputStream output, MapData mapData) throws IOException {

		FrontendPbfTarget target = new FrontendPbfTarget(output, mapData.getBoundary());

		TargetUtil.renderWorldObjects(target, mapData, true);

		target.finish();

	}

}

package org.osm2world.core.world.modules.building.roof;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.osm2world.core.target.common.material.NamedTexCoordFunction.GLOBAL_X_Z;
import static org.osm2world.core.target.common.material.TexCoordUtil.triangleTexCoordLists;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.osm2world.core.map_data.data.TagSet;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonWithHolesXZ;
import org.osm2world.core.math.TriangleXYZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.math.algorithms.CAGUtil;
import org.osm2world.core.math.shapes.PolygonShapeXZ;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.world.attachment.AttachmentConnector;
import org.osm2world.core.world.attachment.AttachmentSurface;
import org.osm2world.core.world.data.TerrainBoundaryWorldObject;

public class FlatRoof extends HeightfieldRoof {

	protected AttachmentSurface attachmentSurface;

	public FlatRoof(PolygonWithHolesXZ originalPolygon, TagSet tags, Material material) {
		super(originalPolygon, tags, 0, material);
	}

	@Override
	public PolygonWithHolesXZ getPolygon() {
		return originalPolygon;
	}

	@Override
	public Collection<VectorXZ> getInnerPoints() {
		return emptyList();
	}

	@Override
	public Collection<LineSegmentXZ> getInnerSegments() {
		return emptyList();
	}

	@Override
	public Double getRoofHeightAt_noInterpolation(VectorXZ pos) {
		return 0.0;
	}

	@Override
	public Collection<AttachmentSurface> getAttachmentSurfaces(double baseEle, int level) {

		if (attachmentSurface == null) {
			AttachmentSurface.Builder builder = new AttachmentSurface.Builder("roof" + level, "roof", "floor" + level);
			this.renderTo(builder, baseEle);
			attachmentSurface = builder.build();
		}

		return singleton(attachmentSurface);

	}

	@Override
	public void renderTo(Target target, double baseEle) {

		/* subtract attached rooftop areas (parking, helipads, pools, etc.) from the roof polygon */

		List<PolygonShapeXZ> subtractPolys = new ArrayList<>();

		if (attachmentSurface != null) {
			for (AttachmentConnector connector : attachmentSurface.getAttachedConnectors()) {
				if (connector.object instanceof TerrainBoundaryWorldObject) {
					subtractPolys.addAll(((TerrainBoundaryWorldObject)connector.object).getTerrainBoundariesXZ());
				}
			}
		}

		Collection<? extends PolygonShapeXZ> polygons;

		if (subtractPolys.isEmpty()) {
			polygons = singleton(this.getPolygon());
		} else {
			subtractPolys.addAll(this.getPolygon().getHoles());
			polygons = CAGUtil.subtractPolygons(this.getPolygon().getOuter(), subtractPolys);
		}

		/* triangulate and render the (remaining) roof polygon */

		List<TriangleXZ> trianglesXZ = new ArrayList<>();
		for (PolygonShapeXZ polygon : polygons) {
			trianglesXZ.addAll(polygon.getTriangulation());
		}

		List<TriangleXYZ> trianglesXYZ = trianglesXZ.stream().map(t -> t.xyz(baseEle)).collect(toList());

		/* draw triangles */

		target.drawTriangles(material, trianglesXYZ,
				triangleTexCoordLists(trianglesXYZ, material, GLOBAL_X_Z));

	}

}

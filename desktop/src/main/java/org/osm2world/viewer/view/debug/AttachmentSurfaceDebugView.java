package org.osm2world.viewer.view.debug;

import static java.util.Collections.emptyList;
import static org.osm2world.scene.color.Color.ORANGE;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.osm2world.math.VectorXYZ;
import org.osm2world.math.shapes.FlatSimplePolygonShapeXYZ;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.ImmutableMaterial;
import org.osm2world.scene.material.Material;
import org.osm2world.scene.material.Material.Interpolation;
import org.osm2world.world.attachment.AttachmentSurface;
import org.osm2world.world.data.WorldObject;

public class AttachmentSurfaceDebugView extends StaticDebugView {

	private static final Color BASE_ELE_COLOR = ORANGE;

	private final Map<String, Color> surfaceTypeColors = new HashMap<>();

	public AttachmentSurfaceDebugView() {

		super("Attachment surfaces", "shows surfaces that other WorldObjects can attach themselves to");

		surfaceTypeColors.put("wall", new Color(1.0f, 1.0f, 0));
		surfaceTypeColors.put("roof", new Color(1.0f, 0.8f, 0.8f));

	}

	@Override
	protected void fillOutput(JOGLOutput output) {

		for (WorldObject object : scene.getWorldObjects()) {
			for (AttachmentSurface surface : object.getAttachmentSurfaces()) {

				String type = surface.getTypes().iterator().next();
				Color color = getOrCreateColor(type);

				for (FlatSimplePolygonShapeXYZ face : surface.getFaces()) {

					Material material = new ImmutableMaterial(Interpolation.FLAT, color);
					output.drawConvexPolygon(material, face.vertices(), emptyList());

					//draw base ele
					for (int i = 0; i < face.vertices().size() - 1; i++) {
						VectorXYZ v1 = face.vertices().get(i);
						VectorXYZ v2 = face.vertices().get(i + 1);
						v1 = v1.y(surface.getBaseEleAt(v1.xz()));
						v2 = v2.y(surface.getBaseEleAt(v2.xz()));
						output.drawLineStrip(BASE_ELE_COLOR, 2, v1, v2);
					}

				}

			}
		}

	}

	private Color getOrCreateColor(String surfaceType) {

		if (!surfaceTypeColors.containsKey(surfaceType)) {

			Random random = new Random(surfaceTypeColors.size());
			float h = random.nextFloat();
			float s = (random.nextInt(2000) + 1000) / 10000f;
			float b = 0.9f;
			final Color newColor = Color.getHSBColor(h, s, b);

			surfaceTypeColors.put(surfaceType, newColor);

		}

		return surfaceTypeColors.get(surfaceType);

	}

}

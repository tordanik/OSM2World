package org.osm2world.viewer.view.debug;

import static org.osm2world.math.VectorXYZ.addYList;
import static org.osm2world.world.modules.common.WorldModuleGeometryUtil.createTriangleStripBetween;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osm2world.math.shapes.PolylineShapeXZ;
import org.osm2world.math.shapes.PolylineXYZ;
import org.osm2world.math.shapes.PolylineXZ;
import org.osm2world.output.jogl.JOGLOutput;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.material.Material;
import org.osm2world.world.modules.BridgeModule;

public class BridgeDataDebugView extends StaticDebugView {

	public BridgeDataDebugView() {
		super("Bridge data",
				"draws internal results of bridge geometry calculations");
	}

	private record LineStyle(Color color, double height) {}

	@Override
	public void fillOutput(JOGLOutput output) {

		for (BridgeModule.Bridge<?> bridge : scene.getWorldObjects(BridgeModule.Bridge.class)) {

			Map<PolylineShapeXZ, LineStyle> lines = new HashMap<>();

			var centerline = bridge.getCenterline();
			if (centerline != null) {
				lines.put(centerline, new LineStyle(new Color(1f, 0f, 0f), 2));
			}

			for (PolylineXZ edge : bridge.getEdges()) {
				lines.put(edge, new LineStyle(new Color(1f, 1f, 0f), 2));
			}

			for (PolylineXZ cap : bridge.getCaps()) {
				lines.put(cap, new LineStyle(new Color(0.5f, 1f, 0f), 0));
			}

			for (var entry : lines.entrySet()) {
				
				Color color = entry.getValue().color;
				double height = entry.getValue().height;

				PolylineXZ line = new PolylineXZ(entry.getKey().vertices());
				PolylineXYZ lineXYZ = line.xyz(bridge::getBridgeEleAt);

				if (height > 0) {
					Material material = new Material(Material.Interpolation.FLAT, color).makeDoubleSided();
					output.drawTriangleStrip(material, createTriangleStripBetween(lineXYZ.getVertices(),
							addYList(lineXYZ.getVertices(), height)), List.of());
				} else {
					output.drawLineStrip(entry.getValue().color(), 1, lineXYZ.getVertices());
				}

			}

		}

	}

}

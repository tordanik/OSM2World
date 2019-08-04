package org.osm2world.viewer.model;

import java.util.HashSet;
import java.util.Set;

import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.NoneEleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.TerrainInterpolator;
import org.osm2world.core.map_elevation.creation.ZeroInterpolator;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.core.target.common.rendering.Projection;
import org.osm2world.viewer.view.debug.DebugView;

public class RenderOptions {

	public Camera camera = null;
	public Projection projection = Defaults.PERSPECTIVE_PROJECTION;

	public Set<DebugView> activeDebugViews = new HashSet<DebugView>();

	private boolean showGrid = true;
	private boolean showTerrain = true;
	private boolean wireframe = false;
	private boolean backfaceCulling = true;

	Class<? extends TerrainInterpolator> interpolatorClass = ZeroInterpolator.class;
	Class<? extends EleConstraintEnforcer> enforcerClass = NoneEleConstraintEnforcer.class;

	public boolean isShowGrid() {
		return showGrid;
	}
	public void setShowGrid(boolean showGrid) {
		this.showGrid = showGrid;
	}
	public boolean isShowTerrain() {
		return showTerrain;
	}
	public void setShowTerrain(boolean showTerrain) {
		this.showTerrain = showTerrain;
	}
	public boolean isWireframe() {
		return wireframe;
	}
	public void setWireframe(boolean wireframe) {
		this.wireframe = wireframe;
	}
	public boolean isBackfaceCulling() {
		return backfaceCulling;
	}
	public void setBackfaceCulling(boolean backfaceCulling) {
		this.backfaceCulling = backfaceCulling;
	}

	public Class<? extends TerrainInterpolator> getInterpolatorClass() {
		return interpolatorClass;
	}

	public void setInterpolatorClass(Class<? extends TerrainInterpolator> interpolatorClass) {
		this.interpolatorClass = interpolatorClass;
	}

	public Class<? extends EleConstraintEnforcer> getEnforcerClass() {
		return enforcerClass;
	}

	public void setEnforcerClass(Class<? extends EleConstraintEnforcer> enforcerClass) {
		this.enforcerClass = enforcerClass;
	}

}

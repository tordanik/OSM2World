package org.osm2world.core.world.modules;

import static java.util.Collections.singletonList;
import static org.osm2world.core.world.modules.common.WorldModuleParseUtil.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.osm2world.core.map_data.data.MapArea;
import org.osm2world.core.map_data.data.MapElement;
import org.osm2world.core.map_data.data.MapNode;
import org.osm2world.core.map_data.data.MapWaySegment;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.data.EleConnector;
import org.osm2world.core.map_elevation.data.GroundState;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.RenderableToAllTargets;
import org.osm2world.core.target.Target;
import org.osm2world.core.target.common.model.Model;
import org.osm2world.core.target.common.model.obj.ExternalModel;
import org.osm2world.core.world.data.AreaWorldObject;
import org.osm2world.core.world.data.NodeWorldObject;
import org.osm2world.core.world.data.WorldObject;
import org.osm2world.core.world.modules.common.AbstractModule;

/**
 * adds external 3D models to the world.
 * The {@link Model} instances are loaded from files,
 * and placed in the scene based on special OSM tags.
 */
public class ExternalModelModule extends AbstractModule {
	
	private abstract static class ExternalModelWorldObject<E extends MapElement>
			implements WorldObject, RenderableToAllTargets {
		
		// TODO will later require a better solution to allow models on bridges etc.
		
		protected final E element;
		protected final Model model;
		
		protected EleConnector eleConnector = null;
		
		private ExternalModelWorldObject(E element, Model model) {
			this.element = element;
			this.model = model;
		}
		
		@Override
		public E getPrimaryMapElement() {
			return element;
		}
		
		@Override
		public GroundState getGroundState() {
			return GroundState.ON;
		}
		
		@Override
		public void defineEleConstraints(EleConstraintEnforcer enforcer) {}
		
		@Override
		public void renderTo(Target<?> target) {
			
			VectorXYZ position = eleConnector.getPosXYZ();
			
			double direction = parseDirection(element.getTags(), 0);
			Double height = (double)parseHeight(element.getTags(), 0);
			Double width = (double)parseWidth(element.getTags(), 0);
			Double length = (double)parseLength(element.getTags(), 0);
			
			if (height == 0) {
				height = null;
			}
			
			if (width == 0) {
				width = null;
			}
			
			if (length == 0) {
				length = null;
			}
			
			target.drawModel(model, position, direction, height, width, length);
			
		}
		
	}
	
	private static class ExternalModelNodeWorldObject extends ExternalModelWorldObject<MapNode>
			implements NodeWorldObject {
		
		private ExternalModelNodeWorldObject(MapNode node, Model model) {
			super(node, model);
		}
		
		@Override
		public Iterable<EleConnector> getEleConnectors() {
		
			if (eleConnector == null) {
				VectorXZ pos = getPosition(element);
				eleConnector = new EleConnector(pos, element, GroundState.ON);
			}
			
			return singletonList(eleConnector);
			
		}

		private VectorXZ getPosition(MapNode element) {
			if (element.getTags().containsKey("model:lon") &&
					element.getTags().containsKey("model:lat")) {
				// I need an access to mapProjection here, or at the moment
				// of MapNode creation
				
				// Better to have it here because here I have full access to 
				// model itself nad model metadata
				return element.getPos();
			}
			else {
				return element.getPos();
			}
		}
		
	}
	
	private static class ExternalModelAreaWorldObject extends ExternalModelWorldObject<MapArea>
			implements AreaWorldObject {
		
		private ExternalModelAreaWorldObject(MapArea area, Model model) {
			super(area, model);
		}
		
		@Override
		public Iterable<EleConnector> getEleConnectors() {
			
			if (eleConnector == null) {
				VectorXZ pos = element.getOuterPolygon().getCentroid();
				eleConnector = new EleConnector(pos, element, GroundState.ON);
			}
			
			return singletonList(eleConnector);
			
		}
		
	}
	
	@Override
	protected void applyToElement(MapElement element) {
		
		if (element.getPrimaryRepresentation() != null) return;
		
		// ways are not yet supported because there's no easy way to obtain the other way segments
		if (element instanceof MapWaySegment) return;
		
		if (element.getTags().containsKey("model:url")) {
			
			// only use external models if enabled via config options
			//if (!config.getBoolean("useExternalModels", false)) return;
			if (!config.getBoolean("useExternalModels", true)) return;
			
			try {
				
				URL modelURL = new URL(element.getTags().getValue("model:url"));
				
				Model model = new ExternalModel(modelURL.toString());
				
				/* place the model in the scene, wrapped in a world object */
				
				if (element instanceof MapNode) {
					
					MapNode node = (MapNode)element;
					NodeWorldObject worldObject = new ExternalModelNodeWorldObject(node, model);
					node.addRepresentation(worldObject);
					
				} else if (element instanceof MapArea) {
					
					MapArea area = (MapArea)element;
					AreaWorldObject worldObject = new ExternalModelAreaWorldObject(area, model);
					area.addRepresentation(worldObject);
					
				} else {
					
					throw new Error("unsupported element type for external model: " + element);
					
				}
				
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
			
		}
		
	}
	
}

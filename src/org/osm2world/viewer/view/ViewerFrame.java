package org.osm2world.viewer.view;

import static java.util.Arrays.asList;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_elevation.creation.EleTagElevationCalculator;
import org.osm2world.core.map_elevation.creation.ElevationCalculator;
import org.osm2world.core.map_elevation.creation.ForceElevationCalculator;
import org.osm2world.core.map_elevation.creation.LevelTagElevationCalculator;
import org.osm2world.core.map_elevation.creation.ZeroElevationCalculator;
import org.osm2world.viewer.control.actions.AboutAction;
import org.osm2world.viewer.control.actions.ExitAction;
import org.osm2world.viewer.control.actions.ExportObjAction;
import org.osm2world.viewer.control.actions.ExportObjDirAction;
import org.osm2world.viewer.control.actions.ExportPOVRayAction;
import org.osm2world.viewer.control.actions.ExportScreenshotAction;
import org.osm2world.viewer.control.actions.HelpControlsAction;
import org.osm2world.viewer.control.actions.OpenOSMAction;
import org.osm2world.viewer.control.actions.OrthoBoundsAction;
import org.osm2world.viewer.control.actions.OrthoTileAction;
import org.osm2world.viewer.control.actions.ResetCameraAction;
import org.osm2world.viewer.control.actions.SetCameraToCoordinateAction;
import org.osm2world.viewer.control.actions.SetElevationCalculatorAction;
import org.osm2world.viewer.control.actions.StatisticsAction;
import org.osm2world.viewer.control.actions.ToggleBackfaceCullingAction;
import org.osm2world.viewer.control.actions.ToggleDebugViewAction;
import org.osm2world.viewer.control.actions.ToggleOrthographicProjectionAction;
import org.osm2world.viewer.control.actions.ToggleWireframeAction;
import org.osm2world.viewer.control.navigation.DefaultNavigation;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.debug.ClearingDebugView;
import org.osm2world.viewer.view.debug.EleDebugView;
import org.osm2world.viewer.view.debug.HelpView;
import org.osm2world.viewer.view.debug.MapDataBoundsDebugView;
import org.osm2world.viewer.view.debug.MapDataDebugView;
import org.osm2world.viewer.view.debug.MapDataElevationDebugView;
import org.osm2world.viewer.view.debug.NetworkDebugView;
import org.osm2world.viewer.view.debug.OrthoBoundsDebugView;
import org.osm2world.viewer.view.debug.QuadtreeDebugView;
import org.osm2world.viewer.view.debug.RoofDataDebugView;
import org.osm2world.viewer.view.debug.TerrainAABBDebugView;
import org.osm2world.viewer.view.debug.TerrainBoundaryAABBDebugView;
import org.osm2world.viewer.view.debug.TerrainBoundaryDebugView;
import org.osm2world.viewer.view.debug.TerrainCellLabelsView;
import org.osm2world.viewer.view.debug.TerrainElevationGridDebugView;
import org.osm2world.viewer.view.debug.TerrainNormalsDebugView;
import org.osm2world.viewer.view.debug.TerrainOutlineDebugView;
import org.osm2world.viewer.view.debug.TerrainView;
import org.osm2world.viewer.view.debug.TriangulationDebugView;
import org.osm2world.viewer.view.debug.WorldObjectNormalsDebugView;
import org.osm2world.viewer.view.debug.WorldObjectView;

public class ViewerFrame extends JFrame {

	public final ViewerGLCanvas glCanvas;
	
	private final Data data;
	private final RenderOptions renderOptions;
	private final MessageManager messageManager;
		
	public ViewerFrame(final Data data, final MessageManager messageManager,
			final RenderOptions renderOptions, final Configuration config) {

		super("OSM2World Viewer");

		this.data = data;
		this.renderOptions = renderOptions;
		this.messageManager = messageManager;
		
		data.setConfig(config);
		
		createMenuBar();
		
		glCanvas = new ViewerGLCanvas(data, messageManager, renderOptions);
		add(glCanvas, BorderLayout.CENTER);
		
		new FileDrop(glCanvas, new FileDrop.Listener() {
			@Override
			public void filesDropped(File[] files) {
				if (files.length >= 1) {
					new OpenOSMAction(ViewerFrame.this, data, renderOptions)
						.openOSMFile(files[0]);
				}
			}
		});
		
		DefaultNavigation navigation = new DefaultNavigation(this, renderOptions);
		glCanvas.addMouseListener(navigation);
		glCanvas.addMouseMotionListener(navigation);
		glCanvas.addMouseWheelListener(navigation);
		glCanvas.addKeyListener(navigation);
		
		// also add the help view, but don't include it in the menu
		new ToggleDebugViewAction(new HelpView(), -1, true,
				this, data, renderOptions);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		pack();

	}

	private void createMenuBar() {

		JMenuBar menu = new JMenuBar();

		{ //"File"

			JMenu subMenu = new JMenu("File");
			subMenu.setMnemonic(KeyEvent.VK_F);
			subMenu.add(new OpenOSMAction(this, data, renderOptions));
			subMenu.add(new ExportObjAction(this, data, messageManager, renderOptions));
			subMenu.add(new ExportObjDirAction(this, data, messageManager, renderOptions));
			subMenu.add(new ExportPOVRayAction(this, data, messageManager, renderOptions));
			subMenu.add(new ExportScreenshotAction(this, data, messageManager, renderOptions));
			subMenu.add(new StatisticsAction(this, data));
			subMenu.add(new ExitAction());
			menu.add(subMenu);

		} { //"View"

			JMenu subMenu = new JMenu("View");
			subMenu.setMnemonic(KeyEvent.VK_V);
			
			subMenu.add(new JCheckBoxMenuItem(new ToggleWireframeAction(this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleBackfaceCullingAction(this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new WorldObjectView(), KeyEvent.VK_W, true,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new TerrainView(), KeyEvent.VK_T, true,
					this, data, renderOptions)));

			subMenu.addSeparator();
			
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new TerrainBoundaryAABBDebugView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new TerrainAABBDebugView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new ClearingDebugView(), KeyEvent.VK_L, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new MapDataDebugView(), KeyEvent.VK_G, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new MapDataElevationDebugView(), KeyEvent.VK_E, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new RoofDataDebugView(), KeyEvent.VK_R, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new NetworkDebugView(), KeyEvent.VK_X, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new QuadtreeDebugView(), KeyEvent.VK_Q, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new TerrainCellLabelsView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new TerrainBoundaryDebugView(), KeyEvent.VK_B, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new TerrainOutlineDebugView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new TerrainNormalsDebugView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new WorldObjectNormalsDebugView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new TriangulationDebugView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new MapDataBoundsDebugView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new OrthoBoundsDebugView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new TerrainElevationGridDebugView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new EleDebugView(), -1, false,
					this, data, renderOptions)));
						
			menu.add(subMenu);
			
		} { //"Camera"

			JMenu subMenu = new JMenu("Camera");
			subMenu.setMnemonic(KeyEvent.VK_C);
			subMenu.add(new ResetCameraAction(this, data, renderOptions));
			subMenu.add(new SetCameraToCoordinateAction(this, data, renderOptions));
			subMenu.add(new OrthoTileAction(this, data, renderOptions));
			subMenu.add(new OrthoBoundsAction(this, data, renderOptions));
			subMenu.add(new JCheckBoxMenuItem(
					new ToggleOrthographicProjectionAction(this, data, renderOptions)));
			menu.add(subMenu);

		} { //"Options"

			JMenu subMenu = new JMenu("Options");
			JMenu eleCalcMenu = new JMenu("ElevationCalculator");
			subMenu.add(eleCalcMenu);
			subMenu.setMnemonic(KeyEvent.VK_O);
			
			ButtonGroup eleCalcGroup = new ButtonGroup();
			
			for (ElevationCalculator eleCalc : asList(
					new ZeroElevationCalculator(),
					new ForceElevationCalculator(),
					new EleTagElevationCalculator(),
					new LevelTagElevationCalculator())) {
				
				JRadioButtonMenuItem item = new JRadioButtonMenuItem(
						new SetElevationCalculatorAction(
								eleCalc, this,  data, renderOptions));
				
				eleCalcGroup.add(item);
				eleCalcMenu.add(item);
				
			}
			
			menu.add(subMenu);

		} { //"Help"

			JMenu subMenu = new JMenu("Help");
			subMenu.add(new HelpControlsAction());
			subMenu.add(new AboutAction());
			subMenu.setMnemonic(KeyEvent.VK_H);

			menu.add(subMenu);

		}

		this.setJMenuBar(menu);
		
	}
	
	public MessageManager getMessageManager() {
		return messageManager;
	}

}

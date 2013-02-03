package org.osm2world.viewer.view;

import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;

import java.awt.BorderLayout;
import java.io.File;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_elevation.creation.BridgeTunnelElevationCalculator;
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
import org.osm2world.viewer.control.actions.ReloadOSMAction;
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
import org.osm2world.viewer.view.debug.DebugView;
import org.osm2world.viewer.view.debug.EleDebugView;
import org.osm2world.viewer.view.debug.FaceDebugView;
import org.osm2world.viewer.view.debug.Map2dTreeDebugView;
import org.osm2world.viewer.view.debug.MapDataBoundsDebugView;
import org.osm2world.viewer.view.debug.MapDataDebugView;
import org.osm2world.viewer.view.debug.MapDataElevationDebugView;
import org.osm2world.viewer.view.debug.NetworkDebugView;
import org.osm2world.viewer.view.debug.OrthoBoundsDebugView;
import org.osm2world.viewer.view.debug.QuadtreeDebugView;
import org.osm2world.viewer.view.debug.RoofDataDebugView;
import org.osm2world.viewer.view.debug.SkyboxView;
import org.osm2world.viewer.view.debug.TerrainAABBDebugView;
import org.osm2world.viewer.view.debug.TerrainBoundaryAABBDebugView;
import org.osm2world.viewer.view.debug.TerrainBoundaryDebugView;
import org.osm2world.viewer.view.debug.TerrainCellLabelsView;
import org.osm2world.viewer.view.debug.TerrainElevationGridDebugView;
import org.osm2world.viewer.view.debug.TerrainNormalsDebugView;
import org.osm2world.viewer.view.debug.TerrainOutlineDebugView;
import org.osm2world.viewer.view.debug.TerrainView;
import org.osm2world.viewer.view.debug.WorldObjectNormalsDebugView;
import org.osm2world.viewer.view.debug.WorldObjectView;

public class ViewerFrame extends JFrame {

	public final ViewerGLCanvas glCanvas;
	
	private final Data data;
	private final RenderOptions renderOptions;
	private final File configFile;
	private final MessageManager messageManager;
		
	public ViewerFrame(final Data data, final MessageManager messageManager,
			final RenderOptions renderOptions, final Configuration config,
			final File configFile) {

		super("OSM2World Viewer");

		this.data = data;
		this.renderOptions = renderOptions;
		this.configFile = configFile;
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
						.openOSMFile(files[0], true);
				}
			}
		});
		
		DefaultNavigation navigation = new DefaultNavigation(this, renderOptions);
		glCanvas.addMouseListener(navigation);
		glCanvas.addMouseMotionListener(navigation);
		glCanvas.addMouseWheelListener(navigation);
		glCanvas.addKeyListener(navigation);
		
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		
		pack();
		
	}

	private void createMenuBar() {

		JMenuBar menu = new JMenuBar();

		{ //"File"

			JMenu subMenu = new JMenu("File");
			subMenu.setMnemonic(VK_F);
			subMenu.add(new OpenOSMAction(this, data, renderOptions));
			subMenu.add(new ReloadOSMAction(this, data, renderOptions, configFile));
			subMenu.add(new ExportObjAction(this, data, messageManager, renderOptions));
			subMenu.add(new ExportObjDirAction(this, data, messageManager, renderOptions));
			subMenu.add(new ExportPOVRayAction(this, data, messageManager, renderOptions));
			subMenu.add(new ExportScreenshotAction(this, data, messageManager, renderOptions));
			subMenu.add(new StatisticsAction(this, data));
			subMenu.add(new ExitAction());
			menu.add(subMenu);

		} { //"View"
			
			JMenu subMenu = new JMenu("View");
			subMenu.setMnemonic(VK_V);
			
			subMenu.add(new JCheckBoxMenuItem(new ToggleWireframeAction(this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleBackfaceCullingAction(this, data, renderOptions)));
			
			initAndAddDebugView(subMenu, VK_W, true,
					new WorldObjectView(renderOptions));
			initAndAddDebugView(subMenu, VK_T, true,
					new TerrainView(renderOptions));
			initAndAddDebugView(subMenu, -1, true,
					new SkyboxView());
			
			subMenu.addSeparator();
			
			initAndAddDebugView(subMenu, -1, false,
					new TerrainBoundaryAABBDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new TerrainAABBDebugView());
			initAndAddDebugView(subMenu, VK_L, false,
					new ClearingDebugView());
			initAndAddDebugView(subMenu, VK_G, false,
					new MapDataDebugView());
			initAndAddDebugView(subMenu, VK_E, false,
					new MapDataElevationDebugView());
			initAndAddDebugView(subMenu, VK_R, false,
					new RoofDataDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new FaceDebugView());
			initAndAddDebugView(subMenu, VK_X, false,
					new NetworkDebugView());
			initAndAddDebugView(subMenu, VK_Q, false,
					new QuadtreeDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new Map2dTreeDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new TerrainCellLabelsView());
			initAndAddDebugView(subMenu, VK_B, false,
					new TerrainBoundaryDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new TerrainOutlineDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new TerrainNormalsDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new WorldObjectNormalsDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new MapDataBoundsDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new OrthoBoundsDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new TerrainElevationGridDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new EleDebugView());
			
			menu.add(subMenu);
			
		} { //"Camera"

			JMenu subMenu = new JMenu("Camera");
			subMenu.setMnemonic(VK_C);
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
			subMenu.setMnemonic(VK_O);
			
			ButtonGroup eleCalcGroup = new ButtonGroup();
			
			for (ElevationCalculator eleCalc : asList(
					new BridgeTunnelElevationCalculator(),
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
			subMenu.setMnemonic(VK_H);

			menu.add(subMenu);

		}

		this.setJMenuBar(menu);
		
	}

	/**
	 * initializes a debug view and adds a menu item for it
	 */
	private void initAndAddDebugView(JMenu menu, int keyEvent,
			boolean enabled, DebugView debugView) {
		
		debugView.setConfiguration(data.getConfig());
		
		menu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
				debugView, keyEvent, enabled,
				this, data, renderOptions)));
		
	}
	
	public MessageManager getMessageManager() {
		return messageManager;
	}

}

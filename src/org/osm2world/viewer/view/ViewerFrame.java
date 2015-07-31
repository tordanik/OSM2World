package org.osm2world.viewer.view;

import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.List;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JRadioButtonMenuItem;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.map_elevation.creation.EleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.InverseDistanceWeightingInterpolator;
import org.osm2world.core.map_elevation.creation.LPEleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.LeastSquaresInterpolator;
import org.osm2world.core.map_elevation.creation.LinearInterpolator;
import org.osm2world.core.map_elevation.creation.NaturalNeighborInterpolator;
import org.osm2world.core.map_elevation.creation.NoneEleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.SimpleEleConstraintEnforcer;
import org.osm2world.core.map_elevation.creation.TerrainInterpolator;
import org.osm2world.core.map_elevation.creation.ZeroInterpolator;
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
import org.osm2world.viewer.control.actions.SetEleConstraintEnforcerAction;
import org.osm2world.viewer.control.actions.SetTerrainInterpolatorAction;
import org.osm2world.viewer.control.actions.ShowCameraConfigurationAction;
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
import org.osm2world.viewer.view.debug.EleConnectorDebugView;
import org.osm2world.viewer.view.debug.EleConstraintDebugView;
import org.osm2world.viewer.view.debug.EleDebugView;
import org.osm2world.viewer.view.debug.FaceDebugView;
import org.osm2world.viewer.view.debug.InternalCoordsDebugView;
import org.osm2world.viewer.view.debug.InverseDistanceWeightingInterpolatorDebugView;
import org.osm2world.viewer.view.debug.LatLonDebugView;
import org.osm2world.viewer.view.debug.LeastSquaresInterpolatorDebugView;
import org.osm2world.viewer.view.debug.LinearInterpolatorDebugView;
import org.osm2world.viewer.view.debug.Map2dTreeDebugView;
import org.osm2world.viewer.view.debug.MapDataBoundsDebugView;
import org.osm2world.viewer.view.debug.MapDataDebugView;
import org.osm2world.viewer.view.debug.NaturalNeighborInterpolatorDebugView;
import org.osm2world.viewer.view.debug.NetworkDebugView;
import org.osm2world.viewer.view.debug.OrthoBoundsDebugView;
import org.osm2world.viewer.view.debug.QuadtreeDebugView;
import org.osm2world.viewer.view.debug.RoofDataDebugView;
import org.osm2world.viewer.view.debug.ShadowView;
import org.osm2world.viewer.view.debug.SkyboxView;
import org.osm2world.viewer.view.debug.TerrainBoundaryAABBDebugView;
import org.osm2world.viewer.view.debug.TerrainBoundaryDebugView;
import org.osm2world.viewer.view.debug.WorldObjectNormalsDebugView;
import org.osm2world.viewer.view.debug.WorldObjectView;

import com.google.common.base.Function;

public class ViewerFrame extends JFrame {

	private static final long serialVersionUID = 5807635150399807163L;

	public final ViewerGLCanvas glCanvas;
	
	private final Data data = new Data();
	private final RenderOptions renderOptions = new RenderOptions();
	private final MessageManager messageManager = new MessageManager();
	
	private final File configFile;
	
	/**
	 * 
	 * @param config  configuration object, != null
	 * @param configFile  properties (where config was loaded from), can be null
	 * @param inputFile  osm data file to be loaded at viewer start, can be null
	 */
	public ViewerFrame(final Configuration config,
			final File configFile, File inputFile) {
		
		super("OSM2World Viewer");
		
		this.configFile = configFile;
		data.setConfig(config);
		
		createMenuBar();
		
		// select OpengGL implementation. TODO: autodetection
		GLProfile profile;
		if ("shader".equals(config.getString("joglImplementation"))) {
			profile = GLProfile.get(GLProfile.GL3);
		} else {
			profile = GLProfile.get(GLProfile.GL2);
		}
		GLCapabilities caps = new GLCapabilities(profile);
		
		// set MSAA (Multi Sample Anti-Aliasing)
		int msaa = config.getInt("msaa", 0);
		if (msaa > 0) {
			caps.setSampleBuffers(true);
			caps.setNumSamples(msaa);
		}
		
		glCanvas = new ViewerGLCanvas(data, messageManager, renderOptions, caps);
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
		
		if (inputFile != null) {
			new OpenOSMAction(this, data, renderOptions).openOSMFile(inputFile, true);
		}
		
	}
	
	private final Function<File, ActionListener> actionForFileFunction =
			new Function<File, ActionListener>() {
		
		public ActionListener apply(final File file) {
			
			return new ActionListener() {
				@Override public void actionPerformed(ActionEvent e) {
					new OpenOSMAction(ViewerFrame.this, data,
							renderOptions).openOSMFile(file, true);
				}
			};
			
		}
		
	};

	private void createMenuBar() {

		JMenuBar menu = new JMenuBar();

		{ //"File"

			JMenu recentFilesMenu = new JMenu("Recent files");
			
			new RecentFilesUpdater(recentFilesMenu, actionForFileFunction);
			
			JMenu subMenu = new JMenu("File");
			subMenu.setMnemonic(VK_F);
			subMenu.add(new OpenOSMAction(this, data, renderOptions));
			subMenu.add(new ReloadOSMAction(this, data, renderOptions, configFile));
			subMenu.add(recentFilesMenu);
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
			initAndAddDebugView(subMenu, -1, true,
					new SkyboxView());
			
			subMenu.addSeparator();
			
			initAndAddDebugView(subMenu, -1, false,
					new TerrainBoundaryAABBDebugView());
			initAndAddDebugView(subMenu, VK_L, false,
					new ClearingDebugView());
			initAndAddDebugView(subMenu, VK_D, false,
					new MapDataDebugView());
			initAndAddDebugView(subMenu, VK_E, false,
					new EleConnectorDebugView());
			initAndAddDebugView(subMenu, VK_C, false,
					new EleConstraintDebugView());
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
			initAndAddDebugView(subMenu, VK_B, false,
					new TerrainBoundaryDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new WorldObjectNormalsDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new MapDataBoundsDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new OrthoBoundsDebugView());
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new InternalCoordsDebugView(), -1, false,
					this, data, renderOptions)));
			subMenu.add(new JCheckBoxMenuItem(new ToggleDebugViewAction(
					new LatLonDebugView(), -1, false,
					this, data, renderOptions)));
			initAndAddDebugView(subMenu, -1, false,
					new EleDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new NaturalNeighborInterpolatorDebugView(renderOptions));
			initAndAddDebugView(subMenu, -1, false,
					new LeastSquaresInterpolatorDebugView(renderOptions));
			initAndAddDebugView(subMenu, -1, false,
					new InverseDistanceWeightingInterpolatorDebugView(renderOptions));
			initAndAddDebugView(subMenu, -1, false,
					new LinearInterpolatorDebugView(renderOptions));
			initAndAddDebugView(subMenu, -1, false,
					new ShadowView(renderOptions));
			
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
			subMenu.add(new ShowCameraConfigurationAction(data, renderOptions));
			menu.add(subMenu);

		} { //"Options"

			JMenu subMenu = new JMenu("Options");
			subMenu.setMnemonic(VK_O);
			
			JMenu interpolatorMenu = new JMenu("TerrainInterpolator");
			subMenu.add(interpolatorMenu);
			
			ButtonGroup interpolatorGroup = new ButtonGroup();
			
			@SuppressWarnings("unchecked")
			List<Class<? extends TerrainInterpolator>> interpolatorClasses = asList(
					ZeroInterpolator.class,
					LinearInterpolator.class,
					InverseDistanceWeightingInterpolator.class,
					LeastSquaresInterpolator.class,
					NaturalNeighborInterpolator.class);
			
			for (Class<? extends TerrainInterpolator> c : interpolatorClasses) {
				
				JRadioButtonMenuItem item = new JRadioButtonMenuItem(
						new SetTerrainInterpolatorAction(c,
								this, data, renderOptions));
				
				interpolatorGroup.add(item);
				interpolatorMenu.add(item);
				
			}
			
			JMenu enforcerMenu = new JMenu("EleConstraintEnforcer");
			subMenu.add(enforcerMenu);
			
			ButtonGroup enforcerGroup = new ButtonGroup();
			
			@SuppressWarnings("unchecked")
			List<Class<? extends EleConstraintEnforcer>> enforcerClasses = asList(
					NoneEleConstraintEnforcer.class,
					SimpleEleConstraintEnforcer.class,
					LPEleConstraintEnforcer.class);
			
			for (Class<? extends EleConstraintEnforcer> c : enforcerClasses) {
				
				JRadioButtonMenuItem item = new JRadioButtonMenuItem(
						new SetEleConstraintEnforcerAction(c,
								this, data, renderOptions));
				
				enforcerGroup.add(item);
				enforcerMenu.add(item);
				
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

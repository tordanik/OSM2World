package org.osm2world.viewer.view;

import static java.awt.event.KeyEvent.*;
import static java.util.Arrays.asList;
import static org.osm2world.core.target.gltf.GltfTarget.GltfFlavor.GLB;
import static org.osm2world.core.target.gltf.GltfTarget.GltfFlavor.GLTF;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;
import javax.swing.*;

import org.osm2world.core.conversion.O2WConfig;
import org.osm2world.core.map_elevation.creation.*;
import org.osm2world.core.target.common.mesh.LevelOfDetail;
import org.osm2world.viewer.control.actions.*;
import org.osm2world.viewer.control.navigation.DefaultNavigation;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.debug.*;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;

public class ViewerFrame extends JFrame {

	private static final long serialVersionUID = 5807635150399807163L;

	/**
	 * Number of bits for stencil buffer
	 */
	private static final int STENCIL_BITS = 8;

	public ViewerGLCanvas glCanvas;

	private final Data data;
	private final RenderOptions renderOptions = new RenderOptions();
	private final MessageManager messageManager = new MessageManager();
	private final List<DebugView> debugViews = new ArrayList<>();

	/**
	 *
	 * @param config  configuration object, != null
	 * @param lod  initial {@link LevelOfDetail} setting
	 * @param configFiles  properties (where config was loaded from), can be null
	 * @param inputFile  osm data file to be loaded at viewer start, can be null
	 */
	public ViewerFrame(final O2WConfig config, @Nullable LevelOfDetail lod,
			final List<File> configFiles, File inputFile) {

		super("OSM2World Viewer");

		data = new Data(configFiles, config);

		if (lod != null) {
			renderOptions.setLod(lod);
		}

		createMenuBar();

		createCanvas(config);

		setDefaultCloseOperation(EXIT_ON_CLOSE);

		pack();

		if (inputFile != null) {
			new OpenOSMAction(this, data, renderOptions).openOSMFile(inputFile, true);
		}

	}

	private void createMenuBar() {

		JMenuBar menu = new JMenuBar();

		{ //"File"

			JMenu recentFilesMenu = new JMenu("Recent files");

			new RecentFilesUpdater(recentFilesMenu, (File file) -> new OpenOSMAction(
					ViewerFrame.this, data, renderOptions).openOSMFile(file, true));

			JMenu subMenu = new JMenu("File");
			subMenu.setMnemonic(VK_F);
			subMenu.add(new OpenOSMAction(this, data, renderOptions));
			subMenu.add(new ReloadOSMAction(this, data, renderOptions));
			subMenu.add(recentFilesMenu);
			subMenu.add(new DownloadOverpassAction(this, data, renderOptions));
			subMenu.add(new ExportGltfAction(this, data, messageManager, renderOptions, GLTF));
			subMenu.add(new ExportGltfAction(this, data, messageManager, renderOptions, GLB));
			subMenu.add(new ExportObjAction(this, data, messageManager, renderOptions));
			subMenu.add(new ExportObjDirAction(this, data, messageManager, renderOptions));
			subMenu.add(new ExportPOVRayAction(this, data, messageManager, renderOptions));
			subMenu.add(new ExportPngAction(this, data, messageManager, renderOptions));
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
					new AttachmentSurfaceDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new AttachmentConnectorDebugView());
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
					new QuadtreeDebugView());
			initAndAddDebugView(subMenu, -1, false,
					new Map2dTreeDebugView());
			initAndAddDebugView(subMenu, VK_B, false,
					new GroundFootprintDebugView());
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

			JMenu lodMenu = new JMenu("Level of Detail");
			subMenu.add(lodMenu);

			var lodGroup = new ButtonGroup();

			for (LevelOfDetail lod : LevelOfDetail.values()) {
				var item = new JRadioButtonMenuItem(new SetLodAction(lod, this, data, renderOptions));
				lodGroup.add(item);
				lodMenu.add(item);
			}

			JMenu interpolatorMenu = new JMenu("TerrainInterpolator");
			subMenu.add(interpolatorMenu);

			ButtonGroup interpolatorGroup = new ButtonGroup();

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

			JMenu eleCalculatorMenu = new JMenu("EleCalculator");
			subMenu.add(eleCalculatorMenu);

			ButtonGroup eleCalculatorGroup = new ButtonGroup();

			List<Class<? extends EleCalculator>> enforcerClasses = asList(
					NoOpEleCalculator.class,
					EleTagEleCalculator.class,
					BridgeTunnelEleCalculator.class,
					ConstraintEleCalculator.class);

			for (Class<? extends EleCalculator> c : enforcerClasses) {

				JRadioButtonMenuItem item = new JRadioButtonMenuItem(
						new SetEleCalculatorAction(c,
								this, data, renderOptions));

				eleCalculatorGroup.add(item);
				eleCalculatorMenu.add(item);

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

		debugViews.add(debugView);

	}

	public MessageManager getMessageManager() {
		return messageManager;
	}

	/**
	 * Prepare OpenGL (Profile/Capabilities) and create matching canvas.
	 * @param config config for OpenGL parameters
	 */
	private void createCanvas(O2WConfig config) {
		// select OpenGL implementation. TODO: autodetection
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

		if ("shader".equals(config.getString("joglImplementation"))) {
			if ("shadowVolumes".equals(config.getString("shadowImplementation")) || "both".equals(config.getString("shadowImplementation")))
				caps.setStencilBits(STENCIL_BITS);
		}

		glCanvas = new ViewerGLCanvas(this, data, messageManager, renderOptions, caps);
		add(glCanvas, BorderLayout.CENTER);

		new FileDrop(glCanvas, files -> {
			if (files.length >= 1) {
				new OpenOSMAction(ViewerFrame.this, data, renderOptions)
					.openOSMFile(files[0], true);
			}
		});

		DefaultNavigation navigation = new DefaultNavigation(this, renderOptions);
		glCanvas.addMouseListener(navigation);
		glCanvas.addMouseMotionListener(navigation);
		glCanvas.addMouseWheelListener(navigation);
		glCanvas.addKeyListener(navigation);
	}

	/**
	 * Update with new configuration. May recreate the canvas when the OpenGL-Parameters changed
	 */
	public void setConfiguration(O2WConfig config) {
		if (!checkConfiguration(config)) {
			System.out.println("OpenGL configuration changed. Recreating canvas.");

			// reset all DebugViews to free resources bound to the old OpenGL context. The context still needs to exist at this point
			for(DebugView d : debugViews) {
				d.reset();
			}

			// destroy old OpenGL context
			remove(glCanvas);
			glCanvas.destroy();

			// recreate
			createCanvas(config);
			pack();
		}
	}

	/**
	 * Check if the current OpenGL-Parameters match the given configuration.
	 * @return true if the configuration matches, false otherwise. In the latter case the GLCanvas needs to be recreated
	 */
	private boolean checkConfiguration(O2WConfig config) {
		if ("shader".equals(config.getString("joglImplementation"))) {
			if (!glCanvas.getGLProfile().isGL3())
				return false;
		} else {
			if (!glCanvas.getGLProfile().isGL2())
				return false;
		}

		int msaa = config.getInt("msaa", 0);
		if (glCanvas.getChosenGLCapabilities().getNumSamples() != msaa)
			return false;

		if (glCanvas.getChosenGLCapabilities().getSampleBuffers() != msaa > 0)
			return false;


		if ("shader".equals(config.getString("joglImplementation"))
				&& ("shadowVolumes".equals(config.getString("shadowImplementation")) || "both".equals(config.getString("shadowImplementation")))) {
			if (glCanvas.getChosenGLCapabilities().getStencilBits() != STENCIL_BITS)
				return false;
		}
		return true;
	}

}

package org.osm2world.viewer.control.actions;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.target.Target;
import org.osm2world.target.TargetUtil;
import org.osm2world.target.common.rendering.MutableCamera;
import org.osm2world.target.povray.POVRayTarget;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class ExportPOVRayAction extends AbstractExportAction {

	private static final long serialVersionUID = -6019526579344140850L; //generated serialVersionUID

	public ExportPOVRayAction(ViewerFrame viewerFrame, Data data,
			MessageManager messageManager, RenderOptions renderOptions) {

		super("Export POVRay file", viewerFrame, data, messageManager, renderOptions);
		putValue(SHORT_DESCRIPTION, "Writes a source file for the POVRay raytracer");
		putValue(MNEMONIC_KEY, KeyEvent.VK_P);
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				KeyEvent.VK_P, ActionEvent.CTRL_MASK));

	}

	protected FileNameExtensionFilter getFileNameExtensionFilter() {
		return new FileNameExtensionFilter("POVRay source files", "pov");
	}

	@Override
	protected void performExport(File file) throws HeadlessException {

		try {

			/* create camera and perspective
			   compensating for left- vs. right-handed coords */

			MutableCamera povRayCamera = new MutableCamera();
			povRayCamera.setCamera(
					renderOptions.camera.pos(),
					renderOptions.camera.lookAt(),
					renderOptions.camera.up());

			boolean underground = data.getConfig() == null || data.getConfig().getBoolean("renderUnderground", true);

			/* write the file */
			Target gltfTarget = new POVRayTarget(file, povRayCamera, renderOptions.projection);
			gltfTarget.setConfiguration(data.getConfig());
			TargetUtil.renderWorldObjects(gltfTarget, data.getConversionResults().getMapData(), underground);
			gltfTarget.finish();

			messageManager.addMessage("exported POVRay file " + file);

		} catch (IOException e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"Could not export POVRay file", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

}

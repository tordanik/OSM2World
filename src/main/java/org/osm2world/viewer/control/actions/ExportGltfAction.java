package org.osm2world.viewer.control.actions;

import java.awt.HeadlessException;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.gltf.GltfTarget;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class ExportGltfAction extends AbstractExportAction {

	private static final long serialVersionUID = -6233943695461766122L;  //generated serialVersionUID

	public ExportGltfAction(ViewerFrame viewerFrame, Data data,
			MessageManager messageManager, RenderOptions renderOptions) {

		super("Export glTF file", viewerFrame, data, messageManager, renderOptions);
		putValue(SHORT_DESCRIPTION, "Writes a .gltf file");
		putValue(MNEMONIC_KEY, KeyEvent.VK_G);

	}

	@Override
	protected FileNameExtensionFilter getFileNameExtensionFilter() {
		return new FileNameExtensionFilter("glTF files", "gltf");
	}

	@Override
	protected void performExport(File file) throws HeadlessException {

		try {

			boolean underground = data.getConfig() == null || data.getConfig().getBoolean("renderUnderground", true);

			/* write the file */
			GltfTarget gltfTarget = new GltfTarget(file, null);
			TargetUtil.renderWorldObjects(gltfTarget, data.getConversionResults().getMapData(), underground);
			gltfTarget.finish();

			messageManager.addMessage("exported .gltf file " + file);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"Could not export .gltf file",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

}

package org.osm2world.viewer.control.actions;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.Serial;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.gltf.GltfTarget;
import org.osm2world.core.target.gltf.GltfTarget.GltfFlavor;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class ExportGltfAction extends AbstractExportAction {

	@Serial
	private static final long serialVersionUID = -6233943695461766122L;  //generated serialVersionUID

	private final GltfFlavor flavor;

	public ExportGltfAction(ViewerFrame viewerFrame, Data data,
							MessageManager messageManager, RenderOptions renderOptions, GltfFlavor flavor) {

		super("Export " + flavor.toString().toLowerCase() + " file", viewerFrame, data, messageManager, renderOptions);
		this.flavor = flavor;
		putValue(SHORT_DESCRIPTION, "Writes a ." + flavor.toString().toLowerCase() + " file");
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
			GltfTarget gltfTarget = new GltfTarget(file, flavor,null);
			gltfTarget.setConfiguration(data.getConfig());
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

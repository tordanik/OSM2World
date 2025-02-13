package org.osm2world.viewer.control.actions;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.output.OutputUtil;
import org.osm2world.output.obj.ObjOutput;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class ExportObjAction extends AbstractExportAction {

	private static final long serialVersionUID = -6233943695461766122L;  //generated serialVersionUID

	public ExportObjAction(ViewerFrame viewerFrame, Data data,
			MessageManager messageManager, RenderOptions renderOptions) {

		super("Export OBJ file", viewerFrame, data, messageManager, renderOptions);
		putValue(SHORT_DESCRIPTION, "Writes a Wavefront .obj file");
		putValue(MNEMONIC_KEY, KeyEvent.VK_O);

	}

	protected FileNameExtensionFilter getFileNameExtensionFilter() {
		return new FileNameExtensionFilter("Wavefront .obj files", "obj");
	}

	@Override
	protected void performExport(File file) throws HeadlessException {

		try {

			/* write the file */
			O2WConfig config = data.getConfig();

			ObjOutput target = new ObjOutput(file, data.getConversionResults().getMapProjection());
			target.setConfiguration(config);

			boolean underground = config == null || config.getBoolean("renderUnderground", true);
			OutputUtil.renderWorldObjects(target, data.getConversionResults().getMapData(), underground);

			messageManager.addMessage("exported Wavefront .obj file " + file);

		} catch (IOException e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"Could not export Wavefront .obj file",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

}

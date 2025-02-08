package org.osm2world.viewer.control.actions;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.target.Target;
import org.osm2world.target.TargetUtil;
import org.osm2world.target.obj.ObjMultiFileTarget;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class ExportObjDirAction extends AbstractExportAction {

	private static final long serialVersionUID = 6380889966390760664L; //generated serialVersionUID

	public ExportObjDirAction(ViewerFrame viewerFrame, Data data,
			MessageManager messageManager, RenderOptions renderOptions) {

		super("Export OBJ directory", viewerFrame, data, messageManager, renderOptions);
		putValue(SHORT_DESCRIPTION, "Writes several smaller Wavefront .obj files to a directory");

	}

	@Override
	protected boolean chooseDirectory() {
		return true;
	}

	protected FileNameExtensionFilter getFileNameExtensionFilter() {
		return null;
	}

	@Override
	protected void performExport(File file) throws HeadlessException {

		try {

			String thresholdString = JOptionPane.showInputDialog(
					viewerFrame, "Graphics primitives per file", 10000);

			int primitiveThresholdPerFile = Integer.parseInt(thresholdString);

			/* write the file */

			Target target = new ObjMultiFileTarget(file, data.getConversionResults().getMapProjection(),
					primitiveThresholdPerFile);
			target.setConfiguration(data.getConfig());
			boolean underground = data.getConfig().getBoolean("renderUnderground", true);
			TargetUtil.renderWorldObjects(target, data.getConversionResults().getMapData(), underground);
			target.finish();

			messageManager.addMessage("exported Wavefront .obj file " + file);

		} catch (IOException e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"Could not export Wavefront .obj file",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"please enter a valid number of primitives per file",
					JOptionPane.ERROR_MESSAGE);
		}
	}

}

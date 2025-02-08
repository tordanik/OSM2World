package org.osm2world.viewer.control.actions;

import static java.lang.Math.floor;

import java.awt.*;
import java.io.File;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.target.TargetUtil;
import org.osm2world.target.image.ImageOutputFormat;
import org.osm2world.target.image.ImageTarget;
import org.osm2world.util.Resolution;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class ExportPngAction extends AbstractExportAction {

	public ExportPngAction(ViewerFrame viewerFrame, Data data,
						   MessageManager messageManager, RenderOptions renderOptions) {

		super("Export PNG image", viewerFrame, data, messageManager, renderOptions);
		putValue(SHORT_DESCRIPTION, "Writes a .png file using the current camera position");

	}

	protected FileNameExtensionFilter getFileNameExtensionFilter() {
		return new FileNameExtensionFilter(".png images", "png");
	}

	@Override
	protected void performExport(File file) throws HeadlessException {

		try {

			int width = 2048;
			int height = (int) floor(viewerFrame.glCanvas.getHeight() / (float)viewerFrame.glCanvas.getWidth() * width);

			boolean underground = data.getConfig() == null || data.getConfig().getBoolean("renderUnderground", true);

			/* write the file */

			var target = new ImageTarget(file, ImageOutputFormat.PNG, new Resolution(width, height),
					renderOptions.camera, renderOptions.projection,
					data.getConversionResults().getMapData().getBoundary());

			target.setConfiguration(data.getConfig());
			TargetUtil.renderWorldObjects(target, data.getConversionResults().getMapData(), underground);
			target.finish();

			messageManager.addMessage("exported .png image file " + file);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"Could not export PNG image",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}

	}

}

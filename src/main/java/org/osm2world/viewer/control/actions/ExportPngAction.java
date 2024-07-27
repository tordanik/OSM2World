package org.osm2world.viewer.control.actions;

import static java.lang.Math.floor;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.console.CLIArgumentsUtil;
import org.osm2world.console.ImageExporter;
import org.osm2world.core.util.Resolution;
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

			/* write the file */

			ImageExporter exporter = ImageExporter.create(
					data.getConfig(),
					data.getConversionResults(),
					new Resolution(width, height));

			exporter.writeImageFile(file, CLIArgumentsUtil.OutputMode.PNG,
					width, height,
					renderOptions.camera, renderOptions.projection);

			messageManager.addMessage("exported .png image file " + file);

		} catch (IOException e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"Could not export PNG image",
					JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}

	}

}

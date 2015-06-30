package org.osm2world.viewer.control.actions;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.MessageManager;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.awt.Screenshot;

public class ExportScreenshotAction extends AbstractExportAction {

	private static final long serialVersionUID = 8777435425813342813L; //generated serialVersionUID

	public ExportScreenshotAction(ViewerFrame viewerFrame, Data data,
			MessageManager messageManager, RenderOptions renderOptions) {

		super("Export Screenshot", viewerFrame, data, messageManager, renderOptions);
		putValue(SHORT_DESCRIPTION, "Writes the current display content to an image file");
		putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
				KeyEvent.VK_PRINTSCREEN, ActionEvent.CTRL_MASK));

	}

	protected FileNameExtensionFilter getFileNameExtensionFilter() {
		return new FileNameExtensionFilter("PNG image files", "png");
	}

	@Override
	protected void performExport(File file) throws HeadlessException {

		try {
			
			viewerFrame.glCanvas.getContext().makeCurrent();
			
			AWTGLReadBufferUtil reader = new AWTGLReadBufferUtil(viewerFrame.glCanvas.getGLProfile(), true);
			BufferedImage img = reader.readPixelsToBufferedImage(viewerFrame.glCanvas.getGL(), true);
		    ImageIO.write(img, "png", file);

			viewerFrame.glCanvas.getContext().release();
			
			messageManager.addMessage("exported PNG file " + file);

		} catch (IOException e) {
			JOptionPane.showMessageDialog(viewerFrame,
					e.toString(),
					"Could not export PNG file", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}

}

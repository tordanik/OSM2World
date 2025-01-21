package org.osm2world.viewer.control.actions;

import static org.osm2world.core.GlobalValues.*;

import java.awt.*;
import java.awt.event.ActionEvent;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

public class AboutAction extends AbstractAction {

	private static final long serialVersionUID = -6717063896933933005L; //generated serialVersionUID

	public AboutAction() {
		super("About OSM2World");
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		JDialog dialog = new JDialog();
		dialog.setTitle("About OSM2World");
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.add(createAboutContent(), BorderLayout.CENTER);
		dialog.setSize(800, 600);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);

	}

	private static JScrollPane createAboutContent() {

		var tabText = new JEditorPane();
		tabText.setContentType("text/html");
		tabText.setText("<html><body style='padding: 10px;'>"
				+ "<h1>OSM2World</h1>"
				+ "<p>Version " + VERSION_STRING + "</p>"
				+ "<br/><table>"
				+ "<tr><td>Website<td><a href='" + OSM2WORLD_URI + "'>" + OSM2WORLD_URI.replace("https://", "") + "</a>"
				+ "<tr><td>Wiki<td><a href='" + WIKI_URI + "'>" + WIKI_URI.replace("https://", "") + "</a><br>"
				+ "<tr><td>Forum<td><a href='https://community.osm.org/tag/osm2world'>community.osm.org/tag/osm2world</a><br>"
				+ "<tr><td>Issues<td><a href='https://github.com/tordanik/OSM2World/issues'>github.com/tordanik/OSM2World/issues</a><br>"
				+ "</table></p></body></html>");
		tabText.setEditable(false);
		tabText.setOpaque(false);
		tabText.addHyperlinkListener(e -> {
			if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
				try {
					Desktop.getDesktop().browse(e.getURL().toURI());
				} catch (Exception ignored) {}
			}
		});

		return new JScrollPane(tabText);

	}

}

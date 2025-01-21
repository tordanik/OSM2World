package org.osm2world.viewer.control.actions;

import static org.osm2world.core.GlobalValues.*;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.InputStream;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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

		var tabbedPane = new JTabbedPane();
		tabbedPane.addTab("About", createAboutTabContent());
		tabbedPane.addTab("Third-party licenses", createLicenseTabContent());

		dialog.setLayout(new BorderLayout());
		dialog.add(tabbedPane, BorderLayout.CENTER);

		dialog.setSize(800, 600);
		dialog.setLocationRelativeTo(null);
		dialog.setVisible(true);

	}

	private static JComponent createAboutTabContent() {

		String text = "<html><body style='padding: 10px;'>"
				+ "<h1>OSM2World</h1>"
				+ "<p>Version " + VERSION_STRING + "</p>"
				+ "<br/><table>"
				+ "<tr><td>Website<td><a href='" + OSM2WORLD_URI + "'>" + OSM2WORLD_URI.replace("https://", "") + "</a>"
				+ "<tr><td>Wiki<td><a href='" + WIKI_URI + "'>" + WIKI_URI.replace("https://", "") + "</a><br>"
				+ "<tr><td>Forum<td><a href='https://community.osm.org/tag/osm2world'>community.osm.org/tag/osm2world</a><br>"
				+ "<tr><td>Issues<td><a href='https://github.com/tordanik/OSM2World/issues'>github.com/tordanik/OSM2World/issues</a><br>"
				+ "</table></p></body></html>";

		return createReadonlyHtmlComponent(text);

	}

	private static JComponent createLicenseTabContent() {

		String attributionText;

		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

		try (InputStream attributionStream = classLoader.getResourceAsStream("attribution.xml")) {

			if (attributionStream != null) {

				var attributionTextBuilder = new StringBuilder();
				attributionTextBuilder.append("<html><body style='padding: 10px;'>");

				Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(attributionStream);
				document.getDocumentElement().normalize();

				// Get all <dependency> elements
				NodeList dependencyNodes = document.getElementsByTagName("dependency");
				for (int i = 0; i < dependencyNodes.getLength(); i++) {
					Node node = dependencyNodes.item(i);
					if (node.getNodeType() == Node.ELEMENT_NODE && node instanceof Element element) {

						Node name = element.getElementsByTagName("name").item(0);
						Node version = element.getElementsByTagName("version").item(0);
						Node url = element.getElementsByTagName("projectUrl").item(0);
						NodeList licenses = element.getElementsByTagName("license");

						if (name != null && licenses.getLength() > 0) {

							attributionTextBuilder.append("<h2>").append(name.getTextContent());
							if (version != null) {
								attributionTextBuilder.append(" ").append(version.getTextContent());
							}
							attributionTextBuilder.append("</h2>");

							if (url != null) {
								attributionTextBuilder.append("<p>").append(url.getTextContent()).append("</p>");
							}

							for (int licenseIndex = 0; licenseIndex < licenses.getLength(); licenseIndex++) {
								Node license = licenses.item(licenseIndex);
								attributionTextBuilder.append("<p>").append(license.getTextContent()).append("</p>");
							}

							attributionTextBuilder.append("<br/>");

						}

					}
				}

				attributionTextBuilder.append("</body></html>");
				attributionText = attributionTextBuilder.toString();

			} else {
				throw new Exception("Could not load resource");
			}
		} catch (Exception e) {
			attributionText = "Could not read license information: <br/>" + e.getMessage();
		}

		return createReadonlyHtmlComponent(attributionText);

	}

	/** returns a component showing scrollable, read-only HTML text with clickable links */
	private static JComponent createReadonlyHtmlComponent(String text) {

		var tabText = new JEditorPane();
		tabText.setContentType("text/html");
		tabText.setText(text);
		tabText.setCaretPosition(0);
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

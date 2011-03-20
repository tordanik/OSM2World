package org.osm2world.core.osm.creation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * workaround for the inability of Osmosis to read the JOSM XML format.
 */
public final class JOSMFileHack {
	
	private JOSMFileHack() {}
	
	/**
	 * creates a temporary file in the .osm format. This removes some
	 * JOSM-specific attributes present in the original file,
	 * and sets fake versions for unversioned elements.
	 * 
	 * The generated file should <em>not</em> be used for anything except
	 * feeding it to OSM2World.
	 */
	public static final File createTempOSMFile(File josmFile) throws
			IOException, ParserConfigurationException, SAXException,
			TransformerException {
		
		/* parse original file */
		
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(josmFile);
		
		/* modify DOM */
		
		NodeList nodes = doc.getDocumentElement().getChildNodes();
		Collection<Node> nodesToDelete = new ArrayList<Node>();
		
		for (int i = 0; i < nodes.getLength(); i++) {
			if (nodes.item(i) instanceof Element) {
				Element element = (Element) nodes.item(i);
				if ("node".equals(element.getNodeName())
						|| "way".equals(element.getNodeName())
						|| "relation".equals(element.getNodeName())) {
					if ("delete".equals(element.getAttribute("action"))) {
						nodesToDelete.add(element);
					} else if (!element.hasAttribute("version")) {
						element.setAttribute("version", "424242");
					}
				}
			}
		}
		
		for (Node node : nodesToDelete) {
			doc.getDocumentElement().removeChild(node);
		}
		
		/* write result */
		
		File tempFile = File.createTempFile("workaround", ".osm", null);
		tempFile.deleteOnExit();
		
		TransformerFactory tFactory = TransformerFactory.newInstance();
		Transformer transformer = tFactory.newTransformer();
		
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(new FileOutputStream(tempFile));
		transformer.transform(source, result);
		
		return tempFile;
		
	}
	
}

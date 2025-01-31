package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

import javax.annotation.Nullable;
import javax.swing.*;

import org.osm2world.core.math.geo.LatLonBounds;
import org.osm2world.core.osm.creation.OSMDataReaderView;
import org.osm2world.core.osm.creation.OverpassReader;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class DownloadOverpassAction extends AbstractLoadOSMAction {

	private static final long serialVersionUID = 1L;

	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public DownloadOverpassAction(ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super("Download OSM data", viewerFrame, data, renderOptions);
		putValue(SHORT_DESCRIPTION, "Download OpenStreetMap data from Overpass API");
		putValue(MNEMONIC_KEY, KeyEvent.VK_D);

	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		LatLonBounds bounds = askLatLonBounds();

		if (bounds != null) {
			loadOSMData(new OSMDataReaderView(new OverpassReader(), bounds), true);
		}

	}

	private @Nullable LatLonBounds askLatLonBounds() {

		double minLat = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "minLat"));
		double minLon = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "minLon"));
		double maxLat = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "maxLat"));
		double maxLon = Double.parseDouble(
				JOptionPane.showInputDialog(viewerFrame, "maxLon"));

		return new LatLonBounds(minLat, minLon, maxLat, maxLon);

		/*
		JDialog dialog = new JDialog(viewerFrame);
		dialog.setTitle("Select data bounds");
		dialog.setSize(600, 300);

		JXMapKit map = new JXMapKit();

		map.setDefaultProvider(DefaultProviders.OpenStreetMaps);
		map.setCenterPosition(new GeoPosition(50.746, 7.154));
		map.setZoom(3);
		dialog.add(map, java.awt.BorderLayout.CENTER);

		JPanel settingsPanel = new JPanel();
		dialog.add(settingsPanel, java.awt.BorderLayout.EAST);
		BoxLayout settingsPanelLayout = new BoxLayout(settingsPanel, BoxLayout.PAGE_AXIS);
		settingsPanel.setLayout(settingsPanelLayout);

		ButtonGroup buttonGroup = new ButtonGroup();
		JRadioButton coordinatesRB = new JRadioButton("Coordinates");
		JRadioButton customQueryRB = new JRadioButton("Custom Query");
		buttonGroup.add(coordinatesRB);
		buttonGroup.add(customQueryRB);
		settingsPanel.add(coordinatesRB);

		JTextField minLatField = new JTextField();
		settingsPanel.add(new JLabel("minimum latitude"));
		settingsPanel.add(minLatField);
		JTextField minLonField = new JTextField();
		settingsPanel.add(new JLabel("minimum longitude"));
		settingsPanel.add(minLonField);
		JTextField maxLatField = new JTextField();
		settingsPanel.add(new JLabel("maximum latitude"));
		settingsPanel.add(maxLatField);
		JTextField maxLonField = new JTextField();
		settingsPanel.add(new JLabel("maximum longitude"));
		settingsPanel.add(maxLonField);

		settingsPanel.add(customQueryRB);

		JTextArea queryArea = new JTextArea();
		settingsPanel.add(queryArea);

		dialog.setVisible(true);

		*/

	}

}

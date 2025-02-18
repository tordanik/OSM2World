package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.osm2world.scene.mesh.LevelOfDetail;
import org.osm2world.output.statistics.StatisticsOutput;
import org.osm2world.scene.Scene;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.view.StatisticsDialog;
import org.osm2world.viewer.view.ViewerFrame;


public class StatisticsAction extends AbstractAction implements Observer {

	private static final long serialVersionUID = -7894095901533692645L;
	private final ViewerFrame viewerFrame;
	private final Data data;

	public StatisticsAction(ViewerFrame viewerFrame, Data data) {

		super("Statistics");
		putValue(SHORT_DESCRIPTION, "Shows statistics for the current scene");

		this.viewerFrame = viewerFrame;
		this.data = data;

		setEnabled(false);
		data.addObserver(this);

	}

	@Override
	public void update(Observable o, Object arg) {
		setEnabled(data.getConversionResults() != null);
	}

	@Override
	public void actionPerformed(ActionEvent arg0) {

		Scene conversionResults = data.getConversionResults();

		LevelOfDetail lod = data.getConfig().getLod();
		StatisticsOutput stats = new StatisticsOutput(lod);

		stats.outputScene(conversionResults);
		new StatisticsDialog(viewerFrame, stats).setVisible(true);

	}

}

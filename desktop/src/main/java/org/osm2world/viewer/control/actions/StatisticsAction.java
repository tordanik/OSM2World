package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.*;

import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.conversion.ConfigUtil;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.common.mesh.LevelOfDetail;
import org.osm2world.core.target.statistics.StatisticsTarget;
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

		Results conversionResults = data.getConversionResults();

		LevelOfDetail lod = ConfigUtil.readLOD(data.getConfig());
		StatisticsTarget stats = new StatisticsTarget(lod);

		TargetUtil.renderWorldObjects(stats, conversionResults.getMapData(), true);
		new StatisticsDialog(viewerFrame, stats).setVisible(true);

	}

}

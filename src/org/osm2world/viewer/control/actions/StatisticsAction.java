package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.util.Observable;
import java.util.Observer;

import javax.swing.AbstractAction;

import org.osm2world.core.ConversionFacade.Results;
import org.osm2world.core.target.TargetUtil;
import org.osm2world.core.target.statistics.StatisticsTarget;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.view.StatisticsDialog;
import org.osm2world.viewer.view.ViewerFrame;


public class StatisticsAction extends AbstractAction implements Observer {

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
		
		StatisticsTarget stats = new StatisticsTarget();
		
		TargetUtil.renderWorldObjects(stats, conversionResults.getMapData(), true);
		TargetUtil.renderObject(stats, conversionResults.getTerrain());
		new StatisticsDialog(viewerFrame, stats).setVisible(true);
		
	}
		
}

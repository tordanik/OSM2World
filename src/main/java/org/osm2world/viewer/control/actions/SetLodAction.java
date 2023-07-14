package org.osm2world.viewer.control.actions;

import java.awt.event.ActionEvent;
import java.io.Serial;

import javax.swing.*;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.target.common.mesh.LevelOfDetail;
import org.osm2world.viewer.model.Data;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;

public class SetLodAction extends AbstractAction {

	@Serial
	private static final long serialVersionUID = 1L;

	LevelOfDetail lod;
	ViewerFrame viewerFrame;
	Data data;
	RenderOptions renderOptions;

	public SetLodAction(LevelOfDetail lod, ViewerFrame viewerFrame, Data data, RenderOptions renderOptions) {

		super(lod.name());

		putValue(SELECTED_KEY, lod == renderOptions.getLod());

		this.lod = lod;
		this.viewerFrame = viewerFrame;
		this.data = data;
		this.renderOptions = renderOptions;

	}

	@Override
	public void actionPerformed(ActionEvent e) {

		renderOptions.setLod(lod);

		Configuration config = data.getConfig();
		config.clearProperty("lod");
		config.addProperty("lod", lod.ordinal());
		data.setConfig(config);

		putValue(SELECTED_KEY, lod == renderOptions.getLod());

	}

}

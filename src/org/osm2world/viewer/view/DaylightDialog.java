package org.osm2world.viewer.view;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.target.common.lighting.GlobalLightingParameters;

import org.osm2world.core.target.jogl.Sky;

import java.util.Calendar;
import java.util.Map;
import java.util.Locale;
import java.util.TreeMap;

import java.awt.Color;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JSlider;
import javax.swing.UIManager;

public class DaylightDialog extends JDialog
{
	public DaylightDialog(ViewerFrame parent)
	{
		super(parent, "Time and Date");

		// Not default in GTK
		UIManager.put("Slider.paintValue", Boolean.FALSE);

		Calendar cal = Calendar.getInstance();

		// Date Selection
		cal.set(2001,0,1,12,0,0);


		JSlider date = new JSlider(1,365, 1);
		JLabel dateLabel = new JLabel("Jan 1  ");

		date.addChangeListener((e) -> {
				cal.set(Calendar.DAY_OF_YEAR, date.getValue());
				dateLabel.setText(cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + " " + cal.get(Calendar.DAY_OF_MONTH));
				updateRender(cal);
		});

		JPanel datePane = new JPanel();
		datePane.add(date);
		datePane.add(Box.createHorizontalGlue());
		datePane.add(dateLabel);

		datePane.setBorder(BorderFactory.createCompoundBorder(
								BorderFactory.createEmptyBorder(10,10,10,10)
								, BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Date")
						));


		// Time Selection
		JSlider time = new JSlider(0,24 * 60 - 1);
		JLabel timeLabel = new JLabel("12:00");

		time.setSnapToTicks(true);
		time.setMajorTickSpacing(15);
		time.setMinorTickSpacing(1);

		time.addChangeListener(
				(e) -> {
					cal.set(Calendar.HOUR_OF_DAY, time.getValue() / 60);
					cal.set(Calendar.MINUTE, time.getValue() % 60);
					timeLabel.setText(time.getValue() / 60 + ":"
						+ String.format("%02d", time.getValue() % 60));
					updateRender(cal);
				});

		JPanel timePane = new JPanel();
		timePane.add(time);
		timePane.add(timeLabel);
		timePane.add(Box.createHorizontalGlue());

		timePane.setBorder(BorderFactory.createCompoundBorder(
								BorderFactory.createEmptyBorder(10,10,10,10)
								, BorderFactory.createTitledBorder(BorderFactory.createLineBorder(Color.GRAY), "Time")
						));


		JButton close = new JButton("Close");
		close.addActionListener((e)->closeDialog());

		JPanel buttonPane = new JPanel();
		buttonPane.add(close);

		updateRender(cal);


		// Construct UI
		JPanel pane = new JPanel();

		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));
		pane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

		pane.add(datePane);
		pane.add(timePane);
		pane.add(buttonPane);

		this.add(pane);
		this.pack();

		this.setVisible(true);
	}

	public void updateRender(Calendar date)
	{
		GlobalLightingParameters.DEFAULT.setTime(date);
		Sky.setTime(date);
	}

	public void closeDialog()
	{
		this.dispose();
	}
}

package org.osm2world.viewer.view;

import java.util.Calendar;
import java.util.Map;
import java.util.Locale;
import java.util.TreeMap;

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
		JPanel datePane = new JPanel();
		cal.set(2001,0,1,12,0,0);


		JSlider date = new JSlider(1,365, 1);
		JLabel dateLabel = new JLabel("Jan 1");

		date.addChangeListener((e) -> {
				cal.set(Calendar.DAY_OF_YEAR, date.getValue());
				dateLabel.setText(cal.getDisplayName(Calendar.MONTH, Calendar.SHORT, Locale.getDefault()) + " " + cal.get(Calendar.DAY_OF_MONTH));
				updateRender(cal);
		});

		datePane.add(new JLabel("Date"));
		datePane.add(date);
		datePane.add(dateLabel);


		// Time Selection
		JPanel timePane = new JPanel();

		JSlider time = new JSlider(0,24 * 60 - 1);
		JLabel timeLabel = new JLabel("12:00");

		time.setSnapToTicks(true);
		time.setMajorTickSpacing(15);
		time.setMinorTickSpacing(15);

		time.addChangeListener(
				(e) -> {
					cal.set(Calendar.HOUR_OF_DAY, time.getValue() / 60);
					cal.set(Calendar.MINUTE, time.getValue() % 60);
					timeLabel.setText(time.getValue() / 60 + ":"
						+ String.format("%02d", time.getValue() % 60));
					updateRender(cal);
				});

		timePane.add(new JLabel("Time"));
		timePane.add(time);
		timePane.add(timeLabel);


		JButton close = new JButton("Close");
		close.addActionListener((e)->closeDialog());


		// Construct UI
		JPanel pane = new JPanel();
		pane.setLayout(new BoxLayout(pane, BoxLayout.PAGE_AXIS));

		pane.add(datePane);
		pane.add(timePane);
		pane.add(close);

		this.add(pane);
		this.pack();
		this.setVisible(true);
	}

	public void updateRender(Calendar date)
	{
		System.out.println("Calculating sun position on: " + date.getTime().toString() + "...");
		int day = date.get(Calendar.DAY_OF_YEAR);
		int hour = date.get(Calendar.HOUR_OF_DAY);


	}

	public void closeDialog()
	{
		this.dispose();
	}
}

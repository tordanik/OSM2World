package org.osm2world.viewer.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;

import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;

import java.util.function.Supplier;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConversionException;

public class ShaderDialog extends JDialog {
	private Configuration config;

	private List<Supplier<String>> viewerSettings = new LinkedList<>();
	private JCheckBox[] exportSettings;

	//private static final long serialVersionUID = -1016979607905010257L; //generated serialVersionUID
	
	public ShaderDialog(ViewerFrame viewerFrame, Configuration config) {
		// This dialog is modal
		super(viewerFrame, "Shader Configuration", true);

		this.config = config;
		
		JPanel panel = new JPanel();
		BorderLayout layout = new BorderLayout();
		panel.setLayout(layout);

		JPanel optionsPane = new JPanel();
		optionsPane.setLayout(new GridLayout(0, 3));
		//optionsPane.setLayout(new BoxLayout(optionsPane, BoxLayout.PAGE_AXIS));
		optionsPane.setBackground(Color.WHITE);
		
		JScrollPane scrollPane = new JScrollPane(optionsPane, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scrollPane.setMaximumSize(new Dimension(750,500));
		scrollPane.setPreferredSize(new Dimension(750,500));

		optionsPane.add(Box.createGlue());
		optionsPane.add(new JLabel("Viewer"));
		optionsPane.add(new JLabel("Export"));

		//viewerSettings = new JCheckBox[changeable.length];
		//exportSettings = new JCheckBox[changeable.length];

		Iterator<String> keyIter = config.getKeys();

		while(keyIter.hasNext())
		{
			String key = keyIter.next();
			optionsPane.add(new JLabel(key));
			try {
				boolean value = config.getBoolean(key);
				JCheckBox box = new JCheckBox();
				box.setSelected(value);
				optionsPane.add(box);
				viewerSettings.add(()->box.isSelected()?"true":"false");
			} catch (ConversionException e) {
				try {
					int value = config.getInt(key);
					JSpinner spinner = new JSpinner();
					spinner.setValue(value);
					optionsPane.add(spinner);
					viewerSettings.add(()->((Integer)spinner.getValue()).toString());
				} catch (ConversionException e1) {
					JTextField text = new JTextField(config.getString(key));
					optionsPane.add(text);
					viewerSettings.add(text::getText);
				}
			}
			finally
			{
				optionsPane.add(Box.createGlue());
			}
		/*for(int i = 0; i < changeable.length; i++)
		{
			// Add the option title
			optionsPane.add(new JLabel(changeable[i]));

			viewerSettings[i] = new JCheckBox();
			optionsPane.add(viewerSettings[i]);

			exportSettings[i] = new JCheckBox();
			optionsPane.add(exportSettings[i]);*/
		}

		// Center
		panel.add(scrollPane);

		// South
		JButton apply = new JButton("Apply");
		apply.addActionListener((e)->{applySettings();});

		JButton close = new JButton("Close");
		close.addActionListener((e)->{closeDialog();});

		JButton reset = new JButton("Reset");
		reset.addActionListener((e)->{resetSettings();});

		JPanel buttonPane = new JPanel();
		buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
		buttonPane.add(reset);
		buttonPane.add(Box.createHorizontalGlue());
		buttonPane.add(close);
		buttonPane.add(apply);

		panel.add(buttonPane, BorderLayout.SOUTH);


		this.add(panel);
		this.pack();
		this.setVisible(true);
	}

	public void applySettings()
	{
		for(Supplier<String> property : viewerSettings)
		{
			System.out.println(property.get());
		}
		//{
		//	System.out.println(changeable[i] + "_v:\t" + viewerSettings[i].isSelected());
		//	System.out.println(changeable[i] + "_e:\t" + exportSettings[i].isSelected());
		//}
		System.out.println("Settings Applied");
	}

	public void closeDialog()
	{
		System.out.println("Settings Closed");
		this.dispose();
	}

	public void resetSettings()
	{
		System.out.println("Load Defaults");
	}

}

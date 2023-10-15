package org.osm2world.viewer.view;

import static org.osm2world.core.target.statistics.StatisticsTarget.Stat.*;

import java.awt.event.KeyEvent;
import java.util.*;

import javax.swing.*;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Materials;
import org.osm2world.core.target.statistics.StatisticsTarget;
import org.osm2world.core.target.statistics.StatisticsTarget.Stat;


public class StatisticsDialog extends JDialog {

	private static final long serialVersionUID = -2724106939639635472L;
	private static final Map<Stat, String> statNames = new HashMap<Stat, String>();
	static {
		statNames.put(OBJECT_COUNT, "objects");
		statNames.put(PRIMITIVE_COUNT, "primitives");
		statNames.put(TOTAL_TRIANGLE_COUNT, "triangles");
		statNames.put(TRIANGLES_COUNT, "t. groups");
		statNames.put(TRIANGLE_STRIP_COUNT, "t. strips");
		statNames.put(TRIANGLE_FAN_COUNT, "t. fans");
		statNames.put(CONVEX_POLYGON_COUNT, "polygons");
		statNames.put(VBO_VALUE_COUNT, "vbo values");
	}

	public StatisticsDialog(JFrame owner, StatisticsTarget stats) {
		super(owner, "Statistics");

		/* collect content for tables */

		List<Material> materialList =
				new ArrayList<Material>(stats.getKnownMaterials());
		List<Class<?>> classList =
				new ArrayList<Class<?>>(stats.getKnownRenderableClasses());

		int numColumns = Stat.values().length + 1;
		int numMaterials = materialList.size();
		int numClasses = classList.size();

		String[] columnNames = new String[numColumns];

		Object[][] perMaterialData =
				new Object[numMaterials + 1][numColumns];
		Object[][] perClassData =
				new Object[numClasses + 1][numColumns];

		Comparator<?>[] comparators = new Comparator<?>[numColumns];

		columnNames[0] = "name";
		perMaterialData[numMaterials][0] = "TOTAL";
		perClassData[numClasses][0] = "TOTAL";
		comparators[0] = null;

		for (int col = 0; col < numColumns; ++col) {

			Stat stat = (col == 0) ? null : Stat.values()[col-1];

			if (col != 0) {
				columnNames[col] = statNames.get(stat);
				comparators[col] = LONG_COMPARATOR;
			}

			for (int row = 0; row < numMaterials; ++row) {

				if (col == 0) {
					String name = Materials.getUniqueName(materialList.get(row));
					if (name == null) {
						name = materialList.get(row).toString();
					}
					perMaterialData[row][0] = name;
				} else {
					perMaterialData[row][col] =
						stats.getCountForMaterial(materialList.get(row), stat);
				}

			}

			for (int row = 0; row < numClasses; ++row) {

				if (col == 0) {
					perClassData[row][0] = classList.get(row).getSimpleName();
				} else {
					perClassData[row][col] =
						stats.getCountForClass(classList.get(row), stat);
				}
			}

			if (col != 0) {
				perMaterialData[numMaterials][col] = stats.getGlobalCount(stat);
				perClassData[numClasses][col] = stats.getGlobalCount(stat);
			}

		}

		/* create tabs */

		JTabbedPane tabs = new JTabbedPane();
		this.add(tabs);

		{ // global statistics

			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

			panel.add(new JLabel("Level of detail: " + stats.lod));
			panel.add(new JLabel("object types: " + classList.size()));
			panel.add(new JLabel("materials: " + materialList.size()));

			for (Stat stat : Stat.values()) {
				panel.add(new JLabel(statNames.get(stat) + ": "
						+ stats.getGlobalCount(stat)));
			}

			tabs.addTab("Global", null, panel, "Global statistics");
			tabs.setMnemonicAt(0, KeyEvent.VK_G);

		} { // statistics per material

			JComponent component = createTableComponent(
					columnNames, perMaterialData, comparators);

			tabs.addTab("Materials", null, component,
					"Statistics per material");

			tabs.setMnemonicAt(1, KeyEvent.VK_M);

		} { // statistics per class

			JComponent component = createTableComponent(
					columnNames, perClassData, comparators);

			tabs.addTab("Classes", null, component,
					"Statistics per WorldObject class");

			tabs.setMnemonicAt(2, KeyEvent.VK_C);

		}

		setSize(800, 500);

	}



	private static final Comparator<?> LONG_COMPARATOR = new Comparator<Object>() {
		@Override public int compare(Object o1, Object o2) {
			return ((Long)o1).compareTo((Long)o2);
		}
	};

	private static JComponent createTableComponent(String[] columnNames,
			Object[][] perMaterialData, Comparator<?>[] comparators) {

		JTable table = new JTable(perMaterialData, columnNames);
		table.setFillsViewportHeight(true);

		TableRowSorter<TableModel> sorter =
				new TableRowSorter<TableModel>(table.getModel());

		for (int col = 0; col < columnNames.length; ++col) {
			sorter.setComparator(col, comparators[col]);
		}

		table.setRowSorter(sorter);

		return new JScrollPane(table);

	}

}

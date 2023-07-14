package org.osm2world.viewer.view;

import static java.lang.Math.min;
import static java.util.Arrays.sort;
import static java.util.Arrays.stream;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javax.swing.*;

import org.osm2world.viewer.Viewer;

/**
 * keeps a "recent files" menu current based on a persistently stored list
 */
public class RecentFilesUpdater implements PreferenceChangeListener {

	private static final int MAX_ENTRIES = 10;

	private final JMenu recentFilesMenu;
	private final Consumer<File> openFileAction;

	/**
	 * creates the updater, registers it as a listener and builds the initial content of the menu
	 *
	 * @param openFileAction  function which can be used to open a file
	 */
	public RecentFilesUpdater(JMenu recentFilesMenu, Consumer<File> openFileAction) {

		this.recentFilesMenu = recentFilesMenu;
		this.openFileAction = openFileAction;

		prefs().addPreferenceChangeListener(this);

		updateRecentFilesMenu();

	}

	@Override
	public void preferenceChange(PreferenceChangeEvent e) {
		updateRecentFilesMenu();
	}

	/**
	 * builds (or rebuilds) the menu items in the {@link #recentFilesMenu}
	 * based on the current state of the preferences
	 */
	private void updateRecentFilesMenu() {

		recentFilesMenu.removeAll();

		try {

			for (File file : fileListFromPrefs()) {
				JMenuItem menuItem = new JMenuItem(file.getName());
				menuItem.addActionListener(e -> openFileAction.accept(file));
				recentFilesMenu.add(menuItem);
			}

		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		recentFilesMenu.setEnabled(recentFilesMenu.getItemCount() > 0);

	}

	/**
	 * returns the {@link Preferences} node used to persistently store the recent files list
	 */
	private static Preferences prefs() {
		return Preferences.userNodeForPackage(Viewer.class).node("recentFiles");
	}

	/**
	 * returns the list of recent files from user preferences.
	 * The list is sorted from newest to oldest entry.
	 */
	private static List<File> fileListFromPrefs() throws BackingStoreException {
		String[] keys = prefs().keys();
		sort(keys);
		return new ArrayList<>(stream(keys).map(key -> new File(prefs().get(key, null))).toList());
	}

	/**
	 * adds a file to the persistently stored list of recent files.
	 * May replace an existing entry if maximum capacity is reached.
	 */
	public static void addRecentFile(File newFile) {

		newFile = newFile.getAbsoluteFile();

		try {

			List<File> files = fileListFromPrefs();

			files.remove(newFile);
			files.add(0, newFile);

			prefs().clear();

			for (int i = 0; i < min(files.size(), MAX_ENTRIES); i++) {
				prefs().put("entry_" + (char)('a' + i),
						files.get(i).getAbsolutePath());
			}

		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

	}

}
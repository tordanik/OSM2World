package org.osm2world.viewer.view;

import static java.lang.Math.min;
import static java.util.Arrays.sort;

import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;
import java.util.prefs.Preferences;

import javax.swing.JMenu;
import javax.swing.JMenuItem;

import org.osm2world.viewer.Viewer;

import com.google.common.base.Function;

/**
 * keeps a "recent files" menu current based on a persistently stored list
 */
public class RecentFilesUpdater implements PreferenceChangeListener {

	private static final int MAX_ENTRIES = 10;

	private final JMenu recentFilesMenu;
	private final Function<File, ActionListener> actionForFile;

	/**
	 * creates the updater, registers it as a listener
	 * and builds the initial content of the menu
	 *
	 * @param actionForFile
	 *   function creating the listener for opening a particular file
	 */
	public RecentFilesUpdater(JMenu recentFilesMenu,
			Function<File, ActionListener> actionForFile) {

		this.recentFilesMenu = recentFilesMenu;
		this.actionForFile = actionForFile;

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
				menuItem.addActionListener(actionForFile.apply(file));
				recentFilesMenu.add(menuItem);
			}

		} catch (BackingStoreException e) {
			e.printStackTrace();
		}

		recentFilesMenu.setEnabled(recentFilesMenu.getItemCount() > 0);

	}

	/**
	 * returns the {@link Preferences} node used to persistently store the
	 * recent files list
	 */
	private static final Preferences prefs() {
		return Preferences.userNodeForPackage(Viewer.class).node("recentFiles");
	}

	/**
	 * returns the list of recent files from user preferences.
	 * The list is sorted from newest to oldest entry.
	 */
	private static final List<File> fileListFromPrefs()
			throws BackingStoreException {

		List<File> result = new ArrayList<File>();

		String[] keys = prefs().keys();
		sort(keys);

		for (int i = 0; i < keys.length; i++) {
			String pathname = prefs().get(keys[i], null);
			result.add(new File(pathname));
		}

		return result;

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
package org.osm2world.core.target.common.model;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.apache.commons.configuration.Configuration;
import org.osm2world.core.target.gltf.GltfModel;
import org.osm2world.core.world.creation.WorldModule;

/**
 * this class defines {@link Model}s that can be used by all {@link WorldModule}s,
 * similar to {@link org.osm2world.core.target.common.material.Materials}
 */
public class Models {

	/** prevents instantiation */
	private Models() {}

	private static final Map<String, Model> models = new HashMap<>();

	/**
	 * returns a model based on its name
	 *
	 * @param name  case-insensitive name of the material
	 */
	public static @Nullable Model getModel(@Nullable String name) {

		if (name == null) return null;

		for (String key : models.keySet()) {
			if (name.equalsIgnoreCase(key)) {
				return models.get(name);
			}
		}

		return null;

	}

	public static void configureModels(Configuration config) {

		models.clear();

		Iterator<String> keyIterator = config.getKeys();

		while (keyIterator.hasNext()) {

			String key = keyIterator.next();

			Matcher matcher = Pattern.compile("model_(.+)").matcher(key);

			if (matcher.matches()) {

				String modelName = matcher.group(1);
				String fileName = config.getString(key);

				try {
					Model model = GltfModel.loadFromFile(new File(fileName));
					models.put(modelName, model);
				} catch (IOException e) {
					System.err.println("Unable to load model " + modelName + ":");
					e.printStackTrace();
				}

			}
		}

	}

}

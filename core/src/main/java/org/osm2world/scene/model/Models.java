package org.osm2world.scene.model;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

import org.osm2world.conversion.O2WConfig;
import org.osm2world.output.gltf.GltfModel;
import org.osm2world.scene.material.Materials;
import org.osm2world.world.creation.WorldModule;

/**
 * this class defines {@link Model}s that can be used by all {@link WorldModule}s,
 * similar to {@link Materials}
 */
public class Models {

	/** prevents instantiation */
	private Models() {}

	/** map with all known models; keys are in lower case */
	private static final Map<String, List<Model>> models = new HashMap<>();

	/**
	 * returns a model based on its name, if one is available
	 *
	 * @param name  case-insensitive name of the material
	 */
	synchronized public static @Nullable Model getModel(@Nullable String name) {

		if (name == null) return null;

		List<Model> knownModels = models.get(name.toLowerCase(Locale.ROOT));
		if (knownModels != null && !knownModels.isEmpty()) {
			return knownModels.get(0);
		} else {
			return null;
		}

	}

	/**
	 * variant of {@link #getModel(String)} which picks one of several available models randomly.
	 */
	synchronized public static @Nullable Model getModel(@Nullable String name, Random random) {

		if (name == null) return null;

		List<Model> knownModels = models.get(name.toLowerCase(Locale.ROOT));
		if (knownModels != null && !knownModels.isEmpty()) {
			return knownModels.get(random.nextInt(knownModels.size()));
		} else {
			return null;
		}

	}

	synchronized public static void configureModels(O2WConfig config) {

		models.clear();

		Iterator<String> keyIterator = config.getKeys();

		while (keyIterator.hasNext()) {

			String key = keyIterator.next();

			Matcher matcher = Pattern.compile("model_(.+)").matcher(key);

			if (matcher.matches()) {

				String modelName = matcher.group(1);
				List<String> fileNames = config.getList(key);

				try {
					List<Model> ms = new ArrayList<>(fileNames.size());
					for (String fileName : fileNames) {
						URI modelUri = config.resolveFileConfigProperty(fileName, false, true);
						if (modelUri == null) {
							System.err.println("Can't read model file " + fileName);
						} else {
							ms.add(GltfModel.loadFromUri(modelUri, null, null));
						}
					}
					models.put(modelName.toLowerCase(Locale.ROOT), ms);
				} catch (IOException e) {
					System.err.println("Unable to load model " + modelName + ":");
					e.printStackTrace();
				}

			}
		}

	}

}

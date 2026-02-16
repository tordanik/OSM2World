package org.osm2world.map_data.data;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import javax.annotation.Nullable;

import org.osm2world.util.platform.json.JsonUtil;
import org.teavm.flavour.json.JsonPersistable;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * additional information associated with a {@link MapData} dataset that goes beyond what's directly part of the data.
 * For example, it includes information derived from country borders which contain the dataset, but are often far
 * beyond the dataset's bounding box.
 *
 * @param land  whether the dataset is at sea (false), on land (true), or unknown/mixed (null)
 */
@JsonPersistable
public record MapMetadata (@Nullable String locale, @Nullable Boolean land) {

	@JsonCreator
	public MapMetadata(@JsonProperty(value="locale", required=false) @Nullable String locale,
			@JsonProperty(value="land", required=false) @Nullable Boolean land) {
		this.locale = locale;
		this.land = land;
	}

	public static MapMetadata metadataFromJson(File metadataFile) throws IOException {
		try (var fileReader = new FileReader(metadataFile)) {
			return metadataFromJson(fileReader);
		}
	}

	public static MapMetadata metadataFromJson(Reader metadataReader) throws IOException {
		return JsonUtil.fromJson(metadataReader, MapMetadata.class);
	}

}

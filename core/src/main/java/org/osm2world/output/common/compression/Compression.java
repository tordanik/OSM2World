package org.osm2world.output.common.compression;

public enum Compression {

	NONE, ZIP, GZ;

	public String extension() {
		return switch (this) {
			case NONE -> "";
			case ZIP -> ".zip";
			case GZ -> ".gz";
		};
	}

}

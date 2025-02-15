package org.osm2world.conversion;

/**
 * implemented by classes that want to be informed about a conversion run's progress
 */
public interface ProgressListener {

	enum Phase {
		MAP_DATA,
		REPRESENTATION,
		ELEVATION,
		TERRAIN,
		OUTPUT,
		FINISHED
	}

	/**
	 * announces the current progress.
	 * Will be called at least at the start of each new {@link Phase}.
	 *
	 * @param progress  rough estimate of the conversion's progress as a value between 0.0 and 1.0
	 */
	void updateProgress(Phase currentPhase, double progress);

}

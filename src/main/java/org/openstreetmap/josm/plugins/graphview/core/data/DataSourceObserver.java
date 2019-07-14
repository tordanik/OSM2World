package org.openstreetmap.josm.plugins.graphview.core.data;

/**
 * observer that will be informed about changes in a DataSource
 * if it has been registered using {@link DataSource#addObserver(DataSourceObserver)}.
 * Not every DataSource will send updates, because some don't change.
 */
public interface DataSourceObserver {

	/**
	 * informs this observer about changes in an observed data source
	 * @param dataSource  observed data source that has changed; != null
	 */
	public void update(DataSource<?, ?, ?, ?> dataSource);

}

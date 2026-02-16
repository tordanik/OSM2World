package org.osm2world.util.uri;

import java.io.IOException;
import java.net.URI;

/**
 * Client to load content via HTTP(S).
 * Only for use inside {@link LoadUriUtil}.
 */
interface HttpUriImplementation {

    String fetchText(URI uri) throws IOException;
    byte[] fetchBinary(URI uri) throws IOException;

	boolean checkExists(URI uri);

}
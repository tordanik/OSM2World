package org.osm2world.util.platform.uri;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;

import org.junit.Test;

public class LoadUriUtilTest {

	@Test
	public void testDataUri() throws IOException {

		URI textBase64 = URI.create("data:text/vnd-example+xyz;foo=bar;base64,R0lGODdh");
		assertEquals("GIF87a", LoadUriUtil.fetchText(textBase64));

		URI textUTF8 = URI.create("data:text/plain;charset=UTF-8;page=21,the%20data:1234,5678");
		assertEquals("the data:1234,5678", LoadUriUtil.fetchText(textUTF8));

	}

}

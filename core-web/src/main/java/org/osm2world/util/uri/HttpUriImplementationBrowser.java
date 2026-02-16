package org.osm2world.util.uri;

import java.net.URI;

import org.teavm.interop.Async;
import org.teavm.interop.AsyncCallback;
import org.teavm.jso.ajax.XMLHttpRequest;
import org.teavm.jso.typedarrays.ArrayBuffer;
import org.teavm.jso.typedarrays.Int8Array;

/**
 * Implementation of {@link HttpUriImplementation} for use in the browser.
 */
public class HttpUriImplementationBrowser implements HttpUriImplementation {

	/** Sets up {@link LoadUriUtil} to use this implementation. */
	public static void register() {
		LoadUriUtil.setImplementation(new HttpUriImplementationBrowser());
	}

	@Async
	@Override
	public native String fetchText(URI uri);

	private void fetchText(URI uri, AsyncCallback<String> callback) {
		var xhr = new XMLHttpRequest();
		xhr.open("GET", uri.toString());
		xhr.setResponseType("text");
		xhr.setOnReadyStateChange(() -> {
			if (xhr.getReadyState() == 4) {
				if (xhr.getStatus() == 200) {
					callback.complete(xhr.getResponseText());
				} else {
					callback.error(new RuntimeException("HTTP " + xhr.getStatus() + " for URI " + uri));
				}
			}
		});
		xhr.send();
	}

	@Async
	@Override
	public native byte[] fetchBinary(URI uri);

	private void fetchBinary(URI uri, AsyncCallback<byte[]> handler) {
		var xhr = new XMLHttpRequest();
		xhr.open("GET", uri.toString());
		xhr.setResponseType("arraybuffer");
		xhr.setOnReadyStateChange(() -> {
			if (xhr.getReadyState() == 4) {
				if (xhr.getStatus() == 200) {
					ArrayBuffer buffer = xhr.getResponse().cast();
					byte[] data = arrayBufferToByteArray(buffer);
					handler.complete(data);
				} else {
					handler.error(new RuntimeException("HTTP " + xhr.getStatus() + " for URI " + uri));
				}
			}
		});
		xhr.send();
	}

	private byte[] arrayBufferToByteArray(ArrayBuffer buffer) {
		var int8Array = new Int8Array(buffer);
		byte[] bytes = new byte[int8Array.getLength()];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = int8Array.get(i);
		}
		return bytes;
	}

	@Async
	@Override
	public native boolean checkExists(URI uri);

	public void checkExists(URI uri, AsyncCallback<Boolean> handler) {
		var xhr = new XMLHttpRequest();
		xhr.open("HEAD", uri.toString());
		xhr.setOnReadyStateChange(() -> {
			if (xhr.getReadyState() == 4) {
				handler.complete(xhr.getStatus() == 200);
			}
		});
		xhr.send();
	}

}
package org.osm2world.core.target.common.model.obj.parser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class ModelLinksProxy {

	private String localCachePath;

	public ModelLinksProxy(String localCachePath) {
		this.localCachePath = localCachePath;
	}
	
	public static String resolveLink(URL base, String link) throws MalformedURLException {
		if (new File(link).isAbsolute()) {
			URL root = new URL(base.getProtocol() + "://" + base.getAuthority());
			return new URL(root, link).toString();
		}
		else {
			return new URL(base, link).toString();
		}
	}

	public File getFile(String link) {
		try {
			if(isURL(link)) {
				File file = getPathForObjUrl(link);
				return saveFile(file, new URL(link));
			}
			return null;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private File saveFile(File file, URL link)
			throws IOException, FileNotFoundException, MalformedURLException, InterruptedException {
		
		if (!file.exists()) {
			file.getParentFile().mkdirs();
			if(file.createNewFile()) {
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				try {
				    java.nio.channels.FileLock lock = fileOutputStream.getChannel().lock();
				    try {
				    	saveFile(fileOutputStream, link);
				    } finally {
				        lock.release();
				    }
				} finally {
					fileOutputStream.close();
				}
			}
		}
		
		// File exists, but might be locked by other thred/app for writing data
		// wait untill it will be released
		int timeout = 30 * 1000;
		while(!file.canWrite()) {
			if (timeout > 0) {
				Thread.sleep(100);
				timeout -= 100;
			}
			else {
				break;
			}
		}
		
		return file;
	}
	
	private void saveFile(FileOutputStream fileOutputStream, URL url) throws IOException {
		ReadableByteChannel rbc = Channels.newChannel(url.openStream());
		fileOutputStream.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
	}

	private File getPathForObjUrl(String link) {
		try {
			URL url = new URL(link);
			String path = url.getPath();
			
			return new File(localCachePath, path);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public File getLinkedFile(String local, String base) {
		try {
			String link = resolveLink(new URL(base), local);
			return getFile(link);
		}
		catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	private static boolean isURL(String link) {
		return link.startsWith("http://") || link.startsWith("https://");
	}

}

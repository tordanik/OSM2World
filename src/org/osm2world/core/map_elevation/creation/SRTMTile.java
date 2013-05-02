package org.osm2world.core.map_elevation.creation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;

/**
 * a single SRTM data tile.
 * 
 * Multiple such tiles are used by {@link SRTMData} to build coverage
 * for larger regions.
 */
class SRTMTile {

	/** value indicating a lack of data */
	public static final short BLANK_VALUE = -32768;
	
	/** length of each dimension of an SRTM tile in pixels */
	static final int PIXELS = 1201;
		
	public final File file;
	private final ShortBuffer data;
	
	public SRTMTile(File file) throws IOException {
		
		this.file = file;
		
		data = loadDataFromFile(file);
				
	}

	private static ShortBuffer loadDataFromFile(File file) throws IOException {

		FileInputStream fis = new FileInputStream(file);
		FileChannel fc = fis.getChannel();
		ByteBuffer bb = ByteBuffer.allocateDirect((int) fc.size());
		while (bb.remaining() > 0) fc.read(bb);
		fc.close();
		fis.close();
		
		bb.flip();
		
		// choose the right endianness
		return bb.order(ByteOrder.BIG_ENDIAN).asShortBuffer();
		
	}
	
	public final short getData(int x, int y) {
		assert 0 <= x && x < PIXELS && 0 <= y && y < PIXELS;
		return data.get((1200 - y) * 1201 + x);
	}
	
	@Override
	public String toString() {
		return file.getName();
	}
	
}

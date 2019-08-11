package org.osm2world.core.target.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.osm2world.core.target.common.material.TexCoordFunction;

import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;

public class ImageTextureData extends TextureData {

	/** 
	 * Path to the texture file.
	 * Represents a permanent, already saved
	 * image file in contrast to the file 
	 * field in {@link TextTextureData} 
	 */
	private final File file;
	
	private File convertedToPng;
	
	public ImageTextureData(File file, double width, double height, Wrap wrap, TexCoordFunction texCoordFunction,
			boolean colorable, boolean isBumpMap) {
		
		super(width, height, wrap, texCoordFunction, colorable, isBumpMap);
		
		this.file = file;
		this.convertedToPng = null;
	}
	
	public File getFile() {
		
		if(this.file.getName().endsWith(".svg") && this.convertedToPng == null) {
			convertedToPng = SVG2PNG(this.file);
		}
		
		if(this.file.getName().endsWith(".svg") && this.convertedToPng != null) {
			return convertedToPng;
		}
		
		return this.file;
		
	}
	
	
	/**
	 * Converts an .svg image file into a (temporary) .png
	 * 
	 * @param svg The svg file to be converted
	 * @return a File object representation of the generated png
	 */
	private File SVG2PNG(File svg) {
		
		String prefix = svg.getName().substring(0, svg.getName().indexOf('.')) + "osm2World";
		
		//create a temporary file in the default temporary-file directory
		File outputFile = null;
		try {
			outputFile = File.createTempFile(prefix, ".png");
			outputFile.deleteOnExit();
		} catch (IOException e) {
			System.err.println("Temporary file "+prefix+".png could not be created");
			e.printStackTrace();
		}
		
		//create the transcoder input
        String svgURI = svg.toURI().toString();
        TranscoderInput input = new TranscoderInput(svgURI);
        
        //create the transcoder output
        OutputStream ostream = null; //TODO: remove =null
        
		try {
			ostream = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
        TranscoderOutput output = new TranscoderOutput(ostream);
        
        PNGTranscoder t = new PNGTranscoder();
     
        //save the image.
        try {
			t.transcode(input, output);
		} catch (TranscoderException e) {
			System.err.println("Could not convert svg to png: "+svg.getName());
			e.printStackTrace();
		}
        
        //clear and close the stream
        try {
			ostream.flush();
			ostream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
        
        return outputFile;
	}
	
	@Override
	public String toString() {
		return "ImageTextureData [file=" + file + ", width=" + width + ", height=" + height + ", wrap=" + wrap
				+ ", texCoordFunction=" + coordFunction + ", colorable=" + colorable + ", isBumpMap=" + isBumpMap + "]";
	}

	//auto-generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		return result;
	}

	//auto-generated
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ImageTextureData other = (ImageTextureData) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		return true;
	}
	
	

}


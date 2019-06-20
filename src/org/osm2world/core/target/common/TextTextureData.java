package org.osm2world.core.target.common;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.osm2world.core.target.common.material.TexCoordFunction;

public class TextTextureData extends TextureData {
	
	/**
	 * File generated based on {@link #text}
	 * and temporarily saved until application termination
	 */
	private File file;
	
	public String text;
	public Font font;
	public final double topOffset;
	public final double leftOffset;
	
	public TextTextureData(String t, Font font, double w, double h, double topOffset, double leftOffset, 
			Wrap wrap, TexCoordFunction texCoordFunction, boolean colorable, boolean isBumpMap) {
		
		super(w, h, wrap, texCoordFunction, colorable, isBumpMap);
		
		this.text = t;
		this.font = font;
		this.topOffset = topOffset;
		this.leftOffset = leftOffset;
		this.file = null;
	}
	
	public File getFile() {
		
		if(file == null) {
			
			if(!(text.equals(""))) {
			
				//temporary BufferedImage to extract font metrics
				BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = image.createGraphics();
				
				Font font = this.font == null ? new Font("Interstate", Font.BOLD, 100) : this.font ; //Interstate defaults to "Dialog" right now
				//extract font metrics
				FontMetrics fm = g2d.getFontMetrics(font);
				int stringWidth = fm.stringWidth(this.text);
				int stringHeight = fm.getHeight();
				g2d.dispose();
				
				//image with actual size and text
				int imageWidth = (int) (stringWidth + stringWidth*leftOffset/100);
				int imageHeight = (int) (stringHeight + stringHeight*leftOffset/100);
				image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
				g2d = image.createGraphics();
				g2d.setFont(font);
				g2d.setPaint(Color.black);
				
				//centered text
				int xCoord = (int)(imageWidth*leftOffset/100 - stringWidth/2);
				int yCoord = (int)(imageHeight*topOffset/100 + stringHeight/3 );
				
				g2d.drawString(this.text, xCoord, yCoord);
				
				g2d.dispose();
						
				String prefix = text+"osm2world";
				
				this.file = createPng(prefix, image);
				return this.file;
				
			} else { //create blank texture
				
				BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
				
				String prefix = "osm2world";
				
				this.file = createPng(prefix, image);
				return this.file;
			}
		}
		
		return this.file;
	}
	
	private File createPng(String prefix, BufferedImage image) {
		
		File outputFile = null;
		
		try {
			outputFile = File.createTempFile(prefix, ".png");
			outputFile.deleteOnExit();
			ImageIO.write(image, "png", outputFile);
		}catch(IOException e) {	
			System.err.println("Exception in createPng: "+prefix);
			e.printStackTrace();		
		}
		
		return outputFile;
	}
	
	//auto-generated
	@Override
	public String toString() {
		return "TextTextureData [text=" + text + ", file=" + file + ", font=" + font + ", topOffset=" + topOffset
				+ ", leftOffset=" + leftOffset + "]";
	}

	//auto-generated
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((file == null) ? 0 : file.hashCode());
		result = prime * result + ((font == null) ? 0 : font.hashCode());
		long temp;
		temp = Double.doubleToLongBits(leftOffset);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((text == null) ? 0 : text.hashCode());
		temp = Double.doubleToLongBits(topOffset);
		result = prime * result + (int) (temp ^ (temp >>> 32));
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
		TextTextureData other = (TextTextureData) obj;
		if (file == null) {
			if (other.file != null)
				return false;
		} else if (!file.equals(other.file))
			return false;
		if (font == null) {
			if (other.font != null)
				return false;
		} else if (!font.equals(other.font))
			return false;
		if (Double.doubleToLongBits(leftOffset) != Double.doubleToLongBits(other.leftOffset))
			return false;
		if (text == null) {
			if (other.text != null)
				return false;
		} else if (!text.equals(other.text))
			return false;
		if (Double.doubleToLongBits(topOffset) != Double.doubleToLongBits(other.topOffset))
			return false;
		return true;
	}
	
}

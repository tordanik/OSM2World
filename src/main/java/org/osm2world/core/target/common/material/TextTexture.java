package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class TextTexture extends TextureData {

	/**
	 * File generated based on {@link #text}
	 * and temporarily saved until application termination
	 */
	private File file = null;

	public final String text;
	public Font font;
	public final double topOffset;
	public final double leftOffset;
	public Color textColor;

	/**
	 * A scalar value to determine the size of the rendered text
	 * in regards to the size of the image
	 */
	public final double relativeFontSize;

	public TextTexture(String t, Font font, double w, double h, double topOffset, double leftOffset, Color textColor, double relativeFontSize,
			Wrap wrap, TexCoordFunction texCoordFunction) {

		super(w, h, wrap, texCoordFunction);

		this.text = t;
		this.font = font;
		this.topOffset = topOffset;
		this.leftOffset = leftOffset;
		this.textColor = textColor;
		this.relativeFontSize = relativeFontSize;

	}

	@Override
	public File getRasterImage() {

		if(file == null) {

			if(!(text.equals(""))) {

				//temporary BufferedImage to extract font metrics
				BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
				Graphics2D g2d = image.createGraphics();

				Font font = this.font == null ? new Font("Interstate", Font.BOLD, 100) : this.font ;

				//extract font metrics
				FontMetrics fm = g2d.getFontMetrics(font);
				int stringWidth = fm.stringWidth(this.text);
				int stringHeight = fm.getHeight();
				g2d.dispose();

				//image with actual size and text
				int imageHeight = (int) (stringHeight/(relativeFontSize/100));

				double signAspectRatio = this.width/this.height;
				int imageWidth = (int) (imageHeight*signAspectRatio);

				image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
				g2d = image.createGraphics();
				g2d.setFont(font);
				g2d.setPaint(textColor);

				//place text
				int xCoord = (int)(imageWidth*leftOffset/100 - stringWidth/2);
				int yCoord = (int)(imageHeight*topOffset/100 + stringHeight/3 );

				g2d.drawString(this.text, xCoord, yCoord);

				g2d.dispose();

				String prefix = text+"osm2world";

				this.file = createPng(prefix, image);
				return this.file;

			} else {

				//create blank texture

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

	public static enum FontStyle {

		PLAIN, BOLD, ITALIC;

		private static final Map<String, FontStyle> map = new HashMap<>(FontStyle.values().length);

		//initialize the map
		static {
			map.put("PLAIN", PLAIN);
			map.put("BOLD", BOLD);
			map.put("ITALIC", ITALIC);
		}

		public static int getStyle(String s) {

			/*using a map will return null if s is not a valid input
			whereas valueOf(s) would result in a RuntimeException*/
			FontStyle style = map.get(s);

			if(style==null) {
				return Font.PLAIN;
			}else {

				switch(style) {

					case PLAIN:
						return Font.PLAIN;
					case BOLD:
						return Font.BOLD;
					case ITALIC:
						return Font.ITALIC;
					default:
						return Font.PLAIN;
				}
			}
		}
	}

	@Override
	public String toString() {
		return text;
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
		TextTexture other = (TextTexture) obj;
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

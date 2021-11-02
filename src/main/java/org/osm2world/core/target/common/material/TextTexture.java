package org.osm2world.core.target.common.material;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.osm2world.core.target.common.texcoord.TexCoordFunction;

public class TextTexture extends RuntimeTexture {

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

	public TextTexture(String text, Font font, double width, double height,
			@Nullable Double widthPerEntity, @Nullable Double heightPerEntity,
			double topOffset, double leftOffset, Color textColor, double relativeFontSize,
			Wrap wrap, Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {

		super(width, height, widthPerEntity, heightPerEntity, wrap, texCoordFunction);

		this.text = text;
		this.font = font;
		this.topOffset = topOffset;
		this.leftOffset = leftOffset;
		this.textColor = textColor;
		this.relativeFontSize = relativeFontSize;

	}

	@Override
	public BufferedImage getBufferedImage() {

		if (!text.isEmpty()) {

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

			return image;

		} else {

			//create blank texture
			return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		}

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

}

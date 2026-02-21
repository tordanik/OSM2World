package org.osm2world.scene.material;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.osm2world.scene.color.Color;
import org.osm2world.scene.texcoord.TexCoordFunction;

import com.google.common.base.Objects;

public class TextTexture extends RuntimeTexture {

	public enum FontStyle {

		PLAIN, BOLD, ITALIC;

		public static FontStyle parseValue(String s) {

			if (s == null) {
				return PLAIN;
			} else {
				return switch (s.toUpperCase()) {
					case "BOLD" -> BOLD;
					case "ITALIC" -> ITALIC;
					default -> PLAIN;
				};
			}

		}

		public int getFontStyleInt() {
			return switch (this) {
				case PLAIN -> Font.PLAIN;
				case BOLD -> Font.BOLD;
				case ITALIC -> Font.ITALIC;
			};
		}

	}

	public final String text;
	public final @Nullable String fontName;
	public final FontStyle fontStyle;
	public final double topOffset;
	public final double leftOffset;
	public Color textColor;

	/**
	 * A scalar value to determine the size of the rendered text
	 * in regard to the size of the image
	 */
	public final double relativeFontSize;

	public TextTexture(String text, @Nullable String fontName, FontStyle fontStyle, TextureDataDimensions dimensions,
			double topOffset, double leftOffset, Color textColor, double relativeFontSize,
			Wrap wrap, Function<TextureDataDimensions, TexCoordFunction> texCoordFunction) {

		super(dimensions, wrap, texCoordFunction);

		this.text = text;
		this.fontName = fontName;
		this.fontStyle = fontStyle;
		this.topOffset = topOffset;
		this.leftOffset = leftOffset;
		this.textColor = textColor;
		this.relativeFontSize = relativeFontSize;

	}

	@Override
	public BufferedImage createBufferedImage() {

		if (!text.isEmpty()) {

			//temporary BufferedImage to extract font metrics
			BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

			Graphics2D g2d = image.createGraphics();

			Font font = new Font(fontName != null ? fontName : "Dialog", fontStyle.getFontStyleInt(), 100);

			//extract font metrics
			FontMetrics fm = g2d.getFontMetrics(font);
			int stringWidth = fm.stringWidth(this.text);
			int stringHeight = fm.getHeight();
			g2d.dispose();

			//image with actual size and text
			int imageHeight = (int) (stringHeight/(relativeFontSize/100));

			double signAspectRatio = this.dimensions.width()/this.dimensions.height();
			int imageWidth = (int) (imageHeight*signAspectRatio);

			image = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_ARGB);
			g2d = image.createGraphics();
			g2d.setFont(font);
			g2d.setPaint(new java.awt.Color(textColor.getRGB()));

			//place text
			int xCoord = (int)(imageWidth*leftOffset/100 - (double) stringWidth / 2);
			int yCoord = (int)(imageHeight*topOffset/100 + (double) stringHeight / 3);

			g2d.drawString(this.text, xCoord, yCoord);

			g2d.dispose();

			return image;

		} else {

			//create blank texture
			return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);

		}

	}

	@Override
	public String toString() {
		return text;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (!(obj instanceof TextTexture other)) return false;
		return dimensions().equals(other.dimensions())
				&& Objects.equal(wrap, other.wrap)
				&& Objects.equal(coordFunction, other.coordFunction)
				&& Objects.equal(text, other.text)
				&& Objects.equal(fontName, other.fontName)
				&& Objects.equal(fontStyle, other.fontStyle)
				&& topOffset == other.topOffset
				&& leftOffset == other.leftOffset
				&& Objects.equal(textColor, other.textColor)
				&& relativeFontSize == other.relativeFontSize;
	}

	@Override
	public int hashCode() {
		HashCodeBuilder builder = new HashCodeBuilder();
		builder.append(dimensions());
		builder.append(wrap);
		builder.append(coordFunction);
		builder.append(text);
		builder.append(fontName);
		builder.append(fontStyle);
		builder.append(topOffset);
		builder.append(leftOffset);
		builder.append(textColor);
		builder.append(relativeFontSize);
		return builder.toHashCode();
	}

}

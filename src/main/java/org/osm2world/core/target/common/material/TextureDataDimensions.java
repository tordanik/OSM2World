package org.osm2world.core.target.common.material;

import javax.annotation.Nullable;

/**
 * the dimensions of a texture. See {@link TextureData} for the meaning for the fields.
 */
public class TextureDataDimensions {

	public final double width;
	public final double height;
	public final @Nullable Double widthPerEntity;
	public final @Nullable Double heightPerEntity;

	public TextureDataDimensions(double width, double height,
			@Nullable Double widthPerEntity, @Nullable Double heightPerEntity) {
		this.width = width;
		this.height = height;
		this.widthPerEntity = widthPerEntity;
		this.heightPerEntity = heightPerEntity;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(height);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((heightPerEntity == null) ? 0 : heightPerEntity.hashCode());
		temp = Double.doubleToLongBits(width);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		result = prime * result + ((widthPerEntity == null) ? 0 : widthPerEntity.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TextureDataDimensions other = (TextureDataDimensions) obj;
		if (Double.doubleToLongBits(height) != Double.doubleToLongBits(other.height))
			return false;
		if (heightPerEntity == null) {
			if (other.heightPerEntity != null)
				return false;
		} else if (!heightPerEntity.equals(other.heightPerEntity))
			return false;
		if (Double.doubleToLongBits(width) != Double.doubleToLongBits(other.width))
			return false;
		if (widthPerEntity == null) {
			if (other.widthPerEntity != null)
				return false;
		} else if (!widthPerEntity.equals(other.widthPerEntity))
			return false;
		return true;
	}

}

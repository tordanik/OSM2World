package org.teavm.classlib.java.awt.geom;

/**
 * minimal replacement for java.awt.geom.Rectangle2D, which TeaVM doesn't provide.
 * TeaVM uses this class in place of the original (based on the package and the "T" prefix).
 * Only contains the parts used by the Tinfour library.
 */
public abstract class TRectangle2D {

	public abstract double getX();
	public abstract double getY();
	public abstract double getWidth();
	public abstract double getHeight();

	public abstract void setRect(double x, double y, double w, double h);

	public double getMinX() {
		return getX();
	}

	public double getMinY() {
		return getY();
	}

	public double getMaxX() {
		return getX() + getWidth();
	}

	public double getMaxY() {
		return getY() + getHeight();
	}

	/** expands this rectangle to include the point */
	public void add(double newX, double newY) {
		double x1 = Math.min(getMinX(), newX);
		double x2 = Math.max(getMaxX(), newX);
		double y1 = Math.min(getMinY(), newY);
		double y2 = Math.max(getMaxY(), newY);
		setRect(x1, y1, x2 - x1, y2 - y1);
	}

	public static class Double extends TRectangle2D {

		public double x;
		public double y;
		public double width;
		public double height;

		public Double() {}

		public Double(double x, double y, double w, double h) {
			setRect(x, y, w, h);
		}

		@Override
		public double getX() {
			return x;
		}

		@Override
		public double getY() {
			return y;
		}

		@Override
		public double getWidth() {
			return width;
		}

		@Override
		public double getHeight() {
			return height;
		}

		@Override
		public void setRect(double x, double y, double w, double h) {
			this.x = x;
			this.y = y;
			this.width = w;
			this.height = h;
		}

	}

}

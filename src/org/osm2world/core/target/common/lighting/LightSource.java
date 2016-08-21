package org.osm2world.core.target.common.lighting;

import java.awt.Color;
import org.osm2world.core.math.VectorXYZW;
import org.osm2world.core.math.VectorXYZ;


public class LightSource {
	public final VectorXYZW pos;
	public final LightColor color;

	public LightSource(VectorXYZ pos, Color La, Color Ls, Color Ld) {
		this.pos = new VectorXYZW(pos.x, pos.y, pos.z, 1.0);
		this.color = new LightColor(La, Ls, Ld);
	}

	public LightSource(VectorXYZW pos, Color La, Color Ls, Color Ld) {
		this.pos = pos;
		this.color = new LightColor(La, Ls, Ld);
	}


	public LightSource(VectorXYZ pos, Color color) {
		this(pos, color, color, color);
	}

	public LightSource(VectorXYZW pos, Color color) {
		this(pos, color, color, color);
	}


	@Override
	public String toString() {
		return "Light source: "+ pos + " @ "+ color;
	}


	public class LightColor {
		public final Color La;
		public final Color Ls;
		public final Color Ld;

		public LightColor(Color La, Color Ls, Color Ld) {
			this.La = La;
			this.Ls = Ls;
			this.Ld = Ld;
		}

		@Override
		public boolean equals(Object o) {
			if(o instanceof LightColor)
				return La.equals(((LightColor)o).La)
						&& Ls.equals(((LightColor)o).Ls)
						&& Ld.equals(((LightColor)o).Ld);
			return false;
		}

		@Override
		public int hashCode() {
			return La.hashCode() ^ Ls.hashCode() ^ Ld.hashCode();
		}

		@Override
		public String toString() {
			return "LightColor[la="+ La +", ls=" + Ls + ", ld=" + Ld + "]";
		}

	}

}

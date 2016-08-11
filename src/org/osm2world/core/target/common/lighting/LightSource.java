package org.osm2world.core.target.common.lighting;

import java.awt.Color;
import org.osm2world.core.math.VectorXYZW;
import org.osm2world.core.math.VectorXYZ;


public class LightSource {
	public final VectorXYZW pos;
	public final Color La;
	public final Color Ls;
	public final Color Ld;

	public LightSource(VectorXYZ pos, Color La, Color Ls, Color Ld) {
		this.pos = new VectorXYZW(pos.x, pos.y, pos.z, 1.0);
		this.La = La;
		this.Ls = Ls;
		this.Ld = Ld;
	}

	public LightSource(VectorXYZ pos, Color color) {
		this(pos, color, color, color);
	}

	public LightSource(VectorXYZW pos, Color La, Color Ls, Color Ld) {
		this.pos = pos;
		this.La = La;
		this.Ls = Ls;
		this.Ld = Ld;
	}

	public LightSource(VectorXYZW pos, Color color) {
		this(pos, color, color, color);
	}


	@Override
	public String toString() {
		return "Light source: "+ pos + " @ "+ La +", " + Ls + ", " + Ld;
	}
}

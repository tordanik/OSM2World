package org.osm2world.scene.color;

/** utility class providing common sets of color names and related methods */
public final class ColorNameDefinitions {

	private ColorNameDefinitions() {}

	/** produces a combined definition by looking up any names missing from the first definition in the second one */
	public static ColorNameDefinition combine(ColorNameDefinition d1, ColorNameDefinition d2) {
		return (String name) -> {
			if (d1.contains(name)) {
				return d1.get(name);
			} else {
				return d2.get(name);
			}
		};
	}

	public static final ColorNameDefinition CSS_COLORS = (String name) -> {

		return switch (name) {
			case "aliceblue" -> new Color(240, 248, 255);
			case "antiquewhite" -> new Color(250, 235, 215);
			case "aqua" -> new Color(0, 255, 255);
			case "aquamarine" -> new Color(127, 255, 212);
			case "azure" -> new Color(240, 255, 255);
			case "beige" -> new Color(245, 245, 220);
			case "bisque" -> new Color(255, 228, 196);
			case "black" -> new Color(0, 0, 0);
			case "blanchedalmond" -> new Color(255, 235, 205);
			case "blue" -> new Color(0, 0, 255);
			case "blueviolet" -> new Color(138, 43, 226);
			case "brown" -> new Color(165, 42, 42);
			case "burlywood" -> new Color(222, 184, 135);
			case "cadetblue" -> new Color(95, 158, 160);
			case "chartreuse" -> new Color(127, 255, 0);
			case "chocolate" -> new Color(210, 105, 30);
			case "coral" -> new Color(255, 127, 80);
			case "cornflowerblue" -> new Color(100, 149, 237);
			case "cornsilk" -> new Color(255, 248, 220);
			case "crimson" -> new Color(220, 20, 60);
			case "cyan" -> new Color(0, 255, 255);
			case "darkblue" -> new Color(0, 0, 139);
			case "darkcyan" -> new Color(0, 139, 139);
			case "darkgoldenrod" -> new Color(184, 134, 11);
			case "darkgray" -> new Color(169, 169, 169);
			case "darkgreen" -> new Color(0, 100, 0);
			case "darkgrey" -> new Color(169, 169, 169);
			case "darkkhaki" -> new Color(189, 183, 107);
			case "darkmagenta" -> new Color(139, 0, 139);
			case "darkolivegreen" -> new Color(85, 107, 47);
			case "darkorange" -> new Color(255, 140, 0);
			case "darkorchid" -> new Color(153, 50, 204);
			case "darkred" -> new Color(139, 0, 0);
			case "darksalmon" -> new Color(233, 150, 122);
			case "darkseagreen" -> new Color(143, 188, 143);
			case "darkslateblue" -> new Color(72, 61, 139);
			case "darkslategray" -> new Color(47, 79, 79);
			case "darkslategrey" -> new Color(47, 79, 79);
			case "darkturquoise" -> new Color(0, 206, 209);
			case "darkviolet" -> new Color(148, 0, 211);
			case "deeppink" -> new Color(255, 20, 147);
			case "deepskyblue" -> new Color(0, 191, 255);
			case "dimgray" -> new Color(105, 105, 105);
			case "dimgrey" -> new Color(105, 105, 105);
			case "dodgerblue" -> new Color(30, 144, 255);
			case "firebrick" -> new Color(178, 34, 34);
			case "floralwhite" -> new Color(255, 250, 240);
			case "forestgreen" -> new Color(34, 139, 34);
			case "fuchsia" -> new Color(255, 0, 255);
			case "gainsboro" -> new Color(220, 220, 220);
			case "ghostwhite" -> new Color(248, 248, 255);
			case "gold" -> new Color(255, 215, 0);
			case "goldenrod" -> new Color(218, 165, 32);
			case "gray" -> new Color(128, 128, 128);
			case "green" -> new Color(0, 128, 0);
			case "greenyellow" -> new Color(173, 255, 47);
			case "grey" -> new Color(128, 128, 128);
			case "honeydew" -> new Color(240, 255, 240);
			case "hotpink" -> new Color(255, 105, 180);
			case "indianred" -> new Color(205, 92, 92);
			case "indigo" -> new Color(75, 0, 130);
			case "ivory" -> new Color(255, 255, 240);
			case "khaki" -> new Color(240, 230, 140);
			case "lavender" -> new Color(230, 230, 250);
			case "lavenderblush" -> new Color(255, 240, 245);
			case "lawngreen" -> new Color(124, 252, 0);
			case "lemonchiffon" -> new Color(255, 250, 205);
			case "lightblue" -> new Color(173, 216, 230);
			case "lightcoral" -> new Color(240, 128, 128);
			case "lightcyan" -> new Color(224, 255, 255);
			case "lightgoldenrodyellow" -> new Color(250, 250, 210);
			case "lightgray" -> new Color(211, 211, 211);
			case "lightgreen" -> new Color(144, 238, 144);
			case "lightgrey" -> new Color(211, 211, 211);
			case "lightpink" -> new Color(255, 182, 193);
			case "lightsalmon" -> new Color(255, 160, 122);
			case "lightseagreen" -> new Color(32, 178, 170);
			case "lightskyblue" -> new Color(135, 206, 250);
			case "lightslategray" -> new Color(119, 136, 153);
			case "lightslategrey" -> new Color(119, 136, 153);
			case "lightsteelblue" -> new Color(176, 196, 222);
			case "lightyellow" -> new Color(255, 255, 224);
			case "lime" -> new Color(0, 255, 0);
			case "limegreen" -> new Color(50, 205, 50);
			case "linen" -> new Color(250, 240, 230);
			case "magenta" -> new Color(255, 0, 255);
			case "maroon" -> new Color(128, 0, 0);
			case "mediumaquamarine" -> new Color(102, 205, 170);
			case "mediumblue" -> new Color(0, 0, 205);
			case "mediumorchid" -> new Color(186, 85, 211);
			case "mediumpurple" -> new Color(147, 112, 219);
			case "mediumseagreen" -> new Color(60, 179, 113);
			case "mediumslateblue" -> new Color(123, 104, 238);
			case "mediumspringgreen" -> new Color(0, 250, 154);
			case "mediumturquoise" -> new Color(72, 209, 204);
			case "mediumvioletred" -> new Color(199, 21, 133);
			case "midnightblue" -> new Color(25, 25, 112);
			case "mintcream" -> new Color(245, 255, 250);
			case "mistyrose" -> new Color(255, 228, 225);
			case "moccasin" -> new Color(255, 228, 181);
			case "navajowhite" -> new Color(255, 222, 173);
			case "navy" -> new Color(0, 0, 128);
			case "oldlace" -> new Color(253, 245, 230);
			case "olive" -> new Color(128, 128, 0);
			case "olivedrab" -> new Color(107, 142, 35);
			case "orange" -> new Color(255, 165, 0);
			case "orangered" -> new Color(255, 69, 0);
			case "orchid" -> new Color(218, 112, 214);
			case "palegoldenrod" -> new Color(238, 232, 170);
			case "palegreen" -> new Color(152, 251, 152);
			case "paleturquoise" -> new Color(175, 238, 238);
			case "palevioletred" -> new Color(219, 112, 147);
			case "papayawhip" -> new Color(255, 239, 213);
			case "peachpuff" -> new Color(255, 218, 185);
			case "peru" -> new Color(205, 133, 63);
			case "pink" -> new Color(255, 192, 203);
			case "plum" -> new Color(221, 160, 221);
			case "powderblue" -> new Color(176, 224, 230);
			case "purple" -> new Color(128, 0, 128);
			case "red" -> new Color(255, 0, 0);
			case "rosybrown" -> new Color(188, 143, 143);
			case "royalblue" -> new Color(65, 105, 225);
			case "saddlebrown" -> new Color(139, 69, 19);
			case "salmon" -> new Color(250, 128, 114);
			case "sandybrown" -> new Color(244, 164, 96);
			case "seagreen" -> new Color(46, 139, 87);
			case "seashell" -> new Color(255, 245, 238);
			case "sienna" -> new Color(160, 82, 45);
			case "silver" -> new Color(192, 192, 192);
			case "skyblue" -> new Color(135, 206, 235);
			case "slateblue" -> new Color(106, 90, 205);
			case "slategray" -> new Color(112, 128, 144);
			case "slategrey" -> new Color(112, 128, 144);
			case "snow" -> new Color(255, 250, 250);
			case "springgreen" -> new Color(0, 255, 127);
			case "steelblue" -> new Color(70, 130, 180);
			case "tan" -> new Color(210, 180, 140);
			case "teal" -> new Color(0, 128, 128);
			case "thistle" -> new Color(216, 191, 216);
			case "tomato" -> new Color(255, 99, 71);
			case "turquoise" -> new Color(64, 224, 208);
			case "violet" -> new Color(238, 130, 238);
			case "wheat" -> new Color(245, 222, 179);
			case "white" -> new Color(255, 255, 255);
			case "whitesmoke" -> new Color(245, 245, 245);
			case "yellow" -> new Color(255, 255, 0);
			case "yellowgreen" -> new Color(154, 205, 50);
			default -> null;
		};

	};

}

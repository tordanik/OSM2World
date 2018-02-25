package org.osm2world.core.target.common.model.obj.parser;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
import org.osm2world.core.math.AxisAlignedBoundingBoxXYZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.TextureData;
import org.osm2world.core.target.common.TextureData.Wrap;
import org.osm2world.core.target.common.material.ConfMaterial;
import org.osm2world.core.target.common.material.Material;
import org.osm2world.core.target.common.material.Material.Interpolation;
import org.osm2world.core.target.common.material.Material.Shadow;
import org.osm2world.core.target.common.material.Material.Transparency;
import org.osm2world.core.target.common.material.NamedTexCoordFunction;

public class ObjModel {
	
	private File objFile;
	private Map<String, Material> materials = new HashMap<>();
	private ObjMaterial curentMtl;
	
	private List<VectorXYZ> vertexes = new ArrayList<>();
	private List<VectorXYZ> vertexNorms = new ArrayList<>();
	private List<VectorXZ> textureVertexes = new ArrayList<>();
	private Material faceMtl;
	private List<ObjFace> faces = new ArrayList<>();
	private AxisAlignedBoundingBoxXYZ bbox;
	private ModelLinksProxy proxy;
	private String base;
	
	private static final class ObjMaterial {
		String materialFileLink;
		public String name;
		
		float ni;

		float[] ka;
		float[] kd;
		float[] ks;
		
		String map_Ka;
		String map_Kd;
		
	}
	
	private static String strip(String str) {
		return StringUtils.strip(str, " \t");
	}

	private static String parseStringParam(String line, String key) {
		return strip(StringUtils.removeStartIgnoreCase(line, key));
	}
	
	private float parseFloatParam(String line, String key) {
		return Float.valueOf(strip(StringUtils.removeStartIgnoreCase(line, key)));
	}
	
	private float[] parseFloatTriplet(String key, String line) {
		String numbers = strip(StringUtils.removeStartIgnoreCase(line, key));
		String[] split = StringUtils.split(numbers, " \t");
		return new float[]{
				Float.valueOf(split[0]),
				Float.valueOf(split[1]),
				Float.valueOf(split[2])
		};
	}
	
	private float[] parseFloatDuplet(String key, String line) {
		String numbers = strip(StringUtils.removeStartIgnoreCase(line, key));
		String[] split = StringUtils.split(numbers, " \t");
		return new float[]{
				Float.valueOf(split[0]),
				Float.valueOf(split[1])
		};
	}

	public ObjModel(String link, ModelLinksProxy resolver) {
		this.proxy = resolver;
		this.base = link;
		this.objFile = this.proxy.getFile(link);
		
		try {
			this.iterateOverObjFile();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void iterateOverObjFile() throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(objFile));  
		String line = br.readLine();  
		while (line != null)  
		{  
			line = StringUtils.strip(line);
			if (StringUtils.startsWithIgnoreCase(line, "#")) {
				handleObjComment(line);
			}
			else if (StringUtils.startsWithIgnoreCase(line, "call")){
				handleCall(line);
			}
			else if (StringUtils.startsWithIgnoreCase(line, "mtllib")){
				handleMtlLib(line);
			}
			else {
				handleObjLine(line);
			}
			
		    line = br.readLine();  
		}
		br.close();
		clear();
	}

	private void handleCall(String line) {
		System.err.println("call statement in obj is not supported");
	}

	private void handleMtlLib(String line) throws IOException {
		String mtlPath = parseStringParam(line, "mtllib");
		String mtlLink = ModelLinksProxy.resolveLink(new URL(this.base), mtlPath);
		File mtlFile = this.proxy.getLinkedFile(mtlLink, this.base);
		
		parseMtlFile(mtlFile, mtlLink);
	}

	private void parseMtlFile(File mtlFile, String mtlLink) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(mtlFile));  
		String line = StringUtils.strip(br.readLine());
		while (line != null)  
		{  
			if (line.startsWith("#")) {
				handleMtlComment(line);
			}
			else if(StringUtils.startsWithIgnoreCase(line, "newmtl")) {
				handleMtlNewMtl(line, mtlLink);
			}
			else if(StringUtils.startsWithIgnoreCase(line, "ka")) {
				handleMtlKa(line);
			}
			else if(StringUtils.startsWithIgnoreCase(line, "kd")) {
				handleMtlKd(line);
			}
			else if(StringUtils.startsWithIgnoreCase(line, "ks")) {
				handleMtlKs(line);
			}
			else if(StringUtils.startsWithIgnoreCase(line, "ni")) {
				handleMtlNi(line);
			}
			else if(StringUtils.startsWithIgnoreCase(line, "map_Ka")) {
				handleMtlMapKa(line);
			}
			else if(StringUtils.startsWithIgnoreCase(line, "map_Kd")) {
				handleMtlMapKd(line);
			}
			
		    line = StringUtils.strip(br.readLine());  
		    line = StringUtils.remove(line, '\t');
		}
		br.close();
		parseCurentMtl();
	}

	private void handleMtlMapKd(String line) {
		this.curentMtl.map_Kd = parseStringParam(line, "map_kd");
	}

	private void handleMtlMapKa(String line) {
		this.curentMtl.map_Ka = parseStringParam(line, "map_ka");
	}

	private void handleMtlNi(String line) {
		this.curentMtl.ni = parseFloatParam(line, "ni");
	}

	private void handleMtlKs(String line) {
		this.curentMtl.ks = parseFloatTriplet("Ks", line); 
	}

	private void handleMtlKd(String line) {
		this.curentMtl.kd = parseFloatTriplet("Kd", line);
	}

	private void handleMtlKa(String line) {
		this.curentMtl.ka = parseFloatTriplet("Ka", line);
	}

	private void handleMtlNewMtl(String line, String mtlLink) throws IOException {
		parseCurentMtl();
		String name = parseStringParam(line, "newmtl");
		this.curentMtl = new ObjMaterial();
		this.curentMtl.name = name;
		this.curentMtl.materialFileLink = mtlLink;
	}

	private void parseCurentMtl() throws IOException {
		if (this.curentMtl != null && this.curentMtl.name != null) {
			
			boolean mapsEmpty = this.curentMtl.map_Ka == null && this.curentMtl.map_Kd == null;
			
			if (this.curentMtl.ka == null) {
				this.curentMtl.ka = new float[]{0.7f, 0.7f, 0.7f};
			}
			if (this.curentMtl.kd == null) {
				this.curentMtl.kd = new float[]{0.3f, 0.3f, 0.3f};
			}
			if (this.curentMtl.ks == null) {
				this.curentMtl.ks = new float[]{0.0f, 0.0f, 0.0f};
			}
			
			float ambientAverage = (this.curentMtl.ka[0] + this.curentMtl.ka[1] + this.curentMtl.ka[2]) / 3.0f;
			float diffuseAverage = (this.curentMtl.kd[0] + this.curentMtl.kd[1] + this.curentMtl.kd[2]) / 3.0f;
			
			boolean kaComponentsEqual = 
					this.curentMtl.ka[0] == this.curentMtl.ka[1] && this.curentMtl.ka[1] == this.curentMtl.ka[2];
			boolean kdComponentsEqual = 
					this.curentMtl.kd[0] == this.curentMtl.kd[1] && this.curentMtl.kd[1] == this.curentMtl.kd[2];
			
			boolean kaCorrespondKd = Math.abs(ambientAverage + diffuseAverage - 1.0) < 0.0001f;
			boolean colorable = mapsEmpty || !kaComponentsEqual || !kdComponentsEqual || !kaCorrespondKd;
			
			float specularAverage = (this.curentMtl.ks[0] + this.curentMtl.ks[1] + this.curentMtl.ks[2]) / 3.0f;
			Float shiness = this.curentMtl.ni;
			
			Color diffuseColor = new Color(
					(int)(this.curentMtl.kd[0] * 255), 
					(int)(this.curentMtl.kd[1] * 255),
					(int)(this.curentMtl.kd[2] * 255));
			
			Color ambientColor = new Color(
					(int)(this.curentMtl.ka[0] * 255), 
					(int)(this.curentMtl.ka[1] * 255),
					(int)(this.curentMtl.ka[2] * 255));
			
			int rm = Math.min(diffuseColor.getRed()   + ambientColor.getRed(),   255);
			int gm = Math.min(diffuseColor.getGreen() + ambientColor.getGreen(), 255);
			int bm = Math.min(diffuseColor.getBlue()  + ambientColor.getBlue(),  255);
			
			Color meanColor = new Color(rm, gm, bm); 
			
			ConfMaterial m = new ConfMaterial(Interpolation.FLAT, meanColor);
			
			m.setAmbientFactor(ambientAverage);
			m.setDiffuseFactor(diffuseAverage);
			
			m.setSpecularFactor(specularAverage);
			m.setShininess(shiness.intValue());
			
			m.setInterpolation(Interpolation.FLAT);
			m.setShadow(Shadow.FALSE);
			m.setTransparency(Transparency.FALSE);
			
			if (!mapsEmpty) {
				String textureLink = this.curentMtl.map_Ka == null 
						? this.curentMtl.map_Kd : this.curentMtl.map_Ka;
				
				File texture = resolveTexture(
						textureLink, 
						this.curentMtl.materialFileLink);
				
				BufferedImage bimg = ImageIO.read(texture);
				
				List<TextureData> tl = new ArrayList<>();
				
				tl.add(new TextureData(texture, bimg.getWidth(), bimg.getHeight(), 
						Wrap.REPEAT, NamedTexCoordFunction.GLOBAL_X_Z, colorable, false));
				
				m.setTextureDataList(tl);
			}
			
			this.materials.put(curentMtl.name, m);
		}
	}

	private File resolveTexture(String texture, String materialFileLink) {
		return this.proxy.getLinkedFile(texture, materialFileLink);
	}

	private void handleMtlComment(String line) {
		// Do nothing
	}

	private void handleObjLine(String line) {
		if (StringUtils.startsWithIgnoreCase(line, "v ")) {
			float[] xyz = parseFloatTriplet("v", line);
			handleVertex(new VectorXYZ(-xyz[0], xyz[1], xyz[2])
					.rotateY(Math.toRadians(180.0)));
		}
		else if (StringUtils.startsWithIgnoreCase(line, "vt")) {
			float[] uv = parseFloatDuplet("vt ", line);
			handleTextureVertex(new VectorXZ(uv[0], uv[1]));
		}
		else if (StringUtils.startsWithIgnoreCase(line, "vn")) {
			float[] xyz = parseFloatTriplet("vn ", line);
			handleVertexNormal(new VectorXYZ(xyz[0], xyz[1], xyz[2]));
		}
		else if (StringUtils.startsWithIgnoreCase(line, "g ")) {
			String groupName = parseStringParam(line, "g");
			handleGroup(groupName);
		}
		else if (line.startsWith("usemtl ")) {
			String mtlName = line.replace("usemtl ", "").trim();
			handleUseMtl(mtlName);
		}
		else if (line.startsWith("s ")) {
			String smoothGroup = line.replace("s ", "").trim();
			handleSmoothGroup(smoothGroup);
		}
		else if (line.startsWith("f ")) {
			String faceLine = line.replace("f ", "").trim();
			handleFace(faceLine);
		}
	}

	private void handleVertexNormal(VectorXYZ vectorXYZ) {
		vertexNorms.add(vectorXYZ);
	}

	public static final class ObjFace {
		public Material material;
		public List<VectorXYZ> vs;
		public List<VectorXYZ> normals;
		public List<List<VectorXZ>> texCoordLists;
	}
	
	private void handleFace(String faceLine) {
		ObjFace f = new ObjFace();
		f.material = this.faceMtl;
		String[] points = faceLine.split(" ");
		for (String pointRef : points) {
			String[] components = pointRef.split("/");
			Integer vi = Integer.valueOf(components[0]);
			if (f.vs == null) {
				f.vs = new ArrayList<>();
			}
			
			if (vi >= 0) {
				f.vs.add(this.vertexes.get(vi - 1));
			}
			else {
				// it's negative so use minus
				f.vs.add(this.vertexes.get(this.vertexes.size() + vi));
			}
			
			updateBbox(f.vs);
			
			if (components.length > 1) {
				if(StringUtils.stripToNull(components[1]) != null) {
					
					if (f.texCoordLists == null) {
						f.texCoordLists = new ArrayList<>();
						f.texCoordLists.add(new ArrayList<>());
					} 
					
					int tvi = Integer.valueOf(components[1]);
					if (tvi >= 0) {
						f.texCoordLists.get(0).add(textureVertexes.get(tvi - 1));
					}
					else {
						f.texCoordLists.get(0).add(textureVertexes.get(
								textureVertexes.size() + tvi));
					}
				} 
			}
			
			if (components.length > 2) {
				if(f.normals == null) {
					f.normals = new ArrayList<>();
				}
				int vni = Integer.valueOf(components[2]);
				
				if (vni >= 0) {
					f.normals.add(this.vertexNorms.get(vni - 1));
				}
				else {
					f.normals.add(this.vertexNorms.get(
							this.vertexNorms.size() + vni));
				}
			}
		}
		this.faces.add(f);
	}
	
	private void updateBbox(List<VectorXYZ> vs) {
		if (this.bbox == null) {
			this.bbox = new AxisAlignedBoundingBoxXYZ(vs);
		}
		else {
			this.bbox = AxisAlignedBoundingBoxXYZ.union(
					this.bbox, new AxisAlignedBoundingBoxXYZ(vs));
		}
	}

	public List<ObjFace> listFaces() {
		return faces;
	}

	private void handleGroup(String groupName) {
		
	}

	private void handleSmoothGroup(String smoothGroup) {
		
	}

	private void handleUseMtl(String mtlName) {
		this.faceMtl = this.materials.get(mtlName);
		if (this.faceMtl == null) {
			System.err.println("Warn: material " + mtlName + " not found");
		}
	}

	private void handleTextureVertex(VectorXZ vectorXZ) {
		this.textureVertexes.add(vectorXZ);
	}

	private void handleVertex(VectorXYZ vectorXYZ) {
		this.vertexes.add(vectorXYZ);
	}

	private void handleObjComment(String line) {
		// Do Nothing
	}

	private void clear() {
		this.textureVertexes = null;
		this.vertexes = null;
	}
	
	public AxisAlignedBoundingBoxXYZ getBBOX() {
		return this.bbox;
	}

}

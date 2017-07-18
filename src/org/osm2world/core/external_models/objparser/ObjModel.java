package org.osm2world.core.external_models.objparser;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.apache.commons.lang.StringUtils;
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

public class ObjParser {
	
	private File objFile;
	private Map<String, Material> materials = new HashMap<>();
	private ObjMaterial curentMtl;
	
	private List<VectorXYZ> vertexes = new ArrayList<>();
	private List<VectorXYZ> vertexNorms = new ArrayList<>();
	private List<VectorXZ> textureVertexes = new ArrayList<>();
	private Material faceMtl;
	private List<ObjFace> faces = new ArrayList<>();
	
	private static final class ObjMaterial {
		File materialFile;
		public String name;
		
		float ni;

		float[] ka;
		float[] kd;
		float[] ks;
		
		String map_Ka;
		String map_Kd;
		
	}
	
	public ObjParser(File objFile) {
		this.objFile = objFile;
		
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
			if (line.startsWith("#")) {
				handleObjComment(line);
			}
			else if (line.startsWith("call")){
				handleCall(line);
			}
			else if (line.startsWith("mtllib")){
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
		String mtlPath = StringUtils.strip(line.replace("mtllib", ""));
		File mtlFile = new File(mtlPath);
		if(!mtlFile.isAbsolute()) {
			mtlFile = new File(this.objFile.getParent(), mtlPath);
		}
		
		parseMtlFile(mtlFile);
	}

	private void parseMtlFile(File mtlFile) throws IOException {
		BufferedReader br = new BufferedReader(new FileReader(mtlFile));  
		String line = StringUtils.strip(br.readLine());
		while (line != null)  
		{  
			if (line.startsWith("#")) {
				handleMtlComment(line);
			}
			else if(line.startsWith("newmtl")) {
				handleMtlNewMtl(line, mtlFile);
			}
			else if(line.startsWith("Ka")) {
				handleMtlKa(line);
			}
			else if(line.startsWith("Kd")) {
				handleMtlKd(line);
			}
			else if(line.startsWith("Ks")) {
				handleMtlKs(line);
			}
			else if(line.startsWith("Ni")) {
				handleMtlNi(line);
			}
			else if(line.startsWith("map_Ka")) {
				handleMtlMapKa(line);
			}
			else if(line.startsWith("map_Kd")) {
				handleMtlMapKd(line);
			}
			
		    line = StringUtils.strip(br.readLine());  
		    line = StringUtils.remove(line, '\t');
		}
		br.close();
		parseCurentMtl();
	}

	private void handleMtlMapKd(String line) {
		this.curentMtl.map_Kd = line.replace("map_Kd ", "").trim();
	}

	private void handleMtlMapKa(String line) {
		this.curentMtl.map_Ka = line.replace("map_Ka ", "").trim();
	}

	private void handleMtlNi(String line) {
		this.curentMtl.ni = 
				Float.valueOf(line.replace("Ni ", ""));
	}

	private void handleMtlKs(String line) {
		String[] split = line.replace("Ks ", "").split(" ");
		this.curentMtl.ks = new float[]{
				Float.valueOf(split[0]),
				Float.valueOf(split[1]),
				Float.valueOf(split[2])
		};
	}

	private void handleMtlKd(String line) {
		String[] split = line.replace("Kd ", "").split(" ");
		this.curentMtl.kd = new float[]{
				Float.valueOf(split[0]),
				Float.valueOf(split[1]),
				Float.valueOf(split[2])
		};
	}

	private void handleMtlKa(String line) {
		String[] split = line.replace("Ka ", "").split(" ");
		this.curentMtl.ka = new float[]{
				Float.valueOf(split[0]),
				Float.valueOf(split[1]),
				Float.valueOf(split[2])
		};
	}

	private void handleMtlNewMtl(String line, File mtlFile) throws IOException {
		parseCurentMtl();
		
		String name = StringUtils.strip(line.replace("newmtl", ""));
		this.curentMtl = new ObjMaterial();
		this.curentMtl.name = name;
		this.curentMtl.materialFile = mtlFile;
	}

	private void parseCurentMtl() throws IOException {
		if (this.curentMtl != null && this.curentMtl.name != null) {
			
			boolean mapsEmpty = this.curentMtl.map_Ka == null && this.curentMtl.map_Kd == null;
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
				File texture = resolveTexture(
						this.curentMtl.map_Ka, 
						this.curentMtl.materialFile);
				
				BufferedImage bimg = ImageIO.read(texture);
				
				List<TextureData> tl = new ArrayList<>();
				
				tl.add(new TextureData(texture, bimg.getWidth(), bimg.getHeight(), 
						Wrap.REPEAT, NamedTexCoordFunction.GLOBAL_X_Z, colorable, false));
				
				m.setTextureDataList(tl);
			}
			
			this.materials.put(curentMtl.name, m);
		}
	}

	private File resolveTexture(String texture, File materialFile) {
		return new File(materialFile.getParentFile(), texture);
	}

	private void handleMtlComment(String line) {
		// Do nothing
	}

	private void handleObjLine(String line) {
		if (line.startsWith("v ")) {
			String[] split = line.replace("v ", "").trim().split(" ");
			handleVertex(new VectorXYZ(
					Double.valueOf(split[0]),
					Double.valueOf(split[1]),
					Double.valueOf(split[2])));
		}
		else if (line.startsWith("vt ")) {
			String[] split = line.replace("vt ", "").trim().split(" ");
			handleTextureVertex(new VectorXZ(
					Double.valueOf(split[0]),
					Double.valueOf(split[1])));
		}
		else if (line.startsWith("vn ")) {
			String[] split = line.replace("vn ", "").trim().split(" ");
			handleVertexNormal(new VectorXYZ(
					Double.valueOf(split[0]),
					Double.valueOf(split[1]),
					Double.valueOf(split[2])));
		}
		else if (line.startsWith("g ")) {
			String groupName = line.replace("g ", "").trim();
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
				f.vs.add(this.vertexes.get(vi));
			}
			else {
				// it's negative so use minus
				f.vs.add(this.vertexes.get(this.vertexes.size() + vi));
			}
			
			if (components.length > 1) {
				if(StringUtils.stripToNull(components[1]) != null) {
					
					if (f.texCoordLists == null) {
						f.texCoordLists = new ArrayList<>();
						f.texCoordLists.add(new ArrayList<>());
					} 
					
					int tvi = Integer.valueOf(components[1]);
					if (tvi >= 0) {
						f.texCoordLists.get(0).add(textureVertexes.get(tvi));
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
					f.normals.add(this.vertexNorms.get(vni));
				}
				else {
					f.normals.add(this.vertexNorms.get(
							this.vertexNorms.size() + vni));
				}
			}
		}
		this.faces.add(f);
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

}

package org.osm2world.viewer.view;

import java.awt.Color;
import java.io.IOException;

import javax.media.opengl.GL2ES2;

import com.jogamp.graph.curve.opengl.RenderState;
import com.jogamp.graph.curve.opengl.TextRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontFactory;
import com.jogamp.graph.font.FontSet;
import com.jogamp.graph.geom.opengl.SVertex;
import com.jogamp.opengl.util.glsl.ShaderState;

public class TextRendererShader implements org.osm2world.viewer.view.TextRenderer {
	private TextRenderer textRenderer;
	private Font textRendererFont = null;
	private int width = 0, height = 0;
	private float scale = 1;
	private GL2ES2 gl;
	
	public TextRendererShader(GL2ES2 gl) {
		this.gl = gl;
		try {
			textRendererFont = FontFactory.getDefault().get(FontSet.FAMILY_REGULAR, FontSet.STYLE_SERIF);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		RenderState renderState = RenderState.createRenderState(new ShaderState(), SVertex.factory());
		textRenderer = TextRenderer.create(renderState, 0);
		textRenderer.init(gl);
		if (!textRenderer.isInitialized()) {
			throw new IllegalStateException("Text renderer not initlialized.");
		}

		// Workaround to get shader initialized properly (fixes problems at first drawText call in some situations)
		textRenderer.enable(gl, true);
		textRenderer.enable(gl, false);
	}

//	@Override
//	public void drawText(String string, Vector3D pos, Color color) {
//		textRenderer.resetModelview(gl);
//		textRenderer.setColorStatic(gl, color.getRed(), color.getGreen(), color.getBlue());
//		float[] posF = {(float)pos.getX(), (float)pos.getY(), -(float)pos.getZ()};
//		int[] texSize = {0};
//		textRenderer.drawString3D(gl.getGL2ES2(), textRendererFont, string, posF, 12, texSize);
//	}

	protected void drawText(String string, float x, float y, Color color) {
		textRenderer.enable(gl, true);
		textRenderer.setColorStatic(gl, color.getRed(), color.getGreen(), color.getBlue());
		textRenderer.resetModelview(gl);
		textRenderer.translate(gl, x, y, 0);
		float[] posF = {0, 0, 0}; // not used in TextRendererImpl01
		int[] texSize = {0};
		textRenderer.drawString3D(gl, textRendererFont, string, posF, (int) (12 * scale), texSize);
		textRenderer.enable(gl, false);
	}
	
	@Override
	public void destroy() {
		textRenderer.destroy(gl);
		textRenderer = null;
		textRendererFont = null;
		gl = null;
	}

	@Override
	public void drawTextTop(String string, float x, float y, Color color) {
		this.drawText(string, x*scale, height - y*scale, color);
	}

	@Override
	public void drawTextBottom(String string, float x, float y, Color color) {
		this.drawText(string, x*scale, y*scale, color);
	}

	@Override
	public void reshape(int width, int height) {
		this.width = width;
		this.height = height;
		textRenderer.reshapeOrtho(gl, width, height, -100000, 100000);
	}

	@Override
	public void setScale(float scale) {
		this.scale = scale;
	}

}

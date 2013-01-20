package org.osm2world;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

import org.osm2world.DelaunayTriangulation.DelaunayTriangle;
import org.osm2world.core.math.LineSegmentXZ;
import org.osm2world.core.math.PolygonXZ;
import org.osm2world.core.math.TriangleXZ;
import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;

public class NNDebugViewer {

	public static void main(String[] args) {
		new DebugViewerFrame().setVisible(true);
	}
	
	private static final double SIZE = 100;
	
	private static class NNDebugPanel extends JPanel {
		
		private List<VectorXYZ> points;
		private DelaunayTriangulation triangulation;
		
		private double minX, maxX, minZ, maxZ;
		
		public NNDebugPanel() {
			clear();
		}
		
		public void clear() {
			
			minX = minZ = Float.MAX_VALUE;
			maxX = maxZ = -Float.MAX_VALUE;
			
			points = new ArrayList<VectorXYZ>();
			triangulation = null;
			
			this.add(new VectorXYZ(-SIZE, 0, -SIZE));
			this.add(new VectorXYZ(+SIZE, 0, -SIZE));
			this.add(new VectorXYZ(+SIZE, 0, +SIZE));
			this.add(new VectorXYZ(-SIZE, 0, +SIZE));
			
			this.repaint(0);
			
		}
		
		public void add(VectorXYZ p) {
			
			points.add(p);
			updateMinMax(p);
			
			this.repaint(0);
			
			if (points.size() > 4) {
			
				triangulation.insert(p);
				
			} else if (points.size() == 4)  {
				
				triangulation = new DelaunayTriangulation(
					points.get(0), points.get(1), points.get(2), points.get(3));
								
			}
			
			this.repaint(0);

		}

		private void updateMinMax(VectorXYZ v) {
			if (v.x < minX) minX = v.x;
			if (v.x > maxX) maxX = v.x;
			if (v.z < minZ) minZ = v.z;
			if (v.z > maxZ) maxZ = v.z;
		}
		
		@Override
		public void paint(Graphics g) {
			
			super.paint(g);
			
			if (points.size() > 4) {

				/* draw Voronoi cells */
				
				Random random = new Random(0);
				
				for (VectorXYZ p : points) {
					
					g.setColor(new Color(
							0.5f + random.nextFloat() / 2,
							0.5f + random.nextFloat() / 2,
							0.5f + random.nextFloat() / 2));
					
					for (TriangleXZ t : triangulation.getVoronoiParts(p)) {
						fill(g, t);
					}
					
				}
				
				VectorXYZ p = points.get(points.size() - 1);
				
				/* draw Voronoi cell of most recently added point */
				
//				g.setColor(Color.YELLOW);
//
//				for (TriangleXYZ t : triangulation.getVoronoiParts(p)) {
//					fill(g, new TriangleXZ(t.v1.xz(), t.v2.xz(), t.v3.xz()));
//				}
				
				/* draw circumcircles */
				
				g.setColor(Color.GREEN);
				
				for (DelaunayTriangle t : triangulation.getIncidentTriangles(p)) {

					VectorXZ center = t.getCircumcircleCenter();
					double radius = t.p0.distanceToXZ(center);
				
					draw(g, center);
					drawCircle(g, center, radius);
										
				}
				
			}
			
			/* draw triangles */
			
			g.setColor(Color.RED);
			
			for (DelaunayTriangle triangle : triangulation.triangles) {
				draw(g, triangle.asTriangleXZ());
			}
						
			/* draw points */
			
			g.setColor(Color.BLACK);
			
			for (VectorXYZ p : points) {
				draw(g, p.xz());
			}
			
		}
			
		private void draw(Graphics g, VectorXZ p) {
			drawPoint(g, p);
		}
			
		private void draw(Graphics g, TriangleXZ t) {
			g.drawPolygon(
					new int[] {coordX(t.v1), coordX(t.v2), coordX(t.v3)},
					new int[] {coordY(t.v1), coordY(t.v2), coordY(t.v3)},
					3);
		}
		
	private void fill(Graphics g, TriangleXZ t) {
		g.fillPolygon(
				new int[] {coordX(t.v1), coordX(t.v2), coordX(t.v3)},
				new int[] {coordY(t.v1), coordY(t.v2), coordY(t.v3)},
				3);
	}
		
		private void draw(Graphics g, PolygonXZ p) {

			for (VectorXZ vertex : p.getVertices()) {
				drawPoint(g, vertex);
			}
			
			for (LineSegmentXZ segment : p.getSegments()) {
				drawLine(g, segment.p1, segment.p2);
			}
			
		}
		
		private void drawPoint(Graphics g, VectorXZ p) {
			int x = coordX(p), y = coordY(p);
			g.fillPolygon(
					new int[] {x-3, x  , x+3, x  },
					new int[] {  y, y+3, y  , y-3},
					4);
		}
		
		private void drawLine(Graphics g, VectorXZ p1, VectorXZ p2) {
			g.drawLine(
					coordX(p1), coordY(p1),
					coordX(p2), coordY(p2));
		}

		private void drawCircle(Graphics g, VectorXZ center, double radius) {
			
			VectorXZ bottomLeft = center.add(new VectorXZ(-radius, radius));
			VectorXZ topRight = center.add(new VectorXZ(radius, -radius));
						
			g.drawOval(
					coordX(bottomLeft), coordY(bottomLeft),
					coordX(topRight) - coordX(bottomLeft),
					coordY(topRight) - coordY(bottomLeft));
			
		}
		
		private void drawArrow(Graphics g, VectorXZ p1, VectorXZ p2) {
			
			drawLine(g, p1, p2);
						
			VectorXZ arrowVector = p2.subtract(p1);
			VectorXZ arrowDir = arrowVector.normalize();
			double headSize = (maxX - minX) / 25;
						
			VectorXZ headBase = p2.subtract(arrowDir.mult(headSize));
			VectorXZ headRight = headBase.add(arrowDir.rightNormal().mult(0.5*headSize));
			VectorXZ headLeft = headBase.subtract(arrowDir.rightNormal().mult(0.5*headSize));
			
			drawLine(g, headRight, p2);
			drawLine(g, headLeft, p2);
			
		}
		
		private int coordX(VectorXZ v) {
			return (int)((this.getWidth()*0.05) + (v.x - minX) / (maxX - minX) * (this.getWidth()*0.9));
		}
		
		private int coordY(VectorXZ v) {
			return (int)((this.getHeight()*0.95) - (v.z - minZ) / (maxZ - minZ) * (this.getHeight()*0.9));
		}
		
	}

	private static class DebugViewerFrame extends JFrame {
		
		public DebugViewerFrame() {
			super("Visual Geometry Debugger");
			this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			
			this.setSize(800, 600);
						
			this.setLayout(new BorderLayout());
			NNDebugPanel panel = new NNDebugPanel();
			this.add(panel, BorderLayout.CENTER);
			
			JMenuBar menuBar = new JMenuBar();
						
			{
				JMenu addMenu = new JMenu("add");
				addMenu.setMnemonic(KeyEvent.VK_A);
				
				addMenu.add(new AddRandomPointAction(panel));
				
				menuBar.add(addMenu);
			}
			
			menuBar.add(new JMenuItem(new ClearAction(panel)));
		
			this.setJMenuBar(menuBar);
			
		}
		
	}
	
	private static final class ClearAction extends AbstractAction {

		private final NNDebugPanel panel;
		
		public ClearAction(NNDebugPanel panel) {
			super("clear");
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
					KeyEvent.VK_C, ActionEvent.CTRL_MASK));
			this.panel = panel;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			panel.clear();
		}
		
	}

	private static final class AddRandomPointAction extends AbstractAction {
		
		private final NNDebugPanel panel;
		
		private static Random random = new Random();
		
		public AddRandomPointAction(NNDebugPanel panel) {
			super("random point");
			putValue(ACCELERATOR_KEY, KeyStroke.getKeyStroke(
					KeyEvent.VK_R, ActionEvent.CTRL_MASK));
			this.panel = panel;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			
			if (panel.points.size() <= 4) {
				random = new Random(1);
			}
			
			double x = (random.nextDouble() * 2 * SIZE) - SIZE;
			double z = (random.nextDouble() * 2 * SIZE) - SIZE;
			
			panel.add(new VectorXYZ(x, 0, z));
			
		}
		
	}
	
	private static class Line {
		public final VectorXZ from;
		public final VectorXZ to;
		public Line(VectorXZ from, VectorXZ to) {
			this.from = from;
			this.to = to;
		}
	}
	
	private static class Arrow extends Line {
		public Arrow(VectorXZ from, VectorXZ to) {
			super(from, to);
		}
	}
		
}

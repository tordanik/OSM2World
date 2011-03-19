package org.osm2world.viewer.control.navigation;


import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.HashSet;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.event.MouseInputListener;

import org.osm2world.core.math.VectorXYZ;
import org.osm2world.core.math.VectorXZ;
import org.osm2world.core.target.common.rendering.Camera;
import org.osm2world.viewer.model.RenderOptions;
import org.osm2world.viewer.view.ViewerFrame;


public class DefaultNavigation extends MouseAdapter implements KeyListener, MouseInputListener {

	private final RenderOptions renderOptions;
	private final ViewerFrame viewerFrame;
	
	public DefaultNavigation(ViewerFrame viewerFrame, RenderOptions renderOptions) {
		
		this.viewerFrame = viewerFrame;
		this.renderOptions = renderOptions;
		
		Timer timer = new Timer("KeyboardNavigation"); 
		timer.scheduleAtFixedRate(KEYBOARD_TASK, 0, 20);
		
	}
	
	private boolean translationDrag = false;
	private boolean rotationDrag = false;
	private Point previousMousePoint;

	private final Set<Integer> pressedKeys = new HashSet<Integer>();

	@Override
	public void keyPressed(KeyEvent e) {
		synchronized (pressedKeys) {
			pressedKeys.add(e.getKeyCode());
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {
		synchronized (pressedKeys) {
			pressedKeys.remove(e.getKeyCode());
		}
	}
	
	@Override
	public void keyTyped(KeyEvent e) {}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		
		Point currentMousePoint = e.getPoint();
		
		float movementX = currentMousePoint.x - previousMousePoint.x;
		float movementY = currentMousePoint.y - previousMousePoint.y;
		
		Camera camera = renderOptions.camera;
		
		if (camera != null) {
	
			if (translationDrag) {
				
				camera.moveForward(movementY);
				camera.moveRight(movementX);
				
			} else if (rotationDrag) {
					
				/* left/right */
				
				camera.rotateY(movementX/100);
				
				/* up/down */
								
				VectorXYZ toLookAt = camera.getLookAt().subtract(camera.getPos());
				
				if (toLookAt.length() > 0.1f) {
				
					VectorXZ moveLookAt = toLookAt.xz().mult(-movementY * 0.02f);
					VectorXYZ newLookAt = camera.getLookAt().add(moveLookAt);
					
					camera.setLookAt(newLookAt);
					
				}
						
			}
		
		}
			
		previousMousePoint = currentMousePoint;
			
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if (e.getButton() == MouseEvent.BUTTON1) {
			viewerFrame.setCursor(new Cursor(Cursor.MOVE_CURSOR));
			translationDrag = true;
		} else {
			viewerFrame.setCursor(new Cursor(Cursor.MOVE_CURSOR));
			rotationDrag = true;
		}
		previousMousePoint = e.getPoint();
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		viewerFrame.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
		translationDrag = false;
		rotationDrag = false;
	}
	
	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {

		Camera c = renderOptions.camera;

		if (c != null) {

			VectorXYZ toLookAt = c.getLookAt().subtract(c.getPos());
			VectorXYZ move = toLookAt.mult(e.getWheelRotation() < 0 ? 0.2f : -0.25f);
			VectorXYZ newPos = c.getPos().add(move);

			c.setPos(newPos);

		}

	}
	
	private final TimerTask KEYBOARD_TASK = new TimerTask() {
	
		@Override
		public void run() {

			Camera c = renderOptions.camera;

			if (c != null) {

				synchronized (pressedKeys) {

					for (int key : pressedKeys) {

						switch (key) {
						case KeyEvent.VK_W:
							c.moveForward(1);
							break;
						case KeyEvent.VK_S:
							c.moveForward(-1);
							break;
						case KeyEvent.VK_A:
							c.moveRight(1);
							break;
						case KeyEvent.VK_D:
							c.moveRight(-1);
							break;
						case KeyEvent.VK_UP:
							c.move(0,0.5,0);
							break;
						case KeyEvent.VK_DOWN:
							c.move(0,-0.5,0);
							break;
						case KeyEvent.VK_RIGHT:
							c.rotateY(Math.PI/100);
							break;
						case KeyEvent.VK_LEFT:
							c.rotateY(-Math.PI/100);
							break;	
						}

					}

				}
			}

		}

	};

}

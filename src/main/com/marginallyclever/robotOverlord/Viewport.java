package com.marginallyclever.robotOverlord;

import javax.vecmath.Matrix4d;
import javax.vecmath.Point3d;
import javax.vecmath.Vector3d;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.glu.GLU;
import com.marginallyclever.convenience.MatrixHelper;
import com.marginallyclever.convenience.PrimitiveSolids;
import com.marginallyclever.convenience.Ray;
import com.marginallyclever.robotOverlord.swingInterface.view.ViewPanel;
import com.marginallyclever.robotOverlord.uiExposedTypes.BooleanEntity;
import com.marginallyclever.robotOverlord.uiExposedTypes.DoubleEntity;
import com.marginallyclever.robotOverlord.uiExposedTypes.StringEntity;

/**
 * Wrapper for all projection matrix stuff at the start of the render pipeline.
	// OpenGL camera: -Z=forward, +X=right, +Y=up
 * @author Dan Royer
 * @since 1.6.0
 *
 */
public class Viewport extends Entity {
	/**
	 * 
	 */
	private static final long serialVersionUID = -7613439702723031982L;
	
	private int canvasWidth, canvasHeight;
	// mouse position in GUI
	private double cursorX,cursorY;
	// is mouse pressed in GUI?
	private boolean isPressed;

	// calculated when rendering.  so won't be valid on the first frame.
	private Matrix4d projectionMatrix = new Matrix4d();
	
	private DoubleEntity nearZ=new DoubleEntity("Near Z",5.0);
	private DoubleEntity farZ=new DoubleEntity("Far Z",2000.0);
	private DoubleEntity fieldOfView=new DoubleEntity("FOV",60.0);
	private StringEntity attachedTo=new StringEntity("Attached to","");
	private BooleanEntity drawOrthographic=new BooleanEntity("Orthographic",false);
	
	
	public Viewport() {
		super();
		
		setName("Viewport");
		addChild(drawOrthographic);
		addChild(farZ);
		addChild(nearZ);
		addChild(fieldOfView);
		addChild(attachedTo);
			
		isPressed=false;
	}
	
	public void renderPerspective(GL2 gl2) {
		double zNear = nearZ.get();
		double zFar = farZ.get();
		double fH = Math.tan( Math.toRadians(fieldOfView.get()/2) ) * zNear;
		double aspect = (double)canvasWidth / (double)canvasHeight;
		double fW = fH * aspect;
	
		gl2.glFrustum(-fW,fW,-fH,fH,zNear,zFar);
	}
	
	public void renderOrtho(GL2 gl2,double zoom) {
        double w = canvasWidth/10;
        double h = canvasHeight/10;
        
		gl2.glOrtho(-w/zoom, w/zoom, -h/zoom, h/zoom, nearZ.get(), farZ.get());
	}
	
	public void renderOrtho(GL2 gl2) {
		PoseEntity camera = getAttachedTo();
        Camera c = (Camera)camera;
        double z = c.getZoom()/100;

        renderOrtho(gl2,z);
	}

	public void renderShared(GL2 gl2) {
		// store the projection matrix for later
        double [] m = new double[16];
        gl2.glGetDoublev(GL2.GL_PROJECTION_MATRIX, m, 0);
        projectionMatrix.set(m);

    	gl2.glMatrixMode(GL2.GL_MODELVIEW);
        gl2.glLoadIdentity();

		PoseEntity camera = getAttachedTo();
		if(camera !=null) {
			Matrix4d mFinal = camera.getPoseWorld();
			mFinal.invert();
			MatrixHelper.applyMatrix(gl2, mFinal);
		}
	}
	
	public void renderChosenProjection(GL2 gl2) {
    	gl2.glMatrixMode(GL2.GL_PROJECTION);
		gl2.glLoadIdentity();
		
		if(drawOrthographic.get()) {
			renderOrtho(gl2);
		} else {
			renderPerspective(gl2);
		}
		
        renderShared(gl2);
	}
	
	public void renderPick(GL2 gl2,double pickX,double pickY) {
        // get the current viewport dimensions to set up the projection matrix
        int[] viewportDimensions = new int[4];
		gl2.glGetIntegerv(GL2.GL_VIEWPORT,viewportDimensions,0);

		GLU glu = GLU.createGLU(gl2);
        
		// Set up a tiny viewport that only covers the area behind the cursor. 
		// Tiny viewports are faster.
        gl2.glMatrixMode(GL2.GL_PROJECTION);
        gl2.glLoadIdentity();
		glu.gluPickMatrix(pickX, canvasHeight-pickY, 5.0, 5.0, viewportDimensions,0);

		if(drawOrthographic.get()) {
			renderOrtho(gl2);
		} else {
			renderPerspective(gl2);
		}
		
		renderShared(gl2);
	}
	
	// reach out from the camera into the world and find the nearest object (if any) that the ray intersects.
	public Ray rayPick() {
		// OpenGL camera: -Z=forward, +X=right, +Y=up
		// get the ray coming through the viewport in the current projection.
		Ray ray = new Ray();

		if(drawOrthographic.get()) {
			// orthographic projection
			ray.start = new Point3d(
					cursorX*canvasWidth/10,
					cursorY*canvasHeight/10,
					0);
			ray.direction.set(0,0,-1);
			PoseEntity camera = getAttachedTo();
			Matrix4d m2 = camera.getPoseWorld();
			m2.transform(ray.direction);
			m2.transform(ray.start);
		} else {
			// perspective projection
			double aspect = (double)canvasWidth / (double)canvasHeight;
			double t = Math.tan(Math.toRadians(fieldOfView.get()/2));
			ray.direction.set(cursorX*t*aspect,cursorY*t,-1);
			
			// adjust the ray by the camera world pose.
			PoseEntity camera = getAttachedTo();
			Matrix4d m2 = camera.getPoseWorld();
			m2.transform(ray.direction);
			ray.start.set(camera.getPosition());
		}
		ray.direction.normalize();
		
		return ray; 
	}

	public void showPickingTest(GL2 gl2) {
		renderChosenProjection(gl2);
		gl2.glPushMatrix();

		Ray r = rayPick();

		double cx=cursorX;
		double cy=cursorY;
        int w = canvasWidth;
        int h = canvasHeight;
        setCursor(0,0);        Ray tl = rayPick();
        setCursor(w,0);        Ray tr = rayPick();
        setCursor(0,h);        Ray bl = rayPick();
        setCursor(w,h);        Ray br = rayPick();
		cursorX=cx;
		cursorY=cy;

        double scale=20;
        tl.direction.scale(scale);
        tr.direction.scale(scale);
        bl.direction.scale(scale);
        br.direction.scale(scale);
        r.direction .scale(scale);
        
        Vector3d tl2 = new Vector3d(tl.direction);
        Vector3d tr2 = new Vector3d(tr.direction);
        Vector3d bl2 = new Vector3d(bl.direction);
        Vector3d br2 = new Vector3d(br.direction);
        Vector3d r2  = new Vector3d(r.direction );
        
        tl2.add(tl.start);
        tr2.add(tr.start);
        bl2.add(bl.start);
        br2.add(br.start);
        r2.add(r.start);
        
        gl2.glDisable(GL2.GL_TEXTURE_2D);
		gl2.glDisable(GL2.GL_LIGHTING);
		
        gl2.glColor3d(1, 0, 0);
		gl2.glBegin(GL2.GL_LINES);
		gl2.glVertex3d(tl.start.x, tl.start.y, tl.start.z);		gl2.glVertex3d(tl2.x, tl2.y, tl2.z);
		gl2.glVertex3d(tr.start.x, tr.start.y, tr.start.z);		gl2.glVertex3d(tr2.x, tr2.y, tr2.z);
		gl2.glVertex3d(bl.start.x, bl.start.y, bl.start.z);		gl2.glVertex3d(bl2.x, bl2.y, bl2.z);
		gl2.glVertex3d(br.start.x, br.start.y, br.start.z);		gl2.glVertex3d(br2.x, br2.y, br2.z);

        gl2.glColor3d(1, 1, 1);
		gl2.glVertex3d(r.start.x, r.start.y, r.start.z);		gl2.glVertex3d(r2.x,r2.y,r2.z);
		gl2.glEnd();
        gl2.glColor3d(0, 1, 0);
		gl2.glBegin(GL2.GL_LINE_LOOP);
		gl2.glVertex3d(tl2.x, tl2.y, tl2.z);
		gl2.glVertex3d(tr2.x, tr2.y, tr2.z);
		gl2.glVertex3d(br2.x, br2.y, br2.z);
		gl2.glVertex3d(bl2.x, bl2.y, bl2.z);
		gl2.glEnd();
        gl2.glColor3d(0, 0, 1);
		gl2.glBegin(GL2.GL_LINE_LOOP);
		gl2.glVertex3d(tl.start.x, tl.start.y, tl.start.z);
		gl2.glVertex3d(tr.start.x, tr.start.y, tr.start.z);
		gl2.glVertex3d(br.start.x, br.start.y, br.start.z);
		gl2.glVertex3d(bl.start.x, bl.start.y, bl.start.z);
		gl2.glEnd();
		
		PrimitiveSolids.drawStar(gl2,r2,5);
		gl2.glPopMatrix();
	}

	public void setCursor(int x,int y) {
		cursorX= (2.0*x/canvasWidth)-1.0;
		cursorY= 1.0-(2.0*y/canvasHeight);
        //Log.message("X"+cursorX+" Y"+cursorY);
	}

	// mouse was pressed in GUI
	public void pressed() {
		isPressed=true;
	}

	// mouse was released in GUI
	public void released() {
		isPressed=false;
	}
	
	// is mouse pressed in GUI?
	public boolean isPressed() {
		return isPressed;
	}
	
	public int getCanvasWidth() {
		return canvasWidth;
	}

	public void setCanvasWidth(int canvasWidth) {
		this.canvasWidth = canvasWidth;
	}

	public int getCanvasHeight() {
		return canvasHeight;
	}

	public void setCanvasHeight(int canvasHeight) {
		this.canvasHeight = canvasHeight;
	}
	
	public double getAspectRatio() {
		return canvasWidth/canvasHeight;
	}

	public PoseEntity getAttachedTo() {
		return (PoseEntity)findByPath(attachedTo.get());
	}
	
	public void setAttachedTo(String s) {
		attachedTo.set(s);
	}
	
	@Override
	public void getView(ViewPanel view) {
		view.pushStack("V", "Viewport");
		view.add(drawOrthographic);
		view.add(farZ);
		view.add(nearZ);
		view.add(fieldOfView);
		//view.addStaticText(attachedTo.getName());
		view.popStack();
		super.getView(view);
	}
}

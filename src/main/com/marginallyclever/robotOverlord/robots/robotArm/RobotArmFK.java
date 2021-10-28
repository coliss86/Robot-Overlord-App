package com.marginallyclever.robotOverlord.robots.robotArm;

import java.beans.PropertyChangeEvent;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import com.jogamp.opengl.GL2;
import com.marginallyclever.convenience.Cuboid;
import com.marginallyclever.convenience.IntersectionHelper;
import com.marginallyclever.convenience.MathHelper;
import com.marginallyclever.convenience.MatrixHelper;
import com.marginallyclever.convenience.OpenGLHelper;
import com.marginallyclever.convenience.PrimitiveSolids;
import com.marginallyclever.robotOverlord.PoseEntity;
import com.marginallyclever.robotOverlord.shape.Mesh;
import com.marginallyclever.robotOverlord.shape.Shape;
import com.marginallyclever.robotOverlord.swingInterface.view.ViewElementButton;
import com.marginallyclever.robotOverlord.swingInterface.view.ViewPanel;
import com.marginallyclever.robotOverlord.uiExposedTypes.BooleanEntity;

/**
 * Simulation of a robot arm with Forward Kinematics based on Denavit Hartenberg parameters.
 * It manages a set of {@link RobotArmBone} and a tool center position.
 * @see <a href='https://en.wikipedia.org/wiki/Denavit%E2%80%93Hartenberg_parameters'>DH parameters</a>
 * @see <a href='https://en.wikipedia.org/wiki/Forward_kinematics'>Forward Kinematics</a>
 * @author Dan Royer
 * @since 2021-02-24
 */
public class RobotArmFK extends PoseEntity {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2436924907127292890L;
	
	private Shape base;
	private ArrayList<RobotArmBone> bones = new ArrayList<RobotArmBone>();
	
	private BooleanEntity showAngles = new BooleanEntity("Show Angles",false);
	private BooleanEntity showEndEffector = new BooleanEntity("Show End Effector",true);
	private PoseEntity toolCenterPoint = new PoseEntity("TCP");
		
	public RobotArmFK() {
		super(RobotArmFK.class.getSimpleName());
				
		loadModel();
		
		for(int i=0;i<bones.size();++i) {
			RobotArmBone b = bones.get(i);
			b.updateMatrix();
		}
	}
	
	@Override
	public Object clone() throws CloneNotSupportedException {
		RobotArmFK b = (RobotArmFK)super.clone();
		b.bones = new ArrayList<RobotArmBone>();
		Iterator<RobotArmBone> i = bones.iterator();
		while(i.hasNext()) {
			b.bones.add((RobotArmBone)(i.next().clone()));
		}
		
		return b;
	}
	
	/**
	 * Set up the hierarchy according to the DH parameters.  Also load the shapes.  
	 * The physical origin of the shapes does not match the DH linkage description of the robot, 
	 * so adjust the {@link RobotArmBone.shapeOffset} of each bone to compensate.
	 */
	protected void loadModel() {
		bones.clear();
	}

	// Use the cumulative pose of each Sixi3Bone to adjust the model origins.
	protected void adjustModelOriginsToDHLinks() {
		Matrix4d current = new Matrix4d();
		current.setIdentity();
		for( RobotArmBone bone : bones ) {
			bone.updateMatrix();
			current.mul(bone.getPose());
			Matrix4d iWP = new Matrix4d(current);
			iWP.invert();			
			bone.getShape().setPose(iWP);
		}
	}

	protected void addBone(RobotArmBone bone) {
		bones.add(bone);
	}
	
	protected void setTextureFilename(String fname) {
		base.getMaterial().setTextureFilename(fname);
		for( RobotArmBone bone : bones ) {
			bone.setTexturefilename(fname);
		}
	}
	
	@Override
	public void render(GL2 gl2) {		
		gl2.glPushMatrix();
			MatrixHelper.applyMatrix(gl2, myPose);
			drawMeshes(gl2);
			drawExtras(gl2);
		gl2.glPopMatrix();
		
		super.render(gl2);
	}
	
	private void drawMeshes(GL2 gl2) {
		base.render(gl2);

		gl2.glPushMatrix();
		for( RobotArmBone bone : bones ) {
			// draw model with local shape offset
			bone.updateMatrix();
			MatrixHelper.applyMatrix(gl2, bone.getPose());
			bone.getShape().render(gl2);
		}
		gl2.glPopMatrix();
	}
	
	private void drawExtras(GL2 gl2) {
		// turn of textures so lines draw good
		boolean wasTex = gl2.glIsEnabled(GL2.GL_TEXTURE_2D);
		gl2.glDisable(GL2.GL_TEXTURE_2D);
		// turn off lighting so lines draw good
		boolean wasLit = gl2.glIsEnabled(GL2.GL_LIGHTING);
		gl2.glDisable(GL2.GL_LIGHTING);
		// draw on top of everything else
		int wasOver=OpenGLHelper.drawAtopEverythingStart(gl2);
		
		if(showLineage.get()) drawLineage(gl2);
		if(showAngles.get()) drawAngles(gl2);
		// bounding boxes are always relative to base?
		if(showBoundingBox.get()) drawBoundindBoxes(gl2);
		
		if(showEndEffector.get()) {
			Matrix4d m = getEndEffector();
			MatrixHelper.drawMatrix(gl2, m, 6);
		}

		// return state if needed
		OpenGLHelper.drawAtopEverythingEnd(gl2,wasOver);
		if(wasLit) gl2.glEnable(GL2.GL_LIGHTING);
		if(wasTex) gl2.glEnable(GL2.GL_TEXTURE_2D);	
	}
	
	private void drawBoundindBoxes(GL2 gl2) {
		boolean hit = collidesWithSelf();
		if(hit) gl2.glColor3d(1, 0, 0);
		else    gl2.glColor3d(1, 1, 1);

		Matrix4d w = getPoseWorld();
		w.invert();
		
		gl2.glPushMatrix();
		MatrixHelper.applyMatrix(gl2, w);
			ArrayList<Cuboid> list = getCuboidList();
			for(Cuboid c : list) {
				gl2.glPushMatrix();
				c.getPose(w);
				MatrixHelper.applyMatrix(gl2, w);
				PrimitiveSolids.drawBoxWireframe(gl2, c.getBoundsBottom(),c.getBoundsTop());
				gl2.glPopMatrix();
			}
		gl2.glPopMatrix();
	}

	private void drawAngles(GL2 gl2) {
		boolean cullOn = gl2.glIsEnabled(GL2.GL_CULL_FACE);
		gl2.glDisable(GL2.GL_CULL_FACE);
		
		gl2.glPushMatrix();
			int j = bones.size()+1;
			for( RobotArmBone bone : bones ) {
				bone.updateMatrix();
				double bmin = bone.getAngleMin();
				double bmax = bone.getAngleMax();
				double color = Math.abs(0.5-MathHelper.getUnitInRange(bmin,bmax,bone.theta));
				// curve of movement
				gl2.glColor4d(1,1-color,1-color,0.6);
				gl2.glBegin(GL2.GL_TRIANGLE_FAN);
				gl2.glVertex3d(0, 0, 0);
				double diff = bone.theta-180;
				double end = Math.abs(diff);
				double dir = diff>0?1:-1;
				double radius = j;
				for(double a = 0; a<end;a+=5) {
					double s = Math.sin(Math.toRadians(-a*dir)) * radius;
					double c = Math.cos(Math.toRadians(-a*dir)) * radius;
					gl2.glVertex3d(s, c,0);
				}
				gl2.glEnd();

				MatrixHelper.applyMatrix(gl2, bone.getPose());
				--j;
			}
		gl2.glPopMatrix();

		if(cullOn) gl2.glEnable(GL2.GL_CULL_FACE);
	}

	private void drawLineage(GL2 gl2) {
		Vector3d v = new Vector3d();
		// then the bones, overtop and unlit.
		gl2.glPushMatrix();
			int j = bones.size()+1;
			for( RobotArmBone bone : bones ) {
				bone.updateMatrix();
				// draw bone origin
				PrimitiveSolids.drawStar(gl2,j*2);
				// draw line to next bone
				bone.getPose().get(v);
				gl2.glColor3d(1, 0, 1);
				gl2.glBegin(GL2.GL_LINES);
				gl2.glVertex3d(0, 0, 0);
				gl2.glVertex3d(v.x,v.y,v.z);
				gl2.glEnd();

				MatrixHelper.applyMatrix(gl2, bone.getPose());
				--j;
			}
		gl2.glPopMatrix();
	}
	
	public Matrix4d getToolCenterPoint() {
		return toolCenterPoint.getPose();
	}
	
	public void setToolCenterPoint(Matrix4d tcpNew) {
		Matrix4d tcpOld = toolCenterPoint.getPose();
		this.toolCenterPoint.setPose(tcpNew);

		notifyPropertyChangeListeners(new PropertyChangeEvent(this,"tcp",tcpOld,tcpNew));
	}

	/**
	 * Find the current end effector pose, relative to the base of this robot
	 * @param m where to store the end effector pose.
	 */
	public Matrix4d getEndEffector() {
		Matrix4d m = new Matrix4d();
		m.setIdentity();
		for( RobotArmBone bone : bones ) {
			m.mul(bone.getPose());
		}
		m.mul(toolCenterPoint.getPose());
		
		return m;
	}

	@Override
	public void getView(ViewPanel view) {
		view.pushStack("FK","Forward Kinematics");
		ViewElementButton button = view.addButton("Center all");
		button.addPropertyChangeListener((evt)-> {
			double [] v = new double[bones.size()]; 
			for(int i=0;i<bones.size();++i) {
				RobotArmBone b = bones.get(i);
				v[i]=b.getAngleMiddle();
			}
			setAngles(v);
		});
		view.add(showAngles);
		view.popStack();
		
		super.getView(view);
	}
	
	/**
	 * 
	 * @param list where to collect the information.  Must be {@link RobotArmFK#NUM_BONES} long.
	 * @throws InvalidParameterException
	 */
	public double [] getAngles() {
		double [] list = new double[bones.size()];
		
		int i=0;
		for( RobotArmBone bone : bones ) {
			list[i++] = bone.theta;
		}
		return list;
	}
	
	/**
	 * Update the theta angles of each bone in the robot and the FK sliders on the panel.
	 * It does not allow you to set the angle of a bone outside the angleMax/angleMin of that bone.
	 * The new values will be tested against the bone limits.
	 * If the arm moves to a new position a {@link PropertyChangeEvent} notice will be fired.  The
	 * {@link PropertyChangeEvent.propertyName} will be "ee".
	 * @param list new theta values.  Must be {@link Sixi3FK.getNumBones()} long.
	 */
	public void setAngles(double [] list) {
		boolean changed=false;
		
		int i=0;
		for( RobotArmBone b : bones ) {
			double v = list[i++];
			double t = b.getTheta();
			b.setAngleWRTLimits(v);
			if( t != b.getTheta() )
				changed=true;
		}

		if(changed) {
			// theta values actually changed so update matrixes and get the new end effector position.
			Matrix4d eeOld = getEndEffector();
			for( RobotArmBone b : bones ) {
				b.updateMatrix();
			}
			Matrix4d eeNew = getEndEffector();

			notifyPropertyChangeListeners(new PropertyChangeEvent(this,"ee",eeOld,eeNew));
		}
	}
	
	public ArrayList<Cuboid> getCuboidList() {
		ArrayList<Cuboid> list = new ArrayList<Cuboid>();

		// current pose starts as root pose
		Matrix4d currentBonePose = getPoseWorld();
		Matrix4d cuboidAfter = new Matrix4d();

		list.addAll(base.getCuboidList());
		for(Cuboid c : list) {
			// add current pose to cube pose and save that as cube pose.
			cuboidAfter.mul(currentBonePose,base.getPose());
			c.setPose(cuboidAfter);
		}
		
		for( RobotArmBone bone : bones ) {
			// add bone to current pose
			currentBonePose.mul(bone.getPose());
			
			ArrayList<Cuboid> list2 = bone.getShape().getCuboidList();
			for(Cuboid c : list2) {
				Cuboid nc = new Cuboid();
				nc.set(c);
				// add current pose to cube pose and save that as cube pose.
				cuboidAfter.mul(currentBonePose,bone.getShape().getPose());
				nc.setPose(cuboidAfter);
				list.add(nc);
			}
		}
		
		return list;
	}
	
	/**
	 * @return true if this robot currently collide with itself.
	 */
	public boolean collidesWithSelf() {
		ArrayList<Cuboid> list = getCuboidList();
		
		for(int i=0;i<list.size();++i) {
			Cuboid a = list.get(i);
			for(int j=i+2;j<list.size();++j) {
				Cuboid b = list.get(j);
				if(IntersectionHelper.cuboidCuboid(a, b)) {
					Matrix4d ma = new Matrix4d();
					Matrix4d mb = new Matrix4d();
					a.getPose(ma);
					b.getPose(mb);
					Mesh sa = a.getShape();
					Mesh sb = b.getShape();
					
					return IntersectionHelper.meshMesh(ma,sa,mb,sb);
				}
			}
		}
		return false;
	}

	@Override
	public String toString() {
		String angles = "";
		String add="";
		for( RobotArmBone b : bones ) {
			angles += add + Double.toString(b.theta);
			add=",";
		}
		return "Sixi3FK {"+angles+"}";
	}

	public boolean fromString(String s) {
		final String header="Sixi3FK {";
		if(!s.startsWith(header )) return false;
		
		// strip "Sixi3FK {" and "}" from either end.
		s=s.substring(header.length(),s.length()-1);
		// split by comma
		String [] pieces = s.split(",");
		if(pieces.length != bones.size()) return false;
		
		double [] v = new double[bones.size()];
		for(int i=0; i<bones.size(); ++i) {
			v[i] = Double.parseDouble(pieces[i]);
		}
		
		setAngles(v);
		
		return true;
	}
	
	public int getNumBones() {
		return bones.size();
	}
	
	public RobotArmBone getBone(int i) {
		return bones.get(i);
	}
	
	public Shape getBaseShape() {
		return base;
	}
	
	public void setBaseShape(Shape s) {
		base=s;
	}
	
	/**
	 * Measures the difference between the latest end effector matrix and the target matrix.
	 * It is a combination of the linear distance and the rotation distance (collectively known as the Twist)
	 * @return the error term.
	 */
	public double getDistanceToTarget(final Matrix4d target) {
		double [] d = MatrixHelper.getCartesianBetweenTwoMatrixes(getEndEffector(), target);
		double sum=0;
		for(int i=0;i<d.length;++i) sum+=d[i];
		return sum;
	}
}

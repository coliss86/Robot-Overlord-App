package com.marginallyclever.robotOverlord.engine.dhRobot;

import java.util.ArrayList;

import javax.swing.JPanel;
import javax.vecmath.Matrix4d;

import com.jogamp.opengl.GL2;
import com.marginallyclever.robotOverlord.RobotOverlord;
import com.marginallyclever.robotOverlord.entity.modelInWorld.ModelInWorld;

/**
 * Denavit–Hartenberg parameters
 * @author Dan Royer
 * See https://en.wikipedia.org/wiki/Denavit%E2%80%93Hartenberg_parameters
 */
public class DHLink extends ModelInWorld {
	// length (mm) along previous Z to the common normal
	private double d;
	// angle (degrees) about previous Z, from old X to new X
	private double theta;
	// length (mm) of the common normal. Assuming a revolute joint, this is the radius about previous Z
	private double r;
	// angle (degrees) about common normal, from old Z axis to new Z axis
	private double alpha;
	
	// computed matrix based on the D-H parameters
	public Matrix4d poseCumulative;
	
	// dynamics are described in a 4x4 matrix
	//     [ Ixx Ixy Ixz } XgM ]
	// J = [ Iyx Iyy Iyz } YgM ]
	//     [ Izx Izy Izz } ZgM ]
	//     [ XgM YgM ZgM }  M  ]
	// where mass M, Ng is the center of mass, and I terms represent the inertia.
	public Matrix4d inertia;
	
	// Any combination of the READ_ONLY_* flags, used to control the GUI.
	public int flags;
	
	public final static int READ_ONLY_D		= 1;
	public final static int READ_ONLY_THETA	= 1<<1;
	public final static int READ_ONLY_R		= 1<<2;
	public final static int READ_ONLY_ALPHA	= 1<<3;
	
	protected double rangeMin,rangeMax;
	
	public double maxVelocity;	// not used yet
	public double maxAcceleration;	// not used yet
	public double maxTorque;	// not used yet

	// Changes visual quality of angle range curve.  Must be a whole number >=2
	public final static double ANGLE_RANGE_STEPS=20;
	
	// the gcode letter representing this link
	protected String letter="";
	
	protected transient DHLinkPanel linkPanel;

	
	public DHLink() {
		super();
		setName("DHLink");
		pose = new Matrix4d();
		poseCumulative = new Matrix4d();
		inertia = new Matrix4d();
		
		flags=0;
		d=0;
		theta=0;
		r=0;
		alpha=0;
		model=null;
		rangeMin=-90;
		rangeMax=90;
		maxVelocity=Double.MAX_VALUE;
		maxAcceleration=Double.MAX_VALUE;
		maxTorque=Double.MAX_VALUE;
		
	}
	
	public DHLink(DHLink arg0) {
		super();
		pose = new Matrix4d();
		poseCumulative = new Matrix4d();
		inertia = new Matrix4d();
		
		set(arg0);
	} 
	
	public void set(DHLink arg0) {
		setName(arg0.getName());
		pose.set(arg0.pose);
		poseCumulative.set(arg0.poseCumulative);
		inertia.set(arg0.inertia);
		cuboid.set(arg0.cuboid);

		flags = arg0.flags;
		d = arg0.d;
		theta=arg0.theta;
		r=arg0.r;
		alpha=arg0.alpha;
		model=arg0.model;
		rangeMin=arg0.rangeMin;
		rangeMax=arg0.rangeMax;
		maxVelocity=arg0.maxVelocity;
		maxAcceleration=arg0.maxAcceleration;
		maxTorque=arg0.maxTorque;
	}
	

	@Override
	public ArrayList<JPanel> getContextPanel(RobotOverlord gui) {
		ArrayList<JPanel> list = super.getContextPanel(gui);
		if(list==null) list = new ArrayList<JPanel>();
		
		linkPanel = new DHLinkPanel(gui,this);
		list.add(linkPanel);
		
		return list;
	}
	
	/**
	 * Equivalent to (n-1)T(n) = TransZ(n-1)(dn) * RotZ(n-1)(theta) * TransX(n)(r) * RotX(alpha)
	 */
	public void refreshPoseMatrix() {
		assert(!Double.isNaN(theta));
		assert(!Double.isNaN(alpha));
		assert(!Double.isNaN(r));
		assert(!Double.isNaN(d));
		double ct = Math.cos(Math.toRadians(theta));
		double ca = Math.cos(Math.toRadians(alpha));
		double st = Math.sin(Math.toRadians(theta));
		double sa = Math.sin(Math.toRadians(alpha));
		
		pose.m00 = ct;		pose.m01 = -st*ca;		pose.m02 = st*sa;		pose.m03 = r*ct;
		pose.m10 = st;		pose.m11 = ct*ca;		pose.m12 = -ct*sa;		pose.m13 = r*st;
		pose.m20 = 0;		pose.m21 = sa;			pose.m22 = ca;			pose.m23 = d;
		pose.m30 = 0;		pose.m31 = 0;			pose.m32 = 0;			pose.m33 = 1;
	}
	
	@Override
	public void render(GL2 gl2) {
		// change material property here to color by range

		float [] diffuse = material.getDiffuseColor();
		setAngleColorByRange(gl2);
		
		super.render(gl2);
		
		material.setDiffuseColor(diffuse[0],diffuse[1],diffuse[2],diffuse[3]);
	}
	
	/**
	 * Render the min/max/current angle for one link in a D-H chain.  
	 * Changes the current render matrix!  Clean up after yourself!  
	 * @param gl2 the render context
	 */
	public void renderAngles(GL2 gl2) {
		// draw the angle range
		double k;
		final double scale=10;
		
		boolean isLit = gl2.glIsEnabled(GL2.GL_LIGHTING);
		gl2.glDisable(GL2.GL_LIGHTING);
		
		gl2.glColor3f(0, 0, 0);
		if((flags & READ_ONLY_THETA)==0) {
			// display the curve around z (in the xy plane)
			gl2.glPushMatrix();
			gl2.glTranslated(0, 0, d);
			gl2.glScaled(scale, scale, scale);
			gl2.glColor4d(0,0,0,0.35);
			gl2.glBegin(GL2.GL_LINE_STRIP);
			gl2.glVertex3d(0, 0, 0);
			for(k=0;k<=ANGLE_RANGE_STEPS;++k) {
				double j=(rangeMax-rangeMin)*(k/ANGLE_RANGE_STEPS)+rangeMin;
				gl2.glVertex3d(
						Math.cos(Math.toRadians(j)), 
						Math.sin(Math.toRadians(j)), 
						0);
			}
			gl2.glVertex3d(0, 0, 0);
			gl2.glEnd();
			setAngleColorByRange(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
			gl2.glVertex3d(0, 0, 0);
			double mid=(rangeMax+rangeMin)/2;
			double steps = Math.floor(Math.abs(mid-theta));
			for(k=0;k<steps;++k) {
				double j = (theta-mid)*(k/steps)+mid;
				gl2.glVertex3d(
						Math.cos(Math.toRadians(j)), 
						Math.sin(Math.toRadians(j)), 
						0);
			}
			gl2.glEnd();
			gl2.glPopMatrix();
		}
		if((flags & READ_ONLY_D)==0) {
			// display the prismatic nature of d
			gl2.glPushMatrix();
			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3d(0,  1, this.rangeMin);
			gl2.glVertex3d(0, -1, this.rangeMin);
			gl2.glVertex3d(0,  0, this.rangeMin);
			gl2.glVertex3d(0,  0, this.rangeMax);
			gl2.glVertex3d(0,  1, this.rangeMax);
			gl2.glVertex3d(0, -1, this.rangeMax);
			gl2.glVertex3d(0,  1, d);
			gl2.glVertex3d(0, -1, d);
			gl2.glEnd();
			gl2.glPopMatrix();
		}
		if((flags & READ_ONLY_ALPHA)==0) {
			// display the curve around x (in the yz plane)
			gl2.glPushMatrix();
			gl2.glTranslated(r, 0, d);
			gl2.glRotated(this.theta, 0, 0, 1);
			gl2.glScaled(scale, scale, scale);
			gl2.glColor4d(0,0,0,0.35);
			gl2.glBegin(GL2.GL_LINE_STRIP);
			gl2.glVertex3d(0, 0, 0);
			for(k=0;k<=ANGLE_RANGE_STEPS;++k) {
				double j=(rangeMax-rangeMin)*(k/ANGLE_RANGE_STEPS)+rangeMin;
				gl2.glVertex3d(
						0,
						Math.cos(Math.toRadians(j)),
						Math.sin(Math.toRadians(j)));
			}
			gl2.glVertex3d(0, 0, 0);
			gl2.glEnd();
			setAngleColorByRange(gl2);
			gl2.glBegin(GL2.GL_TRIANGLE_FAN);
			gl2.glVertex3d(0, 0, 0);
			double mid=(rangeMax+rangeMin)/2;
			double steps = Math.floor(Math.abs(mid-alpha));
			for(k=0;k<steps;++k) {
				double j = (alpha-mid)*(k/steps)+mid;
				gl2.glVertex3d(0,
						Math.cos(Math.toRadians(j)), 
						Math.sin(Math.toRadians(j))
						);
			}
			gl2.glEnd();/*
			gl2.glVertex3d(0, 0, 0);
			gl2.glVertex3d(
					0,
					Math.cos(Math.toRadians(this.alpha)),
					Math.sin(Math.toRadians(this.alpha)));
			gl2.glEnd();*/
			gl2.glPopMatrix();
		}
		if((flags & READ_ONLY_R)==0) {
			// display the prismatic nature of r
			gl2.glPushMatrix();
			gl2.glTranslated(0, 0, d);
			gl2.glRotated(this.theta, 0, 0, 1);
			gl2.glBegin(GL2.GL_LINES);
			gl2.glVertex3d(this.rangeMin,  1, 0);
			gl2.glVertex3d(this.rangeMin, -1, 0);
			gl2.glVertex3d(this.rangeMin,  0, 0);
			gl2.glVertex3d(this.rangeMax,  0, 0);
			gl2.glVertex3d(this.rangeMax,  1, 0);
			gl2.glVertex3d(this.rangeMax, -1, 0);
			gl2.glVertex3d(            r,  1, 0);
			gl2.glVertex3d(            r, -1, 0);
			gl2.glEnd();
			gl2.glPopMatrix();
		}
		if(isLit) gl2.glEnable(GL2.GL_LIGHTING);
	}
	
	/**
	 * color the angle line green in the safe zone, red near the limits
	 * @param gl2 the render context
	 */
	public void setAngleColorByRange(GL2 gl2) {
		double a=0;
		if((flags & READ_ONLY_THETA)==0) a=theta;
		else a=alpha;
		
		double halfRange = (rangeMax-rangeMin)/2;
		double midRange = (rangeMax+rangeMin)/2;
		float safety = (float)(Math.abs(a-midRange)/halfRange);
		safety*=safety*safety;  // squared
		//gl2.glColor4d(safety,1-safety,0,0.5);
//		float [] diffuse = {safety,1-safety,0,0};
		
		float [] original = material.getDiffuseColor();
		
		original[0]+=safety;
		original[1]-=safety;
		original[2]-=safety;
		
		material.setDiffuseColor(original[0],original[1],original[2],original[3]);
	}
	
	public boolean hasAdjustableValue() {
		return flags != (READ_ONLY_D | READ_ONLY_R | READ_ONLY_THETA | READ_ONLY_ALPHA );
	}
	
	/**
	 * In any DHLink there should only be one parameter that changes in value.  Return that value.
	 */
	public double getAdjustableValue() {
		if((flags & READ_ONLY_D    )==0) return getD();
		if((flags & READ_ONLY_THETA)==0) return getTheta();
		if((flags & READ_ONLY_R    )==0) return getR();
		return getAlpha();
	}
	
	/**
	 * Set the (one) adjustable value, after making sure it is within the range limits.
	 */
	public void setAdjustableValue(double v) {
		//System.out.println("Adjust begins");
		v = Math.max(Math.min(v, rangeMax), rangeMin);
		if((flags & READ_ONLY_D    )==0) setD(v);
		if((flags & READ_ONLY_THETA)==0) setTheta(v);
		if((flags & READ_ONLY_R    )==0) setR(v);
		if((flags & READ_ONLY_ALPHA)==0) setAlpha(v);
		//System.out.println("Adjust ends");
	}

	public double getD() {
		return d;
	}

	public void setD(double v) {
		if(d==v) return;
		setChanged();
		this.d = v;
		notifyObservers(v);
	}

	public double getTheta() {
		return theta;
	}

	public void setTheta(double v) {
		if(theta==v) return;
		setChanged();
		this.theta = v;
		notifyObservers(v);
	}

	public double getR() {
		return r;
	}

	public void setR(double v) {
		if(r==v) return;
		setChanged();
		this.r = v;
		notifyObservers(v);
	}

	public double getAlpha() {
		return alpha;
	}

	public void setAlpha(double v) {
		if(alpha==v) return;
		setChanged();
		this.alpha = v;
		notifyObservers(v);
	}

	public double getRangeMin() {
		return rangeMin;
	}

	public void setRangeMin(double rangeMin) {
		if(this.rangeMin == rangeMin) return;
		this.rangeMin = rangeMin;
	}

	public double getRangeMax() {
		return rangeMax;
	}

	public void setRangeMax(double rangeMax) {
		if(this.rangeMax == rangeMax) return;
		this.rangeMax = rangeMax;
	}
	
	public void setRange(double rangeMin,double rangeMax) {
		setRangeMin(rangeMin);
		setRangeMax(rangeMax);
	}

	public double getRangeCenter() {
		return (rangeMax+rangeMin)/2.0;
	}
	
	public Matrix4d getPoseCumulative() {
		return poseCumulative;
	}

	public void setLetter(String letter) {
		this.letter = letter;
		
		this.setName("DHLink "+letter);
	}
	public String getLetter() {
		return letter;
	}
}

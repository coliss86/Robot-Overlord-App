package com.marginallyclever.robotOverlord.robots.dog;

import java.util.ArrayList;

import javax.vecmath.Vector3d;

import com.jogamp.opengl.GL2;

public class ConvexShadow {
	private ArrayList<Vector3d> hull = new ArrayList<Vector3d>();
	
	public ConvexShadow() {}
	
	public void add(Vector3d p) {
		int s = hull.size();
		if(s<2) hull.add(p);
		else if(s<3) addThirdPointClockwise(p);
		else if(!pointIsInsideHull(p)) addPointCarefully(p);
	}
	
	private void addThirdPointClockwise(Vector3d c) {
		Vector3d a=hull.get(0);
		Vector3d b=hull.get(1);
		if(pointIsOnTheLeft(c,a,b)) hull.add(1, c);	// new order is acb
		else 						hull.add(   c);	// new order is abc
	}
	
	private boolean pointIsOnTheLeft(Vector3d c,Vector3d a,Vector3d b) {
		Vector3d d=new Vector3d();
		Vector3d e=new Vector3d();
		
		d.sub(b,a);
		d=orthogonalXY(d);
		e.sub(c,a);
		
		return d.dot(e)>0;
	}
	
	// See https://en.wikipedia.org/wiki/Gift_wrapping_algorithm
	private void addPointCarefully(Vector3d p) {
		ArrayList<Vector3d> hull2 = new ArrayList<Vector3d>();
		hull.add(p);
		// first is left-most point in the set.
		Vector3d pointOnHull = hull.get(0);
		for( Vector3d n : hull ) {
			if(pointOnHull.x>n.x) pointOnHull=n;
		}

		Vector3d firstPoint = pointOnHull;
		
		Vector3d endPoint;
		do {
			hull2.add(pointOnHull);
			endPoint = hull.get(0); 
			for( Vector3d b : hull ) {
				if(endPoint == pointOnHull || pointIsOnTheLeft(b, pointOnHull, endPoint)) {
					endPoint = b;
				}
			}
			pointOnHull = endPoint;
		} while(endPoint!=firstPoint);
		
		hull = hull2;
	}

	@SuppressWarnings("unused")
	private Vector3d getCenterOfHull() {
		Vector3d center = new Vector3d();
		
		int s = hull.size();
		for(int i=0;i<s;++i) center.add(hull.get(i));
		center.scale(1.0/(double)s);
		
		return center;
	}

	/**
	 * The hull can be described as a fan of triangles all sharing p0.
	 * if p is inside any of the triangles then it is inside the fan.
	 * @param p the point
	 * @return true if inside the fan.
	 */ 
	private boolean pointIsInsideHull(Vector3d p) {
		Vector3d a=hull.get(0);
		int s = hull.size();
		for(int i=1;i<s;++i) {
			int j=(i+1)%s;
			Vector3d b=hull.get(j);
			if(pointIsOnTheLeft(p, a, b)) return false;
			a=b;
		}
		return false;
	}

	// Is point p inside triangle abc?  Works with clockwise and counter-clockwise triangles.
	@SuppressWarnings("unused")
	private boolean pointIsInTriangleXY(Vector3d p, Vector3d a, Vector3d b, Vector3d c) {
		boolean r0 = pointIsOnTheLeft(p, a, b);
		boolean r1 = pointIsOnTheLeft(p, b, c);
		if(r0!=r1) return false;
		boolean r2 = pointIsOnTheLeft(p, c, a);
		return (r0==r2);
	}

	private Vector3d orthogonalXY(Vector3d d) {
		return new Vector3d(d.y,-d.x,0);
	}

	public void renderAsFan(GL2 gl2) {
		gl2.glBegin(GL2.GL_TRIANGLE_FAN);
		for( Vector3d p : hull ) gl2.glVertex3d(p.x,p.y,p.z);
		gl2.glEnd();
	}
	
	public void renderAsLine(GL2 gl2) {
		gl2.glBegin(GL2.GL_LINE_LOOP);
		for( Vector3d p : hull ) gl2.glVertex3d(p.x,p.y,p.z);
		gl2.glEnd();
	}
}

package com.marginallyclever.robotOverlord.robots.dog;

import com.jogamp.opengl.GL2;
import com.marginallyclever.robotOverlord.Entity;

public class DogWalkOne extends Entity implements DogAnimator {
	/**
	 * 
	 */
	private static final long serialVersionUID = -3164195940151204314L;

	
	public DogWalkOne() {
		super("DogWalkOne - Fixed motion");
	}
	
	@Override
	public void walk(DogRobot dogRobot,GL2 gl2) {
		dogRobot.setDHParameters();
		
		double t = System.currentTimeMillis()*0.001;
		
		for(int i=0;i<4;++i) {
			DogLeg leg = dogRobot.getLeg(i);
			leg.elbow.theta += Math.toDegrees(Math.sin(t))/4;
			leg.foot.theta += Math.toDegrees(Math.cos(t))/4;
			t+=Math.PI/2;
		}
		dogRobot.updateAllLegMatrixes();
	}
}
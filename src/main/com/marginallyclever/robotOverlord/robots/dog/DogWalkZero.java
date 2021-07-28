package com.marginallyclever.robotOverlord.robots.dog;

import com.jogamp.opengl.GL2;
import com.marginallyclever.robotOverlord.Entity;


public class DogWalkZero extends Entity implements DogAnimator {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3298668462432637529L;

	public DogWalkZero() {
		super("DogWalkZero - Freeze");
	}
	
	@Override
	public void walk(DogRobot robot, GL2 gl2) {}
}

package com.marginallyclever.robotOverlord.demos.robotArms;

import com.marginallyclever.robotOverlord.robots.robotArm.RobotArmBone;
import com.marginallyclever.robotOverlord.robots.robotArm.RobotArmFK;
import com.marginallyclever.robotOverlord.shape.Shape;

public class Mantis extends RobotArmFK {
	private static final long serialVersionUID = 1L;

	public Mantis() {
		super();
		setName("Mantis");
	}
	
	@Override
	protected void loadModel() {
		setBaseShape(new Shape("Base","/AH/AH0.obj"));
		
		// name d r a t max min file
		addBone(new RobotArmBone("X",24.5+2.7     ,0              ,-90,  0,120,-120,"/AH/AH1.obj"));
		addBone(new RobotArmBone("Y",0            ,13.9744 + 8.547,  0,-90,170,-170,"/AH/AH2.obj"));
		addBone(new RobotArmBone("Z",0            ,0              ,-90,-90, 86, -91,"/AH/AH3.obj"));
		addBone(new RobotArmBone("U",8.547+14.6855,0              , 90,  0, 90, -90,"/AH/AH4.obj"));
		addBone(new RobotArmBone("V",0            ,0              ,-90,  0, 90, -90,"/AH/AH5.obj"));
		addBone(new RobotArmBone("W",5.0          ,0              ,  0,  0,170,-170,"/AH/AH6.obj"));

		setTextureFilename("/AH/AH_BASE.png");
		adjustModelOriginsToDHLinks();
	}
}

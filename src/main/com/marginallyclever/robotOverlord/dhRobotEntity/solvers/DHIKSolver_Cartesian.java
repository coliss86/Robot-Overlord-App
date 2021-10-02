package com.marginallyclever.robotOverlord.dhRobotEntity.solvers;

import javax.vecmath.Matrix4d;

import com.marginallyclever.convenience.StringHelper;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.robotOverlord.dhRobotEntity.DHRobotModel;
import com.marginallyclever.robotOverlord.dhRobotEntity.PoseFK;

/**
 * Solves Inverse Kinematics for cartesian robots like 3D printers and milling machines.
 * Effectively three prismatic (sliding) joints.
 * @author Dan Royer
 */
@Deprecated
public class DHIKSolver_Cartesian extends DHIKSolver {
	//public double d0,d1,d2;

	/**
	 * @return the number of double values needed to store a valid solution from this DHIKSolver.
	 */
	@Override
	public int getSolutionSize() {
		return 3;
	}

	/**
	 * Starting from a known local origin and a known local hand position, calculate the travel for the given pose.
	 * @param robot The DHRobot description. 
	 * @param targetMatrix the pose that robot is attempting to reach in this solution.
	 * @param keyframe store the computed solution in keyframe.
	 */
	@Override
	public SolutionType solve(DHRobotModel robot,final Matrix4d targetMatrix,final PoseFK keyframe) {
		Matrix4d targetPoseAdj = new Matrix4d(targetMatrix);
		
		if(robot.getToolIndex()!=-1) {
			// there is a transform between the wrist and the tool tip.
			// use the inverse to calculate the wrist Z axis and wrist position.
			robot.getCurrentTool().refreshDHMatrix();
			Matrix4d inverseToolPose = new Matrix4d(robot.getCurrentTool().getPose());
			inverseToolPose.invert();
			targetPoseAdj.mul(inverseToolPose);
		}
		
		Matrix4d m4 = new Matrix4d(targetPoseAdj);

		keyframe.fkValues[0] = m4.m23;
		keyframe.fkValues[1] = m4.m03;
		keyframe.fkValues[2] = m4.m13;
	 
		
		if(true) {
			Log.message("solution={"+StringHelper.formatDouble(keyframe.fkValues[0])+","+
					StringHelper.formatDouble(keyframe.fkValues[1])+","+
					StringHelper.formatDouble(keyframe.fkValues[2])+"}");
		}
		return SolutionType.ONE_SOLUTION;
	}
}

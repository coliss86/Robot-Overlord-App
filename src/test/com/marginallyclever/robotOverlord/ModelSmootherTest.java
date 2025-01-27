package com.marginallyclever.robotOverlord;

import java.io.IOException;

import com.marginallyclever.robotOverlord.shape.MeshSmoother;

public class ModelSmootherTest {
	
	//@Test
	public void smoothAll() throws IOException {		
		float vertexEpsilon = 0.1f;
		float normalEpsilon = 0.25f;
		String wd = System.getProperty("user.dir");
		System.out.println("Working directory="+wd);
		
		System.out.println("hand");			MeshSmoother.smoothModel("/AH/WristRot.stl",		wd + "/AH/WristRot-smooth.stl",		vertexEpsilon,normalEpsilon);
		System.out.println("anchor");		MeshSmoother.smoothModel("/AH/rotBaseCase.stl",	wd + "/AH/rotBaseCase-smooth.stl",	vertexEpsilon,normalEpsilon);
		System.out.println("shoulder");		MeshSmoother.smoothModel("/AH/Shoulder_r1.stl",	wd + "/AH/Shoulder_r1-smooth.stl",	vertexEpsilon,normalEpsilon);
		System.out.println("elbow");		MeshSmoother.smoothModel("/AH/Elbow.stl",			wd + "/AH/Elbow-smooth.stl",		vertexEpsilon,normalEpsilon);
		System.out.println("forearm");		MeshSmoother.smoothModel("/AH/Forearm.stl",		wd + "/AH/Forearm-smooth.stl",		vertexEpsilon,normalEpsilon);
		System.out.println("wrist");		MeshSmoother.smoothModel("/AH/Wrist_r1.stl",		wd + "/AH/Wrist_r1-smooth.stl",		vertexEpsilon,normalEpsilon);
	}
	
}

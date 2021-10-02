module com.marginallyclever.robotOverlord {
	requires java.desktop;
	requires java.prefs;
	requires java.logging;
	requires junit;
	requires org.apache.commons.io;
	requires com.fasterxml.jackson.core;
	requires com.fasterxml.jackson.annotation;
	requires com.fasterxml.jackson.databind;
	requires org.json;
	requires org.slf4j;
	requires vecmath;
	requires jsch;
	requires jinput;
	requires jogamp.fat;
	requires annotations;
	requires jssc;
	requires batik.all;
	requires xml.apis.ext;
	
	uses com.marginallyclever.robotOverlord.Entity;
	provides com.marginallyclever.robotOverlord.Entity with 
		com.marginallyclever.robotOverlord.dhRobotEntity.DHBuilderApp,
		com.marginallyclever.robotOverlord.robots.sixi3.RobotArmIK,
		com.marginallyclever.robotOverlord.robots.LinearStewartPlatform,
		com.marginallyclever.robotOverlord.robots.RotaryStewartPlatform,
		com.marginallyclever.robotOverlord.robots.skycam.Skycam,
		com.marginallyclever.robotOverlord.dhRobotEntity.sixi2.Sixi2,
		com.marginallyclever.robotOverlord.Camera,
		com.marginallyclever.robotOverlord.shape.Shape,
		com.marginallyclever.robotOverlord.Light,
		com.marginallyclever.robotOverlord.Decal,
		com.marginallyclever.robotOverlord.demoAssets.Box,
		com.marginallyclever.robotOverlord.demoAssets.Grid,
		com.marginallyclever.robotOverlord.dhRobotEntity.dhTool.Sixi2ChuckGripper,
		com.marginallyclever.robotOverlord.dhRobotEntity.dhTool.Sixi2LinearGripper,
		com.marginallyclever.robotOverlord.robots.dog.DogRobot;
	
	uses com.marginallyclever.robotOverlord.shape.ShapeLoadAndSave;
	provides com.marginallyclever.robotOverlord.shape.ShapeLoadAndSave with
		com.marginallyclever.robotOverlord.shape.shapeLoadAndSavers.ShapeLoadAndSaveAMF,
		com.marginallyclever.robotOverlord.shape.shapeLoadAndSavers.ShapeLoadAndSaveOBJ,
		com.marginallyclever.robotOverlord.shape.shapeLoadAndSavers.ShapeLoadAndSavePLY,
		com.marginallyclever.robotOverlord.shape.shapeLoadAndSavers.ShapeLoadAndSaveSTL,
		com.marginallyclever.robotOverlord.shape.shapeLoadAndSavers.ShapeLoadAndSave3MF;
	
	uses com.marginallyclever.robotOverlord.dhRobotEntity.dhTool.DHTool;
	provides com.marginallyclever.robotOverlord.dhRobotEntity.dhTool.DHTool with
		com.marginallyclever.robotOverlord.dhRobotEntity.dhTool.Sixi2LinearGripper;
}
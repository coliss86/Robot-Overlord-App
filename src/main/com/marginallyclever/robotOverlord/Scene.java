package com.marginallyclever.robotOverlord;

import java.util.ArrayList;
import java.util.List;

import javax.vecmath.Vector3d;

import com.jogamp.opengl.GL2;
import com.marginallyclever.convenience.Cuboid;
import com.marginallyclever.convenience.IntersectionHelper;
import com.marginallyclever.convenience.OpenGLHelper;
import com.marginallyclever.convenience.log.Log;
import com.marginallyclever.robotOverlord.swingInterface.view.ViewPanel;
import com.marginallyclever.robotOverlord.uiExposedTypes.ColorEntity;

/**
 * Container for all the visible objects in a scene.
 * @author Dan Royer
 * @since 1.6.0
 */
public class Scene extends Entity {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2990084741436544957L;
	public ColorEntity ambientLight = new ColorEntity("Ambient light",0.2,0.2,0.2,1);
	
	public Scene() {
		super();
		setName(Scene.class.getSimpleName());
	}
	
	public void render(GL2 gl2, Camera camera) {
		// Clear the screen and depth buffer
        gl2.glClear(GL2.GL_DEPTH_BUFFER_BIT | GL2.GL_COLOR_BUFFER_BIT);
        // Don't draw triangles facing away from camera
		gl2.glCullFace(GL2.GL_BACK);
		// PASS 0: all the lights
    	
    	// global ambient light
		gl2.glLightModelfv( GL2.GL_LIGHT_MODEL_AMBIENT, ambientLight.getFloatArray(),0);
		
		renderLights(gl2);
		
        camera.render(gl2);
        
		// PASS 1: everything not a light
		for( Entity obj : children ) {
			if(obj instanceof Light) continue;
			// name for picking
			if(obj instanceof PoseEntity) {
				gl2.glPushName(((PoseEntity)obj).getPickName());
			}
			obj.render(gl2);
			// name for picking
			if(obj instanceof PoseEntity) {
				gl2.glPopName();
			}
		}
		
		// PASS 2: everything transparent?
		//renderAllBoundingBoxes(gl2);
	}
	
	private void renderLights(GL2 gl2) {
		turnOffAllLights(gl2);
		
		int i=0;
		for( Entity obj : children ) {
			if(obj instanceof Light) {
				Light light = (Light)obj;
				if(light.isOn()) {
					light.setupLight(gl2,i++);
				}
			}
		}
	}

	private void turnOffAllLights(GL2 gl2) {
		for(int i=0;i<GL2.GL_MAX_LIGHTS;++i) {
			gl2.glDisable(GL2.GL_LIGHT0+i);
		}
	}

	@SuppressWarnings("unused")
	private void renderAllBoundingBoxes(GL2 gl2) {
		// turn of textures so lines draw good
		boolean wasTex = gl2.glIsEnabled(GL2.GL_TEXTURE_2D);
		gl2.glDisable(GL2.GL_TEXTURE_2D);
		// turn off lighting so lines draw good
		boolean wasLit = gl2.glIsEnabled(GL2.GL_LIGHTING);
		gl2.glDisable(GL2.GL_LIGHTING);
		// draw on top of everything else
		int wasOver=OpenGLHelper.drawAtopEverythingStart(gl2);

		renderAllBoundingBoxes(gl2,this);
		
		// return state if needed
		OpenGLHelper.drawAtopEverythingEnd(gl2,wasOver);
		if(wasLit) gl2.glEnable(GL2.GL_LIGHTING);
		if(wasTex) gl2.glEnable(GL2.GL_TEXTURE_2D);	
	}
	
	private void renderAllBoundingBoxes(GL2 gl2, Entity me) {
		for( Entity child : me.getChildren() ) {
			if(child instanceof Collidable) {
				ArrayList<Cuboid> list = ((Collidable)child).getCuboidList();
				for( Cuboid c : list ) {
					c.render(gl2);
				}
			}
			renderAllBoundingBoxes(gl2,child);
		}
	}

	// Search only my children to find the PhysicalEntity with matchin pickName.
	public PoseEntity pickPhysicalEntityWithName(int pickName) {
		for( Entity obj : children ) {
			if(!(obj instanceof PoseEntity)) continue;
			PoseEntity pe = (PoseEntity)obj;
			if( pe.getPickName()==pickName ) {
				return pe;  // found!
			}
		}
		
		return null;
	}
		
	/**
	 * Find all Entities within epsilon mm of pose.
	 * TODO Much optimization could be done here to reduce the search time.
	 * @param target the center of the cube around which to search.   
	 * @param radius the maximum distance to search for entities.
	 * @return a list of found PhysicalObjects
	 */
	public List<PoseEntity> findPhysicalObjectsNear(Vector3d target,double radius) {
		radius/=2;
		
		//Log.message("Finding within "+epsilon+" of " + target);
		List<PoseEntity> found = new ArrayList<PoseEntity>();
		
		// check all children
		for( Entity e : children ) {
			if(e instanceof PoseEntity) {
				// is physical, therefore has position
				PoseEntity po = (PoseEntity)e;
				//Log.message("  Checking "+po.getDisplayName()+" at "+pop);
				Vector3d pop = new Vector3d();
				pop.sub(po.getPosition(),target);
				if(pop.length()<=radius) {
					//Log.message("  in range!");
					// in range!
					found.add(po);
				}
			}
		}
		
		return found;
	}

	/**
	 * @param listA all the cuboids being tested against the world.
	 * @return true if any cuboid in {@code listA} intersects any {@link Cuboid} in the world.
	 */
	public boolean collisionTest(ArrayList<Cuboid> listA) {
		
		// check all children
		for( Entity b : children ) {
			if( !(b instanceof Collidable) ) continue;
			
			ArrayList<Cuboid> listB = ((Collidable)b).getCuboidList();
			if( listB == null ) continue;
			
			if(listB.get(0)==listA.get(0)) {
				// don't test against yourself.
				continue;
			}
			
			// now we have both lists, test them against each other.
			for( Cuboid cuboidA : listA ) {
				for( Cuboid cuboidB : listB ) {
					if( IntersectionHelper.cuboidCuboid(cuboidA,cuboidB) ) {
						Log.message("Collision between "+
							listA.indexOf(cuboidA)+
							" and "+
							b.getName()+"."+listB.indexOf(cuboidB));
						return true;
					}
				}
			}
		}

		// no intersection
		return false;
	}
	
	@Override
	public void getView(ViewPanel view) {
		view.pushStack("Sc", "Scene");
		view.add(ambientLight);
		view.popStack();
	}
}

package assimp.common;

/** Enumerates all supported types of light sources.
 */
public enum LightSourceType {

	aiLightSource_UNDEFINED     /*= 0x0*/,

	/** A directional light source has a well-defined direction
	 * but is infinitely far away. That's quite a good 
	 * approximation for sun light.
	 */
	aiLightSource_DIRECTIONAL   /*= 0x1*/,

	/** A point light source has a well-defined position
	 * in space but no direction - it emits light in all
	 * directions. A normal bulb is a point light.
	 */
	aiLightSource_POINT         /*= 0x2*/,

	/** A spot light source emits light in a specific 
	 * angle. It has a position and a direction it is pointing to.
	 * A good example for a spot light is a light spot in
	 * sport arenas.
	 */
	aiLightSource_SPOT          /*= 0x3*/,
}

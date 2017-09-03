package assimp.common;


/** Stores the memory requirements for different components (e.g. meshes, materials,
 *  animations) of an import. All sizes are in bytes.
 *  @see Importer#getMemoryRequirements()
*/
public class MemoryInfo {

	/** Storage allocated for texture data */
	public int textures;

	/** Storage allocated for material data  */
	public int materials;

	/** Storage allocated for mesh data */
	public int meshes;

	/** Storage allocated for node data */
	public int nodes;

	/** Storage allocated for animation data */
	public int animations;

	/** Storage allocated for camera data */
	public int cameras;

	/** Storage allocated for light data */
	public int lights;

	/** Total storage allocated for the full import. */
	public int total;
	
	void reset(){
		textures = 0;
		materials = 0;
		meshes = 0;
		nodes = 0;
		animations = 0;
		cameras = 0;
		lights = 0;
		total = 0;
	}
}

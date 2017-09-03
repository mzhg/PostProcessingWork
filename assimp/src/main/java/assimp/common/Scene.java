package assimp.common;

/** The root structure of the imported data. <p>
 * 
 *  Everything that was imported from the given file can be accessed from here.
 *  Objects of this class are generally maintained and owned by Assimp, not
 *  by the caller. You shouldn't want to instance it, nor should you ever try to
 *  delete a given scene on your own.
 */
public class Scene implements Copyable<Scene>{
	
	// -------------------------------------------------------------------------------
	/** 
	 * Specifies that the scene data structure that was imported is not complete.
	 * This flag bypasses some internal validations and allows the import 
	 * of animation skeletons, material libraries or camera animation paths 
	 * using Assimp. Most applications won't support such data. 
	 */
	public static final int AI_SCENE_FLAGS_INCOMPLETE	= 0x1;

	/** 
	 * This flag is set by the validation postprocess-step (aiPostProcess_ValidateDS)
	 * if the validation is successful. In a validated scene you can be sure that
	 * any cross references in the data structure (e.g. vertex indices) are valid.
	 */
	public static final int AI_SCENE_FLAGS_VALIDATED	=0x2;

	/** 
	 * This flag is set by the validation postprocess-step (aiPostProcess_ValidateDS)
	 * if the validation is successful but some issues have been found.
	 * This can for example mean that a texture that does not exist is referenced 
	 * by a material or that the bone weights for a vertex don't sum to 1.0 ... .
	 * In most cases you should still be able to use the import. This flag could
	 * be useful for applications which don't capture Assimp's log output.
	 */
	public static final int AI_SCENE_FLAGS_VALIDATION_WARNING  	=0x4;

	/** 
	 * This flag is currently only set by the aiProcess_JoinIdenticalVertices step.
	 * It indicates that the vertices of the output meshes aren't in the internal
	 * verbose format anymore. In the verbose format all vertices are unique,
	 * no vertex is ever referenced by more than one face.
	 */
	public static final int AI_SCENE_FLAGS_NON_VERBOSE_FORMAT  	=0x8;

	 /** 
	 * Denotes pure height-map terrain data. Pure terrains usually consist of quads, 
	 * sometimes triangles, in a regular grid. The x,y coordinates of all vertex 
	 * positions refer to the x,y coordinates on the terrain height map, the z-axis
	 * stores the elevation at a specific point.<p>
	 *
	 * TER (Terragen) and HMP (3D Game Studio) are height map formats.
	 * @note Assimp is probably not the best choice for loading *huge* terrains -
	 * fully triangulated data takes extremely much free store and should be avoided
	 * as long as possible (typically you'll do the triangulation when you actually
	 * need to render it).
	 */
	public static final int AI_SCENE_FLAGS_TERRAIN =0x10;

	/** Any combination of the AI_SCENE_FLAGS_XXX flags. By default 
	* this value is 0, no flags are set. Most applications will
	* want to reject all scenes with the AI_SCENE_FLAGS_INCOMPLETE 
	* bit set.
	*/
	public int mFlags;


	/** The root node of the hierarchy. <p>
	* 
	* There will always be at least the root node if the import
	* was successful (and no special flags have been set). 
	* Presence of further nodes depends on the format and content 
	* of the imported file.
	*/
	public Node mRootNode;


	/** The array of meshes. <p>
	*
	* Use the indices given in the aiNode structure to access 
	* this array. The array is mNumMeshes in size. If the
	* AI_SCENE_FLAGS_INCOMPLETE flag is not set there will always 
	* be at least ONE material.
	*/
	public Mesh[] mMeshes;

	/** The array of materials. <p>
	* 
	* Use the index given in each aiMesh structure to access this
	* array. The array is mNumMaterials in size. If the
	* AI_SCENE_FLAGS_INCOMPLETE flag is not set there will always 
	* be at least ONE material.
	*/
	public Material[] mMaterials;

	/** The array of animations. <p>
	*
	* All animations imported from the given file are listed here.
	* The array is mNumAnimations in size.
	*/
	public Animation[] mAnimations;

	/** The array of embedded textures.<p>
	* 
	* Not many file formats embed their textures into the file.
	* An example is Quake's MDL format (which is also used by
	* some GameStudio versions)
	*/
	public Texture[] mTextures;

	/** The array of light sources.<p>
	* 
	* All light sources imported from the given file are
	* listed here. The array is mNumLights in size.
	*/
	public Light[] mLights;

	/** The array of cameras.<p>
	* 
	* All cameras imported from the given file are listed here.
	* The array is mNumCameras in size. The first camera in the
	* array (if existing) is the default camera view into
	* the scene.
	*/
	public Camera[] mCameras;
	
	@Override
	public Scene copy() {
		Scene scene = new Scene();
		scene.mAnimations = AssUtil.copyOf(mAnimations);
		scene.mCameras = AssUtil.copyOf(mCameras);
		scene.mFlags = mFlags;
		scene.mLights = AssUtil.copyOf(mLights);
		scene.mMaterials = AssUtil.copyOf(mMaterials);
		scene.mMeshes = AssUtil.copyOf(mMeshes);
		if(mRootNode != null)
			scene.mRootNode = mRootNode.copy();
		scene.mTextures = AssUtil.copyOf(mTextures);
		
		// source private data might be NULL if the scene is user-allocated (i.e. for use with the export API)
//		ScenePriv(dest)->mPPStepsApplied = ScenePriv(src) ? ScenePriv(src)->mPPStepsApplied : 0;  TODO
		return scene;
	}
	
	/** Return the number of meshes in the scene. */
	public int getNumMeshes() { return mMeshes!= null ? mMeshes.length : 0;}
	
	/** Return the number of materials in the scene. */
	public int getNumMaterials() { return mMaterials != null ? mMaterials.length : 0;}
	
	/** The number of animations in the scene. */
	public int getNumAnimations() { return mAnimations != null ? mAnimations.length : 0; }
	
	/** The number of textures embedded into the file */
	public int getNumTextures() { return mTextures != null ? mTextures.length : 0;}
	
	/** The number of light sources in the scene. Light sources
	 * are fully optional, in most cases this attribute will be 0 
     */
	public int getNumLights() { return mLights != null ? mLights.length : 0;}
	
	/** The number of cameras in the scene. Cameras
	 * are fully optional, in most cases this attribute will be 0 
     */
	public int getNumCameras() { return mCameras != null ? mCameras.length : 0;}
	
	/** Check whether the scene contains meshes
	 * Unless no special scene flags are set this will always be true.
	 */ 
	public boolean hasMeshes() { return mMeshes != null; }

	/** Check whether the scene contains materials
	 * Unless no special scene flags are set this will always be true.
	 */
	public boolean hasMaterials(){ return mMaterials != null; }

	/** Check whether the scene contains lights */
	public boolean hasLights() { return mLights != null; }

	/** Check whether the scene contains textures */
	public boolean hasTextures() { return mTextures != null; }

	/** Check whether the scene contains cameras */
	public boolean hasCameras() { return mCameras != null; }

	/** Check whether the scene contains animations */
	public boolean hasAnimations() { return mAnimations != null; }
}

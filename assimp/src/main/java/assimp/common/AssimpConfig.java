package assimp.common;

public class AssimpConfig {

	public static boolean ASSIMP_BUILD_DEBUG;
	public static boolean ASSIMP_BUILD_NO_VALIDATEDS_PROCESS = false;
	
	public static boolean MESH_USE_NATIVE_MEMORY = true;
	public static boolean LOADING_USE_NATIVE_MEMORY = false;
	public static final int UINT_MAX = -1;
	
	public static boolean ASSIMP_BUILD_BLENDER_NO_STATS = false;
	public static boolean ASSIMP_BUILD_BLENDER_DEBUG  = true;
	
	public static boolean ASSIMP_BLEND_WITH_GLU_TESSELLATE = false;
	public static boolean ASSIMP_BLEND_WITH_POLY_2_TRI = true;
	public static boolean ASSIMP_BUILD_MS3D_ONE_NODE_PER_MESH = false;
	
	/** The max length of string data. */
	public static final int MAXLEN = 1024;
	
	/** default limit for bone count  */
	public static final int AI_SBBC_DEFAULT_MAX_BONES = 60;
	
	/**
	 * Maximum bone count per mesh for the SplitbyBoneCount step.<p>
	 *
	 * Meshes are split until the maximum number of bones is reached. The default
	 * value is AI_SBBC_DEFAULT_MAX_BONES, which may be altered at
	 * compile-time.<br>
	 * Property data type: integer.
	 */
	public static final String AI_CONFIG_PP_SBBC_MAX_BONES = "PP_SBBC_MAX_BONES";
	
	/** 
	 *  Enables time measurements.<p>
	 *
	 *  If enabled, measures the time needed for each part of the loading
	 *  process (i.e. IO time, importing, postprocessing, ..) and dumps
	 *  these timings to the DefaultLogger. See the @link perf Performance
	 *  Page@endlink for more information on this topic.
	 * 
	 * Property type: boolean. Default value: false.
	 */
	public static final String AI_CONFIG_GLOB_MEASURE_TIME = "GLOB_MEASURE_TIME";
	
	public static final int MAJOR_VERSION = 3;
	public static final int MINOR_VERSION = 1;
	
	// Legal information string - dont't remove this.
	public static final String LEGAL_INFORMATION =
		"Open Asset Import Library (Assimp).\n"+
		"A free C/C++ library to import various 3D file formats into applications\n\n"+
		"(c) 2008-2010, assimp team\n"+
		"License under the terms and conditions of the 3-clause BSD license\n"+
		"http://assimp.sourceforge.net\n";
	
	/**
	 * Configures the AC loader to collect all surfaces which have the
	 * "Backface cull" flag set in separate meshes.<p>
	 * Property type: boolean. Default value: true.
	 */
	public static final String AI_CONFIG_IMPORT_AC_SEPARATE_BFCULL = "IMPORT_AC_SEPARATE_BFCULL";
	
	/**
	 * Configures whether the AC loader evaluates subdivision surfaces (
	 *  indicated by the presence of the 'subdiv' attribute in the file). By
	 *  default, Assimp performs the subdivision using the standard 
	 *  Catmull-Clark algorithm.<p>
	 *  Property type: boolean. Default value: true.
	 */
	public static final String AI_CONFIG_IMPORT_AC_EVAL_SUBDIVISION = "IMPORT_AC_EVAL_SUBDIVISION";
	
	/** Configures the ASE loader to always reconstruct normal vectors
	 *	basing on the smoothing groups loaded from the file.<p>
	 * 
	 * Some ASE files have carry invalid normals, other don't.
	 * * Property type: bool. Default value: true.
	 */
	public static final String AI_CONFIG_IMPORT_ASE_RECONSTRUCT_NORMALS	=
		"IMPORT_ASE_RECONSTRUCT_NORMALS";

	// ---------------------------------------------------------------------------
	/** Configures the M3D loader to detect and process multi-part 
	 *    Quake player models.<p>
	 *
	 * These models usually consist of 3 files, lower.md3, upper.md3 and
	 * head.md3. If this property is set to true, Assimp will try to load and
	 * combine all three files if one of them is loaded. 
	 * Property type: bool. Default value: true.
	 */
	public static final String AI_CONFIG_IMPORT_MD3_HANDLE_MULTIPART =
		"IMPORT_MD3_HANDLE_MULTIPART";

	// ---------------------------------------------------------------------------
	/** Tells the MD3 loader which skin files to load.<p>
	 *
	 * When loading MD3 files, Assimp checks whether a file 
	 * <md3_file_name>_<skin_name>.skin is existing. These files are used by
	 * Quake III to be able to assign different skins (e.g. red and blue team) 
	 * to models. 'default', 'red', 'blue' are typical skin names.
	 * Property type: String. Default value: "default".
	 */
	public static final String AI_CONFIG_IMPORT_MD3_SKIN_NAME =
		"IMPORT_MD3_SKIN_NAME";

	// ---------------------------------------------------------------------------
	/** Specify the Quake 3 shader file to be used for a particular
	 *  MD3 file. This can also be a search path.<p>
	 *
	 * By default Assimp's behaviour is as follows: If a MD3 file 
	 * <tt><any_path>/models/<any_q3_subdir>/<model_name>/<file_name>.md3</tt> is 
	 * loaded, the library tries to locate the corresponding shader file in
	 * <tt><any_path>/scripts/<model_name>.shader</tt>. This property overrides this
	 * behaviour. It can either specify a full path to the shader to be loaded
	 * or alternatively the path (relative or absolute) to the directory where
	 * the shaders for all MD3s to be loaded reside. Assimp attempts to open 
	 * <tt><dir>/<model_name>.shader</tt> first, <tt><dir>/<file_name>.shader</tt> 
	 * is the fallback file. Note that <dir> should have a terminal (back)slash.
	 * Property type: String. Default value: n/a.
	 */
	public static final String AI_CONFIG_IMPORT_MD3_SHADER_SRC =
		"IMPORT_MD3_SHADER_SRC";

	// ---------------------------------------------------------------------------
	/** Configures the LWO loader to load just one layer from the model.<p>
	 * 
	 * LWO files consist of layers and in some cases it could be useful to load
	 * only one of them. This property can be either a string - which specifies
	 * the name of the layer - or an integer - the index of the layer. If the
	 * property is not set the whole LWO model is loaded. Loading fails if the
	 * requested layer is not available. The layer index is zero-based and the
	 * layer name may not be empty.<br>
	 * Property type: Integer. Default value: all layers are loaded.
	 */
	public static final String AI_CONFIG_IMPORT_LWO_ONE_LAYER_ONLY			=
		"IMPORT_LWO_ONE_LAYER_ONLY";

	// ---------------------------------------------------------------------------
	/** Configures the MD5 loader to not load the MD5ANIM file for
	 *  a MD5MESH file automatically.<p>
	 * 
	 * The default strategy is to look for a file with the same name but the
	 * MD5ANIM extension in the same directory. If it is found, it is loaded
	 * and combined with the MD5MESH file. This configuration option can be
	 * used to disable this behaviour.
	 * 
	 * * Property type: bool. Default value: false.
	 */
	public static final String AI_CONFIG_IMPORT_MD5_NO_ANIM_AUTOLOAD			=
		"IMPORT_MD5_NO_ANIM_AUTOLOAD";

	// ---------------------------------------------------------------------------
	/** Defines the begin of the time range for which the LWS loader
	 *    evaluates animations and computes aiNodeAnim's.<p>
	 * 
	 * Assimp provides full conversion of LightWave's envelope system, including
	 * pre and post conditions. The loader computes linearly subsampled animation
	 * chanels with the frame rate given in the LWS file. This property defines
	 * the start time. Note: animation channels are only generated if a node
	 * has at least one envelope with more tan one key assigned. This property.
	 * is given in frames, '0' is the first frame. By default, if this property
	 * is not set, the importer takes the animation start from the input LWS
	 * file ('FirstFrame' line)<br>
	 * Property type: Integer. Default value: taken from file.
	 *
	 * @see AI_CONFIG_IMPORT_LWS_ANIM_END - end of the imported time range
	 */
	public static final String AI_CONFIG_IMPORT_LWS_ANIM_START			=
		"IMPORT_LWS_ANIM_START";
	public static final String AI_CONFIG_IMPORT_LWS_ANIM_END			=
		"IMPORT_LWS_ANIM_END";

	// ---------------------------------------------------------------------------
	/** Defines the output frame rate of the IRR loader.<p>
	 * 
	 * IRR animations are difficult to convert for Assimp and there will
	 * always be a loss of quality. This setting defines how many keys per second
	 * are returned by the converter.<br>
	 * Property type: integer. Default value: 100
	 */
	public static final String AI_CONFIG_IMPORT_IRR_ANIM_FPS				=
		"IMPORT_IRR_ANIM_FPS";

	// ---------------------------------------------------------------------------
	/** Ogre Importer will try to find referenced materials from this file.<p>
	 *
	 * Ogre meshes reference with material names, this does not tell Assimp the file
	 * where it is located in. Assimp will try to find the source file in the following 
	 * order: <material-name>.material, <mesh-filename-base>.material and
	 * lastly the material name defined by this config property.
	 * <br>
	 * Property type: String. Default value: Scene.material.
	 */
	public static final String AI_CONFIG_IMPORT_OGRE_MATERIAL_FILE	=
		"IMPORT_OGRE_MATERIAL_FILE";

	// ---------------------------------------------------------------------------
	/** Ogre Importer detect the texture usage from its filename.<p>
	 *
	 * Ogre material texture units do not define texture type, the textures usage
	 * depends on the used shader or Ogres fixed pipeline. If this config property
	 * is true Assimp will try to detect the type from the textures filename postfix:
	 * _n, _nrm, _nrml, _normal, _normals and _normalmap for normal map, _s, _spec,
	 * _specular and _specularmap for specular map, _l, _light, _lightmap, _occ 
	 * and _occlusion for light map, _disp and _displacement for displacement map.
	 * The matching is case insensitive. Post fix is taken between last "_" and last ".".
	 * Default behavior is to detect type from lower cased texture unit name by 
	 * matching against: normalmap, specularmap, lightmap and displacementmap.
	 * For both cases if no match is found aiTextureType_DIFFUSE is used.
	 * <br>
	 * Property type: Bool. Default value: false.
	 */
	public static final String AI_CONFIG_IMPORT_OGRE_TEXTURETYPE_FROM_FILENAME =
		"IMPORT_OGRE_TEXTURETYPE_FROM_FILENAME";

	/** Specifies whether the IFC loader skips over IfcSpace elements.<p>
	 *
	 * IfcSpace elements (and their geometric representations) are used to
	 * represent, well, free space in a building storey.<br>
	 * Property type: Bool. Default value: true.
	 */
	public static final String AI_CONFIG_IMPORT_IFC_SKIP_SPACE_REPRESENTATIONS = "IMPORT_IFC_SKIP_SPACE_REPRESENTATIONS";


	// ---------------------------------------------------------------------------
	/** Specifies whether the IFC loader skips over 
	 *    shape representations of type 'Curve2D'.<p>
	 *
	 * A lot of files contain both a faceted mesh representation and a outline
	 * with a presentation type of 'Curve2D'. Currently Assimp doesn't convert those,
	 * so turning this option off just clutters the log with errors.<br>
	 * Property type: Bool. Default value: true.
	 */
	public static final String AI_CONFIG_IMPORT_IFC_SKIP_CURVE_REPRESENTATIONS ="IMPORT_IFC_SKIP_CURVE_REPRESENTATIONS";

	// ---------------------------------------------------------------------------
	/** Specifies whether the IFC loader will use its own, custom triangulation
	 *   algorithm to triangulate wall and floor meshes.<p>
	 *
	 * If this property is set to false, walls will be either triangulated by
	 * #aiProcess_Triangulate or will be passed through as huge polygons with 
	 * faked holes (i.e. holes that are connected with the outer boundary using
	 * a dummy edge). It is highly recommended to set this property to true
	 * if you want triangulated data because #aiProcess_Triangulate is known to
	 * have problems with the kind of polygons that the IFC loader spits out for
	 * complicated meshes.
	 * Property type: Bool. Default value: true.
	 */
	public static final String AI_CONFIG_IMPORT_IFC_CUSTOM_TRIANGULATION ="IMPORT_IFC_CUSTOM_TRIANGULATION";

	public static final String AI_CONFIG_IMPORT_COLLADA_IGNORE_UP_DIRECTION ="IMPORT_COLLADA_IGNORE_UP_DIRECTION";
	


	// ---------------------------------------------------------------------------
	/**Global setting to disable generation of skeleton dummy meshes<p>
	 *
	 * Skeleton dummy meshes are generated as a visualization aid in cases which
	 * the input data contains no geometry, but only animation data.
	 * Property data type: boolean. Default value: false
	 */
	public static final String AI_CONFIG_IMPORT_NO_SKELETON_MESHES = "IMPORT_NO_SKELETON_MESHES";
	
	// ---------------------------------------------------------------------------
	/** Set whether the fbx importer will merge all geometry layers present
	 *    in the source file or take only the first.
	 *
	 * The default value is true (1)
	 * Property type: bool
	 */
	public static final String AI_CONFIG_IMPORT_FBX_READ_ALL_GEOMETRY_LAYERS = 
		"IMPORT_FBX_READ_ALL_GEOMETRY_LAYERS";

	// ---------------------------------------------------------------------------
	/** Set whether the fbx importer will read all materials present in the
	 *    source file or take only the referenced materials.
	 *
	 * This is void unless IMPORT_FBX_READ_MATERIALS=1.
	 *
	 * The default value is false (0)
	 * Property type: bool
	 */
	public static final String AI_CONFIG_IMPORT_FBX_READ_ALL_MATERIALS =
		"IMPORT_FBX_READ_ALL_MATERIALS";

	// ---------------------------------------------------------------------------
	/** Set whether the fbx importer will read materials.
	 *
	 * The default value is true (1)
	 * Property type: bool
	 */
	public static final String AI_CONFIG_IMPORT_FBX_READ_MATERIALS =
		"IMPORT_FBX_READ_MATERIALS";

	// ---------------------------------------------------------------------------
	/** Set whether the fbx importer will read cameras.
	 *
	 * The default value is true (1)
	 * Property type: bool
	 */
	public static final String AI_CONFIG_IMPORT_FBX_READ_CAMERAS =
		"IMPORT_FBX_READ_CAMERAS";

	// ---------------------------------------------------------------------------
	/** Set whether the fbx importer will read light sources.
	 *
	 * The default value is true (1)
	 * Property type: bool
	 */
	public static final String AI_CONFIG_IMPORT_FBX_READ_LIGHTS =
		"IMPORT_FBX_READ_LIGHTS";

	// ---------------------------------------------------------------------------
	/** Set whether the fbx importer will read animations.
	 *
	 * The default value is true (1)
	 * Property type: bool
	 */
	public static final String AI_CONFIG_IMPORT_FBX_READ_ANIMATIONS =
		"IMPORT_FBX_READ_ANIMATIONS";

	// ---------------------------------------------------------------------------
	/** Set whether the fbx importer will act in strict mode in which only
	 *    FBX 2013 is supported and any other sub formats are rejected. FBX 2013
	 *    is the primary target for the importer, so this format is best
	 *    supported and well-tested.
	 *
	 * The default value is false (0)
	 * Property type: bool
	 */
	public static final String AI_CONFIG_IMPORT_FBX_STRICT_MODE =
		"IMPORT_FBX_STRICT_MODE";

	// ---------------------------------------------------------------------------
	/** Set whether the fbx importer will preserve pivot points for
	 *    transformations (as extra nodes). If set to false, pivots and offsets
	 *    will be evaluated whenever possible.<p>
	 *
	 * The default value is true (1)<br>
	 * Property type: bool
	 */
	public static final String AI_CONFIG_IMPORT_FBX_PRESERVE_PIVOTS =
		"IMPORT_FBX_PRESERVE_PIVOTS";

	// ---------------------------------------------------------------------------
	/** Specifies whether the importer will drop empty animation curves or
	 *    animation curves which match the bind pose transformation over their
	 *    entire defined range.
	 *
	 * The default value is true (1)
	 * Property type: bool
	 */
	public static final String AI_CONFIG_IMPORT_FBX_OPTIMIZE_EMPTY_ANIMATION_CURVES =
		"IMPORT_FBX_OPTIMIZE_EMPTY_ANIMATION_CURVES";
	
	/** Set the vertex animation keyframe to be imported<p>
	 *
	 * ASSIMP does not support vertex keyframes (only bone animation is supported).
	 * The library reads only one frame of models with vertex animations.
	 * By default this is the first frame.<p>
	 * <b>note</b> The default value is 0. This option applies to all importers.
	 *   However, it is also possible to override the global setting
	 *   for a specific loader. You can use the AI_CONFIG_IMPORT_XXX_KEYFRAME
	 *   options (where XXX is a placeholder for the file format for which you
	 *   want to override the global setting).
	 * Property type: integer.
	 */
	public static final String AI_CONFIG_IMPORT_GLOBAL_KEYFRAME	= "IMPORT_GLOBAL_KEYFRAME";

	public static final String AI_CONFIG_IMPORT_MD3_KEYFRAME	=	"IMPORT_MD3_KEYFRAME";
	public static final String AI_CONFIG_IMPORT_MD2_KEYFRAME	=	"IMPORT_MD2_KEYFRAME";
	public static final String AI_CONFIG_IMPORT_MDL_KEYFRAME	=	"IMPORT_MDL_KEYFRAME";
	public static final String AI_CONFIG_IMPORT_MDC_KEYFRAME	=	"IMPORT_MDC_KEYFRAME";
	public static final String AI_CONFIG_IMPORT_SMD_KEYFRAME	=	"IMPORT_SMD_KEYFRAME";
	public static final String AI_CONFIG_IMPORT_UNREAL_KEYFRAME	= "IMPORT_UNREAL_KEYFRAME";
	
	/** Configures the UNREAL 3D loader to separate faces with different
	 *    surface flags (e.g. two-sided vs. single-sided).<p>
	 *
	 * * Property type: boolean. Default value: true.
	 */
	public static final String AI_CONFIG_IMPORT_UNREAL_HANDLE_FLAGS = "UNREAL_HANDLE_FLAGS";
	
	// ---------------------------------------------------------------------------
	/** Configures the terragen import plugin to compute uv's for 
	 *  terrains, if not given. Furthermore a default texture is assigned.<p>
	 *
	 * UV coordinates for terrains are so simple to compute that you'll usually
	 * want to compute them on your own, if you need them. This option is intended
	 * for model viewers which want to offer an easy way to apply textures to
	 * terrains.<p>
	 * * Property type: boolean. Default value: false.
	 */
	public static final String AI_CONFIG_IMPORT_TER_MAKE_UVS = "IMPORT_TER_MAKE_UVS";
}

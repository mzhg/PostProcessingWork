package assimp.importer.fbx;

/** FBX import settings, parts of which are publicly accessible via their corresponding AI_CONFIG constants */
final class ImportSettings {

	/** enable strict mode:<ul>
	 *  <li>only accept fbx 2012, 2013 files
	 *  <li>on the slightest error, give up.</ul>
	 *
	 *  Basically, strict mode means that the fbx file will actually
	 *  be validated. Strict mode is on by default. */
	boolean strictMode = true;

	/** specifies whether all geometry layers are read and scanned for
	  * usable data channels. The FBX spec indicates that many readers
	  * will only read the first channel and that this is in some way
	  * the recommended way- in reality, however, it happens a lot that 
	  * vertex data is spread among multiple layers. The default
	  * value for this option is true.*/
	boolean readAllLayers = true;

	/** specifies whether all materials are read, or only those that
	 *  are referenced by at least one mesh. Reading all materials
	 *  may make FBX reading a lot slower since all objects
	 *  need to be processed .
	 *  This bit is ignored unless readMaterials=true*/
	boolean readAllMaterials;


	/** import materials (true) or skip them and assign a default 
	 *  material. The default value is true.*/
	boolean readMaterials = true;

	/** import cameras? Default value is true.*/
	boolean readCameras = true;

	/** import light sources? Default value is true.*/
	boolean readLights = true;

	/** import animations (i.e. animation curves, the node
	 *  skeleton is always imported). Default value is true. */
	boolean readAnimations = true;

	/** read bones (vertex weights and deform info).
	 *  Default value is true. */
	boolean readWeights = true;

	/** preserve transformation pivots and offsets. Since these can
	 *  not directly be represented in assimp, additional dummy
	 *  nodes will be generated. Note that settings this to false
	 *  can make animation import a lot slower. The default value
	 *  is true.<p>
	 *
	 *  The naming scheme for the generated nodes is:
	 *    <OriginalName>_$AssimpFbx$_<TransformName><p>
	 *  
	 *  where <TransformName> is one of<ul>
	 *    <li>RotationPivot
	 *    <li>RotationOffset
	 *    <li>PreRotation
	 *    <li>PostRotation
	 *    <li>ScalingPivot
	 *    <li>ScalingOffset
	 *    <li>Translation
	 *    <li>Scaling
	 *    <li>Rotation
	 *    </ul>
	 **/
	boolean preservePivots = true;

	/** do not import animation curves that specify a constant
	 *  values matching the corresponding node transformation.
	 *  The default value is true. */
	boolean optimizeEmptyAnimationCurves = true;
}

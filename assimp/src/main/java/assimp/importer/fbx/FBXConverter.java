package assimp.importer.fbx;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.Vector3f;

import assimp.common.Animation;
import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.Bone;
import assimp.common.Camera;
import assimp.common.DefaultLogger;
import assimp.common.Face;
import assimp.common.Light;
import assimp.common.LightSourceType;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import assimp.common.Metadata;
import assimp.common.MetadataEntry;
import assimp.common.Node;
import assimp.common.NodeAnim;
import assimp.common.ObjectHolder;
import assimp.common.QuatKey;
import assimp.common.Scene;
import assimp.common.TextureType;
import assimp.common.UVTransform;
import assimp.common.VectorKey;
import assimp.common.VertexWeight;

final class FBXConverter {

	static final String MAGIC_NODE_TAG = "_$AssimpFbx$";
	static final int NO_MATERIAL_SEPARATION = -1;
	
	/** the different parts that make up the final local transformation of a fbx node */
//	enum TransformationComp
//	{
	static final int 
		TransformationComp_Translation = 0,
		TransformationComp_RotationOffset = 1,
		TransformationComp_RotationPivot  = 2,
		TransformationComp_PreRotation    = 3,
		TransformationComp_Rotation  	  = 4,
		TransformationComp_PostRotation	  = 5,
		TransformationComp_RotationPivotInverse = 6,
		TransformationComp_ScalingOffset  = 7,
		TransformationComp_ScalingPivot   = 8,
		TransformationComp_Scaling		  = 9,
		TransformationComp_ScalingPivotInverse = 10,
		TransformationComp_GeometricTranslation= 11,
		TransformationComp_GeometricRotation   = 12,
		TransformationComp_GeometricScaling	   = 13,

		TransformationComp_MAXIMUM 			   = 14;
//	};
	
	// 0: not assigned yet, others: index is value - 1
	int defaultMaterialIndex;

	final ArrayList<Mesh> meshes = new ArrayList<Mesh>();
	final ArrayList<Material> materials = new ArrayList<Material>();
	final ArrayList<Animation> animations = new ArrayList<Animation>();
	final ArrayList<Light> lights = new ArrayList<Light>();
	final ArrayList<Camera> cameras = new ArrayList<Camera>();

//	typedef std::map<Material*, int> MaterialMap;
//	MaterialMap materials_converted;
	final Object2IntMap<FBXMaterial> materials_converted = new Object2IntOpenHashMap<FBXMaterial>();

//	typedef std::map<Geometry*, std::vector<int> > MeshMap;
//	MeshMap meshes_converted;
	final Map<Geometry, IntArrayList> meshes_converted = new HashMap<Geometry, IntArrayList>();

	// fixed node name -> which trafo chn components have animations?
//	typedef std::map<std::string, int> NodeAnimBitMap;
//	NodeAnimBitMap node_anim_chn_bits;
	final Object2IntMap<String> node_anim_chn_bits = new Object2IntOpenHashMap<String>();

	// name -> has had its prefix_stripped?
//	typedef std::map<std::string, bool> NodeNameMap;
//	NodeNameMap node_names;
	final Map<String, Boolean> node_names = new HashMap<String, Boolean>();

//	typedef std::map<std::string, std::string> NameNameMap;
//	NameNameMap renamed_nodes;
	final Map<String, String> renamed_nodes = new HashMap<String, String>();

	double anim_fps;

	Scene out;
	final Document doc;
	double min_time = 1e10;
	double max_time = -1e10;
	
	private FBXConverter(Scene out, Document doc){
		this.out = out;
		this.doc = doc;
		
		// animations need to be converted first since this will
		// populate the node_anim_chn_bits map, which is needed
		// to determine which nodes need to be generated.
		convertAnimations();
		convertRootNode();

		if(doc.settings().readAllMaterials) {
			// unfortunately this means we have to evaluate all objects
//			BOOST_FOREACH(ObjectMap::value_type& v,doc.Objects()) {
			for (Long2ObjectMap.Entry<LazyObject> v : doc.objects().long2ObjectEntrySet()){

				final FBXObject ob = v.getValue().get(false);
				if(ob == null) {
					continue;
				}

//				Material* mat = dynamic_cast<Material*>(ob);
//				if(mat) {
//
//					if (materials_converted.find(mat) == materials_converted.end()) {
//						ConvertMaterial(*mat, 0);
//					}
//				}
				
				if(ob instanceof FBXMaterial){
					FBXMaterial mat = (FBXMaterial)ob;
					if(materials_converted.get(mat) != null ) // performance issue.
						convertMaterial(mat, null);
				}
			}
		}

		transferDataToScene();

		// if we didn't read any meshes set the AI_SCENE_FLAGS_INCOMPLETE
		// to make sure the scene passes assimp's validation. FBX files
		// need not contn geometry (i.e. camera animations, raw armatures).
		if (out.getNumMeshes() == 0) {
			out.mFlags |= Scene.AI_SCENE_FLAGS_INCOMPLETE;
		}
	}
	
	static double convert_fbx_time(double time){
		return time/ 46186158000L;
	}
	
	// ------------------------------------------------------------------------------------------------
	// find scene root and trigger recursive scene conversion
	void convertRootNode() 
	{
		out.mRootNode = new Node();
		out.mRootNode.mName = ("RootNode");

		// root has ID 0
		convertNodes(0L, out.mRootNode, new Matrix4f());
	}


	// ------------------------------------------------------------------------------------------------
	// collect and assign child nodes
	void convertNodes(long id, Node parent, Matrix4f parent_transform /*Matrix4x4& parent_transform = Matrix4x4()*/)
	{
		List<Connection> conns = doc.getConnectionsByDestinationSequenced(id, "Model");

//		std::vector<Node*> nodes;
//		nodes.reserve(conns.size());
		List<Node> nodes = new ArrayList<Node>(conns.size());

//		std::vector<Node*> nodes_chn;
		ArrayList<Node> nodes_chn = new ArrayList<Node>();

		try {
//			BOOST_FOREACH(Connection* con, conns) {
			for (Connection con : conns){

				// ignore object-property links
				if(/*con->PropertyName().length()*/ !AssUtil.isEmpty(con.propertyName())) {
					continue;
				}

				FBXObject object = con.sourceObject();
				if(object == null) {
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn("fled to convert source object for Model link");
					continue;
				}


				if(object instanceof Model) {
					Model model = (Model)object;
					nodes_chn.clear();

					Matrix4f new_abs_transform = new Matrix4f(parent_transform);

					// even though there is only a single input node, the design of
					// assimp (or rather: the complicated transformation chn that
					// is employed by fbx) means that we may need multiple Node's
					// to represent a fbx node's transformation.
					generateTransformationNodeChn(model,nodes_chn);

//					assert(nodes_chn.size());
					assert nodes_chn.size() > 0;

					final String original_name = fixNodeName(model.name());

					// check if any of the nodes in the chn has the name the fbx node
					// is supposed to have. If there is none, add another node to 
					// preserve the name - people might have scripts etc. that rely
					// on specific node names.
					Node name_carrier = null;
//					BOOST_FOREACH(Node* prenode, nodes_chn) {
					for(Node prenode : nodes_chn){
						if ( !FBXUtil.strcmp(prenode.mName, original_name) ) {
							name_carrier = prenode;
							break;
						}
					}

					if(name_carrier == null) {
						nodes_chn.add(name_carrier = new Node(original_name));
//						name_carrier = nodes_chn.back();
					}

					//setup metadata on newest node
					setupNodeMetadata(model, AssUtil.back(nodes_chn));

					// link all nodes in a row
					Node last_parent = parent;
//					BOOST_FOREACH(Node* prenode, nodes_chn) {
					for (Node prenode : nodes_chn){
//						assert(prenode);

						if(last_parent != parent) {
//							last_parent->mNumChildren = 1;
//							last_parent->mChildren = new Node*[1];
//							last_parent->mChildren[0] = prenode;
							last_parent.mChildren = new Node[]{prenode};
						}

						prenode.mParent = last_parent;
						last_parent = prenode;

//						new_abs_transform *= prenode->mTransformation;
						Matrix4f.mul(new_abs_transform, prenode.mTransformation, new_abs_transform);
					}

					// attach geometry
					convertModel(model, AssUtil.back(nodes_chn), new_abs_transform);

					// attach sub-nodes
					convertNodes(model.ID(), AssUtil.back(nodes_chn), new_abs_transform);

					if(doc.settings().readLights) {
						convertLights(model);
					}

					if(doc.settings().readCameras) {
						convertCameras(model);
					}

					nodes.add(nodes_chn./*front()*/get(0));	
					nodes_chn.clear();
				}
			}

//			if(nodes.size() > 0) {
//				parent.mChildren = new Node*[nodes.size()]();
//				parent.mNumChildren = static_cast<int>(nodes.size());
//
//				std::swap_ranges(nodes.begin(),nodes.end(),parent.mChildren);
//			}
			parent.mChildren = AssUtil.toArray(nodes, Node.class);
		} 
		catch(Exception e)	{
//			Util::delete_fun<Node> deleter;
//			std::for_each(nodes.begin(),nodes.end(),deleter);
//			std::for_each(nodes_chn.begin(),nodes_chn.end(),deleter);
			e.printStackTrace();
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	void convertLights(Model model)
	{
		List<NodeAttribute> node_attrs = model.getAttributes();
//			BOOST_FOREACH(NodeAttribute* attr, node_attrs) {
		for(NodeAttribute attr : node_attrs){
//				Light* light = dynamic_cast<Light*>(attr);
//				if(light) {
//					ConvertLight(model, *light);
//				}
			if(attr instanceof FBXLight){
				convertLight(model, (FBXLight)attr);
			}
		}
	}
	
	// ------------------------------------------------------------------------------------------------
	void convertCameras(Model model)
	{
//		std::vector<NodeAttribute*>& node_attrs = model.GetAttributes();
//		BOOST_FOREACH(NodeAttribute* attr, node_attrs) {
//			Camera* cam = dynamic_cast<Camera*>(attr);
//			if(cam) {
//				ConvertCamera(model, *cam);
//			}
//		}
		
		for(NodeAttribute attr : model.getAttributes()){
			if(attr instanceof FBXCamera)
				convertCamera(model, (FBXCamera)attr);
		}
	}


	// ------------------------------------------------------------------------------------------------
	void convertLight(Model model, FBXLight light)
	{
		Light out_light;
		lights.add(out_light = new Light());
//		Light* out_light = lights.back();

		out_light.mName = fixNodeName(model.name());

		final float intensity = light.intensity();
		final Vector3f col = light.color();

		out_light.mColorDiffuse.set(col.x,col.y,col.z);
		out_light.mColorDiffuse.x *= intensity;
		out_light.mColorDiffuse.y *= intensity;
		out_light.mColorDiffuse.z *= intensity;

		out_light.mColorSpecular.set(out_light.mColorDiffuse);

		switch(light.lightType())
		{
		case FBXLight.Type_Point:
			out_light.mType = LightSourceType.aiLightSource_POINT;
			break;

		case FBXLight.Type_Directional:
			out_light.mType = LightSourceType.aiLightSource_DIRECTIONAL;
			break;

		case FBXLight.Type_Spot:
			out_light.mType = LightSourceType.aiLightSource_SPOT;
			out_light.mAngleOuterCone = (float)Math.toRadians(light.outerAngle());
			out_light.mAngleInnerCone = (float)Math.toRadians(light.innerAngle());
			break;

		case FBXLight.Type_Area:
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("cannot represent area light, set to UNDEFINED");
			out_light.mType = LightSourceType.aiLightSource_UNDEFINED;
			break;

		case FBXLight.Type_Volume:
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("cannot represent volume light, set to UNDEFINED");
			out_light.mType = LightSourceType.aiLightSource_UNDEFINED;
			break;
		default:
			assert(false);
		}

		// XXX: how to best convert the near and far decay ranges?
		switch(light.decayType())
		{
		case FBXLight.Decay_None:
			out_light.mAttenuationConstant = 1.0f;
			break;
		case FBXLight.Decay_Linear:
			out_light.mAttenuationLinear = 1.0f;
			break;
		case FBXLight.Decay_Quadratic:
			out_light.mAttenuationQuadratic = 1.0f;
			break;
		case FBXLight.Decay_Cubic:
			if(DefaultLogger.LOG_OUT)DefaultLogger.warn("cannot represent cubic attenuation, set to Quadratic");
			out_light.mAttenuationQuadratic = 1.0f;
			break;
		default:
			assert(false);
		}
	}


	// ------------------------------------------------------------------------------------------------
	void convertCamera(Model model, FBXCamera cam)
	{
		Camera out_camera;
		cameras.add(out_camera = new Camera());
//		Camera* out_camera = cameras.back();

		out_camera.mName = (fixNodeName(model.name()));

		out_camera.mAspect = cam.aspectWidth() / cam.aspectHeight();
		out_camera.mPosition.set(cam.position());
//		out_camera.mLookAt = cam.interestPosition() - out_camera.mPosition;
		Vector3f.sub(cam.interestPosition(),out_camera.mPosition, out_camera.mLookAt);

		// BUG HERE cam.FieldOfView() returns 1.0f every time.  1.0f is default value.
		out_camera.mHorizontalFOV = (float)Math.toRadians(cam.fieldOfView());
	}


	// ------------------------------------------------------------------------------------------------
	// this returns unified names usable within assimp identifiers (i.e. no space characters -
	// while these would be allowed, they are a potential trouble spot so better not use them).
	String nameTransformationComp(int comp)
	{
		switch(comp)
		{
		case TransformationComp_Translation:
			return "Translation";
		case TransformationComp_RotationOffset:
			return "RotationOffset";
		case TransformationComp_RotationPivot:
			return "RotationPivot";
		case TransformationComp_PreRotation:
			return "PreRotation";
		case TransformationComp_Rotation:
			return "Rotation";
		case TransformationComp_PostRotation:
			return "PostRotation";
		case TransformationComp_RotationPivotInverse:
			return "RotationPivotInverse";
		case TransformationComp_ScalingOffset:
			return "ScalingOffset";
		case TransformationComp_ScalingPivot:
			return "ScalingPivot";
		case TransformationComp_Scaling:
			return "Scaling";
		case TransformationComp_ScalingPivotInverse:
			return "ScalingPivotInverse";
		case TransformationComp_GeometricScaling:
			return "GeometricScaling";
		case TransformationComp_GeometricRotation:
			return "GeometricRotation";
		case TransformationComp_GeometricTranslation:
			return "GeometricTranslation";
		case TransformationComp_MAXIMUM: // this is to silence compiler warnings
			break;
		}

		assert(false);
		return null;
	}


	// ------------------------------------------------------------------------------------------------
	// note: this returns the REAL fbx property names
	String nameTransformationCompProperty(int comp)
	{
		switch(comp)
		{
		case TransformationComp_Translation:
			return "Lcl Translation";
		case TransformationComp_RotationOffset:
			return "RotationOffset";
		case TransformationComp_RotationPivot:
			return "RotationPivot";
		case TransformationComp_PreRotation:
			return "PreRotation";
		case TransformationComp_Rotation:
			return "Lcl Rotation";
		case TransformationComp_PostRotation:
			return "PostRotation";
		case TransformationComp_RotationPivotInverse:
			return "RotationPivotInverse";
		case TransformationComp_ScalingOffset:
			return "ScalingOffset";
		case TransformationComp_ScalingPivot:
			return "ScalingPivot";
		case TransformationComp_Scaling:
			return "Lcl Scaling";
		case TransformationComp_ScalingPivotInverse:
			return "ScalingPivotInverse";
		case TransformationComp_GeometricScaling:
			return "GeometricScaling";
		case TransformationComp_GeometricRotation:
			return "GeometricRotation";
		case TransformationComp_GeometricTranslation:
			return "GeometricTranslation";
		case TransformationComp_MAXIMUM: // this is to silence compiler warnings
			break;
		}

		assert(false);
		return null;
	}


	// ------------------------------------------------------------------------------------------------
	Vector3f transformationCompDefaultValue(int comp, Vector3f out)
	{
		// XXX a neat way to solve the never-ending special cases for scaling 
		// would be to do everything in log space!
		if(comp == TransformationComp_Scaling)out.set(1.f,1.f,1.f); 
		else out.set(0,0,0);
		return out;
	}
	
	// ------------------------------------------------------------------------------------------------
	void getRotationMatrix(int mode, Vector3f rotation, Matrix4f out)
	{
		out.setIdentity();
		if(mode == Model.RotOrder_SphericXYZ) {
			DefaultLogger.error("Unsupported RotationMode: SphericXYZ");
			return;
		}

		final float angle_epsilon = 1e-6f;

//		out = Matrix4x4();

		boolean is_id[] = { true, true, true };

		Matrix4f[] temp = new Matrix4f[3];
		AssUtil.initArray(temp);
		if(Math.abs(rotation.z) > angle_epsilon) {
//			Matrix4x4::RotationZ(AI_DEG_TO_RAD(rotation.z),temp[2]);
			temp[2].rotate((float)Math.toRadians(rotation.z), Vector3f.Z_AXIS);
			is_id[2] = false;
		}
		if(Math.abs(rotation.y) > angle_epsilon) {
//			Matrix4x4::RotationY(AI_DEG_TO_RAD(rotation.y),temp[1]);
			temp[1].rotate((float)Math.toRadians(rotation.y), Vector3f.Y_AXIS);
			is_id[1] = false;
		}
		if(Math.abs(rotation.x) > angle_epsilon) {
//			Matrix4x4::RotationX(AI_DEG_TO_RAD(rotation.x),temp[0]);
			temp[0].rotate((float)Math.toRadians(rotation.x), Vector3f.X_AXIS);
			is_id[0] = false;
		}

		int order[] = {-1, -1, -1};

		// note: rotation order is inverted since we're left multiplying as is usual in assimp
		switch(mode)
		{
		case Model.RotOrder_EulerXYZ:
			order[0] = 2;
			order[1] = 1;
			order[2] = 0;
			break;

		case Model.RotOrder_EulerXZY: 
			order[0] = 1;
			order[1] = 2;
			order[2] = 0;
			break;

		case Model.RotOrder_EulerYZX:
			order[0] = 0;
			order[1] = 2;
			order[2] = 1;
			break;

		case Model.RotOrder_EulerYXZ: 
			order[0] = 2;
			order[1] = 0;
			order[2] = 1;
			break;

		case Model.RotOrder_EulerZXY: 
			order[0] = 1;
			order[1] = 0;
			order[2] = 2;
			break;

		case Model.RotOrder_EulerZYX:
			order[0] = 0;
			order[1] = 1;
			order[2] = 2;
			break;

			default:
				assert(false);
		}
        
        assert((order[0] >= 0) && (order[0] <= 2));
        assert((order[1] >= 0) && (order[1] <= 2));
        assert((order[2] >= 0) && (order[2] <= 2));

		if(!is_id[order[0]]) {
			out.load(temp[order[0]]);
		}

		if(!is_id[order[1]]) {
//			out = out * temp[order[1]];
			Matrix4f.mul(out, temp[order[1]], out);
		}

		if(!is_id[order[2]]) {
//			out = out * temp[order[2]];
			Matrix4f.mul(out, temp[order[2]], out);
		}
	}


	// ------------------------------------------------------------------------------------------------
	/** checks if a node has more than just scaling, rotation and translation components */
	boolean needsComplexTransformationChn(Model model)
	{
		final PropertyTable props = model.props();
		ObjectHolder<Boolean> ok = new ObjectHolder<Boolean>();

		final float zero_epsilon = 1e-6f;
		for (int i = 0; i < TransformationComp_MAXIMUM; ++i) {
//			TransformationComp comp = static_cast<TransformationComp>(i);
			final int comp = i;

			if( comp == TransformationComp_Rotation || comp == TransformationComp_Scaling || comp == TransformationComp_Translation ||
				comp == TransformationComp_GeometricScaling || comp == TransformationComp_GeometricRotation || comp == TransformationComp_GeometricTranslation ) { 
				continue;
			}

			final Vector3f v = PropertyTable.propertyGet(props,nameTransformationCompProperty(comp),ok);
			if(ok.get() && v.lengthSquared() > zero_epsilon) {
				return true;
			}
		}

		return false;
	}


	// ------------------------------------------------------------------------------------------------
	// note: name must be a FixNodeName() result
	String nameTransformationChnNode(String name, int comp)
	{
		return name + (MAGIC_NODE_TAG) + "_" + nameTransformationComp(comp);
	}

	void translation(Vector3f v, Matrix4f mat){
		mat.m30 = v.x;
		mat.m31 = v.y;
		mat.m32 = v.z;
	}
	
	void scaling(Vector3f v, Matrix4f mat){
		mat.m00 = v.x;
		mat.m11 = v.y;
		mat.m22 = v.z;
	}

	// ------------------------------------------------------------------------------------------------
	/** note: memory for output_nodes will be managed by the caller */
	void generateTransformationNodeChn(Model model, List<Node> output_nodes)
	{
		PropertyTable props = model.props();
		int rot = model.rotationOrder();

		ObjectHolder<Boolean> ok = new ObjectHolder<Boolean>();

//		Matrix4x4 chn[TransformationComp_MAXIMUM];
		Matrix4f[] chn = new Matrix4f[TransformationComp_MAXIMUM];  // TODO need Cache?
//		std::fill_n(chn, static_cast<int>(TransformationComp_MAXIMUM), Matrix4x4());
		AssUtil.initArray(chn);
		
		// generate transformation matrices for all the different transformation components
		final float zero_epsilon = 1e-6f;
		boolean is_complex = false;

		Vector3f PreRotation =PropertyTable.propertyGet(props,"PreRotation",ok);
		if(ok.get() && PreRotation.lengthSquared() > zero_epsilon) {
			is_complex = true;

			getRotationMatrix(rot, PreRotation, chn[TransformationComp_PreRotation]);
		}

		Vector3f PostRotation = PropertyTable.propertyGet(props,"PostRotation",ok);
		if(ok.get() && PostRotation.lengthSquared() > zero_epsilon) {
			is_complex = true;
			
			getRotationMatrix(rot, PostRotation, chn[TransformationComp_PostRotation]);
		}

		Vector3f RotationPivot = PropertyTable.propertyGet(props,"RotationPivot",ok);
		if(ok.get() && RotationPivot.lengthSquared() > zero_epsilon) {
			is_complex = true;
			
			translation(RotationPivot,chn[TransformationComp_RotationPivot]);
			translation(RotationPivot.negate(),chn[TransformationComp_RotationPivotInverse]);
		}

		Vector3f RotationOffset = PropertyTable.propertyGet(props,"RotationOffset",ok);
		if(ok.get() && RotationOffset.lengthSquared() > zero_epsilon) {
			is_complex = true;

			translation(RotationOffset,chn[TransformationComp_RotationOffset]);
		}

		Vector3f ScalingOffset = PropertyTable.propertyGet(props,"ScalingOffset",ok);
		if(ok.get() && ScalingOffset.lengthSquared() > zero_epsilon) {
			is_complex = true;
			
			translation(ScalingOffset,chn[TransformationComp_ScalingOffset]);
		}

		Vector3f ScalingPivot = PropertyTable.propertyGet(props,"ScalingPivot",ok);
		if(ok.get() && ScalingPivot.lengthSquared() > zero_epsilon) {
			is_complex = true;

			translation(ScalingPivot,chn[TransformationComp_ScalingPivot]);
			translation(ScalingPivot.negate(),chn[TransformationComp_ScalingPivotInverse]);
		}

		Vector3f Translation = PropertyTable.propertyGet(props,"Lcl Translation",ok);
		if(ok.get() && Translation.lengthSquared() > zero_epsilon) {
			translation(Translation,chn[TransformationComp_Translation]);
		}

		Vector3f Scaling = PropertyTable.propertyGet(props,"Lcl Scaling",ok);
		if(ok.get() && Math.abs(Scaling.lengthSquared()-1.0f) > zero_epsilon) {
			scaling(Scaling,chn[TransformationComp_Scaling]);
		}

		Vector3f Rotation = PropertyTable.propertyGet(props,"Lcl Rotation",ok);
		if(ok.get() && Rotation.lengthSquared() > zero_epsilon) {
			getRotationMatrix(rot, Rotation, chn[TransformationComp_Rotation]);
		}
		
		Vector3f GeometricScaling = PropertyTable.propertyGet(props, "GeometricScaling", ok);
		if (ok.get() && Math.abs(GeometricScaling.lengthSquared() - 1.0f) > zero_epsilon) {
			scaling(GeometricScaling, chn[TransformationComp_GeometricScaling]);
		}
		
		Vector3f GeometricRotation = PropertyTable.propertyGet(props, "GeometricRotation", ok);
		if (ok.get() && GeometricRotation.lengthSquared() > zero_epsilon) {
			getRotationMatrix(rot, GeometricRotation, chn[TransformationComp_GeometricRotation]);
		}

		Vector3f GeometricTranslation = PropertyTable.propertyGet(props, "GeometricTranslation", ok);
		if (ok.get() && GeometricTranslation.lengthSquared() > zero_epsilon){
			translation(GeometricTranslation, chn[TransformationComp_GeometricTranslation]);
		}

		// is_complex needs to be consistent with NeedsComplexTransformationChn()
		// or the interplay between this code and the animation converter would
		// not be guaranteed.
		assert(needsComplexTransformationChn(model) == is_complex);

		String name = fixNodeName(model.name());

		// now, if we have more than just Translation, Scaling and Rotation,
		// we need to generate a full node chn to accommodate for assimp's
		// lack to express pivots and offsets.
		if(is_complex && doc.settings().preservePivots) {
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.info("generating full transformation chn for node: " + name);

			// query the anim_chn_bits dictionary to find out which chn elements
			// have associated node animation channels. These can not be dropped 
			// even if they have identity transform in bind pose.
//			NodeAnimBitMap::const_iterator it = node_anim_chn_bits.find(name);
//			int anim_chn_bitmask = (it == node_anim_chn_bits.end() ? 0 : (*it).second);
			final int anim_chn_bitmask = node_anim_chn_bits.getInt(name);

			int bit = 0x1;
			for (int i = 0; i < TransformationComp_MAXIMUM; ++i, bit <<= 1) {
				final int comp = i;
				
				if (AssUtil.isIdentity(chn[i]) && (anim_chn_bitmask & bit) == 0) {
					continue;
				}

				Node nd = new Node();
				output_nodes.add(nd);
				
				nd.mName = (nameTransformationChnNode(name, comp));
				nd.mTransformation.load(chn[i]);
			}

			assert(output_nodes.size() > 0);
			return;
		}

		// else, we can just multiply the matrices together
		Node nd = new Node();
		output_nodes.add(nd);

		nd.mName = (name);

		for (int i = 0; i < TransformationComp_MAXIMUM; ++i) {
//			nd->mTransformation = nd->mTransformation * chn[i];
			Matrix4f.mul(nd.mTransformation, chn[i], nd.mTransformation);
		}
	}
	
	// ------------------------------------------------------------------------------------------------

	void setupNodeMetadata(Model model, Node nd)
	{
		PropertyTable props = model.props();
		Map<String, Object> unparsedProperties = props.getUnparsedProperties();

		// create metadata on node
		int numStaticMetaData = 2;
		Metadata data = new Metadata();
		int numProperties = unparsedProperties.size() + numStaticMetaData;
		data.mKeys = new String[numProperties];
		data.mValues = new MetadataEntry[numProperties];
		nd.mMetaData = data;
		int index = 0;

		// find user defined properties (3ds Max)
		data.set(index++, "UserProperties", PropertyTable.propertyGet(props, "UDP3DSMAX", ""));
		unparsedProperties.remove("UDP3DSMAX");
		// preserve the info that a node was marked as Null node in the original file.
		data.set(index++, "IsNull", model.isNull() ? true : false);

		// add unparsed properties to the node's metadata
//		BOOST_FOREACH(DirectPropertyMap::value_type& prop, unparsedProperties) {
		for (Map.Entry<String, Object> prop : unparsedProperties.entrySet()){

			// Interpret the property as a concrete type
//			if (TypedProperty<bool>* interpreted = prop.second->As<TypedProperty<bool> >())
//				data.Set(index++, prop.first, interpreted->Value());
//			else if (TypedProperty<int>* interpreted = prop.second->As<TypedProperty<int> >())
//				data.Set(index++, prop.first, interpreted->Value());
//			else if (TypedProperty<uint64_t>* interpreted = prop.second->As<TypedProperty<uint64_t> >())
//				data.Set(index++, prop.first, interpreted->Value());
//			else if (TypedProperty<float>* interpreted = prop.second->As<TypedProperty<float> >())
//				data.Set(index++, prop.first, interpreted->Value());
//			else if (TypedProperty<std::string>* interpreted = prop.second->As<TypedProperty<std::string> >())
//				data.Set(index++, prop.first, String(interpreted->Value()));
//			else if (TypedProperty<Vector3D>* interpreted = prop.second->As<TypedProperty<Vector3D> >())
//				data.Set(index++, prop.first, interpreted->Value());
//			else
//				assert(false);
			data.set(index++, prop.getKey(), prop.getValue());
		}
	}

	// ------------------------------------------------------------------------------------------------
	void convertModel(Model model, Node nd, Matrix4f node_global_transform)
	{
		List<Geometry> geos = model.getGeometry();

//		std::vector<int> meshes;
//		meshes.reserve(geos.size());
		IntArrayList meshes = new IntArrayList(geos.size());

//		BOOST_FOREACH(Geometry* geo, geos) {
		for(Geometry geo : geos){
			
			if(geo instanceof MeshGeometry){
				MeshGeometry mesh = (MeshGeometry)(geo);
				IntArrayList indices = convertMesh(mesh, model, node_global_transform);
				if(indices != null)
					meshes.addAll(indices);
			}else{
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("ignoring unrecognized geometry: " + geo.name());
			}

//			if(mesh) {
//				std::vector<int>& indices = ConvertMesh(*mesh, model, node_global_transform);
//				std::copy(indices.begin(),indices.end(),std::back_inserter(meshes) );
//			}
//			else {
//				FBXImporter::LogW
//			}
		}

		if(meshes.size() > 0) {
//			nd.mMeshes = new int[meshes.size()]();
//			nd.mNumMeshes = static_cast<int>(meshes.size());
//
//			std::swap_ranges(meshes.begin(),meshes.end(),nd.mMeshes);
			if(meshes.size() == meshes.elements().length){
				nd.mMeshes = meshes.elements();
			}else{
				nd.mMeshes = meshes.toIntArray();
			}
		}
	}


	// ------------------------------------------------------------------------------------------------
	// MeshGeometry -> Mesh, return mesh index + 1 or 0 if the conversion fled
	IntArrayList convertMesh(MeshGeometry mesh,Model model, Matrix4f node_global_transform)
	{
//		std::vector<int> temp; 
		IntArrayList temp /*= new IntArrayList()*/;
//		MeshMap::const_iterator it = meshes_converted.find(&mesh);
//		if (it != meshes_converted.end()) {
//			std::copy((*it).second.begin(),(*it).second.end(),std::back_inserter(temp));
//			return temp;
//		}
		
		IntArrayList it = meshes_converted.get(mesh);
		if(it != null)
			return it;

		FloatBuffer vertices = mesh.getVertices();
		IntArrayList faces = mesh.getFaceIndexCounts();
		if(AssUtil.isEmpty(vertices) || AssUtil.isEmpty(faces)) {
			if(DefaultLogger.LOG_OUT)DefaultLogger.warn("ignoring empty geometry: " + mesh.name());
			return null;
		}

		// one material per mesh maps easily to Mesh. Multiple material 
		// meshes need to be split.
		IntArrayList mindices = mesh.getMaterialIndices();
		if (doc.settings().readMaterials && !AssUtil.isEmpty(mindices)) {
			int[] elem = mindices.elements();
			int base = elem[0];
//			BOOST_FOREACH(MatIndexArray::value_type index, mindices) {
			for (int i = 0; i < mindices.size(); i++){
				int index = elem[i];
				if(index != base) {
					return convertMeshMultiMaterial(mesh, model, node_global_transform);
				}
			}
		}

		temp = new IntArrayList(1);
		// faster codepath, just copy the data
		temp.add(convertMeshSingleMaterial(mesh, model, node_global_transform));
		return temp;
	}


	// ------------------------------------------------------------------------------------------------
	Mesh setupEmptyMesh(MeshGeometry mesh)
	{
		Mesh out_mesh = new Mesh();
		meshes.add(out_mesh);
//		meshes_converted[&mesh].push_back(static_cast<int>(meshes.size()-1));
		IntArrayList list = meshes_converted.get(mesh);
		if(list == null)
			meshes_converted.put(mesh, list = new IntArrayList());
		list.add(meshes.size() - 1);

		// set name
		String name = mesh.name();
		if (/*name.substr(0,10) == "Geometry::"*/ name.startsWith("Geometry::")) {
			name = name.substring(10);
		}

		if(!AssUtil.isEmpty(name)) {
			out_mesh.mName = (name);
		}

		return out_mesh;
	}


	// ------------------------------------------------------------------------------------------------
	int convertMeshSingleMaterial(MeshGeometry mesh, Model model, Matrix4f node_global_transform)	
	{
		IntArrayList mindices = mesh.getMaterialIndices();
		Mesh out_mesh = setupEmptyMesh(mesh); 

		FloatBuffer vertices = mesh.getVertices();
		IntArrayList faces = mesh.getFaceIndexCounts();

		// copy vertices
//		out_mesh.mNumVertices = static_cast<int>(vertices.size());
//		out_mesh.mVertices = new Vector3D[vertices.size()];
//		std::copy(vertices.begin(),vertices.end(),out_mesh.mVertices);
		out_mesh.mVertices = MemoryUtil.refCopy(vertices, AssimpConfig.MESH_USE_NATIVE_MEMORY);

		// generate dummy faces
//		out_mesh.mNumFaces = static_cast<int>(faces.size());
//		Face* fac = out_mesh.mFaces = new Face[faces.size()]();
		out_mesh.mFaces = new Face[faces.size()];
		int fac = 0;
		
		int cursor = 0;
//		BOOST_FOREACH(int pcount, faces) {
		for(int i = 0; i < faces.size(); i++){
			int pcount = faces.getInt(i);
//			Face& f = *fac++;
			Face f = out_mesh.mFaces[fac++] = Face.createInstance(pcount);
//			f.mNumIndices = pcount;
//			f.mIndices = new int[pcount];
			switch(pcount) 
			{
			case 1:
				out_mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_POINT;
				break;
			case 2:
				out_mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_LINE;
				break;
			case 3:
				out_mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_TRIANGLE;
				break;
			default:
				out_mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_POLYGON;
				break;
			}
			for (int k = 0; k < pcount; ++k) {
//				f.mIndices[i] = cursor++;
				f.set(k, cursor ++);
			}
		}

		// copy normals
//		FloatBuffer normals = mesh.GetNormals();
//		if(normals.size()) {
//			_assert(normals.size() == vertices.size());
//
//			out_mesh.mNormals = new Vector3D[vertices.size()];
//			std::copy(normals.begin(),normals.end(),out_mesh.mNormals);
//		}
		
		out_mesh.mNormals = MemoryUtil.refCopy(mesh.getNormals(), AssimpConfig.MESH_USE_NATIVE_MEMORY);

		// copy tangents - assimp requires both tangents and bitangents (binormals)
		// to be present, or neither of them. Compute binormals from normals
		// and tangents if needed.
		FloatBuffer tangents = mesh.getTangents();
		FloatBuffer binormals = mesh.getBinormals();

		if(tangents != null) {
			FloatBuffer tempBinormals;
			if (/*!binormals->size()*/ binormals == null) {
				FloatBuffer normals = mesh.getNormals();
				if (normals != null) {
//					tempBinormals.resize(normals.size());
					tempBinormals = MemoryUtil.createFloatBuffer(normals.remaining(),AssimpConfig.MESH_USE_NATIVE_MEMORY);
					Vector3f l = new Vector3f();
					Vector3f r = new Vector3f();
					Vector3f d = new Vector3f();
					int count = tangents.remaining()/3;
					for (int i = 0; i < count; ++i) {
//						tempBinormals[i] = normals[i] ^ tangents[i];
						l.load(normals);
						r.load(tangents);
						Vector3f.cross(l, r, d);
						d.store(tempBinormals);
					}
					
					normals.flip();
					tangents.flip();
					tempBinormals.flip();

					binormals = tempBinormals;
				}
				else {
					binormals = null;	
				}
			}

			if(binormals != null) {
				assert(tangents.remaining() == vertices.remaining() && binormals.remaining() == vertices.remaining());

//				out_mesh.mTangents = new Vector3D[vertices.size()];
//				std::copy(tangents.begin(),tangents.end(),out_mesh.mTangents);
//
//				out_mesh.mBitangents = new Vector3D[vertices.size()];
//				std::copy(binormals->begin(),binormals->end(),out_mesh.mBitangents);
				out_mesh.mTangents   = MemoryUtil.refCopy(tangents,  AssimpConfig.MESH_USE_NATIVE_MEMORY);
				out_mesh.mBitangents = MemoryUtil.refCopy(binormals, AssimpConfig.MESH_USE_NATIVE_MEMORY);
			}
		}

		// copy texture coords
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i) {
			FloatBuffer uvs = mesh.getTextureCoords(i);
			if(uvs == null) {
				break;
			}

//			Vector3D* out_uv = out_mesh.mTextureCoords[i] = new Vector3D[vertices.size()];
//			BOOST_FOREACH(Vector2D& v, uvs) {
//				*out_uv++ = Vector3D(v.x,v.y,0.0f);
//			}
			FloatBuffer out_uv = out_mesh.mTextureCoords[i] = MemoryUtil.createFloatBuffer(vertices.remaining(), AssimpConfig.MESH_USE_NATIVE_MEMORY);
			int count = vertices.remaining()/3;
			for(int k = 0; k < count; k++){
				out_uv.put(uvs.get()).put(uvs.get()).put(0);
			}
			
			uvs.flip();
			out_uv.flip();
			out_mesh.mNumUVComponents[i] = 2;
		}

		// copy vertex colors
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; ++i) {
			FloatBuffer colors = mesh.getVertexColors(i);
			if(colors == null) {
				break;
			}

//			out_mesh.mColors[i] = new Color4D[vertices.size()];
//			std::copy(colors.begin(),colors.end(),out_mesh.mColors[i]);
			out_mesh.mColors[i] = MemoryUtil.refCopy(colors, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		}

		if(!doc.settings().readMaterials || AssUtil.isEmpty(mindices)) {
			DefaultLogger.error("no material assigned to mesh, setting default material");
			out_mesh.mMaterialIndex = getDefaultMaterial();
		}
		else {
			convertMaterialForMesh(out_mesh,model,mesh,mindices.getInt(0));
		}

		if(doc.settings().readWeights && mesh.deformerSkin() != null) {
			convertWeights(out_mesh, model, mesh, node_global_transform, NO_MATERIAL_SEPARATION, null);
		}

		return (meshes.size() - 1);
	}
	
	// ------------------------------------------------------------------------------------------------
	IntArrayList convertMeshMultiMaterial(MeshGeometry mesh, Model model, Matrix4f node_global_transform)	
	{
		IntArrayList mindices = mesh.getMaterialIndices();
		if(AssUtil.isEmpty(mindices))
			return null;
	
//		std::set<MatIndexArray::value_type> had;
//		std::vector<unsigned int> indices;
		IntSet had = new IntOpenHashSet();
		IntArrayList indices = new IntArrayList();

//		BOOST_FOREACH(MatIndexArray::value_type index, mindices) {
		for (int i = 0; i < mindices.size(); i++){
			int index = mindices.getInt(i);
			if(/*had.find(index) == had.end()*/ !had.contains(index)) {
				indices.add(convertMeshMultiMaterial(mesh, model, index, node_global_transform));
				had.add(index);
			}
		}

		return indices;
	}


	// ------------------------------------------------------------------------------------------------
	int convertMeshMultiMaterial(MeshGeometry mesh, Model model, int index, Matrix4f node_global_transform)	
	{
		final Mesh out_mesh = setupEmptyMesh(mesh);

		IntArrayList mindices = mesh.getMaterialIndices();
		FloatBuffer vertices = mesh.getVertices();
		IntArrayList faces = mesh.getFaceIndexCounts();

		final boolean process_weights = doc.settings().readWeights && mesh.deformerSkin() != null;

		int count_faces = 0;
		int count_vertices = 0;

		// count faces
//		std::vector<unsigned int>::const_iterator itf = faces.begin();
//		for(MatIndexArray::const_iterator it = mindices.begin(), 
//			end = mindices.end(); it != end; ++it, ++itf)
		for(int i = 0; i < mindices.size(); i++)
		{	
			if (mindices.getInt(i) != index) {
				continue;
			}
			++count_faces;
			count_vertices += /**itf*/ faces.getInt(i);
		}

		assert(count_faces > 0);
		assert(count_vertices > 0);

		// mapping from output indices to DOM indexing, needed to resolve weights
		int[] reverseMapping = null;

		if (process_weights) {
			reverseMapping = new int[count_vertices];
//			reverseMapping.resize(count_vertices);
		}

		// allocate output data arrays, but don't fill them yet
		out_mesh.mNumVertices = count_vertices;
		out_mesh.mVertices = MemoryUtil.createFloatBuffer(count_vertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY)/*new Vector3D[count_vertices]*/;

//		out_mesh.mNumFaces = count_faces;
//		Face* fac = out_mesh.mFaces = new Face[count_faces]();
		out_mesh.mFaces = new Face[count_faces];

		// allocate normals
		FloatBuffer normals = mesh.getNormals();
		if(normals != null) {
			assert(normals.remaining() == vertices.remaining());
//			out_mesh.mNormals = new Vector3D[vertices.size()];
			out_mesh.mNormals = MemoryUtil.createFloatBuffer(count_vertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY);
		}

		// allocate tangents, binormals. 
		FloatBuffer tangents = mesh.getTangents();
		FloatBuffer binormals = mesh.getBinormals();

		if(tangents != null) {
			FloatBuffer tempBinormals;
			if (/*!binormals->size()*/ binormals == null) {
				if (normals/*.size()*/ != null) {
					// XXX this computes the binormals for the entire mesh, not only 
					// the part for which we need them.
//					tempBinormals.resize(normals.size());
					tempBinormals = MemoryUtil.createFloatBuffer(normals.remaining(), AssimpConfig.MESH_USE_NATIVE_MEMORY);
					int size =tangents.remaining()/3;
					final Vector3f l = new Vector3f();
					final Vector3f r = new Vector3f();
					for (int i = 0; i < size; ++i) {
//						tempBinormals[i] = normals[i] ^ tangents[i];
						l.load(normals);
						r.load(tangents);
						Vector3f.cross(l, r, r);
						r.store(tempBinormals);
					}
					
					normals.flip();
					tangents.flip();
					tempBinormals.flip();

					binormals = tempBinormals;
				}
				else {
					binormals = null;	
				}
			}

			if(binormals != null) {
				assert(tangents.remaining() == vertices.remaining() && binormals.remaining() == vertices.remaining());

				out_mesh.mTangents = MemoryUtil.createFloatBuffer(count_vertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY); //new Vector3D[vertices.size()];
				out_mesh.mBitangents = MemoryUtil.createFloatBuffer(count_vertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY); //new Vector3D[vertices.size()];
			}
		}

		// allocate texture coords
		int num_uvs = 0;
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i, ++num_uvs) {
			FloatBuffer uvs = mesh.getTextureCoords(i);
			if(AssUtil.isEmpty(uvs)) {
				break;
			}

			out_mesh.mTextureCoords[i] = MemoryUtil.createFloatBuffer(count_vertices * 3, AssimpConfig.MESH_USE_NATIVE_MEMORY); //new Vector3D[vertices.size()];
			out_mesh.mNumUVComponents[i] = 2;
		}

		// allocate vertex colors
		int num_vcs = 0;
		for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_COLOR_SETS; ++i, ++num_vcs) {
			FloatBuffer colors = mesh.getVertexColors(i);
			if(AssUtil.isEmpty(colors)) {
				break;
			}

			out_mesh.mColors[i] = MemoryUtil.createFloatBuffer(count_vertices * 4, AssimpConfig.MESH_USE_NATIVE_MEMORY); //new Color4D[vertices.size()];
		}

		int cursor = 0, in_cursor = 0;

//		itf = faces.begin();
//		for(MatIndexArray::const_iterator it = mindices.begin(), 
//			end = mindices.end(); it != end; ++it, ++itf) 
		for (int i = 0; i < mindices.size(); ++i)
		{	
//			unsigned int pcount = *itf;
			final int pcount = faces.getInt(i);
			if (mindices.getInt(i) != index) {
				in_cursor += pcount;
				continue;
			}

//			Face& f = *fac++;
			Face f = out_mesh.mFaces[i] = Face.createInstance(pcount);

//			f.mNumIndices = pcount;
//			f.mIndices = new unsigned int[pcount];
			switch(pcount) 
			{
			case 1:
				out_mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_POINT;
				break;
			case 2:
				out_mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_LINE;
				break;
			case 3:
				out_mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_TRIANGLE;
				break;
			default:
				out_mesh.mPrimitiveTypes |= Mesh.aiPrimitiveType_POLYGON;
				break;
			}
			for (int j = 0; j < pcount; ++j, ++cursor, ++in_cursor) {
//				f.mIndices[i] = cursor;
				f.set(j, cursor);

				if(reverseMapping != null) {
					reverseMapping[cursor] = in_cursor;
				}

//				out_mesh.mVertices[cursor] = vertices[in_cursor];
				MemoryUtil.arraycopy(vertices, in_cursor * 3, out_mesh.mVertices, cursor * 3, 3);

				if(out_mesh.mNormals != null) {
//					out_mesh.mNormals[cursor] = normals[in_cursor];
					MemoryUtil.arraycopy(normals, in_cursor * 3, out_mesh.mNormals, cursor * 3, 3);
				}

				if(out_mesh.mTangents != null) {
//					out_mesh.mTangents[cursor] = tangents[in_cursor];
//					out_mesh.mBitangents[cursor] = (*binormals)[in_cursor];
					MemoryUtil.arraycopy(tangents, in_cursor * 3, out_mesh.mTangents, cursor * 3, 3);
					MemoryUtil.arraycopy(binormals, in_cursor * 3, out_mesh.mBitangents, cursor * 3, 3);
				}

				for (int k = 0; k < num_uvs; ++k) {
					FloatBuffer uvs = mesh.getTextureCoords(k);
//					out_mesh.mTextureCoords[i][cursor] = Vector3D(uvs[in_cursor].x,uvs[in_cursor].y, 0.0f);
					MemoryUtil.arraycopy(uvs, in_cursor * 2, out_mesh.mTextureCoords[k], cursor * 3, 2);
				}

				for (int k = 0; k < num_vcs; ++k) {
					FloatBuffer cols = mesh.getVertexColors(k);
//					out_mesh.mColors[i][cursor] = cols[in_cursor];
					MemoryUtil.arraycopy(cols, in_cursor * 4, out_mesh.mColors[i], cursor * 4, 4);
				}
			}
		}
	
		convertMaterialForMesh(out_mesh,model,mesh,index);

		if(process_weights) {
			convertWeights(out_mesh, model, mesh, node_global_transform, index, reverseMapping);
		}

		return /*static_cast<unsigned int>*/(meshes.size() - 1);
	}

	// ------------------------------------------------------------------------------------------------
	/** - if materialIndex == NO_MATERIAL_SEPARATION, materials are not taken into
	 *  account when determining which weights to include. 
	 *  - outputVertStartIndices is only used when a material index is specified, it gives for
	 *    each output vertex the DOM index it maps to. */
	void convertWeights(Mesh out, Model model, MeshGeometry geo, Matrix4f node_global_transform /*= Matrix4x4()*/,
		int materialIndex /*= NO_MATERIAL_SEPARATION*/, 
		int[] outputVertStartIndices /*= NULL*/)
	{
		assert(geo.deformerSkin() != null);

		IntArrayList out_indices = new IntArrayList();
		IntArrayList index_out_indices = new IntArrayList();
		IntArrayList count_out_indices = new IntArrayList();

		Skin sk = geo.deformerSkin();

		ArrayList<Bone> bones = new ArrayList<>(AssUtil.size(sk.clusters()));
//		bones.reserve(sk.Clusters().size());

		final boolean no_mat_check = materialIndex == NO_MATERIAL_SEPARATION;
		assert(no_mat_check || outputVertStartIndices != null);

		try {

//			BOOST_FOREACH(Cluster* cluster, sk.Clusters()) {
			for(Cluster cluster : sk.clusters()){
//				_assert(cluster);

				IntArrayList indices = cluster.getIndices();

				if(AssUtil.isEmpty(indices)) {
					continue;
				}

				IntArrayList mats = geo.getMaterialIndices();

				boolean ok = false;		

//				int no_index_sentinel = std::numeric_limits<int>::max();
				final int no_index_sentinel = -1;

				count_out_indices.clear();
				index_out_indices.clear();
				out_indices.clear();

				// now check if *any* of these weights is contned in the output mesh,
				// taking notes so we don't need to do it twice.
//				BOOST_FOREACH(WeightIndexArray::value_type index, indices) {
				for (int j = 0; j < indices.size(); j++){
					int index = indices.getInt(j);

					long value = geo.toOutputVertexIndex(index/*, count*/);
					final int out_idx = AssUtil.decodeFirst(value);
					final int count   = AssUtil.decodeSecond(value);
					final int[] mapping = geo.outputData();

					index_out_indices.add(no_index_sentinel);
					count_out_indices.add(0);

					for(int i = 0; i < count; ++i) {					
						if (no_mat_check || mats.getInt(geo.faceForVertexIndex(mapping[out_idx + i]))
								/*(mats[geo.FaceForVertexIndex(out_idx[i])])*/ == materialIndex) {
							
							int back = index_out_indices.size() - 1;
							if (index_out_indices.topInt() == no_index_sentinel) {
								index_out_indices.set(back, out_indices.size());
							}

							if (no_mat_check) {
								out_indices.add(/*out_idx[i]*/mapping[out_idx + i]);
							}
							else {
								// this extra lookup is in O(logn), so the entire algorithm becomes O(nlogn)
								/*std::vector<unsigned int>::iterator it = std::lower_bound(
									outputVertStartIndices->begin(),
									outputVertStartIndices->end(),
									out_idx[i]
								);*/
								
								int it = AssUtil.lower_bound(outputVertStartIndices, 0, outputVertStartIndices.length, mapping[out_idx + i]);

//								out_indices.push_back(std::distance(outputVertStartIndices->begin(), it));
								out_indices.add(it);
							}

//							++count_out_indices.back();
							++count_out_indices.elements()[count_out_indices.size() - 1];
							ok = true;
						}
					}		
				}

				// if we found at least one, generate the output bones
				// XXX this could be heavily simplified by collecting the bone
				// data in a single step.
				if (ok) {
					convertCluster(bones, model, cluster, out_indices, index_out_indices, 
						count_out_indices, node_global_transform);
				}
			}
		}
		catch (Exception e) {
//			std::for_each(bones.begin(),bones.end(),Util::delete_fun<Bone>());
			e.printStackTrace();;
		}

//		if(bones.isEmpty()) {
//			return;
//		}

//		out.mBones = new Bone[bones.size()];
//		out.mNumBones = static_cast<unsigned int>(bones.size());

//		std::swap_ranges(bones.begin(),bones.end(),out->mBones);
		out.mBones = AssUtil.toArray(bones, Bone.class);
	}
	
	// ------------------------------------------------------------------------------------------------
	void convertCluster(ArrayList<Bone> bones, Model model, Cluster cl, 		
		IntArrayList out_indices, IntArrayList index_out_indices, IntArrayList count_out_indices,
		Matrix4f node_global_transform)
	{

		Bone bone = new Bone();
		bones.add(bone);

		bone.mName = fixNodeName(cl.targetNode().name());

//		bone.mOffsetMatrix = cl.transformLink();
//		bone.mOffsetMatrix.inverse();
		Matrix4f.invert(cl.transformLink(), bone.mOffsetMatrix);

//		bone.mOffsetMatrix = bone.mOffsetMatrix * node_global_transform;
		Matrix4f.mul(bone.mOffsetMatrix, node_global_transform, bone.mOffsetMatrix);

//		bone.mNumWeights = static_cast<unsigned int>(out_indices.size());
		/*VertexWeight* cursor =*/ bone.mWeights = new VertexWeight[out_indices.size()];
		AssUtil.initArray(bone.mWeights);
		int cursor = 0;
		final int  no_index_sentinel = -1; //std::numeric_limits<int>::max();
		final FloatArrayList weights = cl.getWeights();

		final int c = index_out_indices.size();
		for (int i = 0; i < c; ++i) {
			final int index_index =  index_out_indices.getInt(i);

			if (index_index == no_index_sentinel) {
				continue;
			}

			final int cc = count_out_indices.getInt(i);
			for (int j = 0; j < cc; ++j) {
				VertexWeight out_weight = bone.mWeights[cursor++];

				out_weight.mVertexId = (out_indices.getInt(index_index + j));
				out_weight.mWeight = weights.getFloat(i);
			}			
		}
	}

	// ------------------------------------------------------------------------------------------------
	void convertMaterialForMesh(Mesh out, Model model, MeshGeometry geo, int materialIndex)
	{
		// locate source materials for this mesh
		ArrayList<FBXMaterial> mats = model.getMaterials();
		if (materialIndex >= mats.size() || materialIndex < 0) {
			DefaultLogger.error("material index out of bounds, setting default material");
			out.mMaterialIndex = getDefaultMaterial();
			return;
		}

		final FBXMaterial mat = mats.get(materialIndex);
//		MaterialMap::const_iterator it = materials_converted.find(mat);
//		if (it != materials_converted.end()) {
//			out->mMaterialIndex = (*it).second;
//			return;
//		}
		
		if(materials_converted.containsKey(mat)){
			out.mMaterialIndex = materials_converted.getInt(mat);
			return;
		}

		out.mMaterialIndex = convertMaterial(mat, geo);	
//		materials_converted[mat] = out->mMaterialIndex;
		materials_converted.put(mat, out.mMaterialIndex);
	}


	// ------------------------------------------------------------------------------------------------
	int getDefaultMaterial()
	{
		if (defaultMaterialIndex != 0) {
			return defaultMaterialIndex - 1; 
		}

		Material out_mat = new Material();
		materials.add(out_mat);

		Vector3f diffuse = new Vector3f(0.8f,0.8f,0.8f);
		out_mat.addProperty(diffuse,Material.AI_MATKEY_COLOR_DIFFUSE, 0,0);

//		String s;
//		s.Set(AI_DEFAULT_MATERIAL_NAME);

		out_mat.addProperty(Material.AI_DEFAULT_MATERIAL_NAME,Material.AI_MATKEY_NAME,0,0);

		defaultMaterialIndex = materials.size();
		return defaultMaterialIndex - 1;
	}


	// ------------------------------------------------------------------------------------------------
	// Material -> Material
	int convertMaterial(FBXMaterial material, MeshGeometry mesh)
	{
		PropertyTable props = material.props();

		// generate empty output material
		Material out_mat = new Material();
//		materials_converted[&material] = static_cast<unsigned int>(materials.size());
		materials_converted.put(material, materials.size());

		materials.add(out_mat);

		// stip Material:: prefix
		String name = material.name();
//		if(name.substr(0,10) == "Material::") {
//			name = name.substr(10);
//		}
		
		if(name.startsWith("Material::")){
			name = name.substring(10);
		}

		// set material name if not empty - this could happen
		// and there should be no key for it in this case.
		if(name.length() > 0) {
			out_mat.addProperty(name,Material.AI_MATKEY_NAME,0,0);
		}

		// shading stuff and colors
		setShadingPropertiesCommon(out_mat,props);
	
		// texture assignments
		setTextureProperties(out_mat,material.textures(), mesh);
		setTextureProperties2(out_mat,material.layeredTextures(), mesh);

		return /*static_cast<unsigned int>*/(materials.size() - 1);
	}
	
	// ------------------------------------------------------------------------------------------------
	void trySetTextureProperties(Material out_mat, Map<String, FBXTexture> textures, String propName, int target, MeshGeometry mesh)
	{
//		TextureMap::const_iterator it = textures.find(propName);
//		if(it == textures.end()) {
//			return;
//		}

//		Texture* tex = (*it).second;
		final FBXTexture tex = textures.get(propName);
		if(tex !=null )
		{
//			String path;
//			path.Set(tex->RelativeFilename());

			out_mat.addProperty(/*&path*/tex.relativeFilename(),Material._AI_MATKEY_TEXTURE_BASE,target,0);

			UVTransform uvTrafo = new UVTransform();
			// XXX handle all kinds of UV transformations
			uvTrafo.mScaling.set(tex.uvScaling());
			uvTrafo.mTranslation.set(tex.uvTranslation());
			out_mat.addProperty(uvTrafo,Material._AI_MATKEY_UVTRANSFORM_BASE,target,0);

			PropertyTable props = tex.props();

			int uvIndex = 0;

			ObjectHolder<Boolean> ok = new ObjectHolder<Boolean>();
			String uvSet = PropertyTable.propertyGet(props,"UVSet",ok);
			if(ok.get()) {
				// "default" is the name which usually appears in the FbxFileTexture template
				if(!uvSet.equals("default") && uvSet.length() > 0) {
					// this is a bit awkward - we need to find a mesh that uses this
					// material and scan its UV channels for the given UV name because
					// assimp references UV channels by index, not by name.

					// XXX: the case that UV channels may appear in different orders
					// in meshes is unhandled. A possible solution would be to sort
					// the UV channels alphabetically, but this would have the side
					// effect that the primary (first) UV channel would sometimes
					// be moved, causing trouble when users read only the first
					// UV channel and ignore UV channel assignments altogether.

//					final int matIndex = static_cast<unsigned int>(std::distance(materials.begin(), 
//						std::find(materials.begin(),materials.end(),out_mat)
//					));
					final int matIndex = materials.indexOf(out_mat);


          uvIndex = -1;
          if (mesh == null)
          {					
//					  BOOST_FOREACH(MeshMap::value_type& v,meshes_converted) {
        	  		  for(Map.Entry<Geometry, IntArrayList> v : meshes_converted.entrySet()){
//						  MeshGeometry* mesh = dynamic_cast<MeshGeometry*> (v.first);
//						  if(!mesh) {
//							  continue;
//						  }
        	  			  
        	  			  MeshGeometry _mesh = null;
        	  			  if(v.getKey() instanceof MeshGeometry){
        	  				  _mesh = (MeshGeometry)v.getKey();
        	  			  }
        	  			  
        	  			  if(_mesh == null)
        	  				  continue;

						  IntArrayList mats = _mesh.getMaterialIndices();
//						  if(std::find(mats.begin(),mats.end(),matIndex) == mats.end()) {
//							  continue;
//						  }
						  if(!mats.contains(matIndex))
							  continue;

						  int index = -1;
						  for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i) {
							  if(_mesh.getTextureCoords(i) == null) {
								  break;
							  }
							  String name = _mesh.getTextureCoordChannelName(i);
							  if(name.equals(uvSet)) {
								  index = i;
								  break;
							  }
						  }
						  if(index == -1) {
							  if(DefaultLogger.LOG_OUT)
								  DefaultLogger.warn("did not find UV channel named " + uvSet + " in a mesh using this material");
							  continue;
						  }

						  if(uvIndex == -1) {
							  uvIndex = index;
						  }
						  else {
							  if(DefaultLogger.LOG_OUT)
								  DefaultLogger.warn("the UV channel named " + uvSet + " appears at different positions in meshes, results will be wrong");
						  }
					  }
          }
          else
          {
						int index = -1;
						for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i) {
							if(mesh.getTextureCoords(i) == null) {
								break;
							}
							String name = mesh.getTextureCoordChannelName(i);
							if(name.equals(uvSet)) {
								index = (i);
								break;
							}
						}
						if(index == -1) {
							if(DefaultLogger.LOG_OUT)
								  DefaultLogger.warn("did not find UV channel named " + uvSet + " in a mesh using this material");
						}

						if(uvIndex == -1) {
							uvIndex = index;
						}
          }

					if(uvIndex == -1) {
						if(DefaultLogger.LOG_OUT)
							  DefaultLogger.warn("fled to resolve UV channel " + uvSet + ", using first UV channel");
						uvIndex = 0;
					}
				}
			}

			out_mat.addProperty(uvIndex,Material._AI_MATKEY_UVWSRC_BASE,target,0);
		}
	}

	// ------------------------------------------------------------------------------------------------
	void trySetTextureProperties2(Material out_mat, Map<String, LayeredTexture> layeredTextures, String propName, 
		int target, MeshGeometry mesh)
	{
//		LayeredTextureMap::const_iterator it = layeredTextures.find(propName);
//		if(it == layeredTextures.end()) {
//			return;
//		}
//
//		Texture* tex = (*it).second->getTexture();
		FBXTexture tex = null;
		LayeredTexture lt;
		if((lt = layeredTextures.get(propName)) != null){
			tex = lt.getTexture();
		}else{
			return;
		}
		

//		String path;
//		path.Set(tex->RelativeFilename());

		out_mat.addProperty(/*&path*/tex.relativeFilename(), Material._AI_MATKEY_TEXTURE_BASE,target,0);

		UVTransform uvTrafo = new UVTransform();
		// XXX handle all kinds of UV transformations
		uvTrafo.mScaling.set(tex.uvScaling());
		uvTrafo.mTranslation.set(tex.uvTranslation());
		out_mat.addProperty(uvTrafo,Material._AI_MATKEY_UVTRANSFORM_BASE,target,0);

		PropertyTable props = tex.props();

		int uvIndex = 0;

		ObjectHolder<Boolean> ok = new ObjectHolder<Boolean>();
		String uvSet = PropertyTable.propertyGet(props,"UVSet",ok);
		if(ok.get()) {
			// "default" is the name which usually appears in the FbxFileTexture template
			if(!uvSet.equals("default") && uvSet.length() > 0) {
				// this is a bit awkward - we need to find a mesh that uses this
				// material and scan its UV channels for the given UV name because
				// assimp references UV channels by index, not by name.

				// XXX: the case that UV channels may appear in different orders
				// in meshes is unhandled. A possible solution would be to sort
				// the UV channels alphabetically, but this would have the side
				// effect that the primary (first) UV channel would sometimes
				// be moved, causing trouble when users read only the first
				// UV channel and ignore UV channel assignments altogether.

//				unsigned int matIndex = static_cast<unsigned int>(std::distance(materials.begin(), 
//					std::find(materials.begin(),materials.end(),out_mat)
//					));
				final int matIndex = materials.indexOf(out_mat);
			  uvIndex = -1;
        if (mesh == null)
        {					
//					BOOST_FOREACH(MeshMap::value_type& v,meshes_converted) {
        			for(Geometry geo : meshes_converted.keySet()){
//						MeshGeometry* mesh = dynamic_cast<MeshGeometry*> (v.first);
//						if(!mesh) {
//							continue;
//						}
        				if(geo  == null) continue;
        				mesh = (MeshGeometry)geo;
						IntArrayList mats = mesh.getMaterialIndices();
//						if(std::find(mats.begin(),mats.end(),matIndex) == mats.end()) {
//							continue;
//						}
						if(!mats.contains(matIndex))continue;

						int index = -1;
						for (int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i) {
							if(mesh.getTextureCoords(i) == null) {
								break;
							}
							String name = mesh.getTextureCoordChannelName(i);
							if(name.equals(uvSet)) {
								index = /*static_cast<int>*/(i);
								break;
							}
						}
						if(index == -1) {
							if(DefaultLogger.LOG_OUT)
								  DefaultLogger.warn("did not find UV channel named " + uvSet + " in a mesh using this material");
							continue;
						}

						if(uvIndex == -1) {
							uvIndex = index;
						}
						else {
							if(DefaultLogger.LOG_OUT)
								 DefaultLogger.warn("the UV channel named " + uvSet + " appears at different positions in meshes, results will be wrong");
						}
					}
        }
        else
        {
					int index = -1;
					for ( int i = 0; i < Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS; ++i) {
						if(mesh.getTextureCoords(i) == null) {
							break;
						}
						String name = mesh.getTextureCoordChannelName(i);
						if(name.equals(uvSet)) {
							index = /*static_cast<int>*/(i);
							break;
						}
					}
					if(index == -1) {
						if(DefaultLogger.LOG_OUT)
							  DefaultLogger.warn("did not find UV channel named " + uvSet + " in a mesh using this material");
					}

					if(uvIndex == -1) {
						uvIndex = index;
					}
        }

				if(uvIndex == -1) {
					if(DefaultLogger.LOG_OUT)
						  DefaultLogger.warn("fled to resolve UV channel " + uvSet + ", using first UV channel");
					uvIndex = 0;
				}
			}
		}

		out_mat.addProperty(uvIndex,Material._AI_MATKEY_UVWSRC_BASE,target,0);
	}

	// ------------------------------------------------------------------------------------------------
	void setTextureProperties(Material out_mat, Map<String, FBXTexture> textures, MeshGeometry mesh)
	{
		trySetTextureProperties(out_mat, textures, "DiffuseColor", TextureType.aiTextureType_DIFFUSE.ordinal(), mesh);
		trySetTextureProperties(out_mat, textures, "AmbientColor", TextureType.aiTextureType_AMBIENT.ordinal(), mesh);
		trySetTextureProperties(out_mat, textures, "EmissiveColor", TextureType.aiTextureType_EMISSIVE.ordinal(), mesh);
		trySetTextureProperties(out_mat, textures, "SpecularColor", TextureType.aiTextureType_SPECULAR.ordinal(), mesh);
		trySetTextureProperties(out_mat, textures, "TransparentColor", TextureType.aiTextureType_OPACITY.ordinal(), mesh);
		trySetTextureProperties(out_mat, textures, "ReflectionColor", TextureType.aiTextureType_REFLECTION.ordinal(), mesh);
		trySetTextureProperties(out_mat, textures, "DisplacementColor", TextureType.aiTextureType_DISPLACEMENT.ordinal(), mesh);
		trySetTextureProperties(out_mat, textures, "NormalMap", TextureType.aiTextureType_NORMALS.ordinal(), mesh);
		trySetTextureProperties(out_mat, textures, "Bump", TextureType.aiTextureType_HEIGHT.ordinal(), mesh);
		trySetTextureProperties(out_mat, textures, "ShininessExponent", TextureType.aiTextureType_SHININESS.ordinal(), mesh);
	}

	// ------------------------------------------------------------------------------------------------
	void setTextureProperties2(Material out_mat, Map<String, LayeredTexture> layeredTextures, MeshGeometry mesh)
	{
		trySetTextureProperties2(out_mat, layeredTextures, "DiffuseColor", TextureType.aiTextureType_DIFFUSE.ordinal(), mesh);
		trySetTextureProperties2(out_mat, layeredTextures, "AmbientColor", TextureType.aiTextureType_AMBIENT.ordinal(), mesh);
		trySetTextureProperties2(out_mat, layeredTextures, "EmissiveColor", TextureType.aiTextureType_EMISSIVE.ordinal(), mesh);
		trySetTextureProperties2(out_mat, layeredTextures, "SpecularColor", TextureType.aiTextureType_SPECULAR.ordinal(), mesh);
		trySetTextureProperties2(out_mat, layeredTextures, "TransparentColor", TextureType.aiTextureType_OPACITY.ordinal(), mesh);
		trySetTextureProperties2(out_mat, layeredTextures, "ReflectionColor", TextureType.aiTextureType_REFLECTION.ordinal(), mesh);
		trySetTextureProperties2(out_mat, layeredTextures, "DisplacementColor", TextureType.aiTextureType_DISPLACEMENT.ordinal(), mesh);
		trySetTextureProperties2(out_mat, layeredTextures, "NormalMap", TextureType.aiTextureType_NORMALS.ordinal(), mesh);
		trySetTextureProperties2(out_mat, layeredTextures, "Bump", TextureType.aiTextureType_HEIGHT.ordinal(), mesh);
		trySetTextureProperties2(out_mat, layeredTextures, "ShininessExponent", TextureType.aiTextureType_SHININESS.ordinal(), mesh);
	}
	
	// ------------------------------------------------------------------------------------------------
	Vector3f getColorPropertyFromMaterial(PropertyTable props, String baseName, ObjectHolder<Boolean> result)
	{
		Vector3f diffuse = PropertyTable.propertyGet(props,baseName,result);
		if(result.get()) {
			return /*Color3D(Diffuse.x,Diffuse.y,Diffuse.z)*/ diffuse;
		}
		else {
			Vector3f diffuseColor = PropertyTable.propertyGet(props,baseName + "Color", result);
			if(result.get()) {
				float diffuseFactor = PropertyTable.propertyGet(props,baseName + "Factor",result);
				if(result.get()) {
//					DiffuseColor *= DiffuseFactor;
					diffuseColor.scale(diffuseFactor);
				}

				return diffuseColor/*Color3D(diffuseColor.x,diffuseColor.y,diffuseColor.z)*/;
			}
		}
		result.set(false);
		return new Vector3f(0.0f,0.0f,0.0f);
	}

	// ------------------------------------------------------------------------------------------------
	void setShadingPropertiesCommon(Material out_mat, PropertyTable props)
	{
		// set shading properties. There are various, redundant ways in which FBX materials
		// specify their shading settings (depending on shading models, prop
		// template etc.). No idea which one is right in a particular context. 
		// Just try to make sense of it - there's no spec to verify this agnst, 
		// so why should we.
		final ObjectHolder<Boolean> ok = new ObjectHolder<Boolean>();
		final Vector3f Diffuse = getColorPropertyFromMaterial(props,"Diffuse",ok);
		if(ok.get()) {
			out_mat.addProperty(Diffuse, Material.AI_MATKEY_COLOR_DIFFUSE, 0, 0);
		}

		final Vector3f Emissive = getColorPropertyFromMaterial(props,"Emissive",ok);
		if(ok.get()) {
			out_mat.addProperty(Emissive,Material.AI_MATKEY_COLOR_EMISSIVE, 0, 0);
		}

		final Vector3f Ambient = getColorPropertyFromMaterial(props,"Ambient",ok);
		if(ok.get()) {
			out_mat.addProperty(Ambient,Material.AI_MATKEY_COLOR_AMBIENT, 0, 0);
		}

		final Vector3f Specular = getColorPropertyFromMaterial(props,"Specular",ok);
		if(ok.get()) {
			out_mat.addProperty(Specular,Material.AI_MATKEY_COLOR_SPECULAR, 0, 0);
		}

		float Opacity = PropertyTable.propertyGet(props,"Opacity",ok);
		if(ok.get()) {
			out_mat.addProperty(Opacity,Material.AI_MATKEY_OPACITY, 0, 0);
		}

		float Reflectivity = PropertyTable.propertyGet(props,"Reflectivity",ok);
		if(ok.get()) {
			out_mat.addProperty(Reflectivity,Material.AI_MATKEY_REFLECTIVITY, 0, 0);
		}

		float Shininess = PropertyTable.propertyGet(props,"Shininess",ok);
		if(ok.get()) {
			out_mat.addProperty(Shininess,Material.AI_MATKEY_SHININESS_STRENGTH, 0, 0);
		}

		float ShininessExponent = PropertyTable.propertyGet(props,"ShininessExponent",ok);
		if(ok.get()) {
			out_mat.addProperty(ShininessExponent,Material.AI_MATKEY_SHININESS, 0, 0);
		}
	}


	// ------------------------------------------------------------------------------------------------
	// get the number of fps for a FrameRate enumerated value
	static double frameRateToDouble(int fp, double customFPSVal /*= -1.0*/)
	{
		switch(fp) {
			case FileGlobalSettings.FrameRate_DEFAULT:
				return 1.0;

			case FileGlobalSettings.FrameRate_120:
				return 120.0;

			case FileGlobalSettings.FrameRate_100:
				return 100.0;

			case FileGlobalSettings.FrameRate_60:
				return 60.0;

			case FileGlobalSettings.FrameRate_50:
				return 50.0;

			case FileGlobalSettings.FrameRate_48:
				return 48.0;

			case FileGlobalSettings.FrameRate_30:
			case FileGlobalSettings.FrameRate_30_DROP:
				return 30.0;

			case FileGlobalSettings.FrameRate_NTSC_DROP_FRAME:
			case FileGlobalSettings.FrameRate_NTSC_FULL_FRAME:
				return 29.9700262;

			case FileGlobalSettings.FrameRate_PAL:
				return 25.0;

			case FileGlobalSettings.FrameRate_CINEMA:
				return 24.0;

			case FileGlobalSettings.FrameRate_1000:
				return 1000.0;

			case FileGlobalSettings.FrameRate_CINEMA_ND:
				return 23.976;

			case FileGlobalSettings.FrameRate_CUSTOM:
				return customFPSVal;

			case FileGlobalSettings.FrameRate_MAX: // this is to silence compiler warnings
				break;
		}

		assert(false);
		return -1.0f;
	}


	// ------------------------------------------------------------------------------------------------
	// convert animation data to Animation et al
	void convertAnimations() 
	{
		// first of all determine framerate
		final int fps = doc.globalSettings().timeMode();
		final float custom = doc.globalSettings().customFrameRate();
		anim_fps = frameRateToDouble(fps, custom);

//		std::vector<AnimationStack*>& animations = doc.AnimationStacks();
//		BOOST_FOREACH(AnimationStack* stack, animations) {
//			ConvertAnimationStack(*stack);
//		}
		List<AnimationStack> animations = doc.animationStacks();
		if(animations != null)
			for(AnimationStack stack : doc.animationStacks()){
				convertAnimationStack(stack);
			}
	}
	
	// ------------------------------------------------------------------------------------------------
	// rename a node already partially converted. fixed_name is a string previously returned by 
	// FixNodeName, new_name specifies the string FixNodeName should return on all further invocations 
	// which would previously have returned the old value.
	//
	// this also updates names in node animations, cameras and light sources and is thus slow.
	//
	// NOTE: the caller is responsible for ensuring that the new name is unique and does
	// not collide with any other identifiers. The best way to ensure this is to only
	// append to the old name, which is guaranteed to match these requirements.
	void renameNode(String fixed_name, String new_name)
	{
		assert(node_names.containsKey(fixed_name));
		assert(node_names.containsKey(new_name));

//		renamed_nodes[fixed_name] = new_name;
		renamed_nodes.put(fixed_name, new_name);

//		String fn(fixed_name);

//		BOOST_FOREACH(Camera* cam, cameras) {
		for(Camera cam : cameras){
			if (cam.mName.equals(fixed_name)) {
				cam.mName = (new_name);
				break;
			}
		}

//		BOOST_FOREACH(Light* light, lights) {
		for(Light light : lights) {
			if (light.mName.equals(fixed_name)) {
				light.mName = (new_name);
				break;
			}
		}

//		BOOST_FOREACH(Animation* anim, animations) {
		for(Animation anim : animations){
			for (int i = 0; i < anim.getNumChannels(); ++i) {
				NodeAnim na = anim.mChannels[i];
				if (na.mNodeName.equals(fixed_name)) {
					na.mNodeName = (new_name);
					break;
				}
			}
		}
	}


	// ------------------------------------------------------------------------------------------------
	// takes a fbx node name and returns the identifier to be used in the assimp output scene.
	// the function is guaranteed to provide consistent results over multiple invocations
	// UNLESS RenameNode() is called for a particular node name.
	String fixNodeName(String name)
	{
		// strip Model:: prefix, avoiding ambiguities (i.e. don't strip if 
		// this causes ambiguities, well possible between empty identifiers,
		// such as "Model::" and ""). Make sure the behaviour is consistent
		// across multiple calls to FixNodeName().
		if(name.startsWith("Model::")/*name.substr(0,7) == "Model::"*/) {
			String temp = name.substring(7);

//			NodeNameMap::const_iterator it = node_names.find(temp);
//			if (it != node_names.end()) {
//				if (!(*it).second) {
//					return FixNodeName(name + "_");
//				}
//			}
			Boolean it = node_names.get(temp);
			if(it != null){
				if(!it)
					return fixNodeName(name + "_");
			}
//			node_names[temp] = true;
			node_names.put(temp, true);

//			NameNameMap::const_iterator rit = renamed_nodes.find(temp);
//			return rit == renamed_nodes.end() ? temp : (*rit).second;
			String rit = renamed_nodes.get(temp);
			return rit == null ? temp : rit;
		}

//		NodeNameMap::const_iterator it = node_names.find(name);
//		if (it != node_names.end()) {
//			if ((*it).second) {
//				return FixNodeName(name + "_");
//			}
//		}
		Boolean it = node_names.get(name);
		if(it != null){
			if(it)
				return fixNodeName(name + "_");
		}
//		node_names[name] = false;
		node_names.put(name, false);

//		NameNameMap::const_iterator rit = renamed_nodes.find(name);
//		return rit == renamed_nodes.end() ? name : (*rit).second;
		String rit = renamed_nodes.get(name);
		return rit == null ? name : rit;
	}


//	typedef std::map<AnimationCurveNode*, AnimationLayer*> LayerMap;

	// XXX: better use multi_map ..
//	typedef std::map<std::string, std::vector<AnimationCurveNode*> > NodeMap;


	// ------------------------------------------------------------------------------------------------
	void convertAnimationStack(AnimationStack st)
	{				
		ArrayList<AnimationLayer> layers = st.layers();
		if(AssUtil.isEmpty(layers)) {
			return;
		}

		Animation anim = new Animation();
		animations.add(anim);

		// strip AnimationStack:: prefix
		String name = st.name();
//		if(name.substr(0,16) == "AnimationStack::") {
//			name = name.substr(16);
//		}
		
		if(name.startsWith("AnimationStack::")){
			name = name.substring(16);
		}

		anim.mName = (name);
		
		// need to find all nodes for which we need to generate node animations -
		// it may happen that we need to merge multiple layers, though.
		Map<String, List<AnimationCurveNode>> node_map = new HashMap<>();

		// reverse mapping from curves to layers, much faster than querying 
		// the FBX DOM for it.
		Map<AnimationCurveNode, AnimationLayer> layer_map = new HashMap<AnimationCurveNode, AnimationLayer>();

//		final String prop_whitelist[] = {
//			"Lcl Scaling",
//			"Lcl Rotation",
//			"Lcl Translation"
//		};
		//TODO
		String prop_whitelist = "Lcl Scaling,Lcl Rotation,Lcl Translation";
//		BOOST_FOREACH(AnimationLayer* layer, layers) {
		for(AnimationLayer layer: layers){
			assert(layer != null);

			List<AnimationCurveNode> nodes = layer.nodes(prop_whitelist);
//			BOOST_FOREACH(AnimationCurveNode* node, nodes) {
			for(AnimationCurveNode node : nodes){
				assert(node != null);

//				Model model = dynamic_cast<Model*>(node->Target());
//				// this can happen - it could also be a NodeAttribute (i.e. for camera animations)
//				if(!model) {
//					continue;
//				}
				
				Model model = null;
				if(node.target() instanceof Model){
					model = (Model)node.target();
				}else{
					continue;
				}

				String _name = fixNodeName(model.name());
//				node_map[name].push_back(node);
				List<AnimationCurveNode> list = node_map.get(_name);
				if(list == null)
					node_map.put(_name, list = new ArrayList<AnimationCurveNode>());
				list.add(node);

//				layer_map[node] = layer;
				layer_map.put(node, layer);
			}
		}

		// generate node animations
		ArrayList<NodeAnim> node_anims = new ArrayList<>();

		min_time = 1e10;
		max_time = -1e10;

//		try {
//			BOOST_FOREACH(NodeMap::value_type& kv, node_map) {
			for(Map.Entry<String, List<AnimationCurveNode>> kv : node_map.entrySet()){
				generateNodeAnimations(node_anims,  kv.getKey(), kv.getValue(), layer_map);
			}
//		}
//		catch(std::exception&) {
//			std::for_each(node_anims.begin(), node_anims.end(), Util::delete_fun<NodeAnim>());
//			throw;
//		}

		if(node_anims.size() > 0) {
//			anim->mChannels = new NodeAnim*[node_anims.size()]();
//			anim->mNumChannels = static_cast<unsigned int>(node_anims.size());
//
//			std::swap_ranges(node_anims.begin(),node_anims.end(),anim->mChannels);
			anim.mChannels = AssUtil.toArray(node_anims, NodeAnim.class);
		}
		else {
			// empty animations would fl validation, so drop them
//			delete anim;
//			animations.pop_back();
			animations.remove(animations.size() - 1);
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.info("ignoring empty AnimationStack (using IK?): " + name);
			return;
		}

		// for some mysterious reason, mDuration is simply the maximum key -- the
		// validator always assumes animations to start at zero.
		anim.mDuration = max_time /*- min_time */;
		anim.mTicksPerSecond = anim_fps;
	}
	
	// ------------------------------------------------------------------------------------------------
	void generateNodeAnimations(List<NodeAnim> node_anims, String fixed_name, List<AnimationCurveNode> curves, Map<AnimationCurveNode, AnimationLayer> layer_map)
	{

//		NodeMap node_property_map;
		Map<String, List<AnimationCurveNode>> node_property_map = new HashMap<>();
		assert(!AssUtil.isEmpty(curves));

		// sanity check whether the input is ok
		if(AssimpConfig.ASSIMP_BUILD_DEBUG){
			FBXObject target = null;
//			BOOST_FOREACH(AnimationCurveNode* node, curves) {
			for(AnimationCurveNode node : curves){
				if(target == null) {
					target = node.target();
				}
				assert(node.target() == target);
			}
		}

		AnimationCurveNode curve_node = null;
//		BOOST_FOREACH(AnimationCurveNode* node, curves) {
		for(AnimationCurveNode node:curves){
			assert(node != null);

			if (AssUtil.isEmpty(node.targetProperty())) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("target property for animation curve not set: " + node.name());
				continue;
			}

			curve_node = node;
			if (AssUtil.isEmpty(node.curves())) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("no animation curves assigned to AnimationCurveNode: " + node.name());
				continue;
			}

//			node_property_map[node->TargetProperty()].push_back(node);
			FBXUtil.put(node_property_map, node.targetProperty(), node);
		}

		assert(curve_node != null);
		assert(curve_node.targetAsModel() != null);

		Model target = curve_node.targetAsModel();

		// check for all possible transformation components
//		NodeMap::const_iterator chn[TransformationComp_MAXIMUM];
		@SuppressWarnings("unchecked")
		List<AnimationCurveNode>[] chn = new List[TransformationComp_MAXIMUM];

		boolean has_any = false;
		boolean has_complex = false;

		for (int i = 0; i < TransformationComp_MAXIMUM; ++i) {
			final int comp = /*static_cast<TransformationComp>*/(i);

			// inverse pivots don't exist in the input, we just generate them
			if (comp == TransformationComp_RotationPivotInverse || comp == TransformationComp_ScalingPivotInverse) {
				chn[i] = /*node_property_map.end()*/ null;
				continue;
			}

			chn[i] = node_property_map.get(nameTransformationCompProperty(comp));
			if (chn[i] != null/*node_property_map.end()*/) {

				// check if this curves contns redundant information by looking
				// up the corresponding node's transformation chn.
				if (doc.settings().optimizeEmptyAnimationCurves && 
					isRedundantAnimationData(target, comp, (chn[i])/*.second*/)) {

					if(DefaultLogger.LOG_OUT)
						DefaultLogger.debug("dropping redundant animation channel for node " + target.name());
					continue;
				}

				has_any = true;

				if (comp != TransformationComp_Rotation && comp != TransformationComp_Scaling && comp != TransformationComp_Translation &&
					comp != TransformationComp_GeometricScaling && comp != TransformationComp_GeometricRotation && comp != TransformationComp_GeometricTranslation )
				{
					has_complex = true;
				}
			}
		}

		if (!has_any) {
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("ignoring node animation, did not find any transformation key frames");
			return ;
		}

		// this needs to play nicely with GenerateTransformationNodeChn() which will
		// be invoked _later_ (animations come first). If this node has only rotation,
		// scaling and translation _and_ there are no animated other components either,
		// we can use a single node and also a single node animation channel.
		if (!has_complex && !needsComplexTransformationChn(target)) {
			NodeAnim nd = generateSimpleNodeAnim(fixed_name, target, chn, layer_map,true // input is TRS order, assimp is SRT
				);

			assert(nd != null);
			node_anims.add(nd);
			return ;
		}

		// otherwise, things get gruesome and we need separate animation channels
		// for each part of the transformation chn. Remember which channels
		// we generated and pass this information to the node conversion
		// code to avoid nodes that have identity transform, but non-identity
		// animations, being dropped.
		int flags = 0, bit = 0x1;
		for (int i = 0; i < TransformationComp_MAXIMUM; ++i, bit <<= 1) {
//			TransformationComp comp = static_cast<TransformationComp>(i);
			final int comp = i;

			if (chn[i] != null/*node_property_map.end()*/) {
				flags |= bit;

				assert(comp != TransformationComp_RotationPivotInverse);
				assert(comp != TransformationComp_ScalingPivotInverse);

				String chn_name = nameTransformationChnNode(fixed_name, comp);

				NodeAnim na = null;
				switch(comp) 
				{
				case TransformationComp_Rotation:
				case TransformationComp_PreRotation:
				case TransformationComp_PostRotation:
				case TransformationComp_GeometricRotation:
					na = generateRotationNodeAnim(chn_name, 
						target, 
						/*(*chn[i]).second*/chn[i],
						layer_map);

					break;

				case TransformationComp_RotationOffset:
				case TransformationComp_RotationPivot:
				case TransformationComp_ScalingOffset:
				case TransformationComp_ScalingPivot:
				case TransformationComp_Translation:
				case TransformationComp_GeometricTranslation:
					na = generateTranslationNodeAnim(chn_name, 
						target, 
						/*(*chn[i]).second*/chn[i],
						layer_map,false);

					// pivoting requires us to generate an implicit inverse channel to undo the pivot translation
					if (comp == TransformationComp_RotationPivot) {
						String invName = nameTransformationChnNode(fixed_name, 
							TransformationComp_RotationPivotInverse);

						NodeAnim inv = generateTranslationNodeAnim(invName, 
							target, 
							/*(*chn[i]).second*/chn[i],
							layer_map,
							true);

						assert(inv != null);
						node_anims.add(inv);

						assert(TransformationComp_RotationPivotInverse > i);
						flags |= bit << (TransformationComp_RotationPivotInverse - i);
					}
					else if (comp == TransformationComp_ScalingPivot) {
						String invName = nameTransformationChnNode(fixed_name, 
							TransformationComp_ScalingPivotInverse);

						NodeAnim inv = generateTranslationNodeAnim(invName, 
							target, 
							/*(*chn[i]).second*/chn[i],
							layer_map,
							true);

						assert(inv != null);
						node_anims.add(inv);
					
						assert(TransformationComp_RotationPivotInverse > i);
						flags |= bit << (TransformationComp_RotationPivotInverse - i);
					}

					break;

				case TransformationComp_Scaling:
				case TransformationComp_GeometricScaling:
					na = generateScalingNodeAnim(chn_name, 
						target, 
						/*(*chn[i]).second*/chn[i],
						layer_map);

					break;

				default:
					assert(false);
				}

				assert(na != null);
				node_anims.add(na);
				continue;
			}
		}

//		node_anim_chn_bits[fixed_name] = flags;
		node_anim_chn_bits.put(fixed_name, flags);
		
		return;
	}
	
	// ------------------------------------------------------------------------------------------------
	boolean isRedundantAnimationData(Model target, int comp,List<AnimationCurveNode> curves)
	{
		assert(curves.size() > 0);

		// look for animation nodes with
		//  * sub channels for all relevant components set
		//  * one key/value pr per component
		//  * combined values match up the corresponding value in the bind pose node transformation
		// only such nodes are 'redundant' for this function.

		if (curves.size() > 1) {
			return false;
		}

		AnimationCurveNode nd = curves./*front()*/get(0);
		Map<String, AnimationCurve> sub_curves = nd.curves();

		AnimationCurve dx = sub_curves.get("d|X");
		AnimationCurve dy = sub_curves.get("d|Y");
		AnimationCurve dz = sub_curves.get("d|Z");

		if (dx == /*sub_curves.end()*/null || dy == /*sub_curves.end()*/null || dz == /*sub_curves.end()*/null) {
			return false;
		}

		FloatArrayList vx = dx.getValues();
		FloatArrayList vy = dy.getValues();
		FloatArrayList vz = dz.getValues();

		if(AssUtil.size(vx) != 1 || AssUtil.size(vy) != 1 || AssUtil.size(vz) != 1) {
			return false;
		}

		Vector3f dyn_val = new Vector3f(vx.getFloat(0), vy.getFloat(0), vz.getFloat(0));
		Vector3f static_val = PropertyTable.propertyGet(target.props(), 
			nameTransformationCompProperty(comp), 
			transformationCompDefaultValue(comp, new Vector3f())
		);

		float epsilon = 1e-6f;
//		return (dyn_val - static_val).SquareLength() < epsilon;
		return Vector3f.distanceSquare(dyn_val, static_val) < epsilon;
	}


	// ------------------------------------------------------------------------------------------------
	NodeAnim generateRotationNodeAnim(String name, Model target, List<AnimationCurveNode> curves,
			Map<AnimationCurveNode, AnimationLayer> layer_map)
	{
//		ScopeGuard<NodeAnim> na(new NodeAnim());
		NodeAnim na = new NodeAnim();
		na.mNodeName = (name);

		convertRotationKeys(na, curves, layer_map, target.rotationOrder());

		// dummy scaling key
		na.mScalingKeys = new VectorKey[]{new VectorKey()};
//		na.mNumScalingKeys = 1;

//		na.mScalingKeys[0].mTime = 0.;
//		na.mScalingKeys[0].mValue.set(1.0f,1.0f,1.0f);

		// dummy position key
		na.mPositionKeys = new VectorKey[]{new VectorKey()};
//		na.mNumPositionKeys = 1;
//
//		na.mPositionKeys[0].mTime = 0.;
//		na.mPositionKeys[0].mValue = Vector3D();

		return na/*.dismiss()*/;
	}


	// ------------------------------------------------------------------------------------------------
	NodeAnim generateScalingNodeAnim(String name, Model target, List<AnimationCurveNode> curves,
			Map<AnimationCurveNode, AnimationLayer> layer_map)
	{
//		ScopeGuard<NodeAnim> na(new NodeAnim());
		NodeAnim na = new NodeAnim();
		na.mNodeName = name;

		convertScaleKeys(na, curves, layer_map);

		// dummy rotation key
		na.mRotationKeys = new QuatKey[]{new QuatKey()};
//		na.mNumRotationKeys = 1;
//
//		na.mRotationKeys[0].mTime = 0.;
//		na.mRotationKeys[0].mValue = Quaternion();

		// dummy position key
		na.mPositionKeys = new VectorKey[] {new VectorKey()};
//		na.mNumPositionKeys = 1;
//
//		na.mPositionKeys[0].mTime = 0.;
//		na.mPositionKeys[0].mValue = Vector3D();

		return na/*.dismiss()*/;
	}


	// ------------------------------------------------------------------------------------------------
	NodeAnim generateTranslationNodeAnim(String name, Model target, List<AnimationCurveNode> curves,
			Map<AnimationCurveNode, AnimationLayer> layer_map,
		   boolean inverse/* = false*/)
	{
//		ScopeGuard<NodeAnim> na(new NodeAnim());
//		na.mNodeName.Set(name);
		NodeAnim na = new NodeAnim();
		na.mNodeName = name;

		convertTranslationKeys(na, curves, layer_map);

		if (inverse) {
			for (int i = 0; i < na.getNumPositionKeys(); ++i) {
				na.mPositionKeys[i].mValue.scale(-1.0f);
			}
		}

		// dummy scaling key
		na.mScalingKeys = new VectorKey[]{new VectorKey()};
//		na.mNumScalingKeys = 1;

		na.mScalingKeys[0].mTime = 0.f;
		na.mScalingKeys[0].mValue.set(1.0f,1.0f,1.0f);

		// dummy rotation key
		na.mRotationKeys = new QuatKey[]{new QuatKey()};
//		na.mNumRotationKeys = 1;
//
//		na.mRotationKeys[0].mTime = 0.;
//		na.mRotationKeys[0].mValue = Quaternion();

		return na/*.dismiss()*/;
	}


	// ------------------------------------------------------------------------------------------------
	// generate node anim, extracting only Rotation, Scaling and Translation from the given chn
	NodeAnim generateSimpleNodeAnim(String name, Model target, 
//		NodeMap::const_iterator chn[TransformationComp_MAXIMUM], 
		List<AnimationCurveNode>[] chn,
//		NodeMap::const_iterator iter_end,
		Map<AnimationCurveNode, AnimationLayer> layer_map,
		boolean reverse_order /*= false*/)

	{
//		ScopeGuard<NodeAnim> na(new NodeAnim());
//		na.mNodeName.Set(name);
		NodeAnim na = new NodeAnim();
		na.mNodeName = name;

		PropertyTable props = target.props();

		// need to convert from TRS order to SRT?
		if(reverse_order) {
		
			Vector3f def_scale = null , def_translate = null;
			Quaternion def_rot = new Quaternion();

			ArrayList<KeyFrameList> scaling = new ArrayList<>();
			ArrayList<KeyFrameList> translation = new ArrayList<>();
			ArrayList<KeyFrameList> rotation = new ArrayList<>();
			
			if(chn[TransformationComp_Scaling] != null) {
				scaling = getKeyframeList(chn[TransformationComp_Scaling]/*(*chn[TransformationComp_Scaling]).second*/);
			}
			else {
				def_scale = PropertyTable.propertyGet(props,"Lcl Scaling",new Vector3f(1.f,1.f,1.f));
			}

			if(chn[TransformationComp_Translation] != null) {
				translation = getKeyframeList(chn[TransformationComp_Translation]/*(*chn[TransformationComp_Translation]).second*/);
			}
			else {
				def_translate = PropertyTable.propertyGet(props,"Lcl Translation",new Vector3f(0.f,0.f,0.f));
			}
			
			if(chn[TransformationComp_Rotation] != null) {
				rotation = getKeyframeList(chn[TransformationComp_Rotation]/*(*chn[TransformationComp_Rotation]).second*/);
			}
			else {
				eulerToQuaternion(PropertyTable.propertyGet(props,"Lcl Rotation",new Vector3f(0.f,0.f,0.f)),
					target.rotationOrder(), def_rot);
			}

			ArrayList<KeyFrameList> joined = new ArrayList<>(scaling.size() + translation.size() + rotation.size());
//			joined.insert(joined.end(), scaling.begin(), scaling.end());
//			joined.insert(joined.end(), translation.begin(), translation.end());
//			joined.insert(joined.end(), rotation.begin(), rotation.end());
			joined.addAll(scaling);
			joined.addAll(translation);
			joined.addAll(rotation);

			LongArrayList times = getKeyTimeList(joined);

			QuatKey[] out_quat = new QuatKey[times.size()];
			VectorKey[] out_scale = new VectorKey[times.size()];
			VectorKey[] out_translation = new VectorKey[times.size()];

			convertTransformOrder_TRStoSRT(out_quat, out_scale, out_translation, 
				scaling, 
				translation, 
				rotation, 
				times,
				target.rotationOrder(),
				def_scale,
				def_translate,
				def_rot);

			// XXX remove duplicates / redundant keys which this operation did
			// likely produce if not all three channels were equally dense.

//			na.mNumScalingKeys = static_cast<unsigned int>(times.size());
//			na.mNumRotationKeys = na.mNumScalingKeys;
//			na.mNumPositionKeys = na.mNumScalingKeys;

			na.mScalingKeys = out_scale;
			na.mRotationKeys = out_quat;
			na.mPositionKeys = out_translation;
		}
		else {

			// if a particular transformation is not given, grab it from
			// the corresponding node to meet the semantics of NodeAnim,
			// which requires all of rotation, scaling and translation
			// to be set.
			if(chn[TransformationComp_Scaling] != null) {
				convertScaleKeys(na, chn[TransformationComp_Scaling],/*(*chn[TransformationComp_Scaling]).second, */
					layer_map);
			}
			else {
				na.mScalingKeys = new VectorKey[]{new VectorKey()};
//				na.mNumScalingKeys = 1;

				na.mScalingKeys[0].mTime = 0.f;
				na.mScalingKeys[0].mValue.set(PropertyTable.propertyGet(props,"Lcl Scaling",
					new Vector3f(1.f,1.f,1.f)));
			}

			if(chn[TransformationComp_Rotation] != null) {
				convertRotationKeys(na, /*(*chn[TransformationComp_Rotation]).second*/chn[TransformationComp_Rotation], 
					layer_map,
					target.rotationOrder());
			}
			else {
				na.mRotationKeys = new QuatKey[]{new QuatKey()};
//				na.mNumRotationKeys = 1;

				na.mRotationKeys[0].mTime = 0.f;
				eulerToQuaternion(PropertyTable.propertyGet(props,"Lcl Rotation",new Vector3f(0.f,0.f,0.f)),
					target.rotationOrder(), na.mRotationKeys[0].mValue);
			}

			if(chn[TransformationComp_Translation] != null) {
				convertTranslationKeys(na, chn[TransformationComp_Translation], //(*chn[TransformationComp_Translation]).second, 
					layer_map);
			}
			else {
				na.mPositionKeys = new VectorKey[]{new VectorKey()};
//				na.mNumPositionKeys = 1;

				na.mPositionKeys[0].mTime = 0.f;
				na.mPositionKeys[0].mValue.set(PropertyTable.propertyGet(props,"Lcl Translation",
					new Vector3f(0.f,0.f,0.f)));
			}

		}
		return na/*.dismiss()*/;
	}
	
	// ------------------------------------------------------------------------------------------------
	ArrayList<KeyFrameList> getKeyframeList(List<AnimationCurveNode> nodes)
	{
		ArrayList<KeyFrameList> inputs = new ArrayList<>(nodes.size()*3);
//		inputs.reserve(nodes.size()*3);

//		BOOST_FOREACH(const AnimationCurveNode* node, nodes) {
		for (AnimationCurveNode node : nodes){
//			assert(node);

			Map<String, AnimationCurve> curves = node.curves();
			if(curves == null) continue;
//			BOOST_FOREACH(const AnimationCurveMap::value_type& kv, curves) {
			for(Map.Entry<String, AnimationCurve> kv : curves.entrySet()){
				String kv_first = kv.getKey();
				
				int mapto;
				if (kv_first.equals("d|X")) {
					mapto = 0;
				}
				else if (kv_first.equals("d|Y")) {
					mapto = 1;
				}
				else if (kv_first.equals("d|Z")) {
					mapto = 2;
				}
				else {
					if(DefaultLogger.LOG_OUT)
						DefaultLogger.warn("ignoring scale animation curve, did not recognize target component");
					continue;
				}

				AnimationCurve curve = kv.getValue();
				assert(AssUtil.size(curve.getKeys()) == AssUtil.size(curve.getValues()) && AssUtil.size(curve.getKeys()) > 0);

//				inputs.push_back(boost::make_tuple(&curve->GetKeys(), &curve->GetValues(), mapto));
				inputs.add(new KeyFrameList(curve.getKeys(), curve.getValues(), mapto));
			}
		}
		return inputs; // pray for NRVO :-)
	}


	// ------------------------------------------------------------------------------------------------
	LongArrayList getKeyTimeList(ArrayList<KeyFrameList> inputs)
	{
		assert(inputs.size() > 0);

		// reserve some space upfront - it is likely that the keyframe lists
		// have matching time values, so max(of all keyframe lists) should 
		// be a good estimate.
		LongArrayList keys = null;
		
		int estimate = 0;
//		BOOST_FOREACH(const KeyFrameList& kfl, inputs) {
		for(KeyFrameList kfl : inputs){
//			estimate = std::max(estimate, kfl.get<0>()->size());
			estimate = Math.max(estimate, kfl.first.size());
		}

		keys = new LongArrayList(estimate);

//		std::vector<unsigned int> next_pos;
//		next_pos.resize(inputs.size(),0);
		int[] next_pos = new int[inputs.size()];

		final int count = inputs.size();
		while(true) {

//			uint64_t min_tick = std::numeric_limits<uint64_t>::max();
			long min_tick = Long.MAX_VALUE;
			for (int i = 0; i < count; ++i) {
				KeyFrameList kfl = inputs.get(i);

				LongArrayList first = kfl.first;
				if (first.size() > next_pos[i] && first.getLong(next_pos[i]) < min_tick) {
					min_tick = first.getLong(next_pos[i]);
				}
			}

			if (min_tick == /*std::numeric_limits<uint64_t>::max()*/Long.MAX_VALUE) {
				break;
			}
			keys.add(min_tick);

			for (int i = 0; i < count; ++i) {
				KeyFrameList kfl = inputs.get(i);
				LongArrayList first = kfl.first;

				while(first.size() > next_pos[i] && first.getLong(next_pos[i]) == min_tick) {
					++next_pos[i];
				}
			}
		}	

		return keys;
	}


	// ------------------------------------------------------------------------------------------------
	void interpolateKeys(VectorKey[] valOut, int offset, LongArrayList keys, ArrayList<KeyFrameList> inputs, boolean geom)

	{
		assert(AssUtil.size(keys) > 0);
		assert(valOut != null);

//		std::vector<unsigned int> next_pos;
//		final int count = inputs.size();
//
//		next_pos.resize(inputs.size(),0);
		final int count = inputs.size();
		int[] next_pos = new int[inputs.size()];

//		BOOST_FOREACH(KeyTimeList::value_type time, keys) {
		for(int j = 0; j < keys.size(); j++){
			long time = keys.getLong(j);
			float result[] = {0.0f, 0.0f, 0.0f};
			if(geom) {
				result[0] = result[1] = result[2] = 1.0f;
			}

			for (int i = 0; i < count; ++i) {
				KeyFrameList kfl = inputs.get(i);

				final int ksize = kfl.first.size();
				if (ksize > next_pos[i] && kfl.first.getLong(next_pos[i]) == time) {
					++next_pos[i]; 
				}

				final int id0 = next_pos[i]>0 ? next_pos[i]-1 : 0;
				final int id1 = next_pos[i]==ksize ? ksize-1 : next_pos[i];

				// use lerp for interpolation
				final float valueA = kfl.second.getFloat(id0);
				final float valueB = kfl.second.getFloat(id1);

				final long timeA = kfl.first.getLong(id0);
				final long timeB = kfl.first.getLong(id1);

				// do the actual interpolation in double-precision arithmetics
				// because it is a bit sensitive to rounding errors.
				final double factor = timeB == timeA ? 0. : (double)((time - timeA) / (timeB - timeA));
				final float interpValue = (float)(valueA + (valueB - valueA) * factor);

				if(geom) {
					result[kfl.third] *= interpValue;
				}
				else {
					result[kfl.third] += interpValue;
				}
			}

			// magic value to convert fbx times to seconds
			valOut[offset].mTime = (float) (convert_fbx_time(time) * anim_fps);

			min_time = Math.min(min_time, valOut[offset].mTime);
			max_time = Math.max(max_time, valOut[offset].mTime);

			valOut[offset].mValue.x = result[0];
			valOut[offset].mValue.y = result[1];
			valOut[offset].mValue.z = result[2];
			
			++offset;
		}
	}


	// ------------------------------------------------------------------------------------------------
	void interpolateKeys(QuatKey[] valOut, int offset, LongArrayList keys, ArrayList<KeyFrameList> inputs, boolean geom, int order)
	{
		assert(AssUtil.size(keys) > 0);
		assert(valOut != null);

//		boost::scoped_array<aiVectorKey> temp(new aiVectorKey[keys.size()]);
		VectorKey[] temp = new VectorKey[keys.size()];
		AssUtil.initArray(temp);
		interpolateKeys(temp,0,keys,inputs,geom);

		Matrix4f m = new Matrix4f();

		Quaternion lastq = new Quaternion();
		Quaternion quat = new Quaternion(/*aiMatrix3x3(m)*/);
		for (int i = 0, c = keys.size(); i < c; ++i) {

			valOut[i].mTime = temp[i].mTime;

			
			getRotationMatrix(order, temp[i].mValue, m);
			
			quat.setFromMatrix(m);
			// take shortest path by checking the inner product
			// http://www.3dkingdoms.com/weekly/weekly.php?a=36
			if (quat.x * lastq.x + quat.y * lastq.y + quat.z * lastq.z + quat.w * lastq.w < 0)
			{
				quat.x = -quat.x;
				quat.y = -quat.y;
				quat.z = -quat.z;
				quat.w = -quat.w;
			} 
			lastq.set(quat);

			valOut[i].mValue.set(quat); 
		}
	}


	// ------------------------------------------------------------------------------------------------
	void convertTransformOrder_TRStoSRT(QuatKey[] out_quat, VectorKey[] out_scale, VectorKey[] out_translation, 
		ArrayList<KeyFrameList> scaling, ArrayList<KeyFrameList> translation, ArrayList<KeyFrameList> rotation, 
		LongArrayList times, int order,Vector3f def_scale,Vector3f def_translate,Quaternion def_rotation)
	{
		if (rotation.size() > 0) {
			interpolateKeys(out_quat, 0, times, rotation, false, order);
		}
		else {
			for (int i = 0; i < times.size(); ++i) {
				out_quat[i].mTime = (float) (convert_fbx_time(times.getLong(i)) * anim_fps);
				out_quat[i].mValue.set(def_rotation);
			}
		}

		if (scaling.size() > 0) {
			interpolateKeys(out_scale, 0, times, scaling, true);
		}
		else {
			for (int i = 0; i < times.size(); ++i) {
				out_scale[i].mTime = (float) (convert_fbx_time(times.getLong(i)) * anim_fps);
				out_scale[i].mValue.set(def_scale);
			}
		}

		if (translation.size() > 0) {
			interpolateKeys(out_translation, 0, times, translation, false);
		}
		else {
			for (int i = 0; i < times.size(); ++i) {
				out_translation[i].mTime = (float) (convert_fbx_time(times.getLong(i)) * anim_fps);
				out_translation[i].mValue.set(def_translate);
			}
		}

		final int count = times.size();
		Matrix4f mat = new Matrix4f();
		Matrix4f rot = new Matrix4f();
		for (int i = 0; i < count; ++i) {
			Quaternion r = out_quat[i].mValue;
			Vector3f s = out_scale[i].mValue;
			Vector3f t = out_translation[i].mValue;

//			aiMatrix4x4 mat, temp;
//			aiMatrix4x4::Translation(t, mat);
//			mat *= aiMatrix4x4( r.GetMatrix() );
//			mat *= aiMatrix4x4::Scaling(s, temp);
			mat.setIdentity();
			mat.m30 = t.x;
			mat.m31 = t.y;
			mat.m32 = t.z;
			r.toMatrix(rot);
			Matrix4f.mul(mat, rot, mat);
			mat.scale(s);
			
//			mat.decompose(s, r, t);
			AssUtil.decompose(mat, s, r, t);
		}
	}


	// ------------------------------------------------------------------------------------------------
	// euler xyz -> quat
	void eulerToQuaternion(Vector3f rot, int order, Quaternion quat) 
	{
		Matrix4f m = new Matrix4f();
		getRotationMatrix(order, rot, m);

//		return aiQuaternion(aiMatrix3x3(m));
		quat.setFromMatrix(m);
	}


	// ------------------------------------------------------------------------------------------------
	void convertScaleKeys(NodeAnim na, List<AnimationCurveNode> nodes, Map<AnimationCurveNode, AnimationLayer> layers)
	{
		assert(AssUtil.size(nodes) > 0);

		// XXX for now, assume scale should be blended geometrically (i.e. two
		// layers should be multiplied with each other). There is a FBX 
		// property in the layer to specify the behaviour, though.
		ArrayList<KeyFrameList> inputs = getKeyframeList(nodes);
		LongArrayList keys = getKeyTimeList(inputs);

//		na->mNumScalingKeys = static_cast<unsigned int>(keys.size());
		na.mScalingKeys = new VectorKey[keys.size()];
		AssUtil.initArray(na.mScalingKeys);
		interpolateKeys(na.mScalingKeys, 0,keys, inputs, true);
	}


	// ------------------------------------------------------------------------------------------------
	void convertTranslationKeys(NodeAnim na, List<AnimationCurveNode> nodes, Map<AnimationCurveNode, AnimationLayer> layers)
	{
		assert(AssUtil.size(nodes) > 0);

		// XXX see notes in ConvertScaleKeys()
		ArrayList<KeyFrameList> inputs = getKeyframeList(nodes);
		LongArrayList keys = getKeyTimeList(inputs);

//		na->mNumPositionKeys = static_cast<unsigned int>(keys.size());
		na.mPositionKeys = new VectorKey[keys.size()];
		AssUtil.initArray(na.mPositionKeys);
		interpolateKeys(na.mPositionKeys, 0,keys, inputs, false);
	}


	// ------------------------------------------------------------------------------------------------
	void convertRotationKeys(NodeAnim na, List<AnimationCurveNode> nodes, Map<AnimationCurveNode, AnimationLayer> layers,
		int order)
	{
		assert(AssUtil.size(nodes) > 0);

		// XXX see notes in ConvertScaleKeys()
		ArrayList<KeyFrameList> inputs = getKeyframeList(nodes);
		LongArrayList keys = getKeyTimeList(inputs);

//		na->mNumRotationKeys = static_cast<unsigned int>(keys.size());
		na.mRotationKeys = AssUtil.initArray(new QuatKey[keys.size()]);
		interpolateKeys(na.mRotationKeys, 0,keys, inputs, false, order);
	}


	// ------------------------------------------------------------------------------------------------
	// copy generated meshes, animations, lights, cameras and textures to the output scene
	void transferDataToScene()
	{
		assert(out.mMeshes == null);

		// note: the trailing () ensures initialization with NULL - not
		// many C++ users seem to know this, so pointing it out to avoid
		// confusion why this code works.

//		if(meshes.size() > 0) {
//			out->mMeshes = new aiMesh*[meshes.size()]();
//			out->mNumMeshes = static_cast<unsigned int>(meshes.size());
//
//			std::swap_ranges(meshes.begin(),meshes.end(),out->mMeshes);
//		}
		
		out.mMeshes = AssUtil.toArray(meshes, Mesh.class);

//		if(materials.size()) {
//			out->mMaterials = new aiMaterial*[materials.size()]();
//			out->mNumMaterials = static_cast<unsigned int>(materials.size());
//
//			std::swap_ranges(materials.begin(),materials.end(),out->mMaterials);
//		}
		out.mMaterials = AssUtil.toArray(materials, Material.class);
//		if(animations.size()) {
//			out->mAnimations = new aiAnimation*[animations.size()]();
//			out->mNumAnimations = static_cast<unsigned int>(animations.size());
//
//			std::swap_ranges(animations.begin(),animations.end(),out->mAnimations);
//		}
		out.mAnimations = AssUtil.toArray(animations, Animation.class);
		
//		if(lights.size()) {
//			out->mLights = new aiLight*[lights.size()]();
//			out->mNumLights = static_cast<unsigned int>(lights.size());
//
//			std::swap_ranges(lights.begin(),lights.end(),out->mLights);
//		}
		out.mLights = AssUtil.toArray(lights, Light.class);
//		if(cameras.size()) {
//			out->mCameras = new aiCamera*[cameras.size()]();
//			out->mNumCameras = static_cast<unsigned int>(cameras.size());
//
//			std::swap_ranges(cameras.begin(),cameras.end(),out->mCameras);
//		}
		out.mCameras = AssUtil.toArray(cameras, Camera.class);
	}
	
	static void convertToAssimpScene(Scene out, Document doc){
		Parser.init();
		new FBXConverter(out, doc);
		Parser.clear();
	}
	
	private final class KeyFrameList{
		LongArrayList first;
		FloatArrayList second;
		int third;
		public KeyFrameList(LongArrayList first, FloatArrayList second,int third) {
			this.first = first;
			this.second = second;
			this.third = third;
		}
	}
}

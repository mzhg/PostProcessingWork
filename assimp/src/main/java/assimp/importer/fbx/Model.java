package assimp.importer.fbx;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.AssUtil;

/** DOM base class for FBX models (even though its semantics are more "node" than "model" */
final class Model extends FBXObject{

//	enum RotOrder
//	{ 
	static final int 
		RotOrder_EulerXYZ = 0, 
		RotOrder_EulerXZY = 1, 
		RotOrder_EulerYZX = 2, 
		RotOrder_EulerYXZ = 3, 
		RotOrder_EulerZXY = 4, 
		RotOrder_EulerZYX = 5,

		RotOrder_SphericXYZ = 6,

		RotOrder_MAX = 7; // end-of-enum sentinel
//	};


//	enum TransformInheritance
//	{
	static final int 
		TransformInheritance_RrSs = 0,
		TransformInheritance_RSrs = 1,
		TransformInheritance_Rrs  = 2,

		TransformInheritance_MAX = 3; // end-of-enum sentinel
//	};
	
	private ArrayList<FBXMaterial> materials;
	private ArrayList<Geometry> geometry;
	private ArrayList<NodeAttribute> attributes;

	private String shading = "Y";
	private String culling;
	private PropertyTable props;
	
	public Model(long id, Element element, Document doc, String name) {
		super(id, element, name);
		
		Scope sc = Parser.getRequiredScope(element);
		Element Shading = sc.get("Shading");
		Element Culling = sc.get("Culling");

		if(Shading != null) {
			shading = Parser.getRequiredToken(Shading,0).stringContents();
		}

		if (Culling != null) {
			culling = Parser.parseTokenAsStringSafe(Parser.getRequiredToken(Culling,0));
		}

		props = Document.getPropertyTable(doc,"Model.FbxNode",element,sc, false);
		resolveLinks(element,doc);
	}
	
	String shading() {
		return shading;
	}

	String culling() {
		return culling;
	}

	PropertyTable props() {
		return props;
	}

	/** Get material links */
	ArrayList<FBXMaterial> getMaterials() {
//		if(materials == null)
//			materials = new ArrayList<>();
		return materials;
	}


	/** Get geometry links */
	ArrayList<Geometry> getGeometry() {
//		if(geometry == null)
//			geometry = new ArrayList<>();
		return geometry;
	}


	/** Get node attachments */
	ArrayList<NodeAttribute> getAttributes() {
//		if(attributes == null)
//			attributes = new ArrayList<NodeAttribute>();
		return attributes;
	}

	/** convenience method to check if the node has a Null node marker */
	boolean isNull(){
		List<NodeAttribute>/*const std::vector<const NodeAttribute*>&*/ attrs = getAttributes();
		if(attrs == null) return false;
		
//		BOOST_FOREACH(const NodeAttribute* att, attrs) {
		for(NodeAttribute att : attrs){
//			const Null* null_tag = dynamic_cast<const Null*>(att);
//			if(null_tag) {
//				return true;
//			}
			if(att instanceof Null)
				return true;
		}

		return false;
	}
	
	void resolveLinks(Element element, Document doc){
		String arr[] = {"Geometry","Material","NodeAttribute"};

		// resolve material
		List<Connection> conns = doc.getConnectionsByDestinationSequenced(ID(),arr);

//		materials (conns.size());
//		geometry.reserve(conns.size());
//		attributes.reserve(conns.size());
//		materials  = AssUtil.reserve(materials, conns.size());
//		geometry   = AssUtil.reserve(geometry, conns.size());
//		attributes = AssUtil.reserve(attributes, conns.size());
//		BOOST_FOREACH(const Connection* con, conns) {
		for(Connection con : conns){

			// material and geometry links should be Object-Object connections
			if (/*con->PropertyName().length()*/ !AssUtil.isEmpty(con.propertyName())) {
				continue;
			}

			final FBXObject ob = con.sourceObject();
			if(ob == null) {
				FBXUtil.DOMWarning("failed to read source object for incoming Model link, ignoring",element);
				continue;
			}

//			final FBXMaterial mat = dynamic_cast<const Material*>(ob);
//			if(mat) {
//				materials.push_back(mat);
//				continue;
//			}

//			const Geometry* const geo = dynamic_cast<const Geometry*>(ob);
//			if(geo) {
//				geometry.push_back(geo);
//				continue;
//			}

//			const NodeAttribute* const att = dynamic_cast<const NodeAttribute*>(ob);
//			if(att) {
//				attributes.push_back(att);
//				continue;
//			}
			
			if(ob instanceof FBXMaterial){
				if(materials == null)
					materials = new ArrayList<>();
				materials.add((FBXMaterial)ob);
			}else if(ob instanceof Geometry){
				if(geometry == null)
					materials = new ArrayList<>();
				geometry.add((Geometry)ob);
			}else if(ob instanceof NodeAttribute){
				if(attributes == null)
					materials = new ArrayList<>();
				attributes.add((NodeAttribute)ob);
			}

			FBXUtil.DOMWarning("source object for model link is neither Material, NodeAttribute nor Geometry, ignoring",element);
			continue;
		}
	}
	
	int quaternionInterpolate() { return PropertyTable.propertyGet(props(), "QuaternionInterpolate",  0);}

	Vector3f rotationOffset() { return PropertyTable.propertyGet(props(), "RotationOffset", new  Vector3f());}

	Vector3f rotationPivot() { return PropertyTable.propertyGet(props(), "RotationPivot", new  Vector3f());}

	Vector3f scalingOffset() { return PropertyTable.propertyGet(props(), "ScalingOffset", new  Vector3f());}

	Vector3f scalingPivot() { return PropertyTable.propertyGet(props(), "ScalingPivot", new  Vector3f());}

	boolean translationActive() { return PropertyTable.propertyGet(props(), "TranslationActive",  false);}

	Vector3f translationMin() { return PropertyTable.propertyGet(props(), "TranslationMin", new  Vector3f());}

	Vector3f translationMax() { return PropertyTable.propertyGet(props(), "TranslationMax", new  Vector3f());}

	boolean translationMinX() { return PropertyTable.propertyGet(props(), "TranslationMinX",  false);}

	boolean translationMaxX() { return PropertyTable.propertyGet(props(), "TranslationMaxX",  false);}

	boolean translationMinY() { return PropertyTable.propertyGet(props(), "TranslationMinY",  false);}

	boolean translationMaxY() { return PropertyTable.propertyGet(props(), "TranslationMaxY",  false);}

	boolean translationMinZ() { return PropertyTable.propertyGet(props(), "TranslationMinZ",  false);}

	boolean translationMaxZ() { return PropertyTable.propertyGet(props(), "TranslationMaxZ",  false);}

	int rotationOrder() {
		final int ival = PropertyTable.propertyGet(props(), "RotationOrder", Integer.valueOf( 0));
		if (ival < 0 || ival >= RotOrder_MAX/*AI_CONCAT(type, _MAX)*/) {
			assert( 0 >= 0 &&  0 < RotOrder_MAX/*AI_CONCAT(type, _MAX)*/);
			return ( 0);
		}
		return (ival);
	}

	boolean rotationSpaceForLimitOnly() { return PropertyTable.propertyGet(props(), "RotationSpaceForLimitOnly",  false);}

	float rotationStiffnessX() { return PropertyTable.propertyGet(props(), "RotationStiffnessX",  0.0f);}

	float rotationStiffnessY() { return PropertyTable.propertyGet(props(), "RotationStiffnessY",  0.0f);}

	float rotationStiffnessZ() { return PropertyTable.propertyGet(props(), "RotationStiffnessZ",  0.0f);}

	float axisLen() { return PropertyTable.propertyGet(props(), "AxisLen",  0.0f);}

	Vector3f preRotation() { return PropertyTable.propertyGet(props(), "PreRotation", new  Vector3f());}

	Vector3f postRotation() { return PropertyTable.propertyGet(props(), "PostRotation", new  Vector3f());}

	boolean rotationActive() { return PropertyTable.propertyGet(props(), "RotationActive",  false);}

	Vector3f rotationMin() { return PropertyTable.propertyGet(props(), "RotationMin", new  Vector3f());}

	Vector3f rotationMax() { return PropertyTable.propertyGet(props(), "RotationMax", new  Vector3f());}

	boolean rotationMinX() { return PropertyTable.propertyGet(props(), "RotationMinX",  false);}

	boolean rotationMaxX() { return PropertyTable.propertyGet(props(), "RotationMaxX",  false);}

	boolean rotationMinY() { return PropertyTable.propertyGet(props(), "RotationMinY",  false);}

	boolean rotationMaxY() { return PropertyTable.propertyGet(props(), "RotationMaxY",  false);}

	boolean rotationMinZ() { return PropertyTable.propertyGet(props(), "RotationMinZ",  false);}

	boolean rotationMaxZ() { return PropertyTable.propertyGet(props(), "RotationMaxZ",  false);}

	int inheritType() {
		final int ival = PropertyTable.propertyGet(props(), "InheritType", Integer.valueOf( 0));
		if (ival < 0 || ival >= TransformInheritance_MAX/*AI_CONCAT(type, _MAX)*/) {
			assert( 0 >= 0 &&  0 < TransformInheritance_MAX/*AI_CONCAT(type, _MAX)*/);
			return ( 0);
		}
		return (ival);
	}

	boolean scalingActive() { return PropertyTable.propertyGet(props(), "ScalingActive",  false);}

	Vector3f scalingMin() { return PropertyTable.propertyGet(props(), "ScalingMin", new  Vector3f());}

	Vector3f scalingMax() { return PropertyTable.propertyGet(props(), "ScalingMax", new  Vector3f(1.f,1.f,1.f));}

	boolean scalingMinX() { return PropertyTable.propertyGet(props(), "ScalingMinX",  false);}

	boolean scalingMaxX() { return PropertyTable.propertyGet(props(), "ScalingMaxX",  false);}

	boolean scalingMinY() { return PropertyTable.propertyGet(props(), "ScalingMinY",  false);}

	boolean scalingMaxY() { return PropertyTable.propertyGet(props(), "ScalingMaxY",  false);}

	boolean scalingMinZ() { return PropertyTable.propertyGet(props(), "ScalingMinZ",  false);}

	boolean scalingMaxZ() { return PropertyTable.propertyGet(props(), "ScalingMaxZ",  false);}

	Vector3f geometricTranslation() { return PropertyTable.propertyGet(props(), "GeometricTranslation", new  Vector3f());}

	Vector3f geometricRotation() { return PropertyTable.propertyGet(props(), "GeometricRotation", new  Vector3f());}

	Vector3f geometricScaling() { return PropertyTable.propertyGet(props(), "GeometricScaling", new  Vector3f(1.f, 1.f, 1.f));}

	float minDampRangeX() { return PropertyTable.propertyGet(props(), "MinDampRangeX",  0.0f);}

	float minDampRangeY() { return PropertyTable.propertyGet(props(), "MinDampRangeY",  0.0f);}

	float minDampRangeZ() { return PropertyTable.propertyGet(props(), "MinDampRangeZ",  0.0f);}

	float maxDampRangeX() { return PropertyTable.propertyGet(props(), "MaxDampRangeX",  0.0f);}

	float maxDampRangeY() { return PropertyTable.propertyGet(props(), "MaxDampRangeY",  0.0f);}

	float maxDampRangeZ() { return PropertyTable.propertyGet(props(), "MaxDampRangeZ",  0.0f);}

	float minDampStrengthX() { return PropertyTable.propertyGet(props(), "MinDampStrengthX",  0.0f);}

	float minDampStrengthY() { return PropertyTable.propertyGet(props(), "MinDampStrengthY",  0.0f);}

	float minDampStrengthZ() { return PropertyTable.propertyGet(props(), "MinDampStrengthZ",  0.0f);}

	float maxDampStrengthX() { return PropertyTable.propertyGet(props(), "MaxDampStrengthX",  0.0f);}

	float maxDampStrengthY() { return PropertyTable.propertyGet(props(), "MaxDampStrengthY",  0.0f);}

	float maxDampStrengthZ() { return PropertyTable.propertyGet(props(), "MaxDampStrengthZ",  0.0f);}

	float preferredAngleX() { return PropertyTable.propertyGet(props(), "PreferredAngleX",  0.0f);}

	float preferredAngleY() { return PropertyTable.propertyGet(props(), "PreferredAngleY",  0.0f);}

	float preferredAngleZ() { return PropertyTable.propertyGet(props(), "PreferredAngleZ",  0.0f);}

	boolean show() { return PropertyTable.propertyGet(props(), "Show",  true);}

	boolean lODBox() { return PropertyTable.propertyGet(props(), "LODBox",  false);}

	boolean freeze() { return PropertyTable.propertyGet(props(), "Freeze",  false);}




}

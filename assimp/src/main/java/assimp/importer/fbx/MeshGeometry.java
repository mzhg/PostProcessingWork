package assimp.importer.fbx;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.List;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.DefaultLogger;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;

/** DOM class for FBX geometry of type "Mesh"*/
final class MeshGeometry extends Geometry{
	
	static final int TYPE_UNKOWN = 0;
	static final int TYPE_FLOAT = 1;
	static final int TYPE_INT = 2;
	static final int TYPE_VEC2 = 3;
	static final int TYPE_VEC3 = 4;
	static final int TYPE_VEC4 = 5;

	// cached data arrays
	private IntArrayList materials;
	private FloatBuffer vertices;
	private IntArrayList faces;
	private IntArrayList facesVertexStartIndices;
	private FloatBuffer tangents;
	private FloatBuffer binormals;
	private FloatBuffer normals;

	private String[] uvNames = new String[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS];
	private FloatBuffer[] uvs= new FloatBuffer[Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS];
	private FloatBuffer[] colors = new FloatBuffer[Mesh.AI_MAX_NUMBER_OF_COLOR_SETS];

	private int[] mapping_counts;
	private int[] mapping_offsets;
	private int[] mappings;
	
	public MeshGeometry(long id, Element element, String name, Document doc) {
		super(id, element, name, doc);
		
		Scope sc = element.compound();
		if (sc == null) {
			FBXUtil.DOMError("failed to read Geometry object (class: Mesh), no data scope found");
		}

		// must have Mesh elements:
		Element Vertices = Parser.getRequiredElement(sc,"Vertices",element);
		Element PolygonVertexIndex = Parser.getRequiredElement(sc,"PolygonVertexIndex",element);

		// optional Mesh elements:
		List<Element> Layer = sc.getCollection("Layer");

//		std::vector<aiVector3D> tempVerts;
		FloatArrayList tempVerts = new FloatArrayList();
		Parser.parseVectorDataArray3f(tempVerts,Vertices);

		if(tempVerts.isEmpty()) {
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("encountered mesh with no vertices");
			return;
		}

//		std::vector<int> tempFaces;
		IntArrayList tempFaces = new IntArrayList();
		Parser.parseVectorDataArray1i(tempFaces,PolygonVertexIndex);

		if(tempFaces.isEmpty()) {
			if(DefaultLogger.LOG_OUT)
				DefaultLogger.warn("encountered mesh with no faces");
			return;
		}
		
//		vertices.reserve(tempFaces.size());
//		faces.reserve(tempFaces.size() / 3);
		vertices = MemoryUtil.createFloatBuffer(tempFaces.size() * 3, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
		faces = new IntArrayList(tempFaces.size() / 3);
		
//		mapping_offsets.resize(tempVerts.size());
//		mapping_counts.resize(tempVerts.size(),0);
//		mappings.resize(tempFaces.size());
		mapping_offsets = new int[tempVerts.size()];
		mapping_counts  = new int[tempVerts.size()];
		mappings        = new int[tempFaces.size()];

		final int vertex_count = tempVerts.size();

		// generate output vertices, computing an adjacency table to
		// preserve the mapping from fbx indices to *this* indexing.
		int count = 0;
		int[] _mapping_counts = mapping_counts;
//		BOOST_FOREACH(int index, tempFaces) {
		for(int i = 0; i < tempFaces.size(); i++){
			int index = tempFaces.getInt(i);
			final int absi = index < 0 ? (-index - 1) : index;
			if(absi >= vertex_count) {
				FBXUtil.DOMError("polygon vertex index out of range",PolygonVertexIndex);
			}

//			vertices.push_back(tempVerts[absi]);
			vertices.put(tempVerts.elements(), 3 * absi, 3);
			++count;

//			++mapping_counts[absi];
			++_mapping_counts[absi];

			if (index < 0) {
				faces.add(count);
				count = 0;
			}
		}

		int cursor = 0;
		int[] _mapping_offsets = mapping_offsets;
		for (int i = 0, e = tempVerts.size(); i < e; ++i) {
			_mapping_offsets[i] = cursor;
			cursor += _mapping_counts[i];

			_mapping_counts[i] = 0;
		}

		cursor = 0;
//		BOOST_FOREACH(int index, tempFaces) {
		for(int i = 0; i < tempFaces.size(); i++){
			int index = tempFaces.getInt(i);
			int absi = index < 0 ? (-index - 1) : index;
			mappings[mapping_offsets[absi] + mapping_counts[absi]++] = cursor++;
		}
		
		// if settings.readAllLayers is true:
		//  * read all layers, try to load as many vertex channels as possible
		// if settings.readAllLayers is false:
		//  * read only the layer with index 0, but warn about any further layers 
//		for (ElementMap::const_iterator it = Layer.first; it != Layer.second; ++it) {
		for (Element it : Layer){
			List<Token> tokens = it.tokens();

			final int index = Parser.parseTokenAsInt(tokens.get(0));
			String err = Parser.get_error();
			if(err != null) {
				FBXUtil.DOMError(err,element);
			}

			if(doc.settings().readAllLayers || index == 0) {
				Scope layer = Parser.getRequiredScope(it);
				readLayer(layer);
			}
			else {
				if (DefaultLogger.LOG_OUT) {
					DefaultLogger.warn("ignoring additional geometry layers");
				}
			}
		}
	}
	
	/** Get a UV coordinate slot, returns an empty array if
	 *  the requested slot does not exist. */
	FloatBuffer getTextureCoords(int index) {
//		static const std::vector<aiVector2D> empty;
		return index >= Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS ? null : uvs[index];
	}


	/** Get a UV coordinate slot, returns an empty array if
	 *  the requested slot does not exist. */
	String getTextureCoordChannelName(int index) {
		return index >= Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS ? "" : uvNames[index];
	}

	/** Get a vertex color coordinate slot, returns an empty array if
	 *  the requested slot does not exist. */
	FloatBuffer getVertexColors(int index) {
		return index >= Mesh.AI_MAX_NUMBER_OF_COLOR_SETS ? null : colors[index];
	}
	
	/** Get a list of all vertex points, non-unique*/
	FloatBuffer getVertices() {	return vertices;}

	/** Get a list of all vertex normals or an empty array if
	 *  no normals are specified. */
	FloatBuffer getNormals() {	return normals;}

	/** Get a list of all vertex tangents or an empty array
	 *  if no tangents are specified */
	FloatBuffer getTangents() {	return tangents;}

	/** Get a list of all vertex binormals or an empty array
	 *  if no binormals are specified */
	FloatBuffer getBinormals(){	return binormals;}

	/** Return list of faces - each entry denotes a face and specifies
	 *  how many vertices it has. Vertices are taken from the 
	 *  vertex data arrays in sequential order. */
	IntArrayList getFaceIndexCounts() {	return faces;}
	
	/** Get per-face-vertex material assignments */
	IntArrayList getMaterialIndices() {	return materials;}

	/** Convert from a fbx file vertex index (for example from a #Cluster weight) or -1
	  * if the vertex index is not valid. */
	final long toOutputVertexIndex(int in_index/*, int[] offset_count*/) {
		int out_index;
		int out_count;
		if(in_index >= mapping_counts.length) {
			return -1;
		}

//		ai_assert(mapping_counts.size() == mapping_offsets.size());
		out_count = mapping_counts[in_index];

//		ai_assert(count != 0);
//		ai_assert(mapping_offsets[in_index] + count <= mappings.size());
		assert out_count != 0;
		assert mapping_offsets[in_index] + out_count <= mappings.length;

//		return &mappings[mapping_offsets[in_index]];
		out_index = mapping_offsets[in_index];
		return AssUtil.encode(out_index, out_count);
	}
	
	int[] outputData() { return mappings;}


	/** Determine the face to which a particular output vertex index belongs.
	 *  This mapping is always unique. */
	int faceForVertexIndex(int in_index) {
//		ai_assert(in_index < vertices.size());
		assert in_index < vertices.remaining();
	
		// in the current conversion pattern this will only be needed if
		// weights are present, so no need to always pre-compute this table
		if (AssUtil.isEmpty(facesVertexStartIndices)) {
			if(facesVertexStartIndices == null){
				facesVertexStartIndices = new IntArrayList(faces.size() + 1);
			}
			facesVertexStartIndices.size(faces.size() + 1);

//			std::partial_sum(faces.begin(), faces.end(), facesVertexStartIndices.begin() + 1);
			AssUtil.partial_sum(faces.elements(), 0, faces.size(), facesVertexStartIndices.elements(), 1);
//			facesVertexStartIndices.pop_back();
			facesVertexStartIndices.popInt();
		}

		assert(facesVertexStartIndices.size() == faces.size());
//		const std::vector<unsigned int>::iterator it = std::upper_bound(
//			facesVertexStartIndices.begin(),
//			facesVertexStartIndices.end(),
//			in_index
//		);
		final int it = AssUtil.upper_bound(facesVertexStartIndices.elements(), 0, facesVertexStartIndices.size(), in_index);

//		return static_cast<unsigned int>(std::distance(facesVertexStartIndices.begin(), it - 1));
		return it - 1;
	}
	
	void readLayer(Scope layer){
		final List<Element> LayerElement = layer.getCollection("LayerElement");
//		for (ElementMap::const_iterator eit = LayerElement.first; eit != LayerElement.second; ++eit) {
		for (Element eit : LayerElement) {
			Scope elayer = Parser.getRequiredScope(eit);

			readLayerElement(elayer);
		}
	}
	
	void readLayerElement(Scope layerElement){
		Element Type = Parser.getRequiredElement(layerElement,"Type",null);
		Element TypedIndex = Parser.getRequiredElement(layerElement,"TypedIndex",null);

		final String type = Parser.parseTokenAsStringSafe(Parser.getRequiredToken(Type,0));
		final int typedIndex = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(TypedIndex,0));

		final Scope top = Parser.getRequiredScope(element);
		final List<Element> candidates = top.getCollection(type);

//		for (ElementMap::const_iterator it = candidates.first; it != candidates.second; ++it) {
		for (Element it : candidates){
			final int index = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(/**(*it).second*/it,0));
			if(index == typedIndex) {
				readVertexData(type,typedIndex,Parser.getRequiredScope(/**(*it).second*/ it));
				return;
			}
		}

		DefaultLogger.error(AssUtil.makeString("failed to resolve vertex layer element: ",type ,", index: ",typedIndex));
	}
	
	void readVertexData(String type, int index, Scope source){
		final String MappingInformationType =Parser.parseTokenAsStringSafe(Parser.getRequiredToken(
				Parser.getRequiredElement(source,"MappingInformationType", null),0)
		);

		final String ReferenceInformationType =Parser.parseTokenAsStringSafe(Parser.getRequiredToken(
				Parser.getRequiredElement(source,"ReferenceInformationType", null),0)
		);
		
		if (type.equals("LayerElementUV")) {
			if(index >= Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS) {
				DefaultLogger.error(AssUtil.makeString("ignoring UV layer, maximum number of UV channels exceeded: ", 
					index," (limit is ",Mesh.AI_MAX_NUMBER_OF_TEXTURECOORDS , ")" ));
				return;
			}

			Element Name = source.get("Name");
			uvNames[index] = "";
			if(Name != null) {
				uvNames[index] =Parser.parseTokenAsStringSafe(Parser.getRequiredToken(Name,0));
			}

			uvs[index] = readVertexDataUV(source,
				MappingInformationType,
				ReferenceInformationType
			);
		}
		else if (type.equals("LayerElementMaterial")) {
			if (materials.size() > 0) {
				DefaultLogger.error("ignoring additional material layer");
				return;
			}

			materials = readVertexDataMaterials(/*temp_materials,*/source,
				MappingInformationType,
				ReferenceInformationType
			);

			// sometimes, there will be only negative entries. Drop the material
			// layer in such a case (I guess it means a default material should
			// be used). This is what the converter would do anyway, and it
			// avoids loosing the material if there are more material layers
			// coming of which at least one contains actual data (did observe
			// that with one test file).
			final int count_neg = AssUtil.count_if(materials.elements(),0 ,materials.size(),/*std::bind2nd(std::less<int>(),0)*/ new AssUtil.UnaryPredicateInt() {
				public boolean accept(int value) {
					return value < 0;
				}
			});
			
			if(count_neg == materials.size()) {
				if(DefaultLogger.LOG_OUT)
					DefaultLogger.warn("ignoring dummy material layer (all entries -1)");
				return;
			}

//			std::swap(temp_materials, materials);
		}
		else if (type.equals("LayerElementNormal")) {
			if (normals != null/*.remaining() > 0*/) {
				DefaultLogger.error("ignoring additional normal layer");
				return;
			}

			normals = readVertexDataNormals(source,
				MappingInformationType,
				ReferenceInformationType
			);
		}
		else if (type.equals("LayerElementTangent")) {
			if (tangents != null/*.size() > 0*/) {
				DefaultLogger.error("ignoring additional tangent layer");
				return;
			}

			tangents = readVertexDataTangents(/*tangents,*/source,
				MappingInformationType,
				ReferenceInformationType
			);
		}
		else if (type.equals("LayerElementBinormal")) {
			if (binormals != null /*.size() > 0*/) {
				DefaultLogger.error("ignoring additional binormal layer");
				return;
			}

			binormals = readVertexDataBinormals(/*binormals,*/source,
				MappingInformationType,
				ReferenceInformationType
			);
		}
		else if (type.equals("LayerElementColor")) {
			if(index >= Mesh.AI_MAX_NUMBER_OF_COLOR_SETS) {
				DefaultLogger.error(AssUtil.makeString("ignoring vertex color layer, maximum number of color sets exceeded: "
					,index," (limit is " , Mesh.AI_MAX_NUMBER_OF_COLOR_SETS, ")" ));
				return;
			}

			colors[index] = readVertexDataColors(/*colors[index],*/source,
				MappingInformationType,
				ReferenceInformationType
			);
		}
	}
	
	FloatBuffer resolveVertexDataArray(int type, Scope source, String MappingInformationType,String ReferenceInformationType,
			String dataElementName,String indexDataElementName, int vertex_count){
		boolean isIntArray = false;
		Object temp = null;
		int count = 0;
		final Element el = Parser.getRequiredElement(source, dataElementName, null);
		switch (type) {
		case TYPE_FLOAT:
		{
			FloatArrayList out = FloatArrayList.wrap(AssUtil.EMPTY_FLOAT);
			Parser.parseVectorDataArray1f(out, el);
			temp = out;
			count = 1;
		}
			break;
		case TYPE_VEC2:
		{
			FloatArrayList out = FloatArrayList.wrap(AssUtil.EMPTY_FLOAT);
			Parser.parseVectorDataArray2f(out, el);
			temp = out;
			count = 2;
		}
			break;
		case TYPE_VEC3:
		{
			FloatArrayList out = FloatArrayList.wrap(AssUtil.EMPTY_FLOAT);
			Parser.parseVectorDataArray3f(out, el);
			temp = out;
			count = 3;
		}
			break;
		case TYPE_VEC4:
		{
			FloatArrayList out = FloatArrayList.wrap(AssUtil.EMPTY_FLOAT);
			Parser.parseVectorDataArray4f(out, el);
			temp = out;
			count = 4;
		}
			break;
		case TYPE_INT:
		{
			IntArrayList out = IntArrayList.wrap(AssUtil.EMPTY_INT);
			Parser.parseVectorDataArray1i(out, el);
			temp = out;
			isIntArray = true;
			count = 5;
		}
			break;
		default:
			// neaver happend
			break;
		}
		
		// handle permutations of Mapping and Reference type - it would be nice to
		// deal with this more elegantly and with less redundancy, but right
		// now it seems unavoidable.
		if (MappingInformationType.equals("ByVertice") && ReferenceInformationType.equals("Direct")) {	
//			data_out.resize(vertex_count);
			assert !isIntArray;
			FloatBuffer data_out = MemoryUtil.createFloatBuffer(count * vertex_count,AssimpConfig.LOADING_USE_NATIVE_MEMORY);
			FloatArrayList tempUV = (FloatArrayList)temp;
			final float[] elem = tempUV.elements();
			final int size = tempUV.size()/count;
			for (int i = 0, e = size; i < e; ++i) {
				final int istart = mapping_offsets[i], iend = istart + mapping_counts[i];
				for (int j = istart; j < iend; ++j) {
//					data_out[mappings[j]] = tempUV[i];
					for(int c = 0; c < count; c++){
						data_out.put(mappings[j] * count + c, elem[i * count + c]);
					}
				}
			}
			
			return data_out;
		}
		else if (MappingInformationType.equals("ByVertice") && ReferenceInformationType.equals("IndexToDirect")) {	
//			data_out.resize(vertex_count);
			assert !isIntArray;
			FloatBuffer data_out = MemoryUtil.createFloatBuffer(count * vertex_count,AssimpConfig.LOADING_USE_NATIVE_MEMORY);
			FloatArrayList tempUV = (FloatArrayList)temp;
			IntArrayList uvIndices = IntArrayList.wrap(AssUtil.EMPTY_INT);
			Parser.parseVectorDataArray1i(uvIndices,Parser.getRequiredElement(source,indexDataElementName, null));
			final int[] indices = uvIndices.elements();
			final float[] elem = tempUV.elements();
			for (int i = 0, e = uvIndices.size(); i < e; ++i) {

				final int istart = mapping_offsets[i], iend = istart + mapping_counts[i];
				for (int j = istart; j < iend; ++j) {
					if(indices[i] >= tempUV.size()) {
						FBXUtil.DOMError("index out of range",Parser.getRequiredElement(source,indexDataElementName, null));
					}
//					data_out[mappings[j]] = tempUV[uvIndices[i]];
					final int uv_value = indices[i];
					for(int c = 0; c < count; c++){
						data_out.put(mappings[j] * count + c, elem[uv_value * count + c]);
					}
				}
			}
			
			return data_out;
		}
		else if (MappingInformationType.equals("ByPolygonVertex") && ReferenceInformationType.equals("Direct")) {
			FloatBuffer data_out = MemoryUtil.createFloatBuffer(count * vertex_count,AssimpConfig.LOADING_USE_NATIVE_MEMORY);
			FloatArrayList tempUV = (FloatArrayList)temp;
			if (tempUV.size()/count != vertex_count) {
				DefaultLogger.error(AssUtil.makeString("length of input data unexpected for ByPolygon mapping: ",
					tempUV.size(), ", expected " , vertex_count)
				);
				return null;
			}

//			data_out.swap(tempUV);
			data_out.put(tempUV.elements(), 0, tempUV.size());
			return data_out;
		}
		else if (MappingInformationType.equals("ByPolygonVertex") && ReferenceInformationType.equals("IndexToDirect")) {	
//			data_out.resize(vertex_count);
			FloatBuffer data_out = MemoryUtil.createFloatBuffer(count * vertex_count,AssimpConfig.LOADING_USE_NATIVE_MEMORY);
			FloatArrayList tempUV = (FloatArrayList)temp;

//			std::vector<int> uvIndices;
//			ParseVectorDataArray(uvIndices,GetRequiredElement(source,indexDataElementName));
			IntArrayList uvIndices = IntArrayList.wrap(AssUtil.EMPTY_INT);
			Parser.parseVectorDataArray1i(uvIndices,Parser.getRequiredElement(source,indexDataElementName, null));
			final int[] indices = uvIndices.elements();
			final float[] elem = tempUV.elements();
			if (uvIndices.size() != vertex_count) {
				DefaultLogger.error("length of input data unexpected for ByPolygonVertex mapping");
				return null;
			}

			int next = 0;
//			BOOST_FOREACH(int i, uvIndices) {
			for(int l = 0;l < uvIndices.size(); l++){
				int i = indices[l];
				if(i >= tempUV.size()/count) {
					FBXUtil.DOMError("index out of range",Parser.getRequiredElement(source,indexDataElementName, null));
				}

//				data_out[next++] = tempUV[i];
				for(int c = 0; c < count; c++){
					data_out.put(count * next + c, elem[count * i + c]);
				}
				next++;
			}
			
			return data_out;
		}
		else {
			DefaultLogger.error(AssUtil.makeString("ignoring vertex data channel, access type not implemented: ",
				 MappingInformationType , "," , ReferenceInformationType));
			
			return null;
		}
	}

	FloatBuffer readVertexDataUV(/*std::vector<aiVector2D>& uv_out, */Scope source, String MappingInformationType, String ReferenceInformationType){
		return resolveVertexDataArray(/*uv_out,*/TYPE_VEC2, source,MappingInformationType,ReferenceInformationType,
				"UV",
				"UVIndex",
				vertices.remaining()/3/*,
				mapping_counts,
				mapping_offsets,
				mappings*/);
	}

	FloatBuffer readVertexDataNormals(/*std::vector<aiVector3D>& normals_out,*/ Scope source, 
		String MappingInformationType,
		String ReferenceInformationType){
		return resolveVertexDataArray(TYPE_VEC3, source,MappingInformationType,ReferenceInformationType,
				"Normals",
				"NormalsIndex",
				vertices.remaining()/3/*,
				mapping_counts,
				mapping_offsets,
				mappings*/);
	}

	FloatBuffer readVertexDataColors(/*std::vector<aiColor4D>& colors_out, */Scope source, 
		String MappingInformationType,
		String ReferenceInformationType){
		return resolveVertexDataArray(/*colors_out,*/ TYPE_VEC4, source,MappingInformationType,ReferenceInformationType,
				"Colors",
				"ColorIndex",
				vertices.remaining()/3/*,
				mapping_counts,
				mapping_offsets,
				mappings*/);
	}

	FloatBuffer readVertexDataTangents(/*std::vector<aiVector3D>& tangents_out,*/ Scope source, 
		String MappingInformationType,
		String ReferenceInformationType){
		return resolveVertexDataArray(/*tangents_out,*/ TYPE_VEC3, source,MappingInformationType,ReferenceInformationType,
				"Tangent",
				"TangentIndex",
				vertices.remaining()/3/*,
				mapping_counts,
				mapping_offsets,
				mappings*/);
	}
	FloatBuffer readVertexDataBinormals(/*std::vector<aiVector3D>& binormals_out, */Scope source, 
		String MappingInformationType,
		String ReferenceInformationType){
		return resolveVertexDataArray(/*binormals_out,*/TYPE_VEC3, source,MappingInformationType,ReferenceInformationType,
				"Binormal",
				"BinormalIndex",
				vertices.remaining()/3/*,
				mapping_counts,
				mapping_offsets,
				mappings*/);
	}

	IntArrayList readVertexDataMaterials(/*MatIndexArray& materials_out, */Scope source, 
		String MappingInformationType,
		String ReferenceInformationType){
		final int face_count = faces.size();
		assert(face_count > 0);
		
		IntArrayList materials_out = IntArrayList.wrap(AssUtil.EMPTY_INT);
		// materials are handled separately. First of all, they are assigned per-face
		// and not per polyvert. Secondly, ReferenceInformationType=IndexToDirect
		// has a slightly different meaning for materials.
		Parser.parseVectorDataArray1i(materials_out,Parser.getRequiredElement(source,"Materials", null));

		if (MappingInformationType.equals("AllSame")) {
			int firstValue = -1;
			// easy - same material for all faces
			if (materials_out.isEmpty()) {
				DefaultLogger.error("expected material index, ignoring");
				return null;
			}
			else if (materials_out.size() > 1) {
				DefaultLogger.warn("expected only a single material index, ignoring all except the first one");
				firstValue = materials_out.getInt(0);
				materials_out.clear();
			}else
				firstValue = materials_out.getInt(0);
			
//			materials.assign(vertices.size(),materials_out[0]);
			materials_out.size(vertices.remaining()/3);
			Arrays.fill(materials_out.elements(), 0, materials_out.size(), firstValue);
		}
		else if (MappingInformationType.equals("ByPolygon") && ReferenceInformationType.equals("IndexToDirect")) {
			materials.size(face_count);

			if(materials_out.size() != face_count) {
				DefaultLogger.error(AssUtil.makeString("length of input data unexpected for ByPolygon mapping: ",
					materials_out.size() ,", expected " ,face_count)
				);
				return null;
			}
		}
		else {
			DefaultLogger.error(AssUtil.makeString("ignoring material assignments, access type not implemented: "
				,MappingInformationType , "," , ReferenceInformationType));
			
			return null;
		}
		
		return materials_out;
	}
}

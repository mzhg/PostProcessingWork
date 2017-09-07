package com.nvidia.developer.opengl.models.obj;

import com.nvidia.developer.opengl.utils.Pair;

import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class NvModelExtObj extends NvModelExt{

	// Helper enum used to indicate the format of each face as it
    // is being parsed from the OBJ file
//    enum OBJFaceFormat
//    {
	private static final int
        Face_Invalid = 0,
        Face_PosOnly = 1,
        Face_PosTex = 2,
        Face_PosTexNormal = 3,
        Face_PosNormal = 4;
//    };
	
	// Number of floats contained in the positions defined by the OBJ file
    private int m_numPositionComponents = 3;
    
    private final VectorCompactor.Difference<Vector3f> difference_Normal  = new VectorCompactor.Difference<Vector3f>(){
		public float diff(Vector3f v1, Vector3f v2) {
			return 1.0f - Vector3f.dot(v1, v2);
		}

		public boolean shouldMerge(float diff, float epsilon) {
			return (diff < epsilon);
		}
    };
    
    private final class Difference_Position<V extends Vector> implements VectorCompactor.Difference<V>{

		@Override
		public float diff(V v1, V v2) {
			float value = 0.0f;
			for(int i = 0; i < v1.getCount(); i++){
				float diff = v1.get(i) - v2.get(i);
				value += diff * diff;
			}
			return value;
		}

		@Override
		public boolean shouldMerge(float diff, float epsilon) {
			return diff < (epsilon * epsilon);
		}
    }

    // Data store for positions used by vertices in the mesh.  Detects duplicates,
    // within a tolerance, and eliminates them, providing a mapping from the position's
    // original index, based on the order it was added to the store, to its new index
    // in the compacted set of positions.
    private final VectorCompactor<Vector4f> m_positions;

    // Data store for normals used by vertices in the mesh. Also removes duplicates
    // like the abover positional one.
    private final VectorCompactor<Vector3f> m_normals;

    // Number of floats contained in the texture coordinates defined by the OBJ file
    private int m_numTexCoordComponents = 2;


    // Duplicate removing data store for texture coordinates used by vertices in the mesh.
    private final VectorCompactor<Vector3f> m_texCoords;
    // Duplicate removing data store for tangent vectors used by vertices in the mesh.
    private final VectorCompactor<Vector3f> m_tangents;

    // Array of material definitions used by the mesh, as defined by any included material libraries
    private final ArrayList<Material> m_rawMaterials = new ArrayList<>();

    // Map from material name to its index in the raw materials array
//    typedef std::tr1::unordered_map<std::string, uint32_t> MaterialMap;
//    MaterialMap m_materialMap;
    private final Map<String, Integer> m_materialMap = new HashMap<>();

    // Simple structure to define a texture used by a material in the mesh.  Currently only
    // contains the name of the texture.  All other texture paramaters defined in the 
    // material library are discarded.
//    struct Texture
//    {
//        std::string m_name;
//        bool operator<(const Texture& other) { return (m_name < other.m_name); }
//        bool operator==(const Texture& other) { return (m_name == other.m_name); }
//    };

    // Array of all textures defined by all materials in the mesh
    private final ArrayList<String> m_textures = new ArrayList<>();

    // Array of all sub meshs that comprise the model
    private final ArrayList<SubMeshObj> m_subMeshes = new ArrayList<>();

    // Axis-aligned bounding box definition
    private final Vector3f m_boundingBoxMin = new Vector3f();
    private final Vector3f m_boundingBoxMax = new Vector3f();
    private final Vector3f m_boundingBoxCenter = new Vector3f();
    
 // Constructor is protected to force the factory method to be used to create a new NvModelExt
    protected NvModelExtObj(float vertMergeThreshold, float normMergeThreshold, int initialVertCount /*= 3000*/){
    	Difference_Position<Vector4f> position4 = new Difference_Position<>();
    	Difference_Position<Vector3f> position3 = new Difference_Position<>();
    	
    	m_positions = new VectorCompactor<>(vertMergeThreshold, initialVertCount, position4);
    	m_normals = new VectorCompactor<>(normMergeThreshold, initialVertCount, difference_Normal);
    	
    	m_texCoords = new VectorCompactor<>(0.00001f, initialVertCount, position3);
    	m_tangents = new VectorCompactor<>(0.00001f, initialVertCount, difference_Normal);
    	
    	m_rawMaterials.ensureCapacity(32);
    	m_subMeshes.ensureCapacity(32);
    	
    	ResetModel();
    }
    
	/**
	 * Create a model from OBJ data
	 * @param filename path/name of the OBJ file data
	 * @param scale the target radius to which we want the model scaled, or <0 if no scaling should be done
	 * @param generateNormals indicate whether per-vertex normals should be estimated and added
	 * @param generateTangents indicate whether per-vertex tangent vectors should be estimated and added
	 * @param vertMergeThreshold the distance between vertices that should be considered "the same" and allow for merging
	 * @param normMergeThreshold the distance between normals that should be considered "the same" and allow for merging
	 * @param initialVertCount the scaling of the internal structures for expected vertex count
	 * @return a new model
	 * @throws IOException 
	 */
	public static NvModelExtObj Create(String filename, float scale,
		boolean generateNormals, boolean generateTangents,
		float vertMergeThreshold /*= 0.01f*/, float normMergeThreshold /*= 0.001f*/, int initialVertCount/* = 3000*/) throws IOException{
		NvModelExtObj pModel = new NvModelExtObj(vertMergeThreshold, normMergeThreshold, initialVertCount);
		pModel.LoadObjFromFile(filename);
		pModel.RescaleToOrigin(scale);
		if (generateNormals)
			pModel.GenerateNormals();

		if (generateTangents)
			pModel.GenerateTangents();

		for (int i = 0; i < pModel.m_subMeshes.size(); i++) {
			pModel.InitProcessedVerts(i);
			pModel.InitProcessedIndices(i);
		}

		return pModel;
	}
	
	/**
	 * Loads the model data from the OBJ file with the given file name
	 * @param fileName Name of the file containing the OBJ definition to load
	 * @throws IOException
	 */
    public void LoadObjFromFile(String fileName)throws IOException{
    	if (null == ms_pLoader)
        {
            throw new NullPointerException("ms_pLoader is null");
        }

        // Use the provided loader callback to load the file into memory
        byte[] pData = ms_pLoader.loadDataFromFile(fileName);
        LoadObjFromMemory(pData);

        // Free the OBJ buffer
//        ms_pLoader.ReleaseData(pData);

//        return result;
    }

    /// 
    /// \param[in] pLoadData 
    /// \return True if the OBJ was parsed successfully and the model now contains
    ///         the processed data.  False if there was a problem processing the data.
    /**
     * Loads the model data from the OBJ file in the given memory buffer
     * @param pLoadData Pointer to the buffer containing the OBJ definition to load
     * @throws IOException 
     */
    public void LoadObjFromMemory(byte[] pLoadData) throws IOException{
    	NvTokenizer tok = new NvTokenizer(new String(pLoadData), "/");
    	
    	int currentMaterial = 0;
    	int currentSmoothingGroup = 0;
		SubMeshObj currentSubMesh = GetSubMeshForMaterial(0);

        boolean bHas4CompPos = false;
        boolean bHas3CompTex = false;
        boolean bBoundingBoxInitialized = false;
        int nextPosIndex = 0;
        int nextNormalIndex = 0;
        int nextTexCoordIndex = 0;
        
        final float[] pos = new float[4];
        //normal, 3 components
        final float[] norm =new float[3];
        final int[] index0 = new int[1];
        
        while (!tok.atEOF())
        {
            if (!tok.readToken())
            {
                tok.consumeToEOL();
                continue; // likely EOL we didn't explicitly handle?
            }

//            const char* tmp = tok.getLastTokenPtr();
            String tmp = tok.getLastToken();
            int compCount = 0;

            switch (tmp.charAt(0))
            {
            case '#':
            {
                //comment line, eat the remainder
                tok.consumeToEOL();
                break;
            }
            case 'v':
            {
                // Some kind of vertex component
            	if(tmp.length() > 1)
            	{
            		switch (tmp.charAt(1))
                    {
	                    case 'n':
	                    {
	                        compCount = tok.getTokenFloatArray(norm);
	                        assert(compCount == 3);
	                        int index = m_normals.Append(new Vector3f().load(norm, 0));
	                        assert(index != -1);
	                        ++nextNormalIndex;
	                        break;
	                    }
	                    case 't':
	                    {
	                        //texcoord, 2 or 3 components
	//                        nv::vec3f texCoord;
	                    	float[] texCoord = norm;
	                        texCoord[2] = 0.0f;  //default r coordinate
	                        compCount = tok.getTokenFloatArray(texCoord);
	                        assert(compCount > 1 && compCount < 4);
	                        if (compCount == 3)
	                        {
	                            // Often, the file will contain 3 texture coordinates, but the third is all 0s
	                            // Attempt to detect this case and reduce our texture coordinate size of possible.
	                            if ((texCoord[2] > -0.0001f) && (texCoord[2] < 0.0001f))
	                            {
	                                compCount = 2;
	                            }
	                        }
	                        int index = m_texCoords.Append(new Vector3f().load(texCoord, 0));
	                        assert(index != -1);
	                        ++nextTexCoordIndex;
	                        bHas3CompTex |= (compCount == 3);
	                        break;
	                    }
	                    case 'p':
	                    {
	                        // Parameter space vertices not supported...
	                        break;
	                    }
                    }
            	}else{
            		//vertex position, 3 or 4 components
                    pos[3] = 1.0f;  //default w coordinate

                    compCount = tok.getTokenFloatArray(pos);
                    assert(compCount > 2 && compCount < 5);
                    if (bBoundingBoxInitialized)
                    {
                        // Grow our bounding box, if necessary
//                        m_boundingBoxMin = Math.min(m_boundingBoxMin, (nv::vec3f)pos);
//                        m_boundingBoxMax = nv::max(m_boundingBoxMax, (nv::vec3f)pos);
                    	m_boundingBoxMin.x = Math.min(m_boundingBoxMin.x, pos[0]);
                    	m_boundingBoxMin.y = Math.min(m_boundingBoxMin.y, pos[1]);
                    	m_boundingBoxMin.z = Math.min(m_boundingBoxMin.z, pos[2]);
                    	
                    	m_boundingBoxMax.x = Math.max(m_boundingBoxMax.x, pos[0]);
                    	m_boundingBoxMax.y = Math.max(m_boundingBoxMax.y, pos[1]);
                    	m_boundingBoxMax.z = Math.max(m_boundingBoxMax.z, pos[2]);
                    }
                    else
                    {
                        // Make sure that our bounding box starts out with a valid, contained point
                        m_boundingBoxMin.load(pos, 0);
                        m_boundingBoxMax.load(pos, 0);
                        bBoundingBoxInitialized = true;
                    }

                    int index = m_positions.Append(new Vector4f().load(pos, 0));
                    assert(index != -1);
                    ++nextPosIndex;
                    bHas4CompPos |= (compCount == 4);
                    break;
            	}
                
                tok.consumeToEOL();
                break;
            }
            case 'f':
            {
                //face
                MeshFace face = new MeshFace();
                MeshVertex vert = new MeshVertex();

                face.m_material = (currentMaterial);
                face.m_smoothingGroup = currentSmoothingGroup;
                face.m_pSubMesh = currentSubMesh;

                // determine the type, and read the initial vertex, all entries in a face must have the same format
                // formats are:
                // 1  #         : Position Only
                // 2  #/#       : Position and TexCoord
                // 3  #/#/#     : Position, TexCoord and Normal
                // 4  #//#      : Position and Normal
                int format = Face_Invalid;

                // Some obj files have malformed face entries that contain a trailing delimiter when 
                // defining Position and TexCoord vertices.  We'll check for this case so that we
                // can consume the extra delimiter and try to recover
                boolean bTrailingDelimiter = false;

                // Indices in OBJ files are 1-based and may be absolute or relative.
                // To facilitate this, we'll read indices into a temporary value that 
                // can be remapped before we store it into our actual data structures.
                int objIndex;
                if (!tok.getTokenInt(index0))
                {
                    assert(false);
                    return;
                }
                objIndex = index0[0]; 
                

                // Remap from the files 1-based, and possibly negative, value to a non-negative,
                // 0-based index, then use that index to retrieve the correct index from the 
                // compacting data store of positions.  We'll do this for all vertex components
                // that we read in from the file.
                vert.m_pos = m_positions.Remap(RemapObjIndex(objIndex, nextPosIndex));

                if (tok.consumeOneDelim() != 0)
                {
                    if (tok.consumeOneDelim() != 0)
                    {
                        // Two delimiters in a row means it has to be format #//#
                        format = Face_PosNormal;

                        // and we need to read in the normal
                        if (!tok.getTokenInt(index0))
                        {
                            throw new AssertionError();
                        }
                        
                        objIndex = index0[0];
                        vert.m_normal = m_normals.Remap(RemapObjIndex(objIndex, nextNormalIndex));
                    }
                    else
                    {
                        // The next token is the texture coordinate
                        if (!tok.getTokenInt(index0))
                        {
                        	throw new AssertionError();
                        }

                        objIndex = index0[0];
                        vert.m_texcoord = m_texCoords.Remap(RemapObjIndex(objIndex, nextTexCoordIndex));

                        // If there's a delimiter following this, then there's also a normal
                        if (tok.consumeOneDelim() != 0)
                        {
                            // Therefore, format #/#/# and we need to fetch the normal
                            // Also, check for malformed face with a trailing delimiter by not consuming
                            // whitespace when reading this token (some bad OBJ files have a #/#/ format
                            // which is not standard).
                            tok.setConsumeWS(false);
                            if (tok.getTokenInt(index0))
                            {
                            	objIndex = index0[0];
                                vert.m_normal = m_normals.Remap(RemapObjIndex(objIndex, nextNormalIndex));
                                format = Face_PosTexNormal;
                            }
                            else
                            {
                                // Set our format to correctly reflect the actual data in the face definition,
                                // but also set our flag so we can consume that extra delimiter each time
                                bTrailingDelimiter = true;
                                format = Face_PosTex;
                            }
                            // Restoring our setting to consume whitespace
                            tok.setConsumeWS(true);
                        }
                        else
                        {
                            // otherwise it's format #/#, so no normal
                            format = Face_PosTex;
                        }
                    }
                }
                else
                {
                    // A single token and no delimiters means a position only format
                    format = Face_PosOnly;
                }

                // Add our new vertex to the submesh, checking to see if one
                // just like it has already been added, using the original one
                // instead if so.
                face.m_verts[0] = currentSubMesh.FindOrAddVertex(vert);
                switch (format)
                {
                case Face_PosOnly:
                {
                    // Get the second vertex
                    if (!tok.getTokenInt(index0))
                    {
                    	throw new AssertionError();
                    }
                    
                    objIndex = index0[0];
                    vert.m_pos = m_positions.Remap(RemapObjIndex(objIndex, nextPosIndex));
                    face.m_verts[1] = currentSubMesh.FindOrAddVertex(vert);

                    // If there are more than three vertices in this face, then we need to create
                    // a triangle fan.  If there are only three vertices, then it will be a fan of
                    // one triangle.
                    boolean bGeneratedFaceNormal = false;
                    while (tok.getTokenInt(index0))
                    {
                    	objIndex = index0[0];
                        vert.m_pos = m_positions.Remap(RemapObjIndex(objIndex, nextPosIndex));
                        face.m_verts[2] = currentSubMesh.FindOrAddVertex(vert);

                        // Generate our face normal.  We only need to do this once and reuse
                        // it for all triangles in the face.
                        if (!bGeneratedFaceNormal)
                        {
                            face.CalculateFaceNormal(m_positions.GetVectors());
                            bGeneratedFaceNormal = true;
                        }

                        currentSubMesh.m_rawFaces.add(face);
                        face = new MeshFace(face);

                        // Move this vertex into the second position so that the next vertex,
                        // if there is one, will create a triangle with the first vertex, this 
                        // vertex and the next one.
                        face.m_verts[1] = face.m_verts[2];
                    }
                    break;
                }
                case Face_PosTex:
                {
                    // Get the second vertex
                    if (!tok.getTokenInt(index0))
                    {
                    	throw new AssertionError();
                    }
                    
                    objIndex = index0[0];
                    vert.m_pos = m_positions.Remap(RemapObjIndex(objIndex, nextPosIndex));

                    // Consume the '/' between the position and texture
                    if (tok.consumeOneDelim() == 0)
                    {
                    	throw new AssertionError();
                    }

                    if (!tok.getTokenInt(index0))
                    {
                    	throw new AssertionError();
                    }
                    objIndex = index0[0];
                    vert.m_texcoord = m_texCoords.Remap(RemapObjIndex(objIndex, nextTexCoordIndex));

                    if (bTrailingDelimiter)
                    {
                        if (tok.consumeOneDelim() == 0)
                        {
                        	throw new AssertionError();
                        }
                    }
                    face.m_verts[1] = currentSubMesh.FindOrAddVertex(vert);

                    // If there are more than three vertices in this face, then we need to create
                    // a triangle fan.  If there are only three vertices, then it will be a fan of
                    // one triangle.
                    boolean bGeneratedFaceNormal = false;
                    while (tok.getTokenInt(index0))
                    {
                    	objIndex = index0[0];
                        vert.m_pos = m_positions.Remap(RemapObjIndex(objIndex, nextPosIndex));

                        // Consume the '/' between the position and texture
                        if (tok.consumeOneDelim() == 0)
                        {
                        	throw new AssertionError();
                        }

                        if (!tok.getTokenInt(index0))
                        {
                        	throw new AssertionError();
                        }
                        objIndex = index0[0];
                        vert.m_texcoord = m_texCoords.Remap(RemapObjIndex(objIndex, nextTexCoordIndex));
                        face.m_verts[2] = currentSubMesh.FindOrAddVertex(vert);

                        // Generate our face normal.  We only need to do this once and reuse
                        // it for all triangles in the face.
                        if (!bGeneratedFaceNormal)
                        {
                            face.CalculateFaceNormal(m_positions.GetVectors());
                            bGeneratedFaceNormal = true;
                        }

                        currentSubMesh.m_rawFaces.add(face);
                        face = new MeshFace(face);

                        // Move this vertex into the second position so that the next vertex,
                        // if there is one, will create a triangle with the first vertex, this 
                        // vertex and the next one.
                        face.m_verts[1] = face.m_verts[2];
                    }
                    break;
                }
                case Face_PosTexNormal:
                {
                    // Get the second vertex
                    if (!tok.getTokenInt(index0))
                    {
                    	throw new AssertionError();
                    }
                    
                    objIndex = index0[0];
                    vert.m_pos = m_positions.Remap(RemapObjIndex(objIndex, nextPosIndex));

                    // Consume the '/' between the position and texture
                    if (tok.consumeOneDelim() == 0)
                    {
                    	throw new AssertionError();
                    }

                    if (!tok.getTokenInt(index0))
                    {
                    	throw new AssertionError();
                    }
                    objIndex = index0[0];
                    vert.m_texcoord = m_texCoords.Remap(RemapObjIndex(objIndex, nextTexCoordIndex));

                    // Consume the '/' between the texture and the normal
                    if (tok.consumeOneDelim() == 0)
                    {
                    	throw new AssertionError();
                    }

                    if (!tok.getTokenInt(index0))
                    {
                    	throw new AssertionError();
                    }
                    objIndex = index0[0];
                    vert.m_normal = m_normals.Remap(RemapObjIndex(objIndex, nextNormalIndex));
                    face.m_verts[1] = currentSubMesh.FindOrAddVertex(vert);

                    // If there are more than three vertices in this face, then we need to create
                    // a triangle fan.  If there are only three vertices, then it will be a fan of
                    // one triangle.
                    while (tok.getTokenInt(index0))
                    {
                    	objIndex = index0[0];
                        vert.m_pos = m_positions.Remap(RemapObjIndex(objIndex, nextPosIndex));

                        // Consume the '/' between the position and texture
                        if (tok.consumeOneDelim() == 0)
                        {
                        	throw new AssertionError();
                        }

                        if (!tok.getTokenInt(index0))
                        {
                        	throw new AssertionError();
                        }
                        objIndex = index0[0];
                        vert.m_texcoord = m_texCoords.Remap(RemapObjIndex(objIndex, nextTexCoordIndex));

                        // Consume the '/' between the texture and the normal
                        if (tok.consumeOneDelim() == 0)
                        {
                        	throw new AssertionError();
                        }

                        if (!tok.getTokenInt(index0))
                        {
                        	throw new AssertionError();
                        }
                        objIndex = index0[0];
                        vert.m_normal = m_normals.Remap(RemapObjIndex(objIndex, nextNormalIndex));
                        face.m_verts[2] = currentSubMesh.FindOrAddVertex(vert);

                        currentSubMesh.m_rawFaces.add(face);
                        face = new MeshFace(face);

                        // Move this vertex into the second position so that the next vertex,
                        // if there is one, will create a triangle with the first vertex, this 
                        // vertex and the next one.
                        face.m_verts[1] = face.m_verts[2];
                    }
                    break;
                }
                case Face_PosNormal:
                {
                    // Get the second vertex
                    if (!tok.getTokenInt(index0))
                    {
                    	throw new AssertionError();
                    }
                    objIndex = index0[0];
                    vert.m_pos = m_positions.Remap(RemapObjIndex(objIndex, nextPosIndex));

                    // Consume the '//' between the position and normal indices
                    if (tok.consumeOneDelim() == 0 || tok.consumeOneDelim() == 0)
                    {
                    	throw new AssertionError();
                    }

                    if (!tok.getTokenInt(index0))
                    {
                    	throw new AssertionError();
                    }
                    objIndex = index0[0];
                    vert.m_normal = m_normals.Remap(RemapObjIndex(objIndex, nextNormalIndex));
                    face.m_verts[1] = currentSubMesh.FindOrAddVertex(vert);

                    // If there are more than three vertices in this face, then we need to create
                    // a triangle fan.  If there are only three vertices, then it will be a fan with
                    // one triangle.
                    while (tok.getTokenInt(index0))
                    {
                    	objIndex = index0[0];
                        vert.m_pos = m_positions.Remap(RemapObjIndex(objIndex, nextPosIndex));
                        assert(RemapObjIndex(objIndex, nextPosIndex) == (objIndex - 1));

                        // Consume the '//' between the position and normal indices
                        if (tok.consumeOneDelim()==0 || tok.consumeOneDelim()==0)
                        {
                        	throw new AssertionError();
                        }

                        if (!tok.getTokenInt(index0))
                        {
                        	throw new AssertionError();
                        }
                        objIndex = index0[0];
                        vert.m_normal = m_normals.Remap(RemapObjIndex(objIndex, nextNormalIndex));
                        face.m_verts[2] = currentSubMesh.FindOrAddVertex(vert);

                        currentSubMesh.m_rawFaces.add(face);
                        face = new MeshFace(face);

                        // Move this vertex into the second position so that the next vertex,
                        // if there is one, will create a triangle with the first vertex, this 
                        // vertex and the next one.
                        face.m_verts[1] = face.m_verts[2];
                    }
                    break;
                }
                default:
                {
                	throw new AssertionError();
                }
                }
                tok.consumeToEOL();
                break;
            }
            case 's':
                if (!tok.getTokenInt(index0)) // should return 0 if no conversion possible, as in the case of the 'off' setting
                {
                	throw new AssertionError();
                }
                tok.consumeToEOL();
/*
#if DEBUG_SMOOTHING_AS_MATS
                {
                    // See if the material already exists
                    char tmp[16];
                    sprintf_s(tmp, 16, "%d", currentSmoothingGroup);
                    std::string materialName = tmp;

                    MaterialMap::iterator fIt = m_materialMap.find(materialName);
                    if (fIt == m_materialMap.end())
                    {
                        // We need to add the material
                        currentMaterial = m_rawMaterials.size();
                        m_materialMap[materialName] = currentMaterial;

                        Material newMaterial;
                        newMaterial.m_name = materialName;
                        newMaterial.m_diffuse = sColors[currentMaterial % sColorCount];
                        m_rawMaterials.push_back(newMaterial);
                    }
                    else
                    {
                        currentMaterial = fIt.second;
                    }
                    currentSubMesh = GetSubMeshForMaterial(currentMaterial);
                }
#endif
*/
                break;
            case 'm':
            {
            	/*
#if DEBUG_SMOOTHING_AS_MATS
                // Using artificially constructed materials, defined by smoothing groups, so 
                // ignore all real materials
                tok.consumeToEOL();
                break; 
#endif
*/
                // mtllib
                String materialLibName;
                if ((materialLibName = tok.getTokenString()) == null)
                {
                	throw new AssertionError();
                }

                // Load the material library so that subsequent faces can use the materials it defines
                LoadMaterialLibraryFromFile(materialLibName);

                tok.consumeToEOL();
                break;
            }
            case 'u':
            {
            	/*
#if DEBUG_SMOOTHING_AS_MATS
                tok.consumeToEOL();
                break;
#endif
*/
                // usemtl
                String materialName;
                if ((materialName = tok.getTokenString()) == null)
                {
                	throw new AssertionError();
                }

                // See if the material already exists
                Integer fIt = m_materialMap.get(materialName);
                if (fIt == null)
                {
                    // We need to add the material
                    currentMaterial = m_rawMaterials.size();
//                    m_materialMap[materialName] = currentMaterial;
                    m_materialMap.put(materialName, currentMaterial);

                    Material newMaterial = new Material();
                    m_rawMaterials.add(newMaterial);
                }
                else
                {
                    currentMaterial = fIt;
                }

                // Switch to the submesh that uses the active material
                currentSubMesh = GetSubMeshForMaterial(currentMaterial);
                tok.consumeToEOL();
                break;
            }
            case 'g':
            case 'o':
                //all presently ignored
            default:
                tok.consumeToEOL();
            }
        }

        m_numPositionComponents = bHas4CompPos ? 4 : 3;
        m_numTexCoordComponents = bHas3CompTex ? 3 : 2;

        OptimizeModel();
    }
	
	@Override
	public int GetMeshCount() { return m_subMeshes.size();}

	@Override
	public SubMesh GetSubMesh(int subMeshID) {
		return GetSubMeshObj(subMeshID);
	}
	
	public SubMeshObj GetSubMeshObj(int subMeshID){
		try {
			return m_subMeshes.get(subMeshID);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public int GetMaterialCount() { return m_rawMaterials.size();}

	@Override
	public Material GetMaterial(int materialID) {
		try {
			return m_rawMaterials.get(materialID);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public int GetTextureCount() { return m_textures.size();}

	@Override
	public String GetTextureName(int textureID) {
		try {
			return m_textures.get(textureID);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}
	
	/**
	 * Modifies all positions in the model so that the model is centered about the origin and uniformly scaled such that the largest extent (distance between
	 * the center and the side) is equal to the given radius.
	 * @param radius New largest extent of the mesh's bounding box
	 */
	public void RescaleToOrigin(float radius){
		Vector3f r = Vector3f.sub(m_boundingBoxMax, m_boundingBoxMin, null);
		r.scale(0.5f);
		
		Vector4f center = new Vector4f();
		Vector3f.add(m_boundingBoxMin, r, center);
		
		float oldRadius = Math.max(r.x, Math.max(r.y, r.z));
        float scale = radius / oldRadius;

        m_positions.RescaleToOrigin(scale, center);
	}

	/**
	 * Iterates over all vertices and generates normals by averaging the normals of all
     * faces, within the same smoothing group, that contain the vertex.  Any faces within
     * smoothing group 0 will not have their vertices' normals averaged, but will have their
     * face normal used for their vertices instead.
	 */
	public void GenerateNormals(){
		if (m_normals.GetVectorCount() > 0)
        {
            // Normals already exist; no need to generate
            return;
        }

        // Every position will generate at least one normal, but possibly more,
        // depending on smoothing groups and other discontinuities.
        // Start by building up a mapping from each position to every 
        // Face/Vertex that references it, divided by smoothing group.
        int numPositions = m_positions.GetVectorCount();
        m_normals.Reserve(numPositions);  // Avoid re-allocations the best we can

//        VertexFaces vertexReferences;
//        vertexReferences.resize(numPositions);
        @SuppressWarnings("unchecked")
		ArrayList<ReferringFaceGroup>[] vertexReferences = (ArrayList<ReferringFaceGroup>[]) new ArrayList<?>[numPositions];
        for(int i = 0; i < numPositions; i++){
        	vertexReferences[i] = new ArrayList<>();
        }
        
        // Iterate over all submeshes and process their faces, adding references to each
        // vertex that the face refers to
//        std::vector<SubMeshObj*>::const_iterator smEnd = m_subMeshes.end();
//		for (std::vector<SubMeshObj*>::iterator smIt = m_subMeshes.begin(); smIt != smEnd; ++smIt)
        for(SubMeshObj pSubMesh : m_subMeshes)
        {
//			SubMeshObj* pSubMesh = *smIt;
        	ArrayList<MeshFace> faces = pSubMesh.m_rawFaces;

//            std::vector<MeshFace>::const_iterator faceEnd = faces.end();
//            for (std::vector<MeshFace>::iterator faceIt = faces.begin(); faceIt != faceEnd; ++faceIt)
        	for(MeshFace pFace : faces)
            {
//                MeshFace* pFace = &(*faceIt);
                for (int vIndex = 0; vIndex < 3; ++vIndex)
                {
                    // Get the index of the vertex
                	int posIndex = pSubMesh.m_srcVertices.get(pFace.m_verts[vIndex]).m_pos;
                    assert(posIndex < numPositions);

                    // Find the face group for the smoothing group that the face belongs to, creating
                    // a new one if one doesn't already exist.
                    ArrayList<ReferringFaceGroup> faceGroups = vertexReferences[posIndex];
                    ReferringFaceGroup pGroup = null;

//                    FaceGroups::const_iterator faceGroupEnd = faceGroups.end();
//                    for (FaceGroups::iterator faceGroupIt = faceGroups.begin(); faceGroupIt != faceGroupEnd; ++faceGroupIt)
                    for(ReferringFaceGroup faceGroupIt : faceGroups)
                    {
                        if (faceGroupIt.m_smoothingGroup == pFace.m_smoothingGroup)
                        {
                            // found one
                            pGroup = faceGroupIt;
                            break;
                        }
                    }
                    if (pGroup == null)
                    {
//                        faceGroups.resize(faceGroups.size() + 1);
//                        pGroup = &(faceGroups.back());
//                       
                    	pGroup = new ReferringFaceGroup();
                    	pGroup.m_smoothingGroup = pFace.m_smoothingGroup;
                    	faceGroups.add(pGroup);
                    }

                    pGroup.m_referringFaces.add(new Pair<>(pFace, vIndex));
                }
            }
        }

        // Mapping of positions to MeshVertex is complete.  Now iterate over all of these mappings
        // and generate normals, associating them with the appropriate MeshVertices.
        final List<Vector4f> positions = m_positions.GetVectors();

//        VertexFaces::const_iterator vfEnd = vertexReferences.end();
//        for (VertexFaces::iterator vfIt = vertexReferences.begin(); vfIt != vfEnd; ++vfIt)
        for(ArrayList<ReferringFaceGroup> faceGroups : vertexReferences)
        {
//            FaceGroups& faceGroups = (*vfIt);
//            FaceGroups::const_iterator fgEnd = faceGroups.end();
//            for (FaceGroups::iterator fgIt = faceGroups.begin(); fgIt != fgEnd; ++fgIt)
        	for(ReferringFaceGroup group : faceGroups)
            {
                // For each smoothing group of faces, we need to generate an averaged normal, except for
                // smoothing group 0, which is the "smoothing off" group
//                ReferringFaceGroup& group = (*fgIt);
                if (group.m_smoothingGroup > 0)
                {
//                    nv::vec3f normal(0.0f, 0.0f, 0.0f);
                	final Vector3f normal = new Vector3f();

                    ArrayList<Pair<MeshFace, Integer>> faces = group.m_referringFaces;
//                    ReferringFaceGroup::FaceVerts::const_iterator fvEnd = faces.end();
                    if (faces.size() == 1)
                    {
                        // Only one face in this group, so just re-use its face normal
//                        normal = faces.front().first.m_faceNormal;
                    	normal.set(faces.get(0).first.m_faceNormal);
                    }
                    else
                    {
                        // Sum up the face normals of each face, where each is weighted by an appropriate factor
//                        for (ReferringFaceGroup::FaceVerts::iterator fvIt = faces.begin(); fvIt != fvEnd; ++fvIt)
                    	for(Pair<MeshFace, Integer> fvIt : faces)
                        {
                            MeshFace pFace = fvIt.first;
//                            NV_ASSERT(NULL != pFace);
//                            normal += (pFace.GetFaceWeight(positions, fvIt.second) * pFace.m_faceNormal);
                            Vector3f.linear(normal, pFace.m_faceNormal, pFace.GetFaceWeight(positions, fvIt.second), normal);
                        }
                    }

                    // Unitize the normal and add it to the shared list
                    float normalLen = normal.length();
                    if (normalLen > 0.0000001)
                    {
                        normal.scale(1.0f / normalLen);
                    }
                    else
                    {
                        // It's a zero-vector, so give it a default value in the positive y so that it's usable, if not correct
                        normal.y = 1.0f;
                    }
                    int normalIndex = m_normals.Append(normal);

                    // Now point all of those face vertices to their new normal
//                    for (ReferringFaceGroup::FaceVerts::iterator fvIt = faces.begin(); fvIt != fvEnd; ++fvIt)
                    for(Pair<MeshFace, Integer> fvIt : faces)
                    {
                        MeshFace pFace = fvIt.first;
                        assert(null != pFace);
                        SubMeshObj pSubMesh = pFace.m_pSubMesh;
                        assert(null != pSubMesh);
                        int newVertIndex = pSubMesh.SetNormal(pFace.m_verts[fvIt.second], normalIndex);
                        pFace.m_verts[fvIt.second] = newVertIndex;
                    }
                }
                else
                {
                    // For each face, add their face normal to the set of normals and point the vertex to it
//                    ReferringFaceGroup::FaceVerts::const_iterator fvEnd = group.m_referringFaces.end();
//                    for (ReferringFaceGroup::FaceVerts::iterator fvIt = group.m_referringFaces.begin(); fvIt != fvEnd; ++fvIt)
                	for(Pair<MeshFace, Integer> fvIt : group.m_referringFaces)
                    {
                        MeshFace pFace = fvIt.first;
                        assert(null != pFace);
                        SubMeshObj pSubMesh = pFace.m_pSubMesh;
                        assert(null != pSubMesh);
                        int normalIndex = m_normals.Append(pFace.m_faceNormal);
                        int newVertIndex = pSubMesh.SetNormal(pFace.m_verts[fvIt.second], normalIndex);
                        pFace.m_verts[fvIt.second] = newVertIndex;
                    }
                }
            }
        }
	}

    /**
     * Iterates over all vertices and generates tangent vectors for them.  Vertices must
     * have normals and texture coordinates in order to generate tangents.
     */
	public void GenerateTangents(){
		if (m_tangents.GetVectorCount() > 0)
        {
            // Tangents already exist; no need to generate
            return;
        }

        if (m_texCoords.GetVectorCount() == 0)
        {
            // We need texture coordinates to generate tangents
            return;
        }

        if (m_normals.GetVectorCount() == 0)
        {
            // We also need normals to generate tangents
            GenerateNormals();
            if (m_normals.GetVectorCount() == 0)
            {
                // Unable to generate the missing normals for some reason 
                return;
            }
        }

        // Iterate over all the vertices in all the faces in all the submeshes and calculate 
        // tangent vectors for each of them.
//		std::vector<SubMeshObj*>::const_iterator smEnd = m_subMeshes.end();
//		for (std::vector<SubMeshObj*>::iterator smIt = m_subMeshes.begin(); smIt != smEnd; ++smIt)
        for(SubMeshObj pSubMesh : m_subMeshes)
        {
//			SubMeshObj* pSubMesh = *smIt;
            ArrayList<MeshFace> faces = pSubMesh.m_rawFaces;

            if (!(pSubMesh.hasNormals()) || !(pSubMesh.hasTexCoords()))
            {
                // this submesh doesn't have the components that we need to generate tangents
                continue;
            }

//            std::vector<MeshFace>::const_iterator faceEnd = faces.end();
//            for (std::vector<MeshFace>::iterator faceIt = faces.begin(); faceIt != faceEnd; ++faceIt)
            for(MeshFace pFace : faces)
            {
//                MeshFace* pFace = &(*faceIt);
				SubMeshObj _pSubMesh = pFace.m_pSubMesh;
				ArrayList<MeshVertex> verts = _pSubMesh.m_srcVertices;

                // We'll need all three positions and all three UV sets to calculate
                // each tangent, so go ahead and load them all once.
//                nv::vec4f positions[3];
				Vector4f[] positions = new Vector4f[3];
                for(int i = 0; i < positions.length; i++) positions[i] = new Vector4f();
                m_positions.GetObject(verts.get(pFace.m_verts[0]).m_pos, positions[0]);
                m_positions.GetObject(verts.get(pFace.m_verts[1]).m_pos, positions[1]);
                m_positions.GetObject(verts.get(pFace.m_verts[2]).m_pos, positions[2]);

//                nv::vec3f uvs[3];
                Vector3f[] uvs = new Vector3f[3];
                for(int i = 0; i < uvs.length; i++) uvs[i] = new Vector3f();
                m_texCoords.GetObject(verts.get(pFace.m_verts[0]).m_texcoord, uvs[0]);
                m_texCoords.GetObject(verts.get(pFace.m_verts[1]).m_texcoord, uvs[1]);

                for (int vIndex = 0; vIndex < 3; ++vIndex)
                {
                    // Given the current index, determine the index
                    // of the adjacent vertices in the definition of
                    // the face.
                	int nextIndex = (vIndex + 1) % 3;
                	int lastIndex = (vIndex + 2) % 3;

                    final Vector3f tangent = new Vector3f();

                    //compute the edge and tc differentials
//                    nv::vec3f dp0 = (nv::vec3f)(positions[nextIndex] - positions[vIndex]);
//                    nv::vec3f dp1 = (nv::vec3f)(positions[lastIndex] - positions[vIndex]);
//                    nv::vec2f dst0 = (nv::vec2f)(uvs[nextIndex] - uvs[vIndex]);
//                    nv::vec2f dst1 = (nv::vec2f)(uvs[lastIndex] - uvs[vIndex]);
                    Vector3f dp0 = Vector3f.sub(positions[nextIndex], positions[vIndex], null);
                    Vector3f dp1 = Vector3f.sub(positions[lastIndex], positions[vIndex], null);
                    Vector2f dst0 = Vector2f.sub(uvs[nextIndex], uvs[vIndex], null);
                    Vector2f dst1 = Vector2f.sub(uvs[lastIndex], uvs[vIndex], null);
                    
                    // Make sure there's no divide by 0
                    float factor = 1.0f;
                    float denom = dst0.x * dst1.y - dst1.x * dst0.y;
                    if (Math.abs(denom) > 0.00001f)
                    {
                        factor /= denom;
                    }

                    //compute sTangent
                    tangent.x = dp0.x * dst1.y - dp1.x * dst0.y;
                    tangent.y = dp0.y * dst1.y - dp1.y * dst0.y;
                    tangent.z = dp0.z * dst1.y - dp1.z * dst0.y;
                    tangent.scale(factor);
                    float tangentLen = tangent.length();
                    if (tangentLen > 0.000001)
                    {
                        tangent.scale(1.0f/tangentLen);
                    }
                    else
                    {
                        // It's a zero-vector, so give it a default value in the positive x so that it's usable, if not correct
                        tangent.x = 1.0f;

                    }
                    int tangentIndex = m_tangents.Append(tangent);
                    int newVertIndex = _pSubMesh.SetTangent(pFace.m_verts[vIndex], tangentIndex);
                    pFace.m_verts[vIndex] = newVertIndex;
                }
            }
        }
	}

	public boolean InitProcessedIndices(int subMeshID){
		if (subMeshID >= m_subMeshes.size())
			return false;
		SubMeshObj pSubMesh = m_subMeshes.get(subMeshID);
        if (null == pSubMesh)
        {
            return false;
        }

		pSubMesh.m_indexCount = (pSubMesh.m_rawFaces.size()) * 3;

		int[] pBuffer = new int[pSubMesh.m_indexCount];
		int pCurrIndex = 0;
//        std::vector<MeshFace>::iterator faceIt = pSubMesh.m_rawFaces.begin();
//        std::vector<MeshFace>::const_iterator faceEnd = pSubMesh.m_rawFaces.end();
//        for (uint32_t* pCurrIndex = pBuffer; faceIt != faceEnd; ++faceIt)
		for(MeshFace faceIt : pSubMesh.m_rawFaces)
        {
            for (int vIndex = 0; vIndex < 3; ++vIndex, ++pCurrIndex)
            {
//                *pCurrIndex = (*faceIt).m_verts[vIndex];
            	pBuffer[pCurrIndex] = faceIt.m_verts[vIndex];
            }
        }

		pSubMesh.m_indices = pBuffer;
        return true;
	}

	public boolean InitProcessedVerts(int subMeshID){
		if (subMeshID >= m_subMeshes.size())
			return false;
		SubMeshObj pSubMesh = m_subMeshes.get(subMeshID);
		if (null == pSubMesh)
        {
            return false;
        }

		// calculate the vertex size and the vertex offsets
		int vertexSize = 0; // In Floats

		// We will always have positions in our vertex, and we will put 
		// them first in the vertex layout
		vertexSize += 3;

		// Account for normals, if there are any
		pSubMesh.m_normalOffset = -1;
		if (pSubMesh.hasNormals())
		{
			pSubMesh.m_normalOffset = vertexSize;
			vertexSize += 3;
		}

		// Account for texture coordinates, if there are any
		pSubMesh.m_texCoordOffset = -1;
		if (pSubMesh.hasTexCoords())
		{
			pSubMesh.m_texCoordOffset = vertexSize;
			vertexSize += 2;
		}

		// Account for tangents, if there are any
		pSubMesh.m_tangentOffset = -1;
		if (pSubMesh.hasTangents())
		{
			pSubMesh.m_tangentOffset = vertexSize;
			vertexSize += 3;
		}

		pSubMesh.m_vertexCount = pSubMesh.m_srcVertices.size();
		pSubMesh.m_vertSize = vertexSize;

		// Allocate a large enough vertex buffer to hold all vertices in the mesh
		pSubMesh.m_vertices = new float[vertexSize * pSubMesh.m_vertexCount];

//		float* pBuffer = pSubMesh.m_vertices;

        // Get pointers to the proper positions in the first vertex in the buffer
//        float* pCurrPosition = (float*)(pBuffer);
//		float* pCurrNormal = (float*)(pBuffer + pSubMesh.m_normalOffset);
//		float* pCurrTexCoord = (float*)(pBuffer + pSubMesh.m_texCoordOffset);
//		float* pCurrTangent = (float*)(pBuffer + pSubMesh.m_tangentOffset);
		
		int pCurrPosition = 0;
		int pCurrNormal   = pSubMesh.m_normalOffset;
		int pCurrTexCoord = pSubMesh.m_texCoordOffset;
		int pCurrTangent  = pSubMesh.m_tangentOffset;

        List<Vector4f> positions = m_positions.GetVectors();
        List<Vector3f> normals = m_normals.GetVectors();
        List<Vector3f> texCoords = m_texCoords.GetVectors();
        List<Vector3f> tangents = m_tangents.GetVectors();

//        const SubMeshObj::MeshVertexArray& srcVerts = pSubMesh.m_srcVertices;
//		SubMeshObj::MeshVertexArray::const_iterator srcIt = srcVerts.begin();
//		SubMeshObj::MeshVertexArray::const_iterator srcEnd = srcVerts.end();
//        for (; srcIt != srcEnd; ++srcIt)
        for(MeshVertex vert : pSubMesh.m_srcVertices)
        {
//            const MeshVertex& vert = (*srcIt);

//            memcpy(pCurrPosition, &(positions[vert.m_pos]), sizeof(float) * 3);
        	pSubMesh.m_vertices[pCurrPosition + 0] = positions.get(vert.m_pos).x;
        	pSubMesh.m_vertices[pCurrPosition + 1] = positions.get(vert.m_pos).y;
        	pSubMesh.m_vertices[pCurrPosition + 2] = positions.get(vert.m_pos).z;
			pCurrPosition += vertexSize;

			if (pSubMesh.m_normalOffset >= 0)
            {
                assert(-1 != vert.m_normal);
//                memcpy(pCurrNormal, &(normals[vert.m_normal]), sizeof(float) * 3);
                pSubMesh.m_vertices[pCurrNormal + 0] = normals.get(vert.m_normal).x;
            	pSubMesh.m_vertices[pCurrNormal + 1] = normals.get(vert.m_normal).y;
            	pSubMesh.m_vertices[pCurrNormal + 2] = normals.get(vert.m_normal).z;
				pCurrNormal += vertexSize;
            }
			if (pSubMesh.m_texCoordOffset >= 0)
            {
				assert(-1 != vert.m_texcoord);
//                memcpy(pCurrTexCoord, &(texCoords[vert.m_texcoord]), sizeof(float) * 2);
				pSubMesh.m_vertices[pCurrTexCoord + 0] = texCoords.get(vert.m_texcoord).x;
            	pSubMesh.m_vertices[pCurrTexCoord + 1] = texCoords.get(vert.m_texcoord).y;
				pCurrTexCoord += vertexSize;
            }
			if (pSubMesh.m_tangentOffset >= 0)
			{
				assert(-1 != vert.m_tangent);
//				memcpy(pCurrTangent, &(tangents[vert.m_tangent]), sizeof(float) * 3);
				pSubMesh.m_vertices[pCurrTangent + 0] = tangents.get(vert.m_tangent).x;
            	pSubMesh.m_vertices[pCurrTangent + 1] = tangents.get(vert.m_tangent).y;
            	pSubMesh.m_vertices[pCurrTangent + 2] = tangents.get(vert.m_tangent).z;
				pCurrTangent += vertexSize;
			}
		}

        return true;
	}
	
	/**
	 * Method to remap an index in an obj file to the corresponding index in the given vector.<p>
    
    * Indices in obj files are 1-based.  Since we're using 0-based vectors
    * to store data in, we need to remap the file indices to our vector indices
    * by subtracting one.  Additionally, indices can be negative, indicating that
    * the index is relative to the most recently defined object (-1 is the most
    * recent). Note that this remapping is not the same as, or related to, the remapping
    * done by the vector compactors that mesh data is stored in.  This is simply to map
    * from the numbers read from the OBJ file to a non-negative, 0-based index.
	 */
    private static int RemapObjIndex(int objIndex, int count)
    {
        return ((objIndex > 0) ? (objIndex - 1) : (count + objIndex));
    }
    
  /// Returns the submesh in the model that uses the given material.  There will
    /// be exactly one submesh for each material defined in the model.
    /// \param[in] materialID ID (or index) of the material to retrieve the mesh for
    /// \return A pointer to the submesh that uses the material with the given ID, 
    ///         if there is one.  Null if there is no such material definition or
    ///         associated mesh.
    /**
     * 
     * @param materialID
     * @return
     */
    private SubMeshObj GetSubMeshForMaterial(int materialID){
    	for(SubMeshObj mesh : m_subMeshes){
    		if(mesh.m_materialId == materialID){
    			return mesh;
    		}
    	}
    	
    	// No sub mesh found with that material. Create a new one and return it
		SubMeshObj pSubmesh = new SubMeshObj();
        pSubmesh.m_materialId = materialID;

        m_subMeshes.add(pSubmesh);
        return pSubmesh;
    }
    
    /** Creates a default material to be used by any mesh that doesn't specify one */
    private void InitializeDefaultMaterial(){
    	// Initialize our material set and map with a default material
        Material defaultMaterial = new Material();
        m_rawMaterials.add(defaultMaterial);
//		m_materialMap["default"] = 0;
        m_materialMap.put("default", 0);
    }

    // Clears out all data to prepare for loading a new model
    private void ResetModel(){
    	m_positions.Clear();
        m_normals.Clear();
        m_texCoords.Clear();
        m_tangents.Clear();

        m_rawMaterials.clear();
        m_materialMap.clear();
        m_textures.clear();
//        std::vector<SubMeshObj*>::iterator it = m_subMeshes.begin();
//		std::vector<SubMeshObj*>::const_iterator itEnd = m_subMeshes.end();
//        for (; it != itEnd; ++it)
//        {
//            delete (*it);
//        }
        m_subMeshes.clear();

        m_numPositionComponents = 3;
        m_numTexCoordComponents = 2;
        m_boundingBoxMin.set(0.0f, 0.0f, 0.0f);
        m_boundingBoxMax.set(0.0f, 0.0f, 0.0f);

        InitializeDefaultMaterial();
    }

    /// Loads a material library file, using the NvModelFileLoader, included by an OBJ file 
    /// and processes its material definitions into a usable format.
    /// \param[in] libName Name of the file containing the material library definitions
    private void LoadMaterialLibraryFromFile(String libName) throws IOException{
    	if (null == ms_pLoader)
        {
            throw new NullPointerException();
        }

        byte[] pData = ms_pLoader.loadDataFromFile(libName);

        LoadMaterialLibraryFromMemory(pData);
//        ms_pLoader.ReleaseData(pData);
    }
    
    private static int strncmp(String a, String b, int size){
    	if(a.equals(b)){
    		return 0;
    	}
    	
    	if(a.startsWith(b)){
    		return 0;
    	}
    	
    	return 1;
    }
    
    /// Loads a material library file from the provided memory buffer
    /// and processes its material definitions into a usable format.
    /// \param[in] pLoadData Pointer to the memory buffer containing the material library definitions
    private void LoadMaterialLibraryFromMemory(byte[] pLoadData){
    	NvTokenizer tok = new NvTokenizer(new String(pLoadData));
        Material pCurrentMaterial = null;
        final float[] float3 = new float[3];
        final int[] int3 = new int[3];
        
        while (!tok.atEOF())
        {
            if (!tok.readToken())
            {
                tok.consumeToEOL();
                continue; // likely EOL we didn't explicitly handle?
            }

            String tmp = tok.getLastToken();

            switch (tmp.charAt(0))
            {
            case '#':
            {
                //comment line, eat the remainder
                tok.consumeToEOL();
                break;
            }
            case 'n':
            {
                // newmtl
                if (0 != strncmp(tmp, "newmtl", 6))
                {
                    // Unsupported token
                    tok.consumeToEOL();
                    break;
                }

                String materialName;
                if ((materialName = tok.getTokenString()) == null)
                {
                	throw new AssertionError();
                }

                tok.consumeToEOL();

                // See if this is a redefinition of an existing material
                Integer fIt = m_materialMap.get(materialName);
                if (fIt != null)
                {
                    // Existing material
                    int materialId = fIt;
                    assert(materialId < m_rawMaterials.size());
                    pCurrentMaterial = m_rawMaterials.get(materialId);
                    break;
                }

                // New material, so we need to create it before filling it in
                Material newMaterial = new Material();
                int materialId = m_rawMaterials.size();
                m_rawMaterials.add(newMaterial);
                pCurrentMaterial = newMaterial; //&(m_rawMaterials.back());
//                m_materialMap[materialName] = materialId;
                m_materialMap.put(materialName, materialId);
                break;
            }
            case 'K':
            {
                switch (tmp.charAt(1))
                {
                case 'a':
                {
                    // Ambient surface color
                    if (tok.getTokenFloatArray(float3) == 0)
                    {
                    	throw new AssertionError();
                    }
                    
                    pCurrentMaterial.m_ambient.load(float3, 0);
                    tok.consumeToEOL();
                    break;
                }
                case 'd':
                {
                    // Diffuse surface color
                    if (tok.getTokenFloatArray(float3) == 0)
                    {
                    	throw new AssertionError();
                    }
                    pCurrentMaterial.m_diffuse.load(float3, 0);
                    tok.consumeToEOL();
                    break;
                }
                case 's':
                {
                    // Specular surface color
                    if (tok.getTokenFloatArray(float3) == 0)
                    {
                    	throw new AssertionError();
                    }
                    pCurrentMaterial.m_specular.load(float3, 0);
                    tok.consumeToEOL();
                    break;
                }
                case 'e':
                {
                    // Emissive surface color
                    if (tok.getTokenFloatArray(float3) == 0)
                    {
                    	throw new AssertionError();
                    }
                    pCurrentMaterial.m_emissive.load(float3, 0);
                    tok.consumeToEOL();
                    break;
                }
                default:
                {
                    // Unknown color type
                    tok.consumeToEOL();
                    break;
                }
                }
                break;
            }
            case 'N':
            {
                if (tmp.charAt(1) == 'i')
                {
                    // Optical Density
                    if (!tok.getTokenFloat(float3))
                    {
                    	throw new AssertionError();
                    }
                    
                    pCurrentMaterial.m_opticalDensity = float3[0];
                }
                else if (tmp.charAt(1) == 's')
                {
                    // Specular exponent
                    if (!tok.getTokenFloat(float3))
                    {
                    	throw new AssertionError();
                    }
                    pCurrentMaterial.m_shininess = (int)float3[0];
                }
                tok.consumeToEOL();
                break;
            }
            case 'T':
            {
                if (tmp.charAt(1) == 'f')
                {
                    // Transmission Filter
                    if (tok.getTokenFloatArray(float3) == 0)
                    {
                    	throw new AssertionError();
                    }
                    pCurrentMaterial.m_transmissionFilter.load(float3, 0);
                    tok.consumeToEOL();
                    break;
                }

                // Check for the Tr version of alpha/dissolve/Transparent value
                if (tmp.charAt(1) != 'r')
                {
                    tok.consumeToEOL();
                    break;
                }
                // Matches Tr, so fall through and handle the same as 'd'
            }
            case 'd':
            {
                // Dissolve
                if (!tok.getTokenFloat(float3))
                {
                	throw new AssertionError();
                }
                pCurrentMaterial.m_alpha = float3[0];
                tok.consumeToEOL();
                break;
            }
            case 'i':
            {
                // Illlumination model
                if (0 != strncmp(tmp, "illum", 5))
                {
                    // Unsupported line type
                    tok.consumeToEOL();
                    break;
                }
//                int32_t illum;
                if (!tok.getTokenInt(int3))
                {
                	throw new AssertionError();
                }
                pCurrentMaterial.m_illumModel = int3[0];
                tok.consumeToEOL();
                break;
            }
            case 'm':
            {
                NvTextureDesc d = new NvTextureDesc();
                if (0 == strncmp(tmp, "map_Ka", 6))
                {
                    // Ambient Color Texture Map
                    if (ReadTextureLine(tok, d))
                    {
                        pCurrentMaterial.m_ambientTextures.add(d);
                    }
                }
                else if (0 == strncmp(tmp, "map_Kd", 6))
                {
                    // Diffuse Color Texture Map
                    if (ReadTextureLine(tok, d))
                    {
                        pCurrentMaterial.m_diffuseTextures.add(d);
                    }
                }
                else if (0 == strncmp(tmp, "map_Ks", 6))
                {
                    // Specular Color Texture Map
                    if (ReadTextureLine(tok, d))
                    {
                        pCurrentMaterial.m_specularTextures.add(d);
                    }
                }
                else if (0 == strncmp(tmp, "map_bump", 8))
                {
                    // Bump Map
                    if (ReadTextureLine(tok, d))
                    {
                        pCurrentMaterial.m_bumpMapTextures.add(d);
                    }
                }
                else if (0 == strncmp(tmp, "map_Ns", 6))
                {
                    // Specular Power Texture Map
                    if (ReadTextureLine(tok, d))
                    {
                        pCurrentMaterial.m_specularPowerTextures.add(d);
                    }
                }
                else if (0 == strncmp(tmp, "map_d", 5))
                {
                    // Alpha (Dissolve) Texture Map
                    if (ReadTextureLine(tok, d))
                    {
                        pCurrentMaterial.m_alphaMapTextures.add(d);
                    }
                }
                else if (0 == strncmp(tmp, "map_decal", 9))
                {
                    // Decal Texture Map
                    if (ReadTextureLine(tok, d))
                    {
                        pCurrentMaterial.m_decalTextures.add(d);
                    }
                }
                else
                {
                    // Unknown directive
                    tok.consumeToEOL();
                }
                break;
            }
            case 'b':
            {
                if (0 == strncmp(tmp, "bump", 4))
                {
                    NvTextureDesc d = new NvTextureDesc();
                    if (ReadTextureLine(tok, d))
                    {
                        pCurrentMaterial.m_bumpMapTextures.add(d);
                    }
                }
                else
                {
                    tok.consumeToEOL();
                }
                break;
            }
            case 'r':
            {
                if (0 == strncmp(tmp, "refl", 4))
                {
                    NvTextureDesc d = new NvTextureDesc();
                    if (ReadTextureLine(tok, d))
                    {
                        pCurrentMaterial.m_reflectionTextures.add(d);
                    }
                }
                else
                {
                    tok.consumeToEOL();
                }
                break;
            }
            default:
            {
                // Unhandled line
                tok.consumeToEOL();
                break;
            }
            }
        }
    }

    // Performs post-loading optimizations on the mesh to reduce data size
    // and redundancy
    private void OptimizeModel(){
    	RemoveEmptySubmeshes();
    }

    // Removes any meshes that have no geometry
    private void RemoveEmptySubmeshes(){
    	SubMeshObj[] srcMesh = m_subMeshes.toArray(new SubMeshObj[m_subMeshes.size()]);
    	
    	m_subMeshes.clear();
    	for(SubMeshObj mesh : srcMesh){
    		if(!mesh.m_rawFaces.isEmpty()){
    			m_subMeshes.add(mesh);
    		}
    	}
    }

    /// Helper method to read a texture definition line from a material file
    /// and add it to the set of textures
    /// \param[in] tok The tokenizer being used to parse the library, with its current
    ///                 position at the start of the parameters for the texture
    ///                 definition to be read
    /// \param[out] desc Texture descriptor to initialize with read in parameters
    /// \return True if the line was successfully parsed and matched to a texture,
    ///         False if an error occurred.
    private boolean ReadTextureLine(NvTokenizer tok, NvTextureDesc desc){
    	desc.m_textureIndex = -1;
        String texture;
        final float[] float3 = new float[3];
        final int[] int3 = new int[3];

        while (!tok.atEOF())
        {
            if (!tok.readToken())
            {
                tok.consumeToEOL();
                break; // likely EOL we didn't explicitly handle?
            }

            String tmp = tok.getLastToken();

            if (tmp.charAt(0) == '-')
            {
                if (0 == strncmp(tmp, "-blendu", 7))
                {
                    // Eat the on/off option
                    String dummy;
                    if ((dummy = tok.getTokenString()) == null)
                    {
                        throw new AssertionError();
                    }
                }
                else if (0 == strncmp(tmp, "-blendv", 7))
                {
                    // Eat the on/off option
                    String dummy;
                    if ((dummy = tok.getTokenString()) == null)
                    {
                    	throw new AssertionError();
                    }
                }
                else if (0 == strncmp(tmp, "-boost", 6))
                {
                    // Eat the boost value
                    float dummy;
                    if (!tok.getTokenFloat(float3))
                    {
                    	throw new AssertionError();
                    }
                }
                else if (0 == strncmp(tmp, "-mm", 3))
                {
                    // Eat the base and gain values
                    float dummy;
                    if (!tok.getTokenFloat(float3))
                    {
                    	throw new AssertionError();
                    }
                    if (!tok.getTokenFloat(float3))
                    {
                    	throw new AssertionError();
                    }
                }
                else if (0 == strncmp(tmp, "-o", 2))
                {
                    // Eat the origin offset value(s) (there will be 1 to 3)
                    float dummy;
                    // Must be at least 1
                    if (!tok.getTokenFloat(float3))
                    {
                    	throw new AssertionError();
                    }
                    // 2 and 3 are optional
                    if (tok.getTokenFloat(float3))
                    {
                        tok.getTokenFloat(float3);
                    }
                }
                else if (0 == strncmp(tmp, "-s", 2))
                {
                    // Eat the scale value(s) (there will be 1 to 3)
                    float dummy;
                    // Must be at least 1
                    if (!tok.getTokenFloat(float3))
                    {
                    	throw new AssertionError();
                    }
                    // 2 and 3 are optional
                    if (tok.getTokenFloat(float3))
                    {
                        tok.getTokenFloat(float3);
                    }
                }
                else if (0 == strncmp(tmp, "-t", 2))
                {
                    // Eat the Turbulence value(s) (there will be 1 to 3)
                    float dummy;
                    // Must be at least 1
                    if (!tok.getTokenFloat(float3))
                    {
                    	throw new AssertionError();
                    }
                    // 2 and 3 are optional
                    if (tok.getTokenFloat(float3))
                    {
                        tok.getTokenFloat(float3);
                    }
                }
                else if (0 == strncmp(tmp, "-texres", 7))
                {
                    // Eat the resolution option
                    String dummy;
                    if ((dummy = tok.getTokenString()) == null)
                    {
                    	throw new AssertionError();
                    }
                }
                else if (0 == strncmp(tmp, "-clamp", 6))
                {
                    // Eat the on/off option
                    String clamping;
                    if ((clamping = tok.getTokenString()) == null)
                    {
                    	throw new AssertionError();
                    }
                    if (clamping.equals("on"))
                    {
                        desc.m_mapModes[0] = desc.m_mapModes[1] = desc.m_mapModes[2] = NvTextureDesc.MapMode_Clamp;
                    }
                }
                else if (0 == strncmp(tmp, "-bm", 3))
                {
                    // Eat the bump multiplier value
                    float dummy;
                    if (!tok.getTokenFloat(float3))
                    {
                    	throw new AssertionError();
                    }
                }
                else if (0 == strncmp(tmp, "-imfchan", 8))
                {
                    // Eat the channel option to create a scalar texture from
//                    std::string dummy;
                    if (tok.getTokenString() == null)
                    {
                    	throw new AssertionError();
                    }
                }
                else if (0 == strncmp(tmp, "-type", 5))
                {
                    // Eat the texture type option 
//                    std::string dummy;
                    if (tok.getTokenString() == null)
                    {
                    	throw new AssertionError();
                    }
                }
            }
            else
            {
                // not an option that we can parse. Could it be a filename (if we don't already have one)?
                if (desc.m_textureIndex == -1)
                {
                    int len = tmp.length();
                    if (len >= 4)
                    {
                        // There are at least enough characters to hold a file extension
                        if (tmp.charAt(len - 4) == '.')
                        {
                            // Fourth character from the end is a period, so a definite possibility.
                            // the other likely option is a floating point value, so try to filter those out
                            int i = 3;
                            while (i > 0)
                            {
                                int digit = tmp.charAt(len - i) - '0';
                                if ((digit >= 0) && (digit <= 9))
                                {
                                    // Found a number, so probably not our file name
                                    break;
                                }
                                --i;
                            }
                            if (i == 0)
                            {
                                // We didn't find a digit, so use this as our filename
                                texture = tmp;
                                for (int index = 0; index < m_textures.size(); ++index)
                                {
                                    if (m_textures.get(index).equals(texture))
                                    {
                                        desc.m_textureIndex = index;
                                        break;
                                    }
                                }
                                if (desc.m_textureIndex == -1)
                                {
                                    // No matching texture found, so this is a new one.
                                    desc.m_textureIndex = m_textures.size();
                                    m_textures.add(texture);
                                }
                            }
                        }
                    }
                }
            }
        }

        return desc.m_textureIndex != -1;
    }

    // Set of faces within a single smoothing group that refer to a vertex
    private final class ReferringFaceGroup{
    	int m_smoothingGroup;
    	final ArrayList<Pair<MeshFace, Integer>> m_referringFaces = new ArrayList<>();
    }
}

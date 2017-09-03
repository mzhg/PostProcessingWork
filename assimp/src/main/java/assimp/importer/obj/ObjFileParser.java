package assimp.importer.obj;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import assimp.common.AssUtil;
import assimp.common.AssimpConfig;
import assimp.common.DefaultLogger;
import assimp.common.FileUtils;
import assimp.common.Material;
import assimp.common.MemoryUtil;
import assimp.common.Mesh;

/** Parser for a obj waveform file */
/*public*/ final class ObjFileParser extends ObjTools{

	static final String DEFAULT_MATERIAL = Material.AI_DEFAULT_MATERIAL_NAME;
	Model m_pModel;
	
	/*public*/ ObjFileParser(ByteBuffer data, String strModelName) {
		super(data);
		
		// Create the model instance to store all the data
		m_pModel = new Model();
		m_pModel.m_ModelName = strModelName;
		m_DataItEnd = data.remaining();
		
	    // create default material and store it
		m_pModel.m_pDefaultMaterial = new ObjMaterial();
		m_pModel.m_pDefaultMaterial.materialName = DEFAULT_MATERIAL;
	    m_pModel.m_MaterialLib.add( DEFAULT_MATERIAL );
		m_pModel.m_MaterialMap.put(DEFAULT_MATERIAL, m_pModel.m_pDefaultMaterial);
		
		// Start parsing the file
		parseFile();
	}
	
	///	Parse the loaded file
	void parseFile(){
		if (m_DataIt == m_DataItEnd)
			return;
		
		while(m_DataIt != m_DataItEnd){
			byte b = data.get(m_DataIt);
			switch (b) {
			case 'v':
				m_DataIt++;
				int v = data.get(m_DataIt);
				if (v == ' ' || v == '\t') {
					if(m_pModel.m_Vertices == null)
						m_pModel.m_Vertices = MemoryUtil.createFloatBuffer(256 * 3, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
					// read in vertex definition
					m_pModel.m_Vertices = getVector3(m_pModel.m_Vertices);
				} else if (v == 't') {
					// read in texture coordinate ( 2D or 3D )
					if(m_pModel.m_TextureCoord == null)
						m_pModel.m_TextureCoord = MemoryUtil.createFloatBuffer(256 * 3, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
                    ++m_DataIt;
                    m_pModel.m_TextureCoord = getVector( m_pModel.m_TextureCoord );
				} else if (v == 'n') {
					// Read in normal vector definition
					if(m_pModel.m_Normals == null)
						m_pModel.m_Normals = MemoryUtil.createFloatBuffer(256 * 3, AssimpConfig.LOADING_USE_NATIVE_MEMORY);
					++m_DataIt;
					m_pModel.m_Normals = getVector3( m_pModel.m_Normals );
				}
				break;

			case 'p': // Parse a face, line or point statement
			case 'l':
			case 'f':
				{
					getFace(b == 'f' ? Mesh.aiPrimitiveType_POLYGON : (b == 'l' 
						? Mesh.aiPrimitiveType_LINE : Mesh.aiPrimitiveType_POINT));
				}
				break;

			case '#': // Parse a comment
				{
					getComment();
				}
				break;

			case 'u': // Parse a material desc. setter
				{
					getMaterialDesc();
				}
				break;

			case 'm': // Parse a material library or merging group ('mg')
				{
					if (data.get(m_DataIt + 1) == 'g')
						getGroupNumberAndResolution();
					else
						getMaterialLib();
				}
				break;

			case 'g': // Parse group name
				{
					getGroupName();
				}
				break;

			case 's': // Parse group number
				{
					getGroupNumber();
				}
				break;

			case 'o': // Parse object name
				{
					getObjectName();
				}
				break;
			
			default:
				{
					m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
				}
				break;
			}
		}
	}
	
	Model getModel() { return m_pModel;}
	
    /// Stores the vector 
	FloatBuffer getVector(FloatBuffer point3d_array ){
		int numComponents = ( 0 );
	    int tmp = ( m_DataIt );
	    while( !isLineEnd(data.get(tmp) ) ) {
	        if( data.get(tmp) == ' ' ) {
	            ++numComponents;
	        }
	        tmp++;
	    }
	    float x, y, z;
	    if( 2 == numComponents ) {
	        copyNextWord( m_buffer, BUFFERSIZE );
	        x = AssUtil.fast_atof( m_buffer );

	        copyNextWord( m_buffer, BUFFERSIZE );
	        y = AssUtil.fast_atof( m_buffer );
	        z = 0.0f;
	    } else if( 3 == numComponents ) {
	        copyNextWord( m_buffer, BUFFERSIZE );
	        x = AssUtil.fast_atof( m_buffer );

	        copyNextWord( m_buffer, BUFFERSIZE );
	        y = AssUtil.fast_atof( m_buffer );

	        copyNextWord( m_buffer, BUFFERSIZE );
	        z = AssUtil.fast_atof( m_buffer );
	    } else {
//	        ai_assert( !"Invalid number of components" );
//	    	throw new RuntimeException("Invalid number of components");
	    	DefaultLogger.error("Invalid number of components");
	    	x = 0;
	    	y =0;
	    	z =0;
	    }
	    point3d_array = MemoryUtil.enlarge(point3d_array, 3);
//	    .push_back( aiVector3D( x, y, z ) );
	    point3d_array.put(x).put(y).put(z);
	    m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
		
		return point3d_array;
	}
	
    ///	Stores the following 3d vector.
	FloatBuffer getVector3(FloatBuffer point3d_array ){
		float x, y, z;
		copyNextWord(m_buffer, BUFFERSIZE);
		x = AssUtil.fast_atof(m_buffer);	
		
		copyNextWord(m_buffer, BUFFERSIZE);
		y = AssUtil.fast_atof(m_buffer);

	    copyNextWord( m_buffer, BUFFERSIZE );
	    z = AssUtil.fast_atof( m_buffer );

	    point3d_array = MemoryUtil.enlarge(point3d_array, 3);
//		point3d_array.push_back( aiVector3D( x, y, z ) );
	    point3d_array.put(x).put(y).put(z);
		m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
		return point3d_array;
	}
	///	Stores the following 3d vector.
	FloatBuffer getVector2(FloatBuffer point2d_array){
		float x, y;
		copyNextWord(m_buffer, BUFFERSIZE);
		x = AssUtil.fast_atof(m_buffer);
		
		copyNextWord(m_buffer, BUFFERSIZE);
		y = AssUtil.fast_atof(m_buffer);

		point2d_array = MemoryUtil.enlarge(point2d_array, 2);
//		point2d_array.push_back(aiVector2D(x, y));
		point2d_array.put(x).put(y);

		m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
		return point2d_array;
	}
	
	
	
	/** Returns pointer a next token
	 *	@param	pBuffer	Pointer to data buffer
	 *	@param	pEnd	Pointer to end of buffer
	 *	@return	Pointer to next token
	 */
	int getNextToken(int pBuffer, int pEnd){
		while ( !isEndOfBuffer( pBuffer, pEnd ) )
		{
			if ( isSeparator( data.get(pBuffer) ) )
				break;
			pBuffer++;
		}
		return getNextWord(pBuffer, pEnd );
	}
	
    ///	Stores the following face.
	void getFace(int type){
		copyNextLine(m_buffer, BUFFERSIZE);
		if (m_DataIt == m_DataItEnd)
			return;

//		char *pPtr = m_buffer;
//		char *pEnd = &pPtr[BUFFERSIZE];
		int pPtr = 0;
		int pEnd = BUFFERSIZE;
		
		pPtr = getNextToken(m_buffer, pPtr, pEnd);
		if (pPtr == pEnd || m_buffer[pPtr] == '\0')
			return;

		IntList pIndices = new IntArrayList();
		IntList pTexID = new IntArrayList();
		IntList pNormalID = new IntArrayList();
		boolean hasNormal = false;

		final int vSize = m_pModel._getNumVertices();
		final int vtSize = m_pModel._getNumTextureCoords();
		final int vnSize = m_pModel._getNumNormals();

		final boolean vt = m_pModel.m_TextureCoord == null;
		final boolean vn = m_pModel.m_Normals == null;
		int iStep = 0, iPos = 0;
		while (pPtr != pEnd)
		{
			iStep = 1;

			if (isLineEnd(m_buffer[pPtr]))
				break;

			if (m_buffer[pPtr]=='/' )
			{
				if (type == Mesh.aiPrimitiveType_POINT) {
					DefaultLogger.error("Obj: Separator unexpected in point statement");
				}
				if (iPos == 0)
				{
					//if there are no texture coordinates in the file, but normals
					if (!vt && vn) {
						iPos = 1;
						iStep++;
					}
				}
				iPos++;
			}
			else if ( isSeparator(m_buffer[pPtr]) )
			{
				iPos = 0;
			}
			else 
			{
				//OBJ USES 1 Base ARRAYS!!!!
				final int iVal = AssUtil.strtoul10(m_buffer, pPtr, null);  //atoi( pPtr );

				// increment iStep position based off of the sign and # of digits
				int tmp = iVal;
				if (iVal < 0)
				    ++iStep;
				while ( ( tmp = tmp / 10 )!=0 )
					++iStep;

				if ( iVal > 0 )
				{
					// Store parsed index
					if ( 0 == iPos )
					{
						pIndices.add( iVal-1 );
					}
					else if ( 1 == iPos )
					{	
						pTexID.add( iVal-1 );
					}
					else if ( 2 == iPos )
					{
						pNormalID.add( iVal-1 );
						hasNormal = true;
					}
					else
					{
						reportErrorTokenInFace();
					}
				}
				else if ( iVal < 0 )
				{
					// Store relatively index
					if ( 0 == iPos )
					{
						pIndices.add( vSize + iVal );
					}
					else if ( 1 == iPos )
					{
						pTexID.add( vtSize + iVal );
					}
					else if ( 2 == iPos )
					{
						pNormalID.add( vnSize + iVal );
						hasNormal = true;
					}
					else
					{
						reportErrorTokenInFace();
					}
				}
			}
			pPtr += iStep;
		}

		if ( pIndices.isEmpty() ) 
		{
			DefaultLogger.error("Obj: Ignoring empty face");
			m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
			return;
		}

		ObjFace face = new ObjFace( pIndices, pNormalID, pTexID, type );
		
		// Set active material, if one set
		if (null != m_pModel.m_pCurrentMaterial) 
			face.m_pMaterial = m_pModel.m_pCurrentMaterial;
		else 
			face.m_pMaterial = m_pModel.m_pDefaultMaterial;

		// Create a default object, if nothing is there
		if ( null == m_pModel.m_pCurrent )
			createObject( "defaultobject" );
		
		// Assign face to mesh
		if ( null == m_pModel.m_pCurrentMesh )
		{
			createMesh();
		}
		
		// Store the face
		m_pModel.m_pCurrentMesh.m_Faces.add( face );
		m_pModel.m_pCurrentMesh.m_uiNumIndices += face.m_pVertices.size();
		m_pModel.m_pCurrentMesh.m_uiUVCoordinates[ 0 ] += face.m_pTexturCoords.size(); 
		if( !m_pModel.m_pCurrentMesh.m_hasNormals && hasNormal ) 
		{
			m_pModel.m_pCurrentMesh.m_hasNormals = true;
		}
		// Skip the rest of the line
		m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
	}
	
	/// Reads the material description.
    void getMaterialDesc(){
    	// Each material request a new object.
    	// Sometimes the object is already created (see 'o' tag by example), but it is not initialized !
    	// So, we create a new object only if the current on is already initialized !
    	if (m_pModel.m_pCurrent != null &&
    		(	m_pModel.m_pCurrent.m_Meshes.size() > 1 ||
    			(m_pModel.m_pCurrent.m_Meshes.size() == 1 && m_pModel.m_Meshes.get(m_pModel.m_pCurrent.m_Meshes.getInt(0)).m_Faces.size() != 0)	)
    		)
    		m_pModel.m_pCurrent = null;

    	// Get next data for material data
    	m_DataIt = getNextToken(m_DataIt, m_DataItEnd);
    	if (m_DataIt == m_DataItEnd)
    		return;

//    	char *pStart = &(*m_DataIt);
    	int pStart = m_DataIt;
    	while ( m_DataIt != m_DataItEnd && !isSeparator(data.get(m_DataIt)) )
    		++m_DataIt;

    	// Get name
//    	std::string strName(pStart, &(*m_DataIt));
//    	if ( strName.empty())
//    		return;
    	
//    	byte[] _name = new byte[m_DataIt - pStart];
    	int pos = data.position();
    	data.position(pStart);
    	data.get(m_buffer, 0, m_DataIt - pStart);
    	data.position(pos);
    	String strName = new String(m_buffer, 0, m_DataIt - pStart);
    	if(strName.isEmpty())
    		return;

    	// Search for material
    	ObjMaterial it = m_pModel.m_MaterialMap.get( strName );
    	if ( it == null )
    	{
    		// Not found, use default material
    		m_pModel.m_pCurrentMaterial = m_pModel.m_pDefaultMaterial;
    		DefaultLogger.error("OBJ: failed to locate material " + strName + ", skipping");
    	}
    	else
    	{
    		// Found, using detected material
    		m_pModel.m_pCurrentMaterial = it;
    		if ( needsNewMesh( strName ))
    		{
    			createMesh();	
    		}
    		m_pModel.m_pCurrentMesh.m_uiMaterialIndex = getMaterialIndex( strName );
    	}

    	// Skip rest of line
    	m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
    }
	///	Gets a comment.
	void getComment(){
		while (m_DataIt != m_DataItEnd)
		{
			if ( '\n' == (data.get(m_DataIt)))
			{
				++m_DataIt;
				break;
			}
			else
			{
				++m_DataIt;
			}
		}
	}
	/// Gets a a material library.
	void getMaterialLib(){
		// Translate tuple
		m_DataIt = getNextToken(m_DataIt, m_DataItEnd);
		if (m_DataIt ==  m_DataItEnd)
			return;
		
//		char *pStart = &(*m_DataIt);
		int pStart = m_DataIt;
		while (m_DataIt != m_DataItEnd && !isNewLine(data.get(m_DataIt)))
			m_DataIt++;

		// Check for existence
//		const std::string strMatName(pStart, &(*m_DataIt));
//		byte[] _name = new byte[m_DataIt - pStart];
    	int pos = data.position();
    	data.position(pStart);
    	data.get(m_buffer, 0, m_DataIt - pStart);
    	data.position(pos);
    	String strMatName = new String(m_buffer, 0, m_DataIt - pStart);
//		IOStream *pFile = m_pIO.Open(strMatName);
    	File pFile = new File(strMatName);

		if (!pFile.exists() || !pFile.canRead())
		{
			DefaultLogger.error("OBJ: Unable to locate material file " + strMatName);
			m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
			return;
		}

		// Import material library data from file
//		std::vector<char> buffer;
//		BaseImporter::TextFileToBuffer(pFile,buffer);
//		m_pIO.Close( pFile );
		ByteBuffer buffer = FileUtils.loadText(pFile, true, AssimpConfig.LOADING_USE_NATIVE_MEMORY);

		// Importing the material library 
		 new ObjFileMtlImporter( buffer, m_pModel );	
	}
	/// Creates a new material.
	void getNewMaterial(){
		m_DataIt = getNextToken(m_DataIt, m_DataItEnd);
		m_DataIt = getNextWord(m_DataIt, m_DataItEnd);
		if ( m_DataIt == m_DataItEnd )
			return;

//		char *pStart = &(*m_DataIt);
		int pStart = m_DataIt;
		int length = data.get(m_DataIt);  // TODO
//		std::string strMat( pStart, *m_DataIt );
		int pos = data.position();
    	data.position(pStart);
    	data.get(m_buffer, 0, length);
    	data.position(pos);
    	String strMat = new String(m_buffer, 0, length);
		
		while ( m_DataIt != m_DataItEnd && isSeparator(data.get(m_DataIt) ) )
			m_DataIt++;
		ObjMaterial it = m_pModel.m_MaterialMap.get( strMat );
		if ( it == null )
		{
			// Show a warning, if material was not found
			DefaultLogger.warn("OBJ: Unsupported material requested: " + strMat);
			m_pModel.m_pCurrentMaterial = m_pModel.m_pDefaultMaterial;
		}
		else
		{
			// Set new material
			if ( needsNewMesh( strMat ) )
			{
				createMesh();	
			}
			m_pModel.m_pCurrentMesh.m_uiMaterialIndex = getMaterialIndex( strMat );
		}

		m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
	}
	/// Gets the group name from file.
	void getGroupName(){
		String[] strGroupName = new String[1];
		   
		m_DataIt = getName(m_DataIt, m_DataItEnd, strGroupName);
		if ( isEndOfBuffer( m_DataIt, m_DataItEnd ) )
			return;

		// Change active group, if necessary
		if ( !m_pModel.m_strActiveGroup.equals(strGroupName[0]) )
		{
			// Search for already existing entry
			IntList it = m_pModel.m_Groups.get(strGroupName[0]);
			
			// We are mapping groups into the object structure
			createObject( strGroupName[0] );
			
			// New group name, creating a new entry
			if (it == null)
			{
//				std::vector<unsigned int> *pFaceIDArray = new std::vector<unsigned int>;
//				m_pModel.m_Groups[ strGroupName ] = pFaceIDArray;
//				m_pModel.m_pGroupFaceIDs = (pFaceIDArray);
				
				IntList pFaceIDArray = new IntArrayList();
				m_pModel.m_Groups.put(strGroupName[0], pFaceIDArray);
				m_pModel.m_pGroupFaceIDs = pFaceIDArray;
			}
			else
			{
				m_pModel.m_pGroupFaceIDs = it;
			}
			m_pModel.m_strActiveGroup = strGroupName[0];
		}
		m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
	}
	/// Gets the group number from file.
	void getGroupNumber(){
		// Not used

	    m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
	}
	/// Gets the group number and resolution from file.
	void getGroupNumberAndResolution(){
		// Not used

		m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
	}
	/// Returns the index of the material. Is -1 if not material was found.
	int getMaterialIndex( String strMaterialName ){
		int mat_index = -1;
		if ( strMaterialName.isEmpty() )
			return mat_index;
		for (int index = 0; index < m_pModel.m_MaterialLib.size(); ++index)
		{
			if ( strMaterialName == m_pModel.m_MaterialLib.get(index))
			{
				mat_index = (int)index;
				break;
			}
		}
		return mat_index;
	}
	
	/// Parse object name
	void getObjectName(){
		m_DataIt = getNextToken(m_DataIt, m_DataItEnd);
		if (m_DataIt == m_DataItEnd)
			return;
//		char *pStart = &(*m_DataIt);
		int pStart = m_DataIt;
		while ( m_DataIt != m_DataItEnd && !isSeparator(data.get(m_DataIt) ) )
			++m_DataIt;

//		std::string strObjectName(pStart, &(*m_DataIt));
		String strObjectName = getName(pStart, m_DataItEnd);
		if (!strObjectName.isEmpty()) 
		{
			// Reset current object
			m_pModel.m_pCurrent = null;
			
			// Search for actual object
			for (ObjObject it : m_pModel.m_Objects)
			{
				if (it.m_strObjName.equals(strObjectName))
				{
					m_pModel.m_pCurrent = it;
					break;
				}
			}

			// Allocate a new object, if current one was not found before
			if ( null == m_pModel.m_pCurrent )
				createObject(strObjectName);
		}
		m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
	}
	
	/// Creates a new object.
	void createObject(String strObjectName){
		m_pModel.m_pCurrent = new ObjObject();
		m_pModel.m_pCurrent.m_strObjName = strObjectName;
		m_pModel.m_Objects.add( m_pModel.m_pCurrent );
		

		createMesh();

		if( m_pModel.m_pCurrentMaterial !=null)
		{
			m_pModel.m_pCurrentMesh.m_uiMaterialIndex = 
				getMaterialIndex( m_pModel.m_pCurrentMaterial.materialName );
			m_pModel.m_pCurrentMesh.m_pMaterial = m_pModel.m_pCurrentMaterial;
		}
	}
	///	Creates a new mesh.
	void createMesh(){
		m_pModel.m_pCurrentMesh = new ObjMesh();
		m_pModel.m_Meshes.add( m_pModel.m_pCurrentMesh );
		int meshId = m_pModel.m_Meshes.size()-1;
		if ( null != m_pModel.m_pCurrent )
		{
			m_pModel.m_pCurrent.m_Meshes.add( meshId );
		}
		else
		{
			DefaultLogger.error("OBJ: No object detected to attach a new mesh instance.");
		}
	}
	///	Returns true, if a new mesh instance must be created.
	boolean needsNewMesh(String rMaterialName ){
		if(m_pModel.m_pCurrentMesh == null)
		{
			// No mesh data yet
			return true;
		}
		boolean newMat = false;
		int matIdx = getMaterialIndex( rMaterialName );
		int curMatIdx = m_pModel.m_pCurrentMesh.m_uiMaterialIndex;
		if ( curMatIdx != ObjMesh.NO_MATERIAL || curMatIdx != matIdx )
		{
			// New material -> only one material per mesh, so we need to create a new 
			// material
			newMat = true;
		}
		return newMat;
	}
	
	///	Error report in token
	void reportErrorTokenInFace(){
		m_DataIt = skipLine( m_DataIt, m_DataItEnd, m_uiLine );
		DefaultLogger.error("OBJ: Not supported token in face description detected");
	}
}

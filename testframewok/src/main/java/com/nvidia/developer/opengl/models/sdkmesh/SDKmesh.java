//--------------------------------------------------------------------------------------
// File: SDKMesh.java
//
// The SDK Mesh format (.sdkmesh) is not a recommended file format for games.  
// It was designed to meet the specific needs of the SDK samples.  Any real-world 
// applications should avoid this file format in favor of a destination format that 
// meets the specific needs of the application.
//
// Copyright (c) Microsoft Corporation. All rights reserved.
//--------------------------------------------------------------------------------------
package com.nvidia.developer.opengl.models.sdkmesh;

import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StringUtils;

import static jet.opengl.postprocessing.common.GLenum.GL_UNSIGNED_SHORT;

//  This class reads the sdkmesh file format for use by the samples
public class SDKmesh {

	private static final int NULL = 0;
	//--------------------------------------------------------------------------------------
	// Hard Defines for the various structures
	//--------------------------------------------------------------------------------------
	
	public static final int SDKMESH_FILE_VERSION = 101,
							MAX_VERTEX_ELEMENTS = 32,
							MAX_VERTEX_STREAMS =16,
							MAX_FRAME_NAME = 100,
							MAX_MESH_NAME = 100,
							MAX_SUBSET_NAME =100,
							MAX_MATERIAL_NAME =100,
							MAX_TEXTURE_NAME =260,
							MAX_MATERIAL_PATH = 260,
							INVALID_FRAME = -1,
							INVALID_MESH = -1,
							INVALID_MATERIAL = -1,
							INVALID_SUBSET =-1,
							INVALID_ANIMATION_DATA =-1,
							ERROR_RESOURCE_VALUE =1,
							INVALID_SAMPLER_SLOT =-1;
	
	private static final int D3D10_PRIMITIVE_TOPOLOGY_UNDEFINED = 0;
	private static final int D3D10_PRIMITIVE_TOPOLOGY_POINTLIST = 1;
	private static final int D3D10_PRIMITIVE_TOPOLOGY_LINELIST = 2;
	private static final int D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP = 3;
	private static final int D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST = 4;
	private static final int D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP = 5;
	
	private static final int D3D10_PRIMITIVE_TOPOLOGY_LINELIST_ADJ= 10;
	private static final int D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP_ADJ = 11;
	private static final int D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST_ADJ  = 12;
	private static final int D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP_ADJ   = 13;
	
	public static int convertDXDrawCMDToGL(int primType){
		switch (primType) {
		case D3D10_PRIMITIVE_TOPOLOGY_UNDEFINED: return 0;
		case D3D10_PRIMITIVE_TOPOLOGY_POINTLIST: return GLenum.GL_POINTS;
		case D3D10_PRIMITIVE_TOPOLOGY_LINELIST:  return GLenum.GL_LINES;
		case D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP:  return GLenum.GL_LINE_STRIP;
		case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST:  return GLenum.GL_TRIANGLES;
		case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP:  return GLenum.GL_TRIANGLE_STRIP;
		case D3D10_PRIMITIVE_TOPOLOGY_LINELIST_ADJ:   return GLenum.GL_LINES_ADJACENCY;
		case D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP_ADJ:   return GLenum.GL_LINE_STRIP_ADJACENCY;
		case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST_ADJ:   return GLenum.GL_TRIANGLES_ADJACENCY;
		case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP_ADJ:   return GLenum.GL_TRIANGLE_STRIP_ADJACENCY;
		}
		
		return 0;
		
	}
	
	// Should query dynamic.
	private static final int MAX_D3D10_VERTEX_STREAMS = 16;
	
	//--------------------------------------------------------------------------------------
	// Enumerated Types.  These will have mirrors in both D3D9 and D3D10
	//--------------------------------------------------------------------------------------
	public static final int 
	// enum SDKMESH_PRIMITIVE_TYPE
//	{
	    PT_TRIANGLE_LIST = 0,
	    PT_TRIANGLE_STRIP =1,
	    PT_LINE_LIST = 2,
	    PT_LINE_STRIP = 3,
	    PT_POINT_LIST = 4,
	    PT_TRIANGLE_LIST_ADJ = 5,
	    PT_TRIANGLE_STRIP_ADJ = 6,
	    PT_LINE_LIST_ADJ =7,
	    PT_LINE_STRIP_ADJ =8,
        PT_QUAD_PATCH_LIST = 9,
        PT_TRIANGLE_PATCH_LIST =10;
//	};
	
	public static final int
//	enum SDKMESH_INDEX_TYPE
//	{
	    IT_16BIT = 0,
	    IT_32BIT = 1;
//	};

	public static final int
//	enum FRAME_TRANSFORM_TYPE
//	{
	    FTT_RELATIVE = 0,
	    FTT_ABSOLUTE = 1;		//This is not currently used but is here to support absolute transformations in the future
//	};
	
	int m_NumOutstandingResources;
	boolean m_bLoading;
	    //BYTE*                         m_pBufferData;
	Object m_hFile;
	Object m_hFileMappingObject;
	List<byte[]> m_MappedPointers;
//	    IDirect3DDevice9* m_pDev9;
//	    ID3D10Device* m_pDev10;
	
//	protected:
	//These are the pointers to the two chunks of data loaded in from the mesh file
    byte[] m_pStaticMeshData;
    byte[] m_pHeapData;
    byte[] m_pAnimationData;
    int[] m_ppVertices;   // hold the offset
    int[] m_ppIndices;    // hold the offset

    //Keep track of the path
//    WCHAR                           m_strPathW[MAX_PATH];
//    char                            m_strPath[MAX_PATH];
    String m_strPath;

    //General mesh info
    SDKmeshHeader m_pMeshHeader;
    SDKMeshVertexBufferHeader[] m_pVertexBufferArray;
    SDKMeshIndexBufferHeader[] m_pIndexBufferArray;
    SDKMeshMesh[] m_pMeshArray;
    SDKMeshSubset[] m_pSubsetArray;
    SDKMeshFrame[] m_pFrameArray;
    SDKmeshMaterial[] m_pMaterialArray;

    // Adjacency information (not part of the m_pStaticMeshData, so it must be created and destroyed separately )
    SDKMeshIndexBufferHeader[] m_pAdjacencyIndexBufferArray;

    //Animation (TODO: Add ability to load/track multiple animation sets)
    SDKAnimationFileHeader m_pAnimationHeader;
    SDKAnimationFrameData[] m_pAnimationFrameData;
    Matrix4f[] m_pBindPoseFrameMatrices;
    Matrix4f[] m_pTransformedFrameMatrices;
    private GLFuncProvider gl;
    
    void loadMaterials(SDKmeshMaterial[] pMaterials) throws IOException{ loadMaterials(pMaterials, null);}
    void loadMaterials(SDKmeshMaterial[] pMaterials, SDKmeshCallbacks pLoaderCallbacks) throws IOException{
//    	char strPath[MAX_PATH];
    	String strPath;
    	int numMaterials = pMaterials.length;
        if( pLoaderCallbacks!=null)
        {
            for( int m = 0; m < numMaterials; m++ )
            {
            	if(pMaterials[m] == null)
            		continue;
                pMaterials[m].pDiffuseTexture = NULL;
                pMaterials[m].pNormalTexture = NULL;
                pMaterials[m].pSpecularTexture = NULL;
//                pMaterials[m].pDiffuseRV10 = NULL;
//                pMaterials[m].pNormalRV10 = NULL;
//                pMaterials[m].pSpecularRV10 = NULL;

                // load textures
                if( !StringUtils.isEmpty(pMaterials[m].diffuseTexture))
                {
                	pMaterials[m].pDiffuseTexture = pLoaderCallbacks.createTextureFromFile( /*pd3dDevice,*/
                                                              pMaterials[m].diffuseTexture/*, &pMaterials[m].pDiffuseRV10,
                                                              pLoaderCallbacks->pContext*/ );
                }
                if( !StringUtils.isEmpty(pMaterials[m].normalTexture) )
                {
                	pMaterials[m].pNormalTexture = pLoaderCallbacks.createTextureFromFile( /*pd3dDevice,*/
                                                              pMaterials[m].normalTexture/*, &pMaterials[m].pNormalRV10,
                                                              pLoaderCallbacks->pContext*/ );
                }
                if( !StringUtils.isEmpty(pMaterials[m].specularTexture) )
                {
                	pMaterials[m].pSpecularTexture = pLoaderCallbacks.createTextureFromFile( /*pd3dDevice,*/
                                                              pMaterials[m].specularTexture/*, &pMaterials[m].pSpecularRV10,
                                                              pLoaderCallbacks->pContext*/ );
                }
            }
        }
        else
        {
        	int textureCount = 0;
            for( int m = 0; m < numMaterials; m++ )
            {
            	if(pMaterials[m] == null)
            		continue;
                pMaterials[m].pDiffuseTexture = NULL;
                pMaterials[m].pNormalTexture = NULL;
                pMaterials[m].pSpecularTexture = NULL;
//                pMaterials[m].pDiffuseRV10 = NULL;
//                pMaterials[m].pNormalRV10 = NULL;
//                pMaterials[m].pSpecularRV10 = NULL;

                // load textures
                if( !StringUtils.isEmpty(pMaterials[m].diffuseTexture))
                {
//                    sprintf_s( strPath, MAX_PATH, "%s%s", m_strPath, pMaterials[m].DiffuseTexture );
//                    if( FAILED( DXUTGetGlobalResourceCache().CreateTextureFromFile( pd3dDevice,
//                                                                                    strPath, &pMaterials[m].pDiffuseRV10,
//                                                                                    true ) ) )
//                        pMaterials[m].pDiffuseRV10 = ( ID3D10ShaderResourceView* )ERROR_RESOURCE_VALUE;
                	strPath = m_strPath + pMaterials[m].diffuseTexture;
                	pMaterials[0].pDiffuseTexture = NvImage.uploadTextureFromDDSFile(strPath);
                	textureCount ++;
                }
                if( !StringUtils.isEmpty(pMaterials[m].normalTexture) )
                {
//                    sprintf_s( strPath, MAX_PATH, "%s%s", m_strPath, pMaterials[m].NormalTexture );
//                    if( FAILED( DXUTGetGlobalResourceCache().CreateTextureFromFile( pd3dDevice,
//                                                                                    strPath,
//                                                                                    &pMaterials[m].pNormalRV10 ) ) )
//                        pMaterials[m].pNormalRV10 = ( ID3D10ShaderResourceView* )ERROR_RESOURCE_VALUE;
                	strPath = m_strPath + pMaterials[m].normalTexture;
                	pMaterials[0].pNormalTexture = NvImage.uploadTextureFromDDSFile(strPath);
                	textureCount ++;
                }
                if( !StringUtils.isEmpty(pMaterials[m].specularTexture) )
                {
//                    sprintf_s( strPath, MAX_PATH, "%s%s", m_strPath, pMaterials[m].SpecularTexture );
//                    if( FAILED( DXUTGetGlobalResourceCache().CreateTextureFromFile( pd3dDevice,
//                                                                                    strPath,
//                                                                                    &pMaterials[m].pSpecularRV10 ) ) )
//                        pMaterials[m].pSpecularRV10 = ( ID3D10ShaderResourceView* )ERROR_RESOURCE_VALUE;
                	strPath = m_strPath + pMaterials[m].specularTexture;
                	pMaterials[0].pSpecularTexture = NvImage.uploadTextureFromDDSFile(strPath);
                	textureCount ++;
                }
            }
            
            if(textureCount > 0)
                gl.glBindTexture(GLenum.GL_TEXTURE_2D, 0);
        }
    }
    
    void createVertexBuffer( /*ID3D10Device* pd3dDevice,*/
                 SDKMeshVertexBufferHeader pHeader, ByteBuffer pVertices){
    	createVertexBuffer(pHeader, pVertices, null);
    }
    
    void createVertexBuffer( /*ID3D10Device* pd3dDevice,*/
            SDKMeshVertexBufferHeader pHeader, ByteBuffer pVertices,
            SDKmeshCallbacks pLoaderCallbacks ){
    	pHeader.dataOffset = 0;
        //Vertex Buffer
//        D3D10_BUFFER_DESC bufferDesc;
//        bufferDesc.ByteWidth = ( int )( pHeader->SizeBytes );
//        bufferDesc.Usage = D3D10_USAGE_DEFAULT;
//        bufferDesc.BindFlags = D3D10_BIND_VERTEX_BUFFER;
//        bufferDesc.CPUAccessFlags = 0;
//        bufferDesc.MiscFlags = 0;

        if( pLoaderCallbacks != null )
        {
        	pHeader.pVB11 = pLoaderCallbacks.createVertexBuffer( GLenum.GL_STATIC_DRAW, pVertices/*pd3dDevice, &pHeader->pVB10, bufferDesc, pVertices,
                                                   pLoaderCallbacks->pContext*/ );
        }
        else
        {
//            D3D10_SUBRESOURCE_DATA InitData;
//            InitData.pSysMem = pVertices;
//            hr = pd3dDevice->CreateBuffer( &bufferDesc, &InitData, &pHeader->pVB10 );
//            DXUT_SetDebugName( pHeader->pVB10, "CDXUTSDKMesh" );
        	int buf = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, buf);
            gl.glBufferData(GLenum.GL_ARRAY_BUFFER, pVertices, GLenum.GL_STATIC_DRAW);
            gl.glBindBuffer(GLenum.GL_ARRAY_BUFFER, 0);
        	pHeader.pVB11 = buf;
        }
    }
    
//HRESULT                         CreateVertexBuffer( IDirect3DDevice9* pd3dDevice,
//                 SDKMESH_VERTEX_BUFFER_HEADER* pHeader, void* pVertices,
//                 SDKMESH_CALLBACKS9* pLoaderCallbacks=NULL );
    void  createIndexBuffer( /*ID3D10Device* pd3dDevice, */SDKMeshIndexBufferHeader pHeader,ByteBuffer pIndices){
    	createIndexBuffer(pHeader, pIndices, null);
    }
    
    void  createIndexBuffer( /*ID3D10Device* pd3dDevice, */SDKMeshIndexBufferHeader pHeader,
                ByteBuffer pIndices, SDKmeshCallbacks pLoaderCallback ){
    	if( pLoaderCallback != null )
        {
        	pHeader.pIB11 = pLoaderCallback.createIndexBuffer( GLenum.GL_STATIC_DRAW, pIndices/*pd3dDevice, &pHeader->pVB10, bufferDesc, pVertices,
                                                   pLoaderCallbacks->pContext*/ );
        }
        else
        {
//            D3D10_SUBRESOURCE_DATA InitData;
//            InitData.pSysMem = pVertices;
//            hr = pd3dDevice->CreateBuffer( &bufferDesc, &InitData, &pHeader->pVB10 );
//            DXUT_SetDebugName( pHeader->pVB10, "CDXUTSDKMesh" );
        	int buf = gl.glGenBuffer();
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, buf);
            gl.glBufferData(GLenum.GL_ELEMENT_ARRAY_BUFFER, pIndices, GLenum.GL_STATIC_DRAW);
            gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, 0);
        	pHeader.pIB11 = buf;
        }
    }
//HRESULT                         CreateIndexBuffer( IDirect3DDevice9* pd3dDevice,
//                SDKMESH_INDEX_BUFFER_HEADER* pHeader, void* pIndices,
//                SDKMESH_CALLBACKS9* pLoaderCallbacks=NULL );

    void createFromFile( /*ID3D10Device* pDev10, IDirect3DDevice9* pDev9,*/ String fileName,
             boolean bCreateAdjacencyIndices,
             SDKmeshCallbacks pLoaderCallbacks) throws IOException{
    	byte[] pData = FileUtils.loadBytes(fileName);
    	createFromMemory(pData, bCreateAdjacencyIndices, false, pLoaderCallbacks);
    }
    
    
    boolean debug = true;
    void  createFromMemory( /*ID3D10Device* pDev10,
               IDirect3DDevice9* pDev9,*/
               byte[] pData,
//               int DataBytes,
               boolean bCreateAdjacencyIndices,
               boolean bCopyStatic,
               SDKmeshCallbacks pLoaderCallbacks
                ) throws IOException{
    	
    	final Vector3f lower = new Vector3f();
    	final Vector3f upper = new Vector3f();
    	final Vector3f pt = new Vector3f();
    	
    	// Set outstanding resources to zero
        m_NumOutstandingResources = 0;
        
        if(bCopyStatic){
        	SDKmeshHeader header = new SDKmeshHeader();
        	header.load(pData, 0);
        	
        	int staticSize = (int) (header.headerSize + header.nonBufferDataSize);
        	m_pHeapData = new byte[staticSize];
        	
        	m_pStaticMeshData = m_pHeapData;
        	System.arraycopy(pData, 0, m_pStaticMeshData, 0, staticSize);
        }else{
        	 m_pHeapData = pData;
             m_pStaticMeshData = pData;
        }
        
        // Pointer fixup
        m_pMeshHeader = new SDKmeshHeader();
        m_pMeshHeader.load(m_pStaticMeshData, 0);
        
        if(debug){
        	System.out.println("data.length = " + pData.length);
        	System.out.println(m_pMeshHeader);
        }
        
        m_pVertexBufferArray = new SDKMeshVertexBufferHeader[m_pMeshHeader.numVertexBuffers];
        int vertexBufferArrayOffset = (int)m_pMeshHeader.vertexStreamHeadersOffset;
        for(int i = 0; i < m_pVertexBufferArray.length; i++){
        	m_pVertexBufferArray[i] = new SDKMeshVertexBufferHeader();
        	vertexBufferArrayOffset = m_pVertexBufferArray[i].load(m_pStaticMeshData, vertexBufferArrayOffset);
        	if(debug)
        		System.out.println(m_pVertexBufferArray[i]);
        }
        
        m_pIndexBufferArray = new SDKMeshIndexBufferHeader[m_pMeshHeader.numIndexBuffers];
        int indexBufferArrayOffset = (int)m_pMeshHeader.indexStreamHeadersOffset;
        for(int i = 0; i < m_pIndexBufferArray.length; i++){
        	m_pIndexBufferArray[i] = new SDKMeshIndexBufferHeader();
        	indexBufferArrayOffset = m_pIndexBufferArray[i].load(m_pStaticMeshData, indexBufferArrayOffset);
        	if(debug)
        		System.out.println(m_pIndexBufferArray[i]);
        }
        
        m_pMeshArray = new SDKMeshMesh[m_pMeshHeader.numMeshes];
        int meshArrayOffset = (int) m_pMeshHeader.meshDataOffset;
        for(int i = 0; i < m_pMeshArray.length;i++){
        	m_pMeshArray[i] = new SDKMeshMesh();
        	meshArrayOffset = m_pMeshArray[i].load(m_pStaticMeshData, meshArrayOffset);
        	if(debug)
        		System.out.println(m_pMeshArray[i]);
        }
        
        m_pSubsetArray = new SDKMeshSubset[m_pMeshHeader.numTotalSubsets];
        int subsetArrayOffset = (int)m_pMeshHeader.subsetDataOffset;
        for(int i = 0; i < m_pSubsetArray.length;i++){
        	m_pSubsetArray[i] = new SDKMeshSubset();
        	subsetArrayOffset = m_pSubsetArray[i].load(m_pStaticMeshData, subsetArrayOffset);
        	if(debug)
        		System.out.println(m_pSubsetArray[i]);
        }
        
        m_pFrameArray = new SDKMeshFrame[m_pMeshHeader.numFrames];
        int frameArrayOffset = (int)m_pMeshHeader.frameDataOffset;
        for(int i = 0; i < m_pFrameArray.length;i++){
        	m_pFrameArray[i] = new SDKMeshFrame();
        	frameArrayOffset = m_pFrameArray[i].load(m_pStaticMeshData, frameArrayOffset);
        	if(debug)
        		System.out.println(m_pFrameArray[i]);
        }
        
        m_pMaterialArray = new SDKmeshMaterial[m_pMeshHeader.numMaterials];
        int materialArrayOffset = (int)m_pMeshHeader.materialDataOffset;
        for(int i = 0; i < m_pMaterialArray.length;i++){
        	m_pMaterialArray[i] = new SDKmeshMaterial();
        	materialArrayOffset = m_pMaterialArray[i].load(m_pStaticMeshData, materialArrayOffset);
        	if(debug)
        		System.out.println(m_pMaterialArray[i]);
        }
        
     // Setup subsets
        for( int i = 0; i < m_pMeshHeader.numMeshes; i++ )
        {
//            m_pMeshArray[i].pSubsets = ( int* )( m_pStaticMeshData + m_pMeshArray[i].SubsetOffset );
//            m_pMeshArray[i].pFrameInfluences = ( int* )( m_pStaticMeshData + m_pMeshArray[i].FrameInfluenceOffset );
        	// TODO
        	
        	int[] array = m_pMeshArray[i].pSubsets = new int[m_pMeshArray[i].numSubsets];
        	int offset = (int) m_pMeshArray[i].subsetOffset;
        	for(int j = 0; j < array.length; j++){
        		array[j] = Numeric.getInt(m_pStaticMeshData, offset);
        		offset += 4;
        	}
        	
        	array = m_pMeshArray[i].pFrameInfluences = new int[m_pMeshArray[i].numFrameInfluences];
        	offset = (int) m_pMeshArray[i].frameInfluenceOffset;
        	for(int j = 0; j < array.length; j++){
        		array[j] = Numeric.getInt(m_pStaticMeshData, offset);
        		offset += 4;
        	}
        }
        
        if(m_pMeshHeader.version != SDKMESH_FILE_VERSION){
        	throw new IllegalArgumentException("Unexpect version: " + m_pMeshHeader.version);
        }
        
        // Setup buffer data pointer
        int pBufferData = (int) (m_pMeshHeader.headerSize + m_pMeshHeader.nonBufferDataSize);
        
        // Get the start of the buffer data
        int bufferDataStart = (int) (m_pMeshHeader.headerSize + m_pMeshHeader.nonBufferDataSize);
        
        // Create Adjacency Indices
        if(bCreateAdjacencyIndices)
        	createAdjacencyIndices(0.001f, pData/*pBufferData - bufferDataStart  TODO */ );
        
        // Create VBs
        m_ppVertices = new int[m_pMeshHeader.numVertexBuffers];
        for(int i = 0; i < m_pMeshHeader.numVertexBuffers; i++){
        	int pVertices = NULL;
        	pVertices = (int) (pBufferData + ( m_pVertexBufferArray[i].dataOffset - bufferDataStart ));
        	
        	ByteBuffer bytes = CacheBuffer.getCachedByteBuffer((int) m_pVertexBufferArray[i].sizeBytes);
        	bytes.put(m_pStaticMeshData, pVertices, (int) m_pVertexBufferArray[i].sizeBytes).flip();
//        	createVertexBuffer(m_pVertexBufferArray[i], bytes, pLoaderCallbacks);
        	
        	m_ppVertices[i] = pVertices;
        }
        
        if(debug)
        	System.out.println("m_ppVertices = " + Arrays.toString(m_ppVertices));
        
        // Create IBs
        m_ppIndices = new int[m_pMeshHeader.numIndexBuffers];
        for( int i = 0; i < m_pMeshHeader.numIndexBuffers; i++ ){
        	int pIndices =  (int) (pBufferData + ( m_pIndexBufferArray[i].dataOffset - bufferDataStart ));
        	
        	ByteBuffer bytes = CacheBuffer.getCachedByteBuffer((int) m_pIndexBufferArray[i].sizeBytes);
        	bytes.put(m_pStaticMeshData, pIndices, (int) m_pIndexBufferArray[i].sizeBytes).flip();
//        	createIndexBuffer(m_pIndexBufferArray[i], bytes, pLoaderCallbacks);
        	
//        	if(debug){
//        		int count = bytes.remaining()/4;
//        		for(int m = 0; m < count;m++){
//        			System.out.println("Index" + m + " = " + bytes.getInt(m << 2));
//        		}
//        	}
        	
        	m_ppIndices[i] = pIndices;
        }
        
        if(debug)
        	System.out.println("m_ppIndices = " + Arrays.toString(m_ppIndices));
        
        // Load Materials
        loadMaterials(m_pMaterialArray, pLoaderCallbacks);
        
        // Create a place to store our bind pose frame matrices
        m_pBindPoseFrameMatrices = new Matrix4f[ m_pMeshHeader.numFrames ];
        
        // Create a place to store our transformed frame matrices
        m_pTransformedFrameMatrices = new Matrix4f[m_pMeshHeader.numFrames];
        
        SDKMeshSubset pSubset = null;
        int PrimType;

        // update bounding volume 
//        SDKMeshMesh currentMesh = m_pMeshArray[0];
        SDKMeshMesh currentMesh = m_pMeshArray[0];
        int tris = 0;
        final float FLT_MAX = Float.MAX_VALUE;
        for (int meshi=0; meshi < m_pMeshHeader.numMeshes; ++meshi) {
            lower.x = FLT_MAX; lower.y = FLT_MAX; lower.z = FLT_MAX;
            upper.x = -FLT_MAX; upper.y = -FLT_MAX; upper.z = -FLT_MAX;
            currentMesh = getMesh( meshi );
            int indsize;
            if (m_pIndexBufferArray[currentMesh.indexBuffer].indexType == IT_16BIT ) {
                indsize = 2;
            }else {
                indsize = 4;        
            }

            for( int subset = 0; subset < currentMesh.numSubsets; subset++ )
            {
                pSubset = getSubset( meshi, subset ); //&m_pSubsetArray[ currentMesh->pSubsets[subset] ];

                PrimType = getPrimitiveType10(pSubset.primitiveType );
 //               assert( PrimType == D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST );// only triangle lists are handled.

                int IndexCount = ( int )pSubset.indexCount;
                int IndexStart = ( int )pSubset.indexStart;
                
                if(debug){
                	System.out.println("IndexCount = " + IndexCount);
                	System.out.println("IndexStart = " + IndexStart);
                }

                /*if( bAdjacent )
                {
                    IndexCount *= 2;
                    IndexStart *= 2;
                }*/
         
            //BYTE* pIndices = NULL;
                //m_ppIndices[i]
//                int *ind = ( int * )m_ppIndices[currentMesh->IndexBuffer];
//                FLOAT *verts =  ( FLOAT* )m_ppVertices[currentMesh->VertexBuffers[0]];
//                int stride = (int)m_pVertexBufferArray[currentMesh->VertexBuffers[0]].StrideBytes;
                int ind = m_ppIndices[currentMesh.indexBuffer];
                int verts = m_ppVertices[currentMesh.vertexBuffers[0]];
                int stride = (int) m_pVertexBufferArray[currentMesh.vertexBuffers[0]].strideBytes;
//               assert (stride % 4 == 0);
//                stride /=4;
                for (int vertind = IndexStart; vertind < IndexStart + IndexCount; ++vertind) {
                    int current_ind=0;
                    if (indsize == 2) { // unsiged short, every index taken 2 bytes.
//                        int ind_div2 = vertind / 2;
////                        current_ind = ind[ind_div2];
//                        current_ind = Numeric.getShort(pData, ind + (ind_div2 << 1));
//                        if (vertind %2 ==0) {
//                            current_ind = current_ind << 16;
//                            current_ind = current_ind >> 16;
//                        }else {
//                            current_ind = current_ind >> 16;
//                        }
                    	current_ind = Numeric.getShort(pData, ind + (vertind << 1)) & 0xFFFF;
                    }else {
//                        current_ind = ind[vertind];
                    	current_ind = Numeric.getInt(pData, ind + (vertind << 2));
                    }
                    tris++;
//                    D3DXVECTOR3 *pt = (D3DXVECTOR3*)&(verts[stride * current_ind]);
                    int pt_offset = verts + (stride * current_ind);
                    pt.x = Numeric.getFloat(pData, pt_offset++);
                    pt.y = Numeric.getFloat(pData, pt_offset++);
                    pt.z = Numeric.getFloat(pData, pt_offset++);
                    if (pt.x < lower.x) {
                        lower.x = pt.x;
                    }
                    if (pt.y < lower.y) {
                        lower.y = pt.y;
                    }
                    if (pt.z < lower.z) {
                        lower.z = pt.z;
                    }
                    if (pt.x > upper.x) {
                        upper.x = pt.x;
                    }
                    if (pt.y > upper.y) {
                        upper.y = pt.y;
                    }
                    if (pt.z > upper.z) {
                        upper.z = pt.z;
                    }
                    //BYTE** m_ppVertices;
                    //BYTE** m_ppIndices;
                }
                //pd3dDeviceContext->DrawIndexed( IndexCount, IndexStart, VertexStart );
            }

//            D3DXVECTOR3 half = upper - lower;
//            half *=0.5f;
            Vector3f half = ((Vector3f)Vector3f.sub(upper, lower, pt)).scale(0.5f);

//            currentMesh->BoundingBoxCenter = lower + half;
//            currentMesh->BoundingBoxExtents = half;
            Vector3f.add(lower, half, currentMesh.boundingBoxCenter);
            currentMesh.boundingBoxExtents.set(half);
            
            if(debug){
            	System.out.println("Mesh" + meshi + " center = " + currentMesh.boundingBoxCenter);
            	System.out.println("Mesh" + meshi + " extent = " + currentMesh.boundingBoxExtents);
            }

        }
        // Update 
    }

//frame manipulation
    void  transformBindPoseFrame( int iFrame, Matrix4f pParentWorld ){
    	if( m_pBindPoseFrameMatrices == null)
            return;

        // Transform ourselves
//        D3DXMATRIX LocalWorld;
//        D3DXMatrixMultiply( &LocalWorld, &m_pFrameArray[iFrame].Matrix, pParentWorld );
//        m_pBindPoseFrameMatrices[iFrame] = LocalWorld;
    	Matrix4f localWorld = null;
    	localWorld = m_pBindPoseFrameMatrices[iFrame];
    	if(localWorld == null)
    		localWorld = new Matrix4f();
    	Matrix4f.mul(pParentWorld, m_pFrameArray[iFrame].matrix, localWorld);
    	m_pBindPoseFrameMatrices[iFrame] = localWorld;

        // Transform our siblings
        if( m_pFrameArray[iFrame].siblingFrame != INVALID_FRAME )
            transformBindPoseFrame( m_pFrameArray[iFrame].siblingFrame, pParentWorld );

        // Transform our children
        if( m_pFrameArray[iFrame].childFrame != INVALID_FRAME )
            transformBindPoseFrame( m_pFrameArray[iFrame].childFrame, localWorld );
    }
    
    void transformFrame( int iFrame, Matrix4f pParentWorld, double fTime ){
    	// Get the tick data
        Matrix4f localTransform;
        int iTick = getAnimationKeyFromTime( fTime );

        if( INVALID_ANIMATION_DATA != m_pFrameArray[iFrame].animationDataIndex )
        {
            SDKAnimationFrameData pFrameData = m_pAnimationFrameData[ m_pFrameArray[iFrame].animationDataIndex ];
            SDKAnimationData pData = pFrameData.pAnimationData[ iTick ];

            // turn it into a matrix (Ignore scaling for now)
//            Vector3f parentPos = pData.translation;
//            D3DXMATRIX mTranslate;
//            D3DXMatrixTranslation( &mTranslate, parentPos.x, parentPos.y, parentPos.z );
//
//            D3DXQUATERNION quat;
//            D3DXMATRIX mQuat;
//            quat.w = pData->Orientation.w;
//            quat.x = pData->Orientation.x;
//            quat.y = pData->Orientation.y;
//            quat.z = pData->Orientation.z;
//            if( quat.w == 0 && quat.x == 0 && quat.y == 0 && quat.z == 0 )
//                D3DXQuaternionIdentity( &quat );
//            D3DXQuaternionNormalize( &quat, &quat );
//            D3DXMatrixRotationQuaternion( &mQuat, &quat );
//            LocalTransform = ( mQuat * mTranslate );
            
            Matrix4f quat = new Matrix4f();
            pData.orientation.toMatrix(quat);
            quat.setTranslate(pData.translation);
            localTransform = quat;
        }
        else
        {
            localTransform = m_pFrameArray[iFrame].matrix;
        }

        // Transform ourselves
//        D3DXMATRIX LocalWorld;
//        D3DXMatrixMultiply( &LocalWorld, &LocalTransform, pParentWorld );
//        m_pTransformedFrameMatrices[iFrame] = LocalWorld;
        Matrix4f localWorld = null;
    	localWorld = m_pTransformedFrameMatrices[iFrame];
    	if(localWorld == null)
    		localWorld = new Matrix4f();
    	Matrix4f.mul(pParentWorld, localTransform, localWorld);
    	m_pTransformedFrameMatrices[iFrame] = localWorld;

        // Transform our siblings
        if( m_pFrameArray[iFrame].siblingFrame != INVALID_FRAME )
            transformFrame( m_pFrameArray[iFrame].siblingFrame, pParentWorld, fTime );

        // Transform our children
        if( m_pFrameArray[iFrame].childFrame != INVALID_FRAME )
            transformFrame( m_pFrameArray[iFrame].childFrame, localWorld, fTime );
    }
    
    void transformFrameAbsolute( int iFrame, double fTime ){
    	final Matrix4f mInvTo = new Matrix4f();
    	final Matrix4f mFrom = new Matrix4f();
    	
    	int iTick = getAnimationKeyFromTime( fTime );

        if( INVALID_ANIMATION_DATA != m_pFrameArray[iFrame].animationDataIndex )
        {
            SDKAnimationFrameData pFrameData = m_pAnimationFrameData[ m_pFrameArray[iFrame].animationDataIndex ];
            SDKAnimationData pData = pFrameData.pAnimationData[ iTick ];
            SDKAnimationData pDataOrig = pFrameData.pAnimationData[ 0 ];

//            D3DXMatrixTranslation( &mTrans1, -pDataOrig->Translation.x,
//                                   -pDataOrig->Translation.y,
//                                   -pDataOrig->Translation.z );
//            D3DXMatrixTranslation( &mTrans2, pData->Translation.x,
//                                   pData->Translation.y,
//                                   pData->Translation.z );
//
//            quat1.x = pDataOrig->Orientation.x;
//            quat1.y = pDataOrig->Orientation.y;
//            quat1.z = pDataOrig->Orientation.z;
//            quat1.w = pDataOrig->Orientation.w;
//            D3DXQuaternionInverse( &quat1, &quat1 );
//            D3DXMatrixRotationQuaternion( &mRot1, &quat1 );
//            mInvTo = mTrans1 * mRot1;
            pDataOrig.orientation.toMatrix(mInvTo);
            mInvTo.transpose();  //Equal to inverse.
            Vector3f trans = pDataOrig.translation;
            mInvTo.translate(-trans.x, -trans.y, -trans.z);

//            quat2.x = pData->Orientation.x;
//            quat2.y = pData->Orientation.y;
//            quat2.z = pData->Orientation.z;
//            quat2.w = pData->Orientation.w;
//            D3DXMatrixRotationQuaternion( &mRot2, &quat2 );
//            mFrom = mRot2 * mTrans2;
            pData.orientation.toMatrix(mFrom);
            mFrom.setTranslate(pData.translation);

//            D3DXMATRIX mOutput = mInvTo * mFrom;
            Matrix4f mOutput = Matrix4f.mul(mFrom, mInvTo, mInvTo);
            m_pTransformedFrameMatrices[iFrame] = mOutput;
        }
    }

    //Direct3D 10 rendering helpers
    void  renderMesh( int iMesh,
         boolean bAdjacent,
//         ID3D10Device* pd3dDevice,
//         ID3D10EffectTechnique* pTechnique,
//         ID3D10EffectShaderResourceVariable* ptxDiffuse,
//         ID3D10EffectShaderResourceVariable* ptxNormal,
//         ID3D10EffectShaderResourceVariable* ptxSpecular,
//         ID3D10EffectVectorVariable* pvDiffuse,
//         ID3D10EffectVectorVariable* pvSpecular 
         int pTechnique,  // OpenGL Program
         int ptxDiffuse,  // diffuse texture unit, based 0
         int ptxNormal,   // normal texture unit, based 0
         int ptxSpecular, // specular texture unit, based 0 
         int pvDiffuse,   // diffuse uniform index
         int pvSpecular   // specular uniform index
         ){
    	if( 0 < getOutstandingBufferResources() )
            return;

        SDKMeshMesh pMesh = m_pMeshArray[iMesh];

        int[] strides = new int[MAX_D3D10_VERTEX_STREAMS];
        int[] offsets = new int[MAX_D3D10_VERTEX_STREAMS];
        int[] pVB     = new int[MAX_D3D10_VERTEX_STREAMS];

        if( pMesh.numVertexBuffers > MAX_D3D10_VERTEX_STREAMS )
            return;

        for( int i = 0; i < pMesh.numVertexBuffers; i++ )
        {
            pVB[i] = m_pVertexBufferArray[ pMesh.vertexBuffers[i] ].pVB11;
            strides[i] = (int)m_pVertexBufferArray[ pMesh.vertexBuffers[i] ].strideBytes;
            offsets[i] = 0;
        }

        SDKMeshIndexBufferHeader[] pIndexBufferArray;
        if( bAdjacent )
            pIndexBufferArray = m_pAdjacencyIndexBufferArray;
        else
            pIndexBufferArray = m_pIndexBufferArray;

//        ID3D10Buffer* pIB = pIndexBufferArray[ pMesh->IndexBuffer ].pIB10;
        int pIB = pIndexBufferArray[pMesh.indexBuffer].pIB11;
//        DXGI_FORMAT ibFormat = DXGI_FORMAT_R16_int;
        int ibFormat = GL_UNSIGNED_SHORT;
        switch( pIndexBufferArray[ pMesh.indexBuffer ].indexType )
        {
            case IT_16BIT:
                ibFormat = GL_UNSIGNED_SHORT;
                break;
            case IT_32BIT:
                ibFormat = GLenum.GL_UNSIGNED_INT;
                break;
        };

//        pd3dDevice->IASetVertexBuffers( 0, pMesh->NumVertexBuffers, pVB, Strides, Offsets );
//        pd3dDevice->IASetIndexBuffer( pIB, ibFormat, 0 );
        //TODO Bind Vertex Buffer here

//        D3D10_TECHNIQUE_DESC techDesc;
//        pTechnique->GetDesc( &techDesc );
        SDKMeshSubset pSubset = null;
        SDKmeshMaterial pMat = null;
        int PrimType;


//        for( int p = 0; p < techDesc.Passes; ++p )
        {
        	gl.glUseProgram(pTechnique);
            for( int subset = 0; subset < pMesh.numSubsets; subset++ )
            {
                pSubset = m_pSubsetArray[ pMesh.pSubsets[subset] ];

                PrimType = getPrimitiveType10( /*( SDKMESH_PRIMITIVE_TYPE )*/pSubset.primitiveType );
                if( bAdjacent )
                {
                    switch( PrimType )
                    {
                        case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST:
                            PrimType = D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST_ADJ;
                            break;
                        case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP:
                            PrimType = D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP_ADJ;
                            break;
                        case D3D10_PRIMITIVE_TOPOLOGY_LINELIST:
                            PrimType = D3D10_PRIMITIVE_TOPOLOGY_LINELIST_ADJ;
                            break;
                        case D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP:
                            PrimType = D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP_ADJ;
                            break;
                    }
                }
                
                PrimType = convertDXDrawCMDToGL(PrimType);

//                pd3dDevice->IASetPrimitiveTopology( PrimType );

                pMat = m_pMaterialArray[ pSubset.materialID ];
//                if( ptxDiffuse && !IsErrorResource( pMat->pDiffuseRV10 ) )
//                    ptxDiffuse->SetResource( pMat->pDiffuseRV10 );
//                if( ptxNormal && !IsErrorResource( pMat->pNormalRV10 ) )
//                    ptxNormal->SetResource( pMat->pNormalRV10 );
//                if( ptxSpecular && !IsErrorResource( pMat->pSpecularRV10 ) )
//                    ptxSpecular->SetResource( pMat->pSpecularRV10 );
//                if( pvDiffuse )
//                    pvDiffuse->SetFloatVector( pMat->Diffuse );
//                if( pvSpecular )
//                    pvSpecular->SetFloatVector( pMat->Specular );
                
                if(ptxDiffuse >=0){
	                gl.glActiveTexture(GLenum.GL_TEXTURE0 + ptxDiffuse);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pDiffuseTexture);
                }
                
                if(ptxNormal >= 0){
                    gl.glActiveTexture(GLenum.GL_TEXTURE0 + ptxNormal);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pNormalTexture);
                }
                
                if(ptxSpecular >= 0){
                    gl.glActiveTexture(GLenum.GL_TEXTURE0 + ptxSpecular);
                    gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pSpecularTexture);
                }
                
                if(pvDiffuse >= 0)
                    gl.glUniform4f(pvDiffuse, pMat.diffuse.x,pMat.diffuse.y,pMat.diffuse.z,pMat.diffuse.w);
                
                if(pvSpecular >= 0)
                    gl.glUniform4f(pvDiffuse, pMat.specular.x,pMat.specular.y,pMat.specular.z,pMat.specular.w);

//                pTechnique->GetPassByIndex( p )->Apply( 0 );

                int IndexCount = ( int )pSubset.indexCount;
                int IndexStart = ( int )pSubset.indexStart;
                int VertexStart = ( int )pSubset.vertexStart;
                if( bAdjacent )
                {
                    IndexCount *= 2;
                    IndexStart *= 2;
                }
//                pd3dDevice->DrawIndexed( IndexCount, IndexStart, VertexStart );
                gl.glDrawElementsBaseVertex(PrimType, IndexCount, ibFormat, IndexStart, VertexStart);
            }
            
            // TODO unbind resources.
        }
    }
    
    void  renderFrame(int iFrame,
          boolean bAdjacent,
//          ID3D10Device* pd3dDevice,
//          ID3D10EffectTechnique* pTechnique,
//          ID3D10EffectShaderResourceVariable* ptxDiffuse,
//          ID3D10EffectShaderResourceVariable* ptxNormal,
//          ID3D10EffectShaderResourceVariable* ptxSpecular,
//          ID3D10EffectVectorVariable* pvDiffuse,
//          ID3D10EffectVectorVariable* pvSpecular
          int pTechnique,
          int ptxDiffuse,
          int ptxNormal,
          int ptxSpecular,
          int pvDiffuse,
          int pvSpecular
    	){
    	if( m_pStaticMeshData == null || m_pFrameArray == null )
            return;

        if( m_pFrameArray[iFrame].mesh != INVALID_MESH )
        {
            renderMesh( m_pFrameArray[iFrame].mesh,
                        bAdjacent,
                        /*pd3dDevice,*/
                        pTechnique,
                        ptxDiffuse,
                        ptxNormal,
                        ptxSpecular,
                        pvDiffuse,
                        pvSpecular );
        }

        // Render our children
        if( m_pFrameArray[iFrame].childFrame != INVALID_FRAME )
            renderFrame( m_pFrameArray[iFrame].childFrame, bAdjacent, /*pd3dDevice,*/ pTechnique, ptxDiffuse, ptxNormal,
                         ptxSpecular, pvDiffuse, pvSpecular );

        // Render our siblings
        if( m_pFrameArray[iFrame].siblingFrame != INVALID_FRAME )
            renderFrame( m_pFrameArray[iFrame].siblingFrame, bAdjacent, /*pd3dDevice,*/ pTechnique, ptxDiffuse, ptxNormal,
                         ptxSpecular, pvDiffuse, pvSpecular );
    }

	void renderFrame( int iFrame,
				         boolean bAdjacent,
				//         ID3D10Device* pd3dDevice,
				         int iDiffuseSlot,
				         int iNormalSlot,
				         int iSpecularSlot ){
		if( m_pStaticMeshData == null || m_pFrameArray == null )
	        return;

	    if( m_pFrameArray[iFrame].mesh != INVALID_MESH )
	    {
	        renderMesh( m_pFrameArray[iFrame].mesh,
	                    bAdjacent,
	                    /*pd3dDevice,*/
	                    iDiffuseSlot,
	                    iNormalSlot,
	                    iSpecularSlot );
	    }

	    // Render our children
	    if( m_pFrameArray[iFrame].childFrame != INVALID_FRAME )
	        renderFrame( m_pFrameArray[iFrame].childFrame, bAdjacent, /*pd3dDevice,*/ iDiffuseSlot, 
	                     iNormalSlot, iSpecularSlot );

	    // Render our siblings
	    if( m_pFrameArray[iFrame].siblingFrame != INVALID_FRAME )
	        renderFrame( m_pFrameArray[iFrame].siblingFrame, bAdjacent, /*pd3dDevice,*/ iDiffuseSlot, 
	                     iNormalSlot, iSpecularSlot );
	}

	void renderMesh( int iMesh,
        boolean bAdjacent,
//        ID3D10Device* pd3dDevice,
        int iDiffuseSlot,
        int iNormalSlot,
        int iSpecularSlot ){
		if( 0 < getOutstandingBufferResources() )
	        return;

	    SDKMeshMesh pMesh = m_pMeshArray[iMesh];

	    int[] strides = new int[MAX_D3D10_VERTEX_STREAMS];
	    int[] offsets = new int[MAX_D3D10_VERTEX_STREAMS];
//	    ID3D10Buffer* pVB[MAX_D3D10_VERTEX_STREAMS];
	    int[] pVB     = new int[MAX_D3D10_VERTEX_STREAMS];

	    if( pMesh.numVertexBuffers > MAX_D3D10_VERTEX_STREAMS )
	        return;

	    for( int i = 0; i < pMesh.numVertexBuffers; i++ )
	    {
	        pVB[i] = m_pVertexBufferArray[ pMesh.vertexBuffers[i] ].pVB11;
	        strides[i] = (int)m_pVertexBufferArray[ pMesh.vertexBuffers[i] ].strideBytes;
	        offsets[i] = 0;
	    }

	    SDKMeshIndexBufferHeader[] pIndexBufferArray;
	    if( bAdjacent )
	        pIndexBufferArray = m_pAdjacencyIndexBufferArray;
	    else
	        pIndexBufferArray = m_pIndexBufferArray;

//	    ID3D10Buffer* pIB = pIndexBufferArray[ pMesh->IndexBuffer ].pIB10;
//	    DXGI_FORMAT ibFormat = DXGI_FORMAT_R16_int;
	    
	    int pIB = pIndexBufferArray[pMesh.indexBuffer].pIB11;
	    int ibFormat = GL_UNSIGNED_SHORT;
	    switch( pIndexBufferArray[ pMesh.indexBuffer ].indexType )
	    {
	    case IT_16BIT:
	        ibFormat = GL_UNSIGNED_SHORT;
	        break;
	    case IT_32BIT:
	        ibFormat = GLenum.GL_UNSIGNED_INT;
	        break;
	    };

//	    pd3dDevice->IASetVertexBuffers( 0, pMesh->NumVertexBuffers, pVB, Strides, Offsets );
//	    pd3dDevice->IASetIndexBuffer( pIB, ibFormat, 0 );

	    SDKMeshSubset pSubset = null;
	    SDKmeshMaterial pMat = null;
	    int PrimType;

	    // TODO Binding Vertex Buffers.
	    gl.glBindBuffer(GLenum.GL_ELEMENT_ARRAY_BUFFER, pIB);
	    for( int subset = 0; subset < pMesh.numSubsets; subset++ )
	    {
	        pSubset = m_pSubsetArray[ pMesh.pSubsets[subset] ];

	        PrimType = getPrimitiveType10( /*( SDKMESH_PRIMITIVE_TYPE )*/pSubset.primitiveType );
	        if( bAdjacent )
	        {
	            switch( PrimType )
	            {
	            case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST:
	                PrimType = D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST_ADJ;
	                break;
	            case D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP:
	                PrimType = D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP_ADJ;
	                break;
	            case D3D10_PRIMITIVE_TOPOLOGY_LINELIST:
	                PrimType = D3D10_PRIMITIVE_TOPOLOGY_LINELIST_ADJ;
	                break;
	            case D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP:
	                PrimType = D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP_ADJ;
	                break;
	            }
	        }
	        
	        int glcmd = convertDXDrawCMDToGL(PrimType);

//	        pd3dDevice->IASetPrimitiveTopology( PrimType );

	        pMat = m_pMaterialArray[ pSubset.materialID ];
//	        if( iDiffuseSlot != INVALID_SAMPLER_SLOT && !IsErrorResource( pMat->pDiffuseRV10 ) )
//	            pd3dDevice->PSSetShaderResources( iDiffuseSlot, 1, &pMat->pDiffuseRV10 );
//	        if( iNormalSlot != INVALID_SAMPLER_SLOT && !IsErrorResource( pMat->pNormalRV10 ) )
//	            pd3dDevice->PSSetShaderResources( iNormalSlot, 1, &pMat->pNormalRV10 );
//	        if( iSpecularSlot != INVALID_SAMPLER_SLOT && !IsErrorResource( pMat->pSpecularRV10 ) )
//	            pd3dDevice->PSSetShaderResources( iSpecularSlot, 1, &pMat->pSpecularRV10 );
	        
	        gl.glActiveTexture(GLenum.GL_TEXTURE0 + iDiffuseSlot);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pDiffuseTexture);  // TODO Don't forget sampler
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + iNormalSlot);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pNormalTexture);  // TODO Don't forget sampler
            gl.glActiveTexture(GLenum.GL_TEXTURE0 + iSpecularSlot);
            gl.glBindTexture(GLenum.GL_TEXTURE_2D, pMat.pSpecularTexture);  // TODO Don't forget sampler

	        int IndexCount = ( int )pSubset.indexCount;
	        int IndexStart = ( int )pSubset.indexStart;
	        int VertexStart = ( int )pSubset.vertexStart;
	        if( bAdjacent )
	        {
	            IndexCount *= 2;
	            IndexStart *= 2;
	        }
	        
//	        pd3dDevice->DrawIndexed( IndexCount, IndexStart, VertexStart );
            gl.glDrawElementsBaseVertex(glcmd, IndexCount, ibFormat, IndexStart, VertexStart);
	    }
	}
	
	
//--------------------------------------------------------------------------------------


//Direct3D 9 rendering helpers
//	void  renderMesh( int iMesh,
//         LPDIRECT3DDEVICE9 pd3dDevice,
//         LPD3DXEFFECT pEffect,
//         D3DXHANDLE hTechnique,
//         D3DXHANDLE htxDiffuse,
//         D3DXHANDLE htxNormal,
//         D3DXHANDLE htxSpecular );
//void                            RenderFrame( int iFrame,
//          LPDIRECT3DDEVICE9 pd3dDevice,
//          LPD3DXEFFECT pEffect,
//          D3DXHANDLE hTechnique,
//          D3DXHANDLE htxDiffuse,
//          D3DXHANDLE htxNormal,
//          D3DXHANDLE htxSpecular );

	public void create(String fileName) throws IOException{
		create(fileName, false, null);
	}
	
    public void  create(String fileName, boolean bCreateAdjacencyIndices, SDKmeshCallbacks pLoaderCallbacks) throws IOException{
    	 createFromFile(fileName, bCreateAdjacencyIndices, pLoaderCallbacks);
     }
//virtual HRESULT                 Create( IDirect3DDevice9* pDev9, LPCTSTR szFileName, bool bCreateAdjacencyIndices=
//false, SDKMESH_CALLBACKS9* pLoaderCallbacks=NULL );
     void create(byte[] pData, int DataBytes, boolean bCreateAdjacencyIndices/*=false*/, boolean bCopyStatic/*=false*/, SDKmeshCallbacks pLoaderCallbacks/*=NULL*/ ) throws IOException{
    	 createFromMemory(pData, bCreateAdjacencyIndices, bCopyStatic, pLoaderCallbacks);
     }
//virtual HRESULT                 Create( IDirect3DDevice9* pDev9, BYTE* pData, int DataBytes,
//bool bCreateAdjacencyIndices=false, bool bCopyStatic=false,
//SDKMESH_CALLBACKS9* pLoaderCallbacks=NULL );
     void loadAnimation(String fileName ) throws IOException{
    	 try(DataInputStream reader = new DataInputStream(new BufferedInputStream(new FileInputStream(new File(fileName))))){
			/////////////////////////
			// Header
			SDKAnimationFileHeader fileheader = new SDKAnimationFileHeader();
			fileheader.read(reader);
			
			m_pAnimationData = new byte[(int) fileheader.animationDataSize];
			reader.readFully(m_pAnimationData);
			
			m_pAnimationHeader = fileheader;
			m_pAnimationFrameData = new SDKAnimationFrameData[fileheader.numFrames];
			int offset = 0;
			for(int i = 0; i < m_pAnimationFrameData.length; i++){
				m_pAnimationFrameData[i] = new SDKAnimationFrameData();
				offset = m_pAnimationFrameData[i].load(m_pAnimationData, offset);
			}
			
			for(int i = 0; i < m_pAnimationFrameData.length; i++){
				SDKAnimationData[] animaData = m_pAnimationFrameData[i].pAnimationData = new SDKAnimationData[m_pAnimationFrameData.length];
				offset = (int) m_pAnimationFrameData[i].dataOffset;
				for(int j = 0; j < m_pAnimationFrameData.length; j++){ // TODO I'm not sure
					animaData[i] = new SDKAnimationData();
					offset = animaData[i].load(m_pAnimationData, offset);
				}
				
				SDKMeshFrame pFrame =findFrame( m_pAnimationFrameData[i].frameName);
				if(pFrame != null)
					pFrame.animationDataIndex = i;
			}
    	 }
     }
     void                    destroy(){
    	 if(m_pStaticMeshData != null){
    		 if(m_pMaterialArray != null){
    			 for( int m = 0; m < m_pMeshHeader.numMaterials; m++ )
    	            {
    				   SDKmeshMaterial mat = m_pMaterialArray[m];
    				   
    				   if(mat.pDiffuseTexture != 0){
                           gl.glDeleteTexture(mat.pDiffuseTexture);
    					   mat.pDiffuseTexture = 0;
    				   }
    				   
    				   if(mat.pNormalTexture != 0){
                           gl.glDeleteTexture(mat.pNormalTexture);
    					   mat.pNormalTexture = 0;
    				   }
    				   
    				   if(mat.pSpecularTexture != 0){
                           gl.glDeleteTexture(mat.pSpecularTexture);
    					   mat.pSpecularTexture = 0;
    				   }
    	            }
    		 }
    	 }
    	 
    	 for( int i = 0; i < m_pMeshHeader.numVertexBuffers; i++ )
         {
//             if( !IsErrorResource( m_pVertexBufferArray[i].pVB9 ) )
//                 SAFE_RELEASE( m_pVertexBufferArray[i].pVB9 );
    		 if( m_pVertexBufferArray[i].pVB11 != 0){
                 gl.glDeleteBuffer( m_pVertexBufferArray[i].pVB11);
    			 m_pVertexBufferArray[i].pVB11 = 0;
    		 }
         }

         for( int i = 0; i < m_pMeshHeader.numIndexBuffers; i++ )
         {
//             if( !IsErrorResource( m_pIndexBufferArray[i].pIB9 ) )
//                 SAFE_RELEASE( m_pIndexBufferArray[i].pIB9 );
        	 
        	 if( m_pIndexBufferArray[i].pIB11 != 0){
                 gl.glDeleteBuffer( m_pIndexBufferArray[i].pIB11);
    			 m_pIndexBufferArray[i].pIB11 = 0;
    		 }
         }
         
         if(m_pAdjacencyIndexBufferArray != null){
        	 for( int i = 0; i < m_pMeshHeader.numIndexBuffers; i++ ){
        		 if( m_pAdjacencyIndexBufferArray[i].pIB11 != 0){
                     gl.glDeleteBuffer( m_pAdjacencyIndexBufferArray[i].pIB11);
        			 m_pAdjacencyIndexBufferArray[i].pIB11 = 0;
        		 }
        	 }
         }
     }
     

     //Frame manipulation
     void transformBindPose( Matrix4f pWorld ) {transformBindPoseFrame(0, pWorld);}
     void transformMesh( Matrix4f pWorld, double fTime ){
    	 if( m_pAnimationHeader  == null)
    	        return;

	    if( FTT_RELATIVE == m_pAnimationHeader.frameTransformType )
	    {
	        transformFrame( 0, pWorld, fTime );

	        // For each frame, move the transform to the bind pose, then
	        // move it to the final position
	        Matrix4f mInvBindPose = new Matrix4f();
	        for( int i = 0; i < m_pMeshHeader.numFrames; i++ )
	        {
//	            D3DXMatrixInverse( &mInvBindPose, NULL, &m_pBindPoseFrameMatrices[i] );
//	            mFinal = mInvBindPose * m_pTransformedFrameMatrices[i];
//	            m_pTransformedFrameMatrices[i] = mFinal;
	        	
	        	if(m_pBindPoseFrameMatrices[i] == null){
	        		m_pBindPoseFrameMatrices[i] = new Matrix4f();
	        		mInvBindPose.setIdentity();
	        	}else     	
	        		Matrix4f.invert(m_pBindPoseFrameMatrices[i], mInvBindPose);
	        	Matrix4f mFinal = m_pTransformedFrameMatrices[i];
	        	if(mFinal == null)
	        		mFinal = new Matrix4f();
	        	
	        	Matrix4f.mul(mFinal, mInvBindPose, mFinal);
	        	m_pTransformedFrameMatrices[i] = mFinal;
	        }
	    }
	    else if( FTT_ABSOLUTE == m_pAnimationHeader.frameTransformType )
	    {
	        for( int i = 0; i < m_pAnimationHeader.numFrames; i++ )
	            transformFrameAbsolute( i, fTime );
	    }
     }

//Adjacency
     void createAdjacencyIndices( /*ID3D10Device* pd3dDevice,*/ float fEpsilon, byte[] pBufferData ){
    	 // TODO Not implement yet.
     }

	//Direct3D 10 Rendering
	//--------------------------------------------------------------------------------------
	public void render( 
			/*ID3D10Device* pd3dDevice,*/
		int iDiffuseSlot,
		int iNormalSlot,
		int iSpecularSlot ){
		renderFrame(0, false, iDiffuseSlot, iNormalSlot, iSpecularSlot);
	}

	void render( 
			/*ID3D10Device* pd3dDevice,*/
//ID3D10EffectTechnique* pTechnique,
//ID3D10EffectShaderResourceVariable* ptxDiffuse = NULL,
//ID3D10EffectShaderResourceVariable* ptxNormal = NULL,
//ID3D10EffectShaderResourceVariable* ptxSpecular = NULL,
//ID3D10EffectVectorVariable* pvDiffuse = NULL,
//ID3D10EffectVectorVariable* pvSpecular = NULL
			int pTechnique,
	          int ptxDiffuse,
	          int ptxNormal,
	          int ptxSpecular,
	          int pvDiffuse,
	          int pvSpecular
    ){
		renderFrame(0, false, pTechnique, ptxDiffuse, ptxNormal, ptxSpecular, pvDiffuse, pvSpecular);
	}
//virtual void                    RenderAdjacent( ID3D10Device* pd3dDevice,
//        ID3D10EffectTechnique* pTechnique,
//        ID3D10EffectShaderResourceVariable* ptxDiffuse = NULL,
//        ID3D10EffectShaderResourceVariable* ptxNormal = NULL,
//        ID3D10EffectShaderResourceVariable* ptxSpecular = NULL,
//        ID3D10EffectVectorVariable* pvDiffuse = NULL,
//        ID3D10EffectVectorVariable* pvSpecular = NULL );
////Direct3D 9 Rendering
//virtual void                    Render( LPDIRECT3DDEVICE9 pd3dDevice,
//LPD3DXEFFECT pEffect,
//D3DXHANDLE hTechnique,
//D3DXHANDLE htxDiffuse = 0,
//D3DXHANDLE htxNormal = 0,
//D3DXHANDLE htxSpecular = 0 );


//Helpers (D3D10 specific)
	public static int getPrimitiveType10( int PrimType ){
		int retType = D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST;

	    switch( PrimType )
	    {
	        case PT_TRIANGLE_LIST:
	            retType = D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST;
	            break;
	        case PT_TRIANGLE_STRIP:
	            retType = D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP;
	            break;
	        case PT_LINE_LIST:
	            retType = D3D10_PRIMITIVE_TOPOLOGY_LINELIST;
	            break;
	        case PT_LINE_STRIP:
	            retType = D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP;
	            break;
	        case PT_POINT_LIST:
	            retType = D3D10_PRIMITIVE_TOPOLOGY_POINTLIST;
	            break;
	        case PT_TRIANGLE_LIST_ADJ:
	            retType = D3D10_PRIMITIVE_TOPOLOGY_TRIANGLELIST_ADJ;
	            break;
	        case PT_TRIANGLE_STRIP_ADJ:
	            retType = D3D10_PRIMITIVE_TOPOLOGY_TRIANGLESTRIP_ADJ;
	            break;
	        case PT_LINE_LIST_ADJ:
	            retType = D3D10_PRIMITIVE_TOPOLOGY_LINELIST_ADJ;
	            break;
	        case PT_LINE_STRIP_ADJ:
	            retType = D3D10_PRIMITIVE_TOPOLOGY_LINESTRIP_ADJ;
	            break;
	    };

	    return retType;
	}
     
	public int getIBFormat10( int iMesh ){
		switch( m_pIndexBufferArray[ m_pMeshArray[ iMesh ].indexBuffer ].indexType )
	    {
	        case IT_16BIT:
	            return GLenum.GL_UNSIGNED_SHORT;
	        case IT_32BIT:
	            return GLenum.GL_UNSIGNED_INT;
	    };
	    return GLenum.GL_UNSIGNED_SHORT;
	}
	
	public int getVB10( int iMesh, int iVB ){
		return m_pVertexBufferArray[ m_pMeshArray[ iMesh ].vertexBuffers[iVB] ].pVB11;
	}

	public int getIB10( int iMesh ){
		return m_pIndexBufferArray[ m_pMeshArray[ iMesh ].indexBuffer ].pIB11;
	}
	
//	int getAdjIB10( int iMesh ){
//		
//	}
	public int getIndexType( int iMesh ){
		return m_pIndexBufferArray[m_pMeshArray[ iMesh ].indexBuffer].indexType;
	}

//Helpers (D3D9 specific)
//static D3DPRIMITIVETYPE         GetPrimitiveType9( SDKMESH_PRIMITIVE_TYPE PrimType );
//D3DFORMAT                       GetIBFormat9( int iMesh );
//IDirect3DVertexBuffer9* GetVB9( int iMesh, int iVB );
//IDirect3DIndexBuffer9* GetIB9( int iMesh );

//Helpers (general)
	String getMeshPath() { return m_strPath;}
	int  getNumMeshes(){ return m_pMeshHeader != null ? m_pMeshHeader.numMeshes : 0;}
	int  getNumMaterials() { return m_pMeshHeader != null ? m_pMeshHeader.numMaterials: 0;}
	int  getNumVBs(){ return m_pMeshHeader != null ? m_pMeshHeader.numVertexBuffers: 0;}
	int  getNumIBs() {return m_pMeshHeader != null ? m_pMeshHeader.numIndexBuffers: 0;}
//IDirect3DVertexBuffer9* GetVB9At( int iVB );
//IDirect3DIndexBuffer9* GetIB9At( int iIB );
	int  getVB10At( int iVB ) { return m_pVertexBufferArray[ iVB ].pVB11;}
	int getIB10At( int iIB ) {return m_pIndexBufferArray[ iIB ].pIB11;}
//	BYTE* GetRawVerticesAt( int iVB );
//	BYTE* GetRawIndicesAt( int iIB );
	SDKmeshMaterial getMaterial( int iMaterial ) {return m_pMaterialArray[ iMaterial ];}
	SDKMeshMesh getMesh( int iMesh ){
		return m_pMeshArray[ iMesh ];
	}
	
	public int getNumSubsets( int iMesh ){
		return m_pMeshArray[ iMesh ].numSubsets;
	}
	
	public SDKMeshSubset getSubset( int iMesh, int iSubset ){return m_pSubsetArray[ m_pMeshArray[ iMesh ].pSubsets[iSubset] ];}
	public int getVertexStride( int iMesh, int iVB ){
		return ( int )m_pVertexBufferArray[ m_pMeshArray[ iMesh ].vertexBuffers[iVB] ].strideBytes;
	}
	
	SDKMeshFrame findFrame( String pszName ){
		for( int i = 0; i < m_pMeshHeader.numFrames; i++ )
	    {
//	        if( _stricmp( m_pFrameArray[i].Name, pszName ) == 0 )
			if(m_pFrameArray[i].name.equals(pszName))
	        {
	            return m_pFrameArray[i];
	        }
	    }
	    return null;
	}
	int getNumVertices( int iMesh, int iVB ) { return (int) m_pVertexBufferArray[ m_pMeshArray[ iMesh ].vertexBuffers[iVB] ].numVertices;}
	int getNumIndices( int iMesh ) { return (int) m_pIndexBufferArray[ m_pMeshArray[ iMesh ].indexBuffer ].numVertices;}
	public Vector3f getMeshBBoxCenter( int iMesh ) {return m_pMeshArray[iMesh].boundingBoxCenter;}
	public Vector3f getMeshBBoxExtents( int iMesh ) { return m_pMeshArray[iMesh].boundingBoxExtents;}
	int getOutstandingResources() { return 0;}
	int getOutstandingBufferResources(){
		int outstandingResources = 0;
	    if( m_pMeshHeader == null)
	        return 1;

	    for( int i = 0; i < m_pMeshHeader.numVertexBuffers; i++ )
	    {
	        if( m_pVertexBufferArray[i].pVB11 == 0 )
	            outstandingResources ++;
	    }

	    for( int i = 0; i < m_pMeshHeader.numIndexBuffers; i++ )
	    {
	        if( m_pIndexBufferArray[i].pIB11 == 0 )
	            outstandingResources ++;
	    }

	    return outstandingResources;
	}
	boolean checkLoadDone(){
		if( 0 == getOutstandingResources() )
	    {
	        m_bLoading = false;
	        return true;
	    }

	    return false;
	}

	boolean  isLoaded(){ return m_pStaticMeshData != null && !m_bLoading;}
	boolean  isLoading(){ return m_bLoading;}

	void setLoading( boolean bLoading ){m_bLoading = bLoading;}
	boolean  hadLoadingError(){
		if( m_pMeshHeader !=null)
	    {
	        for( int i = 0; i < m_pMeshHeader.numVertexBuffers; i++ )
	        {
//	            if( IsErrorResource( m_pVertexBufferArray[i].pVB9 ) )
//	                return TRUE;
	        	if(m_pVertexBufferArray[i].pVB11 == 0)
	        		return true;
	        }

	        for( int i = 0; i < m_pMeshHeader.numIndexBuffers; i++ )
	        {
//	            if( IsErrorResource( m_pIndexBufferArray[i].pIB9 ) )
//	                return TRUE;
	        	if(m_pIndexBufferArray[i].pIB11 == 0)
	        		return true;
	        }
	    }

	    return false;
	}

	//Animation
	int getNumInfluences( int iMesh ){
		return m_pMeshArray[iMesh].numFrameInfluences;
	}
	
    Matrix4f getMeshInfluenceMatrix( int iMesh, int iInfluence ){
    	int iFrame = m_pMeshArray[iMesh].pFrameInfluences[ iInfluence ];  // TODO
        return m_pTransformedFrameMatrices[iFrame];
    }
    
	int getAnimationKeyFromTime( double fTime ){
		int iTick = ( int )( m_pAnimationHeader.animationFPS * fTime );
	    iTick = iTick % ( m_pAnimationHeader.numAnimationKeys - 1 );
	    iTick ++;

	    return iTick;
	}
}

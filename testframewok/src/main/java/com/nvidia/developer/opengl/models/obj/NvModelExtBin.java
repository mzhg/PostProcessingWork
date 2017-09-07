package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Matrix4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

final class NvModelExtBin extends NvModelExt{

	Material[] m_materials;
	int m_materialCount;

    // Array of all textures defined by all materials in the mesh
    final List<String> m_textures = new ArrayList<String>();
	int m_textureCount;
	
	SubMeshBin[] m_subMeshes;
	int m_meshCount;
	
	public static NvModelExtBin Create(String pFileName) throws IOException{
		NvModelExtBin model = new NvModelExtBin();
		if (null == ms_pLoader)
		{
			return null;
		}

		// Use the provided loader callback to load the file into memory
//		char *pData = ms_pLoader.LoadDataFromFile(pFileName);
		byte[] _pData = ms_pLoader.loadDataFromFile(pFileName);
		if (null == _pData)
		{
			return null;
		}

		ByteBuffer pData = ByteBuffer.wrap(_pData).order(ByteOrder.nativeOrder());
		model.LoadFromPreprocessed(pData);

		// Free the OBJ buffer
//		ms_pLoader.ReleaseData(pData);

		return model;
	}
	
	// Helper function for reading in texture descriptions
    private static void ReadTextureDescs(NvModelTextureDesc[] pSrcDescs, List<NvTextureDesc> destArray, int offset, int count)
    {
//        NvModelTextureDesc* pCurrSrcDesc = pSrcDescs + offset;
    	int currSrcDescIndex = offset;

        for (int i = 0; i < count; ++i, ++currSrcDescIndex)
        {
        	NvModelTextureDesc pCurrSrcDesc = pSrcDescs[currSrcDescIndex];
            NvTextureDesc d = new NvTextureDesc();
            d.m_textureIndex = pCurrSrcDesc._textureIndex;
            d.m_UVIndex = pCurrSrcDesc._UVIndex;
            d.m_mapModes[0] = /*static_cast<TextureDesc::MapMode>*/(pCurrSrcDesc._mapModeS);
            d.m_mapModes[1] = /*static_cast<TextureDesc::MapMode>*/(pCurrSrcDesc._mapModeT);
            d.m_mapModes[2] = /*static_cast<TextureDesc::MapMode>*/(pCurrSrcDesc._mapModeU);
            d.m_minFilter = /*static_cast<TextureDesc::FilterMode>*/(pCurrSrcDesc._minFilter);
            destArray.add(d);
        }
    }

	@Override
	public int GetMeshCount() {return m_meshCount;}

	@Override
	public SubMesh GetSubMesh(int subMeshID) {
		try {
			return m_subMeshes[subMeshID];
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public int GetMaterialCount() { return m_materialCount;}

	@Override
	public Material GetMaterial(int materialID) {
		try {
			
			if(m_materials[materialID] == null)
				m_materials[materialID] = new Material();
			return m_materials[materialID];
		} catch (Exception e) {
			System.err.println("Index out of bound! materialID = " + materialID);
			return null;
		}
	}
	
	private boolean LoadFromPreprocessed(ByteBuffer data){
		NvModelExtFileHeader  hdr = new NvModelExtFileHeader();
		int old_pos = data.position();
		hdr.load(data);
		data.position(old_pos);
		
		if (hdr._magic[0] != 'N' || hdr._magic[1] != 'V' ||
	            hdr._magic[2] != 'M' || hdr._magic[3] != 'E')
        {
            return false;
        }
		
		if(DEBUG){
			System.out.println("LoadFromPreprocessed:------------------------------");
			System.out.println(hdr);
		}
		
        switch (hdr._version)
        {
        case 1:
            return LoadFromPreprocessed_v1(data);
        case 2:
            return LoadFromPreprocessed_v2(data);
        case 3:
            return LoadFromPreprocessed_v3(data);
        case 4:
        default:
            return LoadFromPreprocessed_v4(data);
        }
	}
	boolean LoadFromPreprocessed_v1(ByteBuffer data){
//		NvModelExtFileHeader* hdr = (NvModelExtFileHeader*)data;
		NvModelExtFileHeader hdr = new NvModelExtFileHeader();
		hdr.load(data);

        for (int i = 0; i < 3; i++) {
			m_boundingBoxMin.setValue(i, hdr._boundingBoxMin[i]);;
			m_boundingBoxMax.setValue(i, hdr._boundingBoxMax[i]);;
			m_boundingBoxCenter.setValue(i, hdr._boundingBoxCenter[i]);
		}

		m_meshCount = hdr._subMeshCount;
		m_materialCount = hdr._matCount;
            m_textureCount = 0;

		m_subMeshes = new SubMeshBin[m_meshCount];
		m_materials = new Material[m_materialCount];

//        NvModelSubMeshHeader_v1* pModels = (NvModelSubMeshHeader_v1*)(data + hdr._headerSize);
		if(hdr._headerSize != NvModelExtFileHeader.SIZE)
			throw new IllegalArgumentException();
		
		NvModelSubMeshHeader_v1[] pModels = new NvModelSubMeshHeader_v1[m_meshCount];
		for(int i = 0; i < m_meshCount; i++){
			NvModelSubMeshHeader_v1 model = new NvModelSubMeshHeader_v1();
			model.load(data);
			
			pModels[i] = model;
			m_subMeshes[i] = new SubMeshBin();
		}

		// read mesh headers
		for (int i = 0; i < m_meshCount; i++) 
        {
//			SubMesh* pMesh = m_subMeshes + i;
			SubMesh pMesh = m_subMeshes[i];

            NvModelSubMeshHeader_v1  mhdr = pModels[i];
			pMesh.m_vertexCount = mhdr._vertexCount;
			pMesh.m_indexCount = mhdr._indexCount;
			pMesh.m_vertSize = mhdr._vertexSize;
			pMesh.m_normalOffset = mhdr._nOffset;
			pMesh.m_texCoordOffset = mhdr._tcOffset;
            pMesh.m_texCoordCount = (mhdr._tcOffset == -1)? 0 : 1;
			pMesh.m_tangentOffset = mhdr._sTanOffset;
            // _cOffset was not initialized properly in some older files, so
            // if the value isn't sane, ignore it
            if ((mhdr._cOffset > 0) && (mhdr._cOffset < mhdr._vertexSize))
            {
                pMesh.m_colorOffset = mhdr._cOffset;
                pMesh.m_colorCount = 1;
            }
            else
            {
                pMesh.m_colorOffset = -1;
                pMesh.m_colorCount = 0;
            }

            pMesh.m_boneIndexOffset = -1;
            pMesh.m_boneWeightOffset = -1;
            pMesh.m_bonesPerVertex = 0;

			pMesh.m_vertices = new float[pMesh.m_vertSize * pMesh.m_vertexCount];
//			memcpy(pMesh.m_vertices, data + mhdr._vertArrayBase,
//				pMesh.m_vertSize * pMesh.m_vertexCount * sizeof(float));
			for(int j = 0; j < pMesh.m_vertices.length; j++){
				pMesh.m_vertices[j] = data.getFloat(mhdr._vertArrayBase + j * 4);
			}
			
			pMesh.m_indices = new int[pMesh.m_indexCount];
//			memcpy(pMesh.m_indices, data + mhdr._indexArrayBase,
//				pMesh.m_indexCount * sizeof(uint32_t));
			for(int j = 0; j < pMesh.m_indexCount; j++){
				pMesh.m_indices[j] = data.getInt(mhdr._indexArrayBase + 4 * j);
			}
			pMesh.m_materialId = mhdr._matIndex;
		}

//		NvModelMaterialHeader_v1* pSrcMat = (NvModelMaterialHeader_v1*)
//            (data + hdr._headerSize +
//            sizeof(NvModelSubMeshHeader_v1) * m_meshCount);
		NvModelMaterialHeader_v1 pSrcMat = new NvModelMaterialHeader_v1();
		if(data.position() != hdr._headerSize + NvModelSubMeshHeader_v1.SIZE * m_meshCount){
			throw new IllegalArgumentException();
		}
		
		pSrcMat.load(data);
		// read materials
		for (int i = 0; i < m_materialCount; i++) {
			Material pDestMat = GetMaterial(i);

//            memcpy(&pDestMat.m_ambient, &(pSrcMat._ambient), 3 * sizeof(float));
//            memcpy(&pDestMat.m_diffuse, &(pSrcMat._diffuse), 3 * sizeof(float));
//            memcpy(&pDestMat.m_specular, &(pSrcMat._specular), 3 * sizeof(float));
//            memcpy(&pDestMat.m_emissive, &(pSrcMat._emissive), 3 * sizeof(float));
			pDestMat.m_ambient.load(pSrcMat._ambient, 0);
			pDestMat.m_diffuse.load(pSrcMat._diffuse, 0);
			pDestMat.m_specular.load(pSrcMat._specular, 0);
			pDestMat.m_emissive.load(pSrcMat._emissive, 0);
			
            
            pDestMat.m_alpha = pSrcMat._alpha;
            pDestMat.m_shininess = (int)pSrcMat._shininess;
            pDestMat.m_opticalDensity = pSrcMat._opticalDensity;
//            memcpy(&pDestMat.m_transmissionFilter, &(pSrcMat._transmissionFilter), 3 * sizeof(float));
            pDestMat.m_transmissionFilter.load(pSrcMat._transmissionFilter, 0);
            
            pDestMat.m_illumModel = /*(Material::IlluminationModel)*/(pSrcMat._illumModel);

//            memset(&d, 0, sizeof(TextureDesc));
                
            NvModelMaterialHeader_v1 pOldHdr = (pSrcMat);

            if (pOldHdr._ambientTexture != -1)
            {
            	NvTextureDesc d = new NvTextureDesc();
                d.m_textureIndex = pOldHdr._ambientTexture;
                pDestMat.m_ambientTextures.add(d);
            }

            if (pOldHdr._diffuseTexture != -1)
            {
            	NvTextureDesc d = new NvTextureDesc();
                d.m_textureIndex = pOldHdr._diffuseTexture;
                pDestMat.m_diffuseTextures.add(d);
            }

            if (pOldHdr._specularTexture != -1)
            {
            	NvTextureDesc d = new NvTextureDesc();
                d.m_textureIndex = pOldHdr._specularTexture;
                pDestMat.m_specularTextures.add(d);
            }

            if (pOldHdr._bumpMapTexture != -1)
            {
            	NvTextureDesc d = new NvTextureDesc();
                d.m_textureIndex = pOldHdr._bumpMapTexture;
                pDestMat.m_bumpMapTextures.add(d);
            }

            if (pOldHdr._reflectionTexture != -1)
            {
            	NvTextureDesc d = new NvTextureDesc();
                d.m_textureIndex = pOldHdr._reflectionTexture;
                pDestMat.m_reflectionTextures.add(d);
            }

            if (pOldHdr._displacementMapTexture != -1)
            {
            	NvTextureDesc d = new NvTextureDesc();
                d.m_textureIndex = pOldHdr._displacementMapTexture;
                pDestMat.m_displacementMapTextures.add(d);
            }

            if (pOldHdr._specularPowerTexture != -1)
            {
            	NvTextureDesc d = new NvTextureDesc();
                d.m_textureIndex = pOldHdr._specularPowerTexture;
                pDestMat.m_specularPowerTextures.add(d);
            }

            if (pOldHdr._alphaMapTexture != -1)
            {
            	NvTextureDesc d = new NvTextureDesc();
                d.m_textureIndex = pOldHdr._alphaMapTexture;
                pDestMat.m_alphaMapTextures.add(d);
            }

            if (pOldHdr._decalTexture != -1)
            {
            	NvTextureDesc d = new NvTextureDesc();
                d.m_textureIndex = pOldHdr._decalTexture;
                pDestMat.m_decalTextures.add(d);
            }

            // Update pSrcMat pointer to point to the next material header
//            pSrcMat = (NvModelMaterialHeader_v1*)(((char*)pSrcMat) + sizeof(NvModelMaterialHeader_v1));
            pSrcMat.load(data);
        }

		return true;
	}
	
	boolean LoadFromPreprocessed_v2(ByteBuffer data){
//		NvModelExtFileHeader_v2* hdr = (NvModelExtFileHeader_v2*)data;
		NvModelExtFileHeader_v2 hdr = new NvModelExtFileHeader_v2();
		hdr.load(data);

        for (int i = 0; i < 3; i++) {
            m_boundingBoxMin.setValue(i, hdr._boundingBoxMin[i]);;
            m_boundingBoxMax.setValue(i, hdr._boundingBoxMax[i]);;
            m_boundingBoxCenter.setValue(i, hdr._boundingBoxCenter[i]);
        }

        m_meshCount = hdr._subMeshCount;
        m_materialCount = hdr._matCount;
        m_textureCount = hdr._textureCount;

        m_subMeshes = new SubMeshBin[m_meshCount];
        m_materials = new Material[m_materialCount];

//        NvModelSubMeshHeader_v1* pModels = (NvModelSubMeshHeader_v1*)
//            (data + hdr._headerSize);
        if(data.position() != hdr._headerSize){
        	throw new IllegalArgumentException();
        }
        
        NvModelSubMeshHeader_v1[] pModels = new NvModelSubMeshHeader_v1[m_meshCount];
        for(int i = 0; i < m_meshCount; i++){
        	pModels[i] = new NvModelSubMeshHeader_v1();
        	pModels[i].load(data);
        }

        // read mesh headers
        for (int i = 0; i < m_meshCount; i++)
        {
//            SubMesh* pMesh = m_subMeshes + i;
        	SubMesh pMesh = m_subMeshes[i] = new SubMeshBin();

            NvModelSubMeshHeader_v1  mhdr = pModels[i];
            pMesh.m_vertexCount = mhdr._vertexCount;
            pMesh.m_indexCount = mhdr._indexCount;
            pMesh.m_vertSize = mhdr._vertexSize;
            pMesh.m_normalOffset = mhdr._nOffset;
            pMesh.m_texCoordOffset = mhdr._tcOffset;
            pMesh.m_texCoordCount = mhdr._tcSize;
            pMesh.m_tangentOffset = mhdr._sTanOffset;
            pMesh.m_colorOffset = mhdr._cOffset;
            pMesh.m_colorCount = (mhdr._tcOffset == -1) ? 0 : 1;;
            pMesh.m_boneIndexOffset = -1;
            pMesh.m_boneWeightOffset = -1;
            pMesh.m_bonesPerVertex = 0;

            pMesh.m_vertices = new float[pMesh.m_vertSize * pMesh.m_vertexCount];
//            memcpy(pMesh.m_vertices, data + mhdr._vertArrayBase,
//                pMesh.m_vertSize * pMesh.m_vertexCount * sizeof(float));
            for(int j = 0; j < pMesh.m_vertices.length; j++){
            	pMesh.m_vertices[j] = data.getFloat(mhdr._vertArrayBase + j * 4);
            }
            pMesh.m_indices = new int[pMesh.m_indexCount];
//            memcpy(pMesh.m_indices, data + mhdr._indexArrayBase,
//                pMesh.m_indexCount * sizeof(uint32_t));
            for(int j = 0; j < pMesh.m_indices.length; j++){
            	pMesh.m_indices[j] = data.getInt(mhdr._indexArrayBase + j * 4);
            }
            pMesh.m_materialId = mhdr._matIndex;
        }

//        NvModelMaterialHeader* pSrcMat = (NvModelMaterialHeader*)
//            (data + hdr._headerSize +
//            sizeof(NvModelSubMeshHeader_v1) * m_meshCount);
        if(data.position() != hdr._headerSize + NvModelSubMeshHeader_v1.SIZE * m_meshCount){
        	throw new IllegalArgumentException();
        }
        NvModelMaterialHeader pSrcMat = new NvModelMaterialHeader();
        int src_mat_position = data.position();
        pSrcMat.load(data);

        // read materials
        for (int i = 0; i < m_materialCount; i++) {
            Material pDestMat = GetMaterial(i);

//            memcpy(&pDestMat.m_ambient, &(pSrcMat._ambient), 3 * sizeof(float));
//            memcpy(&pDestMat.m_diffuse, &(pSrcMat._diffuse), 3 * sizeof(float));
//            memcpy(&pDestMat.m_specular, &(pSrcMat._specular), 3 * sizeof(float));
//            memcpy(&pDestMat.m_emissive, &(pSrcMat._emissive), 3 * sizeof(float));
            pDestMat.m_ambient.load(pSrcMat._ambient, 0);
            pDestMat.m_diffuse.load(pSrcMat._diffuse, 0);
            pDestMat.m_specular.load(pSrcMat._specular, 0);
            pDestMat.m_emissive.load(pSrcMat._emissive, 0);
            
            pDestMat.m_alpha = pSrcMat._alpha;
            pDestMat.m_shininess = (int)pSrcMat._shininess;
            pDestMat.m_opticalDensity = pSrcMat._opticalDensity;
//            memcpy(&pDestMat.m_transmissionFilter, &(pSrcMat._transmissionFilter), 3 * sizeof(float));
            pDestMat.m_transmissionFilter.load(pSrcMat._transmissionFilter, 0);

            pDestMat.m_illumModel = /*(Material::IlluminationModel)*/(pSrcMat._illumModel);

            // Read texture desc array by back-computing the beginning of our texture desc array from the end of our material header
//            NvModelTextureDesc* pTextureDesc = (NvModelTextureDesc*)(((char*)pSrcMat) + pSrcMat._materialBlockSize);
            
            int numTextures = pSrcMat._ambientTextureCount +
                pSrcMat._diffuseTextureCount +
                pSrcMat._specularTextureCount +
                pSrcMat._bumpMapTextureCount +
                pSrcMat._reflectionTextureCount +
                pSrcMat._displacementMapTextureCount +
                pSrcMat._specularPowerTextureCount +
                pSrcMat._alphaMapTextureCount +
                pSrcMat._decalTextureCount;

//            pTextureDesc -= numTextures;
            NvModelTextureDesc[] pTextureDesc = new NvModelTextureDesc[numTextures];
            int data_offset = src_mat_position + pSrcMat._materialBlockSize - numTextures * NvModelTextureDesc.SIZE;
            int old_pos = data.position();
            data.position(data_offset);
//            pTextureDesc.load(data);
            for(int j = 0; j < numTextures; j++){
            	pTextureDesc[j] = new NvModelTextureDesc();
            	pTextureDesc[j].load(data);
            }
            data.position(old_pos);

            // Read in the Texture Descriptors for each of the possible types of textures
            ReadTextureDescs(pTextureDesc, pDestMat.m_ambientTextures, pSrcMat._ambientTextureOffset, pSrcMat._ambientTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_diffuseTextures, pSrcMat._diffuseTextureOffset, pSrcMat._diffuseTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_specularTextures, pSrcMat._specularTextureOffset, pSrcMat._specularTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_bumpMapTextures, pSrcMat._bumpMapTextureOffset, pSrcMat._bumpMapTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_reflectionTextures, pSrcMat._reflectionTextureOffset, pSrcMat._reflectionTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_displacementMapTextures, pSrcMat._displacementMapTextureOffset, pSrcMat._displacementMapTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_specularPowerTextures, pSrcMat._specularPowerTextureOffset, pSrcMat._specularPowerTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_alphaMapTextures, pSrcMat._alphaMapTextureOffset, pSrcMat._alphaMapTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_decalTextures, pSrcMat._decalTextureOffset, pSrcMat._decalTextureCount);

            // Update pSrcMat pointer to point to the next bit of memory after the end of the texture desc array
//            pSrcMat = (NvModelMaterialHeader*)(((char*)pSrcMat) + pSrcMat._materialBlockSize);
            if(i!= m_materialCount - 1)
            {
            	src_mat_position = data.position();
                int diff = pSrcMat._materialBlockSize;
                pSrcMat.load(data);
                
                if(data.position() - src_mat_position != diff){
                	throw new IllegalArgumentException();
                }
            }
        }

        // Read Texture names
//        m_textures.resize(m_textureCount);
        if ((hdr._version > 1) && m_textureCount > 0)
        {
            // pSrcMat currently points just past the end of the last material header, 
            // so that's where our string table offsets array starts
//            uint32_t* pOffsets = (uint32_t*)pSrcMat;
//            char* pString = (char*)(pOffsets + m_textureCount);
        	int string_offset = data.position() + m_textureCount * 4;
        
            for (int textureIndex = 0; textureIndex < m_textureCount; ++textureIndex)
            {
            	String textureName = extract(data, string_offset);
//                m_textures[textureIndex] = pString;
            	m_textures.add(textureName);
//                pString += m_textures[textureIndex].length() + 1;
            	string_offset += textureName.length() + 1;
            }
        }

        return true;
	}
	
	private static String extract(ByteBuffer buf, int offset){
		StringBuilder sb = new StringBuilder();
		for(int i = offset; i < buf.limit(); i++){
			char c = (char)buf.get(i);
			if(c == 0){
				break;
			}else{
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	
	boolean LoadFromPreprocessed_v3(ByteBuffer data){
//		NvModelExtFileHeader* hdr = (NvModelExtFileHeader*)data;
		NvModelExtFileHeader hdr = new NvModelExtFileHeader();
		hdr.load(data);

        for (int i = 0; i < 3; i++) {
        	 m_boundingBoxMin.setValue(i, hdr._boundingBoxMin[i]);;
             m_boundingBoxMax.setValue(i, hdr._boundingBoxMax[i]);;
             m_boundingBoxCenter.setValue(i, hdr._boundingBoxCenter[i]);
        }

        m_meshCount = hdr._subMeshCount;
        m_materialCount = hdr._matCount;

        m_subMeshes = new SubMeshBin[m_meshCount];
        m_materials = new Material[m_materialCount];

//        uint8_t* pCurrentBufferPointer = ReadTextureBlock(data + hdr._headerSize);
//        pCurrentBufferPointer = ReadSkeletonBlock(pCurrentBufferPointer);
//        pCurrentBufferPointer = ReadMaterials(pCurrentBufferPointer);
//        pCurrentBufferPointer = ReadMeshes_v3(pCurrentBufferPointer);
        
        ReadTextureBlock(data);
        ReadSkeletonBlock(data);
        ReadMaterials(data);
        ReadMeshes_v3(data);

        return true;
	}
	
	boolean LoadFromPreprocessed_v4(ByteBuffer data){
//		NvModelExtFileHeader* hdr = (NvModelExtFileHeader*)data;
		NvModelExtFileHeader hdr = new NvModelExtFileHeader();
		hdr.load(data);

        for (int i = 0; i < 3; i++) {
        	m_boundingBoxMin.setValue(i, hdr._boundingBoxMin[i]);;
            m_boundingBoxMax.setValue(i, hdr._boundingBoxMax[i]);;
            m_boundingBoxCenter.setValue(i, hdr._boundingBoxCenter[i]);
        }

        m_meshCount = hdr._subMeshCount;
        m_materialCount = hdr._matCount;

        m_subMeshes = new SubMeshBin[m_meshCount];
        m_materials = new Material[m_materialCount];
        
        if(DEBUG){
			System.out.println("LoadFromPreprocessed_v4:------------------------------");
			System.out.println("m_meshCount = " + m_meshCount);
			System.out.println("m_materialCount = " + m_materialCount);
		}
        
        if(data.position() != hdr._headerSize){
        	throw new IllegalArgumentException();
        }

//        uint8_t* pCurrentBufferPointer = ReadTextureBlock(data + hdr._headerSize);
//        pCurrentBufferPointer = ReadSkeletonBlock(pCurrentBufferPointer);
//        pCurrentBufferPointer = ReadMaterials(pCurrentBufferPointer);
//        pCurrentBufferPointer = ReadMeshes(pCurrentBufferPointer);
        ReadTextureBlock(data);
        ReadSkeletonBlock(data);
        ReadMaterials(data);
        ReadMeshes(data);

        return true;
	}

    ByteBuffer ReadTextureBlock(ByteBuffer pTextureBlock){
//    	NvModelTextureBlockHeader* pTBH = reinterpret_cast<NvModelTextureBlockHeader*>(pTextureBlock);
//        m_textureCount = pTBH._textureCount;
    	final int position = pTextureBlock.position();
    	NvModelTextureBlockHeader pTBH = new NvModelTextureBlockHeader();
    	pTBH.load(pTextureBlock);
    	m_textureCount = pTBH._textureCount;
    	
    	if(DEBUG){
			System.out.println("ReadTextureBlock:------------------------------");
			System.out.println(pTBH);
		}

        // Read Texture names
//        m_textures.resize(m_textureCount);
        if (m_textureCount > 0)
        {
            // pSrcMat currently points just past the end of the last material header, 
            // so that's where our string table offsets array starts
//            uint32_t* pOffsets = reinterpret_cast<uint32_t*>(pTextureBlock + sizeof(NvModelTextureBlockHeader));
//            char* pStringTable = reinterpret_cast<char*>(pOffsets + m_textureCount);
            int pStringTable = pTextureBlock.position() + m_textureCount * 4;
            for (int textureIndex = 0; textureIndex < m_textureCount; ++textureIndex)
            {
//                char* pString = pStringTable + pOffsets[textureIndex];
//                m_textures[textureIndex] = pString;
            	int offset = pStringTable + pTextureBlock.getInt(position + NvModelTextureBlockHeader.SIZE + textureIndex * 4);
            	String name = extract(pTextureBlock, offset);
            	m_textures.add(name);
            	
            	if(DEBUG){
            		System.out.println("Texture Name:(" + textureIndex + ", " + name+")");
            	}
            	
            }
        }

        pTextureBlock.position(position + pTBH._textureBlockSize);
        return pTextureBlock;
    }
    
    ByteBuffer ReadSkeletonBlock(ByteBuffer pSkeletonBlock){
//    	NvModelSkeletonDataBlockHeader* pSDBH = reinterpret_cast<NvModelSkeletonDataBlockHeader*>(pSkeletonBlock);
    	final int position = pSkeletonBlock.position();
    	NvModelSkeletonDataBlockHeader pSDBH = new NvModelSkeletonDataBlockHeader();
    	pSDBH.load(pSkeletonBlock);
    	
    	if(DEBUG){
    		System.out.println("ReadSkeletonBlock:-------------------------------");
    		System.out.println(pSDBH);
    	}
    	
        int boneCount = pSDBH._boneCount;
        if (boneCount > 0)
        {
            NvSkeletonNode[] pDestNodes = new NvSkeletonNode[boneCount];
//            NvSkeletonNode pDestNode = pDestNodes;
            int destNodeIndex = 0;
            if (null != pDestNodes)
            {
            	NvModelBoneData pSrcNode = new NvModelBoneData();
//                uint8_t* pBoneData = pSkeletonBlock + sizeof(NvModelSkeletonDataBlockHeader);
                for (int boneIndex = 0; boneIndex < boneCount; ++boneIndex, ++destNodeIndex)
                {
                    // pBoneData points to the bone data structure, so retain that pointer before
                    // moving it past the structure to the next element
//                    NvModelBoneData* pSrcNode = reinterpret_cast<NvModelBoneData*>(pBoneData);
                	pSrcNode.load(pSkeletonBlock);
                    
                	NvSkeletonNode pDestNode = pDestNodes[boneIndex] = new NvSkeletonNode();
                    pDestNode.m_parentNode = pSrcNode._parentIndex;
                    pDestNode.m_parentRelTransform.load(pSrcNode._parentRelTransform, 0);
//                    pBoneData += sizeof(NvModelBoneData);

                    // Grab the name of the bone from the current data pointer then move it along
//                    pDestNode.m_name = reinterpret_cast<char*>(pBoneData);
                    pDestNode.m_name = extract(pSkeletonBlock, pSkeletonBlock.position());
//                    pBoneData += pSrcNode._nameLength;
                    pSkeletonBlock.position(pSkeletonBlock.position() + pSrcNode._nameLength);
                    
                    // Get the child indices from the current bone data pointer before moving it past
                    for (int childIndex = 0; childIndex < pSrcNode._numChildren; ++childIndex)
                    {
//                        pDestNode.m_childNodes.push_back(*(reinterpret_cast<int32_t*>(pBoneData)));
//                        pBoneData += sizeof(int32_t);
                    	pDestNode.m_childNodes.push(pSkeletonBlock.getInt());
                    	
                    }

                    // Get the mesh indices from the current bone data pointer before moving it past
                    for (int meshIndex = 0; meshIndex < pSrcNode._numMeshes; ++meshIndex)
                    {
//                        pDestNode.m_meshes.push_back(*(reinterpret_cast<uint32_t*>(pBoneData)));
//                        pBoneData += sizeof(uint32_t);
                    	pDestNode.m_meshes.push(pSkeletonBlock.getInt());
                    }
                }
                
                m_pSkeleton = new NvSkeleton(pDestNodes);
            }
        }

        pSkeletonBlock.position(position + pSDBH._skeletonBlockSize);
        return pSkeletonBlock ;
    }
    
    ByteBuffer ReadMaterials(ByteBuffer pMaterials){
//    	NvModelMaterialHeader* pSrcMat = reinterpret_cast<NvModelMaterialHeader*>(pMaterials);
    	int position = pMaterials.position();
    	
    	NvModelMaterialHeader pSrcMat = new NvModelMaterialHeader();
    	pSrcMat.load(pMaterials);
    	
    	if(DEBUG){
    		System.out.println("ReadMaterials:-------------------------");
    		System.out.println(pSrcMat);
    	}
    	
        for (int matIndex = 0; matIndex < m_materialCount; ++matIndex)
        {
            Material pDestMat = GetMaterial(matIndex);
//            memcpy(&pDestMat.m_ambient, &(pSrcMat._ambient), 3 * sizeof(float));
//            memcpy(&pDestMat.m_diffuse, &(pSrcMat._diffuse), 3 * sizeof(float));
//            memcpy(&pDestMat.m_specular, &(pSrcMat._specular), 3 * sizeof(float));
//            memcpy(&pDestMat.m_emissive, &(pSrcMat._emissive), 3 * sizeof(float));
            
            pDestMat.m_ambient.load(pSrcMat._ambient, 0);
            pDestMat.m_diffuse.load(pSrcMat._diffuse, 0);
            pDestMat.m_specular.load(pSrcMat._specular, 0);
            pDestMat.m_emissive.load(pSrcMat._emissive, 0);
            
            pDestMat.m_alpha = pSrcMat._alpha;
            pDestMat.m_shininess = (int)pSrcMat._shininess;
            pDestMat.m_opticalDensity = pSrcMat._opticalDensity;
//            memcpy(&pDestMat.m_transmissionFilter, &(pSrcMat._transmissionFilter), 3 * sizeof(float));
            pDestMat.m_transmissionFilter.load(pSrcMat._transmissionFilter, 0);

            pDestMat.m_illumModel = (pSrcMat._illumModel);

//            NvModelTextureDesc* pTextureDesc = reinterpret_cast<NvModelTextureDesc*>(reinterpret_cast<uint8_t*>(pSrcMat) + sizeof(NvModelMaterialHeader));
            if(matIndex == 0 && pMaterials.position() - position != NvModelMaterialHeader.SIZE){
            	throw new IllegalArgumentException();
            }
            
            int numTextures = pSrcMat._ambientTextureCount +
                    pSrcMat._diffuseTextureCount +
                    pSrcMat._specularTextureCount +
                    pSrcMat._bumpMapTextureCount +
                    pSrcMat._reflectionTextureCount +
                    pSrcMat._displacementMapTextureCount +
                    pSrcMat._specularPowerTextureCount +
                    pSrcMat._alphaMapTextureCount +
                    pSrcMat._decalTextureCount;

//                pTextureDesc -= numTextures;
            NvModelTextureDesc[] pTextureDesc = new NvModelTextureDesc[numTextures];
            for(int j = 0; j < numTextures; j++){
            	pTextureDesc[j] = new NvModelTextureDesc();
            	pTextureDesc[j].load(pMaterials);
            }

            // Read in the Texture Descriptors for each of the possible types of textures
            ReadTextureDescs(pTextureDesc, pDestMat.m_ambientTextures, pSrcMat._ambientTextureOffset, pSrcMat._ambientTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_diffuseTextures, pSrcMat._diffuseTextureOffset, pSrcMat._diffuseTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_specularTextures, pSrcMat._specularTextureOffset, pSrcMat._specularTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_bumpMapTextures, pSrcMat._bumpMapTextureOffset, pSrcMat._bumpMapTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_reflectionTextures, pSrcMat._reflectionTextureOffset, pSrcMat._reflectionTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_displacementMapTextures, pSrcMat._displacementMapTextureOffset, pSrcMat._displacementMapTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_specularPowerTextures, pSrcMat._specularPowerTextureOffset, pSrcMat._specularPowerTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_alphaMapTextures, pSrcMat._alphaMapTextureOffset, pSrcMat._alphaMapTextureCount);
            ReadTextureDescs(pTextureDesc, pDestMat.m_decalTextures, pSrcMat._decalTextureOffset, pSrcMat._decalTextureCount);

            // Update pSrcMat pointer to point to the next bit of memory after the end of the texture desc array
//            pSrcMat = reinterpret_cast<NvModelMaterialHeader*>(reinterpret_cast<uint8_t*>(pSrcMat) + pSrcMat._materialBlockSize);
//            if(pSrcMat._materialBlockSize != NvModelMaterialHeader.SIZE){
//            	throw new IllegalArgumentException();
//            }
            
            position += pSrcMat._materialBlockSize;
            pMaterials.position(position);
            if(matIndex != m_materialCount -1){
            	pSrcMat.load(pMaterials);
            }
        }
//        return reinterpret_cast<uint8_t*>(pSrcMat);
        return pMaterials;
    }
    ByteBuffer ReadMeshes_v3(ByteBuffer pMeshes){
    	final int position = pMeshes.position();
    	// read mesh headers
    	NvModelSubMeshHeader_v3 pSrcMesh = new NvModelSubMeshHeader_v3();
        for (int i = 0; i < m_meshCount; i++)
        {
//            NvModelSubMeshHeader_v3* pSrcMesh = reinterpret_cast<NvModelSubMeshHeader_v3*>(pMeshes);
        	pSrcMesh.load(pMeshes);
//            SubMesh* pDestMesh = m_subMeshes + i;
        	SubMesh pDestMesh = m_subMeshes[i];

            pDestMesh.m_vertexCount = pSrcMesh._vertexCount;
            pDestMesh.m_indexCount = pSrcMesh._indexCount;
            pDestMesh.m_vertSize = pSrcMesh._vertexSize;
            pDestMesh.m_normalOffset = pSrcMesh._nOffset;
            pDestMesh.m_texCoordOffset = pSrcMesh._tcOffset;
            pDestMesh.m_texCoordCount = pSrcMesh._tcSize;
            pDestMesh.m_tangentOffset = pSrcMesh._sTanOffset;
            pDestMesh.m_colorOffset = pSrcMesh._cOffset;
            pDestMesh.m_colorCount = pSrcMesh._colorCount;
            pDestMesh.m_boneIndexOffset = pSrcMesh._boneIndexOffset;
            pDestMesh.m_boneWeightOffset = pSrcMesh._boneWeightOffset;
            pDestMesh.m_bonesPerVertex = pSrcMesh._bonesPerVertex;
            pDestMesh.m_materialId = pSrcMesh._matIndex;
            
            // Read in the bone map
            int boneCount = pSrcMesh._boneMapCount;
            if (boneCount > 0)
            {
//                uint8_t* pBoneMap = pMeshes + sizeof(NvModelSubMeshHeader_v3);
                int boneMapSize = /*sizeof(int32_t)*/4 * boneCount;
                pDestMesh.m_boneMap.resize(boneCount);
//                memcpy(&(pDestMesh.m_boneMap[0]), pBoneMap, boneMapSize);
                int[] pBoneData = pDestMesh.m_boneMap.getData();
                for(int j = 0; j < boneCount; j++){
                	pBoneData[j] = pMeshes.getInt();
                }
                
//                uint8_t* pBoneTransforms = pBoneMap + boneMapSize;
                int boneTransformsSize = /*sizeof(nv::matrix4f)*/Matrix4f.SIZE * boneCount;
//                pDestMesh.m_meshToBoneTransforms.resize(boneCount);
//                memcpy(&(pDestMesh.m_meshToBoneTransforms[0]), pBoneTransforms, boneTransformsSize);
                for(int j = 0; j < boneCount; j++){
                	Matrix4f mat = new Matrix4f();
                	mat.load(pMeshes);
                	pDestMesh.m_meshToBoneTransforms.add(mat);
                }
            }

            int vertBufferSize = pDestMesh.m_vertSize * pDestMesh.m_vertexCount * 4/*sizeof(float)*/;
            pDestMesh.m_vertices = new float[pDestMesh.m_vertSize * pDestMesh.m_vertexCount];
//            memcpy(pDestMesh.m_vertices, pMeshes + pSrcMesh._vertArrayBase, vertBufferSize);
            for(int j = 0; j < pDestMesh.m_vertices.length; j++){
            	pDestMesh.m_vertices[j] = pMeshes.getFloat(position + pSrcMesh._vertArrayBase + j * 4);
            }
            
            int indexBufferSize = pDestMesh.m_indexCount * 4/*sizeof(uint32_t)*/;
            pDestMesh.m_indices = new int[pDestMesh.m_indexCount];
//            memcpy(pDestMesh.m_indices, pMeshes + pSrcMesh._indexArrayBase, indexBufferSize);
            for(int j = 0; j < pDestMesh.m_indices.length; j++){
            	pDestMesh.m_indices[j] = pMeshes.getInt(position + pSrcMesh._indexArrayBase + j * 4);
            }

//            pMeshes += pSrcMesh._indexArrayBase + indexBufferSize;
            pMeshes.position(position + pSrcMesh._indexArrayBase + indexBufferSize);
        }
        return pMeshes;
    }
    ByteBuffer ReadMeshes(ByteBuffer pMeshes){
    	final int position = pMeshes.position();
    	NvModelSubMeshHeader pSrcMesh = new NvModelSubMeshHeader();
    	
    	// read mesh headers
        for (int i = 0; i < m_meshCount; i++)
        {
//            NvModelSubMeshHeader* pSrcMesh = reinterpret_cast<NvModelSubMeshHeader*>(pMeshes);
        	pSrcMesh.load(pMeshes);
        	if(m_subMeshes[i] == null)
            	m_subMeshes[i] = new SubMeshBin();
            SubMesh pDestMesh = m_subMeshes[i];

            pDestMesh.m_vertexCount = pSrcMesh._vertexCount;
            pDestMesh.m_indexCount = pSrcMesh._indexCount;
            pDestMesh.m_vertSize = pSrcMesh._vertexSize;
            pDestMesh.m_normalOffset = pSrcMesh._nOffset;
            pDestMesh.m_texCoordOffset = pSrcMesh._tcOffset;
            pDestMesh.m_texCoordCount = pSrcMesh._tcSize;
            pDestMesh.m_tangentOffset = pSrcMesh._sTanOffset;
            pDestMesh.m_colorOffset = pSrcMesh._cOffset;
            pDestMesh.m_colorCount = pSrcMesh._colorCount;
            pDestMesh.m_boneIndexOffset = pSrcMesh._boneIndexOffset;
            pDestMesh.m_boneWeightOffset = pSrcMesh._boneWeightOffset;
            pDestMesh.m_bonesPerVertex = pSrcMesh._bonesPerVertex;
            pDestMesh.m_materialId = pSrcMesh._matIndex;
            pDestMesh.m_parentBone = pSrcMesh._parentBone;

            // Read in the bone map
            int boneCount = pSrcMesh._boneMapCount;
            if (boneCount > 0)
            {
//                uint8_t* pBoneMap = pMeshes + sizeof(NvModelSubMeshHeader);
                int boneMapSize = 4/*sizeof(int32_t)*/ * boneCount;
                pDestMesh.m_boneMap.resize(boneCount);
//                memcpy(&(pDestMesh.m_boneMap[0]), pBoneMap, boneMapSize);
                int[] pBoneData = pDestMesh.m_boneMap.getData();
                for(int j = 0; j < boneCount; j++){
                	pBoneData[j] = pMeshes.getInt();
                }

//                uint8_t* pBoneTransforms = pBoneMap + boneMapSize;
                int boneTransformsSize = Matrix4f.SIZE/*sizeof(nv::matrix4f)*/ * boneCount;
//                pDestMesh.m_meshToBoneTransforms.resize(boneCount);
//                memcpy(&(pDestMesh.m_meshToBoneTransforms[0]), pBoneTransforms, boneTransformsSize);
                for(int j = 0; j < boneCount; j++){
                	Matrix4f mat = new Matrix4f();
                	mat.load(pMeshes);
                	pDestMesh.m_meshToBoneTransforms.add(mat);
                }
            }

            int vertBufferSize = pDestMesh.m_vertSize * pDestMesh.m_vertexCount * 4/*sizeof(float)*/;
            pDestMesh.m_vertices = new float[pDestMesh.m_vertSize * pDestMesh.m_vertexCount];
//            memcpy(pDestMesh.m_vertices, pMeshes + pSrcMesh._vertArrayBase, vertBufferSize);
            for(int j = 0; j < pDestMesh.m_vertices.length; j++){
            	pDestMesh.m_vertices[j] = pMeshes.getFloat(position + pSrcMesh._vertArrayBase + j * 4);
            }

            int indexBufferSize = pDestMesh.m_indexCount * 4/*sizeof(uint32_t)*/;
            pDestMesh.m_indices = new int[pDestMesh.m_indexCount];
//            memcpy(pDestMesh.m_indices, pMeshes + pSrcMesh._indexArrayBase, indexBufferSize);
            for(int j = 0; j < pDestMesh.m_indices.length; j++){
            	pDestMesh.m_indices[j] = pMeshes.getInt(position + pSrcMesh._indexArrayBase + j * 4);
            }

//            pMeshes += pSrcMesh._indexArrayBase + indexBufferSize;
            pMeshes.position(position + pSrcMesh._indexArrayBase + indexBufferSize);
        }
        return pMeshes;
    }

	@Override
	public int GetTextureCount() {
		return m_textureCount;
	}

	@Override
	public String GetTextureName(int textureID) {
		try {
			return m_textures.get(textureID);
		} catch (Exception e) {
			return "";
		}
	}
}

package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Vector3f;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import jet.opengl.postprocessing.util.FileUtils;

public abstract class NvModelExt {
	
	public static boolean DEBUG = true;

	// Pointer to the NvModelFileLoader object to use for all File I/O operations
	protected static NvModelFileLoader ms_pLoader = new NvModelFileLoader() {
		@Override
		public byte[] loadDataFromFile(String fileName) throws IOException {
			return FileUtils.loadBytes(fileName);
		}
	};
		
	protected NvModelExt(){
		
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
	public static NvModelExt CreateFromObj(String filename, float scale,
		boolean generateNormals, boolean generateTangents,
		float vertMergeThreshold /*= 0.01f*/, float normMergeThreshold /*= 0.001f*/, int initialVertCount/* = 3000*/) throws IOException{
		return NvModelExtObj.Create(filename, scale, generateNormals, generateTangents, vertMergeThreshold, normMergeThreshold, initialVertCount);
	}
	
	private static int appendTextureDescs(List<NvModelTextureDesc> destDescs, List<NvTextureDesc> srcDescs, int currentOffset, int[] outOffset)
    {
        if (srcDescs.isEmpty())
        {
            outOffset[0] = -1;
            return 0;
        }
        outOffset[0] = currentOffset;

        // Add all of the texture descriptors to our descriptor array
//        Nv::TextureDescArray::const_iterator texDescIt;
//        Nv::TextureDescArray::const_iterator texDescEnd = srcDescs.end();
//        for (texDescIt = srcDescs.begin(); texDescIt != texDescEnd; ++texDescIt)
        for(NvTextureDesc srcDesc : srcDescs)
        {
//            const TextureDesc& srcDesc = *texDescIt;
            NvModelTextureDesc destDesc = new NvModelTextureDesc();
            destDesc._textureIndex = srcDesc.m_textureIndex;
            destDesc._UVIndex = srcDesc.m_UVIndex;

            // Map modes and filter modes are equivalent between the two structures, so they can just be copied
            destDesc._mapModeS = /*static_cast<NvModelTextureDesc::MapMode>*/(srcDesc.m_mapModes[0]);
            destDesc._mapModeT = /*static_cast<NvModelTextureDesc::MapMode>*/(srcDesc.m_mapModes[1]);
            destDesc._mapModeU = /*static_cast<NvModelTextureDesc::MapMode>*/(srcDesc.m_mapModes[2]);
            destDesc._minFilter = /*static_cast<NvModelTextureDesc::FilterMode>*/(srcDesc.m_minFilter);
            destDescs.add(destDesc);
        }
        return srcDescs.size();
    }

	/**
	 * Create a model from a preprocessed "NVE" file, which is much faster and more efficient to load than OBJ
	 * @param filename path/name of the NVE file data
	 * @return a pointer to the new model
	 * @throws IOException 
	 */
	public static NvModelExt CreateFromPreprocessed(String filename) throws IOException{
		return NvModelExtBin.Create(filename);
	}

	/**
	 * Sets the file loader object to be used when requesting external files to be loaded into memory.
	 * @param pLoader Pointer to the file loader to request file data from
	 */
	public static void SetFileLoader(NvModelFileLoader pLoader) { ms_pLoader = pLoader; }

	/**
	 * Get the point defined by the minimum values in each axis contained within the axis-aligned bounding box of the model.
	 * @return Vector containing the minimum X,Y and Z of the bounding box
	 */
	public Vector3f GetMinExt() { return m_boundingBoxMin; }

	/**
	 * Get the point defined by the maximum values in each axis contained within the axis-aligned bounding box of the model.
	 * @return Vector containing the maximum X,Y and Z of the bounding box
	 */
	public Vector3f GetMaxExt() { return m_boundingBoxMax; }

	/**
	 * Get the point defined by the center of the axis-aligned bounding box of the model.
	 * @return Vector containing the center of the bounding box
	 */
	public Vector3f GetCenter() { return m_boundingBoxCenter; }

	/**
	 * Returns the number of meshes contained in the model
	 * @return Number of meshes contained in the model
	 */
    public abstract int GetMeshCount();

    /**
     * Returns the mesh contained in the model with the given ID 
     * @param subMeshID  ID, or index, of the mesh to retrieve
     * @return A pointer to the mesh with the given ID.  null if no mesh exists with that ID.
     */
	public abstract SubMesh GetSubMesh(int subMeshID);

	/**
	 * Returns the number of materials defined by the model
	 * @return Number of materials defined by the model
	 */
	public abstract int GetMaterialCount();

	/**
	 * Returns the material in the model with the given ID 
	 * @param materialID ID, or index, of the material to retrieve
	 * @return A pointer to the material with the given ID. null if no material exists with that ID.
	 */
	public abstract Material GetMaterial(int materialID);

	/**
	 * Returns the number of textures used by the model
	 * @return Number of textures used by the model
	 */
	public abstract int GetTextureCount();

	/**
	 * Returns the name of the texture in the model with the given ID
	 * @param textureID ID, or index, of the texture to retrieve
	 * @return A string containing the file name of the texture with the given ID.  Null if no texture exists with that ID.
	 */
	public abstract String GetTextureName(int textureID);

	/**
	 * Returns a pointer to the skeleton used by the model, if it exists
	 * @return Pointer to the model's skeleton or null if the model does not have one.
	 */
	public NvSkeleton GetSkeleton() { return m_pSkeleton; }

	/**
	 * Serializes the model out to a file in a binary format that can quickly be loaded back in
	 * @param filename Name of the file in which to write the model's data
	 * @return True if the model was successfully written to the file, False if an error occurred during file creation or writing.
	 */
	public boolean WritePreprocessedModel(String filename){
		File fp = new File(filename);
		File parent = fp.getParentFile();
		if(!parent.exists())
			parent.mkdirs();
		
		try(DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(filename)))){
			@SuppressWarnings("unused")
			int totalBytesWritten = 0;
	        totalBytesWritten += WriteFileHeader(out);
	        totalBytesWritten += WriteTextureBlock(out);
	        totalBytesWritten += WriteSkeletonBlock(out);
	        totalBytesWritten += WriteMaterials(out);
	        WriteMeshes(out);
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
		
		return true;
	}
	
	protected int WriteFileHeader(DataOutputStream out) throws IOException{
		
		return 0;
	}
	protected int WriteTextureBlock(DataOutputStream out) throws IOException{
		return 0;
	}
	protected int WriteSkeletonBlock(DataOutputStream out) throws IOException{
		return 0;
	}
	protected int WriteMaterials(DataOutputStream out) throws IOException{
		return 0;
	}
	protected int WriteMeshes(DataOutputStream out) throws IOException{
		return 0;
	}
	protected int WritePaddedString(File fp, String str){
		return 0;
	}

    protected int GetPaddedStringLength(String str){
    	// Account for null character and round up to the next 4-byte multiple (+1+3)/4*4
        return (((str.length() + 4) / 4) * 4); 
    }

	

    // Pointer to the skeleton for the model. NULL if it doesn't contain one.
    NvSkeleton m_pSkeleton;

	// Axis-aligned bounding box definition
	final Vector3f m_boundingBoxMin = new Vector3f();
	final Vector3f m_boundingBoxMax = new Vector3f();
	final Vector3f m_boundingBoxCenter = new Vector3f();
}

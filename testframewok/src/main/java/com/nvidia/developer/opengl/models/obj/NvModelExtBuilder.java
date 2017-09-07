package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.List;

public final class NvModelExtBuilder extends NvModelExt{

	final List<Material> m_materials = new ArrayList<>();
	final List<String> m_textures = new ArrayList<>();
	final List<SubMeshBuilder> m_subMeshes = new ArrayList<>();
	
	public static NvModelExtBuilder Create(){
		NvModelExtBuilder pModel = new NvModelExtBuilder();

		return pModel;
	}
	
	public int GetTextureId(String name){return GetTextureId(name, true);}
	public int GetTextureId(String name, boolean bAdd /*= true*/){
		if(name == null)
			return -1;
		
		int textureId;
        for (textureId = 0; textureId < m_textures.size(); ++textureId)
        {
            if (name.equals(m_textures.get(textureId)))
            {
                return textureId;
            }
        }
        if (bAdd)
        {
            m_textures.add(name);
            return textureId;
        }
        return -1;
	}

	public void UpdateBoundingBox(){
		if (m_subMeshes.isEmpty())
        {
//            m_boundingBoxMin = m_boundingBoxMax = m_boundingBoxCenter = nv::vec3f(0.0f, 0.0f, 0.0f);
			m_boundingBoxMin.set(0.0f, 0.0f, 0.0f);
			m_boundingBoxMax.set(0.0f, 0.0f, 0.0f);
			m_boundingBoxCenter.set(0.0f, 0.0f, 0.0f);
            return;
        }

		final float FLT_MAX = Float.MAX_VALUE;
        m_boundingBoxMin.set( FLT_MAX, FLT_MAX, FLT_MAX);
        m_boundingBoxMax.set(-FLT_MAX,-FLT_MAX,-FLT_MAX);

//        std::vector<SubMeshBuilder*>::iterator meshIt = m_subMeshes.begin();
//        std::vector<SubMeshBuilder*>::iterator meshEnd = m_subMeshes.end();
//        for (; meshIt != meshEnd; ++meshIt)
        final Matrix4f meshXfm = new Matrix4f();
        final Vector4f v = new Vector4f();
        for(SubMeshBuilder pSubMesh : m_subMeshes)
        {
//            const SubMesh* pSubMesh = *meshIt;
            boolean bTransformVerts = false;
//            nv::matrix4f meshXfm;
            meshXfm.setIdentity();
            if ((pSubMesh.m_parentBone != -1) && (null != m_pSkeleton))
            {
//                const nv::matrix4f* offsetXfm = m_pSkeleton.GetTransform(pSubMesh.m_parentBone);
//                meshXfm = *offsetXfm;
            	Matrix4f offsetXfm = m_pSkeleton.GetTransform(pSubMesh.m_parentBone);
            	Matrix4f.mul(meshXfm, offsetXfm, meshXfm);
                bTransformVerts = true;
            }
            
            int vert_index = 0;
            float[] pVerts = pSubMesh.getVertices();
            int vertCount = pSubMesh.getVertexCount();
            int vertSize = pSubMesh.getVertexSize();
            for (; vertCount > 0;--vertCount, vert_index += vertSize)
            {
//                nv::vec4f v(pVerts[0], pVerts[1], pVerts[2], 1.0f);
            	v.set(pVerts[vert_index + 0], pVerts[vert_index + 1], pVerts[vert_index + 2], 1.0f);
                if (bTransformVerts)
                {
//                    v = meshXfm * v;
                	Matrix4f.transform(meshXfm, v, v);
                }

                for (int comp = 0; comp < 3; ++comp)
                {
                    if (v.get(comp) < m_boundingBoxMin.get(comp))
                    {
                        m_boundingBoxMin.setValue(comp, v.get(comp));
                    }
                    if (v.get(comp) > m_boundingBoxMax.get(comp))
                    {
                        m_boundingBoxMax.setValue(comp, v.get(comp));
                    }
                }
            }
        }
//        m_boundingBoxCenter = (m_boundingBoxMax + m_boundingBoxMin) * 0.5f;
        Vector3f.add(m_boundingBoxMax, m_boundingBoxMin, m_boundingBoxCenter);
        m_boundingBoxCenter.scale(0.5f);
	}

	public void SetSkeleton(NvSkeleton pSkeleton) { m_pSkeleton = pSkeleton; }
	
	@Override
	public int GetMeshCount() { return m_subMeshes.size();}

	@Override
	public SubMesh GetSubMesh(int subMeshID) {
		try {
			return m_subMeshes.get(subMeshID);
		} catch (IndexOutOfBoundsException e) {
			return null;
		}
	}

	@Override
	public int GetMaterialCount() { return m_materials.size();}

	@Override
	public Material GetMaterial(int materialID) {
		try {
			return m_materials.get(materialID);
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
	 * Many source meshes will come in referring to their textures with a path
     * that is absolute or otherwise does not match its location on the runtime
     * system.  We provide this option to strip any path information from the 
     * texture names contained in the mesh, leaving only a filename and extension.
	 */
    public void StripTextureNamePaths()
    {
//        std::vector<std::string>::iterator textureIt = m_textures.begin();
//        std::vector<std::string>::iterator textureEnd = m_textures.end();
//        for (; textureIt != textureEnd; ++textureIt)
    	if(m_textures.isEmpty())
    		return;
    	
    	List<String> new_filenames = new ArrayList<>(m_textures.size());
    	for(String filename : m_textures)
        {
//            std::string& filename = *textureIt;
//            int delimiterPos = filename.find_last_of("/");
    		int delimiterPos = filename.lastIndexOf('/');
            if (delimiterPos != -1)
            {
                filename = filename.substring(delimiterPos + 1, -1);
            }
            delimiterPos = filename.lastIndexOf('\\');
            if (delimiterPos != -1)
            {
                filename = filename.substring(delimiterPos + 1, -1);
            }
            delimiterPos = filename.lastIndexOf(':');
            if (delimiterPos != -1)
            {
                filename = filename.substring(delimiterPos + 1, -1);
            }
            
            new_filenames.add(filename);
        }
    	
    	m_textures.clear();
    	m_textures.addAll(new_filenames);
    }

    /**
     * Since we only load .dds files at runtime, we need to be able to re-target
     * textures referred to by materials to their .dds counterparts.
     */
    public void ConvertTextureNamesToDDS()
    {
//        std::vector<std::string>::iterator textureIt = m_textures.begin();
//        std::vector<std::string>::iterator textureEnd = m_textures.end();
//        for (; textureIt != textureEnd; ++textureIt)
    	if(m_textures.isEmpty())
    		return;
    	
    	List<String> new_filenames = new ArrayList<>(m_textures.size());
    	for(String filename : m_textures)
        {
//            std::string& filename = *textureIt;
            int extPos = filename.lastIndexOf('.');
            if (extPos != -1)
            {
                filename = filename.substring(0, extPos);
            }
            filename += ".dds";
            new_filenames.add(filename);
        }
    	
    	m_textures.clear();
    	m_textures.addAll(new_filenames);
    }

    /// NvAssetLoader insists that all assets must be a direct child of an "assets"
    /// directory.  In order to keep all of the assets from being a jumbled mess,
    /// subdirectories are often created in that "assets" directory.  If the texture
    /// name does not contain the relative path from its containing assets directory,
    /// the loader will never find it.  Thus this method which allows you to prepend
    /// a string, such as "textures/", to the name of each texture used by the model.
    /// \param prefix Prefix string to be appended to each texture name used by the model.
    ///               If the intent is to prepend a path to the texture, make sure to 
    ///               include the trailing "/" character.
    public void PrependToTextureNames(String prefix)
    {
//        std::vector<std::string>::iterator textureIt = m_textures.begin();
//        std::vector<std::string>::iterator textureEnd = m_textures.end();
//        for (; textureIt != textureEnd; ++textureIt)
    	if(m_textures.isEmpty())
    		return;
    	
    	List<String> new_filenames = new ArrayList<>(m_textures.size());
    	for(String filename : m_textures)
        {
//            std::string& filename = *textureIt;
            filename = prefix + filename;
            new_filenames.add(filename);
        }
    	
    	m_textures.clear();
    	m_textures.addAll(new_filenames);
    }
}

//----------------------------------------------------------------------------------
// File:        NvGLUtils/NvMaterialGL.h
// SDK Version: v3.00 
// Email:       gameworks@nvidia.com
// Site:        http://developer.nvidia.com/
//
// Copyright (c) 2014-2015, NVIDIA CORPORATION. All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
//  * Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
//  * Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//  * Neither the name of NVIDIA CORPORATION nor the names of its
//    contributors may be used to endorse or promote products derived
//    from this software without specific prior written permission.
//
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS ``AS IS'' AND ANY
// EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
// PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
// CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
// EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
// PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
// PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
// OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
// (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
//
//----------------------------------------------------------------------------------
package com.nvidia.developer.opengl.models.obj;

import org.lwjgl.util.vector.Vector3f;

/**
 * Simple class to hold values for a material, which can be 
 * used when rendering an NvGLMeshExt
 */
public class NvMaterialGL {

	// Material Name
    public String m_name;

    // Ambient Color
    public Vector3f m_ambient;

    // Diffuse Color
    public Vector3f m_diffuse;

    // Emissive Color
    public Vector3f m_emissive;

    // Specular Color
    public Vector3f m_specular;

    // Specular Power
    public int m_shininess;

    // Translucency
    public float m_alpha;

    // Diffuse Texture Map
    public int m_diffuseTexture;

    // Bump (Normal) Texture Map
    public int m_bumpMapTexture;
    
  /// Initializes material attributes from the material in the given
    /// model at the given index.
    /// \param[in] pModel Pointer to the source model that contains the
    ///             material definitions.
    /// \param[in] materialIndex Index of the material to get properties from
    /// \return True if the material could be initialized from the requested
    ///         model and material index.  False if there was a problem and
    ///         the material could not be initialized.
    boolean InitFromMaterial(NvModelExt pModel, int materialIndex){
    	if (null == pModel)
			return false;

		Material pMaterial = pModel.GetMaterial(materialIndex);
		if (null == pMaterial)
			return false;

		m_ambient = pMaterial.m_ambient;
		m_diffuse = pMaterial.m_diffuse;
		m_emissive = pMaterial.m_emissive;
		m_specular = pMaterial.m_specular;
		m_shininess = pMaterial.m_shininess;

		m_alpha = pMaterial.m_alpha;

		if (pMaterial.m_diffuseTextures.isEmpty())
		{
			m_diffuseTexture = -1;
		}
		else
		{
			m_diffuseTexture = pMaterial.m_diffuseTextures.get(0).m_textureIndex;
		}
		if (pMaterial.m_bumpMapTextures.isEmpty())
		{
			m_bumpMapTexture = -1;
		}
		else
		{
			m_bumpMapTexture = pMaterial.m_bumpMapTextures.get(0).m_textureIndex;
		}

		return true;
    }
}

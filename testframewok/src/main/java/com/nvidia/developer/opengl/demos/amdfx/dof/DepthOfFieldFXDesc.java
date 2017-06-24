package com.nvidia.developer.opengl.demos.amdfx.dof;

import com.nvidia.developer.opengl.demos.amdfx.common.AMD_Camera;

import org.lwjgl.util.vector.Vector2i;

import jet.opengl.postprocessing.texture.Texture2D;

/**
 * Created by mazhen'gui on 2017/6/24.
 */

public class DepthOfFieldFXDesc {
    public final AMD_Camera m_Camera = new AMD_Camera();
    public final Vector2i m_screenSize = new Vector2i();
    public int  m_scaleFactor;
    public int  m_maxBlurRadius;

//    ID3D11Device*        m_pDevice;
//    ID3D11DeviceContext* m_pDeviceContext;

    public Texture2D m_pCircleOfConfusionSRV;
    public Texture2D  m_pColorSRV;
    public Texture2D m_pResultUAV;

    DepthOfFieldFXOpaqueDesc m_pOpaque;
}

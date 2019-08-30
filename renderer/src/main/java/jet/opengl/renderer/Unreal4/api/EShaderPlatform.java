package jet.opengl.renderer.Unreal4.api;

public enum EShaderPlatform {
    SP_PCD3D_SM5		/*= 0*/,
    SP_OPENGL_SM4		/*= 1*/,
    SP_PS4				/*= 2*/,
    /** Used when running in Feature Level ES2 in OpenGL. */
    SP_OPENGL_PCES2		/*= 3*/,
    SP_XBOXONE			/*= 4*/,
    SP_PCD3D_SM4		/*= 5*/,
    SP_OPENGL_SM5		/*= 6*/,
    /** Used when running in Feature Level ES2 in D3D11. */
    SP_PCD3D_ES2		/*= 7*/,
    SP_OPENGL_ES2_ANDROID /*= 8*/,
    SP_OPENGL_ES2_WEBGL /*= 9*/,
    SP_OPENGL_ES2_IOS	/*= 10*/,
    SP_METAL			/*= 11*/,
    SP_OPENGL_SM4_MAC	/*= 12*/,
    SP_METAL_MRT		/*= 13*/,
    SP_OPENGL_ES31_EXT	/*= 14*/,
    /** Used when running in Feature Level ES3_1 in D3D11. */
    SP_PCD3D_ES3_1		/*= 15*/,
    /** Used when running in Feature Level ES3_1 in OpenGL. */
    SP_OPENGL_PCES3_1	/*= 16*/,
    SP_METAL_SM5		/*= 17*/,
    SP_VULKAN_PCES3_1	/*= 18*/,
    SP_METAL_SM4		/*= 19*/,
    SP_VULKAN_SM4		/*= 20*/,
    SP_VULKAN_SM5		/*= 21*/,
    SP_VULKAN_ES3_1_ANDROID /*= 22*/,
    SP_METAL_MACES3_1 	/*= 23*/,
    SP_METAL_MACES2		/*= 24*/,
    SP_OPENGL_ES3_1_ANDROID /*= 25*/,
    SP_WOLF				/*= 26*/,
    SP_WOLF_FORWARD		/*= 27*/,

//    SP_NumPlatforms		= 28,
//    SP_NumBits			= 5,
}

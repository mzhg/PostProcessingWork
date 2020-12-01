package jet.opengl.demos.amdfx.sssr;

public class FfxSssrCreateContextInfo {
    public int apiVersion;
    public int maxReflectionViewCount;
    public int frameCountBeforeMemoryReuse;
    public int uploadBufferSize;
//    const FfxSssrLoggingCallbacks* pLoggingCallbacks; ///< Can be null.
    public String pRoughnessTextureFormat; ///< Used in the HLSL files to define the format of the resource containing surface roughness.
    public String pUnpackRoughnessSnippet; ///< Used in the HLSL files to unpack the roughness from the provided resource.
    public String pNormalsTextureFormat; ///< Used in the HLSL files to define the format of the resource containing the normals.
    public String pUnpackNormalsSnippet; ///< Used in the HLSL files to unpack the normals from the provided resource.
    public String pSceneTextureFormat; ///< Used in the HLSL files to define the format of the resource containing the rendered scene.
    public String pUnpackSceneRadianceSnippet; ///< Used in the HLSL files to unpack the rendered scene from the provided resource.
    public String pDepthTextureFormat; ///< Used in the HLSL files to define the format of the resource containing depth.
    public String pUnpackDepthSnippet; ///< Used in the HLSL files to unpack the depth values from the provided resource.
    public String pMotionVectorFormat; ///< Used in the HLSL files to define the format of the resource containing the motion vectors.
    public String pUnpackMotionVectorsSnippet; ///< Used in the HLSL files to unpack the motion vectors from the provided resource.

}

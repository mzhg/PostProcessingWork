package jet.opengl.renderer.Unreal4.distancefield;

import org.lwjgl.util.vector.Vector4f;

import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.postprocessing.texture.TextureGL;

// Come from the file "GlobalDistanceFieldParameters.h"
public class FGlobalDistanceFieldParameterData {

    public final Vector4f[] CenterAndExtent = new Vector4f[UE4Engine.GMaxGlobalDistanceFieldClipmaps];
    public final Vector4f[] WorldToUVAddAndMul = new Vector4f[UE4Engine.GMaxGlobalDistanceFieldClipmaps];
    public final TextureGL[] Textures = new TextureGL[UE4Engine.GMaxGlobalDistanceFieldClipmaps];
    public float GlobalDFResolution;
    public float MaxDistance;

    public void reset(){
        for(int i = 0; i < UE4Engine.GMaxGlobalDistanceFieldClipmaps; i++){
            CenterAndExtent[i].set(0,0,0,0);
            WorldToUVAddAndMul[i].set(0,0,0,0);
            Textures[i] = null;
        }

        GlobalDFResolution = 0;
        MaxDistance = 0;
    }
}

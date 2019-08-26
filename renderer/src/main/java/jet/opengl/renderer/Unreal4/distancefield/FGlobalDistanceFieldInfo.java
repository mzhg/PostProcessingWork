package jet.opengl.renderer.Unreal4.distancefield;

import java.util.ArrayList;

import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.postprocessing.texture.TextureGL;

public class FGlobalDistanceFieldInfo {
    public boolean bInitialized = false;
    public ArrayList<FGlobalDistanceFieldClipmap> MostlyStaticClipmaps = new ArrayList<>();
    public ArrayList<FGlobalDistanceFieldClipmap> Clipmaps = new ArrayList<>();
    public final FGlobalDistanceFieldParameterData ParameterData = new FGlobalDistanceFieldParameterData();

    public void UpdateParameterData(float MaxOcclusionDistance, int GAOGlobalDFResolution, float GAOConeHalfAngle){
        if (Clipmaps.size() > 0)
        {
            for (int ClipmapIndex = 0; ClipmapIndex < UE4Engine.GMaxGlobalDistanceFieldClipmaps; ClipmapIndex++)
            {
                TextureGL TextureValue = ClipmapIndex < Clipmaps.size()
                        ? Clipmaps.get(ClipmapIndex).RenderTarget //->GetRenderTargetItem().ShaderResourceTexture
				: null;

                ParameterData.Textures[ClipmapIndex] = TextureValue;

                if (ClipmapIndex < Clipmaps.size())
                {
				    final FGlobalDistanceFieldClipmap Clipmap = Clipmaps.get(ClipmapIndex);
//                    ParameterData.CenterAndExtent[ClipmapIndex].set(Clipmap.Bounds.GetCenter(), Clipmap.Bounds.GetExtent().X);
                    Clipmap.Bounds.center(ParameterData.CenterAndExtent[ClipmapIndex]);
                    ParameterData.CenterAndExtent[ClipmapIndex].w = Clipmap.Bounds.xMax() - ParameterData.CenterAndExtent[ClipmapIndex].x; // todo

                    // GlobalUV = (WorldPosition - GlobalVolumeCenterAndExtent[ClipmapIndex].xyz + GlobalVolumeScollOffset[ClipmapIndex].xyz) / (GlobalVolumeCenterAndExtent[ClipmapIndex].w * 2) + .5f;
                    // WorldToUVMul = 1.0f / (GlobalVolumeCenterAndExtent[ClipmapIndex].w * 2)
                    // WorldToUVAdd = (GlobalVolumeScollOffset[ClipmapIndex].xyz - GlobalVolumeCenterAndExtent[ClipmapIndex].xyz) / (GlobalVolumeCenterAndExtent[ClipmapIndex].w * 2) + .5f
//				    const FVector WorldToUVAdd = (Clipmap.ScrollOffset - Clipmap.Bounds.GetCenter()) / (Clipmap.Bounds.GetExtent().X * 2) + FVector(.5f);
//                  ParameterData.WorldToUVAddAndMul[ClipmapIndex] = FVector4(WorldToUVAdd, 1.0f / (Clipmap.Bounds.GetExtent().X * 2));

                    final float InvBoundsSize = 1/(ParameterData.CenterAndExtent[ClipmapIndex].w * 2);
                    final float WorldToUVAddX = (Clipmap.ScrollOffset.x - ParameterData.CenterAndExtent[ClipmapIndex].x) * InvBoundsSize + 0.5f;
                    final float WorldToUVAddY = (Clipmap.ScrollOffset.y - ParameterData.CenterAndExtent[ClipmapIndex].y) * InvBoundsSize + 0.5f;
                    final float WorldToUVAddZ = (Clipmap.ScrollOffset.z - ParameterData.CenterAndExtent[ClipmapIndex].z) * InvBoundsSize + 0.5f;
                    ParameterData.WorldToUVAddAndMul[ClipmapIndex].set(WorldToUVAddX, WorldToUVAddY, WorldToUVAddZ, InvBoundsSize);
                }
                else
                {
                    ParameterData.CenterAndExtent[ClipmapIndex].set(0, 0, 0, 0);
                    ParameterData.WorldToUVAddAndMul[ClipmapIndex].set(0, 0, 0, 0);
                }
            }

            ParameterData.GlobalDFResolution = GAOGlobalDFResolution;

//            extern float GAOConeHalfAngle;  comes from the file "DistanceFieldAmbientOcclusion.cpp"
		    final float GlobalMaxSphereQueryRadius = (float) (MaxOcclusionDistance / (1 + Math.tan(GAOConeHalfAngle)));
            ParameterData.MaxDistance = GlobalMaxSphereQueryRadius;
        }
        else
        {
//            FPlatformMemory::Memzero(&ParameterData, sizeof(ParameterData));
            ParameterData.reset();
        }

        bInitialized = true;
    }

}

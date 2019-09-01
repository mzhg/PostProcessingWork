package jet.opengl.renderer.Unreal4.scenes;

import jet.opengl.renderer.Unreal4.distancefield.FGlobalDistanceFieldClipmapState;

public class FSceneViewState {
    public int GlobalDistanceFieldUpdateIndex;
    public final FGlobalDistanceFieldClipmapState[] GlobalDistanceFieldClipmapState = new FGlobalDistanceFieldClipmapState[UE4Engine.GMaxGlobalDistanceFieldClipmaps];

    public boolean bInitializedGlobalDistanceFieldOrigins = false;
}

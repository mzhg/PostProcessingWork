package jet.opengl.renderer.Unreal4.scenes;

import jet.opengl.renderer.Unreal4.UE4Engine;
import jet.opengl.renderer.Unreal4.distancefield.FGlobalDistanceFieldClipmapState;

public abstract class FSceneViewState extends FSceneViewStateInterface{
    public int GlobalDistanceFieldUpdateIndex;
    public final FGlobalDistanceFieldClipmapState[] GlobalDistanceFieldClipmapState = new FGlobalDistanceFieldClipmapState[UE4Engine.GMaxGlobalDistanceFieldClipmaps];

    public boolean bInitializedGlobalDistanceFieldOrigins = false;
}

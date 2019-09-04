package jet.opengl.renderer.Unreal4;

/** Specifies which component of the scene rendering should be output to the final render target. */
public enum ESceneCaptureSource {
    SCS_SceneColorHDR /*UMETA(DisplayName="SceneColor (HDR) in RGB, Inv Opacity in A")*/,
    SCS_SceneColorHDRNoAlpha /*UMETA(DisplayName="SceneColor (HDR) in RGB, 0 in A")*/,
    SCS_FinalColorLDR /*UMETA(DisplayName="Final Color (LDR) in RGB")*/,
    SCS_SceneColorSceneDepth /*UMETA(DisplayName="SceneColor (HDR) in RGB, SceneDepth in A")*/,
    SCS_SceneDepth /*UMETA(DisplayName="SceneDepth in R")*/,
    SCS_DeviceDepth /*UMETA(DisplayName = "DeviceDepth in RGB")*/,
    SCS_Normal /*UMETA(DisplayName="Normal in RGB (Deferred Renderer only)")*/,
    SCS_BaseColor /*UMETA(DisplayName = "BaseColor in RGB (Deferred Renderer only)")*/,
    SCS_FinalColorHDR /*UMETA(DisplayName = "Final Color (HDR) in Linear sRGB gamut")*/
}

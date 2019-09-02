package jet.opengl.renderer.Unreal4.scenes;

public enum EAntiAliasingMethod {
    AAM_None /*UMETA(DisplayName="None")*/,
    AAM_FXAA /*UMETA(DisplayName="FXAA")*/,
    AAM_TemporalAA /*UMETA(DisplayName="TemporalAA")*/,
    /** Only supported with forward shading.  MSAA sample count is controlled by r.MSAACount. */
    AAM_MSAA /*UMETA(DisplayName="MSAA")*/,
//    AAM_MAX,
}

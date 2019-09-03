package jet.opengl.renderer.Unreal4.editor;

public enum EAutoExposureMethod {
    /** Not supported on mobile, requires compute shader to construct 64 bin histogram */
    AEM_Histogram  /*UMETA(DisplayName = "Auto Exposure Histogram")*/,
    /** Not supported on mobile, faster method that computes single value by downsampling */
    AEM_Basic      /*UMETA(DisplayName = "Auto Exposure Basic")*/,
    /** Uses camera settings. */
    AEM_Manual   /*UMETA(DisplayName = "Manual")*/,
}

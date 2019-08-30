package jet.opengl.renderer.Unreal4.api;

public enum ERenderQueryType {
    // e.g. WaitForFrameEventCompletion()
    RQT_Undefined,
    // Result is the number of samples that are not culled (divide by MSAACount to get pixels)
    RQT_Occlusion,
    // Result is time in micro seconds = 1/1000 ms = 1/1000000 sec
    RQT_AbsoluteTime,
}

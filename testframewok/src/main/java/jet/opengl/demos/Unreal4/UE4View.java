package jet.opengl.demos.Unreal4;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.util.Recti;

public class UE4View {
    public float FurthestReflectionCaptureDistance = 0;
    public float NearClippingDistance;
    public float FarClippingDistance;
    public int NumBoxReflectionCaptures;
    public int NumSphereReflectionCaptures;
    public FForwardLightingResources ForwardLightingResources;

    public final Matrix4f ViewMatrix = new Matrix4f();

    public final Recti ViewRect = new Recti();

    public void updateViews(int viewWidth, int viewHeight, Matrix4f view, Matrix4f proj, float cameraFar, float cameraNear){
        ViewRect.set(0,0,viewWidth, viewHeight);
        NearClippingDistance = cameraNear;
        FarClippingDistance = cameraFar;

        ViewMatrix.load(view);
    }

    private UE4View(){
        ForwardLightingResources = new FForwardLightingResources();
    }

    private static UE4View g_Instance;

    public static UE4View getInstance(){
        if(g_Instance == null)
            g_Instance = new UE4View();

        return g_Instance;
    }
}

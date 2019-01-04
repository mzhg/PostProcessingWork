package jet.opengl.demos.gpupro.rvi;

import com.nvidia.developer.opengl.app.NvSampleApp;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

public class RasterizedVoxelIGDemo extends NvSampleApp implements ICONST{
    CAMERA mainCamera;
    GLOBAL_ILLUM globalIllum;
    DIRECTIONAL_LIGHT dirLight;
    PATH_POINT_LIGHT[] pathPointLights = new PATH_POINT_LIGHT[NUM_PATH_POINT_LIGHTS];
    boolean pathLigthsEnabled;
    boolean pathLightsAnimated;
    boolean showHelp;

    @Override
    protected void initRendering() {
        DX11_RENDERER.getInstance().Create();

        if(DX11_RENDERER.getInstance().CreatePostProcessor(new DEFERRED_LIGHTING()) == null)
            return;

        // the GLOBAL_ILLUM post-processor is responsible for generating dynamic global illumination
        globalIllum = DX11_RENDERER.getInstance().CreatePostProcessor(new GLOBAL_ILLUM());

        DX11_RENDERER.getInstance().CreatePostProcessor(new SKY());

        DX11_RENDERER.getInstance().CreatePostProcessor(new FINAL_PROCESSOR());

        OnInit();
    }

    boolean OnInit()
    {
        // cache pointer to main camera
        mainCamera = DX11_RENDERER.getInstance().GetCamera(MAIN_CAMERA_ID);

        // set initial camera position/ rotation
//        mainCamera->Update(VECTOR3D(632.0f,150.0f,-142.0f),VECTOR3D(158.0f,0.0f,0.0f));

        // load sponza mesh
//        if(!DEMO::resourceManager->LoadDemoMesh("meshes/sponza.mesh")) todo
//        return false;

        // create directional light
        dirLight = DX11_RENDERER.getInstance().CreateDirectionalLight(new Vector3f(0.2403f,-0.9268f,0.2886f),new Vector4f(1.0f,1.0f,1.0f, 1),1.5f);

        // set control-points of path for all moving point-lights
        PATH_POINT_LIGHT.SetControlPoints(-1350.0f,1250.0f,-600.0f,500.0f);

        // create point lights that follow a simple path
        if(!pathPointLights[0].Init(new Vector3f(-550.0f,10.0f,500.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(1.0f,0.0f,0.0f)))
            return false;
        if(!pathPointLights[1].Init(new Vector3f(550.0f,10.0f,500.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(1.0f,0.0f,0.0f)))
            return false;
        if(!pathPointLights[2].Init(new Vector3f(-550.0f,10.0f,-600.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(-1.0f,0.0f,0.0f)))
            return false;
        if(!pathPointLights[3].Init(new Vector3f(550.0f,10.0f,-600.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(-1.0f,0.0f,0.0f)))
            return false;
        if(!pathPointLights[4].Init(new Vector3f(-1350.0f,10.0f,-30.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(0.0f,0.0f,1.0f)))
            return false;
        if(!pathPointLights[5].Init(new Vector3f(1250.0f,10.0f,-30.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(0.0f,0.0f,-1.0f)))
            return false;
        if(!pathPointLights[6].Init(new Vector3f(1250.0f,720.0f,450.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(1.0f,0.0f,0.0f)))
            return false;
        if(!pathPointLights[7].Init(new Vector3f(1200.0f,720.0f,-600.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(1.0f,0.0f,0.0f)))
            return false;
        if(!pathPointLights[8].Init(new Vector3f(-1350.0f,720.0f,460.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(-1.0f,0.0f,0.0f)))
            return false;
        if(!pathPointLights[9].Init(new Vector3f(-1320.0f,720.0f,-600.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(-1.0f,0.0f,0.0f)))
            return false;
        if(!pathPointLights[10].Init(new Vector3f(-40.0f,720.0f,500.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(0.0f,0.0f,1.0f)))
            return false;
        if(!pathPointLights[11].Init(new Vector3f(-40.0f,720.0f,-600.0f),260.0f,new Vector4f(1.0f,1.0f,0.9f,1),1.5f,new Vector3f(0.0f,0.0f,-1.0f)))
            return false;

        return true;
    }

    @Override
    public void display() {
        DX11_RENDERER.getInstance().ClearFrame();

        OnRun();

        DX11_RENDERER.getInstance().UpdateLights();

        /*int numDemoMeshes = DEMO::resourceManager->GetNumDemoMeshes();  todo  render the mesh
        for(int i=0;i<numDemoMeshes;i++)
        {
            DEMO_MESH *demoMesh = DEMO::resourceManager->GetDemoMesh(i);
            if(demoMesh->IsActive())
                demoMesh->AddSurfaces();
        }*/

        /*int numFonts = DEMO::resourceManager->GetNumFonts();
        for(int i=0;i<numFonts;i++)
        {
            FONT *font = DEMO::resourceManager->GetFont(i);
            if(font->IsActive())
                font->AddSurfaces();
        }*/

        // draw all surfaces
        DX11_RENDERER.getInstance().DrawSurfaces();
    }

    private void OnRun(){
        float dt =  getFrameDeltaTime();
        if(pathLigthsEnabled && pathLightsAnimated)
        {
            for(int i=0;i<NUM_PATH_POINT_LIGHTS;i++)
                pathPointLights[i].Update(dt);
        }
    }
}

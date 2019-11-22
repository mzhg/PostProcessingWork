package jet.opengl.demos.nvidia.waves.ocean;

import java.io.IOException;
import java.util.HashMap;

import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;

final class ShaderManager{

    private interface CreateTechnique{
        Technique newInstance();
    }

    private static Technique newTechnique(){ return new Technique();}
    private static Technique SmokeTechnique() { return new OceanSmokeTechnique();}
    private static Technique SprayTechnique() { return new OceanSprayTechnique();}
    private static Technique SurfaceTechnique() { return new OceanSurfaceTechnique();}
    private static Technique SHeightTechnique() { return new OceanSurfaceHeightTechnique();}
    private static Technique VesselTechnique()  { return new OceanVesselTechnique();}
    private static Technique SkyboxTechnique()  { return new SkyboxTechnique();}

    private static final String PATH = "nvidia/NvOcean/shaders/";

    private static final String DEFUALT_VERT = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";
    private static final String DEFUALT_FRAG = "shader_libs/PostProcessingDefaultScreenSpacePS.frag";

    private static ShaderManager gInstance;
    private ShaderManager(){
        Technique current;
        // ocean_marker fx
        mPrograms.put("RenderMarkerTech", current = createTech("MarkerVS.vert", "MarkerPS.frag"));
        current.EnableDepth().SolidNoCull().Opaque();

        //ocean_psm.fx
        mPrograms.put("PropagatePSMTech", createComputeTech("PSMPropagationCS.comp"));
        mPrograms.put("RenderPSMToUITech", current = createFragTech("PSMToUIPS.frag"));
        current.NoDepthStencil().SolidNoCull().Opaque();

        // ocean_smoke.fx
        mPrograms.put("RenderSmokeToSceneTech", current = createTech(ShaderManager::SmokeTechnique, "DummyVS.vert", ShaderType.VERTEX, "RenderParticlesToSceneGS.gemo", ShaderType.GEOMETRY, "RenderParticlesToScenePS.frag", ShaderType.FRAGMENT));
        current.ReadOnlyDepth().SolidNoCull().TranslucentBlendRGB();

        mPrograms.put("RenderSmokeToPSMTech", current = createTech(ShaderManager::SmokeTechnique,"DummyVS.vert", ShaderType.VERTEX, "RenderParticlesToPSMGS.gemo", ShaderType.GEOMETRY, "RenderParticlesToPSMPS.frag", ShaderType.FRAGMENT));
        current.NoDepthStencil().SolidNoCull().PSMBlend();

        mPrograms.put("EmitParticlesTech", createTech(ShaderManager::SmokeTechnique, "EmitParticlesCS.comp", ShaderType.COMPUTE));
        mPrograms.put("SimulateParticlesTech", createTech(ShaderManager::SmokeTechnique, "SimulateParticlesCS.comp", ShaderType.COMPUTE));
        mPrograms.put("BitonicSortTech", createTech(ShaderManager::SmokeTechnique, "BitonicSortCS.comp", ShaderType.COMPUTE));
        mPrograms.put("MatrixTransposeTech", createTech(ShaderManager::SmokeTechnique, "MatrixTransposeCS.comp", ShaderType.COMPUTE));

        // ocean_spray.fx
        mPrograms.put("RenderSprayToSceneTech", current = createTech(ShaderManager::SprayTechnique, "RenderParticlesToSceneVS.vert", ShaderType.VERTEX, "RenderParticlesToSceneHS.tesc", ShaderType.TESS_CONTROL, "RenderParticlesToSceneDS.tese", ShaderType.TESS_EVAL, "SprayRenderParticlesToScenePS.frag", ShaderType.FRAGMENT));
        current.ReadDepth().SolidNoCull().Translucent();

        mPrograms.put("RenderSprayToFoamTech", current = createTech(ShaderManager::SprayTechnique,"DummyVS.vert", ShaderType.VERTEX,"RenderParticlesToFoamGS.gemo",ShaderType.GEOMETRY, "RenderParticlesToFoamPS.frag", ShaderType.FRAGMENT));
        current.NoDepthStencil().SolidNoCull().AddBlend();

        mPrograms.put("RenderSprayToPSMTech", current = createTech(ShaderManager::SprayTechnique, "DummyVS.vert", ShaderType.VERTEX,"SprayRenderParticlesToPSMGS.gemo", ShaderType.GEOMETRY,"SprayRenderParticlesToPSMPS.frag", ShaderType.FRAGMENT));
        current.NoDepthStencil().SolidNoCull().PSMBlend();

        mPrograms.put("InitSprayParticles", createTech(ShaderManager::SprayTechnique, "InitSprayParticlesCS.comp", ShaderType.COMPUTE));
        mPrograms.put("SimulateSprayParticles", createTech(ShaderManager::SprayTechnique, "SimulateSprayParticlesCS.comp", ShaderType.COMPUTE));
        mPrograms.put("PrepareDispatchArguments", createTech(ShaderManager::SprayTechnique, "DispatchArgumentsCS.comp", ShaderType.COMPUTE));
        mPrograms.put("InitSortTech", createTech(ShaderManager::SprayTechnique, "InitSortCS.comp", ShaderType.COMPUTE));
//        mPrograms.put("BitonicSortTech", createTech(ShaderManager::SprayTechnique, "BitonicSortCS.comp", ShaderType.COMPUTE));

//        mPrograms.put("SensorVisualizationTech", current = createTech("SensorVisualizationVS.vert", "SensorVisualizationPS.frag", ShaderManager::SprayTechnique));  todo
//        current.EnableDepth().SolidNoCull().Opaque();

//        mPrograms.put("AudioVisualizationTech", current = createTech("AudioVisualizationVS.vert", "AudioVisualizationPS.frag", ShaderManager::SprayTechnique));  todo
//        current.NoDepthStencil().SolidNoCull().Translucent();

        // ocean_surface.fx
        mPrograms.put("RenderOceanSurfTech_Pass_PatchSolid", current = createTech(ShaderManager::SurfaceTechnique, "OceanWaveVS.vert", ShaderType.VERTEX, "HS_PNTriangles.tesc", ShaderType.TESS_CONTROL, "DS_PNTriangles.tese", ShaderType.TESS_EVAL, "OceanWavePS.frag", ShaderType.FRAGMENT));
        current.EnableDepth().SolidFront().Opaque();

        mPrograms.put("RenderOceanSurfTech_Pass_PatchWireframe", current = createTech(ShaderManager::SurfaceTechnique, "OceanWaveVS.vert", ShaderType.VERTEX, "HS_PNTriangles.tesc", ShaderType.TESS_CONTROL, "DS_PNTriangles.tese", ShaderType.TESS_EVAL, "MarkerPS.frag", ShaderType.FRAGMENT));
        current.EnableDepth().Wireframe().Opaque();

//        mPrograms.put("RenderMarkerTech", current = createTech("DisplayBufferVS.vert", "DisplayBufferPS.frag"));  todo
//        current.AlwaysDepth().SolidFront().Translucent();

        mPrograms.put("LocalFoamMapTech", current = createFragTech("ShiftFadeBlurLocalFoamPixelShader.frag", ShaderManager::SurfaceTechnique));
        current.DisableDepth().SolidFront().Opaque();

        // ocean_surface_heights.fx
        mPrograms.put("RenderSurfaceToReverseLookupTech", current = createTech(ShaderManager::SHeightTechnique, "RenderSurfaceToReverseLookupVS.vert", ShaderType.VERTEX, "RenderSurfaceToReverseLookupHS.tesc", ShaderType.TESS_CONTROL, "RenderSurfaceToReverseLookupDS.tese", ShaderType.TESS_EVAL, "ParticlePS.frag", ShaderType.FRAGMENT));
        current.NoDepthStencil().SolidNoCull().Opaque();

//        mPrograms.put("RenderQuadToUITech")
        mPrograms.put("RenderMarkerTech", current = createTech("MarkerVS.vert", "MarkerPS.frag", ShaderManager::newTechnique));
        current.EnableDepth().SolidNoCull().Opaque();

        // ocean_vessel.fx
        mPrograms.put("RenderVesselToSceneTech0", current=createTech("VesselVS.vert", "VesselToScenePS.frag", ShaderManager::VesselTechnique));
        current.EnableDepth().SolidBack().Opaque();

        mPrograms.put("RenderVesselToSceneTech1", current=createTech("VesselVS.vert", "VesselToScenePS.frag", ShaderManager::VesselTechnique));
        current.EnableDepth().SolidNoCull().Opaque();

        mPrograms.put("RenderVesselToShadowMapTech", current=createTech("VesselVS.vert", null, ShaderManager::VesselTechnique));
        current.EnableDepth().ShadowRS().Opaque();

        mPrograms.put("RenderVesselToHullProfileTech", current=createTech("VesselVS.vert", "VesselToHullProfilePS.frag", ShaderManager::VesselTechnique));
        current.EnableDepth().SolidBack().Opaque();

        mPrograms.put("RenderQuadToCrackFixTech", current=createTech(DEFUALT_VERT, "QuadToCrackFixPS.frag", ShaderManager::VesselTechnique));
        current.DisableDepth().SolidNoCull().Opaque();

        // skybox.fx
        mPrograms.put("SkyboxTech", current=createTech("SkyboxVS.vert","SkyboxPS.frag",ShaderManager::SkyboxTechnique));
        current.EnableDepth().SolidNoCull().Opaque();

        mPrograms.put("DownsampleDepthTech", current=createTech(DEFUALT_VERT,"DownsampleDepthPS.frag",ShaderManager::SkyboxTechnique));
        current.WriteDepth().SolidNoCull().Opaque();

        mPrograms.put("UpsampleParticlesTech", current=createTech(DEFUALT_VERT,"UpsampleParticlesPS.frag",ShaderManager::SkyboxTechnique));
        current.EnableDepth().SolidNoCull().Additive();
    }

    private static Technique createTech(String vert, String frag){
        return createTech(vert, frag, ShaderManager::newTechnique);
    }

    private static Technique createTech(String vert, String frag, CreateTechnique creator){
        Technique technique = creator.newInstance();
        try {
            if(vert != null && !vert.equals(DEFUALT_VERT)){
                vert = PATH + vert;
            }

            if(frag != null &&  !frag.equals(DEFUALT_FRAG)){
                frag = PATH + frag;
            }

            technique.setSourceFromFiles(vert, frag);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return technique;
    }

    private static  Technique createTech(CreateTechnique creator, Object...args){
        Technique technique = creator.newInstance();
        final int shaderCount = args.length/2;
        ShaderSourceItem[] items = new ShaderSourceItem[shaderCount];
        for(int i = 0; i < shaderCount; i++){
            String file = (String) args[2*i+0];
            ShaderType type = (ShaderType) args[2*i+1];
            if(!file.equals(DEFUALT_VERT) && !file.equals(DEFUALT_FRAG)){
                file = PATH + file;
            }

            CharSequence source = null;
            try {
                source = ShaderLoader.loadShaderFile(file);
            } catch (IOException e) {
                e.printStackTrace();
            }

            items[i] = new ShaderSourceItem(source, type);
        }

        technique.setSourceFromStrings(items);
        return technique;
    }

    private static Technique createComputeTech(String compute){
        return createComputeTech(compute, ShaderManager::newTechnique);
    }

    private static Technique createComputeTech(String compute, CreateTechnique creator){
        Technique technique = creator.newInstance();

        CharSequence source = null;
        try {
            source = ShaderLoader.loadShaderFile(PATH+compute);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ShaderSourceItem item = new ShaderSourceItem(source, ShaderType.COMPUTE);
        technique.setSourceFromStrings(item);

        return technique;
    }

    private static Technique createFragTech(String frag){
        return createFragTech(frag, ShaderManager::newTechnique);
    }

    private static Technique createFragTech(String frag, CreateTechnique creator){
        Technique technique = creator.newInstance();
        try {
            technique.setSourceFromFiles(DEFUALT_VERT, PATH+frag);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return technique;
    }

    private HashMap<String, Technique> mPrograms = new HashMap<>();

    public<T extends Technique> T getProgram(String name){
        return (T) mPrograms.get(name);
    }

    public static ShaderManager getInstance(){
        if(gInstance == null){
            gInstance = new ShaderManager();
        }

        return gInstance;
    }
}

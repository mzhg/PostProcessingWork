package jet.opengl.demos.nvidia.waves.crest;

import java.io.IOException;
import java.util.HashMap;

import jet.opengl.demos.nvidia.waves.ocean.Technique;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderLoader;
import jet.opengl.postprocessing.shader.ShaderSourceItem;
import jet.opengl.postprocessing.shader.ShaderType;
import jet.opengl.postprocessing.util.CommonUtil;

final class ShaderManager {

    private static final String PATH = "nvidia/crest/shaders/";

    private static final String DEFUALT_VERT = "shader_libs/PostProcessingDefaultScreenSpaceVS.vert";
    private static final String DEFUALT_FRAG = "shader_libs/PostProcessingDefaultScreenSpacePS.frag";

    private interface CreateTechnique{
        Technique newInstance();
    }

    private ShaderManager(){
        Technique current;
        mPrograms.put("Crest/Inputs/Animated Waves/Gerstner Batch0", current = createTech("AnimWavesGerstnerBatch.vert", "AnimWavesGerstnerBatch.frag", CommonUtil.toArray(new Macro("_DIRECT_TOWARDS_POINT", 0))));
        setAnimWaveStates(current);
        mPrograms.put("Crest/Inputs/Animated Waves/Gerstner Batch1", current = createTech("AnimWavesGerstnerBatch.vert", "AnimWavesGerstnerBatch.frag", CommonUtil.toArray(new Macro("_DIRECT_TOWARDS_POINT", 1))));
        setAnimWaveStates(current);

        mPrograms.put("Crest/Inputs/Dynamic Waves/Dampen Circle", current = createTech("DynWavesDampenCircle.vert","DynWavesDampenCircle.frag", null));
        setDynWavesDampenStates(current);

        mPrograms.put("Crest/Inputs/Depth/Ocean Depth From Geometry", current = createTech("OceanDepths.vert","OceanDepths.frag", null));
        setMinBlend(current);
        mPrograms.put("Crest/Inputs/Depth/Cached Depths", current = createTech("OceanDepthsCache.vert","OceanDepthsCache.frag", null));
        setMinBlend(current);

        mPrograms.put("Crest/Inputs/Animated Waves/Add From Texture00", current = createTech("AnimWavesAddFromTex.vert","AnimWavesAddFromTex.frag", Macro.asMacros("_HEIGHTSONLY_ON", 0, "_SSSFROMALPHA_ON", 0)));
        mPrograms.put("Crest/Inputs/Animated Waves/Add From Texture01", current = createTech("AnimWavesAddFromTex.vert","AnimWavesAddFromTex.frag", Macro.asMacros("_HEIGHTSONLY_ON", 0, "_SSSFROMALPHA_ON", 1)));
        mPrograms.put("Crest/Inputs/Animated Waves/Add From Texture10", current = createTech("AnimWavesAddFromTex.vert","AnimWavesAddFromTex.frag", Macro.asMacros("_HEIGHTSONLY_ON", 1, "_SSSFROMALPHA_ON", 0)));
        mPrograms.put("Crest/Inputs/Animated Waves/Add From Texture11", current = createTech("AnimWavesAddFromTex.vert","AnimWavesAddFromTex.frag", Macro.asMacros("_HEIGHTSONLY_ON", 1, "_SSSFROMALPHA_ON", 1)));
        mPrograms.put("Crest/Inputs/Animated Waves/Add Water Height From Geometry", current = createTech("AnimWavesAddHeightFromGeometry.vert","AnimWavesAddHeightFromGeometry.frag", null));
        mPrograms.put("Crest/Inputs/Animated Waves/Push Water Under Convex Hull", current = createTech("AnimWavesRemoveGeometry.vert","AnimWavesRemoveGeometry.frag", null));
        mPrograms.put("Crest/Inputs/Animated Waves/Set Water Height To Geometry", current = createTech("AnimWavesSetHeightToGeometry.vert","AnimWavesSetHeightToGeometry.frag", null));
        mPrograms.put("Crest/Inputs/Animated Waves/Wave Particle", current = createTech("AnimWavesWaveParticle.vert","AnimWavesWaveParticle.frag", null));
        mPrograms.put("Crest/Inputs/Dynamic Waves/Add Bump", current = createTech("DynWavesAddBump.vert","DynWavesAddBump.frag", null));
        mPrograms.put("Crest/Inputs/Dynamic Waves/Object Interaction", current = createTech("DynWavesObjectInteraction.vert","DynWavesObjectInteraction.frag", null));
        setDynWavesOIStates(current);

        mPrograms.put("Crest/Inputs/Flow/Fixed Direction", current = createTech("FlowFixedDirection.vert","FlowFixedDirection.frag", null));
        mPrograms.put("Crest/Inputs/Foam/Add From Texture", current = createTech("FoamAddFromTex.vert","FoamAddFromTex.frag", null));
        mPrograms.put("Crest/Inputs/Foam/Add From Vert Colours", current = createTech("FoamAddFromVertCol..vert","FoamAddFromVertCol.frag", null));
        setDynWavesOIStates(current);

        mPrograms.put("Crest/Ocean Surface Alpha", current = createTech("OceanSurfaceAlpha.vert","OceanSurfaceAlpha.frag", null));
        mPrograms.put("QueryDisplacements", current = createComputeTech("QueryDisplacements.comp"));
        mPrograms.put("QueryFlow", current = createComputeTech("QueryFlow.comp"));
        mPrograms.put("ShapeCombine", current = createComputeTech("ShapeCombine.comp"));
        mPrograms.put("UpdateDynWaves", current = createComputeTech("UpdateDynWaves.comp"));
        mPrograms.put("UpdateFoam", current = createComputeTech("UpdateFoam.comp"));
        mPrograms.put("UpdateShadow", current = createComputeTech("UpdateShadow.comp"));
        mPrograms.put("Hidden/Crest/Simulation/Combine Animated Wave LODs", current = createTech("ShapeCombine.vert","ShapeCombine.frag", null));
    }

    private static void setAnimWaveStates(Technique technique){
        technique.getBlend().blendEnable = true;
        technique.getBlend().srcBlend  = GLenum.GL_ONE;
        technique.getBlend().destBlend  = GLenum.GL_ONE;
        technique.getBlend().srcBlendAlpha  = GLenum.GL_ONE;
        technique.getBlend().destBlendAlpha  = GLenum.GL_ONE;

        technique.getDepthStencil().depthEnable  = false;
        technique.getRaster().cullFaceEnable = false;
    }

    private static void setDynWavesDampenStates(Technique technique){
        technique.getBlend().blendEnable = true;
        technique.getBlend().srcBlend  = GLenum.GL_SRC_ALPHA;
        technique.getBlend().destBlend  = GLenum.GL_ONE_MINUS_SRC_ALPHA;
        technique.getBlend().srcBlendAlpha  = GLenum.GL_SRC_ALPHA;
        technique.getBlend().destBlendAlpha  = GLenum.GL_ONE_MINUS_SRC_ALPHA;
        technique.getDepthStencil().depthEnable  = false;
        technique.getRaster().cullFaceEnable = false;
    }

    private static void setMinBlend(Technique technique){
        technique.getBlend().blendEnable = true;
        technique.getBlend().srcBlend  = GLenum.GL_ONE;
        technique.getBlend().destBlend  = GLenum.GL_ONE;
        technique.getBlend().blendOp = GLenum.GL_MIN;

        technique.getDepthStencil().depthEnable  = false;
        technique.getRaster().cullFaceEnable = false;  // todo
    }

    private static void setDynWavesOIStates(Technique technique){
        technique.getBlend().blendEnable = true;
        technique.getBlend().srcBlend  = GLenum.GL_ONE;
        technique.getBlend().destBlend  = GLenum.GL_ONE;

        technique.getDepthStencil().depthEnable  = true;
        technique.getDepthStencil().depthFunc = GLenum.GL_ALWAYS;
        technique.getDepthStencil().depthWriteMask = false;
        technique.getRaster().cullFaceEnable = false;  // todo
    }

    private static Technique newTechnique(){ return new Technique();}

    private static ShaderManager gInstance;

    private HashMap<Object, Technique> mPrograms = new HashMap<>();

    public<T extends Technique> T getProgram(Object name){
        return (T) mPrograms.get(name);
    }

    public Technique getWaveRender(WaveRenderDesc desc){
        Technique technique = mPrograms.get(desc);
        if(technique == null){
            technique = createTech("Ocean.vert", "Ocean.frag", desc.getMacros());
            mPrograms.put(desc.clone(), technique);
        }

        return technique;
    }

    public static ShaderManager getInstance(){
        if(gInstance == null){
            gInstance = new ShaderManager();
        }

        return gInstance;
    }

    private static Technique createTech(String vert, String frag){
        return createTech(vert, frag, null, ShaderManager::newTechnique);
    }

    private static Technique createTech(String vert, String frag, Macro[] macros){
        return createTech(vert, frag, macros, ShaderManager::newTechnique);
    }

    private static Technique createTech(String vert, String frag , Macro[] macros,ShaderManager.CreateTechnique creator){
        Technique technique = creator.newInstance();
        try {
            technique.setSourceFromFiles(PATH + vert, PATH+frag, macros);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return technique;
    }

    private static  Technique createTech(ShaderManager.CreateTechnique creator, Object...args){
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

    private static Technique createComputeTech(String compute, ShaderManager.CreateTechnique creator){
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

    private static Technique createFragTech(String frag, ShaderManager.CreateTechnique creator){
        Technique technique = creator.newInstance();
        try {
            technique.setSourceFromFiles(DEFUALT_VERT, PATH+frag);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return technique;
    }
}

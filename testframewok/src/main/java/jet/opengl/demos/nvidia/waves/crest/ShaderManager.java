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
        mPrograms.put("Crest/Inputs/Animated Waves/Gerstner Batch 0", current = createTech("AnimWavesGerstnerBatch.vert", "AnimWavesGerstnerBatch.frag", CommonUtil.toArray(new Macro("_DIRECT_TOWARDS_POINT", 0))));
        setAnimWaveStates(current);
        mPrograms.put("Crest/Inputs/Animated Waves/Gerstner Batch 1", current = createTech("AnimWavesGerstnerBatch.vert", "AnimWavesGerstnerBatch.frag", CommonUtil.toArray(new Macro("_DIRECT_TOWARDS_POINT", 1))));
        setAnimWaveStates(current);
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

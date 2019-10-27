package jet.opengl.demos.nvidia.waves.ocean;

import java.util.HashMap;

final class ShaderManager {

    private static ShaderManager gInstance;
    private ShaderManager(){}

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

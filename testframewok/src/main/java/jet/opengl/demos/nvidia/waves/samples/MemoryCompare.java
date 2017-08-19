package jet.opengl.demos.nvidia.waves.samples;

import jet.opengl.postprocessing.util.DebugTools;

/**
 * Created by mazhen'gui on 2017/8/9.
 */

final class MemoryCompare {

    private static final String FILE_PATH = "E:\\textures\\WaveWorks\\";

    public static void main(String[] args){
        testSimulation();
        testGradient();
    }

    static void testSimulation(){
        compares("ComputeH0_0","ComputeH0_1", "ComputeH0_2", "ComputeH0_3",
                "ComputeRow_Dt_0", "ComputeRow_Dt_1", "ComputeRow_Dt_2", "ComputeRow_Dt_3",
                "ComputeRow_Ht_0", "ComputeRow_Ht_1", "ComputeRow_Ht_2", "ComputeRow_Ht_3",
                "Displacement_0", "Displacement_1", "Displacement_2", "Displacement_3");
    }

    static void testGradient(){
        compares("FoamX0", "FoamX1", "FoamX2", "FoamX3",
                 "FoamY0", "FoamY1", "FoamY2", "FoamY3",
                 "Gradient0", "Gradient1", "Gradient2", "Gradient3");
    }

    private static void compares(String... tokens){
        for(int i = 0; i < tokens.length; i ++){
            System.out.println(String.format("test%s: ", tokens[i]));
            String gl_file = FILE_PATH + tokens[i] + ".txt";
            String dx_file = String.format(FILE_PATH + "%s_DX.txt", tokens[i]);
            String result_file = String.format(FILE_PATH + "%sResult.txt", tokens[i]);

            DebugTools.fileCompare(gl_file, dx_file, result_file);
            System.out.println();
        }
    }
}

package jet.opengl.demos.intel.cloud;

import jet.opengl.postprocessing.util.DebugTools;

/**
 * Created by mazhen'gui on 2017/7/17.
 */

final class MemCompare {

    private static final String FILE_PATH = "E:\\textures\\OutdoorCloudResources\\";

    static void testLiSpCloudTransparency(){
        System.out.println("testLiSpCloudTransparency: ");
        DebugTools.fileCompare(FILE_PATH + "LiSpCloudTransparencyGL.txt",
                FILE_PATH + "LiSpCloudTransparencyDX.txt",
                FILE_PATH + "LiSpCloudTransparencResult.txt");
        System.out.println();
    }

    static void testLiSpCloudMinMaxDepth(){
        System.out.println("testLiSpCloudMinMaxDepth: ");
        DebugTools.fileCompare(FILE_PATH + "LiSpCloudMinMaxDepthGL.txt",
                FILE_PATH + "LiSpCloudMinMaxDepthDX.txt",
                FILE_PATH + "LiSpCloudMinMaxDepthResult.txt");
        System.out.println();
    }

    static void testPrecomputeOpticalDepth(){
        System.out.println("testPrecomputeOpticalDepth: ");
        DebugTools.fileCompare(FILE_PATH + "PrecomputeOpticalDepthGL.txt",
                FILE_PATH + "PrecomputeOpticalDepthDX.txt",
                FILE_PATH + "PrecomputeOpticalDepthResult.txt");
        System.out.println();
    }

    static void testMultipleSctrInParticleLUT(){
        System.out.println("testMultipleSctrInParticleLUT: ");
        DebugTools.fileCompare(FILE_PATH + "MultipleSctrInParticleLUT_GL.txt",
                FILE_PATH + "MultipleSctrInParticleLUT_DX.txt",
                FILE_PATH + "MultipleSctrInParticleLUT_Result.txt");
        System.out.println();
    }

    static void testSingleSctrInParticleLUT(){
        System.out.println("testSingleSctrInParticleLUT: ");
        DebugTools.fileCompare(FILE_PATH + "SingleSctrInParticleLUT_GL.txt",
                FILE_PATH + "SingleSctrInParticleLUT_DX.txt",
                FILE_PATH + "SingleSctrInParticleLUT_Result.txt");
        System.out.println();
    }

    static void testProcessCloudGrid(){

        String[] tokens = {"PackedCellLocations", "CloudGrid", "ValidCellsUnorderedList", "VisibleCellsUnorderedList" };
        for(int i = 0; i < tokens.length; i ++){
            System.out.println(String.format("test%s: ", tokens[i]));
            String gl_file = String.format(FILE_PATH + "%sGL.txt", tokens[i]);
            String dx_file = String.format(FILE_PATH + "%sDX.txt", tokens[i]);
            String result_file = String.format(FILE_PATH + "%sResult.txt", tokens[i]);

            DebugTools.fileCompare(gl_file, dx_file, result_file);
            System.out.println();
        }
    }

    public static void main(String[] args) {
//        testLiSpCloudTransparency();
//        testLiSpCloudMinMaxDepth();

//        testPrecomputeOpticalDepth();
//        testMultipleSctrInParticleLUT();
//        testSingleSctrInParticleLUT();

        testProcessCloudGrid();
    }
}

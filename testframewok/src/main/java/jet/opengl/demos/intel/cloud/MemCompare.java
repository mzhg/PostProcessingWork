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

    public static void main(String[] args) {
        testLiSpCloudTransparency();
        testLiSpCloudMinMaxDepth();
    }
}

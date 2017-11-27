package jet.opengl.demos.intel.cloud;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.util.DebugTools;

import static jet.opengl.demos.intel.cloud.ProcessCloudGridTest.THREAD_GROUP_SIZE;

/**
 * Created by mazhen'gui on 2017/7/17.
 */

final class MemCompare {

    private static final String FILE_PATH = "E:\\textures\\OutdoorCloudResources\\";

    static void testCloudDensity(){
        for(int i = 0; i < 11; i++){
            String token = "MaxDensityMipMap" + i;

            System.out.println(String.format("test%s: ", token));
            String gl_file = String.format(FILE_PATH + "%sGL.txt", token);
            String dx_file = String.format(FILE_PATH + "%sDX.txt", token);
            String result_file = String.format(FILE_PATH + "%sResult.txt", token);

            DebugTools.fileCompare(gl_file, dx_file, result_file);
            System.out.println();
        }


        System.out.println();
    }

    static void testLiSpCloudTransparency(){
        System.out.println("testLiSpCloudTransparency: ");
        DebugTools.fileCompare(FILE_PATH + "LiSpCloudTransparencyGL.txt",
                FILE_PATH + "LiSpCloudTransparencyDX.txt",
                FILE_PATH + "LiSpCloudTransparencResult.txt", 1.0f);
        System.out.println();
    }

    static void testLiSpCloudMinMaxDepth(){
        System.out.println("testLiSpCloudMinMaxDepth: ");
        DebugTools.fileCompare(FILE_PATH + "LiSpCloudMinMaxDepthGL.txt",
                FILE_PATH + "LiSpCloudMinMaxDepthDX.txt",
                FILE_PATH + "LiSpCloudMinMaxDepthResult.txt", 0.0f);
        System.out.println();
    }

    static void testPrecomputeOpticalDepth(){
        System.out.println("testPrecomputeOpticalDepth: ");
        DebugTools.fileCompare(FILE_PATH + "PrecomputeOpticalDepthGL.txt",
                FILE_PATH + "PrecomputeOpticalDepthDX.txt",
                FILE_PATH + "PrecomputeOpticalDepthResult.txt", 0.0f);
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

    static int countof(String filename){
        try(BufferedReader srcIn = new BufferedReader(new FileReader(FILE_PATH + filename))){
            String line;
            int count = 0;
            while ((line = srcIn.readLine()) != null){
                StringTokenizer tokenizer = new StringTokenizer(line, ",[] \n");
                while (tokenizer.hasMoreElements()){
                    int intValue = Integer.parseInt(tokenizer.nextToken());
                    if(intValue > 0){
                        count++;
                    }
                }
            }

            return count;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    static void testDispatchArgs(int index){
        int gl_count = countof("ValidCellsUnorderedListGL.txt");
        int dx_count = countof("ValidCellsUnorderedListDX.txt");

        final int s = 2;
        int gl_dispatch = (gl_count * s*s*s * 4 + THREAD_GROUP_SIZE-1) / THREAD_GROUP_SIZE;
        int dx_dispatch = (dx_count * s*s*s * 4 + THREAD_GROUP_SIZE-1) / THREAD_GROUP_SIZE;

        System.out.printf("DispatchArgs%d: GL = %d, DX = %d, gl_count = %d, dx_count = %d .\n\n", index, gl_dispatch, dx_dispatch, gl_count, dx_count);
    }

    static void testProcessCloudGrid(){

        String[] tokens = {/*"PackedCellLocations",*/ "CloudGrid", "ValidCellsUnorderedList", "VisibleCellsUnorderedList" };
        for(int i = 0; i < tokens.length; i ++){
            System.out.println(String.format("test%s: ", tokens[i]));
            String gl_file = String.format(FILE_PATH + "%sGL.txt", tokens[i]);
            String dx_file = String.format(FILE_PATH + "%sDX.txt", tokens[i]);
            String result_file = String.format(FILE_PATH + "%sResult.txt", tokens[i]);

            if(i == 0) {
                DebugTools.fileCompare(gl_file, dx_file, result_file, -1);
            }else{
                DebugTools.fileCompareIntegerSets(gl_file, dx_file, result_file);
            }
            System.out.println();
        }
    }

    static void testEvaluateDensity(){

        String[] tokens = {"CellDensity","LightAttenuatingMass", "CloudParticles", "VisibleParticlesUnorderedList", "ParticlesLighting"};
        for(int i = 0; i < tokens.length; i ++){
            System.out.println(String.format("test%s: ", tokens[i]));
            String gl_file = String.format(FILE_PATH + "%sGL.txt", tokens[i]);
            String dx_file = String.format(FILE_PATH + "%sDX.txt", tokens[i]);
            String result_file = String.format(FILE_PATH + "%sResult.txt", tokens[i]);

            DebugTools.fileCompare(gl_file, dx_file, result_file);
            System.out.println();
        }
    }

    static void testSortParticles(){
        String[] tokens = {"VisibleParticlesSortedList","VisibleParticlesMergedList", "SerializedVisibleParticles"};
        for(int i = 0; i < tokens.length; i ++){
            System.out.println(String.format("test%s: ", tokens[i]));
            String gl_file = String.format(FILE_PATH + "%sGL.txt", tokens[i]);
            String dx_file = String.format(FILE_PATH + "%sDX.txt", tokens[i]);
            String result_file = String.format(FILE_PATH + "%sResult.txt", tokens[i]);

            DebugTools.fileCompare(gl_file, dx_file, result_file);
            System.out.println();
        }
    }

    static void testRenderFlat(){
        String[] tokens = {"ScrSpaceCloudTransparency","ScrSpaceDistToCloud", "ScreenCloudColor"};
        for(int i = 0; i < tokens.length; i ++){
            System.out.println(String.format("test%s: ", tokens[i]));
            String gl_file = String.format(FILE_PATH + "%sGL.txt", tokens[i]);
            String dx_file = String.format(FILE_PATH + "%sDX.txt", tokens[i]);
            String result_file = String.format(FILE_PATH + "%sResult.txt", tokens[i]);

            DebugTools.fileCompare(gl_file, dx_file, result_file);
            System.out.println();
        }
    }

    static void testDownscaled(){
        String[] tokens = {"DownscaledScrCloudTransparency","DownscaledScrDistToCloud", "DownscaledScrCloudColor"};
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
//        testCloudDensity();
        testLiSpCloudTransparency();
        testLiSpCloudMinMaxDepth();

        /*testPrecomputeOpticalDepth();   TODO Testing pass

        testMultipleSctrInParticleLUT();
        testSingleSctrInParticleLUT();*/

        testProcessCloudGrid();

        testDispatchArgs(0);
        testEvaluateDensity();
        testSortParticles();
        if(true) return;
        testDownscaled();
        testRenderFlat();

        /*DebugTools.genLoadBytebuffer(SGlobalCloudAttribs.class);
        DebugTools.genStoreBytebuffer(SGlobalCloudAttribs.class);*/
    }
}

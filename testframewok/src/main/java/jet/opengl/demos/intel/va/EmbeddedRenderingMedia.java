package jet.opengl.demos.intel.va;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

final class EmbeddedRenderingMedia {
    static final int BINARY_EMBEDDER_ITEM_COUNT = 22;

    // Elements (names)
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_0 = "textures:\\Font.dds";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_1 = "shaders:\\vaCanvas.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_2 = "shaders:\\vaGBuffer.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_3 = "shaders:\\vaASSAO.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_4 = "shaders:\\vaASSAO_main_disk.h";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_5 = "shaders:\\vaASSAO_types.h";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_6 = "shaders:\\vaLighting.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_7 = "shaders:\\vaMaterialShared.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_8 = "shaders:\\vaPostProcess.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_9 = "shaders:\\vaPostProcessBlur.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_10 = "shaders:\\vaPostProcessTonemap.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_11 = "shaders:\\vaRenderMesh.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_12 = "shaders:\\vaShaderCore.h";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_13 = "shaders:\\vaShared.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_14 = "shaders:\\vaSharedTypes.h";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_15 = "shaders:\\vaSharedTypes_PostProcess.h";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_16 = "shaders:\\vaSharedTypes_PostProcessBlur.h";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_17 = "shaders:\\vaSharedTypes_PostProcessTonemap.h";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_18 = "shaders:\\vaSimpleMesh.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_19 = "shaders:\\vaSimpleParticles.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_20 = "shaders:\\vaSimpleShadowMap.hlsl";
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d_21 = "shaders:\\vaSky.hlsl";


// Array of element names
    static final String s_BE_Names_de41db492a3745959d15def7ae64a41d[] = {
        s_BE_Names_de41db492a3745959d15def7ae64a41d_0,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_1,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_2,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_3,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_4,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_5,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_6,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_7,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_8,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_9,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_10,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_11,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_12,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_13,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_14,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_15,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_16,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_17,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_18,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_19,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_20,
                s_BE_Names_de41db492a3745959d15def7ae64a41d_21,
    };
// Array of element data
    static final byte[][] s_BE_Datas_de41db492a3745959d15def7ae64a41d = new byte[BINARY_EMBEDDER_ITEM_COUNT][];
    // Array of element data sizes
    static final int s_BE_Sizes_de41db492a3745959d15def7ae64a41d[] = {
            0x12960,
            0xd79,
            0xcd8,
            0x113b2,
            0x79b,
            0x1c1c,
            0x1213,
            0xcb7,
            0x1128,
            0x896,
            0x87d,
            0x2180,
            0x834,
            0x351c,
            0x20f4,
            0xdc7,
            0xa59,
            0xa7a,
            0x1724,
            0x43e3,
            0x261b,
            0xf4c,
            0x41de0,
            0x4a584,
    };
    // Array of element data timestamps
    /*static double s_BE_Times_de41db492a3745959d15def7ae64a41d[] = {
            0x48d343b781f6e17c,
            0x48d3f47fe0be46c2,
            0x48d3f47fe0b25ae0,
            0x48d45cefd487aab2,
            0x48d3eb97ba0f0ed1,
            0x48d3f47fe0c7d047,
            0x48d3f47fe0a66ef9,
            0x48d3f47fe09a8313,
            0x48d3f47fe08e971c,
            0x48d3f47fe082ab40,
            0x48d3f47fe07921ba,
            0x48d3f47fe06d35ce,
            0x48d3f47fe06149ed,
            0x48d3f47fe057c067,
            0x48d3f47fe04bd481,
            0x48d3f47fe0424afb,
            0x48d3f47fe0365f1f,
            0x48d3f47fe02a7333,
            0x48d3f47fe020e9a3,
            0x48d3f47fe0176028,
            0x48d3f47fe00911f4,
            0x48d3f47fdffd25ef,
            0x48d05cdaf0ab1610,
            0x48d05cdaf0ae7170,
    };*/

//    private static BigInteger

    static final String[] BINARY_EMBEDDER_NAMES =  s_BE_Names_de41db492a3745959d15def7ae64a41d;
    static final byte[][] BINARY_EMBEDDER_DATAS = s_BE_Datas_de41db492a3745959d15def7ae64a41d;
    static final int[] BINARY_EMBEDDER_SIZES = s_BE_Sizes_de41db492a3745959d15def7ae64a41d;

    static void load(){
        if(s_BE_Datas_de41db492a3745959d15def7ae64a41d[0] != null)
            return;

        try (BufferedReader in = new BufferedReader(new InputStreamReader(FileUtils.open("Intel/va/EmbeddedRenderingMedia.txt")))){
            String line;
            int line_index = 0;
            while ((line = in.readLine()) != null){
                int start = line.indexOf('{');
                int end = line.lastIndexOf('}');

                StringTokenizer tokenizer = new StringTokenizer(line.substring(start+1, end), " ,");

                int size = s_BE_Sizes_de41db492a3745959d15def7ae64a41d[line_index];
                byte[] bytes = new byte[size];
                for(int i = 0; i < size; i++){
                    bytes[i] = (byte) Integer.parseInt(tokenizer.nextToken());
                }

                s_BE_Datas_de41db492a3745959d15def7ae64a41d[line_index] = bytes;
                line_index ++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

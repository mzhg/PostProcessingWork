package jet.opengl.demos.nvidia.hbaoplus;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/12/9.
 */

final class Shaders {
    static final int RESOLVE_DEPTH_0 = 0;
    static final int RESOLVE_DEPTH_1 = 1;
    static final int RESOLVE_DEPTH_COUNT = 2;

    static final int FETCH_GBUFFER_NORMAL_0 = 0;
    static final int FETCH_GBUFFER_NORMAL_1 = 1;
    static final int FETCH_GBUFFER_NORMAL_2 = 2;
    static final int FETCH_GBUFFER_NORMAL_COUNT = 3;

    static final int ENABLE_BLUR_0 = 0;
    static final int ENABLE_BLUR_1 = 1;
    static final int ENABLE_BLUR_COUNT = 2;

    static final int ENABLE_SHARPNESS_PROFILE_0 = 0;
    static final int ENABLE_SHARPNESS_PROFILE_1 = 1;
    static final int ENABLE_SHARPNESS_PROFILE_COUNT = 2;

    static final int KERNEL_RADIUS_2 = 0;
    static final int KERNEL_RADIUS_4 = 1;
    static final int KERNEL_RADIUS_COUNT = 2;

    static final int ENABLE_FOREGROUND_AO_0 = 0;
    static final int ENABLE_FOREGROUND_AO_1 = 1;

    static final int ENABLE_BACKGROUND_AO_0 = 0;
    static final int ENABLE_BACKGROUND_AO_1 = 1;

    static final int ENABLE_DEPTH_THRESHOLD_0 = 0;
    static final int ENABLE_DEPTH_THRESHOLD_1 = 1;

    final CopyDepth_PS[] copyDepth_PS = new CopyDepth_PS[RESOLVE_DEPTH_COUNT];
    final LinearizeDepth_PS[] linearizeDepth_PS = new LinearizeDepth_PS[RESOLVE_DEPTH_COUNT];
    final DeinterleaveDepth_PS deinterleaveDepth_PS = new DeinterleaveDepth_PS();
    final DebugNormals_PS[] debugNormals_PS = new DebugNormals_PS[FETCH_GBUFFER_NORMAL_COUNT];
    final ReconstructNormal_PS reconstructNormal_PS = new ReconstructNormal_PS();
    final ReinterleaveAO_PS[] reinterleaveAO_PS  = new ReinterleaveAO_PS[ENABLE_BLUR_COUNT];

    final Blur_PS[][] blurX_PS = new Blur_PS[ENABLE_SHARPNESS_PROFILE_COUNT][KERNEL_RADIUS_COUNT];
    final Blur_PS[][] blurY_PS = new Blur_PS[ENABLE_SHARPNESS_PROFILE_COUNT][KERNEL_RADIUS_COUNT];
    final CoarseAO_PS[][][][] coarseAO_PS = new CoarseAO_PS[2][2][2][3];

    void create(){
        deinterleaveDepth_PS.create(loadShader("g_DeinterleaveDepth_PS_GL.frag"));
        reconstructNormal_PS.create(loadShader("g_ReconstructNormal_PS_GL.frag"));
    }

    CopyDepth_PS copyDepth_PS(int idx){
        CopyDepth_PS result = copyDepth_PS[idx];
        if(result == null){
            String[] filenames = {
                    "g_CopyDepth_PS_RESOLVE_DEPTH_0_GL.frag", "g_CopyDepth_PS_RESOLVE_DEPTH_1_GL.frag"
            };

            result = copyDepth_PS[idx] = new CopyDepth_PS();
            result.create(loadShader(filenames[idx]));
        }

        return result;
    }

    LinearizeDepth_PS linearizeDepth_PS(int idx){
        LinearizeDepth_PS result = linearizeDepth_PS[idx];
        if(result == null){
            String[] filenames = {
                    "g_LinearizeDepth_PS_RESOLVE_DEPTH_0_GL.frag", "g_LinearizeDepth_PS_RESOLVE_DEPTH_1_GL.frag"
            };

            result = linearizeDepth_PS[idx] = new LinearizeDepth_PS();
            result.create(loadShader(filenames[idx]));
        }

        return result;
    }

    DebugNormals_PS debugNormals_PS(int idx){
        DebugNormals_PS result = debugNormals_PS[idx];
        if(result == null){
            String[] filenames = {
                    "g_DebugNormals_PS_FETCH_GBUFFER_NORMAL_0_GL.frag", "g_DebugNormals_PS_FETCH_GBUFFER_NORMAL_1_GL.frag",
                    "g_DebugNormals_PS_FETCH_GBUFFER_NORMAL_2_GL.frag"
            };

            result = debugNormals_PS[idx] = new DebugNormals_PS();
            result.create(loadShader(filenames[idx]));
        }

        return result;
    }

    ReinterleaveAO_PS reinterleaveAO_PS(int idx){
        ReinterleaveAO_PS result = reinterleaveAO_PS[idx];
        if(result == null){
            String[] filenames = {
                    "g_ReinterleaveAO_PS_ENABLE_BLUR_0_GL.frag", "g_ReinterleaveAO_PS_ENABLE_BLUR_1_GL.frag",
            };

            result = reinterleaveAO_PS[idx] = new ReinterleaveAO_PS();
            result.create(loadShader(filenames[idx]));
        }

        return result;
    }

    Blur_PS blurX_PS(int idx, int radius){
        Blur_PS result = blurX_PS[idx][radius];
        if(result == null){
            String[][] filenames = {
                    {"g_BlurX_PS_ENABLE_SHARPNESS_PROFILE_0_KERNEL_RADIUS_2_GL.frag", "g_BlurX_PS_ENABLE_SHARPNESS_PROFILE_0_KERNEL_RADIUS_4_GL.frag"},
                    {"g_BlurX_PS_ENABLE_SHARPNESS_PROFILE_1_KERNEL_RADIUS_2_GL.frag", "g_BlurX_PS_ENABLE_SHARPNESS_PROFILE_1_KERNEL_RADIUS_4_GL.frag"}
            };

            result = blurX_PS[idx][radius] = new Blur_PS();
            result.create(loadShader(filenames[idx][radius]));
        }

        return result;
    }

    Blur_PS blurY_PS(int idx, int radius){
        Blur_PS result = blurY_PS[idx][radius];
        if(result == null){
            String[][] filenames = {
                    {"g_BlurY_PS_ENABLE_SHARPNESS_PROFILE_0_KERNEL_RADIUS_2_GL.frag", "g_BlurY_PS_ENABLE_SHARPNESS_PROFILE_0_KERNEL_RADIUS_4_GL.frag"},
                    {"g_BlurY_PS_ENABLE_SHARPNESS_PROFILE_1_KERNEL_RADIUS_2_GL.frag", "g_BlurY_PS_ENABLE_SHARPNESS_PROFILE_1_KERNEL_RADIUS_4_GL.frag"}
            };

            result = blurY_PS[idx][radius] = new Blur_PS();
            result.create(loadShader(filenames[idx][radius]));
        }

        return result;
    }

    CoarseAO_PS coarseAO_PS(int foreground, int background, int ed_threshold, int gbuffer){
        CoarseAO_PS result = coarseAO_PS[foreground][background][ed_threshold][gbuffer];
        if(result == null){

            String[][][][] filenames = new String[2][2][2][3];
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_0_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_1_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_2_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_0_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_1_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_2_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_0_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_1_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_2_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_0_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_1_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_0_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_2_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_0_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_1_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_2_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_0_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_1_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_0_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_2_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_0_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_1_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_0_FETCH_GBUFFER_NORMAL_2_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_0_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_1_GL.frag";
            filenames[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2] = "g_CoarseAO_PS_ENABLE_FOREGROUND_AO_1_ENABLE_BACKGROUND_AO_1_ENABLE_DEPTH_THRESHOLD_1_FETCH_GBUFFER_NORMAL_2_GL.frag";

            result = coarseAO_PS[foreground][background][ed_threshold][gbuffer] = new CoarseAO_PS();
            result.create(loadShader(filenames[foreground][background][ed_threshold][gbuffer]));
        }

        return result;
    }

    void release(){
        dispose(copyDepth_PS);
        dispose(linearizeDepth_PS);
        deinterleaveDepth_PS.dispose();
        dispose(debugNormals_PS);
        reconstructNormal_PS.disable();

        dispose(reinterleaveAO_PS);
        dispose(blurX_PS[0]);
        dispose(blurX_PS[1]);
        dispose(blurY_PS[0]);
        dispose(blurY_PS[1]);

        if(coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0] == null)
            return;

        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1].disable();
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2].disable();

        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_0][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_0][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_0] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_1] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_0][FETCH_GBUFFER_NORMAL_2] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_0] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_1] = null;
        coarseAO_PS[ENABLE_FOREGROUND_AO_1][ENABLE_BACKGROUND_AO_1][ENABLE_DEPTH_THRESHOLD_1][FETCH_GBUFFER_NORMAL_2] = null;
    }

    private static void dispose(Disposeable[] array){
        for(int i = 0; i < array.length; i++){
            if(array[i] != null){
                array[i].dispose();
                array[i] = null;
            }
        }
    }

    static StringBuilder parseText(String source){
        StringBuilder sb = new StringBuilder();

        int start =  -1;
        int end =  -1;

        while(true){
            start = source.indexOf('"',end + 1);
            if(start < 0) break;
            end  = source.indexOf('"', start + 1);
            if(end < 0) throw new RuntimeException("Uncomplete syna");

            sb.append(source, start + 1, end - 2).append('\n');
        }

        return sb;
    }

    static CharSequence loadShader(String filename){


        return null;
    }
}

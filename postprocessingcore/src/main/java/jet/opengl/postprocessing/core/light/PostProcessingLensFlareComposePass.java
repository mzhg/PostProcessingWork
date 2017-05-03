package jet.opengl.postprocessing.core.light;

import java.io.IOException;
import java.util.Arrays;

import jet.opengl.postprocessing.core.PostProcessingDownsampleProgram;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.core.RenderTexturePool;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/5/3.
 */

final class PostProcessingLensFlareComposePass extends PostProcessingRenderPass {

    private static GaussionBlurProgram[] g_GaussionBlurPrograms = new GaussionBlurProgram[8];
    private static PostProcessingDownsampleProgram g_DownsampleProgram = null;
    private static PostProcessingGhostImageProgram g_GhostImageProgram = null;
    private static PostProcessingGlareComposeProgram g_GlareComposeProgram = null;

    private final Texture2D[] blur_bufferA = new Texture2D[4];
    private final Texture2D[] blur_bufferB = new Texture2D[4];
    private final Texture2D[] compose_buffer = new Texture2D[3];

    public PostProcessingLensFlareComposePass() {
        super("BlurCompose");

        // Two output: 0 for Blur Compose, 1 for Ghost Compose.
        set(1, 2);
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        Texture2D input = getInput(0);
        Texture2D output = getOutputTexture(0);
        if(input == null){
            return;
        }

        try {
            initPrograms(input.getWidth(), input.getHeight());
            allocateTempTextures(input.getWidth(), input.getHeight(), input.getFormat());
        } catch (IOException e) {
            e.printStackTrace();
        }

//        blur(compose_buffer[LEVEL_0],  blur_bufferA[LEVEL_0], blur_bufferB[LEVEL_0], BLURH4);
        blur(context, 0,0);

//        GLError.checkError();
        for (int i=1;i<4;i++) {
            Texture2D src = (i ==1) ? input : compose_buffer[i-2];
            downsample(context, src, compose_buffer[i - 1]);
//    		saveImage(compose_buffer[i].getColorTexture(0));
//            GLError.checkError();
            int ii = i * 2;
//            blur(compose_buffer[i], blur_bufferA[i], blur_bufferB[i], ii/*(BLURH4+i*2) > BLURH12 ? BLURH12 : (BLURH4+i*2)*/);
            blur(context, i, ii);
        }

        releaseTempTextures();

        // generate ghost image and output to index 1
        ColorModulation colorModulation = PostProcessingLightStreakerPass.g_ColorModulation;
        if(parameters.isStartStreaker()){
            genGhostImage(context, colorModulation.camera_ghost_modulation1st, colorModulation.camera_ghost_modulation2nd);
        }else{
            genGhostImage(context, colorModulation.filmic_ghost_modulation1st, colorModulation.filmic_ghost_modulation2nd);
        }

        // Compose the blur image and output result to index 0


        releaseTextures();
    }

    private void composeBlurImage(PostProcessingRenderContext context, Texture2D output){
        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_GlareComposeProgram);
        g_GlareComposeProgram.setMixCoeff(0.3f, 0.3f, 0.25f, 0.20f);

        context.bindTexture(blur_bufferA[0], 0, 0);   // Blur compose
        context.bindTexture(blur_bufferA[1], 1, 0);   // Light Streaker compose
        context.bindTexture(blur_bufferA[2], 2, 0);   // Ghost Image
        context.bindTexture(null, 3, 0);
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
    }

    private void glareComposeProgram(PostProcessingRenderContext context, Texture2D output){
        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_GlareComposeProgram);
        g_GlareComposeProgram.setMixCoeff(1.2f, 0.8f, 0.1f, 0.0f);  // TODO

        context.bindTexture(blur_bufferA[0], 0, 0);
        context.bindTexture(blur_bufferA[1], 1, 0);
        context.bindTexture(blur_bufferA[2], 2, 0);
        context.bindTexture(blur_bufferA[3], 3, 0);
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
    }

    private void genGhostImage(PostProcessingRenderContext context, float[] ghost_modulation1st, float[] ghost_modulation2nd) {
        final Texture2D ghost2nd_buffer = getOutputTexture(1);
        final Texture2D ghost1st_buffer = RenderTexturePool.getInstance().findFreeElement(ghost2nd_buffer.getWidth(), ghost2nd_buffer.getHeight(), ghost2nd_buffer.getFormat());

        context.setViewport(0,0, ghost1st_buffer.getWidth(), ghost1st_buffer.getHeight());
        context.setVAO(null);
        context.setProgram(g_GhostImageProgram);
        g_GhostImageProgram.setScale(-4.0f, 3.0f, -2.0f, 0.3f);
        g_GhostImageProgram.setColorCoeff(ghost_modulation1st);

        context.bindTexture(blur_bufferA[0], 0, 0);
        context.bindTexture(blur_bufferA[1], 1, 0);
        context.bindTexture(blur_bufferA[1], 2, 0);
        context.bindTexture(blur_bufferA[0], 3, 0);  // TODO Lens Mask
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(ghost1st_buffer);

        context.drawFullscreenQuad();

        context.setViewport(0,0, ghost2nd_buffer.getWidth(), ghost2nd_buffer.getHeight());
        context.setVAO(null);
        context.setProgram(g_GhostImageProgram);
        g_GhostImageProgram.setScale(3.6f, 2.0f, 0.9f, -0.55f);  // TODO Parameters
        g_GhostImageProgram.setColorCoeff(ghost_modulation2nd);

        context.bindTexture(ghost1st_buffer, 0, 0);
        context.bindTexture(ghost1st_buffer, 1, 0);
        context.bindTexture(blur_bufferA[1], 2, 0);
        context.bindTexture(blur_bufferA[0], 3, 0);  // TODO Lens Mask
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(ghost2nd_buffer);

        context.drawFullscreenQuad();

        RenderTexturePool.getInstance().freeUnusedResource(ghost1st_buffer);
    }

    private void downsample(PostProcessingRenderContext context, Texture2D src, Texture2D output){
        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_DownsampleProgram);

        context.bindTexture(src, 0, 0);
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
    }

    private void blur(PostProcessingRenderContext context, int level, int blurWidth) {
        Texture2D src = (level == 0) ? getInput(0) : compose_buffer[level - 1];
        Texture2D temp = blur_bufferB[level];
        Texture2D dest = blur_bufferA[level];

        run_pass(context, blurWidth, src, temp);
        run_pass(context, blurWidth+1, temp, dest);
    }

    private void run_pass(PostProcessingRenderContext context,int prog, Texture2D src, Texture2D output) {
        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(g_GaussionBlurPrograms[prog]);

        context.bindTexture(src, 0, 0);
        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
    }

    private void allocateTempTextures(int width, int height, int format){
        final RenderTexturePool pool = RenderTexturePool.getInstance();
        for(int i = 0; i < 4; i++){
            blur_bufferA[i] = pool.findFreeElement(width, height, format);
            blur_bufferB[i] = pool.findFreeElement(width, height, format);

            if(i > 0){
                compose_buffer[i-1] = pool.findFreeElement(width, height, format);
            }

            width /= 2;
            height /= 2;
        }
    }

    private void releaseTempTextures(){
        final RenderTexturePool pool = RenderTexturePool.getInstance();
        for(int i = 0; i < 4; i++){
//            pool.freeUnusedResource(blur_bufferA[i]);
            pool.freeUnusedResource(blur_bufferB[i]);

//            blur_bufferA[i] = null;
            blur_bufferB[i] = null;

            if(i > 0){
                pool.freeUnusedResource(compose_buffer[i-1]);
                compose_buffer[i-1] = null;
            }
        }
    }

    private void releaseTextures(){
        final RenderTexturePool pool = RenderTexturePool.getInstance();
        for(int i = 0; i < 4; i++){
            pool.freeUnusedResource(blur_bufferA[i]);
            blur_bufferA[i] = null;
        }
    }

    private static void initPrograms(int w, int h) throws IOException {
        if(g_GaussionBlurPrograms[0] != null)
            return;

        float[] s = GaussionBlurProgram.std_weights;
        for(int i = 0;i < 4; i++){
            float weight = s[i];
            g_GaussionBlurPrograms[2 * i + 0] = new GaussionBlurProgram(w, h, false, weight);
            g_GaussionBlurPrograms[2 * i + 1] = new GaussionBlurProgram(w, h, true , weight);
            w /=2;
            h /=2;

            addDisposedResource(g_GaussionBlurPrograms[2 * i + 0]);
            addDisposedResource(g_GaussionBlurPrograms[2 * i + 1]);
        }

        addDisposedResource(()-> Arrays.fill(g_GaussionBlurPrograms, null));

        g_DownsampleProgram = new PostProcessingDownsampleProgram(0);
        addDisposedResource(g_DownsampleProgram);

        g_GhostImageProgram = new PostProcessingGhostImageProgram();
        addDisposedResource(g_GhostImageProgram);

        g_GlareComposeProgram = new PostProcessingGlareComposeProgram();
        addDisposedResource(g_GlareComposeProgram);
    }

    @Override
    public void computeOutDesc(int index, Texture2DDesc out) {
        Texture2D input = getInput(0);
        if(input != null){
            input.getDesc(out);
        }

        super.computeOutDesc(index, out);
    }
}

package jet.opengl.postprocessing.core.dof;

import java.io.IOException;
import java.util.Arrays;

import jet.opengl.postprocessing.core.PostProcessingFrameAttribs;
import jet.opengl.postprocessing.core.PostProcessingParameters;
import jet.opengl.postprocessing.core.PostProcessingRenderContext;
import jet.opengl.postprocessing.core.PostProcessingRenderPass;
import jet.opengl.postprocessing.texture.SamplerUtils;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

final class PostProcessingDOFRecombinePass extends PostProcessingRenderPass {

    private static PostProcessingDOFRecombineProgram[] g_DOFRecombinePrograms = new PostProcessingDOFRecombineProgram[4];

    private int m_sampler;
    private final boolean m_bInFarBlur;
    private final boolean m_bInNearBlur;

    public PostProcessingDOFRecombinePass(boolean bInFarBlur, boolean bInNearBlur) {
        super("DOFRecombine");
        m_bInFarBlur = bInFarBlur;
        m_bInNearBlur = bInNearBlur;

        // input0: scene texture
        // input1: depth texture
        // input2: far blurred texture
        // input3: near blurred texture
        set(2+(bInFarBlur?1:0)+(bInNearBlur?1:0),1);
    }

    private static PostProcessingDOFRecombineProgram getDOFRecombineProgram(boolean bInFarBlur, boolean bInNearBlur){
        int low = bInNearBlur ? 1:0;
        int high = bInFarBlur ? 2:0;

        int index = low | high;
        if(g_DOFRecombinePrograms[index] == null){
            try {
                g_DOFRecombinePrograms[index] = new PostProcessingDOFRecombineProgram(bInFarBlur, bInNearBlur);
                addDisposedResource(g_DOFRecombinePrograms[index]);
                addDisposedResource(()-> Arrays.fill(g_DOFRecombinePrograms, null));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return g_DOFRecombinePrograms[index];
    }

    @Override
    public void process(PostProcessingRenderContext context, PostProcessingParameters parameters) {
        PostProcessingDOFRecombineProgram dofRecombineProgram = getDOFRecombineProgram(m_bInFarBlur, m_bInNearBlur);
        if(m_sampler == 0){
            m_sampler = SamplerUtils.getDefaultSampler();
        }

        Texture2D input0 = getInput(0);
        Texture2D input1 = getInput(1);
        Texture2D input2 = getInput(2);
        Texture2D input3 = getInput(3);
        Texture2D output = getOutputTexture(0);

        context.setViewport(0,0, output.getWidth(), output.getHeight());
        context.setVAO(null);
        context.setProgram(dofRecombineProgram);
        PostProcessingFrameAttribs frameAttribs = context.getFrameAttribs();

        dofRecombineProgram.setUniform(input0.getWidth(), input0.getHeight(), frameAttribs.cameraNear, frameAttribs.cameraFar,
                parameters.getFocalDepth(), parameters.getFocalLength(), parameters.getNearTransitionRegion(), parameters.getFarTransitionRegion());

        context.bindTexture(input0, 0, 0);
        context.bindTexture(input1, 1, m_sampler);
        if(m_bInFarBlur)
            context.bindTexture(input2, 2, 0);   // far blurred texture
        if(m_bInFarBlur)
            context.bindTexture(input3, 3, 0);  // near blurred texture
        else
            context.bindTexture(input2, 3, 0);  // no far, input2 must be near blurred texture.


        context.setBlendState(null);
        context.setDepthStencilState(null);
        context.setRasterizerState(null);
        context.setRenderTarget(output);

        context.drawFullscreenQuad();
        context.bindTexture(input1, 1, 0); // reset the samplers
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

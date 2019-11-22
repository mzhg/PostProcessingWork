package jet.opengl.demos.nvidia.waves.ocean;

import jet.opengl.postprocessing.common.BlendState;
import jet.opengl.postprocessing.common.DepthStencilState;
import jet.opengl.postprocessing.common.GLStateTracker;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.common.RasterizerState;
import jet.opengl.postprocessing.shader.GLSLProgram;
import jet.opengl.postprocessing.texture.TextureGL;

public class Technique extends GLSLProgram {
    protected final RasterizerState mRaster = new RasterizerState();
    protected final DepthStencilState mDepthStencil = new DepthStencilState();
    protected final BlendState  mBlend = new BlendState();

    protected boolean mEnableState = true;

    public void setStateEnabled(boolean flag){ mEnableState = flag;}
    public boolean isStateEnabled() { return mEnableState;}

    public RasterizerState getRaster(){ return mRaster;}
    public DepthStencilState getDepthStencil(){ return mDepthStencil;}
    public BlendState getBlend(){ return mBlend;}

//    @Override
//    public void enable() {
//        throw new UnsupportedOperationException("Use the parameter function to instead this");
//    }

    /** Apply the params to the program and setup all of render states. Override this method should call super implements at the last line. */
    public void enable(TechniqueParams params){
        if(!isComputeProgram() && mEnableState) {
            GLStateTracker stateTracker = GLStateTracker.getInstance();
            stateTracker.setRasterizerState(mRaster);
            stateTracker.setDepthStencilState(mDepthStencil);
            stateTracker.setBlendState(mBlend);
        }

        super.enable();
    }

    Technique PSMBlend(){
       /* BlendState PSMBlend
        {
            BlendEnable[0] = TRUE;
            RenderTargetWriteMask[0] = 0xF;

            SrcBlend = Zero;
            DestBlend = Inv_Src_Color;
            BlendOp = Add;

            SrcBlendAlpha = Zero;
            DestBlendAlpha = Inv_Src_Alpha;
            BlendOpAlpha = Add;
        };*/

        mBlend.blendEnable = true;
        mBlend.srcBlend = GLenum.GL_ZERO;
        mBlend.destBlend= GLenum.GL_ONE_MINUS_SRC_COLOR;
        mBlend.blendOp = mBlend.blendOpAlpha = GLenum.GL_FUNC_ADD;
        mBlend.srcBlendAlpha = GLenum.GL_ZERO;
        mBlend.destBlendAlpha = GLenum.GL_ONE_MINUS_SRC_ALPHA;
        mRaster.colorWriteMask = 0xF;

        return this;
    }

    Technique ReadOnlyDepth(){
        /*DepthStencilState ReadOnlyDepth
        {
            DepthEnable = TRUE;
            DepthWriteMask = ZERO;
            DepthFunc = LESS_EQUAL;
            StencilEnable = FALSE;
        };*/

        mDepthStencil.depthEnable = true;
        mDepthStencil.depthFunc = GLenum.GL_LEQUAL;
        mDepthStencil.depthWriteMask = false;
        mDepthStencil.stencilEnable = false;

        return this;
    }

    Technique ReadDepth(){
        /*DepthStencilState ReadDepth
        {
            DepthEnable = TRUE;
            DepthWriteMask = ZERO;
            DepthFunc = LESS_EQUAL;
        };*/

        return ReadOnlyDepth();
    }

    Technique NoDepthStencil(){

        /*DepthStencilState NoDepthStencil
        {
            DepthEnable = FALSE;
            StencilEnable = FALSE;
        };*/

        mDepthStencil.depthEnable = false;
        mDepthStencil.stencilEnable = false;

        return this;
    }

    Technique SolidNoCull(){
        /*RasterizerState SolidNoCull
        {
            FillMode = SOLID;
            CullMode = NONE;

            MultisampleEnable = True;
        };*/

        mRaster.fillMode = GLenum.GL_FILL;
        mRaster.cullFaceEnable = false;
        // todo multisample
        return this;
    }

    Technique TranslucentBlendRGB(){
        /*BlendState TranslucentBlendRGB
        {
            BlendEnable[0] = TRUE;
            RenderTargetWriteMask[0] = 0xF;

            SrcBlend = SRC_ALPHA;
            DestBlend = INV_SRC_ALPHA;
            BlendOp = Add;

            SrcBlendAlpha = ZERO;
            DestBlendAlpha = INV_SRC_ALPHA;
            BlendOpAlpha = Add;
        };*/

        this.mBlend.blendEnable = true;
        this.mBlend.srcBlend = GLenum.GL_SRC_ALPHA;
        this.mBlend.destBlend = GLenum.GL_ONE_MINUS_SRC_ALPHA;
        this.mBlend.srcBlendAlpha = GLenum.GL_ZERO;
        this.mBlend.destBlendAlpha = GLenum.GL_ONE_MINUS_SRC_ALPHA;
        this.mBlend.blendOp = this.mBlend.blendOpAlpha = GLenum.GL_FUNC_ADD;
        mRaster.colorWriteMask = 0xF;
        return this;
    }

    Technique Translucent(){ return TranslucentBlendRGB();}

    Technique AddBlend(){
        /*BlendState AddBlend
        {
            BlendEnable[0] = TRUE;
            RenderTargetWriteMask[0] = 0xF;

            SrcBlend = ONE;
            DestBlend = ONE;
            BlendOp = Add;
        };*/

        mBlend.blendEnable = true;
        mBlend.srcBlend = GLenum.GL_ONE;
        mBlend.destBlend = GLenum.GL_ONE;
        mBlend.blendOp = GLenum.GL_FUNC_ADD;

        return this;
    }

    Technique Opaque(){
       /* BlendState Opaque
        {
            BlendEnable[0] = FALSE;
            RenderTargetWriteMask[0] = 0xF;
        };*/

       mBlend.blendEnable = false;
        mRaster.colorWriteMask = 0xF;
        return this;
    }

    Technique EnableDepth(){
        /*DepthStencilState EnableDepth
        {
            DepthEnable = TRUE;
            DepthWriteMask = ALL;
            DepthFunc = LESS_EQUAL;
            StencilEnable = FALSE;
        };*/

        mDepthStencil.depthEnable =true;
        mDepthStencil.depthWriteMask = true;
        mDepthStencil.depthFunc = GLenum.GL_LEQUAL;
        mDepthStencil.stencilEnable = false;

        return this;
    }

    Technique DisableDepth(){
        /*DepthStencilState DisableDepth
        {
            DepthEnable = FALSE;
            DepthWriteMask = ALL;
            DepthFunc = ALWAYS;
            StencilEnable = FALSE;
        };*/

        mDepthStencil.depthEnable = false;
        mDepthStencil.depthWriteMask = true;
        mDepthStencil.depthFunc = GLenum.GL_ALPHA;
        mDepthStencil.stencilEnable = false;

        return this;
    }

    Technique AlwaysDepth(){
        /*DepthStencilState AlwaysDepth
        {
            DepthEnable = TRUE;
            DepthWriteMask = ALL;
            DepthFunc = ALWAYS;
            StencilEnable = FALSE;
        };*/

        mDepthStencil.depthEnable = true;
        mDepthStencil.depthWriteMask = true;
        mDepthStencil.depthFunc = GLenum.GL_ALPHA;
        mDepthStencil.stencilEnable = false;

        return this;
    }

    Technique DepthCompare(){
        /*DepthStencilState DepthCompare
        {
            DepthEnable = TRUE;
            DepthWriteMask = ZERO;
            DepthFunc = LESS_EQUAL;
        };*/
        mDepthStencil.depthEnable = true;
        mDepthStencil.depthWriteMask = false;
        mDepthStencil.depthFunc = GLenum.GL_LEQUAL;
        mDepthStencil.stencilEnable = false;
        return this;
    }

    Technique SolidFront(){
        /*RasterizerState Solid
        {
            FillMode = SOLID;
            CullMode = FRONT;

            MultisampleEnable = True;
        };*/

        mRaster.fillMode = GLenum.GL_FILL;
        mRaster.cullMode = GLenum.GL_FRONT;
        mRaster.cullFaceEnable = true;
        mRaster.frontCounterClockwise = true;

        return this;
    }

    Technique Wireframe(){
        /*RasterizerState Wireframe
        {
            FillMode = WIREFRAME;
            CullMode = FRONT;

            MultisampleEnable = True;
        };*/

        mRaster.fillMode = GLenum.GL_LINE;
        mRaster.cullMode = GLenum.GL_FRONT;
        mRaster.cullFaceEnable = true;
        mRaster.frontCounterClockwise = true;

        return this;
    }

    Technique WireframeNoCull(){
        /*RasterizerState Wireframe
        {
            FillMode = WIREFRAME;
            CullMode = NONE;

            MultisampleEnable = True;
        };*/

        mRaster.fillMode = GLenum.GL_LINE;
        mRaster.cullFaceEnable = false;

        return this;
    }

    Technique ParticleRS(){
        return SolidNoCull();
    }

    Technique SolidBack(){
        /*RasterizerState Solid
        {
            FillMode = SOLID;
            CullMode = Back;
            MultisampleEnable = True;
        };*/

        mRaster.fillMode = GLenum.GL_FILL;
        mRaster.cullMode = GLenum.GL_BACK;
        mRaster.cullFaceEnable = true;
        mRaster.frontCounterClockwise =true;
        return this;
    }

    Technique WireframeBack(){
        /*RasterizerState SolidWireframe
        {
            FillMode = WIREFRAME;
            CullMode = Back;
            MultisampleEnable = True;
        };*/

        mRaster.fillMode = GLenum.GL_LINE;
        mRaster.cullMode = GLenum.GL_BACK;
        mRaster.cullFaceEnable = true;
        mRaster.frontCounterClockwise =true;
        return this;
    }

    Technique ShadowRS(){
        /*RasterizerState ShadowRS
        {
            FillMode = SOLID;
            CullMode = NONE;

            SlopeScaledDepthBias = 4.0f;
        };*/


        return SolidNoCull();
    }

    Technique WriteDepth(){
        /*DepthStencilState WriteDepth
        {
            DepthEnable = TRUE;
            DepthWriteMask = ALL;
            DepthFunc = ALWAYS;
            StencilEnable = FALSE;
        };*/

        mDepthStencil.depthEnable = true;
        mDepthStencil.depthWriteMask = true;
        mDepthStencil.depthFunc = GLenum.GL_ALWAYS;
        mDepthStencil.stencilEnable = false;

        return this;
    }

    Technique Additive(){
        /*BlendState Additive
        {
            BlendEnable[0] = TRUE;
            RenderTargetWriteMask[0] = 0xF;

            SrcBlend = ONE;
            DestBlend = SRC_ALPHA;
            BlendOp = Add;
        };*/

        mBlend.blendEnable = true;
        mBlend.srcBlend = GLenum.GL_ONE;
        mBlend.destBlend = GLenum.GL_SRC_ALPHA;
        mBlend.blendOp = GLenum.GL_ADD;

        return this;
    }

    void bindTexture(int unit, TextureGL texture, int sampler){
        if(texture != null){
            gl.glBindTextureUnit(unit, texture.getTexture());
            gl.glBindSampler(unit, sampler);
        }else{
            gl.glBindTextureUnit(unit, 0);
            gl.glBindSampler(unit, 0);
        }
    }

    void bindImage(int unit, TextureGL texture, boolean read){
        if(texture != null){
            gl.glBindImageTexture(unit, texture.getTexture(), 0, true, 0, read? GLenum.GL_READ_WRITE:GLenum.GL_WRITE_ONLY, texture.getFormat());
        }else{
            gl.glBindImageTexture(unit, 0, 0, true, 0, read? GLenum.GL_READ_WRITE:GLenum.GL_WRITE_ONLY, GLenum.GL_RGBA8);
        }
    }
}

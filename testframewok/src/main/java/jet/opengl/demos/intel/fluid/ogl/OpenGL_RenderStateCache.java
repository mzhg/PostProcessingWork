package jet.opengl.demos.intel.fluid.ogl;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.demos.intel.fluid.render.AlphaState;
import jet.opengl.demos.intel.fluid.render.BlendState;
import jet.opengl.demos.intel.fluid.render.CompareFuncE;
import jet.opengl.demos.intel.fluid.render.DepthState;
import jet.opengl.demos.intel.fluid.render.MaterialProperties;
import jet.opengl.demos.intel.fluid.render.RasterState;
import jet.opengl.demos.intel.fluid.render.RenderState;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;

/**
 * Created by Administrator on 2018/4/5 0005.
 */

final class OpenGL_RenderStateCache {
    public static final int sType = 1380020071;

    /* Cache of current render state.  Used to avoid unnecessary state change calls into underlying API. */
    private final RenderState mCurrentState = new RenderState();

    OpenGL_RenderStateCache() {}

    void apply( RenderState renderState  ){
        /*PERF_BLOCK( OpenGL_RenderStateCache__Apply ) ;
        RENDER_CHECK_ERROR( OpenGL_RenderStateCache_Apply_before ) ;*/

        // TODO: FIXME: If render state has already been set, do not set it again.
        // TODO: FIXME: In debug mode, verify the underlying state matches the material.

        //if( ! memcmp( & mCurrentState.mBlendState , & renderState.mBlendState , sizeof( mCurrentState.mBlendState ) ) )
        {
            blendState_Apply( renderState.mBlendState ) ;
        }

        //if( ! memcmp( & mCurrentState.mDepthState , & renderState.mDepthState , sizeof( mCurrentState.mDepthState ) ) )
        {
            depthState_Apply( renderState.mDepthState ) ;
        }

        //if( ! memcmp( & mCurrentState.mAlphaState , & renderState.mAlphaState , sizeof( mCurrentState.mAlphaState ) ) )
        {
            alphaState_Apply( renderState.mAlphaState ) ;
        }

        //if( ! memcmp( & mCurrentState.mRasterState , & renderState.mRasterState , sizeof( mCurrentState.mRasterState ) ) )
        {
            rasterState_Apply( renderState.mRasterState ) ;
        }

        //if( ! memcmp( & mCurrentState.mShadeMode , & renderState.mShadeMode , sizeof( mCurrentState.mShadeMode ) ) )
        {
//            ShadeMode_Apply( renderState.mShadeMode ) ;
        }

        //if( ! memcmp( & mCurrentState.mMaterialProperties , & renderState.mMaterialProperties , sizeof( mCurrentState.mMaterialProperties ) ) )
        {
            materialProperties_Apply( renderState.mMaterialProperties ) ;
        }

        mCurrentState.set(renderState);

//        RENDER_CHECK_ERROR( OpenGL_RenderStateCache_Apply_after ) ;
    }

    public RenderState getRenderStateCache() { return mCurrentState ; }

    static void getRenderState( RenderState renderState ){
        /*PERF_BLOCK( OpenGL_RenderStateCache__GetRenderState ) ;
        RENDER_CHECK_ERROR( OpenGL_RenderStateCache_Get_before ) ;*/

        //BlendState_Get( renderState.mBlendState ) ;
        //DepthState_Get( renderState.mDepthState ) ;
        //AlphaState_Get( renderState.mAlphaState ) ;
        //RasterState_Get( renderState.mRasterState ) ;
        //ShadeMode_Get( renderState.mShadeMode ) ;
        //MaterialProperties_Get( renderState.mMaterialProperties ) ;
        transforms_Get( renderState.mTransforms ) ;

//        RENDER_CHECK_ERROR( OpenGL_RenderStateCache_Get_after ) ;
    }

    // Private variables -----------------------------------------------------------
    // Public variables ------------------------------------------------------------
    // Private functions -----------------------------------------------------------

    private static void blendState_Apply( BlendState blendState )
    {
//        PERF_BLOCK( Render__BlendState_Apply ) ;
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        if( blendState.mBlendEnabled )
        {
            gl.glEnable(GLenum.GL_BLEND ) ;
        }
        else
        {
            gl.glDisable( GLenum.GL_BLEND ) ;
        }

        int srcFactor = GLenum.GL_ONE ;

        switch( blendState.mBlendSrcAlpha )
        {
            case BLEND_FACTOR_ZERO          : srcFactor = GLenum.GL_ZERO                ; break ;
            case BLEND_FACTOR_ONE           : srcFactor = GLenum.GL_ONE                 ; break ; // Default / additive
            case BLEND_FACTOR_SRC_ALPHA     : srcFactor = GLenum.GL_SRC_ALPHA           ; break ; // Typical alpha blending
            case BLEND_FACTOR_INV_SRC_ALPHA : srcFactor = GLenum.GL_ONE_MINUS_SRC_ALPHA ; break ;
            // TODO: FIXME: Support other blend factors
            default: throw new IllegalArgumentException() ;
        }

        int dstFactor = GLenum.GL_ONE ;

        switch( blendState.mBlendDstAlpha )
        {
            case BLEND_FACTOR_ZERO          : dstFactor = GLenum.GL_ZERO                ; break ;
            case BLEND_FACTOR_ONE           : dstFactor = GLenum.GL_ONE                 ; break ; // Default / additive
            case BLEND_FACTOR_SRC_ALPHA     : dstFactor = GLenum.GL_SRC_ALPHA           ; break ;
            case BLEND_FACTOR_INV_SRC_ALPHA : dstFactor = GLenum.GL_ONE_MINUS_SRC_ALPHA ; break ; // Typical alpha blending
            // TODO: FIXME: Support other blend factors
            default: throw new IllegalArgumentException() ;
        }

        gl.glBlendFunc( srcFactor , dstFactor ) ;

        // TODO: FIXME: Support other blend modes.
        assert ( blendState.mBlendSrcColor == blendState.mBlendSrcAlpha ) ;
        assert ( blendState.mBlendDstColor == blendState.mBlendDstAlpha ) ;
        assert ( blendState.mBlendOpColor  == blendState.mBlendOpAlpha ) ;
        assert ( BlendState.BlendOpE.BLEND_OP_ADD  == blendState.mBlendOpColor ) ;

//        RENDER_CHECK_ERROR( BlendState_Apply ) ;
    }

    static int compareFunction( CompareFuncE compareFunc )
    {
//        PERF_BLOCK( Render__compareFunction ) ;
        switch( compareFunc )
        {
            case CMP_FUNC_NEVER         : return GLenum.GL_NEVER  ;
            case CMP_FUNC_LESS          : return GLenum.GL_LESS   ;
            case CMP_FUNC_GREATER_EQUAL : return GLenum.GL_GEQUAL ;
            case CMP_FUNC_ALWAYS        : return GLenum.GL_ALWAYS ;
            // TODO: FIXME: Support other comparison functions. See glDepthFunc, glAlphaFunc.
            default: throw  new IllegalArgumentException() ;
        }
    }

    static void depthState_Apply(DepthState depthState )
    {
//        PERF_BLOCK( Render__DepthState_Apply ) ;
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        switch( depthState.mDepthFunc )
        {
            case CMP_FUNC_NEVER         :
            case CMP_FUNC_LESS          :
            case CMP_FUNC_GREATER_EQUAL :
                // TODO: FIXME: Support other depth modes. See glDepthFunc.
                gl.glEnable( GLenum.GL_DEPTH_TEST ) ;
                break ;
            case CMP_FUNC_ALWAYS        :
                gl.glDisable( GLenum.GL_DEPTH_TEST ) ;
                break ;
            default: throw new IllegalArgumentException();
        }

        final int depthFunc = compareFunction( depthState.mDepthFunc ) ;
        gl.glDepthFunc( depthFunc  ) ;

        gl.glDepthMask( depthState.mDepthWriteEnabled ? true : false ) ;

        // TODO: FIXME: Support other depth mode states.
        assert ( 0 == depthState.mDepthBias ) ;
        assert ( depthState.mDepthClip ) ;

//        RENDER_CHECK_ERROR( DepthState_Apply ) ;
    }

    static void alphaState_Apply( AlphaState alphaState )
    {
//        PERF_BLOCK( Render__AlphaState_Apply ) ;
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        // Set alpha-test state.
        if( alphaState.mAlphaTest )
        {   // Enable testing against alpha channel.
            gl.glEnable( GLenum.GL_ALPHA_TEST ) ;
        }
        else
        {   // Disable testing against alpha channel.
            gl.glDisable( GLenum.GL_ALPHA_TEST ) ;
        }

        final int alphaFunc = compareFunction( alphaState.mAlphaFunc ) ;

//        gl.glAlphaFunc( alphaFunc , alphaState.mAlphaRef ) ;  TODO

//        RENDER_CHECK_ERROR( AlphaState_Apply ) ;
    }

    static void rasterState_Apply( RasterState rasterState )
    {
//        PERF_BLOCK( Render__RasterState_Apply ) ;
        GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
        {
            int polygonMode = GLenum.GL_FILL ;
            switch( rasterState.mFillMode )
            {
                case FILL_MODE_POINT       : polygonMode = GLenum.GL_POINT    ; break ;
                case FILL_MODE_WIREFRAME   : polygonMode = GLenum.GL_LINE     ; break ;
                case FILL_MODE_SOLID       : polygonMode = GLenum.GL_FILL     ; break ;
                default: throw new IllegalArgumentException() ;
            }
            gl.glPolygonMode(GLenum.GL_FRONT_AND_BACK , polygonMode ) ;
        }

        {
            if(  RasterState.CullModeE.CULL_MODE_NONE == rasterState.mCullMode )
            {
                gl.glDisable(GLenum.GL_CULL_FACE ) ;
            }
            else
            {
                gl.glEnable( GLenum.GL_CULL_FACE ) ;
                int cullFace = GLenum.GL_BACK ;
                switch( rasterState.mCullMode )
                {
                    case CULL_MODE_FRONT: cullFace = GLenum.GL_FRONT  ; break ;
                    case CULL_MODE_BACK : cullFace = GLenum.GL_BACK   ; break ;
                    default: throw new IllegalArgumentException() ;
                }
                gl.glCullFace( cullFace ) ;
                gl.glFrontFace( GLenum.GL_CCW ) ; // PeGaSys defines a front face as being wound CCW.
            }
        }

        gl.glEnable( GLenum.GL_NORMALIZE )        ;   // Probably should use GL_RESCALE_NORMAL instead but this version of OpenGL does not seem to have that

//        RENDER_CHECK_ERROR( RasterState_Apply ) ;
    }

    /*static void ShadeMode_Apply( const ShadeModeE & shadeMode )
    {
        PERF_BLOCK( Render__ShadeMode_Apply ) ;

        GLenum shadeModel = GL_SMOOTH ;
        switch( shadeMode )
        {
            case SHADE_MODE_FLAT  : shadeModel = GL_FLAT   ; break ;
            case SHADE_MODE_SMOOTH: shadeModel = GL_SMOOTH ; break ;
            default: FAIL() ; break ;
        }
        glShadeModel( shadeModel ) ;

        RENDER_CHECK_ERROR( ShadeMode_Apply ) ;
    }*/

    static void materialProperties_Apply( MaterialProperties materialProperties )
    {
        /*PERF_BLOCK( Render__MaterialProperties_Apply ) ;  TODO

        // Enable per-vertex color.
        // NOTE: Should only apply when providing per-vertex colors.
        // NOTE: glColorMaterial (re)sets the material, so any set-material done before this is overwritten.
        // see http://www.opengl.org/archives/resources/features/KilgardTechniques/oglpitfall/
        // Always be careful to set glColorMaterial before you enable GL_COLOR_MATERIAL.
        glColorMaterial( GL_FRONT_AND_BACK , GL_AMBIENT_AND_DIFFUSE ) ;
        glEnable( GL_COLOR_MATERIAL ) ;
        //glDisable( GL_COLOR_MATERIAL ) ;

        // Set material color.
        // If vertices have a color, they will override this.
        // This is only meaningful when the vertex format lacks color.
        glColor4fv( (float*) & materialProperties.mDiffuseColor  ) ;
        glMaterialfv( GL_FRONT , GL_AMBIENT   , (float*) & materialProperties.mAmbientColor  ) ;
        glMaterialfv( GL_FRONT , GL_DIFFUSE   , (float*) & materialProperties.mDiffuseColor  ) ;
        glMaterialfv( GL_FRONT , GL_SPECULAR  , (float*) & materialProperties.mSpecularColor ) ;
        glMateriali ( GL_FRONT , GL_SHININESS , static_cast< int >( materialProperties.mSpecularPower ) ) ;
        glMaterialfv( GL_FRONT , GL_EMISSION  , (float*) & materialProperties.mEmissiveColor ) ;

        RENDER_CHECK_ERROR( MaterialProperties_Apply ) ;*/
    }

    static void transforms_Get( Matrix4f transforms )
    {
//        PERF_BLOCK( Render__Transforms_Get ) ;

        /*Mat44 viewMatrix ;  TODO
        glGetFloatv( GL_MODELVIEW_MATRIX , (GLfloat *) & transforms.mViewMatrix ) ;*/

//        RENDER_CHECK_ERROR( Transforms_Get ) ;
    }
}

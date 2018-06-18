package jet.opengl.demos.intel.fluid.render;

import java.util.ArrayList;

/**
 * Render state and material within a Technique used to render a mesh.<p></p>
 * Created by Administrator on 2018/3/13 0013.
 */

public class Pass {
    private Technique           mParentTechnique    ;   ///< Technique that owns this Pass.
    private RenderState             mRenderState        ;   ///< RenderState.
    private final ArrayList<TextureStage> mTextureStages = new ArrayList<>();   ///< Array of texture stages.

    public Pass( Technique parentTechnique ){
        mParentTechnique = parentTechnique;
    }

    public void apply( ApiBase renderApi ){
        renderApi.applyRenderState( getRenderState() ) ;
        if( ! mTextureStages.isEmpty() )
        {
            for( int unit = 0 ; unit < mTextureStages.size() ; ++ unit )
            {   // For each texture unit...
                TextureBase  texture = mTextureStages.get(unit).mTexture;
                assert ( texture !=null) ;
                texture.bind( renderApi , mTextureStages.get(unit).mSamplerState ) ;
            }
        }
        else
        {   // Mesh has no texture.
            renderApi.disableTexturing() ;
        }
    }

    public RenderState getRenderState() { return mRenderState ; }

    public int getNumTextureStages() { return mTextureStages.size() ; }

    /** Add an empty TextureStage to this pass, for caller to populate.
     */
    public void addTextureStage(){
        mTextureStages.add(new TextureStage() ) ;
    }

    public void addTextureStage(TextureStage textureStage ){
        mTextureStages.add( textureStage ) ;
    }

    public TextureStage getTextureStage(int idxStage )       { return mTextureStages.get(idxStage) ; }
}

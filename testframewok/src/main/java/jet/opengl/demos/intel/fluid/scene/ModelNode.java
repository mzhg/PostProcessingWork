package jet.opengl.demos.intel.fluid.scene;

import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.intel.fluid.render.ApiBase;
import jet.opengl.demos.intel.fluid.utils.Camera;

/**
 * Scene node representing a model.<p></p>

 A model has ModelData which is essentially a collection of meshes,
 where each mesh has a material and geometry (e.g. vertices, triangles).
 * Created by mazhen'gui on 2018/3/12.
 */

public class ModelNode extends SceneNodeBase {

    public static final int sTypeId = 1298426958 /*'MdlN'*/ ; ///< Type identifier for a testModelNode scene node
    public static final int MAX_NUM_LIGHTS_PER_MODEL = 8 ;


    private ModelData       mModelData;  ///< Sharable model data such as meshes.
    private final Light[]   mLightsCache = new Light[ MAX_NUM_LIGHTS_PER_MODEL ] ;  ///< Cache of lights that apply to this model
    private int             mNumLights                               ;  ///< Number of lights in mLightsCache.

    /**
     * Construct base part of a scene node.<p></p>
     * Scene nodes can be assigned a "type" which can be used, for example, by
     * scene node visitors (ISceneNode::IVisitor) to determine whether that
     * visitor should operate on the node it visits.
     *
     * @param sceneManager Scene manager that contains this node.
     */
    public ModelNode(ISceneManager sceneManager) { super(sceneManager, sTypeId);}

    public void render(){
        setLocalToWorld() ;

        Camera camera      = getSceneManager().getCurrentCamera() ;
        ApiBase renderApi   = getSceneManager().getApi() ;

        renderApi.setCamera(camera ) ;

        // In OpenGL, lights are transformed by current MODELVIEW matrix but light node is in world space so must set view (camera) before setting lights.  Alternatively, could set view matrix in OpenGL_Api::SetLight.
        renderApi.setLights(this ) ;

        renderApi.setLocalToWorld( getLocalToWorld() ) ;

        mModelData.render( renderApi ) ;

        // TODO: Render diagnostic text at this location (0,0,0)

        renderChildren() ;
    }

    public ModelData         newModelData(){
        ModelData newModelData    = new ModelData() ;
        setModelData( newModelData ) ;
        return getModelData() ;
    }

    public void              setModelData( ModelData modelData ){
        mModelData = modelData ;
    }

    public ModelData         getModelData() { return mModelData ; }

    /// Clear all lights cached for this model.
    public void              clearLightsCache() { mNumLights = 0 ; }

    public void              updateLightsCache( Light light ){
        boolean addedLight = false ;

        // If given light is closer than any cached light, insert given light into cache.
//        const Vec3  sepCandidate    = light.getPosition() - getPosition() ;
        final ReadableVector3f sepCandidate = Vector3f.sub(light.getPos3(), getPos3(), null);
        final float dist2Candidate  = sepCandidate.lengthSquared() ;
        for( int iLight = 0 ; iLight < mNumLights ; ++ iLight )
        {   // For each light in cache...
//                const Vec3  sep     = light->GetPosition() - GetPosition() ;
            ReadableVector3f sep = Vector3f.sub(mLightsCache[iLight].getPos3(), getPos3(), null);
            final float dist2   = sep.lengthSquared() ;
            if( dist2Candidate < dist2 )
            {   // Candidate is closer than current cached light.
//                DEBUG_BREAK() ; // not tested
                for( int j = iLight + 1 ; j < mNumLights ; ++ j )
                {   // For each subsequent light...
                    mLightsCache[ j ] = mLightsCache[ j - 1 ] ; // Shift it forward.
                }
                mLightsCache[ iLight ] = light ;
                addedLight = true ;
                break ;
            }
        }

        if( ! addedLight && ( mNumLights < MAX_NUM_LIGHTS_PER_MODEL ) )
        {   // Did not add given light and cache is not full.
            mLightsCache[ mNumLights ] = light ;    // Append given light to end of cache.
            ++ mNumLights ;
        }
    }

    /// Return number of lights cached for this model.
    public int    getNumLights() { return mNumLights ; }

    /** Return address of cached light at given index.
     \param idx  Index into light cache for this model
     */
    public Light       getLight( int idx )
    {
        assert ( idx < mNumLights ) ;
        return mLightsCache[ idx ] ;
    }
}

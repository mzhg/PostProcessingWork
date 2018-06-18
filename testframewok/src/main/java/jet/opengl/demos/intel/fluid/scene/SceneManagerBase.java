package jet.opengl.demos.intel.fluid.scene;

import jet.opengl.demos.intel.fluid.render.ApiBase;
import jet.opengl.demos.intel.fluid.utils.Camera;

/**
 * Base class for a manager for a scene of nodes.<p></p>
 * Created by Administrator on 2018/3/14 0014.
 */

public class SceneManagerBase implements ISceneManager {
    private SceneNodeBase   mRootSceneNode      ;   ///< Ancestor of all scene nodes.
    private SceneNodeBase   mLightsContainer    ;   ///< Proxy scene shallow-containing each light node.
    private SceneNodeBase   mCamerasContainer   ;   ///< Proxy scene shallow-containing each camera node.
    private ApiBase         mApi                ;   ///< Non-owned address of low-level render system device
    private Camera          mCurrentCamera      ;   ///< Address of camera currently being used to render scene.

    public SceneManagerBase( ApiBase renderApi ){
        mRootSceneNode = new SceneNodeBase(this, SceneNodeBase.sTypeId);
        mLightsContainer = new SceneNodeBase(this, SceneNodeBase.sTypeId);
        mCamerasContainer = new SceneNodeBase(this, SceneNodeBase.sTypeId);

        mApi = renderApi;
    }

    @Override
    public ISceneNode getRootSceneNode() {
        return mRootSceneNode;
    }

    @Override
    public ISceneNode getAuxLightsContainer() {
        return mLightsContainer;
    }

    @Override
    public ISceneNode getAuxCamerasContainer() {
        return mCamerasContainer;
    }

    @Override
    public Camera getCurrentCamera() {
        return mCurrentCamera;
    }

    @Override
    public ApiBase getApi() {
        return mApi;
    }

    @Override
    public void renderScene(Camera camera) {
        mCurrentCamera = camera ;
        compileLights() ;
        RenderSceneVisitor renderSceneVisitor = new RenderSceneVisitor();
        mRootSceneNode.visit(renderSceneVisitor ) ;
        mCurrentCamera = null;
    }

    /** Convenience routine to create and add new a camera to the scene.<p></p>

     <b>Note:</b> Since cameras are a special kind of node, unlikely to be rendered,
     we might want to make AddCamera a method of ISceneManager instead of
     just a utility routine in this class.  See comments in AddLight.
     */
    public Camera addCamera(){
        Camera camera = new Camera( this ) ;
        mRootSceneNode.adoptChild( camera ) ;
        mCamerasContainer.adoptChild( camera ) ;
        return camera ;
    }

    public Light  addLight(){
        Light light = new Light( this ) ;
        mRootSceneNode.adoptChild( light ) ;
        mLightsContainer.adoptChild( light ) ;
        return light ;
    }

    /** Functor to update lights cache for a light receiver.

     Rendering a model entails shading it, which requires applying one or
     more lights to the model.  A scene can have more lights than the
     underlying render system can readily support, so the render system
     has to determine which lights are most appropriate for each model.
     The model caches which lights to use to render it.

     This LightVisitor determines, for each model, which lights to use
     to render it.
     */
    class LightVisitor implements ISceneNode.IVisitor
    {
        /** Initialize a light visitor for a given light receiver.

         \param lightReceiver    ModelNode that will receive light during rendering
         */
        LightVisitor( ModelNode lightReceiver )
//                : mLightReceiver( lightReceiver )
        {
            /*PERF_BLOCK( LightVisitor__LightVisitor ) ;
            ASSERT( lightReceiver.GetNumLights() == 0 ) ;*/
            mLightReceiver = lightReceiver;
        }


        /** Update light cache of model to receive light.

         \param sceneNode    reference to scene node to visit,
         which could be a light.

         This method is run on each node in a scene, including nodes
         which are not lights.  This method ignores nodes which are
         not lights.  For nodes that are lights, this method
         determines whether the light is appropriate for shading the
         model assigned to it at construction time, and if so,
         caches the given sceneNode (a light) for that model to use
         during the shading phase.
         */
        public void doVisite( ISceneNode sceneNode )
        {
//                    PERF_BLOCK( LightVisitor__invoke ) ;

            SceneNodeBase snb = (SceneNodeBase) sceneNode;
            final int  typeId = snb.getTypeId() ;
            if( Light.sTypeId == typeId )
            {   // Found a light.
                final Light light = (Light) snb;
                mLightReceiver.updateLightsCache( light ) ;
            }
        }


        private ModelNode mLightReceiver  ;   ///< Address of model to light.
    } ;

    private final class LightModelVisitor implements ISceneNode.IVisitor{
        ISceneNode mLightsContainer ;

        LightModelVisitor(ISceneNode lightsContainer){
            mLightsContainer = lightsContainer;
        }

        @Override
        public void doVisite(ISceneNode lightReceiver) {
            SceneNodeBase snb = (SceneNodeBase)( lightReceiver ) ;
            if( snb.getTypeId() == ModelNode.sTypeId )
            {   // This node is a model, therefore a potential light receiver.
                ModelNode  mn = (ModelNode)( snb ) ;
                mn.clearLightsCache() ;
                LightVisitor lightVisitor = new LightVisitor( mn ) ;

                mLightsContainer.visit(lightVisitor ) ;
            }
        }
    }

    private void compileLights(){
        LightModelVisitor lightModelVisitor = new LightModelVisitor( mLightsContainer ) ;
        mRootSceneNode.visit(lightModelVisitor ) ;
    }

    private class RenderSceneVisitor implements ISceneNode.IVisitor
    {
        @Override
        public void doVisite(ISceneNode sceneNode) {
            sceneNode.render() ;
        }
    } ;

    @Override
    public void clear() {
        mLightsContainer.clear( ISceneNode.DO_NOT_DELETE_NOTES ) ;
        mCamerasContainer.clear( ISceneNode.DO_NOT_DELETE_NOTES ) ;
        mRootSceneNode.clear() ;
    }
}

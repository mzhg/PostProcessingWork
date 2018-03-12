package jet.opengl.demos.intel.fluid.scene;

import jet.opengl.demos.intel.fluid.render.ApiBase;
import jet.opengl.demos.intel.fluid.utils.Camera;

/**
 * Interface for a scene manager.<p></p>

 A scene manager processes renderable nodes at a relatively high
 level.  This processing might entail object-level frustum-culling,
 occlusion, deciding which lights affect which geometry, deciding
 which geometry level-of-detail to use for a particular model, or
 establishing scene-wide parameters for shadow-casting.<p></p>

 The scene manager typically will use some spatial partitioning
 data structures and algorithms; the details depend on the particular
 implementation of scene manager.<p></p>

 It would be up to the scene manager, for example, to implement a
 portal-style rendering algorithm, and to decide whether to use BSP
 trees or octrees, or simply to render everything on every frame.
 The majority of client code should neither know nor care about such
 details (except perhaps when instantiating the scene manager, if
 there is a choice among multiple of them).  Furthermore, the
 RenderQueue should be totally independent of all aspects details of
 the concrete scene manager.<p></p>

 The scene manager can also be queried to provide feedback about
 which objects were rendered and, if not, why; where they behind the
 camera, beyond the far-clip plane or hidden behind some other
 object?  And because it likely spatially partitions all nodes, a
 scene manager could also potentially provide information about
 ray-casting, such as for picking.<p></p>

 The input to a scene manager is data that is, for the most part,
 platform-independent.  Contents of a RenderQueue, in contrast,
 would typically contain platform-specific data also specific to
 a particular lower-level rendering API (such as DirectX versus
 OpenGL).  So a concrete SceneManager is expected not to have any
 platform- or API-specific code.<p></p>

 The result of processing a scene would typically result in a
 populating one or more RenderQueues with vertex buffers and render
 states.  The RenderQueues themselves might further sort objects
 by material or there could be a RenderQueue per material.  There
 might be separate RenderQueues for opaque and translucent objects.
 World geometry and UI might also reside in separate RenderQueues.<p></p>

 It would be hypothetically possible to implement the scene manager
 differently, where there would be a single kind of scene manager,
 and its various policies would be implemented via abstractions.
 But with this approach, a given concrete scene manager implements
 all policy, partly for the sake of reducing the number of virtual
 calls.<p></p>
 * Created by mazhen'gui on 2018/3/12.
 */

public interface ISceneManager {
    /// Get root scene node that contains all other nodes in this scene.
    ISceneNode    getRootSceneNode();

    /// Get auxiliary container for all lights in scene.
    /// The main scene also contains all of these lights.  This is
    /// strictly a secondary, non-owning container.
    ISceneNode    getAuxLightsContainer();

    /// Get auxiliary container for all cameras in scene.
    /// The main scene also contains all of these cameras.  This is
    /// strictly a secondary, non-owning container.
    ISceneNode    getAuxCamerasContainer();

    /// Return address of camera currently used to render scene.
    Camera getCurrentCamera();

    /// Return address of underlying rendering API object that this manager uses to render its scene.
    ApiBase       getApi();

    /// Render a scene with the given camera.
    void          renderScene(Camera  camera );

    /// Clear this scene.
    void          clear();
}

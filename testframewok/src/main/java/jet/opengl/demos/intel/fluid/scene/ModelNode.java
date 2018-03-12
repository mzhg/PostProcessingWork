package jet.opengl.demos.intel.fluid.scene;

/**
 * Scene node representing a model.<p></p>

 A model has ModelData which is essentially a collection of meshes,
 where each mesh has a material and geometry (e.g. vertices, triangles).
 * Created by mazhen'gui on 2018/3/12.
 */

public class ModelNode extends SceneNodeBase {
    /**
     * Construct base part of a scene node.<p></p>
     * Scene nodes can be assigned a "type" which can be used, for example, by
     * scene node visitors (ISceneNode::IVisitor) to determine whether that
     * visitor should operate on the node it visits.
     *
     * @param sceneManager Scene manager that contains this node.
     * @param typeId       Which type of node this is.
     */
    public ModelNode(ISceneManager sceneManager, int typeId) {
        super(sceneManager, typeId);
    }
}

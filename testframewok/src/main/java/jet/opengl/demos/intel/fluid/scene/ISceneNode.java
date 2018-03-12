package jet.opengl.demos.intel.fluid.scene;

import jet.opengl.demos.intel.fluid.utils.IRenderable;

/**
 * Interface for a node in a scene graph.<p></p>
 * Created by mazhen'gui on 2018/3/12.
 */

public interface ISceneNode extends IRenderable{
    interface IVisitor
    {
        /** Operation to perform on each SceneNode visited.
         @param sceneNode    ISceneNode to visit.
         */
        void doVisite( ISceneNode sceneNode );
    }

    // enum ClearDeletePolicy
    int DO_NOT_DELETE_NOTES = 0;
    int DELETE_NODES = 1;

    /** Return parent of this node, or NULL if it is the root. */
    ISceneNode      getParent();

    /** Add the given node as a child to this one. */
    void            adoptChild( ISceneNode sceneNode);

    /** Remove the given node, which must be an immediate child of this one. */
    void            orphanChild( ISceneNode sceneNode);

    /** Visit this node, then recursively visit each descendant node in the scene.
     @param  visitor  Visitor to apply to each scene node.
     */
    void            visit( IVisitor  visitor );

    /** Remove all child nodes this node parents. */
    void            Clear( int deleteNode /*= DELETE_NODES*/ );
}

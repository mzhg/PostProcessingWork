package jet.opengl.demos.intel.fluid.scene;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;

import java.util.LinkedList;

/**
 * Base class for a scene node.<p></p>
 *
 * Scene nodes represent objects in a render scene.<p></p>
 *
 * Specialized scene nodes include lights, cameras and models.<p></p>
 *
 * Games can specialize SceneNodeBase to add custom scene nodes,
 * such as particle systems and other visual effects, nodes to use for
 * visualization and posable character models.<p></p>
 * Created by mazhen'gui on 2018/3/12.
 */
public class SceneNodeBase implements ISceneNode{

    public static final int sTypeId = 1397641028;  // 'SNOD'
    /** Type identifier for this object */
    private int                mTypeId         ;
    /** Manager that contains the scene with this node.  Non-owning cache. */
    private ISceneManager      mSceneManager   ;
    /** SceneNode that immediately contains this one.  Non-owning. */
    private SceneNodeBase      mParent         ;
    /** Child nodes owned by this object. */
    private final LinkedList<ISceneNode> mSceneNodes = new LinkedList<>();
    /** Position relative to parent node */
    private final Vector3f     mPosition = new Vector3f();
    /** Orientation, in axis-angle form, relative to parent node */
    private final Quaternion   mOrientation = new Quaternion();
    /** Scale relative to parent node */
    private final Vector3f     mScale = new Vector3f(1,1,1);
    /** Local-to-world transform to place this node into world coordinates.  Assigned from mPosition, mOrientation and mScale. */
    private final Matrix4f     mLocalToWorld = new Matrix4f();

    private SceneNodeBase(){} // Disallow default construction.

    /** Construct base part of a scene node.<p></p>
     Scene nodes can be assigned a "type" which can be used, for example, by
     scene node visitors (ISceneNode::IVisitor) to determine whether that
     visitor should operate on the node it visits.
     @param sceneManager Scene manager that contains this node.

     @param typeId   Which type of node this is.
     */
    public SceneNodeBase( ISceneManager sceneManager , int typeId ){
        mTypeId = typeId;
        mSceneManager = sceneManager;
    }

    /** Update this node's absolute transformation based on its parent.*/
    public void update()
    {
//        PERF_BLOCK( SceneNodeBase__Update ) ;

//        ASSERT( static_cast< SceneNodeBase * >( this )->GetTypeId() != sTypeId ) ; // Derived class must reassign type id.
    }

    @Override
    public void render() {}

    @Override
    public ISceneNode getParent() {
        return mParent;
    }

    /** Add the given node as a child to this one, and take ownership of it. */
    @Override
    public void adoptChild(ISceneNode child) {
        mSceneNodes.add( child ) ;
    }

    /** Remove the given node, a child of this one, and relinquish ownership of it. */
    @Override
    public void orphanChild(ISceneNode child) {
        mSceneNodes.remove(child);
    }

    @Override
    public void visit(IVisitor visitor) {
        visitor.doVisite( this ) ;
        for( ISceneNode sceneNode : mSceneNodes )
        {
//            ISceneNode * & sceneNode = * iter ;
            assert ( ((SceneNodeBase) sceneNode ).getTypeId() != sTypeId ) ; // Derived class must reassign type id.
            visitor.doVisite(sceneNode ) ;
        }
    }

    /** Clear children from this node.

     @param deleteNode Whether to delete node or just remove it.
     Usually, a SceneNode owns its children, in the sense that the parent
     deletes its children.  But some SceneNodes, (for example the lights
     and cameras containers in the scene manager) are redundant auxiliary
     shallow containers that do not own their contents; they exist only
     to idenitfy certain kinds of nodes.  That is useful to optimize
     iterating over those kinds of nodes, and it is useful to use ISceneNode
     as the container since then ISceneNode::Visit can operate with it.
     */
    @Override
    public void clear(int deleteNode) {
        for( ISceneNode sceneNode : mSceneNodes )
        {
            assert ( ((SceneNodeBase) sceneNode ).getTypeId() != sTypeId ); // Derived class must reassign type id.
            sceneNode.clear( deleteNode ) ;
            if( DELETE_NODES == deleteNode )
            {
//                delete sceneNode ;
            }
        }
        mSceneNodes.clear() ;
    }

    /** Return SceneManager that manages this scene node. */
    protected ISceneManager getSceneManager() { return mSceneManager ; }

    /// Return type identifier for this node.
    public int getTypeId() { return mTypeId ; }

    public void renderChildren(){
//        PERF_BLOCK( SceneNodeBase__RenderChildren ) ;
//        ASSERT( static_cast< SceneNodeBase * >( this )->GetTypeId() != sTypeId ) ; // Derived class must reassign type id.
        for( ISceneNode sceneNode : mSceneNodes )
        {
//            ISceneNode * & sceneNode = * iter ;
            assert ( ((SceneNodeBase)sceneNode).getTypeId() != sTypeId ) ; // Derived class must reassign type id.
            sceneNode.render();
        }
    }

    /// Set world-space position that applies to this scene node as a translation.
    public void setPosition(ReadableVector3f position ) { mPosition.set(position); }

    /// Get world-space position that applies to this scene node as a translation
    public ReadableVector3f getPos3() { return mPosition ; }

    public void setOrientation(ReadableVector4f orientation )   { mOrientation.set(orientation); ; }
    public Quaternion getOrientation() { return mOrientation ; }

    /// Set scale that applies to this scene node. Only x, y, z components have meaning.
    public void setScale(ReadableVector3f scale ) { mScale.set(scale); }

    /// Return scale that applies to this scene node.
    public ReadableVector3f getScale() { return mScale; }

    /// Set local-to-world transform for this scene node position, orientation and scale..
    public void setLocalToWorld(){
        mLocalToWorld.m30 = 0;
        mLocalToWorld.m31 = 0;
        mLocalToWorld.m32 = 0;
        mLocalToWorld.m33 = 1;

        mOrientation.toMatrix(mLocalToWorld);
        mLocalToWorld.scale(mScale);

        mLocalToWorld.m30 = mPosition.x;
        mLocalToWorld.m31 = mPosition.y;
        mLocalToWorld.m32 = mPosition.z;
    }

    /** Return scale that applies to this scene node. */
    public Matrix4f getLocalToWorld() { return mLocalToWorld ; }
}

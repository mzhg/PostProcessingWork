package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.util.CacheBuffer;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaRenderMeshDrawList {

    public static final int SIZE = Integer.MAX_VALUE/(1024 * 1024);
    private final List< Entry >   m_drawList = new ArrayList<>();

    public void Reset( )                            { m_drawList.clear(); }
    public int  Count( )                            { return m_drawList.size(); }
    public void Insert( VaRenderMesh mesh, ReadableVector3f scale, ReadableVector3f translation, ReadableVector4f rotation, ReadableVector4f color /*= vaVector4( 1.0f, 1.0f, 1.0f, 1.0f )*/, int subPartMask /*= 0xFFFFFFFF*/ ) {
        m_drawList.add(new Entry( mesh, scale, translation, rotation, color, subPartMask ) );
    }
    public void Insert( VaRenderMesh mesh, ReadableVector3f translation, ReadableVector4f rotation, ReadableVector4f color /*= vaVector4( 1.0f, 1.0f, 1.0f, 1.0f )*/, int subPartMask /*= 0xFFFFFFFF*/ ){
        m_drawList.add(new Entry( mesh, translation, rotation, color, subPartMask ) );
    }

    public void Insert( VaRenderMesh mesh, Matrix4f transform){
        Insert(mesh, transform, new Vector4f(1,1,1,1), 0xFFFFFFFF);
    }

    public void Insert( VaRenderMesh mesh, Matrix4f transform, ReadableVector4f color /*= vaVector4( 1.0f, 1.0f, 1.0f, 1.0f )*/, int subPartMask /*= 0xFFFFFFFF*/ ){
//        vaVector3 scale, translation; vaQuaternion rotation;
        final Vector3f scale = CacheBuffer.getCachedVec3();
        final Vector3f translation = CacheBuffer.getCachedVec3();
        final Quaternion rotation = CacheBuffer.getCachedQuat();
        try{
            Matrix4f.decompose(transform, scale, rotation, translation );
            Insert( mesh, scale, translation, rotation, color, subPartMask );
        }finally {
            CacheBuffer.free(scale);
            CacheBuffer.free(translation);
            CacheBuffer.free(rotation);
        }
    }

//        const Entry &                                   operator[] ( int index ) const      { return m_drawList[index]; }
    public Entry get(int index) { return m_drawList.get(index);}

    public static final class Entry
    {
        public VaRenderMesh               Mesh;
        public final Vector3f             Translation;
        public final Vector3f             Scale;
        public final Quaternion           Rotation;
        public int                        SubPartMask;

        // per-instance data; could be a color but it could be an index into more elaborate data storage somewhere, etc.
        public final Vector4f             Color;

        public Entry(VaRenderMesh mesh, ReadableVector3f translation, ReadableVector4f rotation){
            this(mesh, translation, rotation, Vector4f.ONE, 0xFFFFFFFF);
        }

        public Entry(VaRenderMesh mesh, ReadableVector3f translation, ReadableVector4f rotation, ReadableVector4f color /*= vaVector4( 1.0f, 1.0f, 1.0f, 1.0f )*/, int subPartMask /*= 0xFFFFFFFF*/ ) //: Mesh(mesh), Translation( translation ), Rotation(rotation), Color( color ), SubPartMask(subPartMask), Scale( 1.0f, 1.0f, 1.0f ) { }
        {
            Mesh = mesh;
            Translation = new Vector3f(translation);
            Scale       = new Vector3f(1,1,1);
            Rotation    = new Quaternion(rotation);
            SubPartMask = subPartMask;
            Color       = new Vector4f(color);

        }

        public Entry( VaRenderMesh mesh, ReadableVector3f scale, ReadableVector3f translation, ReadableVector4f rotation){
            this(mesh, scale, translation, rotation, Vector4f.ONE, 0xFFFFFFFF);
        }

        public Entry( VaRenderMesh mesh, ReadableVector3f scale, ReadableVector3f translation, ReadableVector4f rotation, ReadableVector4f color /*= vaVector4( 1.0f, 1.0f, 1.0f, 1.0f )*/,int subPartMask /*= 0xFFFFFFFF*/ )// : Mesh( mesh ), Scale( scale ), Translation( translation ), Rotation( rotation ), Color( color ), SubPartMask( subPartMask ) { }
        {
            Mesh = mesh;
            Translation = new Vector3f(translation);
            Scale       = new Vector3f(scale);
            Rotation    = new Quaternion(rotation);
            SubPartMask = subPartMask;
            Color       = new Vector4f(color);
        }
    }
}

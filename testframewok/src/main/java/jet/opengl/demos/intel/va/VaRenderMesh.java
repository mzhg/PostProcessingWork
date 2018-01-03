package jet.opengl.demos.intel.va;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.util.Numeric;
import jet.opengl.postprocessing.util.StackByte;
import jet.opengl.postprocessing.util.StackFloat;
import jet.opengl.postprocessing.util.StackInt;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaRenderMesh extends VaAssetResource implements Disposeable{
    public static final int
            WindingOrder_None = 0,            // todo: remove this, can't be none
            WindingOrder_Clockwise = 1,
            WindingOrder_CounterClockwise = 2;

    // wstring const                                   m_name;                 // unique (within renderMeshManager) name
    private final TT_Trackee< VaRenderMesh >                  m_trackee;
    private VaRenderMeshManager                           m_renderMeshManager;

    private int                                  m_frontFaceWinding;
    private boolean                                            m_tangentBitangentValid;

    private VaTriangleMesh               m_triangleMesh;

    // This will most likely go out in VA_02; only one part per mesh will be supported in order to increase simplicity on the rendering backend, allow for easier instancing, etc.
    private final ArrayList<SubPart> m_parts = new ArrayList<>();

    private final VaBoundingBox  m_boundingBox = new VaBoundingBox();      // local bounding box around the mesh

    protected VaRenderMesh( VaRenderMeshManager renderMeshManager, UUID uid ){
        super(uid);

        m_trackee = new TT_Trackee<>(renderMeshManager.GetRenderMeshTracker(), this);
        m_renderMeshManager = renderMeshManager;

        m_frontFaceWinding          = WindingOrder_CounterClockwise;
//        m_boundingBox               = vaBoundingBox::Degenerate;
        m_tangentBitangentValid     = true;
    }

    //const wstring &                                 GetName( ) const                                    { return m_name; };

    public VaRenderMeshManager                           GetManager( )                                 { return m_renderMeshManager; }
    public int                                             GetListIndex( )                               { return m_trackee.GetIndex( ); }

    public VaBoundingBox                           GetAABB( )                                    { return m_boundingBox; }

    // This will most likely go out in VA_02; only one part per mesh will be supported in order to increase simplicity on the rendering backend, allow for easier instancing, etc.
    public List<SubPart> GetParts( )                                   { return m_parts; }
    public void                                            SetParts( List<SubPart> parts ) { /*m_parts = parts;*/ m_parts.clear(); m_parts.addAll(parts); } //assert( parts.size() <= c_maxSubParts ); }

    public VaTriangleMesh        GetTriangleMesh(  )                           { return m_triangleMesh; }
    public void                  SetTriangleMesh( VaTriangleMesh mesh ) {m_triangleMesh = mesh;}
    public void    CreateTriangleMesh( /*const vector<StandardVertex> &*/ StackByte vertices, /*const vector<uint32> &*/ StackInt indices ){
//        shared_ptr<StandardTriangleMesh> mesh = VA_RENDERING_MODULE_CREATE_SHARED( vaRenderMesh::StandardTriangleMesh );
        VaTriangleMesh mesh = VaRenderingModuleRegistrar.CreateModuleTyped("StandardTriangleMesh", null);

        mesh.Vertices = vertices;
        mesh.Indices = indices;

        SetTriangleMesh( mesh );

        UpdateAABB( );
    }

    public int                                  GetFrontFaceWindingOrder( )                   { return m_frontFaceWinding; }
    public void                                 SetFrontFaceWindingOrder( int winding )  { m_frontFaceWinding = winding; }

    public boolean                              GetTangentBitangentValid( )                   { return m_tangentBitangentValid; }
    public void                                 SetTangentBitangentValid( boolean value )              { m_tangentBitangentValid = value; }

    public void                                            UpdateAABB( ){
        if( m_triangleMesh != null )
             VaTriangleMesh.CalculateBounds( m_triangleMesh.Vertices,  StandardVertex.SIZE, 0, m_boundingBox);
        else {
//            m_boundingBox = vaBoundingBox::Degenerate;
            m_boundingBox.Min.set(0,0,0);
            m_boundingBox.Size.set(0,0,0);
        }
    }

    public boolean                                            Save( VaStream outStream ){ throw new UnsupportedOperationException();}
    public boolean                                            Load( VaStream inStream ) { throw new UnsupportedOperationException();}
    public  void                                    ReconnectDependencies( ){
        for( int i = 0; i < m_parts.size(); i++ )
        {
            /*std::shared_ptr<vaRenderMaterial> materialSharedPtr;
            VaUIDObjectRegistrar.GetInstance( ).ReconnectDependency*//*<vaRenderMaterial>*//*( *//*materialSharedPtr,*//* m_parts.get(i).MaterialID );
            m_parts[i].Material = materialSharedPtr;*/
            m_parts.get(i).Material = VaUIDObjectRegistrar.GetInstance( ).ReconnectDependency/*<vaRenderMaterial>*/( /*materialSharedPtr,*/ m_parts.get(i).MaterialID );
        }
    }

    // create mesh with normals, with provided vertices & indices
    public static VaRenderMesh                 Create(Matrix4f transform, /*const std::vector<vaVector3> &*/StackFloat vertices, /*const std::vector<vaVector3> &*/StackFloat normals,
                                                      /*const std::vector<vaVector4> &*/StackFloat tangents, /*const std::vector<vaVector2> &*/StackFloat texcoords0,
                                                      /*const std::vector<vaVector2> &*/StackFloat texcoords1, /*const std::vector<uint32> &*/StackInt indices,
                                                      int frontFaceWinding /*= vaWindingOrder::CounterClockwise*/ ){
        int vertex_count = vertices.size() / 3;
        int normal_count = normals.size() / 3;
        int tangent_count = tangents.size() / 4;
        int texcoord0_count = texcoords0.size()/2;
        int texcoord1_count = texcoords1.size()/2;

        if((vertex_count != normal_count) || (vertex_count != tangent_count) || (vertex_count != texcoord0_count) || (vertex_count != texcoord1_count)){
            throw new IllegalArgumentException();
        }


        StandardVertex vertex = new StandardVertex();
        StackByte vertexBytes = new StackByte(StandardVertex.SIZE * vertex_count);
        vertex.Color = 0xFFFFFFFF;
        for(int i = 0; i < vertex_count; i++){
            vertex.Position.x = vertices.get(i * 3 + 0);
            vertex.Position.y = vertices.get(i * 3 + 1);
            vertex.Position.z = vertices.get(i * 3 + 2);

            vertex.Normal.x = normals.get(i * 3 + 0);
            vertex.Normal.y = normals.get(i * 3 + 1);
            vertex.Normal.z = normals.get(i * 3 + 2);

            vertex.Tangent.x = tangents.get(i * 4 + 0);
            vertex.Tangent.y = tangents.get(i * 4 + 1);
            vertex.Tangent.z = tangents.get(i * 4 + 2);
            vertex.Tangent.w = tangents.get(i * 4 + 3);

            vertex.TexCoord0.x = texcoords0.get(i * 2 + 0);
            vertex.TexCoord0.y = texcoords0.get(i * 2 + 1);

            vertex.TexCoord1.x = texcoords1.get(i * 2 + 0);
            vertex.TexCoord1.y = texcoords1.get(i * 2 + 1);

            Matrix4f.transformCoord(transform, vertex.Position, vertex.Position);
            Matrix4f.transformNormal(transform, vertex.Normal, vertex.Normal);
            Matrix4f.transformNormal(transform, vertex.Tangent, vertex.Tangent);

            vertex.Store(vertexBytes);
        }

        VaRenderMesh mesh = VaRenderMeshManager.GetInstance( ).CreateRenderMesh( );
        mesh.CreateTriangleMesh( vertexBytes, indices );
//        mesh.SetParts( vector<vaRenderMesh::SubPart>( 1, vaRenderMesh::SubPart( 0, (int)indices.size( ), weak_ptr<vaRenderMaterial>( ) ) ) );
        mesh.SetParts(Arrays.asList(new SubPart(0, indices.size( ), null)));
        mesh.SetFrontFaceWindingOrder( frontFaceWinding );

        return mesh;
    }

    // these use vaStandardShapes::Create* functions and create shapes with center in (0, 0, 0) and each vertex magnitude of 1 (normalized), except where specified otherwise, and then transformed by the provided transform
    public static VaRenderMesh CreatePlane( Matrix4f transform, float sizeX /*= 1.0f*/, float sizeY /*= 1.0f*/ ){
        StackFloat  vertices = new StackFloat(3 * 4);
        StackFloat  normals = new StackFloat(3 * 4);
        StackFloat  tangents = new StackFloat(4 * 4);
        StackFloat  texcoords0 = new StackFloat(2 * 4);
        StackFloat  texcoords1 = new StackFloat(2 * 4);
        StackInt     indices = new StackInt(6);

        VaStandardShapes.CreatePlane( vertices, indices, sizeX, sizeY );
        int windingOrder = WindingOrder_CounterClockwise;

        VaTriangleMeshTools.GenerateNormals( normals, vertices, indices, windingOrder == WindingOrder_CounterClockwise );

        Vector4f defaultTan = new Vector4f(1,0,0,1);
        Vector3f temp = new Vector3f();
        for( int i = 0; i < vertices.size( ); i++ )
        {
//            tangents[i] = vaVector4( 1.0f, 0.0f, 0.0f, 1.0f );
            VaTriangleMeshTools.loadVertex(temp, vertices.getData(), i*3);

            tangents.push(defaultTan);
            Vector2f texcoord = new Vector2f(temp.x / sizeX + 0.5f,temp.y / sizeY + 0.5f );
            texcoords0.push(texcoord);
            texcoords1.push(texcoord);
        }

        return Create( transform, vertices, normals, tangents, texcoords0, texcoords1, indices, WindingOrder_CounterClockwise );
    }

    public static VaRenderMesh CreateTetrahedron( Matrix4f transform, boolean shareVertices ){
        StackFloat  vertices = new StackFloat(3 * 4);
        StackFloat  normals = new StackFloat(3 * 4);
        StackFloat  tangents = new StackFloat(4 * 4);
        StackFloat  texcoords0 = new StackFloat(2 * 4);
        StackFloat  texcoords1 = new StackFloat(2 * 4);
        StackInt     indices = new StackInt(6);

        VaStandardShapes.CreateTetrahedron( vertices, indices, shareVertices );
        int windingOrder = WindingOrder_Clockwise;

        VaTriangleMeshTools.GenerateNormals( normals, vertices, indices, windingOrder == WindingOrder_CounterClockwise );

        FillDummyTTT( vertices, normals, tangents, texcoords0, texcoords1 );

        return Create( transform, vertices, normals, tangents, texcoords0, texcoords1, indices, windingOrder );
    }

    /*public static VaRenderMesh                 CreateCube( Matrix4f transform, bool shareVertices, float edgeHalfLength = 0.7071067811865475f );
    public static VaRenderMesh                 CreateOctahedron( Matrix4f transform, bool shareVertices );
    public static VaRenderMesh                 CreateIcosahedron( Matrix4f transform, bool shareVertices );
    public static VaRenderMesh                 CreateDodecahedron( Matrix4f transform, bool shareVertices );
    public static VaRenderMesh                 CreateSphere( Matrix4f transform, int tessellationLevel, bool shareVertices );
    public static VaRenderMesh                 CreateCylinder( Matrix4f transform, float height, float radiusBottom, float radiusTop, int tessellation, bool openTopBottom, bool shareVertices );
    */
    public static VaRenderMesh                 CreateTeapot( Matrix4f transform ){
        return null;
    }

    // dummy tangents, for better, http://www.terathon.com/code/tangent.html or http://developer.nvidia.com/object/NVMeshMender.html
    private static void FillDummyTTT( StackFloat vertices, StackFloat normals, StackFloat tangents, StackFloat texcoords0, StackFloat texcoords1 )
    {
        Vector3f bitangent = new Vector3f();
        Vector3f vertex = new Vector3f();
        Vector3f normal = new Vector3f();
        for( int i = 0; i < vertices.size( )/3; i++ )
        {
            VaTriangleMeshTools.loadVertex(vertex, vertices.getData(), i*3);
            VaTriangleMeshTools.loadVertex(normal, normals.getData(), i*3);
//            vaVector3 bitangent = ( vertices[i] + vaVector3( 0.0f, 0.0f, -5.0f ) ).Normalize( );
            bitangent.set(vertex);
            bitangent.z += -5.0f;
            bitangent.normalise();

            if( Vector3f.dot( bitangent, normal ) > 0.9f ) {
//                bitangent = (vertices[i] + vaVector3(-5.0f, 0.0f, 0.0f)).Normalize();
                bitangent.set(vertex);
                bitangent.x += -5.0f;
                bitangent.normalise();
            }
//            tangents[i] = vaVector4( vaVector3::Cross( bitangent, normals[i] ).Normalize( ), 1.0 );
            Vector3f.cross(bitangent, normal, bitangent).normalise();
            tangents.push(bitangent);
            tangents.push(1.0f);

            Vector2f texcoord = new Vector2f( vertex.x / 2.0f + 0.5f, vertex.y / 2.0f + 0.5f );

            texcoords0.push(texcoord);
            texcoords1.push(texcoord);
        }
    }

    @Override
    public void dispose() {
        m_trackee.release();
    }

    // only standard mesh storage supported at the moment
    public static final class StandardVertex implements VaTriangleMesh.VertexType
    {
        public static final int SIZE = Vector4f.SIZE * 4;
        // first 4 bytes
        public final Vector3f Position = new Vector3f();
        public int    Color;

        // next 4 bytes (.w not encoded - can be used for skinning indices for example; should probably be compressed to 16bit floats on the rendering side)
        public final Vector4f Normal = new Vector4f();

        // next 4 bytes (.w stores -1/1 handedness for determining bitangent)
        public final Vector4f   Tangent = new Vector4f();

        // next 2 bytes (first UVs; should probably be compressed to 16bit floats on the rendering side)
        public final Vector2f TexCoord0 = new Vector2f();

        // next 2 bytes (second UVs; should probably be compressed to 16bit floats on the rendering side)
        public final Vector2f   TexCoord1 = new Vector2f();

        public StandardVertex( ) { }
        /*StandardVertex( const vaVector3 & position ) : Position( position ), Normal( vaVector4( 0, 1, 0, 0 ) ), Color( 0xFF808080 ), Tangent( 0, 0, 0, 0 ), TexCoord0( 0, 0 ), TexCoord1( 0, 0 ) {}
        StandardVertex( const vaVector3 & position, const uint32_t & color ) : Position( position ), Normal( vaVector4( 0, 1, 0, 0 ) ), Tangent( 0, 0, 0, 0 ), TexCoord0( 0, 0 ), TexCoord1( 0, 0 ), Color( color ) { }
        StandardVertex( const vaVector3 & position, const vaVector4 & normal, const uint32_t & color ) : Position( position ), Normal( normal ), Color( color ), Tangent( 0, 0, 0, 0 ), TexCoord0( 0, 0 ), TexCoord1( 0, 0 ) { }
        StandardVertex( const vaVector3 & position, const vaVector4 & normal, const vaVector4 & tangent, const vaVector2 & texCoord0, const uint32_t & color ) : Position( position ), Normal( normal ), Tangent( tangent ), TexCoord0( texCoord0 ), TexCoord1( 0, 0 ), Color( color ) { }
        StandardVertex( const vaVector3 & position, const vaVector4 & normal, const vaVector4 & tangent, const vaVector2 & texCoord0, const vaVector2 & texCoord1, const uint32_t & color ) : Position( position ), Normal( normal ), Tangent( tangent ), TexCoord0( texCoord0 ), TexCoord1( texCoord1 ), Color( color ) { }
*/
        /*bool operator ==( const StandardVertex & cmpAgainst )
        {
            return      ( Position == cmpAgainst.Position ) && ( Normal == cmpAgainst.Normal ) && ( Tangent == cmpAgainst.Tangent ) && ( TexCoord0 == cmpAgainst.TexCoord0 ) && ( TexCoord1 == cmpAgainst.TexCoord1 ) && ( Color == cmpAgainst.Color );
        }*/

        @Override
        public int GetStride() {
            return SIZE;
        }

        @Override
        public void Store(StackByte bytes) {
            int offset = bytes.size();
            bytes.resize(offset + GetStride());
            byte[] data = bytes.getData();

            Numeric.getBytes(Position.x, data, offset);  offset += 4;
            Numeric.getBytes(Position.y, data, offset);  offset += 4;
            Numeric.getBytes(Position.z, data, offset);  offset += 4;
            Numeric.getBytes(Color, data, offset);  offset += 4;

            Numeric.getBytes(Normal.x, data, offset);  offset += 4;
            Numeric.getBytes(Normal.y, data, offset);  offset += 4;
            Numeric.getBytes(Normal.z, data, offset);  offset += 4;
            Numeric.getBytes(Normal.w, data, offset);  offset += 4;

            Numeric.getBytes(Tangent.x, data, offset);  offset += 4;
            Numeric.getBytes(Tangent.y, data, offset);  offset += 4;
            Numeric.getBytes(Tangent.z, data, offset);  offset += 4;
            Numeric.getBytes(Tangent.w, data, offset);  offset += 4;

            Numeric.getBytes(TexCoord0.x, data, offset);  offset += 4;
            Numeric.getBytes(TexCoord0.y, data, offset);  offset += 4;

            Numeric.getBytes(TexCoord1.x, data, offset);  offset += 4;
            Numeric.getBytes(TexCoord1.y, data, offset);  offset += 4;
        }
    }

    public static final class StandardVertexAnimationPart
    {
        public int      Indices;    // (8888_UINT)
        public int      Weights;    // (8888_UNORM)
    };

    //static const int                                c_maxSubParts   = 32;

    // This will most likely go out in VA_02; only one part per mesh will be supported in order to increase simplicity on the rendering backend, allow for easier instancing, etc.
    public static final class SubPart
    {
        // not sure if subpart name is needed - Mesh and Material will have names, should be enough? still, I'll leave it here for future
        // static const int                            cNameMaxLength          = 32;
        // char                                        Name[cNameMaxLength];

        int                                         IndexStart;
        int                                         IndexCount;
        VaRenderMaterial                            Material;
        // used during loading - could be moved into a separate structure and disposed of after loading
        UUID MaterialID;

        SubPart( ) { }
        SubPart( int indexStart, int indexCount, VaRenderMaterial material ) /*: IndexStart( indexStart ), IndexCount( indexCount ), Material( material ) { }*/
        {
            IndexStart = indexStart;
            IndexCount = indexCount;
            Material = material;
        }
    }
}

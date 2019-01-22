package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Matrix4f;

import jet.opengl.postprocessing.common.Disposeable;

public class GeometryFX_Filter implements Disposeable {

    public static final int
        GeometryFX_FilterDuplicateIndices = 0x1,
        GeometryFX_FilterBackface = 0x2,
        GeometryFX_FilterFrustum = 0x8,
        GeometryFX_FilterSmallPrimitives = 0x20,
        GeometryFX_ClusterFilterBackface = 0x1 << 10;

    private GeometryFX_OpaqueFilterDesc impl_;

    public GeometryFX_Filter(GeometryFX_FilterDesc pDesc){
        GeometryFX_FilterDesc desc = pDesc != null ? pDesc :new GeometryFX_FilterDesc();

        impl_ = new GeometryFX_OpaqueFilterDesc(desc);
    }

    /**
     Register meshes for the static mesh renderer.

     This function must be called exactly once.

     @note This function may call functions on the ID3D11Device.
     */
    MeshHandle[] RegisterMeshes(int meshCount, int [] verticesInMesh,  int [] indicesInMesh){
        assert(meshCount > 0);
        assert(verticesInMesh != null);
        assert(indicesInMesh != null);

        return impl_.RegisterMeshes(meshCount, verticesInMesh, indicesInMesh);
    }

    /**
     Set the data for a mesh.

     RegisterMeshes() must have been called previously.

     @note This function may call functions on the ID3D11Device and the
     immediate context.
     */
    void SetMeshData(MeshHandle handle, float[] vertexData, int[] indexData){
        assert(vertexData != null);
        assert(indexData != null);

        impl_.SetMeshData(handle, vertexData, indexData);
    }

    /**
     Start a render pass.

     From here on, the context should no longer be used by the application
     until EndRender() has been called.

     @note If the multi-indirect-draw extension is present, the context must be
     equal to the immediate context.

     @note A render pass will change the D3D device state. In particular, the
     following states will be changed:

     - vertex shader, pixel shader and compute shader (the library assumes no
     hull or domain shader is bound)
     - resources bound to the vertex shader, pixel shader and compute shader
     - the topology
     */
    void BeginRender(/*ID3D11DeviceContext *pContext, const */GeometryFX_FilterRenderOptions options,
        Matrix4f view, Matrix4f projection,int windowWidth,  int windowHeight){

//        assert(context != nullptr);
        assert(windowWidth > 0);
        assert(windowHeight > 0);

        FilterContext filterContext = new FilterContext();
        filterContext.options = options;
        filterContext.projection.load(projection);
        filterContext.view.load(view);
        filterContext.windowWidth = windowWidth;
        filterContext.windowHeight = windowHeight;

        /*const auto inverseView = DirectX::XMMatrixInverse (nullptr, view);
        DirectX::XMFLOAT4X4 float4x4;
        XMStoreFloat4x4 (&float4x4, inverseView);

        filterContext.eye = DirectX::XMVectorSet (float4x4._41, float4x4._42, float4x4._43, 1);*/

        Matrix4f.decompseRigidMatrix(view, filterContext.eye, null, null);

        impl_.BeginRender(/*context,*/ filterContext);
    }

    /**
     Render a mesh.

     Only valid within a BeginRender/EndRender pair. This function will render
     the mesh with the specified world matrix.
     */
    void RenderMesh(MeshHandle handle, Matrix4f world){
        impl_.RenderMeshInstanced(handle, world);
    }

    /**
     Render a mesh with instancing.

     Only valid within a BeginRender/EndRender pair. This function will render
     a number of instances, each with its own world matrix.
     */
    void RenderMeshInstanced(MeshHandle handle, Matrix4f[] worlds){
        impl_.RenderMeshInstanced(handle, worlds);
    }

    /**
     End a render pass.

     This function will call functions on the context passed to BeginRender().
     */
    void EndRender(){
        impl_.EndRender();
    }

    /*
     Get the buffers for a mesh.

     If a parameter is set to null, it won't be written.
     */
    /*void GetBuffersForMesh(const MeshHandle &handle,
                           ID3D11Buffer **ppVertexBuffer,
                           int32 *pVertexOffset,
                           ID3D11Buffer **ppIndexBuffer,
                           int32 *pIndexOffset) const;*/

    /**
     Get info about a mesh.
     */
    int GetMeshInfo(MeshHandle  handle/*, int32 *pIndexCount*/){
        return impl_.GetMeshInfo(handle/*, indexCount*/);
    }

    @Override
    public void dispose() {
        // todo
    }
}

package jet.opengl.demos.amdfx.geometry;

import jet.opengl.postprocessing.buffer.BufferGL;

interface IMeshManager {
    void Allocate(/*ID3D11Device *pDevice, const*/ int meshCount, /*const*/ int []verticesPerMesh,
        /*const*/ int []indicesPerMesh);

    void SetData(/*ID3D11Device *pDevice, ID3D11DeviceContext *pContext,*/  int meshIndex,
                                                                            float[] pVertexData, int[] pIndexData);

    StaticMesh GetMesh(int index);
    int GetMeshCount();

    BufferGL GetMeshConstantsBuffer();

    BufferGL GetIndexBuffer ();
    BufferGL GetVertexBuffer ();
    BufferGL GetIndexBufferSRV ();
    BufferGL GetVertexBufferSRV ();

    static IMeshManager CreateGlobalMeshManager(){
        return new MeshManagerGlobal();
    }
}

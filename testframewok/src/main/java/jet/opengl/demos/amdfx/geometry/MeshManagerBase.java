package jet.opengl.demos.amdfx.geometry;

import com.nvidia.developer.opengl.ui.NvUIButton;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

abstract class MeshManagerBase implements IMeshManager{
    List<StaticMesh> meshes_ = new ArrayList<>();
    BufferGL meshConstantsBuffer_;
    BufferGL meshConstantsBufferView_;

    void CreateMeshConstantsBuffer(/*ID3D11Device *device*/) {
        MeshConstants[] meshConstants = new MeshConstants[GetMeshCount()];

        for (int i = 0; i < GetMeshCount(); ++i)
        {
            StaticMesh mesh = meshes_.get(i);
            meshConstants[i] = new MeshConstants();
            meshConstants[i].faceCount = mesh.faceCount;
            meshConstants[i].indexOffset = mesh.indexOffset;
            meshConstants[i].vertexCount = mesh.vertexCount;
            meshConstants[i].vertexOffset = mesh.vertexOffset;
        }

        /*D3D11_BUFFER_DESC bufferDesc = {};
        bufferDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE;
        bufferDesc.ByteWidth = static_cast<UINT>(meshConstants.size() * sizeof(MeshConstants));
        bufferDesc.MiscFlags = D3D11_RESOURCE_MISC_BUFFER_STRUCTURED;
        bufferDesc.StructureByteStride = sizeof(MeshConstants);
        bufferDesc.Usage = D3D11_USAGE_IMMUTABLE;

        D3D11_SUBRESOURCE_DATA initialData;
        initialData.pSysMem = meshConstants.data();
        initialData.SysMemPitch = bufferDesc.ByteWidth;
        initialData.SysMemSlicePitch = bufferDesc.ByteWidth;

        device->CreateBuffer(&bufferDesc, &initialData, &meshConstantsBuffer_);
        SetDebugName(meshConstantsBuffer_.Get(), "Mesh constants buffer");*/

        ByteBuffer buffer = CacheBuffer.getCachedByteBuffer(meshConstants.length * MeshConstants.SIZE);
        CacheBuffer.put(buffer, meshConstants);
        buffer.flip();

        meshConstantsBuffer_ = new BufferGL();
        meshConstantsBuffer_.initlize(GLenum.GL_UNIFORM_BUFFER, buffer.remaining(), buffer, GLenum.GL_DYNAMIC_DRAW);

        /*for (std::vector<std::unique_ptr<StaticMesh>>::iterator it = meshes_.begin(),
            end = meshes_.end();
        it != end; ++it)*/
        for(StaticMesh it : meshes_)
        {
            it.meshConstantsBuffer = meshConstantsBuffer_;
        }

        /*D3D11_SHADER_RESOURCE_VIEW_DESC srvDesc;
        srvDesc.Format = DXGI_FORMAT_UNKNOWN;
        srvDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
        srvDesc.Buffer.ElementOffset = 0;
        srvDesc.Buffer.ElementWidth = GetMeshCount();
        device->CreateShaderResourceView(
                meshConstantsBuffer_.Get(), &srvDesc, &meshConstantsBufferView_);

        SetDebugName(meshConstantsBufferView_.Get(), "Mesh constants buffer view");*/

        meshConstantsBufferView_ = meshConstantsBuffer_;
    }

    @Override
    public StaticMesh GetMesh(final int index) { return meshes_.get(index); }

    @Override
    public int GetMeshCount() { return meshes_.size();}

    @Override
    public BufferGL GetMeshConstantsBuffer()
    {
        return meshConstantsBufferView_;
    }
}

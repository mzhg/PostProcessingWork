package jet.opengl.demos.amdfx.geometry;

import org.lwjgl.util.vector.Vector;
import org.lwjgl.util.vector.Vector3f;

import java.nio.ByteBuffer;
import java.util.List;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.CacheBuffer;

/** Allocate everything from one large buffer */
final class MeshManagerGlobal extends MeshManagerBase{

    private BufferGL vertexBuffer_;
//    ComPtr<ID3D11ShaderResourceView> vertexBufferSRV_;
    private BufferGL indexBuffer_;
//    ComPtr<ID3D11ShaderResourceView> indexBufferSRV_;

    @Override
    public void Allocate(int meshCount, int[] verticesPerMesh, int[] indicesPerMesh) {
        int totalVertexCount = 0;
        int totalIndexCount = 0;

        for (int i = 0; i < meshCount; ++i)
        {
            totalVertexCount += verticesPerMesh[i];
            totalIndexCount += indicesPerMesh[i];
        }

        CreateVertexBuffer(/*device,*/ totalVertexCount);
        CreateIndexBuffer(/*device,*/ totalIndexCount);

        int indexOffset = 0;
        int vertexOffset = 0;
        for (int i = 0; i < meshCount; ++i)
        {
            StaticMesh mesh = new StaticMesh(verticesPerMesh[i], indicesPerMesh[i], i);
            meshes_.add(mesh);
            mesh.vertexBuffer = vertexBuffer_;
//            mesh.vertexBufferSRV = vertexBufferSRV_;
            mesh.indexBuffer = indexBuffer_;
//            mesh.indexBufferSRV = indexBufferSRV_;

            mesh.indexOffset = indexOffset;
            indexOffset += indicesPerMesh[i] * /*sizeof(int)*/4;

            mesh.vertexOffset = vertexOffset;
            vertexOffset += verticesPerMesh[i] * 3 * /*sizeof(float)*/4;
        }

        CreateMeshConstantsBuffer(/*device*/);
    }

    StaticMesh.Cluster[] CreateClusters (int indexCount,
        /*const void* vertexData,
        const void* indexData*/float[] vertices, int[] indices)
    {
//        const int32_t* indices = static_cast<const int32_t*> (indexData);
//        const float* vertices = static_cast<const float*> (vertexData);

        // 16 KiB stack space
        /*struct Triangle
        {
            DirectX::XMVECTOR vtx[3];
        };

        std::array<Triangle, SmallBatchMergeConstants::BATCH_SIZE * 3> triangleCache;*/

        class Triangle{
            final Vector3f vtx0 = new Vector3f();
            final Vector3f vtx1 = new Vector3f();
            final Vector3f vtx2 = new Vector3f();
        }

        Triangle[] triangleCache = new Triangle[SmallBatchMergeConstants.BATCH_SIZE * 3];

        final int triangleCount = indexCount / 3;
        final int clusterCount = (triangleCount + SmallBatchMergeConstants.BATCH_SIZE - 1)
            / SmallBatchMergeConstants.BATCH_SIZE;

        StaticMesh.Cluster[] result = new StaticMesh.Cluster[clusterCount];

        for (int i = 0; i < clusterCount; ++i)
        {
            final int clusterStart = i * SmallBatchMergeConstants.BATCH_SIZE;
            final int clusterEnd = Math.min (clusterStart + SmallBatchMergeConstants.BATCH_SIZE,
                triangleCount);

            final int clusterTriangleCount = clusterEnd - clusterStart;

            // Load all triangles into our local cache
            for (int triangleIndex = clusterStart; triangleIndex < clusterEnd; ++triangleIndex)
            {
                triangleCache[triangleIndex - clusterStart].vtx0 .set (
                    vertices[indices[triangleIndex * 3 + 0] * 3 + 0],
                    vertices[indices[triangleIndex * 3 + 0] * 3 + 1],
                    vertices[indices[triangleIndex * 3 + 0] * 3 + 2]
//                    1.0f
                );

                triangleCache[triangleIndex - clusterStart].vtx1.set (
                    vertices[indices[triangleIndex * 3 + 1] * 3 + 0],
                    vertices[indices[triangleIndex * 3 + 1] * 3 + 1],
                    vertices[indices[triangleIndex * 3 + 1] * 3 + 2]/*,
                    1.0f*/
                );

                triangleCache[triangleIndex - clusterStart].vtx2.set (
                    vertices[indices[triangleIndex * 3 + 2] * 3 + 0],
                    vertices[indices[triangleIndex * 3 + 2] * 3 + 1],
                    vertices[indices[triangleIndex * 3 + 2] * 3 + 2]/*,
                    1.0f*/
                );
            }

            final float max_value = Float.MAX_VALUE;
            Vector3f aabbMin = new Vector3f(max_value, max_value, max_value);
            Vector3f aabbMax = new Vector3f(-max_value, -max_value, -max_value);

            Vector3f coneAxis = new Vector3f();

            for (int triangleIndex = 0; triangleIndex < clusterTriangleCount; ++triangleIndex)
            {
                Triangle triangle = triangleCache[triangleIndex];

                aabbMin = Vector3f.min (aabbMin, triangle.vtx0, aabbMin);
                aabbMax = Vector3f.max (aabbMax, triangle.vtx0, aabbMax);

                aabbMin = Vector3f.min (aabbMin, triangle.vtx1, aabbMin);
                aabbMax = Vector3f.max (aabbMax, triangle.vtx1, aabbMax);

                aabbMin = Vector3f.min (aabbMin, triangle.vtx2, aabbMin);
                aabbMax = Vector3f.max (aabbMax, triangle.vtx2, aabbMax);

                /*const auto triangleNormal = DirectX::XMVector3Normalize (
                    DirectX::XMVector3Cross (
                    DirectX::XMVectorSubtract (triangle.vtx[1], triangle.vtx[0]),
                DirectX::XMVectorSubtract( triangle.vtx[2], triangle.vtx[0])));*/

                Vector3f vtx10 = Vector3f.sub(triangle.vtx1, triangle.vtx0, null);
                Vector3f vtx20 = Vector3f.sub(triangle.vtx2, triangle.vtx0, null);
                Vector3f triangleNormal = Vector3f.cross(vtx10, vtx20, vtx20);
                triangleNormal.normalise();

//                coneAxis = DirectX::XMVectorAdd (coneAxis, DirectX::XMVectorNegate (triangleNormal));
                triangleNormal.scale(-1);
                Vector3f.add(coneAxis, triangleNormal, coneAxis);
            }

            // This is the cosine of the cone opening angle - 1 means it's 0?
            // we're minimizing this value (at 0, it would mean the cone is 90?
            // open)
            float coneOpening = 1;
            boolean validCluster = true;

            /*const auto center = DirectX::XMVectorDivide (DirectX::XMVectorAdd (aabbMin, aabbMax),
            DirectX::XMVectorSet (2, 2, 2, 2));*/
            Vector3f center = Vector3f.mix(aabbMin, aabbMax, 0.5f, null);

//            coneAxis = DirectX::XMVector3Normalize (coneAxis);
            coneAxis.normalise();

            float t = -Float.MAX_VALUE; //-std::numeric_limits<float>::infinity ();

            // We nee a second pass to find the intersection of the line
            // center + t * coneAxis with the plane defined by each
            // triangle
            for (int triangleIndex = 0; triangleIndex < clusterTriangleCount; ++triangleIndex)
            {
                Triangle triangle = triangleCache[triangleIndex];
                // Compute the triangle plane from the three vertices

                /*const auto triangleNormal = DirectX::XMVector3Normalize (
                    DirectX::XMVector3Cross (
                    DirectX::XMVectorSubtract (triangle.vtx[1], triangle.vtx[0]),
                DirectX::XMVectorSubtract (triangle.vtx[2], triangle.vtx[0])));*/

                Vector3f vtx10 = Vector3f.sub(triangle.vtx1, triangle.vtx0, null);
                Vector3f vtx20 = Vector3f.sub(triangle.vtx2, triangle.vtx0, null);
                Vector3f triangleNormal = Vector3f.cross(vtx10, vtx20, vtx20);
                triangleNormal.normalise();

                final float directionalPart = /*DirectX::XMVectorGetX (
                    DirectX::XMVector3Dot (coneAxis, DirectX::XMVectorNegate (triangleNormal)));*/
                    -Vector3f.dot(coneAxis, triangleNormal);

                if (directionalPart < 0)
                {
                    // No solution for this cluster - at least two triangles
                    // are facing each other
                    validCluster = false;
                    break;
                }

                // We need to intersect the plane with our cone ray which is
                // center + t * coneAxis, and find the max
                // t along the cone ray (which points into the empty
                // space)
                // See: https://en.wikipedia.org/wiki/Line%E2%80%93plane_intersection
                /*const float td = DirectX::XMVectorGetX (DirectX::XMVectorDivide (
                    DirectX::XMVector3Dot (DirectX::XMVectorSubtract(center, triangle.vtx[0]), triangleNormal),
                DirectX::XMVectorSet (-directionalPart, -directionalPart, -directionalPart, -directionalPart)));*/
                Vector3f alongToCenter = Vector3f.sub(center, triangle.vtx0, vtx10);
                float dotValue = Vector3f.dot(alongToCenter, triangleNormal);
                final float td = dotValue/-directionalPart;

                t = Math.max (t, td);

                coneOpening = Math.min (coneOpening, directionalPart);
            }

            result[i] = new StaticMesh.Cluster();
            result[i].aabbMax.set(aabbMax);
            result[i].aabbMin.set(aabbMin);

            // cos (PI/2 - acos (coneOpening))
            result[i].coneAngleCosine = (float) Math.sqrt (1 - coneOpening * coneOpening);
            /*result[i].coneCenter = DirectX::XMVectorAdd (center,
                DirectX::XMVectorMultiply (coneAxis, DirectX::XMVectorSet (t, t, t, t)));*/
            Vector3f.linear(center, coneAxis,t, result[i].coneCenter);
            result[i].coneAxis.set(coneAxis);

//#if AMD_GEOMETRY_FX_ENABLE_CLUSTER_CENTER_SAFETY_CHECK
            // If distance of coneCenter to the bounding box center is more
            // than 16x the bounding box extent, the cluster is also invalid
            // This is mostly a safety measure - if triangles are nearly
            // parallel to coneAxis, t may become very large and unstable
            final float aabbSize = //DirectX::XMVectorGetX (DirectX::XMVector3Length (DirectX::XMVectorSubtract (aabbMax, aabbMin)));
                                    Vector3f.distance(aabbMax, aabbMin);
            final float coneCenterToCenterDistance = /*DirectX::XMVectorGetX (
                DirectX::XMVector3Length (
                DirectX::XMVectorSubtract (result[i].coneCenter,
                DirectX::XMVectorDivide (
                DirectX::XMVectorAdd (aabbMax, aabbMin),
            DirectX::XMVectorSet (2, 2, 2, 2))
            )));*/
                Vector3f.distance(result[i].coneCenter, center);

            if (coneCenterToCenterDistance > (16 * aabbSize))
            {
                validCluster = false;
            }
//#endif

            result[i].valid = validCluster;
        }

        return result;
    }

    @Override
    public void SetData(int meshIndex, float[] pVertexData, int[] pIndexData) {
        /*D3D11_BOX dstBox;
        dstBox.left = meshes_[meshIndex]->vertexOffset;
        dstBox.right = dstBox.left + meshes_[meshIndex]->vertexCount * 3 * sizeof(float);
        dstBox.top = 0;
        dstBox.bottom = 1;
        dstBox.front = 0;
        dstBox.back = 1;
        context->UpdateSubresource(vertexBuffer_.Get(), 0, &dstBox, vertexData, 0, 0);*/

        int offset = meshes_.get(meshIndex).vertexOffset;
        int size = meshes_.get(meshIndex).vertexCount * 3 * /*sizeof(float)*/4;
//        pVertexData.position(0).limit(size);  todo

        vertexBuffer_.update(offset, CacheBuffer.wrap(pVertexData));

        offset = meshes_.get(meshIndex).indexOffset;
        size = meshes_.get(meshIndex).indexCount * /*sizeof(int)*/4;
//        context->UpdateSubresource(indexBuffer_.Get(), 0, &dstBox, indexData, 0, 0);
        indexBuffer_.update(offset, CacheBuffer.wrap(pIndexData));

        meshes_.get(meshIndex).clusters = CreateClusters (meshes_.get(meshIndex).indexCount,
                pVertexData, pIndexData);
    }

    @Override
    public BufferGL GetIndexBuffer() {
        return indexBuffer_;
    }

    @Override
    public BufferGL GetVertexBuffer() {
        return vertexBuffer_;
    }

    @Override
    public BufferGL GetIndexBufferSRV() {
        return indexBuffer_;
    }

    @Override
    public BufferGL GetVertexBufferSRV() {
        return vertexBuffer_;
    }

    private void CreateVertexBuffer(/*ID3D11Device *device, const*/ int vertexCount) {
        /*D3D11_BUFFER_DESC vbDesc = {};
        vbDesc.Usage = D3D11_USAGE_DEFAULT;
        vbDesc.BindFlags = D3D11_BIND_VERTEX_BUFFER | D3D11_BIND_SHADER_RESOURCE;
        vbDesc.ByteWidth = sizeof(float) * 3 * vertexCount;
        vbDesc.StructureByteStride = 0;
        vbDesc.MiscFlags = D3D11_RESOURCE_MISC_BUFFER_ALLOW_RAW_VIEWS;
        device->CreateBuffer(&vbDesc, nullptr, &vertexBuffer_);
        SetDebugName(vertexBuffer_.Get(), "Global source vertex buffer");*/

        vertexBuffer_ = new BufferGL();
        vertexBuffer_.initlize(GLenum.GL_ARRAY_BUFFER, 4*3 * vertexCount, null, GLenum.GL_DYNAMIC_DRAW);

        /*D3D11_SHADER_RESOURCE_VIEW_DESC vbSrv;
        vbSrv.ViewDimension = D3D11_SRV_DIMENSION_BUFFEREX;
        vbSrv.BufferEx.FirstElement = 0;
        vbSrv.BufferEx.Flags = D3D11_BUFFEREX_SRV_FLAG_RAW;
        vbSrv.BufferEx.NumElements = vbDesc.ByteWidth / 4;
        vbSrv.Format = DXGI_FORMAT_R32_TYPELESS;

        device->CreateShaderResourceView(vertexBuffer_.Get(), &vbSrv, &vertexBufferSRV_);
        SetDebugName(vertexBufferSRV_.Get(), "Global source vertex buffer resource view");*/
    }

    private void CreateIndexBuffer(/*ID3D11Device *device, const*/ int indexCount) {
        /*D3D11_BUFFER_DESC ibDesc = {};
        ibDesc.Usage = D3D11_USAGE_DEFAULT;
        ibDesc.BindFlags = D3D11_BIND_INDEX_BUFFER | D3D11_BIND_SHADER_RESOURCE;
        ibDesc.ByteWidth = indexCount * sizeof(int);
        ibDesc.StructureByteStride = sizeof(int);

        device->CreateBuffer(&ibDesc, nullptr, &indexBuffer_);
        SetDebugName(indexBuffer_.Get(), "Global index buffer");

        D3D11_SHADER_RESOURCE_VIEW_DESC ibSrv;
        ibSrv.Format = DXGI_FORMAT_R32_UINT;
        ibSrv.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
        ibSrv.Buffer.ElementOffset = 0;
        ibSrv.Buffer.ElementWidth = sizeof(int);
        ibSrv.Buffer.FirstElement = 0;
        ibSrv.Buffer.NumElements = static_cast<UINT>(indexCount);

        device->CreateShaderResourceView(indexBuffer_.Get(), &ibSrv, &indexBufferSRV_);
        SetDebugName(indexBufferSRV_.Get(), "Global source index buffer view");*/

        indexBuffer_ = new BufferGL();
        indexBuffer_.initlize(GLenum.GL_ELEMENT_ARRAY_BUFFER, indexCount*4, null, GLenum.GL_DYNAMIC_DRAW);
    }
}

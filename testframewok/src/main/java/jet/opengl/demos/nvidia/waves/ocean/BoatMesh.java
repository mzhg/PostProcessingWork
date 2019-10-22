package jet.opengl.demos.nvidia.waves.ocean;

import com.nvidia.developer.opengl.models.sdkmesh.D3D9Enums;
import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshIndexBufferHeader;
import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshVertexBufferHeader;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;
import com.nvidia.developer.opengl.models.sdkmesh.VertexElement9;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;

final class BoatMesh extends SDKmesh implements D3D9Enums, OceanConst {

    // Hook to create input layout from mesh data
    ID3D11InputLayout CreateInputLayout(//	ID3D11Device* pd3dDevice,
                                        int iMesh, int iVB,
                                        Object pShaderBytecodeWithInputSignature, int BytecodeLength/*,
                                  ID3D11InputLayout** pIL*/
    ){
        // Translate from D3D9 decl...
        SDKMeshVertexBufferHeader pVBHeader = m_pVertexBufferArray[m_pMeshArray[ iMesh ].vertexBuffers[iVB]];

        D3D11_INPUT_ELEMENT_DESC[] vertex_layout = new D3D11_INPUT_ELEMENT_DESC[MAX_VERTEX_ELEMENTS];
        for(int num_layout_elements = 0; num_layout_elements < pVBHeader.decl.length; num_layout_elements ++) {

            VertexElement9 d3d9_decl_element = pVBHeader.decl[num_layout_elements];
            D3D11_INPUT_ELEMENT_DESC d3d11_layout_element = vertex_layout[num_layout_elements];

            // Translate usage
            switch(d3d9_decl_element.usage) {
                case D3DDECLUSAGE_POSITION: d3d11_layout_element.SemanticName = "POSITION"; break;
                case D3DDECLUSAGE_NORMAL: d3d11_layout_element.SemanticName = "NORMAL"; break;
                case D3DDECLUSAGE_TEXCOORD: d3d11_layout_element.SemanticName = "TEXCOORD"; break;
                case D3DDECLUSAGE_COLOR: d3d11_layout_element.SemanticName = "COLOR"; break;
                default:
                    return null;	// Whoops, this usage not handled yet!
            }

            // Translate usage index
            d3d11_layout_element.SemanticIndex = d3d9_decl_element.usageIndex;

            // Translate type
            switch(d3d9_decl_element.type) {
                case D3DDECLTYPE_FLOAT1: d3d11_layout_element.Format = DXGI_FORMAT_R32_FLOAT; break;
                case D3DDECLTYPE_FLOAT2: d3d11_layout_element.Format = DXGI_FORMAT_R32G32_FLOAT; break;
                case D3DDECLTYPE_FLOAT3: d3d11_layout_element.Format = DXGI_FORMAT_R32G32B32_FLOAT; break;
                case D3DDECLTYPE_FLOAT4: d3d11_layout_element.Format = DXGI_FORMAT_R32G32B32A32_FLOAT; break;
                case D3DDECLTYPE_D3DCOLOR: d3d11_layout_element.Format = DXGI_FORMAT_R8G8B8A8_UNORM; break;
                default:
                    return null;	// Whoops, this format not handled yet!
            }

            // Translate stream
            d3d11_layout_element.InputSlot = d3d9_decl_element.stream;

            // Translate offset
            d3d11_layout_element.AlignedByteOffset = d3d9_decl_element.offset;

            // No instancing
            d3d11_layout_element.InputSlotClass = /*D3D11_INPUT_PER_VERTEX_DATA*/0;
            d3d11_layout_element.InstanceDataStepRate = 0;

            ++num_layout_elements;
        }

        return ID3D11InputLayout.createInputLayoutFrom(vertex_layout);
//        return pd3dDevice->CreateInputLayout(vertex_layout, num_layout_elements, pShaderBytecodeWithInputSignature, BytecodeLength, pIL);
    }

    SDKMeshVertexBufferHeader getVBHeader(int i)  { return m_pVertexBufferArray[i]; }
    SDKMeshIndexBufferHeader getIBHeader(int i)  { return m_pIndexBufferArray[i]; }
}

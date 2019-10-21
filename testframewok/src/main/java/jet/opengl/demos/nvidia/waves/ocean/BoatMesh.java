package jet.opengl.demos.nvidia.waves.ocean;

import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshIndexBufferHeader;
import com.nvidia.developer.opengl.models.sdkmesh.SDKMeshVertexBufferHeader;
import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;

public class BoatMesh extends SDKmesh {

    // Hook to create input layout from mesh data
    /*HRESULT CreateInputLayout(	ID3D11Device* pd3dDevice,
                                  UINT iMesh, UINT iVB,
								const void *pShaderBytecodeWithInputSignature, SIZE_T BytecodeLength,
                                  ID3D11InputLayout** pIL
    );*/

    SDKMeshVertexBufferHeader getVBHeader(int i)  { return m_pVertexBufferArray[i]; }
    SDKMeshIndexBufferHeader getIBHeader(int i)  { return m_pIndexBufferArray[i]; }
}

package jet.opengl.demos.gpupro.rvi;

import jet.opengl.postprocessing.buffer.BufferGL;
import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.ShaderType;

final class DX11_STRUCTURED_BUFFER implements Disposeable {
    private int bindingPoint; // shader binding point
    private int elementCount; // number of structured elements in buffer
    private int elementSize; // size of 1 structured element in bytes
    private BufferGL structuredBuffer;
//    ID3D11UnorderedAccessView *unorderedAccessView;
//    ID3D11ShaderResourceView *shaderResourceView;

    private GLFuncProvider gl;

    DX11_STRUCTURED_BUFFER()
    {
        bindingPoint = 0;
        elementCount = 0;
        elementSize = 0;
        structuredBuffer = null;
//        unorderedAccessView = NULL;
//        shaderResourceView = NULL;
    }

    boolean Create(int bindingPoint,int elementCount,int elementSize){
        this.bindingPoint = bindingPoint;
        this.elementCount = elementCount;
        this.elementSize = elementSize;

        gl = GLFuncProviderFactory.getGLFuncProvider();

//        D3D11_BUFFER_DESC bufferDesc;
//        int stride = elementSize;
//        bufferDesc.ByteWidth = stride*elementCount;
//        bufferDesc.Usage = D3D11_USAGE_DEFAULT;
//        bufferDesc.BindFlags = D3D11_BIND_SHADER_RESOURCE | D3D11_BIND_UNORDERED_ACCESS;
//        bufferDesc.CPUAccessFlags = 0;
//        bufferDesc.MiscFlags = D3D11_RESOURCE_MISC_BUFFER_STRUCTURED;
//        bufferDesc.StructureByteStride = stride;
//        if(DEMO::renderer->GetDevice()->CreateBuffer(&bufferDesc,NULL,&structuredBuffer)!=S_OK)
//        return false;

        structuredBuffer = new BufferGL();
        structuredBuffer.initlize(GLenum.GL_UNIFORM_BUFFER, elementSize*elementCount, null, GLenum.GL_STREAM_COPY);

        /*D3D11_UNORDERED_ACCESS_VIEW_DESC uavDesc;
        uavDesc.Format = DXGI_FORMAT_UNKNOWN;
        uavDesc.ViewDimension = D3D11_UAV_DIMENSION_BUFFER;
        uavDesc.Buffer.FirstElement = 0;
        uavDesc.Buffer.Flags = 0;
        uavDesc.Buffer.NumElements = elementCount;
        if(DEMO::renderer->GetDevice()->CreateUnorderedAccessView(structuredBuffer,&uavDesc,&unorderedAccessView)!=S_OK)
        return false;

        D3D11_SHADER_RESOURCE_VIEW_DESC srvDesc;
        srvDesc.Format = DXGI_FORMAT_UNKNOWN;
        srvDesc.ViewDimension = D3D11_SRV_DIMENSION_BUFFER;
        srvDesc.Buffer.ElementOffset = 0;
        srvDesc.Buffer.ElementWidth = elementCount;
        if(DEMO::renderer->GetDevice()->CreateShaderResourceView(structuredBuffer,&srvDesc,&shaderResourceView)!=S_OK)
        return false;*/

        return true;
    }

    void Bind(ShaderType shaderType) {
        gl.glBindBufferBase(GLenum.GL_SHADER_STORAGE_BUFFER, bindingPoint, structuredBuffer.getBuffer());

        /*switch(shaderType)
        {
            case VERTEX:
                DEMO::renderer->GetDeviceContext()->VSSetShaderResources(bindingPoint,1,&shaderResourceView);
                break;

            case GEOMETRY_SHADER:
                DEMO::renderer->GetDeviceContext()->GSSetShaderResources(bindingPoint,1,&shaderResourceView);
                break;

            case FRAGMENT_SHADER:
                DEMO::renderer->GetDeviceContext()->PSSetShaderResources(bindingPoint,1,&shaderResourceView);
                break;

            case COMPUTE_SHADER:
                DEMO::renderer->GetDeviceContext()->CSSetShaderResources(bindingPoint,1,&shaderResourceView);
                break;
        }*/
    }

    BufferGL GetUnorderdAccessView() { return structuredBuffer;}

    int GetBindingPoint()
    {
        return bindingPoint;
    }

    int GetElementCount()
    {
        return elementCount;
    }

    int GetElementSize()
    {
        return elementCount;
    }

    @Override
    public void dispose() {
        SAFE_RELEASE(structuredBuffer);
    }
}

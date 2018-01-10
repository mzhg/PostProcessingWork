package jet.opengl.demos.intel.va;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.common.GLFuncProvider;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderProgram;

/**
 * Created by Administrator on 2017/11/19 0019.
 */
public final class VaDirectXVertexShader extends VaDirectXShader {
    ID3D11InputLayout m_inputLayout;
    final ArrayList<D3D11_INPUT_ELEMENT_DESC>
            m_inputLayoutElements = new ArrayList<D3D11_INPUT_ELEMENT_DESC>();

    void  SetInputLayout( D3D11_INPUT_ELEMENT_DESC...  elements/*, int elementCount*/ ){
        m_inputLayoutElements.clear();
        if(elements != null){
            for(int i = 0; i < elements.length; i++){
                m_inputLayoutElements.add(elements[i]);
            }
        }

        buildInputLayout();
    }

    private void buildInputLayout(){
        final int count = m_inputLayoutElements.size();
        final int[] types = new int[count];
        final int[] sizes = new int[count];
        final int[] offsets = new int[count];
        int strideInBytes = 0;

        for(int i = 0; i < count; i++){
            offsets[i] = strideInBytes;

            D3D11_INPUT_ELEMENT_DESC desc = m_inputLayoutElements.get(i);
            switch (desc.Format){
                case GLenum.GL_RGB32F:
                    types[i] = GLenum.GL_FLOAT;
                    sizes[i] = 3;
                    strideInBytes += 3 * 4;
                    break;
                case GLenum.GL_RGBA8:
                    types[i] = GLenum.GL_UNSIGNED_BYTE;
                    sizes[i] = 4;
                    strideInBytes += 4;
                    break;
                case GLenum.GL_RGBA32F:
                    types[i] = GLenum.GL_FLOAT;
                    sizes[i] = 4;
                    strideInBytes += 4 * 4;
                    break;
                case GLenum.GL_RG32F:
                    types[i] = GLenum.GL_FLOAT;
                    sizes[i] = 2;
                    strideInBytes += 2 * 4;
                    break;
                default:
                    throw new IllegalArgumentException("Unkown format: " + Integer.toHexString(desc.Format));
            }
        }

        int finalStrideInBytes = strideInBytes;
        m_inputLayout = new ID3D11InputLayout() {
            final GLFuncProvider gl = GLFuncProviderFactory.getGLFuncProvider();
            @Override
            public void bind() {
                for(int i = 0; i < count; i++){
                    if(types[i] == GLenum.GL_UNSIGNED_BYTE){
                        gl.glVertexAttribPointer(i, sizes[i], types[i], true, finalStrideInBytes, offsets[i]);
                    }else if (types[i] == GLenum.GL_FLOAT){
                        gl.glVertexAttribPointer(i, sizes[i], types[i], false, finalStrideInBytes, offsets[i]);
                    }else {
                        throw new IllegalArgumentException("Unsupport type: " + Integer.toHexString(types[i]));
                    }

                    gl.glEnableVertexAttribArray(i);
                }
            }

            @Override
            public void unbind() {
                for(int i = 0; i < count; i++){
                    gl.glDisableVertexAttribArray(i);
                }
            }
        };
    }

    public ID3D11InputLayout       GetInputLayout( ) { return m_inputLayout; }

    public ShaderProgram get() { return m_shader; }

    public void CreateShaderAndILFromFile(String filePath, String shaderModel, String entryPoint, Macro[] macros, D3D11_INPUT_ELEMENT_DESC[] elements/*, uint32 elementCount */){
        SetInputLayout( elements/*, elementCount*/ );
        CreateShaderFromFile( filePath, shaderModel, entryPoint, macros );
    }
    public void CreateShaderAndILFromBuffer( CharSequence shaderCode, String shaderModel, String entryPoint, Macro[] macros, D3D11_INPUT_ELEMENT_DESC[] elements/*, uint32 elementCount*/ ){
        SetInputLayout( elements/*, elementCount*/ );
        CreateShaderFromBuffer( shaderCode, shaderModel, entryPoint, macros );
    }

    public void CreateShaderAndILFromFile(String filePath, String shaderModel, String entryPoint, List<Macro> macros, D3D11_INPUT_ELEMENT_DESC[] elements/*, uint32 elementCount*/ ){
        SetInputLayout( elements/*, elementCount*/ );
        CreateShaderFromFile( filePath, shaderModel, entryPoint, macros );

    }
    public void CreateShaderAndILFromBuffer( String shaderCode, String shaderModel, String entryPoint, List<Macro> macros, D3D11_INPUT_ELEMENT_DESC[] elements/*, uint32 elementCount*/ ){
        SetInputLayout( elements/*, elementCount*/ );
        CreateShaderFromBuffer( shaderCode, shaderModel, entryPoint, macros );
    }

    @Override
    public void SetToD3DContext() {}


    @Override
    protected void CreateShader(){
        super.CreateShader();

        /*if( ( m_inputLayout == NULL ) && ( m_inputLayoutElements.size( ) > 0 ) && ( shaderBlob != NULL ) )  TODO
        {
            D3D11_INPUT_ELEMENT_DESC elements[64];
            if( m_inputLayoutElements.size( ) > _countof( elements ) )
            {
                assert( false );
                SAFE_RELEASE( shaderBlob );
                return;
            }
            for( uint32 i = 0; i < m_inputLayoutElements.size( ); i++ )
                elements[i] = m_inputLayoutElements[i];

            V( vaDirectXCore::GetDevice( )->CreateInputLayout( elements, (UINT)m_inputLayoutElements.size( ),
                shaderBlob->GetBufferPointer( ), shaderBlob->GetBufferSize( ), &m_inputLayout ) );

            vaDirectXCore::NameObject( m_inputLayout, "vaDirectXVertexShader input layout object" );
        }*/
    }
}

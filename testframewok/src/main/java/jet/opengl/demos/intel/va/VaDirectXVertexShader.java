package jet.opengl.demos.intel.va;

import java.util.ArrayList;
import java.util.List;

import jet.opengl.demos.intel.cput.D3D11_INPUT_ELEMENT_DESC;
import jet.opengl.demos.intel.cput.ID3D11InputLayout;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.shader.ShaderProgram;

/**
 * Created by Administrator on 2017/11/19 0019.
 */
public final class VaDirectXVertexShader extends VaDirectXShader {
    ID3D11InputLayout m_inputLayout;
    final ArrayList<D3D11_INPUT_ELEMENT_DESC>
            m_inputLayoutElements = new ArrayList<D3D11_INPUT_ELEMENT_DESC>();

    void                       SetInputLayout( D3D11_INPUT_ELEMENT_DESC...  elements/*, int elementCount*/ ){
        m_inputLayoutElements.clear();
        if(elements != null){
            for(int i = 0; i < elements.length; i++){
                m_inputLayoutElements.add(elements[i]);
            }
        }
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

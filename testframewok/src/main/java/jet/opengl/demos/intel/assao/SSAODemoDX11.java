package jet.opengl.demos.intel.assao;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Quaternion;
import org.lwjgl.util.vector.ReadableVector3f;
import org.lwjgl.util.vector.ReadableVector4f;
import org.lwjgl.util.vector.Vector3f;

import jet.opengl.demos.intel.va.VaDirectXConstantsBuffer;
import jet.opengl.demos.intel.va.VaDirectXCore;
import jet.opengl.demos.intel.va.VaDirectXPixelShader;
import jet.opengl.demos.intel.va.VaDirectXShaderManager;
import jet.opengl.demos.intel.va.VaDrawContext;
import jet.opengl.demos.intel.va.VaRenderDevice;
import jet.opengl.demos.intel.va.VaRenderDeviceDX11;
import jet.opengl.demos.intel.va.VaRenderingCore;
import jet.opengl.demos.intel.va.VaRenderingModuleRegistrar;
import jet.opengl.postprocessing.common.GLCheck;
import jet.opengl.postprocessing.shader.Macro;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2018/1/3.
 */

public final class SSAODemoDX11 extends ASSAODemo /*implements VaDirectXNotifyTarget*/{

    private VaDirectXConstantsBuffer/*< SSAODemoGlobalConstants >*/ m_constantsBuffer;

    private VaDirectXPixelShader m_overlayPS;
    private VaDirectXPixelShader m_opacityGraphPS;
//    std::vector< std::pair< std::string, std::string > >    m_staticShaderMacros;
    private boolean m_shadersDirty;

    private SSAODemoDX11(){
        // this is for the project-related shaders
//        VaDirectXShaderManager.GetInstance( ).RegisterShaderSearchPath( resourceRoot + L"Media/Shaders" );
//        vaRenderingCore::GetInstance( ).RegisterAssetSearchPath( vaCore::GetExecutableDirectory( ) + L"Media/Textures" );
//        vaRenderingCore::GetInstance( ).RegisterAssetSearchPath( vaCore::GetExecutableDirectory( ) + L"Media/Meshes" );
//        VaDirectXShaderManager.GetInstance( ).RegisterShaderSearchPath( vaCore::GetExecutableDirectory( ) + L"Media/Shaders" );
        
        m_shadersDirty = true;
    }

    public static SSAODemoDX11 newInstance(){
        ExternalSSAOWrapper.RegisterExternalSSAOWrapperDX11();
        VaRenderingModuleRegistrar.RegisterModule("ASSAOWrapper", (params->new ASSAOWrapper()));

        final String resourceRoot = "intel/va/";

        VaRenderingCore.GetInstance().RegisterAssetSearchPath(resourceRoot + "models");
        VaDirectXShaderManager.GetInstance( ).RegisterShaderSearchPath( resourceRoot + "shaders" );

        return new SSAODemoDX11();
    }

    @Override
    protected void initRendering() {
        OnStarted();
//        m_constantsBuffer.Create( );
        VaDirectXCore.GetInstance().PostDeviceCreated();

        VaRenderDevice renderDevice = new VaRenderDeviceDX11();

        Initialize(renderDevice);
        GLCheck.checkError();
        getGLContext().setSwapInterval(0);
    }

    @Override
    protected void update(float dt) {
        OnTick(dt);
    }

    @Override
    public void display() {
        OnRender();

        VaRenderingCore.ClosePrintLog();
    }

    @Override
    protected void reshape(int width, int height) {
        if(width <= 0 || height <=0)
            return;

        OnResized(width, height, true);
    }

    @Override
    public void onDestroy() {
//        VaDirectXCore.GetInstance().PostDeviceDestroyed();
        super.onDestroy();
    }

    private void UpdateShadersIfDirty(VaDrawContext drawContext ){
        Macro[] newStaticShaderMacros = null;

        if( ( drawContext.SimpleShadowMap != null ) && ( drawContext.SimpleShadowMap.GetVolumeShadowMapPlugin( ) != null ) ) {
            newStaticShaderMacros = drawContext.SimpleShadowMap.GetVolumeShadowMapPlugin().GetShaderMacros();
        }

        if( newStaticShaderMacros != m_staticShaderMacros )
        {
            m_staticShaderMacros = newStaticShaderMacros;
            m_shadersDirty = true;
        }

        if( m_shadersDirty )
        {
            m_shadersDirty = false;

//            std::vector<D3D11_INPUT_ELEMENT_DESC> inputElements = vaVertexInputLayoutsDX11::BillboardSpriteVertexDecl( );
//            m_overlayPS.CreateShaderFromFile( L"VSMGlobals.hlsl", "ps_5_0", "AVSMDebugOverlayPS", m_staticShaderMacros );
//            m_opacityGraphPS.CreateShaderFromFile( L"VSMGlobals.hlsl", "ps_5_0", "AVSMDebugOpacityGraphPS", m_staticShaderMacros );
        }
    }

    private void UpdateConstants( VaDrawContext drawContext ){

    }

    @Override
    protected void DrawDebugOverlay(VaDrawContext drawContext) {

    }

    public static void main(String[] args){
        test_decomposeRotationYawPitchRoll();

        test_decompose();
    }

    static void test_decomposeRotationYawPitchRoll(){
        final int N = 100;
        for(int i = 0; i<N;i++){
            float yaw = Numeric.random(-Numeric.PI/2, +Numeric.PI/2);
            float pitch = Numeric.random(-Numeric.PI/2, +Numeric.PI/2);
            float roll = Numeric.random(-Numeric.PI/2, +Numeric.PI/2);

            Matrix4f mat = Matrix4f.rotationYawPitchRoll(yaw, pitch, roll, null);
            Vector3f result = Matrix4f.decomposeRotationYawPitchRoll(mat, null);

            if(Numeric.isClose(result.x, yaw, 0.01f) == false|| Numeric.isClose(result.y, pitch, 0.01f) == false||
                    Numeric.isClose(result.z, roll, 0.01f) == false){
                System.out.println(String.format("%d: (%f, %f, %f)--(%f, %f, %f)",i, yaw, pitch, roll, result.x, result.y, result.z));
            }

        }
    }

    static void test_decompose(){
        final Vector3f translate = new Vector3f();
        final Vector3f scale = new Vector3f();
        final Quaternion rot = new Quaternion();

        final Vector3f translate1 = new Vector3f();
        final Vector3f scale1 = new Vector3f();
        final Quaternion rot1 = new Quaternion();

        int errorCount = 0;
        final int N = 100;
        for(int i = 0; i<N;i++){
            translate.x = Numeric.random(-1000, +1000);
            translate.y = Numeric.random(-1000, +1000);
            translate.z = Numeric.random(-1000, +1000);

            scale.x = Numeric.random(0.01f, 100);
            scale.y = Numeric.random(0.01f, 100);
            scale.z = Numeric.random(0.01f, 100);

            rot.x = Numeric.random(-100, 100);
            rot.y = Numeric.random(-100, 100);
            rot.z = Numeric.random(-100, 100);
            rot.w = Numeric.random(-100, 100);
            rot.normalise();

            Matrix4f combined = new Matrix4f();
            combined.setTranslate(translate);
            rot.toMatrix(combined);
            combined.scale(scale);

            Matrix4f.decompose(combined, scale1, rot1, translate1);
            if(!isClose(translate, translate1) ||!isClose(scale, scale1) || !isClose(rot, rot1)){
                System.out.println(String.format("%d: (%s)--(%s)",i, mkStr(translate, scale, rot), mkStr(translate1, scale1, rot1)));
                errorCount++;
            }
        }

        if(errorCount == 0){
            System.out.println("Compare done!");
        }

        System.out.println();

        rot.setIdentity();
        Matrix4f rotMat = new Matrix4f();
        rot.toMatrix(rotMat);
        rot1.setFromMatrix(rotMat);

        if(!isClose(rot, rot1)){
            System.out.println(String.format("(%f, %f, %f, %f)--(%f, %f, %f, %f)", rot.x,rot.y, rot.z, rot.w,rot1.x,rot1.y, rot1.z, rot1.w));
        }
    }

    static String mkStr(Vector3f translate, Vector3f scale, Quaternion rot){
        StringBuilder sb = new StringBuilder(64);
        sb.append(translate.x).append(",").append(translate.y).append(",").append(translate.z).append(",");
        sb.append(scale.x).append(",").append(scale.y).append(",").append(scale.z).append(",");
        sb.append(rot.x).append(",").append(rot.y).append(",").append(rot.z).append(",").append(rot.w).append(',');

        return sb.toString();
    }


    static boolean isClose(ReadableVector3f a, ReadableVector3f b){
        return Numeric.isClose(a.getX(), b.getX(), 0.1f)&&
                Numeric.isClose(a.getY(), b.getY(), 0.1f)&&
                Numeric.isClose(a.getZ(), b.getZ(), 0.1f);
    }

    static boolean isClose(ReadableVector4f a, ReadableVector4f b){
        return Numeric.isClose(a.getX(), b.getX(), 0.01f)&&
                Numeric.isClose(a.getY(), b.getY(), 0.01f)&&
                Numeric.isClose(a.getW(), b.getW(), 0.01f)&&
                Numeric.isClose(a.getZ(), b.getZ(), 0.01f);
    }

    static boolean isClose(Quaternion a, Quaternion b){
        if(Math.signum(a.w) != Math.signum(b.w)){
            return Numeric.isClose(a.getX(), -b.getX(), 0.01f)&&
                    Numeric.isClose(a.getY(), -b.getY(), 0.01f)&&
                    Numeric.isClose(a.getW(), -b.getW(), 0.01f)&&
                    Numeric.isClose(a.getZ(), -b.getZ(), 0.01f);
        }else{
            return Numeric.isClose(a.getX(), b.getX(), 0.01f)&&
                    Numeric.isClose(a.getY(), b.getY(), 0.01f)&&
                    Numeric.isClose(a.getW(), b.getW(), 0.01f)&&
                    Numeric.isClose(a.getZ(), b.getZ(), 0.01f);
        }
    }
}

package jet.opengl.demos.intel.va;

import java.util.ArrayList;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by Administrator on 2017/11/19 0019.
 */

public final class VaDirectXCore implements Disposeable{

    private int m_SwapChainWidth;
    private int m_SwapChainHeight;

    private static VaDirectXCore g_Instance;

    /*ID3D11Device*                               m_pd3dDevice;
    IDXGISwapChain*                             m_pSwapChain;
    DXGI_SURFACE_DESC 			                m_backBufferSurfaceDesc;
    ID3D11DeviceContext*                        m_immediateContext;*/
    //
//    typedef std::vector<vaDirectXNotifyTarget*> NotifyTargetContainerType;
    private final ArrayList<VaDirectXNotifyTarget> m_notifyTargets = new ArrayList<>();
    private int m_notifyTargetsCurrentMinZeroedIndex;
    private int m_notifyTargetsNumberOfZeroed;
    //
    private boolean m_traversingNotifyTargets;
    //
    private VaDirectXShaderManager m_shaderManager;
    //
    private String m_adapterNameShort;
    private String m_adapterNameID;
    private boolean m_deviceCreated;

    public static VaDirectXCore GetInstance(){
        if(g_Instance == null){
            g_Instance = new VaDirectXCore();
        }

        return g_Instance;
    }

    public static void helperInitlize(VaDirectXNotifyTarget target){
        if(target == null) {
            LogUtil.e(LogUtil.LogType.DEFAULT, "Attemp to notify a null object.");
            return;
        }

        VaDirectXCore instance = GetInstance();
        if(instance != null && instance.m_deviceCreated){
            target.OnDeviceCreated();

            if(instance.m_SwapChainWidth > 0 && instance.m_SwapChainHeight > 0) {
                target.OnResizedSwapChain(instance.m_SwapChainWidth, instance.m_SwapChainHeight);
            }
        }

        instance.RegisterNotifyTargetInternal(target);
    }

    private VaDirectXCore( ){
        /*m_pd3dDevice = NULL;
        m_pSwapChain = NULL;
        m_immediateContext = NULL;
        memset( &m_backBufferSurfaceDesc, 0, sizeof( m_backBufferSurfaceDesc ) );*/

        m_traversingNotifyTargets = false;
        m_notifyTargetsCurrentMinZeroedIndex = Integer.MAX_VALUE;
        m_notifyTargetsNumberOfZeroed = 0;

        m_shaderManager = new VaDirectXShaderManager( );
    }

    @Override
    public void dispose() {
        if( VaAssetPackManager.GetInstance() != null )
            VaAssetPackManager.GetInstance().OnRenderingAPIAboutToShutdown();

        VaAssetPackManager.g_Instance = null;
        PostDeviceDestroyed( );

        /*assert( m_pd3dDevice == NULL );
        assert( m_pSwapChain == NULL );*/
        assert( m_notifyTargets.size( ) == 0 );
        assert( !m_traversingNotifyTargets );

        g_Instance = null;
    }

    //
    private void EarlySetDevice( /*const DXGI_ADAPTER_DESC1 & adapterDesc, ID3D11Device* device*/ ){
        /*assert( m_pd3dDevice == NULL );  TODO

        wstring name = wstring( adapterDesc.Description );
        std::replace( name.begin(), name.end(), L' ', L'_' );

        m_adapterNameShort  = name;
        m_adapterNameID     = vaStringTools::Format( L"%s-%08X_%08X", name.c_str(), adapterDesc.DeviceId, adapterDesc.Revision );

        m_pd3dDevice = device;
        m_pd3dDevice->AddRef( );*/
    }
    //
    public void PostDeviceCreated( /*ID3D11Device* device, IDXGISwapChain* swapChain*/ ){
        /*if( device == NULL )
        {
            m_pd3dDevice = device;
            m_pd3dDevice->AddRef( );
        }
        else
        {
            assert( m_pd3dDevice == device );
        }
        assert( m_pSwapChain == NULL );
        m_pSwapChain = swapChain;
        m_pSwapChain->AddRef( );
        device->GetImmediateContext( &m_immediateContext );*/

        m_deviceCreated = true;
        VaDirectXTools.vaDirectXTools_OnDeviceCreated();

        assert( !m_traversingNotifyTargets );
        m_traversingNotifyTargets = true;

        // ensure the vector is defragmented and all NULL ptrs are removed
        TickInternal( );

        /*for( NotifyTargetContainerType::const_iterator it = m_notifyTargets.begin( ); it != m_notifyTargets.end( ); it++ )
        ( *it )->OnDeviceCreated( device, swapChain );*/
        for(VaDirectXNotifyTarget notifyTarget : m_notifyTargets){
            notifyTarget.OnDeviceCreated();
        }

        assert( m_traversingNotifyTargets );
        m_traversingNotifyTargets = false;
    }

    public void PostDeviceDestroyed( ){
        /*SAFE_RELEASE( m_immediateContext );
        SAFE_RELEASE( m_pd3dDevice );
        SAFE_RELEASE( m_pSwapChain );*/

        m_deviceCreated = false;
        assert( !m_traversingNotifyTargets );
        m_traversingNotifyTargets = true;

        // ensure the vector is defragmented and all NULL ptrs are removed
        TickInternal( );

        for(VaDirectXNotifyTarget notifyTarget : m_notifyTargets){
            notifyTarget.OnDeviceDestroyed();
        }

        assert( m_traversingNotifyTargets );
        m_traversingNotifyTargets = false;

//        VaDirectXTools.vaDirectXTools_OnDeviceDestroyed( );  TODO
    }

    public void PostReleasingSwapChain( ){
        assert( !m_traversingNotifyTargets );
        m_traversingNotifyTargets = true;

        // ensure the vector is defragmented and all NULL ptrs are removed
        TickInternal( );

        for(VaDirectXNotifyTarget notifyTarget : m_notifyTargets){
            notifyTarget.OnReleasingSwapChain();
        }

        assert( m_traversingNotifyTargets );
        m_traversingNotifyTargets = false;
    }

    public void PostResizedSwapChain( /*const DXGI_SURFACE_DESC & backBufferSurfaceDesc*/int swapChainWidth, int swapChainHeight ){
//        m_backBufferSurfaceDesc = backBufferSurfaceDesc;

        m_SwapChainWidth = swapChainWidth;
        m_SwapChainHeight = swapChainHeight;
        assert( !m_traversingNotifyTargets );
        m_traversingNotifyTargets = true;

        // ensure the vector is defragmented and all NULL ptrs are removed
        TickInternal( );

        for(VaDirectXNotifyTarget notifyTarget : m_notifyTargets){
            notifyTarget.OnResizedSwapChain(swapChainWidth, swapChainHeight);
        }

        assert( m_traversingNotifyTargets );
        m_traversingNotifyTargets = false;
    }
    //
    private void TickInternal( ){
        // defragment and remove NULL pointers from the vector, in a quick way
        if( m_notifyTargetsNumberOfZeroed > 0 )
        {
            int lastNonZero = ( m_notifyTargets.size( ) ) - 1;
            while( ( lastNonZero >= 0 ) && ( m_notifyTargets.get(lastNonZero) == null ) ) lastNonZero--;
            for( int i = m_notifyTargetsCurrentMinZeroedIndex; ( i < lastNonZero ) && ( m_notifyTargetsNumberOfZeroed>0 ); i++ )
            {
                VaDirectXNotifyTarget notifyTarget = m_notifyTargets.get(i);
                if( notifyTarget == null )
                {
                    // swap with the last
                    /*m_notifyTargets[i] = m_notifyTargets[lastNonZero];
                    m_notifyTargets[lastNonZero] = NULL;*/

                    m_notifyTargets.set(i, m_notifyTargets.get(lastNonZero));
                    m_notifyTargets.set(lastNonZero, null);

                    // have to update the index now!
                    m_notifyTargets.get(i).setStorageIndex(i);
                    // optimization
                    m_notifyTargetsNumberOfZeroed--;
                    while( ( lastNonZero >= 0 ) && ( m_notifyTargets.get(lastNonZero) == null ) ) lastNonZero--;
                    if( lastNonZero < 0 )
                        break;
                }
            }
            while( ( lastNonZero >= 0 ) && ( m_notifyTargets.get(lastNonZero) == null ) ) lastNonZero--;
            if( lastNonZero < 0 )
            {
                m_notifyTargets.clear( );
            }
            else
            {
//                m_notifyTargets.resize( lastNonZero + 1 );
                int oldSize = m_notifyTargets.size();
                if(oldSize < lastNonZero + 1){
                    for(int k = oldSize; k < lastNonZero + 1; k++){
                        m_notifyTargets.add(null);
                    }
                }else if(oldSize > lastNonZero + 1){
                    for(int k = oldSize; k > lastNonZero + 1; k--){
                        m_notifyTargets.remove(m_notifyTargets.size() - 1);
                    }
                }
            }
            m_notifyTargetsCurrentMinZeroedIndex = Integer.MAX_VALUE;
            m_notifyTargetsNumberOfZeroed = 0;
        }
    }

    /*static ID3D11Device *               GetDevice( )                   { return GetInstance( ).m_pd3dDevice; }
    static ID3D11DeviceContext *        GetImmediateContext( )         { return GetInstance( ).m_immediateContext; }
    static IDXGISwapChain *             GetSwapChain( )                { return GetInstance( ).m_pSwapChain; }
    static const DXGI_SURFACE_DESC &    GetBackBufferSurfaceDesc( )    { return GetInstance( ).m_backBufferSurfaceDesc; }
    static void                         NameObject( ID3D11DeviceChild * object, const char * permanentNameString );
    static void                         NameObject( ID3D11Resource * resourceobject, const char * permanentNameString );*/
    //
    public static void  ProcessInformationQueue( ){

    }
    //
    // generic GPU-specific name - used to serialize adapter-specific stuff like settings or shader cache
    public String GetAdapterNameShort( )     { return m_adapterNameShort; }
    public String GetAdapterNameID( )        { return m_adapterNameID; }

    public void RegisterNotifyTargetInternal( VaDirectXNotifyTarget rh ){
        if( m_traversingNotifyTargets )
            LogUtil.e(LogUtil.LogType.DEFAULT, "Registering new vaDirectXNotifyTarget from the notification callback is currently not supported.");

        m_notifyTargets.add( rh );

        rh.setStorageIndex((m_notifyTargets.size( ) ) - 1);
    }

    public void UnregisterNotifyTargetInternal( VaDirectXNotifyTarget rh ){
        if( m_traversingNotifyTargets )
            LogUtil.e(LogUtil.LogType.DEFAULT, "Registering new vaDirectXNotifyTarget from the notification callback is currently not supported.");

        assert( rh.getStorageIndex() >= 0 );
        assert( m_notifyTargets.get(rh.getStorageIndex()) == rh );
//        m_notifyTargets[rh->m_storageIndex] = NULL;
        m_notifyTargets.set(rh.getStorageIndex(), null);

        m_notifyTargetsCurrentMinZeroedIndex = Math.min( m_notifyTargetsCurrentMinZeroedIndex, rh.getStorageIndex() );
        m_notifyTargetsNumberOfZeroed++;
//        rh->m_storageIndex = 0;
        rh.setStorageIndex(0);
    }

    private static void RegisterNotifyTarget( VaDirectXNotifyTarget rh ){
        GetInstance( ).RegisterNotifyTargetInternal( rh );
    }

    private static void UnregisterNotifyTarget( VaDirectXNotifyTarget rh ){
        GetInstance().UnregisterNotifyTargetInternal(rh);
    }
}

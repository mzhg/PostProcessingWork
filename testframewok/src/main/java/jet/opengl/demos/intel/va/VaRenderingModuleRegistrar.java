package jet.opengl.demos.intel.va;

import java.util.HashMap;
import java.util.Map;

import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/11/17.
 */

public class VaRenderingModuleRegistrar {

    final Map< String, ModuleCreateFunction > m_modules = new HashMap<>();

    private VaRenderingModuleRegistrar(){}

    private static VaRenderingModuleRegistrar g_Instance;

    public static void                             RegisterModule( String name, ModuleCreateFunction moduleCreateFunction ){
        // make sure the singleton is alive
        CreateSingletonIfNotCreated();

        assert( !name.isEmpty() );

//        auto it = g_Instance.m_modules.find( name );
        if( g_Instance.m_modules.containsKey(name) )
        {
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("vaRenderingCore::RegisterModule - name '%s' already registered!", name));
            return;
        }
//        GetInstance().m_modules.insert( std::pair< std::string, ModuleInfo >( name, ModuleInfo( moduleCreateFunction ) ) );
        g_Instance.m_modules.put(name, moduleCreateFunction);
    }

    public static VaRenderingModule              CreateModule( String name, VaConstructorParamsBase  params ){
        ModuleCreateFunction function = g_Instance.m_modules.get(name);
        if( function == null )
        {
            /*wstring wname = vaStringTools::SimpleWiden( name );
            VA_ERROR( L"vaRenderingCore::CreateModule - name '%s' not registered.", wname.c_str( ) );*/
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("vaRenderingCore::CreateModule - name '%s' not registered.", name));
            return null;
        }

        VaRenderingModule  ret = function.call( params );

        ret.InternalRenderingModuleSetTypeName( name );

        /*try {
            Method method = ret.getClass().getMethod("InternalRenderingModuleSetTypeName", String.class);
            method.invoke(ret, name);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }*/
        return ret;
    }

    //void                                  CreateModuleArray( int inCount, vaRenderingModule * outArray[] );

    public static<T extends VaRenderingModule > T  CreateModuleTyped( String name, VaConstructorParamsBase params ){
        T ret = null;
        VaRenderingModule createdModule = CreateModule( name, params );
        try {
            ret = (T)createdModule;
        } catch (Exception e) {
            /*e.printStackTrace();*/
        }

        if( ret == null )
        {
//            wstring wname = vaStringTools::SimpleWiden( name );
//            VA_ERROR( L"vaRenderingModuleRegistrar::CreateModuleTyped failed for '%s'; have you done VA_RENDERING_MODULE_REGISTER( vaSomeClass, vaSomeClassDX11 )?; is vaSomeClass inheriting vaRenderingModule with 'public'? ", wname.c_str() );
            LogUtil.e(LogUtil.LogType.DEFAULT, String.format("vaRenderingModuleRegistrar::CreateModuleTyped failed for '%s'; have you done VA_RENDERING_MODULE_REGISTER( vaSomeClass, vaSomeClassDX11 )?; is vaSomeClass inheriting vaRenderingModule with 'public'? ", name));
        }

        return ret;
    }

    public interface ModuleCreateFunction{
        VaRenderingModule  call( VaConstructorParamsBase params );
    }

    static void                             CreateSingletonIfNotCreated( ){
        if(g_Instance == null)
            g_Instance = new VaRenderingModuleRegistrar();
    }

    static void                             DeleteSingleton( ){
        g_Instance = null;
    }
}

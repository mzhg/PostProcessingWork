package jet.opengl.demos.nvidia.waves.crest.loddata;

import java.lang.reflect.Type;
import java.util.List;

/** Registers input to a particular LOD data. */
public abstract class RegisterLodDataInput<LodDataType extends LodDataMgr> extends RegisterLodDataInputBase {

    boolean _disableRenderer = true;

    protected void OnEnable()
    {
        if (_disableRenderer)
        {
            var rend = GetComponent<Renderer>();
            if (rend)
            {
                rend.enabled = false;
            }
        }

        Type type = getClass().getGenericInterfaces()[0];
        List<ILodDataInput> registrar = GetRegistrar(type.getClass());
        registrar.add(this);
    }

    protected void OnDisable()
    {
        Type type = getClass().getGenericInterfaces()[0];
        List<ILodDataInput> registrar = GetRegistrar(type.getClass());
        if (registrar != null)
        {
            registrar.remove(this);
        }
    }
}

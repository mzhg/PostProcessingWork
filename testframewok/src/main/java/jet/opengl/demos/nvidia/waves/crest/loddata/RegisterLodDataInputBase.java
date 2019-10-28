package jet.opengl.demos.nvidia.waves.crest.loddata;

import com.nvidia.developer.opengl.models.obj.Material;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jet.opengl.demos.nvidia.waves.crest.CommandBuffer;
import jet.opengl.demos.nvidia.waves.crest.MonoBehaviour;

/** Base class for scripts that register input to the various LOD data types. */
public abstract class RegisterLodDataInputBase extends MonoBehaviour implements ILodDataInput {
    public abstract float Wavelength();

    @Override
    public boolean Enabled() { return true; }

    public static int sp_Weight = 0; //Shader.PropertyToID("_Weight");

    static Map<Class<?>, ArrayList<ILodDataInput>> _registrar = new HashMap<>();

    public static List<ILodDataInput> GetRegistrar(Class<?> lodDataMgrType)
    {
        ArrayList<ILodDataInput> registered = _registrar.get(lodDataMgrType);
        if (/*!_registrar.TryGetValue(lodDataMgrType, out registered)*/ registered == null)
        {
            registered = new ArrayList<>();
            _registrar.put(lodDataMgrType, registered);
        }
        return registered;
    }

//    Renderer _renderer;
    Material[] _materials = new Material[2];

    protected void Start()
    {
       /* _renderer = GetComponent<Renderer>();

        if (_renderer)
        {
            _materials[0] = _renderer.sharedMaterial;
            _materials[1] = new Material(_renderer.sharedMaterial);
        }*/

       throw new UnsupportedOperationException();
    }

    public void Draw(CommandBuffer buf, float weight, int isTransition)
    {
        if ( weight > 0f)
        {
//            _materials[isTransition].SetFloat(sp_Weight, weight);  todo

            buf.DrawRenderer( _materials[isTransition]);
        }
    }

//    public int MaterialCount => _materials.Length;
    public Material GetMaterial(int index) {
        return _materials[index];
    }

    public int MaterialCount(){
        return _materials.length;
    }
}

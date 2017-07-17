package jet.opengl.demos.amdfx.common;

import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.util.StackInt;

public final class AMD_Mesh {

	private final List<MaterialGroup> _material_group = new ArrayList<>();
	private String                    _name = "default";
    private int                       _id;
    private SDKmesh m_sdkMesh;
    private boolean                   m_isSdkMesh;
    
    private final List<Vertex>        _vertex = new ArrayList<>();
    private final StackInt _index  = new StackInt();
    private final List<Texture2D>     _srv = new ArrayList<>();

    public void Create(String path, String name, boolean sdkmesh){
    	m_isSdkMesh = sdkmesh;

        if (sdkmesh)
        {
//            char filename[256];
//            sprintf(filename, "%s%s", path, name);
//            std::string fname(filename);
//            std::wstring wfname(fname.begin(), fname.end());

        	String fname = path + File.separator + name;
            try {
                m_sdkMesh = new SDKmesh();
				m_sdkMesh.create(fname, false, null);
                m_sdkMesh.printMeshInformation("");
			} catch (IOException e) {
				e.printStackTrace();
			}
        }
        else
        {
            // the minimal-dependencies version of AMD_SDK
            // only supports sdkmesh
            throw new IllegalArgumentException();
        }
    }

    public void Render(){
    	if (m_isSdkMesh)
        {
            m_sdkMesh.render(0, -1, -1);
        }
        else
        {
            // the minimal-dependencies version of AMD_SDK
            // only supports sdkmesh
        	throw new IllegalArgumentException();
        }
    }

    public void Release(){
    	if(m_isSdkMesh){

    	}
    }

    Texture2D[] srv(){ return null;}
	
	private static final class MaterialGroup{
		int _first_index;
        int _index_count;
        int _texture_index;
	}
	
	private static final class Vertex
    {
        final float[] position = new float[3];
        final float[] normal   = new float[3];
        final float[] uv       = new float[2];
    };
}

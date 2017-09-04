package jet.opengl.demos.amdfx.common;

import com.nvidia.developer.opengl.models.sdkmesh.SDKmesh;

import java.io.File;
import java.io.IOException;

public final class AMD_Mesh {

    private SDKmesh m_sdkMesh;
    private boolean                   m_isSdkMesh;

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
}

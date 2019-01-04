package jet.opengl.demos.gpupro.rvi;

import com.nvidia.developer.opengl.models.obj.NvModelExt;
import com.nvidia.developer.opengl.models.obj.NvModelExtGL;
import com.nvidia.developer.opengl.models.obj.NvModelFileLoader;

import java.io.IOException;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.util.DebugTools;
import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;

public class SponzaMesh implements NvModelFileLoader, Disposeable {

    private NvModelExtGL m_modelsExt;

    public SponzaMesh(){
        FileLoader old = FileUtils.g_IntenalFileLoader;
        FileUtils.setIntenalFileLoader(FileLoader.g_DefaultFileLoader);
        m_modelsExt = loadModelExt("sponza.obj");
        FileUtils.setIntenalFileLoader(old);
    }

    @Override
    public byte[] loadDataFromFile(String fileName) throws IOException {
        final String file = "E:\\SDK\\VCTRenderer\\engine\\assets\\models\\crytek-sponza\\";

        NvModelExtGL.setTexturePath(file);
        return DebugTools.loadBytes(file + fileName);
    }

    NvModelExtGL loadModelExt(String model_filename)
    {
        NvModelExt.SetFileLoader(this);
        NvModelExt pModel = null;
        try {
            pModel = NvModelExt.CreateFromObj(model_filename, 40.0f, true, true, 0.01f, 0.001f, 3000);
        } catch (IOException e) {
            e.printStackTrace();
        }

        NvModelExtGL pGLModelExt = NvModelExtGL.Create(pModel);

        return pGLModelExt;
    }

    @Override
    public void dispose() {
        if(m_modelsExt != null){
        }
    }
}

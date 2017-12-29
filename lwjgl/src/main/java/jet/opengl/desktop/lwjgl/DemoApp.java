package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.NvAppBase;
import com.nvidia.developer.opengl.app.NvEGLConfiguration;
import com.nvidia.developer.opengl.utils.NvGfxAPIVersion;
import com.nvidia.developer.opengl.utils.NvImage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/4/12.
 */

public class DemoApp {

    static {
        System.setProperty("jet.opengl.postprocessing.debug", "true");
    }

    public static void run(NvAppBase app){
        NvEGLConfiguration config = new NvEGLConfiguration();
        app.configurationCallback(config);

        LwjglApp baseApp = new LwjglApp();
        GLContextConfig glconfig = baseApp.getGLContextConfig();
        glconfig.alphaBits = config.alphaBits;
        glconfig.depthBits = config.depthBits;
        glconfig.stencilBits = config.stencilBits;

        glconfig.redBits = config.redBits;
        glconfig.greenBits = config.greenBits;
        glconfig.blueBits = config.blueBits;
        glconfig.debugContext = config.debugContext;
        glconfig.multiSamplers = config.multiSamplers;
        baseApp.registerGLEventListener(app);
        baseApp.registerGLFWListener(new InputAdapter(app, app, app));
        app.setGLContext(baseApp);
        baseApp.start();
    }

    public static void main(String[] args) {
//        LogUtil.setLoggerLevel(LogUtil.LogType.NV_FRAMEWROK, Level.OFF);
//        LogUtil.setLoggerLevel(LogUtil.LogType.DEFAULT, Level.OFF);

        final String path = "app\\src\\main\\assets\\";
        FileUtils.setIntenalFileLoader(new FileLoader() {
            @Override
            public InputStream open(String file) throws FileNotFoundException {
                if(file.contains(path)){  // Not safe
                    return new FileInputStream(file);
                }
                return new FileInputStream(path + file);
            }

            @Override
            public String getCanonicalPath(String file) throws IOException {
                if(file.contains(path)){
                    return new File(file).getCanonicalPath();
                }
                return new File(path + file).getCanonicalPath();
            }

            @Override
            public boolean exists(String file) {
                if(file.contains(path)){
                    return new File(file).exists();
                }

                return new File(path + file).exists();
            }

            @Override
            public String resolvePath(String file) {
                if(file.contains(path)){
                    return file;
                }
                return path + file;
            }
        });

        NvImage.setAPIVersion(NvGfxAPIVersion.GL4_4);
//        run(new HBAOPlusDemo());
//        run(new ASSAODemoDebug());
        run(new ShaderTest());
//        testRectVertex();
    }

    private static void testRectVertex(){
        int mWidth = 1,
            mHeight = 1,
            mSegsH = 1,
            mSegsW = 1;
        int i, j;
        for(i = 0; i <= mSegsH; i++){
            for(j = 0; j <= mSegsW; j++){

                //Vertices
                float v1 = ((float)j/mSegsW - 0.5f)*mWidth;
                float v2 = ((float)i/mSegsH - 0.5f)*mHeight;

                System.out.printf("Vertex: %f, %f.\n", v1, v2);

                //TextureCoords
                float u = (float) j / (float) mSegsW;
                float v = (float) i / (float) mSegsH;

                System.out.printf("Texcoord: %f, %f.\n", u, v);
            }
        }
    }


}

package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.NvAppBase;
import com.nvidia.developer.opengl.app.NvEGLConfiguration;
import com.nvidia.developer.opengl.tests.ComputeBasicGLSL;

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
        final String path = "app/src/main/assets/";
        FileUtils.setIntenalFileLoader(new FileLoader() {
            @Override
            public InputStream open(String file) throws FileNotFoundException {
                return new FileInputStream(path + file);
            }

            @Override
            public String getParent(String file) {
                return new File(path + file).getParent();
            }

            @Override
            public String getCanonicalPath(String file) throws IOException {
                return new File(path + file).getCanonicalPath();
            }
        });

//        System.out.println(new File("").getAbsolutePath());
        run(new ComputeBasicGLSL());

//        System.out.println("1280/720 = " + 720f/1280f);
//        System.out.println("800/600 = " + 600f/800f);
    }
}

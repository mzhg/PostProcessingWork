package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.NvAppBase;
import com.nvidia.developer.opengl.app.NvEGLConfiguration;
import com.nvidia.developer.opengl.utils.NvGfxAPIVersion;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.opengl.EXTTextureCompressionLATC;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengles.NVTextureCompressionS3TC;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jet.opengl.demos.labs.scattering.AtmosphereTest;
import jet.opengl.demos.labs.scattering.Chapman;
import jet.opengl.demos.nvidia.face.sample.FaceWorkDemo;
import jet.opengl.demos.nvidia.face.sample.FaceWorkTest;
import jet.opengl.demos.nvidia.waves.samples.SampleD3D11;
import jet.opengl.demos.nvidia.waves.samples.TestD3D11;
import jet.opengl.demos.postprocessing.LightingVolumeDemo;
import jet.opengl.demos.postprocessing.OutdoorLightScatteringSample;
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

        //获取可用内存
        long value = Runtime.getRuntime().freeMemory();
        System.out.println("The aviable memory:"+value/1024/1024+"mb");
        //获取jvm的总数量，该值会不断的变化
        long  totalMemory = Runtime.getRuntime().totalMemory();
        System.out.println("The total memory:"+totalMemory/1024/1024+"mb");
        //获取jvm 可以最大使用的内存数量，如果没有被限制 返回 Long.MAX_VALUE;
        long maxMemory = Runtime.getRuntime().maxMemory();
        System.out.println("The maxmum memory:"+maxMemory/1024/1024+"mb");

        value = Runtime.getRuntime().availableProcessors();
        System.out.println("The CPU cores:" + value);

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
//        run(new HBAODemo());
//        run(new ASSAODemoDebug());
//        run(SSAODemoDX11.newInstance());
//        testRectVertex();
//        run(new OutdoorLightScatteringSample());
//        run(new AVSMDemo());
//        run(new ShaderTest());
//        run(new SoftShadowDemo());
//        run(new ShaderNoise());
//        run(new Flight404());
//        run(new LightingVolumeDemo());
//        run(new TestD3D11());
//        run(new Chapman());
//        run(new AtmosphereTest());
        run(new FaceWorkDemo());
//        run(new FaceWorkTest());
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

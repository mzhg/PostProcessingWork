package jet.opengl.desktop.lwjgl;

import com.nvidia.developer.opengl.app.NvAppBase;
import com.nvidia.developer.opengl.app.NvEGLConfiguration;
import com.nvidia.developer.opengl.utils.NvGfxAPIVersion;
import com.nvidia.developer.opengl.utils.NvImage;

import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import jet.opengl.demos.flight404.Flight404;
import jet.opengl.demos.gpupro.ibl.IndirectLighting;
import jet.opengl.demos.intel.avsm.AVSMDemo;
import jet.opengl.demos.nvidia.fire.PerlinFire;
import jet.opengl.demos.nvidia.shadows.SoftShadowDemo;
import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.Numeric;

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
        baseApp.setTile(app.getClass().getSimpleName());
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

        testParaboloidMatrix();

        NvImage.setAPIVersion(NvGfxAPIVersion.GL4_4);
//        run(new HBAODemo());
//        run(new ASSAODemoDebug());
//        run(SSAODemoDX11.newInstance());
//        testRectVertex();
//        run(new OutdoorLightScatteringSample());
//        run(new AVSMDemo());
//        run(new ShaderTest());
        run(new SoftShadowDemo());
//        run(new ShaderNoise());
//        run(new Flight404());
//        run(new LightingVolumeDemo());
//        run(new TestD3D11());
//        run(new Chapman());
//        run(new AtmosphereTest());
//        run(new FaceWorkDemo());
//        run(new FaceWorkTest());
//        run(new VolumetricLightingDemo());
//        run(new ParaboloidShadowDemo());
//        run(new LightPropagationVolumeDemo());
//        run(new AntiAliasingDemo());
//        run(new HybridRendererDemo());
//        run(new CloudSkyDemo());
//        run(new IndirectLighting());
//        run(new PerlinFire());
    }

    private static void testParaboloidMatrix(){
        Vector3f lightPos = new Vector3f(10, 15, 00);
        final float near = 0.5f;
        final float far = 50.f;

        Vector3f dir = new Vector3f(Numeric.random(-1, 1), Numeric.random(-1, 1), Numeric.random(-1, 1));
        dir.normalise();

        Vector3f pos1 = Vector3f.scale(dir, 0.2f * far, null);
        Vector3f pos2 = Vector3f.scale(dir, 0.5f * far, null);

        Vector4f shadowUV1 = ParaboloidProject(pos1, near, far);
        Vector4f shadowUV2 = ParaboloidProject(pos2, near, far);

        float depth1 = shadowUV1.z * (far - near) + near;
        float depth2 = shadowUV2.z * (far - near) + near;
        Vector3f constructPos1 = Vector3f.scale(dir, depth1, null);
        Vector3f constructPos2 = Vector3f.scale(dir, depth2, null);

        System.out.println("shadowUV1 = " + shadowUV1);
        System.out.println("shadowUV2 = " + shadowUV2);
        System.out.println("constructPos1 = " + constructPos1);
        System.out.println("constructPos2 = " + constructPos2);
        System.out.println("pos1 = " + pos1);
        System.out.println("pos2 = " + pos2);
    }

    static Vector4f ParaboloidProject(Vector3f P, float zNear, float zFar)
    {
        Vector4f outP = new Vector4f();
        outP.w = P.z > 0 ? 1 : 0;
        float z = P.z;
        P.z = Math.abs(P.z);
        float lenP = Vector3f.length(P);
//        outP.xyz = P.xyz/lenP;
        Vector3f.scale(P, 1.0f/lenP, outP);
        outP.x = outP.x / (outP.z + 1);
        outP.y = outP.y / (outP.z + 1);
        outP.z = (lenP - zNear) / (zFar - zNear);
        P.z = z;
        return outP;
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

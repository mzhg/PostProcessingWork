package jet.opengl.desktop.jogl;

import com.jogamp.opengl.DebugGL4;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import com.nvidia.developer.opengl.app.NvAppBase;
import com.nvidia.developer.opengl.app.NvEGLConfiguration;
import com.nvidia.developer.opengl.app.NvGLAppContext;

import javax.swing.JFrame;

import jet.opengl.postprocessing.common.GLAPI;
import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/10/12.
 */
public class JoglApp extends JFrame implements GLEventListener, NvGLAppContext {

    private GLCanvas glCanvas;
    private NvAppBase app;
    private JoglInputAdapter2 inputAdapter;
    private GLAutoDrawable gl;

    public JoglApp(NvAppBase app, int width, int height, boolean fullScreen){
        this.app = app;
        setSize(width, height);
        setResizable(false);
        if(fullScreen){
            getGraphicsConfiguration().getDevice().setFullScreenWindow(this);
        }

        glCanvas = new GLCanvas();
        glCanvas.addGLEventListener(this);
        add(glCanvas);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setVisible(true);
        app.setGLContext(this);

        inputAdapter = new JoglInputAdapter2(app, app, app);
        glCanvas.addMouseListener(inputAdapter);
        glCanvas.addMouseMotionListener(inputAdapter);
        glCanvas.addKeyListener(inputAdapter);

        final FPSAnimator animator = new FPSAnimator(glCanvas, 1000,false );
        animator.start();
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        drawable.setGL(new DebugGL4(drawable.getGL().getGL4()));
        gl = drawable;
        LogUtil.i(LogUtil.LogType.DEFAULT, "GLThreadName: " + Thread.currentThread().getName());
        GLFuncProviderFactory.initlizeGLFuncProvider(GLAPI.JOGL, drawable.getGL().getGL4());
        setSwapInterval(0);
        app.onCreate();
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        app.onDestroy();
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        app.draw();
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        app.onResize(width, height);
    }

    @Override
    public boolean setSwapInterval(int interval) {
        return gl.getContext().setSwapInterval(interval);
    }

    @Override
    public int width() {
        return getWidth();
    }

    @Override
    public int height() {
        return getHeight();
    }

    @Override
    public NvEGLConfiguration getConfiguration() {
        return null;
    }

    @Override
    public void requestExit() {
        dispose();
    }

    @Override
    public void setAppTitle(String title) {
        setTitle(title);
    }

    @Override
    public void showDialog(String msg, String errorStr) {

    }

    @Override
    public String getAppTitle() {
        return getTitle();
    }
}

package jet.opengl.desktop.jogl;

import com.nvidia.developer.opengl.app.GLEventListener;
import com.nvidia.developer.opengl.app.NvAppBase;
import com.nvidia.developer.opengl.app.NvEGLConfiguration;
import com.nvidia.developer.opengl.app.NvSampleApp;
import com.nvidia.developer.opengl.utils.NvGfxAPIVersion;
import com.nvidia.developer.opengl.utils.NvImage;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;

/**
 * Created by mazhen'gui on 2017/10/10.
 */
public class JoglDemoMain implements GLEventListener, ListSelectionListener, ItemListener, ActionListener {
    private JFrame mFrame;
    private JList<DemoDesc> mComboBox;
    private JScrollPane     mComboBoxPanel;
    private JComboBox<String> mResolutions;
    private JCheckBox mFullScreen;
    private JButton   mStartApp;
    private final Vector<String> mResolutionList = new Vector<>();
    private final Vector<DemoDesc> mDemoList = new Vector<>();
    private NvSampleApp mCurrentDemo;
    private int mCurrentDemoIdx = -1;
    private Point mSize = new Point();

    public JoglDemoMain(){
        initDemoLists();
        initResolutionLists();

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
        });

        NvImage.setAPIVersion(NvGfxAPIVersion.GL4_4);

        mFrame = new JFrame();
        mFrame.setTitle("Graphics Demos[Jogl]");
        mFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mFrame.setResizable(false);
        Container contentPane = mFrame.getContentPane();

        {
            mComboBox = new JList<>(mDemoList);
            mComboBox.setName("List");
            mComboBoxPanel = new JScrollPane(mComboBox);
            mComboBox.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            mComboBox.addListSelectionListener(this);
            mComboBox.setSelectedIndex(0);

            Border etchedLoweredBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
            Border titledBorderAtTop =
                    BorderFactory.createTitledBorder(etchedLoweredBorder,
                            "Demo List",
                            TitledBorder.CENTER,
                            TitledBorder.TOP);
            mComboBoxPanel.setBorder(titledBorderAtTop);

            contentPane.add(mComboBoxPanel, BorderLayout.CENTER);
        }

        {
            JPanel panel = new JPanel();
            panel.setLayout(new FlowLayout());

            mFullScreen = new JCheckBox("Fullscreen");
            mResolutions = new JComboBox<>(mResolutionList);
            mResolutions.addItemListener(this);

            mStartApp = new JButton("Run Demo!");
            mStartApp.addActionListener(this);
            mStartApp.setBackground(Color.ORANGE);
            mStartApp.setForeground(Color.BLUE);
            panel.add(mFullScreen);
            panel.add(mResolutions);
            panel.add(mStartApp);

            Border etchedLoweredBorder = BorderFactory.createEtchedBorder(EtchedBorder.LOWERED);
            Border titledBorderAtTop =
                    BorderFactory.createTitledBorder(etchedLoweredBorder,
                            "Options",
                            TitledBorder.CENTER,
                            TitledBorder.TOP);
            panel.setBorder(titledBorderAtTop);

            contentPane.add(panel, BorderLayout.SOUTH);
        }

        mFrame.pack();
        mFrame.setLocationRelativeTo(null);
        mFrame.setVisible(true);
    }

    public static void main(String[] args) {
        new JoglDemoMain();
    }

    @Override
    public void onDestroy() {
        mCurrentDemoIdx = -1;
    }

    public void onCreate() {}
    public void onResize(int width, int height) {}
    public void draw() {}

    private void initDemoLists(){
//        for(int i = 0; i < 10; i++){
//            mDemoList.add(new DemoDesc(null, "Label" + i, ""));
//        }

        mDemoList.add(new DemoDesc("jet.opengl.demos.postprocessing.LightingVolumeDemo", "LightingVolumeDemo", "LightingVolumeDemo"));
    }

    private void initResolutionLists(){
        mResolutionList.add("1280x720");
        mResolutionList.add("1024x768");
        mResolutionList.add("1980x1080");
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if(!event.getValueIsAdjusting()){
            // TODO
        }
    }

    @Override
    public void itemStateChanged(ItemEvent itemEvent) {

    }

    @Override
    public void actionPerformed(ActionEvent actionEvent) {
        int idx = mComboBox.getSelectedIndex();
        if(idx >= 0 && mCurrentDemoIdx != idx){
            DemoDesc demoDesc = mDemoList.get(idx);
            mCurrentDemoIdx = idx;
            if(demoDesc.classPath != null){
                startDemo(demoDesc);
            }
        }
    }

    private void startDemo(DemoDesc desc){
        if(mCurrentDemo != null){
            mCurrentDemo.getGLContext().requestExit();
        }

        mCurrentDemo = desc.newInstance();
//        mFrame.setVisible(false);
        Point size = getResolutuion();
        run(mCurrentDemo, size.x, size.y, mFullScreen.isSelected());
    }

    private Point getResolutuion(){
        int idx = mResolutions.getSelectedIndex();
        String string = mResolutionList.get(idx);
        String[] a = string.split("x");
        mSize.x = Integer.parseInt(a[0]);
        mSize.y = Integer.parseInt(a[1]);
        return mSize;
    }

    private void run(NvAppBase app, int width, int height, boolean fullscreen){
        NvEGLConfiguration config = new NvEGLConfiguration();
        app.configurationCallback(config);

        /*LwjglApp baseApp = new LwjglApp();
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
        baseApp.registerGLEventListener(this);
        baseApp.setWindowSize(width, height);
        baseApp.setFullScreenMode(fullscreen);
        app.setGLContext(baseApp);
        baseApp.start();*/

        new JoglApp(app, width, height, fullscreen);
    }

    private static final class DemoDesc{
        String classPath;
        String label;
        String title;

        DemoDesc(String classPath, String label, String title){
            this.classPath = classPath;
            this.label = label;
            this.title = title;
        }

        NvSampleApp newInstance(){
            if(classPath == null)
                return null;

            try {
                Class<?> clazz = Class.forName(classPath);
                return (NvSampleApp)clazz.newInstance();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            return null;
        }

        @Override
        public String toString() {
            return label;
        }
    }
}

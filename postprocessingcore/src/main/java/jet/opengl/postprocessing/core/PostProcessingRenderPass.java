package jet.opengl.postprocessing.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jet.opengl.postprocessing.common.Disposeable;
import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.util.Numeric;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public abstract class PostProcessingRenderPass {

    private static final List<Disposeable> g_Resources = new ArrayList<>();

    private static final Texture2D[] EMPTY_TEX2D = new Texture2D[0];

    private int[] m_Dependencies;
    private int[] m_FixDependencies;
    private int[] m_DependencyCount;
    boolean m_bProcessed;

    protected Texture2D[] m_PassInputs;
    private Texture2D[] m_PassOutputs;
    private InputDesc[] m_InputDescs;

    private int[] m_FixOutputWidth;
    private int[] m_FixOutputHeight;

    private String name;

    public PostProcessingRenderPass(String name){
        this.name = name;
    }

    public String getName() {return  name;}

    protected void set(int inputCount, int outputCount){
        if(inputCount > 0){
            m_InputDescs = new InputDesc[inputCount];
            for(int i = 0; i < inputCount; i++)
                m_InputDescs[i] = new InputDesc();

            m_PassInputs = new Texture2D[inputCount];
        }else{
            m_PassInputs = EMPTY_TEX2D;
        }

        if(outputCount > 0){
            m_PassOutputs = new Texture2D[outputCount];
            m_Dependencies = new int[outputCount];
            m_FixDependencies = new int[outputCount];
            m_DependencyCount = new int[outputCount];

            m_FixOutputWidth = new int[outputCount];
            m_FixOutputHeight = new int[outputCount];
        }else{
            m_PassOutputs = EMPTY_TEX2D;
            m_Dependencies = Numeric.EMPTY_INT;
            m_FixDependencies = Numeric.EMPTY_INT;
            m_DependencyCount = Numeric.EMPTY_INT;

            m_FixOutputWidth = Numeric.EMPTY_INT;
            m_FixOutputHeight = Numeric.EMPTY_INT;
        }
    }

    public int getInputCount() {return m_PassInputs.length;}
    public int getOutputCount() {return m_PassOutputs.length;}

    public Texture2D getInput(int idx) {
        int length = m_PassInputs.length;
        return idx >= length ? null : m_PassInputs[idx];
    }

    public abstract void process(PostProcessingRenderContext context, PostProcessingParameters parameters);

    public Texture2D getOutputTexture(int idx) {
        int length = m_PassOutputs.length;
        return idx >= length ? null : m_PassOutputs[idx];
    }

    public void markOutputSlot(int slot){
        m_DependencyCount[slot]++;
    }

    public void releaseResource(int idx)
    {
        RenderTexturePool.getInstance().freeUnusedResource(m_PassOutputs[idx]);
        m_PassOutputs[idx] = null;
    }

    public void setOutputFixSize(int slot, int width, int height){
        m_FixOutputWidth[slot] = width;
        m_FixOutputHeight[slot] = height;
    }

    public void computeOutDesc(int index, Texture2DDesc out){
        int fixWidth = m_FixOutputWidth[index];
        int fixHeight = m_FixOutputHeight[index];

        if(fixWidth > 0 && fixHeight > 0){
            out.width = fixWidth;
            out.height = fixHeight;
        }
    }



    protected boolean useIntenalOutputTexture(){ return false;}

    void _process(PostProcessingRenderContext context, PostProcessingParameters parameters)
    {
        // TODO other stuff to do here.
        process(context, parameters);
        m_bProcessed = true;
    }

    public void setDependency(int slot, PostProcessingRenderPass dependencyPass, int depentSlot)
    {
        m_InputDescs[slot].dependencyPass = dependencyPass;
        m_InputDescs[slot].slot = depentSlot;

        dependencyPass.increaseDependency(depentSlot);
    }

    protected InputDesc getInputDesc(int index){
        return m_InputDescs[index];
    }

    void reset()
    {
//        m_Dependencies = m_FixDependencies;
        System.arraycopy(m_FixDependencies, 0, m_Dependencies, 0, m_FixDependencies.length);
        m_bProcessed = false;
        Arrays.fill(m_DependencyCount, 0);
    }

    void addDependency(int depentSlot)
    {
        ++m_Dependencies[depentSlot];
    }

    void increaseDependency(int depentSlot)  {m_FixDependencies[depentSlot]++;}
    void setDependencies(int depentSlot, int dependencies) { m_FixDependencies[depentSlot] = dependencies; }
    int getDependencyCount(int depentSlot)
    {
        return m_Dependencies[depentSlot];
    }

    void resolveDependencies()
    {
        for(int i = 0; i < m_DependencyCount.length; i++){
            if (m_Dependencies[i] > 0 && m_DependencyCount[i] > 0)
            {
                m_Dependencies[i] -= m_DependencyCount[i];
                m_DependencyCount[i] = 0;

                if (m_Dependencies[i] == 0)
                {
                    // the internal reference is released
                    //				pooledRenderTarget.safeRelease();
                    //				pooledRenderTarget = null;
//                    return true;
                    releaseResource(i);
                }else if(m_Dependencies[i] < 0){
                    throw new Error("Inner Error!!!");
                }
            }
        }

//        return false;
    }

    void setInputTextures(List<Texture2D> _inputTextures)
    {
        //				mInputTextures = _inputTextures;
        for (int i = 0; i < m_PassInputs.length; i++)
        {
            m_PassInputs[i] = _inputTextures.get(i);
        }
    }

    void setOutputRenderTexture(int slot, Texture2D texture)
    {
        m_PassOutputs[slot] = texture;
    }

    protected static void addDisposedResource(Disposeable disposeable){
        g_Resources.add(disposeable);
    }

    static void releaseResources(){
        for(Disposeable res : g_Resources ){
            res.dispose();
        }
        g_Resources.clear();
    }
}

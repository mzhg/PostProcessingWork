package jet.opengl.postprocessing.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import jet.opengl.postprocessing.texture.Texture2D;
import jet.opengl.postprocessing.texture.Texture2DDesc;
import jet.opengl.postprocessing.texture.TextureUtils;
import jet.opengl.postprocessing.util.LogUtil;

/**
 * Created by mazhen'gui on 2017/4/13.
 */
final class RenderTexturePool {

    private final HashSet<Texture2D> m_CreatedTextures = new HashSet<>();
    private final HashMap<Texture2DDesc, List<Texture2D>> m_RenderTexturePool = new HashMap<>();
    private static RenderTexturePool g_Instance;

    private RenderTexturePool(){}

    public static RenderTexturePool getInstance(){
        if(g_Instance == null)
            g_Instance = new RenderTexturePool();

        return g_Instance;
    }

    public Texture2D findFreeElement(Texture2DDesc desc){
        List<Texture2D> texture2DList = m_RenderTexturePool.get(desc);
        if(texture2DList == null || texture2DList.isEmpty()){
            Texture2D texture2D = TextureUtils.createTexture2D(desc, null);
            m_CreatedTextures.add(texture2D);
            LogUtil.i(LogUtil.LogType.DEFAULT, "Create a new Texture in the RenderTexturePool. Created Texture Count: " + m_CreatedTextures.size());
            return texture2D;
        }else{
            LogUtil.i(LogUtil.LogType.DEFAULT, "Retrive a Texture from the RenderTexturePool." );
            return texture2DList.remove(texture2DList.size() - 1);
        }
    }

    public void freeUnusedResource(Texture2D tex){
        if(m_CreatedTextures.contains(tex)){
            Texture2DDesc desc = tex.getDesc();
            List<Texture2D> texture2DList = m_RenderTexturePool.get(desc);
            if(texture2DList == null){
                texture2DList = new ArrayList<>(4);
                m_RenderTexturePool.put(desc, texture2DList);
            }

            LogUtil.i(LogUtil.LogType.DEFAULT, "Put a unused texture into the RenderTexturePool." );
            for(Texture2D texture2D : texture2DList){
                if(texture2D == tex || texture2D.getTexture() == tex.getTexture()){
                    return;
                }
            }

            texture2DList.add(tex);
        }else{
            LogUtil.e(LogUtil.LogType.DEFAULT, "Couldn't put the external texture into the RenderTexturePool");
        }
    }
}

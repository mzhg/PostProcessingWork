package jet.opengl.postprocessing.common;

import jet.opengl.postprocessing.util.CommonUtil;

/**
 * Created by mazhen'gui on 2017/4/1.
 */

public interface Disposeable {

    void dispose();

    default void SAFE_RELEASE(Disposeable res){
        CommonUtil.safeRelease(res);
    }

    default void SAFE_DELETE_ARRAY(Disposeable[] reses){
        if(reses != null){
            for(int i = 0; i < reses.length; i++){
                Disposeable res = reses[i];
                CommonUtil.safeRelease(res);
                reses[i] = null;
            }
        }
    }
}

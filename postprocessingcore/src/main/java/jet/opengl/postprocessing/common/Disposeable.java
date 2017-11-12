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
}

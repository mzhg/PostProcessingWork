package jet.opengl.postprocessing.util;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public class CommonUtil {

    private CommonUtil(){}

    public static boolean equals(Object a, Object b){
        if(a == null && b == null) {
            return true;
        }else if(a == null || b== null){
            return false;
        }else{
            return a.equals(b);
        }
    }

    public static<T> T[] toArray(T...args){
        return args;
    }

    public static void safeRelease(Disposeable res){
        if(res != null){
            res.dispose();
        }
    }
}

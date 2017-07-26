package jet.opengl.postprocessing.util;

import java.util.List;

import jet.opengl.postprocessing.common.Disposeable;

/**
 * Created by mazhen'gui on 2017/4/17.
 */

public final class CommonUtil {

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

    public static final boolean contain(Object[] a, Object o){
        for(int i = 0; i < a.length; i++){
            if(equals(a[i], o))
                return true;
        }

        return false;
    }

    public static long equal_range(List<?> values, Object var){
        int start = -1;
        int end = -1;

        for(int i = 0; i < values.size(); i++){
            Object obj = values.get(i);
            boolean isEqauled = equals(obj, var);
            if(start == -1){
                if(isEqauled)
                    start = i;
            }

            if(end == -1 || (i - end) == 1){
                if(isEqauled)
                    end = i;
            }else{
                break;
            }
        }

        return Numeric.encode(start, start >=0? (end + 1) : end);
    }
}

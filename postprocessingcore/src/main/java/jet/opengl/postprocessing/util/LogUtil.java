package jet.opengl.postprocessing.util;

import java.util.logging.Logger;

/**
 * Created by mazhen'gui on 2017/4/6.
 */

public class LogUtil {

    public enum LogType{
        DEFAULT,
        NV_FRAMEWROK
    }

    public static Logger getDefaultLogger(){
        return Logger.getLogger("PostProcessing");
    }

    public static Logger getNVFrameworkLogger(){
        return Logger.getLogger("NvLogger");
    }

    public static void i(LogType type,  String msg){
        switch (type) {
            case DEFAULT:
                getDefaultLogger().info(msg);
                break;
            case NV_FRAMEWROK:
                getNVFrameworkLogger().info(msg);
                break;
        }
    }

    public static void e(LogType type,  String msg){
        switch (type) {
            case DEFAULT:
                getDefaultLogger().severe(msg);
                break;
            case NV_FRAMEWROK:
                getNVFrameworkLogger().severe(msg);
                break;
        }
    }
}

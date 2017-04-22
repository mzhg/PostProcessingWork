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

    public interface GetMessage{
        String get();
    }

    public static void i(LogType type, GetMessage msg){
        switch (type) {
            case DEFAULT:
                getDefaultLogger().info(msg.get());
                break;
            case NV_FRAMEWROK:
                getNVFrameworkLogger().info(msg.get());
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

    public static void e(LogType type,  GetMessage msg){
        switch (type) {
            case DEFAULT:
                getDefaultLogger().severe(msg.get());
                break;
            case NV_FRAMEWROK:
                getNVFrameworkLogger().severe(msg.get());
                break;
        }
    }
}

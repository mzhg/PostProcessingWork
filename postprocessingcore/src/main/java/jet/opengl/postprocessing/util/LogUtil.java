package jet.opengl.postprocessing.util;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by mazhen'gui on 2017/4/6.
 */

public class LogUtil {

    private static final Logger g_DefaultLogger = Logger.getLogger("PostProcessing");
    private static final Logger g_NV_FRAMWORK_Logger = Logger.getLogger("NvLogger");

    public enum LogType{
        DEFAULT,
        NV_FRAMEWROK
    }

    public static void setLoggerLevel(LogType type, Level level){
        switch (type){
            case DEFAULT:
                getDefaultLogger().setLevel(level);
                break;
            case NV_FRAMEWROK:
                getNVFrameworkLogger().setLevel(level);
                break;
        }
    }

    public static Logger getDefaultLogger(){
        return g_DefaultLogger;
    }

    public static Logger getNVFrameworkLogger(){
        return g_NV_FRAMWORK_Logger;
    }

    public static void i(LogType type,  String msg){
        switch (type) {
            case DEFAULT:
                g_DefaultLogger.info(msg);
                break;
            case NV_FRAMEWROK:
                g_NV_FRAMWORK_Logger.info(msg);
                break;
        }
    }

    public interface GetMessage{
        String get();
    }

    public static void i(LogType type, GetMessage msg){
        switch (type) {
            case DEFAULT:
                g_DefaultLogger.info(msg.get());
                break;
            case NV_FRAMEWROK:
                g_NV_FRAMWORK_Logger.info(msg.get());
                break;
        }
    }

    public static void e(LogType type,  String msg){
        switch (type) {
            case DEFAULT:
                g_DefaultLogger.severe(msg);
                break;
            case NV_FRAMEWROK:
                g_NV_FRAMWORK_Logger.severe(msg);
                break;
        }
    }

    public static void e(LogType type,  GetMessage msg){
        switch (type) {
            case DEFAULT:
                g_DefaultLogger.severe(msg.get());
                break;
            case NV_FRAMEWROK:
                g_NV_FRAMWORK_Logger.severe(msg.get());
                break;
        }
    }
}

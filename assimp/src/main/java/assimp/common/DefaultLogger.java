package assimp.common;

public final class DefaultLogger {

	public static boolean LOG_OUT = false;
	
	public static void debug(String str){
		if(LOG_OUT)System.out.println("Debug: " + str);
	}

	public static void info(String str) {
		if(LOG_OUT)System.out.println("Info: " + str);
	}

	public static void warn(String string) {
		if(LOG_OUT)System.out.println("Warn: " + string);
	}

	public static void error(String string) {
		if(LOG_OUT)System.err.println("Error: " + string);
	}
}

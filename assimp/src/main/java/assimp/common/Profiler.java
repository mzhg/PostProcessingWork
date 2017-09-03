package assimp.common;

import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;

public class Profiler {

	private final Object2LongMap<String> regions = new Object2LongOpenHashMap<String>(6);
	
	public Profiler() {
		regions.defaultReturnValue(-1);
	}
	
	/** Start a named timer */
	public void beginRegion(String region) {
//		regions[region] = boost::timer();
//		DefaultLogger::get()->debug((format("START `"),region,"`"));
		
		regions.put(region, System.currentTimeMillis());
		if(DefaultLogger.LOG_OUT){
			DefaultLogger.debug("START `" + region + "`");
		}
	}
	
	/** End a specific named timer and write its end time to the log */
	public void endRegion(String region) {
//		RegionMap::const_iterator it = regions.find(region);
//		if (it == regions.end()) {
//			return;
//		}
//
//		DefaultLogger::get()->debug((format("END   `"),region,"`, dt= ",(*it).second.elapsed()," s"));
		
		long start = regions.getLong(region);
		if(start == -1)
			return;
		
		if(DefaultLogger.LOG_OUT){
			float elapsed = (System.currentTimeMillis() - start) * 0.001f;
			DefaultLogger.debug("END   `" + region + "`, dt= " + (elapsed) + " s");
		}
		
	}
}

package assimp.common;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import it.unimi.dsi.fastutil.objects.Object2FloatOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Matrix4f;

/** Internal PIMPL implementation for {@link Importer}<p> */
final class ImporterPimpl {

	/** Progress handler for feedback. */
	ProgressHandler mProgressHandler;
	boolean mIsDefaultProgressHandler;

	/** Format-specific importer worker objects - one for each format we can read.*/
	List<BaseImporter> mImporter/* = new ArrayList<BaseImporter>()*/;

	/** Post processing steps we can apply at the imported data. */
	List<BaseProcess> mPostProcessingSteps/* = new ArrayList<BaseProcess>()*/;

	/** The imported data, if ReadFile() was successful, NULL otherwise. */
	Scene mScene;

	/** The error description, if there was one. */
	String mErrorString;

	/** List of integer properties */
	final Object2IntMap<String> mIntProperties = new Object2IntOpenHashMap<>(7);

	/** List of floating-point properties */
	final Object2FloatMap<String> mFloatProperties = new Object2FloatOpenHashMap<>(7);

	/** List of string properties */
	final Map<String, String> mStringProperties = new HashMap<String,String>(7);

	/** List of Matrix properties */
	final Map<String, Matrix4f> mMatrixProperties = new HashMap<String, Matrix4f>(7);

	/** Used for testing - extra verbose mode causes the ValidateDataStructure-Step
	 *  to be executed before and after every single postprocess step */
	boolean bExtraVerbose;

	/** Used by post-process steps to share data */
//	SharedPostProcessInfo* mPPShared; 
	final Map<String, Object> mPPShared = new HashMap<String, Object>(8);
	
	InputStream mIOHandler;
 }

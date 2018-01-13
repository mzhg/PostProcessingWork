package jet.opengl.postprocessing.shader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import jet.opengl.postprocessing.common.GLFuncProviderFactory;
import jet.opengl.postprocessing.common.GLenum;
import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.StringUtils;

public class ShaderLoader {
	
	public static final String AMD_DRIVER = "__AMD_DRIVER__";
	public static final String NVIDIA_DRIVER = "__NVIDIA_DRIVER__";
	public static boolean DEBUG = false;

	static WeakReference<CharSequence> defaultHeader;
	static final WeakHashMap<String, CharSequence> includeCaches = new WeakHashMap<String, CharSequence>();
	static String renderer = null;
	
	/** Initialize the ShaderLoader, this method should called on the OpenGL thread. */
	private static void init(){
		if(renderer == null){
			renderer = GLFuncProviderFactory.getGLFuncProvider().glGetString(GLenum.GL_RENDERER).toUpperCase();
		}
	}
	
	public static CharSequence getDefaultHeader(){
		init();
		
		CharSequence result = null;
		if(defaultHeader != null)
			result = defaultHeader.get();
		
		if(result == null){
			StringBuilder sb = new StringBuilder();
			if(renderer.contains("AMD"))
				sb.append("#define ").append(AMD_DRIVER);
			else if(renderer.contains("NVIDIA"))
				sb.append("#define ").append(NVIDIA_DRIVER);
			else{
				// TODO other video driver.
			}
			
			if(sb.length() > 0)
				sb.append('\n');
			result = sb;
			defaultHeader = new WeakReference<CharSequence>(sb);
		}
		
		return result;
	}
	
	public static CharSequence loadShaderFile(String filepath) throws IOException{
		return loadShaderFile(filepath, false, false);
	}
	
	public static CharSequence loadShaderFile(String filepath, boolean classFile)throws IOException{
		return loadShaderFile(filepath, classFile, false);
	}
	
	@Deprecated
	public static CharSequence loadShaderFile(String filepath, boolean classFile, boolean includeDefaultHeader)throws IOException{
		IncludeFile file = new IncludeFile(filepath, classFile, false);
		Map<String, LoaderParameters.FileDesc> processing = new HashMap<>();
		if(DEBUG)
			System.out.println("start loading file: " + filepath);
		CharSequence result = internalLoadShaderFile(file, processing, new LoaderParameters());
		if(includeDefaultHeader){
			CharSequence defaultHeader = getDefaultHeader();
			if(result instanceof StringBuilder){
				StringBuilder sb = (StringBuilder)result;
				sb.ensureCapacity(sb.length() + defaultHeader.length());
				sb.insert(0, defaultHeader);
				return sb;
			}else{
				StringBuilder sb = new StringBuilder(result.length() + defaultHeader.length());
				sb.append(defaultHeader).append(result);
				return sb;
			}
		}
		
		if(DEBUG)
			System.out.println("Included Tags: " + processing.toString());
		
		return result;
	}
	
	public static CharSequence loadShaderFile(LoaderParameters params)throws IOException{
		IncludeFile file = new IncludeFile(params.filepath, params.classFile, false);
//		Set<String> processing = new HashSet<String>();
		Map<String, LoaderParameters.FileDesc> processing = new HashMap<>();
		if(DEBUG)
			System.out.println("start loading file: " + params.filepath);
		CharSequence result = internalLoadShaderFile(file, processing, params);
		if(params.includeDefaultHeader){
			CharSequence defaultHeader = getDefaultHeader();
			if(result instanceof StringBuilder){
				StringBuilder sb = (StringBuilder)result;
				sb.ensureCapacity(sb.length() + defaultHeader.length());
				sb.insert(0, defaultHeader);
				return sb;
			}else{
				StringBuilder sb = new StringBuilder(result.length() + defaultHeader.length());
				sb.append(defaultHeader).append(result);
				return sb;
			}
		}

		if(params.includeFiles != null){
			params.includeFiles.addAll(processing.values());
		}
		
		return result;
	}
	
	private static CharSequence internalLoadShaderFile(IncludeFile file, Map<String, LoaderParameters.FileDesc> processing, LoaderParameters params)throws IOException{
		InputStream in = null;
		FileLoader loader = params.fileLoader != null ? params.fileLoader : FileUtils.g_IntenalFileLoader;

		String parent;
		String key = file.getKey();
		if(key != null && processing.containsKey(key)){
			if(DEBUG)
				System.out.println("The include file " + file.filepath + " has included into the cache!");
			return "";
		}
		
		if(file.isClassFile){
			in = ClassLoader.getSystemResourceAsStream(file.filepath);
			if(in == null){
				// re-search from std-class library if satisfy condition
				int dot = file.filepath.indexOf('/');
				if(dot == -1){
					file.filepath = IncludeFile.STANDARD_CLASS + file.filepath;
					in = ClassLoader.getSystemResourceAsStream(file.filepath);
				}
				
				if(in == null){
					throw new NullPointerException("Conldn't find the class-file: " + file.filepath);
				}
			}
			
			int dot = file.filepath.lastIndexOf('/');
			if(dot != -1)
				parent = file.filepath.substring(0, dot);
			else
				parent = "";
			
			if(key == null)
				key = "<class>" + file.filepath;
		}else{
			if(DEBUG){
				System.out.println("Start Loading: " + file.filepath);
			}

			in = loader.open(file.filepath);
			if(in == null)
				throw new NullPointerException("Couldn't find the external-file: " + file.filepath);
//			parent = ff.getParent();
			parent = FileUtils.getParent(file.filepath);

			if(key == null)
				key = "<file>"+loader.getCanonicalPath(file.filepath);
		}
		
		if(processing.containsKey(key)){
			in.close();
			if(DEBUG)
				System.out.println("The include file " + file.filepath + " has included into the cache!");
			return "";
		}
		
		file.key = key; // assign the key value to this file.
		
		processing.put(key, new LoaderParameters.FileDesc(file.filepath, file.isClassFile));
		if(DEBUG)
			System.out.println("Add " + key);
		
		CharSequence string = includeCaches.get(key);
		if(string == null){
			final List<String> subIncludesFiles = new ArrayList<String>();
			final List<String> includeTags = new ArrayList<String>();
			final String tag = "#include";
			string = FileUtils.loadText(in, params.igoreComment, params.charset, new FileUtils.LineFilter() {
				public String filter(String line) {
					int src_offset = StringUtils.firstNotEmpty(line);
//					int idx = line.indexOf(tag);
					boolean flag = src_offset >= 0 ? StringUtils.startWith(line, src_offset, tag, 0) : false /* if src_offset == -1, the line is empty */;
					if(/*idx >= 0*/ flag){
						subIncludesFiles.add(line.substring(src_offset + tag.length()));
						String includeTag = "@@" + line + "@@";
						includeTags.add(includeTag);
						return includeTag;
					}else{
						return line;
					}
				}
			});
			
			in.close();
			if(subIncludesFiles.isEmpty()){
				CharSequence s = string/*.toString()*/;
				includeCaches.put(key, s);
//				includeFiles.put(key, s);
				return string;
			}else{
				IncludeFile parentFile = new IncludeFile(parent, file.isClassFile, true);
				CharSequence[] sequences = new CharSequence[subIncludesFiles.size()];
				int i = 0;
				int totalLength = 0;
				for(String sif : subIncludesFiles){
					IncludeFile subFile = IncludeFile.parse(sif, parentFile);
					// Check file validation
					String file_key = subFile.getKey();
					IncludeFile tp = file;
					while(tp != null){
						if(file_key.equals(tp.key)){
							throw new UnsupportedOperationException("Unsupport the mutual-included situation<" +tp.filepath + " : " + file.filepath + ">");
						}
						
						tp = tp.parent;
					}
					
					IncludeFile.makeRelation(subFile, file);
					
					CharSequence seq = internalLoadShaderFile(subFile, processing, params);
					totalLength += seq.length() - includeTags.get(i).length();
					sequences[i++] = seq;
				}
				
				StringBuilder sb = (StringBuilder)string;
				sb.ensureCapacity(sb.length() + totalLength);
				int cap = sb.capacity();
				int prev = 0;
				for(i = 0; i < subIncludesFiles.size(); i++){
					String includeTag = includeTags.get(i);
					int i0 = sb.indexOf(includeTag, prev);
					if(i0 < 0)
						throw new Error("Unexpected i0");
					sb.delete(i0, i0 + includeTag.length());
					sb.insert(i0, sequences[i]);
					prev = i0 + sequences[i].length();
				}
				if(sb.length() > cap)
					System.err.println("The allocate capacity is not enought");
				
//				includeCaches.put(key, sb);  Could cause bug
				return sb;
			}
		}else{
			if(DEBUG){
				System.out.println("Retrive file: " + file.key);
			}
			if(in != null)
				in.close();
			return string;
		}
		
	}


	
}

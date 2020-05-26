package jet.opengl.postprocessing.shader;

import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;

import jet.opengl.postprocessing.util.FileLoader;
import jet.opengl.postprocessing.util.FileUtils;
import jet.opengl.postprocessing.util.StringUtils;

/**
 * Include form:<ul>
 * <li> #include "shader.glsl"
 * <li> #include "../shader.glsl"
 * <li> #include "<external>shader.glsl"
 * <li> #include "<class>pack1/pac2/shader.glsl"
 * <li> #include "<std-class>shader.glsl"
 *  </ul>
 */
final class IncludeFile {
	
	static final String STANDARD_CLASS = "jet/opengl/shader/libs/";
	
	String filepath;
	boolean isClassFile;
	boolean isDir;
	String key;
	IncludeFile parent;
	List<IncludeFile> children;
	
	public IncludeFile() {
	}
	
	public IncludeFile(String filepath, boolean isClassFile, boolean isDir) {
		this.filepath = filepath;
		this.isClassFile = isClassFile;
		this.isDir = isDir;
	}
	
	String getKey(){ /*if(key == null) throw new Error("Inner Error"); */return key;}
	
	static IncludeFile parse(String includeTag, IncludeFile parentFile) throws IOException{
		boolean is_std_path = false;
		String errorStr = includeTag;
		IncludeFile result = new IncludeFile();
		
		includeTag = includeTag.replace("#include", "");
		int i1 = includeTag.indexOf('\"');
		int i2 = includeTag.lastIndexOf('\"');
		
		if(i1 >= 0 && i2 > i1){
			// make sure the remaining string is blank
			if(i2 < includeTag.length() - 1){
				if(!StringUtils.isBlank(includeTag, i2 + 1))
					throw new IncludeSynaxErrorException("Invalid include tag: " + includeTag);
			}
			includeTag = includeTag.substring(i1 + 1, i2);

			if(ShaderLoader.isIgoreFile(includeTag)) {
				return null;
			}
		}else if(i1 >= 0 && i2 == i1){  // only find one '"'
			throw new IllegalArgumentException("Invalid includeTag: " + errorStr);
		}
		
		i1 = includeTag.indexOf('<');
		if(i1 >=0){
			i2 = includeTag.indexOf('>', i1+1);
			if(i2 < 0)
				throw new IllegalArgumentException("Invalid includeTag: " + errorStr);
			String tag = includeTag.substring(i1+1, i2).toLowerCase();
			result.filepath = includeTag.substring(i2 + 1);
			if(tag.equals("external")){
				result.isClassFile = false;
			}else if(tag.equals("class")){
				result.isClassFile = true;
			}else if(tag.equals("std-class")){
				result.isClassFile = true;
				result.filepath = STANDARD_CLASS + result.filepath;
				is_std_path = true;
			}
		}else{
			result.filepath = includeTag;
			result.isClassFile = parentFile.isClassFile;
		}
		
		// check file validation
//		boolean c = result.filepath.startsWith("../");
//		if(c && result.isClassFile && !parent.isClassFile)
//			throw new IllegalArgumentException("Can't use '../' in the non-class file heri");
		
		if(result.isClassFile){
			if(result.filepath.startsWith("../")){
				String[] arrays = null;
				int count = 0;
				
				// parent file must be a class path
				if(!parentFile.isClassFile)
					throw new IllegalArgumentException("The classpath contains the '../', but the parent file is not a classpath.");
				// detect the parent path is weather a file or dictionary.
				InputStream in = ClassLoader.getSystemResourceAsStream(parentFile.filepath);
				if(in == null){// parent is package-name
					// Test weather the package-name exist.
					Package pack = Package.getPackage(parentFile.filepath);
					if(pack == null)
						throw new NullPointerException("The parent package name<" + parentFile.filepath + "doesn't exits");
					else{
						arrays = parentFile.filepath.split("/");
						count = arrays.length;
					}
				}else{  // parent is file
					in.close();
					arrays = parentFile.filepath.split("/");
					count = arrays.length - 1;
				}
				
				while(result.filepath.startsWith("../")){
					count--;
					result.filepath = result.filepath.substring(3);
				}
				
				StringBuilder sb = new StringBuilder();
				for(int i = 0; i < count; i++)
					sb.append(arrays[i]).append('/');
				sb.append(result.filepath);
				result.filepath = sb.toString();
			}else{
				if(parentFile.isClassFile && !is_std_path){
					if(parentFile.filepath.endsWith("/"))
						result.filepath = parentFile.filepath + result.filepath;
					else
						result.filepath = parentFile.filepath + "/" + result.filepath;
				}
			}
			
			// check weather the class-file has exits.
			InputStream in = ClassLoader.getSystemResourceAsStream(result.filepath);
			if(in == null)
				throw new NullPointerException("The class-file <" + result.filepath + "> doesn't exits. The parent = " + parentFile.filepath );
			else
				in.close();
			
			result.key = "<class>" + result.filepath;
		}else{ // external file
			FileLoader loader = FileUtils.g_IntenalFileLoader;
			if(loader.exists(result.filepath)){
				result.key = "<file>" + loader.getCanonicalPath(result.filepath);
				return result;
			}
			
			if(!parentFile.isClassFile){
				if(parentFile.isDir){
//					file = new File(parentFile.filepath, result.filepath);
					String file = parentFile.filepath + '\\' + result.filepath;
					if(loader.exists(file)){
						result.filepath = loader.getCanonicalPath(file);
						result.key = "<file>" + result.filepath;
						return result;
					}
				}else{
//					file = new File(new File(parentFile.filepath).getParent(), result.filepath);
					String file = FileUtils.getParent(parentFile.filepath) + "/" + result.filepath;
					if(loader.exists(file)){
						result.filepath = loader.getCanonicalPath(file);
						result.key = "<file>" + result.filepath;
						return result;
					}
				}
			}
			
			throw new NullPointerException("Couldn't find the include file: " + result.filepath);
			
			// search from the parent path.  TODO
//			List<String> tokens = new ArrayList<>();
//			StringTokenizer tokenizer = new StringTokenizer(parent.filepath, "/\\ \t\n");
//			while (tokenizer.hasMoreElements()) {
//				tokens.add(tokenizer.nextToken());
//			}
//			
//			if(!parent.isDir && tokens.size() > 0)
//				tokens.remove(tokens.size() - 1);
		}
		
		return result;
	}
	
	static void makeRelation(IncludeFile child, IncludeFile parent){
		child.parent = parent;
		if(parent.children == null)
			parent.children = new LinkedList<IncludeFile>();
		parent.children.add(child);
	}
}

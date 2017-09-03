package assimp.common;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;

public final class FileUtils {

	@SuppressWarnings("resource")
	public static void copyFile(File src, File dest) throws IOException{
		FileChannel inChannel = new FileInputStream(src).getChannel();
		FileChannel outChannel = new FileOutputStream(dest).getChannel();

		int maxCount = (64 * 1024 * 1024) - (32 * 1024);
		long size = inChannel.size();
		long position = 0;
		while (position < size) {
			position += inChannel.transferTo(position, maxCount, outChannel);
		}

		if (inChannel != null)
			inChannel.close();

		if (outChannel != null)
			outChannel.close();
		
		dest.setLastModified(src.lastModified());
	}
	
	public static StringBuilder loadText(File file){
		return loadText(file, false);
	}
	
	// TODO This method have a bug.
	public static ByteBuffer loadText(File file, boolean igoreComment, boolean natived){
		ByteBuffer buf = null;
		final List<Integer> tokens = new ArrayList<>();
		
		try(FileInputStream file_in = new FileInputStream(file);
				BufferedReader in = new BufferedReader(new InputStreamReader(file_in))){
			buf = MemoryUtil.createByteBuffer(file_in.available() + 1, natived);
			
			String line;
			boolean inComment = false;
			while((line = in.readLine()) != null){
				if(igoreComment){
					inComment = igoreComment(line, inComment, tokens);
					int count = tokens.size()/2;
					boolean append = false;
					for(int i = 0; i < count; i++){
						int start = tokens.get(2 * i);
						int end = tokens.get(2 * i + 1);
						String subLine = line.substring(start, end).trim();
						if(subLine.length() > 0){
//							buf.append(subLine).append(' ');
							for(int s = 0; s < subLine.length(); s++){
								buf.put((byte)subLine.charAt(s));
							}
							buf.put((byte)' ');
							append = true;
						}
					}
					
					if(append) buf.put((byte)'\n');
				}else{
					for(int s = 0; s < line.length(); s++){
						buf.put((byte)line.charAt(s));
					}
					buf.put((byte)'\n');
				}
			}
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		buf.flip();
		return buf.duplicate();
	}
	
	public static void save(String filename, byte[] buf, int offset, int len){
		try {
			DataOutputStream out = new DataOutputStream(new FileOutputStream(filename));
			out.write(buf, 0, len);
			out.flush();
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static StringBuilder loadText(File file, boolean igoreComment){
		StringBuilder buf = new StringBuilder(128);
		final List<Integer> tokens = new ArrayList<>();
		
		try {
			FileReader fin = new FileReader(file);
			BufferedReader in = new BufferedReader(fin);
			String line;
			boolean inComment = false;
			while((line = in.readLine()) != null){
				if(igoreComment){
					inComment = igoreComment(line, inComment, tokens);
					int count = tokens.size()/2;
					boolean append = false;
					for(int i = 0; i < count; i++){
						int start = tokens.get(2 * i);
						int end = tokens.get(2 * i + 1);
						String subLine = line.substring(start, end).trim();
						if(subLine.length() > 0){
							buf.append(subLine).append(' ');
							append = true;
						}
					}
					
					if(append) buf.append('\n');
				}else{
					buf.append(line).append('\n');
				}
			}
			
			in.close();
			fin.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return buf;
	}
	
	public static final StringBuilder loadTextFromClassPath(Class<?> cls, String filename){
		InputStream input = cls.getResourceAsStream(filename);
		if(input == null) return null;
		
		try {
			BufferedReader in = new BufferedReader(new InputStreamReader(input));
			StringBuilder sb = new StringBuilder(Math.max(1, input.available()));
			String s;

			while ((s = in.readLine()) != null)
				sb.append(s).append('\n');
			in.close();
			input.close();
			return sb;
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}
	
	private static boolean igoreComment(String line, boolean inComment, List<Integer> tokens){
		tokens.clear();
		int startIndex = 0;
		if(inComment){
			int index = line.indexOf("*/");
			if(index == -1)
				return true;
			else{
				tokens.add(0);
				tokens.add(index);
				startIndex = index+2;
			}
		}
		
		// filter all the /**/ comment
		while(true){
			int start = line.indexOf("/*", startIndex);
			int end = line.indexOf("*/", startIndex);
			
			if(start == -1 && end != - 1){
//				throw new RuntimeException("Synax Error: " + line);
				
			}
			
			if(start == -1 && end == -1){
				// no comment
				break;
			}
			
			if(startIndex < start - 1){
				tokens.add(startIndex);
				tokens.add(start);
			}
			
			if(start != -1 && end == -1){
				// still in comment.
				return true;
			}
			
			if(start != -1 && end != -1){
				startIndex = end + 2;
			}
		}
		
		// filter the '//' comment
		int index = line.indexOf("//");
		tokens.add(startIndex);
		if(index == -1){
			tokens.add(line.length());
		}else{
			tokens.add(index);
		}
		
		return false;
	}
	
	public static ByteBuffer loadBinary(String filepath, boolean natived) throws IOException{
		return loadBinary(new File(filepath), natived);
	}
	
	public static ByteBuffer loadBinary(File file, boolean natived) throws IOException{
		@SuppressWarnings("resource")
		FileChannel in = new FileInputStream(file).getChannel();
		ByteBuffer buf;
		if(natived)
			buf = ByteBuffer.allocateDirect((int)in.size()).order(ByteOrder.nativeOrder());
		else
			buf = ByteBuffer.allocate((int)in.size()).order(ByteOrder.nativeOrder());
		
		in.read(buf);
		in.close();
		buf.flip();
		return buf;
	}
	
	private FileUtils(){}
}

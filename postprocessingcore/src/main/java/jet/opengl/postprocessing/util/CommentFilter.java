package jet.opengl.postprocessing.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class CommentFilter{

	BufferedReader reader;
	private String singleComment = "//";
	private String pairCommentLeft = "/*";
	private String pairCommentRight = "*/";
	StringBuilder lineBuffer = new StringBuilder();
	private boolean inComment;
	
	public CommentFilter(Reader in) {
		if(in instanceof BufferedReader){
			reader = (BufferedReader)in;
		}else{
			reader = new BufferedReader(in);
		}
	}
	
	public String nextLine()throws IOException{
		String line = reader.readLine();
		if(line == null)
			return null;
		
		while(parseLine(line)){
			line = reader.readLine();
			if(line == null){
				String result =  lineBuffer.toString();
				lineBuffer.setLength(0);
				return result;
			}
		}
		
		String result =  lineBuffer.toString();
		lineBuffer.setLength(0);
		return result;
	}
	
	private boolean parseLine(String line){
		int fromIndex = 0;
		int singleIndex;
		int pairStart;
		
		while(true){
			if(inComment){
				int pairEnd = line.indexOf(pairCommentRight);
				if(pairEnd >= 0){ // find the "*/"
					fromIndex = pairEnd + pairCommentRight.length();
					inComment = false;
					continue;
				}else{  // No find the "*/" continue parsing.
					return true;
				}
			}else{
				singleIndex = line.indexOf(singleComment, fromIndex);
				pairStart = line.indexOf(pairCommentLeft, fromIndex);
				
				if(singleIndex >=0 && pairStart >=0){
					if(singleIndex < pairStart)
						pairStart = -1;
					else
						singleIndex = -1;
				}
				
				if(singleIndex >= 0){
					lineBuffer.append(line.substring(fromIndex, singleIndex));
					return false;
				}
				
				if(pairStart >= 0){
					int length = pairCommentRight.length();
					lineBuffer.append(line.substring(fromIndex, pairStart));
					int pairEnd = line.indexOf(pairCommentRight, pairStart + length);
					if(pairEnd >= 0){
						fromIndex = pairEnd + length;
						continue;
					}else{
						inComment = true;
						return true;  // we need a new line.
					}
				}
				
				// singleIndex and the pairStart are both -1. This is a pure string.
				lineBuffer.append(line.substring(fromIndex));
				return false;
			}
		}
	}
}

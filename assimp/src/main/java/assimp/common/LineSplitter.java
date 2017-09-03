/*
Open Asset Import Library (assimp)
----------------------------------------------------------------------

Copyright (c) 2006-2012, assimp team
All rights reserved.

Redistribution and use of this software in source and binary forms, 
with or without modification, are permitted provided that the 
following conditions are met:

* Redistributions of source code must retain the above
  copyright notice, this list of conditions and the
  following disclaimer.

* Redistributions in binary form must reproduce the above
  copyright notice, this list of conditions and the
  following disclaimer in the documentation and/or other
  materials provided with the distribution.

* Neither the name of the assimp team, nor the names of its
  contributors may be used to endorse or promote products
  derived from this software without specific prior
  written permission of the assimp team.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS 
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT 
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT 
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT 
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY 
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE 
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

----------------------------------------------------------------------
*/
package assimp.common;

/** Usage:
<pre>
for(LineSplitter splitter(stream);splitter;++splitter) {

	if (*splitter == "hi!") {
	   ...
	}
    else if (splitter->substr(0,5) == "hello") {
	   ...
	   // access the third token in the line (tokens are space-separated)
	   if (strtol(splitter[2]) > 5) { .. }
	}

	std::cout << "Current line is: " << splitter.get_index() << std::endl;
}
</pre> */
public class LineSplitter {

	int idx;
	final StringBuilder cur = new StringBuilder(512);
	StreamReader stream;
	boolean swallow, skip_empty_lines, trim;
	
	public LineSplitter(StreamReader stream){
		this(stream, true, true);
	}
	
	public LineSplitter(StreamReader stream, boolean skip_empty_lines /*= true*/, boolean trim /*= true*/) {
		this.stream = stream;
		this.skip_empty_lines = skip_empty_lines;
		this.trim = trim;
	}
	
	public LineSplitter next(){
		if(swallow) {
			swallow = false;
			return this;
		}
		if (!hasMore()) {
			throw new RuntimeException("End of file, no more lines to be retrieved.");
		}
		byte s;
		if(cur.length() > 0)
			cur.delete(0, cur.length());
		while(stream.getRemainingSize() > 0 /*&& (s = stream.getI1(),1)*/) {
			s = stream.getI1();
			if (s == '\n' || s == '\r') {
				if (skip_empty_lines) {
					while (stream.getRemainingSize() > 0  && ((s = stream.getI1()) == ' ' || s == '\r' || s == '\n'));
					if (stream.getRemainingSize() > 0) {
						stream.incPtr(-1);
					}
				}
				else {
					// skip both potential line terminators but don't read past this line.
					if (stream.getRemainingSize() > 0 && (s == '\r' && stream.getI1() != '\n')) {
						stream.incPtr(-1);
					}
					if (trim) {
						while (stream.getRemainingSize() > 0 && ((s = stream.getI1()) == ' ' || s == '\t'));
						if (stream.getRemainingSize() > 0) {
							stream.incPtr(-1);
						}
					}
				}
				break;
			}
//			cur += s;
			cur.append((char)s);
		}
		++idx;
		return this;
	}
	
	public String get_token(int idx){
		int s = 0;
		int end = cur.length();
		for(; s < end; s++){
			char c = cur.charAt(s);
			if(c != ' ' && c != '\t')
				break;
		}
		
		for(int i = 0; i < idx; i++){
			if(s == end)
				throw new IndexOutOfBoundsException("Token index out of range, EOL reached");
			
			for(; s < end; ++s){
				char c = cur.charAt(s);
				if(c == ' ' || c == '\t')
					break;
			}
			
			for(; s < end; s++){
				char c = cur.charAt(s);
				if(c != ' ' && c != '\t')
					break;
			}
		}
		
		return cur.substring(s);
	}
	
	public void get_tokens(String[] tokens){ get_tokens(tokens, tokens.length);}
	
	public void get_tokens(String[] tokens, int count){
		int s = 0;
		int end = cur.length();
		for(; s < end; s++){
			char c = cur.charAt(s);
			if(c != ' ' && c != '\t')
				break;
		}
		
		for(int i = 0; i < count; i++){
			if(s == end)
				throw new IndexOutOfBoundsException("Token index out of range, EOL reached");
			
			int prev = s;
			for(; s < end; ++s){
				char c = cur.charAt(s);
				if(c == ' ' || c == '\t')
					break;
			}
			
			tokens[i] = cur.substring(prev, s);
			
			for(; s < end; s++){
				char c = cur.charAt(s);
				if(c != ' ' && c != '\t')
					break;
			}
		}
	}
	
	public StringBuilder get() { return cur;}
	
	public boolean hasMore() { return stream.getRemainingSize() > 0;}
	
	// -----------------------------------------
	/** line indices are zero-based, empty lines are included */
	public int line_idx(){ return idx;}

	public int get_index() { return idx;}

	// -----------------------------------------
	/** access the underlying stream object */
	public StreamReader get_stream() {	return stream;}

	// -----------------------------------------
	/** !strcmp((*this)->substr(0,strlen(check)),check) */
	public boolean match_start(String check) {
		for(int i = 0; i < check.length(); i++){
			if(cur.charAt(i) != check.charAt(i))
				return false;
		}
		
		return true;
	}


	// -----------------------------------------
	/** swallow the next call to ++, return the previous value. */
	public void swallow_next_increment() {
		swallow = true;
	}
}

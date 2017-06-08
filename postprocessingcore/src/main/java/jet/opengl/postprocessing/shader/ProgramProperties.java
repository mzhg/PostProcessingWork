package jet.opengl.postprocessing.shader;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ProgramProperties {

	public int programID;

	/** True if program is currently flagged for deletion, and false otherwise. */
	public boolean delete_status;
	
	/** True if the last link operation on program was successful, and false otherwise. */
	public boolean link_status;
	
	/** True or if the last validation operation on program was successful, and false otherwise */
	public boolean validate_status;
	
	/** The number of characters in the information log for program including the null termination character (i.e., the size of the character buffer required to store the information log). If program has no information log, a value of 0 is returned. */
	public int info_log_length;
	
	/** The source string of the program log. */
	public String info_log_source = "";
	
	/** The number of shader objects attached to program. */
	public int attached_shaders;
	
	/** The names of the attached shaders to program */
	public String[] attached_shader_names;
	
	/**
	 * The number of active attribute atomic counter buffers used by program.
	 */
	public int active_atomic_counter_buffers;
	
	/**
	 * The number of active attribute variables for program.
	 */
	public int active_attributes;
	
	/** The properties of active attribute variables for program. */
	public AttribProperties[] active_attribute_properties;
	
	/**
	 * The length of the longest active attribute name for program, including the null termination character (i.e., the size of the character buffer required to store the longest attribute name). If no active attributes exist, 0 is returned.
	 */
	public int active_attribute_max_length;
	
	/** The number of active uniform variables for program. */
	public int active_uniforms;
	
	/** The properties of active uniform variables for program. */
	public UniformProperty[] active_uniform_properties;
	
	/**
	 * The length of the longest active uniform variable name for program, including the null termination character (i.e., the size of the character buffer required to store the longest uniform variable name). If no active uniform variables exist, 0 is returned.
	 */
	public int active_uniform_max_length;
	
	/**
	 * The length of the program binary, in bytes that will be returned by a call to glGetProgramBinary. When a progam's GL_LINK_STATUS is GL_FALSE, its program binary length is zero.
	 */
	public int program_binary_length;
	
	/**
	 * An array of three integers containing the local work group size of the compute program as specified by its input layout qualifier(s). program must be the name of a program object that has been previously linked successfully and contains a binary for the compute shader stage.
	 */
	public int[] compute_work_group_size;
	
	/** A symbolic constant indicating the buffer mode used when transform feedback is active. This may be GL_SEPARATE_ATTRIBS or GL_INTERLEAVED_ATTRIBS. */
	public int transform_feedback_buffer_mode;
	
	/** The number of varying variables to capture in transform feedback mode for the program.*/
	public int transform_feedback_varyings;
	
	/** The length of the longest variable name to be used for transform feedback, including the null-terminator. */
	public int transform_feedback_varying_max_length;
	
	/** The maximum number of vertices that the geometry shader in program will output. */
	public int geometry_vertices_out;
	
	/** A symbolic constant indicating the primitive type accepted as input to the geometry shader contained in program.*/
	public int geometry_input_type;
	
	/** A symbolic constant indicating the primitive type that will be output by the geometry shader contained in program. */
	public int geometry_output_type;
	
	public void toString(PrintWriter writer){
		writer.append("ProgramID: ").append(Integer.toString(programID)).append('\n');
		writer.append("DELETE_STATUS: ").append(Boolean.toString(delete_status)).append('\n');
		writer.append("LINK_STATUS: ").append(Boolean.toString(link_status)).append('\n');
		writer.append("VALIDATE_STATUS: ").append(Boolean.toString(validate_status)).append('\n');
		
		if(info_log_length > 0)
			writer.append("INFO_LOG: \n").append(info_log_source).append('\n').append('\n');
		
		if(active_attributes > 0){
			writer.append("ACTIVE ATTRIBUTES:\n");
			
			for(int i = 0; i < active_attributes; i++){
				writer.append('\t').append(active_attribute_properties[i].toString()).append('\n');
			}
		}
		
		if(active_uniforms > 0){
			writer.append("ACTIVE UNIFORMS: \n");
			
			for(int i = 0; i < active_uniforms; i++){
				writer.append('\t').append(active_uniform_properties[i].toString()).append('\n');
			}
		}
	}
	
	@Override
	public String toString() {
		StringWriter out = new StringWriter();
		PrintWriter writer = new PrintWriter(out);
		toString(writer);
		
		return out.toString();
	}
}

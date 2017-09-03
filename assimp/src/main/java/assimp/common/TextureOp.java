package assimp.common;

/** Defines how the Nth texture of a specific type is combined with
 *  the result of all previous layers.
 *
 *  Example (left: key, right: value): <br>
 *  <pre>
 *  DiffColor0     - gray
 *  DiffTextureOp0 - aiTextureOpMultiply
 *  DiffTexture0   - tex1.png
 *  DiffTextureOp0 - aiTextureOpAdd
 *  DiffTexture1   - tex2.png
 *  </pre>
 *  Written as equation, the final diffuse term for a specific pixel would be: 
 *  <pre>
 *  diffFinal = DiffColor0 * sampleTex(DiffTexture0,UV0) + 
 *     sampleTex(DiffTexture1,UV0) * diffContrib;
 *  </pre>
 *  where 'diffContrib' is the intensity of the incoming light for that pixel.
 */
public enum TextureOp {

	/** T = T1 * T2 */
	aiTextureOp_Multiply /*= 0x0*/,

	/** T = T1 + T2 */
	aiTextureOp_Add /*= 0x1*/,

	/** T = T1 - T2 */
	aiTextureOp_Subtract /*= 0x2*/,

	/** T = T1 / T2 */
	aiTextureOp_Divide /*= 0x3*/,

	/** T = (T1 + T2) - (T1 * T2) */
	aiTextureOp_SmoothAdd /*= 0x4*/,

	/** T = T1 + (T2-0.5) */
	aiTextureOp_SignedAdd /*= 0x5*/,
}

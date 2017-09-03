package assimp.common;

/** Defines all shading models supported by the library<p>
*
*  The list of shading modes has been taken from Blender.
*  See Blender documentation for more information. The API does
*  not distinguish between "specular" and "diffuse" shaders (thus the
*  specular term for diffuse shading models like Oren-Nayar remains
*  undefined). <p>
*  Again, this value is just a hint. Assimp tries to select the shader whose
*  most common implementation matches the original rendering results of the
*  3D modeller which wrote a particular model as closely as possible.
*/
public enum ShadingMode {

	// 写入的时候按原值写入，读的时候按原值读
	/** Flat shading. Shading is done on per-face base, 
     *  diffuse only. Also known as 'faceted shading'.
     */
    aiShadingMode_Flat /*= 0x1*/,

    /** Simple Gouraud shading. 
     */
    aiShadingMode_Gouraud /*=	0x2*/,

    /** Phong-Shading -
     */
    aiShadingMode_Phong /*= 0x3*/,

    /** Phong-Blinn-Shading
     */
    aiShadingMode_Blinn	/*= 0x4*/,

    /** Toon-Shading per pixel
     *
	 *  Also known as 'comic' shader.
     */
    aiShadingMode_Toon /*= 0x5*/,

    /** OrenNayar-Shading per pixel
     *
     *  Extension to standard Lambertian shading, taking the
     *  roughness of the material into account
     */
    aiShadingMode_OrenNayar /*= 0x6*/,

    /** Minnaert-Shading per pixel
     *
     *  Extension to standard Lambertian shading, taking the
     *  "darkness" of the material into account
     */
    aiShadingMode_Minnaert /*= 0x7*/,

    /** CookTorrance-Shading per pixel
	 *
	 *  Special shader for metallic surfaces.
     */
    aiShadingMode_CookTorrance /*= 0x8*/,

    /** No shading at all. Constant light influence of 1.0.
    */
    aiShadingMode_NoShading /*= 0x9*/,

	 /** Fresnel shading
     */
    aiShadingMode_Fresnel /*= 0xa*/,
}

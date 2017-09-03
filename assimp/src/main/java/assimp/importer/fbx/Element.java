package assimp.importer.fbx;

import java.util.ArrayList;

import javax.swing.text.html.HTMLEditorKit.Parser;

/** FBX data entity that consists of a key:value tuple.
*
*  Example:
*  <pre>
*    AnimationCurve: 23, "AnimCurve::", "" {
*        [..]
*    }
*  </pre>
*
*  As can be seen in this sample, elements can contain nested #Scope
*  as their trailing member.  **/
final class Element {

	private Token key_token;
	private ArrayList<Token> tokens;
	private Scope compound;
	
	public Element(Token key_token, Parser parser) {
	}
	
	Scope compound() {	return compound;}
	Token keyToken() {	return key_token;}
	ArrayList<Token> tokens() {	return tokens;}
}

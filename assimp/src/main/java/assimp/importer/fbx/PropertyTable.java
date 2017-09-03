package assimp.importer.fbx;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.lwjgl.util.vector.Vector3f;

import assimp.common.AssUtil;
import assimp.common.ObjectHolder;

/** Represents a property table as can be found in the newer FBX files (Properties60, Properties70)*/
final class PropertyTable {

	private Map<String, Element> lazyProps;
	private Map<String, Object> props;
	private PropertyTable templateProps;
	private Element element;
	
	public PropertyTable() {
	}
	
	public PropertyTable(Element element, PropertyTable templateProps) {
		this.templateProps = templateProps;
		this.element = element;
		
		Scope scope = Parser.getRequiredScope(element);
//		BOOST_FOREACH(const ElementMap::value_type& v, scope.Elements()) {
		for (Map.Entry<String, LinkedList<Element>> v : scope.elements().entrySet()){
			String v_first = v.getKey();
			Element v_second = v.getValue().getFirst();
			
			if(v_first != "P") {
				FBXUtil.DOMWarning("expected only P elements in property table",v_second);
				continue;
			}

			String name = peekPropertyName(v_second);
			if(AssUtil.isEmpty(name)) {
				FBXUtil.DOMWarning("could not read property name",v_second);
				continue;
			}

//			LazyPropertyMap::const_iterator it = lazyProps.find(name);
			if (/*it != lazyProps.end()*/ lazyProps.containsKey(name)) {
				FBXUtil.DOMWarning("duplicate property name, will hide previous value: " + name,v_second);
				continue;
			}

//			lazyProps[name] = v.second;
			lazyProps.put(name, v_second);
		}
	}
	
	// read a typed property out of a FBX element. The return value is NULL if the property cannot be read.
	static Object readTypedProperty(Element element)
	{
//		ai_assert(element.KeyToken().StringContents() == "P");

//		const TokenList& tok = element.Tokens();
//		ai_assert(tok.size() >= 5);
		List<Token> tok = element.tokens();
		assert (tok.size() >= 5);

		final String s = Parser.parseTokenAsStringSafe(tok.get(1));
//		const char* const cs = s.c_str();
		if (!FBXUtil.strcmp(s,"KString")) {
//			return new TypedProperty<std::string>(ParseTokenAsString(*tok[4]));
			return Parser.parseTokenAsStringSafe(tok.get(4));
		}
		else if (!FBXUtil.strcmp(s,"bool") || !FBXUtil.strcmp(s,"Bool")) {
//			return new TypedProperty<bool>(ParseTokenAsInt(*tok[4]) != 0);
			return Parser.parseTokenAsIntSafe(tok.get(4)) != 0;
		}
		else if (!FBXUtil.strcmp(s,"int") || !FBXUtil.strcmp(s,"enum")) {
//			return new TypedProperty<int>(ParseTokenAsInt(*tok[4]));
			return Parser.parseTokenAsIntSafe(tok.get(4));
		}
		else if (!FBXUtil.strcmp(s,"ULongLong")) {
//			return new TypedProperty<uint64_t>(ParseTokenAsID(*tok[4]));
			return Parser.parseTokenAsIDSafe(tok.get(4));
		}
		else if (!FBXUtil.strcmp(s,"Vector3D") || 
			!FBXUtil.strcmp(s,"ColorRGB") || 
			!FBXUtil.strcmp(s,"Vector") || 
			!FBXUtil.strcmp(s,"Color") || 
			!FBXUtil.strcmp(s,"Lcl Translation") || 
			!FBXUtil.strcmp(s,"Lcl Rotation") || 
			!FBXUtil.strcmp(s,"Lcl Scaling")
			) {
//			return new TypedProperty<aiVector3D>(aiVector3D(
//				ParseTokenAsFloat(*tok[4]),
//				ParseTokenAsFloat(*tok[5]),
//				ParseTokenAsFloat(*tok[6]))
//			);
			
			float x = Parser.parseTokenAsFloatSafe(tok.get(4));
			float y = Parser.parseTokenAsFloatSafe(tok.get(5));
			float z = Parser.parseTokenAsFloatSafe(tok.get(6));
			return new Vector3f(x, y, z);
		}
		else if (!FBXUtil.strcmp(s,"double") || !FBXUtil.strcmp(s,"Number") || !FBXUtil.strcmp(s,"KTime") || !FBXUtil.strcmp(s,"Float")) {
//			return new TypedProperty<float>(ParseTokenAsFloat(*tok[4]));
			return Parser.parseTokenAsFloatSafe(tok.get(4));
		}
		return null;
	}
	
	static String peekPropertyName(Element element)
	{
//		ai_assert(element.KeyToken().StringContents() == "P");
//		const TokenList& tok = element.Tokens();
		List<Token> tok = element.tokens();
		if(tok.size() < 4) {
			return "";
		}

//		return ParseTokenAsString(*tok[0]);
		return Parser.parseTokenAsStringSafe(tok.get(0));
	}
	
	// ------------------------------------------------------------------------------------------------
	Object get(String name)
	{
//		PropertyMap::const_iterator it = props.find(name);
		Object value = props.get(name);
		if (/*it == props.end()*/ value == null) {
			// hasn't been parsed yet?
//			LazyPropertyMap::const_iterator lit = lazyProps.find(name);
			Element lit = lazyProps.get(name);
			if(lit != /*lazyProps.end()*/ null) {
//				props[name] = readTypedProperty(*(*lit).second);
//				it = props.find(name);
				props.put(name, value = readTypedProperty(lit));
				assert value != null;

//				ai_assert(it != props.end());
			}

			if (/*it == props.end()*/value == null) {
				// check property template
				if(templateProps != null) {
					return templateProps.get(name);
				}

				return null;
			}
		}
		
		return /*(*it).second*/ value;
	}

	Map<String, Object> getUnparsedProperties()
	{
		Map<String, Object> result = new HashMap<String, Object>();

		// Loop through all the lazy properties (which is all the properties)
//		BOOST_FOREACH(const LazyPropertyMap::value_type& element, lazyProps) {
		for(Map.Entry<String, Element> element : lazyProps.entrySet()) {

			// Skip parsed properties
//			if (props.end() != props.find(element.first)) continue;
			if(element.getValue() == null) continue;

			// Read the element's value.
			// Wrap the naked pointer (since the call site is required to acquire ownership)
			// std::unique_ptr from C++11 would be preferred both as a wrapper and a return value.
//			boost::shared_ptr<Property> prop = boost::shared_ptr<Property>(ReadTypedProperty(*element.second));
			Object prop = readTypedProperty(element.getValue());
				
			// Element could not be read. Skip it.
			if (prop == null) continue;

			// Add to result
//			result[element.first] = prop;
			result.put(element.getKey(), prop);
		}

		return result;
	}
	
	/** PropertyTable's need not be coupled with FBX elements so this can be NULL*/
	public Element getElement() {	return element;	}

	public PropertyTable templateProps() {	return templateProps;}
	
	@SuppressWarnings("unchecked")
	static<T> T propertyGet(PropertyTable in, String name, T defaultValue){
		Object prop = in.get(name);
		if(prop == null)
			return defaultValue;
		
		// strong typing, no need to be lenient 
		try {
			return (T)prop;
		} catch (ClassCastException e) {
			return null;
		}
	}
	
	@SuppressWarnings("unchecked")
	static<T> T propertyGet(PropertyTable in, String name, ObjectHolder<Boolean> result){
		Object prop = in.get(name);
		if(prop == null){
			result.set(Boolean.FALSE);
			return null;
		}
		
		// strong typing, no need to be lenient
		try {
			result.set(Boolean.TRUE);
			return (T)prop;
		} catch (ClassCastException e) {
			result.set(Boolean.FALSE);
			return null;
		}
	}
	
}

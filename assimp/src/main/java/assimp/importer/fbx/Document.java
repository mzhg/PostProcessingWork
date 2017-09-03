package assimp.importer.fbx;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import assimp.common.AssUtil;

/** DOM root for a FBX file */
final class Document {

	static final int MAX_CLASSNAMES = 6;
	private ImportSettings settings;

	private Long2ObjectMap<LazyObject> objects;
	private Parser parser;

	private Map<String, PropertyTable> templates;
	private Long2ObjectMap<List<Connection>> src_connections;
	private Long2ObjectMap<List<Connection>> dest_connections;

	private int fbxVersion;
	private String creator;
	private final int[] creationTimeStamp = new int[7];

	private LongArrayList animationStacks;
	private ArrayList<AnimationStack> animationStacksResolved;

	private FileGlobalSettings globals;
	
	public Document(Parser parser, ImportSettings settings) {
		this.parser = parser;
		this.settings = settings;
		
		// cannot use array default initialization syntax because vc8 fails on it
//		for (unsigned int i = 0; i < 7; ++i) {
//			creationTimeStamp[i] = 0;
//		}

		readHeader();
		readPropertyTemplates();

		readGlobalSettings();

		// this order is important, connections need parsed objects to check
		// whether connections are ok or not. Objects may not be evaluated yet,
		// though, since this may require valid connections.
		readObjects();
		readConnections();
	}
	
	LazyObject getObject(long id){ return objects.get(id);}

	boolean isBinary() { return parser.isBinary();}

	int fbxVersion() {	return fbxVersion;}

	String creator() {	return creator;}

	// elements (in this order): Uear, Month, Day, Hour, Second, Millisecond
	int[] creationTimeStamp() {	return creationTimeStamp;}

	FileGlobalSettings globalSettings() {
//		ai_assert(globals.get());
		return globals;
	}

	Map<String, PropertyTable> templates(){	return templates;}
	Long2ObjectMap<LazyObject> objects(){	return objects;}

	ImportSettings settings(){	return settings;}

	Long2ObjectMap<List<Connection>> connectionsBySource(){	return src_connections;}
	Long2ObjectMap<List<Connection>> connectionsByDestination(){	return dest_connections;}

	// note: the implicit rule in all DOM classes is to always resolve
	// from destination to source (since the FBX object hierarchy is,
	// with very few exceptions, a DAG, this avoids cycles). In all
	// cases that may involve back-facing edges in the object graph,
	// use LazyObject::IsBeingConstructed() to check.

	List<Connection> getConnectionsBySourceSequenced(long source){
		return getConnectionsSequenced(source, src_connections);
	}
	List<Connection> getConnectionsByDestinationSequenced(long dest){
		return getConnectionsSequenced(dest, dest_connections);
	}

	List<Connection> getConnectionsBySourceSequenced(long source,String classname) {
		String[] arr = {classname};
		return getConnectionsBySourceSequenced(source, arr);
	}
	List<Connection> getConnectionsByDestinationSequenced(long dest,String classname){
//		const char* arr[] = {classname};
		return getConnectionsByDestinationSequenced(dest, new String[]{classname});
	}

	List<Connection> getConnectionsBySourceSequenced(long source, String[] classnames){
		return getConnectionsSequenced(source, true, connectionsBySource(),classnames);
	}
	
	List<Connection> getConnectionsByDestinationSequenced(long dest, String[] classnames){
		return getConnectionsSequenced(dest, false, connectionsByDestination(),classnames);
	}

	List<AnimationStack> animationStacks(){
		if (!AssUtil.isEmpty(animationStacksResolved) || AssUtil.isEmpty(animationStacks)) {
			return animationStacksResolved;
		}

//		animationStacksResolved.reserve(animationStacks.size());
		animationStacksResolved = AssUtil.reserve(animationStacksResolved, animationStacks.size());
//		BOOST_FOREACH(uint64_t id, animationStacks) {
		for(int i = 0; i < animationStacks.size(); i++){
			final LazyObject  lazy = getObject(animationStacks.get(i));
			AnimationStack stack;
			if(lazy  == null|| (stack = (AnimationStack)lazy.get(false)) == null) {
				FBXUtil.DOMWarning("failed to read AnimationStack object");
				continue;
			}
			animationStacksResolved.add(stack);
		}

		return animationStacksResolved;
	}
	
	List<Connection> getConnectionsSequenced(long id, Long2ObjectMap<List<Connection>> conns){
		List<Connection> range = conns.get(id);
		if(range == null) return Collections.emptyList();
		ArrayList<Connection> temp = new ArrayList<Connection>(range);
		Collections.sort(temp);
		return temp;
	}
	
	List<Connection> getConnectionsSequenced(long id, boolean is_src, Long2ObjectMap<List<Connection>> conns, String[] classnames){
		int count = classnames.length;
		/*ai_assert(count != 0 && count <= MAX_CLASSNAMES)*/ assert (count != 0 && count <= MAX_CLASSNAMES);

//		size_t lenghts[MAX_CLASSNAMES];
//
//		const size_t c = count;
//		for (size_t i = 0; i < c; ++i) {
//			lenghts[i] = strlen(classnames[i]);
//		}

//		std::vector<const Connection*> temp;

//		const std::pair<ConnectionMap::const_iterator,ConnectionMap::const_iterator> range = 
//			conns.equal_range(id);
//
//		temp.reserve(std::distance(range.first,range.second));
		List<Connection> range = conns.get(id);
		if(range == null)
			return Collections.emptyList();
		ArrayList<Connection> temp = new ArrayList<Connection>(range.size());
//		for (ConnectionMap::const_iterator it = range.first; it != range.second; ++it) {
		for (Connection second : range){
			Token key = (is_src 
				? second.lazyDestinationObject()
				: second.lazySourceObject()
			).getElement().keyToken();

//			const char* obtype = key.begin();
//
//			for (size_t i = 0; i < c; ++i) {
//				ai_assert(classnames[i]);
//				if(static_cast<size_t>(std::distance(key.begin(),key.end())) == lenghts[i] && !strncmp(classnames[i],obtype,lenghts[i])) {
//					obtype = NULL;
//					break;
//				}
//			}
//			if(obtype) {
//				continue;
//			}
			
			ByteBuffer obtype = key.contents();
			for(int i = 0;i < count; i++){
				if(AssUtil.equals(obtype, classnames[i], 0, obtype.remaining())){
					obtype = null;
					break;
				}
			}
			
			if(obtype != null)
				continue;

			temp.add(second);
		}

//		std::sort(temp.begin(), temp.end(), std::mem_fun(&Connection::Compare));
		temp.sort(null);
		return temp; // NRVO should handle this
	}
	
	void readHeader(){
		// read ID objects from "Objects" section
		Scope sc = parser.getRootScope();
	    final Element ehead = sc.get("FBXHeaderExtension");
		if(ehead == null || ehead.compound() == null) {
			FBXUtil.DOMError("no FBXHeaderExtension dictionary found");
		}

		Scope shead = ehead.compound();
		fbxVersion = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Parser.getRequiredElement(shead,"FBXVersion",ehead), 0));

		// while we maye have some success with newer files, we don't support
		// the older 6.n fbx format
		if(fbxVersion < 7100) {
			FBXUtil.DOMError("unsupported, old format version, supported are only FBX 2011, FBX 2012 and FBX 2013");
		}
		if(fbxVersion > 7300) {
			if(settings().strictMode) {
				FBXUtil.DOMError("unsupported, newer format version, supported are only FBX 2011, FBX 2012 and FBX 2013"+
					" (turn off strict mode to try anyhow) ");
			}
			else {
				FBXUtil.DOMWarning("unsupported, newer format version, supported are only FBX 2011, FBX 2012 and FBX 2013,"+
					" trying to read it nevertheless");
			}
		}
		

		final Element ecreator = shead.get("Creator");
		if(ecreator != null) {
			creator = Parser.parseTokenAsStringSafe(Parser.getRequiredToken(ecreator,0));
		}

		final Element etimestamp = shead.get("CreationTimeStamp");
		if(etimestamp != null && etimestamp.compound() != null) {
			Scope stimestamp = etimestamp.compound();
			creationTimeStamp[0] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Parser.getRequiredElement(stimestamp,"Year", null),0));
			creationTimeStamp[1] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Parser.getRequiredElement(stimestamp,"Month", null),0));
			creationTimeStamp[2] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Parser.getRequiredElement(stimestamp,"Day", null),0));
			creationTimeStamp[3] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Parser.getRequiredElement(stimestamp,"Hour", null),0));
			creationTimeStamp[4] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Parser.getRequiredElement(stimestamp,"Minute", null),0));
			creationTimeStamp[5] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Parser.getRequiredElement(stimestamp,"Second", null),0));
			creationTimeStamp[6] = Parser.parseTokenAsIntSafe(Parser.getRequiredToken(Parser.getRequiredElement(stimestamp,"Millisecond", null),0));
		}
	}
	
	void readObjects(){
		// read ID objects from "Objects" section
		Scope sc = parser.getRootScope();
		final Element eobjects = sc.get("Objects");
		if(eobjects == null || eobjects.compound() == null) {
			FBXUtil.DOMError("no Objects dictionary found");
		}

		// add a dummy entry to represent the Model::RootNode object (id 0),
		// which is only indirectly defined in the input file
//		objects[0] = new LazyObject(0L, *eobjects, *this);
		objects.put(0, new LazyObject(0L, eobjects, this));

		Scope sobjects = eobjects.compound();
//		BOOST_FOREACH(ElementMap::value_type& el, sobjects.Elements()) {
		for (Map.Entry<String, LinkedList<Element>> el : sobjects.elements().entrySet()){
			// extract ID 
//			TokenList& tok = el.second->Tokens();
			final Element el_second = el.getValue().getFirst();
			List<Token> tok = el_second.tokens();
			
			if (tok.isEmpty()) {
				FBXUtil.DOMError("expected ID after object key",el_second);
			}

//			char* err;

			final long id = Parser.parseTokenAsID(tok.get(0));
			String err = Parser.get_error();
			if(err != null) {
				FBXUtil.DOMError(err,el_second);
			}

			// id=0 is normally implicit
			if(id == 0L) {
				FBXUtil.DOMError("encountered object with implicitly defined id 0",el_second);
			}

			if(objects.get(id) != /*objects.end()*/ null) {
				FBXUtil.DOMWarning("encountered duplicate object id, ignoring first occurrence",el_second);
			}

//			objects[id] = new LazyObject(id, *el.second, *this);
			objects.put(id, new LazyObject(id, el_second, this));

			// grab all animation stacks upfront since there is no listing of them
			if(!FBXUtil.strcmp(el.getKey(),"AnimationStack")) {
				animationStacks.add(id);
			}
		}
	}
	
	void readPropertyTemplates(){
		Scope sc = parser.getRootScope();
		// read property templates from "Definitions" section
		Element edefs = sc.get("Definitions");
		if(edefs == null || edefs.compound() == null) {
			FBXUtil.DOMWarning("no Definitions dictionary found");
			return;
		}

		Scope sdefs = edefs.compound();
		LinkedList<Element> otypes = sdefs.getCollection("ObjectType");
		if(otypes == null)
			return;
		
//		for(ElementMap::const_iterator it = otypes.first; it != otypes.second; ++it) {
		for(Element el : otypes){
//			Element& el = *(*it).second;
			sc = el.compound();
			if(sc == null) {
				FBXUtil.DOMWarning("expected nested scope in ObjectType, ignoring",el);
				continue;
			}

			List<Token> tok = el.tokens();
			if(AssUtil.isEmpty(tok)) {
				FBXUtil.DOMWarning("expected name for ObjectType element, ignoring",el);
				continue;
			}

			final String oname = Parser.parseTokenAsString(tok.get(0));

//			ElementCollection templs = sc->GetCollection("PropertyTemplate");
			LinkedList<Element> templs = sc.getCollection("PropertyTemplate");
			if(templs == null) continue;
			
//			for(ElementMap::const_iterator it = templs.first; it != templs.second; ++it) {
			for(Element ele : templs){
//				Element& el = *(*it).second;
				sc = ele.compound();
				if(sc == null) {
					FBXUtil.DOMWarning("expected nested scope in PropertyTemplate, ignoring",ele);
					continue;
				}

//				TokenList& tok = el.Tokens();
				List<Token> toks = ele.tokens();
				if(AssUtil.isEmpty(toks)) {
					FBXUtil.DOMWarning("expected name for PropertyTemplate element, ignoring",ele);
					continue;
				}

				final String pname = Parser.parseTokenAsStringSafe(toks.get(0));

				Element properties70 = sc.get("Properties70");
				if(properties70 != null) {
					PropertyTable props = new PropertyTable(
						properties70,null/*boost::shared_ptr<PropertyTable>(static_cast<PropertyTable*>(NULL))*/
					);

//					templates[oname+"."+pname] = props;
					templates.put(oname+"."+pname, props);
				}
			}
		}
	}
	
	void readConnections(){
		Scope sc = parser.getRootScope();
		// read property templates from "Definitions" section
		Element econns = sc.get("Connections");
		if(econns == null || econns.compound() == null) {
			FBXUtil.DOMError("no Connections dictionary found");
		}

		long insertionOrder = 0l;

		Scope sconns = econns.compound();
//		ElementCollection conns = sconns.GetCollection("C");
		List<Element> conns = sconns.getCollection("C");
		if(conns == null) return;
//		for(ElementMap::const_iterator it = conns.first; it != conns.second; ++it) {
		for (Element el : conns){
//			Element& el = *(*it).second;
			String type = Parser.parseTokenAsStringSafe(Parser.getRequiredToken(el,0));
			long src = Parser.parseTokenAsIDSafe(Parser.getRequiredToken(el,1));
			long dest = Parser.parseTokenAsIDSafe(Parser.getRequiredToken(el,2));

			// OO = object-object connection
			// OP = object-property connection, in which case the destination property follows the object ID
			String prop = (type.equals("OP") ? Parser.parseTokenAsStringSafe(Parser.getRequiredToken(el,3)) : "");

//			if(objects.find(src) == objects.end()) {
			if(!objects.containsKey(src)){
				FBXUtil.DOMWarning("source object for connection does not exist",el);
				continue;
			}

			// dest may be 0 (root node) but we added a dummy object before
//			if(objects.find(dest) == objects.end()) {
			if(!objects.containsKey(dest)){
				FBXUtil.DOMWarning("destination object for connection does not exist",el);
				continue;
			}

			// add new connection
			Connection c = new Connection(insertionOrder++,src,dest,prop,this);
//			src_connections.insert(ConnectionMap::value_type(src,c));  
//			dest_connections.insert(ConnectionMap::value_type(dest,c));
			FBXUtil.put(src_connections, src, c);
			FBXUtil.put(dest_connections, dest, c);
		}
	}
	
	void readGlobalSettings(){
		Scope sc = parser.getRootScope();
		final Element ehead = sc.get("GlobalSettings");
		if(ehead==null || ehead.compound() == null) {
			FBXUtil.DOMWarning("no GlobalSettings dictionary found");

			globals = (new FileGlobalSettings(this, /*boost::make_shared<const*/new  PropertyTable/*>*/()));
			return;
		}

		PropertyTable props = getPropertyTable(this, "", ehead, ehead.compound(), true);

		if(props == null) {
			FBXUtil.DOMError("GlobalSettings dictionary contains no property table");
		}

		globals = (new FileGlobalSettings(this, props));
	}
	
	// ------------------------------------------------------------------------------------------------
	// fetch a property table and the corresponding property template 
	static PropertyTable getPropertyTable(Document doc, String templateName, Element element, 
		Scope sc, boolean no_warn /*= false*/)
	{
//			const Element* const Properties70 = sc["Properties70"];
//			boost::shared_ptr<const PropertyTable> templateProps = boost::shared_ptr<const PropertyTable>(
//				static_cast<const PropertyTable*>(NULL));
		final Element properties70 = sc.get("Properties70");
		PropertyTable templateProps = null;

		if(!AssUtil.isEmpty(templateName)) {
//				PropertyTemplateMap::const_iterator it = doc.Templates().find(templateName); 
//				if(it != doc.Templates().end()) {
//					templateProps = (*it).second;
//				}
			templateProps = doc.templates().get(templateName);
		}

		if(properties70 == null) {
			if(!no_warn) {
				FBXUtil.DOMWarning("property table (Properties70) not found",element);
			}
			if(templateProps != null) {
				return templateProps;
			}
			else {
//					return boost::make_shared<const PropertyTable>();
				return new PropertyTable();
			}
		}
//			return boost::make_shared<const PropertyTable>(*Properties70,templateProps);
		return new PropertyTable(properties70,templateProps);
	}
}

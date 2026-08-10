package translator.r2rml.datalog;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.base.Sys;

import be.ugent.idlab.knows.dataio.record.CSVRecord;
import be.ugent.idlab.knows.dataio.record.Record;
import be.ugent.rml.Executor;
import be.ugent.rml.Mapping;
import be.ugent.rml.MappingFactory;
import be.ugent.rml.MappingInfo;
import be.ugent.rml.NAMESPACES;
import be.ugent.rml.PredicateObjectGraphMapping;
import be.ugent.rml.StrictMode;
import be.ugent.rml.Utils;
import be.ugent.rml.conformer.MappingConformer;
import be.ugent.rml.extractor.Extractor;
import be.ugent.rml.records.RecordsFactory;
import be.ugent.rml.store.Quad;
import be.ugent.rml.store.QuadStore;
import be.ugent.rml.store.QuadStoreFactory;
import be.ugent.rml.store.RDF4JStore;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;

public class DatalogGenerator {
    static int d_count =0;
    static int g_count =0;
    static int jc_count =0;
    static int l_count =0;
    static int subj_count=0;
    static  List<String> schema;
    static  List<String> schema2;
    static   String subj_map = ""; 
    static LinkedHashSet<String> declarations = new LinkedHashSet<String>();
    static  HashMap<Term,Integer> ls = new HashMap<Term,Integer>(); 
    static  HashMap<Term,String> subj_map2 = new HashMap<Term, String>(); 
    //static  HashMap<Term, String>term_predicates = new HashMap<Term,String>();
    static  HashMap<Term, String>term_predicates2 = new HashMap<Term,String>();
    static  HashMap<Term,String>joins = new HashMap<Term,String>();
    static  HashMap<Term, LinkedList<Term>>graph_terms = new HashMap<Term,LinkedList<Term>>();
    static  HashMap<Term, LinkedList<String>>graph_predicates = new HashMap<Term,LinkedList<String>>();
    static  HashMap<Term, LinkedList<String>> maps_po = new HashMap<Term,LinkedList<String>>();
    static   HashMap<Term, LinkedList<String>>maps_join = new HashMap<Term, LinkedList<String>>();
    static HashMap<Term, Term> link_graph= new HashMap<Term,Term>();
    static  HashMap<Term, String>vars = new HashMap<Term,String>();
    static  HashMap<Term, String>datatypes = new HashMap<Term,String>();
    static  String variables="";
    static  String variables2="";
    static  String variables22="";
    static String variablesdec ="";
    static String variablesdec2 ="";
    static String tablename ="";
    static String tablename2 ="";
    static final String defaultBaseIRI = "http://example.com/base";
    static String baseIRI="";
    static  HashMap<String,String>donepreds= new HashMap<String,String>();
    static HashMap<String,String>doneobjs= new HashMap<String,String>();
    static HashMap<String,String>donesubj= new HashMap<String,String>();
    static  HashMap<String,String>donesubjtermtypes= new HashMap<String,String>();
    static  HashMap<String,String>donetermtypes= new HashMap<String,String>();
    static  HashMap<String,String>tablesterms= new HashMap<String,String>();

    public static void exec_dlog(String mappingfiledirectory,  String CONNECTION, String username, String password, Boolean base,String output) throws Exception {
        //String CONNECTION =  "jdbc:mysql://localhost:3306/r2rml";
         //System.out.println(dbClassName);
            // Class.forName(xxx) loads the jdbc classes and
            // creates a drivermanager class factory
          //  Class.forName(dbClassName);

        //     Properties for user and password.
            Properties p = new Properties();
            p.put("user",username);
            p.put("password",password);

    //	    String mappingfiledirectory= "C:\\Users\\aliha\\OneDrive\\Desktop\\rmlmapper-java-master\\r2rml-datalog\\test\\mapping.rml.ttl";
    	LinkedHashSet<String> rules=new LinkedHashSet<String>();
    	     List<String> edbs= new LinkedList<String>();
   	 String mapPath = Utils.getFile(mappingfiledirectory).getParent();//path to the mapping file that needs to be executed
         File mappingFile = new File(mappingfiledirectory);
        InputStream mappingStream = new FileInputStream(mappingFile);
        InputStream mappingStream2 = new FileInputStream(mappingFile);
        baseIRI = Utils.getBaseDirectiveTurtleOrDefault(mappingStream2, defaultBaseIRI);
        // System.out.println(mapPath);
         QuadStore rmlStore = QuadStoreFactory.read(mappingStream);

         RecordsFactory factory = new RecordsFactory(mapPath,mappingfiledirectory);
         QuadStore outputStore = new RDF4JStore();
         Map<String, String> mappingOptions = new HashMap<>();
       //  mappingOptions.put("jdbcDriver", dbClassName);
         mappingOptions.put("username", username);
         mappingOptions.put("password", password);
         mappingOptions.put("jdbcDSN", CONNECTION);
         //String baseIRI = Utils.getBaseDirectiveTurtleOrDefault(mappingStream, defaultBaseIRI);
         MappingConformer mc = new MappingConformer(rmlStore,mappingOptions);
         mc.conform();
         Executor executor = new Executor(mc.getStore(), factory, outputStore, baseIRI, null);
        List <Term> tms=executor.getTriplesMaps();
        //System.out.println();
        MappingFactory f = new MappingFactory(null, baseIRI, StrictMode.BEST_EFFORT);
        File x;
       
        try { 
        	Path filePath = Paths.get(output);
        	Files.createDirectories(filePath.getParent());
        	x = new File(output);}catch (NullPointerException e){
         x = new File(mapPath+"/Datalog_rules"+".rs");
        }     
         x.createNewFile();

         FileWriter out = new FileWriter(x);
         for (Term a:tms) {
         	Mapping m =f.createMapping(a, rmlStore);
         	 

         	List<Record> lr=factory.createRecords(a, rmlStore);
         	List<Term> logicalSources = Utils.getObjectsFromQuads(rmlStore.getQuads(a, new NamedNode(NAMESPACES.RML2 + "logicalSource"), null));
         	Term logicalsource = logicalSources.get(0);
         	List<Term> table =Utils.getObjectsFromQuads(rmlStore.getQuads(logicalsource, new NamedNode(NAMESPACES.RML2 + "iterator"), null));
         	 tablename = table.get(0).stringValue().replaceAll(" ", "").replaceAll("`", "");
         	tablename = tablename.replaceAll("[\\r\\n\\t]+", "").trim();

         	if (tablename != null && tablename.toLowerCase().contains("select")) {
         	    Matcher matcher = Pattern.compile("(?i)\\bfrom\\b\\s+([A-Za-z_][A-Za-z0-9_$]*)").matcher(tablename);
         	    tablename = matcher.find() ? matcher.group(1) : "query";
         	}
         	 System.out.println(tablename);
         List<String>edbss=	generateEDBs (a,  lr,logicalsource,tablename);   
         if (!edbss.isEmpty()) {
        	 File xx;
        	 String path = Utils.getFile(x.getPath()).getParent();
        	 xx= new File(path+"/"+tablename+"_lt"+d_count+".facts");
            xx.createNewFile();
            FileWriter outt = new FileWriter(xx); 
             for (String s:edbss) {
             	outt.write(s+"\n");
             }
             outt.close();
         }
             
//         for(String s:edbs) {
//         	System.out.println(s);
//         }
  rules.addAll(GenerateMapRules(mapPath,rmlStore, m, lr, edbs, f,factory,base,logicalsource));
  if (d_count<jc_count) {
		 d_count=jc_count+1;
	 }else {
		 d_count++;
	 }
	 l_count++;

 }
    //     out.write(".functor  toIRI(x:symbol):symbol \n"); 
        // System.out.println(mapPath+"//Datalog_rules"+".rs");
//         File x;
//         try {  x = new File(output);}catch (NullPointerException e){
//          x = new File(mapPath+"/Datalog_rules"+".rs");
//         }
//          x.createNewFile();
//          FileWriter out = new FileWriter(x);
          
  for (String s:declarations) {
 	 out.write(s+"\n");
  }
  out.flush();
  for (String h:rules) {
  	out.write(h+"\n");
  }
  out.flush();
  out.close();
  System.out.println("✔ Translation completed. Datalog program and facts files are written to: " + Utils.getFile(x.getPath()).getParent());
          }

    public static LinkedHashSet<String> GenerateMapRules(String mapPath,QuadStore qs,Mapping h,List<Record>lr,List<String>edbs,MappingFactory f, RecordsFactory factory,boolean base, Term logicalsource) throws Exception{
     	LinkedHashSet<String>rules= new LinkedHashSet<String>();
    	 graph_predicates.clear();
    	 term_predicates2.clear();
    	maps_po.clear();
  	maps_join.clear();
    	graph_terms.clear();
    	link_graph.clear();
    	joins.clear();
    	vars.clear();
    	subj_map2.clear();
    	datatypes.clear();
  generateSubjectRules (qs,h,lr,edbs,f,base);
  rules.addAll(generatePredicateRules (qs,h,lr,edbs));
  rules.addAll(generateObjectRules (mapPath,qs,h,f,lr,edbs,factory,base));
  rules.addAll(generateSubjectTermtypeRule(qs,h, f,base,logicalsource));
  rules.addAll(generatePredicateObjectTermtypeRule(qs,h,f,lr,base,logicalsource));
  rules.addAll(generateGraphTermtypeRule(qs,h, f,base));
  rules.addAll(generateClassRules(qs,h,edbs));
  rules.addAll( generateTriplesRules(h));
  rules.addAll( generateTriplesJoinRules(h));
  rules.add(".decl triple(s:symbol,p:symbol,o:symbol)");
  rules.add(".decl quadruple(s:symbol,p:symbol,o:symbol,g:symbol)");
  rules.add(".output triple");
  rules.add(".output quadruple");
 // for (String r :rules) {
// 	 System.out.println(r);
 // }
  return rules;
     }
    public static List<String> generateEDBs (Term tm, List<Record> lr, Term logicalsource, String tablename) throws Exception{
     	List<String> EDBs = new LinkedList<String>();
     	String decl="";
     	Boolean found = false;
     	Set<String>s = ((CSVRecord) lr.get(0)).getData().keySet();
    	 schema= new LinkedList<String>(s);
    	 schema.remove("key");
     	 if (ls.containsKey(logicalsource)) {
     	 	d_count=ls.get(logicalsource);
     	 	found=true;
     	 }
     	 
     	 if (!found) {
         for (int i=0; i<lr.size();i++) {
         	 CSVRecord rr = (CSVRecord) lr.get(i);
         	 String pred ="";
         	decl=".decl "+tablename+"_lt"+d_count+"(";
 for (int j=0;j<schema.size()-1;j++) {
 	decl=decl+schema.get(j).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+":symbol, ";
 		pred =pred+rr.get(schema.get(j)).toString().replace("[", "").replace("]", "	");
 	}
 decl=decl+schema.get(schema.size()-1).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+":symbol)";
 	pred =pred+rr.get(schema.get(schema.size()-1)).toString().replace("[", "").replace("]", "	");
 EDBs.add(pred);
         }
  int temp=d_count;
  ls.put(logicalsource, temp);
  declarations.add(decl);
  declarations.add(".input "+tablename+"_lt"+d_count);
     	 }
 		return EDBs;
     }
     
     public static List<String> generateEDBs2 (Term tm, List<Record> lr, Term logicalsource, String tablename) throws Exception{
     	List<String> EDBs = new LinkedList<String>();
     	 Set<String>s = ((CSVRecord) lr.get(0)).getData().keySet();
     	String decl="";
     	Boolean found = false; 
     	 if (ls.containsKey(logicalsource)) {
     	 	jc_count=ls.get(logicalsource);
     	 	found=true;
     	 }else {
     	 	jc_count=d_count+1;
     	 }
   	 schema2= new LinkedList<String>(s);
 	 //System.out.println(schema2);
   	schema2.remove("key");
   	 if (!found) {
         for (int i=0; i<lr.size();i++) {
         	 CSVRecord rr = (CSVRecord) lr.get(0);  	    	        	         
         	decl=".decl "+tablename+"_lt"+jc_count+"(";
         	 String pred="";
         	 for (int j=0;j<schema2.size()-1;j++) {
         			decl=decl+schema2.get(j).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+":symbol, ";
         				pred =pred+rr.get(schema2.get(j)).toString().replace("[", "").replace("]", "	");
         		}
         	 decl=decl+schema2.get(schema2.size()-1).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+":symbol)";
         			pred =pred+lr.get(i).get(schema2.get(schema2.size()-1)).toString().replace("[", "").replace("]", "	");
  EDBs.add(pred);
 	 int temp=jc_count;
 	 ls.put(logicalsource, temp);
 	 declarations.add(decl);
 	declarations.add(".input "+tablename+" lt"+jc_count);
 	}

 }
 		return EDBs;
     }
     public static void generateTermrules2 (MappingInfo ff,QuadStore qs,Mapping h,List<Record>lr) throws Exception{
       	variables2 ="";
       	variables22 ="";
       	variablesdec2 ="";
         	 for (int i=0; i<schema2.size()-1;i++) {
         		 variables2= variables2 + schema2.get(i).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+", ";
         		variables22= variables22 + "z"+i+", ";
         		variablesdec2= variablesdec2 + "z"+i+":symbol, ";
      			 }
         	 variables2 = variables2+ schema2.get(schema2.size()-1).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "");
         	variables22 = variables22+ "z"+(schema2.size()-1);
         	// variablesdec2= "x:symbol";
         	variablesdec2= variablesdec2 + "z"+(schema2.size()-1)+":symbol";
         	 for (Quad q:qs.getQuads(ff.getTerm(), null, null)) {
            if (q.getPredicate().getValue().contains("template")) {
           	 String temp = generateTemplate2(Utils.parseTemplate(q.getObject().getValue(), false)); 
                String predicate2=temp+", "+variables2+")";
               term_predicates2.put(ff.getTerm(), predicate2);
          	}
            else if (q.getPredicate().getValue().contains("reference")) {
           	term_predicates2.put(ff.getTerm(), schema2.get(schema2.indexOf(q.getObject().getValue())).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+", "+variables2+")");   
            }
            else if (q.getPredicate().getValue().contains("constant")) {
           	term_predicates2.put(ff.getTerm(), "\""+q.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables2+")");
            }
       }
       }
       public static String generateTemplate(List<Extractor> l) {
       	String base= "cat(";
       	String temp = base;
       	LinkedList<String>vars= new LinkedList<String>();
       	int count =1;
       	for (Extractor e: l){
       		if (e.toString().startsWith("ReferenceExecutor that works with ")) {
       			String a = e.toString().replace("ReferenceExecutor that works with ", "");
       			//vars.add("@toIRI(x"+schema.indexOf(a)+")");
       			vars.add(schema.get(schema.indexOf(a)).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", ""));
       			
       		}else {
       			vars.add(e.toString());
       		}
       	}
       	if (vars.size()==2) {
       		temp=temp+vars.getFirst()+","+vars.getLast()+")";
       	}else if (vars.size()==1){
       		temp=vars.getFirst();
       	}else {
   for (int i=0;i<vars.size();i++) {
   	if (i==0) {
   		temp=temp+vars.get(i)+",";
   	}else
   	if (vars.size()-i>1) {
   		temp= temp+base+vars.get(i)+",";
   		count+=1;
   	}else {
   		temp=temp+vars.get(i);
   	}
   }
   for (int i=0;i<count;i++) {
   	temp=temp+")";
   }
       	}
       	temp=temp.replaceAll(" ", "");
       		return temp;
       	}
       public static String generateTemplate2(List<Extractor> l) {
       	String base= "cat(";
       	String temp = base;
       	LinkedList<String>vars= new LinkedList<String>();
       	int count =1;
       	for (Extractor e: l){
       		if (e.toString().startsWith("ReferenceExecutor that works with ")) {
       			String a = e.toString().replace("ReferenceExecutor that works with ", "");
       			//vars.add("@toIRI(z"+schema2.indexOf(a)+")");
       			vars.add(schema2.get(schema2.indexOf(a)).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", ""));
       		}else {
       			vars.add(e.toString());
       		}
       	}
       	if (vars.size()==2) {
       		temp=temp+vars.getFirst()+","+vars.getLast()+")";
       	}else if (vars.size()==1){
       		temp=vars.getFirst();
       	}else {
   for (int i=0;i<vars.size();i++) {
   	if (i==0) {
   		temp=temp+vars.get(i)+",";
   	}else
   	if (vars.size()-i>1) {
   		temp= temp+base+vars.get(i)+",";
   		count+=1;
   	}else {
   		temp=temp+vars.get(i);
   	}
   }
   for (int i=0;i<count;i++) {
   	temp=temp+")";
   }
       	}
       	temp=temp.replaceAll(" ", "");
       		return temp;
       	}
       
       public static void generateTermrules(MappingInfo ff,PredicateObjectGraphMapping ff2, QuadStore qs,Mapping h,List<Record>lr) throws Exception{
       	variables ="";
       	variablesdec="";
         	 for (int i=0; i<schema.size()-1;i++) {
         		 variables= variables + schema.get(i).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+", ";
         		variablesdec= variablesdec + "x"+i+":symbol, ";
      			 }
         	 variables = variables+ schema.get(schema.size()-1).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "");
         	variablesdec= variablesdec + "x"+(schema.size()-1)+":symbol";
        	//variablesdec= "x:symbol";
         	 for (Quad q:qs.getQuads(ff.getTerm(), null, null)) {
            if (q.getPredicate().getValue().contains("template")) {
           	 String temp = generateTemplate(Utils.parseTemplate(q.getObject().getValue(), false)); 
                String predicate2=temp+", "+variables+")";
               term_predicates2.put(ff.getTerm(), predicate2);
          	}
            else if (q.getPredicate().getValue().contains("reference")) {
           	term_predicates2.put(ff.getTerm(), schema.get(schema.indexOf(q.getObject().getValue())).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+", "+variables+")");         	  
            }
            else if (q.getPredicate().getValue().contains("constant")) {
           	term_predicates2.put(ff.getTerm(), "\""+q.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
            }else if (q.getPredicate().getValue().contains("graphMap")) {
           	 List <Quad> lq = qs.getQuads(q.getSubject(), q.getPredicate(), null);
      for (Quad qqs: lq) {     	 
           	 Quad qq=qs.getQuad(qqs.getObject(), null, null);
   String gval2=qq.getPredicate().getValue();
   if (gval2.contains("template")) {
   	 String temp = generateTemplate(Utils.parseTemplate(qq.getObject().getValue(), false)); 
       String predicate2=temp+", "+variables+")";
      term_predicates2.put(qq.getSubject(), predicate2); 
   	if (!graph_terms.containsKey(q.getSubject())) {
   		LinkedList<Term>a= new LinkedList<Term>();
   		a.add(q.getObject());
   	graph_terms.put(q.getSubject(), a);
   	}else {
   		graph_terms.get(q.getSubject()).add(q.getObject());
   	}
   	}
   else if (gval2.contains("reference")) {
   	if (!graph_terms.containsKey(ff.getTerm())) {
   		LinkedList<Term>a= new LinkedList<Term>();
   		a.add(q.getObject());
   	graph_terms.put(q.getSubject(), a);
   	}else {
   		graph_terms.get(q.getSubject()).add(q.getObject());
   	}
   	term_predicates2.put(qq.getSubject(), schema.get(schema.indexOf(qq.getObject().getValue())).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+", "+variables+")");	  
   }
   else if (gval2.contains("constant")&&!(qq.getObject().getValue().equals("http://w3id.org/rml/defaultGraph"))) {
   	if (!graph_terms.containsKey(ff.getTerm())) {
   		LinkedList<Term>a= new LinkedList<Term>();
   		a.add(q.getObject());
   	graph_terms.put(q.getSubject(), a);
   	}else {
   		graph_terms.get(q.getSubject()).add(q.getObject());
   	}
   	term_predicates2.put(qq.getSubject(), "\""+qq.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
   }
      }
            }
            
            try {
        if (qs.getQuad(null, null, ff.getTerm()).getPredicate().getValue().contains("predicateMap")) {
           	 List <Quad> lq = qs.getQuads(ff2.getGraphMappingInfo().getTerm(), null, null);
           	   for (Quad qqs: lq) {
           		   if (qqs.getPredicate().getValue().contains("graphMap")) {
           	        	 Quad qq=qs.getQuad(qqs.getObject(), null, null);
           	String gval2=qq.getPredicate().getValue();
           	if (gval2.contains("template")) {
           		 String temp = generateTemplate(Utils.parseTemplate(qq.getObject().getValue(), false)); 
           	    String predicate2=temp+", "+variables+")";
           	  term_predicates2.put(qq.getSubject(), predicate2);
           		if (!graph_terms.containsKey(ff.getTerm())) {
           			LinkedList<Term>a= new LinkedList<Term>();
           			a.add(qqs.getObject());
           		graph_terms.put(ff.getTerm(), a);
           		}else {
           			graph_terms.get(ff.getTerm()).add(qqs.getObject());
           		}
           		}
           	else if (gval2.contains("reference")) {
           		if (!graph_terms.containsKey(ff.getTerm())) {
           			LinkedList<Term>a= new LinkedList<Term>();
           			a.add(qqs.getObject());
           		graph_terms.put(ff.getTerm(), a);
           		}else {
           			graph_terms.get(ff.getTerm()).add(qqs.getObject());
           		}
           		term_predicates2.put(qq.getSubject(), schema.get(schema.indexOf(qq.getObject().getValue())).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+", "+variables+")");
           	}
           	else if (gval2.contains("constant")&&!(qq.getObject().getValue().equals("http://w3id.org/rml/defaultGraph"))) {
           		if (!graph_terms.containsKey(ff.getTerm())) {
           			LinkedList<Term>a= new LinkedList<Term>();
           			a.add(qqs.getObject());
           		graph_terms.put(ff.getTerm(), a);
           		}else {
           			graph_terms.get(ff.getTerm()).add(qqs.getObject());
           		}
           		term_predicates2.put(qq.getSubject(), "\""+qq.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
           	}
           	   } 
           	   }
            }}catch (NullPointerException e) {
            }
         	 }
       }
      public static String getLangaugeTag(QuadStore q, MappingInfo ff) {
  		for (Quad qq:q.getQuads(null, null, null)){
  			if (qq.getSubject().equals(ff.getTerm())&&qq.getPredicate().getValue().equals("http://w3id.org/rml/language")) {
  					return (qq.getObject().getValue());
  				}
  				
  			}
  		return "";
  		
  	}
      
      public static void generateSubjectRules (QuadStore qs,Mapping h,List<Record>lr,List<String>edbs,MappingFactory f, boolean base) throws Exception{
       	MappingInfo ff = h.getSubjectMappingInfo();
       	 generateTermrules(ff,null, qs, h, lr);
       	 
       }
       
      public static HashSet<String> generateSubjectTermtypeRule(QuadStore qs, Mapping h, MappingFactory f, boolean base, Term logicalsource){
         	HashSet<String> al= new HashSet<String>();
      	   String rule2="";
            	List<Term> termTypes = Utils.getObjectsFromQuads(qs.getQuads(h.getSubjectMappingInfo().getTerm(), new NamedNode("http://w3id.org/rml/termType"), null));
            	String termtype ="";
            	
            	if (termTypes.contains(new NamedNode("http://w3id.org/rml/BlankNode"))) {
            		termtype="http://w3id.org/rml/BlankNode";
            	}else {
            		termtype="http://w3id.org/rml/IRI";
            	}
            	String head=term_predicates2.get(h.getSubjectMappingInfo().getTerm());
            	if (!donesubj.containsKey(head)||!termtype.equals(donesubjtermtypes.get(head))||!logicalsource.getValue().equals(tablesterms.get(head))) {
            		String dec= ".decl "+"Subject"+l_count+"_"+"lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
                 	declarations.add(dec);
         	 if (termtype.equals("http://w3id.org/rml/BlankNode")) {
         		
         		String[]s2= head.split(", ");
         		 rule2 = "Subject"+l_count+"_"+"lt"+d_count+"("+"cat(\"_:\","+s2[0]+"), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
         		subj_map="Subject"+l_count+"_"+"lt"+d_count+"("+"s, "+variables+")";
         		al.add(rule2);
         	} else {
     		String[]s2= head.split(", ");
     		String ma=term_predicates2.get(h.getSubjectMappingInfo().getTerm());
            if (ma.contains("\"https")||ma.contains("\"http")||!base) {
                 rule2 = "Subject"+l_count+"_"+"lt"+d_count+"("+ "cat(\"<\",cat("+s2[0]+",\">\")), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
            }else {
                rule2 = "Subject"+l_count+"_"+"lt"+d_count+"("+ "cat(cat(\"<\",cat(\""+baseIRI+"\","+s2[0]+"))"+",\">\"), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
            }
     		subj_map="Subject"+l_count+"_"+"lt"+d_count+"("+"s, "+variables+")";
     		al.add(rule2);
     }
         	 donesubj.put(head, subj_map);
         	 donesubjtermtypes.put(head, termtype);
         	 tablesterms.put(head, logicalsource.getValue());
            	}else {
            		subj_map=donesubj.get(head);
            	}
     return al;
         }
         
         public static void generateSubjectTermtypeRule2 (QuadStore qs, Mapping h, MappingFactory f,boolean base){
     		String head=term_predicates2.get(h.getSubjectMappingInfo().getTerm());
     		String[]s2= head.split(", ");
            subj_map2.put(h.getSubjectMappingInfo().getTerm(),"Subject"+jc_count+"_"+"lt"+jc_count+"("+"s2, "+variables22+")");
         }
         
         public static HashSet<String> generateGraphTermtypeRule (QuadStore qs, Mapping h, MappingFactory f, boolean base){
         	HashSet<String> al= new HashSet<String>();
         	for (Term g :graph_terms.keySet()) {
         		for (Term t:graph_terms.get(g)) {
         		String head=term_predicates2.get(t);
         		String[]s2= head.split(", ");
         		String ma=term_predicates2.get(t);
             	String dec= ".decl "+"Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
             	declarations.add(dec);
             	String rule2="";
                if (ma.contains("\"https")||ma.contains("\"http")||!base) {
                 rule2 = "Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+"cat(\"<\",cat("+s2[0]+",\">\")), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
               }else {
                rule2 = "Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+"cat(cat(\"<\",cat(\""+baseIRI+"\","+s2[0]+"))"+",\">\"), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
               }
         		if (!graph_predicates.containsKey(g)) {
         			LinkedList<String> a = new LinkedList<String>();
         			a.add("Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+"g, "+variables+")");
         		graph_predicates.put(g, a);
         		}else {
         			graph_predicates.get(g).add("Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+"g, "+variables+")");
         		}
         		
         		al.add(rule2);
         	}
         		g_count++;		
         		
         }
     		return al;
         	}
         public static LinkedHashSet<String> generatePredicateObjectTermtypeRule (QuadStore q, Mapping h, MappingFactory f, List<Record> lr, boolean base, Term logicalsource) throws Exception{
            	LinkedHashSet<String> al= new LinkedHashSet<String>();
            	int i=0;
            	for (PredicateObjectGraphMapping pog :h.getPredicateObjectGraphMappings()) {
            		try {
            			if (!h.getSubjectMappingInfo().getTerm().equals(pog.getObjectMappingInfo().getTerm())){
            	    	String map="";
            			
            		String head=term_predicates2.get(pog.getPredicateMappingInfo().getTerm());
            		String[]s2= head.split(", ");
            		if (!donepreds.containsKey(head)||!logicalsource.getValue().equals(tablesterms.get(head))) {
                	String dec= ".decl "+"Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
                	declarations.add(dec);
                	String ma=term_predicates2.get(pog.getPredicateMappingInfo().getTerm());
                   String rule2="";
                   if (ma.contains("\"https")||ma.contains("\"http")||!base) {
                        rule2 = "Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+"cat(\"<\",cat("+s2[0]+",\">\")), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
                   }else {
                       rule2 = "Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+"cat(cat(\"<\",cat(\""+baseIRI+"\","+s2[0]+"))"+",\">\"), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
                   }
            		map="Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+"p, "+variables+")";
            		al.add(rule2);
            		donepreds.put(head, map);
            		tablesterms.put(head, logicalsource.getValue());
            			}else {
            		map=donepreds.get(head);
            			}
                		Term t = pog.getObjectMappingInfo().getTerm();               	  
                		List<Term> termTypes = Utils.getObjectsFromQuads(q.getQuads(t, new NamedNode("http://w3id.org/rml/termType"), null));
                		String head2=term_predicates2.get(t);
                		String tt ="";
      				List<Quad> lq=q.getQuads(t, null, null, null);
        				Boolean fo=false;
        				Boolean fo2=false;
        				for (Quad qqq:lq) {
        					if (qqq.getPredicate().getValue().contains("http://w3id.org/rml/template")) {
        						fo =true;
        						break;
        					}else if (qqq.getPredicate().getValue().contains("http://w3id.org/rml/constant")) {
        						fo2=true;
        					}
        				}
                		if (termTypes.isEmpty()) {
                			if (fo||fo2) {
                			tt= "http://w3id.org/rml/IRI";
                			}else {
                				tt= "http://w3id.org/rml/Literal";	
                			}
                		}
            			if (!doneobjs.containsKey(head2)||!donetermtypes.get(head2).equals(tt)||!logicalsource.getValue().equals(tablesterms.get(head2))) {
            				String dec= ".decl "+"Object"+l_count+""+i+"_"+"lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
                        	declarations.add(dec);
            		if (termTypes.contains(new NamedNode("http://w3id.org/rml/IRI"))||(fo&&termTypes.isEmpty())||fo2) {
            			//String head2=term_predicates2.get(t);
            			String[]s22= head2.split(", ");
                		String ma=term_predicates2.get(t);
                		String rule22="";
                       if (ma.contains("\"https")||ma.contains("\"http")||!base) {
                        rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+"cat(\"<\",cat("+s22[0]+",\">\")), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
                      }else {
                       rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+"cat(cat(\"<\",cat(\""+baseIRI+"\","+s22[0]+"))"+",\">\"), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
                      }
                		map=map+", "+"Object"+l_count+""+i+"_"+"lt"+d_count+"("+"o, "+variables+")";
                		doneobjs.put(head2, "Object"+l_count+""+i+"_"+"lt"+d_count+"("+"o, "+variables+")");
                		donetermtypes.put(head2, "http://w3id.org/rml/IRI");
                		al.add(rule22);
            	}else if (termTypes.contains(new NamedNode("http://w3id.org/rml/BlankNode"))) {
            		//String head2=term_predicates2.get(t);
        			String[]s22= head2.split(", ");
            		String rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+"cat(\"_:\","+s22[0]+"), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
            		map=map+", "+"Object"+l_count+""+i+"_"+"lt"+d_count+"("+"o, "+variables+")";
            		doneobjs.put(head2, "Object"+l_count+""+i+"_"+"lt"+d_count+"("+"o, "+variables+")");
            		donetermtypes.put(head2, "http://w3id.org/rml/BlankNode");
            		al.add(rule22);
            	}else {
               	 String lantag= getLangaugeTag(q, pog.getObjectMappingInfo());
            		//String head2=term_predicates2.get(t);
        			String[]s22= head2.split(", ");
            		String rule22="";
            		if (lantag.equals("")) {
            			if (datatypes.containsKey(t)) {
            					rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+"cat(cat(\"\\\"\",cat("+s22[0]+",\"\\\"\")), \"^^<"+datatypes.get(t)+">\""+"), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
            			}else {
            		 rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+"cat(\"\\\"\",cat("+s22[0]+",\"\\\"\"))," +variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
            			}}else {
            			rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+ "cat(cat(\"\\\"\",cat("+s22[0]+",\"\\\"\")),\"@"+lantag+"\""+"), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
            		}
            		map=map+", "+"Object"+l_count+""+i+"_"+"lt"+d_count+"("+"o, "+variables+")";
            		doneobjs.put(head2, "Object"+l_count+""+i+"_"+"lt"+d_count+"("+"o, "+variables+")");
            		donetermtypes.put(head2, "http://w3id.org/rml/Literal");
            		al.add(rule22);
            	}
            		tablesterms.put(head2, logicalsource.getValue());
            			}
            			else {
            				map=map+", "+doneobjs.get(head2);
            			}
            		if (maps_po.containsKey(pog.getPredicateMappingInfo().getTerm())) {
            		maps_po.get(pog.getPredicateMappingInfo().getTerm()).add(map);
            		}else {
            			maps_po.put(pog.getPredicateMappingInfo().getTerm(), new LinkedList<String>());
            			maps_po.get(pog.getPredicateMappingInfo().getTerm()).add(map);
            		}
            		i++;
            }
            		}catch(NullPointerException e) {
            	String map="";
        		String head=term_predicates2.get(pog.getPredicateMappingInfo().getTerm());
        		Term ts=q.getQuad(pog.getParentTriplesMap(), new NamedNode("http://w3id.org/rml/subjectMap"), null).getObject();
        		String[]s2= head.split(", ");
            	String dec= ".decl "+"Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
            	declarations.add(dec);
            	String ma=term_predicates2.get(pog.getPredicateMappingInfo().getTerm());
            	String rule2="";
               if (ma.contains("\"https")||ma.contains("\"http")||!base) {
                    rule2 = "Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+"cat(\"<\",cat("+s2[0]+",\">\")), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
               }else {
                   rule2 = "Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+"cat(cat(\"<\",cat(\""+baseIRI+"\","+s2[0]+"))"+",\">\"), "+variables+")"+" :- "+ tablename+"_lt"+d_count+"("+ variables+").";
               }
        		map="Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+"p, "+variables+")";
        		al.add(rule2);
        		map=map+", "+subj_map2.get(ts);
          		if (maps_join.containsKey(ts)) {
            		maps_join.get(ts).add(map);
            		}else {
            			maps_join.put(ts, new LinkedList<String>());
            			maps_join.get(ts).add(map);
            		}
            			link_graph.put(ts, pog.getPredicateMappingInfo().getTerm());
            		
        		i++;
            }
            		}
            	return al;
            }
         
         public static LinkedHashSet<String> generatePredicateRules (QuadStore qs,Mapping h,List<Record>lr,List<String>edbs) throws Exception{
         	LinkedHashSet<String> rules = new LinkedHashSet<String>();
         	List<PredicateObjectGraphMapping> l = h.getPredicateObjectGraphMappings();
         	for (PredicateObjectGraphMapping pogm:l) {
         		//MappingInfo ff2 =pogm.getGraphMappingInfo();
         		MappingInfo ff =pogm.getPredicateMappingInfo();
         		generateTermrules(ff,pogm, qs, h, lr);
         	}
     		return rules;
         	
         }
         public static LinkedHashSet<String> generateObjectRules (String mapPath,QuadStore qs,Mapping h, MappingFactory f,List<Record>lr,List<String>edbs,RecordsFactory factory, boolean base) throws Exception{
          	LinkedHashSet<String> rules = new LinkedHashSet<String>();
          	List<PredicateObjectGraphMapping> l = h.getPredicateObjectGraphMappings();
          	for (PredicateObjectGraphMapping pogm:l) {
          		try {
          			//MappingInfo ff2 =pogm.getGraphMappingInfo();
              		MappingInfo ff =pogm.getObjectMappingInfo();
              		generateTermrules(ff,pogm, qs, h, lr);
              		try { 
              		//	Quad qqs=qs.getQuad(ff.getTerm(), null,null);
              			Quad qq=qs.getQuad(ff.getTerm(), new NamedNode("http://w3id.org/rml/reference"),null);
              			String datatype = lr.get(0).getDataType(qq.getObject().getValue());        			
                     	if (!datatype.equals(null)) {
                  		datatypes.put(pogm.getObjectMappingInfo().getTerm(), datatype);
                  	}
              		}catch (java.lang.Exception s) {
              		}

          		}
          			catch(NullPointerException e) {
          				rules.addAll(generateJoinRules(mapPath,pogm, qs, f, edbs, pogm.getParentTriplesMap(), factory,base));
          			}

          	}
      		return rules;
          }
     public static LinkedHashSet<String> generateJoinRules(String mapPath,PredicateObjectGraphMapping pogm,QuadStore q, MappingFactory f,List<String>edbs,Term t, RecordsFactory factory, boolean base) throws Exception{
     	LinkedHashSet<String>rules= new LinkedHashSet<String>();
     	String join="";
     	Mapping m =f.createMapping(t, q);
     	List<Record> lr=factory.createRecords(t, q);
     	if (!pogm.getJoinConditions().isEmpty()) {
    		List<Term> logicalSources = Utils.getObjectsFromQuads(q.getQuads(t, new NamedNode(NAMESPACES.RML2 + "logicalSource"), null));
         	Term logicalsource = logicalSources.get(0);
            for (Quad qq:q.getQuads(null, null, null, null)) {
              	 System.out.println(qq.getSubject()+" "+qq.getPredicate()+" "+qq.getObject());
                }
         	List<Term> table =Utils.getObjectsFromQuads(q.getQuads(logicalsource, new NamedNode(NAMESPACES.RML2 + "iterator"), null));
        	 tablename2 = table.get(0).stringValue().replaceAll(" ", "").replaceAll("`", "");
        	tablename2 = tablename2.replaceAll("[\\r\\n\\t]+", "").trim();

        	if (tablename2 != null && tablename2.toLowerCase().contains("select")) {
        	    Matcher matcher = Pattern.compile("(?i)\\bfrom\\b\\s+([A-Za-z_][A-Za-z0-9_$]*)").matcher(tablename2);
        	    tablename2 = matcher.find() ? matcher.group(1) : "query";
        	}
        	 System.out.println(tablename2);
           List<String>edbss=	generateEDBs2 (t,  lr,logicalsource,tablename2);
            if (!edbss.isEmpty()) {
           	 File xx;
            	 xx= new File(mapPath+"/lt"+jc_count+".facts");
               xx.createNewFile();
               FileWriter outt = new FileWriter(xx);
                for (String s:edbss) {
                	outt.write(s+"\n");
                }
                outt.close();
            }
            generateTermrules2(m.getSubjectMappingInfo(), q, m, lr);
            vars.put(m.getSubjectMappingInfo().getTerm(), variables2);
            generateSubjectTermtypeRule2(q,m, f,base);
            for (Quad qq:q.getQuads(null, null, null, null)) {
          	  if ((qq.getPredicate().getValue().equals("http://w3id.org/rml/parentTriplesMap"))&&(qq.getObject().getValue().equals(t.getValue()))) {
          		 Term po=qq.getSubject();
          		 int i=0;
          		 for (Quad qw: q.getQuads(po, new NamedNode("http://w3id.org/rml/joinCondition"), null)) {
          		 Term jc = qw.getObject();
          		String parent =q.getQuad(jc, new NamedNode("http://w3id.org/rml/parent"), null).getObject().getValue();
          		String child =q.getQuad(jc, new NamedNode("http://w3id.org/rml/child"), null).getObject().getValue();
              	String dec1= ".decl "+"eval_jcc_"+jc.getValue()+"("+variablesdec+", "+"y"+schema.indexOf(child)+":symbol)";
              	String dec= ".decl "+"eval_jcp_"+jc.getValue()+"("+variablesdec2+", "+"y"+schema2.indexOf(parent)+":symbol)";
              	declarations.add(dec1);  
              	declarations.add(dec);  
          		String rule1= "eval_jcc_"+jc.getValue()+"("+schema.get(schema.indexOf(child)).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+", "+variables+") :- "+ tablename+"_lt"+l_count+"("+ variables+").";
          		String rule12= "eval_jcp_"+jc.getValue()+"("+schema2.get(schema2.indexOf(parent)).toLowerCase().replace("(", "_").replace(")", "").replaceAll(" ", "")+", "+variables2+") :- "+ tablename2+"_lt"+jc_count+"("+ variables2+").";
          		join=join+", eval_jcc_"+jc.getValue()+"("+"v"+i+", "+variables+"), "+"eval_jcp_"+jc.getValue()+"("+"v"+i+", "+variables22+")";
          		rules.add(rule12);
          		rules.add(rule1);
          		i++;
          	  }
          		 joins.put(m.getSubjectMappingInfo().getTerm(),join);
          	  }
            }
              	}else {
              		  generateTermrules(m.getSubjectMappingInfo(),null, q, m, lr);
              		  generateSubjectTermtypeRule2(q,m, f,base);
              	}
          	return rules;
              }
              public static LinkedHashSet<String> generateClassRules(QuadStore q, Mapping h,List<String>edbs){
              	LinkedHashSet<String>rules= new LinkedHashSet<String>();
              	for (Quad qq:q.getQuads(null, null, null,null)) {
              		if ((qq.getPredicate().getValue().equals("http://w3id.org/rml/class"))&&(qq.getSubject().getValue().equals(h.getSubjectMappingInfo().getTerm().getValue()))){
              	 		if (graph_terms.containsKey(h.getSubjectMappingInfo().getTerm())) {
              	 			for (String m:graph_predicates.get(h.getSubjectMappingInfo().getTerm())) {
                  			String rule = "quadruple(s, \"<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>\", "+"\"<"+qq.getObject().getValue()+">\""+", g) :- "+subj_map+", "+
                  					m+".";
                  			rules.add(rule);
                  	}}else {
                  			String rule = "triple(s, \"<http://www.w3.org/1999/02/22-rdf-syntax-ns#type>\", "+"\"<"+qq.getObject().getValue()+">\""+") :- "+subj_map+
                  					".";
                  			rules.add(rule);
                  	}
              			
              		}
              	}
          		return rules;
              }
              
              public static LinkedHashSet<String> generateTriplesRules(Mapping h){
              	LinkedHashSet<String>rules= new LinkedHashSet<String>();
              	Boolean gr=false; 
              		if (graph_terms.containsKey(h.getSubjectMappingInfo().getTerm())) {
              			gr=true;
              		for (Term tt :maps_po.keySet()) {
              			for (String k:maps_po.get(tt)) {
              				for (String m:graph_predicates.get(h.getSubjectMappingInfo().getTerm())) {
              			String rule = "quadruple(s,p,o,g) :- "+subj_map+", "+ k+", "+m+".";
              			rules.add(rule);
              		}
              			}
              	}
              		}
              		for (Term tt2 :maps_po.keySet()) {
              			for (String k:maps_po.get(tt2)) {
              			if (graph_terms.containsKey(tt2)) {
              				for (String m:graph_predicates.get(tt2)) {
              				String rule = "quadruple(s,p,o,g) :- "+subj_map+", "+ k+", "+m+".";
                  			rules.add(rule);
              			}
              			}else if (gr==false) {
              				String rule = "triple(s,p,o) :- "+subj_map+", "+ k+".";
                  			rules.add(rule);
              			}
              		}
              		}
          		return rules;
              }
              public static LinkedHashSet<String> generateTriplesJoinRules(Mapping h){
              	LinkedHashSet<String>rules= new LinkedHashSet<String>();
              	Boolean gr=false; 
              		if (graph_terms.containsKey(h.getSubjectMappingInfo().getTerm())) {
              			gr=true;
              		for (Term tt :subj_map2.keySet()) {
              			for (String k:maps_join.get(tt)) {
              			if (!joins.containsKey(tt)) {
              				for (String m:graph_predicates.get(h.getSubjectMappingInfo().getTerm())) {
              			String rule = "quadruple(s,p,s2,g) :- "+subj_map+", "+ k+", "+m+".";
              			rules.add(rule);
              		}}else {
              			for (String m:graph_predicates.get(h.getSubjectMappingInfo().getTerm())) {
              			String rule = "quadruple(s,p,s2,g) :- "+subj_map+", "+ k+joins.get(tt)+", "+m+".";
              			rules.add(rule);
              		}
              		}
              	}
              		}
              		}
              		for (Term tt2 :subj_map2.keySet()) {
              			Term tp = link_graph.get(tt2);
              			if (graph_terms.containsKey(tp)) {
              				for (String k:maps_join.get(tt2)) {
              				if (!joins.containsKey(tt2)) {
              					for (String m:graph_predicates.get(tp)) {
              	    			String rule = "quadruple(s,p,s2,g) :- "+subj_map+", "+ k+", "+m+".";
              	    			rules.add(rule);
              	    		}
              				}else {
              					for (String m:graph_predicates.get(tp)) {
              	    			String rule = "quadruple(s,p,s2,g) :- "+subj_map+", "+ k+joins.get(tt2)+", "+m+".";
              	    			rules.add(rule);
              	    		}
              				}
              			}
              			}else if (gr==false) {
              				for (String k:maps_join.get(tt2)) {
              				if (!joins.containsKey(tt2)) {
              	    			String rule = "triple(s,p,s2) :- "+subj_map+", "+k+".";
              	    			rules.add(rule);
              	    		}else {
              	    			String rule = "triple(s,p,s2) :- "+subj_map+", "+ k+joins.get(tt2)+".";
              	    			rules.add(rule);
              	    		}
              			}
              			}
              		}
              
              		return rules;
          }
       }
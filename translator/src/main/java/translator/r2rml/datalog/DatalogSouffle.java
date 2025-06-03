package translator.r2rml.datalog;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;
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

import be.ugent.idlab.knows.dataio.record.CSVRecord;
import be.ugent.idlab.knows.dataio.record.Record;
import be.ugent.rml.Executor;
import be.ugent.rml.Mapping;
import be.ugent.rml.MappingFactory;
import be.ugent.rml.MappingInfo;
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

public class DatalogSouffle {
    static int d_count =0;
    static int g_count =0;
    static int jc_count =0;
    static int l_count =0;
    static  List<String> schema;
    static  List<String> schema2;
    static   String subj_map = ""; 
    static LinkedHashSet<String> declarations = new LinkedHashSet<String>();
    static  HashMap<CSVRecord,Integer> ls = new HashMap<CSVRecord,Integer>(); 
    static  HashMap<Term,String> subj_map2 = new HashMap<Term, String>(); 
    static  HashMap<Term, String>term_predicates = new HashMap<Term,String>();
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
    static String variablesdec ="";
    static String variablesdec2 ="";
    static final String defaultBaseIRI = "http://example.com/base";
    static String baseIRI="";

	private static final String dbClassName = "com.mysql.cj.jdbc.Driver";
    private static final String CONNECTION = "jdbc:mysql://127.0.0.1/r2rml";
    public static void exec_dlog(String mappingfiledirectory, boolean base,String output) throws Exception {
        //String CONNECTION =  "jdbc:mysql://localhost:3306/r2rml";
         //System.out.println(dbClassName);
            // Class.forName(xxx) loads the jdbc classes and
            // creates a drivermanager class factory
//            Class.forName(dbClassName);

        //     Properties for user and password. Here the user and password are both 'paulr'
            Properties p = new Properties();
//            p.put("user",username);
//            p.put("password",password);

    	    // Now try to connect
    	 //   Connection c = DriverManager.getConnection(CONNECTION,p);

   // 	    System.out.println("It works !");
 //  	Scanner input = new Scanner(System.in);
//    	System.out.println("Enter the name of the mapping file attached to it's path: ");
//        String path = input.nextLine();
//        input.close();
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
//for (Quad q:rmlStore.getQuads(null, null, null, null)) {
//	System.out.println(q.getSubject()+" "+ q.getPredicate()+" "+q.getObject());
//}
         RecordsFactory factory = new RecordsFactory(mapPath,mappingfiledirectory);
         QuadStore outputStore = new RDF4JStore();
         Map<String, String> mappingOptions = new HashMap<>();
//         mappingOptions.put("jdbcDriver", dbClassName);
//         mappingOptions.put("username", "r2rml");
//         mappingOptions.put("password", "r2rml");
     	//String baseIRI = Utils.getBaseDirectiveTurtleOrDefault(mappingStream, defaultBaseIRI);
//         mappingOptions.put("jdbcDSN", CONNECTION);
         MappingConformer mc = new MappingConformer(rmlStore,mappingOptions);
         mc.conform();
         Executor executor = new Executor(mc.getStore(), factory, outputStore, baseIRI, null);
         
        List <Term> tms=executor.getTriplesMaps();
        MappingFactory f = new MappingFactory(null, baseIRI, StrictMode.BEST_EFFORT);
        File x;
        
        try {  x = new File(output);}catch (NullPointerException e){
         x = new File(mapPath+"/Datalog_rules"+".rs");
        }     
         x.createNewFile();

         FileWriter out = new FileWriter(x);
         for (Term a:tms) {
         	Mapping m =f.createMapping(a, rmlStore);
         	 

         	List<Record> lr=factory.createRecords(a, rmlStore);
       //      File xx = new File("C:\\Users\\aliha\\OneDrive\\Desktop\\rmlmapper-java-master\\r2rml-datalog\\test\\lt"+d_count+".facts");
             //x.createNewFile();
         List<String>edbss=	generateEDBs (a,  lr);
         if (!edbss.isEmpty()) {
        	 File xx;
         	 xx= new File(mapPath+"/lt"+d_count+".facts");
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
  rules.addAll(GenerateMapRules(mapPath,rmlStore, m, lr, edbs, f,factory,base));
  if (d_count<jc_count) {
		 d_count=jc_count+1;
	 }else {
		 d_count++;
	 }
	 l_count++;

 }
         out.write(".functor  toIRI(x:symbol):symbol \n"); 

         for (String s:declarations) {
         	 out.write(s+"\n");
          }
          out.flush();
          for (String h:rules) {
          	out.write(h+"\n");
          }
          out.flush();
          out.close();
          //c.close();
                  }
 //c.close();
         
    public static LinkedHashSet<String> GenerateMapRules(String mapPath,QuadStore qs,Mapping h,List<Record>lr,List<String>edbs,MappingFactory f, RecordsFactory factory,boolean base) throws Exception{
     	LinkedHashSet<String>rules= new LinkedHashSet<String>();
    	 graph_predicates.clear();
    	 term_predicates.clear();
    	 term_predicates2.clear();
    	maps_po.clear();
  	maps_join.clear();
    	graph_terms.clear();
    	link_graph.clear();
    	joins.clear();
    	vars.clear();
    	subj_map2.clear();
    	datatypes.clear();
  rules.addAll(generateSubjectRules (qs,h,lr,edbs));
  rules.addAll(generatePredicateRules (qs,h,lr,edbs));
  rules.addAll(generateObjectRules (mapPath,qs,h,f,lr,edbs,factory,base));
  rules.addAll(generateSubjectTermtypeRule(qs,h, f,base));
  rules.addAll(generatePredicateObjectTermtypeRule(qs,h,f,lr,base));
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
    public static List<String> generateEDBs (Term tm, List<Record> lr) throws Exception{
     	List<String> EDBs = new LinkedList<String>();
     	String decl="";
         for (int i=0; i<lr.size();i++) {
         	 CSVRecord rr = (CSVRecord) lr.get(i);
         	 Boolean found = false;
 if (ls.containsKey(rr)) {
 	d_count=ls.get(rr);
 	found=true;
 }
 //if (!found) {
         	 Set<String>s = rr.getData().keySet();
         	 s.remove("key");
         	 schema= new LinkedList<String>(s);
         	 decl=".decl lt"+d_count+"(";
         	 String pred ="";
 for (int j=0;j<schema.size()-1;j++) {
 	decl=decl+"x"+j+":symbol, ";
 	try {
 		String x =lr.get(i).getDataType(schema.get(j).toString());
 	if (x.contains("http://www.w3.org/2001/XMLSchema#integer")||x.contains("http://www.w3.org/2001/XMLSchema#boolean")) {
 		if (lr.get(i).get(schema.get(j)).isEmpty()) {
 			//pred=pred+lr.get(i).get(schema.get(j)).toString().replace("[", "\"").replace("]", "\"")+",";
 			pred =pred+lr.get(i).get(schema.get(j)).toString().replace("[", "").replace("]", "	");
 		}else {
 	//pred=pred+lr.get(i).get(schema.get(j)).toString().replace("[", "").replace("]", "")+",";
 	pred =pred+lr.get(i).get(schema.get(j)).toString().replace("[", "").replace("]", "	");
 	//System.out.println(schema.get(j).toString().replace("[", "\"").replace("]", "\""));
 }}
 	else {
 	//pred=pred+lr.get(i).get(schema.get(j)).toString().replace("[", "\"").replace("]", "\"")+",";
 	pred =pred+lr.get(i).get(schema.get(j)).toString().replace("[", "").replace("]", "	");
 }
 	}catch (java.lang.NullPointerException e) {
 		//pred=pred+lr.get(i).get(schema.get(j)).toString().replace("[", "\"").replace("]", "\"")+",";
 		pred =pred+lr.get(i).get(schema.get(j)).toString().replace("[", "").replace("]", "	");
 	}
 }
 decl=decl+"x"+(schema.size()-1)+":symbol)";
 try {
 	String x =lr.get(i).getDataType(schema.get(schema.size()-1).toString());
 if (x.contains("http://www.w3.org/2001/XMLSchema#integer")||x.contains("http://www.w3.org/2001/XMLSchema#boolean")) {
 //pred=pred+lr.get(i).get(schema.get(schema.size()-1)).toString().replace("[", "").replace("]", "")+")";
 pred =pred+lr.get(i).get(schema.get(schema.size()-1)).toString().replace("[", "").replace("]", "");
 }else {
 	//pred=pred+lr.get(i).get(schema.get(schema.size()-1)).toString().replace("[", "\"").replace("]", "\"")+")";
 	pred =pred+lr.get(i).get(schema.get(schema.size()-1)).toString().replace("[", "").replace("]", "");
 }
 }catch (java.lang.NullPointerException e) {
 	//pred=pred+lr.get(i).get(schema.get(schema.size()-1)).toString().replace("[", "\"").replace("]", "\"")+")";
 	pred =pred+lr.get(i).get(schema.get(schema.size()-1)).toString().replace("[", "").replace("]", "");
 }
 EDBs.add(pred);

  int temp=d_count;
  ls.put(rr, temp);
  declarations.add(decl);
  declarations.add(".input lt"+d_count);
 }
      //   }
 		return EDBs;
     }
     
     public static List<String> generateEDBs2 (Term tm, List<Record> lr) throws Exception{
     	List<String> EDBs = new LinkedList<String>();
     	String decl="";
         for (int i=0; i<lr.size();i++) {
         	 CSVRecord rr = (CSVRecord) lr.get(0);
         	 Boolean found = false;
         	 Set<String>s = rr.getData().keySet();
         	s.remove("key");
         	 schema2= new LinkedList<String>(s);
 if (ls.containsKey(rr)) {
 	jc_count=ls.get(rr);
 	found=true;
 }else {
 	jc_count=d_count+1;
 }
 //if (!found) {
 decl=".decl lt"+jc_count+"(";
         	 String pred="";
         	 for (int j=0;j<schema2.size()-1;j++) {
         			decl=decl+"x"+j+":symbol, ";
         			try {
         				String x =lr.get(i).getDataType(schema2.get(j).toString());
         			if (x.contains("http://www.w3.org/2001/XMLSchema#integer")||x.contains("http://www.w3.org/2001/XMLSchema#boolean")) {
         				if (lr.get(i).get(schema2.get(j)).isEmpty()) {
         					//pred=pred+lr.get(i).get(schema.get(j)).toString().replace("[", "\"").replace("]", "\"")+",";
         					pred =pred+lr.get(i).get(schema2.get(j)).toString().replace("[", "").replace("]", "	");
         				}else {
         			//pred=pred+lr.get(i).get(schema.get(j)).toString().replace("[", "").replace("]", "")+",";
         			pred =pred+lr.get(i).get(schema2.get(j)).toString().replace("[", "").replace("]", "	");
         			//System.out.println(schema.get(j).toString().replace("[", "\"").replace("]", "\""));
         		}}
         			else {
         			//pred=pred+lr.get(i).get(schema.get(j)).toString().replace("[", "\"").replace("]", "\"")+",";
         			pred =pred+lr.get(i).get(schema2.get(j)).toString().replace("[", "").replace("]", "	");
         		}
         			}catch (java.lang.NullPointerException e) {
         				//pred=pred+lr.get(i).get(schema.get(j)).toString().replace("[", "\"").replace("]", "\"")+",";
         				pred =pred+lr.get(i).get(schema2.get(j)).toString().replace("[", "").replace("]", "	");
         			}
         		}
         	 decl=decl+"x"+(schema2.size()-1)+":symbol)";
         	 try {
         			String x =lr.get(i).getDataType(schema2.get(schema2.size()-1).toString());
         		if (x.contains("http://www.w3.org/2001/XMLSchema#integer")||x.contains("http://www.w3.org/2001/XMLSchema#boolean")) {
         		//pred=pred+lr.get(i).get(schema.get(schema.size()-1)).toString().replace("[", "").replace("]", "")+")";
         		pred =pred+lr.get(i).get(schema2.get(schema2.size()-1)).toString().replace("[", "").replace("]", "");
         		}else {
         			//pred=pred+lr.get(i).get(schema.get(schema.size()-1)).toString().replace("[", "\"").replace("]", "\"")+")";
         			pred =pred+lr.get(i).get(schema2.get(schema2.size()-1)).toString().replace("[", "").replace("]", "");
         		}
         		}catch (java.lang.NullPointerException e) {
         			//pred=pred+lr.get(i).get(schema.get(schema.size()-1)).toString().replace("[", "\"").replace("]", "\"")+")";
         			pred =pred+lr.get(i).get(schema2.get(schema2.size()-1)).toString().replace("[", "").replace("]", "");
         		}
  EDBs.add(pred);
 	 int temp=jc_count;
 	 ls.put(rr, temp);
 	 declarations.add(decl);
 	 declarations.add(".input lt"+jc_count);
 	}

 //}
 		return EDBs;
     }
     public static LinkedHashSet<String> generateTermrules2 (MappingInfo ff,QuadStore qs,Mapping h,List<Record>lr) throws Exception{
      	variables2 ="";
      	variablesdec2 ="";
      	LinkedHashSet<String> rules = new LinkedHashSet<String>();
      	String rule ="";
      	//String decl=".decl ";
        	 for (int i=0; i<schema2.size()-1;i++) {
        		 variables2= variables2 + "z"+i+", ";
        		variablesdec2= variablesdec2 + "z"+i+":symbol, ";
     			 }
        	 variables2 = variables2+ "z"+""+(schema2.size()-1);
        	variablesdec2= variablesdec2 + "z"+(schema2.size()-1)+":symbol";
        	 for (Quad q:qs.getQuads(ff.getTerm(), null, null)) {
           if (q.getPredicate().getValue().contains("template")) {
          	 String temp = generateTemplate2(Utils.parseTemplate(q.getObject().getValue(), false)); 
               String predicate="eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+"y"+", "+variables2+")";
               String predicate2="eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+temp+", "+variables2+")";
              term_predicates.put(ff.getTerm(), predicate);
              term_predicates2.put(ff.getTerm(), predicate2);
           rule= predicate2 +" :- lt"+jc_count+"("+ variables2+").";
        //   decl = decl+"eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+variablesdec2+", "+"y:symbol"+")";
         	  rules.add(rule);
         	 // declarations.add(decl);
         	}
           else if (q.getPredicate().getValue().contains("reference")) {
          	 term_predicates.put(ff.getTerm(), "eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+"z"+schema2.indexOf(q.getObject().getValue())+", "+variables2+")"); 
          	term_predicates2.put(ff.getTerm(), "eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+"z"+schema2.indexOf(q.getObject().getValue())+", "+variables2+")"); 
             	  rule= "eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+"z"+schema2.indexOf(q.getObject().getValue())+", "+variables2+") :- lt"+jc_count+"("+ variables2+").";
             //   decl = decl+"eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+variablesdec2+", "+"y:symbol)";
             	  rules.add(rule);
             	//  declarations.add(decl);
             	  
           }
           else if (q.getPredicate().getValue().contains("constant")) {
          	 term_predicates.put(ff.getTerm(), "eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+"\""+q.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables2+")");
          	term_predicates2.put(ff.getTerm(), "eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+"\""+q.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables2+")");
               	  rule= "eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+"\""+q.getObject().getValue()+"\""+", "+variables2+") :- lt"+jc_count+"("+ variables2+").";
               	 rules.add(rule);
               //    decl = decl+"eval_"+ff.getTerm().getValue()+"_lt"+jc_count+"("+variablesdec2+", "+"y:symbol"+")";
                //   declarations.add(decl);
           }
//           else if (q.getPredicate().getValue().contains("graphMap")) {
//         	  String decl=".decl ";
//          	 List <Quad> lq = qs.getQuads(q.getSubject(), q.getPredicate(), null);
//     for (Quad qqs: lq) {     	 
//          	 Quad qq=qs.getQuad(qqs.getObject(), null, null);
//          	 String gval2=qq.getPredicate().getValue();
//          	 String gval=qq.getSubject().getValue();
//          	 if (gval2.contains("template")) {
//          		 String temp = generateTemplate(Utils.parseTemplate(qq.getObject().getValue(), false)); 
//          	     String predicate="eval_"+gval+d_count+"("+"y"+", "+variables+")";
//          	     String predicate2="eval_"+gval+d_count+"("+temp+", "+variables+")";
//                   decl = decl+"eval_"+gval+d_count+"("+variablesdec2+", "+"y:symbol"+")";
//          	    term_predicates.put(qq.getSubject(), predicate);
//          	   term_predicates2.put(qq.getSubject(), predicate2);
//          		if (!graph_terms.containsKey(q.getSubject())) {
//          			LinkedList<Term>a= new LinkedList<Term>();
//          			a.add(q.getObject());
//          		graph_terms.put(q.getSubject(), a);
//          		}else {
//          			graph_terms.get(q.getSubject()).add(q.getObject());
//          		}
//          	 rule= predicate2 +" :- lt"+d_count+"("+ variables+").";
//          	 declarations.add(decl);
//          	 	  rules.add(rule);
//          	 	}
//          	 else if (gval2.contains("reference")) {
//          			if (!graph_terms.containsKey(q.getSubject())) {
//          				LinkedList<Term>a= new LinkedList<Term>();
//          				a.add(q.getObject());
//          			graph_terms.put(q.getSubject(), a);
//          			}else {
//          				graph_terms.get(q.getSubject()).add(q.getObject());
//          			}
//          	 	 term_predicates.put(qq.getSubject(), "eval_"+gval+d_count+"("+"x"+schema.indexOf(qq.getObject().getValue())+", "+variables+")"); 
//          	 	term_predicates2.put(qq.getSubject(), "eval_"+gval+d_count+"("+"x"+schema.indexOf(qq.getObject().getValue())+", "+variables+")");
//          	   	  rule= "eval_"+gval+d_count+"("+"x"+schema.indexOf(qq.getObject().getValue())+", "+variables+") :- lt"+jc_count+"("+ variables+").";
//          	   	 decl = decl+"eval_"+gval+d_count+"("+variablesdec+", "+"y:symbol"+")";
//          	   	 declarations.add(decl);
//          	   	  rules.add(rule);
//          	   	  
//          	 }
//          	 else if (gval2.contains("constant")&&!(qq.getObject().getValue().equals("http://w3id.org/rml/defaultGraph"))) {
//          			if (!graph_terms.containsKey(q.getSubject())) {
//          				LinkedList<Term>a= new LinkedList<Term>();
//          				a.add(q.getObject());
//          			graph_terms.put(q.getSubject(), a);
//          			}else {
//          				graph_terms.get(q.getSubject()).add(q.getObject());
//          			}
//          	 	 term_predicates.put(qq.getSubject(), "eval_"+gval+d_count+"("+"\""+qq.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
//          	 	term_predicates2.put(qq.getSubject(), "eval_"+gval+d_count+"("+"\""+qq.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
//          	     	  rule= "eval_"+gval+d_count+"("+"\""+qq.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+") :- lt"+d_count+"("+ variables+").";
//          	     	 decl = decl+"eval_"+gval+d_count+"("+variablesdec+", "+"y:symbol"+")";
//              	   	 declarations.add(decl);
//          	     	 rules.add(rule);
//          	 }
//      }
//           }
      }
   		return rules;
      }
      public static String generateTemplate(List<Extractor> l) {
      	String base= "cat(";
      	String temp = base;
      	LinkedList<String>vars= new LinkedList<String>();
      	int count =1;
      	for (Extractor e: l){
      		if (e.toString().startsWith("ReferenceExecutor that works with ")) {
      			String a = e.toString().replace("ReferenceExecutor that works with ", "");
      			vars.add("@toIRI(x"+schema.indexOf(a)+")");
      		}else {
      			vars.add(e.toString());
      		}
      	}
      	if (vars.size()==2) {
      		temp=temp+vars.getFirst()+", "+vars.getLast()+")";
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
      			vars.add("@toIRI(z"+schema2.indexOf(a)+")");
      		}else {
      			vars.add(e.toString());
      		}
      	}
      	if (vars.size()==2) {
      		temp=temp+vars.getFirst()+", "+vars.getLast()+")";
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
      		return temp;
      	}
      public static LinkedHashSet<String> generateTermrules (MappingInfo ff,PredicateObjectGraphMapping ff2, QuadStore qs,Mapping h,List<Record>lr) throws Exception{
      	variables ="";
      	variablesdec="";
      	LinkedHashSet<String> rules = new LinkedHashSet<String>();
      	String rule ="";
      	String decl=".decl ";
        	 for (int i=0; i<schema.size()-1;i++) {
        		 variables= variables + "x"+i+", ";
        		variablesdec= variablesdec + "x"+i+":symbol, ";
     			 }
        	 variables = variables+ "x"+""+(schema.size()-1);
       	variablesdec= variablesdec + "x"+(schema.size()-1)+":symbol";
        	 for (Quad q:qs.getQuads(ff.getTerm(), null, null)) {
           if (q.getPredicate().getValue().contains("template")) {
          	 String temp = generateTemplate(Utils.parseTemplate(q.getObject().getValue(), false)); 
               String predicate="eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+"y"+", "+variables+")";
               String predicate2="eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+temp+", "+variables+")";
              term_predicates.put(ff.getTerm(), predicate);
              term_predicates2.put(ff.getTerm(), predicate2);
           rule= predicate2+" :- lt"+d_count+"("+ variables+").";
           decl = ".decl "+"eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
           declarations.add(decl);
         	  rules.add(rule);
         	}
           else if (q.getPredicate().getValue().contains("reference")) {
          	 term_predicates.put(ff.getTerm(), "eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+"x"+schema.indexOf(q.getObject().getValue())+", "+variables+")");
          	term_predicates2.put(ff.getTerm(), "eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+"x"+schema.indexOf(q.getObject().getValue())+", "+variables+")");
             	  rule= "eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+"x"+schema.indexOf(q.getObject().getValue())+", "+variables+") :- lt"+d_count+"("+ variables+").";
             	  rules.add(rule);
                decl = ".decl "+"eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
                declarations.add(decl);
             	  
           }
           else if (q.getPredicate().getValue().contains("constant")) {
          	 term_predicates.put(ff.getTerm(), "eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+"\""+q.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
          	term_predicates2.put(ff.getTerm(), "eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+"\""+q.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
               	  rule= "eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+"\""+q.getObject().getValue()+"\""+", "+variables+") :- lt"+d_count+"("+ variables+").";
               	 rules.add(rule);
                   decl = ".decl "+"eval_"+ff.getTerm().getValue()+"_lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
                   declarations.add(decl);
           }else if (q.getPredicate().getValue().contains("graphMap")) {
          	 List <Quad> lq = qs.getQuads(q.getSubject(), q.getPredicate(), null);
     for (Quad qqs: lq) {     	 
          	 Quad qq=qs.getQuad(qqs.getObject(), null, null);
  String gval2=qq.getPredicate().getValue();
  String gval=qq.getSubject().getValue();
  if (gval2.contains("template")) {
  	 String temp = generateTemplate(Utils.parseTemplate(qq.getObject().getValue(), false)); 
      String predicate="eval_"+gval+d_count+"("+"y"+", "+variables+")";
      String predicate2="eval_"+gval+d_count+"("+temp+", "+variables+")";
      decl = ".decl "+"eval_"+gval+d_count+"("+variablesdec+", "+"y:symbol"+")";
     term_predicates.put(qq.getSubject(), predicate);
     term_predicates2.put(qq.getSubject(), predicate2); 
  	if (!graph_terms.containsKey(q.getSubject())) {
  		LinkedList<Term>a= new LinkedList<Term>();
  		a.add(q.getObject());
  	graph_terms.put(q.getSubject(), a);
  	}else {
  		graph_terms.get(q.getSubject()).add(q.getObject());
  	}
  rule= predicate2+" :- lt"+d_count+"("+ variables+").";
  	  rules.add(rule);
  	  declarations.add(decl);
  	}
  else if (gval2.contains("reference")) {
  	if (!graph_terms.containsKey(ff.getTerm())) {
  		LinkedList<Term>a= new LinkedList<Term>();
  		a.add(q.getObject());
  	graph_terms.put(q.getSubject(), a);
  	}else {
  		graph_terms.get(q.getSubject()).add(q.getObject());
  	}
  	 term_predicates.put(qq.getSubject(), "eval_"+gval+d_count+"("+"x"+schema.indexOf(qq.getObject().getValue())+", "+variables+")"); 
  	term_predicates2.put(qq.getSubject(), "eval_"+gval+d_count+"("+"x"+schema.indexOf(qq.getObject().getValue())+", "+variables+")");
    	  rule= "eval_"+gval+d_count+"("+"x"+schema.indexOf(qq.getObject().getValue())+", "+variables+") :- lt"+d_count+"("+ variables+").";
    	 decl = ".decl "+"eval_"+gval+d_count+"("+variablesdec+", "+"y:symbol"+")";
    	 declarations.add(decl);
    	  rules.add(rule);
    	  
  }
  else if (gval2.contains("constant")&&!(qq.getObject().getValue().equals("http://w3id.org/rml/defaultGraph"))) {
  	if (!graph_terms.containsKey(ff.getTerm())) {
  		LinkedList<Term>a= new LinkedList<Term>();
  		a.add(q.getObject());
  	graph_terms.put(q.getSubject(), a);
  	}else {
  		graph_terms.get(q.getSubject()).add(q.getObject());
  	}
  	 term_predicates.put(qq.getSubject(), "eval_"+gval+d_count+"("+"\""+qq.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
  	term_predicates2.put(qq.getSubject(), "eval_"+gval+d_count+"("+"\""+qq.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
      	  rule= "eval_"+gval+d_count+"("+"\""+qq.getObject().getValue()+"\""+", "+variables+") :- lt"+d_count+"("+ variables+").";
      	  	 decl = ".decl "+"eval_"+gval+d_count+"("+variablesdec+", "+"y:symbol"+")";
      	  	 declarations.add(decl);
      	 rules.add(rule);
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
          	String gval=qq.getSubject().getValue();
          	if (gval2.contains("template")) {
          		 String temp = generateTemplate(Utils.parseTemplate(qq.getObject().getValue(), false)); 
          	    String predicate="eval_"+gval+d_count+"("+"y"+", "+variables+")";
          	    String predicate2="eval_"+gval+d_count+"("+temp+", "+variables+")";
          	   term_predicates.put(qq.getSubject(), predicate);
          	  term_predicates2.put(qq.getSubject(), predicate2);
          		if (!graph_terms.containsKey(ff.getTerm())) {
          			LinkedList<Term>a= new LinkedList<Term>();
          			a.add(qqs.getObject());
          		graph_terms.put(ff.getTerm(), a);
          		}else {
          			graph_terms.get(ff.getTerm()).add(qqs.getObject());
          		}
          	rule= predicate2+" :- lt"+d_count+"("+ variables+").";
           	 decl = ".decl "+"eval_"+gval+d_count+"("+variablesdec+", "+"y:symbol"+")";
            	 declarations.add(decl);
          		  rules.add(rule);
          		}
          	else if (gval2.contains("reference")) {
          		if (!graph_terms.containsKey(ff.getTerm())) {
          			LinkedList<Term>a= new LinkedList<Term>();
          			a.add(qqs.getObject());
          		graph_terms.put(ff.getTerm(), a);
          		}else {
          			graph_terms.get(ff.getTerm()).add(qqs.getObject());
          		}
          		 term_predicates.put(qq.getSubject(), "eval_"+gval+d_count+"("+"x"+schema.indexOf(qq.getObject().getValue())+", "+variables+")");
          		term_predicates2.put(qq.getSubject(), "eval_"+gval+d_count+"("+"x"+schema.indexOf(qq.getObject().getValue())+", "+variables+")");
          	  	  rule= "eval_"+gval+d_count+"("+"x"+schema.indexOf(qq.getObject().getValue())+", "+variables+") :- lt"+d_count+"("+ variables+").";
          	  	  rules.add(rule);
          	   	 decl = ".decl "+"eval_"+gval+d_count+"("+variablesdec+", "+"y:symbol"+")";
          	  	 declarations.add(decl);
          	  	  
          	}
          	else if (gval2.contains("constant")&&!(qq.getObject().getValue().equals("http://w3id.org/rml/defaultGraph"))) {
          		if (!graph_terms.containsKey(ff.getTerm())) {
          			LinkedList<Term>a= new LinkedList<Term>();
          			a.add(qqs.getObject());
          		graph_terms.put(ff.getTerm(), a);
          		}else {
          			graph_terms.get(ff.getTerm()).add(qqs.getObject());
          		}
          		 term_predicates.put(qq.getSubject(), "eval_"+gval+d_count+"("+"\""+qq.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
          		term_predicates2.put(qq.getSubject(), "eval_"+gval+d_count+"("+"\""+qq.getObject().getValue().replaceAll("\"", "")+"\""+", "+variables+")");
          	    	  rule= "eval_"+gval+d_count+"("+"\""+qq.getObject().getValue()+"\""+", "+variables+") :- lt"+d_count+"("+ variables+").";
          	    	 rules.add(rule);
          	      	 decl = ".decl "+"eval_"+gval+d_count+"("+variablesdec+", "+"y:symbol"+")";
          	      	 declarations.add(decl);
          	}
          	   } 
          	   }
           }}catch (NullPointerException e) {
//         	  List <Quad> laq=qs.getQuads(null, null, null,null);
//         	  for (Quad hf:laq) {
//         	  System.out.println(hf.getSubject()+" "+hf.getPredicate()+" "+hf.getObject());  
//         	  }
           }
        	 }
     		return rules;
      }
      public static String getLangaugeTag(QuadStore q, MappingInfo ff) {
  		for (Quad qq:q.getQuads(null, null, null)){
  			if (qq.getSubject().equals(ff.getTerm())&&qq.getPredicate().getValue().equals("http://w3id.org/rml/language")) {
  					return (qq.getObject().getValue());
  				}
  				
  			}
  		return "";
  		
  	}
      
      public static LinkedHashSet<String> generateSubjectRules (QuadStore qs,Mapping h,List<Record>lr,List<String>edbs) throws Exception{
       	MappingInfo ff = h.getSubjectMappingInfo();
       	return generateTermrules(ff,null, qs, h, lr);
       	 
       }
       
       public static HashSet<String> generateSubjectTermtypeRule (QuadStore qs, Mapping h, MappingFactory f, boolean base){
       	HashSet<String> al= new HashSet<String>();
       	String dec= ".decl "+"Subject"+l_count+"_"+"lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
       	declarations.add(dec);
          	List<Term> termTypes = Utils.getObjectsFromQuads(qs.getQuads(h.getSubjectMappingInfo().getTerm(), new NamedNode("http://w3id.org/rml/termType"), null));
       	 if (termTypes.contains(new NamedNode("http://w3id.org/rml/BlankNode"))) {
       		String head=term_predicates.get(h.getSubjectMappingInfo().getTerm());
       		String[]s2= head.split("\\(");
       		String[]s3=s2[1].split(", ");
       		s3[0]="y";
       		//s3[s3.length-1]= "?s";
       		String vars = Arrays.toString(s3).replace("[", "").replace("]", ")").replace("))", ")");	
       		head=head.replace(head.substring(head.indexOf("(")), "("+vars);
       		String rule2 = "Subject"+l_count+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(\"_:\",s),")+" :- "+ head.replace("y,", "s,")+".";
       		subj_map="Subject"+l_count+"_"+"lt"+d_count+"("+vars.replace("y,", "s,");
       		al.add(rule2);
       	} else {
   		String head=term_predicates.get(h.getSubjectMappingInfo().getTerm());
   		String[]s2= head.split("\\(");
   		String[]s3=s2[1].split(", ");
   		s3[0]="y";
   		//s3[s3.length-1]= "?s";
   		String vars = Arrays.toString(s3).replace("[", "").replace("]", ")").replace("))", ")");
   		String ma=term_predicates2.get(h.getSubjectMappingInfo().getTerm());
   		head=head.replace(head.substring(head.indexOf("(")), "("+vars);
   		String rule2="";
          if (ma.contains("\"https")||ma.contains("\"http")||!base) {
               rule2 = "Subject"+l_count+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(\"<\",cat(s,\">\")),")+" :- "+ head.replace("y,", "s,")+".";
          }else {
              rule2 = "Subject"+l_count+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(cat(\"<\",cat(\""+baseIRI+"\",s))"+",\">\"),")+" :- "+ head.replace("y,", "s,")+".";
          }
   		//String rule2 = "Subject"+l_count+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(\"<\",cat(s,\">\")),")+" :- "+ head.replace("y,", "s,")+".";
   		subj_map="Subject"+l_count+"_"+"lt"+d_count+"("+vars.replace("y,", "s,");
   		al.add(rule2);
   }
   return al;
       }
       
       public static HashSet<String> generateSubjectTermtypeRule2 (QuadStore qs, Mapping h, MappingFactory f,boolean base){
       	HashSet<String> al= new HashSet<String>();
//       	String dec= ".decl "+"Subject"+l_count+"_"+"lt"+jc_count+"("+variablesdec2+", "+"y:symbol"+")";
//       	declarations.add(dec);
       	List<Term> termTypes = Utils.getObjectsFromQuads(qs.getQuads(h.getSubjectMappingInfo().getTerm(), new NamedNode("http://w3id.org/rml/termType"), null));
       	 if (termTypes.contains(new NamedNode("http://w3id.org/rml/BlankNode"))) {
       		String head=term_predicates.get(h.getSubjectMappingInfo().getTerm());
       		String[]s2= head.split("\\(");
       		String[]s3=s2[1].split(", ");
       		s3[0]= "y";
       		String vars = Arrays.toString(s3).replace("[", "").replace("]", ")").replace("))", ")");
       		head=head.replace(head.substring(head.indexOf("(")), "("+vars);
       		String rule2 = "Subject"+jc_count+"_"+"lt"+jc_count+"("+vars.replace("y,", "cat(\"_:\",s2),") +" :- "+ head.replace("y,", "s2,")+".";
       		subj_map2.put(h.getSubjectMappingInfo().getTerm(),"Subject"+jc_count+"_"+"lt"+jc_count+"("+vars.replace("y,", "s2,"));
       		al.add(rule2);
       	}
       	 else {
   		String head=term_predicates.get(h.getSubjectMappingInfo().getTerm());
   		String[]s2= head.split("\\(");
   		String[]s3=s2[1].split(", ");
   		s3[0]= "y";
   		String vars = Arrays.toString(s3).replace("[", "").replace("]", ")").replace("))", ")").replace("))", ")");
   		head=head.replace(head.substring(head.indexOf("(")), "("+vars);
   		String ma=term_predicates2.get(h.getSubjectMappingInfo().getTerm());
   		String rule2="";
          if (ma.contains("\"https")||ma.contains("\"http")||!base) {
               rule2 = "Subject"+jc_count+"_"+"lt"+jc_count+"("+vars.replace("y,", "cat(\"<\",cat(s2,\">\")),")+" :- "+ head.replace("y,", "s2,")+".";
          }else {
              rule2 = "Subject"+jc_count+"_"+"lt"+jc_count+"("+vars.replace("y,", "cat(cat(\"<\",cat(\""+baseIRI+"\",s2))"+",\">\"),")+" :- "+ head.replace("y,", "s2,")+".";
          }
   		//String rule2 = "Subject"+jc_count+"_"+"lt"+jc_count+"("+vars.replace("y,", "cat(\"<\",cat(s2,\">\")),")+" :- "+ head.replace("y,", "s2,")+".";
   		subj_map2.put(h.getSubjectMappingInfo().getTerm(), "Subject"+jc_count+"_"+"lt"+jc_count+"("+vars.replace("y,", "s2,"));
   		al.add(rule2);
   }
   return al;
       }
       
       public static HashSet<String> generateGraphTermtypeRule (QuadStore qs, Mapping h, MappingFactory f, boolean base){
       	HashSet<String> al= new HashSet<String>();
       //	int i=0;
       	for (Term g :graph_terms.keySet()) {
       		for (Term t:graph_terms.get(g)) {
       		String head=term_predicates.get(t);
       		String[]s2= head.split("\\(");
       		String[]s3=s2[1].split(", ");
       		s3[0]= "y";
       		String vars = Arrays.toString(s3).replace("[", "").replace("]", ")").replace("))", ")");
       		head=head.replace(head.substring(head.indexOf("(")), "("+vars);
       		String ma=term_predicates2.get(t);
           	String dec= ".decl "+"Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
           	declarations.add(dec);
           	String rule2="";
              if (ma.contains("\"https")||ma.contains("\"http")||!base) {
               rule2 = "Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(\"<\",cat(g,\">\")),") +" :- "+ head.replace("y,", "g,")+".";
             }else {
              rule2 = "Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(cat(\"<\",cat(\""+baseIRI+"\",g))"+",\">\"),")+" :- "+ head.replace("y,", "g,")+".";
             }
       		//String rule2 = "Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(\"<\",cat(g,\">\")),") +" :- "+ head.replace("y,", "g,")+".";
       		if (!graph_predicates.containsKey(g)) {
       			LinkedList<String> a = new LinkedList<String>();
       			a.add("Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+vars.replace("y,", "g,"));
       		graph_predicates.put(g, a);
       		}else {
       			graph_predicates.get(g).add("Graph"+l_count+""+g_count+"_"+"lt"+d_count+"("+vars.replace("y,", "g,"));
       		}
       		
       		al.add(rule2);
       	}
       		g_count++;		
       		
       }
   		return al;
       	}
       public static LinkedHashSet<String> generatePredicateObjectTermtypeRule (QuadStore q, Mapping h, MappingFactory f, List<Record> lr, boolean base) throws Exception{
       	LinkedHashSet<String> al= new LinkedHashSet<String>();
       	int i=0;
   HashMap<String,String>donepreds= new HashMap<String,String>();
   HashMap<String,String>doneobjs= new HashMap<String,String>();
       	for (PredicateObjectGraphMapping pog :h.getPredicateObjectGraphMappings()) {
       		try {
       			if (!h.getSubjectMappingInfo().getTerm().equals(pog.getObjectMappingInfo().getTerm())){
       	    	String map="";
       			if (!donepreds.containsKey(pog.getPredicateMappingInfo().getTerm().getValue())) {
       		//if (!(pog.getObjectMappingInfo().getTerm().getValue().equals(h.getSubjectMappingInfo().getTerm().getValue()))) {
       
       		String head=term_predicates.get(pog.getPredicateMappingInfo().getTerm());
       		String[]s2= head.split("\\(");
       		String[]s3=s2[1].split(", ");
       		s3[0]= "y";

       		String vars = Arrays.toString(s3).replace("[", "").replace("]", ")").replace("))", ")");
       		head=head.replace(head.substring(head.indexOf("(")), "("+vars);
           	String dec= ".decl "+"Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
           	declarations.add(dec);
           	String ma=term_predicates2.get(pog.getPredicateMappingInfo().getTerm());
              String rule2="";
              if (ma.contains("\"https")||ma.contains("\"http")||!base) {
                   rule2 = "Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(\"<\",cat(p,\">\")),")+" :- "+ head.replace("y,", "p,")+".";
              }else {
                  rule2 = "Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(cat(\"<\",cat(\""+baseIRI+"\",p))"+",\">\"),")+" :- "+ head.replace("y,", "p,")+".";
              }
       		//String rule2 = "Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(\"<\",cat(p,\">\")),")+" :- "+ head.replace("y,", "p,")+".";
       		map="Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+vars.replace("y,", "p,");
       		al.add(rule2);
       		donepreds.put(pog.getPredicateMappingInfo().getTerm().getValue(), map);
       			}else {
       		map=donepreds.get(pog.getPredicateMappingInfo().getTerm().getValue());
       			}
           		Term t = pog.getObjectMappingInfo().getTerm();
               	String dec= ".decl "+"Object"+l_count+""+i+"_"+"lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
               	declarations.add(dec);  
           		List<Term> termTypes = Utils.getObjectsFromQuads(q.getQuads(t, new NamedNode("http://w3id.org/rml/termType"), null));
       			if (!doneobjs.containsKey(pog.getObjectMappingInfo().getTerm().getValue())) {
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
       		if (termTypes.contains(new NamedNode("http://w3id.org/rml/IRI"))||(fo&&termTypes.isEmpty())||fo2) {
       			String head2=term_predicates.get(t);
       			String[]s22= head2.split("\\(");
           		String[]s33=s22[1].split(", ");
           		s33[0]= "y";
           		String ma=term_predicates2.get(t);
           		String vars2 = Arrays.toString(s33).replace("[", "").replace("]", ")").replace("))", ")");
           		head2=head2.replace(head2.substring(head2.indexOf("(")), "("+vars2);
           		String rule22="";
                  if (ma.contains("\"https")||ma.contains("\"http")||!base) {
                   rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,", "cat(\"<\",cat(o,\">\")),")+" :- "+ head2.replace("y,", "o,")+".";
                 }else {
                  rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,", "cat(cat(\"<\",cat(\""+baseIRI+"\",o))"+",\">\"),")+" :- "+ head2.replace("y,", "o,")+".";
                 }
           		//String rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,", "cat(\"<\",cat(o,\">\")),")+" :- "+ head2.replace("y,", "o,")+".";
           		map=map+", "+"Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,", "o,");
           		doneobjs.put(pog.getObjectMappingInfo().getTerm().getValue(), "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,", "o,"));
           		al.add(rule22);
       	}else if (termTypes.contains(new NamedNode("http://w3id.org/rml/BlankNode"))) {
       		String head2=term_predicates.get(t);
   			String[]s22= head2.split("\\(");
       		String[]s33=s22[1].split(", ");
       		s33[0]= "y";
       		String vars2 = Arrays.toString(s33).replace("[", "").replace("]", ")").replace("))", ")");
       		head2=head2.replace(head2.substring(head2.indexOf("(")), "("+vars2);
       		String rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,", "cat(\"_:\",o),")+" :- "+ head2.replace("y,", "o,")+".";
       		map=map+", "+"Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,", "o,");
       		doneobjs.put(pog.getObjectMappingInfo().getTerm().getValue(), "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,", "o,"));
       		al.add(rule22);
       	}else {
          	 String lantag= getLangaugeTag(q, pog.getObjectMappingInfo());
       		String head2=term_predicates.get(t);
   			String[]s22= head2.split("\\(");
       		String[]s33=s22[1].split(", ");
       		s33[0]= "y";
       		String vars2 = Arrays.toString(s33).replace("[", "").replace("]", ")").replace("))", ")");
       		head2=head2.replace(head2.substring(head2.indexOf("(")), "("+vars2);
       		String rule22="";
       		if (lantag.equals("")) {
       			if (datatypes.containsKey(t)) {
       				//if (datatypes.get(t).contains("http://www.w3.org/2001/XMLSchema#date")) {
       					rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,", "cat(cat(\"\\\"\",cat(o,\"\\\"\")), \"^^<"+datatypes.get(t)+">\""+"),")+" :- "+ head2.replace("y,", "o,")+".";
       				//}else {
       				//String iris_func_pred="TO_"+datatypes.get(t).replace("http://www.w3.org/2001/XMLSchema#", "").toUpperCase()+"(?oo,?ooo)";
       				// rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2+" :- "+ head2.replace("?o)", "?oo)")+", "+iris_func_pred+", STRING_CONCAT(?ooo,\'\\'\',\'^^\',\'<"+datatypes.get(t)+">'"+",?o).";
       			//}
       			}else {
       		 rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,","cat(\"\\\"\",cat(o,\"\\\"\")),") +" :- "+ head2.replace("y,","o,")+".";
       			}}else {
       			rule22 = "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,", "cat(cat(\"\\\"\",cat(o,\"\\\"\")),\"@"+lantag+"\""+"),")+" :- "+ head2.replace("y,", "o,")+".";
       		}
       		map=map+", "+"Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,","o,");
       		doneobjs.put(pog.getObjectMappingInfo().getTerm().getValue(), "Object"+l_count+""+i+"_"+"lt"+d_count+"("+vars2.replace("y,","o,"));
       		al.add(rule22);
       	}
       			}
       			else {
       				map=map+", "+doneobjs.get(pog.getObjectMappingInfo().getTerm().getValue());
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
   		String head=term_predicates.get(pog.getPredicateMappingInfo().getTerm());
   		List<Quad> qs=q.getQuads(pog.getParentTriplesMap(), null, null);
   		Term ts=q.getQuad(pog.getParentTriplesMap(), new NamedNode("http://w3id.org/rml/subjectMap"), null).getObject();
   		String[]s2= head.split("\\(");
   		String[]s3=s2[1].split(", ");
   		s3[0]= "y";
   		String vars = Arrays.toString(s3).replace("[", "").replace("]", ")").replace("))", ")");
   		head=head.replace(head.substring(head.indexOf("(")), "("+vars);
       	String dec= ".decl "+"Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+variablesdec+", "+"y:symbol"+")";
       	declarations.add(dec);
       	String ma=term_predicates2.get(pog.getPredicateMappingInfo().getTerm());
       	String rule2="";
          if (ma.contains("\"https")||ma.contains("\"http")||!base) {
               rule2 = "Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(\"<\",cat(p,\">\")),")+" :- "+ head.replace("y,", "p,")+".";
          }else {
              rule2 = "Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(cat(\"<\",cat(\""+baseIRI+"\",p))"+",\">\"),")+" :- "+ head.replace("y,", "p,")+".";
          }
   		//String rule2 = "Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+vars.replace("y,", "cat(\"<\",cat(p,\">\")),")+" :- "+ head.replace("y,", "p,")+".";
   		map="Predicate"+l_count+""+i+"_"+"lt"+d_count+"("+vars.replace("y,", "p,");
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
       		rules.addAll(generateTermrules(ff,pogm, qs, h, lr));
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
            		rules.addAll(generateTermrules(ff,pogm, qs, h, lr));
            		try { 
            			Quad qqs=qs.getQuad(ff.getTerm(), null,null);
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
               List<String>edbss=	generateEDBs2 (t,  lr);
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
      rules.addAll(generateTermrules2(m.getSubjectMappingInfo(), q, m, lr));
      vars.put(m.getSubjectMappingInfo().getTerm(), variables2);
      rules.addAll(generateSubjectTermtypeRule2(q,m, f,base));
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
    		String rule1= "eval_jcc_"+jc.getValue()+"("+"x"+schema.indexOf(child)+", "+variables+") :- lt"+l_count+"("+ variables+").";
    		String rule12= "eval_jcp_"+jc.getValue()+"("+"z"+schema2.indexOf(parent)+", "+variables2+") :- lt"+jc_count+"("+ variables2+").";
    		join=join+", eval_jcc_"+jc.getValue()+"("+"v"+i+", "+variables+"), "+"eval_jcp_"+jc.getValue()+"("+"v"+i+", "+variables2+")";
    		rules.add(rule12);
    		rules.add(rule1);
    		i++;
    	  }
    		 joins.put(m.getSubjectMappingInfo().getTerm(),join);
    	  }
      }
        	}else {
        		  rules.addAll(generateTermrules(m.getSubjectMappingInfo(),null, q, m, lr));
        		  rules.addAll(generateSubjectTermtypeRule2(q,m, f,base));
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
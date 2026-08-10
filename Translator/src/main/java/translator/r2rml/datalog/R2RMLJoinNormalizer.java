package translator.r2rml.datalog;
import org.apache.jena.rdf.model.*;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.*;

public class R2RMLJoinNormalizer {

    // Namespaces
    static final String RR_NS = "http://www.w3.org/ns/r2rml#";
    static final String RML_NS = "http://semweb.mmlab.be/ns/rml#";

    // R2RML Properties
    static final Property rrLogicalTable = ResourceFactory.createProperty(RR_NS, "logicalTable");
    static final Property rrTableName = ResourceFactory.createProperty(RR_NS, "tableName");
    static final Property rrSqlQuery = ResourceFactory.createProperty(RR_NS, "sqlQuery");
    static final Property rrSubjectMap = ResourceFactory.createProperty(RR_NS, "subjectMap");
    static final Property rrPredicateObjectMap = ResourceFactory.createProperty(RR_NS, "predicateObjectMap");
    static final Property rrPredicateMap = ResourceFactory.createProperty(RR_NS, "predicateMap");
    static final Property rrPredicate = ResourceFactory.createProperty(RR_NS, "predicate");
    static final Property rrConstant = ResourceFactory.createProperty(RR_NS, "constant");
    static final Property rrObjectMap = ResourceFactory.createProperty(RR_NS, "objectMap");
    static final Property rrParentTriplesMap = ResourceFactory.createProperty(RR_NS, "parentTriplesMap");
    static final Property rrJoinCondition = ResourceFactory.createProperty(RR_NS, "joinCondition");
    static final Property rrParent = ResourceFactory.createProperty(RR_NS, "parent");
    static final Property rrChild = ResourceFactory.createProperty(RR_NS, "child");
    static final Property rrTemplate = ResourceFactory.createProperty(RR_NS, "template");

    // RML Properties
    static final Property rmlLogicalSource = ResourceFactory.createProperty(RML_NS, "logicalSource");

    /**
     * Normalizes joins from a mapping document (.ttl), writes the normalized mapping to mappings_normalized.ttl
     * in the same directory as the input mapping document, and does NOT overwrite the original file.
     * Removes TriplesMaps that have no predicate-object maps after normalization.
     */
    public static String normalizeMappingDocument(String mappingDocumentPath) throws Exception {
        Model model = RDFDataMgr.loadModel(mappingDocumentPath);
        Model normalized = normalizeJoins(model);
        removeEmptyTriplesMaps(normalized);

        // Determine output path
        File inputFile = new File(mappingDocumentPath);
        File outputFile = new File(inputFile.getParentFile(), "mappings_normalized.ttl");
        try (OutputStream out = new FileOutputStream(outputFile)) {
            RDFDataMgr.write(out, normalized, org.apache.jena.riot.RDFFormat.TURTLE_PRETTY);
        }
        return outputFile.getAbsolutePath();
    }

    /**
     * Finds all TriplesMaps, both R2RML and RML, and normalizes joins.
     */
    public static Model normalizeJoins(Model model) {
        Model result = ModelFactory.createDefaultModel();
        result.setNsPrefixes(model.getNsPrefixMap());
        int joinCounter = 0;

        List<Resource> triplesMaps = getAllTriplesMaps(model);

        for (Resource tm : triplesMaps) {
            boolean hasJoin = false;
            List<Resource> joinPoMs = new ArrayList<>();
            // Detect if TM has any join and collect PoMs
            StmtIterator pomIter = tm.listProperties(rrPredicateObjectMap);
            while (pomIter.hasNext()) {
                Resource pom = pomIter.next().getObject().asResource();
                Statement objectMapStmt = pom.getProperty(rrObjectMap);
                if (objectMapStmt != null) {
                    Resource objMap = objectMapStmt.getObject().asResource();
                    if (objMap.hasProperty(rrParentTriplesMap) && objMap.hasProperty(rrJoinCondition)) {
                        hasJoin = true;
                        joinPoMs.add(pom);
                    }
                }
            }
            if (hasJoin) {
                // 1. Copy the original TriplesMap, excluding join PoMs
                deepCopyTriplesMapExcludeJoins(tm, model, result);

                // 2. Add normalized TriplesMap for the join(s)
                Resource joinTM = result.createResource(tm.getURI() + "_Join" + (++joinCounter));
                joinTM.addProperty(RDF.type, ResourceFactory.createResource(RR_NS + "TriplesMap"));

                // Copy logicalTable or logicalSource
                Statement logicalTableStmt = tm.getProperty(rrLogicalTable);
                if (logicalTableStmt != null && logicalTableStmt.getObject().isResource()) {
                    Resource logicalTable = deepCopyBlankNode(logicalTableStmt.getObject().asResource(), model, result);
                    joinTM.addProperty(rrLogicalTable, logicalTable);
                }
                Statement logicalSourceStmt = tm.getProperty(rmlLogicalSource);
                if (logicalSourceStmt != null && logicalSourceStmt.getObject().isResource()) {
                    Resource logicalSource = deepCopyBlankNode(logicalSourceStmt.getObject().asResource(), model, result);
                    joinTM.addProperty(rmlLogicalSource, logicalSource);
                }

                // For join normalization, try to build a SQL query only if both TMs have rr:tableName
                String joinSql = null;
                Resource joinPOM = joinPoMs.get(0);
                Resource objMap = joinPOM.getProperty(rrObjectMap).getObject().asResource();
                Resource parentTM = objMap.getPropertyResourceValue(rrParentTriplesMap);
                if (tm.hasProperty(rrLogicalTable) && tm.getProperty(rrLogicalTable).getObject().isResource() &&
                    tm.getProperty(rrLogicalTable).getObject().asResource().hasProperty(rrTableName) &&
                    parentTM != null && parentTM.hasProperty(rrLogicalTable) &&
                    parentTM.getProperty(rrLogicalTable).getObject().isResource() &&
                    parentTM.getProperty(rrLogicalTable).getObject().asResource().hasProperty(rrTableName)) {
                    SQLJoinInfo joinInfo = extractJoinInfo(model, tm, parentTM, objMap);
                    joinSql = buildJoinSql(joinInfo);
                    Resource logicalTable = result.createResource();
                    logicalTable.addProperty(rrSqlQuery, joinSql);
                    joinTM.addProperty(rrLogicalTable, logicalTable);
                }

                // Copy subject map
                Statement subjMapStmt = tm.getProperty(rrSubjectMap);
                if (subjMapStmt != null && subjMapStmt.getObject().isResource()) {
                    Resource subjectMapCopy = deepCopyBlankNode(subjMapStmt.getObject().asResource(), model, result);
                    joinTM.addProperty(rrSubjectMap, subjectMapCopy);
                }

                // Copy ONLY join predicateObjectMaps, normalized
                for (Resource pom : joinPoMs) {
                    Statement objectMapStmt = pom.getProperty(rrObjectMap);
                    Resource objMap2 = objectMapStmt.getObject().asResource();
                    Resource parentTM2 = objMap2.getPropertyResourceValue(rrParentTriplesMap);
                    Resource newPOM = result.createResource();

                    // Copy predicateMap (deep copy)
                    Statement predMapStmt = pom.getProperty(rrPredicateMap);
                    if (predMapStmt != null && predMapStmt.getObject().isResource()) {
                        Resource predMapCopy = deepCopyBlankNode(predMapStmt.getObject().asResource(), model, result);
                        newPOM.addProperty(rrPredicateMap, predMapCopy);
                    }
                    // Copy predicate (shortcut)
                    Statement predStmt = pom.getProperty(rrPredicate);
                    if (predStmt != null) {
                        newPOM.addProperty(rrPredicate, predStmt.getObject());
                    }

                    // Build objectMap from parent's subjectMap template
                    Resource parentSubjMap = parentTM2.getPropertyResourceValue(rrSubjectMap);
                    if (parentSubjMap != null) {
                        Statement parentTemplateStmt = parentSubjMap.getProperty(rrTemplate);
                        if (parentTemplateStmt != null) {
                            Resource newObjMap = result.createResource();
                            newObjMap.addProperty(rrTemplate, parentTemplateStmt.getObject());
                            newPOM.addProperty(rrObjectMap, newObjMap);
                        }
                    }
                    newPOM.addProperty(RDF.type, ResourceFactory.createResource(RR_NS + "PredicateObjectMap"));
                    joinTM.addProperty(rrPredicateObjectMap, newPOM);
                }
            } else {
                // Deep copy as-is
                deepCopyTriplesMap(tm, model, result);
            }
        }
        return result;
    }

    /**
     * Finds all TriplesMaps in the model (R2RML and RML).
     */
    public static List<Resource> getAllTriplesMaps(Model model) {
        List<Resource> result = new ArrayList<>();
        // R2RML TriplesMaps: look for rr:logicalTable
        StmtIterator sit1 = model.listStatements(null, rrLogicalTable, (RDFNode) null);
        while (sit1.hasNext()) {
            Resource subj = sit1.next().getSubject();
            if (!result.contains(subj)) {
                result.add(subj);
            }
        }
        // RML TriplesMaps: look for rml:logicalSource
        StmtIterator sit2 = model.listStatements(null, rmlLogicalSource, (RDFNode) null);
        while (sit2.hasNext()) {
            Resource subj = sit2.next().getSubject();
            if (!result.contains(subj)) {
                result.add(subj);
            }
        }
        return result;
    }

    /** Deep copy a TriplesMap, but exclude any predicateObjectMaps that are join mappings (RefObjectMap with joinCondition). */
    private static void deepCopyTriplesMapExcludeJoins(Resource tm, Model src, Model dest) {
        Resource newTM = dest.createResource(tm.getURI());
        newTM.addProperty(RDF.type, ResourceFactory.createResource(RR_NS + "TriplesMap"));
        // logicalTable
        Statement ltStmt = tm.getProperty(rrLogicalTable);
        if (ltStmt != null && ltStmt.getObject().isResource()) {
            Resource ltCopy = deepCopyBlankNode(ltStmt.getObject().asResource(), src, dest);
            newTM.addProperty(rrLogicalTable, ltCopy);
        }
        // logicalSource
        Statement lsStmt = tm.getProperty(rmlLogicalSource);
        if (lsStmt != null && lsStmt.getObject().isResource()) {
            Resource lsCopy = deepCopyBlankNode(lsStmt.getObject().asResource(), src, dest);
            newTM.addProperty(rmlLogicalSource, lsCopy);
        }
        // subjectMap
        Statement subjMapStmt = tm.getProperty(rrSubjectMap);
        if (subjMapStmt != null && subjMapStmt.getObject().isResource()) {
            Resource subjMapCopy = deepCopyBlankNode(subjMapStmt.getObject().asResource(), src, dest);
            newTM.addProperty(rrSubjectMap, subjMapCopy);
        }
        // predicateObjectMaps
        StmtIterator pomIter = tm.listProperties(rrPredicateObjectMap);
        while (pomIter.hasNext()) {
            Statement pomStmt = pomIter.next();
            if (pomStmt.getObject().isResource()) {
                Resource pom = pomStmt.getObject().asResource();
                Statement objectMapStmt = pom.getProperty(rrObjectMap);
                boolean isJoin = false;
                if (objectMapStmt != null) {
                    Resource objMap = objectMapStmt.getObject().asResource();
                    if (objMap.hasProperty(rrParentTriplesMap) && objMap.hasProperty(rrJoinCondition)) {
                        isJoin = true;
                    }
                }
                // Only copy non-join PoMs!
                if (!isJoin) {
                    Resource pomCopy = deepCopyBlankNode(pom, src, dest);
                    newTM.addProperty(rrPredicateObjectMap, pomCopy);
                }
            }
        }
    }

    /** Deep copy a TriplesMap (including all predicate-object maps). */
    private static void deepCopyTriplesMap(Resource tm, Model src, Model dest) {
        Resource newTM = dest.createResource(tm.getURI());
        newTM.addProperty(RDF.type, ResourceFactory.createResource(RR_NS + "TriplesMap"));
        // logicalTable
        Statement ltStmt = tm.getProperty(rrLogicalTable);
        if (ltStmt != null && ltStmt.getObject().isResource()) {
            Resource ltCopy = deepCopyBlankNode(ltStmt.getObject().asResource(), src, dest);
            newTM.addProperty(rrLogicalTable, ltCopy);
        }
        // logicalSource
        Statement lsStmt = tm.getProperty(rmlLogicalSource);
        if (lsStmt != null && lsStmt.getObject().isResource()) {
            Resource lsCopy = deepCopyBlankNode(lsStmt.getObject().asResource(), src, dest);
            newTM.addProperty(rmlLogicalSource, lsCopy);
        }
        // subjectMap
        Statement subjMapStmt = tm.getProperty(rrSubjectMap);
        if (subjMapStmt != null && subjMapStmt.getObject().isResource()) {
            Resource subjMapCopy = deepCopyBlankNode(subjMapStmt.getObject().asResource(), src, dest);
            newTM.addProperty(rrSubjectMap, subjMapCopy);
        }
        // predicateObjectMaps
        StmtIterator pomIter = tm.listProperties(rrPredicateObjectMap);
        while (pomIter.hasNext()) {
            Statement pomStmt = pomIter.next();
            if (pomStmt.getObject().isResource()) {
                Resource pomCopy = deepCopyBlankNode(pomStmt.getObject().asResource(), src, dest);
                newTM.addProperty(rrPredicateObjectMap, pomCopy);
            }
        }
    }

    // Deep copy a blank node and its properties (recursive)
    private static Resource deepCopyBlankNode(Resource orig, Model src, Model dest) {
        Resource copy = dest.createResource();
        StmtIterator it = orig.listProperties();
        while (it.hasNext()) {
            Statement s = it.next();
            RDFNode o = s.getObject();
            if (o.isResource() && o.asResource().isAnon()) {
                copy.addProperty(s.getPredicate(), deepCopyBlankNode(o.asResource(), src, dest));
            } else {
                copy.addProperty(s.getPredicate(), o);
            }
        }
        return copy;
    }

    private static SQLJoinInfo extractJoinInfo(Model model, Resource childTM, Resource parentTM, Resource objMap) {
        // Get child table name (R2RML only)
        Resource childLogicalTable = childTM.getPropertyResourceValue(rrLogicalTable);
        String childTable = childLogicalTable.getProperty(rrTableName).getString();

        // Get parent table name (R2RML only)
        Resource parentLogicalTable = parentTM.getPropertyResourceValue(rrLogicalTable);
        String parentTable = parentLogicalTable.getProperty(rrTableName).getString();

        // Get join conditions
        List<String> childCols = new ArrayList<>();
        List<String> parentCols = new ArrayList<>();
        StmtIterator joinConds = objMap.listProperties(rrJoinCondition);
        while (joinConds.hasNext()) {
            Resource joinCond = joinConds.next().getObject().asResource();
            String childCol = joinCond.getProperty(rrChild).getString();
            String parentCol = joinCond.getProperty(rrParent).getString();
            childCols.add(childCol);
            parentCols.add(parentCol);
        }
        return new SQLJoinInfo(childTable, parentTable, childCols, parentCols);
    }

    private static String buildJoinSql(SQLJoinInfo info) {
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT * FROM ").append(info.childTable).append(" AS child JOIN ")
          .append(info.parentTable).append(" AS parent ON ");
        List<String> conds = new ArrayList<>();
        for (int i = 0; i < info.childCols.size(); i++) {
            conds.add("child." + info.childCols.get(i) + " = parent." + info.parentCols.get(i));
        }
        sb.append(String.join(" AND ", conds));
        return sb.toString();
    }

    /**
     * Removes TriplesMaps from the model that have no rr:predicateObjectMap.
     * Safe against ConcurrentModificationException.
     */
    private static void removeEmptyTriplesMaps(Model model) {
        List<Resource> toRemove = new ArrayList<>();
        ResIterator tms = model.listResourcesWithProperty(RDF.type, ResourceFactory.createResource(RR_NS + "TriplesMap"));
        while (tms.hasNext()) {
            Resource tm = tms.next();
            if (!tm.hasProperty(rrPredicateObjectMap)) {
                toRemove.add(tm);
            }
        }
        for (Resource tm : toRemove) {
            removeResourceAndBNodes(model, tm);
        }
    }

    /**
     * Recursively removes all statements about a resource and its blank node descendants.
     * Safe against ConcurrentModificationException.
     */
    private static void removeResourceAndBNodes(Model model, Resource resource) {
        Set<Resource> visited = new HashSet<>();
        Queue<Resource> queue = new LinkedList<>();
        queue.add(resource);
        while (!queue.isEmpty()) {
            Resource res = queue.poll();
            if (visited.contains(res)) continue;
            visited.add(res);
            StmtIterator props = res.listProperties();
            List<Statement> stmtsToRemove = new ArrayList<>();
            List<Resource> bnodes = new ArrayList<>();
            while (props.hasNext()) {
                Statement stmt = props.next();
                RDFNode obj = stmt.getObject();
                if (obj.isResource() && obj.asResource().isAnon()) {
                    bnodes.add(obj.asResource());
                }
                stmtsToRemove.add(stmt);
            }
            for (Statement stmt : stmtsToRemove) {
                model.remove(stmt);
            }
            model.removeAll(null, null, res);
            queue.addAll(bnodes);
        }
    }

    static class SQLJoinInfo {
        String childTable, parentTable;
        List<String> childCols, parentCols;
        SQLJoinInfo(String c, String p, List<String> cc, List<String> pc) {
            childTable = c; parentTable = p; childCols = cc; parentCols = pc;
        }
    }
}
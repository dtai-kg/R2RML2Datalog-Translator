.functor  toIRI(x:symbol):symbol
.decl Mapping(x:symbol, y:symbol, z:symbol)
.decl lt(x:symbol, y:symbol,z:symbol,k:symbol)
.decl Temp(z:symbol,m:number, k:symbol, x:symbol)
.decl Temp0(z:symbol,m:number, k:symbol)
.decl Termval(x:symbol, y:symbol,z:symbol,k:symbol)
.decl Subject(x:symbol, y:symbol,z:symbol,k:symbol)
.decl Predicate(x:symbol, y:symbol,z:symbol,k:symbol)
.decl Object(x:symbol, y:symbol,z:symbol,k:symbol)
.decl Graph(x:symbol, y:symbol,z:symbol,k:symbol)
.decl EvalTemp(x:symbol, y:symbol,z:symbol,k:symbol)
.decl BuildTemp(x:symbol, q:symbol, y:symbol,z:symbol,m:number, k:symbol)
.decl Datatype(x:symbol)
.decl Lantag(x:symbol)
.decl Column(x:symbol)
.decl Bl(x:symbol)
.decl Joincond(x:symbol)
.decl hasGraph(x:symbol)
.decl Violated(x:symbol, y:symbol,z:symbol,m:symbol, k:symbol)
.decl NoJoin(x:symbol, y:symbol,z:symbol,m:symbol, k:symbol)
Mapping("TriplesMap1", "rdfs:type", "rr:TriplesMap").
Mapping("TriplesMap1", "rr:logicalTable", "ltb").
Mapping("ltb", "rr:tableName", "Student").
Mapping("TriplesMap1", "rr:subjectMap", "smb").
Mapping("smb", "rr:template", "tid").
Mapping("TriplesMap1", "rr:predicateObjectMap", "pomb").
Mapping("pomb", "rr:predicateMap", "pmb").
Mapping("pmb", "rr:constant", "foaf:name").
Mapping("pomb", "rr:objectMap", "omb").
Mapping("omb", "rr:column", "Name").
lt("Student", "r1","Name", "Alice"). 
lt("Student", "r2", "Name", "Bob").
Temp0("tid", 1, "http://example.com/").
Temp("tid", 1, "Name", "").
Termval("Const", tm, tr_id, y) :- Mapping(tm, "rr:subjectMap", tr_id), Mapping(tr_id, "rr:constant", y).
Termval("Const", tm, tr_id, y) :- Mapping(tm, q, z), Mapping(z, k, tr_id), Mapping(tr_id, "rr:constant", y).
Termval(r, tm, tr_id, y) :- Mapping(tm, "rr:subjectMap", tr_id), Mapping(tr_id, "rr:column", x), Mapping(tm, "rr:logicalTable", lt_id), Mapping(lt_id,"rr:tableName",t_n), lt(t_n, r, x, y).
Termval(r, tm, tr_id, y) :- Mapping(tm, q, z), Mapping(z, k, tr_id), Mapping(tr_id, "rr:column", x), Mapping(tm, "rr:logicalTable", lt_id), Mapping(lt_id,"rr:tableName",t_n), lt(t_n, r, x, y).
Termval(r, tm, tr_id, y) :- Mapping(tm, "rr:subjectMap", tr_id), Mapping(tr_id, "rr:template", t_id), EvalTemp(lt_id, t_id, r, y).
Termval(r, tm, tr_id, y) :- Mapping(tm, q, z), Mapping(z, k, tr_id), Mapping(tr_id, "rr:template", t_id), EvalTemp(lt_id, t_id, r, y).
BuildTemp(lt_id, t_n, t_id, r, 1, cat(cat(s, @toIRI(x)), s1)) :- Mapping(tm, z, tr_id), lt(t_n, r, c1, x), Mapping(tr_id, "rr:template", t_id), Temp(t_id, 1, c1, s1), Temp0(t_id, m, s), Mapping(tm, "rr:logicalTable", lt_id), Mapping(lt_id,"rr:tableName",t_n). 
BuildTemp(lt_id, t_n, t_id, r, i, cat(cat(z, @toIRI(x)), si)) :- Temp(t_id, i, ci, si), lt(t_n, r, ci, x), BuildTemp(lt_id, t_n, t_id, r, i-1, z). 
EvalTemp(lt_id, t_id, r, x) :- Temp0(t_id, m, s), BuildTemp(lt_id, t_n, t_id, r, m, x).
Bl(sm_id) :- Mapping(sm_id, "rr:termType", "rr:BlankNode").
Subject(x, tm, sm_id, cat("<",cat(y,">"))) :- Mapping(tm, "rr:subjectMap", sm_id), !Bl(sm_id), Termval(x, tm, sm_id, y).
Subject(x, tm, sm_id, cat("_:",y)) :- Mapping(tm, "rr:subjectMap", sm_id), Mapping(sm_id, "rr:termType", "rr:BlankNode"), Termval(x, tm, sm_id, y).
Predicate(x, tm, pom_id, cat("<",cat(y,">"))) :- Termval(x, tm, pm_id, y), Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:predicateMap", pm_id).
Datatype(om_id) :- Mapping(om_id, "rr:datatype", dm).
Lantag(om_id) :- Mapping(om_id, "rr:language", lm).
Column(om_id) :- Mapping(om_id, "rr:column", x).
Object(x, tm, pom_id, cat("<",cat(y,">"))) :- Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:objectMap", om_id), Termval(x, tm, om_id, y), Mapping(om_id, "rr:termType", "rr:IRI").
Object(x, tm, pom_id, cat("<",cat(y,">"))) :- Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:objectMap", om_id), !Datatype(om_id), !Lantag(om_id), !Column(om_id), Termval(x, tm, om_id, y).
Object(x, tm, pom_id, cat("_:",y)) :- Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:objectMap", om_id), Termval(x, tm, om_id, y), Mapping(om_id, "rr:termType", "rr:BlankNode").
Object(x, tm, pom_id, y) :- Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:objectMap", om_id), !Datatype(om_id), !Lantag(om_id), Mapping(om_id, "rr:termType", "rr:Literal"), Termval(x, tm, om_id, y).
Object(x, tm, pom_id, y) :- Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:objectMap", om_id), Column(om_id), !Datatype(om_id), !Lantag(om_id), Termval(x, tm, om_id, y).
Object(x, tm, pom_id, cat(y,dm)) :- Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:objectMap", om_id), Termval(x, tm, om_id, y), Mapping(om_id, "rr:datatype", dm).
Object(x, tm, pom_id, cat(y,lm)) :- Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:objectMap", om_id), Termval(x, tm, om_id, y), Mapping(om_id, "rr:language", lm).
Joincond(om_id) :- Mapping(om_id, "rr:joinCondition", jc_id).
Object(x, tm, pom_id, y) :- Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:objectMap", om_id), !Joincond(om_id), Mapping(om_id, "rr:parentTriplesMap", tm_2), Subject(x, tm_2, sm2_id, y).
Violated(tpn, tcn, rp, rc, jc_id) :- Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:objectMap", om_id), Mapping(tm, "rr:logicalTable", lt_c), Mapping(lt_c, "rr:tableName", tcn), Mapping(om_id, "rr:parentTriplesMap", tm_2), Mapping(tm_2, "rr:logicalTable", ltp), Mapping(ltp, "rr:tableName", tpn), Mapping(jc_id, "rr:parent", p), Mapping(jc_id, "rr:child", c), lt(tpn, rp, p, x), lt(tcn, rc, c, y), x != y.
NoJoin(om_id, tpn, tcn, rp, rc) :- Mapping(om_id, "rr:joinCondition", jc_id), Violated(tpn, tcn, rp, rc, jc_id).
Object(rc, tm, pom_id, y) :- Mapping(tm, "rr:predicateObjectMap", pom_id), Mapping(pom_id, "rr:objectMap", om_id), Mapping(tm, "rr:logicalTable", lt_c), Mapping(lt_c, "rr:tableName", tcn), Mapping(om_id, "rr:parentTriplesMap", tm_2), Subject(rp, tm_2, sm2_id, y), Mapping(tm_2, "rr:logicalTable", lt_p), Mapping(ltp, "rr:tableName", tpn), lt(tpn, rp, c1, x1), lt(tcn, rc, c2, x2), !NoJoin(om_id, tpn, tcn, rp, rc).
Graph(x, tm, tr_id, cat("<",cat(y,">"))) :- Mapping(tr_id, "rr:graphMap", gm_id), Termval(x, tm, gm_id, y).
hasGraph(t_id) :- Mapping(t_id, "rr:graphMap", gm_id).
triple(s, "rdfs:type", c_IRI) :- Subject(x, tm, sm_id, s), !hasGraph(sm_id), Mapping(sm_id, "rr:class", c_IRI).
quadruple(s, "rdfs:type", c_IRI, g) :- Subject(x, tm, sm_id, s), Graph(x, tm, sm_id, g), Mapping(sm_id, "rr:class", c_IRI).
quadruple(s, "rdfs:type", c_IRI, g) :- Subject(r, tm, sm_id, s), Graph("Const", tm, sm_id, g), Mapping(sm_id, "rr:class", c_IRI).
quadruple(s, "rdfs:type", c_IRI, g) :- Subject("Const", tm, sm_id, s), Graph(r, tm, sm_id, g), Mapping(sm_id, "rr:class", c_IRI).
triple(s, p, o) :- Subject(x, tm, sm_id, s), Predicate(x, tm, pom_id, p), Object(x, tm, pom_id, o), !hasGraph(sm_id), !hasGraph(pom_id).
triple(s, p, o) :- Subject(r, tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object("Const", tm, pom_id, o), !hasGraph(sm_id), !hasGraph(pom_id).
triple(s, p, o) :- Subject("Const", tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object(r, tm, pom_id, o), !hasGraph(sm_id), !hasGraph(pom_id).
triple(s, p, o) :- Subject("Const", tm, sm_id, s), Predicate(r, tm, pom_id, p), Object("Const", tm, pom_id, o), !hasGraph(sm_id), !hasGraph(pom_id).
triple(s, p, o) :- Subject(r, tm, sm_id, s), Predicate(r, tm, pom_id, p), Object("Const", tm, pom_id, o), !hasGraph(sm_id), !hasGraph(pom_id).
triple(s, p, o) :- Subject("Const", tm, sm_id, s), Predicate(r, tm, pom_id, p), Object(r, tm, pom_id, o), !hasGraph(sm_id), !hasGraph(pom_id).
triple(s, p, o) :- Subject(r, tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object(r, tm, pom_id, o), !hasGraph(sm_id), !hasGraph(pom_id).
quadruple(s, p, o, g) :- Subject(x, tm, sm_id, s), Predicate(x, tm, pom_id, p), Object(x, tm, pom_id, o), Graph(x, tm, sm_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate(r, tm, pom_id, p), Object(r, tm, pom_id, o), Graph(r, tm, sm_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object(r, tm, pom_id, o), Graph(r, tm, sm_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate(r, tm, pom_id, p), Object("Const", tm, pom_id, o), Graph(r, tm, sm_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate(r, tm, pom_id, p), Object(r, tm, pom_id, o), Graph("Const", tm, sm_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object(r, tm, pom_id, o), Graph(r, tm, sm_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate(r, tm, pom_id, p), Object("Const", tm, pom_id, o), Graph(r, tm, sm_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate(r, tm, pom_id, p), Object(r, tm, pom_id, o), Graph("Const", tm, sm_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object("Const", tm, pom_id, o), Graph(r, tm, sm_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object(r, tm, pom_id, o), Graph("Const", tm, sm_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate(r, tm, pom_id, p), Object("Const", tm, pom_id, o), Graph("Const", tm, sm_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object("Const", tm, pom_id, o), Graph(r, tm, sm_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object(r, tm, pom_id, o), Graph("Const", tm, sm_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate(r, tm, pom_id, p), Object("Const", tm, pom_id, o), Graph("Const", tm, sm_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object("Const", tm, pom_id, o), Graph("Const", tm, sm_id, g).
quadruple(s, p, o, g) :- Subject(x, tm, sm_id, s), Predicate(x, tm, pom_id, p), Object(x, tm, pom_id, o), Graph(x, tm, pom_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate(r, tm, pom_id, p), Object(r, tm, pom_id, o), Graph(r, tm, pom_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object(r, tm, pom_id, o), Graph(r, tm, pom_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate(r, tm, pom_id, p), Object("Const", tm, pom_id, o), Graph(r, tm, pom_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate(r, tm, pom_id, p), Object(r, tm, pom_id, o), Graph("Const", tm, pom_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object(r, tm, pom_id, o), Graph(r, tm, pom_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate(r, tm, pom_id, p), Object("Const", tm, pom_id, o), Graph(r, tm, pom_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate(r, tm, pom_id, p), Object(r, tm, pom_id, o), Graph("Const", tm, pom_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object("Const", tm, pom_id, o), Graph(r, tm, pom_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object(r, tm, pom_id, o), Graph("Const", tm, pom_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate(r, tm, pom_id, p), Object("Const", tm, pom_id, o), Graph("Const", tm, pom_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object("Const", tm, pom_id, o), Graph(r, tm, pom_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object(r, tm, pom_id, o), Graph("Const", tm, pom_id, g).
quadruple(s, p, o, g) :- Subject("Const", tm, sm_id, s), Predicate(r, tm, pom_id, p), Object("Const", tm, pom_id, o), Graph("Const", tm, pom_id, g).
quadruple(s, p, o, g) :- Subject(r, tm, sm_id, s), Predicate("Const", tm, pom_id, p), Object("Const", tm, pom_id, o), Graph("Const", tm, pom_id, g).
.decl triple(s:symbol,p:symbol,o:symbol)
.decl quadruple(s:symbol,p:symbol,o:symbol,g:symbol)
.output triple
.output quadruple
.output Termval
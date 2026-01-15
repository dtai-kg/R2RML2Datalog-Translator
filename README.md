# R2RML2Datalog Translator

[![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Java](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.java.com/en/)
[![Soufflé](https://img.shields.io/badge/Souffl%C3%A9-Compatible-brightgreen.svg)](https://souffle-lang.github.io/index.html)

A tool for translating [R2RML](https://www.w3.org/TR/r2rml/) mappings and relational data (databases) into Datalog programs and fact files in [Soufflé](https://souffle-lang.github.io/index.html) syntax.

Powered by [RMLMapper](https://github.com/RMLio/rmlmapper-java) for parsing mappings and data.

---

## Features

- **Input:** R2RML mappings and relational databases
- **Output:** Datalog programs and fact files (Soufflé syntax)
- **Easy to use:** Simple command-line interface
- **Integration:** Leverages RMLMapper for robust mapping support

---

## Table of Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Usage](#usage)
- [Examples](#examples)
- [Command-Line Options](#command-line-options)
- [Running the Datalog Programs](#running-datalog)
- [Troubleshooting](#troubleshooting)
- [Support & Contact](#support--contact)
- [License](#license)
- [References](#References)

---

## Requirements

- [Java 8+](https://www.java.com/en/download/)
- Access to your relational database
- R2RML mapping file(s)

---

## Installation

1. **Download the JAR**

   Download the latest release of `parser.jar` from the [releases page](https://github.com/dtai-kg/R2RML2Datalog-Translator/releases/tag/v1.0.0) or build it yourself.

2. **Verify Java**

   Make sure Java is installed:

   ```sh
   java -version
   ```

---

## Usage

```sh
java -jar parser.jar -m <mappings.ttl> -dsn <jdbc:driver://...> -u <dbuser> -p <dbpass> -o <output_dir>
```

**Parameters:**
- `-m`: Path to R2RML mappings file
- `-dsn`: Database connection string [JDBC format](https://www.marcobehler.com/guides/jdbc) 
- `-u`: Database username
- `-p`: Database password
- `-o`: (Optional) Output file path

### 3. Help & CLI Options

To see all available options:

```sh
java -jar parser.jar -h
```

---

## Example

**Translate R2RML with MySQL:**

Consider a relational database with a table 'Student' that has the following data: 

| Name   |
|--------|
| Venus  |

The following R2RML mapping file 'mapping.ttl' defines how rows from the `Student` table are mapped to RDF triples.

```turtle
@prefix rr: <http://www.w3.org/ns/r2rml#> .
@prefix foaf: <http://xmlns.com/foaf/0.1/> .
@prefix ex: <http://example.com/> .
@prefix xsd: <http://www.w3.org/2001/XMLSchema#> .
@base <http://example.com/base/> .

<TriplesMap1>
    a rr:TriplesMap ;

    rr:logicalTable [
        rr:tableName "Student" ] ;

    rr:subjectMap [
        rr:template "http://example.com/{Name}" ] ;

    rr:predicateObjectMap [
        rr:predicateMap [ rr:constant foaf:name ] ;
        rr:objectMap [ rr:column "Name" ] ] .

Running the R2RML2Datalog Translator on this mapping file and the relational data can be done with the following command:

```sh
java -jar parser.jar -m mapping.ttl -dsn jdbc:mysql://localhost:3306/mydb -u user -p pass -o outputPath/output.rls
```
The result is the following Datalog program and fact files:

# Datalog Program
```souffle
.functor toIRI(x:symbol):symbol 

.decl lt0(x0:symbol)
.input lt0

.decl eval_7227202602e14113a2d06e0ac084adcd2_lt0(x0:symbol, y:symbol)
.decl eval_7227202602e14113a2d06e0ac084adcd4_lt0(x0:symbol, y:symbol)
.decl eval_7227202602e14113a2d06e0ac084adcd5_lt0(x0:symbol, y:symbol)

.decl Subject0_lt0(x0:symbol, y:symbol)
.decl Predicate00_lt0(x0:symbol, y:symbol)
.decl Object00_lt0(x0:symbol, y:symbol)

eval_7227202602e14113a2d06e0ac084adcd2_lt0(
    cat("http://example.com/", @toIRI(x0)), x0
) :- lt0(x0).

eval_7227202602e14113a2d06e0ac084adcd4_lt0(
    "http://xmlns.com/foaf/0.1/name", x0
) :- lt0(x0).

eval_7227202602e14113a2d06e0ac084adcd5_lt0(
    x0, x0
) :- lt0(x0).

Subject0_lt0(cat("<", cat(s, ">")), x0) :-
    eval_7227202602e14113a2d06e0ac084adcd2_lt0(s, x0).

Predicate00_lt0(cat("<", cat(p, ">")), x0) :-
    eval_7227202602e14113a2d06e0ac084adcd4_lt0(p, x0).

Object00_lt0(cat("\"", cat(o, "\"")), x0) :-
    eval_7227202602e14113a2d06e0ac084adcd5_lt0(o, x0).

.decl triple(s:symbol, p:symbol, o:symbol)
triple(s, p, o) :-
    Subject0_lt0(s, x0),
    Predicate00_lt0(p, x0),
    Object00_lt0(o, x0).

.decl quadruple(s:symbol, p:symbol, o:symbol, g:symbol)

.output triple
.output quadruple

## Running the Datalog Programs

This repository also includes the necessary user-defined C++ functions required by Soufflé to evaluate the generated Datalog programs and facts. These functions are implemented in the file [`functors.cpp`](functors.cpp).

To execute the Datalog programs:

1. **Build Soufflé**  
   Follow the official build instructions provided here:  
   👉 [https://souffle-lang.github.io/build](https://souffle-lang.github.io/build)

2. **Integrate Custom Functors**  
   Add the `functors.cpp` file to the Soufflé source directory and follow the integration commands here:  
   👉 [https://souffle-lang.github.io/functors](https://souffle-lang.github.io/functors)

3. **Execute the Datalog Program**  
   Run Soufflé on the Datalog program and facts files following the simple execution steps here:  
   👉 [https://souffle-lang.github.io/simple](https://souffle-lang.github.io/simple)

Please follow [https://souffle-lang.github.io/execute](https://souffle-lang.github.io/execute) for more execution options

## Troubleshooting

- Ensure you have Java 8 or higher installed and accessible in your environment.
- For database connections, ensure the JDBC driver is available and the connection parameters are correct.

---

## Support & Contact

For any issues, please contact:  
- Ali Elhalwati: [ali.elhalawati@kuleuven.be](mailto:ali.elhalawati@kuleuven.be)  
- Anastasia Dimou: [anastasia.dimou@kuleuven.be](mailto:anastasia.dimou@kuleuven.be)

---

## License

This project is licensed under the [MIT License](LICENSE).

---

## References

- **R2RML: RDB to RDF Mapping Language**  
  Souripriya Das, Seema Sundara, Richard Cyganiak.  
  World Wide Web Consortium (W3C), Working Group Recommendation, 2012.  
  [http://www.w3.org/TR/r2rml/](http://www.w3.org/TR/r2rml/)

- **Automated metadata generation for Linked Data generation and publishing workflows**  
  Anastasia Dimou, Tom De Nies, Ruben Verborgh, Erik Mannens, Peter Mechant, Rik Van de Walle.  
  Proceedings of the 9th Workshop on Linked Data on the Web*, Montreal, Canada, 2016.  
  [Paper](http://events.linkeddata.org/ldow2016/papers/LDOW2016_paper_04.pdf)

- **Soufflé: On Synthesis of Program Analyzers**  
  Herbert Jordan, Bernhard Scholz, Pavle Subotić.  
  In Proceedings of the *Computer Aided Verification (CAV)*, 2016.  
  [https://souffle-lang.github.io/index.html](https://souffle-lang.github.io/index.html)

- **A Declarative Formalization of R2RML Using Datalog and Its Efficient Execution**
  Ali Elhalawati, Anastasia Dimou, and Jan Van den Bussche.
  In RuleChallenge@RuleML+RR, 2025. 
  [Paper](https://ceur-ws.org/Vol-4083/paper63.pdf)

package translator.r2rml.datalog;

import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import be.ugent.rml.Utils;
import be.ugent.rml.store.RDF4JStore;
import ch.qos.logback.classic.Level;

public class Main {
	private static final String defaultBaseIRI = "http://example.com";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final Marker fatal = MarkerFactory.getMarker("FATAL");

    public static void main(String[] args) {
        try {
            run(args, System.getProperty("user.dir"));
        } catch (Exception e) {
            System.out.println(e);
            System.exit(1);
        }
    }

    public static void run(String[] args) throws Exception {
        run(args, System.getProperty("user.dir"));
    }

    /**
     * Main method use for the CLI. Allows to also set the current working directory
     * via the argument basePath.
     *
     * @param args     the CLI arguments
     * @param basePath the basePath used during the execution.
     */
    public static void run(String[] args, String basePath) throws Exception {
        Options options = new Options();
        Option mappingdocOption = Option.builder("m")
                .longOpt("mappingfile")
                .hasArg()
                .numberOfArgs(Option.UNLIMITED_VALUES)
                .desc("one or more mapping file paths and/or strings (multiple values are concatenated). " +
                        "r2rml is converted to rml if needed using the r2rml arguments."
                + "RDF Format is determined based on extension.")
                .build();
        Option outputfileOption = Option.builder("o")
                .longOpt("outputfile")
                .hasArg()
                .desc("path to output file (default: stdout)")
                .build();
        Option helpOption = Option.builder("h")
                .longOpt("help")
                .desc("show help info")
                .build();
        Option jdbcDSNOption = Option.builder("dsn")
                .longOpt("r2rml-jdbcDSN")
                .desc("DSN of the database when using R2RML rules")
                .hasArg()
                .build();
        Option passwordOption = Option.builder("p")
                .longOpt("r2rml-password")
                .desc("password of the database when using R2RML rules")
                .hasArg()
                .build();
        Option usernameOption = Option.builder("u")
                .longOpt("r2rml-username")
                .desc("username of the database when using R2RML rules")
                .hasArg()
                .build();
        Option baseTrueOption = Option.builder("bt")
                .longOpt("base-iri")
                .desc("Include base-iri from mapping file or default base")
                //.hasArg()
                .build();
        options.addOption(mappingdocOption);
        options.addOption(outputfileOption);
        options.addOption(helpOption);
        options.addOption(jdbcDSNOption);
        options.addOption(passwordOption);
        options.addOption(usernameOption);
        options.addOption(baseTrueOption);
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine lineArgs = parser.parse(options, args);

            Properties configFile = null;
            if (checkOptionPresence(helpOption, lineArgs, configFile)) {
                printHelp(options);
                return;
            }

                setLoggerLevel(Level.ERROR);

            String[] mOptionValue = getOptionValues(mappingdocOption, lineArgs, configFile);
            List<InputStream> lis = new ArrayList<>();

            if (mOptionValue == null && System.console() != null) {
                printHelp(options);
                throw new IllegalArgumentException("No mapping file nor via stdin found!");
            }

            InputStream is = new SequenceInputStream(Collections.enumeration(lis));;

            Map<String, String> mappingOptions = new HashMap<>();
            for (Option option : new Option[]{jdbcDSNOption, passwordOption, usernameOption}) {
                if (checkOptionPresence(option, lineArgs, configFile)) {
                    mappingOptions.put(option.getLongOpt().replace("r2rml-", ""), getOptionValues(option, lineArgs, configFile)[0]);
                }
            }

            RDF4JStore rmlStore = new RDF4JStore();
            try {
                rmlStore.read(is, null, RDFFormat.TURTLE);
            }
            catch (RDFParseException e) {
                logger.error(fatal, "Unable to parse mapping rules as Turtle. Does the file exist and is it valid Turtle?", e);
                throw new IllegalArgumentException("Unable to parse mapping rules as Turtle. Does the file exist and is it valid Turtle?");
            }
            if (checkOptionPresence(jdbcDSNOption, lineArgs, configFile)&& checkOptionPresence(usernameOption, lineArgs, configFile)&& checkOptionPresence(passwordOption, lineArgs, configFile)) {
            	if (!checkOptionPresence(baseTrueOption, lineArgs, configFile)) {
            DatalogGenerator.exec_dlog(getOptionValues(mappingdocOption, lineArgs, configFile)[0], getOptionValues(jdbcDSNOption, lineArgs, configFile)[0],  getOptionValues(usernameOption, lineArgs, configFile)[0], getOptionValues(passwordOption, lineArgs, configFile)[0],false, getPriorityOptionValue(outputfileOption, lineArgs, configFile));
            	}else {
            		DatalogGenerator.exec_dlog(getOptionValues(mappingdocOption, lineArgs, configFile)[0], getOptionValues(jdbcDSNOption, lineArgs, configFile)[0],  getOptionValues(usernameOption, lineArgs, configFile)[0], getOptionValues(passwordOption, lineArgs, configFile)[0],true, getPriorityOptionValue(outputfileOption, lineArgs, configFile));	
            	}
            }else {
            	if (!checkOptionPresence(baseTrueOption, lineArgs, configFile)) {
                DatalogSouffle.exec_dlog(getOptionValues(mappingdocOption, lineArgs, configFile)[0],false, getPriorityOptionValue(outputfileOption, lineArgs, configFile));
            	}else {
            		DatalogSouffle.exec_dlog(getOptionValues(mappingdocOption, lineArgs, configFile)[0],true,getPriorityOptionValue(outputfileOption, lineArgs, configFile));
            	}
			}
            String baseIRI = null;
            if (baseIRI == null || baseIRI.isEmpty()) {
                    if (mOptionValue != null) {
                        lis = Arrays.stream(mOptionValue)
                                .map(Utils::getInputStreamFromFileOrContentString)
                                .collect(Collectors.toList());
                    }
                    try (InputStream is2 = new SequenceInputStream(Collections.enumeration(lis))) {
                    	baseIRI = Utils.getBaseDirectiveTurtleOrDefault(is2, defaultBaseIRI);
                    }
            }

        } catch (ParseException exp) {
            logger.error("Parsing failed. Reason: {}", exp.getMessage());
            printHelp(options);
        } catch (IllegalArgumentException exp) {
            throw exp;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    private static boolean checkOptionPresence(Option option, CommandLine lineArgs, Properties properties) {
        return (option.getOpt() != null && lineArgs.hasOption(option.getOpt()))
                || (option.getLongOpt() != null && lineArgs.hasOption(option.getLongOpt()))
                || (properties != null
                && properties.getProperty(option.getLongOpt()) != null
                && !properties.getProperty(option.getLongOpt()).equals("false"));
    }

    private static String getPriorityOptionValue(Option option, CommandLine lineArgs, Properties properties) {
        if (lineArgs.hasOption(option.getOpt())) {
            return lineArgs.getOptionValue(option.getOpt());
        } else if (properties != null && properties.getProperty(option.getLongOpt()) != null) {
            return properties.getProperty(option.getLongOpt());
        } else {
            return null;
        }
    }

    private static String[] getOptionValues(Option option, CommandLine lineArgs, Properties properties) {
        if (lineArgs.hasOption(option.getOpt())) {
            return lineArgs.getOptionValues(option.getOpt());
        } else if (properties != null && properties.getProperty(option.getLongOpt()) != null) {
            return properties.getProperty(option.getLongOpt()).split(" ");
        } else {
            return null;
        }
    }


    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar mapper.jar <options>\noptions:", options);
    }

    private static void setLoggerLevel(Level level) {
        Logger root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        ((ch.qos.logback.classic.Logger) root).setLevel(level);
    }

}

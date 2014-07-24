package org.bgee.pipeline;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.pipeline.annotations.AnnotationCommon;
import org.bgee.pipeline.annotations.SimilarityAnnotation;
import org.bgee.pipeline.gene.InsertGO;
import org.bgee.pipeline.hierarchicalGroups.ParseOrthoXML;
import org.bgee.pipeline.ontologycommon.OntologyTools;
import org.bgee.pipeline.species.GenerateTaxonOntology;
import org.bgee.pipeline.species.InsertTaxa;
import org.bgee.pipeline.uberon.InsertUberon;
import org.bgee.pipeline.uberon.TaxonConstraints;
import org.bgee.pipeline.uberon.Uberon;
import org.bgee.pipeline.uberon.UberonDevStage;
import org.bgee.pipeline.uberon.UberonSocketTool;

/**
 * Entry point of the Bgee pipeline. It is a really basic tool, only used to dispatch 
 * commands to the relevant classes. It does not handle complex parameters, such as 
 * {@code -option myvalue}. Only parameter values are provided to the {@code main} 
 * method, so their order does matter.
 * <p>
 * The first argument is always the name of the action to perform, that will allow 
 * this class to know to which class to dispatch the work. Following arguments 
 * are simply the arguments provided to the {@code main} method of the class 
 * which the work is dispatched to. This first action argument will be removed  
 * from the parameter list before being passed to the class doing the work.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class CommandRunner {
    /**
     * {@code Logger} of the class.
     */
    private final static Logger log = 
            LogManager.getLogger(CommandRunner.class.getName());
    
    /**
     * A {@code String} that is used to separate elements from a list when providing 
     * a response to a socket client (see {@link #socketUberonStagesBetween(Uberon, 
     * int)}).
     */
    public static final String SOCKET_RESPONSE_SEPARATOR = "\t";
    
    /**
     * A {@code String} that is the separator between elements of a same list, 
     * when a list needs to be provided as a single argument for a command line usage. 
     * For instance, a list of IDs to provide as a single argument would be: 
     * {@code Id1 + listSeparator + Id2 + listSeparator + ...}.
     * 
     * @see #parseListArgument(String)
     */
    public static final String LIST_SEPARATOR = ",";
    /**
     * A {@code String} that is the separator between a key and its associated value, 
     * in a list of key-value pairs of a map, 
     * when a map needs to be provided as a single argument for a command line usage. 
     * The separator between the different key-value pairs is {@link #LIST_SEPARATOR}. 
     * For instance, a map of IDs to provide as a single argument would be: 
     * {@code Id1 + KEY_VALUE_SEPARATOR + Id2 + listSeparator + Id3 + KEY_VALUE_SEPARATOR + Id4 + ...}.
     * 
     * @see #parseMapArgument(String)
     */
    public static final String KEY_VALUE_SEPARATOR = "/";
    /**
     * A {@code String} that is the separator between different values associated to a same key, 
     * in a list of key-value pairs of a map, 
     * when a map needs to be provided as a single argument for a command line usage. 
     * 
     * @see #KEY_VALUE_SEPARATOR
     */
    public static final String VALUE_SEPARATOR = "--";
    /**
     * A {@code String} that represents the character to provide an empty list, as argument 
     * of command line usage.
     * 
     * @see #parseListArgument(String)
     */
    public static final String EMPTY_LIST = "-";
    
    
    /**
     * Entry point method of the Bgee pipeline. The first element in {@code args} 
     * should be the name of the action to perform (most of the time, it is 
     * the simple name of the class that will perform the action). All following 
     * elements should be the arguments expected by the {@code main} method 
     * of the class performing the action. 
     * <p>
     * An exception is if the first element in {@code args} is equal to 
     * {@code socketUberonStagesBetween}. In that case, following arguments must be the path 
     * to the Uberon developmental stage ontology, and the port number to use to connect 
     * through sockets. 
     * <p>
     * This {@code main} method does not parse {@code args} to allow the use 
     * of option names (as for instance, {@code -option myvalue}). So parameters 
     * must be provided in expected order. 
     * 
     * @param args          {@code Array} of {@code String}s containing the parameters. 
     *                      First element should be the name of the action to perform 
     *                      (usually, it is the simple name of the targeted class).
     * @throws IllegalArgumentException If {@code args} does not contain the 
     *                                  expected parameters.
     * @throws Exception                Any kind of {@code Exception} thrown by 
     *                                  the class performing the action.
     */
    public static void main(String[] args) throws IllegalArgumentException, Exception {
        log.entry((Object[]) args);
        
        if (args.length < 1) {
            throw log.throwing(new IllegalArgumentException("At least one argument " +
            		"must be provided to determine the job requested to the pipeline."));
        }
        
        //make a new String array from args with first element removed
        String[] newArgs = new String[args.length - 1];
        for (int i = 1; i < args.length; i++) {
            newArgs[i-1] = args[i];
        }
        
        
        //now choose the class to dispatch the work
        switch(args[0]) {
        
        //---------- species and taxonomy -----------
        case "GenerateTaxonOntology": 
            GenerateTaxonOntology.main(newArgs);
            break;
        case "InsertTaxa": 
            InsertTaxa.main(newArgs);
            break;
            
        //---------- uberon -----------
        case "TaxonConstraints": 
            TaxonConstraints.main(newArgs);
            break;
        case "Uberon": 
            Uberon.main(newArgs);
            break;
        case "UberonDevStage": 
            UberonDevStage.main(newArgs);
            break;
        case "InsertUberon": 
            InsertUberon.main(newArgs);
            break;
        case "UberonSocketTool": 
            UberonSocketTool.main(newArgs);
            break;
            
        //---------- Similarity annotation -----------
        case "SimilarityAnnotation": 
            SimilarityAnnotation.main(newArgs);
            break;
            
        //---------- General annotations -----------
        case "AnnotationCommon": 
            AnnotationCommon.main(newArgs);
            break;
            
        //---------- Genes -----------
        case "InsertGO": 
            InsertGO.main(newArgs);
            break;
        case "OntologyTools": 
            OntologyTools.main(newArgs);
            break;
            
        //---------- Hierarchical groups -----------
        case "ParseOrthoXML":
            ParseOrthoXML.main(newArgs);
            break;
            
        default: 
            throw log.throwing(new UnsupportedOperationException("The following action " +
                    "is not recognized: " + args[0]));
        }
        
        log.exit();
    }
    
    /**
     * Delegates to {@link #parseListArgument(String, Class)} with {@code Class} argument 
     * being {@code String.class}.
     * 
     * @param listArg    See same name argument in {@link #parseListArgument(String, Class)}.
     * @return           See returned value in {@link #parseListArgument(String, Class)}.
     */
    public static List<String> parseListArgument(String listArg) {
        log.entry(listArg);
        
        return log.exit(CommandRunner.parseListArgument(listArg, String.class));
    }
    /**
     * Delegates to {@link #parseListArgument(String, Class)} with {@code Class} argument 
     * being {@code Integer.class}.
     * 
     * @param listArg    See same name argument in {@link #parseListArgument(String, Class)}.
     * @return           See returned value in {@link #parseListArgument(String, Class)}.
     */
    public static List<Integer> parseListArgumentAsInt(String listArg) {
        log.entry(listArg);
        
        return log.exit(CommandRunner.parseListArgument(listArg, Integer.class));
    }
    /**
     * Split {@code listArg} based on {@link #LIST_SEPARATOR}. The resulting {@code value}s
     * casted to type {@code T} 
     * are returned as a {@code List}, in the order they were obtained from {@code listArg}.
     * This method is used when a list needs to be provided as a single argument, 
     * for a command line usage. 
     * 
     * @param listArg   A {@code String} corresponding to a list of elements separated by 
     *                  {@link #LIST_SEPARATOR}.
     * @return          A {@code List} of {@code T}s that are the result of the split 
     *                  of {@code listArg}, according to {@code LIST_SEPARATOR}.
     */
    private static <T> List<T> parseListArgument(String listArg, Class<T> type) {
        log.entry(listArg, type);
        
        List<T> resultingList = new ArrayList<T>();
        listArg = listArg.trim();
        if (!listArg.equals(EMPTY_LIST)) {
            for (String arg: listArg.split(LIST_SEPARATOR)) {
                if (StringUtils.isNotBlank(arg)) {
                    String value = arg.trim();
                    if (type.equals(Integer.class)) {
                        resultingList.add(type.cast(Integer.parseInt(value)));
                    } else if (type.equals(Boolean.class)) {
                        resultingList.add(type.cast(Boolean.parseBoolean(value)));
                    } else {
                        resultingList.add(type.cast(value));
                    }
                }
            }
        }
        
        return log.exit(resultingList);
    }
    
    /**
     * Delegates to {@link #parseMapArgument(String, Class)} with {@code Class} argument 
     * being {@code String.class}.
     * 
     * @param mapArg    See same name argument in {@link #parseMapArgument(String, Class)}.
     * @return          See returned value in {@link #parseMapArgument(String, Class)}.
     */
    public static Map<String, Set<String>> parseMapArgument(String mapArg) {
        log.entry(mapArg);
        return log.exit(CommandRunner.parseMapArgument(mapArg, String.class));
    }
    /**
     * Delegates to {@link #parseMapArgument(String, Class)} with {@code Class} argument 
     * being {@code Integer.class}.
     * 
     * @param mapArg    See same name argument in {@link #parseMapArgument(String, Class)}.
     * @return          See returned value in {@link #parseMapArgument(String, Class)}.
     */
    public static Map<String, Set<Integer>> parseMapArgumentAsInteger(String mapArg) {
        log.entry(mapArg);
        return log.exit(CommandRunner.parseMapArgument(mapArg, Integer.class));
    }
    
    /**
     * Split {@code mapArg} representing a map in a command line argument, where 
     * key-value pairs are separated by {@link #LIST_SEPARATOR}, and keys  
     * are separated from their associated value by {@link #KEY_VALUE_SEPARATOR}. 
     * A same key can be associated to several values, this why values of the returned 
     * {@code Map} are {@code Set}s of {@code T}s. Values will be casted to the same type 
     * as {@code type}. Are currently supported: {@code String.class}, {@code Integer.class}, 
     * {@code Boolean.class}. 
     * 
     * @param mapArg    A {@code String} corresponding to a map, see {@link #KEY_VALUE_SEPARATOR} 
     *                  for an example.
     * @param type      The desired returned type of values.
     * @return          A {@code Map} resulting from the split of {@code mapArg}, where keys 
     *                  are {@code String}s that are associated to a {@code Set} of {@code T}s.
     * @see #KEY_VALUE_SEPARATOR
     */
    private static <T> Map<String, Set<T>> parseMapArgument(String mapArg, Class<T> type) {
        log.entry(mapArg, type);
        
        Map<String, Set<T>> resultingMap = new HashMap<String, Set<T>>();
        mapArg = mapArg.trim();
        if (!mapArg.equals(EMPTY_LIST)) {
            for (String arg: mapArg.split(LIST_SEPARATOR)) {
                if (StringUtils.isNotBlank(arg)) {
                    String[] keyValue = arg.split(KEY_VALUE_SEPARATOR);
                    
                    if (keyValue.length != 2 || StringUtils.isBlank(keyValue[0]) || 
                            StringUtils.isBlank(keyValue[1])) {
                        throw log.throwing(new IllegalArgumentException("Incorrect format " +
                                "for a key-value pair in a Map command line argument: " + 
                                arg));
                    }
                        
                    String key = keyValue[0].trim();
                    Set<T> existingValues = resultingMap.get(key);
                    if (existingValues == null) {
                        existingValues = new HashSet<T>();
                        resultingMap.put(key, existingValues);
                    }
                    if (!keyValue[1].trim().equals(EMPTY_LIST)) {
                        for (String value: keyValue[1].trim().split(VALUE_SEPARATOR)) {
                            if (type.equals(Integer.class)) {
                                existingValues.add(type.cast(Integer.parseInt(value)));
                            } else if (type.equals(Boolean.class)) {
                                existingValues.add(type.cast(Boolean.parseBoolean(value)));
                            } else {
                                existingValues.add(type.cast(value));
                            }
                        }
                    }
                }
            }
        }
        
        return log.exit(resultingMap);
    }
}

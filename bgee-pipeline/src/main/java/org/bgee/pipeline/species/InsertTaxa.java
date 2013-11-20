package org.bgee.pipeline.species;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.exception.DAOException;
import org.bgee.model.dao.api.species.SpeciesTO;
import org.bgee.model.dao.api.species.TaxonTO;
import org.bgee.model.dao.mysql.connector.MySQLDAOManager;
import org.bgee.pipeline.MySQLDAOUser;
import org.bgee.pipeline.OntologyUtils;
import org.bgee.pipeline.Utils;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.UnknownOWLOntologyException;

import owltools.graph.OWLGraphManipulator;
import owltools.graph.OWLGraphWrapper;
import owltools.graph.OWLGraphWrapper.ISynonym;
import owltools.sim.SimEngine;

/**
 * Class responsible for inserting species and related NCBI taxonomy into 
 * the Bgee database. This class uses a file containing the IDs of the species 
 * to insert into Bgee, and a simplified version of the NCBI taxonomy stored 
 * as an ontology file. 
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class InsertTaxa extends MySQLDAOUser {
    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(InsertTaxa.class.getName());
    
    /**
     * A {@code String} defining the category of the synonym providing the common 
     * name of taxa in the taxonomy ontology. 
     * See {@code owltools.graph.OWLGraphWrapper.ISynonym}.
     */
    private static final String SYNCOMMONNAMECAT = "genbank_common_name";
    
    /**
     * A {@code OWLGraphWrapper} wrapping the NCBI taxonomy {@code OWLOntology}.
     * This attribute is set by the method {@link #loadTaxOntology(String)}, 
     * and is then used by subsequent methods called.
     */
    private OWLGraphWrapper taxOntWrapper;
    /**
     * Default constructor. 
     */
    public InsertTaxa() {
        super();
    }
    /**
     * Constructor providing the {@code MySQLDAOManager} that will be used by 
     * this object to perform queries to the database. This is useful for unit testing.
     * 
     * @param manager   the {@code MySQLDAOManager} to use.
     */
    public InsertTaxa(MySQLDAOManager manager) {
        super(manager);
    }
    
    /**
     * Main method to trigger the insertion of species and taxonomy into the Bgee 
     * database. Parameters that must be provided in order in {@code args} are: 
     * <ol>
     * <li>path to the tsv files containing the IDs of the species used in Bgee, 
     * corresponding to the NCBI taxonomy ID (e.g., 9606 for human). This is the file 
     * to modify to add/remove a species. The first line should be a header line, 
     * defining a column to get IDs from, named exactly "taxon ID" (other columns 
     * are optional and will be ignored).
     * <li>path to the tsv files containing the IDs of the taxa to be inserted in Bgee, 
     * corresponding to the NCBI taxonomy ID (e.g., 9605 for homo). Whatever the values 
     * in this file are, the branches which the species used in Bgee belong to 
     * will be inserted in any case. Using this file, you can specify additional taxa 
     * to be inserted (along with their ancestors), which is useful, for instance 
     * to back up our homology annotations. It does not matter whether this file 
     * also includes the species used in Bgee, or their ancestors. The first line 
     * of this file should be a header line, efining a column to get IDs from, named 
     * exactly "taxon ID" (other columns are optional and will be ignored).
     * <li>path to the file storing the NCBI taxonomy as an ontology. This taxonomy 
     * must contain all the branches related to the species and taxa to be used 
     * in Bgee.
     * </ol>
     * 
     * @param args  An {@code Array} of {@code String}s containing the requested parameters.
     * @throws FileNotFoundException        If some files could not be found.
     * @throws IOException                  If some files could not be used.
     * @throws OWLOntologyCreationException If an error occurred while loading 
     *                                      the NCBI taxonomy ontology.
     * @throws OBOFormatParserException     If an error occurred while loading 
     *                                      the NCBI taxonomy ontology.
     * @throws IllegalArgumentException     If the files used provided invalid information.
     * @throws DAOException                 If an error occurred while inserting 
     *                                      the data into the Bgee database.
     */
    public static void main(String[] args) throws FileNotFoundException, 
        OWLOntologyCreationException, OBOFormatParserException, IllegalArgumentException, 
        DAOException, IOException {
        log.entry((Object[]) args);
        int expectedArgLength = 3;
        if (args.length != expectedArgLength) {
            throw log.throwing(new IllegalArgumentException("Incorrect number of arguments " +
                    "provided, expected " + expectedArgLength + " arguments, " + args.length + 
                    " provided."));
        }
        
        InsertTaxa insert = new InsertTaxa();
        insert.insertSpeciesAndTaxa(args[0], args[1], args[2]);
        
        log.exit();
    }
    
    /**
     * Inserts species and taxa into the Bgee database. This method uses: 
     * <ul>
     * <li>the path to the TSV file containing the IDs of the species used in Bgee, 
     * corresponding to their NCBI taxonomy ID (e.g., 9606 for human). This is the file 
     * to modify to add/remove a species. The first line should be a header line, 
     * defining a column to get IDs from, named exactly "taxon ID" (other columns 
     * are optional and will be ignored). 
     * <li>the path to a TSV file containing the NCBI taxonomy IDs of additional taxa 
     * to be inserted. Whatever the values in this file are, the branches which 
     * the species used in Bgee belong to will be inserted in any case. Using this file, 
     * you can specify additional taxa to be inserted (along with their ancestors), 
     * which is useful, for instance to back up our homology annotations. It does not 
     * matter whether this file also includes the species used in Bgee, or their ancestors. 
     * The first line of this file should be a header line, defining a column to get IDs from, 
     * named exactly "taxon ID" (other columns are optional and will be ignored). 
     * This file can contain no taxa, as long as the header line is present.
     * <li>the path to the file storing the NCBI taxonomy as an ontology. This taxonomy 
     * must contain all the branches related to the species and taxa to be used 
     * in Bgee.
     * </ul>
     * 
     * @param speciesFile   A {@code String} that is the path to the TSV file 
     *                      containing the IDs of the species used in Bgee
     * @param taxonFile     A {@code String} that is the path to the TSV file 
     *                      containing the IDs of additional taxa to insert in Bgee.
     * @param ncbiOntFile   A {@code String} that is the path to the NCBI taxonomy 
     *                      ontology.
     * @throws FileNotFoundException        If some files could not be found.
     * @throws IOException                  If some files could not be used.
     * @throws OWLOntologyCreationException If an error occurred while loading 
     *                                      the NCBI taxonomy ontology.
     * @throws OBOFormatParserException     If an error occurred while loading 
     *                                      the NCBI taxonomy ontology.
     * @throws IllegalArgumentException     If the files used provided invalid information.
     * @throws DAOException                 If an error occurred while inserting 
     *                                      the data into the Bgee database.
     */
    public void insertSpeciesAndTaxa(String speciesFile, String taxonFile, 
            String ncbiOntFile) throws FileNotFoundException, IOException, 
            OWLOntologyCreationException, OBOFormatParserException, 
            IllegalArgumentException, DAOException {
        log.entry(speciesFile, taxonFile, ncbiOntFile);
        
        this.insertSpeciesAndTaxa(new Utils().getTaxonIds(speciesFile), 
                new Utils().getTaxonIds(taxonFile), 
                OntologyUtils.loadOntology(ncbiOntFile));
        
        log.exit();
    }
    
    /**
     * Inserts species and taxa into the Bgee database. The arguments are: 
     * <ul>
     * <li>a {@code Set} of {@code Integer}s that are the NCBI taxonomy IDs of the species 
     * used in Bgee, (e.g., 9606 for human)
     * <li>a {@code Set} of {@code Integer}s that are the NCBI taxonomy IDs of additional taxa 
     * to be inserted. Whatever the values in this {@code Set} are, the branches which 
     * the species used in Bgee belong to will be inserted in any case. Using these IDs, 
     * you can specify additional taxa to be inserted (along with their ancestors), 
     * which is useful, for instance to back up our homology annotations. It does not 
     * matter whether these IDs also include the species used in Bgee, or their ancestors.
     * <li>an {@code OWLOntology} representing the NCBI taxonomy ontology. This taxonomy 
     * must contain all the branches related to the species and taxa to be used in Bgee.
     * </ul>
     * 
     * @param speciesIds    a {@code Set} of {@code Integer}s that are the IDs 
     *                      of the species used in Bgee
     * @param taxonIds      a {@code Set} of {@code Integer}s that are the IDs 
     *                      of additional taxa to be inserted in Bgee
     * @param taxOntology   An {@code OWLOntology} that is the NCBI taxonomy 
     *                      ontology.
     * @throws OWLOntologyCreationException If an error occurred while loading 
     *                                      the NCBI taxonomy ontology.
     * @throws IllegalArgumentException     If the arguments provided invalid information.
     * @throws DAOException                 If an error occurred while inserting 
     *                                      the data into the Bgee database.
     */
    public void insertSpeciesAndTaxa(Set<Integer> speciesIds, Set<Integer> taxonIds, 
            OWLOntology taxOntology) throws OWLOntologyCreationException, 
            IllegalArgumentException, DAOException {
        log.entry(speciesIds, taxonIds, taxOntology);
        log.info("Starting insertion of species and taxa...");
        
        //catch any IllegalStateException to wrap it into a IllegalArgumentException 
        //(a IllegalStateException would be generated because the OWLOntology loaded 
        //from ncbiOntFile would be invalid, so it would be a wrong argument)
        try {
            
            //load the NCBI taxonomy ontology
            this.taxOntWrapper = new OWLGraphWrapper(taxOntology);
            
            //get the SpeciesTOs to insert their information into the database
            Set<SpeciesTO> speciesTOs = this.getSpeciesTOs(speciesIds);
            
            //now get the TaxonTOs to insert their information into the database.
            //note that using this method will modify the taxonomy ontology, 
            //by removing the Bgee species from it, and removing all taxa not related 
            //to neither speciesIds nor taxonIds.
            Set<TaxonTO> taxonTOs = this.getTaxonTOs(speciesIds, taxonIds);
            
            //now we start a transaction to insert taxa and species in the Bgee data source.
            //note that we do not need to call rollback if an error occurs, calling 
            //closeDAO will rollback any ongoing transaction
            try {
                this.startTransaction();
                //need to insert the taxa first, because species have a reference 
                //to their parent taxon
                this.getTaxonDAO().insertTaxa(taxonTOs);
                //insert species
                this.getSpeciesDAO().insertSpecies(speciesTOs);
                this.commit();
            } finally {
                this.closeDAO();
            }
        } catch (IllegalStateException e) {
            log.catching(e);
            throw log.throwing(new IllegalArgumentException(
                    "The OWLOntology provided is invalid", e));
        }

        log.info("Done inserting species and taxa.");
        log.exit();
    }
    
    /**
     * Obtain the species with their ID provided in {@code speciesIds} from 
     * the NCBI taxonomy ontology wrapped into {@link #taxOntWrapper}, 
     * converts them into {@code SpeciesTO}s, and returns them in a {@code Set}.
     * 
     * @param speciesIds    a {@code Set} of {@code Integer}s that are the IDs 
     *                      of the species used in Bgee
     * @return  A {@code Set} of {@code SpeciesTO}s corresponding to the species 
     *          retrieved from the taxonomy ontology wrapped into {@link #taxOntWrapper}.
     * @throws IllegalStateException    If the {@code OWLOntology} used, wrapped 
     *                                  into {@link #taxOntWrapper}, does not allow 
     *                                  to properly acquire {@code SpeciesTO}s.
     */
    private Set<SpeciesTO> getSpeciesTOs(Set<Integer> speciesIds) throws IllegalStateException {
        log.entry(speciesIds);
        
        Set<SpeciesTO> speciesTOs = new HashSet<SpeciesTO>();
        for (Integer speciesId: speciesIds) {
            OWLClass species = this.taxOntWrapper.getOWLClassByIdentifier(
                    OntologyUtils.getTaxOntologyId(speciesId));
            if (species == null) {
                throw log.throwing(new IllegalStateException(
                        "The provided species ID " + speciesId + 
                        "corresponds to no taxon in the taxonomy ontology."));
            }
            speciesTOs.add(this.toSpeciesTO(species));
        }
        if (speciesTOs.size() != speciesIds.size()) {
            throw log.throwing(new IllegalStateException("The taxonomy ontology " +
            		"did not allow to acquire all the requested species"));
        }
        return log.exit(speciesTOs);
    }
    
    /**
     * Transforms the provided {@code speciesOWLClass} into a {@code SpeciesTO}. 
     * Unexpected errors would occur if {@code speciesOWLClass} does not correspond 
     * to a species in the NCBO taxonomy ontology.
     * 
     * @param speciesOWLClass   A {@code OWLClass} representing a species in the 
     *                          NCBI taxonomy ontology to be transformed into an 
     *                          {@code SpeciesTO}.
     * @return  A {@code SpeciesTO} corresponding to {@code speciesOWLClass}.
     * @throws IllegalStateException    If the {@code OWLOntology} used, wrapped 
     *                                  into {@link #taxOntWrapper}, does not allow 
     *                                  to identify the parent taxon of 
     *                                  {@code speciesOWLClass}.
     */
    private SpeciesTO toSpeciesTO(OWLClass speciesOWLClass) throws IllegalStateException {
        log.entry(speciesOWLClass);
        
        //we need the parent of this leaf to know the parent taxon ID 
        //of the species
        Set<OWLClass> parents = 
                this.taxOntWrapper.getOWLClassDirectAncestors(speciesOWLClass);
        if (parents.size() != 1) {
            throw log.throwing(new IllegalStateException("The taxonomy ontology " +
                    "has incorrect relations between taxa"));
        }
        //get the NCBI ID of the parent taxon of this species.
        //we retrieve the Integer value of the ID used on the NCBI website, 
        //because this is how we store this ID in the database. But we convert it 
        //to a String because the Bgee classes only accept IDs as Strings.
        String parentTaxonId = String.valueOf(
                OntologyUtils.getTaxNcbiId(this.taxOntWrapper.getIdentifier(
                parents.iterator().next())));
        
        //get the NCBI ID of this species.
        //we retrieve the Integer value of the ID used on the NCBI website, 
        //because this is how we store this ID in the database. But we convert it 
        //to a String because the Bgee classes only accept IDs as Strings.
        String speciesId = String.valueOf(OntologyUtils.getTaxNcbiId(
                this.taxOntWrapper.getIdentifier(speciesOWLClass)));
        
        //get the common name synonym
        String commonName = this.getCommonNameSynonym(speciesOWLClass);
        
        //get the genus and species name from the scientific name
        String scientificName = this.taxOntWrapper.getLabel(speciesOWLClass);
        String[] nameSplit = scientificName.split(" ");
        String genus = nameSplit[0];
        String speciesName = nameSplit[1];
        
        //create and return the SpeciesTO
        return log.exit(new SpeciesTO(speciesId, commonName, genus, 
                speciesName, parentTaxonId));
    }
    
    /**
     * Filter and obtains requested taxa from the NCBI taxonomy ontology wrapped into 
     * {@link #taxOntWrapper}, converts them into {@code TaxonTO}s, and 
     * returns them in a {@code Set}.
     * <p>
     * The ontology wrapped into {@link #taxOntWrapper} will be modified to remove 
     * any taxa not related to the species provided through {@code speciesIds}, 
     * or to the taxa provided through {@code taxonIds} (only those taxa and their 
     * ancestors, and the ancestors of the species used in Bgee, will be kept).
     * Also, the species will be removed from the ontology, in order to compute 
     * the parameters of a nested set model, for the taxa only (the taxonomy 
     * is represented as a nested set model in Bgee, and does not include the species).
     * 
     * @param speciesIds    a {@code Set} of {@code Integer}s that are the IDs 
     *                      of the species used in Bgee
     * @param taxonIds      a {@code Set} of {@code Integer}s that are the IDs 
     *                      of additional taxa to be inserted in Bgee
     * @return  A {@code Set} of {@code TaxonTO}s corresponding to the taxa 
     *          retrieved from the taxonomy ontology wrapped into {@link #taxOntWrapper}.
     * @throws IllegalStateException    If the {@code OWLOntology} used, wrapped 
     *                                  into {@link #taxOntWrapper}, does not allow 
     *                                  to properly acquire any {@code TaxonTO}s.
     */
    private Set<TaxonTO> getTaxonTOs(Set<Integer> speciesIds, Set<Integer> taxonIds) 
            throws IllegalStateException {
        log.entry(speciesIds, taxonIds);
        
        //get the least common ancestors of the species used in Bgee: 
        //we get the least common ancestors of all possible pairs of species), 
        //in order to identify the important branching in the ontology for Bgee.
        Set<OWLClass> lcas = new HashSet<OWLClass>();
        SimEngine se = new SimEngine(this.taxOntWrapper);
        
        for (int speciesId1: speciesIds) {
            OWLClass species1 = this.taxOntWrapper.getOWLClassByIdentifier(
                    OntologyUtils.getTaxOntologyId(speciesId1));
            for (int speciesId2: speciesIds) {
                OWLClass species2 = this.taxOntWrapper.getOWLClassByIdentifier(
                        OntologyUtils.getTaxOntologyId(speciesId2));
                if (species1 == species2) {
                    continue;
                }
                for (OWLObject lca: se.getLeastCommonSubsumers(species1, species2)) {
                    if (lca instanceof OWLClass) {
                        lcas.add((OWLClass) lca);
                    }
                }
            }
        }
        if (lcas.isEmpty()) {
            throw log.throwing(new IllegalStateException("The ontology " +
                    "did not allow to identify any least common ancestors of species used."));
        }
        
        //now keep only the taxa related to speciesIds or taxonIds, and remove 
        //the species used in Bgee in order to compute the parameters 
        //of the nested set model,
        this.filterOntology(speciesIds, taxonIds);
        
        //we want to order the taxa based on their scientific name, so we create 
        //a Comparator. This comparator needs the OWLGraphWrapper, so we make 
        //a final variable for taxOntWrapper
        final OWLGraphWrapper wrapper = this.taxOntWrapper;
        Comparator<OWLClass> comparator = new Comparator<OWLClass>() {
            @Override
            public int compare(OWLClass o1, OWLClass o2) {
                return wrapper.getLabel(o1).compareTo(wrapper.getLabel(o2));
            }
        };
        //now we create a List with OWLClass order based on the comparator
        List<OWLClass> classOrder = 
                new ArrayList<OWLClass>(this.taxOntWrapper.getAllOWLClasses());
        Collections.sort(classOrder, comparator);
        
        //get the parameters for the nested set model
        Map<OWLClass, Map<String, Integer>> nestedSetModelParams;
        try {
            //need an OntologyUtils to perform the operations
            OntologyUtils utils = new OntologyUtils(this.taxOntWrapper);
            nestedSetModelParams = utils.computeNestedSetModelParams(classOrder);
        } catch (UnknownOWLOntologyException | OWLOntologyCreationException e) {
          //should not be thrown, OntologyUtils has been provided directly with 
            //an OWLGraphWrapper
            throw log.throwing(new IllegalStateException("An OWLGraphWrapper should " +
                    "have been arleady privided"));
        }
        
        //OK, now we have everything to instantiate the TaxonTOs
        Set<TaxonTO> taxonTOs = new HashSet<TaxonTO>();
        for (OWLClass taxon: this.taxOntWrapper.getAllOWLClasses()) {
            //get the NCBI ID of this taxon.
            //we retrieve the Integer value of the ID used on the NCBI website, 
            //because this is how we store this ID in the database. But we convert it 
            //to a String because the Bgee classes only accept IDs as Strings.
            String taxonId = String.valueOf(OntologyUtils.getTaxNcbiId(
                    this.taxOntWrapper.getIdentifier(taxon)));
            String commonName = this.getCommonNameSynonym(taxon);
            String scientificName = this.taxOntWrapper.getLabel(taxon);
            Map<String, Integer> taxonParams = nestedSetModelParams.get(taxon);
            
            taxonTOs.add(
                    new TaxonTO(taxonId, 
                    commonName, 
                    scientificName, 
                    taxonParams.get(OntologyUtils.LEFTBOUNDKEY), 
                    taxonParams.get(OntologyUtils.RIGHTBOUNDKEY), 
                    taxonParams.get(OntologyUtils.LEVELKEY), 
                    lcas.contains(taxon)));
        }
        
        if (taxonTOs.isEmpty()) {
            throw log.throwing(new IllegalStateException("The taxonomy ontology " +
                    "did not allow to acquire any taxon"));
        }
        
        return log.exit(taxonTOs);
    }
    
    /**
     * Modifies the {@code OWLOntology} wrapped into {link #taxOntWrapper} to remove 
     * any taxa not related to the species provided through {@code speciesIds}, 
     * or to the taxa provided through {@code taxonIds} (only those taxa and their 
     * ancestors, and the ancestors of the species used in Bgee, will be kept).
     * Also, the species will be removed from the ontology, in order to compute 
     * the parameters of a nested set model, for the taxa only (the taxonomy 
     * is represented as a nested set model in Bgee, and does not include the species).
     * The IDs used are NCBI taxonomy IDs (for instance, {@code 9606} for human).
     * 
     * @param ncbiSpeciesIds    A {@code Set} of {@code Integer}s that are the NCBI IDs 
     *                          of the species to be used in Bgee (along with their 
     *                          ancestor taxa).
     * @param taxonIds          A {@code Set} of {@code Integer}s that are the IDs 
     *                          of additional taxa to be inserted in Bgee (along with 
     *                          their ancestors)
     * @throws IllegalStateException    If the {@code OWLOntology} wrapped into 
     *                                  {link #taxOntWrapper} does not contain 
     *                                  some of the requested species or taxa.
     */
    private void filterOntology(Set<Integer> ncbiSpeciesIds, Set<Integer> ncbiTaxonIds) 
        throws IllegalStateException {
        log.entry(ncbiSpeciesIds, ncbiTaxonIds);
        log.trace("Filtering ontology to keep only requested species and taxa...");
        
        Set<Integer> allTaxonIds = new HashSet<Integer>(ncbiSpeciesIds);
        allTaxonIds.addAll(ncbiTaxonIds);
        
        Set<OWLClass> owlClassesToKeep = new HashSet<OWLClass>();
        for (int taxonId: allTaxonIds) {
            OWLClass taxClass = this.taxOntWrapper.getOWLClassByIdentifier(
                    OntologyUtils.getTaxOntologyId(taxonId));
            if (taxClass == null) {
                throw log.throwing(new IllegalStateException("Taxon " + taxonId + 
                        " was not found in the ontology"));
            }
            if (!ncbiSpeciesIds.contains(taxonId)) {
                owlClassesToKeep.add(taxClass);
            }
            owlClassesToKeep.addAll(this.taxOntWrapper.getOWLClassAncestors(taxClass));
        }
        OWLGraphManipulator manipulator = new OWLGraphManipulator(this.taxOntWrapper);
        int taxonRemovedCount = manipulator.filterClasses(owlClassesToKeep);
        
        log.trace("Done filtering, {} taxa removed", taxonRemovedCount);
        log.exit();
    }
    
    /**
     * Returns the synonym corresponding to the common name of the provided 
     * {@code owlClass}. The category of such a synonym is {@link #SYNCOMMONNAMECATE}, 
     * see {@code owltools.graph.OWLGraphWrapper.ISynonym}. Returns {@code null} 
     * if no common name synonym was found.
     * 
     * @param owlClass  The {@code OWLClass} which we want to retrieve 
     *                  the common name for.
     * @return          A {@code String} that is the common name of {@code owlClass}.
     */
    private String getCommonNameSynonym(OWLClass owlClass) {
        log.entry(owlClass);
        
        String commonName = null;
        List<ISynonym> synonyms = this.taxOntWrapper.getOBOSynonyms(owlClass);
        if (synonyms != null) {
            for (ISynonym syn: synonyms) {
                if (syn.getCategory().equals(SYNCOMMONNAMECAT)) {
                    commonName = syn.getLabel();
                    break;
                }
            }
        }
        
        return log.exit(commonName);
    }
}

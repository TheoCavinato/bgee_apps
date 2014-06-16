package org.bgee.pipeline.hierarchicalGroups;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.modelmbean.XMLParseException;
import javax.xml.stream.XMLStreamException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.exception.DAOException;
import org.bgee.model.dao.api.gene.GeneDAO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTOResultSet;
import org.bgee.model.dao.api.hierarchicalgroup.HierarchicalGroupDAO.HierarchicalGroupTO;
import org.bgee.model.dao.mysql.connector.MySQLDAOManager;
import org.bgee.pipeline.MySQLDAOUser;

import sbc.orthoxml.Gene;
import sbc.orthoxml.Group;
import sbc.orthoxml.Species;
import sbc.orthoxml.io.OrthoXMLReader;

/**
 * This class parses the OrthoXML file which contains all the data of the hierarchical 
 * orthologous groups obtained from OMA. It retrieves all the data pertaining to the
 * orthologous genes.
 * 
 * @author Komal Sanjeev
 * @author Valentine Rech de Laval
 * @version Bgee 13
 * @since Bgee 13
 */
public class ParseOrthoXML extends MySQLDAOUser {

    private final static Logger log = LogManager.getLogger(ParseOrthoXML.class.getName());
    
    private static int OMANodeId = 1;
    private int nestedSetId = 0;
    private Set<HierarchicalGroupTO> hierarchicalGroupTOs = 
            new HashSet<HierarchicalGroupTO>();
    private Set<GeneTO> geneTOs = new HashSet<GeneTO>();
    private List<String> genesInDb = new ArrayList<String>();

    /**
     * Default constructor. 
     */
    public ParseOrthoXML() {
        super();
    }
    
    /**
     * Constructor providing the {@code MySQLDAOManager} that will be used by 
     * this object to perform queries to the database. This is useful for unit testing.
     * 
     * @param manager   The {@code MySQLDAOManager} to use.
     */
    public ParseOrthoXML(MySQLDAOManager manager) {
        super(manager);
    }

    /**
     * Main method to trigger the insertion of the hierarchical orthologous groups
     * obtained from OMA into the Bgee database. Parameters that must be provided in order
     * in {@code args} are:
     * <ol>
     * <li>path to the file storing the hierarchical orthologous groups in OrthoXML.
     * </ol>
     * 
     * @param args                      An {@code Array} of {@code String}s containing the
     *                                  requested parameters.
     * @throws FileNotFoundException    If some files could not be found.
     * @throws IllegalArgumentException If the files used provided invalid information.
     * @throws DAOException             If an error occurred while getting or updating 
     *                                  the data into the Bgee database.
     * @throws XMLStreamException       If there is an error in the well-formedness of 
     *                                  the XML or other unexpected processing errors.
     * @throws XMLParseException        If there is an error in parsing the XML retrieved
     *                                  by the OrthoXMLReader.
     */
    public static void main(String[] args) throws FileNotFoundException, 
            IllegalArgumentException, DAOException, 
            XMLStreamException, XMLParseException {
        log.entry((Object[]) args);
        
        int expectedArgLength = 1;
        if (args.length != expectedArgLength) {
            throw log.throwing(new IllegalArgumentException("Incorrect number of " +
                    "arguments provided, expected " + expectedArgLength + " arguments, " +
                    args.length + " provided."));
        }
    
        ParseOrthoXML parser = new ParseOrthoXML();
        parser.parseXML(args[0]);
        
        log.exit();
    }

    /**
     * Performs the complete task of reading the Hierarchical Groups orthoxml file and
     * adding the data into the database.
     * <p>
     * This method reads the Hierarchical Orthologous Groups OrthoXML file, and adds all 
     * the data required to the database. It first retrieves genes id from Ensembl of the 
     * Bgee database to be able to check if genes IDs of the file are Ensembl ID .Then it
     * iterates through all the orthologous groups present in the file and builds a
     * {@code Collection} of {@code HierarchicalGroupTO}s as a nested set model and a
     * {@code Collection} of {@code GeneTO}s.
     * <p>
     * After the data is added to the OMAHierarchicalGroup table and the gene table is
     * updated.
     * 
     * @param orthoXMLFile  A {@code String} that is the path to the OMA groups file.
     * @throws DAOException             If an error occurred while getting or updating the 
     *                                  data into the Bgee database.
     * @throws FileNotFoundException    If some files could not be found.
     * @throws XMLStreamException       If there is an error in the well-formedness of the
     *                                  XML or other unexpected processing errors.
     * @throws XMLParseException        If there is an error in parsing the XML retrieved
     *                                  by the OrthoXMLReader.
     */
    public void parseXML(String orthoXMLFile) throws DAOException, FileNotFoundException, 
            XMLStreamException, XMLParseException {  
        log.entry();
        log.info("Start parsing of OrthoXML file...");

        // Catch any IllegalStateException to wrap it into a IllegalArgumentException 
        // (a IllegalStateException would be generated because the OrthoXML groups
        // loaded from the file would be invalid, so it would be a wrong argument).
        try {
            // Retrieve genes id from Ensembl of the Bgee database to be able to check
            // if OMA genes are Ensembl ID and update OMAGroupId in gene table.
            this.getGenesFromDb();

            // Construct HierarchicalGroupTOs and GeneTOs
            this.generateTOsFromFile(orthoXMLFile);

            // Start a transaction to insert HierarchicalGroupTOs and update GeneTOs
            // in the Bgee data source.Note that we do not need to call rollback if
            // an error occurs, calling closeDAO will rollback any ongoing transaction.
            int nbInsertedGenes = 0, nbUpdatedGenes = 0;
            try {
                this.startTransaction();

                log.info("Start inserting of hierarchical groups...");
                nbInsertedGenes = this.getHierarchicalGroupDAO()
                        .insertHierarchicalGroups(hierarchicalGroupTOs);
                log.info("Done inserting hierarchical groups");

                log.info("Start updating genes...");
                nbUpdatedGenes = this.getGeneDAO().updateGenes(geneTOs,
                        Arrays.asList(GeneDAO.Attribute.OMAPARENTNODEID));
                log.info("Done updating genes.");

                this.commit();
            } finally {
                this.closeDAO();
            }
            log.info("Done parsing of OrthoXML file: {} hierarchical groups inserted and "
                    + "{} genes inserted.", nbInsertedGenes, nbUpdatedGenes);
        } catch (IllegalStateException e) {
            log.catching(e);
            throw log.throwing(new IllegalArgumentException(
                    "The OrthoXML provided is invalid", e));
        }
        log.exit();
    }

    /**
     * Retrieves all gene IDs present into the Bgee database.
     * 
     * @throws DAOException     If an error occurred while getting the data from the Bgee
     *                          database.
     */
    private void getGenesFromDb() throws DAOException {
        log.entry();
        try {
            log.info("Start getting gene IDs...");
            this.getGeneDAO().setAttributes(Arrays.asList(GeneDAO.Attribute.ID));
            GeneTOResultSet rsGenes = this.getGeneDAO().getAllGenes();
            while (rsGenes.next()) {
                genesInDb.add(rsGenes.getTO().getId());
            }
            log.info("Done getting gene IDs");
        } finally {
            this.closeDAO();
        }
        log.info("Done getting gene IDs, {} genes found", genesInDb.size());
        log.exit();
    }

    /**
     * Extract all relevant information from the OrthoXML file and fill the
     * {@code Collection} of {@code HierarchicalGroupTO}s as a nested set model and the
     * {@code Collection} of {@code GeneTO}s.
     * 
     * @param orthoXMLFile  A {@code String} that is the path to the OMA groups file.
     * @throws FileNotFoundException    If some files could not be found.
     * @throws XMLStreamException       If there is an error in the well-formedness of the 
     *                                  XML or other unexpected processing errors.
     * @throws XMLParseException        If there is an error in parsing the XML retrieved
     *                                  by the OrthoXMLReader.
     */
    private void generateTOsFromFile(String orthoXMLFile) throws FileNotFoundException,
            XMLStreamException, XMLParseException {
        log.entry();
        OrthoXMLReader reader = new OrthoXMLReader(new File(orthoXMLFile));
        Group group;
        // Read all the groups in the file iteratively
        while ((group = reader.next()) != null) {
            this.generateTOsFromGroup(group, Integer.parseInt(group.getId()));
            nestedSetId++;
        }
        log.info("Done retrieving hierarchical groups.");
        log.exit();
    }
    
    /**
     * Extract all relevant information from a {@code Group} and fill the
     * {@code Collection} of {@code HierarchicalGroupTO}s as a nested set model and the
     * {@code Collection} of {@code GeneTO}s.
     * 
     * @param group         A {@code Group} that is the path to the OMA groups file.
     * @param OMAGroupId    An {@code int} that the OMA group ID to use for subgroups.
     */
    private void generateTOsFromGroup(Group group, int OMAGroupId) {
        nestedSetId++;
        // Add a HierarchicalGroupTO in collection. We remove 1 to the count to get
        // the number of children i.e. to remove the current group.
        this.addHierarchicalGroupTO(OMAGroupId, group.getProperty("TaxRange"),
                countChildren(group));
        if (group.getGenes() != null) {
            for (Gene groupGene : group.getGenes()) {
                // Parse gene identifiers (named protId in OrthoXML file)
                List<String> genes = Arrays.asList(
                        groupGene.getProteinIdentifier().split("; "));
                for (String geneId : genes) {
                    // Check if that identifier is already in our data source
                    if (genesInDb.contains(geneId)) {
                        // Add new {@code GeneTO} to {@code Collection} of {@code GeneTO}s
                        // to be able to update OMAGroupId in gene table.
                        geneTOs.add(new GeneTO(geneId, "", "", 0, 0, OMANodeId, true));
                        break;
                    }
                }
            }
        }
        // Incrementing the node ID. Done after to be able to set OMA parent node ID
        // into gene table
        OMANodeId++;
        if (group.getChildren() != null && group.getChildren().size() > 0) {
            for (Group childGroup : group.getChildren()) {
                // Recurse
                generateTOsFromGroup(childGroup, OMAGroupId);
                nestedSetId++;
            }
        }
    }

    /**
     * Given a OMAGroupId with the taxonomy range and a number of children, calculate
     * nested set bounds and fill the {@code Collection} of {@code HierarchicalGroupTO}s
     * as a nested set model.
     * 
     * @param OMAGroupId    An {@code int} that is the OMA group ID.
     * @param taxRange      A {@code String} that is the taxonomy range of the
     *                      {@code HierarchicalGroupTO} to create.
     * @param nbChild       An {@code int} that is the number of children of the
     *                      {@code HierarchicalGroupTO} to create.
     */
    private int[] addHierarchicalGroupTO(int OMAGroupId, String taxRange, int nbChild) {
        log.entry(OMAGroupId, taxRange, nbChild);
        // Left
        int left = nestedSetId;
        // Right = left + 2 * number of children + 1;
        int right = nestedSetId + 2 * nbChild + 1;
        log.debug("OMANodeId={}, OMAGroupId={}, left={}, right={}, taxRange={}",
                OMANodeId, OMAGroupId, left, right, taxRange);
        hierarchicalGroupTOs.add(new HierarchicalGroupTO(OMANodeId, OMAGroupId, left,
                right, taxRange));
        int[] bounds = new int[2];
        bounds[0] = left;
        bounds[1] = right;
        return log.exit(bounds);
    }

    /**
     * Counts the number of child {@code Group}s.
     * <p>
     * This method recursively iterates through all subgroups of {@code Group} passed as a
     * parameter and returns the total number of subgroups (children) of that
     * {@code Group}.
     * 
     * @param group The {@code Group} whose the number of child {@code Group}s are to be
     *              counted.
     * @return      An {@code int} giving the total number of child {@code Group}s in the
     *              given {@code Group}.
     */
    private static int countChildren(Group group) {
        log.entry(group);
        int c = 1;
        for (Group childGroup : group.getChildren()) {
            c += countChildren(childGroup);
        }
        if (group.getParent() == null) {
            c--;
        }
        return log.exit(c);
    }

    /**
     * Reads the species IDs of all the species present in the OrthoXML file.
     * 
     * @throws XMLParseException        If there is an error in parsing the XML retrieved
     *                                  by the OrthoXMLReader.
     * @throws XMLStreamException       If there is an error in the well-formedness of the
     *                                  XML or other unexpected processing errors.
     * @throws FileNotFoundException    If the OrthoXMLReader cannot find the file.
     */
    public static ArrayList<String> getSpecies(File file) throws FileNotFoundException,
            XMLStreamException, XMLParseException {
        log.entry();
        // Read the species iteratively
        OrthoXMLReader reader = new OrthoXMLReader(file);
        List<Species> species = new ArrayList<Species>();
        species = reader.getSpecies();
        ArrayList<String> speciesIds = new ArrayList<String>();
        for (Species specie : species) {
            speciesIds.add(specie.getName());
        }
        return log.exit(speciesIds);
    }
}

package org.bgee.model.dao.mysql.gene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.gene.GeneDAO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTOResultSet;
import org.bgee.model.dao.mysql.MySQLDAO;
import org.bgee.model.dao.mysql.MySQLITAncestor;
import org.bgee.model.dao.mysql.connector.BgeePreparedStatement;
import org.junit.Test;

/**
 * Integration tests for {@link MySQLGeneDAO}, performed on a real MySQL database. 
 * See the documentation of {@link org.bgee.model.dao.mysql.MySQLITAncestor} for 
 * important information.
 * 
 * @author Valentine Rech de Laval
 * @version Bgee 13
 * @since Bgee 13
 */

public class MySQLGeneDAOIT extends MySQLITAncestor {
    
    private final static Logger log = 
            LogManager.getLogger(MySQLGeneDAOIT.class.getName());

    public MySQLGeneDAOIT() {
        super();
    }
    
    @Override
    protected Logger getLogger() {
        return log;
    }

    /**
     * Test the select method {@link MySQLGeneDAO#getAllGenes()}.
     */
    @Test
    public void shouldGetAllGenes() throws SQLException {
        log.entry();
        this.getMySQLDAOManager().setDatabaseToUse(System.getProperty(POPULATEDDBKEYKEY));
        // TODO Populate database if empty in a @BeforeClass
        // in MySQLITAncestor instead here
        try (BgeePreparedStatement stmt = this.getMySQLDAOManager().getConnection().
                prepareStatement("select 1 from " + MySQLDAO.DATA_SOURCE_TABLE_NAME)) {
            if (!stmt.getRealPreparedStatement().executeQuery().next()) {
                this.populateAndUseDatabase(System.getProperty(POPULATEDDBKEYKEY));
            }
        }

        // Generate result with the method
        MySQLGeneDAO dao = new MySQLGeneDAO(this.getMySQLDAOManager());
        GeneTOResultSet methResults = dao.getAllGenes();

        // Generate manually expected result
        List<GeneTO> expectedGenes = Arrays.asList(
                new GeneTO("ID1", "genN1", "genDesc1", 11, 12, 2, true), 
                new GeneTO("ID2", "genN2", "genDesc2", 21, 0, 0, true), 
                new GeneTO("ID3", "genN3", "genDesc3", 31, 0, 3, false)); 

        while (methResults.next()) {
            boolean found = false;
            GeneTO methGene = methResults.getTO();
            for (GeneTO expGene: expectedGenes) {
                log.trace("Comparing {} to {}", methGene, expGene);
                if (areGeneTOsEqual(methGene, expGene)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                log.debug("No equivalent gene found for {}", methGene);
                throw log.throwing(new AssertionError("Incorrect generated TO"));
            }
        }
        methResults.close();
        log.exit();
    }

    /**
     * Method to compare two {@code GeneTO}s, to check for complete equality of each
     * attribute. This is because the {@code equals} method of {@code GeneTO}s is solely
     * based on their ID, not on other attributes.
     * 
     * @param geneTO1 A {@code GeneTO} to be compared to {@code geneTO2}.
     * @param geneTO2 A {@code GeneTO} to be compared to {@code geneTO1}.
     * @return {@code true} if {@code geneTO1} and {@code geneTO2} have all attributes
     *         equal.
     */
    private boolean areGeneTOsEqual(GeneTO geneTO1, GeneTO geneTO2) {
        log.entry(geneTO1, geneTO2);
        if (geneTO1.getId().equals(geneTO2.getId()) && 
            (geneTO1.getName() == null && geneTO2.getName() == null || 
              geneTO1.getName() != null && geneTO1.getName().equals(geneTO2.getName())) && 
            geneTO1.getSpeciesId() == geneTO2.getSpeciesId() && 
            geneTO1.getGeneBioTypeId() == geneTO2.getGeneBioTypeId() && 
            geneTO1.getOMAParentNodeId() == geneTO2.getOMAParentNodeId() && 
            geneTO1.isEnsemblGene() == geneTO2.isEnsemblGene()) {
            return log.exit(true);
        }
        log.debug("Genes are not equivalent {}", geneTO1.getOMAParentNodeId());
        return log.exit(false);
    }

    /**
     * Test the select method {@link MySQLGeneDAO#updateGenes()}.
     * @throws SQLException 
     */
    @Test
    public void shouldUpdateGenes() throws SQLException {
        log.entry();
        this.populateAndUseDatabase(System.getProperty(EMPTYDBKEY));

        Collection<GeneTO> geneTOs = Arrays.asList(
                new GeneTO("ID1", "GNMod1", "DescMod1", 31, 12, 7, true),
                new GeneTO("ID2", "GNMod2", "DescMod2", 11, 12, 6, false));
        
        Collection<GeneDAO.Attribute> attributesToUpdate1 = Arrays.asList(
                GeneDAO.Attribute.OMAPARENTNODEID);
        Collection<GeneDAO.Attribute> attributesToUpdate2 = Arrays.asList(
                GeneDAO.Attribute.NAME, GeneDAO.Attribute.DESCRIPTION,
                GeneDAO.Attribute.SPECIESID, GeneDAO.Attribute.GENEBIOTYPEID,
                GeneDAO.Attribute.OMAPARENTNODEID, GeneDAO.Attribute.ENSEMBLGENE);
        
        try {
            //Test with only one Attribute
            MySQLGeneDAO dao = new MySQLGeneDAO(this.getMySQLDAOManager());
            assertEquals("Incorrect number of rows inserted", 2, 
                    dao.updateGenes(geneTOs, attributesToUpdate1));

            try (BgeePreparedStatement stmt = this.getMySQLDAOManager().getConnection().
                    prepareStatement("select 1 from " + MySQLDAO.GENE_TABLE_NAME + 
                            " where " + dao.getLabel(GeneDAO.Attribute.ID) + " = ? and " +
                            dao.getLabel(GeneDAO.Attribute.OMAPARENTNODEID) + "= ?")) {

                stmt.setString(1, "ID1");
                stmt.setInt(2, 7);
                assertTrue("GeneTO incorrectly updated", 
                        stmt.getRealPreparedStatement().executeQuery().next());

                stmt.setString(1, "ID2");
                stmt.setInt(2, 6);
                assertTrue("GeneTO incorrectly updated", 
                        stmt.getRealPreparedStatement().executeQuery().next());
            }
            
            //Test with all Attributes
            assertEquals("Incorrect number of rows inserted", 2, 
                    dao.updateGenes(geneTOs, attributesToUpdate2));

            try (BgeePreparedStatement stmt = this.getMySQLDAOManager().getConnection().
                    prepareStatement("select 1 from " + MySQLDAO.GENE_TABLE_NAME + 
                            " where " + dao.getLabel(GeneDAO.Attribute.ID) + " = ? and " +
                            dao.getLabel(GeneDAO.Attribute.NAME) + "= ? and " + 
                            dao.getLabel(GeneDAO.Attribute.DESCRIPTION) + " = ? and " +
                            dao.getLabel(GeneDAO.Attribute.SPECIESID) + "= ? and " +
                            dao.getLabel(GeneDAO.Attribute.GENEBIOTYPEID) + "= ? and " +
                            dao.getLabel(GeneDAO.Attribute.OMAPARENTNODEID) + "= ? and " +
                            dao.getLabel(GeneDAO.Attribute.ENSEMBLGENE) + " = ?")) {
                stmt.setString(1, "ID1");
                stmt.setString(2, "GNMod1");
                stmt.setString(3, "DescMod1");
                stmt.setInt(4, 31);
                stmt.setInt(5, 12);
                stmt.setInt(6, 7);
                stmt.setBoolean(7, true);
                assertTrue("GeneTO incorrectly updated", 
                        stmt.getRealPreparedStatement().executeQuery().next());

                stmt.setString(1, "ID2");
                stmt.setString(2, "GNMod2");
                stmt.setString(3, "DescMod2");
                stmt.setInt(4, 11);
                stmt.setInt(5, 12);
                stmt.setInt(6, 6);
                stmt.setBoolean(7, false);
                assertTrue("GeneTO incorrectly updated", 
                        stmt.getRealPreparedStatement().executeQuery().next());
            }
        } finally {
            this.emptyAndUseDefaultDB();
        }
        log.exit();
    }
}

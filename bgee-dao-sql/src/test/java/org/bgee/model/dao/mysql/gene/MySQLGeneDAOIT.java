package org.bgee.model.dao.mysql.gene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.DAOManager;
import org.bgee.model.dao.api.gene.GeneDAO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTO;
import org.bgee.model.dao.api.gene.GeneDAO.GeneTOResultSet;
import org.bgee.model.dao.mysql.MySQLITAncestor;
import org.bgee.model.dao.mysql.connector.BgeePreparedStatement;
import org.bgee.model.dao.mysql.species.MySQLSpeciesDAO;
import org.junit.Test;
import org.mockito.Mockito;

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
	
    private final static Logger log = LogManager.getLogger(MySQLGeneDAOIT.class.getName());

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
//    @Test
    public void testGetAllGenes() throws SQLException {
    	log.entry();
    	this.getMySQLDAOManager().setDatabaseToUse(System.getProperty(POPULATEDDBKEYKEY));

    	// Generate result with the method
    	MySQLGeneDAO dao = new MySQLGeneDAO(this.getMySQLDAOManager());
    	dao.setAttributes(Arrays.asList(GeneDAO.Attribute.ID));
    	GeneTOResultSet methResults = dao.getAllGenes();

    	// Generate manually expected result
    	GeneTO geneTO1 = new GeneTO("ID1", "genN1", "genDesc1", 11, 12, 2, true);
    	GeneTO geneTO2 = new GeneTO("ID2", "genN2", "genDesc2", 21, 0, 0, true);
    	GeneTO geneTO3 = new GeneTO("ID3", "genN3", "genDesc3", 31, 0, 3, false);
    	List<GeneTO> expectedGenes = new ArrayList<>();
    	expectedGenes.add(geneTO1);
    	expectedGenes.add(geneTO2);
    	expectedGenes.add(geneTO3);

    	do {
    	    boolean found = false;
    	    GeneTO methGene = methResults.getTO();
    	    for (GeneTO expGene: expectedGenes) {
    	        log.trace("Comparing {} to {}", methGene, expGene);
    	        if (areGeneTOsEqual(methGene, expGene)) {
    	            found = true;
    	        }
    	    }
    	    if (!found) {
    	        log.debug("No equivalent gene found for {}", methGene);
    	        throw log.throwing(new AssertionError("Incorrect generated TO"));
    	    }
    	}
    	while (methResults.next());

    	methResults.close();
    	log.exit();
    }

    /**
     * Method to compare two {@code GeneTO}s, to check for complete equality of each attribute. 
     * This is because the {@code equals} method of {@code GeneTO}s is solely based on their
     * ID, not on other attributes. 
     * 
     * @param geneTO1	A {@code GeneTO} to be compared to {@code geneTO2}.
     * @param geneTO2	A {@code GeneTO} to be compared to {@code geneTO1}.
     * @return	{@code true} if {@code geneTO1} and {@code geneTO2} have all attributes equal.
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
     * Test the select method {@link MySQLGeneDAO#updateOMAGroupIDs()}.
     * @throws SQLException 
     */
//    @Test
    public void testUpdateGenes() throws SQLException {
    	log.entry();
        this.populateAndUseDatabase(EMPTYDBKEY);

    	//TODO populate update test database
    	Collection<GeneTO> geneTOs = new ArrayList<GeneTO>();
    	geneTOs.add(new GeneTO("ID1", "GNMod1", "DescMod1", 31, 12, 7, true));
    	geneTOs.add(new GeneTO("ID2", "GNMod2", "DescMod2", 11, 12, 6, false));
    	
    	Collection<GeneDAO.Attribute> attributesToUpdate1 = new ArrayList<GeneDAO.Attribute>();
    	attributesToUpdate1.add(GeneDAO.Attribute.OMAPARENTNODEID);
    	Collection<GeneDAO.Attribute> attributesToUpdate2 = new ArrayList<GeneDAO.Attribute>();
    	attributesToUpdate2.add(GeneDAO.Attribute.NAME);
    	attributesToUpdate2.add(GeneDAO.Attribute.DESCRIPTION);
    	attributesToUpdate2.add(GeneDAO.Attribute.SPECIESID);
    	attributesToUpdate2.add(GeneDAO.Attribute.GENEBIOTYPEID);
    	attributesToUpdate2.add(GeneDAO.Attribute.OMAPARENTNODEID);
    	attributesToUpdate2.add(GeneDAO.Attribute.ENSEMBLGENE);
    	
    	try {
    		//Test with on Attribute
    		MySQLGeneDAO dao = new MySQLGeneDAO(this.getMySQLDAOManager());
    		assertEquals("Incorrect number of rows inserted", 2, 
    				dao.updateGenes(geneTOs, attributesToUpdate1));

    		try (BgeePreparedStatement stmt = this.getMySQLDAOManager().getConnection().
    				prepareStatement("select 1 from gene where " +
    						"geneID = ? and OMAParentNodeId= ?")) {

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
    				prepareStatement("select 1 from gene where geneID = ? and geneName = ? "
    						+ "and geneDescription = ? and speciesId= ? and geneBioTypeId= ? "
    						+ "and OMAParentNodeId= ? and ensemblGene = ?")) {
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
    	    this.emptyAndUseDefaultDB(EMPTYDBKEY);
    	}
    	log.exit();
    }
}

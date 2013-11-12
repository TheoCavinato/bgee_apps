package org.bgee.model.dao.mysql.species;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.species.TaxonTO;
import org.bgee.model.dao.mysql.MySQLITAncestor;
import org.bgee.model.dao.mysql.connector.BgeePreparedStatement;
import org.junit.Test;

/**
 * Integration tests for {@link MySQLTaxonDAO}, performed on a real MySQL database. 
 * See the documentation of {@link org.bgee.model.dao.mysql.MySQLITAncestor} for 
 * important information.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class MySQLTaxonDAOIT extends MySQLITAncestor {
    private final static Logger log = LogManager.getLogger(MySQLTaxonDAOIT.class.getName());
    
    /**
     * A {@code String} that is the name of the table into which data are inserted 
     * during testing of {@link MySQLTaxonDAO} methods inserting data.
     */
    private final static String INSERTTABLENAME = "taxon";
    
    public MySQLTaxonDAOIT() {
        super();
    }
    @Override
    protected Logger getLogger() {
        return log;
    }
    
    /**
     * Test the insertion method {@link MySQLSpeciesDAO#insertTaxa(Collection)}.
     */
    @Test
    public void shouldInsertTaxa() throws SQLException {
        this.useEmptyDB();
        //create a Collection of TaxonTOs to be inserted
        Collection<TaxonTO> taxonTOs = new ArrayList<TaxonTO>();
        taxonTOs.add(new TaxonTO("10", "commonName1", "sciName1", 1, 10, 1, true));
        taxonTOs.add(new TaxonTO("50", "commonName2", "sciName2", 2, 5, 2, false));
        taxonTOs.add(new TaxonTO("60", "commonName3", "sciName3", 6, 9, 3, true));
        try {
            MySQLTaxonDAO dao = new MySQLTaxonDAO(this.getMySQLDAOManager());
            assertEquals("Incorrect number of rows inserted", 3, 
                    dao.insertTaxa(taxonTOs));
            
            //we manually verify the insertion, as we do not want to rely on other methods 
            //that are tested elsewhere.
            //This test method could be better written (DRY, ...)
            try (BgeePreparedStatement stmt = this.getMySQLDAOManager().getConnection().
                    prepareStatement("select 1 from taxon where taxonId = ? and " +
                            "taxonCommonName = ? and taxonScientificName = ? and " +
                            "taxonLeftBound = ? and taxonRightBound = ? and taxonLevel = ? " +
                            "and bgeeSpeciesLCA = ?")) {
                
                stmt.setInt(1, 10);
                stmt.setString(2, "commonName1");
                stmt.setString(3, "sciName1");
                stmt.setInt(4, 1);
                stmt.setInt(5, 10);
                stmt.setInt(6, 1);
                stmt.setBoolean(7, true);
                assertTrue("TaxonTO incorrectly inserted", 
                        stmt.getRealPreparedStatement().executeQuery().next());
                
                stmt.setInt(1, 50);
                stmt.setString(2, "commonName2");
                stmt.setString(3, "sciName2");
                stmt.setInt(4, 2);
                stmt.setInt(5, 5);
                stmt.setInt(6, 2);
                stmt.setBoolean(7, false);
                assertTrue("TaxonTO incorrectly inserted", 
                        stmt.getRealPreparedStatement().executeQuery().next());
                
                stmt.setInt(1, 60);
                stmt.setString(2, "commonName3");
                stmt.setString(3, "sciName3");
                stmt.setInt(4, 6);
                stmt.setInt(5, 9);
                stmt.setInt(6, 3);
                stmt.setBoolean(7, true);
                assertTrue("TaxonTO incorrectly inserted", 
                        stmt.getRealPreparedStatement().executeQuery().next());
            }
        } finally {
            this.deleteFromTableAndUseDefaultDB(INSERTTABLENAME);
        }
    }
}
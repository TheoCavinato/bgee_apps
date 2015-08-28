package org.bgee.model.dao.mysql.file;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.TOComparator;
import org.bgee.model.dao.api.file.SpeciesDataGroupDAO;
import org.bgee.model.dao.mysql.MySQLITAncestor;
import org.junit.Test;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * The integration tests for the {@link MySQLSpeciesDataGroupDAO} class.
 * @author Philippe Moret
 */
public class MySQLSpeciesDataGroupDAOIT extends MySQLITAncestor {

    private static final Logger log = LogManager.getLogger(MySQLSpeciesDataGroupDAOIT.class.getName());

    @Override
    protected Logger getLogger() {
        return super.getLogger();
    }

    /**
     * Test the {@link MySQLSpeciesDataGroupDAO#getAllSpeciesDataGroup()} method
     * @throws SQLException
     */
    @Test
    public void testGetAllSpeciesDataGroups() throws SQLException {
        super.useSelectDB();
        MySQLSpeciesDataGroupDAO dao = new MySQLSpeciesDataGroupDAO(getMySQLDAOManager());

        List<SpeciesDataGroupDAO.SpeciesDataGroupTO> list = dao.getAllSpeciesDataGroup().getAllTOs();
        List<SpeciesDataGroupDAO.SpeciesDataGroupTO> expected = Arrays.asList(
                new SpeciesDataGroupDAO.SpeciesDataGroupTO("1", "SingleSpecies1", "SS1 is a ..."),
                new SpeciesDataGroupDAO.SpeciesDataGroupTO("2", "MultiSpecies2", "A multi species group...")
        );

        assertTrue("DownloadFileTOs are incorrectly retrieved\nGOT\n"+list+"\nEXPECTED\n"+expected,
                TOComparator.areTOCollectionsEqual(list, expected));
        assertEquals(list, expected);
    }



}

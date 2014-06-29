package org.bgee.model.dao.mysql.hierarchicalgroup;

import java.sql.SQLException;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.exception.DAOException;
import org.bgee.model.dao.api.gene.HierarchicalGroupDAO;
import org.bgee.model.dao.mysql.MySQLDAO;
import org.bgee.model.dao.mysql.connector.BgeePreparedStatement;
/*
import org.bgee.model.dao.mysql.BgeeConnection;
import org.bgee.model.dao.mysql.BgeePreparedStatement;
import org.bgee.model.dao.sql.*;*/
import org.bgee.model.dao.mysql.connector.MySQLDAOManager;

/**
 * A {@code HierarchicalGroupDAO} for MySQL. 
 * 
 * @author Valentine Rech de Laval
 * @version Bgee 13
 * @see org.bgee.model.dao.api.species.HierarchicalGroupTO
 * @since Bgee 13
 */
public class MySQLHierarchicalGroupDAO extends MySQLDAO<HierarchicalGroupDAO.Attribute>
        implements HierarchicalGroupDAO {

    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(MySQLHierarchicalGroupDAO.class.getName());

    /**
     * Constructor providing the {@code MySQLDAOManager} that this {@code MySQLDAO} will
     * use to obtain {@code BgeeConnection}s.
     * 
     * @param manager the {@code MySQLDAOManager} to use.
     * @throws IllegalArgumentException If {@code manager} is {@code null}.
     */
    public MySQLHierarchicalGroupDAO(MySQLDAOManager manager)
            throws IllegalArgumentException {
        super(manager);
    }
    
    //***************************************************************************
    // METHODS NOT PART OF THE bgee-dao-api, USED BY THE PIPELINE AND NOT MEANT 
    //TO BE EXPOSED TO THE PUBLIC API.
    //***************************************************************************
    /**
     * Inserts the provided Hierarchical Groups into the Bgee database, represented as a
     * {@code Collection} of {@code HierarchicalGroupTO}s.
     * 
     * @param terms         A {@code Collection} of {@code HierarchicalGroupTO}s to be
     *                      inserted into the database.
     * @throws DAOException If a {@code SQLException} occurred while trying to insert
     *                      {@code terms}. The {@code SQLException} will be wrapped into a
     *                      {@code DAOException} ({@code DAOs} do not expose these kind of
     *                      implementation details).
     */
    public int insertHierarchicalGroups(Collection<HierarchicalGroupTO> groups)
            throws DAOException {
    	log.entry(groups);
    	int groupInsertedCount = 0;

    	// To not overload MySQL with an error com.mysql.jdbc.PacketTooBigException, 
    	// and because of laziness, we insert terms one at a time
        String sql = "INSERT INTO OMAHierarchicalGroup ("
                + getLabel(HierarchicalGroupDAO.Attribute.ID) + ", "
                + getLabel(HierarchicalGroupDAO.Attribute.OMA_GROUP_ID) + ", "
                + getLabel(HierarchicalGroupDAO.Attribute.LEFT_BOUND) + ", "
                + getLabel(HierarchicalGroupDAO.Attribute.RIGHT_BOUND) + ", "
                + getLabel(HierarchicalGroupDAO.Attribute.TAXON_ID)
                + " values (?, ?, ?, ?, ?)";

    	try (BgeePreparedStatement stmt = 
    			this.getManager().getConnection().prepareStatement(sql)) {
    		for (HierarchicalGroupTO group: groups) {
    			stmt.setInt(1, Integer.parseInt(group.getId()));
    			stmt.setString(2, group.getOMAGroupId());
    			stmt.setInt(3, group.getLeftBound());
    			stmt.setInt(4, group.getRightBound());
    			stmt.setInt(5, group.getTaxonId());
    			groupInsertedCount += stmt.executeUpdate();
    			stmt.clearParameters();
    		}
    		return log.exit(groupInsertedCount);
    	} catch (SQLException e) {
    		throw log.throwing(new DAOException(e));
    	}
    }

    @Override
    public String getLabel(HierarchicalGroupDAO.Attribute attribute) {
        log.entry(attribute);
        
        String label = null;
        if (attribute.equals(HierarchicalGroupDAO.Attribute.ID)) {
            label = "OMANodeId";
        } else if (attribute.equals(HierarchicalGroupDAO.Attribute.OMA_GROUP_ID)) {
            label = "OMAGroupId";
        } else if (attribute.equals(HierarchicalGroupDAO.Attribute.LEFT_BOUND)) {
            label = "OMANodeLeftBound";
        } else if (attribute.equals(HierarchicalGroupDAO.Attribute.RIGHT_BOUND)) {
            label = "OMANodeRightBound";
        } else if (attribute.equals(HierarchicalGroupDAO.Attribute.TAXON_ID)) {
            label = "taxonId";
        } 
        
        return log.exit(label);
    }
    
    @Override
    public String getSQLExpr(HierarchicalGroupDAO.Attribute attribute) {
        log.entry(attribute);
        //no complex SQL expression in this DAO, we just build table_name.label
        return log.exit(MySQLDAO.HIERARCHICAL_GROUP_TABLE_NAME + "." + 
            this.getLabel(attribute));
    }

//
//	BgeeConnection connection;
//
//	/**
//	 * Retrieves all the orthologous genes corresponding to the queried gene at
//	 * the taxonomy level specified.
//	 * <p>
//	 * This method takes as parameters a {@code String} representing the
//	 * gene ID, and a {@code long} representing the NCBI taxonomy ID for
//	 * the taxonomy level queried. Then, the orthologous genes for the submitted
//	 * gene ID at the particular taxonomy level are retrieved and returned as a
//	 * {@code Collection} of {@code String}.
//	 * 
//	 * @param queryGene
//	 *            A {@code String} representing the gene ID queried, whose
//	 *            orthologous genes are to be retrieved.
//	 * 
//	 * @param ncbiTaxonomyId
//	 *            A {@code long} representing the NCBI taxonomy ID of the
//	 *            hierarchical level queried.
//	 * @return A {@code Collection} of {@code String} containing all
//	 *         the orthologous genes of the query gene corresponding to the
//	 *         taxonomy level queried.
//	 * 
//	 * @throws SQLException
//	 */
//	public ArrayList<String> getHierarchicalOrthologousGenes(String queryGene,
//			String ncbiTaxonomyId) throws SQLException {
//
//		log.entry();
//
//		ArrayList<String> orthologousGenes = new ArrayList<String>();
//
//		String sql = "SELECT t5.* FROM gene AS t1 "
//				+ "INNER JOIN hierarchicalGroup AS t2 ON t1.hierarchicalGroupId = t2.hierarchicalGroupId "
//				+ "INNER JOIN hierarchicalGroup AS t3 "
//				+ "ON t3.hierarchicalGroupLeftBound <= t2.hierarchicalGroupLeftBound AND "
//				+ "t3.hierarchicalGroupRightBound >= t2.hierarchicalGroupRightBound AND "
//				+ "t3.orthologousGroupId = t2.orthologousGroupId "
//				+ "INNER JOIN hierarchicalGroup AS t4 "
//				+ "ON t4.hierarchicalGroupLeftBound >= t3.hierarchicalGroupLeftBound AND "
//				+ "t4.hierarchicalGroupRightBound <= t3.hierarchicalGroupRightBound AND "
//				+ "t4.orthologousGroupId = t3.orthologousGroupId "
//				+ "INNER JOIN gene AS t5 ON t5.hierarchicalGroupId = t4.hierarchicalGroupId "
//				+ "WHERE t1.geneId =? and t3.ncbiTaxonomyId=? ;";
//
//		if (log.isDebugEnabled()) {
//			log.debug("QUERY: {}", sql);
//		}
//
//		try {
//			BgeePreparedStatement preparedStatement = connection
//					.prepareStatement(sql);
//			preparedStatement.setString(1, queryGene);
//			preparedStatement.setString(2, ncbiTaxonomyId);
//			ResultSet resultSet = preparedStatement.executeQuery();
//
//			while (resultSet.next()) {
//				orthologousGenes.add(resultSet.getString("geneId"));
//			}
//
//		} catch (SQLException e) {
//			System.out.println(e.toString());
//		} finally {
//			connection.close();
//		}
//
//		return log.exit(orthologousGenes);
//	}
//
//	/**
//	 * Retrieves all the orthologous genes corresponding to the queried gene at
//	 * the taxonomy level specified, belonging to a list of species.
//	 * <p>
//	 * This method takes as parameters a {@code String} representing the
//	 * gene ID, a {@code long} representing the NCBI taxonomy ID for the
//	 * taxonomy level queried, and an {@code ArrayList} representing the
//	 * list of species whose genes are required. Then, the orthologous genes for
//	 * the submitted gene ID at the particular taxonomy level, belonging to each
//	 * of the species submitted are retrieved and returned as a
//	 * {@code Collection} of {@code String}.
//	 * 
//	 * @param queryGene
//	 *            A {@code String} representing the gene ID queried, whose
//	 *            orthologous genes are to be retrieved.
//	 * 
//	 * @param ncbiTaxonomyId
//	 *            A {@code long} representing the NCBI taxonomy ID of the
//	 *            hierarchical level queried.
//	 * @param speciesIds
//	 *            An {@code ArrayList} representing the list of species
//	 *            whose genes are required
//	 * 
//	 * @return A {@code Collection} of {@code String} containing all
//	 *         the orthologous genes of the query gene corresponding to the
//	 *         taxonomy level queried.
//	 * 
//	 * @throws SQLException
//	 * 
//	 */
//	public ArrayList<String> getHierarchicalOrthologousGenesForSpecies(
//			String queryGene, String ncbiTaxonomyId, ArrayList<Long> speciesIds)
//			throws SQLException {
//
//		log.entry();
//
//		ArrayList<String> orthologousGenes = new ArrayList<String>();
//
//		for (long speciesId : speciesIds) {
//
//			String sql = "SELECT t5.* FROM gene AS t1 "
//					+ "INNER JOIN hierarchicalGroup AS t2 ON t1.hierarchicalGroupId = t2.hierarchicalGroupId "
//					+ "INNER JOIN hierarchicalGroup AS t3 "
//					+ "ON t3.hierarchicalGroupLeftBound <= t2.hierarchicalGroupLeftBound AND "
//					+ "t3.hierarchicalGroupRightBound >= t2.hierarchicalGroupRightBound AND "
//					+ "t3.orthologousGroupId = t2.orthologousGroupId "
//					+ "INNER JOIN hierarchicalGroup AS t4 "
//					+ "ON t4.hierarchicalGroupLeftBound >= t3.hierarchicalGroupLeftBound AND "
//					+ "t4.hierarchicalGroupRightBound <= t3.hierarchicalGroupRightBound AND "
//					+ "t4.orthologousGroupId = t3.orthologousGroupId "
//					+ "INNER JOIN gene AS t5 ON t5.hierarchicalGroupId = t4.hierarchicalGroupId "
//					+ "WHERE t1.geneId =? and t3.ncbiTaxonomyId=? and t5.speciesId=? ;";
//
//			if (log.isDebugEnabled()) {
//				log.debug("QUERY: {}", sql);
//			}
//
//			try {
//				BgeePreparedStatement preparedStatement = connection
//						.prepareStatement(sql);
//				preparedStatement.setString(1, queryGene);
//				preparedStatement.setString(2, ncbiTaxonomyId);
//				preparedStatement.setLong(3, speciesId);
//				ResultSet resultSet = preparedStatement.executeQuery();
//
//				while (resultSet.next()) {
//					orthologousGenes.add(resultSet.getString("geneId"));
//				}
//
//			} catch (SQLException e) {
//				System.out.println(e.toString());
//			} finally {
//				connection.close();
//			}
//		}
//
//		return log.exit(orthologousGenes);
//	}
//
//	/**
//	 * 
//	 * Retrieves all the within species paralogs of the queried gene.
//	 * <p>
//	 * This method takes as parameters a {@code String} representing the
//	 * gene ID. Then, all the within species paralogs of the the submitted gene
//	 * ID are retrieved and returned as a {@code Collection} of
//	 * {@code String}.
//	 * 
//	 * @param queryGene
//	 *            A {@code String} representing the gene ID queried, whose
//	 *            within species paralogs are to be retrieved.
//	 * 
//	 * @return A {@code Collection} of {@code String} containing all
//	 *         the within species paralogs genes of the query gene.
//	 * 
//	 * @throws SQLException
//	 * 
//	 */
//	public ArrayList<String> getWithinSpeciesParalogs(String queryGene)
//			throws SQLException {
//
//		log.entry();
//
//		ArrayList<String> paralogusGenes = new ArrayList<String>();
//
//		String sql = "SELECT t6.* FROM gene AS t6"
//				+ "INNER JOIN hierarchicalGroup AS t5 "
//				+ " ON t6.hierarchicalGroupId = t5.hierarchicalGroupId "
//				+ " INNER JOIN hierarchicalGroup AS t4"
//				+ " ON t4.hierarchicalGroupId = ( "
//				+ " 	SELECT t3.hierarchicalGroupId FROM gene AS t1 "
//				+ " 	INNER JOIN hierarchicalGroup AS t2  "
//				+ " 	ON t1.hierarchicalGroupId = t2.hierarchicalGroupId  "
//				+ " 	INNER JOIN hierarchicalGroup AS t3  "
//				+ " 	ON t2.orthologousGroupId = t3.orthologousGroupId "
//				+ " 	AND  t3.hierarchicalGroupLeftBound < t2.hierarchicalGroupLeftBound "
//				+ " 	AND  t3.hierarchicalGroupRightBound > t2.hierarchicalGroupRightBound "
//				+ " 	WHERE t1.geneId = 'ENSXETG00000028037' AND t3.ncbiTaxonomyId='null'  "
//				+ " 	ORDER BY t3.hierarchicalGroupLeftBound DESC LIMIT 1 "
//				+ " )AND t5.orthologousGroupId=t4.orthologousGroupId "
//				+ " AND  t5.hierarchicalGroupLeftBound > t4.hierarchicalGroupLeftBound "
//				+ " AND  t5.hierarchicalGroupRightBound < t4.hierarchicalGroupRightBound; ";
//
//		if (log.isDebugEnabled()) {
//			log.debug("QUERY: {}", sql);
//		}
//
//		try {
//			BgeePreparedStatement preparedStatement = connection
//					.prepareStatement(sql);
//
//			preparedStatement.setString(1, queryGene);
//
//			ResultSet resultSet = preparedStatement.executeQuery();
//
//			while (resultSet.next()) {
//				paralogusGenes.add(resultSet.getString("geneId"));
//			}
//
//		} catch (SQLException e) {
//			System.out.println(e.toString());
//		} finally {
//			connection.close();
//		}
//
//		return log.exit(paralogusGenes);
//	}
//
//	/**
//	 * Retrieves all the orthologous genes corresponding to the queried gene in
//	 * the closest species.
//	 * <p>
//	 * This method takes as parameters a {@code String} representing the
//	 * gene ID. Then, the orthologous genes for the submitted gene ID belonging
//	 * closest species are retrieved and returned as a {@code Collection}
//	 * of {@code String}.
//	 * 
//	 * @param queryGene
//	 *            A {@code String} representing the gene ID queried, whose
//	 *            orthologous genes in it's closest species are to be retrieved.
//	 * 
//	 * @return A {@code Collection} of {@code String} containing all
//	 *         the orthologous genes of the query gene in the closest species.
//	 * 
//	 * @throws SQLException
//	 * 
//	 */
//	public ArrayList<String> getOrthologsInClosestSpecies(String queryGene)
//			throws SQLException {
//
//		log.entry();
//
//		ArrayList<String> orthologousGenes = new ArrayList<String>();
//
//		String sql = "SELECT t6.* FROM gene AS t6"
//				+ " INNER JOIN hierarchicalGroup AS t5 "
//				+ " ON t6.hierarchicalGroupId = t5.hierarchicalGroupId "
//				+ " INNER JOIN hierarchicalGroup AS t4"
//				+ " ON t4.hierarchicalGroupId = ( "
//				+ " 	SELECT t3.hierarchicalGroupId FROM gene AS t1 "
//				+ " 	INNER JOIN hierarchicalGroup AS t2  "
//				+ " 	ON t1.hierarchicalGroupId = t2.hierarchicalGroupId  "
//				+ " 	INNER JOIN hierarchicalGroup AS t3  "
//				+ " 	ON t2.orthologousGroupId = t3.orthologousGroupId "
//				+ " 	AND  t3.hierarchicalGroupLeftBound < t2.hierarchicalGroupLeftBound "
//				+ " 	AND  t3.hierarchicalGroupRightBound > t2.hierarchicalGroupRightBound "
//				+ " 	WHERE t1.geneId = ? AND t3.ncbiTaxonomyId!='null' "
//				+ " 	ORDER BY t3.hierarchicalGroupLeftBound DESC LIMIT 1 "
//				+ " )AND t5.orthologousGroupId=t4.orthologousGroupId "
//				+ " AND  t5.hierarchicalGroupLeftBound > t4.hierarchicalGroupLeftBound "
//				+ " AND  t5.hierarchicalGroupRightBound < t4.hierarchicalGroupRightBound; ";
//
//		if (log.isDebugEnabled()) {
//			log.debug("QUERY: {}", sql);
//		}
//
//		try {
//			BgeePreparedStatement preparedStatement = connection
//					.prepareStatement(sql);
//
//			preparedStatement.setString(1, queryGene);
//
//			ResultSet resultSet = preparedStatement.executeQuery();
//
//			while (resultSet.next()) {
//				orthologousGenes.add(resultSet.getString("geneId"));
//			}
//
//			if (log.isDebugEnabled()) {
//
//			}
//
//		} catch (SQLException e) {
//			System.out.println(e.toString());
//		} finally {
//			connection.close();
//		}
//
//		return log.exit(orthologousGenes);
//	}
//
//	/**
//	 * Retrieves all the orthologous genes corresponding to the queried gene in a
//	 * list of species.
//	 * <p>
//	 * This method takes as parameters a {@code String} representing the
//	 * gene ID, and an {@code ArrayList} representing the list of species
//	 * representing the list of species IDs. Then, the orthologous genes for the
//	 * submitted gene ID belonging to these species are retrieved and returned
//	 * as a {@code Collection} of {@code String}.
//	 * 
//	 * @param queryGene
//	 *            A {@code String} representing the gene ID queried, whose
//	 *            orthologous genes in it's closest species are to be retrieved.
//	 * 
//	 * @param speciesIds
//	 *            An {@code ArrayList} representing the list of species
//	 *            whose genes are required
//	 * 
//	 * @return A {@code Collection} of {@code String} containing all
//	 *         the orthologous genes of the query gene in the closest species.
//	 * 
//	 * @throws SQLException
//	 * 
//	 */
//	public ArrayList<String> getAllHierarchicalOrthologuesForSpecies(
//			String queryGene, ArrayList<Long> speciesIds) throws SQLException {
//
//		log.entry();
//
//		ArrayList<String> orthologousGenes = new ArrayList<String>();
//
//		for (long speciesId : speciesIds) {
//
//			String sql = "SELECT t5.* FROM gene AS t1 "
//					+ "INNER JOIN hierarchicalGroup AS t2 ON t1.hierarchicalGroupId = t2.hierarchicalGroupId "
//					+ "INNER JOIN hierarchicalGroup AS t3 "
//					+ "ON t3.hierarchicalGroupLeftBound <= t2.hierarchicalGroupLeftBound AND "
//					+ "t3.hierarchicalGroupRightBound >= t2.hierarchicalGroupRightBound AND "
//					+ "t3.orthologousGroupId = t2.orthologousGroupId "
//					+ "INNER JOIN hierarchicalGroup AS t4 "
//					+ "ON t4.hierarchicalGroupLeftBound >= t3.hierarchicalGroupLeftBound AND "
//					+ "t4.hierarchicalGroupRightBound <= t3.hierarchicalGroupRightBound AND "
//					+ "t4.orthologousGroupId = t3.orthologousGroupId "
//					+ "INNER JOIN gene AS t5 ON t5.hierarchicalGroupId = t4.hierarchicalGroupId "
//					+ "WHERE t1.geneId =? and t3.hierarchicalGroupLeftBound='1' and t5.speciesId=? ;";
//
//			if (log.isDebugEnabled()) {
//				log.debug("QUERY: {}", sql);
//			}
//
//			try {
//				BgeePreparedStatement preparedStatement = connection
//						.prepareStatement(sql);
//
//				preparedStatement.setString(1, queryGene);
//				preparedStatement.setLong(2, speciesId);
//
//				ResultSet resultSet = preparedStatement.executeQuery();
//
//				while (resultSet.next()) {
//					orthologousGenes.add(resultSet.getString("geneId"));
//				}
//
//				if (log.isDebugEnabled()) {
//
//				}
//
//			} catch (SQLException e) {
//				System.out.println(e.toString());
//			} finally {
//				connection.close();
//			}
//		}
//
//		return log.exit(orthologousGenes);
//
//	}

}

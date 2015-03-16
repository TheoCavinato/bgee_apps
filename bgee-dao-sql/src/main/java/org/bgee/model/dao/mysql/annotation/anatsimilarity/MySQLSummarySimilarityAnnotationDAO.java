package org.bgee.model.dao.mysql.annotation.anatsimilarity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.annotation.anatsimilarity.SummarySimilarityAnnotationDAO;
import org.bgee.model.dao.api.exception.DAOException;
import org.bgee.model.dao.mysql.MySQLDAO;
import org.bgee.model.dao.mysql.connector.BgeePreparedStatement;
import org.bgee.model.dao.mysql.connector.MySQLDAOManager;
import org.bgee.model.dao.mysql.connector.MySQLDAOResultSet;

/**
 * A {@code SummarySimilarityAnnotationDAO} for MySQL.
 *
 * @author Valentine Rech de Laval
 * @version Bgee 13
 * @see org.bgee.model.dao.api.annotation.SummarySimilarityAnnotationDAO.SummarySimilarityAnnotationTO
 * @since Bgee 13
 */
public class MySQLSummarySimilarityAnnotationDAO 
                                extends MySQLDAO<SummarySimilarityAnnotationDAO.Attribute> 
                                implements SummarySimilarityAnnotationDAO {

    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(MySQLSummarySimilarityAnnotationDAO.class.getName());
    
    /**
     * Constructor providing the {@code MySQLDAOManager} that this {@code MySQLDAO} 
     * will use to obtain {@code BgeeConnection}s.
     *
     * @param manager                       The {@code MySQLDAOManager} to use.
     * @throws IllegalArgumentException     If {@code manager} is {@code null}.
     */
    public MySQLSummarySimilarityAnnotationDAO(MySQLDAOManager manager)
            throws IllegalArgumentException {
        super(manager);
    }

    @Override
    public SummarySimilarityAnnotationTOResultSet getAllSummarySimilarityAnnotations()
            throws DAOException {
        log.entry();
        // TODO Auto-generated method stub
        return log.exit(null);
    }

    @Override
    public int insertSummarySimilarityAnnotations(
            Collection<SummarySimilarityAnnotationTO> summaryTOs)
            throws DAOException, IllegalArgumentException {
        log.entry(summaryTOs);

        if (summaryTOs == null || summaryTOs.isEmpty()) {
            throw log.throwing(new IllegalArgumentException(
                    "No summary similarity annotation is given, then no annotation is inserted"));
        }

        int annotationInsertedCount = 0;
        int totalAnnotationNumber = summaryTOs.size();
        
        // And we need to build two different queries. 
        String sqlExpression = "INSERT INTO summarySimilarityAnnotation " +
                "(summarySimilarityAnnotationId, taxonId, negated, CIOId) " +
                "values (?, ?, ?, ?)";
        
        // XXX: To not overload MySQL with an error com.mysql.jdbc.PacketTooBigException, 
        // we insert annotations one at a time, but we should insert them several at once,
        // for instance 100 annotations at a time.
        try (BgeePreparedStatement stmt = 
                this.getManager().getConnection().prepareStatement(sqlExpression)) {
            for (SummarySimilarityAnnotationTO summaryTO: summaryTOs) {
                stmt.setInt(1, Integer.parseInt(summaryTO.getId()));
                stmt.setInt(2, Integer.parseInt(summaryTO.getTaxonId()));
                stmt.setBoolean(3, summaryTO.isNegated());
                stmt.setString(4, summaryTO.getCIOId());
                annotationInsertedCount += stmt.executeUpdate();
                stmt.clearParameters();
                if (log.isDebugEnabled() && annotationInsertedCount % 1000 == 0) {
                    log.debug("{}/{} summary similarity annotations inserted", 
                            annotationInsertedCount, totalAnnotationNumber);
                }
            }
        } catch (SQLException e) {
            throw log.throwing(new DAOException(e));
        }

        return log.exit(annotationInsertedCount);        
    }
    
    @Override
    public int insertSimilarityAnnotationsToAnatEntityIds(
            Collection<SimilarityAnnotationToAnatEntityIdTO> simAnnotToAnatEntityTOs) 
                    throws DAOException, IllegalArgumentException {
        log.entry(simAnnotToAnatEntityTOs);
        
        if (simAnnotToAnatEntityTOs == null || simAnnotToAnatEntityTOs.isEmpty()) {
            throw log.throwing(new IllegalArgumentException(
                    "No summary similarity annotation is given, then no annotation is inserted"));
        }

        int rowInsertedCount = 0;
        int totalToNumber = simAnnotToAnatEntityTOs.size();
        
        // And we need to build two different queries. 
        String sqlExpression = "INSERT INTO similarityAnnotationToAnatEntityId " +
                "(summarySimilarityAnnotationId, anatEntityId) values (?, ?)";
        
        // XXX: To not overload MySQL with an error com.mysql.jdbc.PacketTooBigException, 
        // we insert relations one at a time, but we should insert them several at once,
        // for instance 100 relations at a time.
        try (BgeePreparedStatement stmt = 
                this.getManager().getConnection().prepareStatement(sqlExpression)) {
            for (SimilarityAnnotationToAnatEntityIdTO simAnnotToAnatEntityTO: simAnnotToAnatEntityTOs) {
                stmt.setInt(1, 
                        Integer.parseInt(simAnnotToAnatEntityTO.getSummarySimilarityAnnotationId()));
                stmt.setString(2, simAnnotToAnatEntityTO.getAnatEntityId());
                rowInsertedCount += stmt.executeUpdate();
                stmt.clearParameters();
                if (log.isDebugEnabled() && rowInsertedCount % 1000 == 0) {
                    log.debug("{}/{} similarity annotation to anat. entity inserted", 
                            rowInsertedCount, totalToNumber);
                }
            }
        } catch (SQLException e) {
            throw log.throwing(new DAOException(e));
        }

        return log.exit(rowInsertedCount);        
    }

    /**
     * A {@code MySQLDAOResultSet} specific to {@code SummarySimilarityAnnotationTO}.
     * 
     * @author Valentine Rech de Laval
     * @version Bgee 13
     * @since Bgee 13
     */
    public class MySQLSummarySimilarityAnnotationTOResultSet 
                extends MySQLDAOResultSet<SummarySimilarityAnnotationTO> 
                implements SummarySimilarityAnnotationTOResultSet {

        /**
         * Delegates to {@link MySQLDAOResultSet#MySQLDAOResultSet(BgeePreparedStatement)}
         * super constructor.
         * 
         * @param statement The first {@code BgeePreparedStatement} to execute a query on.
         */
        private MySQLSummarySimilarityAnnotationTOResultSet(BgeePreparedStatement statement) {
            super(statement);
        }
        
        /**
         * Delegates to {@link MySQLDAOResultSet#MySQLDAOResultSet(BgeePreparedStatement, 
         * int, int, int)} super constructor.
         * 
         * @param statement             The first {@code BgeePreparedStatement} to execute 
         *                              a query on.
         * @param offsetParamIndex      An {@code int} that is the index of the parameter 
         *                              defining the offset argument of a LIMIT clause, 
         *                              in the SQL query hold by {@code statement}.
         * @param rowCountParamIndex    An {@code int} that is the index of the parameter 
         *                              specifying the maximum number of rows to return 
         *                              in a LIMIT clause, in the SQL query 
         *                              hold by {@code statement}.
         * @param rowCount              An {@code int} that is the maximum number of rows to use 
         *                              in a LIMIT clause, in the SQL query 
         *                              hold by {@code statement}.
         * @param filterDuplicates      A {@code boolean} defining whether equal 
         *                              {@code TransferObject}s returned by different queries should 
         *                              be filtered: when {@code true}, only one of them will be 
         *                              returned. This implies that all {@code TransferObject}s 
         *                              returned will be stored, implying potentially 
         *                              great memory usage.
         */
        private MySQLSummarySimilarityAnnotationTOResultSet(BgeePreparedStatement statement, 
                int offsetParamIndex, int rowCountParamIndex, int rowCount, 
                boolean filterDuplicates) {
            super(statement, offsetParamIndex, rowCountParamIndex, rowCount, filterDuplicates);
        }

        @Override
        protected SummarySimilarityAnnotationTO getNewTO() throws DAOException {
            log.entry();

            String id = null, taxonId = null, cioId = null; 
            Boolean negated = null;

            ResultSet currentResultSet = this.getCurrentResultSet();
            for (Entry<Integer, String> column: this.getColumnLabels().entrySet()) {
                try {
                    if (column.getValue().equals("summarySimilarityAnnotationId")) {
                        id = currentResultSet.getString(column.getKey());
                        
                    } else if (column.getValue().equals("taxonId")) {
                        taxonId = currentResultSet.getString(column.getKey());

                    } else if (column.getValue().equals("negated")) {
                        negated = currentResultSet.getBoolean(column.getKey());

                    } else if (column.getValue().equals("CIOId")) {
                        cioId = currentResultSet.getString(column.getKey());
                    }

                } catch (SQLException e) {
                    throw log.throwing(new DAOException(e));
                }
            }
            return log.exit(new SummarySimilarityAnnotationTO(id, taxonId, negated, cioId));
        }
    }
}

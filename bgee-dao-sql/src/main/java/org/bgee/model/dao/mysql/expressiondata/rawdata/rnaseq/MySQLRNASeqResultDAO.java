package org.bgee.model.dao.mysql.expressiondata.rawdata.rnaseq;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.exception.DAOException;
import org.bgee.model.dao.api.expressiondata.rawdata.CallSourceRawDataDAO.CallSourceRawDataTO;
import org.bgee.model.dao.api.expressiondata.rawdata.rnaseq.RNASeqResultDAO;
import org.bgee.model.dao.mysql.MySQLDAO;
import org.bgee.model.dao.mysql.connector.BgeePreparedStatement;
import org.bgee.model.dao.mysql.connector.MySQLDAOManager;

public class MySQLRNASeqResultDAO extends MySQLDAO<RNASeqResultDAO.Attribute> 
implements RNASeqResultDAO {

    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(MySQLRNASeqResultDAO.class.getName());

    /**
     * Constructor providing the {@code MySQLDAOManager} that this {@code MySQLDAO} 
     * will use to obtain {@code BgeeConnection}s.
     * 
     * @param manager                       The {@code MySQLDAOManager} to use.
     * @throws IllegalArgumentException     If {@code manager} is {@code null}.
     */
    public MySQLRNASeqResultDAO(MySQLDAOManager manager) throws IllegalArgumentException {
        super(manager);
    }

    @Override
    public int updateNoExpressionConflicts(Set<String> noExprIds) 
            throws DAOException, IllegalArgumentException {
        log.entry(noExprIds);       

        int noExprUpdatedCount = 0;

        String sql = "UPDATE rnaSeqResult SET " + 
                this.attributeToString(RNASeqResultDAO.Attribute.NOEXPRESSIONID) + " = ?, " +
                this.attributeToString(RNASeqResultDAO.Attribute.REASONFOREXCLUSION) + " = ? " +
                "WHERE " + this.attributeToString(RNASeqResultDAO.Attribute.NOEXPRESSIONID) + " = ?";
        
        try (BgeePreparedStatement stmt = this.getManager().getConnection().prepareStatement(sql)) {
            for (String noExprId: noExprIds) {
                stmt.setNull(1, Types.INTEGER);
                stmt.setString(2, CallSourceRawDataTO.ExclusionReason.NOEXPRESSIONCONFLICT.
                        getStringRepresentation());
                stmt.setInt(3, Integer.parseInt(noExprId));
                int isUpdated = stmt.executeUpdate();
                if (isUpdated == 0) {
                    throw log.throwing(new IllegalArgumentException("The provided no-expression ID " +
                            noExprId + " was not found in the data source"));
                }
                noExprUpdatedCount += isUpdated;
                stmt.clearParameters();
            }
            return log.exit(noExprUpdatedCount);
        } catch (SQLException e) {
            throw log.throwing(new DAOException(e));
        }
    }

    private String attributeToString(RNASeqResultDAO.Attribute attribute) {
        log.entry(attribute);
        
        String label = null;
        if (attribute.equals(RNASeqResultDAO.Attribute.ID)) {
            label = "rnaSeqLibraryId";
        } else if (attribute.equals(RNASeqResultDAO.Attribute.GENEID)) {
            label = "geneId";
        } else if (attribute.equals(RNASeqResultDAO.Attribute.LOG2RPK)) {
            label = "log2RPK";
        } else if (attribute.equals(RNASeqResultDAO.Attribute.READSCOUNT)) {
            label = "readsCount";
        } else if (attribute.equals(RNASeqResultDAO.Attribute.EXPRESSIONID)) {
            label = "expressionId";
        } else if (attribute.equals(RNASeqResultDAO.Attribute.NOEXPRESSIONID)) {
            label = "noExpressionId";
        } else if (attribute.equals(RNASeqResultDAO.Attribute.DETECTIONFLAG)) {
            label = "detectionFlag";
        } else if (attribute.equals(RNASeqResultDAO.Attribute.RNASEQDATA)) {
            label = "rnaSeqData";
        } else if (attribute.equals(RNASeqResultDAO.Attribute.REASONFOREXCLUSION)) {
            label = "reasonForExclusion";
        } 
        
        return log.exit(label);
    }
}

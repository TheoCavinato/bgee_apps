package org.bgee.model.dao.mysql.expressiondata;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.exception.DAOException;
import org.bgee.model.dao.api.expressiondata.ConditionDAO;
import org.bgee.model.dao.api.expressiondata.DAODataType;
import org.bgee.model.dao.api.expressiondata.ConditionDAO.GlobalConditionToRawConditionTO.ConditionRelationOrigin;
import org.bgee.model.dao.mysql.MySQLDAO;
import org.bgee.model.dao.mysql.connector.BgeePreparedStatement;
import org.bgee.model.dao.mysql.connector.MySQLDAOManager;
import org.bgee.model.dao.mysql.connector.MySQLDAOResultSet;
import org.bgee.model.dao.mysql.exception.UnrecognizedColumnException;

/**
 * An {@code ConditionDAO} for MySQL. 
 * 
 * @author  Valentine Rech de Laval
 * @author  Frederic Bastian
 * @version Bgee 14, Feb. 2017
 * @see org.bgee.model.dao.api.anatdev.ConditionDAO.ConditionTO
 * @since   Bgee 14, Feb. 2017
 */
public class MySQLConditionDAO extends MySQLDAO<ConditionDAO.Attribute> implements ConditionDAO {
    private final static Logger log = LogManager.getLogger(MySQLConditionDAO.class.getName());
    
    /**
     * A {@code String} that is the field name for species IDs in condition tables.
     */
    public final static String SPECIES_ID = "speciesId";
    public final static String RAW_COND_ID_FIELD = "conditionId";
    public final static String GLOBAL_COND_ID_FIELD = "globalConditionId";
    private final static String COND_REL_ORIGIN_FIELD = "conditionRelationOrigin";
    public final static String ANAT_ENTITY_ID_FIELD = "anatEntityId";

    /**
     * @param tableName             A {@code String} that is the name of the global condition table
     *                              in the SQL query.
     * @param condParamCombination  A {@code Collection} of {@code ConditionDAO.Attribute}s defining the
     *                              condition parameters considered for aggregating the expression data
     *                              (see {@link Attribute#isConditionParameter()}).
     * @return                      A {@code String} that is the part of a WHERE clause
     *                              allowing to select the conditions for the requested
     *                              condition parameter combination.
     * @throws IllegalArgumentException If {@code conditionParameters} is {@code null}, empty,
     *                                  or one of the {@code Attribute}s in {@code conditionParameters}
     *                                  is not a condition parameter attributes (see 
     *                                  {@link ConditionDAO.Attribute#isConditionParameter()}). 
     */
    private static String getCondParamCombinationWhereClause(final String tableName,
            Collection<ConditionDAO.Attribute> condParamCombination) throws IllegalArgumentException {
        log.entry(tableName, condParamCombination);
        if (condParamCombination == null || condParamCombination.isEmpty()) {
            throw log.throwing(new IllegalArgumentException(
                    "A condition parameter combination must be provided."));
        }
        final Set<ConditionDAO.Attribute> condParams = EnumSet.copyOf(condParamCombination);
        if (condParams.stream().anyMatch(a -> !a.isConditionParameter())) {
            throw log.throwing(new IllegalArgumentException("The condition parameter combination "
                    + "contains some Attributes that are not condition parameters: " + condParams));
        }
    
        final Map<String, ConditionDAO.Attribute> colToAttr = getColToAttributesMap();
        return log.exit(EnumSet.allOf(ConditionDAO.Attribute.class).stream()
                .filter(a -> a.isConditionParameter())
                .map(a -> tableName + "." + getSelectExprFromAttribute(a, colToAttr) + " IS "
                        + (condParams.contains(a)? "NOT NULL": "NULL"))
                .collect(Collectors.joining(" AND ", "(", ")")));
    }

    /**
     * Get a {@code Map} associating column names to corresponding {@code ConditionDAO.Attribute}.
     * 
     * @param global    A {@code boolean} defining whether the global conditions (if {@code true})
     *                  were targeted, or the raw conditions (if {@code false}).
     * @return          A {@code Map} where keys are {@code String}s that are column names, 
     *                  the associated value being the corresponding {@code ConditionDAO.Attribute}.
     */
    private static Map<String, ConditionDAO.Attribute> getColToAttributesMap() {
        log.entry();
        Map<String, ConditionDAO.Attribute> colToAttributesMap = new HashMap<>();
        colToAttributesMap.put(GLOBAL_COND_ID_FIELD, ConditionDAO.Attribute.ID);
        //only the original condition table containing all parameters has the field "exprMappedConditionId", 
        //allowing to map conditions used in annotations to conditions used in expression tables.
        colToAttributesMap.put("anatEntityId", ConditionDAO.Attribute.ANAT_ENTITY_ID);
        colToAttributesMap.put("stageId", ConditionDAO.Attribute.STAGE_ID);
        colToAttributesMap.put(SPECIES_ID, ConditionDAO.Attribute.SPECIES_ID);
//        colToAttributesMap.put("sex", ConditionDAO.Attribute.SEX);
//        if (!global) {
//            colToAttributesMap.put("sexInferred", ConditionDAO.Attribute.SEX_INFERRED);
//        }
//        colToAttributesMap.put("strain", ConditionDAO.Attribute.STRAIN);
        
        return log.exit(colToAttributesMap);
    }

    public MySQLConditionDAO(MySQLDAOManager manager) throws IllegalArgumentException {
        super(manager);
    }

    //TODO: check whether this method is to moved/has been moved to RawDataConditionDAO?
//    @Override
//    public ConditionTOResultSet getRawConditionsBySpeciesIds(Collection<Integer> speciesIds,
//            Collection<ConditionDAO.Attribute> attributes) throws DAOException {
//        log.entry(speciesIds, attributes);
//        return log.exit(this.getConditionsBySpeciesIds(false, speciesIds, null, attributes));
//    }

    @Override
    public ConditionTOResultSet getGlobalConditionsBySpeciesIds(Collection<Integer> speciesIds,
            Collection<ConditionDAO.Attribute> conditionParameters, 
            Collection<ConditionDAO.Attribute> attributes) throws DAOException, IllegalArgumentException {
        log.entry(speciesIds, conditionParameters, attributes);
        return log.exit(this.getConditionsBySpeciesIds(speciesIds, conditionParameters, attributes));
    }


    private ConditionTOResultSet getConditionsBySpeciesIds(Collection<Integer> speciesIds,
            Collection<ConditionDAO.Attribute> conditionParameters, 
            Collection<ConditionDAO.Attribute> attributes) throws DAOException, IllegalArgumentException {
        log.entry(speciesIds, conditionParameters, attributes);

        final Set<Integer> speIds = Collections.unmodifiableSet(speciesIds == null? new HashSet<>(): new HashSet<>(speciesIds));
        final Set<ConditionDAO.Attribute> attrs = Collections.unmodifiableSet(attributes == null? 
                EnumSet.noneOf(ConditionDAO.Attribute.class): EnumSet.copyOf(attributes));
        final String tableName = "globalCond";

        StringBuilder sb = new StringBuilder();

        //XXX: ConditionRankInfoTOs will be managed directly by the ConditionTOResultSet if we need it.
        //Anyway, these maxRanks attributes are not described in ConditionDAO.Attributes
        //because we abstracted away this database design with enumerated columns
        //(for a discussion about this design, see http://stackoverflow.com/q/42781299/1768736)
        sb.append(generateSelectClause(tableName, getColToAttributesMap(),
                //for global conditions, we are never going to need the DISTINCT clause,
                //since we are always going to define the NULL/NOT NULL status for
                //all condition parameters. For raw conditions the table is small,
                //so we don't bother and always add the DISTINCT clause.
                false, 
                attrs)).append(" FROM ").append(tableName);
        sb.append(" WHERE ")
          .append(getCondParamCombinationWhereClause(tableName, conditionParameters));
        if (!speIds.isEmpty()) {
            sb.append(" AND ")
              .append(tableName).append(".").append(SPECIES_ID).append(" IN (")
              .append(BgeePreparedStatement.generateParameterizedQueryString(speIds.size()))
              .append(")");
        }
        try {
            BgeePreparedStatement stmt = this.getManager().getConnection().prepareStatement(sb.toString());
            if (!speIds.isEmpty()) {
                stmt.setIntegers(1, speIds, true);
            }
            return log.exit(new MySQLConditionTOResultSet(stmt));
        } catch (SQLException e) {
            throw log.throwing(new DAOException(e));
        }
    }

    @Override
    public int getMaxGlobalConditionId() throws DAOException {
        log.entry();

        String condIdField = getSelectExprFromAttribute(ConditionDAO.Attribute.ID,
                getColToAttributesMap());
        String sql = "SELECT MAX(" + condIdField + ") AS " + condIdField + " FROM globalCond";
    
        try (ConditionTOResultSet resultSet = new MySQLConditionTOResultSet(
                this.getManager().getConnection().prepareStatement(sql))) {
            
            if (resultSet.next() && resultSet.getTO().getId() != null) {
                return log.exit(resultSet.getTO().getId());
            } 
            return log.exit(0);
        } catch (SQLException e) {
            throw log.throwing(new DAOException(e));
        }
    }
    
    @Override
    public Map<Integer, ConditionRankInfoTO> getMaxRanks(Collection<Integer> speciesIds,
            Collection<DAODataType> dataTypes, Collection<ConditionDAO.Attribute> conditionParameters) throws DAOException {
        log.entry(speciesIds, dataTypes, conditionParameters);

        Set<Integer> clonedSpeIds = Collections.unmodifiableSet(
                speciesIds == null? new HashSet<>(): new HashSet<>(speciesIds));
        Set<DAODataType> clonedDataTypes = Collections.unmodifiableSet(
                dataTypes == null || dataTypes.isEmpty()? EnumSet.allOf(DAODataType.class): EnumSet.copyOf(dataTypes));

        StringBuilder rankSb = new StringBuilder();
        StringBuilder globalRankSb = new StringBuilder();
        boolean first = true;
        for (DAODataType dataType: clonedDataTypes) {
            if (!first) {
                rankSb.append(", ");
                globalRankSb.append(", ");
            }
            String rankField = null;
            String globalRankField = null;
            switch(dataType) {
            case EST:
                rankField = "estMaxRank";
                globalRankField = "estGlobalMaxRank";
                break;
            case AFFYMETRIX:
                rankField = "affymetrixMaxRank";
                globalRankField = "affymetrixGlobalMaxRank";
                break;
            case IN_SITU:
                rankField = "inSituMaxRank";
                globalRankField = "inSituGlobalMaxRank";
                break;
            case RNA_SEQ:
                rankField = "rnaSeqMaxRank";
                globalRankField = "rnaSeqGlobalMaxRank";
                break;
            default:
                throw log.throwing(new IllegalStateException("Unsupported data type: " + dataType));
            }
            rankSb.append("IF (").append(rankField).append(" IS NULL, 0, ").append(rankField).append(")");
            globalRankSb.append("IF (").append(globalRankField).append(" IS NULL, 0, ").append(globalRankField).append(")");
            first = false;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("SELECT ").append(SPECIES_ID)
          .append(", MAX(");
        if (clonedDataTypes.size() > 1) {
            sb.append("GREATEST(");
        }
        sb.append(rankSb.toString());
        if (clonedDataTypes.size() > 1) {
            sb.append(")");
        }
        sb.append(") AS maxRank")
          //Of note, global max ranks were not generated for bgee v14
          .append(", MAX(");
        if (clonedDataTypes.size() > 1) {
            sb.append("GREATEST(");
        }
        sb.append(globalRankSb.toString());
        if (clonedDataTypes.size() > 1) {
            sb.append(")");
        }
        sb.append(") AS globalMaxRank")
          .append(" FROM globalCond")
          .append(" WHERE ");
        if (!clonedSpeIds.isEmpty()) {
            sb.append(SPECIES_ID).append(" IN (")
            .append(BgeePreparedStatement.generateParameterizedQueryString(clonedSpeIds.size()))
            .append(") AND ");
        }
        sb.append(getCondParamCombinationWhereClause("globalCond", conditionParameters));
        sb.append(" GROUP BY ").append(SPECIES_ID);

        try (BgeePreparedStatement stmt = this.getManager().getConnection().prepareStatement(sb.toString())) {
            if (!clonedSpeIds.isEmpty()) {
                stmt.setIntegers(1, clonedSpeIds, true);
            }
            try (ResultSet rs = stmt.getRealPreparedStatement().executeQuery()) {
                Map<Integer, ConditionRankInfoTO> results = new HashMap<>();
                Integer speciesId = null;
                BigDecimal maxRank = null, globalMaxRank = null;
                while (rs.next()) {
                    speciesId = rs.getInt(SPECIES_ID);
                    maxRank = rs.getBigDecimal("maxRank");
                    globalMaxRank = rs.getBigDecimal("globalMaxRank");
                    results.put(speciesId, new ConditionRankInfoTO(maxRank, globalMaxRank));
                }
                return log.exit(results);
            }
        } catch (SQLException e) {
            throw log.throwing(new DAOException(e));
        }
    }

    @Override
    public int insertGlobalConditions(Collection<ConditionTO> conditionTOs) throws DAOException {
        log.entry(conditionTOs);
        
        if (conditionTOs == null || conditionTOs.isEmpty()) {
            throw log.throwing(new IllegalArgumentException("No condition provided"));
        }

        Set<ConditionDAO.Attribute> attrs = EnumSet.allOf(ConditionDAO.Attribute.class);
        //The order of the parameters is important for generating the query and then setting the parameters.
        final List<ConditionDAO.Attribute> toPopulate = new ArrayList<>(attrs);

        final Map<String, ConditionDAO.Attribute> colToAttrMap = getColToAttributesMap();
        StringBuilder sql = new StringBuilder(); 
        sql.append("INSERT INTO globalCond (")
           .append(toPopulate.stream()
                   .map(a -> getSelectExprFromAttribute(a, colToAttrMap))
                   .collect(Collectors.joining(", ")))
           .append(") VALUES ")
           .append(conditionTOs.stream()
                   .map(c -> "(" + BgeePreparedStatement.generateParameterizedQueryString(
                           toPopulate.size()) + ")")
                   .collect(Collectors.joining(", ")));

        try (BgeePreparedStatement stmt = 
                this.getManager().getConnection().prepareStatement(sql.toString())) {
            int paramIndex = 1;
            for (ConditionTO conditionTO: conditionTOs) {
                for (ConditionDAO.Attribute attr: toPopulate) {
                    switch (attr) {
                    case ID:
                        stmt.setInt(paramIndex, conditionTO.getId());
                        paramIndex++;
                        break;
                    case SPECIES_ID:
                        stmt.setInt(paramIndex, conditionTO.getSpeciesId());
                        paramIndex++;
                        break;
                    case ANAT_ENTITY_ID:
                        stmt.setString(paramIndex, conditionTO.getAnatEntityId());
                        paramIndex++;
                        break;
                    case STAGE_ID:
                        stmt.setString(paramIndex, conditionTO.getStageId());
                        paramIndex++;
                        break;
                    default:
                        log.throwing(new IllegalStateException("Unsupported attribute: " + attr));
                    }
                }
            }
            
            return log.exit(stmt.executeUpdate());
        } catch (SQLException e) {
            throw log.throwing(new DAOException(e));
        }
    }

    @Override
    public int insertGlobalConditionToRawCondition(
            Collection<GlobalConditionToRawConditionTO> globalCondToRawCondTOs)
                    throws DAOException, IllegalArgumentException {
        log.entry(globalCondToRawCondTOs);

        if (globalCondToRawCondTOs == null || globalCondToRawCondTOs.isEmpty()) {
            throw log.throwing(new IllegalArgumentException("No condition relation provided"));
        }

        List<GlobalConditionToRawConditionTO> toList = new ArrayList<>(globalCondToRawCondTOs);
        int maxElementCount = 5000;
        int iterationCount = toList.size() < maxElementCount? 1: (int) Math.ceil((float) toList.size()/(float) maxElementCount);

        int countUpdated = IntStream.range(0, iterationCount)
        .mapToObj(i -> toList.subList(i * maxElementCount,
                (i + 1) * maxElementCount < toList.size()? (i + 1) * maxElementCount: toList.size()))
        //Warning, this stream must be sequential, our SQL accessors cannot be used in parallel
        .mapToInt((partition) -> {
            StringBuilder sql = new StringBuilder(); 
            sql.append("INSERT INTO globalCondToCond (")
            .append(RAW_COND_ID_FIELD).append(", ")
            .append(GLOBAL_COND_ID_FIELD).append(", ")
            .append(COND_REL_ORIGIN_FIELD)
            .append(") VALUES ");
            for (int i = 0; i < partition.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                sql.append("(").append(BgeePreparedStatement.generateParameterizedQueryString(3))
                .append(") ");
            }
            try (BgeePreparedStatement stmt =
                    this.getManager().getConnection().prepareStatement(sql.toString())) {
                int paramIndex = 1;
                for (GlobalConditionToRawConditionTO to: partition) {
                    stmt.setInt(paramIndex, to.getRawConditionId());
                    paramIndex++;
                    stmt.setInt(paramIndex, to.getGlobalConditionId());
                    paramIndex++;
                    stmt.setString(paramIndex, to.getConditionRelationOrigin().getStringRepresentation());
                    paramIndex++;
                }
                return stmt.executeUpdate();
            } catch (SQLException e) {
                throw log.throwing(new DAOException(e));
            }
        })
        .sum();
        
        return log.exit(countUpdated);
    }
    
    /**
     * Implementation of the {@code ConditionTOResultSet}. 
     * 
     * @author Frederic Bastian
     * @version Bgee 14 Feb. 2017
     * @since Bgee 14 Feb. 2017
     */
    class MySQLConditionTOResultSet extends MySQLDAOResultSet<ConditionDAO.ConditionTO>
            implements ConditionTOResultSet {

        /**
         * @param statement The {@code BgeePreparedStatement}
         * @param global    A {@code boolean} defining whether the global conditions (if {@code true})
         *                  were targeted, or the raw conditions (if {@code false}).
         */
        private MySQLConditionTOResultSet(BgeePreparedStatement statement) {
            super(statement);
        }

        @Override
        protected ConditionDAO.ConditionTO getNewTO() throws DAOException {
            log.entry();
            try {
                final ResultSet currentResultSet = this.getCurrentResultSet();
                Integer id = null, speciesId = null;
                String anatEntityId = null, stageId = null;
                Map<String, ConditionDAO.Attribute> colToAttrMap = getColToAttributesMap();

                COL: for (String columnName : this.getColumnLabels().values()) {
                    //don't use MySQLDAO.getAttributeFromColName because we don't cover all columns
                    //with ConditionDAO.Attributes (max rank columns)
                    ConditionDAO.Attribute attr = colToAttrMap.get(columnName);
                    if (attr == null) {
                        continue COL;
                    }
                    switch (attr) {
                        case ID:
                            id = currentResultSet.getInt(columnName);
                            break;
                        case SPECIES_ID:
                            speciesId = currentResultSet.getInt(columnName);
                            break;
                        case ANAT_ENTITY_ID:
                            anatEntityId = currentResultSet.getString(columnName);
                            break;
                        case STAGE_ID:
                            stageId = currentResultSet.getString(columnName);
                            break;
                        default:
                            log.throwing(new UnrecognizedColumnException(columnName));
                    }
                }
                //XXX: retrieval of ConditionRankInfoTOs associated to a ConditionTO not yet implemented,
                //to be added when needed.
                return log.exit(new ConditionTO(id, anatEntityId, stageId, speciesId, null));
            } catch (SQLException e) {
                throw log.throwing(new DAOException(e));
            }
        }
    }

    /**
     * MySQL implementation of {@code GlobalConditionToRawConditionTOResultSet}.
     *
     * @author Frederic Bastian
     * @version Bgee 14 Mar. 2017
     * @since Bgee 14 Mar. 2017
     */
    public class MySQLGlobalConditionToRawConditionTOResultSet
    extends MySQLDAOResultSet<GlobalConditionToRawConditionTO>
    implements GlobalConditionToRawConditionTOResultSet {

        private MySQLGlobalConditionToRawConditionTOResultSet(BgeePreparedStatement statement) {
            super(statement);
        }

        @Override
        protected GlobalConditionToRawConditionTO getNewTO() throws DAOException {
            log.entry();
            try {
                final ResultSet currentResultSet = this.getCurrentResultSet();
                Integer rawConditionId = null, globalConditionId = null;
                ConditionRelationOrigin relOrigin = null;

                for (String columnName: this.getColumnLabels().values()) {

                    if (columnName.equals(RAW_COND_ID_FIELD)) {
                        rawConditionId = currentResultSet.getInt(columnName);
                    } else if (columnName.equals(GLOBAL_COND_ID_FIELD)) {
                        globalConditionId = currentResultSet.getInt(columnName);
                    } else if (columnName.equals(COND_REL_ORIGIN_FIELD)) {
                        relOrigin = ConditionRelationOrigin.convertToCondRelOrigin(
                                currentResultSet.getString(columnName));
                    }  else {
                        throw log.throwing(new UnrecognizedColumnException(columnName));
                    }
                }

                return log.exit(new GlobalConditionToRawConditionTO(
                        rawConditionId, globalConditionId, relOrigin));
            } catch (SQLException e) {
                throw log.throwing(new DAOException(e));
            }
        }
    }
}

package org.bgee.model.dao.api.expressiondata;

import java.util.Collection;
import java.util.LinkedHashMap;

import org.bgee.model.dao.api.DAO;
import org.bgee.model.dao.api.DAOResultSet;
import org.bgee.model.dao.api.TransferObject;
import org.bgee.model.dao.api.exception.DAOException;

/**
 * DAO defining queries using or retrieving {@link ExperimentExpressionTO}s. 
 * 
 * @author  Valentine Rech de Laval
 * @version Bgee 14, Feb. 2017
 * @since   Bgee 14, Dec. 2016
 */
public interface ExperimentExpressionDAO extends DAO<ExperimentExpressionDAO.Attribute> {
    
    /**
     * The attributes available for {@code ExperimentExpressionTO}
     * <ul>
     *   <li>@{code EXPRESSION_ID} corresponds to {@link ExperimentExpressionTO#getExpressionId()}
     *   <li>@{code EXPERIMENT_ID} corresponds to {@link ExperimentExpressionTO#getExperimentId()}
     *   <li>@{code PRESENT_HIGH_COUNT} corresponds to {@link ExperimentExpressionTO#getPresentHighCount()}}
     *   <li>@{code PRESENT_LOW_COUNT} corresponds to {@link ExperimentExpressionTO#getPresentLowCount()}
     *   <li>@{code ABSENT_HIGH_COUNT} corresponds to {@link ExperimentExpressionTO#getAbsentHighCount()}
     *   <li>@{code ABSENT_LOW_COUNT} corresponds to {@link ExperimentExpressionTO#getAbsentLowCount()}
     *   <li>@{code EXPERIMENT_COUNT} corresponds to {@link ExperimentExpressionTO#getExperimentCount()}
     * </ul>
     */
    enum Attribute implements DAO.Attribute {
        EXPRESSION_ID, EXPERIMENT_ID, PRESENT_HIGH_COUNT, PRESENT_LOW_COUNT,
        ABSENT_HIGH_COUNT, ABSENT_LOW_COUNT, EXPERIMENT_COUNT;
    }
    
    /**
     * Retrieve affymetrix experiment expressions from the data source.
     * <p>
     * The expressions are retrieved and returned as an {@code ExperimentExpressionTOResultSet}.
     * It is the responsibility of the caller to close this {@code DAOResultSet} once results are retrieved.
     * 
     * @param speciesId             An {@code int} that is the ID of the species to retrieve calls for.
     * @param condParameters        A {@code Collection} of {@code ConditionDAO.Attribute}s 
     *                              defining {@code ExperimentExpressionTO}s to return.
     * @return                      An {@code ExperimentExpressionTOResultSet} allowing to obtain 
     *                              the requested {@code ExperimentExpressionTO}s.
     * @throws DAOException             If an error occurred while accessing the data source. 
     */
    public ExperimentExpressionTOResultSet getAffymetrixExpExprsOrderedByGeneIdAndExprId(int speciesId,
        Collection<ConditionDAO.Attribute> condParameters) throws DAOException;

    /**
     * Retrieve RNA-Seq experiment expressions from the data source.
     * <p>
     * The expressions are retrieved and returned as an {@code ExperimentExpressionTOResultSet}.
     * It is the responsibility of the caller to close this {@code DAOResultSet} once results are retrieved.
     * 
     * @param speciesId             An {@code int} that is the ID of the species to retrieve calls for.
     * @param condParameters        A {@code Collection} of {@code ConditionDAO.Attribute}s 
     *                              defining {@code ExperimentExpressionTO}s to return.
     * @return                      An {@code ExperimentExpressionTOResultSet} allowing to obtain 
     *                              the requested {@code ExperimentExpressionTO}s.
     * @throws DAOException             If an error occurred while accessing the data source. 
     */
    public ExperimentExpressionTOResultSet getRNASeqExpExprsOrderedByGeneIdAndExprId(int speciesId,
        Collection<ConditionDAO.Attribute> condParameters) throws DAOException;

    /**
     * Retrieve EST experiment expressions from the data source.
     * <p>
     * The expressions are retrieved and returned as an {@code ExperimentExpressionTOResultSet}.
     * It is the responsibility of the caller to close this {@code DAOResultSet} once results are retrieved.
     * 
     * @param speciesId             An {@code int} that is the ID of the species to retrieve calls for.
     * @param condParameters        A {@code Collection} of {@code ConditionDAO.Attribute}s 
     *                              defining {@code ExperimentExpressionTO}s to return.
     * @param speciesId             A {@code String} that is the ID of the species to retrieve calls for.
     * @return                      An {@code ExperimentExpressionTOResultSet} allowing to obtain 
     *                              the requested {@code ExperimentExpressionTO}s.
     * @throws DAOException             If an error occurred while accessing the data source. 
     */
    public ExperimentExpressionTOResultSet getESTExpExprsOrderedByGeneIdAndExprId(int speciesId,
        Collection<ConditionDAO.Attribute> condParameters) throws DAOException;

    /**
     * Retrieve <em>in situ</em> experiment expressions from the data source.
     * <p>
     * The expressions are retrieved and returned as an {@code ExperimentExpressionTOResultSet}.
     * It is the responsibility of the caller to close this {@code DAOResultSet} once results are retrieved.
     * 
     * @param speciesId             An {@code int} that is the ID of the species to retrieve calls for.
     * @param condParameters        A {@code Collection} of {@code ConditionDAO.Attribute}s 
     *                              defining {@code ExperimentExpressionTO}s to return.
     * @param speciesId             A {@code String} that is the ID of the species to retrieve calls for.
     * @return                      An {@code ExperimentExpressionTOResultSet} allowing to obtain 
     *                              the requested {@code ExperimentExpressionTO}s.
     * @throws DAOException             If an error occurred while accessing the data source. 
     */
    public ExperimentExpressionTOResultSet getInSituExpExprsOrderedByGeneIdAndExprId(int speciesId,
        Collection<ConditionDAO.Attribute> condParameters) throws DAOException;

    /**
     * {@code DAOResultSet} specifics to {@code ExperimentExpressionTO}s
     * 
     * @author  Valentine Rech de Laval
     * @version Bgee 13, Dec. 2016
     * @since   Bgee 13, Dec. 2016
     */
    public interface ExperimentExpressionTOResultSet extends DAOResultSet<ExperimentExpressionTO> {
    }
    
    /**
     * {@code TransferObject} representing an experiment expression in the Bgee database.
     * 
     * @author  Valentine Rech de Laval
     * @version Bgee 14, Jan. 2017
     * @since   Bgee 13, Dec. 2016
     */
    public class ExperimentExpressionTO extends TransferObject {

        private static final long serialVersionUID = 3464643420374159955L;

        /**
         * An {@code Integer} that is the expression ID of this experiment expression.
         */
        private final Integer expressionId;

        /**
         * A {@code String} that is the experiment ID of this experiment expression.
         */
        private final String experimentId;
        
        /**
         * An {@code Integer} that is the count of experiments that produced
         * this experiment expression as present high.
         */
        private final Integer presentHighCount;
        
        /**
         * An {@code Integer} that is the count of experiments that produced
         * this experiment expression as present low.
         */
        private final Integer presentLowCount;
        
        /**
         * An {@code Integer} that is the count of experiments that produced
         * this experiment expression as absent high.
         */
        private final Integer absentHighCount;
        
        /**
         * An {@code Integer} that is the count of experiments that produced
         * this experiment expression as absent low.
         */
        private final Integer absentLowCount;

        /**
         * An {@code Integer} that is the number of experiments with that exact combination of counts 
         * of present high/present low/absent high/absent low that produced this experiment expression.
         */
        private final Integer experimentCount;
        
        /**
         * Constructor providing the experiment ID and counts of experiments that produced
         * this experiment expression.
         * <p>
         * All of these parameters are optional, so they can be {@code null} when not used.
         * Other attributes are set to {@code null}.
         * 
         * @param expressionId      An {@code Integer} that is the expression ID.
         * @param experimentId      A {@code String} that is the experiment ID.
         * @param presentHighCount  An {@code Integer} that is the count of experiments that
         *                          produced this experiment expression as present high.
         * @param presentLowCount   An {@code Integer} that is the count of experiments that
         *                          produced this experiment expression as present low.
         * @param absentHighCount   An {@code Integer} that is the count of experiments 
         *                          produced this experiment expression as absent high.
         * @param absentLowCount    An {@code Integer} that is the count of experiments 
         *                          produced this experiment expression as absent low.
         * @param experimentCount   An {@code Integer} that is the number of experiments with that 
         *                          exact combination of counts of present high/present low/absent 
         *                          high/absent low that produced this experiment expression.
         */
        public ExperimentExpressionTO(Integer expressionId, String experimentId, Integer presentHighCount,
            Integer presentLowCount, Integer absentHighCount, Integer absentLowCount, Integer experimentCount) {
            this.expressionId = expressionId;
            this.experimentId = experimentId;
            this.presentHighCount = presentHighCount;
            this.presentLowCount = presentLowCount;
            this.absentHighCount = absentHighCount;
            this.absentLowCount = absentLowCount;
            this.experimentCount = experimentCount;
        }

        /**
         * @return  The {@code Integer} that is the expression ID of this experiment expression.
         */
        public Integer getExpressionId() {
            return expressionId;
        }

        /**
         * @return  The {@code String} that is the experiment ID of this experiment expression.
         */
        public String getExperimentId() {
            return experimentId;
        }

        /**
         * @return  The {@code Integer} that is the count of experiments that produced
         *          this experiment expression as present high
         */
        public Integer getPresentHighCount() {
            return presentHighCount;
        }

        /**
         * @return  The {@code Integer} that is the count of experiments that produced
         *          this experiment expression as present low
         */
        public Integer getPresentLowCount() {
            return presentLowCount;
        }

        /**
         * @return  The {@code Integer} that is the count of experiments that produced
         *          this experiment expression as absent high
         */
        public Integer getAbsentHighCount() {
            return absentHighCount;
        }

        /**
         * @return  The {@code Integer} that is the count of experiments that produced
         *          this experiment expression as absent low.
         */
        public Integer getAbsentLowCount() {
            return absentLowCount;
        }

        /**
         * @return  The {@code Integer} that is the number of experiments with that
         *          exact combination of counts of present high/present low/absent high/absent low 
         *          that produced this experiment expression.
         */
        public Integer getExperimentCount() {
            return experimentCount;
        }

        @Override
        public String toString() {
            return " - Expression ID: " + getExpressionId() 
                + " - Experiment ID: " + getExperimentId()
                + " - Present high count: " + getPresentHighCount()
                + " - Present low count: " + getPresentLowCount() 
                + " - Absent high count: " + getAbsentHighCount()
                + " - Absent low count: " + getAbsentLowCount()
                + " - Experiment count: " + getExperimentCount();
        }
    }
}

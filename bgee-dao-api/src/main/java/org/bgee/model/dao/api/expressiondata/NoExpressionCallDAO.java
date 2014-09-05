package org.bgee.model.dao.api.expressiondata;

import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.DAO;
import org.bgee.model.dao.api.DAOResultSet;
import org.bgee.model.dao.api.TransferObject;
import org.bgee.model.dao.api.exception.DAOException;
import org.bgee.model.dao.api.expressiondata.CallDAO.CallTO;

/**
 * DAO defining queries using or retrieving {@link NoExpressionCallDAO}s. 
 * 
 * @author Valentine Rech de Laval
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public interface NoExpressionCallDAO extends DAO<NoExpressionCallDAO.Attribute> {

    /**
     * {@code Enum} used to define the attributes to populate in the {@code NoExpressionCallTO}s 
     * obtained from this {@code NoExpressionCallDAO}.
     * <ul>
     * <li>{@code ID: corresponds to {@link CallTO#getId()}.
     * <li>{@code GENEID: corresponds to {@link CallTO#getGeneId()}.
     * <li>{@code STAGEID: corresponds to {@link CallTO#getStageId()}.
     * <li>{@code ANATENTITYID: corresponds to {@link CallTO#getAnatEntityId()}.
     * <li>{@code AFFYMETRIXDATA: corresponds to {@link CallTO#getAffymetrixData()}.
     * <li>{@code RELAXEDINSITUDATA: corresponds to {@link CallTO#getRelaxedInSituData()}.
     * <li>{@code INSITUDATA: corresponds to {@link CallTO#getInSituData()}.
     * <li>{@code RNASEQDATA;: corresponds to {@link CallTO#getRNASeqData()}.
     * <li>{@code INCLUDEPARENTSTRUCTURES}: corresponds to 
     * {@link NoExpressionCallTO#isIncludeParentStructures()}.
     * <li>{@code ORIGINOFLINE}: corresponds to {@link NoExpressionCallTO#getOriginOfLine()}.
     * </ul>
     * @see org.bgee.model.dao.api.DAO#setAttributes(Collection)
     * @see org.bgee.model.dao.api.DAO#setAttributes(Enum[])
     * @see org.bgee.model.dao.api.DAO#clearAttributes()
     */
    public enum Attribute implements DAO.Attribute {
        ID, GENEID, DEVSTAGEID, ANATENTITYID, AFFYMETRIXDATA, 
        RELAXEDINSITUDATA, INSITUDATA, RNASEQDATA,
        INCLUDEPARENTSTRUCTURES, ORIGINOFLINE;
    }

    /**
     * Retrieve all no-expression calls from data source according {@code NoExpressionCallParams}.
     * <p>
     * The no-expression calls are retrieved and returned as a {@code NoExpressionCallTOResultSet}. 
     * It is the responsibility of the caller to close this {@code DAOResultSet} once 
     * results are retrieved.
     * 
     * @param params  An{@code NoExpressionCallParams} that provide the parameters specific 
     *                to no-expression calls.
     * @return        A {@code NoExpressionCallTOResultSet} containing all no-expression calls 
     *                from data source.
     * @throws DAOException If an error occurred when accessing the data source. 
     */
    public NoExpressionCallTOResultSet getAllNoExpressionCalls(NoExpressionCallParams params)
            throws DAOException;
    
    /**
     * Inserts the provided no-expression calls into the Bgee database, 
     * represented as a {@code Collection} of {@code NoExpressionCallTO}s. 
     * 
     * @param noExpressionCalls A {@code Collection} of {@code NoExpressionCallTO}s 
     *                          to be inserted into the database.
     * @return                  An {@code int} that is the number of inserted no-expression calls.
     * @throws DAOException If an error occurred while trying to insert no-expression 
     *                      calls. .
     */
    public int insertNoExpressionCalls(Collection<NoExpressionCallTO> noExpressionCalls)
            throws DAOException;

    /**
     * Inserts the provided correspondence between no-expression and global no-expression calls into 
     * the Bgee database, represented as a {@code Collection} of 
     * {@code GlobalNoExpressionToNoExpressionTO}s. 
     * 
     * @param globalNoExprToNoExprTOs   A {@code Collection} of 
     *                                  {@code GlobalNoExpressionToNoExpressionTO}s to be 
     *                                  inserted into the database.
     * @return                          An {@code int} that is the number of inserted 
     *                                  TOs.
     * @throws DAOException If an error occurred while trying to insert data. 
     */
    public int insertGlobalNoExprToNoExpr(Collection<GlobalNoExpressionToNoExpressionTO> 
                                              globalNoExprToNoExprTOs) throws DAOException;

    /**
     * {@code DAOResultSet} specifics to {@code NoExpressionCallTO}s
     * 
     * @author Valentine Rech de Laval
     * @version Bgee 13
     * @since Bgee 13
     */
    public interface NoExpressionCallTOResultSet extends DAOResultSet<NoExpressionCallTO> {
        
    }

    /**
     * A {@code CallTO} specific to no-expression calls (explicit report of absence 
     * of expression). Their specificity is that they can be produced using data propagation 
     * from parent anatomical entities by <em>is_a</em> or <em>part_of</em> relations. 
     * See {@link #isIncludeParentStructures()} for more details.
     * <p>
     * Of note, there is no data propagation from developmental stages for no-expression 
     * calls.
     * 
     * @author Frederic Bastian
     * @author Valentine Rech de Laval
     * @version Bgee 13
     * @since Bgee 13
     */
    public final class NoExpressionCallTO extends CallTO {
        // TODO modify the class to be immutable.
        private static final long serialVersionUID = 5793434647776540L;
        
        /**
         * {@code Logger} of the class. 
         */
        private final static Logger log = LogManager.getLogger(NoExpressionCallTO.class.getName());

        /**
         * A {@code boolean} defining whether this no-expression call was generated 
         * using the data from the anatomical entity with the ID {@link CallTO#getAnatEntityId()} 
         * alone, or by also considering all its parents by <em>is_a</em> or <em>part_of</em> 
         * relations, even indirect. If {@code true}, all its parents were considered. 
         * So for instance, if B is_a A, and absence of expression has been reported in A, 
         * then B could benefit from this information. In other words, when a gene 
         * is not expressed in a structure, it is expressed nowhere in that structure.
         */
        private boolean includeParentStructures;

        /**
         * An {@code Enum} used to define the origin of a no-expression call.
         * <ul>
         * <li>{@code SELF}: the no-expression call was generated from data coming from 
         * its related anatomical entity itself.
         * <li>{@code PARENT}: the no-expression call was generated by data coming from 
         * one of the parents of its related anatomical entity, through  
         * <em>is_a</em> or <em>part_of</em> relations, even indirect.
         * <li>{@code BOTH}: the expression call was generated by data coming from both 
         * its related anatomical entity, and from one of its parents 
         * by <em>is_a</em> or <em>part_of</em> relations, even indirect.
         * </ul>
         */
        public enum OriginOfLine {
            SELF("self"), PARENT("parent"), BOTH("both");
            
            /**
             * Convert the {@code String} representation of a data state (for instance, 
             * retrieved from a database) into a {@code OriginOfLine}. This method 
             * compares {@code representation} to the value returned by 
             * {@link #getStringRepresentation()}, as well as to the value 
             * returned by {@link Enum#name()}, for each {@code OriginOfLine}, 
             * .
             * 
             * @param representation    A {@code String} representing a data state.
             * @return  A {@code OriginOfLine} corresponding to {@code representation}.
             * @throw IllegalArgumentException  If {@code representation} does not correspond 
             *                                  to any {@code OriginOfLine}.
             */
            public static final OriginOfLine convertToOriginOfLine(String representation) {
                log.entry(representation);
                
                for (OriginOfLine origin: OriginOfLine.values()) {
                    if (origin.getStringRepresentation().equals(representation) || 
                            origin.name().equals(representation)) {
                        return log.exit(origin);
                    }
                }
                throw log.throwing(new IllegalArgumentException("\"" + representation + 
                        "\" does not correspond to any OriginOfLine"));
            }
            
            /**
             * See {@link #getStringRepresentation()}
             */
            private final String stringRepresentation;
            
            /**
             * Constructor providing the {@code String} representation 
             * of this {@code OriginOfLine}.
             * 
             * @param stringRepresentation  A {@code String} corresponding to 
             *                              this {@code OriginOfLine}.
             */
            private OriginOfLine(String stringRepresentation) {
                this.stringRepresentation = stringRepresentation;
            }
            
            /**
             * @return  A {@code String} that is the representation 
             *          for this {@code OriginOfLine}, for instance to be used in a database.
             */
            public String getStringRepresentation() {
                return this.stringRepresentation;
            }
            
            @Override
            public String toString() {
                return this.getStringRepresentation();
            }
        }
        
        /**
         * An {@code OriginOfLine} used to define the origin of the global expression call: 
         * either {@code SELF}, {@code PARENT} or {@code BOTH}.
         * 
         * @see OriginOfLine
         */
        private OriginOfLine originOfLine; 

        /**
         * Default constructor.
         */
        NoExpressionCallTO() {
            this(null, null, null, null, DataState.NODATA, DataState.NODATA, 
                    DataState.NODATA, DataState.NODATA, false, OriginOfLine.SELF);
        }

        /**
         * Constructor providing the gene ID, the anatomical entity ID, the developmental stage ID,  
         * the contribution of Affymetrix, <em>in situ</em> and, RNA-Seq data to the generation of 
         * this call, whether this no-expression call was generated using data from the anatomical 
         * entity with the ID alone, or by also considering all parents by is_a or part_of 
         * relations, even indirect, and, the origin of line
         * 
         * @param id                   A {@code String} that is the ID of this call.
         * @param geneId               A {@code String} that is the ID of the gene associated to 
         *                             this call.
         * @param anatEntityId         A {@code String} that is the ID of the anatomical entity
         *                             associated to this call. 
         * @param stageId              A {@code String} that is the ID of the developmental stage 
         *                             associated to this call. 
         * @param affymetrixData       A {@code DataSate} that is the contribution of Affymetrix  
         *                             data to the generation of this call.
         * @param inSituData           A {@code DataSate} that is the contribution of 
         *                             <em>in situ</em> data to the generation of this call.
         * @param relaxedInSituData    A {@code DataSate} that is the contribution of relaxed 
         *                             <em>in situ</em> data to the generation of this call.
         * @param rnaSeqData           A {@code DataSate} that is the contribution of RNA-Seq data
         *                             to the generation of this call.
         * @param includeParentStructures
         *                             A {@code boolean} defining whether this no-expression call 
         *                             was generated using data from the anatomical entity with the 
         *                             ID alone, or by also considering all parents by is_a or 
         *                             part_of relations, even indirect.
         * @param originOfLine         An {@code OriginOfLine} defining the origin of line.
         */
        public NoExpressionCallTO(String id, String geneId, String anatEntityId, String stageId,
                DataState affymetrixData, DataState inSituData,  
                DataState relaxedInSituData, DataState rnaSeqData, 
                boolean includeParentStructures, OriginOfLine originOfLine) {
            super(id, geneId, anatEntityId, stageId, affymetrixData, DataState.NODATA, 
                    inSituData, relaxedInSituData, rnaSeqData);
            this.includeParentStructures = includeParentStructures;
            this.originOfLine = originOfLine;
        }

        /**
         * Returns the {@code boolean} defining whether this no-expression call was generated 
         * using the data from the anatomical entity with the ID {@link CallTO#getAnatEntityId()} 
         * alone, or by also considering all its parents by <em>is_a</em> or <em>part_of</em> 
         * relations, even indirect. If {@code true}, all its parents were considered. 
         * So for instance, if B is_a A, and absence of expression has been reported in A, 
         * then B could benefit from this information. In other words, when a gene 
         * is not expressed in a structure, it is expressed nowhere in that structure.
         * 
         * @return  If {@code true}, all parents of the anatomical entity were considered.
         */
        public boolean isIncludeParentStructures() {
            return includeParentStructures;
        }

        /**
         * Sets the {@code boolean} defining whether this no-expression call was generated 
         * using the data from the anatomical entity with the ID {@link CallTO#getAnatEntityId()} 
         * alone, or by also considering all its parents by <em>is_a</em> or <em>part_of</em> 
         * relations, even indirect. If {@code true}, all its parents were considered. 
         * So for instance, if B is_a A, and absence of expression has been reported in A, 
         * then B could benefit from this information. In other words, when a gene 
         * is not expressed in a structure, it is expressed nowhere in that structure.
         * 
         * @param includeParentStructures   A {@code boolean} defining whether parents 
         *                                  of the anatomical entity were considered.
         */
        void setIncludeParentStructures(boolean includeParentStructures) {
            this.includeParentStructures = includeParentStructures;
        }

        /**
         * @return  the {@code OriginOfLine} representing the origin of the 
         *          global expression call.
         */
        public OriginOfLine getOriginOfLine() {
            return originOfLine;
        }
        
        /**
         * @param originOfLine  An {@code OriginOfLine} representing the origin of the 
         *                      global expression call.
         */
        void setOriginOfLine(OriginOfLine originOfLine) {
            this.originOfLine = originOfLine;
        }

        
        //**************************************
        // Object methods overridden
        //**************************************

        @Override
        public String toString() {
            return super.toString() +  
                    " - Include Parent Structures: " + this.isIncludeParentStructures() +
                    " - Origin Of Line: " + this.getOriginOfLine();
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            if (this.useOtherAttributesForHashCodeEquals()) {
                result = prime * result + (includeParentStructures ? 1231 : 1237);
                result = prime * result
                        + ((originOfLine == null) ? 0 : originOfLine.hashCode());
            }
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NoExpressionCallTO)) {
                return false;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (this.useOtherAttributesForHashCodeEquals()) {
                NoExpressionCallTO other = (NoExpressionCallTO) obj;
                if (includeParentStructures != other.includeParentStructures) {
                    return false;
                }
                if (originOfLine != other.originOfLine) {
                    return false;
                }
            }
            return true;
        }
    }
    
    /**
     * {@code DAOResultSet} specifics to {@code GlobalNoExpressionToNoExpressionTO}s
     * 
     * @author Valentine Rech de Laval
     * @version Bgee 13
     * @since Bgee 13
     */
    public interface GlobalNoExpressionToNoExpressionTOResultSet 
                        extends DAOResultSet<GlobalNoExpressionToNoExpressionTO> {
    }

    /**
     * A {@code TransferObject} representing relation between no-expression and 
     * globalNoExpression table in the Bgee data source.
     * 
     * @author Valentine Rech de Laval
     * @version Bgee 13
     * @since Bgee 13
     */
    public final class GlobalNoExpressionToNoExpressionTO implements TransferObject {
        // TODO modify the class to be immutable.
        private static final long serialVersionUID = -5283534395161770005L;

        /**
         * A {@code String} representing the ID of the no-expression call.
         */
        private final String noExpressionId;

        /**
         * A {@code String} representing the ID of the global no-expression call.
         */
        private final String globalNoExpressionId;

        /**
         * Constructor providing the no-expression call ID and the global no-expression call ID.  
         **/
        public GlobalNoExpressionToNoExpressionTO(String noExpressionId, 
                String globalNoExpressionId) {
            this.noExpressionId = noExpressionId;
            this.globalNoExpressionId = globalNoExpressionId;
        }

        /**
         * @return  the {@code String} representing the ID of the no-expression call.
         */
        public String getNoExpressionId() {
            return noExpressionId;
        }

        /**
         * @return  the {@code String} representing the ID of the global no-expression call.
         */
        public String getGlobalNoExpressionId() {
            return globalNoExpressionId;
        }
        
        @Override
        public String toString() {
            return "noExpressionId: " + noExpressionId + 
                    "- globalNoExpressionId: " + globalNoExpressionId;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * 
                    result + ((globalNoExpressionId == null) ? 0 : globalNoExpressionId.hashCode());
            result = prime * result + ((noExpressionId == null) ? 0 : noExpressionId.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            GlobalNoExpressionToNoExpressionTO other = (GlobalNoExpressionToNoExpressionTO) obj;
            if (globalNoExpressionId == null) {
                if (other.globalNoExpressionId != null) {
                    return false;
                }
            } else if (!globalNoExpressionId.equals(other.globalNoExpressionId)) {
                return false;
            }
            if (noExpressionId == null) {
                if (other.noExpressionId != null) {
                    return false;
                }
            } else if (!noExpressionId.equals(other.noExpressionId)) {
                return false;
            }
            return true;
        }
    }
}

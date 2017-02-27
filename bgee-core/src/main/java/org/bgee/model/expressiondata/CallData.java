package org.bgee.model.expressiondata;

import java.math.BigDecimal;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.expressiondata.baseelements.CallType;
import org.bgee.model.expressiondata.baseelements.CallType.DiffExpression;
import org.bgee.model.expressiondata.baseelements.CallType.Expression;
import org.bgee.model.expressiondata.baseelements.DataPropagation;
import org.bgee.model.expressiondata.baseelements.DataQuality;
import org.bgee.model.expressiondata.baseelements.DataType;
import org.bgee.model.expressiondata.baseelements.DiffExpressionFactor;

/**
 * A {@code CallData} represents the expression state of a {@link Gene}, in a {@link Condition}. 
 * This class only manages the expression state part, not the spatio-temporal location, 
 * or gene definition part. It represents the expression state of a baseline present/absent call, 
 * or a differential expression call; a call represents an overall summary 
 * of the expression data contained in Bgee (for instance, the expression state of a gene 
 * summarized over all Affymetrix chips studied in a given organ at a given stage).
 * <p>
 * For a class also managing the gene and condition definitions, and managing 
 * expression data from different data types for a given call, see the class {@link Call}. 
 * 
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 14, Feb. 2017
 * @since   Bgee 13, Sept. 2015
 */
//XXX: examples of attributes that could be managed by this class: 
//* count of experiments supporting and contradicting the CallType.
//this is meaningful both from a "query filter" perspective and a "data retrieval" perspective, 
//and this could be common to baseline present/absent and diff. expression analyses 
//(even if we currently store the information only for diff. expression analyses). 
public abstract class CallData<T extends Enum<T> & CallType> {
    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = LogManager.getLogger(CallData.class.getName());

    //**********************************************
    //   INNER CLASSES
    //**********************************************

    //XXX: attributes to be added in the future: min p-value, min/max fold change, ...
    //XXX: where to manage the DiffExpressionFactor? Here, or only in a "Call" class? 
    //But then, we could not use this CallData in query filters to specify the factor to use.
    public static class DiffExpressionCallData extends CallData<DiffExpression> {
        //XXX: I'm not very happy about this field, as it is redundant as compared to the field in 
        //DiffExpressionCall, and as it is not something specific to a data type, 
        //which is what this class is supposed to be about.
        //This field was created only to be able to parameterize queries to a CallService, 
        //though a CallFilter, to request diff. expression calls produced from analyzes 
        //over anatomy, and/or over development.
        //But maybe we can argue that it is always useful to be able to know from which type 
        //of analysis a DiffExpressionCallData comes from...
        private final DiffExpressionFactor diffExpressionFactor;

        private final DataQuality dataQuality;

        private final DataPropagation dataPropagation;

        public DiffExpressionCallData(DiffExpressionFactor factor, DiffExpression callType) {
            this(factor, callType, null);
        }
        public DiffExpressionCallData(DiffExpressionFactor factor, DiffExpression callType, 
                DataType dataType) {
            this(factor, callType, DataQuality.LOW, dataType);
        }
        public DiffExpressionCallData(DiffExpressionFactor factor, DiffExpression callType, 
                DataQuality dataQual, DataType dataType) {
            this(factor, callType, dataQual, dataType, new DataPropagation());
        }
        public DiffExpressionCallData(DiffExpressionFactor factor, DiffExpression callType, 
                DataQuality dataQual, DataType dataType, DataPropagation dataPropagation) {
            super(dataType, callType);
            
            log.entry(factor, callType, dataQual, dataType, dataPropagation);
            
            if (callType == null || dataQual == null || DataQuality.NODATA.equals(dataQual) || 
                dataPropagation == null) {
                        throw log.throwing(new IllegalArgumentException("A DiffExpressionFactor, "
                            + "a CallType, a DataQuality, and a DataPropagation must be defined "
                            + "to instantiate a CallData."));
            }
            callType.checkDataPropagation(dataPropagation);
            if (dataType != null) {
                callType.checkDataType(dataType);
            }

            this.dataQuality = dataQual;
            this.dataPropagation = dataPropagation;
            this.diffExpressionFactor = factor;            
            log.exit();
        }
        
        public DiffExpressionFactor getDiffExpressionFactor() {
            return diffExpressionFactor;
        }
        
        public DataQuality getDataQuality() {
            return dataQuality;
        }
        //XXX: to remove?
        public DataPropagation getDataPropagation() {
            return dataPropagation;
        }

        @Override
        // FIXME do implementation
        public boolean isObservedData() {
            return log.exit(false);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((diffExpressionFactor == null) ? 0 : diffExpressionFactor.hashCode());
            result = prime * result + ((dataPropagation == null) ? 0 : dataPropagation.hashCode());
            result = prime * result + ((dataQuality == null) ? 0 : dataQuality.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            DiffExpressionCallData other = (DiffExpressionCallData) obj;
            if (diffExpressionFactor != other.diffExpressionFactor) {
                return false;
            }
            if (dataPropagation == null) {
                if (other.dataPropagation != null) {
                    return false;
                }
            } else if (!dataPropagation.equals(other.dataPropagation)) {
                return false;
            }
            if (dataQuality != other.dataQuality) {
                return false;
            }
            return true;
        }
        
        @Override
        public String toString() {
            return super.toString() + " - Data quality: " + dataQuality +
                " - Data propagation: " + dataPropagation + " - Diff. expression factor: " + diffExpressionFactor;
        }
    }
    
    public static class ExpressionCallData extends CallData<Expression> {

        private final int presentHighSelfCount;
        
        private final int presentLowSelfCount;
        
        private final int absentHighSelfCount;

        private final int absentLowSelfCount;

        private final int presentHighDescCount;
        
        private final int presentLowDescCount;
        
        private final int absentHighParentCount;

        private final int absentLowParentCount;

        private final int presentHighTotalCount;
        
        private final int presentLowTotalCount;
        
        private final int absentHighTotalCount;
        
        private final int absentLowTotalCount;

        private final int propagatedCount;

        private final BigDecimal rank;
        
        private final BigDecimal rankNorm;
        
        private final BigDecimal rankSum;
        //CallType always inferred from experiment count, constructor useless
//        public ExpressionCallData(CallType callType, DataType dataType) {
//            this(callType, dataType, 0, 0, 0, 0, 0, 0, 0, 0);
//        }

        public ExpressionCallData(DataType dataType, int presentHighSelfCount, 
            int presentLowSelfCount, int absentHighSelfCount, int absentLowSelfCount,
            int presentHighDescCount, int presentLowDescCount, int absentHighParentCount,
            int absentLowParentCount, int presentHighTotalCount, int presentLowTotalCount,
            int absentHighTotalCount, int absentLowTotalCount, int propagatedCount,
            BigDecimal rank, BigDecimal rankNorm, BigDecimal rankSum) {
            super(dataType, inferCallType(presentHighTotalCount,
                presentLowTotalCount, absentHighTotalCount, absentLowSelfCount));
            
            if (presentHighTotalCount < 0 || presentLowTotalCount < 0 || 
                absentHighTotalCount < 0 || absentLowTotalCount < 0 || 
                presentHighSelfCount < 0 || presentLowSelfCount < 0 || 
                absentHighSelfCount < 0 || absentLowSelfCount < 0) {
                throw log.throwing(new IllegalArgumentException("Incorrect count values"));
            }
            if (presentHighTotalCount == 0 && presentLowTotalCount == 0 && 
                absentHighTotalCount == 0 && absentLowTotalCount == 0) {
                throw log.throwing(new IllegalArgumentException(
                    "Total experiment counts must be provided to infer call type and quality"));
            }
            this.presentHighSelfCount = presentHighSelfCount;
            this.presentLowSelfCount = presentLowSelfCount;
            this.absentHighSelfCount = absentHighSelfCount;
            this.absentLowSelfCount = absentLowSelfCount;
            this.presentHighDescCount = presentHighDescCount;
            this.presentLowDescCount = presentLowDescCount;
            this.absentHighParentCount = absentHighParentCount;
            this.absentLowParentCount = absentLowParentCount;
            this.presentHighTotalCount = presentHighTotalCount;
            this.presentLowTotalCount = presentLowTotalCount;
            this.absentHighTotalCount = absentHighTotalCount;
            this.absentLowTotalCount = absentLowTotalCount;
            this.propagatedCount = propagatedCount;
            this.rank = rank;
            this.rankNorm = rankNorm;
            this.rankSum = rankSum;

        }

        public int getPresentHighSelfCount() {
            return presentHighSelfCount;
        }
        public int getPresentLowSelfCount() {
            return presentLowSelfCount;
        }
        public int getAbsentHighSelfCount() {
            return absentHighSelfCount;
        }
        public int getAbsentLowSelfCount() {
            return absentLowSelfCount;
        }
        public int getAllSelfCount() {
            return presentHighSelfCount + presentLowSelfCount + absentHighSelfCount + absentLowSelfCount;
        }

        public int getPresentHighDescCount() {
            return presentHighDescCount;
        }
        public int getPresentLowDescCount() {
            return presentLowDescCount;
        }
        public int getAbsentHighParentCount() {
            return absentHighParentCount;
        }
        public int getAbsentLowParentCount() {
            return absentLowParentCount;
        }

        public int getPresentHighTotalCount() {
            return presentHighTotalCount;
        }
        public int getPresentLowTotalCount() {
            return presentLowTotalCount;
        }
        public int getAbsentHighTotalCount() {
            return absentHighTotalCount;
        }
        public int getAbsentLowTotalCount() {
            return absentLowTotalCount;
        }
        public int getAllTotalCount() {
            return presentHighTotalCount + presentLowTotalCount + absentHighTotalCount + absentLowTotalCount;
        }

        public int getPropagatedCount() {
            return propagatedCount;
        }

        public BigDecimal getRank() {
            return rank;
        }

        public BigDecimal getRankNorm() {
            return rankNorm;
        }

        public BigDecimal getRankSum() {
            return rankSum;
        }

        private static Expression inferCallType(int presentHighTotalCount, int presentLowTotalCount, 
            int absentHighTotalCount, int absentLowTotalCount) {
            log.entry(presentHighTotalCount, presentLowTotalCount,
                absentHighTotalCount, absentLowTotalCount);
            if (presentHighTotalCount > 0 || presentLowTotalCount > 0) {
                return log.exit(Expression.EXPRESSED);
            }
            if (absentHighTotalCount > 0 || absentLowTotalCount > 0) {
                return log.exit(Expression.NOT_EXPRESSED);
            }
            throw log.throwing(new IllegalStateException("Inference of expression is not possible"
                + " because all total counts are null"));
        }

        //DataQual is now inferred only from the integration of all data types, 
        //so it doesn't make sense to have a quality score per data type. We only need experiment counts.
//        @Override
//        public DataQuality getDataQuality() {
//            log.entry();
//            if (this.getPresentHighTotalCount() > 0) {
//                return log.exit(DataQuality.HIGH);
//            }
//            if (this.getPresentLowTotalCount() > 0) {
//                return log.exit(DataQuality.LOW);
//            }
//            if (this.getAbsentHighTotalCount() > 0) {
//                return log.exit(DataQuality.HIGH);
//            }
//            if (this.getAbsentLowTotalCount() > 0) {
//                return log.exit(DataQuality.LOW);
//            }
//            return log.exit(DataQuality.NODATA);
//        }

        @Override
        public boolean isObservedData() {
            log.entry();
            if (this.presentHighSelfCount != 0 || this.presentLowSelfCount != 0 || 
                this.absentHighSelfCount != 0 || this.absentLowSelfCount != 0) {
                return log.exit(true);
            }
            return log.exit(false);
        }

        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + absentHighParentCount;
            result = prime * result + absentHighSelfCount;
            result = prime * result + absentHighTotalCount;
            result = prime * result + absentLowParentCount;
            result = prime * result + absentLowSelfCount;
            result = prime * result + absentLowTotalCount;
            result = prime * result + presentHighDescCount;
            result = prime * result + presentHighSelfCount;
            result = prime * result + presentHighTotalCount;
            result = prime * result + presentLowDescCount;
            result = prime * result + presentLowSelfCount;
            result = prime * result + presentLowTotalCount;
            result = prime * result + propagatedCount;
            result = prime * result + ((rank == null) ? 0 : rank.hashCode());
            result = prime * result + ((rankNorm == null) ? 0 : rankNorm.hashCode());
            result = prime * result + ((rankSum == null) ? 0 : rankSum.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            ExpressionCallData other = (ExpressionCallData) obj;
            if (absentHighParentCount != other.absentHighParentCount) {
                return false;
            }
            if (absentHighSelfCount != other.absentHighSelfCount) {
                return false;
            }
            if (absentHighTotalCount != other.absentHighTotalCount) {
                return false;
            }
            if (absentLowParentCount != other.absentLowParentCount) {
                return false;
            }
            if (absentLowSelfCount != other.absentLowSelfCount) {
                return false;
            }
            if (absentLowTotalCount != other.absentLowTotalCount) {
                return false;
            }
            if (presentHighDescCount != other.presentHighDescCount) {
                return false;
            }
            if (presentHighSelfCount != other.presentHighSelfCount) {
                return false;
            }
            if (presentHighTotalCount != other.presentHighTotalCount) {
                return false;
            }
            if (presentLowDescCount != other.presentLowDescCount) {
                return false;
            }
            if (presentLowSelfCount != other.presentLowSelfCount) {
                return false;
            }
            if (presentLowTotalCount != other.presentLowTotalCount) {
                return false;
            }
            if (propagatedCount != other.propagatedCount) {
                return false;
            }
            if (rank == null) {
                if (other.rank != null) {
                    return false;
                }
            } else if (!rank.equals(other.rank)) {
                return false;
            }
            if (rankNorm == null) {
                if (other.rankNorm != null) {
                    return false;
                }
            } else if (!rankNorm.equals(other.rankNorm)) {
                return false;
            }
            if (rankSum == null) {
                if (other.rankSum != null) {
                    return false;
                }
            } else if (!rankSum.equals(other.rankSum)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "ExpressionCallData [presentHighSelfCount=" + presentHighSelfCount +
                ", presentLowSelfCount=" + presentLowSelfCount + 
                ", absentHighSelfCount=" + absentHighSelfCount + 
                ", absentLowSelfCount=" + absentLowSelfCount + 
                ", presentHighDescCount=" + presentHighDescCount + 
                ", presentLowDescCount=" + presentLowDescCount + 
                ", absentHighParentCount=" + absentHighParentCount + 
                ", absentLowParentCount=" + absentLowParentCount + 
                ", presentHighTotalCount=" + presentHighTotalCount + 
                ", presentLowTotalCount=" + presentLowTotalCount + 
                ", absentHighTotalCount=" + absentHighTotalCount + 
                ", absentLowTotalCount=" + absentLowTotalCount + 
                ", propagatedCount=" + propagatedCount +
                ", rank=" + rank + ", rankNorm=" + rankNorm + ", rankSum=" + rankSum + "]";
        }
    }

    //**********************************************
    //   INSTANCE ATTRIBUTES AND METHODS
    //**********************************************
    
    private final DataType dataType;
    
    private final T callType;
    
    /**
     * Constructor allowing to specify a {@code DataType}. 
     * 
     * @param dataType  The {@code DataType} that allowed to generate the {@code CallType}.
     * @throws IllegalArgumentException    If {@code dataType} is not {@code null}.
     */
    protected CallData(DataType dataType, T callType) throws IllegalArgumentException {
        log.entry(dataType);
        
        if (dataType == null) {
            throw log.throwing(new IllegalArgumentException
                ("A DataType must be defined to instantiate a CallData."));
        }

        this.dataType = dataType;
        this.callType = callType;
        log.exit();
    }
	
    public DataType getDataType() {
        return dataType;
    }

    public T getCallType() {
        return callType;
    }
    
    //TODO: javadoc
    public abstract boolean isObservedData();
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((dataType == null) ? 0 : dataType.hashCode());
        result = prime * result + ((callType == null) ? 0 : callType.hashCode());
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
        CallData<?> other = (CallData<?>) obj;
        if (dataType != other.dataType) {
            return false;
        }
        if (callType != other.callType) {
            return false;
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "Data type: " + dataType + " - Call type: " + callType;
    }
}

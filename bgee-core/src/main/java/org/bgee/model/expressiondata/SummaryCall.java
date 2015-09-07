package org.bgee.model.expressiondata;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import org.bgee.model.expressiondata.DataDeclaration.DataQuality;
import org.bgee.model.expressiondata.DataDeclaration.DataType;

//XXX: should this class also manage the gene part and condition part?
//Or only manage the expression data part, as the CallData classes?
//In the latter case, a gene would then have, e.g., a Map<Condition, SummaryCall>, 
//in the former case, a Set<SummaryCall>
//XXX: And the Condition class should then be able to store IDs rather than objects. 
//And this class able to store a gene ID rather than a Gene object.
//XXX: should we call this class simply "Call" if it also manages genes and conditions?
//XXX: and what if it was a multi-species query? Should we use something like a MultiSpeciesCondition?
public abstract class SummaryCall<T extends SummaryCallType, U extends CallData<?>> {

    //**********************************************
    //   INNER CLASSES
    //**********************************************
    public static interface SummaryCallType {
        public static enum ExpressionSummary implements SummaryCallType {
            EXPRESSED, NOT_EXPRESSED, AMBIGUITY_ETC;
        }
        public static enum DiffExpressionSummary implements SummaryCallType {
            DIFF_EXPRESSED, OVER_EXPRESSED, UNDER_EXPRESSED, NOT_DIFF_EXPRESSED, AMBIGUITY_ETC;
        }
    }
    
    public static class ExpressionSummaryCall 
        extends SummaryCall<ExpressionSummary, ExpressionCallData> {
        
    }
    //XXX: DiffExpressionFactor managed here?
    public static class DiffExpressionSummaryCall 
        extends SummaryCall<DiffExpressionSummary, DiffExpressionCallData> {
        
    }
  //**********************************************
    //   INSTANCE ATTRIBUTES AND METHODS
    //**********************************************
    /**
     * @see #getOverallCallType()
     */
    private final T overallCallType;
    /**
     * @see #getOverallQuality()
     */
    private final DataQuality overallQuality;
    
    private final Map<DataType, U> callDataPerDataTypes;
    
    
}

package org.bgee.model.expressiondata.querytools.filters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.expressiondata.DataParameters.CallType;
import org.bgee.model.expressiondata.DataParameters.CallType.DiffExpression;
import org.bgee.model.expressiondata.DataParameters.DiffExpressionFactor;

/**
 * A <code>BasicCallFilter</code> for differential expression calls.
 * To instantiate a <code>DiffExpressionCallFilter</code>, a differential expression 
 * <code>DiffExpression</code> <code>CallType</code> and a <code>DiffExpressionFactor</code> 
 * must be provided. The <code>DiffExpressionFactor</code> allows 
 * to specify what is the experimental factor the expression comparisons should be based on, 
 * to generate the differential expression calls. For instance, <code>ANATOMY</code> 
 * indicates that the calls are generated by comparing expression in different 
 * organs at a same (broad) developmental stage. 
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @see ExpressionCallFilter
 * @see NoExpressionCallFilter
 * @since Bgee 13
 */
/*
* (non-javadoc)
* If you add attributes to this class, you might need to modify the methods 
 * <code>merge</code> and <code>canMerge</code>.
*/
public class DiffExpressionCallFilter extends BasicCallFilter {
    /**
     * An <code>int</code> defining the minimum number of conditions compared 
     * to perform a differential expression analysis.
     * @see #getMinConditionCount()
     */
    /*
     * (non-javadoc)
     * value of 3 as of Bgee 13
     */
    private static final int MINCONDITIONCOUNT = 3;
	/**
	 * <code>Logger</code> of the class. 
	 */
	private final static Logger log = 
	        LogManager.getLogger(DiffExpressionCallFilter.class.getName());

    /**
     * An <code>int</code> allowing to filter differential expression calls 
     * based on the number of conditions compared. See {@link #getConditionCount()} 
     * for important explanations. 
     */
    private int conditionCount;
    /**
     * A <code>DiffExpressionFactor</code> defining which type of differential analysis  
     * should be used to retrieve differential expression calls. 
     */
    private final DiffExpressionFactor factor;
    
	/**
     * Instantiate a <code>DiffExpressionCallFilter</code> for the given 
     * <code>callType</code> and <code>factor</code>, with no minimum number 
     * of conditions compared requested (besides the default threshold of the Bgee pipeline, 
     * see {@link #getMinConditionCount()}).
     * 
     * @param callType	A <code>DiffExpression</code> <code>CallType</code> defining 
     * 					the type of differential expression calls to use. 
     * @param factor	A <code>DiffExpressionFactor</code> specifying 
     * 					the experimental factor of the differential expression 
     * 					analyses that should be used.
     */
    public DiffExpressionCallFilter(CallType.DiffExpression callType, 
    		DiffExpressionFactor factor) throws IllegalArgumentException {
    	
    	this(callType, factor, 0);
    }
	/**
     * Instantiate a <code>DiffExpressionCallFilter</code> for the given 
     * <code>callType</code> and <code>factor</code>, with <code>conditionCount</code> 
     * as the minimum number of conditions compared requested. This number 
     * of conditions cannot be set below the value returned by {@link 
     * #getMinNumberOfConditions()} (see {@link #setConditionCount(int)} for 
     * more details).
     * 
     * @param callType	A <code>DiffExpression</code> <code>CallType</code> defining 
     * 					the type of differential expression calls to use. 
     * @param factor	A <code>DiffExpressionFactor</code> specifying 
     * 					the experimental factor of the differential expression 
     * 					analyses that should be used.
     * @param conditionCount	An <code>int</code> allowing to filter differential 
     * 							expression calls based on the number of conditions compared. 
     * @throws IllegalArgumentException    if <code>conditionCount</code> is strictly 
     *                                     below the value returned by {@link 
     *                                     #getMinConditionCount()}.
     */
    public DiffExpressionCallFilter(CallType.DiffExpression callType, 
    		DiffExpressionFactor factor, int conditionCount) 
    		throws IllegalArgumentException {
    	
    	super(callType);
    	log.entry(callType, factor);
    	this.factor = factor;
    	this.setConditionCount(conditionCount);
    	log.exit();
    }

    
    /**
     * @see #canMerge(CallFilter, boolean)
     */
    @Override
    protected DiffExpressionCallFilter merge(CallFilter filterToMerge, boolean sameEntity) {
        log.entry(filterToMerge, sameEntity);
        //first, determine whether we can merge the CallFilters
        if (!this.canMerge(filterToMerge, sameEntity)) {
            return log.exit(null);
        }

        //OK, let's proceed to the merging
        //we blindly perform the merging, it is the responsibility of the method 
        //canMerge to determine whether it is appropriate.
        DiffExpressionCallFilter otherFilter = (DiffExpressionCallFilter) filterToMerge;
        DiffExpressionCallFilter mergedCall = 
                new DiffExpressionCallFilter(this.getCallType(), this.getFactor());
        
        super.merge(otherFilter, mergedCall, sameEntity);
        //conditionCount of this CallFilter and of otherFilter should be the same, 
        //but it is not the responsibility of this method to decide whether the merging 
        //makes sense, so we use the highest value
        mergedCall.setConditionCount(Math.max(this.getConditionCount(), 
                otherFilter.getConditionCount()));

        return log.exit(mergedCall);
    }

    /**
     * Determines whether this <code>ExpressionCallFilter</code> and 
     * <code>filterToMerge</code> can be merged. 
     * <p>
     * If <code>sameEntity</code> is <code>true</code>, it means that <code>filterToMerge</code> 
     * and this <code>ExpressionCallFilter</code> are related to a same <code>Entity</code> 
     * (see {@link CallFilter#mergeSameEntityCallFilter(CallFilter)}), otherwise, to different 
     * <code>Entity</code>s (see {@link CallFilter#mergeDiffEntitiesCallFilter(CallFilter)}).
     * 
     * @param filterToMerge   A <code>CallFilter</code> that is tried to be merged 
     *                      with this <code>ExpressionCallFilter</code>.
     * @param sameEntity    a <code>boolean</code> defining whether <code>filterToMerge</code> 
     *                      and this <code>ExpressionCallFilter</code> are related to a same 
     *                      <code>Entity</code>, or different ones. 
     * @return              <code>true</code> if they could be merged. 
     */
    private boolean canMerge(CallFilter filterToMerge, boolean sameEntity) {
        log.entry(filterToMerge, sameEntity);
        
        if (!(filterToMerge instanceof DiffExpressionCallFilter)) {
            return log.exit(false);
        }
        DiffExpressionCallFilter otherFilter = (DiffExpressionCallFilter) filterToMerge;
        
        if (!super.canMerge(otherFilter, sameEntity)) {
            return log.exit(false);
        }
        
        if (!this.getCallType().equals(otherFilter.getCallType()) || 
                !this.getFactor().equals(otherFilter.getFactor()) || 
                this.getConditionCount() != otherFilter.getConditionCount()) {
            return log.exit(false);
        }
        
        return log.exit(true);
    }
    
    //************************************
    //  GETTERS/SETTERS
    //************************************
    /**
     * Return the minimum number of conditions compared to perform a differential 
     * expression analysis in the Bgee database. It is then not possible to provide 
     * a value below this minimum number to the method {@link #setConditionCount(int)}.
     * 
     * @return  An <code>int</code> defining the minimum number of conditions compared 
     *          to perform a differential expression analysis in Bgee.
     */
    public static int getMinConditionCount() {
        return MINCONDITIONCOUNT;
    }
    
    @Override
    public CallType.DiffExpression getCallType() {
        return (DiffExpression) super.getCallType();
    }
	/**
	 * Return the experimental factor that the differential expression analyses used 
	 * should be based on. 
	 * @return 	a <code>DiffExpressionFactor</code> defining which type of 
	 * 			differential analysis should be used to retrieve differential 
	 * 			expression calls. 
	 */
	public DiffExpressionFactor getFactor() {
		return this.factor;
	}
    
	/**
	 * Return the threshold to filter differential expression calls 
     * based on the number of conditions compared. It specifies that only calls 
     * involving some differential expression analyses with at least that number 
     * of conditions compared should be retrieved. For instance, to retrieve calls 
     * involving differential expression analyses having compared at least 5 organs. 
     * <p>
     * This minimum number of conditions applies to conditions related 
     * to the <code>DiffExpressionFactor</code> returned by {@link #getFactor()}. 
     * For instance, if the value returned by {@link #getFactor()} is 
     * <code>ANATOMY</code>, this threshold is the minimum number of organs 
     * compared. Or if equal to <code>DEVELOPMENT</code>, it is the minimum number 
     * of developmental stages studied.
     * <p>
     * Note that the minimum number of conditions specified by the Bgee pipeline 
     * to proceed to a differential expression analysis is still applicable in any case.
     * <p>
     //TODO: remove this part of the javadoc, this has changed and is only kept 
     //to be shown to Marc
     * <strong>Warning: </strong>because Bgee reconciles results obtained from different 
     * experiments, the calls retrieved can still include analyses with less conditions 
     * compared. What this parameter specifies, it is that a call should <em>involve</em> 
     * <em>some</em> analyses with that minimum number of conditions. 
     * For instance: if an analysis compared 3 organs A, B, and C, at a same 
     * developmental stage, and generated a call that a gene is under-expressed in A; 
     * and if another analysis, comparing 5 organs A, B, C, D, and E, at the same 
     * developmental stage, generated a call that the same gene is over-expressed 
     * in A; Then Bgee will generate an over-expression call with low confidence from 
     * these two experiments; using only the experiments with 5 conditions would 
     * generate a high confidence call. But this is not what you would retrieve 
     * by setting this parameter to 5; you would still retrieve the low confidence call, 
     * because data are not re-computed, just filtered.  
     * <p>
     * To actually recompute differential expression calls by filtering experiments 
     * based on the number of conditions compared, use a {@link RawDataFilter} 
     * as part of a {@link CompositeCallFilter}. Only {@link CompositeCallFilter}s 
     * can be used to specify to re-compute data on the fly, see methods accepting 
     * any <code>CallFilter</code>, or specifically <code>CompositeCallFilter</code>s 
     * only. 
	 * 
	 * @return 	An <code>int</code> defining the requested minimum number of conditions 
     * 			compared of analyses involved in generating the calls retrieved. 
	 */
	public int getConditionCount() {
		return this.conditionCount;
	}
	/**
	 * Set the <code>int</code> allowing to filter differential expression calls 
     * based on the minimum number of conditions compared. 
     * See {@link #getConditionCount()} for important explanations. 
     * <p>
     * It is not possible to assign a value below the minimum number of conditions 
     * used to perform a differential expression analysis in Bgee (returned by 
     * {@link #getMinConditionCount()}). Otherwise this method will throw an 
     * <code>IllegalArgumentException</code>.
     * 
	 * @param conditionCount 	An <code>int</code> defining the requested minimum 
	 * 							number of conditions compared, of analyses involved 
	 * 							in generating the calls retrieved. 
	 * @throws IllegalArgumentException    if <code>conditionCount</code> is strictly 
	 *                                     below the value returned by {@link 
	 *                                     #getMinConditionCount()}.
	 */
	public void setConditionCount(int conditionCount) throws IllegalArgumentException {
	    if (conditionCount < getMinConditionCount()) {
	        throw log.throwing(new IllegalArgumentException(
	                "The provided number of conditions to be compared (" + conditionCount + 
	                ") cannot be set below the minimum number of conditions used " +
	                "in the Bgee pipeline (" + getMinConditionCount() + ")"));
	    }
		this.conditionCount = conditionCount;
	}
}

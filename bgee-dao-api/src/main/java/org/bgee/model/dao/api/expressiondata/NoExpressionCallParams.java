package org.bgee.model.dao.api.expressiondata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.expressiondata.CallTO.DataState;

/**
 * This class allows to provide the parameters specific to no-expression calls 
 * (explicit report of absence of expression), when using a {@link DAO}, to params 
 * the no-expression calls used during queries. 
 * It allows to define conditions on the data types and data qualities of 
 * the no-expression calls to use, and to define whether they should have been 
 * generated by taking into account anatomical entities, and all of their ancestors 
 * by <em>is_a</em> or <em>part_of</em> relations (no-expression calls taking into 
 * account absence of expression in parent structures: when a gene is reported to be 
 * not expressed in a structure, it is expressed <strong>nowhere</strong> in that 
 * structure); or without taking into account ancestor anatomical entities.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class NoExpressionCallParams extends CallParams {
    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(NoExpressionCallParams.class.getName());
    
    /**
     * Default constructor.
     */
    public NoExpressionCallParams() {
        super(new NoExpressionCallTO());
    }
    
    @Override
    protected NoExpressionCallTO getReferenceCallTO() {
        return (NoExpressionCallTO) super.getReferenceCallTO();
    }

    //****************************************
    // MERGE METHODS
    //****************************************

    /**
     * @see #canMerge(CallParams, boolean)
     */
    @Override
    public NoExpressionCallParams merge(CallParams paramsToMerge) {
        log.entry(paramsToMerge);
        //first, determine whether we can merge the CallParams
        if (!this.canMerge(paramsToMerge)) {
            return log.exit(null);
        }

        //OK, let's proceed to the merging
        //we blindly perform the merging here, even if if meaningless, it is the 
        //responsibility of the method canMerge to determine whether it is appropriate.
        NoExpressionCallParams otherParams  = (NoExpressionCallParams) paramsToMerge;
        NoExpressionCallParams mergedParams = new NoExpressionCallParams();
        //of note, data types and qualities are merged by super.merge method
        super.merge(otherParams, mergedParams);
        //here, we take care of the merging of the attributes specific to 
        //NoExpressionCallParams (except the data types and qualities, managed by 
        //the parent class)
        mergedParams.setIncludeParentStructures(
                (this.isIncludeParentStructures() || otherParams.isIncludeParentStructures()));

        return log.exit(mergedParams);
    }

    /**
     * Determines whether this {@code NoExpressionCallParams} and 
     * {@code paramsToMerge} can be merged. 
     * 
     * @param paramsToMerge   A {@code CallParams} that is tried to be merged 
     *                      with this {@code NoExpressionCallParams}.
     * @return              {@code true} if they could be merged. 
     */
    @Override
    protected boolean canMerge(CallParams paramsToMerge) {
        log.entry(paramsToMerge);
        
        if (!(paramsToMerge instanceof NoExpressionCallParams)) {
            return log.exit(false);
        }
        NoExpressionCallParams otherParams = (NoExpressionCallParams) paramsToMerge;
        
        //NoExpressionCallParams with different expression propagation rules 
        //are not merged, because no-expression calls using propagation would use 
        //the best data qualities over all parent structures. As a result, 
        //it would not be possible to retrieve data qualities when no propagation is used, 
        //and so, not possible to check for the data quality conditions held 
        //by an NoExpressionCallParams not using propagation.
        //An exception is that, if an NoExpressionCallParams not using propagation 
        //was not requesting any specific quality, it could be merged with 
        //a NoExpressionCallParams using propagation. But it would be a nightmare to deal 
        //with all these specific cases in other parts of the code, as these calls 
        //would not be flagged "not taking into account parent structures" anymore...
        //So, we simply do not merge in that case.
        if (this.isIncludeParentStructures() != otherParams.isIncludeParentStructures()) {
            return log.exit(false);
        }

        //of note, this method also takes care of the check for data types 
        //and qualities
        if (!super.canMerge(otherParams)) {
            return log.exit(false);
        }
        
        return log.exit(true);
    }
    
    
    //***********************************************
    // GETTERS/SETTERS DELEGATED TO referenceCallTO
    //***********************************************
    /**
     * Returns the {@code boolean} defining whether the no-expression calls 
     * used should be based on calls generated using data from anatomical 
     * entities, and all of their ancestors by <em>is_a</em> or <em>part_of</em> 
     * relations (no-expression calls taking into account absence of expression 
     * in parent structures: when a gene is reported to be not expressed in 
     * a structure, it is expressed <strong>nowhere</strong> in that structure); 
     * or without taking into account ancestor anatomical entities. 
     * <p>
     * If {@code true}, all the ancestors will be considered. Default is {@code false}.
     * 
     * @return  A {@code boolean} defining whether absence of expression in ancestors 
     *          of anatomical entities should be considered.
     */
    public boolean isIncludeParentStructures() {
        return this.getReferenceCallTO().isIncludeParentStructures();
    }
    /**
     * Sets the {@code boolean} defining whether the no-expression calls 
     * used should be based on calls generated using data from anatomical 
     * entities, and all of their ancestors by <em>is_a</em> or <em>part_of</em> 
     * relations (no-expression calls taking into account absence of expression 
     * in parent structures: when a gene is reported to be not expressed in 
     * a structure, it is expressed <strong>nowhere</strong> in that structure); 
     * or without taking into account ancestor anatomical entities. 
     * <p>
     * If {@code true}, all the ancestors will be considered. Default is {@code false}.
     * 
     * @param include   A {@code boolean} defining whether absence of expression 
     *                  in ancestors of anatomical entities should be considered.
     */
    public void setIncludeParentStructures(boolean include) {
        this.getReferenceCallTO().setIncludeParentStructures(include);
    }
    
    /**
     * @return  the {@code DataState} defining the requested minimum contribution 
     *          of Affymetrix data to the generation of the calls to be used.
     */
    public DataState getAffymetrixData() {
        return this.getReferenceCallTO().getAffymetrixData();
    }
    /**
     * @param minContribution   the {@code DataState} defining the requested minimum 
     *                          contribution of Affymetrix data to the generation 
     *                          of the calls to be used.
     */
    public void setAffymetrixData(DataState minContribution) {
        this.getReferenceCallTO().setAffymetrixData(minContribution);
    }

    /**
     * @return  the {@code DataState} defining the requested minimum contribution 
     *          of <em>in situ</em> data to the generation of the calls 
     *          to be used.
     */
    public DataState getInSituData() {
        return this.getReferenceCallTO().getInSituData();
    }
    /**
     * @param minContribution   the {@code DataState} defining the requested minimum 
     *                          contribution of <em>in situ</em> data to the generation 
     *                          of the calls to be used.
     */
    public void setInSituData(DataState minContribution) {
        this.getReferenceCallTO().setInSituData(minContribution);
    }

    /**
     * @return  the {@code DataState} defining the requested minimum contribution 
     *          of relaxed <em>in situ</em> data to the generation of the calls 
     *          to be used.
     */
    public DataState getRelaxedInSituData() {
        return this.getReferenceCallTO().getRelaxedInSituData();
    }
    /**
     * @param minContribution   the {@code DataState} defining the requested minimum 
     *                          contribution of relaxed <em>in situ</em> data to 
     *                          the generation of the calls to be used.
     */
    public void setRelaxedInSituData(DataState minContribution) {
        this.getReferenceCallTO().setRelaxedInSituData(minContribution);
    }

    /**
     * @return  the {@code DataState} defining the requested minimum contribution 
     *          of RNA-Seq data to the generation of the calls to be used.
     */
    public DataState getRNASeqData() {
        return this.getReferenceCallTO().getRNASeqData();
    }
    /**
     * @param minContribution   the {@code DataState} defining the requested minimum 
     *                          contribution of RNA-Seq data to the generation 
     *                          of the calls to be used.
     */
    public void setRNASeqData(DataState minContribution) {
        this.getReferenceCallTO().setRNASeqData(minContribution);
    }
}

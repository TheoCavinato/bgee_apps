package org.bgee.model.dao.api.expressiondata;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.expressiondata.CallDAO.CallTO.DataState;
import org.bgee.model.dao.api.expressiondata.ExpressionCallDAO.ExpressionCallTO;

/**
 * This class allows to provide the parameters specific to expression calls, 
 * when using a {@link DAO}, to params the expression calls used during queries. 
 * It allows to define conditions on the data types and data qualities of 
 * the expression calls to use, and to define whether they should have been 
 * generated by taking into account anatomical entities/developmental stages, 
 * and all of their descendants by <em>is_a</em> and <em>part_of</em> relations 
 * (expression taking into account substructures/sub-stages), or without taking 
 * into account substructures/sub-stages.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
/*
 * (non-javadoc)
 * The super class {@code CallParams} provides all the methods related to 
 * data types and their {@code DataState}s, with a {@code protected} visibility.
 * Subclasses should then increase the visibility of the methods relative to 
 * their appropriate data types.
 * 
 * WARNING: if you add parameters specific to this class, you will likely need 
 * to modify the methods merge, canMerge, hasDataRestrictions, and 
 * getDifferentParametersCount.
 */
public class ExpressionCallParams extends CallParams {
    /**
     * {@code Logger} of the class. 
     */
    private final static Logger log = 
            LogManager.getLogger(ExpressionCallParams.class.getName());
    
    /**
     * Default constructor.
     */
    public ExpressionCallParams() {
        super(new ExpressionCallTO());
        this.setIncludeSubstructures(false);
        this.setIncludeSubStages(false);
    }
    
    @Override
    protected ExpressionCallTO getReferenceCallTO() {
        return (ExpressionCallTO) super.getReferenceCallTO();
    }
    
    //****************************************
    // MERGE METHODS
    //****************************************
    
    /**
     * @see #canMerge(CallParams)
     */
    @Override
    protected ExpressionCallParams merge(CallParams paramsToMerge) {
        log.entry(paramsToMerge);
        //first, determine whether we can merge the CallParams
        if (!this.canMerge(paramsToMerge)) {
            return log.exit(null);
        }

        //OK, let's proceed to the merging
        //we blindly perform the merging here, even if if meaningless, it is the 
        //responsibility of the method canMerge to determine whether it is appropriate.
        ExpressionCallParams otherParams = (ExpressionCallParams) paramsToMerge;
        ExpressionCallParams mergedParams = new ExpressionCallParams();
        //of note, data types and qualities are merged by super.merge method
        super.merge(otherParams, mergedParams);
        //here, we take care of the merging of the attributes specific to 
        //ExpressionCallParams (except the data types and qualities, managed by 
        //the parent class)
        mergedParams.setIncludeSubstructures(
                (this.isIncludeSubstructures() || otherParams.isIncludeSubstructures()));
        mergedParams.setIncludeSubStages(
                (this.isIncludeSubStages() || otherParams.isIncludeSubStages()));

        return log.exit(mergedParams);
    }

    /**
     * Determines whether this {@code ExpressionCallParams} and 
     * {@code paramsToMerge} can be merged. 
     * 
     * @param paramsToMerge A {@code CallParams} that is tried to be merged 
     *                      with this {@code ExpressionCallParams}.
     * @return              {@code true} if they could be merged. 
     */
    @Override
    protected boolean canMerge(CallParams paramsToMerge) {
        log.entry(paramsToMerge);
        
        if (!(paramsToMerge instanceof ExpressionCallParams)) {
            return log.exit(false);
        }
        ExpressionCallParams otherParams = (ExpressionCallParams) paramsToMerge;
        
        //ExpressionCallParams with different expression propagation rules 
        //are not merged, because expression calls using propagation would use 
        //the best data qualities over all sub-structures/sub-stages. As a result, 
        //it would not be possible to retrieve data qualities when no propagation is used, 
        //and so, not possible to check for the data quality conditions held 
        //by an ExpressionCallParams not using propagation.
        //An exception is that, if an ExpressionCallParams not using propagation 
        //was not requesting any specific quality, it could be merged with 
        //an ExpressionCallParams using propagation. But it would be a nightmare to deal 
        //with all these specific cases in other parts of the code, as these calls 
        //would not be flagged "not taking into account substructures" anymore...
        //So, we simply do not merge in that case.
        if (this.isIncludeSubstructures() != otherParams.isIncludeSubstructures() || 
                this.isIncludeSubStages() != otherParams.isIncludeSubStages()) {
            return log.exit(false);
        }
        
        //if one of the CallParams has no restriction at all (all data retrieved), 
        //then obviously a merging can occur, as the data retrieved by one CallParams 
        //will be a subset of the data retrieved by the other one.
        //we let this stub here, even if super.canMerge will do the same check 
        //just below, because if other parameters would be added, they should be checked 
        //after this stub (see DiffExpressionCallParams#canMerge(CallParams) for 
        //an example).
        if (!this.hasDataRestrictions() || !otherParams.hasDataRestrictions()) {
            return log.exit(true);
        }
        
        //of note, this method also takes care of the check for data types 
        //and qualities.
        if (!super.canMerge(otherParams)) {
            return log.exit(false);
        }
        
        return log.exit(true);
    }
    
    @Override
    protected boolean hasDataRestrictions() {
        log.entry();
        //the only parameters specific to this class are not seen as parameters 
        //restraining the data retrieved, but rather, as parameters about the way 
        //the data were generated (taking into account substructures or not, etc).
        //And ExpressionCallParams with differences for these parameters could not 
        //be merged (see method canMerge).
        //
        //So we do not consider these parameters as relevant for this method call.
        //We simply delegate to the super class
        
        return log.exit(super.hasDataRestrictions());
    }
    
    @Override
    protected int getDifferentParametersCount(CallParams otherParams) {
        log.entry();
        int diff = 0;
        if (otherParams instanceof ExpressionCallParams) {
            ExpressionCallParams params = (ExpressionCallParams) otherParams;
            //these parameters do not really restrict data retrieved, but 
            //for the sake of completeness...
            //in any case, ExpressionCallParams with different values for 
            //these parameters are not mergeable
            if (this.isIncludeSubstructures() != params.isIncludeSubstructures()) {
                diff++;
            }
            if (this.isIncludeSubStages() != params.isIncludeSubStages()) {
                diff++;
            }
        } else {
            //number of parameters in this class 
            diff = 2;
        }
        
        return log.exit(diff + super.getDifferentParametersCount(otherParams));
    }

    //**************************************
    // GETTERS/SETTERS FOR PARAMETERS SPECIFIC TO THIS CLASS, 
    // DELEGATED TO referenceCallTO
    //**************************************
    /**
     * Returns the {@code boolean} defining whether the expression calls 
     * used should be based on calls generated using data from anatomical 
     * entities, and all of their descendants by <em>is_a</em> and <em>part_of</em> 
     * relations (expression taking into account substructures), or without taking 
     * into account substructures. 
     * <p>
     * If {@code true}, all the descendants will be considered. Default is {@code false}.
     * 
     * @return  A {@code boolean} defining whether expression in substructures 
     *          of an anatomical entity should be considered.
     */
    public boolean isIncludeSubstructures() {
        return this.getReferenceCallTO().isIncludeSubstructures();
    }
    /**
     * Sets the {@code boolean} defining whether the expression calls 
     * used should be based on calls generated using data from anatomical 
     * entities, and all of their descendants by <em>is_a</em> and <em>part_of</em> 
     * relations (expression taking into account substructures), or without taking 
     * into account substructures. 
     * <p>
     * If {@code true}, all the descendants will be considered. Default is {@code false}.
     * 
     * @param include   A {@code boolean} defining whether expression 
     *                  in substructures of an anatomical entity should be considered.
     */
    public void setIncludeSubstructures(boolean include) {
        this.getReferenceCallTO().setIncludeSubstructures(include);
    }

    /**
     * Returns the {@code boolean} defining whether the expression calls 
     * used should be based on calls generated using data from developmental 
     * stages, and all of their descendants by <em>is_a</em> and <em>part_of</em> 
     * relations (expression taking into account sub-stages), or without taking 
     * into account sub-stages. 
     * <p>
     * If {@code true}, all the sub-stage will be considered. Default is {@code false}.
     * 
     * @return  A {@code boolean} defining whether expression in sub-stages 
     *          of a developmental stage should be considered.
     */
    public boolean isIncludeSubStages() {
        return this.getReferenceCallTO().isIncludeSubStages();
    }
    /**
     * Sets the {@code boolean} defining whether the expression calls 
     * used should be based on calls generated using data from developmental 
     * stages, and all of their descendants by <em>is_a</em> and <em>part_of</em> 
     * relations (expression taking into account sub-stages), or without taking 
     * into account sub-stages. 
     * <p>
     * If {@code true}, all the sub-stage will be considered. Default is {@code false}.
     * 
     * @param include   A {@code boolean} defining whether expression 
     *                  in sub-stages of a developmental stage should be considered.
     */
    public void setIncludeSubStages(boolean include) {
        this.getReferenceCallTO().setIncludeSubStages(include);
    }


    //***********************************************
    // SUPER CLASS GETTERS/SETTERS WITH INCREASED VISIBLITY
    //***********************************************
    /*
     * (non-javadoc)
     * The super class {@code CallParams} provides all the methods related to 
     * data types and their {@code DataState}s, with a {@code protected} visibility.
     * Subclasses should then increase the visibility of the methods relative to 
     * their appropriate data types.
     */
    @Override
    public DataState getAffymetrixData() {
        return super.getAffymetrixData();
    }
    @Override
    public void setAffymetrixData(DataState minContribution) {
        super.setAffymetrixData(minContribution);
    }

    @Override
    public DataState getESTData() {
        return super.getESTData();
    }
    @Override
    public void setESTData(DataState minContribution) {
        super.setESTData(minContribution);
    }

    @Override
    public DataState getInSituData() {
        return super.getInSituData();
    }
    @Override
    public void setInSituData(DataState minContribution) {
        super.setInSituData(minContribution);
    }

    @Override
    public DataState getRNASeqData() {
        return super.getRNASeqData();
    }
    @Override
    public void setRNASeqData(DataState minContribution) {
        super.setRNASeqData(minContribution);
    }
}

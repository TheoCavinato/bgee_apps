package org.bgee.model.expressiondata.querytools.filters;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.expressiondata.DataParameters.CallType;

/**
 * A <code>BasicCallFilter</code> for <code>EXPRESSED</code> call type. 
 * Provides methods specific to this call type. 
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @see NoExpressionCallFilter
 * @see DiffExpressionCallFilter
 * @since Bgee 01
 */
public class ExpressionCallFilter extends BasicCallFilter {
	/**
	 * <code>Logger</code> of the class. 
	 */
	private final static Logger log = LogManager.getLogger(ExpressionCallFilter.class.getName());

	/**
	 * A <code>boolean</code> defining whether <code>EXPRESSION</code> calls 
	 * should be propagated to 
	 * {@link org.bgee.model.anatdev.AnatomicalEntity AnatEntity} parents 
	 * following {@link org.bgee.model.ontologycommon.OntologyElement.RelationType 
	 * ISA_PARTOF} relations. 
	 * If <code>true</code>, it means that <code>EXPRESSION</code> calls 
	 * in an <code>AnatEntity</code> will take into account expression in its children.
	 */
	private boolean propagateAnatEntities;
	/**
	 * A <code>boolean</code> defining whether <code>EXPRESSION</code> calls 
	 * should be propagated to 
	 * {@link org.bgee.model.anatdev.Stage DevStage} parents 
	 * following {@link org.bgee.model.ontologycommon.OntologyElement.RelationType 
	 * ISA_PARTOF} relations. 
	 * If <code>true</code>, it means that <code>EXPRESSION</code> calls 
	 * in an <code>DevStage</code> will take into account expression in its child stages.
	 */
	private boolean propagateStages;

	/**
	 * Default constructor. 
	 */
	public ExpressionCallFilter() {
		super(CallType.Expression.EXPRESSED);
	}

	@Override
	public CallFilter mergeSameGeneCallFilter(CallFilter callToMerge) {
		log.entry(callToMerge);
		//first, determine whether we can merge the CallFilters
		if (!(callToMerge instanceof ExpressionCallFilter)) {
			return log.exit(null);
		}
		ExpressionCallFilter otherCall = (ExpressionCallFilter) callToMerge;
		if (!this.canMergeSameGeneCallFilter(otherCall)) {
			return log.exit(null);
		}
		TO CONTINUE
		
		//OK, let's proceed to the merging
		ExpressionCallFilter mergedCall = new ExpressionCallFilter();
		this.mergeSameGeneCallFilter(otherCall, mergedCall);
		TO CONTINUE
		
		
		return log.exit();
	}
	@Override
	public CallFilter mergeDifferentGeneCallFilter(CallFilter callToMerge) {
        log.entry(callToMerge);
		
		return log.exit(null);
	}
	
	//************************************
	//  GETTERS/SETTERS
	//************************************
	/**
	 * Return the <code>boolean</code> defining whether <code>EXPRESSION</code> calls 
	 * should be propagated to 
	 * {@link org.bgee.model.anatdev.AnatomicalEntity AnatEntity} parents 
	 * following {@link org.bgee.model.ontologycommon.OntologyElement.RelationType 
	 * ISA_PARTOF} relations. 
	 * If <code>true</code>, it means that <code>EXPRESSION</code> calls 
	 * in an <code>AnatEntity</code> will take into account expression in its children.
	 *
	 * @return 	a <code>boolean</code>, when <code>true</code>, 
	 * 			<code>EXPRESSION</code> calls data are propagated to 
	 * 			<code>AnatEntity</code> parents.
	 * @see #setPropagateAnatEntities(boolean)
	 * @see #isPropagateStages()
	 * @see #setPropagateStages(boolean)
	 */
	public boolean isPropagateAnatEntities() {
		return this.propagateAnatEntities;
	}
	/**
	 * Set the <code>boolean</code> defining whether <code>EXPRESSION</code> calls 
	 * should be propagated to 
	 * {@link org.bgee.model.anatdev.AnatomicalEntity AnatEntity} parents 
	 * following {@link org.bgee.model.ontologycommon.OntologyElement.RelationType 
	 * ISA_PARTOF} relations. 
	 * If <code>true</code>, it means that <code>EXPRESSION</code> calls 
	 * in an <code>AnatEntity</code> will take into account expression in its children.
	 *
	 * @param propagate 	a <code>boolean</code> defining the propagation rule 
	 * 						between <code>AnatEntity</code>s. 
	 * 						If <code>true</code>, data will be propagated to parents.
	 * @see #isPropagateAnatEntities()
	 * @see #isPropagateStages()
	 * @see #setPropagateStages(boolean)
	 */
	public void setPropagateAnatEntities(boolean propagate) {
		log.entry(propagate);
		this.propagateAnatEntities = propagate;
		log.exit();
	}

	/**
	 * Return the <code>boolean</code> defining whether <code>EXPRESSION</code> calls 
	 * should be propagated to 
	 * {@link org.bgee.model.anatdev.Stage DevStage} parents 
	 * following {@link org.bgee.model.ontologycommon.OntologyElement.RelationType 
	 * ISA_PARTOF} relations. 
	 * If <code>true</code>, it means that <code>EXPRESSION</code> calls 
	 * in a <code>DevStage</code> will take into account expression in its child stages.
	 *
	 * @return 	a <code>boolean</code>, when <code>true</code>, 
	 * 			<code>EXPRESSION</code> calls data are propagated to 
	 * 			<code>DevStage</code> parents.
	 * @see #setPropagateStages(boolean)
	 * @see #isPropagateAnatEntities()
	 * @see #setPropagateAnatEntities(boolean)
	 */
	public boolean isPropagateStages() {
		return this.propagateStages;
	}
	/**
	 * Set the <code>boolean</code> defining whether <code>EXPRESSION</code> calls 
	 * should be propagated to 
	 * {@link org.bgee.model.anatdev.Stage DevStage} parents 
	 * following {@link org.bgee.model.ontologycommon.OntologyElement.RelationType 
	 * ISA_PARTOF} relations. 
	 * If <code>true</code>, it means that <code>EXPRESSION</code> calls 
	 * in a <code>DevStage</code> will take into account expression in its child stages.
	 *
	 * @param propagate	a <code>boolean</code> defining the propagation rule 
	 * 					between <code>DevStage</code>s. 
	 * 					If <code>true</code>, data will be propagated to parents.
	 * @see #isPropagateStages()
	 * @see #isPropagateAnatEntities()
	 * @see #setPropagateAnatEntities(boolean)
	 */
	public void setPropagateStages(boolean propagate) {
		log.entry(propagate);
		this.propagateStages = propagate;
		log.exit();
	}
}

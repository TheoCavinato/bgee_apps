package org.bgee.model.expressiondata.querytools;

import java.util.Collection;

import org.bgee.model.expressiondata.querytools.GeneCallValidator.GeneCallRequirement;

/**
 * This class allows to retrieve <code>AnatomicalEntity</code>s and 
 * <code>Stage</code>s based on their gene expression data. 
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 */
public class AnatDevExpressionQuery 
{
	/**
	 * List the different methods to validate an <code>OntologyEntity</code> 
	 * when performing an expression reasoning on an <code>Ontology</code>.
	 * <ul>
	 * <li><code>STANDARD</code>: for an <code>OntologyEntity</code> to be validated, 
	 * all requested genes must have data calls in that <code>OntologyEntity</code>. 
	 * Any expression data call is accepted (expression, over-expression, no-expression, etc, 
	 * see {@link org.bgee.model.expressiondata.ExprDataParams.CallType 
	 * CallType}).
	 * <li><code>CONSERVATION</code>: for an <code>OntologyEntity</code> to be validated, 
	 * all requested genes must have data calls in agreement: either all genes with  
	 * expression/differential expression calls (<code>CallType.EXPRESSION</code>,  
	 * <code>CallType.OVEREXPRESSION</code>, or 
	 * <code>CallType.UNDEREXPRESSION</code>), or all genes with no-expression calls 
	 * (<code>CallType.NOEXPRESSION</code> or <code>CallType.RELAXEDNOEXPRESSION</code>).
	 * <li><code>DIVERGENCE</code>: for an <code>OntologyEntity</code> to be validated, 
	 * at least one gene must have data calls different from the other genes, for instance, 
	 * one gene with absence of expression while other genes are expressed. 
	 * <li><code>CUSTOM</code>: expression data calls required are set on a per gene 
	 * or per gene group basis, using {@link GeneCallValidator}s.
	 * </ul>
	 * 
	 * @author Frederic Bastian
	 * @version Bgee 13
	 * @since Bgee 13
	 */
    public enum ValidationType {
    	STANDARD, CONSERVATION, DIVERGENCE, CUSTOM;
    }
    
    /**
     * A <code>ValidationType</code> defining the method used to validate 
     * an <code>OntologyEntity</code> when performing an expression reasoning 
     * on an <code>Ontology</code>. 
	 * If this attribute is equal to <code>ValidationType.CUSTOM</code>, then the data 
	 * required for an <code>OntologyEntity</code> to be validated are set on a per gene 
	 * or per gene group basis, using {@ #customValidation}.
     */
    private ValidationType validationType;
    
    restriction on organs? (e.g., jacknife on HOGs for my analyses): only in organs/never in organs kind of?
    		useful for all anaylses or only this class?
    				
    				also, parameters "with mean expression level by experiment", probably useful for all query tools
    				this could be compute for each gene for an organ query, or for each organ on a gene query
    				this could be a last view, after data count, raw data: mean expression compared from raw data
    			    and maybe we can compute a rank for all organs for each experiment independently, something like that
}

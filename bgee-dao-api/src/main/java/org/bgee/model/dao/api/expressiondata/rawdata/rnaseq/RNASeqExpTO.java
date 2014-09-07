package org.bgee.model.dao.api.expressiondata.rawdata.rnaseq;

import org.bgee.model.dao.api.TransferObject;

/**
 * {@code TransferObject} for the class 
 * {@link org.bgee.model.expressiondata.rawdata.rnaseq.RNASeqExp}.
 * <p>
 * For information on this {@code TransferObject} and its fields, 
 * see the corresponding class.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @see org.bgee.model.expressiondata.rawdata.rnaseq.RNASeqExp
 * @since Bgee 12
 */
public class RNASeqExpTO extends TransferObject
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 17567458L;
	
	/**
	 * A {@code String} containing the description of the RNA-Seq experiment. 
	 */
	public String description;
	/**
	 * A {@code String} representing the ID of the data source 
	 * where this RNA-Seq experiment comes from. 
	 */
	public String dataSourceId;
	
	/**
	 * Default constructor. 
	 */
	public RNASeqExpTO()
	{
		super();
		this.description = "";
		this.dataSourceId = "";
	}

}
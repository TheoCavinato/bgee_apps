package org.bgee.model.dao.api.expressiondata.rawdata.insitu;

import java.io.Serializable;

import org.bgee.model.dao.api.TransferObject;

public class InSituExpTO extends TransferObject implements Serializable
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 656756L;
	
	public String description;

	public String dataSourceId;
	
	public InSituExpTO()
	{
		super();
		this.description = "";
		this.dataSourceId = "";
	}

}

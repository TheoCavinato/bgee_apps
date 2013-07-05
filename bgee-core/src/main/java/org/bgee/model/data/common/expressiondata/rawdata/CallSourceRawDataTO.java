package org.bgee.model.data.common.expressiondata.rawdata;

import model.data.common.TransferObject;

public class CallSourceRawDataTO extends TransferObject
{
    public String geneId;
    public String expressionId;
    public String expressionConfidence;
	
	public CallSourceRawDataTO()
    {
    	super();
    	
    	this.geneId = "";
    	this.expressionId = "";
    	this.expressionConfidence = "";
    }
}

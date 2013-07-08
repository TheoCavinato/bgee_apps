package org.bgee.model.dao.api.expressiondata.rawdata.affymetrix;

import org.bgee.model.dao.api.exception.DataAccessException;

/**
 * DAO defining queries using or retrieving {@link AffymetrixExpTO}s. 
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @see AffymetrixExpTO
 * @since Bgee 01
 */
public interface AffymetrixExpDAO 
{
	/**
	 * Retrieve from a data source a <code>AffymetrixExpTO</code>, corresponding to 
	 * an Affymetrix experiment with the ID <code>expId</code>, or <code>null</code> 
	 * if no corresponding experiment could be found.  
	 * 
	 * @param expId 	A <code>String</code> representing the ID 
	 * 					of the Affymetrix experiment to retrieve from the data source. 
	 * @return	A <code>AffymetrixExpTO</code>, encapsulating all the data 
	 * 			related to the Affymetrix experiment, <code>null</code> if none 
	 * 			could be found.
     * @throws DataAccessException 	If an error occurred when accessing the data source. 
	 */
	public AffymetrixExpTO getExperimentById(String expId) throws DataAccessException;
}

package org.bgee.model.expressiondata.querytools.filters;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.expressiondata.Call;
import org.bgee.model.expressiondata.DataParameters.CallType;
import org.bgee.model.expressiondata.DataParameters.DataQuality;
import org.bgee.model.expressiondata.DataParameters.DataType;

/**
 * A {@code CallFilter} specifying conditions to retrieve expression data, 
 * based on the overall expression data calls generated in Bgee. 
 * 
 * @author Frederic Bastian
 * @Version Bgee 13
 * @since Bgee 01
 */
/*
 * (non-javadoc)
 * If you add attributes to this class, you might need to modify the methods 
 * {@code merge} and {@code canMerge}.
 */
public abstract class BasicCallFilter implements CallFilter {
	/**
	 * {@code Logger} of the class. 
	 */
	private final static Logger log = LogManager.getLogger(BasicCallFilter.class.getName());


	/**
	 * A {@code Call} that will hold the parameters of this 
	 * {@code BasicCallFilter}. This is because almost all parameters 
	 * that can be set for this {@code BasicCallFilter} are retrieved 
	 * as attributes of the class {@code Call}. Methods of this 
	 * {@code BasicCallFilter} will simply be delegated to this 
	 * {@code referenceCall}.
	 */
	private final Call referenceCall;
	/**
	 * A {@code boolean} defining whether, when several {@code DataType}s 
	 * are requested for this {@code BasicCallFilter} (or none, meaning all data types 
	 * should be used), the data should be retrieved using any of them, 
	 * or based on the agreement of all of them. The recommended value is {@code false}.
	 * <p>
	 * For instance, if the {@code CallType} requested is {@code Expression}, 
	 * and the {@code DataType}s are {@code AFFYMETRIX} and {@code RNA-Seq}: 
	 * if {@code allDataTypes} is {@code false}, then expression data 
	 * will be retrieved from expression calls generated by Affymetrix or Rna-Seq data 
	 * indifferently; if {@code true}, data will be retrieved from expression calls 
	 * generated by <strong>both</code> Affymetrix and RNA-Seq data.
	 */
	private boolean allDataTypes;
	
	/**
	 * Default constructor not public. At least a {@code CallType} 
	 * should be provided, see {@link #BasicCallFilter(CallType)}.
	 */
	//Default constructor not public on purpose, suppress warning
	@SuppressWarnings("unused")
	private BasicCallFilter() {
		this(CallType.Expression.EXPRESSED);
	}

	/**
	 * Instantiate a {@code BasicCallFilter} for a type of calls 
	 * corresponding to {@code callType}, based on any data type and any quality.
	 * 
	 * @param callType	The {@code CallType} which expression data retrieval 
	 * 					will be based on.
	 */
	public BasicCallFilter(CallType callType) {
		log.entry(callType);
		
		this.referenceCall = new Call(callType);
		this.setAllDataTypes(false);

		log.exit();
	}
	
	@Override
    public BasicCallFilter mergeSameEntityCallFilter(CallFilter filterToMerge) {
        log.entry(filterToMerge);
        return log.exit(this.merge(filterToMerge, true));
    }
    @Override
    public BasicCallFilter mergeDiffEntitiesCallFilter(CallFilter filterToMerge) {
        log.entry(filterToMerge);
        return log.exit(this.merge(filterToMerge, false));
    }
    
    /**
     * Merges this {@code BasicCallFilter} with {@code filterToMerge}, 
     * and returns the resulting merged new {@code BasicCallFilter}.
     * If {@code filterToMerge} cannot be merged with this {@code BasicCallFilter}, 
     * this method returns {@code null}
     * <p>
     * If {@code sameEntity} is {@code true}, this method should correspond to 
     * {@link CallFilter#mergeSameEntityCallFilter(CallFilter)}, otherwise, to 
     * {@link CallFilter#mergeDiffEntitiesCallFilter(CallFilter)}.
     * 
     * @param filterToMerge       a {@code CallFilter} to be merged with this one.
     * @param sameEntity        a {@code boolean} defining whether 
     *                          {@code filterToMerge} and this {@code BasicCallFilter}
     *                          are related to a same {@code Entity}, or different ones. 
     * @return  A newly instantiated {@code BasicCallFilter} corresponding to 
     *          the merging of this {@code BasicCallFilter} and of 
     *          {@code filterToMerge}, or {@code null} if they could not be merged. 
     */
    protected abstract BasicCallFilter merge(CallFilter filterToMerge, boolean sameEntity);
    
	/**
	 * Merges attributes of this {@code BasicCallFilter} with {@code filterToMerge}, 
	 * by storing the merged attributes into {@code newResultingCall}. 
	 * It behaves as the methods {@link CallFilter#mergeSameEntityCallFilter(CallFilter)} 
	 * or {@link CallFilter#mergeDiffEntitiesCallFilter(CallFilter)}, 
	 * except that the newly created {@code BasicCallFilter} resulting 
	 * from the merging is provided to this method, rather than being created by it. 
	 * This is because this abstract class could not instantiate an instance 
	 * of itself, to return a newly merged {@code BasicCallFilter}. 
	 * <p>
	 * If {@code sameEntity} is {@code true}, this method corresponds to 
	 * {@link CallFilter#mergeSameEntityCallFilter(CallFilter)}, otherwise, to 
	 * {@link CallFilter#mergeDiffEntitiesCallFilter(CallFilter)}.
	 * <p>
	 * This method is needed so that child classes do not have to take care 
	 * of the merging of the attributes held by this class. 
	 * <p>
	 * If {@code filterToMerge} cannot be merged with this {@code BasicCallFilter}, 
	 * an {@code IllegalArgumentException} is thrown. This verification is achieved 
	 * by calling {@link #canMerge(BasicCallFilter, boolean)}. This latter 
	 * method only checks compatibility for attributes held by this abstract class.
	 * A child class can still decide that the merging is not possible, according 
	 * to its own attributes. 
	 * 
	 * @param filterToMerge		a {@code BasicCallFilter} to be merged with this one.
	 * @param newResultingCall	the {@code BasicCallFilter} resulting from the merging, 
	 * 							into which merged attributes will be loaded.
	 * @param sameEntity		a {@code boolean} defining whether {@code filterToMerge} 
	 * 							and this {@code BasicCallFilter} are related to a same 
	 * 							{@code Entity}, or different ones. 
	 * @throws IllegalArgumentException	If {@code filterToMerge} should not be merged 
	 * 									with this {@code BasicCallFilter}.
	 * @see {@link #canMerge(BasicCallFilter, boolean)}
	 */
	protected void merge(BasicCallFilter filterToMerge, BasicCallFilter newResultingCall, 
			boolean sameEntity) throws IllegalArgumentException {
		log.entry(filterToMerge, newResultingCall, sameEntity);
		
		if (!this.canMerge(filterToMerge, sameEntity)) {
			throw log.throwing(new IllegalArgumentException("The BasicCallFilter provided " +
					"to be merged is not compatible with the main BasicCallFilter that " +
					"this method was called on. Merging not possible."));
		}
		
		//now we just perform the merging blindly, it is the responsibility of 
		//the method canMerge to decide whether it is appropriate
		
		//merge allDataTypes. When allDataTypes is true, we apply it to the merged filter 
		//only if there is more than 1 data type set, otherwise it is equivalent to false.
		if ((this.isAllDataTypes() && this.getDataTypes().size() != 1) || 
				(filterToMerge.isAllDataTypes() && filterToMerge.getDataTypes().size() != 1)) {
		    newResultingCall.setAllDataTypes(true);
		}
		//merge data types and qualities 
		for (Entry<DataType, DataQuality> entry: this.getDataTypesQualities().entrySet()) {
			DataQuality qualToMerge = filterToMerge.getDataTypesQualities().get(entry.getKey());
			
			if (qualToMerge == null) {
				//if not present in filterToMerge, just add the entry to newResutingCall
				newResultingCall.addDataType(entry.getKey(), entry.getValue());
			} else {
				//otherwise, merge the qualities by keeping the lowest one
				DataQuality mergedQual = qualToMerge;
				if (entry.getValue().compareTo(qualToMerge) <= 0) {
					mergedQual = entry.getValue();
				} 
				newResultingCall.addDataType(entry.getKey(), mergedQual);
			}
		}
		//now we add the data types present in filterToMerge but not present 
		//in this BasicCallFilter.
		for (Entry<DataType, DataQuality> entry: 
			    filterToMerge.getDataTypesQualities().entrySet()) {
			if (!this.getDataTypes().contains(entry.getKey())) {
				newResultingCall.addDataType(entry.getKey(), entry.getValue());
			}
		}	
		
		log.exit();
	}
	
	/**
	 * Defines whether this {@code BasicCallFilter} and {@code filterToMerge} can be 
	 * merged, as far as only the attributes of this abstract class are concerned. 
	 * <p>
	 * If {@code sameEntity} is {@code true}, it means that {@code filterToMerge} 
	 * and this {@code BasicCallFilter} are related to a same {@code Entity} 
	 * (see {@link CallFilter#mergeSameEntityCallFilter(CallFilter)}), otherwise, to different 
	 * {@code Entity}s (see {@link CallFilter#mergeDiffEntitiesCallFilter(CallFilter)}).
	 * <p>
	 * This method should be used by subclasses implementing the methods 
	 * {@link CallFilter#mergeSameEntityCallFilter(CallFilter)} and {@link 
	 * CallFilter#mergeDiffEntitiesCallFilter(CallFilter)}, so that they do not need 
	 * to deal with attributes owned by this class. It means that even if this method 
	 * returns {@code true}, there is no guarantee that the child class 
	 * will accept the merging, regarding it own attributes. 
	 * 
	 * @param filterToMerge	A {@code BasicCallFilter} that is tried to be merged 
	 * 						with this {@code BasicCallFilter}.
	 * @param sameEntity	a {@code boolean} defining whether {@code filterToMerge} 
	 * 						and this {@code BasicCallFilter} are related to a same 
	 * 						{@code Entity}, or different ones. 
	 * @return		{@code true} if they could be merged, only according 
	 * 				to the attributes of this class. 
	 */
	protected boolean canMerge(BasicCallFilter filterToMerge, boolean sameEntity) {
		log.entry(filterToMerge);
		if (this.isAllDataTypes() != filterToMerge.isAllDataTypes()) {
			//if the BasicCallFilter having isAllDataTypes returning true 
			//have only one data type, then it is equivalent to having 
			//isAllDataTypes returning false
			if ((this.isAllDataTypes() && this.getDataTypes().size() != 1) || 
				(filterToMerge.isAllDataTypes() && filterToMerge.getDataTypes().size() != 1)) {
			    return log.exit(false);
			}
		} else if (this.isAllDataTypes()) {
			//if both this BasicCallFilter and filterToMerge have isAllDataTypes 
			//returning true, either they both have only one data type (which is 
			//equivalent to having isAllDataTypes returning false), 
			//or they should have exactly the same data types and qualities
			if ((this.getDataTypes().size() != 1 || filterToMerge.getDataTypes().size() != 1) && 
					!this.haveSameDataTypesQualities(filterToMerge)) {
			    return log.exit(false);
			}
		}
		//if they are BasicCallFilters related to a same Entity, we can stop here, 
		//it is possible to merge BasicCallFilters with different data types and qualities 
		//in this case.
		if (sameEntity) {
		    return log.exit(true);
		}
		//otherwise, if they are related to different Entities, they must have exactly 
		//the same data types and qualities
		return log.exit(this.haveSameDataTypesQualities(filterToMerge));
	}
	
	/**
	 * Determines whether this {@code BasicCallFilter} and {@code callToCompare} 
	 * have exactly the same {@code DataType}s and associated 
	 * {@code DataQuality}s, as returned by {@link #getDataTypesQualities()}.
	 * 
	 * @param callToCompare		A {@code BasicCallFilter} for which we want 
	 * 							to compare {@code DataType}s and associated 
	 * 							{@code DataQuality}s to this one.
	 * @return					{@code true} if this {@code BasicCallFilter} 
	 * 							and {@code callToCompare} have exactly the same 
	 * 							{@code DataType}s and {@code DataQuality}s.
	 */
	private boolean haveSameDataTypesQualities(BasicCallFilter callToCompare) {
		log.entry(callToCompare);
		
		if (this.getDataTypes().size() != callToCompare.getDataTypes().size()) {
			return log.exit(false);
		}
		for (Entry<DataType, DataQuality> entry: 
			    this.getDataTypesQualities().entrySet()) {
			
			DataQuality otherQual = callToCompare.getDataTypesQualities().get(entry.getKey());
			if (otherQual == null || !otherQual.equals(entry.getValue())) {
				return log.exit(false);
			}
		}
		return log.exit(true);
	}
	
	//************************************
	// GETTERS / SETTERS
	//************************************
	
	/**
     * Returns the {@code CallType} defining the type of call of the expression 
     * data to retrieve.
     * 
     * @return the {@code CallType} defining the type of call to use.
     */
	public CallType getCallType() {
		return this.referenceCall.getCallType();
	}
	
	/**
	 * Return the data types and qualities requested for this filter, as 
	 * an unmodifiable {@code Map} with {@code DataType}s as key defining 
	 * the data types to use, the associated value being a {@code DataQuality} 
	 * defining the <strong>minimum</strong> quality level to use for this data type. 
	 * If this {@code Map} is empty, any data type can be used, 
	 * with any data quality (minimum quality threshold set to 
	 * {@code DataQuality.LOW}).
	 * <p>
	 * Whether data retrieved should be based on the agreement of all 
	 * {@code DataType}s (taking into account their associated 
	 * {@code DataQuality}), or only at least one of them, is based on 
	 * the value returned by {@link #isAllDataTypes()}.
	 * 
	 * @return 	The {@code Map} of allowed {@code DataType}s 
	 * 			associated to a {@code DataQuality}.
	 * @see #getDataTypes()
	 */
	public Map<DataType, DataQuality> getDataTypesQualities() {
		return this.referenceCall.getDataTypesQualities();
	}
	/**
	 * Return an unmodifiable {@code set} of {@code DataType}s, being 
	 * the data types to use. The {@code DataType}s are returned 
	 * without their associated {@code DataQuality}, 
	 * see {@link #getDataTypesWithQualities()} to get them. 
	 * <p>
	 * Whether data retrieved should be based on the agreement of all 
	 * {@code DataType}s (taking into account their associated 
	 * {@code DataQuality}), or only at least one of them, is based on 
	 * the value returned by {@link #isAllDataTypes()}.
	 * 
	 * @return 	A {@code Set} of the allowed {@code DataType}s.
	 * @see #getDataTypesWithQualities()
	 */
	public Set<DataType> getDataTypes() {
		return this.referenceCall.getDataTypes();
	}
	/**
	 * Add {@code dataType} to the list of data types to use, 
	 * and use {@code dataQuality} to define the minimum data quality to use 
	 * for this data type. 
	 * <p>
	 * If this {@code DataType} was already set, replace the previous 
	 * {@code DataQuality} value set.
	 * @param dataType 		A {@code DataType} to be added to the allowed data types.
	 * @param dataQuality	A {@code DataQuality} being the minimum quality threshold 
	 * 						to use for this data type.
	 * @throws IllegalArgumentException If the type of call requested 
	 * 									(see {@link #getCallType()}), 
	 * 									and the {@code DataType} added is not compatible,  
	 * 									see {@link org.bgee.model.expressiondata.DataParameters#
	 * 									checkCallTypeDataType(CallType, DataType)}
	 * @see #addDataType(DataType)
	 * @see #addDataTypes(Collection)
	 * @see #addDataTypes(Collection, DataQuality)
	 * @see #addDataTypes(Map)
	 */
	public void addDataType(DataType dataType, DataQuality dataQuality) 
	    throws IllegalArgumentException
	{
		log.entry(dataType, dataQuality);
		this.referenceCall.addDataType(dataType, dataQuality);
		log.exit();
	}
	/**
	 * Add {@code dataType} to the list of data types to use, 
	 * with any quality threshold allowed for this data type. 
	 * <p>
	 * If this {@code DataType} was already set, replace the previous 
	 * {@code DataQuality} minimum quality threshold  for this data type 
	 * by {@code DataQuality.LOW}.
	 * @param dataType 		A {@code DataType} to be added to the allowed data types.
	 * @throws IllegalArgumentException If the type of call requested 
	 * 									(see {@link #getCallType()}), 
	 * 									and the {@code DataType} added is not compatible,  
	 * 									see {@link org.bgee.model.expressiondata.DataParameters#
	 * 									checkCallTypeDataType(CallType, DataType)}
	 * @see #addDataType(DataType, DataQuality)
	 * @see #addDataTypes(Collection)
	 * @see #addDataTypes(Collection, DataQuality)
	 * @see #addDataTypes(Map)
	 */
	public void addDataType(DataType dataType) 
		    throws IllegalArgumentException
	{
		log.entry(dataType);
		this.addDataType(dataType, DataQuality.LOW);
		log.exit();
	}
	/**
	 * Add {@code dataType}s to the list of data types to use, 
	 * with any quality threshold allowed for this data type. 
	 * <p>
	 * If one of these {@code DataType}s was already set, replace the previous 
	 * {@code DataQuality} minimum quality threshold  for this data type 
	 * by {@code DataQuality.LOW}.
	 * 
	 * @param dataTypes 	A {@code Collection} of {@code DataType}s 
	 * 						to be added to the allowed data types.
	 * @throws IllegalArgumentException If the type of call requested 
	 * 									(see {@link #getCallType()}), 
	 * 									and the {@code DataType} added is not compatible,  
	 * 									see {@link org.bgee.model.expressiondata.DataParameters#
	 * 									checkCallTypeDataType(CallType, DataType)}
	 * @see #addDataType(DataType)
	 * @see #addDataType(DataType, DataQuality)
	 * @see #addDataTypes(Collection, DataQuality)
	 * @see #addDataTypes(Map)
	 */
	public void addDataTypes(Collection<DataType> dataTypes) 
		    throws IllegalArgumentException
	{
		log.entry(dataTypes);
		this.addDataTypes(dataTypes, DataQuality.LOW);
		log.exit();
	}
	/**
	 * Add {@code dataType}s to the list of data types to use, 
	 * and use {@code dataQuality} to define the minimum data quality to use 
	 * for all of them. 
	 * <p>
	 * If one of these {@code DataType}s was already set, replace the previous 
	 * {@code DataQuality} value set.
	 * 
	 * @param dataTypes 	A {@code Collection} of {@code DataType}s 
	 * 						to be added to the allowed data types.
	 * @param dataQuality	A {@code DataQuality} being the minimum quality threshold 
	 * 						to use for all these data types.
	 * @throws IllegalArgumentException If the type of call requested 
	 * 									(see {@link #getCallType()}), 
	 * 									and some {@code DataType}s added are not compatible ,  
	 * 									see {@link org.bgee.model.expressiondata.DataParameters#
	 * 									checkCallTypeDataType(CallType, DataType)}
	 * @see #addDataType(DataType)
	 * @see #addDataType(DataType, DataQuality)
	 * @see #addDataTypes(Collection)
	 * @see #addDataTypes(Map)
	 */
	public void addDataTypes(Collection<DataType> dataTypes, DataQuality dataQuality) 
		    throws IllegalArgumentException
	{
		log.entry(dataTypes, dataQuality);
		this.referenceCall.addDataTypes(dataTypes, dataQuality);
		log.exit();
	}
	
	/**
	 * Add {@code dataType}s to the list of data types to use, associated with
	 * a {@code dataQuality} to define the minimum data quality to use 
	 * for each of them. 
	 * <p>
	 * If one of these {@code DataType}s was already set, replace the previous 
	 * {@code DataQuality} value set.
	 * 
	 * @param dataTypes 	A {@code Map} associating {@code DataType}s 
	 * 						with a {@code dataQuality}, to be added to the allowed 
	 * 						data types.
	 * @throws IllegalArgumentException If the type of call requested 
	 * 									(see {@link #getCallType()}), 
	 * 									and some {@code DataType}s added are not compatible ,  
	 * 									see {@link org.bgee.model.expressiondata.DataParameters#
	 * 									checkCallTypeDataType(CallType, DataType)}
	 * @see #addDataType(DataType)
	 * @see #addDataType(DataType, DataQuality)
	 * @see #addDataTypes(Collection)
	 * @see #addDataTypes(Collection, DataQuality)
	 */
	public void addDataTypes(Map<DataType, DataQuality> dataTypes) 
		    throws IllegalArgumentException
	{
		log.entry(dataTypes);
		this.referenceCall.addDataTypes(dataTypes);
		log.exit();
	}

	/**
	 * Return the {@code boolean} defining whether, when {@link #getDataTypes()}
	 * returns several {@code DataType}s (or none, meaning all data types should 
	 * be used), data should be retrieved using any of them, 
	 * or based on the agreement of all of them. 
	 * <p>
	 * For instance, if {@link #getCallType()} returns {@code Expression}, 
	 * and {@link #getDataTypes()} returns {@code AFFYMETRIX} and {@code RNA-Seq}: 
	 * if this method returns {@code false}, then expression data 
	 * will be retrieved from expression calls generated by Affymetrix or Rna-Seq data 
	 * indifferently; if returns {@code true}, data will be retrieved from 
	 * expression calls generated by <strong>both</code> Affymetrix and RNA-Seq data.
	 * <p>
	 * The retrieval of data from each {@code DataType} takes of course always
	 * into account the {@code DataQuality} associated to it (see 
	 * {@link #getDataTypesWithQualities()}).
	 *
	 * @return 	the {@code boolean} defining whether data should be retrieved 
	 * 			based on agreement of all {@code DataType}s, or only at least 
	 * 			one of them.
	 * @see #setAllDataTypes(boolean)
	 * @see #getCallTypes()
	 */
	public boolean isAllDataTypes() {
		return this.allDataTypes;
	}
	/**
	 * Set the {@code boolean} defining whether, when {@link #getDataTypes()}
	 * returns several {@code DataType}s (or none, meaning all data types should 
	 * be used), data should be retrieved using any of them, 
	 * or based on the agreement of all of them. The recommended value is {@code false}.
	 * <p>
	 * For instance, if {@link #getCallType()} returns {@code Expression}, 
	 * and {@link #getDataTypes()} returns {@code AFFYMETRIX} and {@code RNA-Seq}: 
	 * if this method returns {@code false}, then expression data 
	 * will be retrieved from expression calls generated by Affymetrix or Rna-Seq data 
	 * indifferently; if returns {@code true}, data will be retrieved from 
	 * expression calls generated by <strong>both</code> Affymetrix and RNA-Seq data.
	 * <p>
	 * The retrieval of data from each {@code DataType} takes of course always
	 * into account the {@code DataQuality} associated to it (see 
	 * {@link #getDataTypesWithQualities()}).
	 *
	 * @param allDataTypes 	the {@code boolean} defining whether data should 
	 * 						be retrieved based on agreement of all {@code DataType}s, 
	 * 						or only at least one of them. 
	 * @see #isAllDataTypes()
	 * @see #getCallTypes()
	 */
	public void setAllDataTypes(boolean allDataTypes) {
		log.entry(allDataTypes);
		this.allDataTypes = allDataTypes;
		log.exit();
	}

}

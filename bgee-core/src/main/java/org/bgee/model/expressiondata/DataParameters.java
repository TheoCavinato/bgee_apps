package org.bgee.model.expressiondata;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DataParameters {
	//**********************************************
  	//   INNER ENUM CLASSES
  	//**********************************************
	/**
	 * An interface representing expression data calls. It includes two nested 
	 * <code>enum</code> types: 
	 * <ul>
	 * <li>{@link CallType.Expression}: standard expression calls. Includes two values: 
	 *   <ul>
	 *   <li><code>EXPRESSED</code>: standard expression calls.
	 *   <li><code>NOTEXPRESSED</code>: no-expression calls (absence of expression 
	 *   explicitly reported).
	 *   </ul>
	 * <li>{@link CallType.DiffExpression}: differential expression calls obtained 
	 * from differential expression analyzes. Includes three values: 
	 *   <ul>
	 *   <li><code>OVEREXPRESSED</code>: over-expression calls obtained from 
	 *   differential expression analyses.
	 *   <li><code>UNDEREXPRESSED</code>: under-expression calls obtained from 
	 *   differential expression analyses.
	 *   <li><code>NOTDIFFEXPRESSED</code>: means that a gene was studied in 
	 *   a differential expression analysis, but was <strong>not</strong> found to be 
	 *   differentially expressed (neither <code>OVEREXPRESSED</code> nor 
	 *   <code>UNDEREXPRESSED</code> calls). This is different from 
	 *   <code>NOTEXPRESSED</code>, as the gene could actually be expressed, but, 
	 *   not differentially. 
	 *   </ul>
	 * </ul>
	 * 
	 * @author Frederic Bastian
     * @version Bgee 13
     * @since Bgee 13
	 */
	public interface CallType {
		/**
		 * Represents standard expression calls: 
	     * <ul>
	     * <li><code>EXPRESSED</code>: standard expression calls.
	     * <li><code>NOTEXPRESSED</code>: no-expression calls (absence of expression 
	     * explicitly reported).
	     * </ul>
	     * 
	     * @author Frederic Bastian
         * @version Bgee 13
	     * @see DiffExpression
         * @since Bgee 13
		 */
		public enum Expression implements CallType {
			EXPRESSED, NOTEXPRESSED;
		}
		/**
		 * Represents differential expression calls obtained 
		 * from differential expression analyzes: 
		 * <ul>
		 * <li><code>OVEREXPRESSED</code>: over-expression calls obtained from 
		 * differential expression analyses.
		 * <li><code>UNDEREXPRESSED</code>: under-expression calls obtained from 
		 * differential expression analyses.
		 * <li><code>NOTDIFFEXPRESSED</code>: means that a gene was studied in 
		 * a differential expression analysis, but was <strong>not</strong> found to be 
		 * differentially expressed (neither <code>OVEREXPRESSED</code> nor 
		 * <code>UNDEREXPRESSED</code> calls). This is different from 
		 * <code>NOTEXPRESSED</code>, as the gene could actually be expressed, but, 
		 * not differentially. 
		 * </ul>
		 * 
		 * @author Frederic Bastian
         * @version Bgee 13
	     * @see Expression
         * @since Bgee 13
		 */
		public enum DiffExpression implements CallType {
			OVEREXPRESSED, UNDEREXPRESSED, NOTDIFFEXPRESSED;
		}
	}
    
    /**
     * Define the different expression data types used in Bgee.
     * <ul>
     * <li><code>AFFYMETRIX</code>: microarray Affymetrix.
     * <li><code>EST</code>: Expressed Sequence Tag.
     * <li><code>INSITU</code>: <em>in situ</em> hybridization data.
     * <li><code>RELAXEDINSITU</code>: use of <em>in situ</em> hybridization data 
     * to infer absence of expression: the inference 
	 * considers expression patterns described by <em>in situ</em> data as complete. 
	 * It is indeed usual for authors of <em>in situ</em> hybridizations to report 
	 * only localizations of expression, implicitly stating absence of expression 
	 * in all other tissues. When <em>in situ</em> data are available for a gene, 
	 * we considered that absence of expression is assumed in any organ existing 
	 * at the developmental stage studied in the <em>in situ</em>, with no report of 
	 * expression by any data type, in the organ itself, or any substructure. 
     * <li><code>RNASEQ</code>: RNA-Seq data.
     * </ul>
	 * 
     * @author Frederic Bastian
     * @version Bgee 13
     * @since Bgee 13
     */
    public enum DataType {
    	AFFYMETRIX, EST, INSITU, RELAXEDINSITU, RNASEQ;
    }
    /**
     * Define the different confidence level in expression data. 
     * These information is computed differently based on the type of call 
     * and the data type.
	 * 
     * @author Frederic Bastian
     * @version Bgee 13
     * @since Bgee 13
     */
    public enum DataQuality {
    	LOW, HIGH;
    }
    
    /**
     * Define the different types of differential expression analyses, 
     * based on the experimental factor studied: 
     * <ul>
     * <li>ANATOMY: analyses comparing different anatomical structures at a same 
     * (broad) developmental stage. The experimental factor is the anatomy, 
     * these analyses try to identify in which anatomical structures genes are 
     * differentially expressed. 
     * <li>DEVELOPMENT: analyses comparing for a same anatomical structure 
     * different developmental stages. The experimental factor is the developmental time, 
     * these analyses try to identify for a given anatomical structures at which 
     * developmental stages genes are differentially expressed. 
     * </ul>
     * 
     * @author Frederic Bastian
     * @version Bgee 13
     * @since Bgee 13
     */
    public enum DiffExpressionFactor {
    	ANATOMY, DEVELOPMENT;
    }
    
    //**********************************************
  	//   STATIC CLASS ATTRIBUTES AND METHODS
  	//**********************************************
  	/**
  	 * <code>Logger</code> of the class. 
  	 */
  	private final static Logger log = LogManager.getLogger(DataParameters.class.getName());
  	/**
  	 * An unmodifiable <code>Map</code> associating each <code>CallType</code> 
  	 * in the key set to a <code>Set</code> of the <code>DataType</code>s 
  	 * allowing to generate that <code>CallType</code>. 
  	 */
  	private static final Map<CallType, Set<DataType>> allowedDataTypes = 
  			Collections.unmodifiableMap(loadAllowedDataTypes());
  	private static Map<CallType, Set<DataType>> loadAllowedDataTypes () {

  		Map<CallType, Set<DataType>> types = 
  				new HashMap<CallType, Set<DataType>>();
  		//data types generating expression calls
  		types.put(CallType.Expression.EXPRESSED, new HashSet<DataType>());
  		types.get(CallType.Expression.EXPRESSED).add(DataType.AFFYMETRIX);
  		types.get(CallType.Expression.EXPRESSED).add(DataType.EST);
  		types.get(CallType.Expression.EXPRESSED).add(DataType.INSITU);
  		types.get(CallType.Expression.EXPRESSED).add(DataType.RNASEQ);
  		//data types generating no-expression calls
  		types.put(CallType.Expression.NOTEXPRESSED, new HashSet<DataType>());
  		types.get(CallType.Expression.NOTEXPRESSED).add(DataType.AFFYMETRIX);
  		types.get(CallType.Expression.NOTEXPRESSED).add(DataType.INSITU);
  		types.get(CallType.Expression.NOTEXPRESSED).add(DataType.RELAXEDINSITU);
  		types.get(CallType.Expression.NOTEXPRESSED).add(DataType.RNASEQ);
  		//data types generating over-expression calls
  		types.put(CallType.DiffExpression.OVEREXPRESSED, new HashSet<DataType>());
  		types.get(CallType.DiffExpression.OVEREXPRESSED).add(DataType.AFFYMETRIX);
  		types.get(CallType.DiffExpression.OVEREXPRESSED).add(DataType.RNASEQ);
  		//data types generating under-expression calls
  		types.put(CallType.DiffExpression.UNDEREXPRESSED, new HashSet<DataType>());
  		types.get(CallType.DiffExpression.UNDEREXPRESSED).add(DataType.AFFYMETRIX);
  		types.get(CallType.DiffExpression.UNDEREXPRESSED).add(DataType.RNASEQ);
  		//data types generating not-differentially-expressed calls
  		types.put(CallType.DiffExpression.NOTDIFFEXPRESSED, new HashSet<DataType>());
  		types.get(CallType.DiffExpression.NOTDIFFEXPRESSED).add(DataType.AFFYMETRIX);
  		types.get(CallType.DiffExpression.NOTDIFFEXPRESSED).add(DataType.RNASEQ);

  		return types;
  	}
  	/**
  	 * Return an unmodifiable <code>Map</code> associating each <code>CallType</code> 
  	 * in the key set to a <code>Set</code> containing the <code>DataType</code>s 
  	 * allowing to generate that <code>CallType</code>. 
  	 * 
  	 * @return 	an unmodifiable <code>Map</code> providing the allowed <code>DataType</code>s 
  	 * 			for each <code>CallType</code>.
  	 * @see #checkCallTypeDataType(CallType, DataType);
  	 * @see #checkCallTypeDataTypes(CallType, Collection);
  	 */
  	public static Map<CallType, Set<DataType>> getAllowedDataTypes() {
  		return allowedDataTypes;
  	}
  	/**
  	 * Check if <code>DataType</code> is compatible with <code>callType</code> 
  	 * (see {@link #getAllowedDataTypes()}). 
  	 * An <code>IllegalArgumentException</code> is thrown if an incompatibility 
  	 * is detected,
  	 * 
  	 * @param callType 		The <code>CallType</code> to check against 
  	 * 						<code>dataTypes</code>
  	 * @param dataType		A <code>DataType</code> to check against 
  	 * 						<code>callType</code>.
  	 * @throws IllegalArgumentException 	If an incompatibility is detected.
  	 */
  	public static void checkCallTypeDataType(CallType callType, DataType dataType) 
  			throws IllegalArgumentException {
  		checkCallTypeDataTypes(callType, Arrays.asList(dataType));
  	}
  	/**
  	 * Check if all <code>DataType</code>s in <code>dataTypes</code> are compatible 
  	 * with <code>callType</code> (see {@link #getAllowedDataTypes()}). 
  	 * An <code>IllegalArgumentException</code> is thrown if an incompatibility 
  	 * is detected,
  	 * 
  	 * @param callType 		The <code>CallType</code> to check against 
  	 * 						<code>dataTypes</code>
  	 * @param dataTypes		A <code>Collection</code> of <code>DataType</code>s to check 
  	 * 						against <code>callType</code>.
  	 * @throws IllegalArgumentException 	If an incompatibility is detected.
  	 */
  	private static void checkCallTypeDataTypes(CallType callType, Collection<DataType> dataTypes) 
  			throws IllegalArgumentException {
  		log.entry(callType, dataTypes);

  		String exceptionMessage = "";
  		Set<DataType> allowedTypes = getAllowedDataTypes().get(callType);
  		for (DataType dataType: dataTypes) {
  			if (!allowedTypes.contains(dataType)) {
  				exceptionMessage += dataType + " does not allow to generate " + 
  						callType + " calls. ";
  			}
  		}

  		if (!"".equals(exceptionMessage)) {
  			throw log.throwing(new IllegalArgumentException(exceptionMessage));
  		}

  		log.exit();
  	}
}

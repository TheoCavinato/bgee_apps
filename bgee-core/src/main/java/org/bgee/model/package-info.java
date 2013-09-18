/**
 * Core layer (also known as "business layer") of the Bgee application. 
 * <h3>Starting up and releasing resources</h3>
 * Please note that: at the start of the Bgee application, 
 * {@link StartUpShutdown#startUpApplication()} should be called; at then end 
 * of the execution of a <code>Thread</code>, {@link StartUpShutdown#threadTerminated()} 
 * should be called, to release resources hold by it; at application complete shutdown, 
 * {@link StartUpShutdown#shutdownApplication()} shoud be called.
 * <h3>Entities</h3>
 * This core layer provides classes describing "entities" used in Bgee (for instance, 
 * <code>Gene</code>, or <code>Species</code>), and for each of these entity class, 
 * a <code>Factory</code> to obtain objects of this class (for instance, 
 * a <code>GeneFactory</code>, a <code>SpeciesFactory</code>). 
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 01
 */
package org.bgee.model;
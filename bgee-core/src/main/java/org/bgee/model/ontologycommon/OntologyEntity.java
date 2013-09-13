package org.bgee.model.ontologycommon;

import java.util.Map;
import java.util.Set;

import org.bgee.model.Entity;

/**
 * An element part of an {@link Ontology}, holding an {@link org.bgee.model.Entity Entity}. 
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 * @param <T>	a subclass of {@link org.bgee.model.Entity Entity}
 */
public abstract class OntologyEntity<T extends OntologyEntity> extends Entity {

	private Map<RelationType, Set<T>> directParents;
	private Map<RelationType, Set<T>> directChildren;

	/**
     * Constructor providing the <code>id</code> of this <code>OntologyEntity</code>. 
     * This <code>id</code> cannot be <code>null</code>, or empty (""), 
     * or whitespace only, otherwise an <code>IllegalArgumentException</code> 
     * will be thrown. The ID will also be immutable, see {@link #getId()}.
     * 
     * @param id	A <code>String</code> representing the ID of this object.
     * @throws IllegalArgumentException 	if <code>id</code> is <code>null</code>,  
     * 										empty, or whitespace only. 
     */
	public OntologyEntity(String id) throws IllegalArgumentException {
		super(id);
	}
}

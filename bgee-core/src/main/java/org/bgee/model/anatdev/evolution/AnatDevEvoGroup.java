package org.bgee.model.anatdev.evolution;

import java.util.Collection;
import java.util.Set;

import org.bgee.model.anatdev.AnatDevEntity;
import org.bgee.model.species.Taxon;

/**
 * Group of {@link AnatDevEntity}s, related by a transitive evolutionary relation 
 * of a type {@link TransRelationType} (for instance, <code>HOMOLOGY</code>). 
 * This class allows to store the linked <code>AnatDevEntity</code>s (see 
 * {@link #getMainEntites()}), the type of the relation between them (see 
 * {@link #getRelationType()}), the {@link Taxon} specifying the scope of 
 * the relation (see {@link #getTaxon()}), and the {@link AssertionSupport}s 
 * supporting this relation (see {@link #getSupportingInformation()}). 
 * It also allows to store related entities (see below, and 
 * {@link #getRelatedEntities()}).
 * <p>
 * Most of the time, an <code>AnatDevEvoGroup</code> will contain only one 
 * main <code>AnatDevEntity</code> (see {@link #getMainEntites()}); for instance, 
 * the {@link org.bgee.model.anatdev.AnatomicalEntity AnatomicalEntity} "cranium" 
 * has a <code>TransRelationType</code> <code>HOMOLOGY</code>, standing in the 
 * <code>Taxon</code> "Craniata", meaning that "cranium" first evolved in 
 * the taxon "Craniata". This <code>AnatDevEvoGroup</code> would then only contain 
 * one main <code>AnatomicalEntity</code>, "cranium".
 * <p>
 * But as another example, in the case of the homology between "lung" and 
 * "swim bladder", it does not exist any <code>AnatomicalEntity</code> 
 * in the <code>AnatomicalOntology</code>, representing the common ancestral 
 * structure which these organs originated from. So the <code>AnatDevEvoGroup</code> 
 * would contain these two <code>AnatomicalEntity</code>s as main entities.
 * <p>
 * This class also allows to store related <code>AnatDevEntity</code>s (see 
 * {@link #getRelatedEnties()}): these are <code>AnatDevEntity</code>s that should 
 * be grouped along with the main <code>AnatDevEntity</code>s, but are not annotated 
 * as such. Consider for instance an <code>AnatDevEvoGroup</code> representing 
 * the homology of "brain" in the taxon "Chordata". The term "future brain" is not 
 * annotated as homologous in "Chordata" in Bgee, only the term "brain" is, but it would 
 * be good to consider it as well. In that case, the "future brain" will be retrieved 
 * thanks to the {@link org.bgee.model.ontologycommon.Ontology.RelationType TRANSFORMATION_OF} 
 * relation between "brain" and "future brain", and will be stored as a related entity, 
 * so that expression comparison could be made on it as well.
 * <p>
 * Of note, as expressed by the generic type, an <code>AnatDevEvoGroup</code> can also 
 * contain {@link org.bgee.model.anatdev.Stage Stage}s. This is because broad stages, 
 * such as "embryo", are considered homologous for the sake of performing expression 
 * comparisons in different species using developmental time too. 
 * <p>
 * Also, a fully "formed" <code>AnatDevEvoGroup</code> should always contain a 
 * <code>TransRelationType</code>, a <code>Taxon</code>, and some 
 * <code>AssertionSupport</code>s. But we leave opened the possibility to only specify 
 * the main <code>AnatDevEntity</code>s, to be able to create "fake" groupings, 
 * in order for instance to test alternative homology hypotheses, or to provide 
 * randomized relations.
 * 
 * @author Frederic Bastian
 * @version Bgee 13
 * @since Bgee 13
 *
 * @param <T>	The type of <code>AnatDevEntity</code> that this 
 * 				<code>AnatDevEvoGroup</code> contains.
 */
public class AnatDevEvoGroup<T extends AnatDevEntity> {
	/**
	 * Represents the different type of evolutionary transitive relations. 
	 * They are taken from the 
	 * <a href='http://www.obofoundry.org/cgi-bin/detail.cgi?id=homology_ontology'>
	 * HOM ontology</a>, but as long as we do not use more concepts, we will 
	 * simply used this <code>enum</code>.
	 * 
	 * @author Frederic Bastian
	 * @version Bgee 13
     * @since Bgee 13
	 */
    public enum TransRelationType {
    	HOMOLOGY, HOMOPLASY;
    }
    
    /**
     * A <code>Set</code> of <code>T</code>, representing the {@link AnatDevEntity}s 
     * grouped by an evolutionary relation. 
     * <p>
     * Most of the time, it will contain only one <code>AnatDevEntity</code>; 
     * for instance, the <code>AnatomicalEntity</code> "cranium" has a {@link #relationType} 
     * <code>HOMOLOGY</code>, standing in the {@link #taxon} "Craniata"; 
     * this <code>Set</code> would then contain only one entry.
     * <p>
     * But as another example, in the case of the homology between "lung" and 
     * "swim bladder", it does not exist any <code>AnatomicalEntity</code> 
     * in the <code>AnatomicalOntology</code> representing the common ancestral 
     * structure which these organs originated from. So this <code>Set</code> 
     * would then contain these two entries.
     * <p>
     * Related entities that should be grouped along with #mainEntities, but are not 
     * annotated as such, are stored in {@link #relatedEntities}, see this attribute 
     * for more details.
     * <p>
     * Of note, as expressed by the generic type, this <code>Set</code> can also 
     * contain <code>Stage</code>s. This is because broad stages, such as "embryo", 
     * are considered homologous for the sake of performing expression comparisons 
     * in different species using developmental time too. 
     * 
     * @see #relatedEntities
     * @see #relationType
     * @see #taxon
     */
    private Set<T> mainEntities;
    
    /**
     * A <code>Set</code> of <code>T</code>, representing the {@link AnatDevEntity}s 
     * that should be grouped along with {@link #mainEntities}, but are not annotated 
     * as such. Consider for instance an <code>AnatDevEvoGroup</code> representing 
     * the homology of "brain" in the taxon "Chordata". The term "future brain" is not 
     * annotated as homologous in "Chordata" in Bgee, only the term "brain" is, but it would 
     * be good to consider it as well. In that case, the "future brain" will be retrieved 
     * thanks to the {@link org.bgee.model.ontologycommon.Ontology.RelationType 
     * TRANSFORMATION_OF} relation between "brain" and "future brain", and will be stored as 
     * a related entity, so that expression comparison could be made on it as well.
     * <p>
     * <code>AnatDevEntity</code>s stored in this <code>Set</code> never have 
     * themselves an annotated relation of the type {@link #relationType} in Bgee.
     * 
     * @see #mainEntities
     */
    private Set<T> relatedEntities;
    /**
     * A <code>TransRelationType</code> defining the type of relation linking 
     * the {@link #mainEntities}, at the taxonomical range provided by {@link #taxon}.
     * @see #relatedEntities
     * @see #taxon
     */
    private TransRelationType relationType;
    /**
     * A <code>Taxon</code> providing the scope of this evolutionary relation.
     * For instance, if {@link #relationType} is <code>HOMOLOGY</code>, 
     * this attribute designs the taxon of the common ancestor that 
     * the {@link #mainEntities} were inherited from.
     * @see #relatedEntities
     * @see #relationType
     */
    private Taxon taxon;
    /**
     * A <code>Collection</code> of <code>AssertionSupport</code>s providing 
     * the supporting information for this evolutionary relation between 
     * the {@link #mainEntities}. Most of the time, only one 
     * <code>AssertionSupport</code> is backing up this assertion, but several 
     * can be provided (for instance, several types of evidence supporting 
     * an assertion).
     */
    private Collection<AssertionSupport> supportingInformation;
    
}

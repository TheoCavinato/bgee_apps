package org.bgee.model.anatdev.multispemapping;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.anatdev.AnatEntity;
import org.bgee.model.species.Taxon;

/**
 * This class represents a similarity group of anatomical entities.
 * 
 * @author  Philippe Moret
 * @author  Valentine Rech de Laval
 * @author  Frederic Bastian
 * @version Bgee 14 Mar. 2019
 * @since   Bgee 13, Apr. 2016
 */
public class AnatEntitySimilarity {
    private final static Logger log = LogManager.getLogger(AnatEntitySimilarity.class.getName());

    private final Taxon taxon;
    /**
     * @see #getAnatEntities
     */
    private final Set<AnatEntity> anatEntities;
    
    
    //XXX: we might need to include these fields this at some point
    //private String cioId;    
    //
    
    /**
     * @param taxon         The {@code Taxon} this {@code AnatEntitySimilarity} is valid for.
     * @param anatEntities  A {@code Collection} of {@code AnatEntity}s that are part of
     *                      this {@code AnatEntitySimilarity} group.
     */
    public AnatEntitySimilarity(Taxon taxon, Collection<AnatEntity> anatEntities) {
        this.taxon = taxon;
        if (anatEntities == null || anatEntities.isEmpty()) {
            throw log.throwing(new IllegalArgumentException("Anat. entities must be provided."));
        }
        this.anatEntities = Collections.unmodifiableSet(new HashSet<>(anatEntities));
    }
    
    /**
     * @return The {@code Taxon} this {@code AnatEntitySimilarity} is valid for.
     */
    public Taxon getTaxon() {
        return taxon;
    }
    /**
     * @return  An unmodifiable {@code Set} containing all {@code AnatEntity}s that are part of
     *          this {@code AnatEntitySimilarity} group.
     */
    // TODO: To be updated when anat. entities from 'transformation of' will be added
    public Set<AnatEntity> getAllAnatEntities() {
        return anatEntities;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((anatEntities == null) ? 0 : anatEntities.hashCode());
        result = prime * result + ((taxon == null) ? 0 : taxon.hashCode());
        return result;
    }
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        AnatEntitySimilarity other = (AnatEntitySimilarity) obj;
        if (anatEntities == null) {
            if (other.anatEntities != null) {
                return false;
            }
        } else if (!anatEntities.equals(other.anatEntities)) {
            return false;
        }
        if (taxon == null) {
            if (other.taxon != null) {
                return false;
            }
        } else if (!taxon.equals(other.taxon)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("AnatEntitySimilarity [taxon=").append(taxon).append(", anatEntities=").append(anatEntities)
                .append("]");
        return builder.toString();
    }
}
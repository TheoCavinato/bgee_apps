package org.bgee.model.anatdev;

/**
 * Class describing taxon constraints.
 * 
 * @author  Valentine Rech de Laval
 * @author  Frederic Bastian
 * @version Bgee 14 Feb. 2017
 * @since   Bgee 13, May 2016
 * 
 * @param <T> the type of ID of the related entity
 */
//TODO: actually, shouldn't it use a Set<String> speciesIds? We don't have to stick 
//to the design in the database...
public class TaxonConstraint<T> {
    
    /**
     * A {@code T} that is the ID of the entity that has a taxon constraint.
     */
    private final T entityId;
    
    /**
     * An {@code Integer} that is the ID of the species that define the taxon constraint. 
     * If it is {@code null}, it means that the entity exists in all species.
     */
    private final Integer speciesId;

    /**
     * Constructor providing the {@code entityId} and the {@code speciesId}
     * of this {@code TaxonConstraint}.
     * <p>
     * If {@code speciesId} is {@code null}, it means that the entity exists in all species.
     * 
     * @param entityId    A {@code T} that is the ID of the entity that has a taxon constraint.
     * @param speciesId   An {@code Integer} that is the ID of the species that define
     *                    the taxon constraint.
     */
    public TaxonConstraint(T entityId, Integer speciesId) {
        this.entityId = entityId;
        this.speciesId = speciesId;
    }

    /**
     * @return  A {@code T} that is the ID of the entity that has a taxon constraint.
     */
    public T getEntityId() {
        return entityId;
    }

    /**
     * @return  An {@code Integer} that is the ID of the species that define the taxon constraint. 
     *          If it is {@code null}, it means that the entity exists in all species.
     */
    public Integer getSpeciesId() {
        return speciesId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entityId == null) ? 0 : entityId.hashCode());
        result = prime * result + ((speciesId == null) ? 0 : speciesId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TaxonConstraint<?> other = (TaxonConstraint<?>) obj;
        if (entityId == null) {
            if (other.entityId != null)
                return false;
        } else if (!entityId.equals(other.entityId))
            return false;
        if (speciesId == null) {
            if (other.speciesId != null)
                return false;
        } else if (!speciesId.equals(other.speciesId))
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "Entity ID: " + this.getEntityId() + " - Species ID: " + this.getSpeciesId();
    }
}

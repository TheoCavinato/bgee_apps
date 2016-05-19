package org.bgee.model.species;

import org.bgee.model.NamedEntity;

/**
 * Class allowing to describe species used in Bgee.
 * 
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 13, Nov 2013
 * @since   Bgee 01
 */
public class Species extends NamedEntity {
    
	/** @see #getGenus() */
	private final String genus;
	
    /** @see #getSpeciesName() */
    private final String speciesName;
    
    /** @see #getGenomeVersion() */
    private final String genomeVersion;
	
    private final String parentTaxonId;
    
    /**
     * 0-arg constructor private, at least an ID must be provided, see {@link #Species(String)}.
     */
    @SuppressWarnings("unused")
    private Species() {
        this(null);
    }
    
    /**
     * Constructor providing the {@code id} of this {@code Species}.
     * This {@code id} cannot be blank,
     * otherwise an {@code IllegalArgumentException} will be thrown.
     *
     * @param id    A {@code String} representing the ID of this object.
     * @throws IllegalArgumentException if {@code id} is blank.
     */
    public Species(String id) throws IllegalArgumentException {
        this(id, null, null);
    }
    /**
     * Constructor of {@code Species}.
     * @param id            A {@code String} representing the ID of this {@code Species}. 
     *                      Cannot be blank.
     * @param name          A {@code String} representing the (common) name of this {@code Species}.
     * @param description   A {@code String} description of this {@code Species}.
     */
    public Species(String id, String name, String description) throws IllegalArgumentException {
        this(id, name, description, null, null, null, null);
    }
    
    /**
     * Constructor of {@code Species}.
     * @param id            A {@code String} representing the ID of this {@code Species}. 
     *                      Cannot be blank.
     * @param name          A {@code String} representing the (common) name of this {@code Species}.
     * @param description   A {@code String} description of this {@code Species}.
     * @param genus         A {@code String} representing the genus of this {@code Species} 
     *                      (e.g., "Homo" for human).
     * @param speciesName   A {@code String} representing the species name of this 
     *                      {@code Species} (e.g., "sapiens" for human).
     * @param genomeVersion A {@code String} representing the genome version used for 
     *                      this {@code Species}.
     * @param parentTaxonId A {@code String} representing the ID of the parent Taxon of this species
     */
    public Species(String id, String name, String description, String genus, String speciesName,
            String genomeVersion, String parentTaxonId) throws IllegalArgumentException {
        super(id, name, description);
        this.genus = genus;
        this.speciesName = speciesName;
        this.genomeVersion = genomeVersion;
        this.parentTaxonId = parentTaxonId;
    }

    /**
     * @return A {@code String} representing the genus of the species (e.g., "Homo" for human).
     */
    public String getGenus() {
    	return this.genus;
    }

    /**
     * @return  A {@code String} representing the species name 
     *          of this {@code Species} (e.g., "sapiens" for human).
     */
    public String getSpeciesName() {
    	return this.speciesName;
    }
    
    /**
     * @return A {@code String} representing the genome version used for this {@code Species}
     */
    public String getGenomeVersion() {
        return this.genomeVersion;
    }
    
    /**
     * @return  A {@code String} representing the species common name 
     *          (e.g., "human" for Homo sapiens).
     */
    @Override
    //method overridden to provide a more accurate javadoc
    public String getName() {
        return super.getName();
    }
    
    /**
     * @return  A {@code String} that is the scientific name of this {@code Species}, 
     *          for instance, "Homo sapiens" for human. 
     */
    public String getScientificName() {
        return this.getGenus() + " " + this.getSpeciesName();
    }

    /**
     * @return  A {@code String} containing a short representation of the name 
     *          (e.g., "H. sapiens" for Homo sapiens).
     */
    public String getShortName() {
    	if (genus == null || speciesName == null) return "";
    	return genus.toUpperCase().charAt(0) +". "+speciesName;
    }
    
    /**
     * @return A {@code String} representing the ID of the parent Taxon of this species
     */
    public String getParentTaxonId() {
        return this.parentTaxonId;
    }

	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((genus == null) ? 0 : genus.hashCode());
        result = prime * result + ((speciesName == null) ? 0 : speciesName.hashCode());
        result = prime * result + ((genomeVersion == null) ? 0 : genomeVersion.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return super.toString() + "Genus: " + genus + "- Species name: " + speciesName + 
		        " - Genome version=" + genomeVersion;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Species other = (Species) obj;
		if (genus == null) {
			if (other.genus != null) {
				return false;
			}
		} else if (!genus.equals(other.genus)) {
			return false;
		}
		if (speciesName == null) {
			if (other.speciesName != null) {
				return false;
			}
		} else if (!speciesName.equals(other.speciesName)) {
			return false;
		}
        if (genomeVersion == null) {
            if (other.genomeVersion != null) {
                return false;
            }
        } else if (!genomeVersion.equals(other.genomeVersion)) {
            return false;
        }
		return true;
	}
}


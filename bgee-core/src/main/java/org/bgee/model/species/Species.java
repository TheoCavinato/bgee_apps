package org.bgee.model.species;

import org.bgee.model.Entity;

/**
 * Class allowing to describe species used in Bgee.
 * 
 * @author Frederic Bastian
 * @version Bgee 13, Mar 2013
 * @since Bgee 01
 */
public class Species extends Entity {
    /**
     * Constructor providing the {@code id} of this {@code Species}.
     * This {@code id} cannot be {@code null}, or blank,
     * otherwise an {@code IllegalArgumentException} will be thrown.
     *
     * @param id A {@code String} representing the ID of this object.
     * @throws IllegalArgumentException if {@code id} is {@code null},
     *                                  or blank.
     */
    public Species(String id) throws IllegalArgumentException {
        super(id);
    }

    /**
     * Constructor of {@code Species}
     * @param id A {@code String} representing the ID of this object.
     * @param name A {@code String} representing the name of this object.
     * @param description A {@code String} description of the {@Species}
     */
    public Species(String id, String name, String description) {
        super(id, name, description);
    }
}


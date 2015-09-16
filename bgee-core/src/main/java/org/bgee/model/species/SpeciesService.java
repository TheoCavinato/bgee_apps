package org.bgee.model.species;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.Service;
import org.bgee.model.dao.api.DAOManager;
import org.bgee.model.dao.api.species.SpeciesDAO;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * A {@link Service} to obtain {@link Species} objects. 
 * Users should use the {@link ServiceFactory} to obtain {@code SpeciesService}.
 * 
 * @author Philippe Moret
 * @author Frederic Bastian
 * @version Bgee 13 Sept. 2015
 * @since Bgee 13 Sept. 2015
 */
public class SpeciesService extends Service {
    
    private static final Logger log = LogManager.getLogger(SpeciesService.class.getName());
    
    /**
     * 0-arg constructor that will cause this {@code SpeciesService} to use 
     * the default {@code DAOManager} returned by {@link DAOManager#getDAOManager()}. 
     * 
     * @see #SpeciesService(DAOManager)
     */
    public SpeciesService() {
        this(DAOManager.getDAOManager());
    }
    /**
     * @param daoManager    The {@code DAOManager} to be used by this {@code SpeciesService} 
     *                      to obtain {@code DAO}s.
     */
    public SpeciesService(DAOManager daoManager) {
        super(daoManager);
    }

    /**
     * Loads all species that are part of at least one {@code SpeciesDataGroup}.
     * 
     * @return  A {@code Set} containing the {@code Species} part of some {@code SpeciesDataGroup}s.
     * @see org.bgee.model.file.SpeciesDataGroup
     */
    public Set<Species> loadSpeciesInDataGroups() {
        log.entry();
        return log.exit(this.getDaoManager().getSpeciesDAO().getSpeciesFromDataGroups()
                .stream()
                .map(SpeciesService::mapFromTO)
                .collect(Collectors.toSet()));
    }

    /**
     * Maps a {@code SpeciesTO} to a {@code Species} instance (Can be passed as a {@code Function}). 
     * 
     * @param speciesTO The {@code SpeciesTO} to be mapped
     * @return the mapped {@code Species}
     */
    private static Species mapFromTO(SpeciesDAO.SpeciesTO speciesTO) {
        log.entry(speciesTO);
        return log.exit(new Species(speciesTO.getId(), speciesTO.getName(), 
                speciesTO.getDescription()));
    }

}
package org.bgee.model.anatdev;

import java.util.Collection;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.CommonService;
import org.bgee.model.Service;
import org.bgee.model.ServiceFactory;

/**
 * A {@link Service} to obtain {@link TaxonConstraint} objects. 
 * Users should use the {@link org.bgee.model.ServiceFactory} to obtain {@code TaxonConstraint}s.
 * 
 * @author  Valentine Rech de Laval
 * @author  Frederic Bastian
 * @version Bgee 14 Nov. 2017
 * @since   Bgee 13, May 2016
 */
public class TaxonConstraintService extends CommonService {
    
    private static final Logger log = LogManager.getLogger(TaxonConstraintService.class.getName());

    /**
     * @param serviceFactory            The {@code ServiceFactory} to be used to obtain {@code Service}s 
     *                                  and {@code DAOManager}.
     * @throws IllegalArgumentException If {@code serviceFactory} is {@code null}.
     */
    public TaxonConstraintService(ServiceFactory serviceFactory) {
        super(serviceFactory);
    }

    /**
     * Retrieve anatomical entity taxon constraints for a given set of species IDs.
     * 
     * @param speciesIds    A {@code Collection} of {@code Integer}s that are IDs of species 
     *                      for which to return the {@code TaxonConstraint}s.
     * @return              A {@code Stream} of {@code TaxonConstraint}s that are 
     *                      the {@code TaxonConstraint}s for the given set of species IDs.
     */
    public Stream<TaxonConstraint<String>> loadAnatEntityTaxonConstraintBySpeciesIds(Collection<Integer> speciesIds) {
        log.entry(speciesIds);
        
        return log.exit(getDaoManager().getTaxonConstraintDAO()
                    .getAnatEntityTaxonConstraints(speciesIds, null).stream()
                    .map(CommonService::mapTaxonConstraintTOToTaxonConstraint));
    }
    
    /**
     * Retrieve anatomical entity relation taxon constraints for a given set of species IDs.
     * 
     * @param speciesIds    A {@code Collection} of {@code Integer}s that are IDs of species 
     *                      for which to return the {@code TaxonConstraint}s.
     * @return              A {@code Stream} of {@code TaxonConstraint}s that are 
     *                      the {@code TaxonConstraint}s for the given set of species IDs.
     */
    public Stream<TaxonConstraint<Integer>> loadAnatEntityRelationTaxonConstraintBySpeciesIds(
            Collection<Integer> speciesIds) {
        log.entry(speciesIds);
        
        return log.exit(getDaoManager().getTaxonConstraintDAO()
                    .getAnatEntityRelationTaxonConstraints(speciesIds, null).stream()
                    .map(CommonService::mapTaxonConstraintTOToTaxonConstraint));
    }

    /**
     * Retrieve developmental stage taxon constraints for a given set of species IDs.
     * 
     * @param speciesIds    A {@code Collection} of {@code Integer}s that are IDs of species 
     *                      for which to return the {@code TaxonConstraint}s.
     * @return              A {@code Stream} of {@code TaxonConstraint}s that are 
     *                      the {@code TaxonConstraint}s for the given set of species IDs.
     */
    public Stream<TaxonConstraint<String>> loadDevStageTaxonConstraintBySpeciesIds(Collection<Integer> speciesIds) {
        log.entry(speciesIds);
        
        return log.exit(getDaoManager().getTaxonConstraintDAO()
                    .getStageTaxonConstraints(speciesIds, null).stream()
                    .map(CommonService::mapTaxonConstraintTOToTaxonConstraint));
    }
}
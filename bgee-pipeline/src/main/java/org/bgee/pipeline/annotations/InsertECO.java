package org.bgee.pipeline.annotations;

import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.dao.api.ontologycommon.EvidenceOntologyDAO.ECOTermTO;
import org.bgee.model.dao.mysql.connector.MySQLDAOManager;
import org.bgee.pipeline.MySQLDAOUser;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLOntology;

import owltools.graph.OWLGraphWrapper;

/**
 * Class responsible for inserting the ECO ontology into the database.
 * 
 * @author Frederic Bastian
 * @version Bgee 13 Apr. 2015
 * @since Bgee 13
 */
public class InsertECO extends MySQLDAOUser {
    
    private final static Logger log = LogManager.getLogger(InsertECO.class.getName());
    
    /**
     * A {@code String} that is the OBO-like ID of the 'evidence' branch 
     * of the Evidence ontology.
     */
    private static final String EVIDENCE_ID = "ECO:0000000";
    
    /**
     * Default constructor. 
     */
    public InsertECO() {
        this(null);
    }
    /**
     * Constructor providing the {@code MySQLDAOManager} that will be used by 
     * this object to perform queries to the database. This is useful for unit testing.
     * 
     * @param manager   The {@code MySQLDAOManager} to use.
     */
    public InsertECO(MySQLDAOManager manager) {
        super(manager);
    }
    
    /**
     * Insert the provided ECO ontology into the database.
     * 
     * @param ecoOnt    An {@code OWLOntology} that is the ECO ontology.
     * @throws IllegalArgumentException If the provided ECO ontology was invalid and 
     *                                  did not allow to retrieve some required information.
     */
    public void insert(OWLOntology ecoOnt) {
        log.entry(ecoOnt);
        
        try {
            log.info("Start inserting ECO into database...");
            OWLGraphWrapper cioWrapper = new OWLGraphWrapper(ecoOnt);
            this.startTransaction();
            
            Set<ECOTermTO> tos = this.getECOTOs(cioWrapper);
            this.getEvidenceOntologyDAO().insertECOTerms(tos);
            
            this.commit();
            log.info("Done inserting ECO into database, inserted {} terms.", tos.size());
        } finally {
            this.closeDAO();
        }
        
        log.exit();
    }
    
    /**
     * Transform classes in the ontology wrapped in {@code ecoWrapper} into {@code ECOTermTO}s.
     * Deprecated classes are discarded.
     * 
     * @param ecoWrapper    A {@code OWLGraphWrapper} wrapping the ECO Ontology.
     * @return              A {@code Set} of {@code ECOTermTO}s corresponding to 
     *                      valid terms in the ontology wrapped by {@code ecoWrapper}.
     * @throws IllegalArgumentException If the provided ECO ontology was invalid and 
     *                                  did not allow to retrieve some required information.
     */
    private Set<ECOTermTO> getECOTOs(OWLGraphWrapper ecoWrapper) 
            throws IllegalArgumentException {
        log.entry(ecoWrapper);
        
        Set<ECOTermTO> ecoTOs = new HashSet<ECOTermTO>();
        OWLClass evidenceCls = ecoWrapper.getOWLClassByIdentifier(EVIDENCE_ID, true);
        if (evidenceCls == null) {
            throw log.throwing(new IllegalArgumentException("The provided ontology "
                    + "does not contain any 'evidence' term with ID " + EVIDENCE_ID));
        }
        
        for (OWLClass cls: ecoWrapper.getAllOWLClasses()) {
            log.trace("Examining class {}", cls);
            //we retrieve only "evidence" terms, not "assertion method" terms.
            //we also discard obsolete terms.
            if (ecoWrapper.isObsolete(cls) || ecoWrapper.getIsObsolete(cls) || 
                    evidenceCls.equals(cls) || 
                    !ecoWrapper.getAncestorsThroughIsA(cls).contains(evidenceCls)) {
                log.trace("Invalid class, discarded.");
                continue;
            }
            
            //retrieve information for insertion
            String id = ecoWrapper.getIdentifier(cls);
            String label = ecoWrapper.getLabel(cls);
            String description = ecoWrapper.getDef(cls);
            if (StringUtils.isBlank(id) || StringUtils.isBlank(label)) {
                throw log.throwing(new IllegalArgumentException("Missing ID or label "
                        + "for a class in the provided ontology, "
                        + "offending class: " + cls));
            }
            ECOTermTO newTO = new ECOTermTO(id, label, description);
            log.trace("Generating TO: {}", newTO);
            ecoTOs.add(newTO);
        }
        
        if (ecoTOs.isEmpty()) {
            throw log.throwing(new IllegalArgumentException("The provided ontology "
                    + "did not allow to retrieve any valid ECO terms."));
        }
        return log.exit(ecoTOs);
    }
}

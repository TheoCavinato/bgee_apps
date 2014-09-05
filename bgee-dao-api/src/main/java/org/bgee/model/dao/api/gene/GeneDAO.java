package org.bgee.model.dao.api.gene;

import java.util.Collection;

import org.bgee.model.dao.api.DAO;
import org.bgee.model.dao.api.DAOResultSet;
import org.bgee.model.dao.api.EntityTO;
import org.bgee.model.dao.api.exception.DAOException;

/**
 * DAO defining queries using or retrieving {@link GeneTO}s. 
 * 
 * @author Valentine Rech de Laval
 * @version Bgee 13
 * @see GeneTO
 * @since Bgee 13
 */
public interface GeneDAO extends DAO<GeneDAO.Attribute> {
    /**
     * {@code Enum} used to define the attributes to populate in the {@code GeneTO}s 
     * obtained from this {@code GeneDAO}.
     * <ul>
     * <li>{@code ID}: corresponds to {@link GeneTO#getId()}.
     * <li>{@code NAME}: corresponds to {@link GeneTO#getName()}.
     * <li>{@code DOMAIN}: corresponds to {@link GeneTO#getDomain()}.
     * <li>{@code SPECIESID}: corresponds to {@link GeneTO#getSpeciesId()}.
     * <li>{@code GENEBIOTYPEID}: corresponds to {@link GeneTO#getGeneBioTypeId()}.
     * <li>{@code OMAPARENTNODEID}: corresponds to {@link GeneTO#getOMANodeId()}.
     * <li>{@code ENSEMBLGENE}: corresponds to {@link GeneTO#isEnsemblGene()}.
     * </ul>
     * @see org.bgee.model.dao.api.DAO#setAttributes(Collection)
     * @see org.bgee.model.dao.api.DAO#setAttributes(Enum[])
     * @see org.bgee.model.dao.api.DAO#clearAttributes()
     */
    public enum Attribute implements DAO.Attribute {
        ID, NAME, DESCRIPTION, SPECIESID, GENEBIOTYPEID, OMAPARENTNODEID, ENSEMBLGENE;
    }
    
    /**
     * Retrieve all genes from data source.
     * <p>
     * The genes are retrieved and returned as a {@code GeneTOResultSet}. It is the responsibility 
     * of the caller to close this {@code DAOResultSet} once results are retrieved.
     * 
     * @return              A {@code GeneTOResultSet} containing all genes from data source.
     * @throws DAOException If an error occurred when accessing the data source. 
     */
    public GeneTOResultSet getAllGenes();
    
    /**
     * Update {@code Attribute}s of the provided genes, which are represented as a 
     * {@code Collection} of {@code GeneTO}s
     * 
     * @param genes                 A {@code Collection} of {@code GeneTO}s that are genes
     *                              to be updated into the data source.
     * @param attributesToUpdate    A {@code Collection} of {@code Attribute}s that are 
     *                              attributes to be updated into the data source.
     * @return                      An {@code int} representing the number of genes updated.
     */
    public int updateGenes(Collection<GeneTO> genes, Collection<GeneDAO.Attribute> attributesToUpdate);
    
    /**
     * {@code DAOResultSet} specifics to {@code GeneTO}s
     * 
     * @author Valentine Rech de Laval
     * @version Bgee 13
     * @since Bgee 13
     */
    public interface GeneTOResultSet extends DAOResultSet<GeneTO> {
        
    }
    
    /**
     * {@code EntityTO} representing a gene in the Bgee database.
     * 
     * @author Valentine Rech de Laval
     * @version Bgee 13
     * @since Bgee 13
     */
    public class GeneTO extends EntityTO {

        private static final long serialVersionUID = -9011956802137411474L;

        /**
         * An {@code int} that is the species ID of the gene.
         */
        private final int speciesId;
        
        
        /**
         * An {@code int} that is the gene type ID (for instance, the ID for protein_coding).
         */
        private final int geneBioTypeId;
       
        /**
         * An {@code int} that is unique ID for each node inside an OMA Hierarchical Orthologous 
         * Group. It can be {@code null} if the gene does not belong to a hierarchical group. A gene 
         * can belong to one and only one group.
         */
        private final int OMAParentNodeId;
        
        /**
         * A {@code boolean} defining whether this gene is present in Ensembl. For some species, 
         * they are not (for instance, we generate our own custom IDs for some species)
         */
        private final boolean ensemblGene;

        /**
         * Constructor providing the ID (for instance, {@code Ensembl:ENSMUSG00000038253}), 
         * the name (for instance, {@code Hoxa5}), the description and the species ID of this gene.
         * <p>
         * Other attributes are set to default value: the gene description is set to {@code null}, 
         * the BioType and the ID of the OMA Hierarchical Orthologous Group of this gene are set 
         * to 0, and the {code boolean} defining whether this gene is present in Ensembl is set 
         * to {@code true}
         * 
         * @param geneId    A {@code String} that is the ID of this gene.
         * @param geneName  A {@code String} that is the name of this gene.
         * @param speciesId An {@code int} of the species which this gene belongs to.
         */
        public GeneTO(String geneId, String geneName, Integer speciesId) {
            this(geneId, geneName, null, speciesId, 0, 0, true);
        }

        /**
         * Constructor providing the ID (for instance, {@code Ensembl:ENSMUSG00000038253}), 
         * the name (for instance, {@code Hoxa5}), the description, the species ID, the BioType, 
         * the ID of the OMA Hierarchical Orthologous Group, whether this gene is present in 
         * Ensembl (see {@link #isEnsemblGene()}).  
         * 
         * @param geneId            A {@code String} that is the ID of this gene.
         * @param geneName          A {@code String} that is the name of this gene.
         * @param geneDescription   A {@code String} that is the description of this gene.
         * @param speciesId         An {@code int} that is the species ID which this 
         *                          gene belongs to.
         * @param geneBioTypeId     An {@code int} that is the BioType of this gene.
         * @param OMAParentNodeId   An {@code int} that is the ID of the OMA Hierarchical 
         *                          Orthologous Group.
         * @param ensemblGene       A {code boolean} defining whether this gene is present 
         *                          in Ensembl. 
         */
        public GeneTO(String geneId, String geneName, String geneDescription, int speciesId,
                int geneBioTypeId, int OMAParentNodeId, boolean ensemblGene) {
            super(geneId, geneName, geneDescription);
            if (speciesId < 0 || geneBioTypeId < 0 || OMAParentNodeId < 0) {
                throw new IllegalArgumentException("Integer parameters must be positive.");
            }

            this.speciesId = speciesId;
            this.geneBioTypeId = geneBioTypeId;
            this.OMAParentNodeId = OMAParentNodeId;
            this.ensemblGene = ensemblGene;
        }

        /**
         * @return  The {@code String} that is the name of this gene (for instance, "Hoxa5").
         *          Corresponds to the DAO {@code Attribute} {@link GeneDAO.Attribute 
         *          NAME}. Returns {@code null} if value not set.
         */
        @Override
        public String getName() {
            //method overridden only to provide a more accurate javadoc
            return super.getName();
        }


        /**
         * @return  The species ID.
         */
        public int getSpeciesId() {
            return this.speciesId;
        }

        /**
         * @return The gene bio type ID (for instance, the ID for protein_coding).
         */
        public int getGeneBioTypeId() {
            return this.geneBioTypeId;
        }
        
        /**
         * @return  The OMA Hierarchical Orthologous Group ID that this gene belongs to.
         */
        public int getOMAParentNodeId() {
            return this.OMAParentNodeId;
        }
        
        /**
         * @return  The {@code boolean} defining whether this gene is present in Ensembl.
         */
        public boolean isEnsemblGene() {
            return this.ensemblGene;
        }

        @Override
        public String toString() {
            return "ID: " + this.getId() + " - Label: " + this.getName() + 
                   " - Species ID: " + this.getSpeciesId() + 
                   " - Gene bio type ID: " + this.getGeneBioTypeId() + 
                   " - OMA Hierarchical Orthologous Group ID: " + this.getOMAParentNodeId() + 
                   " - Is Ensembl Gene: " + this.isEnsemblGene();
        }
    }
}

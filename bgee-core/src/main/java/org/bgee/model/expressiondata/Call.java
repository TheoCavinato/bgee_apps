package org.bgee.model.expressiondata;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.clustering.Cluster;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.commons.math3.ml.clustering.DBSCANClusterer;
import org.apache.commons.math3.ml.distance.CanberraDistance;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bgee.model.expressiondata.Call.ExpressionCall;
import org.bgee.model.expressiondata.CallData.DiffExpressionCallData;
import org.bgee.model.expressiondata.CallData.ExpressionCallData;
import org.bgee.model.expressiondata.baseelements.DataQuality;
import org.bgee.model.expressiondata.baseelements.DiffExpressionFactor;
import org.bgee.model.expressiondata.baseelements.SummaryCallType;
import org.bgee.model.expressiondata.baseelements.SummaryCallType.DiffExpressionSummary;
import org.bgee.model.expressiondata.baseelements.SummaryCallType.ExpressionSummary;
import org.bgee.model.expressiondata.baseelements.SummaryQuality;
import org.bgee.model.gene.Gene;

/**
 * This class describes the calls related to gene baseline expression and differential expression.
 * 
 * @author  Frederic Bastian
 * @author  Valentine Rech de Laval
 * @version Bgee 14, Feb. 2017
 * @since   Bgee 13, Sept. 2015
 * @param <T> The type of {@code SummaryCallType}.
 * @param <U> The type of {@code CallData}.
 */
//XXX: and what if it was a multi-species query? Should we use something like a MultiSpeciesCondition?
//TODO: move inner classes to different files
public abstract class Call<T extends Enum<T> & SummaryCallType, U extends CallData<?>> {
    private final static Logger log = LogManager.getLogger(Call.class.getName());

    //**********************************************
    //   INNER CLASSES
    //**********************************************
    public static class ExpressionCall extends Call<ExpressionSummary, ExpressionCallData> {

        //**********************************************************
        //   INNER CLASSES, STATIC ATTRIBUTES AND METHODS, 
        //   RELATED TO CLUSTERING BASED ON EXPRESSION RANK SCORE 
        //**********************************************************
        /**
         * Allows to specify the method to use to cluster {@code ExpressionCall}s based on 
         * their global mean rank score. Recommended clustering method as of Bgee 13: 
         * {@code BGEE_DIST_TO_MAX}.
         * <ul>
         * <li>{@code CANBERRA_DIST_TO_MAX}: define clusters so that each member is connected 
         * to another member by a distance below the threshold, using Canberra distance. 
         * This allows to identify "shifts" or "jumps" of rank scores between {@code ExpressionCall}s. 
         * Recommended threshold as of Bgee 13: around 0.19. 
         * <li>{@code BGEE_DIST_TO_MAX}: same as {@code CANBERRA_DIST_TO_MAX}, 
         * but the distance metric used is a Bgee metric, see {@link ExpressionCall.BgeeRankDistance}.
         * Recommended threshold as of Bgee 13: around 1.9. 
         * <li>{@code FIXED_CANBERRA_DIST_TO_MAX}: same as {@code CANBERRA_DIST_TO_MAX}, but each member 
         * is connected by a fixed score distance rather than by a Canberra distance threshold.  
         * The fixed score distance is the difference between the minimum score of the cluster, 
         * and a score defined so that their Canberra distance is equal to the threshold.
         * This avoids to have increasing differences of score corresponding to the same 
         * Canberra distance inside a given cluster. 
         * Recommended threshold as of Bgee 13: around 0.19. 
         * <li>{@code CANBERRA_DBSCAN}: perform a DBScan analysis, using Canberra distance 
         * as distance score, and 1 as the minimum cluster size. The distance threshold 
         * will correspond to the allowed "radius" of clusters. In practice, there is no difference with
         * {@code CANBERRA_DIST_TO_MAX}, as we define outliers as being part of their own cluster. 
         * Recommended threshold as of Bgee 13: around 0.19. 
         * <li>{@code CANBERRA_DIST_TO_MEAN}: define clusters so that the distance between each member, 
         * and the mean of the cluster, is below the distance threshold, using Canberra distance. 
         * Recommended threshold as of Bgee 13: around 0.18. 
         * <li>{@code CANBERRA_DIST_TO_MEDIAN}: define clusters so that the distance between each member, 
         * and the median of the cluster, is below the distance threshold, using Canberra distance. 
         * Recommended threshold as of Bgee 13: around 0.18. 
         * <li>{@code CANBERRA_DIST_TO_MIN}: define clusters so that the distances between all members 
         * are all below the distance threshold, using Canberra distance. 
         * Recommended threshold as of Bgee 13: around 0.2. 
         * </ul>
         * 
         * @author Frederic Bastian
         * @version Bgee 13 June 2016
         * @see #generateMeanRankScoreClustering(Collection, ClusteringMethod, double)
         * @since Bgee 13 June 2016
         */
        public static enum ClusteringMethod {
            FIXED_CANBERRA_DIST_TO_MAX(false), CANBERRA_DIST_TO_MAX(false), CANBERRA_DBSCAN(false), 
            CANBERRA_DIST_TO_MEAN(false), CANBERRA_DIST_TO_MEDIAN(false), CANBERRA_DIST_TO_MIN(false), 
            BGEE_DIST_TO_MAX(true);
            /**
             * @see #isDistanceMeasureAboveOne()
             */
            private final boolean distanceMeasureAboveOne;
            /**
             * @param distanceMeasureAboveOne   See {@link #isDistanceMeasureAboveOne()}.
             */
            private ClusteringMethod(boolean distanceMeasureAboveOne) {
                this.distanceMeasureAboveOne = distanceMeasureAboveOne;
            }
            /**
             * @return  A {@code boolean} defining whether the distance measure 
             *          used by this clustering method ranges from 0 to 1 (if {@code false}), 
             *          or if it is always greater than or equal to 1 (if {@code true}).
             */
            public boolean isDistanceMeasureAboveOne() {
                return distanceMeasureAboveOne;
            }
        }
        /**
         * Used internally to specify the reference score to use when performing 
         * a distance-based clustering. 
         * 
         * @see ClusteringMethod
         * @see #generateDistBasedClustering(Collection, double, DistanceMeasure, DistanceReference)
         */
        private static enum DistanceReference {
            MEAN, MEDIAN, MIN, MAX;
        }
        
        /**
         * A {@code DistanceMeasure} specific to Bgee for clustering {@code ExpressionCall}s 
         * based on their mean global ranks. The provided {@code Array}s can only have a dimension of 1, 
         * otherwise a {@code DimensionMismatchException} will be thrown. Values in the {@code Array}s 
         * can only be stricly positive, otherwise an {@code IllegalArgumentException} will be thrown.
         * <p>
         * Distance measure: {@code (max(score1, score2)^1.03)/min(score1, score2)}.
         * 
         * @author Frederic Bastian
         * @version Bgee 13 June 2016
         * @since Bgee 13 June 2016
         */
        protected static class BgeeRankDistance implements DistanceMeasure {
            private static final long serialVersionUID = -1963975219509338786L;

            @Override
            public double compute(double[] a, double[] b) 
                    throws DimensionMismatchException, IllegalArgumentException {
                if (a.length != 1) {
                    throw log.throwing(new DimensionMismatchException(a.length, 1));
                }
                if (b.length != 1) {
                    throw log.throwing(new DimensionMismatchException(b.length, 1));
                }
                if (a[0] <= 0.000001 || b[0] <= 0.0000001) {
                    throw log.throwing(new IllegalArgumentException("This distance measure "
                            + "doesn't manage negative nor near 0 values"));
                }
                double max = b[0];
                double min = a[0];
                if (a[0] > b[0]) {
                    max = a[0];
                    min = b[0];
                }
                return log.exit(Math.pow(max, 1.03)/min);
            }
        }
        
        /**
         * A {@code Comparator} of {@code ExpressionCall}s, performing the following comparisons 
         * in order: 
         * <ol>
         * <li>comparison based on {@link ExpressionCall#getGlobalMeanRank()}
         * <li>in case of equality, comparison based on {@link ExpressionCall#getGeneId()}
         * <li>in case of equality, and if a {@code ConditionUtils} was provided at instantiation, 
         * comparison based on the relations between {@code Condition}s (see 
         * {@link ExpressionCall#getCondition()} and {@link ConditionUtils#compare(Condition, Condition)})
         * <li>in case of equality, comparison based on the attributes of {@code Condition}s 
         * (see {@link ExpressionCall#getCondition()} and {@link Condition#compareTo(Condition)}.
         * </ol>
         * 
         * @author Frederic Bastian
         * @version Bgee 13 June 2016
         * @since Bgee 13 June 2016
         *
         */
        public static class RankComparator implements Comparator<ExpressionCall> {
            /**
             * The {@code Comparator} to use in method {@link #compare(ExpressionCall, ExpressionCall)}.
             */
            private final Comparator<ExpressionCall> rankComparator;
            /**
             * The {@code ConditionUtils} used by the {@link #rankComparator}. Can be {@code null}.
             */
            private final ConditionUtils conditionUtils;
            
            /**
             * Instantiate a {@code RankComparator} not using any {@code ConditionUtils}.
             */
            public RankComparator() {
                this(null);
            }
            /**
             * Instantiate a {@code RankComparator} that can take into account 
             * relations between {@code Condition}s, thanks to a {@code ConditionUtils}.
             * 
             * @param condUtils A {@code ConditionUtils} used to sort {@code Condition}s based on 
             *                  their relations between each other, in case of equal ranks 
             *                  and equal gene IDs. Can be {@code null} if it is not needed 
             *                  to do such a sorting based on relations.
             */
            public RankComparator(ConditionUtils condUtils) {
                Comparator<ExpressionCall> tmpComparator = Comparator
                        //Order first by global mean rank
                        .comparing(ExpressionCall::getGlobalMeanRank, 
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        //important in case of score equality: 
                        //Order by genes
                        .thenComparing(c -> c.getGene() == null? null : c.getGene().getEnsemblGeneId(), 
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        //Order by species as, in bgee 14, gene IDs are not unique
                        .thenComparing(c -> c.getGene() == null? null : c.getGene().getSpeciesId(), 
                    Comparator.nullsLast(Comparator.naturalOrder()));
                if (condUtils != null) {
                    tmpComparator = tmpComparator
                            //Then, we want the most precise conditions first (for the method 
                            //identifyRedundantCalls, and also for better display)
                            .thenComparing(ExpressionCall::getCondition, 
                                    Comparator.nullsLast(condUtils::compare));
                }
                this.rankComparator = tmpComparator
                        //If everything else fails, simply order by the attributes of the Condition
                        .thenComparing(ExpressionCall::getCondition, 
                                Comparator.nullsLast(Comparator.naturalOrder()));
                this.conditionUtils = condUtils;
            }
            @Override
            public int compare(ExpressionCall call1, ExpressionCall call2) {
                log.entry(call1, call2);
                return log.exit(this.rankComparator.compare(call1, call2));
            }
            /**
             * @return  The {@code ConditionUtils} used by this {@code Comparator}. Can be {@code null} 
             *          if none was used. 
             */
            public ConditionUtils getConditionUtils() {
                return conditionUtils;
            }
        }
        
        /**
         * A {@code ClusteringMethod} that is the default recommended method for clustering 
         * {@code ExpressionCall}s based on their global mean rank. The default distance threshold 
         * to use is provided by {@link #DEFAULT_DISTANCE_THRESHOLD}.
         * 
         * @see #DEFAULT_DISTANCE_THRESHOLD
         * @see #generateMeanRankScoreClustering(Collection, ClusteringMethod, double)
         */
        public static final ClusteringMethod DEFAULT_CLUSTERING_METHOD = ClusteringMethod.BGEE_DIST_TO_MAX;
        /**
         * A {@code double} that is the default recommended distance threshold to apply 
         * to the clustering method defined by {@link #DEFAULT_CLUSTERING_METHOD}, 
         * for clustering {@code ExpressionCall}s based on their global mean rank.
         * 
         * @see #DEFAULT_CLUSTERING_METHOD
         * @see #generateMeanRankScoreClustering(Collection, ClusteringMethod, double)
         */
        public static final double DEFAULT_DISTANCE_THRESHOLD = 1.9;
        
        /**
         * Remove equal calls from the {@code Collection} and order them using 
         * {@link ExpressionCall.RankComparator}.
         * 
         * @param calls     A {@code Collection} of {@code ExpressionCall}s to filter 
         *                  for redundant calls and to order based on their global mean rank.
         * @param condUtils A {@code ConditionUtils} used to sort {@code Condition}s based on 
         *                  their relations between each other, in case of equal ranks 
         *                  and equal gene IDs. Can be {@code null} if it is not needed 
         *                  to do such a sorting based on relations.
         * @return          A {@code List} of {@code ExpressionCall}s filtered and ordered. 
         *                  {@code null} if {@code calls} was {@code null}.
         * @see ExpressionCall.RankComparator
         */
        private static List<ExpressionCall> filterAndOrderByGlobalMeanRank(
                Collection<ExpressionCall> calls, ConditionUtils conditionUtils) {
            log.entry(calls, conditionUtils);
            if (calls == null) {
                return log.exit(null);
            }
            List<ExpressionCall> sortedCalls = new ArrayList<>(new HashSet<ExpressionCall>(calls));
            long startFilteringTimeInMs = System.currentTimeMillis();
            Collections.sort(sortedCalls, new RankComparator(conditionUtils));
            log.debug("Calls sorted in {} ms", System.currentTimeMillis() - startFilteringTimeInMs);
            return log.exit(sortedCalls);
        }
        
        /**
         * Identifies redundant {@code ExpressionCall}s from the provided {@code Collection}. 
         * This method returns {@code ExpressionCall}s for which there exists a more precise call 
         * (i.e., with a more precise condition), at a better or equal rank (see 
         * {@link #getGlobalMeanRank()}). {@code calls} can contain {@code ExpressionCall}s 
         * for different genes. 
         * 
         * @param calls            A {@code Collection} of {@code ExpressionCall}s to filter. 
         *                         If the information of global mean rank or of Condition is missing, 
         *                         an {@code IllegalArgumentException} is thrown. 
         * @param conditionUtils   A {@code ConditionUtils}, containing all the {@code Condition}s 
         *                         related to {@code calls}. Otherwise, an {@code IllegalArgumentException} 
         *                         is thrown. 
         * @return                 A {@code Set} containing the {@code ExpressionCall}s that are redundant.
         * @throws IllegalArgumentException If the {@code Condition} of a provided {@code ExpressionCall} 
         *                                  could not be found in the provided {@code ConditionUtils}, 
         *                                  or if an information of global mean rank or of Condition 
         *                                  was missing in an {@code ExpressionCall}.
         * @see #identifyRedundantCalls(List, ConditionUtils)
         * @see ConditionUtils#isConditionMorePrecise(Condition, Condition)
         * @see ConditionUtils#getDescendantConditions(Condition)
         */
        public static Set<ExpressionCall> identifyRedundantCalls(Collection<ExpressionCall> calls, 
                ConditionUtils conditionUtils) throws IllegalArgumentException {
            log.entry(calls, conditionUtils);
            
            //for the computations, we absolutely need to order the calls using RankComparator
            return log.exit(identifyRedundantCalls(filterAndOrderByGlobalMeanRank(calls, conditionUtils), 
                    conditionUtils));
            
        }
        
        /**
         * Identifies redundant {@code ExpressionCall}s using an already sorted {@code List}. 
         * This method performs exactly the same operation as {@link #identifyRedundantCalls(
         * Collection, ConditionUtils)}, but is provided for performance issue: several methods, 
         * in this class and outside, absolutely need to use a {@code List} of {@code ExpressionCall}s 
         * sorted by the {@code RankComparator} using a {@code ConditionUtils}, which can be costly 
         * for considering relations between {@code Condition}s; it is then possible 
         * to sort a {@code List} of {@code ExpressionCall}s outside of this method, to reuse it 
         * for different method calls.
         * 
         * @param calls            A {@code List} of {@code ExpressionCall}s to filter, sorted using 
         *                         the {@code RankComparator}. 
         * @param conditionUtils   A {@code ConditionUtils}, containing all the {@code Condition}s 
         *                         related to {@code calls}. Otherwise, an {@code IllegalArgumentException} 
         *                         is thrown. 
         * @return                 A {@code Set} containing the {@code ExpressionCall}s that are redundant.
         * @throws IllegalArgumentException If the {@code Condition} of a provided {@code ExpressionCall} 
         *                                  could not be found in the provided {@code ConditionUtils}, 
         *                                  or if an information of global mean rank or of Condition 
         *                                  was missing in an {@code ExpressionCall}, or if the list 
         *                                  was not sorted at least based on ranks.
         * @see ExpressionCall.RankComparator
         * @see ConditionUtils#isConditionMorePrecise(Condition, Condition)
         * @see ConditionUtils#getDescendantConditions(Condition)
         */
        public static Set<ExpressionCall> identifyRedundantCalls(List<ExpressionCall> calls, 
                ConditionUtils conditionUtils) throws IllegalArgumentException {
            log.entry(calls, conditionUtils);
        
            long startFilteringTimeInMs = System.currentTimeMillis();
            
            Set<ExpressionCall> redundantCalls = new HashSet<>();
            Set<ExpressionCall> validatedCalls = new HashSet<>();
            ExpressionCall previousCall = null;
            for (ExpressionCall call: calls) {
                //We cannot make sure that the List was ordered using a RankComparator 
                //with a ConditionUtils, it would be too costly, but we perform a minimal check 
                //on ranks and conditions
                if (call.getGlobalMeanRank() == null) {
                    throw log.throwing(new IllegalArgumentException("Missing rank for call: "
                            + call));
                }
                if (call.getCondition() == null) {
                    throw log.throwing(new IllegalArgumentException("Missing Condition for call: "
                            + call));
                }
                if (previousCall != null && 
                        previousCall.getGlobalMeanRank().compareTo(call.getGlobalMeanRank()) > 0) {
                    throw log.throwing(new IllegalArgumentException("Provided List incorrectly sorted"));
                }
                
                //Retrieve the validated conditions for the currently iterated gene
                Set<Condition> validatedCondition = validatedCalls.stream()
                        .filter(c -> Objects.equals(
                            c.getGene() == null ? null: c.getGene().getEnsemblGeneId(),
                            call.getGene() == null ? null: call.getGene().getEnsemblGeneId()))
                        //Filter by species as, in bgee 14, gene IDs are not unique
                        .filter(c -> Objects.equals(
                            c.getGene() == null ? null: c.getGene().getSpeciesId(),
                            call.getGene() == null ? null: call.getGene().getSpeciesId()))
                        .map(ExpressionCall::getCondition)
                        .collect(Collectors.toSet());
                //check whether any of the validated Condition is a descendant 
                //of the Condition of the iterated call
                if (validatedCondition.isEmpty() || Collections.disjoint(validatedCondition, 
                        conditionUtils.getDescendantConditions(call.getCondition()))) {
                    
                    log.trace("Valid call: {}", call);
                    validatedCalls.add(call);
                } else {
                    log.trace("Redundant call: {}", call);
                    redundantCalls.add(call);
                }
                previousCall = call;
            }
            
            log.debug("Redundant calls filtered in {} ms", System.currentTimeMillis() - startFilteringTimeInMs);
            return log.exit(redundantCalls);
        }
        
        /**
         * Generate a clustering of {@code ExpressionCall}s based on their global mean rank 
         * (see {@link #getGlobalMeanRank()}). The {@code ExpressionCall}s of only one gene 
         * can be clustered at a time, otherwise an {@code IllegalArgumentException} is thrown.
         * 
         * @param calls             A {@code Collection} of {@code ExpressionCall}s of one gene 
         *                          with global mean ranks defined.
         * @param method            The {@code ClusteringMethod} to use for clustering. 
         * @param distanceThreshold A {@code double} that is the distance threshold applied to 
         *                          the {@code ClusteringMethod}. 
         * @return      A {@code Map} where keys are {@code ExpressionCall}s, the associated value 
         *              being the index of the group in which they are clustered, 
         *              based on their expression score. Group indexes are assigned in ascending 
         *              order of expression score, starting from 0.
         * @throws IllegalArgumentException If {@code calls} represents the expression of several genes, 
         *                                  of if the global mean ranks are not defined. 
         */
        public static Map<ExpressionCall, Integer> generateMeanRankScoreClustering(
                Collection<ExpressionCall> calls, ClusteringMethod method, double distanceThreshold) 
                        throws IllegalArgumentException {
            log.entry(calls, method, distanceThreshold);

            //for the computations, we need a List sorted by rank, but we don't need to take 
            //relations between Conditions into account
            return log.exit(generateMeanRankScoreClustering(filterAndOrderByGlobalMeanRank(calls, null), 
                    method, distanceThreshold));
            
        }

        /**
         * Generate a clustering of {@code ExpressionCall}s using a {@code List}. 
         * This method performs exactly the same operation as {@link #generateMeanRankScoreClustering(
         * Collection, ClusteringMethod, double)}, but is provided for performance issue: 
         * some clustering methods need to sort the {@code ExpressionCall}s based on their rank, 
         * and several methods, in this class or outside, absolutely need to use a {@code List} 
         * of {@code ExpressionCall}s sorted by the {@code RankComparator}, 
         * which can be costly when relations between {@code Condition}s need to be considered; 
         * because the clustering needs to be consistent with such lists, it is then possible 
         * to sort a {@code List} of {@code ExpressionCall}s outside of this method, to reuse it 
         * for different method calls.
         * 
         * @param calls             A {@code List} of {@code ExpressionCall}s of one gene 
         *                          sorted at least by their global mean ranks.
         * @param method            The {@code ClusteringMethod} to use for clustering. 
         * @param distanceThreshold A {@code double} that is the distance threshold applied to 
         *                          the {@code ClusteringMethod}. 
         * @return      A {@code Map} where keys are {@code ExpressionCall}s, the associated value 
         *              being the index of the group in which they are clustered, 
         *              based on their expression score. Group indexes are assigned in ascending 
         *              order of expression score, starting from 0.
         * @throws IllegalArgumentException If {@code calls} is not correctly sorted, 
         *                                  or represents the expression of several genes. 
         */
        public static Map<ExpressionCall, Integer> generateMeanRankScoreClustering(
                List<ExpressionCall> calls, ClusteringMethod method, double distanceThreshold) 
                        throws IllegalArgumentException {
            log.entry(calls, method, distanceThreshold);
            long startFilteringTimeInMs = System.currentTimeMillis();
            
            //sanity check
            ExpressionCall previousCall = null;
            for (ExpressionCall call: calls) {
                if (previousCall != null) { 
                    if (previousCall.getGlobalMeanRank().compareTo(call.getGlobalMeanRank()) > 0) {
                        throw log.throwing(new IllegalArgumentException(
                                "Provided List incorrectly sorted"));
                    }
                    if (!Objects.equals(
                            previousCall.getGene() == null ? null: previousCall.getGene().getEnsemblGeneId(),
                            call.getGene() == null ? null: call.getGene().getEnsemblGeneId())) {
                        throw log.throwing(new IllegalArgumentException(
                                "A clustering can only be performed one gene at a time"));
                    }
                }
            }
            
            Map<ExpressionCall, Integer> clustering = null;
            switch(method) {
            case CANBERRA_DBSCAN: 
                clustering = generateDBScanClustering(calls, distanceThreshold, 1, 
                        new CanberraDistance());
                break;
            case CANBERRA_DIST_TO_MEAN: 
                clustering = generateDistBasedClustering(calls, distanceThreshold, 
                        new CanberraDistance(), DistanceReference.MEAN);
                break;
            case CANBERRA_DIST_TO_MEDIAN: 
                clustering = generateDistBasedClustering(calls, distanceThreshold, 
                        new CanberraDistance(), DistanceReference.MEDIAN);
                break;
            case CANBERRA_DIST_TO_MIN: 
                clustering = generateDistBasedClustering(calls, distanceThreshold, 
                        new CanberraDistance(), DistanceReference.MIN);
                break;
            case CANBERRA_DIST_TO_MAX:
                clustering = generateDistBasedClustering(calls, distanceThreshold, 
                        new CanberraDistance(), DistanceReference.MAX);
                break;
            case FIXED_CANBERRA_DIST_TO_MAX:
                clustering = generateFixedCanberraDistToMaxClustering(calls, distanceThreshold);
                break;
            case BGEE_DIST_TO_MAX:
                clustering = generateDistBasedClustering(calls, distanceThreshold, 
                        new BgeeRankDistance(), DistanceReference.MAX);
                break;
            default: 
                throw log.throwing(new IllegalArgumentException("Unrecognized clustering method: " 
                        + method));
            }
            log.trace("Calls clustered in {} ms", System.currentTimeMillis() - startFilteringTimeInMs);
            assert clustering != null;
            return log.exit(clustering);
        }
        /**
         * Generate a clustering of {@code ExpressionCall}s based on their global mean rank 
         * using DBScan (see {@link #getGlobalMeanRank()} and <a href='https://commons.apache.org/proper/commons-math/apidocs/org/apache/commons/math3/ml/clustering/DBSCANClusterer.html'>
         * org.apache.commons.math3.ml.clustering.DBSCANClusterer</a>).
         * 
         * @param calls     A {@code List} of {@code ExpressionCall}s ordered by their global mean rank. 
         *                  DBScan doesn't use a sorted List, but we need one to "fill the gaps" 
         *                  caused by DBScan outliers.
         * @param epislon   A {@code double} that is the distance radius to use for DBScan.
         * @param minSize   An {@code int} that is the minimum size of a resulting cluster.
         * @param measure   A {@code DistanceMeasure} to compute distances. 
         * @return          A {@code Map} where keys are {@code ExpressionCall}s, the associated value 
         *                  being the index of the group in which they are clustered, 
         *                  based on their expression score. Group indexes are assigned in ascending 
         *                  order of expression score, starting from 0.
         */
        private static Map<ExpressionCall, Integer> generateDBScanClustering(
                List<ExpressionCall> calls, double epislon, int minSize, DistanceMeasure measure) {
            log.entry(calls, epislon, minSize, measure);

            /**
             * A wrapper for {@code ExpressionCall} to implement the {@code Clusterable} interface.
             */
            final class ExpressionCallClusterable implements Clusterable {
                private final ExpressionCall refCall;
                
                private ExpressionCallClusterable(ExpressionCall call) {
                    this.refCall = call;
                }
                @Override
                public double[] getPoint() {
                    return new double[]{this.refCall.getGlobalMeanRank().doubleValue()};
                }
                public ExpressionCall getRefExpressionCall() {
                    return refCall;
                }
            }
            
            DBSCANClusterer<ExpressionCallClusterable> clusterer = 
                    new DBSCANClusterer<>(epislon, minSize, measure);
            List<Cluster<ExpressionCallClusterable>> clusters = clusterer.cluster(
                    calls.stream().map(ExpressionCallClusterable::new).collect(Collectors.toList()));
            
            //first, we extract a mapping ExpressionCall -> group index from DBScan clustering
            Map<ExpressionCall, Integer> tmpCallsToGroup = clusters.stream().flatMap(c -> {
                int groupIndex = clusters.indexOf(c);
                return c.getPoints().stream().map(p -> 
                    new AbstractMap.SimpleEntry<ExpressionCall, Integer>(
                        p.getRefExpressionCall(), groupIndex));
            }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            log.debug("Original DBScan clustering: {}", tmpCallsToGroup);
            
            //Now, we fill the ExpressionCalls missing from the clustering because of outliers.
            Map<ExpressionCall, Integer> callsToGroup = new HashMap<>();
            int computedGroupIndex = -1;
            Integer lastIndex = null;
            for (ExpressionCall call: calls) {
                Integer groupIndex = tmpCallsToGroup.get(call);
                //we create a new cluster for each outlier, or if we are really iterating a new group. 
                //So, in groupIndex is null, we are iterating an outlier, we create a cluster; 
                //if lastIndex is null, the last call was an outlier, so either we iterate 
                //another outlier, or a new group, in any case we create a new cluster; 
                //finally, if groupIndex != lastIndex, we are moving from a real cluster 
                //to another cluster.
                if (groupIndex == null || lastIndex == null || groupIndex != lastIndex) {
                    computedGroupIndex++;
                }
                callsToGroup.put(call, computedGroupIndex);
                lastIndex = groupIndex;
            }
            log.debug("Secondary DBScan clustering: {}", callsToGroup);
            
            return log.exit(callsToGroup);
        }
        /**
         * Generate a clustering of {@code ExpressionCall}s based on their global mean rank 
         * using distance from a reference score, defined depending on {@code ref} argument.
         * 
         * @param calls                 A {@code List} of {@code ExpressionCall}s 
         *                              ordered by their global mean rank.
         * @param distanceThreshold     A {@code double} that is the distance threshold to the reference 
         *                              score of a cluster to consider a call outside of the cluster.
         * @param measure               A {@code DistanceMeasure} to compute distances.
         * @param ref                   A {@code DistanceReference} specifying what is the reference score 
         *                              of a cluster: either it is the minimum score of the cluster 
         *                              (then the distance between all scores must be below the threshold), 
         *                              or the maximum score (then all scores are connected to at least 
         *                              one other score below the threshold), or the mean of the cluster 
         *                              (the distance to the mean of all scores is below the threshold), 
         *                              or the median (distance to the median of all scores below 
         *                              the threshold). 
         * @return          A {@code Map} where keys are {@code ExpressionCall}s, the associated value 
         *                  being the index of the group in which they are clustered, 
         *                  based on their expression score. Group indexes are assigned in ascending 
         *                  order of expression score, starting from 0.
         */
        private static Map<ExpressionCall, Integer> generateDistBasedClustering(
                List<ExpressionCall> calls, double distanceThreshold, 
                DistanceMeasure measure, DistanceReference ref) {
            log.entry(calls, distanceThreshold, measure, ref);
            
            Map<ExpressionCall, Integer> callsToGroup = new HashMap<>();
            int groupIndex = -1;
            List<ExpressionCall> groupMember = null;
            for (ExpressionCall call: calls) {
                log.trace("Iterating call for distance-based clustering: {}", call);
                boolean createGroup = false;

                //create a new group if first iteration
                if (groupMember == null) {
                    createGroup = true;
                } else {
                    double[] refScore = new double[1];
                    boolean compareToMin = false;
                    switch (ref) {
                    case MIN: 
                        refScore[0] = groupMember.get(0).getGlobalMeanRank().doubleValue();
                        break;
                    case MAX: 
                        refScore[0] = groupMember.get(groupMember.size() - 1).getGlobalMeanRank()
                                       .doubleValue();
                        break;
                    case MEAN: 
                        refScore[0] = (groupMember.stream()
                                            .mapToDouble(c -> c.getGlobalMeanRank().doubleValue()).sum() 
                                        + call.getGlobalMeanRank().doubleValue())
                                        /(groupMember.size() + 1);
                        compareToMin = true;
                        break;
                    case MEDIAN: 
                        List<ExpressionCall> nextGroupMember = new ArrayList<>(groupMember);
                        nextGroupMember.add(call);
                        refScore[0] = getMedianMeanRankScore(nextGroupMember);
                        compareToMin = true;
                        break;
                    default: 
                        throw log.throwing(new IllegalArgumentException("Unsupported reference: " + ref));
                    }
                    assert refScore[0] != 0;
                    
                    double distance = measure.compute(refScore, 
                            new double[]{call.getGlobalMeanRank().doubleValue()});
                    if (log.isTraceEnabled()) {
                        log.trace("Reference score: {} - current score: {} - Distance: {} - "
                                + "Distance threshold: {} - Compare to min: {} - "
                                + "Rank of first member: {}, Distance to ref: {}", 
                              refScore[0], call.getGlobalMeanRank().doubleValue(), 
                              distance, distanceThreshold, compareToMin, 
                              groupMember.get(0).getGlobalMeanRank().doubleValue(), 
                              measure.compute(refScore, 
                                  new double[]{groupMember.get(0).getGlobalMeanRank().doubleValue()}));
                    }
                    
                        //if the distance between the ref score 
                        //and the currently iterated score is over the threshold 
                    if (distance > distanceThreshold || 
                        //or, in case of distance to mean or median, if, by adding the currently 
                        //iterated score to the group, the distance between the ref. score 
                        //and the minimum score of the group will be over the threshold.
                        (compareToMin && measure.compute(refScore, 
                                    new double[]{groupMember.get(0).getGlobalMeanRank().doubleValue()}) 
                            > distanceThreshold)) {
                        createGroup = true;
                    }
                }
                
                if (createGroup) {
                    log.trace("Create new group");
                    groupIndex++;
                    groupMember = new ArrayList<>();
                }
                groupMember.add(call);
                callsToGroup.put(call, groupIndex);
                log.trace("Assign Call {} to group index {}", call, groupIndex);
            }
            
            return log.exit(callsToGroup);
        }
        
        /**
         * Generates clustering for {@link ClusteringMethod FIXED_CANBERRA_DIST_TO_MAX}.
         * 
         * @param calls                 A {@code List} of {@code ExpressionCall}s 
         *                              ordered by their global mean rank.
         * @param distanceThreshold     A {@code double} that is the distance threshold to the reference 
         *                              score of a cluster to consider a call outside of the cluster.
         * @return          A {@code Map} where keys are {@code ExpressionCall}s, the associated value 
         *                  being the index of the group in which they are clustered, 
         *                  based on their expression score. Group indexes are assigned in ascending 
         *                  order of expression score, starting from 0.
         */
        private static Map<ExpressionCall, Integer> generateFixedCanberraDistToMaxClustering(
                List<ExpressionCall> calls, double distanceThreshold) {
            log.entry(calls, distanceThreshold);
            
            Map<ExpressionCall, Integer> callsToGroup = new HashMap<>();
            int groupIndex = -1;
            double groupAllowedScoreDiff = 0;
            double previousScore = 0;
            for (ExpressionCall call: calls) {
                double currentScore = call.getGlobalMeanRank().doubleValue();

                // create a new group if first iteration, 
                // or if current score over the allowed score diff. 
                // 
                // Note: we could compute a new distance threshold for the max score of the cluster 
                // and the currently iterated score to be in the allowed score diff:
                // new_potential_max = cluster_max + allowed_score_diff
                // dist_threshold = (new_potential_max - cluster_max)/(new_potential_max + cluster_max)
                // dist_threshold = (allowed_score_diff)/(2 * cluster_max + allowed_score_diff)
                if (groupIndex == -1 || 
                        currentScore - previousScore - groupAllowedScoreDiff >= -0.000001) {
                    
                    groupIndex++;
                    // At cluster creation, we compute the allowed difference of score, 
                    // so that a score and the minimum score of the cluster are below 
                    // the distance threshold.
                    // Camberra distance: (score2 - score1)/(score2 + score1) = dist_threshold
                    // Score2 as a function of score1 and dist_threshold: 
                    // score2 = -score1(1 + dist_threshold)/(dist_threshold - 1)
                    // => allowed_score_diff = -score1 * ((1 + dist_threshold)/(dist_threshold - 1) + 1)
                    groupAllowedScoreDiff = -currentScore 
                            * ((1 + distanceThreshold)/(distanceThreshold - 1) + 1);
                } 
                callsToGroup.put(call, groupIndex);
                previousScore = currentScore;
            }
            
            return log.exit(callsToGroup);
        }
        
        /**
         * Get the median mean rank from {@code ExpressionCall}s ordered based on their rank. 
         * 
         * @param calls A {@code List} of {@code ExpressionCall}s ordered by their global mean rank 
         *              (see {@link #getGlobalMeanRank()}).
         * @return      A {@code double} that is the median mean rank. 
         */
        private static double getMedianMeanRankScore(List<ExpressionCall> calls) {
            log.entry(calls);
            int size = calls.size();
            if (size == 0) {
                throw log.throwing(new IllegalArgumentException("Can't compute mediam of empty list"));
            }
            if (size == 1) {
                return log.exit(calls.get(0).getGlobalMeanRank().doubleValue());
            }
            if (size % 2 == 0) {
                return log.exit((calls.get(size/2).getGlobalMeanRank().doubleValue() 
                           + calls.get(size/2 - 1).getGlobalMeanRank().doubleValue())
                           /2);
            } 
            return log.exit(calls.get((size - 1)/2).getGlobalMeanRank().doubleValue());
        }
        
        /**
         * A {@code NumberFormat} to format rank scores less than 10.
         */
        private static final NumberFormat FORMAT1 = getNumberFormat(1);
        /**
         * A {@code NumberFormat} to format rank scores less than 100.
         */
        private static final NumberFormat FORMAT10 = getNumberFormat(10);
        /**
         * A {@code NumberFormat} to format rank scores less than 1000.
         */
        private static final NumberFormat FORMAT100 = getNumberFormat(100);
        /**
         * A {@code NumberFormat} to format rank scores greater than or equal to 1000.
         */
        private static final NumberFormat FORMAT1000 = getNumberFormat(1000);
        /**
         * @param max   An {@code int} to retrieve a {@code NumberFormat} managing values 
         *              less than 10, or less than 100, or less than 1000, or greater than 
         *              or equal to 10000.
         * @return      A {@code NumberFormat} parameterized for formatting rank scores 
         *              of the appropriate range.
         */
        private static final NumberFormat getNumberFormat(int max) {
            log.entry(max);
            NumberFormat formatter = NumberFormat.getInstance(Locale.US);
            formatter.setRoundingMode(RoundingMode.HALF_UP);
            if (max < 10) {
                formatter.setMaximumFractionDigits(2);
                formatter.setMinimumFractionDigits(2);
            } else if (max < 100) {
                formatter.setMaximumFractionDigits(1);
                formatter.setMinimumFractionDigits(1);
            } else if (max < 1000) {
                formatter.setMaximumFractionDigits(0);
                formatter.setMinimumFractionDigits(0);
            } else if (formatter instanceof DecimalFormat) {
                ((DecimalFormat) formatter).applyPattern("0.00E0");
            } else {
                throw log.throwing(new AssertionError("No formatter could be defined"));
            }
            return log.exit(formatter);
        }

        //*******************************************
        // INSTANCE ATTRIBUTES AND METHODS
        //*******************************************
        /**
         * @see #getGlobalMeanRank()
         */
        //TODO: Maybe create a new RankScore class, storing the rank, 
        //plus an information of confidence about it.
        private final BigDecimal globalMeanRank;
        
        public ExpressionCall(Gene gene, Condition condition,
            Collection<ExpressionCallData> callData, BigDecimal globalMeanRank) {
            this(gene, condition, null, null, null, callData, globalMeanRank);
        }
        
        public ExpressionCall(Gene gene, Condition condition, Boolean isObservedData, 
            ExpressionSummary summaryCallType, SummaryQuality summaryQual, 
            Collection<ExpressionCallData> callData, BigDecimal globalMeanRank) {
            this(gene, condition, isObservedData, summaryCallType, summaryQual, callData,
                globalMeanRank, null);
        }

        public ExpressionCall(Gene gene, Condition condition, Boolean isObservedData, 
                ExpressionSummary summaryCallType, SummaryQuality summaryQual, 
                Collection<ExpressionCallData> callData, BigDecimal globalMeanRank,
                Collection<ExpressionCall> sourceCalls) {
            super(gene, condition, isObservedData, summaryCallType, summaryQual, callData,
                sourceCalls == null ? new HashSet<>() : sourceCalls.stream()
                    .map(c -> (Call<ExpressionSummary, ExpressionCallData>) c)
                    .collect(Collectors.toSet()));
            
            if (globalMeanRank != null && globalMeanRank.compareTo(new BigDecimal(0)) <= 0) {
                throw log.throwing(new IllegalArgumentException(
                        "A rank cannot be less than or equal to 0."));
            }
            //BigDecimal are immutable, no need to copy it
            this.globalMeanRank = globalMeanRank;
        }
        
        /**
         * @return  The {@code BigDecimal} corresponding to the score allowing to rank 
         *          this {@code ExpressionCall}.
         *          
         * @see #getFormattedGlobalMeanRank()
         */
        public BigDecimal getGlobalMeanRank() {
            return this.globalMeanRank;
        }
        /**
         * @return  A {@code String} corresponding to the rank score of this call, formatted 
         *          with always 3 digits displayed, e.g.: 1.23, 12.3, 123, 1.23e3, ... 
         *          
         * @see #getGlobalMeanRank()
         */
        public String getFormattedGlobalMeanRank() {
            log.entry();
            if (this.globalMeanRank == null) {
                throw log.throwing(new IllegalStateException("No rank was provided for this call."));
            }
            NumberFormat formatter = null;
            //start with values over 1000, more chances to have a match
            if (this.globalMeanRank.compareTo(new BigDecimal(1000)) >= 0) {
                formatter = FORMAT1000;
            } else if (this.globalMeanRank.compareTo(new BigDecimal(10)) < 0) {
                formatter = FORMAT1;
            } else if (this.globalMeanRank.compareTo(new BigDecimal(100)) < 0) {
                formatter = FORMAT10;
            } else if (this.globalMeanRank.compareTo(new BigDecimal(1000)) < 0) {
                formatter = FORMAT100;
            }
            //1E2 to 1e2
            return log.exit(formatter.format(this.globalMeanRank).toLowerCase(Locale.ENGLISH));
        }
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((globalMeanRank == null) ? 0 : globalMeanRank.hashCode());
            return result;
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
            ExpressionCall other = (ExpressionCall) obj;
            if (globalMeanRank == null) {
                if (other.globalMeanRank != null) {
                    return false;
                }
            } else if (!globalMeanRank.equals(other.globalMeanRank)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("ExpressionCall [getGlobalMeanRank()=").append(getGlobalMeanRank())
                   .append(", super Call=").append(super.toString())
                   .append("]");
            return builder.toString();
        }
        
        
    }
    
    //TODO: check that all DiffExpressionCallData 
    //have the same DiffExpressionFactor, consistent with the DiffExpressionCall
    public static class DiffExpressionCall extends Call<DiffExpressionSummary, DiffExpressionCallData> {
        /**
         * @see #getDiffExpressionFactor()
         */
        private final DiffExpressionFactor diffExpressionFactor;
        
        public DiffExpressionCall(DiffExpressionFactor factor, Gene gene, 
                Condition condition, DiffExpressionSummary summaryCallType, 
                SummaryQuality summaryQual, Collection<DiffExpressionCallData> callData) {
            this(factor, gene, condition, summaryCallType, summaryQual, callData, null);
        }
        
        public DiffExpressionCall(DiffExpressionFactor factor, Gene gene, 
            Condition condition, DiffExpressionSummary summaryCallType, 
            SummaryQuality summaryQual, Collection<DiffExpressionCallData> callData,
            Collection<DiffExpressionCall> sourceCalls) {
            super(gene, condition, null, summaryCallType, summaryQual, callData, 
                sourceCalls == null ? new HashSet<>() : sourceCalls.stream()
                .map(c -> (Call<DiffExpressionSummary, DiffExpressionCallData>) c)
                .collect(Collectors.toSet()));
            this.diffExpressionFactor = factor;
        }

        /**
         * @return  A {@code DiffExpressionFactor} defining the criteria on which comparisons 
         *          of expression levels were made. 
         */
        public DiffExpressionFactor getDiffExpressionFactor() {
            return diffExpressionFactor;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((diffExpressionFactor == null) ? 0 : diffExpressionFactor.hashCode());
            return result;
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
            DiffExpressionCall other = (DiffExpressionCall) obj;
            if (diffExpressionFactor != other.diffExpressionFactor) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            
            StringBuilder builder = new StringBuilder();
            builder.append("DiffExpressionCall [diffExpressionFactor()=").append(getDiffExpressionFactor())
                   .append(", super Call=").append(super.toString())
                   .append("]");
            return builder.toString();
        }
    }
    
    //**********************************************
    //   INSTANCE ATTRIBUTES AND METHODS
    //**********************************************
    
    private final Gene gene;
    
    private final Condition condition;
    
    private final Boolean isObservedData;
    
    private final T summaryCallType;
    
    private final SummaryQuality summaryQuality;
    
    private final Set<U> callData;

    private final Set<Call<T, U>> sourceCalls;

    protected Call(Gene gene, Condition condition, Boolean isObservedData, 
        T summaryCallType, SummaryQuality summaryQual, Collection<U> callData) {
        this(gene, condition, isObservedData, summaryCallType, summaryQual, callData, null);
    }

    protected Call(Gene gene, Condition condition, Collection<U> callData, Set<Call<T, U>> sourceCalls) {
        this(gene, condition, null, null, null, callData, sourceCalls);
    }

    private Call(Gene gene, Condition condition, Boolean isObservedData, 
        T summaryCallType, SummaryQuality summaryQuality, Collection<U> callData, 
        Set<Call<T, U>> sourceCalls) {
        if (DataQuality.NODATA.equals(summaryQuality)) {
            throw log.throwing(new IllegalArgumentException("An actual DataQuality must be provided."));
        }
//        if ((callData == null || callData.isEmpty()) && (isObservedData == null || summaryCallType == null || summaryQuality == null)) {
//            throw log.throwing(new IllegalArgumentException(
//                "Call data must be provided to infer isObservedData, summaryCallType and summaryQuality."));
//        }
        this.gene = gene;
        this.condition = condition;
        this.callData = Collections.unmodifiableSet(
            callData == null? new HashSet<>(): new HashSet<>(callData));
//        if (callData != null && !callData.isEmpty()) {
//            this.isObservedData = isObservedData != null? isObservedData : inferIsObservedData(this.callData);
//            this.summaryCallType = summaryCallType != null? summaryCallType : inferSummaryCallType(this.callData);
//            this.summaryQuality = summaryQuality != null? summaryQuality : inferSummaryQuality(this.callData);            
//        } else {
            this.isObservedData = isObservedData;
            this.summaryCallType = summaryCallType;
            this.summaryQuality = summaryQuality;            
//        }
        this.sourceCalls = Collections.unmodifiableSet(
            sourceCalls == null? new HashSet<>(): new HashSet<>(sourceCalls));
    }
    
    public Gene getGene() {
        return gene;
    }
    public Condition getCondition() {
        return condition;
    }
    public Boolean getIsObservedData() {
        return isObservedData;
    }
    public T getSummaryCallType() {
        return summaryCallType;
    }
    public SummaryQuality getSummaryQuality() {
        return summaryQuality;
    }
    public Set<U> getCallData() {
        return callData;
    }
    public Set<Call<T, U>> getSourceCalls() {
        return sourceCalls;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((gene == null) ? 0 : gene.hashCode());
        result = prime * result + ((condition == null) ? 0 : condition.hashCode());
        result = prime * result + ((isObservedData == null) ? 0 : isObservedData.hashCode());
        result = prime * result + ((summaryCallType == null) ? 0 : summaryCallType.hashCode());
        result = prime * result + ((summaryQuality == null) ? 0 : summaryQuality.hashCode());
        result = prime * result + ((callData == null) ? 0 : callData.hashCode());
        result = prime * result + ((sourceCalls == null) ? 0 : sourceCalls.hashCode());
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
        Call<?, ?> other = (Call<?, ?>) obj;
        if (gene == null) {
            if (other.gene != null) {
                return false;
            }
        } else if (!gene.equals(other.gene)) {
            return false;
        }
        if (condition == null) {
            if (other.condition != null) {
                return false;
            }
        } else if (!condition.equals(other.condition)) {
            return false;
        }
        if (isObservedData == null) {
            if (other.isObservedData != null)
                return false;
        } else if (!isObservedData.equals(other.isObservedData))
            return false;
        if (summaryCallType == null) {
            if (other.summaryCallType != null) {
                return false;
            }
        } else if (!summaryCallType.equals(other.summaryCallType)) {
            return false;
        }
        if (summaryQuality != other.summaryQuality) {
            return false;
        }
        if (callData == null) {
            if (other.callData != null) {
                return false;
            }
        } else if (!callData.equals(other.callData)) {
            return false;
        }
        if (sourceCalls == null) {
            if (other.sourceCalls != null) {
                return false;
            }
        } else if (!sourceCalls.equals(other.sourceCalls)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return    "Gene: " + gene 
                + " - Condition:" + condition
                + " - Is observed data: " + isObservedData
                + " - Summary call type: " + summaryCallType 
                + " - Summary quality: " + summaryQuality 
                + " - Call data: " + callData 
                + " - Source calls: " + sourceCalls;
    }
}

/*******************************************************************************
 * Copyright (c) 2017 Opt4J
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *******************************************************************************/

package org.opt4j.optimizer.ea.espea;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.opt4j.core.Individual;
import org.opt4j.core.IndividualSet;
import org.opt4j.core.IndividualSetListener;
import org.opt4j.core.Objectives;

/**
 * Efficiently computes the energies for each member of an {@link EnergyArchive}.
 * <p>
 * For a archive capacity of n it uses n^2/2 space. Scoring a candidate against
 * the archive members takes n energy calculations when the cache is valid and
 * n^2/2+n when it is not. In a typical optimizer run the cache is valid for
 * over 99% of all candidates.
 * <p>
 * Due to rounding errors, the energies returned may vary from the actual
 * energies computed the classical way. This effect is mostly limited to the
 * earliest generations since members introducing a large amount of energy get
 * removed from the archive quickly once it reaches it's capacity.
 *
 * @author luisgerhorst
 */
public class EnergyCache implements IndividualSetListener {

    private final ScalarizationFunction scalarizationFunction;
    private final Normalizer normalizer;

    private final int capacity;
    /**
     * Used to assign indices to individuals.
     */
    private final IndexMap indexMap;

    private boolean cacheValid;
    /**
     * Stores the energy between the archive members with the corresponding
     * indices. When accessing memberSummands[i][j], i must always be the
     * greater one of the two indices.
     */
    private final double[][] memberSummands;
    /**
     * The total energy the member with the corresponding index introduces into
     * the archive.
     */
    private final double[] memberSums;

    // Saves candidate information for Listener callbacks.
    private Individual candidate;
    private final double[] candidateSummands;

    private final MutationStrategy mutationStrategy;
    public enum MutationStrategy {
        INVERSE,
        RECALCULATE;
    }

    /**
     * @param archive the {@link EnergyArchive} for which the energies will be cached
     * @param scalarizationFunction the {@link ScalarizationFunction} to be used
     * for calculating energies
     * @param normalizer the {@link Normalizer} to be used to normalize
     * objectives before calculating their energies
     */
    public EnergyCache(EnergyArchive archive,
                       ScalarizationFunction scalarizationFunction,
                       Normalizer normalizer,
                       MutationStrategy mutationStrategy)
    {
        this.mutationStrategy = mutationStrategy;
        this.scalarizationFunction = scalarizationFunction;
        this.normalizer = normalizer;
        this.normalizer.cache = this; // allows the normalizer to call invalidate

        archive.addListener(this);
        this.capacity = archive.getCapacity();

        indexMap = new IndexMap(capacity);
        cacheValid = false;
        memberSummands = new double[capacity][];
        for (int i = 0; i < memberSummands.length; i++) {
            memberSummands[i] = new double[i];
        }
        memberSums = new double[capacity];

        candidate = null;
        candidateSummands = new double[capacity];
    }

    /**
     * Used by the {@link Normalizer} to notify the cache that the normalized
     * objectives returned so far are no longer valid. The next time
     * <code>replacementEnergiesFor</code> or <code>energyIntroducedBy</code> is
     * called, the energies of all archive members will be recomputed.
     */
    public void invalidate() {
        cacheValid = false;
    }

    /**
     * Calculates the potential energies introduced by the candidate if it was
     * to replace a member for every individual in the archive. In case the
     * candidate actually makes it into the archive, the corresponding call to
     * add it to the archive call will be very fast when no other call to this
     * function or <code>add</code> are been made in the meantime.
     *
     * @param candidate a <code>individual</code> that is part of the
     * pareto-front and which may enter the archive if it decreases it's overall
     * energy
     * @return a collection of {@link EnergyArchive.RemovalOption} objects
     */
    public Collection<EnergyArchive.RemovalOption>
        replacementEnergiesFor(final Individual candidate)
    {
        if (!cacheValid) {
            updateMemberSums();
        }
        final double[] candidateSums = candidateSums(candidate);

        final List<EnergyArchive.RemovalOption> energies =
            new ArrayList<EnergyArchive.RemovalOption>(capacity);
        for (final IndexMap.Entry entry : indexMap) {
            final int index = entry.index;
            energies.add(new EnergyArchive.RemovalOption(memberSums[index],
                                                         candidateSums[index],
                                                         entry.individual));
        }

        return energies;
    }

    /**
     * Updates the cache (for all archive members) when needed and returns the
     * energy introduced by the given individual.
     *
     * @param individual the archive member
     * @return sum of energies between the given archive member and all other
     * individuals in the archive
     */
    public double energyIntroducedBy(final Individual individual)
    {
        if (!cacheValid) {
            updateMemberSums();
        }
        return memberSums[indexMap.get(individual)];
    }

    private void updateMemberSums()
    {
        for (final IndexMap.Entry rowEntry : indexMap) {
            final int rowIndex = rowEntry.index;
            final Objectives rowObjectives = rowEntry.individual.getObjectives();

            memberSums[rowIndex] = 0;

            final Iterator<IndexMap.Entry> columnIterator = indexMap.iteratorTo(rowIndex);
            while (columnIterator.hasNext()) {
                final IndexMap.Entry columnEntry = columnIterator.next();
                final int columnIndex = columnEntry.index;
                final Objectives columnObjectives = columnEntry.individual.getObjectives();

                final double summand = energyBetween(rowObjectives, columnObjectives);
                memberSums[rowIndex] += summand;
                memberSums[columnIndex] += summand;
                memberSummands[rowIndex][columnIndex] = summand;
            }
        }
        cacheValid = true;
    }

    private double[] candidateSums(final Individual candidate)
    {
        final double total = updateCandidateSummands(candidate);
        final double[] candidateSums = new double[capacity];
        for (int i = 0; i < candidateSums.length; i++) {
            candidateSums[i] = total - candidateSummands[i];
        }
        return candidateSums;
    }

    private double updateCandidateSummands(final Individual candidate)
    {
        this.candidate = candidate;

        final Objectives candidateObjectives = candidate.getObjectives();

        double total = 0;
        for (final IndexMap.Entry entry : indexMap) {
            final int index = entry.index;
            final Objectives memberObjectives = entry.individual.getObjectives();

            final double summand =
                energyBetween(candidateObjectives, memberObjectives);
            total += summand;
            candidateSummands[index] = summand;
        }
        return total;
    }

    /**
     * Called by the {@link EnergyArchive} when an individual is added. Performs
     * the apropriate modifications to the internal data structures to keep them
     * in sync with the archive.
     */
    public void individualAdded(final IndividualSet collection,
                                final Individual added)
    {
        final int addedIndex = indexMap.put(added);
        if (cacheValid) {
            if (added != candidate) {
                updateCandidateSummands(added);
            }
            memberSums[addedIndex] = 0;
            for (final IndexMap.Entry entry : indexMap) {
                mergeCandidateSummand(addedIndex, entry.index);
            }
        }
    }

    private void mergeCandidateSummand(final int addedIndex,
                                       final int memberIndex)
    {
        final double summand = candidateSummands[memberIndex];

        if (addedIndex > memberIndex) {
            memberSummands[addedIndex][memberIndex] = summand;
        } else if (addedIndex < memberIndex) {
            memberSummands[memberIndex][addedIndex] = summand;
        } else {
            return;
        }

        memberSums[addedIndex] += summand;
        memberSums[memberIndex] += summand;
    }

    /**
     * Callback by the {@link EnergyArchive} when an individual is
     * removed. Performs the apropriate modifications to the internal data
     * structures to keep them in sync with the archive.
     */
    public void individualRemoved(final IndividualSet collection,
                                  final Individual removed)
    {
        final int removedIndex = indexMap.remove(removed);
        if (cacheValid) {
            for (final IndexMap.Entry entry : indexMap) {
                substractMemberFromSum(removedIndex, entry.index, removed);
            }
        }
    }

    private void substractMemberFromSum(final int removedIndex,
                                        final int memberIndex,
                                        final Individual removed)
    {

        switch (mutationStrategy) {
		case INVERSE:
            final double summandMinus;
            if (removedIndex > memberIndex) {
                summandMinus = memberSummands[removedIndex][memberIndex];
            } else if (removedIndex < memberIndex) {
                summandMinus = memberSummands[memberIndex][removedIndex];
            } else {
                return;
            }

            // double usually has a precision of 15-17 decimal digits, so this
            // warning is pretty conservative.
            // if (summand / memberSums[memberIndex] > 0.9999) {
            // System.out.println("Warning: A summand makes up more than 99.99% of it's sum, it's substraction may lead to an imprecise result.");
            // }

            // The following operation is the reason sums may differ from the
            // energies calculated the classical way (sum over all summands). When
            // the summand to be substracted is very large (e.g. 10^14) and the
            // remaining sum is small, the precision gets so low it may affect the
            // leading digit of the difference. To prevent such imprecision we could
            // recalculate the sum at this point (sum over all remaining archive
            // members). We don't do this here because such large summands only
            // occur in the early archive and get overridden anyway the next time
            // the normalizer invalidates the cache.
            memberSums[memberIndex] -= summandMinus;
			break;
        default: // RECALCULATE
            memberSums[memberIndex] = 0.0;

			for (final IndexMap.Entry entry : indexMap) {
                final double summandPlus;
                if (entry.index > memberIndex) {
                    summandPlus = memberSummands[entry.index][memberIndex];
                } else if (entry.index < memberIndex) {
                    summandPlus = memberSummands[memberIndex][entry.index];
                } else {
                    continue;
                }

                memberSums[memberIndex] += summandPlus;
            }
			break;
		}
    }

    private double energyBetween(Objectives a, Objectives b) {
        a = normalizer.normalize(a);
        b = normalizer.normalize(b);
        return scalarizationFunction.calculate(a, b) / a.distance(b);
    }

}

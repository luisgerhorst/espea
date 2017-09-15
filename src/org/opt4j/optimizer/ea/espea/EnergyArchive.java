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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.opt4j.core.Individual;
import org.opt4j.core.common.archive.BoundedArchive;
import org.opt4j.core.start.Constant;

import com.google.inject.Inject;
import com.google.inject.Provider;


/**
 * A set of non-dominated individuals. When the capacity of the archive is
 * reached a new candidate may only enter the archive if the overall energy of
 * the archive decreases when a member is replaced by the candidate. If there
 * are multiple members that could be replaced the replacement strategy
 * determines the one chosen.
 *
 * @author luisgerhorst
 */
public class EnergyArchive extends BoundedArchive {

    private final ReplacementStrategy replacementStrategy;

    // Breaks potential circular dependency over TsvLogger.
    private final EnergyCache.MutationStrategy cacheMutationStrategy;
    private final ScalarizationFunction scalarizationFunction;
    private final Provider<Normalizer> normalizerProvider;
    private EnergyCache energyCache;

    @Inject
    public EnergyArchive(ScalarizationFunction scalarizationFunction,
                         Provider<Normalizer> normalizerProvider,
                         @Constant(value = "capacity", namespace = EnergyArchive.class) int capacity,
                         @Constant(value = "replacementStrategy", namespace = EnergyArchive.class) ReplacementStrategy replacementStrategy,
                         @Constant(value = "cacheMutationStrategy", namespace = EnergyCache.class) EnergyCache.MutationStrategy cacheMutationStrategy)
    {
        super(capacity);
        this.replacementStrategy = replacementStrategy;

        this.cacheMutationStrategy = cacheMutationStrategy;
        this.scalarizationFunction = scalarizationFunction;
        this.normalizerProvider = normalizerProvider;
    }

    public void initialize() {
        this.energyCache = new EnergyCache(this, scalarizationFunction,
                                           normalizerProvider.get(),
                                           cacheMutationStrategy);
    }

    @Override
    protected boolean updateWithNondominated(final Collection<Individual> candidates)
    {
        boolean changed = false;
        for (final Individual candidate : candidates) {
            if (this.size() < this.getCapacity()) {
                changed |= addCheckedIndividual(candidate);
            } else if (this.size() == this.getCapacity()) {
                changed |= replaceWithNondominated(candidate);
            }
        }
        return changed;
    }

    /**
     * Replaces an archive member with the candidate if the replacement
     * decreases the overall energy of the archive. If multiple archive members
     * introduce more energy than the candidate, one is chosen according to the
     * replacement strategy.
     */
    private boolean replaceWithNondominated(final Individual candidate)
    {
        // The cache has been invalidated by the normalizer.
        final Collection<RemovalOption> allMembers = energyCache.replacementEnergiesFor(candidate);
        final Collection<RemovalOption> replaceableMembers = replaceableMembers(allMembers);

        // No member member introduces more energy into the system than the
        // candidate would have introduced.
        if (replaceableMembers.isEmpty()) return false;

        final Individual memberToBeReplaced;
        switch (replacementStrategy) {
        case BEST_FEASIBLE_POSITION:
            memberToBeReplaced =
                bestFeasiblePosition(replaceableMembers);
            break;
        case WORST_IN_ARCHIVE:
            memberToBeReplaced =
                worstInArchive(replaceableMembers);
            break;
        default: // LARGEST_ENERGY_DECREASE
            memberToBeReplaced =
                largestEnergyDecrease(replaceableMembers);
            break;
        }

        remove(memberToBeReplaced);
        addCheckedIndividual(candidate);
        return true;
    }

    /**
     * Selects all archive members that introduce more energy into the archive
     * than the candidate would.
     */
    private Collection<RemovalOption> replaceableMembers(Collection<RemovalOption> allMembers)
    {
        final List<RemovalOption> list = new LinkedList<RemovalOption>();
        for (final RemovalOption entry : allMembers) {
            if (entry.memberEnergy > entry.candidateEnergy) {
                list.add(entry);
            }
        }
        return list;
    }

    public enum ReplacementStrategy {
        BEST_FEASIBLE_POSITION,
        WORST_IN_ARCHIVE,
        LARGEST_ENERGY_DECREASE;
    }

    private Individual bestFeasiblePosition(final Collection<RemovalOption> replaceableMembers) {
        Individual toBeReplaced = null;
        double minCandidateEnergy = Double.POSITIVE_INFINITY;
        for (final RemovalOption entry : replaceableMembers) {
            if (entry.candidateEnergy < minCandidateEnergy) {
                toBeReplaced = entry.member;
                minCandidateEnergy = entry.candidateEnergy;
            }
        }
        return toBeReplaced;
    }

    private Individual worstInArchive(final Collection<RemovalOption> replaceableMembers) {
        Individual toBeReplaced = null;
        double maxMemberEnergy = Double.NEGATIVE_INFINITY;
        for (final RemovalOption entry : replaceableMembers) {
            if (entry.memberEnergy > maxMemberEnergy) {
                toBeReplaced = entry.member;
                maxMemberEnergy = entry.memberEnergy;
            }
        }
        assert replaceableMembers.isEmpty() || toBeReplaced != null;
        return toBeReplaced;
    }

    private Individual largestEnergyDecrease(final Collection<RemovalOption> replaceableMembers) {
        Individual toBeReplaced = null;
        double maxEnergyDecrease = Double.NEGATIVE_INFINITY;
        for (final RemovalOption entry : replaceableMembers) {
            final double energyDecrease = entry.memberEnergy - entry.candidateEnergy;
            if (energyDecrease > maxEnergyDecrease) {
                toBeReplaced = entry.member;
                maxEnergyDecrease = energyDecrease;
            }
        }
        return toBeReplaced;
    }

    /**
     * Used to determine the energy decrease for a candidate if it was to
     * replace an archive member.<p>
     *
     * With respect to the {@link ESPEA} paper, this class represents a tuple of
     * e(a), e_{-a}(p) and a.
     */
    public static class RemovalOption {
        /**
         * The total energy <code>member</code> currently introduces into the
         * archive.
         */
        public final double memberEnergy;
        /**
         * The energy a candidate would introduce into the archive if it was to
         * replace <code>member</code>.
         */
        public final double candidateEnergy;
        /**
         * An archive member.
         */
        public final Individual member;
        public RemovalOption(double memberEnergy, double candidateEnergy,
                             Individual member) {
            this.memberEnergy = memberEnergy;
            this.candidateEnergy = candidateEnergy;
            this.member = member;
        }
    }

    /**
     * @param individual archive member whose energy shall be returned
     * @return the sum of energies between the given individual and all other
     * archive members
     */
    public double energyIntroducedBy(final Individual individual) {
        return energyCache.energyIntroducedBy(individual);
    }

}

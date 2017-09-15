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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.opt4j.core.Individual;
import org.opt4j.core.start.Constant;
import org.opt4j.operators.crossover.Pair;
import org.opt4j.optimizers.ea.Coupler;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * Creates distinct couples using tournament selection. Individuals are sorted
 * using the provided <code>Comparator</code>, the first item of the sorted list
 * wins the tournament.
 *
 * @author luisgerhorst
 */
public class CouplerDistinctTournament implements Coupler {

    private final Comparator<Individual> comparator;
    private final int tournamentSize;

    /**
     * @param comparator the comparator used to determine the tournament winner
     * @param tournamentSize the tournament size, e.g. the size of the set of
     * parents from which the 'least' member is chosen to be part of a couple
     */
    @Inject
    public CouplerDistinctTournament(@Named("TournamentComparator") Comparator<Individual> comparator,
                                     @Constant(value = "tournamentSize", namespace = CouplerDistinctTournament.class) int tournamentSize) {
        this.comparator = comparator;
        this.tournamentSize = tournamentSize;
    }

    /**
     * Creates couples that are as distinct as possible (the individuals of a
     * couple are never equal and, when there are enough parents, a couple may
     * only occur once). A minimum of two parents is required.
     *
     * @see org.opt4j.optimizers.ea.Coupler
     */
    @Override
	public Collection<Pair<Individual>> getCouples(final int requested,
                                                   final List<Individual> parents) {
        final Collection<Pair<Individual>> couples =
            new ArrayList<Pair<Individual>>(requested);
        final int parentCount = parents.size();
        if (parentCount < 2) {
            throw new RuntimeException("At least 2 parents are required to create distinct couples.");
        }
        final int distinctCouplesPossible = (int) (parentCount * (parentCount - 1)) / 2;
        while (couples.size() < requested) {
            // Maps individuals to a set of individuals they can be coupled
            // with.
            final Map<Individual, Set<Individual>> coupleMap =
                new HashMap<Individual, Set<Individual>>();
            for (final Individual parent : parents) {
                final Set<Individual> set = new HashSet<Individual>(parents);
                set.remove(parent);
                coupleMap.put(parent, set);
            }

            // Instead of choosing two completely random individuals at a time
            // without enforcing any restrictions on the couples (in a extreme
            // case we could return the same couple requested times) as the
            // authors of the paper do, we only allow couples that contain
            // distinct individuals. Two couples shall also contain distinct
            // individuals.
            final int freeSpaceLeft = requested - couples.size();
            final int todo = Math.min(freeSpaceLeft, distinctCouplesPossible);
            for (int i = 0; i < todo; i++) {
                final Set<Individual> possibleMales = coupleMap.keySet();
                final Individual male = tournamentSelect(possibleMales);
                final Set<Individual> femalesForMale = coupleMap.get(male);
                final Individual female = tournamentSelect(femalesForMale);

                femalesForMale.remove(female);
                if (femalesForMale.isEmpty()) {
                    coupleMap.remove(male);
                }

                final Set<Individual> malesForFemale = coupleMap.get(female);
                malesForFemale.remove(male);
                if (malesForFemale.isEmpty()) {
                    coupleMap.remove(female);
                }

                final Pair<Individual> pair = new Pair<Individual>(male, female);
                couples.add(pair);
            }
        }
		return couples;
	}

    private Individual tournamentSelect(final Collection<Individual> parents) {
        final List<Individual> tournament = randomSubset(parents, tournamentSize);
        Collections.sort(tournament, comparator);
        return tournament.get(0);
    }

    private static <T> List<T> randomSubset(final Collection<T> collection,
                                            final int size) {
        final Vector<T> vector = new Vector<T>(collection);
        Collections.shuffle(vector);
        vector.setSize(Math.min(size, vector.size()));
        return vector;
    }

}

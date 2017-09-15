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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.opt4j.core.Genotype;
import org.opt4j.core.Individual;
import org.opt4j.core.IndividualFactory;
import org.opt4j.core.common.random.Rand;
import org.opt4j.core.start.Constant;
import org.opt4j.operators.algebra.Add;
import org.opt4j.operators.algebra.Algebra;
import org.opt4j.operators.algebra.Index;
import org.opt4j.operators.algebra.Mult;
import org.opt4j.operators.algebra.Sub;
import org.opt4j.operators.algebra.Term;
import org.opt4j.operators.algebra.Var;
import org.opt4j.operators.crossover.Crossover;
import org.opt4j.operators.crossover.Pair;
import org.opt4j.optimizers.ea.Mating;

import com.google.inject.Inject;

/**
 * Creates offspring from a given set of parents by using differential
 * evolution.
 *
 * @author glass, lukasiewycz, luisgerhorst
 */
public class MatingDifferentialEvolution implements Mating {

    private final IndividualFactory individualFactory;
    private final Crossover<Genotype> crossover;
    private final Algebra<Genotype> algebra;
    private final Rand random;
    private final Term term;

    @Inject
    public MatingDifferentialEvolution(IndividualFactory individualFactory,
                                       Crossover<Genotype> crossover,
                                       Algebra<Genotype> algebra,
                                       Rand random,
                                       @Constant(value = "scalingFactor",
                                                 namespace = MatingDifferentialEvolution.class)
                                       double scalingFactor) {
        this.individualFactory = individualFactory;
        this.crossover = crossover;
        this.algebra = algebra;
        this.random = random;

        final Index i0 = new Index(0);
        final Index i1 = new Index(1);
        final Index i2 = new Index(2);
        final Var c = new Var(scalingFactor);
        this.term = new Add(i0, new Mult(c, new Sub(i1, i2)));
    }

    /*
     * (non-Javadoc)
     *
     * @see org.opt4j.optimizer.ea.Mating#getOffspring(int,
     * org.opt4j.core.Individual[])
     */
    @Override
    public Collection<Individual> getOffspring(final int size,
                                               final Individual... parents) {
        return getOffspring(size, Arrays.asList(parents));
    }

    /**
     * Creates offspring from a given set of parents. size must be the number of
     * parents.
     */
    @Override
    public Collection<Individual> getOffspring(final int size,
                                               final Collection<Individual> parents) {
        assert size == parents.size() : "Size must be the number of parents given";
        assert parents.size() >= 4 : "Differential evolution requires at least 4 parents";

        final List<Individual> list = new ArrayList<Individual>(parents);
        final List<Individual> offsprings = new ArrayList<Individual>(size);
        for (final Individual parent : parents) {
            final Individual offspring = createOffspring(parent, list, term);
            offsprings.add(offspring);
        }
        return offsprings;
    }

    private Individual createOffspring(Individual parent,
                                       List<Individual> individuals,
                                       Term term) {
        final Triple triple = getTriple(parent, individuals);

        final Genotype g0 = triple.getFirst().getGenotype();
        final Genotype g1 = triple.getSecond().getGenotype();
        final Genotype g2 = triple.getThird().getGenotype();

        final Genotype result = algebra.algebra(term, g0, g1, g2);
        final Pair<Genotype> g = crossover.crossover(result, parent.getGenotype());

        final Individual i;
        if (random.nextBoolean()) {
            i = individualFactory.create(g.getFirst());
        } else {
            i = individualFactory.create(g.getSecond());
        }
        return i;
    }

    /**
     * The {@link Triple} is a container for three individuals.
     *
     * @author lukasiewycz
     *
     */
    private static class Triple {
        protected final Individual first;

        protected final Individual second;

        protected final Individual third;

        public Triple(final Individual first,
                      final Individual second,
                      final Individual third) {
            super();
            this.first = first;
            this.second = second;
            this.third = third;
        }

        public Individual getFirst() {
            return first;
        }

        public Individual getSecond() {
            return second;
        }

        public Individual getThird() {
            return third;
        }
    }

    /**
     * Returns three different {@link Individual}s from the {@code individuals}
     * list. Each {@link Individual} is not equal to the parent.
     *
     * @param parent
     *            the parent Individual
     * @param individuals
     *            the population
     * @return the three individuals as a Triple
     */
    private Triple getTriple(final Individual parent,
                             final List<Individual> individuals) {
        individuals.remove(parent);
        assert individuals.size() >= 3: "No individuals left";
        final Individual ind0 = individuals
                .remove(random.nextInt(individuals.size()));
        final Individual ind1 = individuals
                .remove(random.nextInt(individuals.size()));
        final Individual ind2 = individuals
                .remove(random.nextInt(individuals.size()));

        final Triple triple = new Triple(ind0, ind1, ind2);

        individuals.add(parent);
        individuals.add(ind0);
        individuals.add(ind1);
        individuals.add(ind2);

        return triple;

    }

}

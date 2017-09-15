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
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.opt4j.core.Individual;
import org.opt4j.core.IndividualFactory;
import org.opt4j.core.optimizer.Archive;
import org.opt4j.core.optimizer.IncompatibilityException;
import org.opt4j.core.optimizer.IndividualCompleter;
import org.opt4j.core.optimizer.IterativeOptimizer;
import org.opt4j.core.optimizer.TerminationException;
import org.opt4j.core.start.Constant;
import org.opt4j.optimizers.ea.Mating;

import com.google.inject.Inject;
import com.google.inject.name.Named;


/**
 * Implementation of a multi-objective optimizer as described in "Obtaining
 * Optimal Pareto Front Approximations using Scalarized Preference Information"
 * by Braun et al 2015.
 * <p>
 * The <code>Archive</code> bound must be a subclass of <code>EnergyArchive</code>.
 *
 * @author luisgerhorst
 */
public class ESPEA implements IterativeOptimizer {

    // Archiving
    private final IndividualCompleter individualCompleter;
    private final EnergyArchive archive;

    // Initial Population
    private final IndividualFactory individualFactory;
    private final int initialPopulationSize;

    // Early Generations
    private final Mating earlyMating;
    private final int earlyGenerationSize;

    // Late Generations
    private Mating lateMating;

    @Inject
    public ESPEA(IndividualCompleter individualCompleter,
                 Archive archive,

                 // Initial Population
                 IndividualFactory individualFactory,
                 @Constant(value = "initialPopulationSize", namespace = ESPEA.class) int initialPopulationSize,

                 // Early Generations
                 @Named("EarlyMating") Mating earlyMating,
                 @Constant(value = "earlyGenerationSize", namespace = ESPEA.class) int earlyGenerationSize,

                 // Late Generations
                 @Named("LateMating") Mating lateMating) {

        // Archiving
        this.individualCompleter = individualCompleter;
        this.archive = (EnergyArchive) archive;

        // Initial Population
        this.individualFactory = individualFactory;
        this.initialPopulationSize = initialPopulationSize;

        // Early Generations
        this.earlyMating = earlyMating;
        this.earlyGenerationSize = earlyGenerationSize;

        // Late Generations
        this.lateMating = lateMating;
    }

    @Override
    public void initialize() throws TerminationException {
        archive.initialize();
    }

    private boolean capacityReached = false;

    @Override
    public void next() throws TerminationException {
        if (archive.size() < 2) {
            initialPopulation();
        } else if (!capacityReached || archive.size() < 4) {
            earlyGeneration();
            capacityReached = archive.size() == archive.getCapacity();
        } else {
            lateGeneration();
        }
    }

    private void evaluate(final Set<Individual> candidates) throws TerminationException
    {
        individualCompleter.complete(candidates);
        archive.update(candidates);
    }

    private void initialPopulation() throws TerminationException {
        final Set<Individual> initialPopulation;
        {
            final float loadFactor = 0.75f;
            final int capacity = (int) Math.ceil(initialPopulationSize / loadFactor);
            initialPopulation = new HashSet<Individual>(capacity, loadFactor);
        }

        for (int i = 0; i < initialPopulationSize; i++) {
            initialPopulation.add(individualFactory.create());
        }

        evaluate(initialPopulation);
    }

    private void earlyGeneration() throws TerminationException {
        final Collection<Individual> offspring =
            earlyMating.getOffspring(earlyGenerationSize, archive);
        evaluate(new CopyOnWriteArraySet<Individual>(offspring));
    }

    private void lateGeneration() throws TerminationException {
        Collection<Individual> offspring;
        try {
            offspring = lateMating.getOffspring(archive.size(), archive);
        } catch (IncompatibilityException ie) {
            // System.out.println("Falling back to early mating method since problem is incompatible with late mating class.");
            lateMating = earlyMating;
            offspring = lateMating.getOffspring(archive.size(), archive);
        }
        evaluate(new CopyOnWriteArraySet<Individual>(offspring));
    }

}

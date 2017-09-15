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

import java.util.Comparator;

import org.opt4j.core.Individual;
import org.opt4j.core.common.archive.BoundedArchive;
import org.opt4j.core.common.completer.ParallelIndividualCompleter;
import org.opt4j.core.config.annotations.Info;
import org.opt4j.core.optimizer.Archive;
import org.opt4j.core.optimizer.IndividualCompleter;
import org.opt4j.core.optimizer.MaxIterations;
import org.opt4j.core.optimizer.OptimizerModule;
import org.opt4j.core.start.Constant;
import org.opt4j.optimizers.ea.ConstantCrossoverRate;
import org.opt4j.optimizers.ea.Coupler;
import org.opt4j.optimizers.ea.CrossoverRate;
import org.opt4j.optimizers.ea.Mating;
import org.opt4j.optimizers.ea.MatingCrossoverMutate;

import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

/**
 * Config module for {@link ESPEA}.
 *
 * @author luisgerhorst
 */
public class ESPEAModule extends OptimizerModule {

    @Override
    public void config() {
        bindIterativeOptimizer(ESPEA.class);

        // Used by ESPEA
        bind(Mating.class)
            .annotatedWith(Names.named("EarlyMating"))
            .to(MatingCrossoverMutate.class)
            .in(SINGLETON);
        bind(Mating.class)
            .annotatedWith(Names.named("LateMating"))
            .to(MatingDifferentialEvolution.class)
            .in(SINGLETON);
        bind(BoundedArchive.class)
            .to(EnergyArchive.class)
            .in(SINGLETON);
        bind(Archive.class)
            .to(BoundedArchive.class)
            .in(SINGLETON);

        // Used by CouplerDistinctTournament
        bind(new TypeLiteral<Comparator<Individual>>() {})
            .annotatedWith(Names.named("TournamentComparator"))
            .to(EnergyComparator.class)
            .in(SINGLETON);

        // Used by MatingCrossoverMutate
        bind(Coupler.class)
            .to(CouplerDistinctTournament.class)
            .in(SINGLETON);
        bind(CrossoverRate.class)
            .to(ConstantCrossoverRate.class)
            .in(SINGLETON);

        // Used by EnergyArchive
        switch (scalarizationFunctionType) {
        case SUM_OF_OBJECTIVES:
            bind(ScalarizationFunction.class)
                .to(ScalarizationFunctionSumOfObjectives.class)
                .in(SINGLETON);
            break;
        case CHEBYSHEV:
            bind(ScalarizationFunction.class)
                .to(ScalarizationFunctionChebyshev.class)
                .in(SINGLETON);
            break;
        default: // NO_PREFERENCE
            bind(ScalarizationFunction.class)
                .to(ScalarizationFunctionNoPreference.class)
                .in(SINGLETON);
            break;
        }

        if (individualCompleterMaxThreads > 1) {
            bind(IndividualCompleter.class)
                .to(ParallelIndividualCompleter.class)
                .in(SINGLETON);
        }
    }


    @Constant(value = "maxThreads", namespace = ParallelIndividualCompleter.class)
    protected int individualCompleterMaxThreads = 1;

    public int getIndividualCompleterMaxThreads() {
        return individualCompleterMaxThreads;
    }

    public void setIndividualCompleterMaxThreads(int individualCompleterMaxThreads) {
        this.individualCompleterMaxThreads = individualCompleterMaxThreads;
    }


    @Constant(value = "replacementStrategy", namespace = EnergyArchive.class)
    protected EnergyArchive.ReplacementStrategy replacementStrategy =
        EnergyArchive.ReplacementStrategy.WORST_IN_ARCHIVE;

    public EnergyArchive.ReplacementStrategy getReplacementStrategy() {
        return replacementStrategy;
    }

    public void setReplacementStrategy(EnergyArchive.ReplacementStrategy replacementStrategy) {
        this.replacementStrategy = replacementStrategy;
    }


    public enum ScalarizationFunctionType {
        NO_PREFERENCE,
        SUM_OF_OBJECTIVES,
        CHEBYSHEV;
    }

    @Info("The type scalarization function to determine preference information.")
    protected ScalarizationFunctionType scalarizationFunctionType =
        ScalarizationFunctionType.NO_PREFERENCE;

    public ScalarizationFunctionType getScalarizationFunctionType() {
        return scalarizationFunctionType;
    }

    public void setScalarizationFunctionType(ScalarizationFunctionType scalarizationFunctionType) {
        this.scalarizationFunctionType = scalarizationFunctionType;
    }


    @MaxIterations
    protected int iterations = 1000;

    public int getIterations() {
        return iterations;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }


    @Constant(value = "capacity", namespace = EnergyArchive.class)
    protected int archiveCapacity = 100;

    public int getArchiveCapacity() {
        return archiveCapacity;
    }

    public void setArchiveCapacity(int archiveCapacity) {
        this.archiveCapacity = archiveCapacity;
    }


    @Constant(value = "tournamentSize", namespace = CouplerDistinctTournament.class)
    protected int tournamentSize = 2;

    public int getTournamentSize() {
        return tournamentSize;
    }

    public void setTournamentSize(int tournamentSize) {
        this.tournamentSize = tournamentSize;
    }

    /**
     * Also known as the differential weight or F.
     */
    @Constant(value = "scalingFactor", namespace = MatingDifferentialEvolution.class)
    protected double differentialEvolutionScalingFactor = 0.5;

    public double getDifferentialEvolutionScalingFactor() {
        return differentialEvolutionScalingFactor;
    }

    public void setDifferentialEvolutionScalingFactor(double differentialEvolutionScalingFactor) {
        this.differentialEvolutionScalingFactor = differentialEvolutionScalingFactor;
    }


    @Constant(value = "rate", namespace = ConstantCrossoverRate.class)
    protected double crossoverRate = 0.9;

    public double getCrossoverRate() {
        return crossoverRate;
    }

    public void setCrossoverRate(double crossoverRate) {
        this.crossoverRate = crossoverRate;
    }


    @Constant(value = "initialPopulationSize", namespace = ESPEA.class)
    protected int initialPopulationSize = 100;

    public int getInitialPopulationSize() {
        return initialPopulationSize;
    }

    public void setInitialPopulationSize(int initialPopulationSize) {
        this.initialPopulationSize = initialPopulationSize;
    }


    @Constant(value = "earlyGenerationSize", namespace = ESPEA.class)
    protected int earlyGenerationSize = 1;

    public int getEarlyGenerationSize() {
        return earlyGenerationSize;
    }

    public void setEarlyGenerationSize(int earlyGenerationSize) {
        this.earlyGenerationSize = earlyGenerationSize;
    }


    @Constant(value = "cacheMutationStrategy", namespace = EnergyCache.class)
    protected EnergyCache.MutationStrategy cacheMutationStrategy = EnergyCache.MutationStrategy.INVERSE;

    public EnergyCache.MutationStrategy getCacheMutationStrategy() {
        return cacheMutationStrategy;
    }

    public void setCacheMutationStrategy(EnergyCache.MutationStrategy cacheMutationStrategy) {
        this.cacheMutationStrategy = cacheMutationStrategy;
    }


}

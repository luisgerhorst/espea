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

import static org.opt4j.core.Objective.Sign.MAX;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.opt4j.core.Individual;
import org.opt4j.core.IndividualFactory;
import org.opt4j.core.IndividualStateListener;
import org.opt4j.core.Objective;
import org.opt4j.core.Objectives;
import org.opt4j.core.Value;

import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * The {@link Normalizer} can be used to normalize {@link Objectives}.
 *
 * @author reimann, luisgerhorst
 */
@Singleton
public class Normalizer implements IndividualStateListener {
	private final Map<Objective, Double> minValues = new HashMap<Objective, Double>();
	private final Map<Objective, Double> maxValues = new HashMap<Objective, Double>();

    public EnergyCache cache;

	/**
	 * Creates a new {@link Normalizer}.
	 *
	 * @param individualFactory
	 *            the individual factory
	 */
	@Inject
	public Normalizer(IndividualFactory individualFactory) {
		individualFactory.addIndividualStateListener(this);
	}

	/**
	 * Returns normalized {@link Objectives}. Each resulting {@link Objective}
	 * is in the range between 0.0 and 1.0 and has to be minimized. Here, 0.0 is
	 * the smallest value seen so far for this {@link Objective} for all
	 * evaluated {@link Individual}s and 1.0 the biggest value, respectively. If
	 * an {@link Objective} is infeasible, it is set to 1.0.
	 *
	 * @param objectives
	 *            the objectives to normalize
	 * @return the normalized objectives
	 */
	public Objectives normalize(Objectives objectives) {
		Objectives normalized = new Objectives();

		for (Entry<Objective, Value<?>> entry : objectives) {
			Objective objective = entry.getKey();
			double oldvalue = toMinProblem(entry.getKey(), entry.getValue());

			double newvalue = 1.0;

			if (oldvalue != Double.MAX_VALUE) {
				Double min = minValues.get(objective);
				Double max = maxValues.get(objective);
				assert min != null;
				assert max != null;
				newvalue = (oldvalue - min) / (max - min);
			}
			normalized.add(objective, newvalue);
		}

		return normalized;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.opt4j.core.IndividualStateListener#inidividualStateChanged(org.opt4j
	 * .core.Individual)
	 */
	@Override
	public void inidividualStateChanged(Individual individual) {
		if (individual.isEvaluated()) {
            boolean changed = false;
			for (Entry<Objective, Value<?>> entry : individual.getObjectives()) {
				Objective objective = entry.getKey();
				double value = toMinProblem(entry.getKey(), entry.getValue());
				if (minValues.get(objective) == null || value < minValues.get(objective)) {
					minValues.put(objective, value);
                    changed = true;
				}
				if (maxValues.get(objective) == null || value > maxValues.get(objective)) {
					maxValues.put(objective, value);
                    changed = true;
				}
			}
            // Invalidate energies cache.
            if (changed && cache != null) cache.invalidate();
		}
	}

	/**
	 * Transforms the {@link Objective} to a minimization objective, i.e., if
	 * the given objective is to be maximized, the negation of the given value
	 * is returned.
	 *
	 * @param objective
	 *            the respective objective
	 * @param value
	 *            the value to transform
	 * @return the corresponding double value
	 */
	public static final double toMinProblem(Objective objective, Value<?> value) {
		Double v = value.getDouble();

		Double result = null;
		if (v == null) {
			result = Double.MAX_VALUE;
		} else if (objective.getSign() == MAX) {
			result = -v;
		} else {
			result = v;
		}

		assert result != null;
		return result;
	}
}

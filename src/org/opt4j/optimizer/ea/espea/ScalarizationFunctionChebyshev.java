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

import org.opt4j.core.Objectives;

/**
 * Implementation of the Chebyshev method for scalarization as described in the
 * {@link ESPEA} paper. The utopia point is 0.
 * 
 * @author luisgerhorst
 */
public class ScalarizationFunctionChebyshev extends ScalarizationFunction {

    protected double calculate(final Objectives objectives) {
        final double[] array = objectives.array();
        double max = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < array.length; i++) {
            // Normally we'd have to substract utopiaPoint[i] from array[i] but
            // since we're using 0 as the utopia point we can leave that out.
            max = Math.max(max, array[i]);
        }
        return max;
    }

}

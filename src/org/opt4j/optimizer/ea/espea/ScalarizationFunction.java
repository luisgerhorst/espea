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
 * Represents a scalarization function for <code>Objectives</code>, denoted as
 * W(y) in the {@link ESPEA} paper. When subclassed, the method
 * <code>calculate</code> must be implemented and calls should return a
 * positive, non-zero value that determines the focus put on a specific area in
 * the objective space. The lower the returned value, the greater the preference
 * since such individuals are more likely to enter the archive.
 * <p>
 *
 * Note that some scalarization functions such as the Nash Bargaining Solution
 * or Proper Utility require a approximation of the paretor front. If the
 * algorithm is used with such a scalarization function it is not recommended to
 * use the current archive as approximation since this introduces a
 * circular dependency and also the algorithm may no longer be evolutionary
 * stable, especially for small archive sizes. Instead it is recommened to use
 * the result of a previous optimizer run as approximation. The run of this
 * optimizer with the appropriate scalarization function will then lead to
 * refined results for the preferred areas of the objective space. The {@link
 * EnergyArchive} does not guarantee that scalarization function values are
 * recomputed when it's contents change.
 *
 * @author luisgerhorst
 */
public abstract class ScalarizationFunction {

    /**
     * For optimization purposes this method may be overridden but otherwise it
     * is preferred if <code>calculate(Objectives)</code> is implemented
     * instead.
     *
     * @param o1 the objectives of the first individual
     * @param o2 the objectives of the second individual
     * @return the product of the charges of the two objectives
     */
    public double calculate(final Objectives o1, final Objectives o2) {
        return calculate(o1) * calculate(o2);
    }

    /**
     * This method is called by <code>calculate(Objectives, Objectives)</code>
     * for each of the two <code>Objectives</code>. It shall be implemented by
     * the subclass.
     *
     * @param objectives the <code>Objectives</code> to be scalarized
     * @return the charge of the <code>Individual</code> with the supplied
     * <code>Objectives</code>, must be greater than 0
     */
    protected abstract double calculate(Objectives objectives);

}

/* Copyright 2010-2011 Centre National d'Études Spatiales
 * Licensed to CS Communication & Systèmes (CS) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * CS licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.orekit.propagation.numerical;


/** This class is a container for additional state parameters and their associated evolution equation.
*
* <p>
* This object is a container allowing the propagator to keep constant consistency between additional
* states and the corresponding equations. It allows to set additional state values, get current
* additional state value and by reference on the associated additional equations.
* </p>
* @see NumericalPropagator
* @see AdditionalEquations
* @author Luc Maisonobe
* @version $Revision: 3409 $ $Date: 2010-06-10 15:42:04 +0200 (jeu., 10 juin 2010) $
*/
public class AdditionalStateAndEquations {

    /** Additional equations. */
    private AdditionalEquations addEquations;

    /** Current additional state. */
    private double[] addState;

    /** Current additional state derivatives. */
    private double[] addStateDot;

    /** Create a new instance of AdditionalStateAndEquations, based on additional equations definition.
     * @param addEqu additional equations.
     */
    public AdditionalStateAndEquations(final AdditionalEquations addEqu) {
        this.addEquations = addEqu;
    }

    /** Get a reference to the current value of the additional state.
     * <p>The array returned is a true reference to the state array, so it may be used
     * to store data into it.</>
     * @return a reference current value of the addditional state.
     */
    public double[] getAdditionalState() {
        return addState;
    }

    /** Get a reference to the current value of the additional state derivatives.
     * <p>The array returned is a true reference to the state array, so it may be used
     * to store data into it.</>
     * @return a reference current value of the addditional state derivatives.
     */
    public double[] getAdditionalStateDot() {
        return addStateDot;
    }

    /** Gets the instance of additional equations.
     * @return current value of the additional equations.
     */
    public AdditionalEquations getAdditionalEquations() {
        return addEquations;
    }

    /** Sets a value to additional state.
     * @param state additional state value.
     */
    public void setAdditionalState(final double[] state) {
        this.addState = state.clone();
        this.addStateDot = new double[state.length];
    }
}

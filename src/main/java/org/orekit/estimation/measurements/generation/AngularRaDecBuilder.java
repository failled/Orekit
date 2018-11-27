/* Copyright 2002-2018 CS Systèmes d'Information
 * Licensed to CS Systèmes d'Information (CS) under one or more
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
package org.orekit.estimation.measurements.generation;

import org.hipparchus.random.CorrelatedRandomVectorGenerator;
import org.orekit.estimation.measurements.AngularRaDec;
import org.orekit.estimation.measurements.GroundStation;
import org.orekit.frames.Frame;
import org.orekit.propagation.SpacecraftState;


/** Builder for {@link AngularRaDec} measurements.
 * @author Luc Maisonobe
 * @since 9.3
 */
public class AngularRaDecBuilder extends AbstractMeasurementBuilder<AngularRaDec> {

    /** Ground station from which measurement is performed. */
    private final GroundStation station;

    /** Reference frame in which the right ascension - declination angles are given. */
    private final Frame referenceFrame;

    /** Simmple constructor.
     * @param noiseSource noise source, may be null for generating perfect measurements
     * @param station ground station from which measurement is performed
     * @param referenceFrame Reference frame in which the right ascension - declination angles are given
     * @param sigma theoretical standard deviation
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     */
    public AngularRaDecBuilder(final CorrelatedRandomVectorGenerator noiseSource,
                               final GroundStation station, final Frame referenceFrame,
                               final double[] sigma, final double[] baseWeight,
                               final int propagatorIndex) {
        super(noiseSource, sigma, baseWeight, propagatorIndex);
        this.station        = station;
        this.referenceFrame = referenceFrame;
    }

    /** {@inheritDoc} */
    @Override
    public AngularRaDec build(final SpacecraftState... states) {

        final int propagatorIndex   = getPropagatorsIndices()[0];
        final double[] sigma        = getTheoreticalStandardDeviation();
        final double[] baseWeight   = getBaseWeight();
        final SpacecraftState state = states[propagatorIndex];

        // create a dummy measurement
        final AngularRaDec dummy = new AngularRaDec(station, referenceFrame, state.getDate(),
                                                    new double[] {
                                                        Double.NaN, Double.NaN
                                                    }, sigma, baseWeight, propagatorIndex);

        // estimate the perfect value of the measurement
        final double[] angular = dummy.estimate(0, 0, states).getEstimatedValue();

        // add the noise
        final double[] noise = getNoise();
        if (noise != null) {
            angular[0] += noise[0];
            angular[1] += noise[1];
        }

        // generate measurement
        return new AngularRaDec(station, referenceFrame, state.getDate(), angular, sigma, baseWeight, propagatorIndex);

    }

}
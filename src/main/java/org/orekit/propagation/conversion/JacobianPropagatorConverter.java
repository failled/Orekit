/* Copyright 2002-2013 CS Systèmes d'Information
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
package org.orekit.propagation.conversion;

import org.apache.commons.math3.analysis.DifferentiableMultivariateVectorFunction;
import org.apache.commons.math3.analysis.MultivariateMatrixFunction;
import org.orekit.errors.OrekitException;
import org.orekit.errors.OrekitExceptionWrapper;
import org.orekit.errors.PropagationException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.JacobiansMapper;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.numerical.PartialDerivativesEquations;
import org.orekit.propagation.sampling.OrekitStepHandler;
import org.orekit.propagation.sampling.OrekitStepInterpolator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

/** Propagator converter using the real jacobian.
 * @author Pascal Parraud
 * @since 6.0
 */
public class JacobianPropagatorConverter extends AbstractPropagatorConverter {

    /** Function computing position/velocity at sample points. */
    private final ObjectiveFunction objectiveFunction;

    /** Numerical propagator builder. */
    private final NumericalPropagatorBuilder builder;

    /** Simple constructor.
     * @param builder builder for adapted propagator
     * @param threshold absolute threshold for optimization algorithm
     * @param maxIterations maximum number of iterations for fitting
     */
    public JacobianPropagatorConverter(final NumericalPropagatorBuilder builder,
                                       final double threshold,
                                       final int maxIterations) {
        super(builder, threshold, maxIterations);
        this.builder = builder;
        this.objectiveFunction = new ObjectiveFunction();
    }

    /** {@inheritDoc} */
    protected DifferentiableMultivariateVectorFunction getObjectiveFunction() {
        return objectiveFunction;
    }

    /** Internal class for computing position/velocity at sample points. */
    private class ObjectiveFunction implements DifferentiableMultivariateVectorFunction {

        /** {@inheritDoc} */
        public double[] value(final double[] arg)
            throws IllegalArgumentException, OrekitExceptionWrapper {
            try {
                final double[] eval = new double[getTargetSize()];

                final NumericalPropagator prop = builder.buildPropagator(getDate(), arg);

                int k = 0;
                for (SpacecraftState state : getSample()) {
                    final PVCoordinates pv = prop.getPVCoordinates(state.getDate(), getFrame());
                    eval[k++] = pv.getPosition().getX();
                    eval[k++] = pv.getPosition().getY();
                    eval[k++] = pv.getPosition().getZ();
                    if (!isOnlyPosition()) {
                        eval[k++] = pv.getVelocity().getX();
                        eval[k++] = pv.getVelocity().getY();
                        eval[k++] = pv.getVelocity().getZ();
                    }
                }

                return eval;

            } catch (OrekitException ex) {
                throw new OrekitExceptionWrapper(ex);
            }
        }

        /** {@inheritDoc} */
        public MultivariateMatrixFunction jacobian() {
            return new MultivariateMatrixFunction() {

                /** {@inheritDoc} */
                public double[][] value(final double[] arg)
                    throws IllegalArgumentException, OrekitExceptionWrapper {
                    try {
                        final double[][] jacob = new double[getTargetSize()][arg.length];

                        final NumericalPropagator prop  = builder.buildPropagator(getDate(), arg);
                        final int stateSize = isOnlyPosition() ? 3 : 6;
                        final int paramSize = getFreeParameters().size();
                        final PartialDerivativesEquations pde = new PartialDerivativesEquations("pde", prop);
                        pde.selectParameters(getFreeParameters().toArray(new String[0]));
                        pde.setInitialJacobians(prop.getInitialState(), stateSize, paramSize);
                        final JacobiansMapper mapper  = pde.getMapper();
                        final JacobianHandler handler = new JacobianHandler(mapper);
                        prop.setMasterMode(handler);

                        int i = 0;
                        for (SpacecraftState state : getSample()) {
                            prop.propagate(state.getDate());
                            final double[][] dYdY0 = handler.getdYdY0();
                            final double[][] dYdP  = handler.getdYdP();
                            for (int k = 0; k < stateSize; k++, i++) {
                                System.arraycopy(dYdY0[k], 0, jacob[i], 0, stateSize);
                                System.arraycopy(dYdP[k], 0, jacob[i], stateSize, paramSize);
                            }
                        }

                        return jacob;

                    } catch (OrekitException ex) {
                        throw new OrekitExceptionWrapper(ex);
                    }
                }

            };
        }

    }

    /** Internal class for jacobians handling. */
    private static class JacobianHandler implements OrekitStepHandler {

        /** Serial Version UID. */
        private static final long serialVersionUID = 8040284226089555027L;

        /** Jacobians mapper. */
        private transient JacobiansMapper mapper;

        /** Jacobian with respect to state. */
        private final double[][] dYdY0;

        /** Jacobian with respect to parameters. */
        private final double[][] dYdP;

        /** Simple constructor.
         * @param mapper Jacobians mapper
         */
        public JacobianHandler(final JacobiansMapper mapper) {
            this.mapper = mapper;
            this.dYdY0  = new double[mapper.getStateDimension()][mapper.getStateDimension()];
            this.dYdP   = new double[mapper.getStateDimension()][mapper.getParameters()];
        }

        /** Get the jacobian with respect to state.
         * @return jacobian with respect to state
         */
        public double[][] getdYdY0() {
            return dYdY0;
        }

        /** Get the jacobian with respect to parameters.
         * @return jacobian with respect to parameters
         */
        public double[][] getdYdP() {
            return dYdP;
        }

        /** {@inheritDoc} */
        public void init(final SpacecraftState s0, final AbsoluteDate t) {
        }

        /** {@inheritDoc} */
        public void handleStep(final OrekitStepInterpolator interpolator, final boolean isLast)
            throws PropagationException {
            try {
                // we want the Jacobians at the end of last step
                if (isLast) {
                    interpolator.setInterpolatedDate(interpolator.getCurrentDate());
                    final SpacecraftState state = interpolator.getInterpolatedState();
                    final double[] p = interpolator.getInterpolatedAdditionalState(mapper.getName());
                    mapper.getStateJacobian(state, p, dYdY0);
                    mapper.getParametersJacobian(state, p, dYdP);
                }
            } catch (PropagationException pe) {
                throw pe;
            } catch (OrekitException oe) {
                throw new PropagationException(oe);
            }
        }

    }

}

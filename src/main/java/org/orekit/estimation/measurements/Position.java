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
package org.orekit.estimation.measurements;

import java.util.Arrays;

import org.hipparchus.exception.LocalizedCoreFormats;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.FastMath;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.TimeStampedPVCoordinates;

/** Class modeling a position only measurement.
 * <p>
 * For position-velocity measurement see {@link PV}.
 * </p>
 * @see PV
 * @author Luc Maisonobe
 * @since 9.3
 */
public class Position extends AbstractMeasurement<Position> {

    /** Identity matrix, for states derivatives. */
    private static final double[][] IDENTITY = new double[][] {
        {
            1, 0, 0, 0, 0, 0
        }, {
            0, 1, 0, 0, 0, 0
        }, {
            0, 0, 1, 0, 0, 0
        }
    };

    /** Covariance matrix of the PV measurement (size 3x3). */
    private final double[][] covarianceMatrix;

    /** Constructor with one double for the standard deviation.
     * <p>The double is the position's standard deviation, common to the 3 position's components.</p>
     * <p>
     * The measurement must be in the orbit propagation frame.
     * </p>
     * <p>This constructor uses 0 as the index of the propagator related
     * to this measurement, thus being well suited for mono-satellite
     * orbit determination.</p>
     * @param date date of the measurement
     * @param position position
     * @param sigmaPosition theoretical standard deviation on position components
     * @param baseWeight base weight
     */
    public Position(final AbsoluteDate date, final Vector3D position,
                    final double sigmaPosition, final double baseWeight) {
        this(date, position, sigmaPosition, baseWeight, 0);
    }

    /** Constructor with one double for the standard deviation.
     * <p>The double is the position's standard deviation, common to the 3 position's components.</p>
     * <p>
     * The measurement must be in the orbit propagation frame.
     * </p>
     * @param date date of the measurement
     * @param position position
     * @param sigmaPosition theoretical standard deviation on position components
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     */
    public Position(final AbsoluteDate date, final Vector3D position,
                    final double sigmaPosition, final double baseWeight,
                    final int propagatorIndex) {
        this(date, position,
             new double[] {
                 sigmaPosition,
                 sigmaPosition,
                 sigmaPosition
             }, baseWeight, propagatorIndex);
    }

    /** Constructor with one vector for the standard deviation and default value for propagator index..
     * <p>The 3-sized vector represents the square root of the diagonal elements of the covariance matrix.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * <p>This constructor uses 0 as the index of the propagator related
     * to this measurement, thus being well suited for mono-satellite
     * orbit determination.</p>
     * @param date date of the measurement
     * @param position position
     * @param sigmaPosition 3-sized vector of the standard deviations of the position
     * @param baseWeight base weight
     */
    public Position(final AbsoluteDate date, final Vector3D position,
                    final double[] sigmaPosition, final double baseWeight) {
        this(date, position, sigmaPosition, baseWeight, 0);
    }

    /** Constructor with one vector for the standard deviation.
     * <p>The 3-sized vector represents the square root of the diagonal elements of the covariance matrix.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param sigmaPosition 3-sized vector of the standard deviations of the position
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     */
    public Position(final AbsoluteDate date, final Vector3D position,
                    final double[] sigmaPosition, final double baseWeight, final int propagatorIndex) {
        this(date, position, buildPvCovarianceMatrix(sigmaPosition), baseWeight, propagatorIndex);
    }

    /**
     * Constructor with a covariance matrix and default value for propagator index.
     * <p>The fact that the covariance matrices are symmetric and positive definite is not checked.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * <p>This constructor uses 0 as the index of the propagator related
     * to this measurement, thus being well suited for mono-satellite
     * orbit determination.</p>
     * @param date date of the measurement
     * @param position position
     * @param positionCovarianceMatrix 3x3 covariance matrix of the position
     * @param baseWeight base weight
     */
    public Position(final AbsoluteDate date, final Vector3D position,
                    final double[][] positionCovarianceMatrix, final double baseWeight) {
        this(date, position, positionCovarianceMatrix, baseWeight, 0);
    }

    /** Constructor with full covariance matrix and all inputs.
     * <p>The fact that the covariance matrix is symmetric and positive definite is not checked.</p>
     * <p>The measurement must be in the orbit propagation frame.</p>
     * @param date date of the measurement
     * @param position position
     * @param covarianceMatrix 6x6 covariance matrix of the PV measurement
     * @param baseWeight base weight
     * @param propagatorIndex index of the propagator related to this measurement
     */
    public Position(final AbsoluteDate date, final Vector3D position,
                    final double[][] covarianceMatrix, final double baseWeight,
                    final int propagatorIndex) {
        super(date,
              new double[] {
                  position.getX(), position.getY(), position.getZ()
              }, extractSigmas(covarianceMatrix),
              new double[] {
                  baseWeight, baseWeight, baseWeight
              }, Arrays.asList(propagatorIndex));
        this.covarianceMatrix = covarianceMatrix;
    }

    /** Get the position.
     * @return position
     */
    public Vector3D getPosition() {
        final double[] pv = getObservedValue();
        return new Vector3D(pv[0], pv[1], pv[2]);
    }

    /** Get the covariance matrix.
     * @return the covariance matrix
     */
    public double[][] getCovarianceMatrix() {
        return covarianceMatrix;
    }

    /** Get the correlation coefficients matrix.
     * <br>This is the 3x3 matrix M such that:</br>
     * <br>Mij = Pij/(σi.σj)</br>
     * <br>Where: <ul>
     * <li> P is the covariance matrix
     * <li> σi is the i-th standard deviation (σi² = Pii)
     * </ul>
     * @return the correlation coefficient matrix (3x3)
     */
    public double[][] getCorrelationCoefficientsMatrix() {

        // Get the standard deviations
        final double[] sigmas = getTheoreticalStandardDeviation();

        // Initialize the correlation coefficients matric to the covariance matrix
        final double[][] corrCoefMatrix = new double[sigmas.length][sigmas.length];

        // Divide by the standard deviations
        for (int i = 0; i < sigmas.length; i++) {
            for (int j = 0; j < sigmas.length; j++) {
                corrCoefMatrix[i][j] = covarianceMatrix[i][j] / (sigmas[i] * sigmas[j]);
            }
        }
        return corrCoefMatrix;
    }

    /** {@inheritDoc} */
    @Override
    protected EstimatedMeasurement<Position> theoreticalEvaluation(final int iteration, final int evaluation,
                                                                   final SpacecraftState[] states) {

        // PV value
        final TimeStampedPVCoordinates pv = states[getPropagatorsIndices().get(0)].getPVCoordinates();

        // prepare the evaluation
        final EstimatedMeasurement<Position> estimated =
                        new EstimatedMeasurement<>(this, iteration, evaluation, states,
                                                   new TimeStampedPVCoordinates[] {
                                                       pv
                                                   });

        estimated.setEstimatedValue(new double[] {
            pv.getPosition().getX(), pv.getPosition().getY(), pv.getPosition().getZ()
        });

        // partial derivatives with respect to state
        estimated.setStateDerivatives(0, IDENTITY);

        return estimated;
    }

    /** Extract standard deviations from a 3x3 position covariance matrix.
     * Check the size of the position covariance matrix first.
     * @param pCovarianceMatrix the 3x" possition covariance matrix
     * @return the standard deviations (3-sized vector), they are
     * the square roots of the diagonal elements of the covariance matrix in input.
     */
    private static double[] extractSigmas(final double[][] pCovarianceMatrix) {

        // Check the size of the covariance matrix, should be 3x3
        if (pCovarianceMatrix.length != 3 || pCovarianceMatrix[0].length != 3) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH_2x2,
                                      pCovarianceMatrix.length, pCovarianceMatrix[0],
                                      3, 3);
        }

        // Extract the standard deviations (square roots of the diagonal elements)
        final double[] sigmas = new double[3];
        for (int i = 0; i < sigmas.length; i++) {
            sigmas[i] = FastMath.sqrt(pCovarianceMatrix[i][i]);
        }
        return sigmas;
    }

    /** Build a 3x3 position covariance matrix from a 3-sized vector (position standard deviations).
     * Check the size of the vector first.
     * @param sigmaP 3-sized vector with position standard deviations
     * @return the 3x3 position covariance matrix
     */
    private static double[][] buildPvCovarianceMatrix(final double[] sigmaP) {
        // Check the size of the vector first
        if (sigmaP.length != 3) {
            throw new OrekitException(LocalizedCoreFormats.DIMENSIONS_MISMATCH, sigmaP.length, 3);

        }

        // Build the 3x3 position covariance matrix
        final double[][] pvCovarianceMatrix = new double[3][3];
        for (int i = 0; i < sigmaP.length; i++) {
            pvCovarianceMatrix[i][i] =  sigmaP[i] * sigmaP[i];
        }
        return pvCovarianceMatrix;
    }

}

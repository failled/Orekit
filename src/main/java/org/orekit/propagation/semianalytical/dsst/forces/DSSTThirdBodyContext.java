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
package org.orekit.propagation.semianalytical.dsst.forces;

import java.util.TreeMap;

import org.hipparchus.analysis.differentiation.DSFactory;
import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.util.CombinatoricsUtils;
import org.hipparchus.util.FastMath;
import org.orekit.bodies.CelestialBody;
import org.orekit.errors.OrekitException;
import org.orekit.propagation.semianalytical.dsst.utilities.AuxiliaryElements;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory;
import org.orekit.propagation.semianalytical.dsst.utilities.CoefficientsFactory.NSKey;
import org.orekit.propagation.semianalytical.dsst.utilities.UpperBounds;
import org.orekit.propagation.semianalytical.dsst.utilities.hansen.HansenThirdBodyLinear;

/** This class is a container for the attributes of
 * {@link org.orekit.propagation.semianalytical.dsst.forces.DSSTThirdBody DSSTThirdBody}.
 * <p>
 * It replaces the last version of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initializeStep(AuxiliaryElements)
 * initializeStep(AuxiliaryElements)} and a part of the method
 * {@link  org.orekit.propagation.semianalytical.dsst.forces.DSSTForceModel#initialize(AuxiliaryElements, boolean)
 * initialize(AuxiliaryElements, boolean)}.
 * </p>
 * <p>
 * This class contains only getter methods
 * </p>
 */
class DSSTThirdBodyContext extends ForceModelContext {

    /** Max power for summation. */
    private static final int    MAX_POWER = 22;

    /** Truncation tolerance for big, eccentric  orbits. */
    private static final double BIG_TRUNCATION_TOLERANCE = 1.e-1;

    /** Truncation tolerance for small orbits. */
    private static final double SMALL_TRUNCATION_TOLERANCE = 1.9e-6;

    /** Maximum power for eccentricity used in short periodic computation. */
    private static final int    MAX_ECCPOWER_SP = 4;

    /** Max power for a/R3 in the serie expansion. */
    private int    maxAR3Pow;

    /** Max power for e in the serie expansion. */
    private int    maxEccPow;

    /** a / R3 up to power maxAR3Pow. */
    private double[] aoR3Pow;

    /** Max power for e in the serie expansion (for short periodics). */
    private int    maxEccPowShort;

    /** Max frequency of F. */
    private int    maxFreqF;

    /** Qns coefficients. */
    private double[][] Qns;

    /** Standard gravitational parameter μ for the body in m³/s². */
    private final double           gm;

    /** Distance from center of mass of the central body to the 3rd body. */
    private double R3;

    // Direction cosines of the symmetry axis
    /** α. */
    private double alpha;
    /** β. */
    private double beta;
    /** γ. */
    private double gamma;

    /** B². */
    private double BB;
    /** B³. */
    private double BBB;

    /** &Chi; = 1 / sqrt(1 - e²) = 1 / B. */
    private double X;
    /** &Chi;². */
    private double XX;
    /** &Chi;³. */
    private double XXX;
    /** -2 * a / A. */
    private double m2aoA;
    /** B / A. */
    private double BoA;
    /** 1 / (A * B). */
    private double ooAB;
    /** -C / (2 * A * B). */
    private double mCo2AB;
    /** B / A(1 + B). */
    private double BoABpo;

    /** mu3 / R3. */
    private double muoR3;

    /** b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B).*/
    private double b;

    /** h * &Chi;³. */
    private double hXXX;
    /** k * &Chi;³. */
    private double kXXX;

    /** V<sub>ns</sub> coefficients. */
    private final TreeMap<NSKey, Double> Vns;

    /** An array that contains the objects needed to build the Hansen coefficients. <br/>
     * The index is s */
    private final HansenThirdBodyLinear[] hansenObjects;

    /** Factory for the DerivativeStructure instances. */
    private final DSFactory factory;

    /** Simple constructor.
     * Performs initialization at each integration step for the current force model.
     * This method aims at being called before mean elements rates computation
     * @param auxiliaryElements auxiliary elements related to the current orbit
     * @param thirdBody body the 3rd body to consider
     * @param parameters values of the force model parameters
     * @throws OrekitException if some specific error occurs
     */
    DSSTThirdBodyContext(final AuxiliaryElements auxiliaryElements, final CelestialBody thirdBody, final double[] parameters)
        throws OrekitException {

        super(auxiliaryElements);

        this.gm = parameters[0];
        this.factory = new DSFactory(1, 1);
        this.Vns = CoefficientsFactory.computeVns(MAX_POWER);

        //Initialise the HansenCoefficient generator
        this.hansenObjects = new HansenThirdBodyLinear[MAX_POWER + 1];
        for (int s = 0; s <= MAX_POWER; s++) {
            this.hansenObjects[s] = new HansenThirdBodyLinear(MAX_POWER, s);
        }

        // Distance from center of mass of the central body to the 3rd body
        final Vector3D bodyPos = thirdBody.getPVCoordinates(auxiliaryElements.getDate(), auxiliaryElements.getFrame()).getPosition();
        R3 = bodyPos.getNorm();

        // Direction cosines
        final Vector3D bodyDir = bodyPos.normalize();
        alpha = bodyDir.dotProduct(auxiliaryElements.getVectorF());
        beta  = bodyDir.dotProduct(auxiliaryElements.getVectorG());
        gamma = bodyDir.dotProduct(auxiliaryElements.getVectorW());

        //&Chi;<sup>-2</sup>.
        BB = auxiliaryElements.getB() * auxiliaryElements.getB();
        //&Chi;<sup>-3</sup>.
        BBB = BB * auxiliaryElements.getB();

        //b = 1 / (1 + B)
        b = 1. / (1. + auxiliaryElements.getB());

        // &Chi;
        X = 1. / auxiliaryElements.getB();
        XX = X * X;
        XXX = X * XX;
        // -2 * a / A
        m2aoA = -2. * auxiliaryElements.getSma() / auxiliaryElements.getA();
        // B / A
        BoA = auxiliaryElements.getB() / auxiliaryElements.getA();
        // 1 / AB
        ooAB = 1. / (auxiliaryElements.getA() * auxiliaryElements.getB());
        // -C / 2AB
        mCo2AB = -auxiliaryElements.getC() * ooAB / 2.;
        // B / A(1 + B)
        BoABpo = BoA / (1. + auxiliaryElements.getB());

        // mu3 / R3
        muoR3 = gm / R3;

        //h * &Chi;³
        hXXX = auxiliaryElements.getH() * XXX;
        //k * &Chi;³
        kXXX = auxiliaryElements.getK() * XXX;

        // Truncation tolerance.
        final double aoR3 = auxiliaryElements.getSma() / R3;
        final double tol = ( aoR3 > .3 || (aoR3 > .15  && auxiliaryElements.getEcc() > .25) ) ? BIG_TRUNCATION_TOLERANCE : SMALL_TRUNCATION_TOLERANCE;

        // Utilities for truncation
        // Set a lower bound for eccentricity
        final double eo2  = FastMath.max(0.0025, auxiliaryElements.getEcc() / 2.);
        final double x2o2 = XX / 2.;
        final double[] eccPwr = new double[MAX_POWER];
        final double[] chiPwr = new double[MAX_POWER];
        eccPwr[0] = 1.;
        chiPwr[0] = X;
        for (int i = 1; i < MAX_POWER; i++) {
            eccPwr[i] = eccPwr[i - 1] * eo2;
            chiPwr[i] = chiPwr[i - 1] * x2o2;
        }

        // Auxiliary quantities.
        final double ao2rxx = aoR3 / (2. * XX);
        double xmuarn       = ao2rxx * ao2rxx * gm / (X * R3);
        double term         = 0.;

        // Compute max power for a/R3 and e.
        maxAR3Pow = 2;
        maxEccPow = 0;
        int n     = 2;
        int m     = 2;
        int nsmd2 = 0;

        do {
            // Upper bound for Tnm.
            term =  xmuarn *
                            (CombinatoricsUtils.factorialDouble(n + m) / (CombinatoricsUtils.factorialDouble(nsmd2) * CombinatoricsUtils.factorialDouble(nsmd2 + m))) *
                            (CombinatoricsUtils.factorialDouble(n + m + 1) / (CombinatoricsUtils.factorialDouble(m) * CombinatoricsUtils.factorialDouble(n + 1))) *
                            (CombinatoricsUtils.factorialDouble(n - m + 1) / CombinatoricsUtils.factorialDouble(n + 1)) *
                            eccPwr[m] * UpperBounds.getDnl(XX, chiPwr[m], n + 2, m);

            if (term < tol) {
                if (m == 0) {
                    break;
                } else if (m < 2) {
                    xmuarn *= ao2rxx;
                    m = 0;
                    n++;
                    nsmd2++;
                } else {
                    m -= 2;
                    nsmd2++;
                }
            } else {
                maxAR3Pow = n;
                maxEccPow = FastMath.max(m, maxEccPow);
                xmuarn *= ao2rxx;
                m++;
                n++;
            }
        } while (n < MAX_POWER);

        maxEccPow = FastMath.min(maxAR3Pow, maxEccPow);

        // allocate the array aoR3Pow
        aoR3Pow = new double[maxAR3Pow + 1];

        aoR3Pow[0] = 1.;
        for (int i = 1; i <= maxAR3Pow; i++) {
            aoR3Pow[i] = aoR3 * aoR3Pow[i - 1];
        }

        maxFreqF = maxAR3Pow + 1;
        maxEccPowShort = MAX_ECCPOWER_SP;

        Qns = CoefficientsFactory.computeQns(gamma, maxAR3Pow, FastMath.max(maxEccPow, maxEccPowShort));

    }

    /** Compute init values for hansen objects.
     * @param B = sqrt(1 - e²).
     * @param element element of the array to compute the init values
     */
    public void computeHansenObjectsInitValues(final double B, final int element) {
        hansenObjects[element].computeInitValues(B, BB, BBB);
    }

    /** Get the Hansen Objects.
     * @return hansenObjects
     */
    public HansenThirdBodyLinear[] getHansenObjects() {
        return hansenObjects;
    }

    /** Get distance from center of mass of the central body to the 3rd body.
     * @return R3
     */
    public double getR3() {
        return R3;
    }

    /** Get direction cosine α for central body.
     * @return α
     */
    public double getAlpha() {
        return alpha;
    }

    /** Get direction cosine β for central body.
     * @return β
     */
    public double getBeta() {
        return beta;
    }

    /** Get direction cosine γ for central body.
     * @return γ
     */
    public double getGamma() {
        return gamma;
    }

    /** Get B².
     * @return B²
     */
    public double getBB() {
        return BB;
    }

    /** Get B³.
     * @return B³
     */
    public double getBBB() {
        return BBB;
    }

    /** Get b = 1 / (1 + sqrt(1 - e²)) = 1 / (1 + B).
     * @return b
     */
    public double getb() {
        return b;
    }

    /** Get &Chi; = 1 / sqrt(1 - e²) = 1 / B.
     * @return &Chi;
     */
    public double getX() {
        return X;
    }

    /** Get &Chi;².
     * @return &Chi;².
     */
    public double getXX() {
        return XX;
    }

    /** Get &Chi;³.
     * @return &Chi;³
     */
    public double getXXX() {
        return XXX;
    }

    /** Get m2aoA = -2 * a / A.
     * @return m2aoA
     */
    public double getM2aoA() {
        return m2aoA;
    }

    /** Get B / A.
     * @return BoA
     */
    public double getBoA() {
        return BoA;
    }

    /** Get ooAB = 1 / (A * B).
     * @return ooAB
     */
    public double getOoAB() {
        return ooAB;
    }

    /** Get mCo2AB = -C / 2AB.
     * @return mCo2AB
     */
    public double getMCo2AB() {
        return mCo2AB;
    }

    /** Get BoABpo = B / A(1 + B).
     * @return BoABpo
     */
    public double getBoABpo() {
        return BoABpo;
    }

    /** Get muoR3 = mu3 / R3.
     * @return muoR3
     */
    public double getMuoR3() {
        return muoR3;
    }

    /** Get hXXX = h * &Chi;³.
     * @return hXXX
     */
    public double getHXXX() {
        return hXXX;
    }

    /** Get kXXX = h * &Chi;³.
     * @return kXXX
     */
    public double getKXXX() {
        return kXXX;
    }

    /** Get standard gravitational parameter μ for the body in m³/s².
     *  @return gm
     */
    public double getGM() {
        return gm;
    }

    /** Get the value of max power for a/R3 in the serie expansion.
     * @return maxAR3Pow
     */
    public int getMaxAR3Pow() {
        return maxAR3Pow;
    }

    /** Get the value of max power for e in the serie expansion.
     * @return maxAR3Pow
     */
    public int getMaxEccPow() {
        return maxEccPow;
    }

    /** Get the value of a / R3 up to power maxAR3Pow.
     * @return aoR3Pow
     */
    public double[] getAoR3Pow() {
        return aoR3Pow;
    }

   /** Get the value of max frequency of F.
     * @return aoR3Pow
     */
    public int getMaxFreqF() {
        return maxFreqF;
    }

    /** Get the value of max power for e in the serie expansion (for short periodics).
     * @return aoR3Pow
     */
    public int getMaxEccPowShort() {
        return maxEccPowShort;
    }

    /** Get the value of Qns coefficients.
     * @return aoR3Pow
     */
    public double[][] getQns() {
        return Qns;
    }

    /** Get the factory for the DerivativeStructure instances.
     * @return factory
     */
    public DSFactory getFactory() {
        return factory;
    }

    /** Get the V<sub>ns</sub> coefficients.
     * @return Vns
     */
    public TreeMap<NSKey, Double> getVns() {
        return Vns;
    }

}
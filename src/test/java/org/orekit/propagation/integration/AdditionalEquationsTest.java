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
package org.orekit.propagation.integration;

import org.hipparchus.geometry.euclidean.threed.Vector3D;
import org.hipparchus.ode.nonstiff.AdaptiveStepsizeIntegrator;
import org.hipparchus.ode.nonstiff.DormandPrince853Integrator;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.orekit.Utils;
import org.orekit.errors.OrekitException;
import org.orekit.forces.gravity.potential.GravityFieldFactory;
import org.orekit.forces.gravity.potential.SHMFormatReader;
import org.orekit.frames.FramesFactory;
import org.orekit.orbits.EquinoctialOrbit;
import org.orekit.orbits.Orbit;
import org.orekit.orbits.OrbitType;
import org.orekit.propagation.SpacecraftState;
import org.orekit.propagation.numerical.NumericalPropagator;
import org.orekit.propagation.semianalytical.dsst.DSSTPropagator;
import org.orekit.time.AbsoluteDate;
import org.orekit.utils.PVCoordinates;

public class AdditionalEquationsTest {
    
    private double                       mu;
    private AbsoluteDate                 initDate;
    private SpacecraftState              initialState;
    private AdaptiveStepsizeIntegrator   integrator;

    /** Test for issue #401 
     *  with a numerical propagator */
    @Test
    public void testInitNumerical() throws OrekitException {
        
        // setup
        InitCheckerEquations checker = new InitCheckerEquations();
        Assert.assertFalse(checker.wasCalled());
        
        // action
        NumericalPropagator propagatorNumerical = new NumericalPropagator(integrator);
        propagatorNumerical.setInitialState(initialState);
        propagatorNumerical.addAdditionalEquations(checker);
        propagatorNumerical.setInitialState(propagatorNumerical.getInitialState().addAdditionalState("linear", 1.5));
        propagatorNumerical.propagate(initDate.shiftedBy(600));
        
        // verify
        Assert.assertTrue(checker.wasCalled());

    }
    
    /** Test for issue #401 
     *  with a DSST propagator */
    @Test
    public void testInitDSST() throws OrekitException {
        
        // setup
        InitCheckerEquations checker = new InitCheckerEquations();
        Assert.assertFalse(checker.wasCalled());
        
        // action
        DSSTPropagator propagatorDSST = new DSSTPropagator(integrator);
        propagatorDSST.setInitialState(initialState);
        propagatorDSST.addAdditionalEquations(checker);
        propagatorDSST.setInitialState(propagatorDSST.getInitialState().addAdditionalState("linear", 1.5));
        propagatorDSST.propagate(initDate.shiftedBy(600));
        
        // verify
        Assert.assertTrue(checker.wasCalled());

    }
    
    @Before
    public void setUp() throws OrekitException {
        Utils.setDataRoot("regular-data:potential/shm-format");
        GravityFieldFactory.addPotentialCoefficientsReader(new SHMFormatReader("^eigen_cg03c_coef$", false));
        mu  = GravityFieldFactory.getUnnormalizedProvider(0, 0).getMu();
        final Vector3D position = new Vector3D(7.0e6, 1.0e6, 4.0e6);
        final Vector3D velocity = new Vector3D(-500.0, 8000.0, 1000.0);
        initDate = AbsoluteDate.J2000_EPOCH;
        final Orbit orbit = new EquinoctialOrbit(new PVCoordinates(position,  velocity),
                                                 FramesFactory.getEME2000(), initDate, mu);
        initialState = new SpacecraftState(orbit);
        double[][] tolerance = NumericalPropagator.tolerances(0.001, orbit, OrbitType.EQUINOCTIAL);
        integrator = new DormandPrince853Integrator(0.001, 200, tolerance[0], tolerance[1]);
        integrator.setInitialStepSize(60);
    }

    @After
    public void tearDown() {
        initDate = null;
        initialState = null;
        integrator = null;
    }
    
    public static class InitCheckerEquations implements AdditionalEquations {
        
        private boolean called;
        
        /** Simple Constructor */
        public InitCheckerEquations() {
            this.called = false;
        }
        
        @Override
        public void init(SpacecraftState initiaState, AbsoluteDate target)
            throws OrekitException {
            called = true;
        }

        @Override
        public double[] computeDerivatives(SpacecraftState s, double[] pDot)
            throws OrekitException {
            pDot[0] = 1.5;
            return new double[7];
        }
        
        @Override
        public String getName() {
            return "linear";
        }

        public boolean wasCalled() {
            return called;
        }

    }
    
}

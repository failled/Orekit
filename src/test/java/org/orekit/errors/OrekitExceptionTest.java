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

package org.orekit.errors;

import org.hipparchus.exception.DummyLocalizable;
import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link OrekitException}.
 *
 * @author Evan Ward
 */
public class OrekitExceptionTest {

    /** Check {@link OrekitException#getMessage()} does not throw a NPE. */
    @Test
    public void testNullString() {
        // action
        OrekitException exception = new OrekitException(new DummyLocalizable(null));

        // verify
        Assert.assertEquals(exception.getMessage(), "");
    }

}

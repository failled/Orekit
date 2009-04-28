/* Copyright 2002-2008 CS Communication & Systèmes
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
package org.orekit.propagation.events;

import org.orekit.errors.OrekitException;
import org.orekit.propagation.SpacecraftState;
import org.orekit.time.AbsoluteDate;

/** Finder for date events.
 * <p>This class finds date events (i.e. occurrence of a predefined date).</p>
 * <p>The default implementation behavior is to {@link EventDetector#STOP
 * stop} propagation at date occurrence. This can be changed by overriding the
 * {@link #eventOccurred(SpacecraftState, boolean) eventOccurred} method in a
 * derived class.</p>
 * @see org.orekit.propagation.Propagator#addEventDetector(EventDetector)
 * @author Luc Maisonobe
 * @version $Revision$ $Date$
 */
public class DateDetector extends AbstractDetector {

    /** Serializable UID. */
    private static final long serialVersionUID = -3885142394082532325L;

    /** Target date. */
    private AbsoluteDate target;

    /** Build a new instance.
     * @param target target date
     */
    public DateDetector(final AbsoluteDate target) {
        super(1.0e9, 1.0e-10);
        this.target = target;
    }

    /** Handle a date event and choose what to do next.
     * <p>The default implementation behavior is to {@link
     * EventDetector#STOP stop} propagation at date occurrence.
     * This can be changed by overriding the {@link
     * #eventOccurred(SpacecraftState, boolean) eventOccurred} method in a derived class.</p>
     * @param s the current state information : date, kinematics, attitude
     * @param increasing if true, the value of the switching function increases
     * when times increases around event (note that increase is measured with respect
     * to physical time, not with respect to propagation which may go backward in time)
     * @return one of {@link #STOP}, {@link #RESET_STATE}, {@link #RESET_DERIVATIVES}
     * or {@link #CONTINUE}
     * @exception OrekitException if some specific error occurs
     */
    public int eventOccurred(final SpacecraftState s, final boolean increasing)
        throws OrekitException {
        return STOP;
    }

    /** {@inheritDoc} */
    public double g(final SpacecraftState s) throws OrekitException {
        return s.getDate().durationFrom(target);
    }

}

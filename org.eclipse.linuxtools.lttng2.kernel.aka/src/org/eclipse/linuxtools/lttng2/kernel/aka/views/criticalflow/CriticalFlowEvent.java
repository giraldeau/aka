/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation (from ControlFlowEvent)
 *******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalflow;

import org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalflow.CriticalFlowPresentationProvider.State;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.TimeEvent;

/**
 * Time Event specific to the control flow view
 */
public class CriticalFlowEvent extends TimeEvent {

    private final State fStatus;

    /**
     * Constructor
     *
     * @param entry
     *            The entry to which this time event is assigned
     * @param time
     *            The timestamp of this event
     * @param duration
     *            The duration of this event
     * @param status
     *            The status assigned to the event
     */
    public CriticalFlowEvent(ITimeGraphEntry entry, long time, long duration,
            State status) {
        super(entry, time, duration);
        fStatus = status;
    }

    /**
     * Get this event's status
     *
     * @return The integer matching this status
     */
    public State getStatus() {
        return fStatus;
    }

    @Override
    public String toString() {
        return "CriticalFlowEvent start=" + fTime + " end=" + (fTime + fDuration) + " duration=" + fDuration + " status=" + fStatus; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    }
}

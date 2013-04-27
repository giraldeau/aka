/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation
 *******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalflow;

import java.util.LinkedHashMap;
import java.util.Map;

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.StateItem;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.TimeGraphPresentationProvider;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;

/**
 * Presentation provider for the control flow view
 */
public class CriticalFlowPresentationProvider extends TimeGraphPresentationProvider {

    public static enum State {
        RUNNING         (new RGB(0x33, 0x99, 0x00)),
        PREEMPTED       (new RGB(0xfd, 0xca, 0x01)),
        TIMER           (new RGB(0x66, 0x99, 0xcc)),
        BLOCK_DEVICE    (new RGB(0x66, 0x00, 0xcc)),
        OTHER           (new RGB(0x66, 0x66, 0x66));

        public final RGB rgb;

        private State (RGB rgb) {
            this.rgb = rgb;
        }
    }

    @Override
    public String getStateTypeName() {
        return Messages.ControlFlowView_stateTypeName;
    }

    @Override
    public StateItem[] getStateTable() {
        StateItem[] stateTable = new StateItem[State.values().length];
        for (int i = 0; i < stateTable.length; i++) {
            State state = State.values()[i];
            stateTable[i] = new StateItem(state.rgb, state.toString());
        }
        return stateTable;
    }

    @Override
    public int getStateTableIndex(ITimeEvent event) {
        if (event instanceof CriticalFlowEvent) {
            State status = ((CriticalFlowEvent) event).getStatus();
            return status.ordinal();
        }
        return TRANSPARENT;
    }

    @Override
    public String getEventName(ITimeEvent event) {
        if (event instanceof CriticalFlowEvent) {
            return ((CriticalFlowEvent) event).getStatus().toString();
        }
        return Messages.CriticalFlowView_multipleStates;
    }

    @Override
    public Map<String, String> getEventHoverToolTipInfo(ITimeEvent event) {
        Map<String, String> retMap = new LinkedHashMap<String, String>();
//        if (event instanceof ControlFlowEvent) {
//            ControlFlowEntry entry = (ControlFlowEntry) event.getEntry();
//            ITmfStateSystem ssq = entry.getTrace().getStateSystem(CtfKernelTrace.STATE_ID);
//            int tid = entry.getThreadId();
//
//            try {
//                //Find every CPU first, then get the current thread
//                int cpusQuark = ssq.getQuarkAbsolute(Attributes.CPUS);
//                List<Integer> cpuQuarks = ssq.getSubAttributes(cpusQuark, false);
//                for (Integer cpuQuark : cpuQuarks) {
//                    int currentThreadQuark = ssq.getQuarkRelative(cpuQuark, Attributes.CURRENT_THREAD);
//                    ITmfStateInterval interval = ssq.querySingleState(event.getTime(), currentThreadQuark);
//                    if (!interval.getStateValue().isNull()) {
//                        ITmfStateValue state = interval.getStateValue();
//                        int currentThreadId = state.unboxInt();
//                        if (tid == currentThreadId) {
//                            retMap.put(Messages.ControlFlowView_attributeCpuName, ssq.getAttributeName(cpuQuark));
//                            break;
//                        }
//                    }
//                }
//
//            } catch (AttributeNotFoundException e) {
//                e.printStackTrace();
//            } catch (TimeRangeException e) {
//                e.printStackTrace();
//            } catch (StateValueTypeException e) {
//                e.printStackTrace();
//            } catch (StateSystemDisposedException e) {
//                /* Ignored */
//            }
//            int status = ((ControlFlowEvent) event).getStatus();
//            if (status == StateValues.PROCESS_STATUS_RUN_SYSCALL) {
//                try {
//                    int syscallQuark = ssq.getQuarkRelative(entry.getThreadQuark(), Attributes.SYSTEM_CALL);
//                    ITmfStateInterval value = ssq.querySingleState(event.getTime(), syscallQuark);
//                    if (!value.getStateValue().isNull()) {
//                        ITmfStateValue state = value.getStateValue();
//                        retMap.put(Messages.ControlFlowView_attributeSyscallName, state.toString());
//                    }
//
//                } catch (AttributeNotFoundException e) {
//                    e.printStackTrace();
//                } catch (TimeRangeException e) {
//                    e.printStackTrace();
//                } catch (StateSystemDisposedException e) {
//                    /* Ignored */
//                }
//            }
//        }

        return retMap;
    }

    @Override
    public void postDrawEvent(ITimeEvent event, Rectangle bounds, GC gc) {
//        if (bounds.width <= gc.getFontMetrics().getAverageCharWidth()) {
//            return;
//        }
//        if (!(event instanceof CriticalFlowEvent)) {
//            return;
//        }
//        CriticalFlowEvent entry = (CriticalFlowEvent) event.getEntry();
//        ITmfStateSystem ss = entry.getTrace().getStateSystem(CtfKernelTrace.STATE_ID);
//        int status = ((CriticalFlowEvent) event).getStatus();
//        if (status != StateValues.PROCESS_STATUS_RUN_SYSCALL) {
//            return;
//        }
//        try {
//            int syscallQuark = ss.getQuarkRelative(entry.getThreadQuark(), Attributes.SYSTEM_CALL);
//            ITmfStateInterval value = ss.querySingleState(event.getTime(), syscallQuark);
//            if (!value.getStateValue().isNull()) {
//                ITmfStateValue state = value.getStateValue();
//                gc.setForeground(gc.getDevice().getSystemColor(SWT.COLOR_WHITE));
//                Utils.drawText(gc, state.toString().substring(4), bounds.x, bounds.y - 2, bounds.width, true, true);
//            }
//        } catch (AttributeNotFoundException e) {
//            e.printStackTrace();
//        } catch (TimeRangeException e) {
//            e.printStackTrace();
//        } catch (StateSystemDisposedException e) {
//            /* Ignored */
//        }
    }

}


/*******************************************************************************
 * Copyright (c) 2013 École Polytechnique de Montréal
 *
 * All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Geneviève Bastien - Initial API and implementation (from ControlFlowEntry)
 *******************************************************************************/

package org.eclipse.linuxtools.lttng2.kernel.aka.views.criticalflow;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeEvent;
import org.eclipse.linuxtools.tmf.ui.widgets.timegraph.model.ITimeGraphEntry;

/**
 * An entry in the Control Flow view
 */
public class CriticalFlowEntry implements ITimeGraphEntry {
    private CriticalFlowEntry fParent = null;
    private final ArrayList<CriticalFlowEntry> fChildren = new ArrayList<CriticalFlowEntry>();
    private String fName;
    private long fStartTime = -1;
    private long fEndTime = -1;
    private List<ITimeEvent> fEventList = new ArrayList<ITimeEvent>();
    private List<ITimeEvent> fZoomedEventList = null;
    private int position;

    /**
     * Constructor
     *
     * @param threadQuark
     *            The attribute quark matching the thread
     * @param trace
     *            The trace on which we are working
     * @param execName
     *            The exec_name of this entry
     * @param threadId
     *            The TID of the thread
     * @param parentThreadId
     *            the Parent_TID of this thread
     * @param startTime
     *            The start time of this process's lifetime
     * @param endTime
     *            The end time of this process
     */
    public CriticalFlowEntry(String taskname, long startTime, long endTime) {
        fName = taskname;
        fStartTime = startTime;
        fEndTime = endTime;
    }

    @Override
    public ITimeGraphEntry getParent() {
        return fParent;
    }

    @Override
    public boolean hasChildren() {
        return fChildren.size() > 0;
    }

    @Override
    public List<CriticalFlowEntry> getChildren() {
        return fChildren;
    }

    @Override
    public String getName() {
        return fName;
    }

    /**
     * Update the entry name
     * @param execName the updated entry name
     */
    public void setName(String execName) {
        fName = execName;
    }

    @Override
    public long getStartTime() {
        return fStartTime;
    }

    @Override
    public long getEndTime() {
        return fEndTime;
    }

    @Override
    public boolean hasTimeEvents() {
        return true;
    }

    @Override
    public Iterator<ITimeEvent> getTimeEventsIterator() {
        return new EventIterator(fEventList, fZoomedEventList);
    }

    @Override
    public Iterator<ITimeEvent> getTimeEventsIterator(long startTime, long stopTime, long visibleDuration) {
        return new EventIterator(fEventList, fZoomedEventList, startTime, stopTime);
    }

    /**
     * Add an event to this process's timeline
     *
     * @param event
     *            The time event
     */
    public void addEvent(ITimeEvent event) {
        long start = event.getTime();
        long end = start + event.getDuration();
        synchronized (fEventList) {
            fEventList.add(event);
            if (fStartTime == -1 || start < fStartTime) {
                fStartTime = start;
            }
            if (fEndTime == -1 || end > fEndTime) {
                fEndTime = end;
            }
        }
    }

    /**
     * Set the general event list of this entry
     *
     * @param eventList
     *            The list of time events
     */
//    public void setEventList(List<ITimeEvent> eventList) {
//        fEventList = eventList;
//    }

    /**
     * Set the zoomed event list of this entry
     *
     * @param eventList
     *            The list of time events
     */
    public void setZoomedEventList(List<ITimeEvent> eventList) {
        fZoomedEventList = eventList;
    }

    /**
     * Add a child entry to this one (to show relationships between processes as
     * a tree)
     *
     * @param child
     *            The child entry
     */
    public void addChild(CriticalFlowEntry child) {
        child.fParent = this;
        fChildren.add(child);
    }

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}
}

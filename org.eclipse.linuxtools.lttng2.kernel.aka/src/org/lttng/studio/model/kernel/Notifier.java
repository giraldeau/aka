package org.lttng.studio.model.kernel;

import java.util.Collection;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

/*
 * Notifier base class largely inspired from SWT.Widget
 */

public class Notifier {

	Multimap<Integer, ModelListener> listeners;

	public Notifier() {
		listeners = HashMultimap.create();
	}

	public void addListener(int eventType, ModelListener listener) {
		listeners.put(eventType, listener);
	}

	public ModelListener[] getListeners(int eventType) {
		Collection<ModelListener> l = listeners.get(eventType);
		ModelListener[] ret = new ModelListener[l.size()];
		return l.toArray(ret);
	}

	public boolean isListening(int eventType) {
		return listeners.containsKey(eventType);
	}

	public void notifyListeners(int eventType, ModelEvent event) {
		for (ModelListener l: listeners.get(eventType)) {
			l.handleEvent(event);
		}
	}

	public void removeListener(int eventType, ModelListener listener) {
		listeners.remove(eventType, listener);
	}

}

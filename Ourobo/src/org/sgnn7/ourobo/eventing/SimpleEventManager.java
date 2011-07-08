package org.sgnn7.ourobo.eventing;

import java.util.ArrayList;
import java.util.List;

public class SimpleEventManager {
	private final List<IChangeEventListener> listeners = new ArrayList<IChangeEventListener>();

	public void notifyManagedListeners() {
		for (IChangeEventListener listener : listeners) {
			listener.handle();
		}
	}

	public void addListenerToManager(IChangeEventListener listener) {
		listeners.add(listener);
	}
}

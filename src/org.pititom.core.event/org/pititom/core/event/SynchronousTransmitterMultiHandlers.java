package org.pititom.core.event;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 *
 * @author Thomas Pérennou
 */
class SynchronousTransmitterMultiHandlers<Source, Event, Data> implements RegisterableTransmitter<Source, Event, Data> {
	
	private final Map<Event, Set<Handler<Source, Event, Data>>> eventHandlerMap;

	public SynchronousTransmitterMultiHandlers() {
		this.eventHandlerMap = new HashMap<Event, Set<Handler<Source, Event, Data>>>();
	}

	@Override
	public void addEventHandler(Handler<Source, Event, Data> eventHandler, Event... eventList) {
		synchronized (this.eventHandlerMap) {
			for (Event event : eventList) {
				Set<Handler<Source, Event, Data>> set = this.eventHandlerMap.get(event);
				if (set == null) {
					set = new HashSet<Handler<Source, Event, Data>>();
					this.eventHandlerMap.put(event, set);
				}
				set.add(eventHandler);
			}
		}
	}
	
	@Override
	public void removeEventHandler(Handler<Source, Event, Data> eventHandler, Event... eventList) {
		synchronized (this.eventHandlerMap) {
			for (Event event : eventList) {
				final Set<Handler<Source, Event, Data>> set = this.eventHandlerMap.get(event);
				if (set != null) {
					set.remove(eventHandler);
				}
			}
		}
	}
	

	@Override
	public void transmit(Source source, Event event, Data data) {
		for (Handler<Source, Event, Data> eventHandler : this.getEventHandlers(event)) {
			eventHandler.handleEvent(source, event, data);
		}
	}
	
	@SuppressWarnings("unchecked")
	private Handler<Source, Event, Data>[] getEventHandlers(Event event) {
		synchronized (this.eventHandlerMap) {
			Set<Handler<Source, Event, Data>> eventHandlers = this.eventHandlerMap.get(event);
			return eventHandlers == null ? new Handler[0] : eventHandlers.toArray(new Handler[eventHandlers.size()]);
		}
	}
}

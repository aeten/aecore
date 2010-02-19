package org.pititom.core.messenger.stream.provider;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.kohsuke.args4j.CmdLineException;
import org.pititom.core.Configurable;
import org.pititom.core.ConfigurationException;
import org.pititom.core.event.Handler;
import org.pititom.core.messenger.MessengerEvent;
import org.pititom.core.messenger.MessengerEventData;
import org.pititom.core.messenger.service.Messenger;
import org.pititom.core.messenger.stream.MessengerObjectInputStream;
import org.pititom.core.messenger.stream.MessengerObjectOutputStream;
import org.pititom.core.stream.editor.StreamControllerConnection;
import org.pititom.core.stream.editor.StreamControllerConfiguration;

/**
 * 
 * @author Thomas Pérennou
 */
public class StreamEditorMessenger<Message, Acknowledge extends Enum<?>> implements
		Messenger<Message, Acknowledge>, Configurable {

	// TODO : make it configurable
	private String name;
	private String hookConfiguration;
	private StreamControllerConfiguration emissionConfiguration;
	private StreamControllerConfiguration[] receptionConfigurationList;
	private StreamControllerConnection receptionConnectionList[];
	private Map<Handler<Messenger<Message, Acknowledge>, MessengerEvent, MessengerEventData<Message, Acknowledge>>, Set<MessengerEvent>> eventHandlers;

	private StreamControllerConnection emissionConnection;

	private StreamMessenger<Message, Acknowledge> messenger = null;

	public StreamEditorMessenger(String name, boolean autoConnect, String hookConfiguration, String emissionConfiguration, String... receptionConfigurationList) throws ConfigurationException, IOException {
		this(name, hookConfiguration, emissionConfiguration, receptionConfigurationList);
		if (autoConnect) {
			this.connect();
		}
	}
	public StreamEditorMessenger() {
		
	}

	public StreamEditorMessenger(String name, String hookConfiguration, String emissionConfiguration, String... receptionConfigurationList) throws ConfigurationException {
		this.name = name;
		this.hookConfiguration = hookConfiguration;

		this.eventHandlers = new HashMap<Handler<Messenger<Message, Acknowledge>, MessengerEvent, MessengerEventData<Message, Acknowledge>>, Set<MessengerEvent>>();

		try {
			this.emissionConfiguration = new StreamControllerConfiguration(emissionConfiguration);
		} catch (CmdLineException exception) {
			throw new ConfigurationException(emissionConfiguration, exception);
		}

		this.receptionConfigurationList = new StreamControllerConfiguration[receptionConfigurationList.length];
		this.receptionConnectionList = new StreamControllerConnection[receptionConfigurationList.length];
		for (int i = 0; i < receptionConfigurationList.length; i++) {
			try {
				this.receptionConfigurationList[i] = new StreamControllerConfiguration(receptionConfigurationList[i]);
			} catch (CmdLineException exception) {
				throw new ConfigurationException(receptionConfigurationList[i], exception);
			}
		}
	}

	@Override
	public void transmit(Message message) {
		if (this.isConnected()) {
			this.messenger.transmit(message);
		}
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public void addEventHandler(Handler<Messenger<Message, Acknowledge>, MessengerEvent, MessengerEventData<Message, Acknowledge>> eventHandler, MessengerEvent... eventList) {
		synchronized (this.eventHandlers) {
			Set<MessengerEvent> set = this.eventHandlers.get(eventHandler);
			if (set == null) {
				set = new HashSet<MessengerEvent>();
				this.eventHandlers.put(eventHandler, set);
			}
			for (MessengerEvent event : eventList) {
				set.add(event);
			}
		}
		if (this.messenger != null) {
			this.messenger.addEventHandler(eventHandler, eventList);
		}
	}

	@Override
	public void removeEventHandler(Handler<Messenger<Message, Acknowledge>, MessengerEvent, MessengerEventData<Message, Acknowledge>> eventHandler, MessengerEvent... eventList) {
		synchronized (this.eventHandlers) {
			Set<MessengerEvent> set = this.eventHandlers.get(eventHandler);
			if (set != null) {
				for (MessengerEvent event : eventList) {
					set.remove(event);
				}
				if (set.size() == 0) {
					this.eventHandlers.remove(eventHandler);
				}
			}
		}
		if (this.messenger != null) {
			this.messenger.removeEventHandler(eventHandler, eventList);
		}
	}

	@Override
	public void connect() throws IOException {
		if (this.messenger != null) {
			return;
		}
		try {
			final ObjectInputStream[] inputStreamList = new ObjectInputStream[this.receptionConfigurationList.length];
			for (int i = 0; i < this.receptionConfigurationList.length; i++) {
				if ((this.receptionConfigurationList[i].getEditorStack() == null) && (this.receptionConfigurationList[i].getInputStream() instanceof ObjectInputStream)) {
					inputStreamList[i] = (ObjectInputStream) this.receptionConfigurationList[i].getInputStream();
				} else {
					PipedOutputStream pipedOut = new PipedOutputStream();
					this.receptionConnectionList[i] = new StreamControllerConnection(this.receptionConfigurationList[i], pipedOut);
					inputStreamList[i] = new MessengerObjectInputStream(new PipedInputStream(pipedOut));

					this.receptionConnectionList[i].connect();
				}
			}

			ObjectOutputStream emissionStream;
			if ((this.emissionConfiguration.getEditorStack() == null) && (this.emissionConfiguration.getOutputStream() instanceof ObjectOutputStream)) {
				emissionStream = (ObjectOutputStream) this.emissionConfiguration.getOutputStream();
			} else {
				final PipedInputStream pipedIn = new PipedInputStream();
				this.emissionConnection = new StreamControllerConnection(this.emissionConfiguration, pipedIn);
				emissionStream = new MessengerObjectOutputStream(new PipedOutputStream(pipedIn));
				this.emissionConnection.connect();

			}
			this.messenger = new StreamMessenger<Message, Acknowledge>(this.name, this.hookConfiguration, emissionStream, inputStreamList);
			synchronized (this.eventHandlers) {
				for (Map.Entry<Handler<Messenger<Message, Acknowledge>, MessengerEvent, MessengerEventData<Message, Acknowledge>>, Set<MessengerEvent>> eventEntry : this.eventHandlers.entrySet()) {
					for (MessengerEvent event : eventEntry.getValue()) {
						this.messenger.addEventHandler(eventEntry.getKey(), event);
					}
				}
			}

		} catch (ConfigurationException exception) {
			throw new IOException(exception);
		}

	}

	@Override
	public void disconnect() throws IOException {
		if ((this.messenger == null) || !this.messenger.isConnected()) {
			return;
		}
		this.messenger.disconnect();
		this.messenger = null;
		for (StreamControllerConnection receptionConnection : this.receptionConnectionList) {
			receptionConnection.disconnect();
		}
		this.emissionConnection.disconnect();
	}

	@Override
	public boolean isConnected() {
		return (this.messenger != null) && this.messenger.isConnected();
	}

	public void configure(String configuration) throws ConfigurationException {
		throw new UnsupportedOperationException("Not supported yet.");
		// TODO
	}

}
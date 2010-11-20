package org.pititom.core.messenger.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.pititom.core.ConfigurationException;
import org.pititom.core.Format;
import org.pititom.core.logging.LogLevel;
import org.pititom.core.logging.Logger;
import org.pititom.core.messenger.MessengerEventData;
import org.pititom.core.messenger.Receiver;
import org.pititom.core.service.Provider;
import org.pititom.core.stream.args4j.InputStreamOptionHandler;

@Provider(Receiver.class)
@Format("args")
public class StreamReceiver<Message> extends Receiver.Helper<Message> {
	static {
		CmdLineParser.registerHandler(InputStream.class, InputStreamOptionHandler.class);
	}
	
	@Option(name = "-is", aliases = "--input-stream", required = true)
	private InputStream inputStream = null;

	/** @deprecated Reserved to configuration building */
	@Deprecated
    public StreamReceiver() {}

	public StreamReceiver(String identifier, ObjectInputStream inputStream) {
		super(identifier);
		this.inputStream = inputStream;
	}

	@Override
	protected void doConnect() throws IOException {
		if (this.configuration != null) {
			try {
				this.configure(this.configuration);
			} catch (ConfigurationException e) {
				throw new IOException(e);
			}
		}
	}

	@Override
	protected void doDisconnect() throws IOException {
		this.inputStream.close();
	}

	@Override
	public void configure(String conf) throws ConfigurationException {
		super.configure(conf);
		this.connected = true;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void receive(MessengerEventData<Message> data) throws IOException {
		try {
			Message message = (Message) ((ObjectInputStream) this.inputStream).readObject();
			data.setMessage(message);
		} catch (Throwable exception) {
			if (this.inputStream.markSupported()) {
				Logger.log(this, LogLevel.ERROR, this.getIdentifier() + " has not been able to read object. Trying to reset the stream…", exception);
				this.inputStream.reset();
				this.receive(data);
			}
			throw new IOException(exception);
		}
	}
}

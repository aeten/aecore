package org.pititom.core.stream;

import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import org.pititom.core.Configurable;
import org.pititom.core.ConfigurationException;

/**
 *
 * @author Thomas Pérennou
 */
public class UdpIpOutputStream extends OutputStream implements Configurable {
	private UdpIpParameters parameters;
	private ByteBuffer buffer;

	public UdpIpOutputStream(UdpIpParameters parameters) {
	    this.parameters = parameters;
	    this.buffer = ByteBuffer.allocate(this.parameters.getMaxPacketSize());
	}
	
	public UdpIpOutputStream(InetSocketAddress destinationInetSocketAddress,
	        InetAddress sourceInetAddress, boolean autoBind, boolean reuse, int maxPacketSize)
	        throws IOException {
	   this(new UdpIpParameters(destinationInetSocketAddress,
	                            sourceInetAddress, autoBind, reuse, maxPacketSize));
		this.buffer = ByteBuffer.allocate(this.parameters.getMaxPacketSize());
	}

	public UdpIpOutputStream() {
		this.parameters = null;
		this.buffer = null;
	}

	@Override
	public void write(int b) throws IOException {
		if (this.buffer == null)
			throw new IOException("Stream must be configured");
		if (this.buffer.position() == this.buffer.capacity())
			this.flush();
		this.buffer.put((byte) b);
	}

	@Override
	public void write(byte[] data, int offset, int length) throws IOException {
		for (int i=offset; i<length ; i++)
			this.write(data[i]);
	}

	@Override
	public void flush() throws IOException {
		this.parameters.getSocket().send(
		        new DatagramPacket(this.buffer.array(), this.buffer.position(),
		                this.parameters.getDestinationInetSocketAddress()));
		this.buffer.clear();
	}

	@Override
	public void close() throws IOException {
		this.parameters.getSocket().close();
	}

	@Override
	public void configure(String configuration) throws ConfigurationException {
		if (this.parameters != null)
			throw new ConfigurationException(configuration, UdpIpInputStream.class
			        .getCanonicalName()
			        + " is already configured");
		try {
			this.parameters = new UdpIpParameters(configuration);
			this.buffer = ByteBuffer.allocate(this.parameters.getMaxPacketSize());
		} catch (Exception exception) {
			throw new ConfigurationException(configuration, exception);
		}
	}

}

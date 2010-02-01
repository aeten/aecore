import org.pititom.core.messenger.extension.MessengerAcknowledgeProtocol;

public class AcknowledgeProtocol implements
		MessengerAcknowledgeProtocol<AbstractMessage, Acknowledge> {

	private final static long TIMEOUT = 1000;
	
	private AcknowledgeProtocol() {
	}

	public static final AcknowledgeProtocol INSTANCE = new AcknowledgeProtocol();

	public static AcknowledgeProtocol getInstance() {
		return INSTANCE;
	}

	@Override
	public boolean isSuccess(Acknowledge acknowledge) {
		switch (acknowledge) {
			case OK:
				return true;
			default:
				return false;
		}
	}

	@Override
	public Acknowledge getAcknowledge(AbstractMessage sentMessage, AbstractMessage recievedMessage) {
		if ((recievedMessage == null) || (recievedMessage.getAcknowledge() == null)) {
			return null;
		}
		switch (recievedMessage.getAcknowledge()) {
			case INVALID_DATA:
			case INVALID_MESSAGE:
			case OK:
				return recievedMessage.getAcknowledge();
			default:
				return null;
		}

	}

	@Override
	public long getAcknowledgedTimeout(AbstractMessage message) {
		if (message.getAcknowledge() == null) {
			return 0;
		}
		switch (message.getAcknowledge()) {
			case SOLLICITED_NEED_ACKNOWLEDGE:
			case UNSOLLICITED_NEED_ACKNOWLEDGE:
				return TIMEOUT;
			default:
				return 0;
		}
	}

}

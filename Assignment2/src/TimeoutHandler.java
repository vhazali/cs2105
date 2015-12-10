import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.TimerTask;

public class TimeoutHandler extends TimerTask {

	/* Constants */
	public static final boolean	DEBUG_MODE	= false;

	/* Member Variables */
	private UDPSegment			_segment;
	private DatagramSocket		_socket;
	private InetAddress			_targetIp;
	private int					_targetPort;

	/* Constructors */
	public TimeoutHandler(UDPSegment seg, DatagramSocket skt, InetAddress ip,
			int port) {
		setSegment(seg);
		setSocket(skt);
		setTargetIp(ip);
		setTargetPort(port);
	}

	/* Accessors and Modifiers */

	public UDPSegment getSegment() {
		return _segment;
	}

	public void setSegment(UDPSegment segment) {
		_segment = segment;
	}

	public DatagramSocket getSocket() {
		return _socket;
	}

	public void setSocket(DatagramSocket socket) {
		_socket = socket;
	}

	public InetAddress getTargetIp() {
		return _targetIp;
	}

	public void setTargetIp(InetAddress targetIp) {
		_targetIp = targetIp;
	}

	public int getTargetPort() {
		return _targetPort;
	}

	public void setTargetPort(int targetPort) {
		_targetPort = targetPort;
	}

	/* Public methods */
	@Override
	public void run() {
		if (getSegment().isAck()) {
			this.cancel();
		}
		else {
			if (DEBUG_MODE) {
				System.out.println("Timeout for segmet: "
						+ getSegment().getSeqNum());
				System.out.println("Sending: " + getSegment().toString());
			}
			sendPacket();
		}
	}

	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append("Current segment: \n");
		result.append(getSegment().toString());
		result.append("Connected to: \n");
		result.append("Target IP address: " + getTargetIp() + "\n");
		result.append("Target port number: " + getTargetPort() + "\n");
		return result.toString();
	}

	/* Private methods */
	private void sendPacket() {
		byte[] payload = new byte[FileSender.MSS];
		getSegment().makePayload(payload);
		if (DEBUG_MODE) {
			System.out.println("sending payload: " + new String(payload));
		}
		try {
			getSocket().send(
					new DatagramPacket(payload, getSegment().getLength(),
							getTargetIp(), getTargetPort()));
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
		}
	}

}

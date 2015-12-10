import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

/**
 * 
 * This class will receive a file from the sender and saves it in the same
 * director under the name specified by the sender.
 * 
 * @input The class will be invoked with the port number to listen to.
 * 
 * @assumption All inputs are correct. Filename from sender will always be under
 *             1000 bytes. Underlying transmission channel is perfect and all
 *             data will be received in good order.
 * 
 * @author Victor Hazali
 * 
 */
class FileReceiver {

	/* Static Variables */
	private static final boolean	DEBUG_MODE	= false;

	/* Member Variables */
	private DatagramSocket			_socket;
	private String					_filename;
	private File					_file;
	private UDPSegment				_rcvdSegment;
	private int						_currSeqNum;
	private FileOutputStream		_fos;
	private BufferedOutputStream	_fileWriter;
	private InetAddress				_clientIpAdd;
	private int						_clientPortNum;

	/* Constructor */

	/**
	 * Constructor
	 * 
	 * @param localPort
	 *            port to listen on
	 */
	public FileReceiver(String localPort) {
		try {
			setSocket(new DatagramSocket(Integer.parseInt(localPort)));
			getSocket().setReuseAddress(true);
		} catch (NumberFormatException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
		} catch (SocketException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
		}
		setCurrSeqNum(0);
	}

	/* Accessors and Modifiers */

	public DatagramSocket getSocket() {
		return _socket;
	}

	public void setSocket(DatagramSocket socket) {
		_socket = socket;
	}

	public String getFilename() {
		return _filename;
	}

	public void setFilename(String filename) {
		_filename = filename;
	}

	public File getFile() {
		return _file;
	}

	public void setFile(File file) {
		_file = file;
	}

	public UDPSegment getRcvdSegment() {
		return _rcvdSegment;
	}

	public void setRcvdSegment(UDPSegment rcvdSegment) {
		_rcvdSegment = rcvdSegment;
	}

	public int getCurrSeqNum() {
		return _currSeqNum;
	}

	public void setCurrSeqNum(int currSeqNum) {
		_currSeqNum = currSeqNum;
	}

	public void incrementSeqNum() {
		_currSeqNum++;
	}

	public FileOutputStream getFos() {
		return _fos;
	}

	public void setFos(FileOutputStream fos) {
		_fos = fos;
	}

	public BufferedOutputStream getFileWriter() {
		return _fileWriter;
	}

	public void setFileWriter(BufferedOutputStream fileWriter) {
		_fileWriter = fileWriter;
	}

	public InetAddress getClientIpAdd() {
		return _clientIpAdd;
	}

	public void setClientIpAdd(InetAddress clientIpAdd) {
		_clientIpAdd = clientIpAdd;
	}

	public int getClientPortNum() {
		return _clientPortNum;
	}

	public void setClientPortNum(int clientPortNum) {
		_clientPortNum = clientPortNum;
	}

	/* Public Methods */

	/**
	 * Method to execute receiving of data to be written to a file
	 */
	public void run() {
		receiveFilename();
		openWriter();
		receiveFile();
		cleanup();
	}

	/* Private Methods */

	/**
	 * Receives the filename from sender. ACK is sent only if the segment is
	 * valid and in order
	 */
	private void receiveFilename() {
		DatagramPacket rcvdPacket = new DatagramPacket(
				new byte[FileSender.MSS], FileSender.MSS);
		while (true) {
			try {
				getSocket().receive(rcvdPacket);
				setRcvdSegment(new UDPSegment(rcvdPacket));
				if (DEBUG_MODE) {
					System.out.println(new String(rcvdPacket.getData()));
					System.out.println(getRcvdSegment().toString());
				}
				if (getRcvdSegment().isValid() && inOrder()) {
					setClientIpAdd(rcvdPacket.getAddress());
					setClientPortNum(rcvdPacket.getPort());
					sendAck();
					break;
				}
			} catch (IOException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Checks if the received segment is in order
	 * 
	 * @return true if received segment's sequence number is 1 more than current
	 *         sequence number. False otherwise.
	 */
	private boolean inOrder() {
		return getRcvdSegment().getSeqNum() == getCurrSeqNum() + 1;
	}

	private void sendAck() {
		if (DEBUG_MODE) {
			System.out.println("Sending ACK for "
					+ getRcvdSegment().getSeqNum());
		}
		getRcvdSegment().convertToAck();
		byte[] payload = new byte[FileSender.MSS];
		getRcvdSegment().makePayload(payload);
		try {
			getSocket().send(
					new DatagramPacket(payload, getRcvdSegment().getLength(),
							getClientIpAdd(), getClientPortNum()));
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(4);
		}
	}

	/**
	 * Opens a buffered output stream that writes to a file
	 * 
	 * @postcondition: writer must be opened successfully at the end of method
	 */
	private void openWriter() {
		try {
			setFos(new FileOutputStream(getFilename()));
		} catch (FileNotFoundException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(2);
		}
		setFileWriter(new BufferedOutputStream(getFos()));

		assert (getFileWriter() != null);
	}

	/**
	 * Receives the contents of the file from the sender. If the segments
	 * received are in order and valid, an ACK segment is sent to the sender and
	 * the contents of the received segment is written
	 */
	private void receiveFile() {
		DatagramPacket rcvdPacket = new DatagramPacket(
				new byte[FileSender.MSS], FileSender.MSS);
		do {
			try {
				getSocket().receive(rcvdPacket);
				setRcvdSegment(new UDPSegment(rcvdPacket));
				if (getRcvdSegment().isValid() && inOrder()) {
					byte[] data = getRcvdSegment().getData();
					int dataLen = getRcvdSegment().getDataLen();
					sendAck();
					writeToFile(data, dataLen);
				}
			} catch (IOException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
				System.exit(3);
			}
		} while (hasDataToReceive(rcvdPacket));
	}

	/**
	 * Method to write data from byte buffer into file on local directory
	 * 
	 * @param data
	 *            Output stream to be used. Should already be open for writing.
	 * @param length
	 *            Length of data to write into file
	 */
	private void writeToFile(byte[] data, int length) {
		if (length == 0) {
			return;
		}
		try {
			getFileWriter().write(data, 0, length);
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(5);
		}
	}

	/**
	 * Method to check if there are still data to be received. End of
	 * transmission is represented by receiving an empty packet
	 * 
	 * @param received
	 *            the last packet received
	 * @return false if packet length = 0. True otherwise
	 */
	private boolean hasDataToReceive(DatagramPacket received) {
		if (received.getLength() == 0) {
			return false;
		}
		return true;
	}

	private void cleanup() {
		try {
			getFileWriter().close();
			getFos().close();
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(6);
		}
		getSocket().close();
	}

	/**
	 * Main method that receives the file.
	 * 
	 * <pre>
	 * Termination codes used and their meaning:
	 * 1: Program not executed with correct arguments
	 * 2: Failed to create new file 
	 * 3: Failed to open socket to receive a datagram packet
	 * 4: Failed to send ACK
	 * 5: Failed to write to file.
	 * 6: Failed to close writers
	 * </pre>
	 * 
	 * @param args
	 *            Array of Strings. Should contain port to listen on
	 */
	public static void main(String[] args) {

		// check if the number of command line argument is 1
		if (args.length != 1) {
			System.out.println("Usage: java FileReceiver port");
			System.exit(1);
		}

		FileReceiver fr = new FileReceiver(args[0]);
		fr.run();
	}
}

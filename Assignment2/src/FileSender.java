import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;

/**
 * 
 * This class will send a file to the desired host using the UDP protocol.
 * 
 * @input The class will be invoked with path/filename, recipient host name,
 *        host's port, as well as a filename for the host to save the file
 *        under.
 * 
 * @assumption All inputs are correct. Filename for host will always be under
 *             1000 bytes. Underlying transmission channel is perfect and all
 *             data will be received in good order.
 * 
 * @author Victor Hazali A0110741X
 * 
 */
class FileSender {

	/* Constants */
	private static final boolean	DEBUG_MODE	= false;
	// Max Segment Size in bytes
	public static final int			MSS			= 1000;
	// Sending delay in msec
	private static final int		SEND_DELAY	= 0;
	// Timeout delay in msec
	private static final int		TO_DELAY	= 20;

	/* Member Variables */
	private File					_fileToSend;
	private int						_hostPortNum;
	private String					_targetFilename;
	private DatagramSocket			_socket;
	private InetAddress				_hostAddress;
	private int						_currSeqNum;
	private DatagramPacket			_rcvdPkt;

	/* Constructors */

	/**
	 * Constructor
	 * 
	 * @param srcFile
	 *            file path for source file
	 * @param destPort
	 *            port to send to
	 * @param destFile
	 *            file name to be used to store the file on the host's end
	 */
	public FileSender(String srcFile, String destPort, String destFile) {
		setFileToSend(new File(srcFile));
		setHostPortNum(Integer.parseInt(destPort));
		setTargetFilename(destFile);
		setCurrSeqNum(1);	// First segment will start with sequence number of 1
		setRcvdPkt(new DatagramPacket(new byte[MSS], MSS));

		try {
			setSocket(new DatagramSocket());
			setHostAddress(InetAddress.getByName("localhost"));
		} catch (SocketException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(2);
		} catch (SecurityException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(3);
		} catch (UnknownHostException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(4);
		}

	}

	/* Accessors and Getters */

	public File getFileToSend() {
		return _fileToSend;
	}

	public void setFileToSend(File fileToSend) {
		_fileToSend = fileToSend;
	}

	public int getHostPortNum() {
		return _hostPortNum;
	}

	public void setHostPortNum(int hostPortNum) {
		_hostPortNum = hostPortNum;
	}

	public String getTargetFilename() {
		return _targetFilename;
	}

	public void setTargetFilename(String targetFilename) {
		_targetFilename = targetFilename;
	}

	public DatagramSocket getSocket() {
		return _socket;
	}

	public void setSocket(DatagramSocket socket) {
		_socket = socket;
	}

	public InetAddress getHostAddress() {
		return _hostAddress;
	}

	public void setHostAddress(InetAddress hostAddress) {
		_hostAddress = hostAddress;
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

	public DatagramPacket getRcvdPkt() {
		return _rcvdPkt;
	}

	public void setRcvdPkt(DatagramPacket rcvdPkt) {
		_rcvdPkt = rcvdPkt;
	}

	/* Public Methods */

	public void run() {
		sendFilename();
		sendFileContents();
	}

	public void sendFilename() {
		UDPSegment filename = new UDPSegment(getCurrSeqNum(),
				UDPSegment.DATA_TYPE, getTargetFilename().getBytes(),
				getTargetFilename().getBytes().length);
		sendSegment(filename);
		incrementSeqNum();
	}

	public void sendFileContents() {
		UDPSegment toSend = new UDPSegment(getCurrSeqNum(),
				UDPSegment.DATA_TYPE);
		try {

			// Opening file reader
			FileInputStream fis = new FileInputStream(getFileToSend());
			BufferedInputStream fileReader = new BufferedInputStream(fis);

			// Reading first packet
			int lengthRead = readFileContents(fileReader, toSend);

			// Sending and reading remaining packets
			while (hasDataToSend(lengthRead)) {

				// Sending current packet
				sendSegment(toSend);

				// Received ACK for current packet
				// and preparing to read next packet
				incrementSeqNum();
				toSend = new UDPSegment(getCurrSeqNum(), UDPSegment.DATA_TYPE);

				// Reading next packet
				lengthRead = readFileContents(fileReader, toSend);
			}

			sendFinalSegment();

			fileReader.close();
			fis.close();
			getSocket().close();
		} catch (FileNotFoundException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(5);
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(7);
		}
	}

	/**
	 * Reads the contents of the file to be sent and places it into the
	 * UDPsegment data
	 * 
	 * @param fileReader
	 *            InputStream to read from
	 * @param seg
	 *            UDP Segment to store the data
	 * @return length of data read
	 */
	private int readFileContents(BufferedInputStream fileReader, UDPSegment seg) {
		int lengthRead = 0;

		try {
			byte[] buffer = new byte[UDPSegment.DATA_SIZE];
			lengthRead = fileReader.read(buffer);
			seg.setData((buffer));
			seg.setDataLen(lengthRead);
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(6);
		}
		return lengthRead;
	}

	/**
	 * Checks if there is any data to send. This is decided by the amount of
	 * bytes of data last read
	 * 
	 * @param lengthRead
	 *            amount of bytes of data last read
	 * @return true if lengthRead >0, false otherwise.
	 */
	private boolean hasDataToSend(int lengthRead) {
		return lengthRead > 0;
	}

	/**
	 * Sends a specified UDP segment over the specified socket to the specified
	 * IP address and port number
	 * 
	 * @precondition toSend must already have the payload, sequence number and
	 *               type
	 * 
	 * @param toSend
	 *            the UDP Segment to be sent
	 */
	private void sendSegment(UDPSegment toSend) {

		toSend.setChecksum(toSend.calculateChecksum());

		Timer timer = new Timer();
		timer.schedule(new TimeoutHandler(toSend, getSocket(),
				getHostAddress(), getHostPortNum()), SEND_DELAY, TO_DELAY);

		while (!toSend.isAck()) {
			try {
				getSocket().receive(getRcvdPkt());
				UDPSegment ACK = new UDPSegment(getRcvdPkt());
				System.out.println("ACK received: " + ACK.toString());
				if (ACK.equals(toSend)) {
					toSend.setAckRcvd(true);
					if (DEBUG_MODE) {
						System.out.println("segment " + toSend.getSeqNum()
								+ "ACKed");
					}
				}
			} catch (IOException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			}
		}

	}

	private void sendFinalSegment() {
		UDPSegment finalSegment = new UDPSegment(getCurrSeqNum(),
				UDPSegment.DATA_TYPE, new byte[0], 0);
		sendSegment(finalSegment);
	}

	/**
	 * Main method that sends the file.
	 * 
	 * <pre>
	 * Termination codes used and their meaning:
	 * 0: Successful run
	 * 1: wrong invocation of commands when running
	 * 2: Socket failed to be opened/bound to specified port
	 * 3: Security exception
	 * 4: Failed to resolve host IP Address
	 * 5: Failed to open file to be sent
	 * 6: I/O exception while reading file contents
	 * 7: I/O exception while closing readers
	 * </pre>
	 * 
	 * @param args
	 *            Array of Strings. Should contain filename of file to be sent,
	 *            recipient host name, host port number and intended filename to
	 *            be saved on host.
	 */
	public static void main(String[] args) {

		// check if the number of command line argument is 4
		if (args.length != 3) {
			System.out
					.println("Usage: java FileSender <path/filename> <unreliNetPort> <rcvFileName>");
			System.exit(1);
		}

		try {

			FileSender fs = new FileSender(args[0], args[1], args[2]);
			fs.run();

		} catch (NullPointerException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(1);
		}

		System.exit(0);
	}
}
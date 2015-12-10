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

	/* Static Variables */
	private static final boolean	DEBUG_MODE	= false;
	private static final int		BUFFER_SIZE	= 1000;

	/* Data Attributes */
	private DatagramSocket			_socket;
	private String					_targetFilename;
	private File					_fileToSend;
	private String					_hostName;
	private int						_hostPortNum;
	private InetAddress				_hostAddress;
	private byte[]					_buffer;

	/* Accessors and Modifiers */
	public DatagramSocket getSocket() {
		return _socket;
	}

	public void setSocket(DatagramSocket socket) {
		_socket = socket;
	}

	public String getTargetFilename() {
		return _targetFilename;
	}

	public void setTargetFilename(String filename) {
		_targetFilename = filename;
	}

	public File getFileToSend() {
		return _fileToSend;
	}

	public void setFileToSend(File file) {
		_fileToSend = file;
	}

	public String getHostName() {
		return _hostName;
	}

	public void setHostName(String hostName) {
		_hostName = hostName;
	}

	public int getHostPortNum() {
		return _hostPortNum;
	}

	public void setHostPortNum(String hostPortNum) {
		_hostPortNum = Integer.parseInt(hostPortNum);
	}

	public void setHostPortNum(int hostPortNum) {
		_hostPortNum = hostPortNum;
	}

	public InetAddress getHostAddress() {
		return _hostAddress;
	}

	public void setHostAddress(InetAddress hostAddress) {
		_hostAddress = hostAddress;
	}

	public byte[] getBuffer() {
		return _buffer;
	}

	public void setBuffer(byte[] buffer) {
		_buffer = buffer;
	}

	/**
	 * Constructor
	 * 
	 * @param fileToOpen
	 *            String containing file path to be sent
	 * @param host
	 *            String containing name of host or IP address of host
	 * @param port
	 *            String containing port number to send to
	 * @param rcvFileName
	 *            String containing filename to be used to store the file on the
	 *            host's end
	 */
	public FileSender(String fileToOpen, String host, String port,
			String rcvFileName) {

		try {

			setSocket(new DatagramSocket());
			setHostAddress(InetAddress.getByName(host));

		} catch (SocketException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(5);
		} catch (SecurityException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(6);
		} catch (UnknownHostException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(7);
		}

		setFileToSend(new File(fileToOpen));
		setHostName(host);
		setHostPortNum(port);
		setTargetFilename(rcvFileName);
		setBuffer(new byte[BUFFER_SIZE]);
	}

	/**
	 * Method to execute sending of data
	 */
	public void run() {
		FileInputStream fis = null;
		sendFilename();

		try {

			// Opening file reader
			fis = new FileInputStream(getFileToSend());
			BufferedInputStream fileReader = new BufferedInputStream(fis);

			// Reading first packet
			int lengthRead = readFileContents(fileReader);

			// Sending and reading remaining packets
			while (hasDataToSend(lengthRead)) {
				sendPacket(lengthRead);
				lengthRead = readFileContents(fileReader);
			}

			sendFinalPacket();

			fileReader.close();
			fis.close();

		} catch (FileNotFoundException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(2);
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(3);
		}
	}

	/**
	 * Sends the filename as the first packet to the host.
	 */
	private void sendFilename() {
		setBuffer(getTargetFilename().getBytes());
		sendPacket(getBuffer().length);
		setBuffer(new byte[BUFFER_SIZE]);
	}

	/**
	 * Sends an empty packet to denote the end of file
	 */
	private void sendFinalPacket() {
		byte[] buffer = new byte[0];
		setBuffer(buffer);
		sendPacket(0);
	}

	/**
	 * Reads the content of the file to be sent into the buffer.
	 * 
	 * @param fileReader
	 *            Reader to use to read file. Reader should already be opened.
	 * @return The number of bytes read.
	 */
	private int readFileContents(BufferedInputStream fileReader) {
		int lengthRead = 0;

		try {
			byte[] buffer = new byte[BUFFER_SIZE];
			lengthRead = fileReader.read(buffer);
			setBuffer(buffer);
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(4);
		}
		return lengthRead;
	}

	/**
	 * Method to send a packet to the host
	 */
	private void sendPacket(int length) {

		DatagramPacket packet = new DatagramPacket(getBuffer(),
				length, getHostAddress(), getHostPortNum());
		try {

			getSocket().send(packet);
			Thread.sleep(10);

		} catch (InterruptedException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(8);
		}

		setBuffer(new byte[BUFFER_SIZE]);
	}

	/**
	 * Method to check if there is still data to be sent. This method will take
	 * in the length of bytes read from the file. If there is data read from the
	 * file, the method will return true; false otherwise.
	 * 
	 * @param lengthRead
	 *            Length of bytes read from file
	 * @return true if length > 0, false otherwise.
	 */
	private boolean hasDataToSend(int lengthRead) {
		if (lengthRead > 0) {
			return true;
		}
		return false;
	}

	/**
	 * Main method that sends the file.
	 * 
	 * <pre>
	 * Termination codes used and their meaning:
	 * 1: Null pointer exception
	 * 2: File to send cannot be found
	 * 3: I/O error in closing reader and/or input stream.
	 * 4: I/O error in reading file to be sent
	 * 5: Socket could not be opened or bound to the specified port
	 * 6: Security settings do not allow for operations
	 * 7: Host name / IP Address cannot be resolved
	 * 8: Failed to send packet
	 * 9: Program not executed with correct arguments
	 * </pre>
	 * 
	 * @param args
	 *            Array of Strings. Should contain filename of file to be sent,
	 *            recipient host name, host port number and intended filename to
	 *            be saved on host.
	 */
	public static void main(String[] args) {

		// check if the number of command line argument is 4
		if (args.length != 4) {
			System.out.println("Usage: java FileSender <path/filename> "
					+ "<rcvHostName> <rcvPort> <rcvFileName>");
			System.exit(9);
		}

		try {

			FileSender fs = new FileSender(args[0], args[1], args[2], args[3]);
			fs.run();

		} catch (NullPointerException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(1);
		}
	}
}
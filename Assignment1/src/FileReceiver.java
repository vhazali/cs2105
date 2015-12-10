import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
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
	private static final int		BUFFER_SIZE	= 1000;

	/* Data Attributes */
	private DatagramSocket			_socket;
	private String					_filename;
	private File					_file;
	private byte[]					_buffer;

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

	public byte[] getBuffer() {
		return _buffer;
	}

	public void setBuffer(byte[] buffer) {
		_buffer = buffer;
	}

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

		setBuffer(new byte[BUFFER_SIZE]);
	}

	/**
	 * Method to execute receiving of data to be written to a file
	 */
	public void run() {
		// TODO can refactor this out to become a class attribute
		DatagramPacket rcvdpkt = new DatagramPacket(getBuffer(),
				getBuffer().length);
		try {

			// Receiving file name
			getSocket().receive(rcvdpkt);
			String filename = new String(rcvdpkt.getData(), 0,
					rcvdpkt.getLength());
			setFilename(filename);

			// Opening file writer
			FileOutputStream fos = new FileOutputStream(getFilename());
			BufferedOutputStream fileWriter = new BufferedOutputStream(fos);

			// Receiving first packet
			getSocket().receive(rcvdpkt);
			setBuffer(rcvdpkt.getData());

			// Writing and receiving remainder of packets
			while (hasDataToReceive(rcvdpkt)) {
				writeToFile(fileWriter, rcvdpkt.getLength());
				getSocket().receive(rcvdpkt);
				setBuffer(rcvdpkt.getData());
			}

			fileWriter.close();
			fos.close();

		} catch (NullPointerException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(2);
		} catch (Exception e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(3);
		}
	}

	/**
	 * Method to write data from byte buffer into file on local directory
	 * 
	 * @param fileWriter
	 *            Output stream to be used. Should already be open for writing.
	 * @param length
	 *            Length of data to write into file
	 */
	private void writeToFile(BufferedOutputStream fileWriter, int length) {
		try {
			fileWriter.write(getBuffer(), 0, length);
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(4);
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

	/**
	 * Main method that receives the file.
	 * 
	 * <pre>
	 * Termination codes used and their meaning:
	 * 1: Program not executed with correct arguments
	 * 2: Failed to create new file 
	 * 3: Failed to open socket to receive a datagram packet
	 * 4: Failed to write to file.
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

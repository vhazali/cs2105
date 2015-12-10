import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SealedObject;
import javax.crypto.SecretKey;

/**
 * TCP Client to communicate with Bob
 * 
 * @author Victor Hazali
 * 
 */
class Alice {  // Alice is a TCP client

	public static final boolean	DEBUG_MODE		= false;

	// ip address of Bob
	String						bobIP;
	// port Bob listens to
	int							bobPort;
	// socket used to talk to Bob
	Socket						connectionSkt;
	// to send session key to Bob
	private ObjectOutputStream	toBob;
	// to read encrypted messages from Bob
	private ObjectInputStream	fromBob;
	// object for encryption and decryption
	private Crypto				crypto;
	// writer to write to file
	private FileWriter			toFile;
	// file to store received and decrypted messages
	public static final String	MESSAGE_FILE	= "msgs.txt";

	public static void main(String[] args) {

		// Check if the number of command line argument is 2
		if (args.length != 2) {
			System.err.println("Usage: java Alice BobIP BobPort");
			System.exit(1);
		}

		new Alice(args[0], args[1]);
	}

	// Constructor
	public Alice(String ipStr, String portStr) {

		initialize(ipStr, portStr);

		// Send session key to Bob
		sendSessionKey();

		// Receive encrypted messages from Bob, decrypt and save them to file
		writeToFile(receiveMessages());

		// Close all resources
		cleanUp();
	}

	// Closes all resources used
	private void cleanUp() {
		try {
			connectionSkt.close();
			toBob.close();
			fromBob.close();
			toFile.close();
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
				System.out.println("Failed to close resources");
			}
			System.exit(1);
		}
	}

	// Initializes all variable
	public void initialize(String ipStr, String portStr) {
		bobIP = ipStr;
		bobPort = Integer.parseInt(portStr);

		try {
			connectionSkt = new Socket(bobIP, bobPort);
		} catch (UnknownHostException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
				System.out.println("Failed to find host based on IP address");
			}
			System.exit(1);
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
				System.out.println("Failed to create connection socket");
			}
			System.exit(1);
		}

		try {
			toBob = new ObjectOutputStream(connectionSkt.getOutputStream());
			fromBob = new ObjectInputStream(connectionSkt.getInputStream());
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
				System.out.println("Failed to get input/output stream");
			}
			System.exit(1);
		}

		crypto = new Crypto();

		try {
			toFile = new FileWriter(new File(MESSAGE_FILE));
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
				System.out.println("IO Exception opening file writer");
			}
			System.exit(1);
		}

		if (DEBUG_MODE) {
			System.out.println("Successfully initialised Alice");
		}
	}

	// Send session key to Bob
	public void sendSessionKey() {
		SealedObject encryptedSesKey = crypto.getSessionKey();
		try {
			toBob.writeObject(encryptedSesKey);
			if (DEBUG_MODE) {
				System.out.println("Session key sent");
			}
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
				System.out.println("IO Exception sending session key");
			}
		}
	}

	// Receive messages one by one from Bob, decrypt
	public String receiveMessages() {
		StringBuilder plainText = new StringBuilder();
		SealedObject encryptedMsg = null;

		try {

			// Assume Bob will send exactly 10 lines
			for (int i = 0; i < 10; i++) {
				encryptedMsg = (SealedObject) fromBob.readObject();
				plainText.append(crypto.decryptMsg(encryptedMsg));
			}

			if (DEBUG_MODE) {
				System.out.println("Messages received");
			}

		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
				System.out.println("IO Exception when reading from bob");
			}
			System.exit(1);
		} catch (ClassNotFoundException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
		}
		return plainText.toString();
	}

	// Writing received messages to file
	public void writeToFile(String plainText) {

		if (plainText == null) {
			return;
		}

		try {
			toFile.write(plainText);

			if (DEBUG_MODE) {
				System.out.println("Messages written");
			}
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
				System.out.println("IO Exception writing to file");
			}
		}
	}

	/*****************/
	/** inner class **/
	/*****************/
	class Crypto {

		// Bob's public key, to be read from file
		private PublicKey			pubKey;
		// Alice generates a new session key for each communication session
		private SecretKey			sessionKey;
		// File that contains Bob' public key
		public static final String	PUBLIC_KEY_FILE	= "public.key";

		// Constructor
		public Crypto() {
			// Read Bob's public key from file
			readPublicKey();
			// Generate session key dynamically
			initSessionKey();
		}

		// Read Bob's public key from file
		public void readPublicKey() {
			try {
				ObjectInputStream ois =
						new ObjectInputStream(new FileInputStream(
								PUBLIC_KEY_FILE));
				this.pubKey = (PublicKey) ois.readObject();
				ois.close();
			} catch (IOException oie) {
				System.out.println("Error reading public key from file");
				System.exit(1);
			} catch (ClassNotFoundException cnfe) {
				System.out.println("Error: cannot typecast to class PublicKey");
				System.exit(1);
			}

			if (DEBUG_MODE) {
				System.out.println("Public key read from file "
						+ PUBLIC_KEY_FILE);
			}
		}

		// Generate a session key
		public void initSessionKey() {
			// suggested AES key length is 128 bits
			try {
				KeyGenerator keyGen = KeyGenerator.getInstance("AES");
				keyGen.init(128);
				sessionKey = keyGen.generateKey();
			} catch (NoSuchAlgorithmException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
					System.out.println("Failed to initialise session key");
				}
				System.exit(1);
			}
			if (DEBUG_MODE) {
				System.out.println("Successfully initialised session key");
			}
		}

		// Seal session key with RSA public key in a SealedObject and return
		public SealedObject getSessionKey() {

			SealedObject sessionKeyObj = null;

			// Alice must use the same RSA key/transformation as Bob specified
			try {
				Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
				cipher.init(Cipher.ENCRYPT_MODE, pubKey);
				sessionKeyObj = new SealedObject(sessionKey.getEncoded(),
						cipher);
			} catch (NoSuchAlgorithmException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			} catch (NoSuchPaddingException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			} catch (InvalidKeyException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			} catch (IllegalBlockSizeException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			}

			if (DEBUG_MODE) {
				System.out.println("Successfully sealed session key");
			}
			return sessionKeyObj;
		}

		// Decrypt and extract a message from SealedObject
		public String decryptMsg(SealedObject encryptedMsgObj) {

			String plainText = null;

			// Alice and Bob use the same AES key/transformation
			try {
				Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
				cipher.init(Cipher.DECRYPT_MODE, sessionKey);
				plainText = (String) encryptedMsgObj.getObject(cipher) + "\n";
				if (DEBUG_MODE) {
					System.out.println("Successfully decrypted message");
				}
			} catch (NoSuchAlgorithmException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			} catch (NoSuchPaddingException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			} catch (InvalidKeyException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			} catch (ClassNotFoundException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			} catch (IllegalBlockSizeException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			} catch (BadPaddingException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			} catch (IOException e) {
				if (DEBUG_MODE) {
					e.printStackTrace();
				}
			}
			return plainText;
		}
	}
}
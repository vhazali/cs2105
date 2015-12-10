import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;

public class UDPSegment {

	/* Constants */

	// All sizes and offsets are in bytes
	private static final int	SEQ_OFFSET		= 0;
	private static final int	SEQ_SIZE		= 4;
	private static final int	CS_OFFSET		= 4;
	private static final int	CS_SIZE			= 8;
	private static final int	TYPE_OFFSET		= 12;
	private static final int	TYPE_SIZE		= 2;
	private static final int	HEADER_SIZE		= CS_SIZE + TYPE_SIZE
														+ SEQ_SIZE;
	private static final int	DATA_OFFSET		= 14;
	public static final int		DATA_SIZE		= FileSender.MSS - HEADER_SIZE;

	public static final char	INVALID_TYPE	= 'I';
	public static final char	ACK_TYPE		= 'A';
	public static final char	DATA_TYPE		= 'D';

	/* Member variables */
	private int					_seqNum;
	private long				_checksum;
	private char				_type;
	private byte[]				_data;
	private boolean				_ackRcvd;
	private int					_dataLen;

	/* Constructors */
	public UDPSegment() {
		setSeqNum(0);
		setChecksum(0);
		setType(INVALID_TYPE);
		setData(new byte[DATA_SIZE]);
		setAckRcvd(false);
		setDataLen(DATA_SIZE);
	}

	public UDPSegment(int seqNum, long checksum, char type, byte[] data,
			int dataLen) {
		assert (dataLen <= FileSender.MSS);
		setSeqNum(seqNum);
		setChecksum(checksum);
		setType(type);
		setData(data);
		setAckRcvd(false);
		setDataLen(dataLen);
	}

	public UDPSegment(int seqNum, char type, byte[] data, int dataLen) {
		assert (dataLen <= FileSender.MSS);
		setSeqNum(seqNum);
		setType(type);
		setData(data);
		setAckRcvd(false);
		setDataLen(dataLen);
	}

	public UDPSegment(int seqNum, char type) {
		setSeqNum(seqNum);
		setType(type);
		setData(new byte[DATA_SIZE]);
		setAckRcvd(false);
		setDataLen(DATA_SIZE);
	}

	public UDPSegment(DatagramPacket pkt) {
		setSeqNum(ByteBuffer.wrap(pkt.getData(), SEQ_OFFSET, SEQ_SIZE).getInt());
		setChecksum(ByteBuffer.wrap(pkt.getData(), CS_OFFSET, CS_SIZE)
				.getLong());
		setType(ByteBuffer.wrap(pkt.getData(), TYPE_OFFSET, TYPE_SIZE)
				.getChar());

		byte[] data = ByteBuffer.wrap(pkt.getData(), DATA_OFFSET,
				pkt.getLength() - HEADER_SIZE).array();
		_data = new byte[FileSender.MSS];
		System.arraycopy(data, DATA_OFFSET, _data, 0, pkt.getLength()
				- HEADER_SIZE);
		setDataLen(pkt.getLength() - HEADER_SIZE);
	}

	/* Accessors and Modifiers */
	public int getSeqNum() {
		return _seqNum;
	}

	public void setSeqNum(int seqNum) {
		_seqNum = seqNum;
	}

	public long getChecksum() {
		return _checksum;
	}

	public void setChecksum(long checksum) {
		_checksum = checksum;
	}

	public char getType() {
		return _type;
	}

	public void setType(char type) {
		_type = type;
	}

	public byte[] getData() {
		return _data;
	}

	public void setData(byte[] data) {
		_data = data;
	}

	public boolean isAckRcvd() {
		return _ackRcvd;
	}

	public void setAckRcvd(boolean isAckRcvd) {
		_ackRcvd = isAckRcvd;
	}

	public int getDataLen() {
		return _dataLen;
	}

	public void setDataLen(int dataLen) {
		_dataLen = dataLen;
	}

	/* Public methods */

	/**
	 * Sets the checksum for the current segment using the payload and header
	 * contents
	 */
	public long calculateChecksum() {
		CRC32 crc = new CRC32();
		byte[] seqNum = ByteBuffer.allocate(SEQ_SIZE).putInt(getSeqNum())
				.array();
		byte[] type = ByteBuffer.allocate(TYPE_SIZE).putChar(getType()).array();

		byte[] buffer = new byte[FileSender.MSS];
		System.arraycopy(seqNum, 0, buffer, SEQ_OFFSET, SEQ_SIZE);
		System.arraycopy(type, 0, buffer, SEQ_SIZE, TYPE_SIZE);
		System.arraycopy(getData(), 0, buffer, SEQ_SIZE + TYPE_SIZE,
				getDataLen());

		crc.update(buffer);

		return crc.getValue();d
	}

	/**
	 * Checks if the segment is a valid segment or if it has been corrupted
	 * during transmission. This is done by calculating the checksum and
	 * comparing it to the value received.
	 * 
	 * @return true if the checksum received and calculated are the same, false
	 *         otherwise.
	 */
	public boolean isValid() {

		if (calculateChecksum() == getChecksum()) {
			return true;
		}
		return false;
	}

	/**
	 * Checks if this is an ACK segment
	 * 
	 * @return true if the type of segment is 'A'. False otherwise
	 */
	public boolean isAck() {
		return getType() == 'A';
	}

	/**
	 * Checks if the segment contains payload
	 * 
	 * @return true if the payload length is not 0. False otherwise
	 */
	public boolean containsData() {
		return getData().length != 0;
	}

	/**
	 * Converts this segment into a series of byte to be transmitted
	 * 
	 * @param payload
	 *            the byte array to store the contents of this segment
	 */
	public void makePayload(byte[] payload) {
		assert (payload != null);
		assert (payload.length == FileSender.MSS);
		assert (getDataLen() <= FileSender.MSS);

		// Converting fields into byte arrays
		byte[] seqNum = ByteBuffer.allocate(SEQ_SIZE).putInt(getSeqNum())
				.array();
		byte[] type = ByteBuffer.allocate(TYPE_SIZE).putChar(getType()).array();
		byte[] checksum = ByteBuffer.allocate(CS_SIZE).putLong(getChecksum())
				.array();

		// Copying into destination array
		System.arraycopy(seqNum, 0, payload, SEQ_OFFSET, SEQ_SIZE);
		System.arraycopy(checksum, 0, payload, CS_OFFSET, CS_SIZE);
		System.arraycopy(type, 0, payload, TYPE_OFFSET, TYPE_SIZE);
		System.arraycopy(getData(), 0, payload, DATA_OFFSET, getDataLen());
	}

	/**
	 * Gets the length of the payload. Paylaod is made up of header and data.
	 * Therefore, this is the same as the length of the sequence number +
	 * checksum + type + data.
	 * 
	 * @return length of payload
	 */
	public int getLength() {
		return SEQ_SIZE + CS_SIZE + TYPE_SIZE + getDataLen();
	}

	public void convertToAck() {
		setType(ACK_TYPE);
		setData(new byte[DATA_SIZE]);
		setDataLen(0);
		setChecksum(calculateChecksum());
	}

	public String headerContents() {
		StringBuilder result = new StringBuilder();
		result.append("seqNum: " + getSeqNum() + "\n");
		result.append("Checksum: " + getChecksum() + "\n");
		result.append("Type: " + getType() + "\n");
		result.append("Length: " + getLength() + "\n");
		return result.toString();
	}

	public String dataContents() {
		StringBuilder result = new StringBuilder();
		result.append("Data: " + new String(getData()) + "\n");
		return result.toString();
	}

	public String memberVariables() {
		StringBuilder result = new StringBuilder();
		result.append("Received ACK: " + isAckRcvd() + "\n");
		result.append("Data length: " + getDataLen() + "\n");
		return result.toString();
	}

	/**
	 * Returns a String representation of this object. Displays all the contents
	 * of the header as well as the payload and whether or not the segment has
	 * been ACKed by host.
	 */
	@Override
	public String toString() {
		StringBuilder result = new StringBuilder();
		result.append(headerContents());
		result.append(dataContents());
		result.append(memberVariables());
		return result.toString();
	}

	/**
	 * Checks if this segment is equals to another. Two segments are said to be
	 * equal if they have the same sequence number and checksum.
	 */
	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (!(o instanceof UDPSegment))
			return false;
		UDPSegment other = (UDPSegment) o;

		if (this.getChecksum() == other.getChecksum()) {
			return this.getSeqNum() == other.getSeqNum();
		}
		return false;
	}

}

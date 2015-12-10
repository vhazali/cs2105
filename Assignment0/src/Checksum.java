import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.CRC32;

/**
 * This class will calculate a checksum for a file to be used to detect data
 * corruption and ensure integrity of file.
 * 
 * @author Victor Hazali A0110741X
 * 
 */
public class Checksum {

	public static final boolean	DEBUG_MODE	= false;

	public static void main(String[] args) {
		try {
			byte[] bytes = Files.readAllBytes(Paths.get(args[0]));
			CRC32 crc = new CRC32();
			crc.update(bytes);
			System.out.println(crc.getValue());
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
				System.out.println("Failed to open file");
			}
			System.exit(1);
		}
	}

}

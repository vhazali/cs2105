import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * This class will read in the contents of a file and create an exact copy of
 * the file in the same directory. File names are specified in the argument of
 * the programs, with the source file coming before the destination file.
 * 
 * @author Victor Hazali A0110741X
 * 
 */
public class Copier {

	public static final boolean	DEBUG_MODE	= false;

	public static void main(String[] args) {
		byte[] buffer = new byte[1000];
		try {
			FileInputStream fis = new FileInputStream(args[0]);
			FileOutputStream fos = new FileOutputStream(args[1]);
			BufferedInputStream fileReader = new BufferedInputStream(fis);
			BufferedOutputStream fileWriter = new BufferedOutputStream(fos);
			int len = fileReader.read(buffer);
			while (len > 0) {
				fileWriter.write(buffer, 0, len);
				buffer = new byte[100];
				len = fileReader.read(buffer);
			}
			fileReader.close();
			fileWriter.close();
			System.out.println(args[0] + " is successfully copied to "
					+ args[1]);
		} catch (IOException e) {
			if (DEBUG_MODE) {
				e.printStackTrace();
			}
			System.exit(1);
		}

	}
}
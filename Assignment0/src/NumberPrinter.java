import java.util.Scanner;
import java.util.Timer;
import java.util.TimerTask;

public class NumberPrinter extends TimerTask {

	private static double	_number;

	public NumberPrinter(double toPrint) {
		setNumber(toPrint);
	}

	public static double getNumber() {
		return _number;
	}

	public void setNumber(double number) {
		_number = number;
	}

	public void run() {
		System.out.println(getNumber());
	}

	public static void main(String[] args) {
		Timer timer = new Timer();
		timer.schedule(new NumberPrinter(Double.parseDouble(args[0])),
				Integer.parseInt(args[1]) * 1000,
				Integer.parseInt(args[2]) * 1000);
		Scanner sc = new Scanner(System.in);
		while (sc.hasNext()) {
			if (sc.next().equals("q")) {
				timer.cancel();
				System.exit(0);
			}
		}
	}
}

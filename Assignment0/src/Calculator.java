/**
 * This class will create a command line calculator. The class takes in 2
 * integer variables and an operator of either addition, subtraction, division
 * or multiplication. The result of the mathematical equation is then returned
 * and displayed to the user.
 * 
 * Assumptions: All input and results will be within the range of the primitive
 * int variable.
 * 
 * @author Victor Hazali A0110741X
 * 
 */

public class Calculator {

	public enum Operator {
		ADDITION, SUBTRACTION, DIVISION, MULTIPLICATION, INVALID;
	}

	/**
	 * Main method that does calculation.
	 * 
	 * <pre>
	 * Termination codes used and their meaning:
	 * 1: incorrect input for operands
	 * 2: incorrect input for operators
	 * 10: division by zero
	 * </pre>
	 */
	public static void main(String[] args) {
		int first = 0, second = 0, result = 0;
		String operator = new String();

		try {
			first = Integer.parseInt(args[0]);
			second = Integer.parseInt(args[2]);
		} catch (Exception e) {
			System.out.println("Error in expression");
			System.exit(1);
		}
		operator = args[1];

		if (operator.equals("+")) {
			result = first + second;
		} else if (operator.equals("-")) {
			result = first - second;
		} else if (operator.equals("*")) {
			result = first * second;
		} else if (operator.equals("/")) {
			if (second == 0) {
				System.out.println("Error in expression");
				System.exit(10);
			}
			result = first / second;
		} else {
			System.out.println("Error in expression");
			System.exit(2);
		}
		System.out.println(args[0] + " " + args[1] + " " + args[2] + " = "
				+ result);
	}
}

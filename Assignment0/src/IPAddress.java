import java.util.Scanner;

public class IPAddress{

	public static void main(String[] args){
		String input = new String();
		Scanner sc = new Scanner(System.in);
		StringBuilder output = new StringBuilder();

		while(sc.hasNext()){
			output = new StringBuilder();
			input = sc.nextLine();
			for(int i=0;i<4;i++){
				if(i!=0){
					output.append(".");
				}
				output.append(Integer.parseInt(input.substring(8*i,8*i+8),2));
			}
			System.out.println(output);
		}
	}
}

import java.io.*;
import java.util.Scanner;

public class configuration {

	private String IPaddress="";
	private int Port;

	public configuration() throws FileNotFoundException, IOException {

        String fileName = "serverinfo.dat";
       
		ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream("serverinfo.dat"));

		try {
			for (int i = 0; i < 3; i++) {
				IPaddress+=Integer.toString(inputStream.readInt());
				IPaddress+=inputStream.readChar();
			}
			IPaddress+=Integer.toString(inputStream.readInt());
		    inputStream.readChar();
			
			Port=inputStream.readInt();
			inputStream.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error: file not found");
			IPaddress = "127.0.0.1";
			Port = 7890;
		} catch (IOException e) {
			System.out.println("Problem with output to file " + fileName);
		}

	}

	public String getIP() {
		return IPaddress;
	}

	public int getPort() {
		return Port;
	}
	
}

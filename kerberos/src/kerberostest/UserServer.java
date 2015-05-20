package kerberostest;

import java.net.*;
import java.io.*;

import javax.xml.bind.DatatypeConverter;


public class UserServer {
	public static void main(String[] args) throws IOException {
		int myPortNumber = Integer.parseInt(args[0]);
		byte[] Kb = DatatypeConverter.parseHexBinary(args[1]);
		
		try (
				ServerSocket serverSocket = new ServerSocket(myPortNumber);
				Socket clientSocket = serverSocket.accept();
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);
				BufferedReader in = new BufferedReader (
						new InputStreamReader(clientSocket.getInputStream()));
				) {
			byte[] randomuserinput = readBytes(clientSocket);
			
			String s2 = new String(randomuserinput);
			
			System.out.println("user server" + s2);			
		}
	}
	
	public static byte[] readBytes(Socket clientSocket) throws IOException {
		InputStream in = clientSocket.getInputStream();
		DataInputStream dis = new DataInputStream(in);
		
		int len = dis.readInt();
		byte[] data = new byte[len];
		if(len > 0) {
			dis.readFully(data);
		}		
		
		return data;
	}
}

package kerberostest;

import java.io.*;
import java.net.*;

public class UserClient {
	public static void main(String[] args) throws IOException {
		//contact key server		
		String hostNameKS = args[0];
		int portNumberKS = Integer.parseInt(args[1]);
		
		//contact user server
		String hostNameUS = args[2];
		int portNumberUS = Integer.parseInt(args[3]);
		
		try (
				Socket echoSocket = new Socket(hostNameKS, portNumberKS);
				PrintWriter out = new PrintWriter(echoSocket.getOutputStream(), true);
				BufferedReader in = new BufferedReader(new InputStreamReader(echoSocket.getInputStream()));
				BufferedReader stdIn = new BufferedReader(new InputStreamReader(System.in));
				
				Socket userSocket = new Socket(hostNameUS, portNumberUS);
				PrintWriter userOut = new PrintWriter(userSocket.getOutputStream(), true);
				BufferedReader userIn = new BufferedReader(new InputStreamReader(userSocket.getInputStream()));
				) {
					String userInput;
			
					while((userInput = stdIn.readLine()) != null) {
						if (userInput == "80000") {
							getSessionReqBytes(userInput.getBytes(), echoSocket);	
						}
						else {
							getSessionReqBytes(userInput.getBytes(), userSocket);	
						}
					}
		} catch(UnknownHostException ex) {
			System.err.println("never heard of host " + hostNameKS);
			System.exit(1);
		}
	}
	
	public static void getSessionReqBytes(byte[] inputByteArray, Socket echoSocket) throws IOException {
		sendSessionReqBytes(inputByteArray, 0, inputByteArray.length, echoSocket);
	}
	
	public static void sendSessionReqBytes(byte[] myByteArray, int start, int len, Socket echoSocket) throws IOException {
		OutputStream out = echoSocket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		
		dos.writeInt(len);
		if(len > 0) {
			dos.write(myByteArray, start, len);
		}
	}
	
	public static void getUserReqBytes(byte[] inputByteArray, Socket userSocket) throws IOException {
		sendUserReqBytes(inputByteArray, 0, inputByteArray.length, userSocket);
	}
	
	public static void sendUserReqBytes(byte[] myByteArray, int start, int len, Socket userSocket) throws IOException {
		OutputStream out = userSocket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		
		dos.writeInt(len);
		if(len > 0) {
			dos.write(myByteArray, start, len);
		}
	}
}

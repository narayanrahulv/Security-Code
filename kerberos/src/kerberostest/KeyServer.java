package kerberostest;

import java.net.*;
import java.nio.*;
import java.util.*;
import java.io.*;
import java.math.BigInteger;

import javax.xml.bind.DatatypeConverter;

public class KeyServer {
	public static void main(String[] args) throws IOException {
		int myPortNumber = Integer.parseInt(args[0]);
		
		try (
				ServerSocket serverSocket = new ServerSocket(myPortNumber);
				Socket clientSocket = serverSocket.accept();
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);
				BufferedReader in = new BufferedReader (
						new InputStreamReader(clientSocket.getInputStream()));
				) {
			byte[] ports = readBytes(clientSocket);
			
			String s2 = new String(ports);
			
			System.out.println("key server" + s2);
			
			/*System.out.println(ports[0]);
			System.out.println(ports[1]);
			
			final ByteBuffer bb = ByteBuffer.wrap(ports);
			bb.order(ByteOrder.LITTLE_ENDIAN);
			System.out.println(bb.getInt());
			
			bb.order(ByteOrder.BIG_ENDIAN);
			System.out.println(bb.getInt());
			
			System.out.println(ports[2]);
			System.out.println(ports[3]);
			
			System.out.println(ports[4]);
			System.out.println(ports[5]);
			
			System.out.println(ports[6]);
			System.out.println(ports[7]);
			
			System.out.println(ports[8]);
			
			System.out.println(ports.length);*/
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
		
		//*****
		/*String s1 = Arrays.toString(data);
		String s2 = new String(data);
		
		System.out.println(s2);
		
		System.out.println(Byte.toString(data[0]));
		System.out.println(Byte.toString(data[1]));
		
		byte[] portA = new byte[2];
		int res = 0;
		
		System.arraycopy(data, 0, portA, 0, 2);
		
		for (int i=0;i<portA.length;i++){
		    res = res | ((portA[i] & 0xff) << i*8);
		}
		
		System.out.println(res);*/
		
		/*BigInteger b = new BigInteger(portA);
		
		System.out.println(b);*/
		//*****		
		
		return data;
	}
}

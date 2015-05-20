package kerberos;

import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.*;

import javax.xml.bind.DatatypeConverter;

import cipher.CBC;

public class UserClient {
	static cipher.DES mydesobj = new cipher.DES("sboxes_default");
	
	static byte[] eKabWithKbWithKa = new byte[8];	//DES(Ka, DES(Kb, Kab))
	static byte[] eKabWithKb = new byte[8];	//DES(Kb, Kab)
	static byte[] eKabWithKa = new byte[8];	//DES(Ka, Kab)
	static byte[] Kab = new byte[8];	//session key
	
	//IV used to encrypt UserClient messages to UserServer
	static byte[] encryptiv = { (byte)0x0b, (byte)0xf2, (byte)0x96, (byte)0x40, (byte)0xc6, (byte)0x31, (byte)0x1c, (byte)0xad };
	
	public static void main(String[] args) throws IOException {
		//purpose of string args is to run with different host names, port numbers addresses etc at different times
		String hostNameKS = args[0];
		int portNumberKS = Integer.parseInt(args[1]);
		
		String hostNameUS = args[2];
		int portNumberUS = Integer.parseInt(args[3]);
		
		byte[] Ka = DatatypeConverter.parseHexBinary(args[5]);
		
		//create socket for key server request
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
						//send session request to KeyServer
						buildSessionReqBytes(userInput.getBytes(), echoSocket);
						
						//obtain E(Ka, Kab) || E(Ka, E(Kb, Kab)) from KeyServer & process session request response
						handleSessionRequestResponse(echoSocket, Ka);
						
						//send communication to UserServer
						//will include (bytes 0-7): DES(Kb,Kab)
						//and CBC(Kab, "user entered text")		
						byte[] cbcEncryptText = getCBCEncryptedText("simple plaintext from UserClient".getBytes(), encryptiv);
							
						byte[] userRequestBytes = new byte[eKabWithKb.length + cbcEncryptText.length];
						
						ByteBuffer userRequest = ByteBuffer.wrap(userRequestBytes);
						userRequest.put(eKabWithKb);
						userRequest.put(cbcEncryptText);
						
						buildUserReqBytes(userRequestBytes, userSocket);
						
						//obtain response from UserServer, decrypt and show in standard output
						handleUserServerResponse(userSocket);
						
						//DEBUG CODE
						//SIMPLE DEBUG CODE TO SEE IF CLIENT CAN COMMUNICATE WITH SERVER
						/*out.println(userInput.getBytes());
						System.out.println("Response: " + in.readLine());*/
						//END DEBUG CODE
					}
		} catch(UnknownHostException ex) {
			System.err.println("never heard of host " + hostNameKS);
			System.exit(1);
		} catch(IOException ex) {
			System.err.println("couldn't talk to " + hostNameKS);
			System.exit(1);
		}
	}
	
	//CREATING SESSION REQUEST
	//***************************************************	
	public static void buildSessionReqBytes(byte[] inputByteArray, Socket echoSocket) throws IOException {
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
	
	//READING SESSION REQUEST RESPONSE
	//***************************************************	
	public static void handleSessionRequestResponse(Socket echoSocket, byte[] Ka) throws IOException {
		byte[] fullEncryptedKab = getSessionRespBytes(echoSocket);
		
		//DEBUG CODE
		/*System.out.println(Arrays.toString(fullEncryptedKab));*/
		//END DEBUG CODE
		
		//first half contains DES(Ka,Kab)
		eKabWithKa = Arrays.copyOfRange(fullEncryptedKab, 0, 8);
		
		//DEBUG CODE
		/*System.out.println(Arrays.toString(eKabWithKa));*/
		//END DEBUG CODE
		
		//use Ka to decrypt first half and obtain Kab
		Kab = mydesobj.decrypt(Ka, eKabWithKa);
		
		//DEBUG CODE
		/*System.out.println(Arrays.toString(Kab));*/
		//END DEBUG CODE
		
		//second half contains DES(Kb,Kab)
		eKabWithKbWithKa = Arrays.copyOfRange(fullEncryptedKab, 8, 16);
		
		//DEBUG CODE
		/*System.out.println(Arrays.toString(eKabWithKbWithKa));
		System.out.println(Arrays.toString(Ka));*/
		//END DEBUG CODE

		//use Ka to decrypt second half of fullEncryptedKab so we can get DES(Kb,Kab) to send to UserClient
		eKabWithKb = mydesobj.decrypt(Ka, eKabWithKbWithKa);	//use Ka to decrypt and get DES(Kb,KAB) to send to UserClient
		
		//DEBUG CODE 
		/*System.out.println(Arrays.toString(eKabWithKb));*/
		//END DEBUG CODE
	}
	
	public static byte[] getSessionRespBytes(Socket echoSocket) throws IOException {
		InputStream echoSockStream = echoSocket.getInputStream();
		DataInputStream keyRespStream = new DataInputStream(echoSockStream);
		
		int len = keyRespStream.readInt();
		byte[] data = new byte[len];
		if(len > 0) {
			keyRespStream.readFully(data);
		}
		
		return data;
	}
	
	//SENDING COMMUNICATION TO USERSERVER
	//***************************************************
	public static byte[] getCBCEncryptedText(byte[] inputText, byte[] iv) {
		byte[] cbcCipher = encryptCBC(iv, Kab, inputText);
		
		return cbcCipher;
	}
	
	public static byte[] encryptCBC(byte[] iv, byte[] key, byte[] plaintext) {
		cipher.CBC mycbcobj = new cipher.CBC(mydesobj);
		mycbcobj.setIV(iv);
		
		return mycbcobj.encrypt(key, plaintext);
	}
	
	public static void buildUserReqBytes(byte[] inputByteArray, Socket userSocket) throws IOException {
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
	
	//PROCESSING RESPONSE FROM USERSERVER
	//***************************************************
	public static void handleUserServerResponse(Socket userSocket) throws IOException {
		byte[] encryptedUserServerResp = getSessionRespBytes(userSocket);
		
		byte[] ptUserServerResp = getCBCDecryptedText(encryptedUserServerResp);
		String userServerResponse = new String(ptUserServerResp);
		
		System.out.println(userServerResponse);
	}
	
	public static byte[] getUserRespBytes(Socket userSocket) throws IOException {
		InputStream userSockStream = userSocket.getInputStream();
		DataInputStream userRespStream = new DataInputStream(userSockStream);
		
		int len = userRespStream.readInt();
		byte[] data = new byte[len];
		if(len > 0) {
			userRespStream.readFully(data);
		}
		
		return data;
	}
	
	public static byte[] getCBCDecryptedText(byte[] inputText) {
		byte[] cbcPlaintext = decryptCBC(Kab, inputText);
		
		return cbcPlaintext;
	}
	
	public static byte[] decryptCBC(byte[] key, byte[] ciphertext) {
		cipher.CBC mycbcobj = new cipher.CBC(mydesobj);
		
		return mycbcobj.decrypt(key, ciphertext);
	}
}

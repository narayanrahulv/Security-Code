package kerberos;

import java.net.*;
import java.nio.*;
import java.util.*;
import java.io.*;

import javax.xml.bind.DatatypeConverter;

public class UserServer {
	static cipher.DES mydesobj = new cipher.DES("sboxes_default");
	
	static byte[] eKabWithKb = new byte[8];	//DES(Kb, Kab)
	static byte[] Kab = new byte[8];	//session key
	
	//IV used to encrypt UserServer response to UserClient
	static byte[] encryptiv = { (byte)0x0b, (byte)0xf2, (byte)0x96, (byte)0x40, (byte)0xc6, (byte)0x31, (byte)0x1c, (byte)0xad };
	
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
					//process communications
					//take in encrypted message from UserClient and decrypt/print to standard output
					handleUserReq(clientSocket, Kb);
					
					//create encrypted message in responst TO UserClient
					byte[] cbcEncryptText = getCBCEncryptedText("user server response to UserClient plaintext".getBytes(), encryptiv);
					
					byte[] serverRespBytes = new byte[cbcEncryptText.length];
					
					ByteBuffer serverResp = ByteBuffer.wrap(serverRespBytes);
					serverResp.put(cbcEncryptText);
					buildRespBytes(serverRespBytes, clientSocket);
				} catch(IOException ex) {	//no need for host exception because we are the host
			System.err.println("problem listening for connection on port " + myPortNumber);
			System.err.println(ex.getMessage());
		}
	}
	
	//HANDLING INCOMING COMMUNICATION REQUEST FROM USERCLIENT
	//***************************************************
	public static void handleUserReq(Socket clientSocket, byte[] Kb) throws IOException {
		byte[] clientContent = getUserReqBytes(clientSocket);		
		
		//DEBUG CODE: MAKE SURE WE'RE GETTING INFO FROM USERCLIENT
		/*System.out.println(Arrays.toString(encryptedKb));*/
		//END DEBUG CODE
		
		//first half contains DES(Kb,Kab)
		eKabWithKb = Arrays.copyOfRange(clientContent, 0, 8);
		
		//DEBUG CODE
		/*System.out.println(Arrays.toString(eKabWithKb));*/
		//END DEBUG CODE
		
		//use Kb to decrypt first half and obtain Kab
		Kab = mydesobj.decrypt(Kb, eKabWithKb);
		
		//DEBUG CODE
		/*System.out.println(Arrays.toString(Kab));*/
		//END DEBUG CODE		
		
		//second half contains CBC(Kab,"actual text content")
		byte[] cbcCipherContent = Arrays.copyOfRange(clientContent, 8, clientContent.length);
		
		//DEBUG CODE
		/*System.out.println(Arrays.toString(cbcContent));*/
		//END DEBUG CODE
		
		byte[] cbcPlainTextContent = getCBCDecryptedText(cbcCipherContent);
		String cbcPT = new String(cbcPlainTextContent);
		
		//DEBUG CODE
		System.out.println(cbcPT);
		//END DEBUG CODE
	}
	
	public static byte[] getUserReqBytes(Socket clientSocket) throws IOException {
		InputStream clientSockStream = clientSocket.getInputStream();
		DataInputStream userReqStream = new DataInputStream(clientSockStream);
		
		int len = userReqStream.readInt();
		byte[] data = new byte[len];
		if(len > 0) {
			userReqStream.readFully(data);
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
	
	//RESPONDING TO USERCLIENT
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
	
	public static void buildRespBytes(byte[] inputByteArray, Socket userSocket) throws IOException {
		sendServerRespBytes(inputByteArray, 0, inputByteArray.length, userSocket);
	}
	
	public static void sendServerRespBytes(byte[] myByteArray, int start, int len, Socket userSocket) throws IOException {
		OutputStream out = userSocket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		
		dos.writeInt(len);
		if(len > 0) {
			dos.write(myByteArray, start, len);
		}
	}
}

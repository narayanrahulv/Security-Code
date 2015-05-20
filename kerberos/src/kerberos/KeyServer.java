package kerberos;

import java.net.*;
import java.nio.*;
import java.util.*;
import java.io.*;

import cipher.*;

import javax.xml.bind.DatatypeConverter;

public class KeyServer {
	//map to store passed in user ports and keys
	//will be used to determine Ka and Kb to send back
	static HashMap<Integer, byte[]> userPortKeyList = new HashMap<>();
	
	//setting to 8 byte session key (64 bit session key)
	static byte[] sessionKab = new byte[8];	
	
	//retrieved Ka and Kb
	static byte[] Ka = new byte[8];
	static byte[] Kb = new byte[8];

	//returned encrypted session key
	static byte[] eKabWithKa = new byte[8];
	static byte[] eKabWithKb = new byte[8];
	static byte[] eKabWithKbWithKa = new byte[8];
	static byte[] fullEncryptedKab = new byte[16];
	
	static cipher.DES mydesobj = new cipher.DES("sboxes_default");
	static cipher.CBC mycbcobj = new cipher.CBC(mydesobj);
			
	public static void main(String[] args) throws IOException {
		int myPortNumber = Integer.parseInt(args[0]);
		
		try (
				ServerSocket serverSocket = new ServerSocket(myPortNumber);
				Socket clientSocket = serverSocket.accept();
				PrintWriter out = new PrintWriter(clientSocket.getOutputStream(),true);
				BufferedReader in = new BufferedReader (
						new InputStreamReader(clientSocket.getInputStream()));
				) {
					//starting at second argument will be a bunch of pairs of values i.e. a list of user port and user key values 
					//use these to populate userPortKeyList which will be used in determining Ka and Kb
					//ASSUMPTION: that parameters will always come in in the same order i.e. server port number first, followed by user_port user_key combos
					//store port as the "hashmap key" and key as the "hashmap value"
					for(int pk=1; pk<args.length; pk+=2) {  
						userPortKeyList.put(Integer.parseInt(args[pk]), DatatypeConverter.parseHexBinary(args[pk+1]));
					}
					
					//DEBUG CODE
					//CHECK IF WE'VE GOT ALL THE USER PORT USER KEY MAPPINGS
					displayUserPortsKeys(userPortKeyList);
					//END DEBUG CODE
					
					//set the common session key
					randomSessionKey(sessionKab);					
					
					//process initial session request
					handleSessionReq(clientSocket);
			
					//return E(Ka, Kab) || E(Ka, E(Kb, Kab)) to UserClient (session request response)
					buildSessionRespBytes(fullEncryptedKab, clientSocket);
					
					//DEBUG CODE
					/*String inputLine;
					while((inputLine = in.readLine()) != null) {
						//SIMPLE DEBUG TO SEE IF SERVER RESPONDS TO CLIENT
						out.println("hey you typed in " + inputLine.toUpperCase() + " right? This is my response");
					} 			*/
					//END DEBUG CODE
				} catch(IOException ex) {	//no need for host exception because we are the host
					System.err.println("problem listening for connection on port " + myPortNumber);
					System.err.println(ex.getMessage());
				}
	}
	
	//CREATING SESSION KEY
	//***************************************************	
	public static void randomSessionKey(byte[] sessionkey) {
		Random myRand = new Random();	
		myRand.nextBytes(sessionkey);
	}
	
	//HANDLING INCOMING SESSION REQUEST FROM USERCLIENT
	//***************************************************	
	public static void handleSessionReq(Socket clientSocket) throws IOException {	
		//GET PORTS
		byte[] ports = getSessionReqBytes(clientSocket);
		
		String sPorts = new String(ports);
		String[] sPortList = sPorts.split(" ");
		
		//DEBUG CODE: MAKE SURE WE'VE GOT RIGHT PORTS FROM USER INPUT
		//System.out.println(sPorts);
		//END DEBUG CODE
		
		//GET KEYS FOR PORTS
		if(sPortList.length == 2) {
			Ka = getPortKey(Integer.parseInt(sPortList[0]));
			Kb = getPortKey(Integer.parseInt(sPortList[1]));
			
			//DEBUG CODE: MAKE SURE WE'RE RETRIEVING THE RIGHT KEYS
			/*System.out.println(Ka);
			System.out.println(Kb);*/
			//END DEBUG CODE
		}
		else {
			System.out.println("you need to pass in 2 port numbers to make a session request");
		}
		
		//build fullEncryptedKab
		eKabWithKa = getEncryptedSessionKeyForA();	//Kab encrypted under DES with Ka
		eKabWithKb = getEncryptedSessionKeyForB();	//Kab encrypted under DES with Kb
		eKabWithKbWithKa = mydesobj.encrypt(Ka, eKabWithKb); //Kab encrypted under DES with Kb and then that encrypted value encrypted under DES with Ka
		
		ByteBuffer combined = ByteBuffer.wrap(fullEncryptedKab);
		combined.put(eKabWithKa);
		combined.put(eKabWithKbWithKa);
		
		//DEBUG CODE: MAKE SURE WE'RE GETTING SOMETHING FOR fullEncryptedKab and that it is 16 characters wide
		/*System.out.println(Arrays.toString(fullEncryptedKab));*/
		//END DEBUG CODE
	}
	
	public static byte[] getSessionReqBytes(Socket clientSocket) throws IOException {
		InputStream clientSockStream = clientSocket.getInputStream();
		DataInputStream keyReqStream = new DataInputStream(clientSockStream);
		
		int len = keyReqStream.readInt();
		byte[] data = new byte[len];
		if(len > 0) {
			keyReqStream.readFully(data);
		}
		
		return data;
	}
	
	public static byte[] getPortKey(int portNum) {
		byte[] returnedKey = userPortKeyList.get(portNum);
		
		return returnedKey;
	}
	
	public static byte[] getEncryptedSessionKeyForA() {
		byte[] eKaKab = mydesobj.encrypt(Ka, sessionKab);
		
		return eKaKab;
	}
	
	public static byte[] getEncryptedSessionKeyForB() {
		byte[] eKbKab = mydesobj.encrypt(Kb, sessionKab);
		
		return eKbKab;
	}
	
	//HANDLING RESPONSE OF SESSION REQUEST TO USERCLIENT
	//***************************************************
	public static void buildSessionRespBytes(byte[] inputByteArray, Socket clientSocket) throws IOException {
		sendSessionRespBytes(inputByteArray, 0, inputByteArray.length, clientSocket);
	}
	
	public static void sendSessionRespBytes(byte[] myByteArray, int start, int len, Socket clientSocket) throws IOException {
		OutputStream out = clientSocket.getOutputStream();
		DataOutputStream dos = new DataOutputStream(out);
		
		dos.writeInt(len);
		if(len > 0) {
			dos.write(myByteArray, start, len);
		}
	}

	//***********************
	//DEBUG CODE	
	public static void displayUserPortsKeys(Map portKeyListing) {
		Iterator<Integer> keySetIterator = portKeyListing.keySet().iterator();

		while(keySetIterator.hasNext()){
		  Integer key = keySetIterator.next();
		  System.out.println("key: " + key + " value: " + portKeyListing.get(key));
		}
	}/**/	
	//END DEBUG CODE
	//***********************
}

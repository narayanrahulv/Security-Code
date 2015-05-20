# Security-Code
Simple kerberos implementation consisting of 
1. Key Server
2. User Server
3. User Client

The key server is run by passing in a key server host name and port followed by a series of user client and user server
port and key pairs
Example: KeyServer keyserverlocalhost 8000 8001 hexkey 8002 hexkey

The user server is run by passing in a user server host name and port number
Example: UserServer userserverlocalhost 8002

The user client is run by passing in a key server host name and port, a user server host name and port, and the user client 
port and key
Example: UserClient keyserverlocalhost 8000 userserverlocalhost 8002 8001 hexkey

When all 3 are up and running the user client makes a "session request" to the key server. The session request can be made
by typing in (for example) 8001 and 8002. The key server looks up the hex key associated with each port number and sends back
a session key to the user client. The session key sent back to the user client will be encrypted using the client's hex key. 
In addition it will send the client an encrypted form of the user server's key. 

Once the client receives the session request, it will send the user server the session key which will be encrypted using the
user server's hex key (this is returned by the key server as part of the session request). Once both the user client and user
server have the session key, they can communicate to each other like a simple chat application. Each line of chat sent over
will be encrypted by the sender using the session key and decrypted at the recipient using the session key.

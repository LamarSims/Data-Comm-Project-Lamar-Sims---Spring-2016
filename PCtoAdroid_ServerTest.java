import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Enumeration;
import java.util.Scanner;

public class PCtoAdroid_ServerTest {
	//Declare and initialize variables
    public static String SERVERIP = "10.0.2.15";  
    public static final int SERVERPORT = 8081;
    private ServerSocket serverSocket;
    private Socket client;
    private InputStream inFromClient;
    private OutputStream outToClient;
    private static Scanner scanInput = new Scanner(System.in);
    private boolean clientConnected = false;
    
    /* no-arg constructor for PCtoAndroid_ServerTest*/
    public PCtoAdroid_ServerTest(){
    	SERVERIP = getLocalIpAddress();
    }
    
    /* Starts the server and connects a client to the server*/
    public void startServer(){
    	new Thread(new Runnable(){
    		@Override
    		public void run(){
    			if (SERVERIP != null){
    				try {
    					System.out.println("Listening on IP: " + SERVERIP + "\nListening on Port: " + SERVERPORT);
                        serverSocket = new ServerSocket(SERVERPORT); //initialize the socket for the server
                        client = serverSocket.accept(); //find a client to connect to the server
                        System.out.println("Connected to: " + client.getRemoteSocketAddress());
                        clientConnected = true;
                        
                        readMessageFromClient(); //calls method to handle message sent from the client to the server
                        sendMessageToClient(); //calls method to handle messages sent to the client from the server
    				}catch (Exception e){
    					e.printStackTrace();
    				}
    			}
    		}
    	}).start();
    }
    
    /* Uses a thread to handle all the messages that the server sends out to the client that is connected */
    public void sendMessageToClient(){
    	new Thread(new Runnable(){
    		@Override
    		public void run(){       
            	System.out.print("Enter message to send to the client. Enter \"Exit\" when you want to close the server.\n");
            	//as long as the Server and Client sockets are open and the client is still connected messages can be sent
    			while (!serverSocket.isClosed() && !client.isClosed() && clientConnected) {
                    try {
                        String outString = scanInput.nextLine(); //get the message from the console to be sent to the client
                        
                        //close Server and Client sockets
                    	if (outString.trim().equalsIgnoreCase("Exit")){
                    		System.out.println("----------\nExiting server.\n----------");
                    		scanInput.close();
                    		serverSocket.close();
                    		client.close();
                    		break;
                    	}
                        
                    	//get the output stream to the client and write the message to the output stream
                    	outToClient = client.getOutputStream();
                    	DataOutputStream out = new DataOutputStream(outToClient);
                        out.writeUTF(outString);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
    		}
    	}).start();
    }
    
    /*Uses thread to handles the reading of messages sent to the server from the client */
    public void readMessageFromClient(){
        new Thread(new Runnable(){
            @Override
            public void run() {
            	//as long as the Server and Client sockets are open and the client is connected reading in messages is allowed
                while (!serverSocket.isClosed() && !client.isClosed() && clientConnected){
                	String clientResponse = "";
                	try {
                		//Get the input stream from the client and read in its message for the server
                    	inFromClient = client.getInputStream();
                    	DataInputStream in = new DataInputStream(inFromClient);
                    	clientResponse = in.readUTF();
                    	
                    	//display message, if there is no message then input stream will wait 5 seconds before checking again
                    	if (!clientResponse.equals("")){
                    		System.out.println("----------\n"
                        			+ "From " + client.getRemoteSocketAddress() + ":\n"
                        			+ clientResponse + "\n"
                        			+ "----------");
                        }else {
                        	inFromClient.wait(5000);
                        }                    	
                    }catch (EOFException eofe) {
                    	System.out.println("Client has disconnected.");
                    	clientConnected = false;
                    } catch (Exception e){
                    	e.printStackTrace();
                    }                    
                }
                //If both server and client socket are online but client is not connected find another client to connect to.
                if (!serverSocket.isClosed() && !client.isClosed() && !clientConnected){
                	findClientToConnect();
                }
            }
        }).start();
    }
    
    /* Used to find another client to connect to if the previous client has disconnected but server is still running */
    public void findClientToConnect(){
    	new Thread(new Runnable(){
    		@Override
    		public void run(){
    			Scanner scan = new Scanner(System.in);
    			String ecs = "";
    			while (true){
        			try {
        				System.out.print("Enter \"Exit\" to close server or enter any key to "
        						+ "continue searching for connection: ");
        				ecs = scan.nextLine(); //read in whether to close server or continue looking for a new connection
        				if (ecs.equalsIgnoreCase("Exit")){
        					//close server and client sockets
        					scanInput.close();
        					serverSocket.close();
        					client.close();
        					System.out.println("Server has been closed.");
        					break;
        				} else {
        					//listen for a new connection
        					System.out.println("Listening on IP: " + SERVERIP + "\nListening on Port: " + SERVERPORT);
        					serverSocket.setSoTimeout(10000); //server will stop listening for connection after 10 seconds
        					client = serverSocket.accept(); //get new connection
    						System.out.println("Connected to: " + client.getRemoteSocketAddress());
                            clientConnected = true;
                            readMessageFromClient(); //start method to handle messages coming from client to server
                            sendMessageToClient();  //start method to handle messages sent to client form server
                            break;
        				}
    					
    				} catch (SocketTimeoutException se) {
    					System.out.println("Server timeout, no connection made in 10sec.");
    				} catch (IOException ioe) {
    					ioe.printStackTrace();
    				} catch (Exception e){
    					e.printStackTrace();
    				}
    			}
    		}
    	}).start();;
    }
    
    // get the IP address for the server
    private String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) { 
                    	return inetAddress.getHostAddress().toString(); }
                }
            }
        } catch (SocketException ex) {
        	System.out.println(ex.toString());
        }
        return null;
    }
    
    /* main method */
    public static void main(String [] args){
       PCtoAdroid_ServerTest past = new PCtoAdroid_ServerTest();
       past.startServer();
    }
}

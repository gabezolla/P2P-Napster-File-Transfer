package napsterTransfer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

public class Servidor {
	
	// <127.0.0.1:8080, arquivos>
    public static ConcurrentHashMap<String, List<String>> filesTable; 
    // <PortaUDP, PortaTCP)
    public static ConcurrentHashMap<Integer, String> peerRelation;
    
    public static ConcurrentHashMap<Integer, String> getPeerRelation() {
		return peerRelation;
	}

	public static void setPeerRelation(ConcurrentHashMap<Integer, String> peerRelation) {
		Servidor.peerRelation = peerRelation;
	}

	public static void setFilesTable(ConcurrentHashMap<String, List<String>> filesTable) {
		Servidor.filesTable = filesTable;
	}

	public ConcurrentHashMap<String, List<String>> getFilesTable() {
		return filesTable;
	}

	public static class ServerThread extends Thread {

        private DatagramPacket recPack;
        private DatagramSocket serverSocket;

        public ServerThread(DatagramPacket recPack, DatagramSocket serverSocket) {
            this.recPack = recPack;
            this.serverSocket = serverSocket;
        }

        public void run() {
            Gson gson = new Gson();

            // Tratar a mensagem recebida
            String info = new String(recPack.getData(), recPack.getOffset(), recPack.getLength()); // Mensagem recebida
            Mensagem mensagem = new Mensagem(); 
            mensagem = gson.fromJson(info, Mensagem.class); // Converter a mensagem recebida (GSON) para um objeto da classe Mensagem.
            String action = mensagem.getAction().toUpperCase(); // Ação requisitada pelo peer

            try { 

                // Tratar o join
                if (action.equals("JOIN")) {                    
                    peerJoin(recPack, serverSocket, mensagem);
                }

                // Tratar o search
                else if(action.equals("SEARCH")) {
                    peerSearch(recPack, serverSocket, mensagem);
                }
                
                // Tratar o leave
                else if(action.equals("LEAVE")) {
                	peerLeave(recPack, serverSocket);
                }
                
                // Tratar o update
                else if(action.equals("UPDATE")) {
                	peerUpdate(recPack, serverSocket, mensagem);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

	
	/* public static class KeepAliveThread extends Thread {
		
		private DatagramSocket serverSocket;

		public KeepAliveThread(DatagramSocket serverSocket) {
			this.serverSocket = serverSocket;
		}
		
		public void run() {
			try {
				if (!filesTable.isEmpty()) {
					for (Map.Entry<String, List<String>> entry : filesTable.entrySet()) {

						DatagramSocket s = new DatagramSocket();
						byte[] buf = new byte[1000];
						DatagramPacket recPack = new DatagramPacket(buf, buf.length);
						InetAddress hostAddress = InetAddress.getByName(entry.getKey().split(":")[0]);
						int port = Integer.parseInt(entry.getKey().split(":")[1]);

						String outString = "ALIVE"; // message to send
						buf = outString.getBytes();

						DatagramPacket out = new DatagramPacket(buf, buf.length, hostAddress, port);
						s.send(out); // send to the server

						s.setSoTimeout(10000); // set the timeout in millisecounds.

						while (true) { // recieve data until timeout
							try {
								s.receive(recPack);
								String received = new String(recPack.getData(), 0, recPack.getLength()).toUpperCase();
								
							} catch (SocketTimeoutException e) {
								// timeout exception.
								System.out.println("Peer [" + entry.getKey().split(":")[0]+ "]:" + entry.getKey().split(":")[1] + " morto. Eliminando seus arquivos");
								try {
									peerLeave(recPack, serverSocket);
								} catch (Exception e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								
								s.close();
							}
						}
					}
					Thread.sleep(30000);					
				}
			} catch (SocketException e1) {
				// TODO Auto-generated catch block
				// e1.printStackTrace();
				System.out.println("Socket closed " + e1);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	} */
	
	public static class KeepAliveThread extends Thread{
		private DatagramSocket serverSocket;
		
		public KeepAliveThread(DatagramSocket serverSocket) {
			this.serverSocket = serverSocket;
		}
		
		public void run() {			
			while(true) {
				try {
					// Intervalo de 30 segundos
					Thread.sleep(30000);
					
					// Mensagem que será enviada para o peer
					String sentence = "ALIVE";
					byte[] sendData = new byte[sentence.length()];
					sendData = sentence.getBytes();
					
					if (!filesTable.isEmpty()) { // Realiza apenas se tiver peers na Hash.
						
						// Loop por toda a filesTable
						for (Map.Entry<String, List<String>> entry : filesTable.entrySet()) {
							
							// Informações do peer que iremos enviar
							InetAddress IPAddress = InetAddress.getByName(entry.getKey().split(":")[0]);
							String ip = IPAddress.toString().split("/")[1];
							int port = Integer.parseInt(entry.getKey().split(":")[1]);
							String peerID = IPAddress + ":" + port;

							// Envio da mensagem 
							DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, port);
							serverSocket.send(sendPacket);
							try {
								// Timeout, caso não recebamos a mensagem, gera a exception SocketTimeoutException e encaminha
								serverSocket.setSoTimeout(3000);
								byte[] data = new byte[1000];
								DatagramPacket recPack = new DatagramPacket(data, data.length);

								try {
									serverSocket.receive(recPack);
									String response = new String(recPack.getData(), 0, recPack.getLength());

								} catch (SocketTimeoutException e) {
									System.out.println("Peer [" + ip + "]:[" + port
											+ "] morto. Eliminando seus arquivos [" + entry.getValue() + "]");
									filesTable.remove(peerID);
								}
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				}catch(Exception e) {e.printStackTrace();}
			}
		}
	}
	
	
    public static void main(String[] args) {

        try {
        	
        	ConcurrentHashMap<String, List<String>> filesTable = new ConcurrentHashMap<String, List<String>>();
        	ConcurrentHashMap<Integer, String> peerRelation = new ConcurrentHashMap<Integer, String>();
        	setFilesTable(filesTable);
        	setPeerRelation(peerRelation);
        	
        	// Porta fixa de 10098, dado no relatório
            DatagramSocket serverSocket = new DatagramSocket(10098);

            while (true) {
                System.out.println("Esperando mensagem...");                

                // Recebimento de pacote
                byte[] recBuffer = new byte[1024];
                DatagramPacket recPack = new DatagramPacket(recBuffer, recBuffer.length);
                serverSocket.receive(recPack); // bloqueia até receber um pacote
                
                // Iniciar a thread com os parâmetros necessários
                ServerThread st = new ServerThread(recPack, serverSocket);
                st.run();
                
                // KeepAlive
                // DatagramSocket keepAliveServer = new DatagramSocket()
                // KeepAliveThread kt = new KeepAliveThread(serverSocket);
                // kt.start();
                
                // Debug do HashMap
                // System.out.println("SITUAÇÃO DA HASH: " + filesTable.toString());
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    /* 
     ** Função para tratar o JOIN do peer.
     */
    public static void peerJoin(DatagramPacket recPack, DatagramSocket serverSocket, Mensagem mensagem) throws Exception {
    	Gson gson = new Gson();
    	
    	// Informações do peer para devolver o pacote
    	InetAddress IPAddress = recPack.getAddress();
    	int port = recPack.getPort();
    	
    	// Informações do peer (peerID == IP:Port)    	
        String peerID = mensagem.getIp() + ":" + mensagem.getPort();
        
        // Relação entre porta do SO (UDP) e porta passada pelo cliente
        peerRelation.put(port, mensagem.getPort());
        
        // Informações do Arquivo
        String filesString = mensagem.getMessage().trim();
        String filesArray[] = filesString.split("; "); // array de arquivos para guardar na hash.
        
        // Armazenar filesArray[] em uma ArrayList
        List<String> fileList;
        fileList = Arrays.asList(filesArray);
        
        // Armazenar fileList no HashMap referente a qual peer enviou os arquivos
        filesTable.put(peerID, fileList);
    
        // Resposta do servidor ao peer
        byte[] sendBuf = new byte[1024];
        Mensagem response = new Mensagem(); // Objeto response terá como action = "JOIN_OK" e message = peerID
        response.setAction("JOIN_OK");
        response.setMessage(peerID);
        String responseJSON = gson.toJson(response, Mensagem.class); // conversão do objeto da classe Mensagem para GSON.
        sendBuf = responseJSON.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);
        serverSocket.send(sendPacket);
    
        // Após mensagem ser enviada
        System.out.println("Peer ["+IPAddress+"]:["+mensagem.getPort()+"] adicionado com arquivos " + fileList.toString());
    }
    
    /* 
    ** Função para tratar o LEAVE do peer.
    */
    public static void peerLeave(DatagramPacket recPack, DatagramSocket serverSocket) throws Exception {
    	Gson gson = new Gson();
    	    	
    	// Informações do peer (UDP)
    	InetAddress IPAddress = recPack.getAddress();
    	int port = recPack.getPort();
    	
    	// Port de comunicação TCP
    	String tcpPort = peerRelation.get(port);
        String peerID = IPAddress.getHostAddress() + ":" + tcpPort;      
        
        // Remove o peer da hash e os arquivos dele
        filesTable.remove(peerID);
        peerRelation.remove(port);
        
        // Gerar um objeto Mensagem para comunicação
        Mensagem response = new Mensagem(); // Objeto response terá como action = "JOIN_OK" e message = peerID
        response.setMessage("LEAVE_OK");
        String responseJSON = gson.toJson(response, Mensagem.class); // conversão do objeto da classe Mensagem para GSON.
        
        // Resposta do servidor ao peer
        byte[] sendBuf = new byte[1024];
        sendBuf = (responseJSON).getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);
        serverSocket.send(sendPacket);
    
        // Após mensagem ser enviada
        System.out.println("Mensagem enviada...");
    }
    
    
	public static void peerSearch(DatagramPacket recPack, DatagramSocket serverSocket, Mensagem mensagem) throws Exception {
    	Gson gson = new Gson();

    	// Informações do peer
    	InetAddress IPAddress = recPack.getAddress();
    	int port = recPack.getPort();
    	
    	// Port de comunicação TCP
    	String tcpPort = peerRelation.get(port);
        String peerID = IPAddress.getHostAddress() + ":" + tcpPort;    
    	
        String fileDesired = mensagem.getMessage().trim(); // Arquivo desejado pelo peer
    	ArrayList<String> arrayOfPeers = new ArrayList<>(); // Lista temporária para armazenar os peers que contém o arquivo
    	
    	System.out.println("Peer ["+IPAddress+"]:["+tcpPort+"] solicitou arquivo [" + fileDesired + "]");
    	
    	for(Map.Entry<String, List<String>> entry : filesTable.entrySet()) {
    		if(entry.getValue().contains(fileDesired)) {
    			arrayOfPeers.add(entry.getKey());
    		}
    	}
    	
    	// Gerar um objeto Mensagem para comunicação
        Mensagem response = new Mensagem(); // Objeto response terá como action = "JOIN_OK" e message = peerID
        response.setMessage("Peers com arquivo solicitado: " + arrayOfPeers.toString());
        String responseJSON = gson.toJson(response, Mensagem.class); // conversão do objeto da classe Mensagem para GSON.
        
    	
    	// Resposta do servidor ao peer
        byte[] sendBuf = new byte[1024];
        sendBuf = (responseJSON).getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);
        serverSocket.send(sendPacket);
    }
	
	public static void peerUpdate(DatagramPacket recPack, DatagramSocket serverSocket, Mensagem mensagem) throws Exception {
    	Gson gson = new Gson();

    	// Informações do peer
    	InetAddress IPAddress = recPack.getAddress();
    	int port = recPack.getPort();
    	String fileDownloaded = mensagem.getMessage().trim();
    	
    	// Port de comunicação TCP
    	String tcpPort = peerRelation.get(port);
        String peerID = IPAddress.getHostAddress() + ":" + tcpPort;    
    	
    	List<String> newList = new ArrayList<>();
    	newList.addAll(filesTable.get(peerID));
    	newList.add(fileDownloaded);

    	filesTable.remove(peerID);
    	filesTable.put(peerID, newList);  
    	

    	// Gerar um objeto Mensagem para comunicação
        Mensagem response = new Mensagem(); // Objeto response terá como action = "JOIN_OK" e message = peerID
        response.setMessage("UPDATE_OK");
        String responseJSON = gson.toJson(response, Mensagem.class); // conversão do objeto da classe Mensagem para GSON.
        
    	
    	// Resposta do servidor ao peer
        byte[] sendBuf = new byte[1024];
        sendBuf = responseJSON.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendBuf, sendBuf.length, IPAddress, port);
        serverSocket.send(sendPacket);
		
	}
}




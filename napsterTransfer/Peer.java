package napsterTransfer;

import java.util.Scanner;
import com.google.gson.*;
import java.io.*;
import java.net.*;

public class Peer {
	
	public static String peerFolder;
	public static InetAddress peerIP;
	public static int peerPort;
	
	// Peer que receberá os arquivos por TCP
	// Referência: https://gist.github.com/CarlEkerot/2693246
    public static class ClientPeer extends Thread {    	    	
    	private Socket s;
    	private String hostIP;
    	private String file;
    	private int hostPort;
    	private DatagramSocket peerSocket;
    	private String peerID;
    	
    	public ClientPeer(String hostIP, int hostPort, String file, String peerID, DatagramSocket peerSocket) {
    		this.hostIP = hostIP;
    		this.hostPort = hostPort;
    		this.file = file; 
    		this.peerID = peerID;
    		this.peerSocket = peerSocket;
    	}
    	
    	public void run() {
    		try {
    			s = new Socket(hostIP, hostPort);
    			
    			// Enviar o arquivo desejado para o server.
    			OutputStream os= s.getOutputStream();
    			DataOutputStream serverWriter = new DataOutputStream(os);    			
    			// serverWriter.writeBytes(peerFolder + ", " + file + "\n");
    			serverWriter.writeBytes(peerFolder + ", " + file + "\n");
    			
    			// Resposta do server
    			InputStreamReader isrServer = new InputStreamReader(s.getInputStream());
    			BufferedReader serverReader = new BufferedReader(isrServer);
    			String folder = serverReader.readLine();  		
    			
    			// Tratar download negado
    			if(folder.equals("DOWNLOAD_NEGADO")) {
    				System.out.println("Download negado. Tente novamente com outro peer");
    				peerUDPClass peerUDP = new peerUDPClass(peerID, peerSocket);
    		        peerUDP.start();
    		        return;
    			}
    			
    			// Estruturas para armazenar o file
    			DataOutputStream dos = new DataOutputStream(s.getOutputStream());
    			FileInputStream fis = new FileInputStream(folder + "/" + file);
    			byte[] buffer = new byte[4096];
    			
    			// Leitura do arquivo
    			int read;
    			while ((read=fis.read(buffer)) > 0) {
    				dos.write(buffer,0,read);
    			}
    			
    			dos.flush();
    			fis.close();
    			dos.close();
    			serverWriter.close();
		        
    			System.out.println("Arquivo " + file + " baixado com sucesso na pasta " + peerFolder);
		        s.close();
		        
		        peerUDPClass peerUDP = new peerUDPClass(peerID, peerSocket);
		        peerUDP.start();
    		    
    		    try {
    				Thread.sleep(100);
    			} catch (InterruptedException e) {
    				// TODO Auto-generated catch block
    				e.printStackTrace();
    			}
    		        		    
    			/* byte b[] = new byte[20002];
        		InputStream is = s.getInputStream();
        		FileOutputStream fr = new FileOutputStream(peerFolder + "/" + file);
        		is.read(b, 0, b.length);
        		fr.write(b, 0, b.length);
        		*/
        		
    			//sendFile(file);
    		} catch (Exception e) {
    			e.printStackTrace();
    		}		
    	}
    	
   }
    
    public static class downloadClass extends Thread {
    	
    	public String request;
    	public DatagramSocket peerSocket;
    	public String peerID;
    	
    	public downloadClass(String request, String peerID, DatagramSocket peerSocket) {
    		this.request = request;
    		this.peerID = peerID;
    		this.peerSocket = peerSocket;
    	}
    	
    	public void run() {
    		// Informações sobre o arquivo e o host escolhido
    		String downloadInfo[] = request.split(": ")[1].split(", ");
    		String downloadIP = (downloadInfo[0]);
    		int downloadPort = Integer.parseInt(downloadInfo[1]);  
    		String downloadFile = downloadInfo[2];
    		
    		// Gerar novamente o UDP.
    		ClientPeer cp = new ClientPeer(downloadIP, downloadPort, downloadFile, peerID, peerSocket);
    		cp.start();
    		
    		try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}    		
    	}
    }
    
    public static class peerUDPClass extends Thread {
    	
    	public String peerID;
    	public DatagramSocket peerSocket;
    	
    	public peerUDPClass(String peerID, DatagramSocket peerSocket) {
    		this.peerID = peerID;
    		this.peerSocket = peerSocket;    		
    	}
    	
    	public void run() {
    		Scanner scan = new Scanner(System.in);
    		System.out.println("Digite a ação, seguida de ':' (dois pontos) e as informações necessárias: ");
    		
    		while(scan.hasNextLine()) {
            	String actionString = scan.nextLine();
            	
            	if(actionString.split(": ")[0].toUpperCase().equals("DOWNLOAD")) {
            		downloadClass dc = new downloadClass(actionString, peerID, peerSocket);
            		dc.start();
            	}
            	
            	String info;
            	
				try {					
					info = requestAction(actionString, peerSocket, peerID);
					if(info.equals("LEAVE_OK")) System.exit(0);
				} catch (Exception e) {
					e.printStackTrace();
				}
            } 
    		System.out.println("Digite a ação, seguida de ':' (dois pontos) e as informações necessárias: ");
    		
    	}
    }
    
    public static class peerTCPClass extends Thread {
    	
    	public ServerSocket serverSocket;
    	
    	public peerTCPClass(ServerSocket serverSocket) {
    		this.serverSocket = serverSocket;    		
    	}
    	
    	public void run() {
    		
    		while(true) {
    			try {
            		// System.out.println("DENTRO DO TCP");
					Socket clientSocket = serverSocket.accept();				
					
					System.out.println("Conexão requerida de " + clientSocket.getInetAddress() + ":" + clientSocket.getPort());
					
					// Ler do cliente
					BufferedReader br = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
					String received = br.readLine();
					
					// Pasta do cliente
					String file = received.split(",")[1].trim();
					String folder = received.split(",")[0].trim();
					
					// System.out.println("Arquivo: " + file + " na pasta peerFolder (server): " + peerFolder);
					File newFile = new File(peerFolder + "/" + file);
										
					// Enviar para o cliente
					OutputStream os= clientSocket.getOutputStream();
					DataOutputStream output = new DataOutputStream(os);
					
					if(Math.random() > 0.8) {
						output.writeBytes("DOWNLOAD_NEGADO\n");	
						return;
					}
					
					else output.writeBytes(peerFolder + "\n");
					
					DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
					FileOutputStream fos = new FileOutputStream(folder + "/" + file);
					byte[] buffer = new byte[4096];
					
					int read;
					while ((read=dis.read(buffer)) > 0) {
				            fos.write(buffer,0,read);
					}
					
					System.out.println("Download concluído!");
					
					fos.close();
					dis.close();
					
					try {
			        	Thread.sleep(100);
			        } catch (InterruptedException e) {
			        	// TODO Auto-generated catch block
			        	e.printStackTrace();
			        }
			        
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
    			return;
    		}
    	}
    	
    }
    
    public static void main(String[] args) throws Exception {
        Scanner scan = new Scanner(System.in);
        DatagramSocket peerSocket = new DatagramSocket();

        // FORMATO: 'ação', 'pasta dos arquivos', 'etc..' 
        // E.G: join, IP, porta, C:/Desktop/pasta
        System.out.println("Oi peer! Para inicializar, digite join: IP, PORTA, PASTA\n" + 
        "E.G: join: 127.0.0.1, 8080, C:/Desktop/pasta");
        
        // String de inicialização
        String iniciateString = scan.nextLine().trim(); 
        
        // Captura da pasta para guardar no objeto
        peerFolder = iniciateString.split(": ")[1].split(", ")[2];
        int peerPortTCP = Integer.parseInt(iniciateString.split(": ")[1].split(", ")[1]);

        // Conexão com o servidor (UDP)
        String peerID = joinServer(iniciateString, peerSocket);
 
        // Guardar IP e porta na classe peer
        peerIP = InetAddress.getByName(peerID.split(":")[0]);
        // peerPort = Integer.parseInt(peerID.split(":")[1]);
        peerPort = peerPortTCP;
        
        // DatagramSocket keepAliveSocket = new DatagramSocket(peerPort);
        
        // Continuos UDP
        peerUDPClass peerUDP = new peerUDPClass(peerID, peerSocket);
        peerUDP.start();
        
        // Continuous Alive
        // respondAlive keepAlive = new respondAlive(keepAliveSocket, peerIP);
        // keepAlive.start();
        
        // Continuous TCP
        ServerSocket serverSocket = new ServerSocket(peerPort);
        peerTCPClass peerTCP = new peerTCPClass(serverSocket); 
        peerTCP.start();
        
    }
    
    public static class respondAlive extends Thread {
    	public DatagramSocket peerSocket;
    	public InetAddress IPAddress;
    
    	public respondAlive(DatagramSocket peerSocket, InetAddress IPAddress) {
    		this.peerSocket = peerSocket;
    		this.IPAddress = IPAddress;
    	}
    	
    	public void run() {
    		
    		while(true) {
				try {
					
					// Recebe o pacote para o Servidor
					byte[] receiveData = new byte[4096];
					DatagramPacket recPack= new DatagramPacket(receiveData, receiveData.length);
					peerSocket.receive(recPack);		
					String sentence = new String(recPack.getData(), 0, recPack.getLength());
					int serverPort = recPack.getPort();
					Scanner scan = new Scanner(System.in);
					System.out.println(sentence);
					
					// Capta o ALIVE_OK
					String response = scan.nextLine();						
					
					// Responde para o server
					byte[] sendData = new byte[4096];
					sendData = response.toUpperCase().trim().getBytes();
					DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
					peerSocket.send(sendPacket);						
					
				} catch(Exception e) {e.printStackTrace();}
			}
    	}
    }

    public static String joinServer(String iniciateString, DatagramSocket peerSocket) throws Exception {
        int serverPort = 10098;
        Gson gson = new Gson();
        
        // Separar a ação (join) do resto da mensagem 
        String infoArray[] = iniciateString.split(": ");  
        
        // Separar as informações passadas por vírgulas (ip, port)
        String peerInfo[] = infoArray[1].split(", ");

        // IP e porta passados pelo peer
        InetAddress IPAddress = InetAddress.getByName(peerInfo[0].trim());
        int port = Integer.parseInt(peerInfo[1].trim());
        
        // Declarar mensagem
        Mensagem mensagem = new Mensagem();      
        
        // Caso exclusivo de join (passar os arquivos na mensagem)            
        // infoArray[0] = action (JOIN) && peerInfo[] = ip, port, mensagem 
        mensagem.setAction(infoArray[0].trim());
        mensagem.setIp(peerInfo[0].trim());
        mensagem.setPort(peerInfo[1].trim());
        
        // Arquivos a partir da pasta 
        String filesPath = peerInfo[2].trim();
        String auxiliar = readFiles(filesPath);
        String filesRead = auxiliar.trim().substring(0, auxiliar.length()-2); // Apenas para tirar um bug onde o ';' entrava no nome do arquivo
        mensagem.setMessage(filesRead.trim());        
                
        // Converter o objeto em GSON para enviar para o servidor
        String mensagemJSON = gson.toJson(mensagem);
                
        // Pacote enviado para o servidor  
        byte[] sendData = new byte[1024];
        sendData = mensagemJSON.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
        peerSocket.send(sendPacket);

        // Pacote de recebimento (recebido pelo peer, vindo do servidor)
        byte[] recBuffer = new byte[1024];
        DatagramPacket recPack = new DatagramPacket(recBuffer, recBuffer.length);
        peerSocket.receive(recPack);
        
        // Print resposta do server
        String info = new String(recPack.getData(), recPack.getOffset(), recPack.getLength());
        Mensagem response = gson.fromJson(info, Mensagem.class);
        System.out.println(response.getAction() + ". Sou peer " + response.getMessage() + " com arquivos: " + mensagem.getMessage());
                
        // Retorna informações sobre o peer
        return response.getMessage();
    }
    
    public static String requestAction(String actionString, DatagramSocket peerSocket, String peerID) throws Exception {
    	int serverPort = 10098;
        Gson gson = new Gson();
                
    	// Separar a ação (join) do resto da mensagem 
        String infoArray[] = actionString.split(": ");        
        Mensagem mensagem = new Mensagem();
        
        // Peer IP
        InetAddress IPAddress = InetAddress.getByName(peerID.split(":")[0]);
        
        // Tratar o leave
        if(infoArray[0].trim().toUpperCase().equals("LEAVE")) {
        	mensagem.setAction("LEAVE");        	
        }
        
        // Tratar as demais mensagens
        else {
        	mensagem.setAction(infoArray[0].trim().toUpperCase());
        	mensagem.setMessage(infoArray[1].trim());        	
        }
        
        // Converter o objeto em GSON para enviar para o servidor
        String mensagemJSON = gson.toJson(mensagem);
                
        // Pacote enviado para o servidor  
        byte[] sendData = new byte[1024];
        sendData = mensagemJSON.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, serverPort);
        peerSocket.send(sendPacket);

        // Pacote de recebimento (recebido pelo peer, vindo do servidor)
        byte[] recBuffer = new byte[1024];
        DatagramPacket recPack = new DatagramPacket(recBuffer, recBuffer.length);
        peerSocket.receive(recPack);
        String info = new String(recPack.getData(), recPack.getOffset(), recPack.getLength());
        
        // Converter de GSON para classe Mensagem
        Mensagem responseMessage = gson.fromJson(info, Mensagem.class);
        System.out.println(responseMessage.getMessage() + "\n");
        
        return info;    	    	
    }
    
    // Função para ler todos os arquivos da pasta do peer e armazenar em uma String
    // Referência: https://stackoverflow.com/questions/1844688/how-to-read-all-files-in-a-folder-from-java
    public static String readFiles(String path) {
        File folder = new File(path);
        File[] listOfFiles = folder.listFiles();
        String fileListString = "";

        if (listOfFiles != null) {
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    fileListString += file.getName() + "; ";
                }
            }
        }
        return fileListString;
    }   
}


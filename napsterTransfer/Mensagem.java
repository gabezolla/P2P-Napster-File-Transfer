package napsterTransfer;

public class Mensagem {
    private String action;    
    private String message;
    private String ip;
    private String port;

	public Mensagem(String action, String message) {
        this.action = action;
        this.message = message;
        this.ip = "";
        this.port = "";        
    }
	
	public Mensagem(String action, String ip, String port, String message) {
        this.action = action;
        this.message = message;
        this.ip = ip;
        this.port = port;        
    }
	
    public Mensagem() {
    }

    public String getIp() {
    	return ip;
    }
    
    public void setIp(String ip) {
    	this.ip = ip;
    }
    
    public String getPort() {
    	return port;
    }
    
    public void setPort(String port) {
    	this.port = port;
    }
    
    public String getAction() {
        return this.action;
    }

    public String getMessage() {
        return this.message;
    }
    
    public void setAction(String action) {
		this.action = action;
	}
    
    public void setMessage(String message) {
		this.message = message;
	}
}

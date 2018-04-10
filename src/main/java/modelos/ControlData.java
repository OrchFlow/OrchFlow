package modelos;

public class ControlData {
	String name, ip, port, controller;

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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof ControlData))
			return false;

		ControlData c = (ControlData) obj;

		return (c.getIp() != null && c.getIp().equals(ip)) && (c.getPort() != null && c.getPort().equals(port));
	}

	public int hashCode() {
		int hash = 1;

		if (ip != null)
			hash = hash * 29 + ip.hashCode();

		if (port != null)
			hash = hash * 31 + port.hashCode();

		return hash;
	}

	public String getController() {
		return controller;
	}

	public void setController(String controller) {
		this.controller = controller;
	}
}

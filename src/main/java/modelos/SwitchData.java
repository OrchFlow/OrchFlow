package modelos;

import java.net.URI;

public class SwitchData {
	private String DPID, inetAddress, name, controller;
	private URI location;

	public String getDPID() {
		return DPID;
	}

	public void setDPID(String dPID) {
		DPID = dPID;
	}

	public String getInetAddress() {
		return inetAddress;
	}

	public void setInetAddress(String inetAddress) {
		this.inetAddress = inetAddress;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public URI getLocation() {
		return location;
	}

	public void setLocation(URI location) {
		this.location = location;
	}

	public String getController() {
		return controller;
	}

	public void setController(String controller) {
		this.controller = controller;
	}
}

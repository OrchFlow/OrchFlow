package modelos;

import java.net.URI;

public class HostData {
	private String mac, ipv4, switchDPID, controller;
	private int port;
	private URI location;

	public URI getLocation() {
		return location;
	}

	public void setLocation(URI location) {
		this.location = location;
	}

	public String getMac() {
		return mac;
	}

	public void setMac(String mac) {
		this.mac = mac;
	}

	public String getIpv4() {
		return ipv4;
	}

	public void setIpv4(String ipv4) {
		this.ipv4 = ipv4;
	}

	public String getSwitchDPID() {
		return switchDPID;
	}

	public void setSwitchDPID(String switchDPID) {
		this.switchDPID = switchDPID;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getController() {
		return controller;
	}

	public void setController(String controller) {
		this.controller = controller;
	}

}

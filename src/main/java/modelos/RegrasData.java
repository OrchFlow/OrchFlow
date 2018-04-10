package modelos;

public class RegrasData {
	private String name, cookie, eth_type, tp_src, tp_dst, ipv4_src, ipv4_dst;
	private boolean active;
	private int idle_timeout, hard_timeout, priority, size, ip_proto;
	private String json;
	private String sw, in_port, out_port, control, mac_src, mac_dst;
	private boolean hibrido;

	public RegrasData() {
		name = cookie = eth_type = tp_src = tp_dst = ipv4_src = ipv4_dst = null;
		active = false;
		idle_timeout = hard_timeout = priority = size = ip_proto = -1;
		json = null;
		sw = in_port = out_port = control = mac_src = mac_dst = null;
		hibrido = false;
	}

	public boolean isHibrido() {
		return hibrido;
	}

	public void setHibrido(boolean hibrido) {
		this.hibrido = hibrido;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCookie() {
		return cookie;
	}

	public void setCookie(String cookie) {
		this.cookie = cookie;
	}

	public String getEth_type() {
		return eth_type;
	}

	public void setEth_type(String eth_type) {
		this.eth_type = eth_type;
	}

	public int getIp_proto() {
		return ip_proto;
	}

	public void setIp_proto(int ip_proto) {
		this.ip_proto = ip_proto;
	}

	public String getTp_src() {
		return tp_src;
	}

	public void setTp_src(String tp_src) {
		this.tp_src = tp_src;
	}

	public String getTp_dst() {
		return tp_dst;
	}

	public void setTp_dst(String tp_dst) {
		this.tp_dst = tp_dst;
	}

	public String getIpv4_src() {
		return ipv4_src;
	}

	public void setIpv4_src(String ipv4_src) {
		this.ipv4_src = ipv4_src;
	}

	public String getIpv4_dst() {
		return ipv4_dst;
	}

	public void setIpv4_dst(String ipv4_dst) {
		this.ipv4_dst = ipv4_dst;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}

	public int getIdle_timeout() {
		return idle_timeout;
	}

	public void setIdle_timeout(int idle_timeout) {
		this.idle_timeout = idle_timeout;
	}

	public int getHard_timeout() {
		return hard_timeout;
	}

	public void setHard_timeout(int hard_timeout) {
		this.hard_timeout = hard_timeout;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public boolean equals(Object obj) {
		if (!(obj instanceof RegrasData))
			return false;

		RegrasData c = (RegrasData) obj;

		return (c.getName() != null && c.getName().equals(name));
	}

	public int hashCode() {
		int hash = 1;

		if (name != null)
			hash = hash * 31 + name.hashCode();

		return hash;
	}

	public String getJson() {
		return json;
	}

	public void setJson(String json) {
		this.json = json;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public String getSw() {
		return sw;
	}

	public void setSw(String sw) {
		this.sw = sw;
	}

	public String getIn_port() {
		return in_port;
	}

	public void setIn_port(String in_port) {
		this.in_port = in_port;
	}

	public String getOut_port() {
		return out_port;
	}

	public void setOut_port(String out_port) {
		this.out_port = out_port;
	}

	public String getControl() {
		return control;
	}

	public void setControl(String control) {
		this.control = control;
	}

	public String getMac_src() {
		return mac_src;
	}

	public void setMac_src(String mac_src) {
		this.mac_src = mac_src;
	}

	public String getMac_dst() {
		return mac_dst;
	}

	public void setMac_dst(String mac_dst) {
		this.mac_dst = mac_dst;
	}

}

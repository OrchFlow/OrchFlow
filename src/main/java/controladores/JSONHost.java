package controladores;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import modelos.ControlData;
import modelos.HostData;
import modelos.SwitchData;
import neo4j.Neo4jHosts;

public class JSONHost {
	private static HostData hostD = new HostData();
	private static Neo4jHosts send = new Neo4jHosts();

	public void readJsonHost_RYU(ArrayList<SwitchData> arraySWD, ControlData controller) throws URISyntaxException {
		JSONObject host;
		JSONArray arrayHosts;

		try {
			// Monta array de link
			String hosts = (String) readJsonFromUrl(
					"http://" + controller.getIp() + ":" + controller.getPort() + "/v1.0/topology/hosts");
			arrayHosts = new JSONArray(hosts);

			for (int i = 0; i < arrayHosts.length(); i++) {
				host = arrayHosts.getJSONObject(i);
				if (host.getJSONArray("ipv4").length() != 0) {
					JSONArray ipv4 = host.getJSONArray("ipv4");
					if (ipv4.getString(0).contains(".")) {
						hostD.setMac(host.getString("mac"));
						hostD.setIpv4(ipv4.getString(0));
						hostD.setSwitchDPID(host.getJSONObject("port").getString("dpid"));
						hostD.setPort(host.getJSONObject("port").getInt("port_no"));

						hostD.setController(controller.getIp() + ":" + controller.getPort());
						// System.out.println(hostD.getIpv4() + " " +
						// hostD.getMac() + " " + hostD.getSwitchDPID() + " " +
						// hostD.getPort());

						send.hosts(arraySWD, hostD);
					}
				}
			}
		}
		// Trata as exceptions que podem ser lançadas no decorrer do processo
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void readJsonHost_FLOODLIGHT(ArrayList<SwitchData> arraySWD, ControlData controller)
			throws URISyntaxException {
		JSONObject host, attach;
		JSONArray arrayHosts, arrayAttach;

		try {
			// Monta array de link
			String hosts = (String) readJsonFromUrl(
					"http://" + controller.getIp() + ":" + controller.getPort() + "/wm/device/");
			arrayHosts = new JSONArray(hosts);

			for (int i = 0; i < arrayHosts.length(); i++) {
				host = arrayHosts.getJSONObject(i);
				if (host.get("ipv4").toString().contains(".")) {
					JSONArray arrayM = new JSONArray(host.get("mac").toString());
					hostD.setMac(arrayM.get(0).toString());
					JSONArray arrayI = new JSONArray(host.get("ipv4").toString());
					hostD.setIpv4(arrayI.get(0).toString());
					arrayAttach = new JSONArray(host.get("attachmentPoint").toString());
					attach = arrayAttach.getJSONObject(0);

					hostD.setSwitchDPID((String) attach.get("switchDPID"));
					hostD.setPort((Integer) attach.get("port"));

					hostD.setController(controller.getIp() + ":" + controller.getPort());

					send.hosts(arraySWD, hostD);
				}
			}
		}
		// Trata as exceptions que podem ser lançadas no decorrer do processo
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void readJsonHost_OPENDAYLIGHT(ArrayList<SwitchData> arraySWD, ControlData controller)
			throws URISyntaxException {

		try {
			String topo = readJsonFromUrl("http://" + controller.getIp() + ":" + controller.getPort()
					+ "/restconf/operational/network-topology:network-topology");

			JSONObject json = new JSONObject(topo);

			JSONObject networktopology = json.getJSONObject("network-topology");
			JSONArray topologies = networktopology.getJSONArray("topology");
			JSONObject topology = topologies.getJSONObject(0);
			JSONArray nodes = topology.getJSONArray("node");

			for (int i = 0; i < nodes.length(); i++) {
				JSONObject node = nodes.getJSONObject(i);

				if (node.getString("node-id").contains("host")) {
					JSONArray nodedetail = node.getJSONArray("host-tracker-service:addresses");

					JSONObject nodeinfo = nodedetail.getJSONObject(0);
					// System.out.println(nodeinfo.getString("mac"));

					hostD.setMac(nodeinfo.getString("mac"));
					hostD.setIpv4(nodeinfo.getString("ip"));
					JSONArray linkdetail = node.getJSONArray("host-tracker-service:attachment-points");
					JSONObject linkinfo = linkdetail.getJSONObject(0);

					String tpid = linkinfo.getString("tp-id");
					hostD.setSwitchDPID(tpid.substring(tpid.indexOf(":") + 1, tpid.lastIndexOf(":")));
					hostD.setPort(Integer.valueOf(tpid.substring(tpid.lastIndexOf(":") + 1)));

					hostD.setController(controller.getIp() + ":" + controller.getPort());
					// System.out.println(hostD.getIpv4() + " " + hostD.getMac()
					// + " " + hostD.getSwitchDPID() + " " + hostD.getPort());
					send.hosts(arraySWD, hostD);
				}
			}
		}
		// Trata as exceptions que podem ser lançadas no decorrer do processo
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public String readJsonFromUrl(String url) {
		URI fromUri;
		String r = null;
		try {
			fromUri = new URI(url);
			WebResource resource = Client.create().resource(fromUri);
			// POST JSON to the relationships URI
			ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
					.header("Authorization", "Basic YWRtaW46YWRtaW4=").get(ClientResponse.class);

			r = response.getEntity(String.class);
			// System.out.println(String.format("POST to [%s], status code [%d],
			// location header []", fromUri, response.getStatus()));
			response.close();
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return r;

	}
}

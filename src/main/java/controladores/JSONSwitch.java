package controladores;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import modelos.ControlData;
import modelos.LinkExtData;
import modelos.SwitchData;
import neo4j.Neo4jNodes;

public class JSONSwitch {
	private static Neo4jNodes sendNode = new Neo4jNodes();
	private static ArrayList<SwitchData> arraySWD = new ArrayList<SwitchData>();

	public ArrayList<SwitchData> switches_RYU(ArrayList<LinkExtData> arrayLED, ControlData controller)
			throws ParseException, URISyntaxException {
		JSONObject port, portsDescs;
		JSONArray arrayPorts, arraySwitches;
		String portNumber, name;

		try {
			// Monta array de switches
			String switches = (String) readJsonFromUrl(
					"http://" + controller.getIp() + ":" + controller.getPort() + "/v1.0/topology/switches");
			arraySwitches = new JSONArray(switches);

			for (int i = 0; i < arraySwitches.length(); i++) {

				SwitchData swD = new SwitchData();
				JSONObject sw = arraySwitches.getJSONObject(i);
				swD.setDPID(sw.getString("dpid"));
				Integer dpid = Integer.parseInt(swD.getDPID(), 16);
				swD.setController(controller.getIp() + ":" + controller.getPort());

				// Monta array de portas
				String pD = (String) readJsonFromUrl(
						"http://" + controller.getIp() + ":" + controller.getPort() + "/stats/portdesc/" + dpid);
				portsDescs = new JSONObject(pD);

				arrayPorts = new JSONArray(portsDescs.getJSONArray(dpid.toString()).toString());

				for (int j = 0; j < arrayPorts.length(); j++) {
					port = arrayPorts.getJSONObject(j);
					portNumber = port.get("port_no").toString();
					name = port.getString("name");

					if (portNumber.equals("LOCAL"))
						swD.setName(port.getString("name"));

					if (name.contains("ext")) {
						LinkExtData linkExt = new LinkExtData();
						linkExt.setDPID(swD.getDPID());
						linkExt.setPort(port.get("port_no").toString());
						int pos = name.indexOf("ext") + 3;

						if (pos != name.length()) {
							linkExt.setNumber(name.substring(pos, name.length()));
						} else {
							linkExt.setNumber("0");
						}
						System.out.println("ryu" + linkExt.getDPID());
						arrayLED.add(linkExt);
					}
				}
				swD.setInetAddress("");
				swD = sendNode.nodes(swD);
				arraySWD.add(swD);
			}
		}

		catch (JSONException e) {
			e.printStackTrace();
		}

		return arraySWD;
	}

	public ArrayList<SwitchData> switches_FLOODLIGHT(ArrayList<LinkExtData> arrayLED, ControlData controller)
			throws ParseException, URISyntaxException {
		JSONObject sw, port, portsDescs;
		JSONArray arraySwitches, arrayPorts;
		String portNumber, name;

		try {
			// Monta array de switches
			String switches = (String) readJsonFromUrl(
					"http://" + controller.getIp() + ":" + controller.getPort() + "/wm/core/controller/switches/json");
			arraySwitches = new JSONArray(switches);

			for (int i = 0; i < arraySwitches.length(); i++) {
				SwitchData swD = new SwitchData();
				sw = arraySwitches.getJSONObject(i);
				swD.setDPID(sw.getString("switchDPID"));
				swD.setInetAddress(sw.getString("inetAddress"));
				swD.setController(controller.getIp() + ":" + controller.getPort());

				// Monta array de portas
				String pD = (String) readJsonFromUrl("http://" + controller.getIp() + ":" + controller.getPort()
						+ "/wm/core/switch/" + swD.getDPID() + "/port-desc/json");
				portsDescs = new JSONObject(pD);
				arrayPorts = portsDescs.getJSONArray("portDesc");

				for (int j = 0; j < arrayPorts.length(); j++) {
					port = arrayPorts.getJSONObject(j);
					portNumber = port.getString("portNumber");
					name = port.getString("name");

					if (portNumber.equals("local"))
						swD.setName(port.getString("name"));

					if (name.contains("ext")) {
						LinkExtData linkExt = new LinkExtData();
						linkExt.setDPID(swD.getDPID());
						linkExt.setPort(port.getString("portNumber"));
						int pos = name.indexOf("ext") + 3;

						if (pos != name.length()) {
							linkExt.setNumber(name.substring(pos, name.length()));
						} else {
							linkExt.setNumber("0");
						}
						System.out.println("fl" + linkExt.getDPID());
						arrayLED.add(linkExt);
					}
				}
				swD = sendNode.nodes(swD);
				arraySWD.add(swD);
			}
		}

		catch (JSONException e) {
			e.printStackTrace();
		}

		return arraySWD;
	}

	public ArrayList<SwitchData> switches_OPENDAYLIGHT(ArrayList<LinkExtData> arrayLED, ControlData controller)
			throws ParseException, URISyntaxException, IOException {

		try {
			// Monta array de switches
			String topo = readJsonFromUrl("http://" + controller.getIp() + ":" + controller.getPort()
					+ "/restconf/operational/network-topology:network-topology");

			JSONObject json = new JSONObject(topo);

			JSONObject networktopology = json.getJSONObject("network-topology");
			JSONArray topologies = networktopology.getJSONArray("topology");
			JSONObject topology = topologies.getJSONObject(0);
			JSONArray nodes = topology.getJSONArray("node");

			for (int i = 0; i < nodes.length(); i++) {
				SwitchData swD = new SwitchData();
				swD.setController(controller.getIp() + ":" + controller.getPort());
				JSONObject node = nodes.getJSONObject(i);
				swD.setDPID(node.getString("node-id").substring(9));

				if (!node.getString("node-id").contains("host")) {
					String nodedetail = (String) readJsonFromUrl("http://" + controller.getIp() + ":"
							+ controller.getPort() + "/restconf/operational/opendaylight-inventory:nodes/node/"
							+ node.getString("node-id"));

					JSONObject nodeinfo = new JSONObject(nodedetail);
					JSONArray nodeports = ((JSONObject) ((JSONArray) nodeinfo.getJSONArray("node")).get(0))
							.getJSONArray("node-connector");

					for (int j = 0; j < nodeports.length(); j++) {
						JSONObject nodeport = nodeports.getJSONObject(j);
						String id = nodeport.getString("id");
						if (id.contains("LOCAL")) {
							swD.setName(nodeport.getString("flow-node-inventory:name"));
						}

						if (nodeport.getString("flow-node-inventory:name").contains("ext")) {
							String name = nodeport.getString("flow-node-inventory:name");
							LinkExtData linkExt = new LinkExtData();
							linkExt.setDPID(swD.getDPID());
							linkExt.setPort(nodeport.getString("flow-node-inventory:port-number"));
							int pos = name.indexOf("ext") + 3;

							if (pos != nodeport.getString("flow-node-inventory:name").length()) {
								linkExt.setNumber(name.substring(pos, name.length()));
							} else {
								linkExt.setNumber("0");
							}
							System.out.println("odl" + linkExt.getDPID());
							arrayLED.add(linkExt);
						}
					}

					swD.setInetAddress("");
					swD = sendNode.nodes(swD);
					arraySWD.add(swD);
					// System.out.println(swD.getDPID() + " " + swD.getName());
				}
			}
		}

		catch (JSONException e) {
			e.printStackTrace();
		}

		return arraySWD;
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

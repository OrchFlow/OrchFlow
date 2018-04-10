package pacote;

import java.net.URI;
import java.net.URISyntaxException;

import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import modelos.RegrasData;

public class Util {
	public static int i = 0;
	private static String passwd = "Basic bmVvNGo6b3JjaGZsb3c=";
	private static String SERVER_ROOT_URI = "http://192.168.56.101:7474/db/data/";

	public static String generateJsonDijkstra(URI endNode) {
		StringBuilder sb = new StringBuilder();
		sb.append("{ \"to\" : \"");
		sb.append(endNode.toString());
		sb.append("\", \"max_depth\" : 100,");

		sb.append("\"relationships\" : { \"type\" : \"link\", \"direction\" : \"all\" },");
		sb.append(" \"algorithm\" : \"shortestPath\" }");
		return sb.toString();
	}

	public static String dijkstra(URI startNode, URI endNode) throws URISyntaxException {
		URI fromUri = new URI(startNode.toString() + "/paths");
		String dijkstraJson = Util.generateJsonDijkstra(endNode);

		WebResource resource = Client.create().resource(fromUri);
		// POST JSON to the relationships URI
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.entity(dijkstraJson).header("Authorization", passwd).post(ClientResponse.class);

		System.out.println(String.format("POST to [%s], status code [%d]", fromUri, response.getStatus()));

		StringBuilder result = new StringBuilder("Caminho não encontrado");
		JSONArray paths = new JSONArray(response.getEntity(String.class));

		for (int i = 0; i < paths.length(); i++) {
			JSONObject path = paths.getJSONObject(i);
			if (path.get("end").toString().equals(endNode.toString())) {
				result.setLength(0);
				result.trimToSize();
				result.append(path.get("relationships").toString());
			}
		}
		response.close();
		return (result.toString());
	}

	public static JSONObject getRel(String link) {
		WebResource resource = Client.create().resource(link);
		// POST JSON to the relationships URI
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.header("Authorization", passwd).get(ClientResponse.class);

		System.out.println(String.format("GET to [%s], status code [%d]", link, response.getStatus()));

		JSONObject entity = new JSONObject(response.getEntity(String.class));

		response.close();
		return entity;
	}

	public static String getControl(String link) throws URISyntaxException {
		URI fromUri = new URI(link);

		WebResource resource = Client.create().resource(fromUri);
		// POST JSON to the relationships URI
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.header("Authorization", passwd).get(ClientResponse.class);

		System.out.println(String.format("POST to [%s], status code [%d]", fromUri, response.getStatus()));

		JSONObject entity = new JSONObject(response.getEntity(String.class));
		JSONObject data = new JSONObject(entity.get("data").toString());
		String controller = data.get("controller").toString();
		response.close();

		return controller;
	}

	public static URI search(String key) throws URISyntaxException {
		String url = SERVER_ROOT_URI + "index/node/index_1445034018615_1/";
		URI fromUri = new URI("");

		if (key.length() > 17)
			fromUri = URI.create(url + "DPID/" + key);
		else
			fromUri = URI.create(url + "mac/" + key);

		WebResource resource = Client.create().resource(fromUri);
		// POST JSON to the relationships URI
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.header("Authorization", passwd).get(ClientResponse.class);

		System.out.println(String.format("GET to [%s], status code [%d]", fromUri, response.getStatus()));

		URI location = null;
		String entity = response.getEntity(String.class);
		if (entity.contains("{")) {
			JSONArray arrayEntity = new JSONArray(entity);
			JSONObject ent = arrayEntity.getJSONObject(0);
			location = URI.create(ent.get("self").toString());
		}
		response.close();

		return (location);
	}

	public static void post(final String url, final String json) {
		new Thread(new Runnable() {
			public void run() {
				URI fromUri;
				try {
					fromUri = new URI(url);
					WebResource resource = Client.create().resource(fromUri);
					ClientResponse response = resource.accept(MediaType.APPLICATION_JSON)
							.type(MediaType.APPLICATION_JSON).entity(json).post(ClientResponse.class);

					if (response.getStatus() == 200) {
						System.out.println(
								String.format("POST to [%s], status code [%d]", fromUri, response.getStatus()));
					}

					response.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}).start();
	}

	public static String generateJsonFlowVOLTA_FLOODLIGHT(RegrasData rD) {
		StringBuilder sb = new StringBuilder();

		sb.append("{");

		// Flow name
		sb.append("\"name\":");
		sb.append("\"");
		sb.append(rD.getName() + "#" + i++);
		sb.append("\"");

		// Priority
		sb.append(", \"priority\":");
		sb.append("\"");
		sb.append(rD.getPriority());
		sb.append("\"");

		// Idle_timeout
		sb.append(", \"idle_timeout\":");
		sb.append("\"");
		sb.append(rD.getIdle_timeout());
		sb.append("\"");

		// Hard_timeout
		sb.append(", \"hard_timeout\":");
		sb.append("\"");
		sb.append(rD.getHard_timeout());
		sb.append("\"");

		// Switch MAC Address
		sb.append(", \"switch\":");
		sb.append("\"");
		sb.append(rD.getSw());
		sb.append("\"");

		// Source MAC
		sb.append(", \"eth_src\":");
		sb.append("\"");
		sb.append(rD.getMac_dst());
		sb.append("\"");

		// Destination MAC
		sb.append(", \"eth_dst\":");
		sb.append("\"");
		sb.append(rD.getMac_src());
		sb.append("\"");

		// Ethernet Type
		sb.append(", \"eth_type\":");
		sb.append("\"");
		sb.append(rD.getEth_type());
		sb.append("\"");

		// Protocol Number
		if (rD.getIp_proto() != -1) {
			sb.append(", \"ip_proto\":");
			sb.append("\"");
			sb.append(rD.getIp_proto());
			sb.append("\"");
		}
		// Source TCP/UDP Port
		if (!rD.getTp_src().isEmpty()) {
			sb.append(", \"tp_src\":");
			sb.append("\"");
			sb.append(rD.getTp_src());
			sb.append("\"");
		}

		// Destination TCP/UDP Port
		if (!rD.getTp_dst().isEmpty()) {
			sb.append(", \"tp_dst\":");
			sb.append("\"");
			sb.append(rD.getTp_dst());
			sb.append("\"");
		}

		// Source IP
		sb.append(", \"ipv4_src\":");
		sb.append("\"");
		sb.append(rD.getIpv4_dst());
		sb.append("\"");

		// Destination IP
		sb.append(", \"ipv4_dst\":");
		sb.append("\"");
		sb.append(rD.getIpv4_src());
		sb.append("\"");

		// sb.append(", \"dl-type\":");
		// sb.append("\"");
		// sb.append("0x0806");
		// sb.append("\"");

		sb.append(", \"cookie\":");
		sb.append("\"");
		sb.append(rD.getCookie());
		sb.append("\"");

		// Active
		sb.append(", \"active\":");
		sb.append("\"");
		sb.append(rD.isActive());
		sb.append("\"");

		// Actions
		sb.append(", \"actions\":");
		sb.append("\"");

		// Set Output
		sb.append("output=");
		sb.append(rD.getIn_port());

		sb.append("\"");
		sb.append("}");

		rD.setJson(sb.toString());

		return sb.toString();
	}

	public static String generateJsonFlowIDA_FLOODLIGHT(RegrasData rD) {
		StringBuilder sb = new StringBuilder();

		sb.append("{");

		// Flow name
		sb.append("\"name\":");
		sb.append("\"");
		sb.append(rD.getName() + "#" + i++);
		sb.append("\"");

		// Priority
		sb.append(", \"priority\":");
		sb.append("\"");
		sb.append(rD.getPriority());
		sb.append("\"");

		// Idle_timeout
		sb.append(", \"idle_timeout\":");
		sb.append("\"");
		sb.append(rD.getIdle_timeout());
		sb.append("\"");

		// Hard_timeout
		sb.append(", \"hard_timeout\":");
		sb.append("\"");
		sb.append(rD.getHard_timeout());
		sb.append("\"");

		// Switch MAC Address
		sb.append(", \"switch\":");
		sb.append("\"");
		sb.append(rD.getSw());
		sb.append("\"");

		// Source MAC
		sb.append(", \"eth_src\":");
		sb.append("\"");
		sb.append(rD.getMac_src());
		sb.append("\"");

		// Destination MAC
		sb.append(", \"eth_dst\":");
		sb.append("\"");
		sb.append(rD.getMac_dst());
		sb.append("\"");

		// Ethernet Type
		sb.append(", \"eth_type\":");
		sb.append("\"");
		sb.append(rD.getEth_type());
		sb.append("\"");

		// Protocol Number
		if (rD.getIp_proto() != -1) {
			sb.append(", \"ip_proto\":");
			sb.append("\"");
			sb.append(rD.getIp_proto());
			sb.append("\"");
		}
		// Source TCP/UDP Port
		if (!rD.getTp_src().isEmpty()) {
			sb.append(", \"tp_src\":");
			sb.append("\"");
			sb.append(rD.getTp_src());
			sb.append("\"");
		}

		// Destination TCP/UDP Port
		if (!rD.getTp_dst().isEmpty()) {
			sb.append(", \"tp_dst\":");
			sb.append("\"");
			sb.append(rD.getTp_dst());
			sb.append("\"");
		}

		// Source IP
		sb.append(", \"ipv4_src\":");
		sb.append("\"");
		sb.append(rD.getIpv4_src());
		sb.append("\"");

		// Destination IP
		sb.append(", \"ipv4_dst\":");
		sb.append("\"");
		sb.append(rD.getIpv4_dst());
		sb.append("\"");

		// sb.append(", \"dl-type\":");
		// sb.append("\"");
		// sb.append("0x0806");
		// sb.append("\"");

		sb.append(", \"cookie\":");
		sb.append("\"");
		sb.append(rD.getCookie());
		sb.append("\"");

		// Active
		sb.append(", \"active\":");
		sb.append("\"");
		sb.append(rD.isActive());
		sb.append("\"");

		// Actions
		sb.append(", \"actions\":");
		sb.append("\"");

		// Set Output
		sb.append("output=");
		sb.append(rD.getOut_port());

		sb.append("\"");
		sb.append("}");

		rD.setJson(sb.toString());

		return sb.toString();
	}

	public static String generateJsonFlowReactive(RegrasData regra) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"name\":");
		sb.append("\"");
		sb.append("R" + regra.getName());
		sb.append("\"");
		sb.append(",\"active\":");
		sb.append("\"");
		sb.append(regra.isActive());
		sb.append("\"");
		sb.append(",\"idle_timeout\":");
		sb.append("\"");
		sb.append(regra.getIdle_timeout());
		sb.append("\"");
		sb.append(",\"hard_timeout\":");
		sb.append("\"");
		sb.append(regra.getHard_timeout());
		sb.append("\"");
		sb.append(",\"priority\":");
		sb.append("\"");
		sb.append(regra.getPriority());
		sb.append("\"");
		if (!regra.getCookie().isEmpty()) {
			sb.append(",\"cookie\":");
			sb.append("\"");
			sb.append(regra.getCookie());
			sb.append("\"");
		} else {
			sb.append(",\"cookie\":");
			sb.append("\"");
			sb.append("0");
			sb.append("\"");
		}
		if (!regra.getEth_type().isEmpty()) {
			sb.append(",\"eth_type\":");
			sb.append("\"");
			sb.append(regra.getEth_type());
			sb.append("\"");
		}
		if (regra.getIp_proto() != -1) {
			sb.append(",\"ip_proto\":");
			sb.append("\"");
			sb.append(regra.getIp_proto());
			sb.append("\"");
		}
		sb.append(",\"hibrido\":");
		sb.append("\"");
		sb.append(regra.isHibrido());
		sb.append("\"");
		if (!regra.getTp_src().isEmpty()) {
			sb.append(",\"tp_src\":");
			sb.append("\"");
			sb.append(regra.getTp_src());
			sb.append("\"");
		}
		if (!regra.getTp_dst().isEmpty()) {
			sb.append(",\"tp_dst\":");
			sb.append("\"");
			sb.append(regra.getTp_dst());
			sb.append("\"");
		}
		sb.append(",\"ipv4_src\":");
		sb.append("\"");
		sb.append(regra.getIpv4_src());
		sb.append("\"");
		sb.append(",\"ipv4_dst\":");
		sb.append("\"");
		sb.append(regra.getIpv4_dst());
		sb.append("\"");
		sb.append(",\"eth_src\":");
		sb.append("\"");
		sb.append(regra.getMac_src());
		sb.append("\"");
		sb.append(",\"eth_dst\":");
		sb.append("\"");
		sb.append(regra.getMac_dst());
		sb.append("\"");
		sb.append(",\"control\":");
		sb.append("\"");
		sb.append(regra.getControl());
		sb.append("\"");
		sb.append(",\"dpid\":");
		sb.append("\"");
		sb.append(regra.getSw());
		sb.append("\"");
		sb.append(",\"out_port\":");
		sb.append("\"");
		sb.append(regra.getOut_port());
		sb.append("\"");
		sb.append(",\"in_port\":");
		sb.append("\"");
		sb.append(regra.getIn_port());
		sb.append("\"");
		sb.append("}");

		System.out.println(sb.toString());
		return sb.toString();
	}
}

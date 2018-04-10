package neo4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

import javax.faces.bean.ManagedBean;
import javax.ws.rs.core.MediaType;

import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import modelos.HostData;
import modelos.SwitchData;

@ManagedBean
public class Neo4jHosts {
	private static String passwd = "Basic bmVvNGo6MTIzNA==";
	public static String SERVER_ROOT_URI = "http://192.168.56.101:7474/db/data/";
	private static Map<String, String> hosts = new TreeMap<String, String>();

	public void hosts(ArrayList<SwitchData> arraySWD, HostData hostD) throws URISyntaxException {
		URI nodeSRC = null;

		hostD.setLocation(createNode(hostD));
		hosts.put(hostD.getIpv4(), hostD.getMac());
		addLabel(hostD.getLocation(), "Host");

		for (int i = 0; i < arraySWD.size(); i++) {
			if (arraySWD.get(i).getDPID().equals(hostD.getSwitchDPID())) {
				nodeSRC = arraySWD.get(i).getLocation();
				break;
			}
		}

		StringBuilder sb = new StringBuilder();

		sb.append("{ \"direction\" : \"" + "bidirectional" + "\", \"dstport\" : " + 1 + ", \"srcport\" : "
				+ String.valueOf(hostD.getPort()) + ", \"type\" : \"" + "host" + "\", \"srcswitch\" : \""
				+ String.valueOf(hostD.getSwitchDPID()) + "\", \"dsthost\" : \"" + String.valueOf(hostD.getMac())
				+ "\"}");

		addRelationship(nodeSRC, hostD, sb.toString());
	}

	private static URI createNode(HostData hostD) {
		final String nodeEntryPointUri = SERVER_ROOT_URI + "index/node/index_1445034018615_1?uniqueness=get_or_create";
		String createJson = generateJsonCreate(hostD);

		WebResource resource = Client.create().resource(nodeEntryPointUri);

		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.entity(createJson).header("Authorization", passwd).post(ClientResponse.class);

		if (response.getStatus() == 201 || response.getStatus() == 200) {
			// final URI location = response.getLocation();
			// System.out.println(String.format("POST to [%s], status code [%d],
			// location header [%s]", nodeEntryPointUri,
			// response.getStatus(), location.toString()));

			JSONObject entity = new JSONObject(response.getEntity(String.class));
			String uri = entity.get("self").toString();
			response.close();

			return URI.create(uri);
		} else {
			// System.out
			// .println(String.format("POST to [%s], status code [%d]",
			// nodeEntryPointUri, response.getStatus()));
			// System.exit(0);
		}

		return null;
	}

	private static String generateJsonCreate(HostData hostD) {
		StringBuilder sb = new StringBuilder();
		sb.append("{ \"key\" : \"mac\",");

		sb.append("\"value\" : \"");
		sb.append(hostD.getMac().toString());
		sb.append("\", ");

		sb.append("\"properties\" : {");
		sb.append("\"mac\" : \"");
		sb.append(hostD.getMac().toString());
		sb.append("\", ");

		sb.append("\"ipv4\" : \"");
		sb.append(hostD.getIpv4().toString());
		sb.append("\", ");

		sb.append("\"controller\" : \"");
		sb.append(hostD.getController().toString());
		sb.append("\"");

		sb.append(" }");

		sb.append(" }");
		return sb.toString();
	}

	private static void addLabel(URI nodeUri, String label) {
		String propertyUri = nodeUri.toString() + "/labels";

		WebResource resource = Client.create().resource(propertyUri);
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.entity("\"" + label + "\"").header("Authorization", passwd).post(ClientResponse.class);

		// System.out.println(String.format("PUT to [%s], status code [%d]",
		// propertyUri, response.getStatus()));
		if (response.getStatus() == 404) {
			// System.exit(0);
		}
		response.close();
	}

	private static void addRelationship(URI startNode, HostData hostD, String jsonAttributes)
			throws URISyntaxException {
		URI fromUri = new URI(SERVER_ROOT_URI + "index/relationship/index_1445034013414_1/?uniqueness=get_or_create");
		String relationshipJson = generateJsonRelationship(startNode, hostD, jsonAttributes);

		WebResource resource = Client.create().resource(fromUri);
		// POST JSON to the relationships URI
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.entity(relationshipJson).header("Authorization", passwd).post(ClientResponse.class);

		if (response.getStatus() == 201) {
			// final String location = response.getLocation().toString();
			// System.out.println(String.format("POST to [%s], status code [%d],
			// location header [%s]", fromUri,
			// response.getStatus(), location));
			response.close();
		} else {
			// System.out.println(String.format("POST to [%s], status code
			// [%d]", fromUri, response.getStatus()));
			// System.exit(0);
		}

	}

	private static String generateJsonRelationship(URI startNode, HostData hostD, String... jsonAttributes) {
		StringBuilder sb = new StringBuilder();

		sb.append("{ \"key\" : \"SrcDst\",");

		sb.append("\"value\" : \"");
		sb.append(hostD.getSwitchDPID() + hostD.getPort() + hostD.getMac() + 1);
		sb.append("\", ");

		sb.append(" \"start\" : \"");
		sb.append(startNode.toString());
		sb.append("\", ");

		sb.append(" \"end\" : \"");
		sb.append(hostD.getLocation().toString());
		sb.append("\", ");

		sb.append("\"type\" : \"link\"");

		if (jsonAttributes == null || jsonAttributes.length < 1) {
			sb.append(" }");
		} else {
			sb.append(", ");
			sb.append("\"properties\" : ");
			for (int i = 0; i < jsonAttributes.length; i++) {
				sb.append(jsonAttributes[i]);
				if (i < jsonAttributes.length - 1) { // Miss off the final comma
					sb.append(", ");
				}
			}
			sb.append(" }");
		}

		return sb.toString();
	}

	public Map<String, String> getHosts() {
		return hosts;
	}
}

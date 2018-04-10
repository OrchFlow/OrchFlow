package neo4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;

import javax.faces.bean.ManagedBean;
import javax.ws.rs.core.MediaType;

import org.json.JSONException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import modelos.LinkExtData;
import modelos.SwitchData;

@ManagedBean
public class Neo4jLinksExt {
	private static String passwd = "Basic bmVvNGo6MTIzNA==";
	public static String SERVER_ROOT_URI = "http://192.168.56.101:7474/db/data/";

	public void links(ArrayList<SwitchData> arraySWD, ArrayList<LinkExtData> arrayLED) throws URISyntaxException {

		for (int j = 0; j < arrayLED.size(); j++) {
			URI nodeSRC = null, nodeDST = null;
			LinkExtData src = null, dst = null;
			for (int i = 0; i < arraySWD.size(); i++) {
				if (arraySWD.get(i).getDPID().equals(arrayLED.get(j).getDPID()) && nodeDST == null) {
					dst = arrayLED.get(j);
					nodeDST = arraySWD.get(i).getLocation();
					i = -1;
					if (j < arrayLED.size() - 1) {
						j++;
					}
				} else if (arraySWD.get(i).getDPID().equals(arrayLED.get(j).getDPID())
						&& dst.getNumber().equals(arrayLED.get(j).getNumber())
						&& !arraySWD.get(i).getDPID().equals(dst.getDPID())) {
					src = arrayLED.get(j);
					nodeSRC = arraySWD.get(i).getLocation();
				}
			}
			StringBuilder sb = new StringBuilder();

			sb.append("{ \"direction\" : \"" + "bidirectional" + "\", \"dstport\" : " + dst.getPort().toString()
					+ ", \"srcport\" : " + src.getPort().toString() + ", \"type\" : \"" + "external"
					+ "\", \"srcswitch\" : \"" + src.getDPID().toString() + "\", \"dstswitch\" : \""
					+ dst.getDPID().toString() + "\"}");
			addRelationship(nodeSRC, nodeDST, src, dst, sb.toString());
		}
	}

	private static void addRelationship(URI startNode, URI endNode, LinkExtData src, LinkExtData dst,
			String jsonAttributes) throws URISyntaxException {
		URI fromUri = new URI(SERVER_ROOT_URI + "index/relationship/index_1445034013414_1/?uniqueness=get_or_create");
		String relationshipJson = generateJsonRelationship(startNode, endNode, src, dst, jsonAttributes);

		WebResource resource = Client.create().resource(fromUri);
		// POST JSON to the relationships URI
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.entity(relationshipJson).header("Authorization", passwd).post(ClientResponse.class);

		if (response.getStatus() == 201) {
			final String location = response.getLocation().toString();
			System.out.println(String.format("POST to [%s], status code [%d], location header [%s]", fromUri,
					response.getStatus(), location));
			response.close();
		} else {
			System.out.println(String.format("POST to [%s], status code [%d]", fromUri, response.getStatus()));
			// System.exit(0);
		}
	}

	private static String generateJsonRelationship(URI startNode, URI endNode, LinkExtData src, LinkExtData dst,
			String... jsonAttributes) {
		StringBuilder sb = new StringBuilder();

		sb.append("{ \"key\" : \"SrcDst\",");

		sb.append("\"value\" : \"");
		sb.append(src.getDPID() + src.getPort() + dst.getDPID() + dst.getPort());
		sb.append("\", ");

		sb.append(" \"start\" : \"");
		sb.append(startNode.toString());
		sb.append("\", ");

		sb.append(" \"end\" : \"");
		sb.append(endNode.toString());
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

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static Object readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			return jsonText;
		} finally {
			is.close();
		}
	}
}

package controladores;

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
import modelos.LinkData;
import modelos.SwitchData;
import neo4j.Neo4jLinks;

public class JSONLink {
	private static LinkData linkD = new LinkData();
	private static Neo4jLinks send = new Neo4jLinks();

	public void readJsonLink_RYU(ArrayList<SwitchData> arraySWD, ControlData controller)
			throws ParseException, URISyntaxException {
		JSONObject link;
		JSONArray arrayLinks;
		// System.out.println("JSONLINKS - ryu");

		try {
			// Monta array de link
			String links = (String) readJsonFromUrl(
					"http://" + controller.getIp() + ":" + controller.getPort() + "/v1.0/topology/links");
			arrayLinks = new JSONArray(links);

			for (int i = 0; i < arrayLinks.length(); i++) {
				link = arrayLinks.getJSONObject(i);
				JSONObject linkSrc = link.getJSONObject("src");
				JSONObject linkDst = link.getJSONObject("dst");

				linkD.setSrcP(linkSrc.getInt("port_no"));
				linkD.setSrcS(linkSrc.getString("dpid"));

				linkD.setDstP(linkDst.getInt("port_no"));
				linkD.setDstS(linkDst.getString("dpid"));

				if (linkSrc.getString("name").contains("ext")) {
					linkD.setType("external");
				} else {
					linkD.setType("internal");
				}
				// linkD.setType((String) link.get("type"));
				// linkD.setDirection((String) link.get("direction"));

				send.links(arraySWD, linkD);
			}

		}
		// Trata as exceptions que podem ser lançadas no decorrer do processo
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void readJsonLink_FLOODLIGHT(ArrayList<SwitchData> arraySWD, ControlData controller)
			throws ParseException, URISyntaxException {
		JSONObject link;
		JSONArray arrayLinks;

		try {
			// Monta array de link
			String links = (String) readJsonFromUrl(
					"http://" + controller.getIp() + ":" + controller.getPort() + "/wm/topology/links/json");
			arrayLinks = new JSONArray(links);

			for (int i = 0; i < arrayLinks.length(); i++) {
				link = arrayLinks.getJSONObject(i);
				linkD.setSrcS(link.getString("src-switch"));
				linkD.setSrcP(link.getInt("src-port"));
				linkD.setDstS(link.getString("dst-switch"));
				linkD.setDstP(link.getInt("dst-port"));
				linkD.setType(link.getString("type"));
				linkD.setDirection(link.getString("direction"));
				send.links(arraySWD, linkD);
			}

		}
		// Trata as exceptions que podem ser lançadas no decorrer do processo
		catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void readJsonLink_OPENDAYLIGHT(ArrayList<SwitchData> arraySWD, ControlData controller)
			throws ParseException, URISyntaxException {

		try {
			// Monta array de link
			String topo = readJsonFromUrl("http://" + controller.getIp() + ":" + controller.getPort()
					+ "/restconf/operational/network-topology:network-topology");

			JSONObject json = new JSONObject(topo);

			JSONObject networktopology = json.getJSONObject("network-topology");
			JSONArray topologies = networktopology.getJSONArray("topology");
			JSONObject topology = topologies.getJSONObject(0);
			JSONArray links = topology.getJSONArray("link");

			for (int i = 0; i < links.length(); i++) {
				JSONObject link = links.getJSONObject(i);
				if (!link.getString("link-id").contains("host")) {
					JSONObject linkSrc = link.getJSONObject("source");
					JSONObject linkDst = link.getJSONObject("destination");

					linkD.setSrcS(linkSrc.getString("source-node").substring(9));
					String tp = linkSrc.getString("source-tp");
					linkD.setSrcP(Integer.valueOf(tp.substring(tp.lastIndexOf(":") + 1)));

					linkD.setDstS(linkDst.getString("dest-node").substring(9));
					tp = linkDst.getString("dest-tp");
					linkD.setDstP(Integer.valueOf(tp.substring(tp.lastIndexOf(":") + 1)));

					// if (linkSrc.getString("link-id").contains("ext")) {
					// linkD.setType("external");
					// } else {
					linkD.setType("internal");
					// }

					// linkD.setDirection((String) link.get("direction"));
					System.out.println(
							linkD.getDstS() + " " + linkD.getSrcS() + " " + linkD.getDstP() + " " + linkD.getSrcP());
					send.links(arraySWD, linkD);
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

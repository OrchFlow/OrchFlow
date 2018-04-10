package pacote;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.ws.rs.core.MediaType;

import org.json.JSONException;
import org.json.simple.parser.ParseException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import controladores.JSONHost;
import controladores.JSONLink;
import controladores.JSONSwitch;
import controladores.ReactiveRoute;
import modelos.ControlData;
import modelos.LinkExtData;
import modelos.SwitchData;
import neo4j.Neo4jHosts;
import neo4j.Neo4jLinksExt;

@SessionScoped
@ManagedBean
public class Controllers {
	private static ArrayList<SwitchData> arraySWD = new ArrayList<SwitchData>();
	private ControlData controller;
	private static List<ControlData> controllers;
	public static String SERVER_ROOT_URI = "http://192.168.56.101:7474/db/data/";
	private String neo4j;

	@PostConstruct
	public void init() {
		controller = new ControlData();
		controllers = new ArrayList<ControlData>();

		neo4j = "http://192.168.56.101:7474";
	}

	public void createNew() {
		if (controllers.contains(controller)) {
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Controlador duplicado!", null);
			FacesContext.getCurrentInstance().addMessage(null, msg);
		} else {
			controllers.add(controller);
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Controlador adicionado com sucesso!",
					null);
			FacesContext.getCurrentInstance().addMessage(null, msg);
			reinit();
		}
	}

	public String reinit() {
		controller = new ControlData();
		return null;
	}

	public void removeAll() {
		controllers.clear();
	}

	public String executar() throws FileNotFoundException, IOException, ParseException, URISyntaxException {
		clearDB();

		JSONSwitch jsonswitch = new JSONSwitch();
		JSONLink jsonlink = new JSONLink();
		JSONHost jsonhost = new JSONHost();
		Neo4jLinksExt sendLinkExt = new Neo4jLinksExt();
		ArrayList<LinkExtData> arrayLED = new ArrayList<LinkExtData>();

		for (int i = 0; i < controllers.size(); i++) {
			if (controllers.get(i).getController().equals("Ryu")) {
				arraySWD = jsonswitch.switches_RYU(arrayLED, controllers.get(i));
				jsonlink.readJsonLink_RYU(arraySWD, controllers.get(i));
				jsonhost.readJsonHost_RYU(arraySWD, controllers.get(i));
			} else if (controllers.get(i).getController().equals("Floodlight")) {
				arraySWD = jsonswitch.switches_FLOODLIGHT(arrayLED, controllers.get(i));
				jsonlink.readJsonLink_FLOODLIGHT(arraySWD, controllers.get(i));
				jsonhost.readJsonHost_FLOODLIGHT(arraySWD, controllers.get(i));
			} else if (controllers.get(i).getController().equals("OpenDaylight")) {
				arraySWD = jsonswitch.switches_OPENDAYLIGHT(arrayLED, controllers.get(i));
				jsonlink.readJsonLink_OPENDAYLIGHT(arraySWD, controllers.get(i));
				jsonhost.readJsonHost_OPENDAYLIGHT(arraySWD, controllers.get(i));
			}
		}

		for (int i = 0; i < controllers.size(); i++) {
			sendArpData(controllers.get(i));
		}

		// Ordena o array de links externos
		Collections.sort(arrayLED, new Comparator<LinkExtData>() {
			public int compare(LinkExtData a, LinkExtData b) {
				return (a.getNumber().compareTo(b.getNumber()));
			}
		});
		sendLinkExt.links(arraySWD, arrayLED);
		ReactiveRoute.setControllers(controllers);
		return "principal.xhtml?faces-redirect=true";
	}

	private void clearDB() throws URISyntaxException {
		// TODO corrigir
		URI fromUri = new URI(SERVER_ROOT_URI + "cypher");
		String json = "{\"query\" : \"MATCH (a)-[b]->(c) DELETE a,b,c\"}";

		WebResource resource = Client.create().resource(fromUri);
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.entity(json).post(ClientResponse.class);

		System.out.println(String.format("POST to [%s], status code [%d]", fromUri, response.getStatus()));

		response.close();
	}

	private void sendArpData(ControlData controller) throws URISyntaxException {
		URI fromUri = new URI("http://" + controller.getIp() + ":" + controller.getPort() + "/arpreply/json");
		String json = generateJsonArp();

		WebResource resource = Client.create().resource(fromUri);
		ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
				.entity(json).post(ClientResponse.class);

		System.out.println(String.format("POST to [%s], status code [%d]", fromUri, response.getStatus()));

		response.close();
	}

	private String generateJsonArp() {
		Neo4jHosts h = new Neo4jHosts();
		Map<String, String> hosts = h.getHosts();

		String prefix = "";
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"");
		sb.append("arpreply");
		sb.append("\":[");
		for (String ip : hosts.keySet()) {
			if (ip != null) {
				sb.append(prefix);
				prefix = ",";
				sb.append("{");
				sb.append("\"ip\":");
				sb.append("\"");
				sb.append(ip);
				sb.append("\"");
				sb.append(", \"mac\":");
				sb.append("\"");
				sb.append(hosts.get(ip));
				sb.append("\"");
				sb.append("}");
			}
		}
		sb.append("]}");
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

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public ControlData getController() {
		return controller;
	}

	public void setController(ControlData controller) {
		this.controller = controller;
	}

	public List<ControlData> getControllers() {
		return controllers;
	}

	@SuppressWarnings("static-access")
	public void setControllers(List<ControlData> controllers) {
		this.controllers = controllers;
	}

	public static List<ControlData> getControl() {
		return controllers;
	}

	public String getNeo4j() {
		return neo4j;
	}

	public void setNeo4j(String neo4j) {
		this.neo4j = neo4j;
	}
}

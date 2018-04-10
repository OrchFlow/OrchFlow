package controladores;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.context.FacesContext;
import javax.ws.rs.core.MediaType;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.simple.parser.ParseException;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import modelos.ControlData;
import modelos.RegrasData;
import neo4j.Neo4jHosts;
import pacote.Controllers;
import pacote.Util;

@SessionScoped
@ManagedBean
public class ReactiveRoute2 {
	public static int i;
	private RegrasData regra;
	private RegrasData regraSelecionada;
	private List<RegrasData> regras;
	private static List<RegrasData> regrasCompletas;

	@PostConstruct
	public void init() {
		regra = new RegrasData();
		regra.setActive(true);
		regra.setIdle_timeout(60);
		regra.setHard_timeout(60);
		regra.setPriority(32767);
		regra.setCookie("0");
		regra.setEth_type("0x800");
		regra.setSize(0);
		i = 0;
		regraSelecionada = new RegrasData();
		regras = new ArrayList<RegrasData>();
		regrasCompletas = new ArrayList<RegrasData>();
	}

	public void reinit() {
		regra = new RegrasData();
		regra.setActive(true);
		regra.setIdle_timeout(60);
		regra.setHard_timeout(60);
		regra.setPriority(32767);
		regra.setCookie("0");
		regra.setEth_type("0x800");
		regra.setSize(0);
		i = 0;
	}

	public void exec(boolean hibrido) {
		regra.setHibrido(hibrido);
		if (!regras.contains(regra)) {
			buscar();
			regra.setSize(i);
			regras.add(regra);
			if (hibrido) {
				FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Rota hibrida criada com sucesso!",
						null);
				FacesContext.getCurrentInstance().addMessage(null, msg);
			} else {
				FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Rota reativa criada com sucesso!",
						null);
				FacesContext.getCurrentInstance().addMessage(null, msg);
			}
			reinit();
		} else {
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Nome duplicado!", null);
			FacesContext.getCurrentInstance().addMessage(null, msg);
		}
	}

	public void removeRegra(RegrasData obj) throws URISyntaxException, IOException, ParseException {
		regras.remove(obj);
		for (RegrasData r : regrasCompletas) {
			if (r.getName().equals(obj.getName())) {
				System.out.println(r.getIpv4_src() + "-" + r.getSw());
				remove(r);
				remove_RYU(r);
			}
		}
	}

	private void remove(RegrasData obj) throws URISyntaxException, IOException, ParseException {
		List<ControlData> controllers = Controllers.getControl();
		for (int j = 0; j < controllers.size(); j++) {
			for (int i = 0; i < obj.getSize(); i++) {

				String fromUri = "http://" + controllers.get(j).getIp() + ":" + controllers.get(j).getPort()
						+ "/wm/staticflowpusher/json";
				String name = "{\"name\":\"" + obj.getName() + i + "\"}";
				String jsonResponse = "";
				URL url = new URL(fromUri);
				HttpURLConnection connection = null;
				connection = (HttpURLConnection) url.openConnection();
				connection.setRequestMethod("POST");
				// We have to override the post method so we can send data
				connection.setRequestProperty("X-HTTP-Method-Override", "DELETE");
				connection.setDoOutput(true);

				// Send request
				OutputStreamWriter wr = new OutputStreamWriter(connection.getOutputStream());
				wr.write(name);
				wr.flush();

				// Get Response
				BufferedReader rd = new BufferedReader(new InputStreamReader(connection.getInputStream()));
				String line;
				while ((line = rd.readLine()) != null) {
					jsonResponse = jsonResponse.concat(line);
				}
				wr.close();
				rd.close();
			}
		}
	}

	public void remove_RYU(RegrasData obj) {
		List<ControlData> controllers = Controllers.getControl();
		for (int j = 0; j < controllers.size(); j++) {

			String fromUri = "http://" + controllers.get(j).getIp() + ":" + controllers.get(j).getPort()
					+ "/stats/flowentry/delete_strict";
			String deleteJson = generateJsonFlowIDA_RYU(obj);
			System.out.println(deleteJson);

			WebResource resource = Client.create().resource(fromUri);
			// POST JSON to the relationships URI
			ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
					.entity(deleteJson).post(ClientResponse.class);

			System.out.println(String.format("POST to [%s], status code [%d]", fromUri, response.getStatus()));

			response.close();

			deleteJson = generateJsonFlowVOLTA_RYU(obj);
			System.out.println(deleteJson);

			resource = Client.create().resource(fromUri);
			// POST JSON to the relationships URI
			response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON).entity(deleteJson)
					.post(ClientResponse.class);

			System.out.println(String.format("POST to [%s], status code [%d]", fromUri, response.getStatus()));

			response.close();
		}
	}

	public void create(String path) throws JSONException, URISyntaxException {
		JSONArray rel = new JSONArray(path);
		Neo4jHosts h = new Neo4jHosts();
		Map<String, String> m = h.getHosts();
		String mac_src = null, mac_dst = null;
		for (String key : m.keySet()) {
			if (m.get(key).equals(regra.getIpv4_src())) {
				mac_src = regra.getIpv4_src();
				regra.setIpv4_src(key);
			}
			if (m.get(key).equals(regra.getIpv4_dst())) {
				mac_dst = regra.getIpv4_dst();
				regra.setIpv4_dst(key);
			}
		}

		// Verifica todos os links do menor caminho entre os hosts
		for (int j = 0; j < rel.length() - 1; j++) {
			RegrasData rD = new RegrasData();
			String link = rel.get(j).toString();
			String proxLink = rel.get(j + 1).toString();

			JSONObject JsonRel = Util.getRel(link);
			JSONObject JsonRelProx = Util.getRel(proxLink);

			JSONObject data = new JSONObject(JsonRel.get("data").toString());
			JSONObject dataProx = new JSONObject(JsonRelProx.get("data").toString());

			// SE O LINK ATUAL É DO TIPO HOST
			if (data.get("type").equals("host")) {
				// rD = new RouteData();
				rD.setSw(data.get("srcswitch").toString());
				rD.setControl(Util.getControl(JsonRel.get("start").toString()));
				rD.setIn_port(data.get("srcport").toString());

				// rD.setActive("true");
				// rD.setCookie("0");
				// rD.setPriority(priority);

				if (data.get("srcswitch").equals(dataProx.get("srcswitch"))) {
					rD.setOut_port(dataProx.get("srcport").toString());
				} else {
					rD.setOut_port(dataProx.get("dstport").toString());
				}
				// arrayRoute.add(rD);

				// SE O PROXIMO LINK É DO TIPO HOST
			} else if (dataProx.get("type").equals("host")) {
				// rD = new RouteData();
				rD.setSw(dataProx.get("srcswitch").toString());
				rD.setControl(Util.getControl(JsonRelProx.get("start").toString()));
				rD.setOut_port(dataProx.get("srcport").toString());

				// rD.setActive("true");
				// rD.setCookie("0");
				// rD.setPriority(priority);

				if (data.get("srcswitch").equals(dataProx.get("srcswitch"))) {
					rD.setIn_port(data.get("srcport").toString());
				} else {
					rD.setIn_port(data.get("dstport").toString());
				}
				// arrayRoute.add(rD);

				// SE AMBOS OS LINKS SÃO INTERNOS
			} else {
				// rD = new RouteData();
				if (data.get("srcswitch").equals(dataProx.get("dstswitch"))) {
					rD.setSw(data.get("srcswitch").toString());
					rD.setIn_port(data.get("srcport").toString());
					rD.setOut_port(dataProx.get("dstport").toString());
					rD.setControl(Util.getControl(JsonRel.get("start").toString()));
				} else if (data.get("srcswitch").equals(dataProx.get("srcswitch"))) {
					rD.setSw(data.get("srcswitch").toString());
					rD.setIn_port(data.get("srcport").toString());
					rD.setOut_port(dataProx.get("srcport").toString());
					rD.setControl(Util.getControl(JsonRel.get("start").toString()));
				} else if (data.get("dstswitch").equals(dataProx.get("dstswitch"))) {
					rD.setSw(data.get("dstswitch").toString());
					rD.setIn_port(data.get("dstport").toString());
					rD.setOut_port(dataProx.get("dstport").toString());
					rD.setControl(Util.getControl(JsonRel.get("end").toString()));
				} else {
					rD.setSw(data.get("dstswitch").toString());
					rD.setIn_port(data.get("dstport").toString());
					rD.setOut_port(dataProx.get("srcport").toString());
					rD.setControl(Util.getControl(JsonRel.get("end").toString()));
				}

				// rD.setActive("true");
				// rD.setCookie("0");
				// rD.setPriority(priority);
				// arrayRoute.add(rD);
			}
			rD.setActive(regra.isActive());
			rD.setCookie(regra.getCookie());
			rD.setPriority(regra.getPriority());
			rD.setMac_src(mac_src);
			rD.setMac_dst(mac_dst);
			rD.setEth_type(regra.getEth_type());
			rD.setHard_timeout(regra.getHard_timeout());
			rD.setIdle_timeout(regra.getIdle_timeout());
			rD.setIp_proto(regra.getIp_proto());
			rD.setIpv4_dst(regra.getIpv4_dst());
			rD.setIpv4_src(regra.getIpv4_src());
			rD.setTp_dst(regra.getTp_dst());
			rD.setTp_src(regra.getTp_src());
			rD.setName(regra.getName());
			rD.setHibrido(regra.isHibrido());

			regrasCompletas.add(rD);
			aplicaRegra(null);
		}
	}

	public static void aplicaRegra(RegrasData regra) throws URISyntaxException {
		if (regra != null) {
			// String controller = null;
			// for (int i = 0; i < Controllers.getControl().size(); i++) {
			// if (regra.getControl().equals(
			// Controllers.getControl().get(i).getIp() + ":" +
			// Controllers.getControl().get(i).getPort())) {
			// controller = Controllers.getControl().get(i).getController();
			// }
			// }

			// String url = null;
			// String flow = null;
			//
			// if (controller.equals("Ryu")) {
			// url = "http://" + regra.getControl() + "/stats/flowentry/add";
			// // flowIDA = generateJsonFlowIDA_RYU(regra);
			// // flowVOLTA = generateJsonFlowVOLTA_RYU(regra);
			// } else if (controller.equals("Floodlight")) {
			// url = "http://" + regra.getControl() +
			// "/wm/staticflowpusher/json";
			// } else if (controller.equals("OpenDaylight")) {
			// // TODO OpenDaylight controller
			// }
			String flow = Util.generateJsonFlowReactive(regra);

			regra.setJson(flow);

			// Util.post(url, flowIDA);
			// Util.post(url, flowVOLTA);
		} else {
			for (int j = 0; j < regrasCompletas.size(); j++) {
				// String controller = null;
				// for (int i = 0; i < Controllers.getControl().size(); i++) {
				// if
				// (regrasCompletas.get(j).getControl().equals(Controllers.getControl().get(i).getIp()
				// + ":"
				// + Controllers.getControl().get(i).getPort())) {
				// controller = Controllers.getControl().get(i).getController();
				// }
				// }

				// String url = null;
				// String flow = null;
				//
				// if (controller.equals("Ryu")) {
				// url = "http://" + regrasCompletas.get(j).getControl() +
				// "/stats/flowentry/add";
				// // flowIDA =
				// // generateJsonFlowIDA_RYU(regrasCompletas.get(j));
				// // flowVOLTA =
				// // generateJsonFlowVOLTA_RYU(regrasCompletas.get(j));
				// } else if (controller.equals("Floodlight")) {
				// url = "http://" + regrasCompletas.get(j).getControl() +
				// "/wm/staticflowpusher/json";
				// } else if (controller.equals("OpenDaylight")) {
				// // TODO OpenDaylight controller
				// }
				String flow = Util.generateJsonFlowReactive(regrasCompletas.get(j));

				regrasCompletas.get(j).setJson(flow);

				// Util.post(url, flowIDA);
				// Util.post(url, flowVOLTA);
			}
			criaJSON();
		}
	}

	private static void criaJSON() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < regrasCompletas.size(); i++) {
			sb.append(regrasCompletas.get(i).getJson());
			if (regrasCompletas.size() > i + 1) {
				sb.append(",");
			}
		}
		sendReactiveData(sb.toString());
	}

	private static void sendReactiveData(String json) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"reactive\":");
		sb.append("[");
		sb.append(json);
		sb.append("]");
		sb.append("}");

		json = sb.toString();
		System.out.println(json);

		for (int i = 0; i < Controllers.getControl().size(); i++) {
			ControlData controller = Controllers.getControl().get(i);
			URI fromUri;
			try {
				fromUri = new URI(
						"http://" + controller.getIp() + ":" + controller.getPort() + "/reactive/flowpusher/json");
				WebResource resource = Client.create().resource(fromUri);
				// POST JSON to the relationships URI
				ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
						.entity(json).post(ClientResponse.class);

				if (response.getStatus() == 200) {
					System.out.println(String.format("POST to [%s], status code [%d]", fromUri, response.getStatus()));
				}

				response.close();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}

	public static void aplicar(String nome) {
		for (RegrasData r : regrasCompletas) {
			if (r.getName().equals(nome)) {
				// JSONArray jsonArray = new JSONArray("[" + regra.getJson() +
				// "]");
				// System.out.println(jsonArray.toString());
				// for (int i = 0; i < jsonArray.length(); i++) {
				try {
					// JSONObject jsonRegra = jsonArray.getJSONObject(i);
					// System.out.println(jsonRegra.toString());
					// RegrasData r = new RegrasData();
					// r.setActive(jsonRegra.getBoolean("active"));
					// r.setControl(jsonRegra.getString("control"));
					// r.setCookie(jsonRegra.getString("cookie"));
					// r.setEth_type(jsonRegra.getString("eth_type"));
					// r.setHard_timeout(jsonRegra.getInt("hard_timeout"));
					// r.setIdle_timeout(jsonRegra.getInt("idle_timeout"));
					// r.setIn_port(jsonRegra.getString("in_port"));
					// r.setIp_proto(jsonRegra.getInt("ip_proto"));
					// r.setIpv4_dst(jsonRegra.getString("ipv4_dst"));
					// r.setIpv4_src(jsonRegra.getString("ipv4_src"));
					// r.setOut_port(jsonRegra.getString("out_port"));
					// r.setPriority(jsonRegra.getInt("priority"));
					// r.setSw(jsonRegra.getString("dpid"));
					// r.setTp_dst(jsonRegra.getString("tp_dst"));
					// r.setTp_src(jsonRegra.getString("tp_src"));
					ProactiveRoute.aplicaRegra(r);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		}

	}

	private static String generateJsonFlowIDA_RYU(RegrasData rD) {
		StringBuilder sb = new StringBuilder();

		sb.append("{");

		// Flow name
		sb.append("\"name\":");
		sb.append("\"");
		sb.append(rD.getName() + i++);
		sb.append("\"");

		// Priority
		sb.append("\"priority\":");
		sb.append(rD.getPriority());

		// Idle_timeout
		sb.append(", \"idle_timeout\":");
		sb.append(rD.getIdle_timeout());

		// Hard_timeout
		sb.append(", \"hard_timeout\":");
		sb.append(rD.getHard_timeout());

		// Switch MAC Address
		sb.append(", \"dpid\":");
		if (rD.getSw().contains("x"))
			sb.append(Integer.parseInt(rD.getSw(), 16));
		else
			sb.append(rD.getSw());

		// sb.append(", \"dl-type\":");
		// sb.append("\"");
		// sb.append("0x0806");
		// sb.append("\"");

		sb.append(", \"cookie\":");
		sb.append(rD.getCookie());

		// Match
		sb.append(", \"match\":");
		sb.append("{");

		// Source MAC
		sb.append("\"dl_src\":");
		sb.append("\"");
		sb.append(rD.getMac_src());
		sb.append("\"");

		// Destination MAC
		sb.append(", \"dl_dst\":");
		sb.append("\"");
		sb.append(rD.getMac_dst());
		sb.append("\"");

		// Source IP
		sb.append(", \"nw_src\":");
		sb.append("\"");
		sb.append(rD.getIpv4_src());
		sb.append("\"");

		// Destination IP
		sb.append(", \"nw_dst\":");
		sb.append("\"");
		sb.append(rD.getIpv4_dst());
		sb.append("\"");

		// Ethernet Type
		sb.append(", \"eth_type\":");
		sb.append(2048);// TODO

		// Protocol Number
		if (rD.getIp_proto() != -1) {
			sb.append(", \"ip_proto\":");
			sb.append("\"");
			sb.append(rD.getIp_proto());
			sb.append("\"");
		}

		// TCP
		if (rD.getIp_proto() == 6) {
			// Source TCP Port
			if (!rD.getTp_src().isEmpty()) {
				sb.append(", \"tcp_src\":");
				sb.append("\"");
				sb.append(rD.getTp_src());
				sb.append("\"");
			}

			// Destination TCP Port
			if (!rD.getTp_dst().isEmpty()) {
				sb.append(", \"tcp_dst\":");
				sb.append("\"");
				sb.append(rD.getTp_dst());
				sb.append("\"");
			}
		}

		// TODO UDP
		sb.append("}");

		// Active
		sb.append(", \"active\":");
		sb.append("\"");
		sb.append(rD.isActive());
		sb.append("\"");

		// Actions
		sb.append(", \"actions\":");
		sb.append("[");

		// Set Output
		sb.append("{");
		sb.append("\"type\":");
		sb.append("\"OUTPUT\"");

		sb.append(", \"port\":");
		sb.append(rD.getOut_port());
		sb.append("}");

		sb.append("]");
		sb.append("}");

		rD.setJson(sb.toString());

		return sb.toString();
	}

	private static String generateJsonFlowVOLTA_RYU(RegrasData rD) {
		StringBuilder sb = new StringBuilder();

		sb.append("{");

		// Flow name
		sb.append("\"name\":");
		sb.append("\"");
		sb.append(rD.getName() + i++);
		sb.append("\"");

		// Priority
		sb.append("\"priority\":");
		sb.append(rD.getPriority());

		// Idle_timeout
		sb.append(", \"idle_timeout\":");
		sb.append(rD.getIdle_timeout());

		// Hard_timeout
		sb.append(", \"hard_timeout\":");
		sb.append(rD.getHard_timeout());

		// Switch MAC Address
		sb.append(", \"dpid\":");
		if (rD.getSw().contains("x"))
			sb.append(Integer.parseInt(rD.getSw(), 16));
		else
			sb.append(rD.getSw());

		// sb.append(", \"dl-type\":");
		// sb.append("\"");
		// sb.append("0x0806");
		// sb.append("\"");

		sb.append(", \"cookie\":");
		sb.append(rD.getCookie());

		// Match
		sb.append(", \"match\":");
		sb.append("{");

		// Source MAC
		sb.append("\"dl_src\":");
		sb.append("\"");
		sb.append(rD.getMac_dst());
		sb.append("\"");

		// Destination MAC
		sb.append(", \"dl_dst\":");
		sb.append("\"");
		sb.append(rD.getMac_src());
		sb.append("\"");

		// Source IP
		sb.append(", \"nw_src\":");
		sb.append("\"");
		sb.append(rD.getIpv4_dst());
		sb.append("\"");

		// Destination IP
		sb.append(", \"nw_dst\":");
		sb.append("\"");
		sb.append(rD.getIpv4_src());
		sb.append("\"");

		// Ethernet Type
		sb.append(", \"eth_type\":");
		sb.append(2048);

		// Protocol Number
		if (rD.getIp_proto() != -1) {
			sb.append(", \"ip_proto\":");
			sb.append("\"");
			sb.append(rD.getIp_proto());
			sb.append("\"");
		}

		// TCP
		if (rD.getIp_proto() == 6) {
			// Source TCP Port
			if (!rD.getTp_src().isEmpty()) {
				sb.append(", \"tcp_dst\":");
				sb.append("\"");
				sb.append(rD.getTp_src());
				sb.append("\"");
			}

			// Destination TCP Port
			if (!rD.getTp_dst().isEmpty()) {
				sb.append(", \"tcp_src\":");
				sb.append("\"");
				sb.append(rD.getTp_dst());
				sb.append("\"");
			}
		}

		// TODO UDP
		sb.append("}");

		// Active
		sb.append(", \"active\":");
		sb.append("\"");
		sb.append(rD.isActive());
		sb.append("\"");

		// Actions
		sb.append(", \"actions\":");
		sb.append("[");

		// Set Output
		sb.append("{");
		sb.append("\"type\":");
		sb.append("\"OUTPUT\"");

		sb.append(", \"port\":");
		sb.append(rD.getIn_port());
		sb.append("}");

		sb.append("]");
		sb.append("}");

		rD.setJson(sb.toString());

		return sb.toString();

	}

	public void buscar() {
		try {
			if (regra.getIpv4_src() != null && regra.getIpv4_dst() != null) {
				URI sSource = Util.search(regra.getIpv4_src()), sDestination = Util.search(regra.getIpv4_dst());
				if (sSource != null && sDestination != null) {
					String path = Util.dijkstra(sSource, sDestination);
					create(path);
				}
			}
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

	}

	public RegrasData getRegra() {
		return regra;
	}

	public List<RegrasData> getRegras() {
		return regras;
	}

	public RegrasData getRegraSelecionada() {
		return regraSelecionada;
	}

	public void setRegraSelecionada(RegrasData regraSelecionada) {
		this.regraSelecionada = regraSelecionada;
	}
}

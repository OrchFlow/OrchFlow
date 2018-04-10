package controladores;

import java.net.URI;
import java.net.URISyntaxException;
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
import org.json.JSONObject;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import modelos.ControlData;
import modelos.RegrasData;
import neo4j.Neo4jHosts;
import pacote.Util;

@SessionScoped
@ManagedBean
public class ReactiveRoute {
	private RegrasData regra;
	private RegrasData regraSelecionada;
	private static List<RegrasData> regras;
	private static List<ControlData> controllers = new ArrayList<ControlData>();
	public static String SERVER_ROOT_URI = "http://localhost:7474/db/data/";

	@PostConstruct
	public void init() {
		regra = new RegrasData();
		regra.setActive(true);
		regra.setIdle_timeout(60);
		regra.setHard_timeout(60);
		regra.setPriority(32767);
		regra.setCookie("0");
		regra.setEth_type("0x800");
		regraSelecionada = new RegrasData();
		regras = new ArrayList<RegrasData>();
	}

	public void reinit() {
		regra = new RegrasData();
		regra.setActive(true);
		regra.setIdle_timeout(60);
		regra.setHard_timeout(60);
		regra.setPriority(32767);
		regra.setCookie("0");
		regra.setEth_type("0x800");
	}

	public void exec(boolean hibrido) {
		regra.setHibrido(hibrido);
		if (!regras.contains(regra)) {
			String json = generateJsonReactive();
			regra.setJson(buscar(json));
			regras.add(regra);
			criaJSON();
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_INFO, "Rota reativa criada com sucesso!", null);
			FacesContext.getCurrentInstance().addMessage(null, msg);
			reinit();
		} else {
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "Nome duplicado!", null);
			FacesContext.getCurrentInstance().addMessage(null, msg);
		}
	}

	private void criaJSON() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < regras.size(); i++) {
			sb.append(regras.get(i).getJson());
			if (regras.size() > i + 1) {
				sb.append(",");
			}
		}
		sendReactiveData(sb.toString());
	}

	public void removeRegra(RegrasData obj) {
		regras.remove(obj);
		criaJSON();
	}

	private String generateJsonReactive() {
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
		return sb.toString();
	}

	private String buscar(String json) {
		try {
			if (regra.getIpv4_src() != null && regra.getIpv4_dst() != null) {
				URI sSource = Util.search(regra.getIpv4_src()), sDestination = Util.search(regra.getIpv4_dst());
				if (sSource != null && sDestination != null) {
					String path = Util.dijkstra(sSource, sDestination);
					return completeJsonReactive(path, json);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	private String completeJsonReactive(String path, String json) throws Exception {
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
		System.out.println(path);
		if (path.contains("[")) {
			JSONArray rel = new JSONArray(path);
			StringBuilder sb = new StringBuilder();

			String next = "";
			for (int j = 0; j < rel.length() - 1; j++) {
				sb.append(next);
				sb.append(json);
				next = ",";
				String dpid, out, in, control;
				String link = rel.get(j).toString();
				String proxLink = rel.get(j + 1).toString();

				JSONObject JsonRel = Util.getRel(link);
				JSONObject JsonRelProx = Util.getRel(proxLink);

				JSONObject data = new JSONObject(JsonRel.get("data").toString());
				JSONObject dataProx = new JSONObject(JsonRelProx.get("data").toString());

				// SE O LINK ATUAL É DO TIPO HOST
				if (data.get("type").equals("host")) {
					// DPID
					dpid = data.get("srcswitch").toString();
					in = data.get("srcport").toString();
					control = Util.getControl(JsonRel.get("start").toString());
					if (data.get("srcswitch").equals(dataProx.get("srcswitch"))) {
						out = dataProx.get("srcport").toString();
					} else {
						out = dataProx.get("dstport").toString();
					}

					// SE O PROXIMO LINK É DO TIPO HOST
				} else if (dataProx.get("type").equals("host")) {
					dpid = dataProx.get("srcswitch").toString();
					out = dataProx.get("srcport").toString();
					control = Util.getControl(JsonRelProx.get("start").toString());
					if (data.get("srcswitch").equals(dataProx.get("srcswitch"))) {
						in = data.get("srcport").toString();
					} else {
						in = data.get("dstport").toString();
					}

					// SE AMBOS OS LINKS SÃO INTERNOS
				} else {
					if (data.get("srcswitch").equals(dataProx.get("dstswitch"))) {
						dpid = data.get("srcswitch").toString();
						out = dataProx.get("dstport").toString();
						in = data.get("srcport").toString();
						control = Util.getControl(JsonRel.get("start").toString());
					} else if (data.get("srcswitch").equals(dataProx.get("srcswitch"))) {
						dpid = data.get("srcswitch").toString();
						out = dataProx.get("srcport").toString();
						in = data.get("srcport").toString();
						control = Util.getControl(JsonRel.get("start").toString());
					} else if (data.get("dstswitch").equals(dataProx.get("dstswitch"))) {
						dpid = data.get("dstswitch").toString();
						out = dataProx.get("dstport").toString();
						in = data.get("dstport").toString();
						control = Util.getControl(JsonRel.get("end").toString());
					} else {
						dpid = data.get("dstswitch").toString();
						out = dataProx.get("srcport").toString();
						in = data.get("dstport").toString();
						control = Util.getControl(JsonRel.get("end").toString());
					}
				}
				regra.setControl(control);
				regra.setSw(dpid);
				regra.setMac_src(mac_src);
				regra.setMac_dst(mac_dst);
				regra.setOut_port(out);
				regra.setIn_port(in);
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
				sb.append(",\"control\":");
				sb.append("\"");
				sb.append(control);
				sb.append("\"");
				sb.append(",\"dpid\":");
				sb.append("\"");
				sb.append(dpid);
				sb.append("\"");
				sb.append(",\"out_port\":");
				sb.append("\"");
				sb.append(out);
				sb.append("\"");
				sb.append(",\"in_port\":");
				sb.append("\"");
				sb.append(in);
				sb.append("\"");
				sb.append("}");
			}
			System.out.println(sb.toString());
			return sb.toString();
		}
		return null;
	}

	private void sendReactiveData(String json) {
		StringBuilder sb = new StringBuilder();
		sb.append("{");
		sb.append("\"reactive\":");
		sb.append("[");
		sb.append(json);
		sb.append("]");
		sb.append("}");

		json = sb.toString();
		System.out.println(json);

		for (int i = 0; i < controllers.size(); i++) {
			ControlData controller = controllers.get(i);
			URI fromUri;
			try {
				fromUri = new URI(
						"http://" + controller.getIp() + ":" + controller.getPort() + "/reactive/flowpusher/json");
				WebResource resource = Client.create().resource(fromUri);
				// POST JSON to the relationships URI
				ClientResponse response = resource.accept(MediaType.APPLICATION_JSON).type(MediaType.APPLICATION_JSON)
						.entity(json).post(ClientResponse.class);

				System.out.println(String.format("POST to [%s], status code [%d]", fromUri, response.getStatus()));

				response.close();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}
	}

	public static List<ControlData> getControllers() {
		return controllers;
	}

	public static void setControllers(List<ControlData> controllers) {
		ReactiveRoute.controllers = controllers;
	}

	public RegrasData getRegra() {
		return regra;
	}

	public RegrasData getRegraSelecionada() {
		return regraSelecionada;
	}

	public void setRegraSelecionada(RegrasData regraSelecionada) {
		this.regraSelecionada = regraSelecionada;
	}

	public List<RegrasData> getRegras() {
		return regras;
	}

	public static void aplicar(String nome) {
		for (RegrasData regra : regras) {
			if (regra.getName().equals(nome)) {
				JSONArray jsonArray = new JSONArray("[" + regra.getJson() + "]");
				System.out.println(jsonArray.toString());
				for (int i = 0; i < jsonArray.length(); i++) {
					try {
						JSONObject jsonRegra = jsonArray.getJSONObject(i);
						System.out.println(jsonRegra.toString());
						RegrasData r = new RegrasData();
						r.setActive(jsonRegra.getBoolean("active"));
						r.setControl(jsonRegra.getString("control"));
						r.setCookie(jsonRegra.getString("cookie"));
						r.setEth_type(jsonRegra.getString("eth_type"));
						r.setHard_timeout(jsonRegra.getInt("hard_timeout"));
						r.setIdle_timeout(jsonRegra.getInt("idle_timeout"));
						r.setIn_port(jsonRegra.getString("in_port"));
						r.setIp_proto(jsonRegra.getInt("ip_proto"));
						r.setIpv4_dst(jsonRegra.getString("ipv4_dst"));
						r.setIpv4_src(jsonRegra.getString("ipv4_src"));
						r.setOut_port(jsonRegra.getString("out_port"));
						r.setPriority(jsonRegra.getInt("priority"));
						r.setSw(jsonRegra.getString("dpid"));
						r.setTp_dst(jsonRegra.getString("tp_dst"));
						r.setTp_src(jsonRegra.getString("tp_src"));
						ProactiveRoute.aplicaRegra(r);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}

			}

		}

	}
}

package rest;

import java.util.Hashtable;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import controladores.ReactiveRoute2;

@Path("/hibrido")
public class Hibrido {
	static Hashtable <String, Long> tabRegra = new Hashtable <String, Long>();
	
	@POST
	@Consumes("text/plain")
	@Produces("text/plain")
	public Response show(String nome) {
		
		try{
			Long tempoEspera = tabRegra.get(nome);
			if (tempoEspera == null || System.currentTimeMillis() >  tempoEspera.longValue() + 60000 ) {
				tabRegra.remove(nome);
				tabRegra.put(nome,  new Long(System.currentTimeMillis()));
				System.out.println("post");
				ReactiveRoute2.aplicar(nome);
			}
		}catch(Exception e){
			e.printStackTrace();
			return Response.serverError().entity("ERRO").build();
		}
		return Response.ok(nome, MediaType.APPLICATION_JSON).build();
	}
}

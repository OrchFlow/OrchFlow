package modelos;

public class LinkData {
	private String srcS, dstS, type, direction;
	private int srcP, dstP;

	public String getSrcS() {
		return srcS;
	}

	public void setSrcS(String srcS) {
		this.srcS = srcS;
	}

	public int getSrcP() {
		return srcP;
	}

	public void setSrcP(int srcP) {
		this.srcP = srcP;
	}

	public String getDstS() {
		return dstS;
	}

	public void setDstS(String dstS) {
		this.dstS = dstS;
	}

	public int getDstP() {
		return dstP;
	}

	public void setDstP(int dstP) {
		this.dstP = dstP;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getDirection() {
		return direction;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}
}

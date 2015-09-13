package std;
import org.jdom2.Element;

import PersistenHashMap.XMLable;


public class XMLMountVolume implements XMLable {
	String name = "";
	String action = "";
	String thedirname = "";
	String mountpointname = "";
	boolean passwordprovided = false;
	String thepassword = "";
	int spaceafter = 15;
	
	public XMLMountVolume () {}
	XMLMountVolume (String thename ,String theaction, String dirname, String mountdir, boolean haspassword, String thepass, int pixspace) {
		name = thename;
		action = theaction;
		thedirname = dirname;
		mountpointname = mountdir;
		passwordprovided = haspassword;
		thepassword = thepass;
		spaceafter = pixspace;
	}
		
	public Element getXML() {
		Element returnElement = new Element (this.getClass().getName());
		returnElement.setAttribute ("name",name );
		returnElement.setAttribute ("action",action );
		returnElement.setAttribute ("dirname",thedirname );
		returnElement.setAttribute ("mountpoint",mountpointname);
		returnElement.setAttribute ("passwordprovided",String.valueOf(passwordprovided) );
		returnElement.setAttribute ("password",thepassword);
		returnElement.setAttribute ("spaceafter",String.valueOf (spaceafter));
		return returnElement;
	}
	public void loadfromXML(Element xmlelement) {
		name = String.valueOf (xmlelement.getAttributeValue("name"));
		action = String.valueOf (xmlelement.getAttributeValue("action"));
		thedirname = String.valueOf (xmlelement.getAttributeValue("dirname"));
		mountpointname = String.valueOf (xmlelement.getAttributeValue("mountpoint"));
		passwordprovided = Boolean.valueOf (xmlelement.getAttributeValue("passwordprovided"));
		thepassword = String.valueOf (xmlelement.getAttributeValue("password"));
		spaceafter = Integer.valueOf(String.valueOf (xmlelement.getAttributeValue("spaceafter")));
	}	
}

package std;

import info.clearthought.layout.TableLayout;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import Crypto.ReadWriteDES;
import SwingGUI.SimpleNotificationDialog;
import SwingGUI.SimpleQuestionDialog;
import SwingGUI.SimpleYesNoDialog;

public class GPGPanel extends JPanel implements ActionListener {
	
	// GPG Keys elements
	final static String tmpdir = System.getProperty ("user.home");
	
	int maximumCryptoStrength = 0;
	JButton loadButton = new JButton ( "Import keys" );
	JButton saveButton = new JButton ( "Export keys" );
	JButton loadinternButton = new JButton ( "Import keys from JAR" );
	JButton eraseButton = new JButton ( "Delete keys" );
	
	SimpleQuestionDialog questionDialog = null;
	SimpleYesNoDialog confirmDialog = null;
	SimpleNotificationDialog msgDialog = null;
	
	JFileChooser loadFileDialog = new JFileChooser ("Import keys");
	JFileChooser saveFileDialog = new JFileChooser ("Export keys");
	{
		loadFileDialog.setFileSelectionMode (JFileChooser.FILES_ONLY);
		saveFileDialog.setFileSelectionMode (JFileChooser.FILES_ONLY);
	}
	
	JFrame theFrame = null;
	
	public GPGPanel ( JFrame fatherFrame ) {
		theFrame = fatherFrame;
		
		// Layout for GPG-Cryptor
		double [][] sizesPanel3 = { { 10,TableLayout.FILL,10 } , 
				{ 10, TableLayout.MINIMUM, 5 , TableLayout.MINIMUM, 5 ,
			          TableLayout.MINIMUM, 5, TableLayout.MINIMUM, 10 } };
		this.setLayout (new TableLayout(sizesPanel3));	

		this.add ( loadButton , "1,1" );
		this.add ( loadinternButton , "1,3" );
		this.add ( saveButton , "1,5" );
		this.add ( eraseButton , "1,7" );
		loadButton.addActionListener (this);
		loadinternButton.addActionListener (this);
		saveButton.addActionListener (this);
		eraseButton.addActionListener ( this );
		
		confirmDialog = new SimpleYesNoDialog ();
		msgDialog = new SimpleNotificationDialog (theFrame);
		questionDialog = new SimpleQuestionDialog (theFrame,true);
		
		maximumCryptoStrength = ReadWriteDES.getCryptStrengthForCypher ("AES");
		if ( maximumCryptoStrength < 256 )	{
			System.err.println ("Maximum crypto-strength is only "+maximumCryptoStrength+"!");
			msgDialog.showQuestionDialog("Maximum cryptographic strength is only "+maximumCryptoStrength +
					"! Please install 'Java Cryptography Extension (JCE) Unlimited Strength'!");
		}
	}

	public void actionPerformed ( ActionEvent e) {
		if ( e.getSource() == loadButton )	{
			System.out.println ("Load");
			if ( loadFileDialog.showOpenDialog (this) == JFileChooser.APPROVE_OPTION )	{		
				questionDialog.setAnswerString( "" );
				String thePass = questionDialog.showQuestionDialog ("Please type password");
				String loadname = loadFileDialog.getSelectedFile().getPath();
				try {
					if ( !loadKeyFromStream ( new FileInputStream (new File (loadname) ), thePass) )	{
						msgDialog.showQuestionDialog ("Password is not correct!");
					}
				}
				catch ( Exception ex)	{	System.err.println ("Error: "+ex.getLocalizedMessage());	}
			}
		
		}
		else if ( e.getSource() == loadinternButton ){
			System.out.println ("Load Intern");
			questionDialog.setAnswerString( "" );
			String thePass = questionDialog.showQuestionDialog ("Please type password");
			System.out.println ("Password was:"+thePass+"!!!");
			InputStream instream = Thread.currentThread().getContextClassLoader().getResourceAsStream ("encryptedkeys.xml");
			if ( !loadKeyFromStream ( instream, thePass) )	{
				msgDialog.showQuestionDialog ("Password is not correct!");
			}			
		}
		else if ( e.getSource() == saveButton ) {
			System.out.println ("Save");
			questionDialog.setAnswerString( "" );
			String thePass = questionDialog.showQuestionDialog ("Please type password");
			String thePassVerify = null;
			if ( questionDialog.wasAccepted() )	{
				questionDialog.setAnswerString( "" );
				thePassVerify = questionDialog.showQuestionDialog ("Please type password again");
			
				if ( questionDialog.wasAccepted() )	{
					System.out.println ("Ok: Encrypting...: "+thePass+" == " + thePassVerify );
				
					if ( thePassVerify.equals(thePass) )	{
						if ( saveFileDialog.showSaveDialog (this) == JFileChooser.APPROVE_OPTION )	{
							saveKeys ( saveFileDialog.getSelectedFile().getPath(), thePass);
						}
					}
					else	{
						msgDialog.showQuestionDialog ("Passwords are not the same!");
					}
				}
			}
		}
		else if ( e.getSource() == eraseButton ) {
			if ( confirmDialog.showQuestionDialog ( "Erase keys?" ) )	{
				deleteKeys ();
			}
		}
	}

	private void deleteKeys ()	{
		// gpg --list-keys | grep pub
		String [] executestr = { "/usr/local/bin/gpg" , "--list-secret-keys" , "--with-colons" , "--fingerprint"};
		System.out.println("Removing keys: ");
		try {
			// delete all secret keys
			Vector <String> fingerprints = new Vector <String> ();
			Process p = Runtime.getRuntime().exec (executestr);
			OutputStream stdin = p.getOutputStream ();
			InputStream stderr = p.getErrorStream ();
			InputStream stdout = p.getInputStream ();
			BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
			String line ="";		
			while ((line = reader.readLine()) != null) {
				if ( line.indexOf("fpr") == 0 )	{	fingerprints.add (line.substring( 12, 52));	}
			}
		
			for ( int i=0 ; i < fingerprints.size() ; i++)	{
				System.out.println ("  Deleting secret key with fingerprint: "+fingerprints.get(i));
				String [] executestr2 = { "/usr/local/bin/gpg" ,"--batch", "--delete-secret-keys" , fingerprints.get(i) };
				p = Runtime.getRuntime().exec (executestr2);
				stdin = p.getOutputStream ();
				stderr = p.getErrorStream ();
				stdout = p.getInputStream ();
				reader = new BufferedReader (new InputStreamReader(stdout));
				writer = new BufferedWriter (new OutputStreamWriter(stdin));	
				while ((line = reader.readLine()) != null) {	System.out.println ("Stdout: " + line);	}
			}
		
		
			// delete all public keys
			String [] executestr3 = { "/usr/local/bin/gpg" , "--list-keys" , "--with-colons" , "--fingerprint"};
			fingerprints = new Vector <String> ();
			p = Runtime.getRuntime().exec (executestr3);
			stdin = p.getOutputStream ();
			stderr = p.getErrorStream ();
			stdout = p.getInputStream ();
			reader = new BufferedReader (new InputStreamReader(stdout));
			writer = new BufferedWriter(new OutputStreamWriter(stdin));
			line ="";		
			while ((line = reader.readLine()) != null) {
				if ( line.indexOf("fpr") == 0 )	{	fingerprints.add (line.substring( 12, 52));	}
			}
		
			for ( int i=0 ; i < fingerprints.size() ; i++)	{
				System.out.println ("  Deleting public key with fingerprint: "+fingerprints.get(i));
				String [] executestr2 = { "/usr/local/bin/gpg" ,"--batch", "--delete-keys" , fingerprints.get(i) };
								
				// delete publc-key
				p = Runtime.getRuntime().exec (executestr2);
				stdin = p.getOutputStream ();
				stderr = p.getErrorStream ();
				stdout = p.getInputStream ();
				reader = new BufferedReader (new InputStreamReader(stdout));
				writer = new BufferedWriter (new OutputStreamWriter(stdin));	
				while ((line = reader.readLine()) != null) {	System.out.println ("Stdout: " + line);	}
			}
		}
		catch (Exception ex) { System.err.println("Error during execution3: "+ex.getLocalizedMessage()); }
	}

	private boolean loadKeyFromStream ( InputStream instream , String passwordStr ) {
		String tmpPublicFilename = nonexistingfilename (tmpdir , "publickey" , "");
		String tmpPrivateFilename = nonexistingfilename (tmpdir , "privatekey" , "");

		try {
			SAXBuilder builder = new SAXBuilder();
			Document doc = builder.build (instream);						
			Element tt = doc.getRootElement();
			String pubkeysencrypted = String.valueOf (tt.getAttributeValue("pubkeys")) ;
			String privkeysencrypted = String.valueOf (tt.getAttributeValue("privkeys")) ;
		
			System.out.println ("Size public : "+pubkeysencrypted.length());
			System.out.println ("Size private: "+privkeysencrypted.length());
		
			byte [] secretkey = ReadWriteDES.getSHA256 (passwordStr);
			byte [] encryptedpubkeykytes = ReadWriteDES.decodeBase64 (pubkeysencrypted);
			byte [] encryptedprivkeykytes = ReadWriteDES.decodeBase64 (privkeysencrypted);
			byte [] pubkeybytes = ReadWriteDES.decryptAES (encryptedpubkeykytes, secretkey);
			byte [] privkeybytes = ReadWriteDES.decryptAES (encryptedprivkeykytes, secretkey);
		
			OutputStream fout = new FileOutputStream ( new File (tmpPublicFilename) );
			fout.write (pubkeybytes );
			fout.close();
		
			fout = new FileOutputStream ( new File (tmpPrivateFilename) );
			fout.write (privkeybytes );
			fout.close();
		
			String [] executestr = { "/usr/local/bin/gpg" , "--import" , tmpPrivateFilename };
			Process p = Runtime.getRuntime().exec (executestr);
			OutputStream stdin = p.getOutputStream ();
			InputStream stderr = p.getErrorStream ();
			InputStream stdout = p.getInputStream ();
			BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
			String line ="";		
			while ((line = reader.readLine()) != null) {	System.out.println ("Stdout: " + line);	}
					
			String [] executestr2 = { "/usr/local/bin/gpg" , "--import" , tmpPublicFilename };
			p = Runtime.getRuntime().exec (executestr2);
			stdin = p.getOutputStream ();
			stderr = p.getErrorStream ();
			stdout = p.getInputStream ();
			reader = new BufferedReader (new InputStreamReader(stdout));
			writer = new BufferedWriter(new OutputStreamWriter(stdin));
			line ="";		
			while ((line = reader.readLine()) != null) {	System.out.println ("Stdout: " + line);	}
		}
		catch (Exception ex) { System.out.println(ex.getLocalizedMessage()); return false; }
		finally	{
			File onefile = new File (tmpPublicFilename);
			if ( onefile.exists() )	{	onefile.delete();	}
			onefile = new File (tmpPrivateFilename);
			if ( onefile.exists() )	{	onefile.delete();	}
		}		
		return true;
	}

	private void saveKeys ( String filename , String passwordStr )	{
		System.out.println ("Saving: "+filename+"  Password: "+passwordStr);
		String tmpPublicFilename = nonexistingfilename (tmpdir , "publickey" , "");
		String tmpPrivateFilename = nonexistingfilename (tmpdir , "privatekey" , "");
		System.out.println ("Public: " + tmpPublicFilename + "   Private: " + tmpPrivateFilename);
	
		String [] executestr = { "/usr/local/bin/gpg" , "-a" , "-o" , tmpPrivateFilename, "--export-secret-keys"};
		System.out.println("Executing: ");
		try {
			// export secret-keys into file
			Process p = Runtime.getRuntime().exec (executestr);
			OutputStream stdin = p.getOutputStream ();
			InputStream stderr = p.getErrorStream ();
			InputStream stdout = p.getInputStream ();
			BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
			String line ="";		
			while ((line = reader.readLine()) != null) {	System.out.println ("Stdout: " + line);	}

		
			// export public-keys into file
			String [] executestr2 = { "/usr/local/bin/gpg" , "-a" , "-o" , tmpPublicFilename, "--export"};
			System.out.println("Executing: ");
			p = Runtime.getRuntime().exec (executestr2);
			stdin = p.getOutputStream ();
			stderr = p.getErrorStream ();
			stdout = p.getInputStream ();
			reader = new BufferedReader (new InputStreamReader(stdout));
			writer = new BufferedWriter(new OutputStreamWriter(stdin));
			line ="";		
			while ((line = reader.readLine()) != null) {	System.out.println ("Stdout: " + line);	}

			// encrypt private keys 
			FileInputStream fis = new FileInputStream ( new File (tmpPrivateFilename) );
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
        	byte[] buf = new byte[1024];
        	for ( int readNum; (readNum = fis.read(buf)) != -1;) {
        		bos.write(buf, 0, readNum); //no doubt here is 0
        	}
        	bos.close();
        	byte[] privatebytes = bos.toByteArray();
        	byte [] secretkey = ReadWriteDES.getSHA256(passwordStr);
        	byte [] privateencryptedbytes = ReadWriteDES.encryptAES (privatebytes, secretkey);
        	String privKeyBase64Str = ReadWriteDES.encodeBase64 (privateencryptedbytes);
        	System.out.println ("Size private: "+privKeyBase64Str.length());
        	        
        	// encrypt public keys
        	fis = new FileInputStream ( new File (tmpPublicFilename) );
			bos = new ByteArrayOutputStream();
        	buf = new byte[1024];
        	for ( int readNum; (readNum = fis.read(buf)) != -1;) {
        	bos.write(buf, 0, readNum); //no doubt here is 0
        	}
        	bos.close();
        	byte[] publicbytes = bos.toByteArray();
        	byte [] publicencryptedbytes = ReadWriteDES.encryptAES (publicbytes, secretkey);
        	String pubKeyBase64Str = ReadWriteDES.encodeBase64 (publicencryptedbytes);
        	System.out.println ("Size public: "+pubKeyBase64Str.length());
        	//System.out.println ("Public Key: " + pubKeyBase64Str );	 
        
        
        	// save both encrypted keys as xml-file
			Element tt = new Element ( "gpgkeyconfiguration" );
			tt.setAttribute( "pubkeys" , pubKeyBase64Str);
			tt.setAttribute( "privkeys" , privKeyBase64Str);
		
			Format format = Format.getPrettyFormat();
			format.setEncoding("ISO-8859-1");
			XMLOutputter out = new XMLOutputter(format);
			Document doc= new Document(tt);	
			try {
				FileOutputStream o = new FileOutputStream ( new File (filename));
				out.output( doc, o);
			}
			catch (Exception ex) { System.out.println(ex.getMessage()); }
		}
		catch (Exception ex) { System.err.println("Error during execution3: "+ex.getLocalizedMessage()); }
		finally	{
			File onefile = new File (tmpPublicFilename);
			if ( onefile.exists() )	{	onefile.delete();	}
			onefile = new File (tmpPrivateFilename);
			if ( onefile.exists() )	{	onefile.delete();	}
		}		
	}


	public static String nonexistingfilename (String dirname, String prefix, String postfix)	{
		String result = "";
		if ( dirname.endsWith(File.separator))	{	dirname = dirname.substring(0, dirname.length()-1);	}
		System.out.println ("Dirname: "+dirname);
	
		for ( int i = 0; i < 10000; i++)	{
			File testfile = new File (dirname + File.separator + prefix + i + postfix);
			if ( !testfile.exists() )	{		
				result = dirname + File.separator + prefix + i + postfix;	break;
			}
		}
		System.out.println ("Result: "+result );
		return result;
	}
}

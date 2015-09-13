package std;
import info.clearthought.layout.TableLayout;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import Crypto.ReadWriteDES;
import SwingGUI.IconTask;
import SwingGUI.KeyDispatcher;
import SwingGUI.SimpleQuestionDialog;

public class VolumeMounter extends JFrame implements ActionListener {
	final static String applicationname = "Vol-Mounter";
	final static String applicationversion = "12.2";
	
	KeyDispatcher oneKeyDispatcher = new KeyDispatcher (this);
	StringBuilder tempPasswd = new StringBuilder ("");
	boolean hiddenGUIVisible = false;
	
	Container contentPane = getContentPane();
	VolumePanel firstPanel = new VolumePanel ( this );
	VolumePanel secondPanel =new VolumePanel ( this );
	GPGPanel thirdPanel = new GPGPanel ( this );
	JPanel fourthPanel = new JPanel ();
	JTabbedPane tabbedPane = new JTabbedPane ();
				
	JButton encryptFileButton = new JButton ("Encrypt File");
	JButton decryptFileButton = new JButton ("Decrypt File");
	JButton normalViewButton   = new JButton ("Normal View");
	
	JFileChooser encryptFileDialog = new JFileChooser ("Encrypt File");
	JFileChooser decryptFileDialog = new JFileChooser ("Decrpyt File");	
	{
		encryptFileDialog.setFileSelectionMode (JFileChooser.FILES_ONLY);
		decryptFileDialog.setFileSelectionMode (JFileChooser.FILES_ONLY);
	}

	SimpleQuestionDialog questionDialog = null;

	JFrame thisframe = this;
	JButton testbutton = new JButton ("test");
	Font thefont = testbutton.getFont().deriveFont ( 14f );
		
	{	setDefaultCloseOperation( DO_NOTHING_ON_CLOSE );	addWindowListener (new ExitRoutine());	}
	private class ExitRoutine extends WindowAdapter {
		public void windowClosing( WindowEvent e ) { 
			try {	SwingUtilities.invokeLater (new Runnable() {	public void run() {		System.exit(0);	} }); }
			catch (Exception ex) { System.err.println(ex.getLocalizedMessage());}			
		}
	}	
	
	VolumeMounter () {
		//first we have to load the configurationfile which is of the type PersistentHashMap
		try {
			// URL theFontURL = oneWindow.getClass().getClassLoader().getResource("gloriahallelujah.ttf");
			InputStream fin1 = Thread.currentThread().getContextClassLoader().getResourceAsStream ("MountPoints.xml");			
			File tempfile = new File (System.getProperty ("java.io.tmpdir") );
			String tempfilename= tempfile.getCanonicalPath()+File.separator+"MountPoints.xml";
			System.out.println (tempfilename);
			firstPanel.loadButtons ( fin1, tempfilename );
		}
		catch (Exception ex) { System.err.println ("Unable to load ressource: "+ex.getLocalizedMessage()); }	
		
		questionDialog = new SimpleQuestionDialog (this,true);
		
		Dimension screensize = java.awt.Toolkit.getDefaultToolkit().getScreenSize ();
		setTitle(applicationname+" V"+applicationversion);	
		
		double [][] sizesPanel4 = { { 10,TableLayout.FILL,10 } , 
				{ 10, TableLayout.MINIMUM, 5 , TableLayout.MINIMUM, 50 ,
			          TableLayout.MINIMUM, 5, TableLayout.MINIMUM, 10 } };
		fourthPanel.setLayout (new TableLayout (sizesPanel4) );	

		fourthPanel.add ( encryptFileButton , "1,1" );
		fourthPanel.add ( decryptFileButton , "1,3" );
		fourthPanel.add ( normalViewButton , "1,5" );
		encryptFileButton.addActionListener (this);
		decryptFileButton.addActionListener (this);
		normalViewButton.addActionListener (this);
				
		adjustLayout ();
		pack ();
		setLocation ( (screensize.width - getSize().width) /2, screensize.height / 8 );		
		oneKeyDispatcher.addActionListener (this);
	}
	
	private void adjustLayout () {
		contentPane.removeAll ();
		if  ( !hiddenGUIVisible )	{
			contentPane.add ( firstPanel );
			contentPane.setPreferredSize (new Dimension ( 380 , firstPanel.getPreferredSize().height ));
		}
		else {
			tabbedPane.removeAll ();
			tabbedPane.add ("Volumes",firstPanel);
			tabbedPane.add ("Secret Volumes",secondPanel);
			tabbedPane.add ("Encryptor",thirdPanel);
			tabbedPane.add ("Cryptor",fourthPanel);
			contentPane.add ( tabbedPane );
			contentPane.setPreferredSize (new Dimension ( 380 , tabbedPane.getPreferredSize().height ));
		}		
	}
	
	private void loadSecretVolumes ( String thePassword ) {
		try {			
			InputStream fin2 = Thread.currentThread().getContextClassLoader().getResourceAsStream ("MountEncrypted.xml");
			File tempfile = new File (System.getProperty ("java.io.tmpdir") );
			String tempfilename= tempfile.getCanonicalPath()+File.separator+"MountDecrypted.xml";
			FileOutputStream fout2 = new FileOutputStream ( new File (tempfilename) );
			FileMethods.FileMethods.streamcopy (fin2, fout2);
			fin2.close();
			fout2.close();
			decryptFile ( tempfilename, thePassword );
			
			System.out.println ( tempfilename );
			FileInputStream fin3 = new FileInputStream (new File(tempfilename)); 
			secondPanel.loadButtons ( fin3, tempfilename );
			fin3.close();
			File delFile = new File ( tempfilename );
			if ( delFile.delete() ) {	System.out.println ("Tempfile successfully deleted");	}
			else					{	System.out.println ("Delete failed!!!");				}
		}	
		catch (Exception ex) { System.err.println ("Unable to load ressource: "+ex.getLocalizedMessage()); }
	}
	
	
	private String passwordCorrect ( String password ) {
		for ( int length = 1 ; length <= password.length() ; length++ )	{
			String testpass = password.substring( password.length() -length , password.length());
			try {
				byte [] secretkey = ReadWriteDES.getSHA256 ( testpass.toString() );
				String hash = ReadWriteDES.encodeBase64 (secretkey);
				// System.out.println ("Pass:"+testpass+ " Hash:"+hash);
				if ( hash.equals ("O/Xk6w4jM40wf/1NKsoq2XATmSygov/tORapHreOpPg=") )	{	return testpass;	}
			}
			catch (Exception ex) { System.err.println ("Unable to load ressource: "+ex.getLocalizedMessage()); }
		}
		return null;
	}
	
	public void actionPerformed (ActionEvent ev) {	
		if ( ev.getSource() == oneKeyDispatcher )	{
			if ( !hiddenGUIVisible )	{
				String pressedKey = ev.getActionCommand();
				tempPasswd.append (pressedKey);
				if ( tempPasswd.length() > 20 )	{	tempPasswd.delete(0, tempPasswd.length() - 20 );	}
				
				//System.out.println ("Key: "+tempPasswd);
				
				try {
					String testPassword = passwordCorrect ( tempPasswd.toString() );
					if ( testPassword != null )	{
						hiddenGUIVisible = true;
						loadSecretVolumes ( testPassword );
					}
				}
				catch ( Exception ex )		{
					System.err.println ("Decoding failed: "+ex.getLocalizedMessage());
				}
				adjustLayout();
				pack ();
			}
		}
		else if ( ev.getSource() == normalViewButton ) {
			hiddenGUIVisible = false;
			adjustLayout();
			pack ();
		}
		else if ( ev.getSource() == encryptFileButton ) {
			if ( encryptFileDialog.showOpenDialog (this) == JFileChooser.APPROVE_OPTION )	{		
				questionDialog.setAnswerString( "" );
				String thePass = questionDialog.showQuestionDialog ("Please type password");
				String loadname = encryptFileDialog.getSelectedFile().getPath();
				// System.out.println ("Filename: "+loadname);
				// System.out.println ("Password: "+thePass);
				encryptFile (loadname, thePass);
			}
		}
		else if ( ev.getSource() == decryptFileButton ) {
			if ( decryptFileDialog.showOpenDialog (this) == JFileChooser.APPROVE_OPTION )	{		
				questionDialog.setAnswerString( "" );
				String thePass = questionDialog.showQuestionDialog ("Please type password");
				String loadname = decryptFileDialog.getSelectedFile().getPath();
				// System.out.println ("Filename: "+loadname);
				// System.out.println ("Password: "+thePass);
				decryptFile (loadname, thePass);
			}
		}
	}
	
	
	private void encryptFile ( String filename , String passWord )	{
		// encrypt private keys 
		try {
			FileInputStream fis = new FileInputStream ( new File (filename) );
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte [50000];
			for ( int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write (buf, 0, readNum); //no doubt here is 0
			}
			bos.close();
			byte[] privatebytes = bos.toByteArray();
			byte [] secretkey = ReadWriteDES.getSHA256 (passWord);
			byte [] privateencryptedbytes = ReadWriteDES.encryptAES (privatebytes, secretkey);
			String privKeyBase64Str = ReadWriteDES.encodeBase64 (privateencryptedbytes);
			
			int byteLength = privateencryptedbytes.length;
			int strlength = privKeyBase64Str.length();
			System.out.println ("StringLength:"+strlength+ " ByteLength:"+byteLength);
			
			FileWriter fo = new FileWriter ( new File (filename) );
			fo.write (privKeyBase64Str);
			fo.flush();
			fo.close();
			//System.out.println ("Size private: "+privKeyBase64Str.length() );
		}
		catch(Exception ex) {	System.err.println ( "Could not encrypt: "+ex.getLocalizedMessage()); }
	}

	private void decryptFile ( String filename , String passWord )	{
		// encrypt private keys 
		try {
			FileInputStream fis = new FileInputStream ( new File (filename) );
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buf = new byte [30000];
			for ( int readNum; (readNum = fis.read(buf)) != -1;) {
				bos.write(buf, 0, readNum); //no doubt here is 0
			}
			bos.close();
			fis.close();
			byte[] privatebytes = bos.toByteArray();
			String encodedStr = new String ( privatebytes );
			int strlength = encodedStr.length();
			privatebytes = ReadWriteDES.decodeBase64 (encodedStr);
			int byteLength = privatebytes.length;
			System.out.println ("StringLength:"+strlength+ " ByteLength:"+byteLength);
			
			byte [] secretkey = ReadWriteDES.getSHA256 (passWord);
			byte [] privatedecryptedbytes = ReadWriteDES.decryptAES (privatebytes, secretkey);
			
			FileOutputStream fo = new FileOutputStream ( new File (filename) );
			fo.write (privatedecryptedbytes );
			fo.flush();
			fo.close ();
		}
		catch(Exception ex) {	System.err.println ( "Could not decrypt: "+ex.getLocalizedMessage()); }
	}

	
	public static void main(String[] args) {
		try {
			System.setProperty ("apple.laf.useScreenMenuBar", "true");			
		    // UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
			UIManager.setLookAndFeel (UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception ex) {	System.err.println ( "Could not set layout: "+ex.getLocalizedMessage()); }
				
		IconTask.IconTaskInit ( "mounter.png", "", 0 );
		VolumeMounter oneMounter = new VolumeMounter();
		oneMounter.setVisible (true);
	}
}




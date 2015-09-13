package std;

import info.clearthought.layout.TableLayout;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.concurrent.TimeUnit;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import PersistenHashMap.PersistentHashMap;
import SwingGUI.SimpleNotificationDialog;
import SwingGUI.SimpleQuestionDialog;

public class VolumePanel extends JPanel implements ActionListener {

	PersistentHashMap <String , XMLMountVolume> myMountpoints;
	JToggleButton [] volumebuttons;
	
	enum MountResult { OK , WRONGPASS , OTHER };
	enum MountError { OK, NoCryptedDirectory, WrongPassword, CANCELLED, OtherError};	
	MountResult theMountRes = MountResult.OK; 
	
	JButton testbutton = new JButton ("test");
	Font thefont = testbutton.getFont().deriveFont (14f);
	String tmpFilename = "";
	
	SimpleQuestionDialog questionDialog = null;
	SimpleNotificationDialog msgDialog = null;
	
	JFrame theFrame = null;
	public VolumePanel (  JFrame fatherFrame ) {
		theFrame = fatherFrame;
		msgDialog = new SimpleNotificationDialog ( theFrame );
		questionDialog = new SimpleQuestionDialog ( theFrame,true);
	}
	
	public void loadButtons ( InputStream fin, String tmpFileName) {
		tmpFilename = tmpFileName;
		myMountpoints = new PersistentHashMap <String, XMLMountVolume> ( tmpFilename, fin );
		
		int numberofpoints = myMountpoints.size();
		System.out.println ("Number of entries: "+numberofpoints);
		volumebuttons = new JToggleButton [numberofpoints];
		
		double [][] sizes = new double [2] [2+numberofpoints*2];
		sizes [0] [0] = 10;	sizes [0] [1] = TableLayout.FILL;	sizes [0] [2] = 10;	
		
		sizes [1] [0] = 2;
		sizes [1] [2+numberofpoints]=2;
		
		for (int i=0 ; i < numberofpoints ; i++) {
			XMLMountVolume aVolume = (XMLMountVolume) myMountpoints.get (String.valueOf (i) );

			sizes [1] [i*2 + 1] = TableLayout.MINIMUM;
			sizes [1] [i*2 + 2] = aVolume.spaceafter;
			
			volumebuttons [i] = new JToggleButton (aVolume.name);
			volumebuttons [i].setFont (thefont);
		}
		this.setLayout ( new TableLayout(sizes) );	
	
		for (int i=0 ; i < volumebuttons.length ; i++) {
			this.add(volumebuttons [i],"1,"+(1+i*2));
			volumebuttons [i].addActionListener(this);
		}
		
	}
	
	
	public void actionPerformed ( ActionEvent ev ) {
		for (int i=0 ; i < volumebuttons.length ; i++) {
			if (ev.getSource()==volumebuttons[i]) {
				XMLMountVolume aVolume = (XMLMountVolume) myMountpoints.get (String.valueOf(i) );
				
				if ( aVolume.action.equals ("mountencfs") )		{
					MountError mountresult =doencfsmount ( aVolume,volumebuttons[i].isSelected() );
					if ( mountresult == MountError.OK )	{	}
					else	{
						volumebuttons [i].setSelected( false );
						if ( mountresult == MountError.NoCryptedDirectory )	{ msgDialog.showQuestionDialog ( "No crypted dir!" );	}
						else if ( mountresult == MountError.CANCELLED )	    { 										 				}
						else if ( mountresult == MountError.WrongPassword ) { msgDialog.showQuestionDialog ("Wrong password!");		}
						else if ( mountresult == MountError.OtherError)		{ msgDialog.showQuestionDialog ("Couldn't mount!");		}
					}
				}
				else if ( aVolume.action.equals ("mountsshfs") )		{
					System.out.println ("MountSSHFS");
					MountError mountresult = dosshfsmount ( aVolume,volumebuttons[i].isSelected() );
					if ( mountresult == MountError.OK )	{	}
					else	{
						volumebuttons [i].setSelected( false );
						if ( mountresult == MountError.CANCELLED )	    	{ 										 				}
						else if ( mountresult == MountError.WrongPassword ) { msgDialog.showQuestionDialog ("Wrong password!");		}
						else if ( mountresult == MountError.OtherError)		{ msgDialog.showQuestionDialog ("Couldn't mount!");		}						
					}
				}
				

			}
		}	
	}
	
	private  MountError doencfsmount ( XMLMountVolume theXMLVolume, boolean mountflag)	{
		if ( mountflag ) {
			// test whether crypted dir exists
			if ( !( new File (theXMLVolume.thedirname).exists()) )	{	return MountError.NoCryptedDirectory;	}
			
			// ask for password if not provided
			String thePass="";
			if ( theXMLVolume.passwordprovided )	{	thePass=theXMLVolume.thepassword;	}
			else	{
				thePass = questionDialog.showQuestionDialog ("Please type password");
				if ( !questionDialog.wasAccepted() )					{	return	MountError.CANCELLED;			}
			}
			
			// try to mount
			theMountRes = mountvolume (theXMLVolume.thedirname,theXMLVolume.mountpointname, thePass); 
			if ( theMountRes == MountResult.OK ) 					{	return MountError.OK;					}	// Mounting performed successfully
			else {												// something went wrong 
				if ( theMountRes == MountResult.WRONGPASS )			{ return MountError.WrongPassword; 	}
				else if ( theMountRes == MountResult.OTHER )		{ return MountError.OtherError;     }
			}
		}
		else {	unmountvolume ( theXMLVolume.mountpointname );		 }	
		return MountError.OK;
	}
	
	private  MountError dosshfsmount ( XMLMountVolume theXMLVolume, boolean mountflag)	{
		if ( mountflag ) {
			// ask for password if not provided
			String thePass="";
			if ( theXMLVolume.passwordprovided )	{	thePass=theXMLVolume.thepassword;	}
			else	{
				thePass = questionDialog.showQuestionDialog ("Please type password");
				if ( !questionDialog.wasAccepted() )		{	return	MountError.CANCELLED;			}
			}

			theMountRes = mountsshfsvolume ( theXMLVolume.thedirname ,theXMLVolume.mountpointname, thePass );
			System.out.println (theMountRes);
			if ( theMountRes == MountResult.WRONGPASS  )	{	return MountError.WrongPassword;	}
			else if ( theMountRes == MountResult.OTHER )	{	return MountError.OtherError;		}
		}
		else {	unmountvolume ( theXMLVolume.mountpointname );		 }	
		return MountError.OK;
	}
	
	
	private MountResult mountsshfsvolume (String dirname, String mountpoint, String password) {
		boolean result = true;
		File thedir = new File (mountpoint);
		if ( !thedir.exists() ) { thedir.mkdir(); }
		
		String [] executestr = new String[] { "/usr/local/bin/sshfs" , dirname , mountpoint , "-o" , "password_stdin"};
		System.out.println("Executing: "+executestr[0]+" \""+executestr[1] +"\" \""+executestr[2]+"\"");
		try {
			Process p = Runtime.getRuntime().exec(executestr);
			OutputStream stdin = p.getOutputStream ();
			InputStream stderr = p.getErrorStream ();
			InputStream stdout = p.getInputStream ();
			
			BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
			BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(stdin));
			
			writer.write ( password+"\n");	writer.flush ();	writer.close();
			//int thechar=0;
		    // while (  (reader.ready())  && ( thechar = reader.read()) > -1) {	System.out.write (thechar);	}	
		    p.waitFor(10, TimeUnit.SECONDS);
		    
			if ( p.exitValue()==0 ) {	return MountResult.OK;	}
			else 					{	return MountResult.WRONGPASS;	}
		}
		catch (Exception ex) { System.err.println("Error during execution: "+ex.getLocalizedMessage()); return MountResult.OTHER;
		}	
	}
	
	private MountResult mountvolume (String dirname, String mountpoint, String password) {
		boolean result = true;
		File thedir = new File (mountpoint);
		if ( !thedir.exists() ) { thedir.mkdir(); }
	
		String [] executestr = new String[] { "/usr/local/bin/encfs" , dirname , mountpoint};
		System.out.println("Executing: "+executestr[0]+" \""+executestr[1] +"\" \""+executestr[2]+"\"");
		try {
			Process p = Runtime.getRuntime().exec(executestr);
			OutputStream stdin = p.getOutputStream ();
			InputStream stderr = p.getErrorStream ();
			InputStream stdout = p.getInputStream ();
			
			BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
			BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(stdin));

			int thechar; 				
			writer.write ( password + "\n" );	writer.flush ();
		    while (( thechar = reader.read()) > -1) {	System.out.write (thechar);	result=false;	}	
			
			if (result == false) {	return MountResult.WRONGPASS;	}
			if ( result && p.exitValue()!=0) {		}
		}
		catch (Exception ex) { System.err.println("Error during execution: "+ex.getLocalizedMessage()); return MountResult.OTHER;
		}	
		return MountResult.OK;
	}
	
	private void unmountvolume ( String mountpoint ) {
		String [] executestr = {"umount",mountpoint};
		System.out.println("Executing: "+executestr[0]+" \""+mountpoint+"\"");
		try {
			Process p = Runtime.getRuntime().exec(executestr);
			OutputStream stdin = p.getOutputStream ();
			InputStream stderr = p.getErrorStream ();
			InputStream stdout = p.getInputStream ();

			BufferedReader reader = new BufferedReader (new InputStreamReader(stdout));
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));

			String line ="";		
			while ((line = reader.readLine()) != null) {	System.out.println ("Stdout: " + line);	}
		}
		catch (Exception ex) { System.err.println("Error during execution: "+ex.getLocalizedMessage()); }		
	}

	

}

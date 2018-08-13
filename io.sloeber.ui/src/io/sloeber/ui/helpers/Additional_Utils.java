package io.sloeber.ui.helpers;

import java.io.File;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.MessageConsoleStream;

import io.sloeber.core.api.BoardDescriptor;
import io.sloeber.core.common.Common;

public class Additional_Utils {
	
	public static String CONSOLE_NAME ="Arduino SPIFFS";

	public String sketchName   	= "";
	public String strImagePath  = "";
	public String strAddress 	= "";
	public String strFlashMode 	= "";
	public String strFlashFreq  = "";
	public String strFlashSize  = "";
	public String strSerialPort	= "";
	public String strAction     = "write_flash";
	public String strDataPath	= "";

	public String strResetMethod = "ck";
	public String strUploadSpeed = "115200";

	public String esptoolCommand;
	public String pythonCommand;
	public String mkspiffsCommand; 
	public String espotaCommand;

	public long spiStart = 0x300000;
	public long spiEnd 	 = 0x3FB000;
	public long spiPage  = 0;
	public long spiBlock = 8192;

	public long spiSize;

	public int fileCount = 0;

	public IProject myBuildProject;

	public BoardDescriptor bd;
	public Properties properties;
	public ICProjectDescription prjDesc;
	public ICConfigurationDescription confDesc;

	MessageConsoleStream console;

	public Additional_Utils(IProject project)
	{
		console = retrieveConsole();
		myBuildProject = project;		

		String strConfigName = CoreModel.getDefault().getProjectDescription(myBuildProject).getActiveConfiguration().getName();
		prjDesc = CoreModel.getDefault().getProjectDescription(myBuildProject);
		confDesc = prjDesc.getConfigurationByName(strConfigName);
		bd = BoardDescriptor.makeBoardDescriptor(confDesc);

		getParameters();	
		strDataPath = getDataPath(false);
		File f = new File(getBuildFolderPath());
		if(!f.exists())
			f.mkdirs();
		strImagePath = getBuildFolderPath() + getBuildFileName();
	}

	public MessageConsoleStream getConsole()
	{
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(console.getConsole());
		return console;
	}

	private MessageConsoleStream retrieveConsole()
	{
		MessageConsole myConsole =null;
		ConsolePlugin plugin = ConsolePlugin.getDefault();
		IConsoleManager conMan = plugin.getConsoleManager();
		IConsole[] existing = conMan.getConsoles();
		if(existing.length>0)
		{
			for(IConsole c:existing )
				if(c.getName().equals(CONSOLE_NAME))
					myConsole = (MessageConsole) c;
		}
		
		if(myConsole == null)
		{
			myConsole = new MessageConsole(CONSOLE_NAME, null);
			conMan.addConsoles(new IConsole[]{myConsole});
		}
		
		conMan.showConsoleView(myConsole);

		return myConsole.newMessageStream();
	}

	public void getParameters()
	{		
		if(myBuildProject != null)
		{	

			sketchName = myBuildProject.getName();
			strImagePath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString()+"/"+myBuildProject.getName() + "/" + sketchName + ".ino.generic.bin";
			strAddress = "0x0";
			strFlashMode = getPref("build.flash_mode").toLowerCase();
			strFlashFreq  = getPref("build.flash_freq")+"m";
			strFlashSize  = getPref("build.flash_size")+"B";
			strSerialPort = bd.getUploadPort();

			strResetMethod = getPref("upload.resetmethod");
			strUploadSpeed = getPref("upload.speed");

			strAction = "write_flash";

			spiStart = getIntPref("build.spiffs_start");
			spiEnd   = getIntPref("build.spiffs_end");
			spiPage = getIntPref("build.spiffs_pagesize");
			if(spiPage == 0) spiPage = 256;
			spiBlock = getIntPref("build.spiffs_blocksize");
			if(spiBlock == 0) spiBlock = 4096;

			spiSize =  spiEnd - spiStart;

			esptoolCommand  = getEsptoolCmd();
			pythonCommand   = getPythonCommand();
			mkspiffsCommand = getMkSpiffsCmd();
			espotaCommand   = getEspOtaCmd();

		}
	}

	public String[] uploadCommand()
	{
		Vector <String> vCommand = new Vector<>();

		if(esptoolCommand.contains(".py"))
		{
			vCommand.add(pythonCommand);
			vCommand.add(esptoolCommand);
			vCommand.add("-p");
			vCommand.add(strSerialPort);
			vCommand.add(strAction);
			vCommand.add("-fs");
			vCommand.add(strFlashSize);
			vCommand.add("-fm");
			vCommand.add(strFlashMode);
			vCommand.add("-ff");
			vCommand.add(strFlashFreq);
			vCommand.add(strAddress);
			vCommand.add(strImagePath);
		}
		else
		{
			vCommand.add(esptoolCommand);
			vCommand.add("-cp");
			vCommand.add(strSerialPort);
			vCommand.add("-cb");
			vCommand.add(strUploadSpeed);
			vCommand.add(strAction);
			vCommand.add("-bz");
			vCommand.add(strFlashSize);
			vCommand.add("-bm");
			vCommand.add(strFlashMode);
			vCommand.add("-bf");
			vCommand.add(strFlashFreq);
			vCommand.add("-ca");
			vCommand.add(strAddress);
			vCommand.add("-cd");
			vCommand.add(strResetMethod);
			vCommand.add("-cf");
			vCommand.add(strImagePath);
			vCommand.add("-cr");
		}

		return vCommand.toArray(new String[vCommand.size()]);
	}

	public String[] uploadNetCommand()
	{
		Vector <String> vCommand = new Vector<>();	
		vCommand.add(pythonCommand);
		vCommand.add(espotaCommand);
		vCommand.add("-p");
		vCommand.add(strSerialPort);
		vCommand.add("-s");
		vCommand.add("-f");
		vCommand.add(strImagePath);

		return vCommand.toArray(new String[vCommand.size()]);
	}

	public String[] makeSpiffsCommand()
	{
		Vector <String> vCommand = new Vector<>();	
		vCommand.add(mkspiffsCommand);
		vCommand.add("-c");
		vCommand.add(strDataPath);
		vCommand.add("-p");
		vCommand.add(String.valueOf(spiPage));
		vCommand.add("-b");
		vCommand.add(String.valueOf(spiBlock));
		vCommand.add("-s");
		vCommand.add(String.valueOf(spiSize));
		vCommand.add(strImagePath);

		return vCommand.toArray(new String[vCommand.size()]);
	}
	
	public String[] eraseCommand()
	{
		Vector <String> vCommand = new Vector<>();

		if(esptoolCommand.contains(".py"))
		{
			vCommand.add(pythonCommand);
			vCommand.add(esptoolCommand);
			vCommand.add("-p");
			vCommand.add(strSerialPort);
			vCommand.add("erase_flash");
		}
		else
		{
			vCommand.add(esptoolCommand);
			vCommand.add("-cp");
			vCommand.add(strSerialPort);
			vCommand.add("-ce");
		}

		return vCommand.toArray(new String[vCommand.size()]);
	}

	public String[] uploadCommand(String strImagePath)
	{
		this.strImagePath = strImagePath;
		return uploadCommand();
	}

	public String[] uploadNetCommand(String strImagePath)
	{
		this.strImagePath = strImagePath;
		return uploadNetCommand();
	}

	public int listenOnProcess(String[] arguments){
		try {
			Runtime rt = Runtime.getRuntime();
			final Process p = rt.exec(arguments);
			Thread thread = new Thread() {
				public void run() {
					try {
						InputStreamReader reader = new InputStreamReader(p.getInputStream());
						int c;
						while ((c = reader.read()) != -1)
							getConsole().print(""+(char) c);
						reader.close();
						getConsole().println("");

						reader = new InputStreamReader(p.getErrorStream());
						while ((c = reader.read()) != -1)
							getConsole().print(""+(char) c);
						reader.close();
						getConsole().println("");
					} catch (Exception e){}
				}
			};
			thread.start();
			int res = p.waitFor();
			thread.join();
			return res;
		} catch (Exception e){
			return -1;
		}
	}

	public void sysExec(final String[] arguments){
		console.println(new Vector<String>(Arrays.asList(arguments)).toString());

		Job job = new Job("Start build Activator") { //$NON-NLS-1$
			@Override
			protected IStatus run(IProgressMonitor _monitor) {
				try {
					if(listenOnProcess(arguments) != 0)
						console.println("Command Failed!");
					else
						console.println("Command Succedeed!");

				} catch (Exception e) {
					console.println(getExceptionMessage(e));
				}
				return Status.OK_STATUS;
			}
		};
		job.setPriority(Job.DECORATE);
		job.schedule();
	}

	public String getBuildFolderPath() {
		String buildpath = "";

		try {
			buildpath = getSketchPath()+"SpiffsRelease/";
		}
		catch (Exception er) 
		{
			console.println("Folder not found "+er.getMessage());
		}	 

		return buildpath;
	}

	public String getBuildFileName() {
		String buildfile = "";

		try {
			buildfile = sketchName + "_"+String.valueOf(spiSize/1024)+"KB.spiffs.bin";
		}
		catch (Exception er) 
		{
			console.println("File Name not created "+er.getMessage());
		}	 

		return buildfile;
	}

	public String getSketchPath()
	{
		return ResourcesPlugin.getWorkspace().getRoot().getLocation().toString()+"/"+myBuildProject.getName()+"/";
	}

	//	public String getTempBuildFolderPath() {
	//		try {
	//			String buildpath = getBuildFolderPath();
	//			return buildpath;
	//		}
	//		catch (Exception er) {
	//			try {
	//				File buildFolder = null;//FileUtils.createTempFolder("build", DigestUtils.md5Hex(s.getMainFilePath()) + ".tmp");
	//				return buildFolder.getAbsolutePath();
	//			}
	//			catch (Exception e) {
	//				// Arduino 1.6.5 doesn't have FileUtils.createTempFolder
	//				// String buildPath = BaseNoGui.getBuildFolder().getAbsolutePath();
	//				java.lang.reflect.Method method;
	//				try {
	//					method = null;//BaseNoGui.class.getMethod("getBuildFolder");
	//					File f = (File) method.invoke(null);
	//					return f.getAbsolutePath();
	//				} catch (Exception ex) {
	//					ex.printStackTrace();
	//				} 
	//			}
	//		}
	//		return "";
	//	}

	public String getPref(String name)
	{	
		String data = "";
		if(name != null && !name.startsWith("a."))
			name = "a."+name;

		if(name!=null)
			data = Common.getBuildEnvironmentVariable(confDesc, name.toUpperCase(), new String());

		return data;
	}

	public long getIntPref(String name)
	{
		long iRetVal = 0;

		String data = getPref(name);
		if(data != null && !data.contentEquals(""))
		{
			if(data.startsWith("0x")) 
				iRetVal = Long.parseLong(data.substring(2), 16);
			else
				iRetVal  = Integer.parseInt(data);
		}
		return iRetVal;
	}

	public String getPythonCommand()
	{
		String strPythonCmd = "";
		if(isWinOs())
		{
			strPythonCmd = getPref("tools.esptool.pythoncmd");
			if(strPythonCmd.equals(""))
				strPythonCmd = getPref("tools.esptool.network_cmd.windows");
		}
		else
		{
			strPythonCmd = getPref("tools.esptool.pythoncmd");
			if(strPythonCmd.equals(""))
				strPythonCmd = getPref("tools.esptool.network_cmd");
		}

		return strPythonCmd;
	}

	public  String getEsptoolCmd()
	{
		String esptoolPath =  getPref("tools.esptool.path").replaceAll("\"", "")+"/"; 
		String esptoolCmd = "";
		if(isWinOs())
			esptoolCmd = getPref("tools.esptool.cmd.windows");
		else
			esptoolCmd = getPref("tools.esptool.cmd");

		return esptoolPath+esptoolCmd;
	}
	
	public  String getEspOtaCmd()
	{
		String esptoolPath =  getPref("runtime.platform.path")+"/tools/"; 
		String esptoolCmd = "";
		esptoolCmd = getPref("tools.esptool.network_cmd");
		if(!esptoolCmd.endsWith(".py"))
			esptoolCmd = "espota.py";

		return esptoolPath+esptoolCmd;
	}	

	public String getMkSpiffsCmd()
	{
		String mkspiffsCmd;		
		String mkspiffsPath= "";

		mkspiffsPath =  getPref("tools.mkspiffs.path").replace("//","/")+"/";

		if(isWinOs())
			mkspiffsCmd = getPref("tools.mkspiffs.cmd.windows");
		else
			mkspiffsCmd = getPref("tools.mkspiffs.cmd");

		return mkspiffsPath+mkspiffsCmd;
	}

	public String getDataPath(boolean createFolder)
	{
		//load a list of all files
		String strDataPath = "";
		if(myBuildProject != null)
		{
			console.println(myBuildProject.getName());
			String strProjPath = ResourcesPlugin.getWorkspace().getRoot().getLocation().toString()+"/"+myBuildProject.getName()+"/";
			strDataPath = strProjPath+"data/";
			File dataFolder = new File(strProjPath, "data");
			if (!dataFolder.exists() && createFolder) {
				dataFolder.mkdirs();
			}

			if(dataFolder.exists() && dataFolder.isDirectory()){
				File[] files = dataFolder.listFiles();
				if(files.length > 0){
					for(File file : files){
						if((file.isDirectory() || file.isFile()) && !file.getName().startsWith(".")) fileCount++;
					}
				}
			}
		}
		else
		{
			console.println("Project Null");
		}
		return strDataPath;
	}

	public int create()
	{
		if(bd.getArchitecture() == null || !bd.getArchitecture().contentEquals("esp8266")){
			console.println("SPIFFS Not Supported on "+ getPref("core"));
			return -1;
		}

		if(spiStart == 0 && spiEnd == 0){
			console.println("SPIFFS Not Defined for "+bd.getBoardName());
			return -1;
		}

		//Make sure mkspiffs binary exists
		File mkSpiffsTool = new File(mkspiffsCommand);
		if (!mkSpiffsTool.exists() || !mkSpiffsTool.isFile()) {
			console.println("SPIFFS Error: "+mkspiffsCommand+" not found!");
			return -1;
		}

		strAddress = String.valueOf(spiStart);

		//		Object[] options = { "Yes", "No" };
		//		String title = "SPIFFS Create";
		String message = "No files have been found in your data folder!\nempty SPIFFS image will be created.";

		if(fileCount == 0)
		{
			console.println(message);
		}

		console.println("SPIFFS Creating Image..."
				+"\n[SPIFFS] data   : "+strDataPath
				+"\n[SPIFFS] size   : "+(spiSize/1024)
				+"\n[SPIFFS] page   : "+spiPage
				+"\n[SPIFFS] block  : "+spiBlock);

		try {
			if(listenOnProcess(makeSpiffsCommand()) != 0){
				throw new Exception("listenOnProcess return -1");
			}
			myBuildProject.refreshLocal(IResource.DEPTH_ZERO, null);
		} catch (Exception e){
			console.println("SPIFFS Create Failed!\n"+getExceptionMessage(e));
			return -1;
		}
		console.println("[SPIFFS] Image Created succesfully to: "+strImagePath);
		return 1;
	}

	public int upload(){

		Boolean isNetwork = false;

		//make sure the serial port or IP is defined
		if (strSerialPort == null || strSerialPort.isEmpty()) {
			console.println("SPIFFS Error: serial port not defined!");
			return -1;
		}

		String strEspTool="";
		//find espota if IP else find esptool
		if(strSerialPort.split("\\.").length == 4){
			isNetwork = true;
			strEspTool = espotaCommand;
		} else {
			strEspTool = esptoolCommand;
		}

		File esp = new File(strEspTool);
		if(!esp.exists()){
			console.println("SPIFFS Error: "+strEspTool+" not found!");
			return -1;
		}

		console.println("[SPIFFS] Uploading Image: "+strImagePath);

		if(isNetwork){
			console.println("[SPIFFS] IP     : "+strSerialPort);
			sysExec(uploadNetCommand(strImagePath));
		} else {
			console.println( "[SPIFFS] address:"+strAddress
							+"\n[SPIFFS] reset  :"+strResetMethod
							+"\n[SPIFFS] port	:"+strSerialPort
							+"\n[SPIFFS] speed  :"+strUploadSpeed);
			sysExec(uploadCommand(strImagePath));
		}
		return 1;
	}
	
	public int erase(){

		//make sure the serial port or IP is defined
		if (strSerialPort == null || strSerialPort.isEmpty()) {
			console.println("SPIFFS Error: serial port not defined!");
			return -1;
		}

		String strEspTool="";
		//find espota if IP else find esptool
		if(strSerialPort.split("\\.").length == 4){
			return -1;
		} else {
			strEspTool = esptoolCommand;
		}

		File esp = new File(strEspTool);
		if(!esp.exists()){
			console.println("SPIFFS Error: "+strEspTool+" not found!");
			return -1;
		}

		console.println("Erasing Flash");
		sysExec(eraseCommand());
		return 1;
	}

	public static boolean isWinOs()
	{
		return System.getProperty("os.name").toLowerCase().contentEquals("win");
	}

	public static String getExceptionStackString(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);

		e.printStackTrace(pw);

		return sw.toString();
	}

	public static String getExceptionMessage(Exception e) {
		String strRet ="";
		String strMess= e.getMessage();

		if(strMess == null || strMess.isEmpty())
		{
			strMess= getExceptionStackString(e);
			if(strMess != null)
				strRet = strMess.split("\n")[0]+" "+strMess.split("\n")[strMess.split("\n").length-1];
		}
		else
			strRet = strMess;

		return strRet;
	}
}

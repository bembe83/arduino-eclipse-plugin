package io.sloeber.ui.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import io.sloeber.ui.Activator;
import io.sloeber.ui.Messages;
import io.sloeber.ui.helpers.MyPreferences;
import io.sloeber.ui.helpers.Additional_Utils;


public class SpiffsJobHandler extends Job {
	IProject myBuildProject = null;
	ExecutionEvent event 	= null;

	public String sketchName   	 = "";
	public String strImagePath   = "";
	public String strAddress 	 = "";
	public String strFlashMode	 = "";
	public String strFlashFreq   = "";
	public String strFlashSize   = "";
	public String strSerialPort  = "";
	public String strAction      = "write_flash";
	public String strDataPath	 = "";
	public String strResetMethod = "";
	public String strUploadSpeed = "";

	public long spiStart = 0;
	public long spiEnd 	 = 0;
	public long spiPage  = 0;
	public long spiBlock = 0;
	public long spiSize;
	
	Additional_Utils spUtil = null;
	
	public SpiffsJobHandler(String name, ExecutionEvent event) {
		super(name);
		this.event = event;
	}

	public SpiffsJobHandler(IProject buildProject, ExecutionEvent event) {
		super(Messages.BuildHandler_Build_Code_of_project + buildProject.getName());
		this.myBuildProject = buildProject;
		this.event = event;
		this.spUtil = new Additional_Utils(myBuildProject);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) 
	{
		IStatus iRet = Status.OK_STATUS;
		try 
		{
			String action = event.getCommand().getParameter("io.sloeber.core.spiffs.action").getName();
			
			Display.getDefault().asyncExec(new Runnable() {
				@Override
				public void run() {
					Shell theShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
					MessageBox dialog = null;
					String strMessage = "";

					boolean create = action.equals(Messages.spiffs_create) || MyPreferences.getBuildBeforeUploadOption();
					boolean upload = action.equals(Messages.spiffs_upload);
					try {
						dialog = new MessageBox(theShell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);

						if(create)
							strMessage = "Do you want to create the spiffs from\n"+spUtil.strDataPath+"\nto\n"+spUtil.getBuildFileName();

						if(create && upload)
							strMessage = strMessage+"<br/> and upload it?";
						else if (upload)
							strMessage = "Do you want to upload the spiffs from\n"+spUtil.getBuildFileName();

						dialog.setText(action.toUpperCase()+" SPIFFS");
						dialog.setMessage(strMessage);
						int iRetVal = dialog.open();

						if(iRetVal==SWT.OK)
						{
							iRetVal = create?spUtil.create():1;

							if(iRetVal != 1)
							{
								throw new Exception("Build Failed");
							}

							iRetVal = upload?spUtil.upload():1;

							if(iRetVal != 1)
							{
								throw new Exception("Upload Failed");
							}

						}
						else
						{
							dialog = new MessageBox(theShell, SWT.ICON_ERROR | SWT.OK);
							dialog.setText("Build SPIFFS");
							dialog.setMessage("Build Cancelled");
							dialog.open();
						}
						
					}catch(Exception e)
					{
						spUtil.getConsole().print(Additional_Utils.getExceptionStackString(e));
						dialog = new MessageBox(theShell, SWT.ICON_ERROR | SWT.OK);
						dialog.setText("SPIFFS");
						dialog.setMessage(Additional_Utils.getExceptionStackString(e));
						dialog.open();
					}
				}
			});

		}catch(Exception e)
		{
			spUtil.getConsole().println(Additional_Utils.getExceptionStackString(e));
			iRet = Status.CANCEL_STATUS;
		}
		return iRet;
	}
}

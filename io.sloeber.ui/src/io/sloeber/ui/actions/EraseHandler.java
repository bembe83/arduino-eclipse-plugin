package io.sloeber.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
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
import io.sloeber.ui.helpers.Additional_Utils;
import io.sloeber.ui.listeners.ProjectExplorerListener;

/**
 * This id a handler to connect the plugin.xml to the code for building the code
 * This method forces a save all before building
 *
 * @author jan
 *
 */
class EraseJobHandler extends Job {
	IProject myBuildProject = null;
	Additional_Utils spUtil = null;

	public EraseJobHandler(String name) {
		super(name);
	}

	public EraseJobHandler(IProject buildProject) {
		super(Messages.BuildHandler_Build_Code_of_project + buildProject.getName());
		this.myBuildProject = buildProject;
		this.spUtil = new Additional_Utils(myBuildProject);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		Display.getDefault().asyncExec(new Runnable() {
			@Override
			public void run() {
				Shell theShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				MessageBox dialog = null;
				String strMessage = "";

				try {
					dialog = new MessageBox(theShell, SWT.ICON_QUESTION | SWT.OK | SWT.CANCEL);

					strMessage = "Do you want to erase the flash";

					dialog.setText("Erase FLASH");
					dialog.setMessage(strMessage);
					int iRetVal = dialog.open();

					if(iRetVal==SWT.OK)
					{
						iRetVal = spUtil.erase();

						if(iRetVal != 1)
							throw new Exception("Erase Failed");
					}
					else
					{
						dialog = new MessageBox(theShell, SWT.ICON_ERROR | SWT.OK);
						dialog.setText("Erase FLASH");
						dialog.setMessage("Erase Cancelled");
						dialog.open();
					}
					
				}catch(Exception e)
				{
					spUtil.getConsole().print(Additional_Utils.getExceptionStackString(e));
					dialog = new MessageBox(theShell, SWT.ICON_ERROR | SWT.OK);
					dialog.setText("Erase FLASH");
					dialog.setMessage(Additional_Utils.getExceptionStackString(e));
					dialog.open();
				}
			}
		});
		return Status.OK_STATUS;
	}
}

public class EraseHandler extends AbstractHandler {
	private Job mBuildJob = null;

	public Job getJob() {
		return this.mBuildJob;
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IProject SelectedProjects[] = ProjectExplorerListener.getSelectedProjects();
		switch (SelectedProjects.length) {
		case 0:
			Activator.log(new Status(IStatus.ERROR, Activator.getId(), Messages.Handler_No_project_found));
			break;
		default:
			PlatformUI.getWorkbench().saveAllEditors(false);
			for (int curProject = 0; curProject < SelectedProjects.length; curProject++) {
				this.mBuildJob = new EraseJobHandler(SelectedProjects[curProject]);
				this.mBuildJob.setPriority(Job.INTERACTIVE);
				this.mBuildJob.schedule();
			}
		}
		return null;
	}

}

/*
 * Copyright 2002-2005 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package info.jtrac.mylar.ui.wizard;

import info.jtrac.mylar.JtracClient;
import info.jtrac.mylar.domain.JtracVersion;

import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.mylar.tasks.core.RepositoryTemplate;
import org.eclipse.mylar.tasks.core.web.WebClientUtil;
import org.eclipse.mylar.tasks.ui.AbstractRepositoryConnectorUi;
import org.eclipse.mylar.tasks.ui.wizards.AbstractRepositorySettingsPage;
import org.eclipse.swt.widgets.Composite;

public class JtracRepositorySettingsPage extends AbstractRepositorySettingsPage {

	public JtracRepositorySettingsPage(AbstractRepositoryConnectorUi repositoryUi) {
		super("JTrac Connection Settings", "Example: http://myserver/jtrac", repositoryUi);
		setNeedsEncoding(false);
	}

	@Override
	protected void createAdditionalControls(Composite parent) {
		for (RepositoryTemplate template : connector.getTemplates()) {
			serverUrlCombo.add(template.repositoryUrl);
		}				
	}

	@Override
	protected boolean isValidUrl(String name) { 
		if ((name.startsWith(URL_PREFIX_HTTPS) || name.startsWith(URL_PREFIX_HTTP)) && !name.endsWith("/")) {
			try {
				new URL(name);
				return true;
			} catch (MalformedURLException e) {
			}
		}
		return false;
	}

	@Override
	protected void validateSettings() {
		try {
			final String serverUrl = getServerUrl();
			final String username = getUserName();
			final String password = getPassword();
			final Proxy proxy;
			if (getUseDefaultProxy()) {
				proxy = WebClientUtil.getSystemProxy();
			} else {
				proxy = WebClientUtil.getProxy(getProxyHostname(), getProxyPort(), getProxyUsername(),
						getProxyPassword());
			}
			getWizard().getContainer().run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					monitor.beginTask("Connecting...", IProgressMonitor.UNKNOWN);
					try {				
						JtracClient client = new JtracClient(serverUrl, username, password, proxy);
						JtracVersion version = client.getJtracVersion();
					} catch (Exception e) {
						throw new InvocationTargetException(e);
					} finally {
						monitor.done();
					}
				}
			});
			MessageDialog.openInformation(null, "Success", "Repository is valid.");
		} catch (InvocationTargetException e) {
			MessageDialog.openWarning(null, "Error", e.getCause().getMessage());
		} catch (Exception e) {
			MessageDialog.openWarning(null, "Error", e.getMessage());
		}
	}

}

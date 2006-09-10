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

package info.jtrac.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.factory.ArtifactFactory;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ScopeArtifactFilter;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectBuilder;

/**
 * @goal generate
 * @requiresDependencyResolution runtime
 * @description ant build properties file generator
 */
public class AntPropsMojo extends AbstractMojo {

	//======================== MOJO ===============================
    /**
     * @parameter expression="${project}"
     */
    private MavenProject project;

    /**
     * @parameter expression="${localRepository}"
     */
    private ArtifactRepository localRepository;

    /**
     * @component role="org.apache.maven.artifact.resolver.ArtifactResolver"
     */
    private ArtifactResolver artifactResolver;

    /**
     * @parameter expression="${project.remoteArtifactRepositories}"
     */
    private List remoteArtifactRepositories;

    /**
     * @component role="org.apache.maven.artifact.factory.ArtifactFactory"
     */
    private ArtifactFactory artifactFactory;

    /**
     * @component role="org.apache.maven.project.MavenProjectBuilder"
     */
    private MavenProjectBuilder mavenProjectBuilder;
    
    //======================== CONFIG ================================
    
    /**
     * @parameter
     */
    private List testPaths;
    
    /**
     * @parameter
     */    
    private List extraPaths;
    
    //======================== PRIVATE ===============================
    
	private Map buildProperties = new LinkedHashMap();
	private Map extraClassPaths = new LinkedHashMap();
	private Map testClassPaths = new LinkedHashMap();
	private Set runtimeFiles;
	private Set filesets = new HashSet();
    
    //========================== MAIN ================================
    
	public void execute() throws MojoExecutionException {
		if (testPaths == null) {
			testPaths = new ArrayList();
		}
		if (extraPaths == null) {
			extraPaths = new ArrayList();
		}
		String repoBaseDir = localRepository.getBasedir().replace('\\','/');					
		try {
			buildProperties.put("m2.repo", repoBaseDir);
			//========================================================
			Collection runtimeArtifacts = project.getArtifacts();
			runtimeFiles = getRelativePaths(getFiles(runtimeArtifacts), repoBaseDir);
			//========================================================
			Set testArtifacts = project.getDependencyArtifacts();
			testArtifacts.addAll(project.getTestArtifacts());
			Collection testFiles = getFiles(testArtifacts);
			testClassPaths.put("m2.repo", getRelativePaths(testFiles, repoBaseDir));			
			//========================================================
			Properties props = loadProperties();
			for (Iterator i = testPaths.iterator(); i.hasNext(); ) {
				TestPath testPath = (TestPath) i.next();
				String baseDirProperty = testPath.getBaseDirProperty();
				String baseDirPath = props.getProperty(baseDirProperty);
				if (baseDirPath == null) {
					getLog().warn("baseDirProperty + '" + baseDirProperty + "' does not exist in build.properties");
					break;
				}
				File baseDir = new File(baseDirPath);
				if (!baseDir.exists() || !baseDir.isDirectory()) {
					getLog().warn("path + '" + baseDirPath + "' is not valid directory");
					break;
				}
				buildProperties.put(baseDirProperty, baseDirPath);
				Set filePaths = new TreeSet();
				for (Iterator j = testPath.getPaths().iterator(); j.hasNext(); ) {
					String path = (String) j.next();
					File file = new File(baseDirPath + "/" + path);
					if (!file.exists()) {
						getLog().warn("additional test path: '" + file.getPath() + "' does not exist");
						continue;
					}
					if (file.isDirectory()) {
						File[] files = file.listFiles();
						for (int x = 0; x < files.length; x++) {
							filePaths.add(getRelativePath(files[x], baseDir.getPath()));
						}
					} else {
						filePaths.add(getRelativePath(file, baseDir.getPath()));
					}					
				}
				testClassPaths.put(baseDirProperty, filePaths);
			}			
			//========================================================
			for (Iterator i = extraPaths.iterator(); i.hasNext(); ) {
				ExtraPath ep = (ExtraPath) i.next();
				Set paths = new TreeSet();
				Collection files = new ArrayList();
				for (Iterator j = ep.getDependencies().iterator(); j.hasNext(); ) {
					Dependency d = (Dependency) j.next();
					if (d.isResolve()) {
						files.addAll(getFiles(d.getGroupId(), d.getArtifactId(), d.getVersion()));
					} else {
						Artifact a = getArtifact(d.getGroupId(), d.getArtifactId(), d.getVersion());
						files.add(resolveArtifact(a));
					}
				}
				paths.addAll(getRelativePaths(files, repoBaseDir));
				extraClassPaths.put(ep.getName(), paths);
				if (ep.isFileset()) {
					filesets.add(ep.getName());
				}
			}			
			//========================================================
			writeAntPropsFile();
		} catch (Exception e) {
			e.printStackTrace();
			throw new MojoExecutionException(e.getLocalizedMessage());
		}
	}
	
	//========================== HELPER METHODS ======================
	
	/**
	 * instantiate a single artifact using Maven, on the fly
	 */
	private Artifact getArtifact(String groupId, String artifactId, String version) {
	    return artifactFactory.createArtifact(groupId, artifactId, version, "", "jar");      
	}

	/**
	 * resolve dependencies for the given artifact details using Maven, on the fly
	 */
	private Collection resolveDependencies(String groupId, String artifactId, String version) throws Exception {
	    Artifact pomArtifact = getArtifact(groupId, artifactId, version); 
	    MavenProject pomProject = mavenProjectBuilder.buildFromRepository(pomArtifact, remoteArtifactRepositories, localRepository);    
	    Collection artifacts = pomProject.createArtifacts(artifactFactory, Artifact.SCOPE_TEST, new ScopeArtifactFilter(Artifact.SCOPE_TEST));
	    Iterator i = artifacts.iterator();
	    while(i.hasNext()) {
	    	Artifact a = (Artifact) i.next();
	        resolveArtifact(a);
	    }
	    artifacts.add(pomArtifact);
	    return artifacts;      
	}

	/**
	 * resolve single artifact to file, and resolve fully from repository if required
	 */
	private File resolveArtifact(Artifact artifact) throws Exception {
	    File f = artifact.getFile();
	    if (f != null) {
	    	return f;
	    }
	    getLog().info("resolving artifact: " + artifact);
	    artifactResolver.resolve(artifact, remoteArtifactRepositories, localRepository);
	    return artifact.getFile();    
	}	
	
	/**
	 * convert a collection of maven artifacts into a collection of files
	 */
	private Collection getFiles(Collection artifacts) throws Exception {
	    Collection files = new ArrayList();
	    Iterator i = artifacts.iterator();
	    while(i.hasNext()) {
	    	Artifact a = (Artifact) i.next();
	        files.add(resolveArtifact(a));
	    }
	    return files;
	}
	
	/**
	 * convenience combination of resolving and getting a bunch of files
	 */
	private Collection getFiles(String groupId, String artifactId, String version) throws Exception {
	    return getFiles(resolveDependencies(groupId, artifactId, version));
	}	
	
	/**
	 * function to return relative path given base path and the target file
	 */
	private String getRelativePath(File file, String basePath) {
	    String p = basePath.replace('\\','/');
	    int len = p.length() + 1;
	    if (p.endsWith("/")) {
	    	len--;
	    }
	    return file.getPath().substring(len).replace('\\','/');
	}
	
	/**
	 * add path entries for the given bunch of files
	 */
	private Set getRelativePaths(Collection files, String basePath) {
		Set paths = new TreeSet();
		Iterator i = files.iterator();
		while (i.hasNext()) {
			File f = (File) i.next();
			String path = getRelativePath(f, basePath);
			paths.add(path);
		}
		return paths;
	}
	
	/**
	 * load properties from file
	 */
	private Properties loadProperties() throws Exception {
		File file = new File("build.properties");
		Properties props = new Properties();
		if (!file.exists()) {
			getLog().warn("build.properties does not exist");
			return props;
		}
		InputStream is = null;
		try {
			is = new FileInputStream("build.properties");			
			props.load(is);
		} finally {
			is.close();
		}
		return props;
	}	
	
	/**
	 * generate properties file with dependency information, ready to use by ant
	 */
	private void writeAntPropsFile() throws Exception {
		OutputStream os = new FileOutputStream("build-deps.properties");
		Writer out = new PrintWriter(os);
		Date date = new Date();
		out.write("# *** generated by the AntProps Maven2 plugin: " + date + " ***\n\n");
		//==========================================================		
		for (Iterator i = buildProperties.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();
			out.write(entry.getKey() + "=" + entry.getValue() + "\n\n");
		}
		//===========================================================
		out.write("runtime.jars=");	
		for (Iterator i = runtimeFiles.iterator(); i.hasNext(); ) {			
			String path = (String) i.next();
			out.write("\\\n    " + path + ",");
		}
		out.write("\n\n");
		//===========================================================
		out.write("test.jars=");
		for (Iterator i = testClassPaths.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();			
			String key = (String) entry.getKey();
			Set paths = (Set) entry.getValue();
			for (Iterator j = paths.iterator(); j.hasNext(); ) {
				String path = (String) j.next();
				out.write("\\\n    ${" + key + "}/" + path + ":");
			}
		}
		out.write("\n\n");
		//===============================================================
		for (Iterator i = extraClassPaths.entrySet().iterator(); i.hasNext(); ) {
			Map.Entry entry = (Map.Entry) i.next();
			String name = (String) entry.getKey();
			boolean isFileset = filesets.contains(name);
			out.write(name + "=");
			Set paths = (Set) entry.getValue();
			for (Iterator j = paths.iterator(); j.hasNext(); ) {
				String path = (String) j.next();
				if (isFileset) {
					out.write("\\\n    " + path + ",");
				} else {
					out.write("\\\n    ${m2.repo}/" + path + ":");
				}
			}
			out.write("\n\n");
		}
		//===============================================================
		out.close();
		os.close();
	}
	
}

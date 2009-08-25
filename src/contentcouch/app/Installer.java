package contentcouch.app;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import contentcouch.file.FileUtil;

public class Installer {
	public static final String USAGE =
		"java contentcouch.app.Installer {install|uninstall}";
	
	public static final String DIRUSAGE =
		"Error: Could not load {propertiesPath} or {propertiesTemplatePath}\n" +
		"Make sure you are running the installer from the directory containing\n" +
		"{propertiesTemplatePath}.";
	
	String propertiesPath = "ccouch-install.properties";
	String propertiesTemplatePath = null;
	File appDir;

	File getPropertiesFile() {  return new File(propertiesPath);  }
	File getPropertiesTemplateFile() {  return new File(propertiesTemplatePath);  }
	
	protected Properties getProperties(File f) {
		Properties props = new Properties();
		FileReader r = null;
		try {
			r = new FileReader(f);
			props.load(r);
		} catch( IOException e ) {
			throw new RuntimeException( "Error while loading install properties", e );
		} finally {
			try {
				if( r != null ) r.close();
			} catch( IOException e ) {
				// Whatever
			}
		}
		return props;
	}
	
	String getCanonicalOrAbsolutePath( File f ) {
		try {
			return f.getCanonicalPath();
		} catch( IOException e ) {
			return f.getAbsolutePath();
		}
	}
	
	public List generateClassPath(File baseDir) {
		List classPath = new ArrayList();
		File jarDir = new File(baseDir.getPath() + "/ext-lib");
		File[] jarDirFiles = jarDir.listFiles();
		for( int i=0; i<jarDirFiles.length; ++i ) {
			File jarFile = jarDirFiles[i];
			if( !jarFile.getName().startsWith(".") && jarFile.getName().endsWith(".jar") ) {
				classPath.add(getCanonicalOrAbsolutePath(jarFile));
			}
		}
		classPath.add(getCanonicalOrAbsolutePath(new File(baseDir + "/web/WEB-INF/classes")));
		return classPath;
	}
	
	public static String classPathToString( List classPath ) {
		String separator = System.getProperty("path.separator");
		String cp = null;
		for( int i=0; i<classPath.size(); ++i ) {
			if( cp == null ) cp = (String)classPath.get(i);
			else cp += separator + (String)classPath.get(i);
		}
		return cp;
	}
	
	public int preInstall() {
		File ptf = getPropertiesTemplateFile();
		if( !ptf.exists() ) {
			System.err.println(DIRUSAGE
				.replace("{propertiesPath}", propertiesPath)
				.replace("{propertiesTemplatePath}", propertiesTemplatePath)
			);
			return 1;
		}
		
		FileUtil.copy(ptf, getPropertiesFile());
		System.err.println("Copied " + propertiesTemplatePath + " to " + propertiesPath );
		System.err.println("Edit " + propertiesPath + " and then run the installer");
		return 0;
	}

	public void writeBatchRunner( Writer w, String classPath, String repoName, String repoPath ) throws IOException {
		w.write("@java -cp \"" + classPath + "\" contentcouch.app.ContentCouchCommand");
		if( repoPath != null ) {
			w.write(" -repo:" + repoName + " \"" + repoPath + "\"");
		}
		w.write(" %*");
	}
	
	public void writeUnixRunner( Writer w, String classPath, String repoName, String repoPath ) throws IOException {
		w.write("java -cp \"" + classPath + "\" contentcouch.app.ContentCouchCommand");
		if( repoPath != null ) {
			w.write(" -repo:" + repoName + " \"" + repoPath + "\"");
		}
		w.write(" \"$@\"");
	}

	String os = null;
	
	public int install() {
		File pf = getPropertiesFile();
		if( !pf.exists() ) {
			preInstall();
			return 1;
		}
		Properties props = getProperties(pf);
		String style = props.getProperty("script-style");
		String scriptPath = props.getProperty("script-path");
		String repoName = props.getProperty("default-repo-name", "localrepo");
		String repoPath = props.getProperty("default-repo-path", null);
		String classPath = classPathToString(generateClassPath(appDir));
		
		if( scriptPath == null ) {
			System.err.println("No script-path specified in " + pf.getPath());
			return 1;
		}
		
		File scriptFile = new File(scriptPath);
		FileUtil.mkParentDirs(scriptFile);
		try {
			FileWriter w = new FileWriter(scriptFile);
			if( "bat".equals(style) ) {
				writeBatchRunner(w, classPath, repoName, repoPath);
				w.close();
			} else {
				writeUnixRunner( w, classPath, repoName, repoPath);
				w.close();
				scriptFile.setExecutable(true);
			}
		} catch( IOException e ) {
			throw new RuntimeException(e);
		}
		System.err.println("Wrote " + scriptFile.getPath());
		return 0;
	}
	
	public int uninstall() {
		System.err.println("Uninstall not implemented");
		return 1;
	}
	
	public int run(String[] args) {
		String command = null;
		for( int i=0; i<args.length; ++i ) {
			if( "-unix".equals(command) ) {
				os = "unix";
			} else if( "-windows".equals(command) ) {
				os = "windows";
			}
			command = args[i];
		}
		
		appDir = new File("..");

		if( os == null ) {
			String whichOS = System.getProperty("os.name");
			if( whichOS.indexOf("Windows") != -1 ) {
				os = "windows";
			} else {
				os = "unix";
			}
		}

		if( propertiesTemplatePath == null ) {
			if( "windows".equals(os) ) {
				propertiesTemplatePath = "ccouch-install.properties.windows-template";
			} else {
				propertiesTemplatePath = "ccouch-install.properties.unix-template";
			}
		}

		if( "configure".equals(command) ) {
			return preInstall();
		} else if( "install".equals(command) ) {
			return install();
		} else if( "uninstall".equals(command) ) {
			return uninstall();
		} else {
			System.err.println("Unrecognized installer command: " + command);
			System.err.println(USAGE);
			return 1;
		}
	}
	
	public static void main(String[] args) {
		System.exit(new Installer().run(args));
	}
}

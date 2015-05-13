package com.github.barakb.ajp;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Created by Barak Bar Orion
 * 5/13/15.
 */
@SuppressWarnings({"UnusedDeclaration"})
@Mojo(name = "annotations-jar", defaultPhase = LifecyclePhase.PACKAGE, threadSafe = true,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AnnotationsJarPluginMojo extends AbstractMojo {


    @Parameter
    private List<String> packages;

    @Parameter
    String jar = "annotations.jar";

    @Component
    protected MavenProject project;

    private JarWriter jarWriter;

    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
//            getLog().info("-*- maven project is " + project);
//            getLog().info("-*- getPackagesList() returns " + getPackagesList());
            @SuppressWarnings("unchecked") Set<Artifact> artifacts = getProject().getArtifacts();
            getLog().info("\n\n\n");
            URLClassLoader cl = createClassLoader(artifacts);
            File file = new File(jar);
            jarWriter = new JarWriter(file);
            processResources(cl);
            getLog().info("wrote jar: " + file.getAbsolutePath());
        } catch (Exception e) {
            throw new MojoExecutionException(e.toString(), e);
        } finally {
            if (jarWriter != null) {
                try {
                    jarWriter.close();
                } catch (Exception ignored) {
                }
            }
        }

    }

    private URLClassLoader createClassLoader(Set<Artifact> artifacts) {
        List<URL> urls = new ArrayList<URL>(artifacts.size());
        for (Artifact artifact : artifacts) {
            if (artifact.getArtifactHandler().isAddedToClasspath()) {
                File file = artifact.getFile();
                if (file != null) {
//                    getLog().info("File exists " + file.exists() + " : " + file.getAbsolutePath());
                    try {
                        urls.add(file.toURI().toURL());
                    } catch (Exception e) {
                        getLog().error("Failed to find URL for file " + file.getAbsolutePath(), e);
                    }
                }
            }
        }
        return new URLClassLoader(urls.toArray(new URL[urls.size()]));
    }


    public void processResources(URLClassLoader cl) {
        for (URL url : cl.getURLs()) {
            processResources(url, cl);
        }
    }

    private void processResources(final URL url, URLClassLoader cl) {
        final File file;
        try {
            file = new File(url.toURI());
            if (file.isDirectory()) {
                processResourcesInDirectory(file, cl);
            } else {
                processResourcesInJarFile(file, cl);
            }
        } catch (URISyntaxException e) {
            getLog().error("Error while converting url " + url + " to URI", e);
        }
    }

    private void processResourcesInJarFile(final File file, URLClassLoader cl) {
        ZipFile zf;
        try {
            zf = new ZipFile(file);
        } catch (final ZipException e) {
            throw new Error(e);
        } catch (final IOException e) {
            throw new Error(e);
        }
        final Enumeration e = zf.entries();
        while (e.hasMoreElements()) {
            final ZipEntry ze = (ZipEntry) e.nextElement();
            final String fileName = ze.getName();
            if (accept(fileName)) {
                processResource(fileName, cl);
            }
        }
        try {
            zf.close();
        } catch (final IOException e1) {
            throw new Error(e1);
        }
    }

    private void processResource(String fileName, URLClassLoader cl) {
        String className = fileName.replace(".class", "").replaceAll("/", ".");
        try {
            Class cls = cl.loadClass(className);
//            getLog().info("processing " + cls);
            if (cls.getAnnotation(Retention.class) != null) {
//                getLog().info("found  Retention " + cls);
                InputStream resourceAsStream = cl.getResourceAsStream(fileName);
//                getLog().info("resourceAsStream of  " + fileName + " is " + resourceAsStream);
                jarWriter.write(fileName, resourceAsStream);
            }
        } catch (Exception e) {
            getLog().error("Error while loading class " + className + " to URI", e);
        }
    }

    private boolean accept(String fileName) {
        if (fileName.toLowerCase().endsWith(".class")) {
            List<String> packagesList = getPackagesList();
            //noinspection LoopStatementThatDoesntLoop
            for (String p : packagesList) {
                if(fileName.toLowerCase().startsWith(p.toLowerCase().replaceAll("\\.", "/"))){
                    return true;
                }
            }
        }
        return false;
    }

    private void processResourcesInDirectory(final File directory, URLClassLoader cl) {
        final File[] fileList = directory.listFiles();
        if (fileList == null) {
            return;
        }
        for (final File file : fileList) {
            if (file.isDirectory()) {
                processResourcesInDirectory(file, cl);
            } else {
                try {
                    final String fileName = file.getCanonicalPath();
                    if (accept(fileName)) {
                        processResource(fileName, cl);
                    }
                } catch (final IOException e) {
                    throw new Error(e);
                }
            }
        }
    }


    private List<String> getPackagesList() {
        if (this.packages == null) {
            return Collections.emptyList();
        }
        return this.packages;
    }

    public MavenProject getProject() {
        return project;
    }


}

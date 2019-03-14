package com.xie;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import static org.apache.commons.io.FileUtils.getFile;

/**
 * 生成 start.sh stop.sh start.bat
 */
@Mojo(name = "package-Plugin", defaultPhase = LifecyclePhase.PACKAGE)
public class PackageMojo extends AbstractMojo {


  @Component
  private MavenProject project;

  @Parameter(defaultValue = "${project.build.directory}")
  private String buildDir;

  @Parameter(defaultValue = "${project.artifactId}")
  private String projectName;

  @Parameter(defaultValue = "${project.version}")
  private String projectVersion;

  @Parameter(property = "mainClass", required = true)
  private String mainClass;

  @Parameter(property = "jvmXms", required = false,defaultValue = "125m")
  String jvmXms;
  @Parameter(property = "jvmXmx", required = false, defaultValue = "512m")
  String jvmXmx;

  @Parameter(property = "releaseDir",required = true)
  private String releaseDir;

  private String del = "-";

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

    try {
      String realReleaseDir;
      if (!releaseDir.equals("")) {
        realReleaseDir = releaseDir;
      } else {
        realReleaseDir = buildDir + File.separator + projectName + del + projectVersion;
      }

      System.out.println("=========================================");
      System.out.println("It is parsing pom copyDependencies...");
      System.out.println("    progress name: " + projectName);
      System.out.println("    program version: " + projectVersion);
      System.out.println("    script file release path: " + realReleaseDir);
      produceScript(realReleaseDir);
      System.out.println("produce script file success...");
      System.out.println("=========================================");
    } catch (Exception e) {
      System.out.println("Exception: " + e.getMessage());
    }
  }

  /**
   * 不能获取子依赖
   * @param dest
   * @throws Exception
   */
  @Deprecated
  public void copyDependencies(String dest) throws Exception {
    Set<Artifact> dependencyArtifacts = project.getDependencyArtifacts();

    File depenJar = null;
    for (Artifact artifact : dependencyArtifacts) {
      if (artifact.getScope().equals(Artifact.SCOPE_TEST)
              || artifact.getScope().equals(Artifact.SCOPE_PROVIDED)) {
        continue;
      }
      File sourceFile = artifact.getFile();
      System.out.println(sourceFile);
      System.out.println(artifact.getDependencyTrail());
      depenJar = new File(dest + File.separator + artifact.getFile().getName());
      if (depenJar.exists()) {
        depenJar.delete();
      }
      try(InputStream inputStream = new FileInputStream(sourceFile);
          OutputStream outputStream = new FileOutputStream(depenJar);){
        IOUtils.copy(inputStream,outputStream);
      }
    }

  }


  public void produceScript(String realReleaseDir) throws IOException {
    String linuxStartName = "start.sh";
    String winStartName = "start.bat";
    String linuxStopName = "stop.sh";
    String projectName = this.projectName + del + projectVersion;

    String linuxStartFilePath = realReleaseDir + File.separator + linuxStartName;
    String winStartFilePath = realReleaseDir + File.separator + winStartName;
    String linuxStopFilePath = realReleaseDir + File.separator + linuxStopName;

    File linuxStartFile = new File(linuxStartFilePath);
    String content = scriptLinuxStartupCommand(projectName, mainClass, jvmXms, jvmXmx);
    toDisk(linuxStartFile, content.getBytes());

    File winStartFile = new File(winStartFilePath);
    content = scriptWinStartupCommand(projectName, mainClass);
    toDisk(winStartFile, content.getBytes());

    File linuxStopFile = new File(linuxStopFilePath);
    content = scriptLinuxStopCommand(projectName);
    toDisk(linuxStopFile, content.getBytes());

  }

  private static String scriptLinuxStopCommand(String projectName) {
    StringBuilder sb = new StringBuilder();
    sb.append("\n");
    sb.append("PID=`ps -ef|grep 'D" + projectName + "'|grep -v grep|awk '{print $2}'`");
    sb.append("\n");
    sb.append("if [ -z $PID ];then");
    sb.append("\n");
    sb.append("    echo \"The program " + projectName + " has been stopped.\"");
    sb.append("\n");
    sb.append("else");
    sb.append("\n");
    sb.append("    kill $PID");
    sb.append("\n");
    sb.append("    echo \"The program " + projectName + " is being killed, please wait for 3 seconds...\"");
    sb.append("\n");
    sb.append("    sleep 3");
    sb.append("\n");
    sb.append("    PID=`ps -ef|grep 'D" + projectName + "'|grep -v grep|awk '{print $2}'`");
    sb.append("\n");
    sb.append("    if [ $PID ];then");
    sb.append("\n");
    sb.append("        kill -9 $PID");
    sb.append("\n");
    sb.append("    fi");
    sb.append("\n");
    sb.append("    echo \"Kill the program " + projectName + " successfully.\"");
    sb.append("\n");
    sb.append("fi");
    return sb.toString();
  }

  public static String scriptLinuxStartupCommand(String projectName, String mainClass, String jvmXms, String jvmXmx) {
    StringBuilder sb = new StringBuilder();
    sb.append("export lib0=./lib");
    sb.append("\n");
    sb.append("PID=`ps -ef|grep 'D" + projectName + " '|grep -v grep|awk '{print $2}'`");
    sb.append("\n");
    sb.append("if [ -z $PID ];then");
    sb.append("\n");
    sb.append("java -D" + projectName + " ");
    sb.append("-Xms"+jvmXms);
    sb.append(" -Xmx"+jvmXmx);
    sb.append("   -classpath ");
    sb.append(".:$lib0/*");
    sb.append(" " + mainClass);
    sb.append(" >/dev/null 2>err.log &");
    sb.append("\n");
    sb.append("else");
    sb.append("\n");
    sb.append("    echo \"The program " + projectName + " has been running.Please stop it firstly.\"");
    sb.append("\n");
    sb.append("fi");
    return sb.toString();
  }

  public static String scriptWinStartupCommand(String projectName, String mainClass) {
    StringBuilder sb = new StringBuilder();
    sb.append("@echo off");
    sb.append("\n");
    sb.append("title " + projectName);
    sb.append("\n");
    sb.append("set lib0=.\\lib");
    sb.append("\n");
    sb.append("java -D" + projectName + " -Xms64m -Xmx512m   -cp %lib0%" + File.separator +"*");
    sb.append(" " + mainClass);
    sb.append(" 2>err.log");
    sb.append("\n");
    sb.append("pause");
    return sb.toString();
  }


  public static void toDisk(File file, byte[] msg) throws IOException {
    try (OutputStream os = new FileOutputStream(file);) {
      os.write(msg);
      os.flush();
    }
  }


  public static void main(String[] args) throws IOException {
    PackageMojo mojo = new PackageMojo();
    mojo.projectName = "hahaha";
    mojo.projectVersion = "0.1";
    mojo.mainClass = "com.main";
    mojo.produceScript("D:\\Help");

    //System.out.println(scriptLinuxStopCommand("ddd"));
  }

}

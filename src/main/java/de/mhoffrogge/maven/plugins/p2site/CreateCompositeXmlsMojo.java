package de.mhoffrogge.maven.plugins.p2site;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This goal creates composite[Content|Artifacts].xml files for the upper directories of a P2 update site repo
 * directories.
 *
 * @author mhoffrog
 */
@Mojo(name = "create-composite-xmls", requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
public class CreateCompositeXmlsMojo extends AbstractP2SiteMojo {

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    executeInternal(true, false);
  }

}

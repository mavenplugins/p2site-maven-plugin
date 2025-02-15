package de.mhoffrogge.maven.plugins.p2site;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * This goal creates template based index.html and optionally composite[Content|Artifacts].xml files for P2 update
 * sites.
 *
 * @author mhoffrog
 */
@Mojo(name = "build-index-html", requiresProject = false, requiresDependencyResolution = ResolutionScope.COMPILE, defaultPhase = LifecyclePhase.PACKAGE)
public class BuildIndexHtmlMojo extends AbstractP2SiteMojo {

  /**
   * Create composite xml files in the parent directories of the underlying P2 update site directories.
   */
  @Parameter(defaultValue = "false", property = "p2site.createCompositeXmls", required = true)
  private boolean createCompositeXmls;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    executeInternal(this.createCompositeXmls, true);
  }

}

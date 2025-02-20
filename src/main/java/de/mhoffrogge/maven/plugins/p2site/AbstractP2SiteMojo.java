package de.mhoffrogge.maven.plugins.p2site;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import de.mhoffrogge.maven.plugins.base.AbstractBaseMojo;

/**
 * This abstract plugin creates template based index.html and composite[Content|Artifacts].xml files for P2 update
 * sites.
 *
 * @author mhoffrog
 */
abstract class AbstractP2SiteMojo extends AbstractBaseMojo {

  /**
   * The base directory to start processing.
   */
  @Parameter(defaultValue = "${basedir}", property = "p2site.baseDir", required = true)
  private String baseDir;

  /**
   * Optional: The subdirectory relative to basedir for the update sites of snapshot versions. If set, and project
   * version is a snapshot version, then the effective basedir will be <code>basedir>/relativeSnapshotsDir</code>
   */
  @Parameter(required = false)
  private String relativeSnapshotsDir;

  /**
   * Optional: The subdirectory relative to basedir for the update sites of release versions. If set, and project
   * version is a release version, then the effective basedir will be <code>basedir/relativeReleasesDir</code>
   */
  @Parameter(required = false)
  private String relativeReleasesDir;

  /**
   * The file name to read the updateSite properties from.
   */
  @Parameter(defaultValue = "updateSite.properties", required = true)
  private String updateSitePropertiesFileName;

  /**
   * If true, the updateSite properties file will be deleted after processing of that files directory level.
   */
  @Parameter(defaultValue = "false", required = true)
  private boolean removeUpdateSitePropertiesFile;

  /**
   * The property name holding the update site name to be shown by the index.html. It must match the placeholder used
   * by the index.html template.
   */
  @Parameter(defaultValue = "update.site.name", required = true)
  private String updateSiteNamePropertyName;

  /**
   * The property name holding the update site repo folder name to be shown by the index.html. It must match the
   * placeholder used by the index.html template.
   */
  @Parameter(defaultValue = "update.site.dirname", required = true)
  private String updateSiteDirNamePropertyName;

  /**
   * The property name holding the update site description to be shown by the index.html. It must match the
   * placeholder used by the index.html template.
   */
  @Parameter(defaultValue = "update.site.description", required = true)
  private String updateSiteDescriptionPropertyName;

  /**
   * The property name holding the update site version to be shown by the index.html. It must match the placeholder
   * used by the index.html template.
   */
  @Parameter(defaultValue = "update.site.version", required = true)
  private String updateSiteVersionPropertyName;

  /**
   * The property name being replaced for the file listing shown by the index.html. It must match the placeholder used
   * by the index.html template.
   */
  @Parameter(defaultValue = "update.site.contents", required = true)
  private String updateSiteContentsPropertyName;

  /**
   * The property name being used as base property name for counted properties. It will get appended ".<1-N>" to list
   * additional forward links in the composite xmls. The counting will stop on the first property value not existing.
   */
  @Parameter(defaultValue = "update.site.forward", required = true)
  private String updateSiteForwardsBasePropertyName;

  /**
   * If true and no updateSite.properties file was found in the baseDir, then abort with an error.
   */
  @Parameter(defaultValue = "false", required = true)
  private boolean isErrorIfNoUpdateSitePropertiesInBaseDir;

  /**
   * If true, then composite xml files will be created for directories only that contain an updateSite.properties
   * file.
   */
  @Parameter(defaultValue = "false", property = "p2site.compositeXmlsForFoldersWithUpdateSitePropertiesOnly", required = true)
  private boolean compositeXmlsForFoldersWithUpdateSitePropertiesOnly;

  /**
   * Optional: The template file name for an update site URLs index.html.
   */
  @Parameter(required = false)
  private String indexUpdateSiteTemplateFileName;

  /**
   * Optional: The template file name for a non update site URLs index.html (e.g.: a composite folder) .
   */
  @Parameter(required = false)
  private String indexNonUpdateSiteTemplateFileName;

  /**
   * Optional: Filename for a file enforced to be copied over <code>baseDir/updateSitePropertiesFileName</code>.
   */
  @Parameter(required = false, property = "p2site.enforceBaseDirUpdateSitePropertiesFile")
  private String enforceBaseDirUpdateSitePropertiesFile;

  /**
   * If true, index.html and composite files will be created in <code>dryRunFolder</code>.
   */
  @Parameter(defaultValue = "false", property = "p2site.dryRun", required = false)
  private boolean dryRun;

  /**
   * The folder to create index.html and composite files in if <code>dryRun=true</code>.
   */
  @Parameter(defaultValue = "${project.build.directory}/p2site_dryrun", required = true)
  private String dryRunFolder;

  /**
   * Optional: Fixed <code>p2.timesStamp</code> to be used in composite files.
   */
  @Parameter(required = false, property = "p2site.timeStamp")
  private String timeStamp;

  private String indexUpdateSiteTemplate;

  private String indexNonUpdateSiteTemplate;

  private boolean isIndexHtmlCreated = false;

  private boolean isToCreateCompositeXmls = true;

  private boolean isToCreateIndexHtml = false;

  private Stack<Pair<String, List<String>>> folderStack = new Stack<Pair<String, List<String>>>();

  private final List<File> filesToBeDeletedAfterRun = new ArrayList<File>();

  private final List<Pair<String, String>> propsToBeReplacedInTemplates = new ArrayList<Pair<String, String>>();

  private static final String INDEX_HTML_FILE_NAME = "index.html";

  private static final String FOLDER_NAME_IS_VERSION_PATTERN = "^[0-9]+\\.[0-9]+.*";

  private static final String COMPOSITE_ARTIFACTS_BASENAME = "compositeArtifacts";

  private static final String COMPOSITE_CONTENT_BASENAME = "compositeContent";

  private static final String COMPOSITE_CONTENT_EXTENSION = ".xml";

  private static final String P2INDEX_FILENAME = "p2.index";

  private static final String COMPOSITE_ARTIFACTS_FILENAME = COMPOSITE_ARTIFACTS_BASENAME + COMPOSITE_CONTENT_EXTENSION;

  private static final String COMPOSITE_CONTENT_FILENAME = COMPOSITE_CONTENT_BASENAME + COMPOSITE_CONTENT_EXTENSION;

  private static final String DEFAULT_UPDATE_SITE_DESCRIPTION = "Eclipse P2 Plugins";

  public static final String UPDATESITE_NO_COMPOSITE_FILES_PROP_NAME = "update.site.no_composite_files";

  protected void executeInternal(final boolean isToCreateCompositeXmls, final boolean isToCreateIndexHtml)
      throws MojoExecutionException, MojoFailureException {
    // Init prop values to be replaced in templates
    // Pair.of(<propName>, <defaultValue or null>)
    this.propsToBeReplacedInTemplates
        .add(Pair.of(this.updateSiteDescriptionPropertyName, DEFAULT_UPDATE_SITE_DESCRIPTION));
    this.propsToBeReplacedInTemplates.add(Pair.of(this.updateSiteVersionPropertyName, null));
    this.propsToBeReplacedInTemplates.add(Pair.of("update.site.title", null));
    this.propsToBeReplacedInTemplates.add(Pair.of("update.site.h2", null));
    this.propsToBeReplacedInTemplates.add(Pair.of("update.site.buildinfo", null));
    try {
      this.timeStamp = StringUtils.defaultIfBlank(this.timeStamp, Long.toString(new Date().getTime()));
      this.isToCreateCompositeXmls = isToCreateCompositeXmls;
      this.isToCreateIndexHtml = isToCreateIndexHtml;
      this.indexUpdateSiteTemplate = getIndexTemplate(this.indexUpdateSiteTemplateFileName,
          "updateSiteIndex_Template.html");
      this.indexNonUpdateSiteTemplate = getIndexTemplate(this.indexNonUpdateSiteTemplateFileName,
          "nonUpdateSiteIndex_Template.html");
      enforceBaseDirUpdateSitePropertiesFileIfConfigured();
      getLog().info("Walking directories for " + this.updateSitePropertiesFileName + " files ...");
      walkAllFiles(getEffectiveBaseDir(), 0);
      if (this.isToCreateIndexHtml && !this.isIndexHtmlCreated) {
        getLog().warn("NO index.html files did have been created.");
      }
    } finally {
      for (final File fileToBeDeleted : this.filesToBeDeletedAfterRun) {
        getLog().info("CLEANUP " + fileToBeDeleted.getAbsolutePath());
        FileUtils.deleteQuietly(fileToBeDeleted);
      }
    }
  }

  /**
   * @return File object for the effective updateSite baseDir
   */
  protected File getEffectiveBaseDir() {
    final boolean isProjectSnapshotVersion = isProjectSnapshotVersion();
    if (isProjectSnapshotVersion && StringUtils.isNotBlank(this.relativeSnapshotsDir)) {
      return new File(this.baseDir, this.relativeSnapshotsDir);
    } else if (!isProjectSnapshotVersion && StringUtils.isNotBlank(this.relativeReleasesDir)) {
      return new File(this.baseDir, this.relativeReleasesDir);
    } else {
      return new File(this.baseDir);
    }
  }

  /**
   * @throws MojoFailureException in case of any functional issue
   */
  protected void enforceBaseDirUpdateSitePropertiesFileIfConfigured() throws MojoFailureException {
    if (StringUtils.isBlank(this.enforceBaseDirUpdateSitePropertiesFile)) {
      return;
    }
    final File sourceFile = new File(this.enforceBaseDirUpdateSitePropertiesFile);
    if (!sourceFile.exists()) {
      throw new MojoFailureException("File does not exist: " + sourceFile.getAbsolutePath());
    }
    if (!sourceFile.isFile()) {
      throw new MojoFailureException("This path is not of type file: " + sourceFile.getAbsolutePath());
    }
    final File targetFile = new File(getEffectiveBaseDir(), this.updateSitePropertiesFileName);
    if (this.dryRun) {
      if (targetFile.exists()) {
        getLog()
            .warn("Existing file " + targetFile.getAbsolutePath() + " did not have been overridden since of DRY RUN!");
        return;
      } else {
        this.filesToBeDeletedAfterRun.add(targetFile);
      }
    }
    try {
      FileUtils.copyFile(sourceFile, targetFile);
    } catch (IOException e) {
      throw new MojoFailureException(
          "Failed to copy file: " + sourceFile.getAbsolutePath() + " to " + targetFile.getAbsolutePath(), e);
    }
    getLog().info("Enforced initial updateSite properties: " + sourceFile.getAbsolutePath() + " -> "
        + targetFile.getAbsolutePath());
  }

  protected String getIndexTemplate(final String indexTemplateFileName, final String defaultResourceName)
      throws MojoFailureException {
    InputStream indexTemplateStream = null;
    try {
      if (StringUtils.isNotBlank(indexTemplateFileName)) {
        final File indexTemplateFile = new File(indexTemplateFileName);
        if (!indexTemplateFile.exists()) {
          throw new MojoFailureException("Index template file does not exist: " + indexTemplateFile.getAbsolutePath());
        }
        if (!indexTemplateFile.isFile()) {
          throw new MojoFailureException("Index template file is not a file: " + indexTemplateFile.getAbsolutePath());
        }
        try {
          indexTemplateStream = new FileInputStream(indexTemplateFile);
        } catch (FileNotFoundException e) {
          throw new MojoFailureException(
              "Index template file does not exist or is not readable: " + indexTemplateFile.getAbsolutePath());
        }
      } else {
        indexTemplateStream = getClass().getResourceAsStream(defaultResourceName);
        if (indexTemplateStream == null) {
          throw new MojoFailureException(defaultResourceName + " does not exist in this plugins JAR package.");
        }
      }
      try {
        return readTextStreamAndNormalizeEOL(indexTemplateStream);
      } catch (IOException e) {
        throw new MojoFailureException("Failed to read the index template file.", e);
      }
    } finally {
      if (indexTemplateStream != null) {
        try {
          indexTemplateStream.close();
        } catch (IOException e) {
          // nothing to do
        }
      }
    }
  }

  protected void walkAllFiles(final File dir, final int depth) throws MojoFailureException {
    if (!dir.exists()) {
      throw new MojoFailureException("Directory does not exist: " + dir.getAbsolutePath());
    }
    if (!dir.isDirectory()) {
      throw new MojoFailureException("This file is not a directory: " + dir.getAbsolutePath());
    }
    final File[] listOfFiles = dir.listFiles();
    if (listOfFiles == null) {
      return;
    }
    addFolderToFolderStackIfNeeded(depth, dir.getName());
    // 1. look for updateSiteIndexPropertiesFile
    for (File file : listOfFiles) {
      if (file.isFile() && StringUtils.equalsIgnoreCase(this.updateSitePropertiesFileName, file.getName())) {
        // we have found it - so we are done for this directory
        getLog().info("Found properties " + file.getAbsolutePath());
        Properties props = new Properties();
        try (FileInputStream finput = new FileInputStream(file)) {
          props.load(finput);
          resolveVariablesInPropValues(props, file.getAbsolutePath());
        } catch (IOException e) {
          throw new MojoFailureException("Failed to read file: " + file.getAbsolutePath(), e);
        }
        createCompositeFilesAndIndexHtmlAsRequired(dir, props, depth, this.isToCreateCompositeXmls);
        if (this.removeUpdateSitePropertiesFile) {
          try {
            Files.delete(Paths.get(file.toURI()));
            getLog().info("REMOVED " + file.getAbsolutePath());
          } catch (IOException e) {
            throw new MojoFailureException("Failed to remove file: " + file.getAbsolutePath(), e);
          }
        }
        return;
      }
    }
    // 2. If we reach out here on depth==0, then check for error
    if (depth == 0 && this.isErrorIfNoUpdateSitePropertiesInBaseDir) {
      throw new MojoFailureException(
          "File " + this.updateSitePropertiesFileName + " is missing in " + dir.getAbsolutePath());
    }
    // 3. look for other directories otherwise
    for (File file : listOfFiles) {
      if (file.isDirectory()) {
        walkAllFiles(file, depth + 1);
      }
    }
    removeFolderFromFolderStackAndCreateCompositeXmlIfNeeded(depth, dir, "", null, this.isToCreateCompositeXmls);
  }

  /**
   * @param props        props the properties to scan for variable place holders
   * @param propFileName the filename the properties did have been read from
   */
  protected void resolveVariablesInPropValues(Properties props, final String propFileName) {
    int remainingVarCount = countPropVars(props);
    for (int iPass = 1; remainingVarCount > 0 && iPass <= 2; iPass++) {
      getLog().debug("remainingVarCount - pass#" + iPass + ": " + remainingVarCount);
      final Set<Object> propsKeySet = props.keySet();
      for (Object propKey : propsKeySet) {
        final String sPropKey = (String) propKey;
        final String sPropValue = props.getProperty(sPropKey);
        for (Object innerPropKey : propsKeySet) {
          final String sInnerPropKey = (String) innerPropKey;
          if (StringUtils.equals(sPropKey, sInnerPropKey)) {
            continue;
          }
          final String sInnerPropValue = props.getProperty(sInnerPropKey);
          final String sInnerPropUdateValue = StringUtils.replace(sInnerPropValue, "${" + sPropKey + "}", sPropValue);
          if (!StringUtils.equals(sInnerPropValue, sInnerPropUdateValue)) {
            props.setProperty(sInnerPropKey, sInnerPropUdateValue);
            getLog()
                .debug("property variable replace - pass#" + iPass + ": ${" + sPropKey + "} -> \"" + sPropValue + "\"");
          }
        }
      }
      remainingVarCount = countPropVars(props);
      if (iPass == 2 && remainingVarCount > 0) {
        getLog().warn("There are unresolvable or cyclic variables in " + propFileName);
      }
    }
  }

  /**
   * @param props the properties to scan for variable place holders
   * @return count of variables within prop values
   */
  protected int countPropVars(Properties props) {
    int ret = 0;
    for (final Entry<Object, Object> entry : props.entrySet()) {
      final String sPropValue = (String) entry.getValue();
      ret += StringUtils.countMatches(sPropValue, "${");
    }
    return ret;
  }

  protected void addFolderToFolderStackIfNeeded(int depth, String folderName) {
    if (depth >= this.folderStack.size()) {
      this.folderStack.push(Pair.of(folderName, new ArrayList<String>()));
    }
  }

  /**
   * @param depth                   the folder walking iteration depth
   * @param dir                     the current dir to create files in
   * @param repoName                the P2 repo name
   * @param props                   properties from updateSite.properties
   * @param isToCreateCompositeXmls true if composite XMLs are to be created
   * @return Pair.of(isCompositeXmlsCreated, isP2IndexCreated)
   * @throws MojoFailureException in case of any failure
   */
  protected Pair<Boolean, Boolean> removeFolderFromFolderStackAndCreateCompositeXmlIfNeeded(final int depth,
      final File dir, final String repoName, final Properties props, final boolean isToCreateCompositeXmls)
      throws MojoFailureException {
    Pair<Boolean, Boolean> ret = Pair.of(false, false);
    if (this.folderStack.size() > depth) {
      final List<String> listOfChilds = this.folderStack.pop().getRight();
      addProbableForwardLinks(listOfChilds, props);
      if (isToCreateCompositeXmls && !Boolean.valueOf(props.getProperty(UPDATESITE_NO_COMPOSITE_FILES_PROP_NAME))
          && !listOfChilds.isEmpty()) {
        final boolean isP2IndexCreated = createCompositeXmls(listOfChilds, dir, repoName);
        ret = Pair.of(true, isP2IndexCreated);
      }
    }
    return ret;
  }

  /**
   * @param listOfChilds list of child folder names
   * @param props        P2 site properties
   */
  protected void addProbableForwardLinks(final List<String> listOfChilds, final Properties props) {
    if (props == null) {
      return;
    }
    for (int i = 1;; i++) {
      final String forwardLink = props.getProperty(this.updateSiteForwardsBasePropertyName + "." + i);
      if (StringUtils.isBlank(forwardLink)) {
        break;
      }
      listOfChilds.add(forwardLink);
    }
  }

  protected boolean createCompositeXmls(List<String> listOfChilds, final File dir, final String repoName)
      throws MojoFailureException {
    getLog().info(" Creating compositeXmls repoName=" + repoName + " with " + listOfChilds.size() + " childs for "
        + dir.getAbsolutePath());
    createCompositeXml(COMPOSITE_ARTIFACTS_BASENAME, listOfChilds, dir, repoName);
    createCompositeXml(COMPOSITE_CONTENT_BASENAME, listOfChilds, dir, repoName);
    return createCompositeP2IndexIfNeeded(dir);
  }

  protected void createCompositeXml(final String compositeBaseName, final List<String> listOfChilds, final File dir,
      final String repoName) throws MojoFailureException {
    final String resourceName = compositeBaseName + "_Template" + COMPOSITE_CONTENT_EXTENSION;
    final InputStream templateStream = getClass().getResourceAsStream(resourceName);
    if (templateStream == null) {
      throw new MojoFailureException(resourceName + " does not exist in this plugins JAR package.");
    }
    String fileContent;
    try {
      fileContent = readTextStreamAndNormalizeEOL(templateStream);
    } catch (IOException e) {
      throw new MojoFailureException("Failed to read the template file " + resourceName + ".", e);
    }
    fileContent = replaceFileContent(fileContent, repoName, "repoName");
    fileContent = replaceFileContent(fileContent, this.timeStamp, "timeStamp");
    fileContent = replaceFileContent(fileContent, Integer.toString(listOfChilds.size()), "childSize");
    final StringBuilder sbFiles = new StringBuilder();
    for (String childLocation : listOfChilds) {
      if (sbFiles.length() > 0) {
        sbFiles.append(System.lineSeparator());
      }
      sbFiles.append(buildCompositeLocation(childLocation));
    }
    fileContent = replaceFileContent(fileContent, sbFiles.toString(), "childLocations");
    final File compositeFile = new File(dir, compositeBaseName + COMPOSITE_CONTENT_EXTENSION);
    writeTargetFileConsideringDryRun(compositeFile, fileContent);
  }

  /**
   * @param templateStream the input stream to read from
   * @return System EOL normalized text file content
   * @throws IOException in case of a file read failure
   */
  protected String readTextStreamAndNormalizeEOL(InputStream templateStream) throws IOException {
    String ret = IOUtils.toString(templateStream, Charset.defaultCharset());
    ret = StringUtils.replace(ret, StringUtils.CR + StringUtils.LF, StringUtils.LF);
    return StringUtils.replace(ret, StringUtils.LF, System.lineSeparator());
  }

  protected boolean createCompositeP2IndexIfNeeded(File dir) throws MojoFailureException {
    if (new File(dir, P2INDEX_FILENAME).exists()) {
      return false;
    }
    final String resourceName = "composite_Template_" + P2INDEX_FILENAME;
    final InputStream templateStream = getClass().getResourceAsStream(resourceName);
    if (templateStream == null) {
      throw new MojoFailureException(resourceName + " does not exist in this plugins JAR package.");
    }
    final String fileContent;
    try {
      fileContent = readTextStreamAndNormalizeEOL(templateStream);
    } catch (IOException e) {
      throw new MojoFailureException("Failed to read the template file " + resourceName + ".", e);
    }
    final File compositeP2IndexFile = new File(dir, P2INDEX_FILENAME);
    writeTargetFileConsideringDryRun(compositeP2IndexFile, fileContent);
    return true;
  }

  /**
   * @param originalTargetFile the target file of a link
   * @param fileContent        the target file content
   * @throws MojoFailureException in any case of a functional issue
   */
  protected void writeTargetFileConsideringDryRun(final File originalTargetFile, final String fileContent)
      throws MojoFailureException {
    final File effectiveTargetFile = this.dryRun
        ? new File(StringUtils.replaceOnce(originalTargetFile.getAbsolutePath(),
            getEffectiveBaseDir().getAbsolutePath(), new File(this.dryRunFolder).getAbsolutePath()))
        : originalTargetFile;
    try {
      FileUtils.write(effectiveTargetFile, fileContent, Charset.defaultCharset());
      getLog().info("  " + (this.dryRun ? "DRYRUN - " : "") + "Created " + effectiveTargetFile.getAbsolutePath());
    } catch (IOException e) {
      throw new MojoFailureException("Failed to write file: " + effectiveTargetFile.getAbsolutePath(), e);
    }
  }

  protected void addToCompositeChilds(final int depth, final String folderName) {
    String childFolderName = folderName;
    for (int i = depth - 1; i >= 0; i--) {
      this.folderStack.elementAt(i).getRight().add(childFolderName);
      getLog().debug(" Folder: " + this.folderStack.elementAt(i).getLeft() + " -  added compoSiteChild[" + i + "]="
          + childFolderName);
      childFolderName = this.folderStack.elementAt(i).getLeft() + "/" + childFolderName;
    }
  }

  protected void createCompositeFilesAndIndexHtmlAsRequired(final File dir, Properties props, final int depth,
      final boolean isToCreateCompositeXmls) throws MojoFailureException {
    addFolderToFolderStackIfNeeded(depth, dir.getName());
    // "clone" properties to preserve values from upper level
    props = new Properties(props);
    // Set version property, if not set and if this folder name matches a version
    // number pattern
    if (StringUtils.isBlank(props.getProperty(this.updateSiteVersionPropertyName))
        && dir.getName().matches(FOLDER_NAME_IS_VERSION_PATTERN)) {
      props.setProperty(this.updateSiteVersionPropertyName, dir.getName());
    }
    boolean isUpdateSite;
    final StringBuilder sbFolderContent;
    {
      final Pair<Boolean, StringBuilder> pairUpdateSiteContent = getUpdateSiteContentFragment(dir, props, depth);
      isUpdateSite = pairUpdateSiteContent.getLeft();
      sbFolderContent = pairUpdateSiteContent.getRight();
    }
    final String repoName = props.getProperty(this.updateSiteNamePropertyName);
    final String dirName = dir.getName();
    final Pair<Boolean, Boolean> pairIsCompositeXmlCreatedIsP2IndexCreated = removeFolderFromFolderStackAndCreateCompositeXmlIfNeeded(
        depth, dir, repoName, props, isToCreateCompositeXmls);
    boolean isCompositeXmlCreated = pairIsCompositeXmlCreatedIsP2IndexCreated.getLeft();
    boolean isP2IndexCreated = pairIsCompositeXmlCreatedIsP2IndexCreated.getRight();
    isUpdateSite |= isP2IndexCreated || isCompositeXmlCreated;
    if (isP2IndexCreated) {
      sbFolderContent.append(System.lineSeparator()).append(buildFileLink(new File(dir, P2INDEX_FILENAME)));
    }
    if (isCompositeXmlCreated) {
      sbFolderContent.append(System.lineSeparator()).append(buildFileLink(new File(dir, COMPOSITE_ARTIFACTS_FILENAME)));
      sbFolderContent.append(System.lineSeparator()).append(buildFileLink(new File(dir, COMPOSITE_CONTENT_FILENAME)));
    }
    if (isUpdateSite) {
      addToCompositeChilds(depth, dir.getName());
    }
    if (this.isToCreateIndexHtml) {
      String indexHtml = isUpdateSite ? new String(this.indexUpdateSiteTemplate)
          : new String(this.indexNonUpdateSiteTemplate);
      indexHtml = replaceFileContent(indexHtml, repoName, this.updateSiteNamePropertyName);
      indexHtml = replaceFileContent(indexHtml, dirName, this.updateSiteDirNamePropertyName);
      indexHtml = replaceFileContent(indexHtml, sbFolderContent.toString(), this.updateSiteContentsPropertyName);
      for (Pair<String, String> propDefaultPair : this.propsToBeReplacedInTemplates) {
        indexHtml = replaceFileContent(indexHtml,
            StringUtils.defaultIfBlank(props.getProperty(propDefaultPair.getKey()), propDefaultPair.getValue()),
            propDefaultPair.getKey());
      }
      final File indexHtmlFile = new File(dir, INDEX_HTML_FILE_NAME);
      writeTargetFileConsideringDryRun(indexHtmlFile, indexHtml);
      this.isIndexHtmlCreated = true;
    }
  }

  protected static String replaceFileContent(final String fileContent, String value, final String propertyName) {
    if (StringUtils.isBlank(value)) {
      value = StringUtils.EMPTY;
    }
    return StringUtils.replace(fileContent, "${" + propertyName + "}", value);
  }

  protected Pair<Boolean, StringBuilder> getUpdateSiteContentFragment(final File dir, final Properties props,
      final int depth) throws MojoFailureException {
    boolean isUpdateSite = false;
    StringBuilder sbFolder = new StringBuilder();
    if (depth > 0) {
      sbFolder.append(buildFolderLink(".."));
    }
    final File[] listOfFiles = dir.listFiles();
    if (listOfFiles == null || listOfFiles.length == 0) {
      return Pair.of(isUpdateSite, sbFolder);
    }
    Arrays.sort(listOfFiles);
    StringBuilder sbFiles = new StringBuilder();
    for (File file : listOfFiles) {
      if (file.isFile() && !isExcludedFileName(file.getName())) {
        if (sbFiles.length() > 0) {
          sbFiles.append(System.lineSeparator());
        }
        sbFiles.append(buildFileLink(file));
        if (!isUpdateSite) {
          isUpdateSite = isUpdateSiteFile(file.getName());
        }
      } else if (file.isDirectory() && !isExcludedDirName(file.getName())) {
        if (sbFolder.length() > 0) {
          sbFolder.append(System.lineSeparator());
        }
        sbFolder.append(buildFolderLink(file.getName()));
        final File probablePropertiesFile = new File(file, this.updateSitePropertiesFileName);
        if (probablePropertiesFile.exists() && probablePropertiesFile.isFile()) {
          // if there are properties defined on this level, then start a new walk with
          // those
          walkAllFiles(file, depth + 1);
        } else {
          createCompositeFilesAndIndexHtmlAsRequired(file, props, depth + 1,
              this.isToCreateCompositeXmls && !this.compositeXmlsForFoldersWithUpdateSitePropertiesOnly);
        }
      }
    }
    sbFolder.append(sbFiles);
    return Pair.of(isUpdateSite, sbFolder);
  }

  protected boolean isUpdateSiteFile(String fileName) {
    return StringUtils.equalsIgnoreCase(fileName, "content.jar")
        || StringUtils.equalsIgnoreCase(fileName, "artifacts.jar")
        || StringUtils.startsWithIgnoreCase(fileName, "content.xml")
        || StringUtils.startsWithIgnoreCase(fileName, "artifacts.xml")
        || StringUtils.equalsIgnoreCase(fileName, P2INDEX_FILENAME);
  }

  protected boolean isExcludedFileName(String fileName) {
    return StringUtils.equalsIgnoreCase(fileName, this.updateSitePropertiesFileName)
        || StringUtils.equalsIgnoreCase(fileName, INDEX_HTML_FILE_NAME) // nl
        || StringUtils.startsWith(fileName, ".") // nl
        || this.isToCreateCompositeXmls && (StringUtils.equalsIgnoreCase(fileName, COMPOSITE_ARTIFACTS_FILENAME)
            || StringUtils.equalsIgnoreCase(fileName, COMPOSITE_CONTENT_FILENAME));
  }

  protected boolean isExcludedDirName(String dirName) {
    return StringUtils.startsWith(dirName, ".");
  }

  protected static String buildFolderLink(String folderName) {
    return String.format(
        "<tr><td><img style='margin-right:4px;' src='https://dev.eclipse.org/small_icons/places/folder.png'><a href='%s/'> %s</a></td></tr>",
        folderName, folderName);
  }

  protected static String buildFileLink(final File file) {
    final String fileName = file.getName();
    String type = "";
    if (StringUtils.endsWithIgnoreCase(fileName, ".xml")) {
      type = "text/xml";
    } else if (StringUtils.endsWithIgnoreCase(fileName, ".txt")
        || StringUtils.equalsIgnoreCase(fileName, P2INDEX_FILENAME)) {
      type = "text/plain";
    }
    final String fileSize = FileUtils.byteCountToDisplaySize(file.length());
    final String fileLastModified = new SimpleDateFormat("yyyy-MM-dd'&nbsp&nbsp'HH:mm")
        .format(new Date(file.lastModified()));
    return String.format(
        "<tr><td style='padding-right:50px;'><img style='margin-right:4px;' src='https://dev.eclipse.org/small_icons/actions/edit-copy.png'><a href='%s' %s> %s</a></td><td style='text-align: right; padding-right:15px;'> %s</td><td> %s</td></tr>",
        fileName, StringUtils.isNotBlank(type) ? "type='" + type + "'" : "", fileName, fileSize, fileLastModified);
  }

  protected static String buildCompositeLocation(String locationName) {
    return String.format("        <child location='%s'/>", locationName);
  }
}

/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.tools;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.apache.cayenne.gen.DefaultClassGenerator;

import java.io.File;

/**
 * Maven mojo to perform class generation from data map. This class is an Maven adapter to
 * DefaultClassGenerator class.
 *
 * @author Andrus Adamchik, Kevin Menard
 * @since 3.0
 *
 * @phase generate-sources
 * @goal cgen
 */
public class CayenneGeneratorMojo extends AbstractMojo
{
    /**
     * Project instance used to add generated source code to the build.
     *
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * Path to additional DataMap XML files to use for class generation.
     *
     * @parameter expression="${cgen.additionalMaps}"
     */
    private File additionalMaps;

    /**
     * Whether we are generating classes for the client tier in a
     * Remote Object Persistence application. Default is <code>false</code>.
     *
     * @parameter expression="${cgen.client}" default-value="false"
     */
    private boolean client;

    /**
     * 	Destination directory for Java classes (ignoring their package names).
     *
     * @parameter expression="${cgen.destDir}" default-value="${project.build.sourceDirectory}/java/generated-sources/cayenne"
     */
    private File destDir;

    /**
     * Specify generated file encoding if different from the default on current platform.
     * Target encoding must be supported by the JVM running Maven build. Standard encodings
     * supported by Java on all platforms are US-ASCII, ISO-8859-1, UTF-8, UTF-16BE,
     * UTF-16LE, UTF-16. See Sun Java Docs for java.nio.charset.Charset for more information.
     *
     * @parameter expression="${cgen.encoding}"
     */
    private String encoding;

    /**
     * Entities (expressed as a perl5 regex) to exclude from template
     * generation. (Default is to include all entities in the DataMap).
     *
     * @parameter expression="${cgen.excludeEntitiesPattern}"
     */
    private String excludeEntitiesPattern;

    /**
     * Entities (expressed as a perl5 regex) to include in template
     * generation. (Default is to include all entities in the DataMap).
     *
     * @parameter expression="${cgen.includeEntitiesPattern}"
     */
    private String includeEntitiesPattern;

    /**
     * If set to <code>true</code>, will generate subclass/superclass pairs,
     * with all generated code included in superclass (default is <code>true</code>).
     *
     * @parameter expression="${cgen.makePairs}" default-value="true"
     */
    private boolean makePairs;

    /**
     * DataMap XML file to use as a base for class generation.
     *
     * @parameter expression="${cgen.map}"
     * @required
     */
    private File map;

    /**
     * Specifies generator iteration target. &quot;entity&quot; performs one iteration for each selected entity.
     * &quot;datamap&quot; performs one iteration per datamap (This is always one iteration since cgen
     * currently supports specifying one-and-only-one datamap). (Default is &quot;entity&quot;)
     *
     * @parameter expression="${cgen.mode}" default-value="entity"
     */
    private String mode;

    /**
     * Name of file for generated output. (Default is &quot;*.java&quot;)
     *
     * @parameter expression="${cgen.outputPattern}" default-value="*.java"
     */
    private String outputPattern;

    /**
     * 	If set to <code>true</code>, will overwrite older versions of generated classes.
     *  Ignored unless makepairs is set to <code>false</code>.
     *
     * @parameter expression="${cgen.overwrite}" default-value="false"
     */
    private boolean overwrite;

    /**
     * Java package name of generated superclasses. Ignored unless <code>makepairs</code>
     * set to <code>true</code>.  If omitted, each superclass will be assigned the same
     * package as subclass. Note that having superclass in a different package would only
     * make sense when <code>usepkgpath</code> is set to <code>true</code>. Otherwise
     * classes from different packages will end up in the same directory.
     *
     * @parameter expression="${cgen.superPkg}"
     */
    private String superPkg;

    /**
     * Location of Velocity template file for Java superclass generation. Ignored unless
     * <code>makepairs</code> set to <code>true</code>. If omitted, default template is used.
     *
     * @parameter expression="${cgen.superTemplate}"
     */
    private String superTemplate;

    /**
     * Location of Velocity template file for Java class generation. If omitted, default template is used.
     *
     * @parameter expression="${cgen.template}"
     */
    private String template;

    /**
     * If set to <code>true</code> (default), a directory tree will be generated in "destDir"
     * corresponding to the class package structure, if set to <code>false</code>, classes will
     * be generated in &quot;destDir&quot; ignoring their package.
     *
     * @parameter expression="${cgen.usePkgPath}" default-value="true"
     */
    private boolean usePkgPath;

    /**
     * Specifies template location and generator behavior. &quot;1.1&quot; is the old behavior,
     * with templates located in &quot;dotemplates&quot; and &quot;classgen&quot; as the only
     * velocity context attribute. &quot;1.2&quot; is the new behavior, with templates located
     * in &quot;dotemplates/v1.2&quot; and &quot;objEntity&quot;, &quot;entityUtils&quot;,
     * &quot;stringUtils&quot;, and &quot;importUtils&quot; in the velocity context. (Default is &quot;1.2&quot;.)
     *
     * @parameter expression="${cgen.version}" default-value="1.1"
     */
    private String version;

    private DefaultClassGenerator generator;
    protected CayenneGeneratorUtil generatorUtil;

    public void execute() throws MojoExecutionException, MojoFailureException
    {
        // Create the destination directory if necessary.
    	// TODO: (KJM 11/2/06) The destDir really should be added as a compilation resource for maven.
        if (!destDir.exists())
        {
            destDir.mkdirs();
        }

        generator = createGenerator();
        generatorUtil = new CayenneGeneratorUtil();
        
        generatorUtil.setExcludeEntitiesPattern(excludeEntitiesPattern);
        generatorUtil.setGenerator(generator);
        generatorUtil.setIncludeEntitiesPattern(includeEntitiesPattern);
        generatorUtil.setLogger(new MavenLogger(this));
        generatorUtil.setMap(map);

        try {
        	generatorUtil.setAdditionalMaps(convertAdditionalDataMaps());
            generatorUtil.execute();
        }
        catch (Exception e) {
            throw new MojoExecutionException("Error generating classes: ", e);
        }
    }

    /**
     * Loads and returns DataMap based on <code>map</code> attribute.
     */
    protected File[] convertAdditionalDataMaps() throws Exception {
    	
    	if (null == additionalMaps) {
            return new File[0];
        }
    	
        if (!additionalMaps.isDirectory()) {
            throw new MojoFailureException("'additionalMaps' must be a directory containing only datamap files.");
        }

        String[] maps = additionalMaps.list();
        File[] dataMaps = new File[maps.length];
        for (int i = 0; i < maps.length; i++) {
            dataMaps[i] = new File(maps[i]);
        }
        return dataMaps;
    }

    /**
     * Factory method to create internal class generator. Called from constructor.
     */
    protected DefaultClassGenerator createGenerator() {
        DefaultClassGenerator gen = new DefaultClassGenerator();

        gen.setClient(client);
        gen.setDestDir(destDir);
        gen.setEncoding(encoding);
        gen.setMakePairs(makePairs);
        gen.setMode(mode);
        gen.setOutputPattern(outputPattern);
        gen.setOverwrite(overwrite);
        gen.setSuperPkg(superPkg);
        gen.setSuperTemplate(superTemplate);
        gen.setTemplate(template);
        gen.setUsePkgPath(usePkgPath);
        gen.setVersionString(version);

        return gen;
    }
}

class MavenLogger implements ILog {
	
	private Log logger;
	
	public MavenLogger(AbstractMojo parent) {
		this.logger = parent.getLog();
	}
	
	public void log(String msg) {
		logger.debug(msg);
	}

	public void log(String msg, int msgLevel) {
		
		switch (msgLevel) {
			case ILog.MSG_DEBUG:
				logger.debug(msg);
				break;
				
			case ILog.MSG_ERR:
				logger.error(msg);
				break;
				
			case ILog.MSG_INFO:
				logger.info(msg);
				break;
				
			case ILog.MSG_VERBOSE:
				logger.info(msg);
				break;
				
			case ILog.MSG_WARN:
				logger.warn(msg);
				break;
				
			default:
				logger.debug(msg);
		}
	}
}
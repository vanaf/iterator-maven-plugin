package com.soebes.maven.plugins.iterator;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.io.File;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;

/**
 * Invoker will execute an Maven instance by iterating through the given items.
 * 
 * @author Karl-Heinz Marbaise <a href="mailto:khmarbaise@apache.org">khmarbaise@apache.org</a>
 */
@Mojo( name = "invoker", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true )
public class InvokerMojo
    extends AbstractInvokerMojo
{

    /**
     * The project currently being build.
     */
    @Parameter( required = true, readonly = true, defaultValue = "${project}" )
    private MavenProject mavenProject;

    /**
     * The current Maven session.
     */
    @Parameter( required = true, readonly = true, defaultValue = "${session}" )
    private MavenSession mavenSession;

    /**
     * The Maven BuildPluginManager component.
     */
    @Component
    private BuildPluginManager pluginManager;

    @Parameter( defaultValue = "${plugin}", required = true, readonly = true )
    private PluginDescriptor pluginDescriptor;

    @Parameter
    private File workingDirectory;

    @Component
    private Invoker invoker;

    /**
     * Possibly to multithreading..
     */
    // private int numberOfThreads;

    public void execute()
        throws MojoExecutionException
    {
        if ( isItemsNull() && isContentNull() )
        {
            throw new MojoExecutionException( "You have to use at least one. Either items element or content element!" );
        }

        if ( isItemsSet() && isContentSet() )
        {
            throw new MojoExecutionException(
                                              "You can use only one element. Either items element or content element but not both!" );
        }

        File localRepository = new File( mavenSession.getSettings().getLocalRepository() );

        invoker.setLocalRepositoryDirectory( localRepository );
        // invoker.setOutputHandler(outputHandler);
        // TODO: Check how it looks if we will use the invokerLogger?
        // invoker.setLogger();

        // getLog().info("local repository: " + localRepository);
        // // getLog().isDebugEnabled()
        // getLog().info("Invoker:" + invoker);
        for ( String item : getItems() )
        {
            try
            {
                getLog().info( "mvn " + item );
                mavenCall( item );
            }
            catch ( MavenInvocationException e )
            {
                getLog().error( "Failure during maven call:", e );
            }
        }
    }

    private File getWorkingDirectoryAfterPlaceHolderIsReplaced( String currentValue )
    {
        File baseDir = getWorkingDirectory();
        if ( baseDir != null && baseDir.toString().contains( getPlaceHolder() ) )
        {
            baseDir = new File( baseDir.toString().replaceAll( getPlaceHolder(), currentValue ) );
        }
        return baseDir;
    }

    private void mavenCall( String item )
        throws MavenInvocationException
    {
        InvocationRequest request = createAndConfigureAnInvocationRequest( item );

        OutputConsumer output = new OutputConsumer( getLog() );
        request.setOutputHandler( output );

        invoker.setWorkingDirectory( getWorkingDirectoryAfterPlaceHolderIsReplaced( item ) );

        InvocationResult result = invoker.execute( request );

        if ( result.getExitCode() == 0 )
        {
            getLog().info( "Maven call Ok." );
        }
        else
        {
            getLog().error( "Maven call was NOT Ok. (" + result.getExitCode() + ")" );
            if ( result.getExecutionException() != null )
            {
                getLog().error( result.getExecutionException().getMessage(), result.getExecutionException().getCause() );
            }
            else
            {
                getLog().error( "No exception" );
            }
        }
    }

    public MavenProject getMavenProject()
    {
        return mavenProject;
    }

    public void setMavenProject( MavenProject mavenProject )
    {
        this.mavenProject = mavenProject;
    }

    public void setThreads( String threads )
    {
        this.threads = threads;
    }

    public File getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory( File workingDirectory )
    {
        this.workingDirectory = workingDirectory;
    }

}

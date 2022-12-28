package cyclic.gradle;

import cyclic.gradle.tasks.CyclicCompile;
import cyclic.gradle.tasks.CyclicSourceDirectorySet;
import cyclic.gradle.tasks.DefaultCyclicSourceDirectorySet;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.plugins.JavaBasePlugin;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.plugins.internal.JvmPluginsHelper;
import org.gradle.api.tasks.TaskProvider;
import org.gradle.api.tasks.compile.CompileOptions;

import static org.gradle.api.internal.lambdas.SerializableLambdas.spec;

@NonNullApi
public class CyclicBasePlugin implements Plugin<Project>{
	
	public static final String CYCLIC_CONFIGURATION_NAME = "cyclic";
	
	// thanks scala & antlr
	
	public void apply(Project project){
		project.getPluginManager().apply(JavaBasePlugin.class);
		
		project.getConfigurations().create(CYCLIC_CONFIGURATION_NAME)
				.setVisible(false)
				.setDescription("The Cyclic compiler to be used in this project.");
		
		// TODO: once available
		// cycCompilerConfig.defaultDependencies(deps -> deps.add(project.getDependencies().create("cyclic.lang:compiler:1.0.0@jar")));
		
		project.getTasks().withType(CyclicCompile.class).configureEach(compile ->
				compile.compilerCp = project.getConfigurations().getByName(CYCLIC_CONFIGURATION_NAME));
		
		applyToSourceSets(project);
	}
	
	private void applyToSourceSets(Project project){
		project.getExtensions().getByType(JavaPluginExtension.class).getSourceSets().all(sourceSet -> {
			String displayName = (String)InvokerHelper.invokeMethod(sourceSet, "getDisplayName", null);
			
			CyclicSourceDirectorySet cycSources = new DefaultCyclicSourceDirectorySet(
					"cyclic",
					displayName + " Cyclic sources",
					project.getObjects());
			var src = project.file("src/" + sourceSet.getName() + "/cyclic");
			cycSources.srcDir(src);
			
			sourceSet.getExtensions().add(CyclicSourceDirectorySet.class, "cyclic", cycSources);
			sourceSet.getAllJava().source(cycSources);
			sourceSet.getAllSource().source(cycSources);
			
			// scala plugin says to explicitly capture in lambda, who am I to disagree?
			@SuppressWarnings("UnnecessaryLocalVariable")
			FileCollection cycFiles = cycSources;
			sourceSet.getResources().getFilter().exclude(
					spec(element -> cycFiles.contains(element.getFile()))
			);
			
			TaskProvider<CyclicCompile> cyclicCompileTask = project.getTasks().register(
					sourceSet.getCompileTaskName("cyclic"),
					CyclicCompile.class,
					cycCompile -> {
						// TODO: customisable compilation
						CompileOptions options = project.getObjects().newInstance(CompileOptions.class);
						JvmPluginsHelper.configureForSourceSet(sourceSet, cycSources, cycCompile, options, project);
						cycCompile.setDescription("Compiles the " + cycSources + ".");
						cycCompile.setSource(cycSources);
						//cycCompile.getJavaLauncher().convention(getToolchainTool(project, JavaToolchainService::launcherFor));
						cycCompile.setSrc(src);
					}
			);
			
			JvmPluginsHelper.configureOutputDirectoryForSourceSet(sourceSet, cycSources, project, cyclicCompileTask, new DefaultProvider<>(() -> project.getObjects().newInstance(CompileOptions.class)));
			
			project.getTasks().named(sourceSet.getClassesTaskName(), task -> task.dependsOn(cyclicCompileTask));
		});
	}
}
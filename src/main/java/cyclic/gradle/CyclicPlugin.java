package cyclic.gradle;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;

@SuppressWarnings("unused")
@NonNullApi
public class CyclicPlugin implements Plugin<Project>{
	
	public void apply(Project project){
		project.getPluginManager().apply(CyclicBasePlugin.class);
		project.getPluginManager().apply(JavaPlugin.class);
	}
}
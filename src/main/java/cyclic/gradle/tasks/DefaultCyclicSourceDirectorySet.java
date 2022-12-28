package cyclic.gradle.tasks;

import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.internal.file.DefaultSourceDirectorySet;
import org.gradle.api.model.ObjectFactory;

public class DefaultCyclicSourceDirectorySet extends DefaultSourceDirectorySet implements CyclicSourceDirectorySet{
	
	public DefaultCyclicSourceDirectorySet(SourceDirectorySet sourceSet){
		super(sourceSet);
		getFilter().include("**/*.cyc");
	}
	
	public DefaultCyclicSourceDirectorySet(String name, String displayName, ObjectFactory objectFactory){
		this(objectFactory.sourceDirectorySet(name, displayName));
	}
}
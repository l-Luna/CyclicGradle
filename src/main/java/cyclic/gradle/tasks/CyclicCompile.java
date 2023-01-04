package cyclic.gradle.tasks;

import org.gradle.api.GradleException;
import org.gradle.api.file.FileCollection;
import org.gradle.api.internal.file.temp.TemporaryFileProvider;
import org.gradle.api.internal.tasks.compile.CompilationFailedException;
import org.gradle.api.tasks.InputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.compile.AbstractCompile;
import org.gradle.internal.jvm.Jvm;
import org.gradle.process.ExecOperations;

import javax.inject.Inject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.jar.JarFile;

public class CyclicCompile extends AbstractCompile{
	
	private final ExecOperations execOps;
	private final TemporaryFileProvider tmps;
	
	// this feels gross, but gradle operates on the file level, while the compiler (currently) only operates on the folder level
	private File src = null;
	
	public FileCollection compilerCp = null;
	
	@Inject
	public CyclicCompile(ExecOperations execOps, TemporaryFileProvider tmps){
		this.execOps = execOps;
		this.tmps = tmps;
	}
	
	@TaskAction
	public void compile() throws IOException{
		// validate the cyclic compiler jar
		if(compilerCp == null || compilerCp.getFiles().size() == 0)
			throw new GradleException("A Cyclic compiler must be specified using the \"cyclic\" dependency configuration.");
		else if(compilerCp.getFiles().size() > 1)
			throw new GradleException("Only one jar file may be specified by the \"cyclic\" dependency configuration.");
		
		var compilerJar = compilerCp.getSingleFile();
		try(JarFile file = new JarFile(compilerJar)){
			var entry = file.getEntry("cyclic_compiler.properties");
			if(entry == null)
				throw new GradleException("Specified Cyclic compiler is not a Cyclic compiler (missing properties).");
		}
		
		// create the fake project file
		String srcDir = getSrc().getAbsolutePath();
		String outDir = getDestinationDirectory().getAsFile().get().getAbsolutePath();
		List<String> jarDeps = getClasspath().getFiles().stream().map(File::getAbsolutePath).toList();
		
		StringBuilder projectText = new StringBuilder("""
				source: "%s"
				output: "%s"
				
				""".formatted(srcDir.replace("\\", "/"), outDir.replace("\\", "/")));
		if(jarDeps.size() > 0){
			projectText.append("dependencies:\n");
			// bleh
			for(String dep : jarDeps)
				if(dep.endsWith(".jar"))
					projectText.append("""
							 - type: "jar"
							   location: "%s"
							""".formatted(dep.replace("\\", "/")));
				else
					projectText.append("""
							 - type: "classFolder"
							   location: "%s"
							""".formatted(dep.replace("\\", "/")));
		}
		
		File projectFile = tmps.createTemporaryFile("project", ".cyc.yaml");
		Files.writeString(projectFile.toPath(), projectText.toString(), StandardOpenOption.WRITE);
		
		try{
			var result = execOps.javaexec(spec -> {
				spec.setExecutable(Jvm.current().getJavaExecutable());
				spec.setClasspath(compilerCp);
				spec.setArgs(List.of("-p", projectFile.getAbsolutePath()));
				spec.setJvmArgs(List.of("--enable-preview"));
			});
			
			if(result.getExitValue() != 0)
				throw new CompilationFailedException(result.getExitValue());
		}finally{
			projectFile.delete();
		}
	}
	
	@InputDirectory
	public File getSrc(){
		return src;
	}
	
	public void setSrc(File src){
		this.src = src;
	}
}
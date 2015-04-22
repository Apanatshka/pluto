package build.pluto.test.build.cycle.fixpoint;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.sugarj.common.FileCommands;

import build.pluto.BuildUnit.State;
import build.pluto.builder.Builder;
import build.pluto.builder.CycleSupport;
import build.pluto.stamp.FileContentStamper;
import build.pluto.stamp.Stamper;

public abstract class NumericBuilder extends Builder<FileInput, IntegerOutput> {

	protected static interface Operator {
		public int apply(int a, int b);
	}

	public NumericBuilder(FileInput input) {
		super(input);
	}

	protected abstract Operator getOperator();

	@Override
	protected final File persistentPath() {
		return FileCommands.addExtension(this.input.getFile().toPath(), "dep").toFile();
	}

	@Override
	protected final Stamper defaultStamper() {
		return FileContentStamper.instance;
	}
	
	@Override
	protected final CycleSupport getCycleSupport() {
		return new NumericCycleSupport();
	}

	@Override
	protected final IntegerOutput build() throws Throwable {
		require(this.input.getFile());
		int myNumber = FileUtils.readIntFromFile(this.input.getFile());

		
		if (this.input.getDepsFile().exists()) {
			require(this.input.getDepsFile());
			List<File> depPaths = FileUtils.readPathsFromFile(
					this.input.getDepsFile(), this.input.getWorkingDir());

			for (File path : depPaths) {
				FileInput input = new FileInput(this.input.getWorkingDir(),
						path);
				IntegerOutput output = null;
				String extension = FileCommands.getExtension(path);
				if (extension.equals("modulo")) {
					output = requireBuild(ModuloBuilder.factory, input);
				} else if (extension.equals("divide")) {
					output = requireBuild(DivideByBuilder.factory, input);
				} else if (extension.equals("gcd")) {
					output = requireBuild(GCDBuilder.factory, input);
				} else {
					throw new IllegalArgumentException("The extension "
							+ extension + " is unknown.");
				}
				// Cycle support: if there is no output currently, ignore the dependency
				//if (output != null && FileCommands.exists(output.getResultFile())) {
				if (output != null) {
					try {
				myNumber = this.getOperator().apply(myNumber,
						output.getResult());
				//require(output.getResultFile());

					} catch (IOException e) {}
				}
			
				//} 
			}
		}
		
		File outFile = FileCommands.replaceExtension(input.getFile().toPath(), "out").toFile();
		FileUtils.writeIntToFile(myNumber, outFile);
		provide(outFile);
		
		setState(State.finished(true));
		return new IntegerOutput(outFile, myNumber);

	}

}

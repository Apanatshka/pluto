package build.pluto.test.build;

import java.io.IOException;
import java.util.ArrayList;

import org.junit.Test;

import build.pluto.builder.BuildRequest;
import build.pluto.builder.BuilderFactory;
import build.pluto.builder.CompileCycleAtOnceBuilder;
import build.pluto.output.None;
import build.pluto.test.build.SimpleBuilder.TestBuilderInput;

public class CycleAtOnceBuilderTest extends SimpleBuildTest{


	@Override
	protected BuildRequest<?,?,?,?> requirementForInput(TestBuilderInput input) {
		return new BuildRequest<ArrayList<TestBuilderInput>,None, SimpleCyclicAtOnceBuilder, BuilderFactory<ArrayList<TestBuilderInput>, None, SimpleCyclicAtOnceBuilder>> (SimpleCyclicAtOnceBuilder.factory, CompileCycleAtOnceBuilder.singletonArrayList(input));
	}
	
	@Test
	public void buildCycle() throws IOException {
		TrackingBuildManager manager = buildMainFile();
		// TODO do some assertions here
	}

	
}

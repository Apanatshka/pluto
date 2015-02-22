package org.sugarj.cleardep.build;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.GraphUtils;
import org.sugarj.cleardep.Mode;
import org.sugarj.cleardep.build.RequiredBuilderFailed.BuilderResult;
import org.sugarj.cleardep.stamp.Stamp;
import org.sugarj.common.Log;
import org.sugarj.common.path.Path;

public class BuildManager {
  
  private final Map<Path, Stamp> editedSourceFiles;
  private Map<Path, Boolean> consistencyMap = new HashMap<>();
  
  public BuildManager() {
    this(null);
  }
  public BuildManager(Map<Path, Stamp> editedSourceFiles) {
    this.editedSourceFiles = editedSourceFiles;
  }
  
  private static class BuildStackEntry {
    private final Builder<?, ?> builder;
    private final Path persistencePath;
    
    
    public BuildStackEntry(Builder<?, ?> builder, Path persistencePath) {
      super();
      this.builder = builder;
      this.persistencePath = persistencePath;
    }
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((builder == null) ? 0 : builder.hashCode());
      result = prime * result + ((persistencePath == null) ? 0 : persistencePath.hashCode());
      return result;
    }
    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      BuildStackEntry other = (BuildStackEntry) obj;
      if (builder == null) {
        if (other.builder != null)
          return false;
      } else if (!builder.equals(other.builder))
        return false;
      if (persistencePath == null) {
        if (other.persistencePath != null)
          return false;
      } else if (!persistencePath.equals(other.persistencePath))
        return false;
      return true;
    }
  
    
    
  }
  
  private Deque<BuildStackEntry> requireCallStack = new ArrayDeque<>();
  
  private  <E extends CompilationUnit> boolean isConsistent(Path depPath, Class<E> resultClass, Mode<E>  mode, Map<Path, Stamp> editedSourceFiles) throws IOException {
    Boolean isConsistent = consistencyMap.get(depPath);
    if (isConsistent != null) {
      return isConsistent;
    }
    
    // We need to do something else
    CompilationUnit rootUnit = CompilationUnit.read(resultClass, mode, depPath);
    
    if (rootUnit == null) {
      consistencyMap.put(depPath, false);
      return false;
    }
    
    List<CompilationUnit> sortedUnits = GraphUtils.sortTopologicalFrom(rootUnit);
    for (CompilationUnit unit :sortedUnits) {
      boolean unitConsistent = true;
      for (CompilationUnit depUnit : unit.getModuleDependencies()) {
        if (!consistencyMap.get(depUnit.getPersistentPath())) {
          unitConsistent = false;
          break;
        }
      }
      if (unitConsistent) {
        unitConsistent = unit.isConsistentShallow(editedSourceFiles);
      }
      consistencyMap.put(unit.getPersistentPath(), unitConsistent);
    }
    
    return consistencyMap.get(depPath);
  }

  
  public <T, E extends CompilationUnit> E require(Builder<T, E> builder, Mode<E> mode) throws IOException {
    
    
    if (builder.manager != this) {
      throw new RuntimeException("Illegal builder using another build manager for this build");
    }
    
    Path dep = builder.persistentPath();
    
    E depResult;
//    = CompilationUnit.readConsistent(builder.resultClass(), mode, builder.context.getEditedSourceFiles(), dep);
//       if (depResult != null)
//         return depResult;
    
    if (this.isConsistent(dep, builder.resultClass(), mode, editedSourceFiles)) {
      depResult = CompilationUnit.read(builder.resultClass(), mode, dep);
      if (!depResult.isConsistent(editedSourceFiles, mode)) {
        throw new AssertionError("BuildManager does not guarantee soundness");
      }
      return depResult;
    }
    
    BuildStackEntry entry = new BuildStackEntry(builder, dep);
    
    if (this.requireCallStack.contains(entry)) {
      throw new BuildCycleException("Build contains a dependency cycle on " + dep);
    }
    this.requireCallStack.push(entry);
    
    depResult = CompilationUnit.create(builder.resultClass(), builder.defaultStamper(), mode, null, dep);
    String taskDescription = builder.taskDescription();
    try {
      depResult.setState(CompilationUnit.State.IN_PROGESS);
      
      if (taskDescription != null)
        Log.log.beginTask(taskDescription, Log.CORE);
      
      // call the actual builder
      builder.triggerBuild(depResult);
//      build(depResult, input);
      
      if (!depResult.isFinished())
        depResult.setState(CompilationUnit.State.SUCCESS);
      depResult.write();
    } catch(RequiredBuilderFailed e) {
      BuilderResult required = e.getLastAddedBuilder();
      depResult.addModuleDependency(required.result);
      depResult.setState(CompilationUnit.State.FAILURE);
      depResult.write();
      
      e.addBuilder(builder, depResult);
      if (taskDescription != null)
        Log.log.logErr("Required builder failed", Log.CORE);
      throw e;
    } catch (Throwable e) {
      depResult.setState(CompilationUnit.State.FAILURE);
      depResult.write();
      Log.log.logErr(e.getMessage(), Log.CORE);
      throw new RequiredBuilderFailed(builder, depResult, e);
    } finally {
      if (taskDescription != null)
        Log.log.endTask();
      BuildStackEntry poppedEntry = requireCallStack.pop();
      if (poppedEntry != entry) {
        throw new AssertionError("Got the wrong build stack entry from the requires stack");
      }
    }
    
    consistencyMap.put(dep, true);
    
    
    if (depResult.getState() == CompilationUnit.State.FAILURE)
      throw new RequiredBuilderFailed(builder, depResult, new IllegalStateException("Builder failed for unknown reason, please confer log."));

    return depResult;
  }
}

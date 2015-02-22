package org.sugarj.cleardep.build;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.cleardep.Mode;

public class BuildRequirement<T_, E_ extends CompilationUnit, B_ extends Builder<T_,E_>> {
    final BuilderFactory<T_, E_, B_> factory;
    final T_ input;
    final Mode<E_> mode;
    public BuildRequirement(BuilderFactory<T_, E_, B_> factory, T_ input, Mode<E_> mode) {
      this.factory = factory;
      this.input = input;
      this.mode = mode;
    }
  }
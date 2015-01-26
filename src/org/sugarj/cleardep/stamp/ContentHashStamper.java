package org.sugarj.cleardep.stamp;

import java.io.IOException;

import org.sugarj.cleardep.CompilationUnit;
import org.sugarj.common.FileCommands;
import org.sugarj.common.path.Path;

/**
 * @author Sebastian Erdweg
 */
public class ContentHashStamper implements Stamper, ModuleStamper {

  private ContentHashStamper() {}
  public static final Stamper instance = new ContentHashStamper();
  public static final ModuleStamper minstance = new ContentHashStamper();
  
  /**
   * @see org.sugarj.cleardep.stamp.Stamper#stampOf(org.sugarj.common.path.Path)
   */
  @Override
  public ContentHashStamp stampOf(Path p) {
    if (!FileCommands.exists(p))
      return new ContentHashStamp(0);
    
    try {
      return new ContentHashStamp(FileCommands.fileHash(p));
    } catch (IOException e) {
      e.printStackTrace();
      return new ContentHashStamp(-1);
    }
  }

  public ContentHashStamp stampOf(CompilationUnit m) {
    if (!m.isPersisted())
      throw new IllegalArgumentException("Cannot compute stamp of non-persisted compilation unit " + m);

    return stampOf(m.getPersistentPath());
  }
  
  public static class ContentHashStamp extends SimpleStamp<Integer> {

    private static final long serialVersionUID = 7535020621495360152L;

    public ContentHashStamp(Integer t) {
      super(t);
    }
    
    @Override
    public boolean equals(Stamp o) {
      return o instanceof ContentHashStamp && super.equals(o);
    }
    
    @Override
    public boolean equals(ModuleStamp o) {
      return o instanceof ContentHashStamp && super.equals((Stamp) o);
    }

    @Override
    public Stamper getStamper() {
      return ContentHashStamper.instance;
    }
    
    @Override
    public ModuleStamper getModuleStamper() {
      return ContentHashStamper.minstance;
    }
  }
}

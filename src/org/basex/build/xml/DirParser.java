package org.basex.build.xml;

import static org.basex.core.Text.*;
import java.io.IOException;
import org.basex.build.Builder;
import org.basex.build.Parser;
import org.basex.core.Main;
import org.basex.core.Prop;
import org.basex.io.IO;

/**
 * This class parses the tokens that are delivered by the
 * {@link XMLScanner} and sends them to the specified database builder.
 *
 * @author Workgroup DBIS, University of Konstanz 2005-09, ISC License
 * @author Christian Gruen
 */
public final class DirParser extends Parser {
  /** Parser reference. */
  private Parser parser;
  /** File filter. */
  private String filter;
  /** Element counter. */
  private int c;

  /**
   * Constructor.
   * @param f file reference
   * @param pr database properties
   */
  public DirParser(final IO f, final Prop pr) {
    super(f, pr);
    if(f.isDir()) {
      filter = pr.get(Prop.CREATEFILTER).replaceAll("\\*", ".*");
      if(!filter.contains(".")) filter = ".*" + filter + ".*";
    } else {
      filter = ".*";
    }
  }

  @Override
  public void parse(final Builder b) throws IOException {
    b.meta.filesize = 0;
    parse(b, io);
    if(Prop.debug) Main.err(NL);
  }

  /**
   * Parses the specified file or parser its children.
   * @param b builder
   * @param path file path
   * @throws IOException I/O exception
   */
  private void parse(final Builder b, final IO path) throws IOException {
    // might slow down the process..
    // if(path.isSymLink()) return;

    if(path.isDir()) {
      for(final IO f : path.children()) parse(b, f);
    } else {
      io = path;
      while(path.more()) {
        if(!path.name().matches(filter)) continue;
        b.meta.filesize += io.length();
        parser = Parser.xmlParser(io, prop);
        parser.doc = doc;
        parser.parse(b);
        if(Prop.debug && ++c % 100 == 0) Main.err(";");
      }
    }
  }

  @Override
  public String tit() {
    return parser != null ? parser.tit() : PROGCREATE;
  }

  @Override
  public String det() {
    return parser != null ? parser.det() : "";
  }

  @Override
  public double prog() {
    return parser != null ? parser.prog() : 0;
  }
}

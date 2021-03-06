package org.basex.query.func.user;

import org.basex.query.*;
import org.basex.query.value.node.*;
import org.basex.util.*;

/**
 * Function implementation.
 *
 * @author BaseX Team 2005-16, BSD License
 * @author Christian Gruen
 */
public final class UserInfo extends UserFn {
  @Override
  public ANode item(final QueryContext qc, final InputInfo ii) {
    return qc.context.users.info();
  }
}

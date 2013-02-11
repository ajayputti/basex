package org.basex.query;

import java.util.*;

import org.basex.query.func.*;
import org.basex.query.util.*;
import org.basex.query.var.*;
import org.basex.util.*;
import org.basex.util.list.*;

/**
 * This class compiles all components of the query that are needed in an order that
 * maximizes the amount of inlining possible.
 *
 * @author BaseX Team 2005-12, BSD License
 * @author Leo Woerteler
 */
public final class QueryCompiler {
  /** Result list. */
  private final ArrayList<int[]> result = new ArrayList<int[]>();
  /** Node stack. */
  private final IntList stack = new IntList();
  /** Index and lowlink list. */
  private final IntList list = new IntList();
  /** Counter for the next free index. */
  private int nextIndex;
  /** Nodes in the stack. */
  private final BitArray inStack = new BitArray();

  /** Adjacency list. */
  final ArrayList<IntList> adjacent = new ArrayList<IntList>();
  /** Declaration list. */
  final ArrayList<Scope> scopes = new ArrayList<Scope>();

  /**
   * Constructor.
   * @param root root expression
   */
  private QueryCompiler(final Scope root) {
    scopes.add(root);
    adjacent.add(null);
  }

  /**
   * Compiles all necessary parts of this query.
   * @param ctx query context
   * @param root root expression
   * @throws QueryException compilation errors
   */
  public static void compile(final QueryContext ctx, final MainModule root)
      throws QueryException {
    new QueryCompiler(root).compile(ctx);
  }

  /**
   * Compiles all necessary parts of this query.
   * @param ctx query context
   * @throws QueryException compilation errors
   */
  private void compile(final QueryContext ctx) throws QueryException {
    tarjan(0);
    for(final int[] comp : result) {
      if(comp.length > 1) {
        for(final int s : comp) {
          final Scope scp = scopes.get(s);
          if(scp instanceof StaticVar)
            throw Err.CIRCVAR.thrw(((StaticVar) scp).info, scp);
        }
        // compile only the entry point, all other functions are compiled recursively
        scopes.get(comp[comp.length - 1]).compile(ctx);
      } else {
        scopes.get(comp[0]).compile(ctx);
      }
    }
    // analyze((MainModule) scopes.get(0), new IdentityHashMap<Scope, Object>());
  }

  private void analyze(final MainModule root,
      final IdentityHashMap<Scope, Object> identityHashMap) {
    final StringBuilder sb = new StringBuilder();
    root.visit(new ASTVisitor() {
      @Override
      public boolean staticVar(final StaticVar var) {
        if(identityHashMap.put(var, var) == null) {
          var.visit(this);
          var.fullDesc(sb).append('\n');
        }
        return true;
      }

      @Override
      public boolean funcCall(final UserFuncCall call) {
        final UserFunc func = call.func();
        if(identityHashMap.put(func, func) == null) {
          func.visit(this);
          sb.append(func).append('\n');
        }
        return true;
      }

      @Override
      public boolean subScope(final Scope sub) {
        return sub.visit(this);
      }
    });
    System.out.println("Inlined:");
    System.out.println(sb.append('\n').append(root).append('\n'));
  }

  /**
   * Algorithm of Tarjan for computing the strongly connected components of a graph.
   * @param v current node
   */
  private void tarjan(final int v) {
    final int ixv = 2 * v, llv = ixv + 1, idx = nextIndex++;
    while(list.size() <= llv) list.add(-1);
    list.set(ixv, idx);
    list.set(llv, idx);

    stack.push(v);
    inStack.set(v);

    for(int w : adjacentTo(v)) {
      final int ixw = 2 * w, llw = ixw + 1;
      if(list.size() <= ixw || list.get(ixw) < 0) {
        // Successor w has not yet been visited; recurse on it
        tarjan(w);
        list.set(llv, Math.min(list.get(llv), list.get(llw)));
      } else if(inStack.get(w)) {
        // Successor w is in stack S and hence in the current SCC
        list.set(llv, Math.min(list.get(llv), list.get(ixw)));
      }
    }

    // If v is a root node, pop the stack and generate an SCC
    if(list.get(llv) == list.get(ixv)) {
      int w;
      int[] out = null;
      do {
        w = stack.pop();
        inStack.clear(w);
        out = out == null ? new int[] { w } : Array.add(out, w);
      } while(w != v);
      result.add(out);
    }
  }

  /**
   * Returns the indices of all scopes called by the given one.
   * @param node source node index
   * @return destination node indices
   */
  private int[] adjacentTo(final int node) {
    IntList adj = adjacent.get(node);
    if(adj == null) {
      adj = new IntList();
      fillIn(scopes.get(node), adj);
      adjacent.set(node, adj);
    }
    return adjacent.get(node).toArray();
  }

  /**
   * Fills in all used scopes of the given one.
   * @param curr current scope
   * @param adj list of adjacent scopes
   */
  private void fillIn(final Scope curr, final IntList adj) {
    curr.visit(new ASTVisitor() {
      @Override
      public boolean staticVar(final StaticVar var) {
        return add(var);
      }

      @Override
      public boolean funcCall(final UserFuncCall call) {
        return add(call.func());
      }

      @Override
      public boolean subScope(final Scope sub) {
        return sub.visit(this);
      }

      private boolean add(final Scope var) {
        for(int i = 0; i < scopes.size(); i++) {
          if(scopes.get(i) == var) {
            if(!adj.contains(i)) adj.add(i);
            return true;
          }
        }
        adj.add(scopes.size());
        scopes.add(var);
        adjacent.add(null);
        return true;
      }
    });
  }
}

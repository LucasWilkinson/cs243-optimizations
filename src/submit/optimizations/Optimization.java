package submit.optimizations;

import joeq.Main.Helper;
import joeq.Class.*;
import joeq.Compiler.Quad.*;

public abstract class Optimization implements ControlFlowGraphVisitor {
    public boolean modifiedFlowGraph = false;
    public abstract void visitCFG(ControlFlowGraph cfg);
    public boolean optimizeClass(jq_Class c) {
        modifiedFlowGraph = false;
        Helper.runPass(c, this);
        return modifiedFlowGraph;
    }
}

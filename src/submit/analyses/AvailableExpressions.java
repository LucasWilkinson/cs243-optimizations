package submit.analyses;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;

import submit.analyses.AnticipatedExpressions;
import submit.analyses.AnticipatedExpressions.*;

/**
 * Skeleton class for implementing a reaching definition analysis
 * using the Flow.Analysis interface.
 */
public class AvailableExpressions implements Flow.Analysis {

    /**
     * Class for the dataflow objects in the ReachingDefs analysis.
     * You are free to change this class or move it to another file.
     */
    public static Set<String> universalSet = new TreeSet<String>();
    public class AvailableSet implements Flow.DataflowObject {
        private Set<String> set;
        /**
         * Methods from the Flow.DataflowObject interface.
         * See Flow.java for the meaning of these methods.
         * These need to be filled in.
         */
        public AvailableSet()
        {
            set =  new TreeSet<String>(universalSet);
        }
        public void setToTop() 
        {
            set = new TreeSet<String>(universalSet);
        }
        public void setToBottom() 
        {
            set = new TreeSet<String>();
        }
        
        /**
         * Meet is a union
         */
        public void meetWith (Flow.DataflowObject o) 
        {
            AvailableSet t = (AvailableSet)o;
            this.set.retainAll(t.set);
        }
        public void copy (Flow.DataflowObject o) 
        {
            AvailableSet t = (AvailableSet)o;
            set = new TreeSet<String>(t.set);
        }

        @Override
        public boolean equals(Object o) 
        {
            if (o instanceof AvailableSet) 
            {
                AvailableSet a = (AvailableSet) o;
                return set.equals(a.set);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return set.hashCode();
        }

        public Set<String> getSet() {
            return set;
        }

        /**
         * toString() method for the dataflow objects which is used
         * by postprocess() below.  The format of this method must
         * be of the form "[ID0, ID1, ID2, ...]", where each ID is
         * the identifier of a quad defining some register, and the
         * list of IDs must be sorted.  See src/test/test.rd.out
         * for example output of the analysis.  The output format of
         * your reaching definitions analysis must match this exactly.
         */
        @Override
        public String toString() { return set.toString(); }
        public void genExpression(String v) { set.add(v); }
        public void genExpressions(Set<String> v) { set.addAll(v); }
        public void killExpression(String v) { set.remove(v); }
        public void killExpressions(Set<String> v) { set.removeAll(v); }
    }

    /**
     * Dataflow objects for the interior and entry/exit points
     * of the CFG. in[ID] and out[ID] store the entry and exit
     * state for the input and output of the quad with identifier ID.
     *
     * You are free to modify these fields, just make sure to
     * preserve the data printed by postprocess(), which relies on these.
     */
    private AvailableSet[] in, out;
    private AvailableSet entry, exit;
    private static HashMap<String, TreeSet<String>> killSets;
    private static AnticipatedExpressions anticipated;

    public static boolean isValidExpression(Quad q) {
        return AnticipatedExpressions.isValidExpression(q);
    }

    public static String expressionString(Quad q) {
        return AnticipatedExpressions.expressionString(q);
    }

    public void registerAnticipated(AnticipatedExpressions anticipated){
        this.anticipated = anticipated;
    }

    /**
     * This method initializes the datflow framework.
     *
     * @param cfg  The control flow graph we are going to process.
     */
    public void preprocess(ControlFlowGraph cfg) {
        // this line must come first.
        System.out.println("Method: "+cfg.getMethod().getName().toString());
        killSets = new HashMap<String, TreeSet<String> >();

        // get the amount of space we need to allocate for the in/out arrays.
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int id = qit.next().getID();
            if (id > max) 
                max = id;
        }
        max += 1;

        // allocate the in and out arrays.
        in = new AvailableSet[max];
        out = new AvailableSet[max];

        // initialize the contents of in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new AvailableSet();
            out[id] = new AvailableSet();
        }

        qit = new QuadIterator(cfg);
        while (qit.hasNext())
        {
            Quad q = (Quad)qit.next();

            if (isValidExpression(q)){
                System.out.println(expressionString(q));
                String exprString = expressionString(q);

                universalSet.add(exprString);

                for (RegisterOperand def : q.getUsedRegisters()) 
                {
                    String key = def.getRegister().toString();
                    if(killSets.get(key) == null)  killSets.put(key, new TreeSet<String>());
                    TreeSet<String> killSet = killSets.get(key);
                    killSet.add(exprString);
                    killSets.put(key, killSet);
                }
            }
        }
        
        // initialize the entry and exit points.
        transferfn.val = new AvailableSet();
        entry = new AvailableSet();
        exit = new AvailableSet();

        // Set boundary condition
        entry.setToBottom(); 
    }

    /**
     * This method is called after the fixpoint is reached.
     * It must print out the dataflow objects associated with
     * the entry, exit, and all interior points of the CFG.
     * Unless you modify in, out, entry, or exit you shouldn't
     * need to change this method.
     *
     * @param cfg  Unused.
     */
    public void postprocess (ControlFlowGraph cfg) {
        System.out.println("entry: " + entry.toString());
        for (int i=1; i<in.length; i++) {
            if (in[i] != null) {
                System.out.println(i + " in:  " + in[i].toString());
                System.out.println(i + " out: " + out[i].toString());
            }
        }
        System.out.println("exit: " + exit.toString());
    }

    /**
    * Other methods from the Flow.Analysis interface.
    * See Flow.java for the meaning of these methods.
    * These need to be filled in.
    */
    public boolean isForward () { return true; }

    public Flow.DataflowObject getEntry() { 
        Flow.DataflowObject result = newTempVar();
        result.copy(entry); 
        return result;
    }
    public Flow.DataflowObject getExit() {
        Flow.DataflowObject result = newTempVar();
        result.copy(exit); 
        return result;
    }
    public void setEntry(Flow.DataflowObject value) {
        entry.copy(value);
    }
    public void setExit(Flow.DataflowObject value) {
        exit.copy(value);
    }
    public Flow.DataflowObject getIn(Quad q) { 
        Flow.DataflowObject result = newTempVar();
        result.copy(in[q.getID()]); 
        return result;
    }
    public Flow.DataflowObject getOut(Quad q) { 
        Flow.DataflowObject result = newTempVar();
        result.copy(out[q.getID()]); 
        return result;
    }
    public void setIn(Quad q, Flow.DataflowObject value) {
        in[q.getID()].copy(value);
    }
    public void setOut(Quad q, Flow.DataflowObject value) {
        out[q.getID()].copy(value); 
    }
    public Flow.DataflowObject newTempVar() { return new AvailableSet(); }

    private TransferFunction transferfn = new TransferFunction ();
    public void processQuad(Quad q) {
        transferfn.val.copy(in[q.getID()]);
        transferfn.visitQuad(q);
        out[q.getID()].copy(transferfn.val);
    }
    
    /* The QuadVisitor that actually does the computation */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        AvailableSet val;
        @Override
        public void visitQuad(Quad q) {

            AnticipatedSet anticipatedSet = (AnticipatedSet) anticipated.getIn(q);
            Set<String> anticipatedExprs = anticipatedSet.getSet();

            val.genExpressions(anticipatedExprs);

            if (q.getDefinedRegisters() != null)
            {
                for (RegisterOperand def : q.getDefinedRegisters()) 
                {
                    String key = def.getRegister().toString();
                    Set<String> killSet = killSets.get(key);
                    if (killSet != null){
                        val.killExpressions(killSet);
                    }
                }
            }
        }
    }
}

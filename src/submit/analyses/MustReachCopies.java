package submit.analyses;

import joeq.Compiler.Quad.*;
import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;

public class MustReachCopies implements Flow.Analysis {

    /**
     * Class for the dataflow objects in the ReachingDefs analysis.
     * You are free to change this class or move it to another file.
     */
    public static Set<Integer> universalSet = new TreeSet<Integer>();
    public class MustReachCopyFlowObject implements Flow.DataflowObject {
        private Set<Integer> set;
        /**
         * Methods from the Flow.DataflowObject interface.
         * See Flow.java for the meaning of these methods.
         * These need to be filled in.
         */
        public MustReachCopyFlowObject()
        {
            set = new TreeSet<Integer>(universalSet);
        }
        public void setToTop() 
        {
            set = new TreeSet<Integer>(universalSet);
        }
        public void setToBottom() 
        {
            set = new TreeSet<Integer>();
        }
        
        /**
         * Meet is a intersection
         */
        public void meetWith(Flow.DataflowObject o) 
        {
            MustReachCopyFlowObject t = (MustReachCopyFlowObject)o;
            this.set.retainAll(t.set);
        }
        public void copy(Flow.DataflowObject o) 
        {
            MustReachCopyFlowObject t = (MustReachCopyFlowObject)o;
            set = new TreeSet<Integer>(t.set);
        }

        /**
         * To be used by optomization 
         */ 
        public Set<Integer> intersection(Set<Integer> s)
        {
            Set<Integer> t = new TreeSet<Integer>(set);
            t.retainAll(s);
            return t;
        }

        @Override
        public boolean equals(Object o) 
        {
            if (o instanceof MustReachCopyFlowObject) 
            {
                MustReachCopyFlowObject a = (MustReachCopyFlowObject) o;
                return set.equals(a.set);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return set.hashCode();
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
        public void genCopy(Integer copy) { set.add(copy); }
        public void killCopy(Integer copy) { set.remove(copy); }
        public void killCopies(Set<Integer> copies) { set.removeAll(copies); }
    }

    /**
     * Dataflow objects for the interior and entry/exit points
     * of the CFG. in[ID] and out[ID] store the entry and exit
     * state for the input and output of the quad with identifier ID.
     *
     * You are free to modify these fields, just make sure to
     * preserve the data printed by postprocess(), which relies on these.
     */
    private MustReachCopyFlowObject[] in, out;
    private MustReachCopyFlowObject entry, exit;
    private static HashMap<String, TreeSet<Integer>> killSets;

    public static boolean isValidCopy(Quad q){
        return q.getOperator() instanceof Operator.Move 
            && Operator.Move.getSrc(q) instanceof RegisterOperand
            && Operator.Move.getDest(q) instanceof RegisterOperand;
    }

    /**
     * This method initializes the datflow framework.
     *
     * @param cfg  The control flow graph we are going to process.
     */
    public void preprocess(ControlFlowGraph cfg) {
        // this line must come first.
        // System.out.println("Method: "+cfg.getMethod().getName().toString());

        // Build the universal set and kill sets
        killSets = new HashMap<String, TreeSet<Integer> >();
        QuadIterator qit = new QuadIterator(cfg);
        while (qit.hasNext())
        {
            Quad q = (Quad)qit.next();
            int id = q.getID();

            if (isValidCopy(q)) {

                universalSet.add(id);

                for (RegisterOperand def : q.getUsedRegisters()) 
                {
                    String key = def.getRegister().toString();
                    if(killSets.get(key) == null)  killSets.put(key, new TreeSet<Integer>());
                    TreeSet<Integer> killSet = killSets.get(key);
                    killSet.add(id);
                    killSets.put(key, killSet);
                }

                for (RegisterOperand def : q.getDefinedRegisters()) 
                {
                    String key = def.getRegister().toString();
                    if(killSets.get(key) == null)  killSets.put(key, new TreeSet<Integer>());
                    TreeSet<Integer> killSet = killSets.get(key);
                    killSet.add(id);
                    killSets.put(key, killSet);
                }
            }
        }

        // get the amount of space we need to allocate for the in/out arrays.
        qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int id = qit.next().getID();
            if (id > max) 
                max = id;
        }
        max += 1;

        // allocate the in and out arrays.
        in = new MustReachCopyFlowObject[max];
        out = new MustReachCopyFlowObject[max];

        // initialize the contents of in and out.
        qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new MustReachCopyFlowObject();
            out[id] = new MustReachCopyFlowObject();
        }

        // initialize the entry and exit points.
        transferfn.val = new MustReachCopyFlowObject();
        
        entry = new MustReachCopyFlowObject();
        exit = new MustReachCopyFlowObject();

        entry.setToBottom(); // Boundary condition
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
        //System.out.println("entry: " + entry.toString());
        //for (int i=1; i<in.length; i++) {
        //    if (in[i] != null) {
        //        System.out.println(i + " in:  " + in[i].toString());
        //        System.out.println(i + " out: " + out[i].toString());
        //    }
        //}
        //System.out.println("exit: " + exit.toString());
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
    public Flow.DataflowObject newTempVar() { return new MustReachCopyFlowObject(); }

    private TransferFunction transferfn = new TransferFunction ();
    public void processQuad(Quad q) {
        transferfn.val.copy(in[q.getID()]);
        transferfn.visitQuad(q);
        out[q.getID()].copy(transferfn.val);
    }
    
    /* The QuadVisitor that actually does the computation */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        MustReachCopyFlowObject val;
        @Override
        public void visitQuad(Quad q) {

            for (RegisterOperand def : q.getDefinedRegisters()) {
                Set<Integer> killSet = killSets.get(def.getRegister().toString());
                if (killSet != null) {
                    val.killCopies(killSet);
                }
            }

            if (isValidCopy(q)){
                val.genCopy(q.getID()); // Ignore const in this analysis
            }
        }
    }
}
package submit.analyses;

// some useful things to import. add any additional imports you need.
import joeq.Compiler.Quad.*;
import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.Operand.RegisterOperand;
/**
 * Skeleton class for implementing a reaching definition analysis
 * using the Flow.Analysis interface.
 */
public class MustReachGetField implements Flow.Analysis {

    /**
     * Class for the dataflow objects in the ReachingDefs analysis.
     * You are free to change this class or move it to another file.
     */
    /**
     * Class for the dataflow objects in the ReachingDefs analysis.
     * You are free to change this class or move it to another file.
     */
    public static Set<Integer> universalSet = new TreeSet<Integer>();
    public class GetfieldSet implements Flow.DataflowObject {
        private Set<Integer> set;
        /**
         * Methods from the Flow.DataflowObject interface.
         * See Flow.java for the meaning of these methods.
         * These need to be filled in.
         */
        public GetfieldSet()
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
            GetfieldSet t = (GetfieldSet)o;
            this.set.retainAll(t.set);
        }
        public void copy(Flow.DataflowObject o) 
        {
            GetfieldSet t = (GetfieldSet)o;
            set = new TreeSet<Integer>(t.set);
        }

        /**
         * To be used by optomization 
         */ 

        public Set<Integer> getSet()
        {   
            return new TreeSet<Integer>(set);
        }

        public Set<Integer> intersection(Set<Integer> s)
        {
            Set<Integer> t = new TreeSet<Integer>(set);
            t.retainAll(s);
            return t;
        }

        @Override
        public boolean equals(Object o) 
        {
            if (o instanceof GetfieldSet) 
            {
                GetfieldSet a = (GetfieldSet) o;
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
        public void genGetfield(Integer gf) { set.add(gf); }
        public void killGetfield(Integer gf) { set.remove(gf); }
        public void killGetfields(Set<Integer> gf) { set.removeAll(gf); }
    }

    /**
     * Dataflow objects for the interior and entry/exit points
     * of the CFG. in[ID] and out[ID] store the entry and exit
     * state for the input and output of the quad with identifier ID.
     *
     * You are free to modify these fields, just make sure to
     * preserve the data printed by postprocess(), which relies on these.
     */
    private GetfieldSet[] in, out;
    private GetfieldSet entry, exit;
    private static HashMap<String, TreeSet<Integer>> killSets;

    public static boolean isGetField(Quad q) {
        return (q.getOperator() instanceof Operator.Getfield);
    }

    public static String getFieldString(Quad q) {
        if (isGetField(q)) {
            return q.getOperator().toString() 
                + Operator.Getfield.getBase(q).toString()
                + Operator.Getfield.getField(q).toString();
        } else {
            throw new RuntimeException("Invalid getfield quad");
        }
    }

    /**
     * This method initializes the datflow framework.
     *
     * @param cfg  The control flow graph we are going to process.
     */
    public void preprocess(ControlFlowGraph cfg) {
        // this line must come first.
        //System.out.println("Method: "+cfg.getMethod().getName().toString());
        killSets = new HashMap<String, TreeSet<Integer> >();

        // allocate the in and out arrays.
        in = new GetfieldSet[cfg.getMaxQuadID() + 1];
        out = new GetfieldSet[cfg.getMaxQuadID() + 1];

        // initialize the contents of in and out.
        QuadIterator qit = new QuadIterator(cfg);
        while (qit.hasNext()) {
            int id = qit.next().getID();
            in[id] = new GetfieldSet();
            out[id] = new GetfieldSet();
        }

        qit = new QuadIterator(cfg);
        while (qit.hasNext())
        {
            Quad q = (Quad)qit.next();

            if (isGetField(q)){
                String fieldString = getFieldString(q);

                universalSet.add(q.getID());

                String key = Operator.Getfield.getBase(q).toString();
                if(killSets.get(key) == null)  killSets.put(key, new TreeSet<Integer>());
                TreeSet<Integer> killSet = killSets.get(key);
                killSet.add(q.getID());

                key = Operator.Getfield.getDest(q).toString();
                if(killSets.get(key) == null)  killSets.put(key, new TreeSet<Integer>());
                killSet = killSets.get(key);
                killSet.add(q.getID());
            }
        }
        
        // initialize the entry and exit points.
        transferfn.val = new GetfieldSet();
        entry = new GetfieldSet();
        exit = new GetfieldSet();

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
        if (q == null){
            System.out.println("Quad " + q);
        }

        if (out[q.getID()] == null){
            System.out.println("out[q] " + out[q.getID()] + " id " + q.getID());
        }

        out[q.getID()].copy(value); 
    }
    public Flow.DataflowObject newTempVar() { return new GetfieldSet(); }

    private TransferFunction transferfn = new TransferFunction ();
    public void processQuad(Quad q) {
        transferfn.val.copy(in[q.getID()]);
        transferfn.visitQuad(q);
        out[q.getID()].copy(transferfn.val);
    }
    
    /* The QuadVisitor that actually does the computation */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor {
        GetfieldSet val;
        @Override
        public void visitQuad(Quad q) {

            if (q.getDefinedRegisters() != null)
            {
                for (RegisterOperand def : q.getDefinedRegisters()) 
                {
                    String key = def.toString();
                    Set<Integer> killSet = killSets.get(key);
                    if (killSet != null){
                        val.killGetfields(killSet);
                    }
                }
            }

            if (isGetField(q)) {
                val.genGetfield(q.getID());
            }else if(q.getOperator() instanceof Operator.Invoke){
                val.setToBottom();
            }
        }
    }
}

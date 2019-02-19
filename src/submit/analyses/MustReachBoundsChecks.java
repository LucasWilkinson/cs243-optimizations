package submit.analyses;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.*;
import joeq.Main.Helper;

public class MustReachBoundsChecks implements Flow.Analysis {

    public static class CheckedArraySet implements Flow.DataflowObject {
        private Set<String> set;

        public CheckedArraySet() { set = new TreeSet<String>() }

        public void setToTop() { set = new TreeSet<String>() }
        public void setToBottom() { set = new TreeSet<String>(universalSet) }

        public void meetWith (Flow.DataflowObject o) {
            CheckedArraySet a = (CheckedArraySet)o;
            
            this.set.addAll(a.set);
        }

        public void copy (Flow.DataflowObject o) {
            CheckedArraySet a = (CheckedArraySet) o;
			set = new TreeSet<String>(a.set);
        }
    }

    public static class CheckerSet implements Flow.DataflowObject {
        private SortedMap<String, CheckedArraySet> map;

        /* 'core' is used to keep track of which variables we need to
         * track */
        private static Set<String> core = new HashSet<String>();
        public static void reset() { core.clear(); }
        public static void register(String key) {
            core.add(key);
        }

        public CheckerSet() 
        {
            map = new TreeMap<String, CheckedArraySet>();
            for (String key : core) {
                map.put(key, new CheckedArraySet());
            }
        }

        public void setToTop() 
        {
        		core = new HashSet<String>(universalSet);
        		
            for (String key : core) {
                map.get(key).setToTop();
            }
        }

        public void setToBottom() 
        {
            core = new HashSet<String>();
        		
            for (String key : core) {
                map.get(key).setToBottom();
            }
        }

        public void meetWith(Flow.DataflowObject o) 
        {
            CheckerSet a = (CheckerSet) o;
            
            this.core.retainAll(a.core);	
        }

        public void copy (Flow.DataflowObject o) {
            ConstantPropTable a = (ConstantPropTable) o;
            for (Map.Entry<String, SingleCP> e : a.map.entrySet()) {
                SingleCP mine = map.get(e.getKey());
                mine.copy(e.getValue());
            }		
        }

        @Override
        public String toString() {
            return map.toString();
        }

        public SingleCP get(String key) {
            return map.get(key);
        }

        @Override
        public boolean equals (Object o) {
            if (o instanceof ConstantPropTable) {
                return map.equals (((ConstantPropTable)o).map);
            }
            return false;
        }
        @Override
        public int hashCode() {
            return map.hashCode();
        }

        public void setUndef(String key) {
            get(key).setUndef();
        }
        public void setConst(String key, int val) {
            get(key).setConst(val);
        }
        public void setNAC(String key) {
            get(key).setNAC();
        }
        public void transfer(String key, String src) {
            get(key).copy(get(src));
        }
    }

    private ConstantPropTable[] in, out;
    private ConstantPropTable entry, exit;
    
    public QuadInterpreter qi;

    public void preprocess (ControlFlowGraph cfg) {
        //System.out.println("Method: "+cfg.getMethod().getName().toString());
        /* Generate initial conditions. */
        QuadIterator qit = new QuadIterator(cfg);
        int max = 0;
        while (qit.hasNext()) {
            int x = qit.next().getID();
            if (x > max) max = x;
        }
        max += 1;
        in = new ConstantPropTable[max];
        out = new ConstantPropTable[max];
        qit = new QuadIterator(cfg);

        ConstantPropTable.reset();

        /* Arguments are always there. */
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            ConstantPropTable.register("R"+i);
        }

        while (qit.hasNext()) {
            Quad q = qit.next();
            for (RegisterOperand def : q.getDefinedRegisters()) {
                ConstantPropTable.register(def.getRegister().toString());
            }
            for (RegisterOperand use : q.getUsedRegisters()) {
                ConstantPropTable.register(use.getRegister().toString());
            }
        }

        entry = new ConstantPropTable();
        exit = new ConstantPropTable();
        transferfn.val = new ConstantPropTable();
        for (int i=0; i<in.length; i++) {
            in[i] = new ConstantPropTable();
            out[i] = new ConstantPropTable();
        }

        for (int i=0; i < numargs; i++) {
            entry.setNAC("R"+i);
        }
        
        qi = new QuadInterpreter(cfg.getMethod());
        
        //System.out.println("Initialization completed.");
    }

    public void postprocess (ControlFlowGraph cfg) {
    		/*
        System.out.println("entry: "+entry.toString());
        for (int i=0; i<in.length; i++) {
            System.out.println(i+" in:  "+in[i].toString());
            System.out.println(i+" out: "+out[i].toString());
        }
        System.out.println("exit: "+exit.toString());
        */
    }

    /* Is this a forward dataflow analysis? */
    public boolean isForward () { return true; }

    /* Routines for interacting with dataflow values. */

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
    public void setEntry(Flow.DataflowObject value) { 
        entry.copy(value); 
    }
    public void setExit(Flow.DataflowObject value) { 
        exit.copy(value); 
    }

    public Flow.DataflowObject newTempVar() { return new ConstantPropTable(); }

    /* Actually perform the transfer operation on the relevant
     * quad. */

    private TransferFunction transferfn = new TransferFunction ();
    public void processQuad(Quad q) {
        transferfn.val.copy(in[q.getID()]);
        Helper.runPass(q, transferfn);
        out[q.getID()].copy(transferfn.val);
    }

    /* The QuadVisitor that actually does the computation */
    public static class TransferFunction extends QuadVisitor.EmptyVisitor
    {
        ConstantPropTable val;
        @Override
        public void visitMove (Quad q) {
            Operand op = Operator.Move.getSrc(q);
            String key = Operator.Move.getDest(q).getRegister().toString();

            if (isUndef(op)) {
                val.setUndef(key);
            } else if (isConst(op)) {
                val.setConst(key, getConst(op));
            } else {
                val.setNAC(key);
            }
        }
        @Override
        public void visitBinary (Quad q) {
            Operand op1 =  Operator.Binary.getSrc1(q);
            Operand op2 =  Operator.Binary.getSrc2(q);
            String key =   Operator.Binary.getDest(q).getRegister().toString();
            Operator opr = q.getOperator();

			
            if ((opr == Operator.Binary.ADD_I.INSTANCE) ||
            	    (opr == Operator.Binary.SUB_I.INSTANCE) ||
            	    (opr == Operator.Binary.MUL_I.INSTANCE) || 
            	    (opr == Operator.Binary.DIV_I.INSTANCE) || 
            	    (opr == Operator.Binary.REM_I.INSTANCE) ||
            	    (opr == Operator.Binary.SHL_I.INSTANCE))
            {
                if (isNAC(op1) || isNAC(op2)) 
                {
                    val.setNAC(key);
                } 
                else if (isUndef(op1) || isUndef(op2)) 
                {
                    val.setUndef(key);
                } 
                else 
                { // both must be constant!
                	   if (opr == Operator.Binary.ADD_I.INSTANCE)
                	   {
                    		val.setConst(key, getConst(op1)+getConst(op2));
                    }
                    else if (opr == Operator.Binary.SUB_I.INSTANCE)
                    {
                    		val.setConst(key, getConst(op1)-getConst(op2));
                    }
                    else if (opr == Operator.Binary.MUL_I.INSTANCE)
                    {
                    		val.setConst(key, getConst(op1)*getConst(op2));
                    }
                    else if (opr == Operator.Binary.DIV_I.INSTANCE)
                    {
                    		val.setConst(key, getConst(op1)/getConst(op2));
                    }
                    else if (opr == Operator.Binary.REM_I.INSTANCE)
                    {
                    		val.setConst(key, getConst(op1)%getConst(op2));
                    }
                    else if (opr == Operator.Binary.SHL_I.INSTANCE)
                    {
                    		val.setConst(key, getConst(op1)<<getConst(op2));
                    }
                }
            } 
            else 
            {
                val.setNAC(key);
            }
            
            
            /*
            if (isNAC(op1) || isNAC(op2)) 
            {
                val.setNAC(key);
            } 
            else if (isUndef(op1) || isUndef(op2)) 
            {
                val.setUndef(key);
            }
            else
            {
            		q.interpret(qi);
            		val.setConst(key, (Integer) qi.getReturnValue());
            }
            */        
        }
        @Override
        public void visitUnary (Quad q) {
            Operand op = Operator.Unary.getSrc(q);
            String key = Operator.Unary.getDest(q).getRegister().toString();
            Operator opr = q.getOperator();

            if (opr == Operator.Unary.NEG_I.INSTANCE) {
                if (isUndef(op)) {
                    val.setUndef(key);
                } else if (isConst(op)) {
                    val.setConst(key, -getConst(op));
                } else {
                    val.setNAC(key);
                }
            } else {
                val.setNAC(key);
            }
        }

        @Override
        public void visitALoad(Quad q) {
            String key = Operator.ALoad.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitALength(Quad q) {
            String key = Operator.ALength.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitGetstatic(Quad q) {
            String key = Operator.Getstatic.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitGetfield(Quad q) {
            String key = Operator.Getfield.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitInstanceOf(Quad q) {
            String key = Operator.InstanceOf.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitNew(Quad q) {
            String key = Operator.New.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitNewArray(Quad q) {
            String key = Operator.NewArray.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitInvoke(Quad q) {
            RegisterOperand op = Operator.Invoke.getDest(q);
            if (op != null) {
                String key = op.getRegister().toString();
                val.setNAC(key);
            }
        }

        @Override
        public void visitJsr(Quad q) {
            String key = Operator.Jsr.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        @Override
        public void visitCheckCast(Quad q) {
            String key = Operator.CheckCast.getDest(q).getRegister().toString();
            val.setNAC(key);
        }

        private boolean isUndef (Operand op) {
            return (op instanceof RegisterOperand && 
                    val.get(((RegisterOperand)op).getRegister().toString()).isUndef());
        }

        private boolean isConst (Operand op) {
            return (op instanceof IConstOperand) || 
            (op instanceof RegisterOperand && 
                    val.get(((RegisterOperand)op).getRegister().toString()).isConst());
        }

        private boolean isNAC (Operand op) {
            return (op instanceof RegisterOperand && 
                    val.get(((RegisterOperand)op).getRegister().toString()).isNAC());
        }

        private int getConst (Operand op) {
            if (op instanceof IConstOperand) {
                return ((IConstOperand)op).getValue();
            }
            if (op instanceof RegisterOperand) {
                SingleCP o = val.get(((RegisterOperand)op).getRegister().toString());
                if (o.state == 1)
                    return o.getConst();
            }
            throw new IllegalArgumentException("Tried to getConst a non-Const!");
        }
    }
}

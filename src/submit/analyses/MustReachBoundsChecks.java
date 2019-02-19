package submit.analyses;

import flow.Flow;
import java.util.*;
import joeq.Compiler.Quad.*;
import joeq.Compiler.Quad.Operand.*;
import joeq.Main.Helper;

public class MustReachBoundsChecks implements Flow.Analysis {

    public static class CheckedArraySet implements Flow.DataflowObject {
        private Set<String> set;
        private static Set<String> universalSet;

        public CheckedArraySet() 
        { 
        		set = new TreeSet<String>(universalSet); 
        	}

        public void setToTop() { set = new TreeSet<String>(universalSet); };
        public void setToBottom() { set = new TreeSet<String>(); };

        public void meetWith (Flow.DataflowObject o) 
        {
            CheckedArraySet a = (CheckedArraySet)o;
            
            set.retainAll(a.set);
        }

        public void copy (Flow.DataflowObject o) 
        {
            CheckedArraySet a = (CheckedArraySet) o;
			set = new TreeSet<String>(a.set);
        }
        
        public boolean equals (Object o)
        {
        		CheckedArraySet a = (CheckedArraySet) o;
        		return set.equals(a.set);
        }
        
       public void set(String s) 
       { 
       		set.add(s);
       }
       
       public void kill(String s)
       {
       		set.remove(s);
       }
       
       public boolean hasArray(String s)
       {
       		return set.contains(s);
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
            for (String key : core) {
                map.get(key).setToTop();
            }
        }

        public void setToBottom() 
        {
            for (String key : core) {
                map.get(key).setToBottom();
            }
        }

        public void meetWith(Flow.DataflowObject o) 
        {
            CheckerSet a = (CheckerSet) o;
           	
            for (Map.Entry<String, CheckedArraySet> e : a.map.entrySet()) 
            {
                CheckedArraySet mine = map.get(e.getKey());
                mine.meetWith(e.getValue());
            }	 
           
        }

        public void copy (Flow.DataflowObject o) 
        {
       	 	CheckerSet a = (CheckerSet) o;
       	 	
            for (Map.Entry<String, CheckedArraySet> e : a.map.entrySet()) 
            {
                CheckedArraySet mine = map.get(e.getKey());
                mine.copy(e.getValue());
            }	
        }
        
        public boolean equals (Object o) 
        {
        		if (o instanceof CheckerSet) 
        		{
        			return map.equals (((CheckerSet)o).map);
        		}
        		return false;
        }

        @Override
        public String toString() {
            return map.toString();
        }

        public CheckedArraySet get(String key) 
        {
            return map.get(key);
        }
        
        public void addArray(String key, String s) 
        {
            get(key).set(s);
        }
        
        public void removeArray(String key, String s)
        {
        		get(key).kill(s);
        }
        
        public void removeArrayFromMap(String s)
        {
        		for (Map.Entry<String, CheckedArraySet> e : map.entrySet()) 
            {
                CheckedArraySet mine = map.get(e.getKey());
                mine.kill(s);
            }	
        }

		public void killChecker(String key) 
		{
			get(key).setToTop();
		}
       
        @Override
        public int hashCode() 
        {
            return map.hashCode();
        }

    }

    private CheckerSet[] in, out;
    private CheckerSet entry, exit;
 
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
        in = new CheckerSet[max];
        out = new CheckerSet[max];
        qit = new QuadIterator(cfg);

        CheckerSet.reset();
	
		Set<String> s = new TreeSet<String>();
		CheckedArraySet.universalSet = s;
		
        int numargs = cfg.getMethod().getParamTypes().length;
        for (int i = 0; i < numargs; i++) {
            CheckerSet.register("R"+i);
            s.add("R"+i);
        }
		
        while (qit.hasNext()) 
        {
            Quad q = qit.next();
            for (Operand def : q.getAllOperands())
            {
            		if(def instanceof RegisterOperand)
            		{
            			RegisterOperand regdef = (RegisterOperand) def;
            			CheckerSet.register(regdef.getRegister().toString());
            		}
            		else if(def instanceof IConstOperand)
            		{
            			IConstOperand constdef = (IConstOperand) def;
            			Integer c = constdef.getValue();
            			CheckerSet.register(c.toString());
            		}
            		else if(def instanceof AConstOperand)
            		{
            			AConstOperand aconstdef = (AConstOperand) def;
            			if (aconstdef.getWrapped() != null)
            			{
	            			String a = aconstdef.getWrapped().toString();
	            			s.add(a);
	            		}
            		}
            	}
            	
            	if(q.getOperator() instanceof Operator.NewArray)
            	{
            		RegisterOperand reg = Operator.NewArray.getDest(q);
            		s.add(reg.getRegister().toString());
            	}
        }
		
        entry = new CheckerSet();
        exit = new CheckerSet();
        transferfn.val = new CheckerSet();
        for (int i=0; i<in.length; i++) {
            in[i] = new CheckerSet();
            out[i] = new CheckerSet();
        }
        
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
        //System.out.println("Finished Bounds Check!");
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

    public Flow.DataflowObject newTempVar() { return new CheckerSet(); }

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
        CheckerSet val;
        
        public void visitQuad(Quad q) 
        {
        		if(q.getOperator() instanceof Operator.BoundsCheck)
        		{
        			Operand array = Operator.BoundsCheck.getRef(q);
        			Operand index = Operator.BoundsCheck.getIndex(q);
        			
        			if(array instanceof RegisterOperand)
        			{
        				RegisterOperand regarray = (RegisterOperand) array;
        				
        				if (index instanceof RegisterOperand)
        				{
        					RegisterOperand regindex = (RegisterOperand) index;
        					val.addArray(regindex.getRegister().toString(), regarray.getRegister().toString());
        				}
        				else if (index instanceof IConstOperand)
        				{
        					IConstOperand constindex = (IConstOperand) index;
        					Integer c = constindex.getValue();
        					val.addArray(c.toString(), regarray.getRegister().toString());
        				}
        			}
        			else if(array instanceof AConstOperand)
        			{
        				AConstOperand constarray = (AConstOperand) array;
        				
        				if (constarray.getWrapped() != null)
        				{
	        				if (index instanceof RegisterOperand)
	        				{
	        				    RegisterOperand regindex = (RegisterOperand) index;
	        					val.addArray(regindex.getRegister().toString(), constarray.getWrapped().toString());
	        				}
	        				else if (index instanceof IConstOperand)
	        				{
	        					IConstOperand constindex = (IConstOperand) index;
	        					Integer c = constindex.getValue();
	        					val.addArray(c.toString(), constarray.getWrapped().toString());
	        				}
	        			}
        			}
        		}
        		else if(q.getOperator() instanceof Operator.NewArray)
        		{
        			RegisterOperand reg = Operator.NewArray.getDest(q);
        			
        			val.removeArrayFromMap(reg.getRegister().toString());
        		}
        		else
        		{
        			//kill the defined reg if they are checkers
        			for (RegisterOperand def : q.getDefinedRegisters()) 
                {
                    String key = def.getRegister().toString();
                    
   				    val.killChecker(key);
                }
        		}
        }
	}
}

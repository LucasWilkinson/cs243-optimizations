package test;

public class ConstantTest {

    private static int run() {
    	
    		int a, b, c, d, e, f, g;
    		
      	a = 3;
      	
      	b = a + 3;
      	
      	c = b + 5;
      	
      	d = a + 10;
      	
      	e = b + 7;
      	
      	f = a + b;
      	
      	g = b + f;
      	
      	b = g + d;
      	
      	return b;
    }

    public static void main(String[] args) {
    
		int result = run();
		
		if (result == 28)
		{
			System.out.println("Fuck Yeah!");
		}
    }
}

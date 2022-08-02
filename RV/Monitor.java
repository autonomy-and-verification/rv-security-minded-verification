
import java.io.IOException;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Set;
import java.util.HashSet;
import java.io.FileReader;
import java.util.Map;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class Monitor {
    private String[] alphabet;
    private HashMap<String, State> states = new HashMap<>();
    private State currentState;
    private Verdict currentVerdict = Verdict.Unknown;
    private String ltl;
    private String monitor;
    private int violations = 0;
    private int events = 0;
    private static int totalviolations = 0;
    private static int totalevents = 0;
    private static int queueLimit = 0;
//    final int queueLimit = 125;
//    final int queueLimit = 71;
    public static String rv;

    public static void main(String[] args) throws IOException {
		String ltl = "Gp";
		String ltlAlphabet = "[p,q]";
//		String log = "secp256-2cars.txt";
//		String log = "secp256-50cars.txt";
//		String log = "secp192-2cars.txt";
//		String log = "secp192-50cars.txt";
		queueLimit = Integer.parseInt(args[2]);
		int numberCars = Integer.parseInt(args[3]);
		Monitor [] monitors = new Monitor [numberCars];
		for (int i = 0; i < numberCars; i++) {
			monitors[i] = new Monitor(args[0], ""+i, ltl, ltlAlphabet);
			monitors[i].verifyLog(args[1]);
		}
		System.out.println("The total number of events: "+totalevents);
		System.out.println("The total number of violations: "+totalviolations);
//		Monitor mon1 = new Monitor(args[0], ltl, ltlAlphabet);

        //System.out.println(mon1);
//        System.out.println("Verdict = "+mon1.next("p"));
 //       System.out.println("Verdict = "+mon1.next("p"));
        
        //System.out.println(mon1.verifyLog("/home/cpscsc/s01rc2/lamaconv/log-pruned.txt"));
        
//        mon1.verifyLog(args[1]);
        
    }

    private static class State {
        private String name;
        private HashMap<String, State> transitions = new HashMap<>();
        private Verdict output;
        private State(String name, Verdict output) {
            this.name = name;
            this.output = output;
        }
    }
    public static enum Verdict { True, False, Unknown, GiveUp };

    public String getLtl() {
        return ltl;
    }

    public Verdict getCurrentVerdict() {
        return this.currentVerdict;
    }
    
    public Verdict verifyLog(String file) {
		try (BufferedReader br = new BufferedReader(new FileReader(file))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (!line.isEmpty() && !line.trim().equals("") && !line.trim().equals("\n")) {
					String carId = line.substring(line.indexOf(": ")+2, line.indexOf(" : Message"));
					if (line.contains("pushed into the Queue") && this.monitor.equals(carId)) {
						events++;
						String value = line.substring(line.indexOf("Queue(")+6, line.indexOf(")"));
						String time = line.substring(0,line.indexOf(" : "));
						String messageId = line.substring(line.indexOf(",")+1, line.indexOf(">"));
						String carIdSender = line.substring(line.indexOf("Message<")+8, line.indexOf(","));
						if (Integer.parseInt(value) > queueLimit) {
							this.next("q");
//							System.out.println("At time "+time+" car with ID "+carId+" violated the property, current queue of "+value+" is greater than the limit of "+queueLimit+". Message ID"+messageId+" was sent by car ID "+carIdSender);
//							violations++;
						}
						else {
							this.next("p");
						}
						if (this.currentVerdict == Verdict.False) {
							System.out.println("At time "+time+" car with ID "+carId+" violated the property, current queue of "+value+" is greater than the limit of "+queueLimit+". Message ID"+messageId+" was sent by car ID "+carIdSender);
							violations++;
						}
					}
				}
			}
			System.out.println("The total number of events for car with ID "+this.monitor+": "+events);
			System.out.println("The total number of violations is for car with ID "+this.monitor+": "+violations);
			totalevents = totalevents + events;
			totalviolations = totalviolations + violations;
			br.close();
		}
		 catch (IOException e) {
			e.printStackTrace();
		}
		return this.currentVerdict;
	}

    public Monitor(String lamaconvPath, String monitor, String ltl, String ltlAlphabet) throws IOException {
        this.ltl = "LTL=" + ltl.replace("and", "AND").replace("or", "OR").replace(" ", "");
        this.monitor = monitor;

        String command = "java -jar " + lamaconvPath + "/rltlconv.jar " + this.ltl + ",ALPHABET=" + ltlAlphabet + " --formula --nbas --min --nfas --dfas --min --moore";

        try(Scanner scanner = new Scanner(Runtime.getRuntime().exec(command).getInputStream()).useDelimiter("\n")) {
            while(scanner.hasNext()) {
                String mooreString = scanner.next();
                if(mooreString.contains("ALPHABET")) {
                    String[] alphabet = mooreString.split("=")[1].trim().replace("[", "").replace("]", "").split(",");
                    this.alphabet = new String[alphabet.length];
                    for(int i = 0; i < alphabet.length; i++) {
                        this.alphabet[i] = alphabet[i].replace("\"", "");
                    }
                } else if(mooreString.contains("STATES")) {
                    for(String state : mooreString.split("=")[1].split(",")) {
                        state = state.trim().replace("[", "").replace("]", "");
                        String name = state.split(":")[0];
                        String verdictStr = state.split(":")[1];
                        Verdict output = verdictStr.equals("true") ? Verdict.True : (verdictStr.equals("false") ? Verdict.False : Verdict.Unknown);
                        this.states.put(name, new State(name, output));
                    }
                } else if(mooreString.contains("START")) {
                    this.currentState = states.get(mooreString.split("=")[1].trim());
                } else if(mooreString.contains("DELTA")) {
                    String[] args = mooreString.substring(mooreString.indexOf("(")+1, mooreString.indexOf(")")).split(",");
                    this.states.get(args[0].trim()).transitions.put(args[1].trim().replace("\"", ""), states.get(mooreString.split("=")[1].trim()));
                }
            }
        }
        this.itIsOkToGiveUp();
    }

    @Override
    public String toString() {
        String res = "MOORE {\n";
        res += "\tALPHABET = [" + String.join(", ", this.alphabet) + "]\n";
        res += "\tSTATES = [";
        boolean first = true;
        for(Map.Entry<String, State> entry : this.states.entrySet()) {
            if(first) { first = false; }
            else { res += ", "; }
            res += entry.getKey() + ":" + (entry.getValue().output == Verdict.True ? "true" : (entry.getValue().output == Verdict.False ? "false" : (entry.getValue().output == Verdict.Unknown ? "?" : "x")));
        }
        res += "]\n";
        res += "\tSTART = " + this.currentState.name + "\n";
        for(Map.Entry<String, State> entry1 : this.states.entrySet()) {
            for(Map.Entry<String, State> entry2 : entry1.getValue().transitions.entrySet()) {
                res += "\tDELTA(" + entry1.getKey() + ", " + entry2.getKey() + ") = " + entry2.getValue().name + "\n";
            }
        }
        res += "}";
        return res;
    }

    private void itIsOkToGiveUp() {
        for(Map.Entry<String, State> entry : this.states.entrySet()) {
            if(entry.getValue().output == Verdict.Unknown && !canReachFinalVerdictState(entry.getValue())) {
                entry.getValue().output = Verdict.GiveUp;
            }
        }
    }

    private boolean canReachFinalVerdictState(State state) {
        Set<String> visited = new HashSet<String>();
        return canReachFinalVerdictStateAux(state, visited);
    }

    private boolean canReachFinalVerdictStateAux(State state, Set<String> visited) {
        if(visited.contains(state.name)) {
            return false;
        } else {
            visited.add(state.name);
            if(state.output == Verdict.True || state.output == Verdict.False) {
                return true;
            }
            for(Map.Entry<String, State> entry : state.transitions.entrySet()) {
                if(canReachFinalVerdictStateAux(entry.getValue(), visited)) {
                    return true;
                }
            }
            return false;
        }
    }

    public Verdict next(String event) {
        event = event.toLowerCase();
        if(currentState.transitions.containsKey(event)) {
            currentState = currentState.transitions.get(event);
            currentVerdict = currentState.output;
            return currentVerdict;
        } else if(currentState.transitions.containsKey("?")) {
            currentState = currentState.transitions.get("?");
            currentVerdict = currentState.output;
            return currentVerdict;
        }
        return currentVerdict;
    }
}

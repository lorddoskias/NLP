package hmmtagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 *
 * @author Nikolay Borisov
 */
public class HMMTagger {

    private static final int COUNT = 0;
    private static final int TYPE = 1;
    private static final int TOKEN_TYPE = 2;
    private static final int WORD = 3;
    private static final String WORDTAG = "WORDTAG";
    private static Map<String, Map<String, Double>> emissionParams = new HashMap<>();
    private static Map<String, Integer> wordCount = new HashMap<>();
    private static Map<String, Integer> ngramCount = new HashMap<>();
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        
        countWords(args[0]);
        teachTagger(args[0]);
        
        BufferedWriter out = new BufferedWriter(new FileWriter(args[1] + ".tagged"));
        BufferedReader in = new BufferedReader(new FileReader(args[1]));
        String line;
        
        while((line = in.readLine()) != null) {
            if (line.equals("")) { out.newLine(); continue;}
            
            out.write(line + " " + getMaxEmissionParameter(line));
            out.newLine();
        }
        
        
        out.close();
        in.close();
    }
    
    
    private static void teachTagger(String inputFile) throws IOException {
        
        String line;
        Map<String, Integer> tagCount = countTags(inputFile);

        //now we need to go through the file again and compute the 
        // emission parameters for each x|y
        BufferedReader in = new BufferedReader(new FileReader(inputFile));
        
        while((line = in.readLine()) != null) {
            
            String tokens[] = line.split(" ");
            if (tokens[TYPE].equals(WORDTAG)) {
                Map<String, Double> m;
                int countY = tagCount.get(tokens[TOKEN_TYPE]);
                int observationCount = Integer.parseInt(tokens[COUNT]);
                //added this to not overwrite a certain hashmap
                if (emissionParams.get(tokens[WORD]) != null) {
                    m = emissionParams.get(tokens[WORD]);
                } else {
                    m = new HashMap<>(1);
                }
                m.put(tokens[TOKEN_TYPE], ((double) observationCount / (double)countY));
                emissionParams.put(tokens[WORD], m);
            }
        }
        
        in.close();
    }
    
    private static Map<String, Integer> countTags(String input) throws FileNotFoundException, IOException {
        String line;
        Map<String, Integer> tagCount = new HashMap<>();

        BufferedReader in = new BufferedReader(new FileReader(input));
        //1. First preprocess the file to get count for each and every distinct tag 

        while ((line = in.readLine()) != null) {
            String tokens[] = line.split(" ");
            if (tokens[TYPE].equals(WORDTAG)) {
                if (tagCount.get(tokens[TOKEN_TYPE]) == null) {
                    tagCount.put(tokens[TOKEN_TYPE], Integer.parseInt(tokens[COUNT]));
                } else {
                    int oldCount = tagCount.get(tokens[TOKEN_TYPE]);
                    tagCount.put(tokens[TOKEN_TYPE],  oldCount + Integer.parseInt(tokens[COUNT]));
                }
            }
        }
        
        in.close();

        return tagCount;
    }
   
    private static void countWords(String input) throws FileNotFoundException, IOException {

        String line;
        BufferedReader in = new BufferedReader(new FileReader(input));

        while ((line = in.readLine()) != null) {
            String elements[] = line.split(" ");
            
            if (elements.length < 4) {  continue; /*ignore n-grams info */}
            
            if (wordCount.get(elements[3]) == null) {
                wordCount.put(elements[3], Integer.parseInt(elements[0]));
            } else {
                int count = wordCount.get(elements[3]);
                wordCount.put(elements[3], count + Integer.parseInt(elements[0]));
            }
        }
        in.close();
    }
    
    public static String getMaxEmissionParameter(String x) throws IOException {
        Map<String, Double> m;
        double bestScore = -1;
        String bestType = "";
        
        if (wordCount.get(x) == null || wordCount.get(x) < 5) {
            m = emissionParams.get("_RARE_");
        } else {
            m = emissionParams.get(x);
        }
        //find the highest scoring tag
        for (Entry<String, Double> candidate : m.entrySet()) {
            if (candidate.getValue() > bestScore) {
                bestScore = candidate.getValue();
                bestType = candidate.getKey();
            }
        }
        
        return bestType;
    }
}


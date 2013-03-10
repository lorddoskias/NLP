package hmmtagger;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
    private static Map<String, Map<String, Double>> emissionParams = new HashMap<>(); //contains the result for e(y|x)
    private static Map<String, Integer> elementCounts = new HashMap<>();    //holds count of different NGRAMs/WORDTAGs
    private static Map<String, Double> ngramParam = new HashMap<>();   //holds q(Yi | Yi-2, Yi-1) for each ngram 
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws FileNotFoundException, IOException {
        
        countWords(args[0]);
        teachTagger(args[0]);
        
        //open test data
//        BufferedWriter out = new BufferedWriter(new FileWriter(args[1] + ".tagged"));
//        BufferedReader in = new BufferedReader(new FileReader(args[1]));
//        String line;
//        
//        while((line = in.readLine()) != null) {
//            if (line.equals("")) { out.newLine(); continue;}
//            
//            out.write(line + " " + getMaxEmissionParameter(line));
//            out.newLine();
//        }
//
//        out.close();
//        in.close();
        
        tagFile(args[1]);
    }
    
    
    private static void teachTagger(String countFile) throws IOException {
        
        String line;
        Map<String, Integer> tagCount = countTags(countFile);

        //now we need to go through the file again and compute the 
        // emission parameters for each x|y
        BufferedReader in = new BufferedReader(new FileReader(countFile));
        
        while((line = in.readLine()) != null) {
            String tokens[] = line.split(" ");
            
            if (tokens[TYPE].equals(WORDTAG)) {
                //compute the emission parameters for each x|y
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
                
            } else if(tokens[TYPE].equals("3-GRAM")) {
                //compute parameters for q(Yi|Yi-2, Yi-1)
                int divident = elementCounts.get(getNgramId(tokens));
                int divisor = elementCounts.get(tokens[2] + " " + tokens[3]);
                ngramParam.put(getNgramId(tokens), ((double) divident / (double) divisor));
                
            }  
        }
        
        in.close();
    }
    
    /**
     * Count the number of times a particular tag is seen in the data set
     * @param input
     * @return
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private static Map<String, Integer> countTags(String input) throws FileNotFoundException, IOException {
        String line;
        Map<String, Integer> tagCount = new HashMap<>();

        BufferedReader in = new BufferedReader(new FileReader(input));
        
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
   
    /**
     * Prases the count file into a hashmap, making it convenient to work 
     * with the data
     * @param countFile
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private static void countWords(String countFile) throws FileNotFoundException, IOException {

        String line;
        BufferedReader in = new BufferedReader(new FileReader(countFile));

        while ((line = in.readLine()) != null) {
            String elements[] = line.split(" ");

            if (elements[TYPE].contains("GRAM")) {
                //WE HAVE an NGRAM ENTRY
                elementCounts.put(getNgramId(elements), Integer.parseInt(elements[COUNT]));
            } else {
                //we have a WORDTAG entry 
                if (elementCounts.get(elements[WORD]) == null) {
                    elementCounts.put(elements[WORD], Integer.parseInt(elements[COUNT]));
                } else {
                    int oldCount = elementCounts.get(elements[WORD]);
                    elementCounts.put(elements[WORD], oldCount + Integer.parseInt(elements[COUNT]));
                }
            }
        }
        in.close();
    }
    
    /**
     * Utility function to get the ngram id out of an string array
     * @param elements
     * @return 
     */
    private static String getNgramId(String[] elements) {
        String ngram = elements[2];
        for (int i = 3; i < elements.length; i++) {
            ngram += " " + elements[i];
        }
        
        return ngram;
    }
    
    public static String getMaxEmissionParameter(String x) throws IOException {
        Map<String, Double> m;
        double bestScore = -1;
        String bestType = "";
        
        m = (elementCounts.get(x) == null || elementCounts.get(x) < 5) ? emissionParams.get("_RARE_") : emissionParams.get(x);

        //find the highest scoring tag
        for (Entry<String, Double> candidate : m.entrySet()) {
            if (candidate.getValue() > bestScore) {
                bestScore = candidate.getValue();
                bestType = candidate.getKey();
            }
        }
        
        return bestType;
    }
    
    public static void tagFile(String inputFile) throws IOException {

        BufferedReader in = new BufferedReader(new FileReader(inputFile));
        String line;
        List<String> sentence = new ArrayList<>(15);

        while ((line = in.readLine()) != null) {
            // read a sentence
            if (!line.equals("")) {
                sentence.add(line);
                
            } else {
                
                //run viterbi
                System.out.println(Arrays.toString(sentence.toArray()));
                sentence.clear();
            }
        }
        in.close();
    }
}


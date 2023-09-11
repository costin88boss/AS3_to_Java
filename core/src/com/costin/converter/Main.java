package com.costin.converter;

import java.io.*;
import java.util.ArrayList;

public class Main {

    private static ArrayList<String> neededFiles = new ArrayList<>();
    private static ArrayList<String> existingFiles = new ArrayList<>();
    private static String pInput = "input", pOutput = "output";

    public static void main(String[] args) {
        File input = new File(pInput);
        File output = new File(pOutput);
        input.mkdir();
        output.mkdir();

        processFiles(input, output);

        for (String needed :
                neededFiles) {
            if(existingFiles.contains(needed)) {
                continue;
            }

            needed = needed.replace(".", "\\");
            //needed = needed.replace("*", "Fuck");
            // will not come if ends with asterisk
            boolean hasSlash = needed.contains("\\");
            File neededFile;
            if(hasSlash)
                neededFile = new File(pOutput + "\\" + needed.substring(0, needed.lastIndexOf("\\") + 1));
            else neededFile = new File(pOutput + "\\" + needed);
            File file;
            neededFile.mkdirs();
            if(hasSlash) file = new File(neededFile.getPath() + "\\" + needed.substring(needed.lastIndexOf("\\") + 1) + ".java");
            else file = new File(neededFile.getPath() + "\\" + needed + ".java");
            try {
                file.createNewFile();
                BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                String p;
                if(hasSlash)
                    p = needed.substring(0, needed.lastIndexOf("\\"));
                else p = needed;
                p = p.replace("\\", ".");
                writer.write("package " + p + ";" +
                        "\n" +
                        "\npublic class " + file.getName().substring(0, file.getName().length() - 5) + " {" +
                        "\n" +
                        "\n}");
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (File fl :
                filesToBePostProcessed) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(fl));
                StringBuilder stringBuilder = new StringBuilder();
                while (reader.ready()) {
                    stringBuilder.append(reader.readLine()).append("\n");
                }
                reader.close();
                String thingy = LanguageProcesser.postProcess(stringBuilder.toString());
                //BufferedWriter writer = new BufferedWriter(new FileWriter(fl));
                //writer.write(thingy);
                //writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
;
    private static ArrayList<File> filesToBePostProcessed = new ArrayList<>();

    private static void processFiles(File input, File output) {
        if (input.exists() && input.isDirectory())
            for (File input2 :
                    input.listFiles()) {
                if (!input2.exists()) continue;
                if (input2.isFile()) {
                    if (input2.getName().endsWith(".as")) {
                        try {
                            BufferedReader reader = new BufferedReader(new FileReader(input2));
                            StringBuilder stringBuilder = new StringBuilder();
                            while (reader.ready()) {
                                stringBuilder.append(reader.readLine()).append("\n");
                            }
                            reader.close();
                            System.out.println("Processing " + input2.getName());
                            ArrayList<String> needed = LanguageProcesser.process(stringBuilder.toString());
                            String java = needed.remove(0);
                            // output file
                            String path = input2.getPath().substring(pInput.length() + 1, input2.getPath().length() - input2.getName().length());
                            path = pOutput + "\\" + path;
                            File folder = new File(path);
                            folder.mkdirs();
                            File file = new File(path + "\\" + input2.getName().subSequence(0, input2.getName().length()-3) + ".java");
                            existingFiles.add(input2.getPath().substring(pInput.length() + 1, input2.getPath().length() - 3).replace("\\", "."));

                            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                            writer.write(java);
                            writer.close();

                            for (String _needed :
                                    needed) {
                                if(!neededFiles.contains(_needed)) {
                                    if(_needed.endsWith("*") || _needed.endsWith(".")) {
                                        continue;
                                    }
                                    neededFiles.add(_needed);
                                }
                            }
                            filesToBePostProcessed.add(file);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    processFiles(input2, output);
                }
            }
    }
}

package fr.openium.sga.displayer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StyleConverter {

    static Pattern classpathURLPattern = Pattern.compile("'classpath:([^']*)'");

    public static String convert(File file) throws IOException {
        byte[] bytes = new byte[(int)file.length()];
        FileInputStream in = new FileInputStream(file);
        in.read(bytes);
        in.close();
        String input = new String(bytes);
        Matcher matcher = classpathURLPattern.matcher(input);
        while(matcher.find()) {
            String resource = matcher.group(1);
            URL url = StyleConverter.class.getClassLoader().getResource(resource);
            //System.out.println("converted url: " + url.toString());
            input = input.replace(matcher.group(), "'" + url.toString() + "'");
            matcher.reset(input);
        }
        return input;
    }
}
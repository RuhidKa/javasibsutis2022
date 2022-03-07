import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PingResult {
    private String ip;
    private Double time;

    public PingResult(String ip, Double time) {
        this.ip = ip;
        this.time = time;
    }

    public void print() {
        System.out.println("IP: " + ip);
        System.out.println("Среднее время: " + String.valueOf(time) + " мс");
        System.out.println();
    }

    public Double time() {
        return time;
    }

    public String ip() {
        return ip;
    }
}

public final class PingTimeMeasure {
    private static ArrayList<PingResult> connectionTime = new ArrayList<PingResult>();
    private final static int packetNum = 5;

    private PingTimeMeasure() {}

    private static String extractValue(String str, String key) {
        Pattern p = Pattern.compile(".*" + key + "=([0-9]+)");
        Matcher match = p.matcher(str);
        match.find();
        return match.group(1);
    }

    public static int add(String ip)
    throws IOException, InterruptedException
    {
        boolean isWindows = System.getProperty("os.name").toLowerCase().contains("win");
    
        ProcessBuilder pb = new ProcessBuilder("ping", isWindows ? "-n" : "-c", String.valueOf(packetNum), ip);
        Process proc = pb.start();

        BufferedReader stdInput = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        int exitSuccess = proc.waitFor();

        String s = null;
        Double totalTimeMs = .0;
        while ((s = stdInput.readLine()) != null)
        {
            if (s.contains("time=")) {
                String timeMsString = extractValue(s, "time");
                Double timeMs = Double.parseDouble(timeMsString);
                totalTimeMs += timeMs;
            }
            else if (s.contains("could not find host") || s.contains("Name or service not known")) {
                System.out.println("Не удалось найти сервер по адресу '" + ip + "'. Сервер пропущен.");
                return exitSuccess;
            }
            else if (s.contains("Destination Host Unreachable") || s.contains("Destination host unreachable") || s.contains("Request timed out") || s.contains("100% packet loss")) {
                System.out.println("Запрос на сервер '" + ip + "' остался безответным. Сервер пропущен.");
                return exitSuccess;
            }
        }
        totalTimeMs /= packetNum;
        PingResult res = new PingResult(ip, totalTimeMs);
        connectionTime.add(res);
        Collections.sort(connectionTime, Comparator.comparing(PingResult::time).reversed());

        return exitSuccess;
    }

    public static void print() {
        for (PingResult res : connectionTime) {
            res.print();
        }
    }

    public static void writeToFile(String destination) {
        try (PrintWriter out = new PrintWriter(destination)) {
            for (PingResult res : connectionTime) {
                out.println(res.ip() + ";" + res.time());
            }
        } catch (FileNotFoundException e) {
            System.out.println("Ошибка записи в файл '" + destination + "'!");
        }
    }
}

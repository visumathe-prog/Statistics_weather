import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

class WeatherRecord {
    int year;
    Integer month;
    Integer day;
    String file;
    String time;
    Double temperature;
    Double windSpeed;
    Double seaLevel;
    Double humidity;

    public WeatherRecord(int year, Integer month, Integer day, String file) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.file = file;
    }

    @Override
    public String toString() {
        return String.format("Year: %d, Month: %s, Day: %s, File: %s, Time: %s, Temp: %s, Wind: %s, Sea: %s, Hum: %s",
            year, month, day, file, time, temperature, windSpeed, seaLevel, humidity);
    }
}

public class WeatherAnalyzer {
    private static final String WEATHER_FOLDER = "Weather";
    
    public static void main(String[] args) {
        System.out.println("=".repeat(50));
        System.out.println("WEATHER DATA ANALYZER");
        System.out.println("=".repeat(50));
        
        List<WeatherRecord> data = loadAllWeatherData();
        
        if (data.isEmpty()) {
            System.out.println("\nNo data found! Please check:");
            System.out.println("1. 'Weather' folder exists in current directory");
            System.out.println("2. It contains CSV files with weather data");
            System.out.print("\nPress Enter to exit...");
            try {
                System.in.read();
            } catch (IOException e) {
                // Ignore
            }
            return;
        }
        
        Scanner scanner = new Scanner(System.in);
        boolean running = true;
        
        while (running) {
            System.out.println("\n" + "=".repeat(50));
            System.out.println("SELECT WEATHER METRIC");
            System.out.println("=".repeat(50));
            System.out.println("What would you like to analyze?");
            
            List<String> metrics = new ArrayList<>();
            Map<Integer, String> metricOptions = new LinkedHashMap<>();
            int optionNum = 1;
            
            if (checkMetricAvailable(data, "temperature")) {
                metrics.add("temperature");
                metricOptions.put(optionNum++, "1. Temperature (°F)");
            }
            if (checkMetricAvailable(data, "windSpeed")) {
                metrics.add("windSpeed");
                metricOptions.put(optionNum++, "2. Wind Speed (MPH)");
            }
            if (checkMetricAvailable(data, "seaLevel")) {
                metrics.add("seaLevel");
                metricOptions.put(optionNum++, "3. Sea Level Pressure (inHg)");
            }
            if (checkMetricAvailable(data, "humidity")) {
                metrics.add("humidity");
                metricOptions.put(optionNum++, "4. Humidity (%)");
            }
            
            if (metrics.isEmpty()) {
                System.out.println("\nNo weather metrics found in the data!");
                System.out.print("Press Enter to exit...");
                try {
                    System.in.read();
                } catch (IOException e) {
                    // Ignore
                }
                return;
            }
            
            metricOptions.put(optionNum, "5. Exit");
            
            for (int i = 1; i <= metricOptions.size(); i++) {
                System.out.println(metricOptions.get(i));
            }
            
            System.out.print("\nEnter choice (1-" + optionNum + "): ");
            String choice = scanner.nextLine().trim();
            
            if (choice.equals(String.valueOf(optionNum))) {
                System.out.println("\nGoodbye!");
                break;
            }
            
            Map<String, String> metricMap = new HashMap<>();
            int metricIndex = 1;
            for (String metric : metrics) {
                metricMap.put(String.valueOf(metricIndex++), metric);
            }
            
            if (!metricMap.containsKey(choice)) {
                System.out.println("\nInvalid choice!");
                continue;
            }
            
            String selectedMetric = metricMap.get(choice);
            MetricInfo metricInfo = getMetricInfo(selectedMetric);
            
            System.out.println("\nSelected: " + metricInfo.name);
            
            boolean backToMetrics = false;
            while (!backToMetrics) {
                System.out.println("\n" + "=".repeat(50));
                System.out.println(metricInfo.displayName + " ANALYSIS");
                System.out.println("=".repeat(50));
                System.out.println("What would you like to find?");
                System.out.println("1. Minimum value");
                System.out.println("2. Maximum value");
                System.out.println("3. Average value");
                System.out.println("4. Back to metric selection");
                
                System.out.print("\nEnter choice (1-4): ");
                String analysisChoice = scanner.nextLine().trim();
                
                if (analysisChoice.equals("4")) {
                    backToMetrics = true;
                    continue;
                }
                
                if (!Arrays.asList("1", "2", "3").contains(analysisChoice)) {
                    System.out.println("\nInvalid choice!");
                    continue;
                }
                
                System.out.println("\nSelect time period:");
                System.out.println("1. For a specific YEAR");
                System.out.println("2. For a specific MONTH");
                System.out.println("3. For a specific DAY");
                
                System.out.print("\nEnter choice (1-3): ");
                String period = scanner.nextLine().trim();
                
                if (!Arrays.asList("1", "2", "3").contains(period)) {
                    System.out.println("\nInvalid choice!");
                    continue;
                }
                
                List<Integer> years = getAvailableYears(data);
                if (years.isEmpty()) {
                    System.out.println("\nNo year data available!");
                    continue;
                }
                
                System.out.println("\nAvailable years:");
                for (int i = 0; i < years.size(); i++) {
                    System.out.println((i + 1) + ". " + years.get(i));
                }
                
                int yearIdx;
                try {
                    System.out.print("\nSelect year (1-" + years.size() + "): ");
                    yearIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                    if (yearIdx < 0 || yearIdx >= years.size()) {
                        System.out.println("\nInvalid selection!");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("\nInvalid selection!");
                    continue;
                }
                
                int selectedYear = years.get(yearIdx);
                Integer selectedMonth = null;
                Integer selectedDay = null;
                
                if (period.equals("2") || period.equals("3")) {
                    List<Integer> months = getAvailableMonths(data, selectedYear);
                    if (months.isEmpty()) {
                        System.out.println("\nNo month data for " + selectedYear + "!");
                        continue;
                    }
                    
                    System.out.println("\nAvailable months for " + selectedYear + ":");
                    Map<Integer, String> monthNames = new HashMap<>();
                    monthNames.put(1, "January");
                    monthNames.put(2, "February");
                    monthNames.put(3, "March");
                    monthNames.put(4, "April");
                    monthNames.put(5, "May");
                    monthNames.put(6, "June");
                    monthNames.put(7, "July");
                    monthNames.put(8, "August");
                    monthNames.put(9, "September");
                    monthNames.put(10, "October");
                    monthNames.put(11, "November");
                    monthNames.put(12, "December");
                    
                    for (int i = 0; i < months.size(); i++) {
                        int month = months.get(i);
                        String name = monthNames.getOrDefault(month, "Month " + month);
                        System.out.println((i + 1) + ". " + name);
                    }
                    
                    try {
                        System.out.print("\nSelect month (1-" + months.size() + "): ");
                        int monthIdx = Integer.parseInt(scanner.nextLine().trim()) - 1;
                        if (monthIdx < 0 || monthIdx >= months.size()) {
                            System.out.println("\nInvalid selection!");
                            continue;
                        }
                        selectedMonth = months.get(monthIdx);
                    } catch (NumberFormatException e) {
                        System.out.println("\nInvalid selection!");
                        continue;
                    }
                }
                
                if (period.equals("3")) {
                    List<Integer> days = getAvailableDays(data, selectedYear, selectedMonth);
                    if (days.isEmpty()) {
                        System.out.println("\nNo day data for " + selectedYear + "-" + selectedMonth + "!");
                        continue;
                    }
                    
                    System.out.println("\nAvailable days for " + selectedYear + "-" + selectedMonth + ":");
                    String daysStr = days.stream().map(String::valueOf).collect(Collectors.joining(", "));
                    System.out.println("Days: " + daysStr);
                    
                    try {
                        System.out.print("\nSelect day (1-" + Collections.max(days) + "): ");
                        selectedDay = Integer.parseInt(scanner.nextLine().trim());
                        if (!days.contains(selectedDay)) {
                            System.out.println("\nDay " + selectedDay + " not available!");
                            continue;
                        }
                    } catch (NumberFormatException e) {
                        System.out.println("\nInvalid selection!");
                        continue;
                    }
                }
                
                List<WeatherRecord> filtered = filterData(data, selectedYear, selectedMonth, selectedDay);
                
                if (filtered.isEmpty()) {
                    System.out.println("\nNo data found for selected criteria!");
                    continue;
                }
                
                switch (analysisChoice) {
                    case "1":
                        WeatherRecord minResult = findExtreme(filtered, selectedMetric, "min");
                        if (minResult != null) {
                            displayResult(selectedMetric, "minimum", minResult, selectedYear, selectedMonth, selectedDay);
                        } else {
                            System.out.println("\nNo " + metricInfo.name.toLowerCase() + " data found for selected period!");
                        }
                        break;
                    case "2":
                        WeatherRecord maxResult = findExtreme(filtered, selectedMetric, "max");
                        if (maxResult != null) {
                            displayResult(selectedMetric, "maximum", maxResult, selectedYear, selectedMonth, selectedDay);
                        } else {
                            System.out.println("\nNo " + metricInfo.name.toLowerCase() + " data found for selected period!");
                        }
                        break;
                    case "3":
                        Double avgValue = calculateAverage(filtered, selectedMetric);
                        if (avgValue != null) {
                            displayResult(selectedMetric, "average", avgValue, selectedYear, selectedMonth, selectedDay);
                        } else {
                            System.out.println("\nNo " + metricInfo.name.toLowerCase() + " data found for selected period!");
                        }
                        break;
                }
                
                while (true) {
                    System.out.print("\nWould you like another " + metricInfo.name.toLowerCase() + " analysis? (y/n): ");
                    String again = scanner.nextLine().trim().toLowerCase();
                    if (again.equals("y")) {
                        break;
                    } else if (again.equals("n")) {
                        backToMetrics = true;
                        break;
                    } else {
                        System.out.println("Please enter 'y' or 'n'");
                    }
                }
            }
        }
        scanner.close();
    }
    
    private static List<WeatherRecord> loadAllWeatherData() {
        System.out.println("Loading weather data...");
        
        List<WeatherRecord> allData = new ArrayList<>();
        List<Path> csvFiles = findAllCsvFiles();
        
        if (csvFiles.isEmpty()) {
            System.out.println("No CSV files found in 'Weather' folder!");
            return allData;
        }
        
        System.out.println("Found " + csvFiles.size() + " CSV files");
        
        for (Path filepath : csvFiles) {
            DateInfo dateInfo = extractDateFromPath(filepath);
            
            if (dateInfo.year == null) {
                continue;
            }
            
            try {
                List<String> lines = Files.readAllLines(filepath);
                if (lines.isEmpty()) {
                    continue;
                }
                
                String firstLine = lines.get(0);
                String delimiter = firstLine.contains("\t") ? "\t" : ",";
                
                List<List<String>> rows = new ArrayList<>();
                for (String line : lines) {
                    if (line.trim().isEmpty()) {
                        continue;
                    }
                    List<String> row = parseCSVLine(line, delimiter);
                    rows.add(row);
                }
                
                if (rows.size() < 2) {
                    continue;
                }
                
                List<String> header = rows.get(0);
                
                Map<String, Integer> colIndices = new HashMap<>();
                colIndices.put("time", 0);
                colIndices.put("temperature", null);
                colIndices.put("windSpeed", null);
                colIndices.put("seaLevel", null);
                colIndices.put("humidity", null);
                
                for (int i = 0; i < header.size(); i++) {
                    String colLower = header.get(i).toLowerCase();
                    
                    if (colLower.contains("temp")) {
                        colIndices.put("temperature", i);
                    } else if (colLower.contains("wind") && colLower.contains("speed")) {
                        colIndices.put("windSpeed", i);
                    } else if ((colLower.contains("sea") && colLower.contains("level")) || colLower.contains("pressure")) {
                        colIndices.put("seaLevel", i);
                    } else if (colLower.contains("humid")) {
                        colIndices.put("humidity", i);
                    } else if (colLower.contains("time")) {
                        colIndices.put("time", i);
                    }
                }
                
                for (int rowIdx = 1; rowIdx < rows.size(); rowIdx++) {
                    List<String> row = rows.get(rowIdx);
                    if (row.size() < 2) {
                        continue;
                    }
                    
                    WeatherRecord record = new WeatherRecord(
                        dateInfo.year,
                        dateInfo.month,
                        dateInfo.day,
                        filepath.getFileName().toString()
                    );
                    
                    Integer timeIdx = colIndices.get("time");
                    if (timeIdx != null && timeIdx < row.size()) {
                        record.time = row.get(timeIdx).trim();
                    }
                    
                    Integer tempIdx = colIndices.get("temperature");
                    if (tempIdx != null && tempIdx < row.size()) {
                        String tempStr = row.get(tempIdx).trim();
                        if (!isNullOrNA(tempStr)) {
                            try {
                                record.temperature = Double.parseDouble(tempStr);
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                    }
                    
                    Integer windIdx = colIndices.get("windSpeed");
                    if (windIdx != null && windIdx < row.size()) {
                        String windStr = row.get(windIdx).trim();
                        if (!isNullOrNA(windStr)) {
                            try {
                                record.windSpeed = Double.parseDouble(windStr);
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                    }
                    
                    Integer seaIdx = colIndices.get("seaLevel");
                    if (seaIdx != null && seaIdx < row.size()) {
                        String seaStr = row.get(seaIdx).trim();
                        if (!isNullOrNA(seaStr)) {
                            try {
                                record.seaLevel = Double.parseDouble(seaStr);
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                    }
                    
                    Integer humidIdx = colIndices.get("humidity");
                    if (humidIdx != null && humidIdx < row.size()) {
                        String humidStr = row.get(humidIdx).trim();
                        if (!isNullOrNA(humidStr)) {
                            try {
                                record.humidity = Double.parseDouble(humidStr);
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                    }
                    
                    if (record.temperature != null || record.windSpeed != null || 
                        record.seaLevel != null || record.humidity != null) {
                        allData.add(record);
                    }
                }
            } catch (IOException e) {
                System.out.println("Error reading " + filepath + ": " + e.getMessage());
                continue;
            }
        }
        
        System.out.println("Loaded " + allData.size() + " weather records");
        
        List<String> availableMetrics = new ArrayList<>();
        if (allData.stream().anyMatch(d -> d.temperature != null)) {
            availableMetrics.add("temperature");
        }
        if (allData.stream().anyMatch(d -> d.windSpeed != null)) {
            availableMetrics.add("wind speed");
        }
        if (allData.stream().anyMatch(d -> d.seaLevel != null)) {
            availableMetrics.add("sea level pressure");
        }
        if (allData.stream().anyMatch(d -> d.humidity != null)) {
            availableMetrics.add("humidity");
        }
        
        if (!availableMetrics.isEmpty()) {
            System.out.println("Available metrics: " + String.join(", ", availableMetrics));
        }
        
        Set<Integer> years = allData.stream()
            .filter(d -> d.year != 0)
            .map(d -> d.year)
            .collect(Collectors.toSet());
        List<Integer> sortedYears = new ArrayList<>(years);
        Collections.sort(sortedYears);
        
        if (!sortedYears.isEmpty()) {
            String yearsStr = sortedYears.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
            System.out.println("Available years: " + yearsStr);
        }
        
        return allData;
    }
    
    private static List<Path> findAllCsvFiles() {
        List<Path> csvFiles = new ArrayList<>();
        Path folder = Paths.get(WEATHER_FOLDER);
        
        if (!Files.exists(folder) || !Files.isDirectory(folder)) {
            return csvFiles;
        }
        
        try {
            Files.walk(folder)
                .filter(Files::isRegularFile)
                .filter(path -> path.toString().toLowerCase().endsWith(".csv"))
                .forEach(csvFiles::add);
        } catch (IOException e) {
            System.out.println("Error walking directory: " + e.getMessage());
        }
        
        return csvFiles;
    }
    
    private static List<String> parseCSVLine(String line, String delimiter) {
        List<String> result = new ArrayList<>();
        if (delimiter.equals(",")) {
            // Simple CSV parsing
            String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", -1);
            for (String part : parts) {
                // Remove surrounding quotes if present
                String cleaned = part.trim();
                if (cleaned.startsWith("\"") && cleaned.endsWith("\"")) {
                    cleaned = cleaned.substring(1, cleaned.length() - 1);
                }
                result.add(cleaned);
            }
        } else {
            // Tab delimiter
            String[] parts = line.split("\t");
            for (String part : parts) {
                result.add(part.trim());
            }
        }
        return result;
    }
    
    private static boolean isNullOrNA(String str) {
        return str == null || str.isEmpty() || str.equals("N/A") || 
               str.equals("-") || str.equals("NA") || str.equalsIgnoreCase("null");
    }
    
    private static class DateInfo {
        Integer year;
        Integer month;
        Integer day;
    }
    
    private static DateInfo extractDateFromPath(Path filepath) {
        DateInfo info = new DateInfo();
        
        // Extract year from path
        for (Path part : filepath) {
            String partStr = part.toString();
            if (partStr.matches("\\d{4}")) {
                try {
                    int y = Integer.parseInt(partStr);
                    if (y >= 2012 && y <= 2015) {
                        info.year = y;
                        break;
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
        }
        
        // Extract from filename
        String filename = filepath.getFileName().toString();
        Pattern[] patterns = {
            Pattern.compile("(\\d{4})[-_](\\d{1,2})[-_](\\d{1,2})"),  // 2014-5-1
            Pattern.compile("(\\d{1,2})[-_](\\d{1,2})[-_](\\d{4})")   // 1-5-2014
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(filename);
            if (matcher.find()) {
                try {
                    String[] groups = new String[3];
                    for (int i = 0; i < 3; i++) {
                        groups[i] = matcher.group(i + 1);
                    }
                    
                    if (groups[0].length() == 4) {  // Year first
                        int y = Integer.parseInt(groups[0]);
                        int m = Integer.parseInt(groups[1]);
                        int d = Integer.parseInt(groups[2]);
                        
                        if (info.year == null) info.year = y;
                        if (info.month == null) info.month = m;
                        info.day = d;
                    } else {
                        int first = Integer.parseInt(groups[0]);
                        int second = Integer.parseInt(groups[1]);
                        int y = Integer.parseInt(groups[2]);
                        
                        if (first > 12) {  // First is day
                            info.day = first;
                            info.month = second;
                            if (info.year == null) info.year = y;
                        } else {  // First is month
                            info.month = first;
                            info.day = second;
                            if (info.year == null) info.year = y;
                        }
                    }
                    break;
                } catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
                    continue;
                }
            }
        }
        
        // Extract month from path if not found
        if (info.month == null) {
            for (Path part : filepath) {
                String partStr = part.toString();
                if (partStr.matches("\\d{1,2}")) {
                    try {
                        int m = Integer.parseInt(partStr);
                        if (m >= 1 && m <= 12) {
                            info.month = m;
                            break;
                        }
                    } catch (NumberFormatException e) {
                        // Ignore
                    }
                }
            }
        }
        
        return info;
    }
    
    private static List<Integer> getAvailableYears(List<WeatherRecord> data) {
        return data.stream()
            .filter(d -> d.year != 0)
            .map(d -> d.year)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    private static List<Integer> getAvailableMonths(List<WeatherRecord> data, int year) {
        return data.stream()
            .filter(d -> d.year == year && d.month != null)
            .map(d -> d.month)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    private static List<Integer> getAvailableDays(List<WeatherRecord> data, int year, int month) {
        return data.stream()
            .filter(d -> d.year == year && d.month != null && d.month == month && d.day != null)
            .map(d -> d.day)
            .distinct()
            .sorted()
            .collect(Collectors.toList());
    }
    
    private static List<WeatherRecord> filterData(List<WeatherRecord> data, Integer year, Integer month, Integer day) {
        return data.stream()
            .filter(d -> (year == null || d.year == year))
            .filter(d -> (month == null || (d.month != null && d.month == month)))
            .filter(d -> (day == null || (d.day != null && d.day == day)))
            .collect(Collectors.toList());
    }
    
    private static WeatherRecord findExtreme(List<WeatherRecord> data, String metric, String mode) {
        // First filter valid records
        List<WeatherRecord> validData = new ArrayList<>();
        for (WeatherRecord record : data) {
            Double value = getMetricValue(record, metric);
            if (value != null) {
                validData.add(record);
            }
        }
        
        if (validData.isEmpty()) {
            return null;
        }
        
        if (mode.equals("min")) {
            WeatherRecord minRecord = validData.get(0);
            Double minValue = getMetricValue(minRecord, metric);
            
            for (int i = 1; i < validData.size(); i++) {
                WeatherRecord currentRecord = validData.get(i);
                Double currentValue = getMetricValue(currentRecord, metric);
                if (currentValue < minValue) {
                    minValue = currentValue;
                    minRecord = currentRecord;
                }
            }
            return minRecord;
        } else {
            WeatherRecord maxRecord = validData.get(0);
            Double maxValue = getMetricValue(maxRecord, metric);
            
            for (int i = 1; i < validData.size(); i++) {
                WeatherRecord currentRecord = validData.get(i);
                Double currentValue = getMetricValue(currentRecord, metric);
                if (currentValue > maxValue) {
                    maxValue = currentValue;
                    maxRecord = currentRecord;
                }
            }
            return maxRecord;
        }
    }
    
    private static Double calculateAverage(List<WeatherRecord> data, String metric) {
        List<Double> values = new ArrayList<>();
        for (WeatherRecord record : data) {
            Double value = getMetricValue(record, metric);
            if (value != null) {
                values.add(value);
            }
        }
        
        if (values.isEmpty()) {
            return null;
        }
        
        double total = 0.0;
        for (Double value : values) {
            total += value;
        }
        
        return total / values.size();
    }
    
    private static Double getMetricValue(WeatherRecord record, String metric) {
        switch (metric) {
            case "temperature":
                return record.temperature;
            case "windSpeed":
                return record.windSpeed;
            case "seaLevel":
                return record.seaLevel;
            case "humidity":
                return record.humidity;
            default:
                return null;
        }
    }
    
    private static boolean checkMetricAvailable(List<WeatherRecord> data, String metric) {
        for (WeatherRecord record : data) {
            Double value = getMetricValue(record, metric);
            if (value != null) {
                return true;
            }
        }
        return false;
    }
    
    private static class MetricInfo {
        String name;
        String unit;
        String displayName;
        
        MetricInfo(String name, String unit, String displayName) {
            this.name = name;
            this.unit = unit;
            this.displayName = displayName;
        }
    }
    
    private static MetricInfo getMetricInfo(String metric) {
        switch (metric) {
            case "temperature":
                return new MetricInfo("Temperature", "°F", "TEMPERATURE");
            case "windSpeed":
                return new MetricInfo("Wind Speed", "MPH", "WIND SPEED");
            case "seaLevel":
                return new MetricInfo("Sea Level Pressure", "inHg", "SEA LEVEL PRESSURE");
            case "humidity":
                return new MetricInfo("Humidity", "%", "HUMIDITY");
            default:
                return new MetricInfo(metric, "", metric.toUpperCase());
        }
    }
    
    private static void displayResult(String metric, String mode, Object result, 
                                     int year, Integer month, Integer day) {
        MetricInfo metricInfo = getMetricInfo(metric);
        
        System.out.println("\n" + "=".repeat(50));
        System.out.println(mode.toUpperCase() + " " + metricInfo.displayName);
        System.out.println("=".repeat(50));
        
        String periodStr;
        if (day != null && month != null) {
            periodStr = String.format("%02d/%02d/%d", day, month, year);
        } else if (month != null) {
            Map<Integer, String> monthNames = new HashMap<>();
            monthNames.put(1, "January");
            monthNames.put(2, "February");
            monthNames.put(3, "March");
            monthNames.put(4, "April");
            monthNames.put(5, "May");
            monthNames.put(6, "June");
            monthNames.put(7, "July");
            monthNames.put(8, "August");
            monthNames.put(9, "September");
            monthNames.put(10, "October");
            monthNames.put(11, "November");
            monthNames.put(12, "December");
            
            String monthName = monthNames.getOrDefault(month, "Month " + month);
            periodStr = monthName + " " + year;
        } else {
            periodStr = String.valueOf(year);
        }
        
        System.out.println("Period: " + periodStr);
        
        if (mode.equals("average")) {
            Double avg = (Double) result;
            System.out.printf("%s: %.2f%s\n", metricInfo.name, avg, metricInfo.unit);
        } else {
            WeatherRecord record = (WeatherRecord) result;
            Double value = getMetricValue(record, metric);
            if (value != null) {
                System.out.printf("%s: %.2f%s\n", metricInfo.name, value, metricInfo.unit);
            }
            System.out.println("Time: " + (record.time != null ? record.time : "N/A"));
            
            if (day != null && month != null) {
                System.out.printf("Date: %02d/%02d/%d\n", day, month, year);
            } else {
                String actualDate = formatDate(record.day, record.month, record.year);
                if (!actualDate.equals(String.valueOf(year))) {
                    System.out.println("Date: " + actualDate);
                }
            }
        }
        
        System.out.println("=".repeat(50));
    }
    
    private static String formatDate(Integer day, Integer month, int year) {
        if (day != null && month != null) {
            return String.format("%02d/%02d/%d", day, month, year);
        } else if (month != null) {
            return String.format("%02d/%d", month, year);
        } else {
            return String.valueOf(year);
        }
    }
}
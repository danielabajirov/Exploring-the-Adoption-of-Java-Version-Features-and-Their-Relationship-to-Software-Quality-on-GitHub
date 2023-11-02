package org.example;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.AnnotationDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.ast.body.Parameter;
import java.net.*;


import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.model.resolution.TypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;


import java.lang.invoke.ConstantBootstraps;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.*;
import java.util.stream.*;
import java.io.*;
import java.nio.file.*;

public class JavaCodeAnalyzer {

    private static Map<String, Integer> methodCounts = new HashMap<>();
    private static final Logger logger = Logger.getLogger(JavaCodeAnalyzer.class.getName());
    private static boolean flag = true;
    private static int errorCount = 0;
    private static final int MAX_ERROR_COUNT = 4; // Maximum allowed errors, not used in the provided snippet
    private static int counterTotalRepo = 0;
    private static int counterTotalRepoWritten = 0;
    private static boolean flagFro = true;

    public static void main(String[] args) throws IOException {
        String parentDir = "D:\\Repositories";
        List<String> years = Arrays.asList("2023"); // or any other years we need

        initializeLogger();

        TypeSolver typeSolver = new ReflectionTypeSolver();
        JavaSymbolSolver symbolSolver = new JavaSymbolSolver(typeSolver);
        StaticJavaParser.getConfiguration().setSymbolResolver(symbolSolver);

        for (String year : years) {
            processRepositoriesForYear(parentDir, year);
        }
    }

    /**
     * Initializes the logger with a file handler and formatter.
     */
    private static void initializeLogger() {
        try {
            FileHandler fileHandler = new FileHandler("repositories_years_logs.txt", true);
            logger.addHandler(fileHandler);
            SimpleFormatter formatter = new SimpleFormatter();
            fileHandler.setFormatter(formatter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Process all repositories for a given year.
     *
     * @param parentDir The parent directory of the repositories.
     * @param year      The year to process repositories for.
     * @throws IOException Throws if there is an error processing the repositories.
     */
    private static void processRepositoriesForYear(String parentDir, String year) throws IOException {
        counterTotalRepo = 0;
        counterTotalRepoWritten = 0;
        String csvFileName = "csvRepoitory";
        String outputFile = "OutputFile";
        Map<String, String> repoNamesWithFirstCommitDates = loadRepositoryNamesAndFirstCommitDatesFromCSV(csvFileName);

        for (Map.Entry<String, String> entry : repoNamesWithFirstCommitDates.entrySet()) {
            counterTotalRepo++;
            String repoName = entry.getKey();
            String firstCommitDate = entry.getValue();
            analyzeRepositoryForYear(parentDir, year, repoName, firstCommitDate);
            analyzeRepository(parentDir, outputFile);
        }

        logger.log(Level.INFO, "Number of the repository from year: " + year + " is currently: " + counterTotalRepo +
                " But the repos written are only: " + counterTotalRepoWritten);
    }

    /**
     * Analyzes a specific repository for the given year.
     *
     * @param parentDir       The parent directory of repositories.
     * @param year            The year to analyze the repository for.
     * @param repoName        The name of the repository.
     * @param firstCommitDate The first commit date of the repository.
     */
    private static void analyzeRepositoryForYear(String parentDir, String year, String repoName, String firstCommitDate) {
        String repoPath = Paths.get(parentDir, repoName).toString();
        System.out.println("Now analyzing the file: " + repoName + " having the year: " + year);

        // Here the 'dates' array should be provided, in this case, it's hard-coded for demonstration.
        String[] dates = {"15 July 2023"};

        if (Files.exists(Paths.get(repoPath))) {
            for (String date : dates) {
                if (isDateInYear(date, year)) {
                    Optional<String> validDate = findValidDate(repoPath, date, firstCommitDate);
                    validDate.ifPresent(validDateString -> processValidDate(repoPath, validDateString, date, year, repoName));
                }
            }
        } else {
            logger.log(Level.WARNING, "Repository not found: " + repoName);
        }
    }

    /**
     * Resets the repository to a specified state based on a date, then analyzes it.
     *
     * @param repoPath      The path to the repository.
     * @param validDate     The valid date to reset the repository to.
     * @param date          The target date for the analysis.
     * @param year          The year of the analysis.
     * @param repoName      The name of the repository.
     */
    private static void processValidDate(String repoPath, String validDate, String date, String year, String repoName) {
        System.out.println("We have a valid date");
        System.out.println("Now analyzing the file: " + repoName + " having the year: " + year);
        resetRepositoryTo(repoPath, validDate);
        String formattedDate = date.replace(" ", "_");
        String outputFile = "csvPath";
        analyzeRepository(repoPath, outputFile, date, year);
        counterTotalRepoWritten++;
    }

    private static Map<String, String> loadRepositoryNamesAndFirstCommitDatesFromCSV(String csvFileName) {
        Map<String, String> repoNamesWithFirstCommitDates = new HashMap<>();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFileName))) {
            String line;

            // Skip the header line
            br.readLine();

            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length >= 2) {
                    String repoName = values[0].trim();
                    String firstCommitDate = values[1].trim();
                    repoNamesWithFirstCommitDates.put(repoName, firstCommitDate);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return repoNamesWithFirstCommitDates;
    }

    /**
     * Checks if the given date string is within the specified year.
     *
     * @param dateString The date string to check.
     * @param year       The year to compare against.
     * @return true if the date is within the year, false otherwise.
     */
    private static boolean isDateInYear(String dateString, String year) {
        LocalDate date = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("d MMMM yyyy"));
        return String.valueOf(date.getYear()).equals(year);
    }

    /**
     * Finds a valid date for repository analysis. If the target date is before the first commit,
     * it uses the day after the first commit. Otherwise, it looks for the first commit within the range.
     *
     * @param repoPath        The path to the repository.
     * @param dateString      The target date for the analysis.
     * @param firstCommitDate The date of the first commit.
     * @return An Optional containing the valid date if one is found, or an empty Optional otherwise.
     */
    private static Optional<String> findValidDate(String repoPath, String dateString, String firstCommitDate) {
        LocalDate targetDate = LocalDate.parse(dateString, DateTimeFormatter.ofPattern("d MMMM yyyy"));
        LocalDate repoFirstCommitDate = LocalDate.parse(firstCommitDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        if (targetDate.isBefore(repoFirstCommitDate) || targetDate.equals(repoFirstCommitDate)) {
            // Use the day after the first commit as the valid date
            return Optional.of(repoFirstCommitDate.plusDays(1).toString());
        } else {
            // If the target date is after the first commit, find the first commit within the range (implementation not shown)
            return getFirstCommitWithinRange(repoPath, dateString);
        }
    }


    private static Optional<String> getFirstCommitWithinRange(String repoPath, String date) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
        LocalDate localDate = LocalDate.parse(date, formatter);

        String startDate = localDate.withDayOfYear(1).format(DateTimeFormatter.ISO_DATE); // January 1 of the given year
        String endDate = localDate.format(DateTimeFormatter.ISO_DATE); // The specified date

        ProcessBuilder processBuilder = new ProcessBuilder("git", "log", "--since=" + startDate, "--until=" + endDate, "--pretty=format:%cd", "--date=short");
        processBuilder.directory(new File(repoPath));

        try {
            Process process = processBuilder.start();

            // Read stdout and stderr concurrently to prevent blocking
            Future<String> stdout = readStreamAsync(process.getInputStream());
            Future<String> stderr = readStreamAsync(process.getErrorStream());

            process.waitFor();

            // Get the results from the async reads
            String firstCommit = stdout.get();
            String errors = stderr.get();

            // If there were any errors, print or log them
            if (!errors.isEmpty()) {
                System.err.println(errors);  // Or log the error using a logger
            }

            return Optional.ofNullable(firstCommit.split("\n")[0]);  // Only the first line
        } catch (Exception e) {
            e.printStackTrace();
        }

        return Optional.empty();
    }

    private static Future<String> readStreamAsync(InputStream stream) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        return executor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        });
    }

    private static void analyzeRepository(String repoPath, String outputFile, String date, String year) {
        System.out.println(repoPath);

        try (Stream<Path> paths = Files.walk(Paths.get(repoPath))) {
            List<FileStats> fileStatsList = paths.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".java"))
                    .map(path -> {
                        try {
                            return calculateLinesOfCode(path.toFile(), repoPath, date, year);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            writeToCsvLinesOfCode(outputFile, fileStatsList);

        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read files from " + repoPath, e);
        }
    }

    private static FileStats calculateLinesOfCode(File javaFile, String repoPath, String date, String year) throws IOException {
        long loc = 0;
        if(repoPath.equals("D:\\Repositories\\MOEAFramework")) {
            int counter = 0;
        }
        try (Stream<String> lines = Files.lines(javaFile.toPath(), StandardCharsets.ISO_8859_1)) {
            loc = lines.filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("//"))
                    .count(); // We skip empty lines and single line comments for a basic LoC count
        } catch (MalformedInputException e ) {
            try(Stream<String> lines = Files.lines(javaFile.toPath(), StandardCharsets.ISO_8859_1)) {
                loc = lines.filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("//")).count();

            } catch (IOException error) {
                logger.log(Level.WARNING,"Failed to read lines from " + javaFile.getPath(), error);
            }
        }
        catch (IOException e) {
            logger.log(Level.WARNING, "Failed to read lines from " + javaFile.getPath(), e);
        }
        return new FileStats(repoPath, javaFile.getName(), date, year, loc);
    }

    private static void writeToCsvLinesOfCode(String outputFile, List<FileStats> fileStatsList) {
        try (FileWriter csvWriter = new FileWriter(outputFile, true)) {
            csvWriter.append("Repository");
            csvWriter.append(",");
            csvWriter.append("File");
            csvWriter.append(",");
            csvWriter.append("Date");
            csvWriter.append(",");
            csvWriter.append("Year");
            csvWriter.append(",");
            csvWriter.append("LinesOfCode");
            csvWriter.append("\n");

            for (FileStats stats : fileStatsList) {
                csvWriter.append(stats.getRepository());
                csvWriter.append(",");
                csvWriter.append(stats.getFile());
                csvWriter.append(",");
                csvWriter.append(stats.getDate());
                csvWriter.append(",");
                csvWriter.append(stats.getYear());
                csvWriter.append(",");
                csvWriter.append(String.valueOf(stats.getLoc()));
                csvWriter.append("\n");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write to CSV file", e);
        }
    }


    private static void analyzeRepository(String repoPath, String outputFile) {
        if(flag) {
            System.out.println(repoPath);

            try (Stream<Path> paths = Files.walk(Paths.get(repoPath))) {
                paths.filter(Files::isRegularFile)
                        .filter(p -> p.toString().endsWith(".java"))
                        .forEach(path -> {
                            try {
                                if (errorCount > MAX_ERROR_COUNT) {
                                    skipRepository(repoPath, outputFile);
                                    return;
                                }
                                File javaFile = path.toFile();
                                analyzeFile(javaFile, repoPath);
                            } catch (Exception e) {
                                logger.log(Level.WARNING, "Failed to analyze " + path + ", skipping this file.", e);
                                errorCount++;
                            }
                        });
            } catch (IOException e) {
                logger.log(Level.SEVERE, "Failed to read files from " + repoPath, e);
            }

            if (errorCount <= MAX_ERROR_COUNT) {
                writeCountsToCsv(outputFile);
                methodCounts.clear();
            }
        }
        // Skip adempiere, android_frameworks_base, batfish, corretto-11, corretto-8, cyclops, dragonwell11, desugar_jdk_libs, dragonwell8, error-prone, fastjson, functionaljava, geotools, helidon, Iris, j2objc, jackrabbit-oak, jandex, janino, jdk, JetBrainsRuntime
        // microstream, netbeans, opennms, picocli, PojavLauncher_iOS, quarkus, questdb, qulice, reactor-core, RikkaX, robovm, SapMachine, SIMRS-Khanza, SquidLib, Tachidesk-Server, TencentKona-11, TencentKona-8, Thanox, valhalla, xtext, xwiki-platform
        //if(repoPath.equals("D:\\Repositories\\xwiki-platform"))
         //   flag = true;

    }

    private static String getDefaultBranch(String repoPath) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            processBuilder.command("git", "symbolic-ref", "refs/remotes/origin/HEAD");
            processBuilder.directory(new File(repoPath));
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String output = reader.readLine();
            if (output != null) {
                return output.replace("refs/remotes/origin/", "");
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to get default branch for " + repoPath, e);
        }
        return null;
    }

    private static void resetRepositoryTo(String repoPath, String date) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder();
            String defaultBranch = getDefaultBranch(repoPath);
            processBuilder.command("git", "rev-list", "-n", "1", "--before=" + date, defaultBranch);
            processBuilder.directory(new File(repoPath));
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String commitHash = reader.readLine();

            if (commitHash != null && !commitHash.trim().isEmpty()) {
                ProcessBuilder resetProcessBuilder = new ProcessBuilder();
                resetProcessBuilder.command("git", "reset", "--hard", commitHash);
                resetProcessBuilder.directory(new File(repoPath));
                Process resetProcess = resetProcessBuilder.start();
                resetProcess.waitFor();
                logger.log(Level.INFO, "Reset " + repoPath + " to commit: " + commitHash);
            } else {
                logger.log(Level.WARNING, "No commit found before " + date + " in " + repoPath);
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to reset " + repoPath + " to date " + date, e);
        }
    }

    private static void analyzeFile(File javaFile, String repoPath) {
        try {
            CompilationUnit cu = StaticJavaParser.parse(javaFile);

            ImportVisitor importVisitor = new ImportVisitor();
            importVisitor.visit(cu, null);
            String imports = String.join(";", importVisitor.imports);

            AnnotationVisitor annotationVisitor = new AnnotationVisitor();
            annotationVisitor.visit(cu, null);
            String annotations = String.join(";", annotationVisitor.annotations);

            // Here we break down the parsing by method.
            for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                analyzeMethod(method, repoPath, javaFile.getName(), imports, annotations);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to parse " + javaFile.getPath() + ", skipping this file." + e);
            errorCount++;
        }
    }



    private static void skipRepository(String repoPath, String outputFile) {
        try (FileWriter logWriter = new FileWriter("log_repository_skipped.txt", true)) {
            logWriter.append(repoPath + " was skipped due to excessive errors.\n");
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write to log file", e);
        }

        errorCount = 0;
        methodCounts.clear();
    }

    private static void analyzeMethod(MethodDeclaration method, String repoPath, String fileName, String imports, String annotations) {
        // Insert code to analyze the method here
        new MethodCallVisitor(repoPath, fileName, imports, annotations).visit(method, null);
    }

    private static class AnnotationVisitor extends VoidVisitorAdapter<Void> {
        List<String> annotations = new ArrayList<>();

        @Override
        public void visit(AnnotationDeclaration n, Void arg) {
            super.visit(n, arg);
            annotations.add(n.getNameAsString());
        }
    }

    private static class ImportVisitor extends VoidVisitorAdapter<Void> {
        List<String> imports = new ArrayList<>();

        @Override
        public void visit(ImportDeclaration n, Void arg) {
            super.visit(n, arg);
            imports.add(n.getNameAsString());
        }
    }

    private static class MethodCallVisitor extends VoidVisitorAdapter<Void> {
        private String repoPath;
        private String fileName;
        private String imports;
        private String annotations;

        MethodCallVisitor(String repoPath, String fileName, String imports, String annotations) {
            this.repoPath = repoPath;
            this.fileName = fileName;
            this.imports = imports;
            this.annotations = annotations;
        }

        @Override
        public void visit(MethodCallExpr n, Void arg) {
            super.visit(n, arg);
            String currentRepoName = Paths.get(repoPath).getFileName().toString();
            String methodName = n.getNameAsString();

            String params = n.getArguments().stream()
                    .map(argument -> {
                        try {
                            return argument.calculateResolvedType().describe();
                        } catch (Exception e) {
                            return "unknown";
                        }
                    })
                    .collect(Collectors.joining(";"));

            String key = currentRepoName + "," + fileName + "," + methodName + "," + imports + "," + params + "," + annotations;

            methodCounts.put(key, methodCounts.getOrDefault(key, 0) + 1);
        }
    }

    private static void writeCountsToCsv(String outputFile) {
        try (FileWriter csvWriter = new FileWriter(outputFile, true)) {
            counterTotalRepoWritten++;
            csvWriter.append("Repository");
            csvWriter.append(",");
            csvWriter.append("File");
            csvWriter.append(",");
            csvWriter.append("Method");
            csvWriter.append(",");
            csvWriter.append("Count");
            csvWriter.append(",");
            csvWriter.append("Imports");
            csvWriter.append(",");
            csvWriter.append("Parameters");
            csvWriter.append(",");
            csvWriter.append("Annotations");
            csvWriter.append("\n");

            for (Map.Entry<String, Integer> entry : methodCounts.entrySet()) {
                String[] parts = entry.getKey().split(",", 6);

                csvWriter.append(parts[0]);
                csvWriter.append(",");
                csvWriter.append(parts[1]);
                csvWriter.append(",");
                csvWriter.append(parts[2]);
                csvWriter.append(",");
                csvWriter.append(entry.getValue().toString());
                csvWriter.append(",");
                csvWriter.append("[" + parts[3].replace(",", ";") + "]");
                csvWriter.append(",");
                csvWriter.append("[" + parts[4].replace(",", ";") + "]");
                csvWriter.append(",");
                csvWriter.append("[" + parts[5].replace(",", ";") + "]");
                csvWriter.append("\n");
            }
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to write to CSV file", e);
        }
    }

    static class FileStats {
        private final String repository;
        private final String file;
        private final String date;
        private final String year;
        private final long loc;

        public FileStats(String repository, String file, String date, String year, long loc) {
            this.repository = repository;
            this.file = file;
            this.date = date;
            this.year = year;
            this.loc = loc;
        }

        public String getRepository() {
            return repository;
        }

        public String getFile() {
            return file;
        }

        public String getDate() {
            return date;
        }

        public String getYear() {
            return year;
        }

        public long getLoc() {
            return loc;
        }
    }
}

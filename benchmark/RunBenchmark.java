import org.opt4j.optimizers.ea.*;
import org.opt4j.optimizers.de.*;
import org.opt4j.optimizers.rs.*;
import org.opt4j.optimizers.sa.*;
import org.opt4j.optimizers.mopso.*;

import org.opt4j.benchmarks.dtlz.*;
import org.opt4j.benchmarks.wfg.*;
import org.opt4j.benchmarks.zdt.*;

import org.opt4j.viewer.*;
import org.opt4j.core.start.*;
import org.opt4j.core.optimizer.*;
import org.opt4j.core.*;
import java.io.*;
import java.util.*;
import org.opt4j.core.problem.*;
import org.opt4j.optimizer.ea.espea.*;
import java.util.concurrent.*;
import java.lang.*;
import java.util.concurrent.atomic.*;
import org.opt4j.bcceval.*;
import com.google.inject.*;
import org.opt4j.core.common.archive.*;
import java.lang.management.*;
import java.util.logging.*;

public class RunBenchmark
{

    private static int threads = 3;
    private static final int REPS = 200;
    private static final int WARMUP = 5;

    private static long runtimeLimit = 3_000_000_000L; // ns

    private static final int EVALUATION_LIMIT = 300_000;
    private static final int ARCHIVE_SIZE = 300;
    private static final int ITERATIONS = 1000;
    private static final int GENERATION_SIZE = 100;
    private static final double DE_SCALING_FACTOR = 0.5;
    private static final EnergyArchive.ReplacementStrategy REPLACEMENT_STRATEGY
        = EnergyArchive.ReplacementStrategy.WORST_IN_ARCHIVE;
    private static final ESPEAModule.ScalarizationFunctionType SCALARIZATION_FUNCTION
        = ESPEAModule.ScalarizationFunctionType.NO_PREFERENCE;
    private static final EnergyCache.MutationStrategy CACHE_MUTATION_STRATEGY
        = EnergyCache.MutationStrategy.INVERSE;

    private static void saveConfig() throws Exception {
        FileWriter fw = new FileWriter(dataDir+CONFIG_FILENAME);
        BufferedWriter dataStream =
            new BufferedWriter(fw);
        dataStream.write("threads = "+threads+"\n"+
                         "reps = "+REPS+"\n"+
                         "warmup = "+WARMUP+"\n"+
                         "cputime_limit = "+runtimeLimit+"\n"+
                         "evaluation_limit = "+EVALUATION_LIMIT+"\n"+
                         "archive_size = "+ARCHIVE_SIZE+"\n"+
                         "iterations = "+ITERATIONS+"\n"+
                         "generation_size = "+GENERATION_SIZE+"\n"+
                         "differential_evolution_scaling_factor = "+DE_SCALING_FACTOR+"\n"+
                         "replacement_strategy = "+REPLACEMENT_STRATEGY+"\n"+
                         "scalarization_function = "+SCALARIZATION_FUNCTION+"\n"+
                         "cache_mutation_strategy = "+CACHE_MUTATION_STRATEGY+"\n");
        dataStream.close();
    }

    private static List<Optimizer> optimizers() {
        List<Optimizer> os = new LinkedList<Optimizer>();
        switch (test) {
        case GENERAL:
            general(os);
            break;
        case CACHE_MUTATION_STRATEGY:
            cacheMutationStrategy(os);
            break;
        case CACHE_MUTATION_STRATEGY_RUNTIME_LIMIT:
            cacheMutationStrategy(os);
            break;
        case PARALLEL_COMPLETION:
            parallelCompletion(os);
            break;
        case GENERATION_SIZE:
            batchProcessing(os);
            break;
        default:
            throw new RuntimeException("No optimizers defined for test "+test);
        }
        return os;
    }

    private static ESPEAModule standardESPEA() {
        ESPEAModule espea = new ESPEAModule();
        espea.setArchiveCapacity(ARCHIVE_SIZE);
        espea.setInitialPopulationSize(ARCHIVE_SIZE);
        espea.setDifferentialEvolutionScalingFactor(DE_SCALING_FACTOR);
        espea.setReplacementStrategy(REPLACEMENT_STRATEGY);
        espea.setScalarizationFunctionType(SCALARIZATION_FUNCTION);
        espea.setCacheMutationStrategy(CACHE_MUTATION_STRATEGY);
        return espea;
    }

    private static void variance(List<Optimizer> os) {
        for (int i = 0; i < 1; i++) {
            ESPEAModule espea = standardESPEA();
            os.add(new Optimizer("EAPEA", espea));
        }
    }

    private static void varianceEA(List<Optimizer> os) {
        for (int i = 0; i < 10; i++) {
            EvolutionaryAlgorithmModule ea = new EvolutionaryAlgorithmModule();
            ea.setGenerations(ITERATIONS);
            ea.setAlpha(GENERATION_SIZE);
            os.add(new Optimizer("EA", ea));
        }
    }

    private static void batchProcessing(List<Optimizer> os) {
        {
            ESPEAModule espea = standardESPEA();
            espea.setEarlyGenerationSize(100);
            os.add(new Optimizer("100", espea));
        }
        {
            ESPEAModule espea = standardESPEA();
            espea.setEarlyGenerationSize(200);
            os.add(new Optimizer("200", espea));
        }
    }

    private static void parallelCompletion(List<Optimizer> os) {
        {
            ESPEAModule espea = standardESPEA();
            espea.setIndividualCompleterMaxThreads(1);
            os.add(new Optimizer("seq", espea));
        }
        {
            ESPEAModule espea = standardESPEA();
            espea.setIndividualCompleterMaxThreads(4);
            os.add(new Optimizer("par4", espea));
        }
    }

    private static void cacheMutationStrategy(List<Optimizer> os) {
        {
            ESPEAModule espea = standardESPEA();
            espea.setCacheMutationStrategy(EnergyCache.MutationStrategy.INVERSE);
            os.add(new Optimizer("INV", espea));
        }
        {
            ESPEAModule espea = standardESPEA();
            espea.setCacheMutationStrategy(EnergyCache.MutationStrategy.RECALCULATE);
            os.add(new Optimizer("REC", espea));
        }
    }

    private static void general(List<Optimizer> os) {
        RandomSearchModule rs = new RandomSearchModule();
        rs.setIterations(ITERATIONS);
        rs.setBatchsize(GENERATION_SIZE);
        os.add(new Optimizer("RS", rs));

        EvolutionaryAlgorithmModule ea = new EvolutionaryAlgorithmModule();
        ea.setGenerations(ITERATIONS);
        ea.setAlpha(GENERATION_SIZE);
        os.add(new Optimizer("EA", ea));

        DifferentialEvolutionModule de = new DifferentialEvolutionModule();
        de.setGenerations(ITERATIONS);
        de.setAlpha(GENERATION_SIZE);
        de.setScalingFactor(DE_SCALING_FACTOR);
        os.add(new Optimizer("DE", de));

        MOPSOModule mopso = new MOPSOModule();
        mopso.setArchiveSize(ARCHIVE_SIZE);
        mopso.setIterations(ITERATIONS);
        os.add(new Optimizer("MOPSO", mopso));

        SimulatedAnnealingModule sa = new SimulatedAnnealingModule();
        sa.setIterations(ITERATIONS);
        os.add(new Optimizer("SA", sa));

        {
            ESPEAModule espea = standardESPEA();
            os.add(new Optimizer("EAPEA", espea));
        }
    }

    private static List<Problem> problems() {
        List<Problem> ps = new LinkedList<Problem>();

        for (DTLZModule.Function f : DTLZModule.Function.values()) {
            // if (f != DTLZModule.Function.DTLZ1 &&
                // f != DTLZModule.Function.DTLZ6) continue;
            DTLZModule module = new DTLZModule();
            module.setFunction(f);
            module.setEncoding(DTLZModule.Encoding.DOUBLE);
            if (module.getEncoding() == DTLZModule.Encoding.DOUBLE) {
                ps.add(new Problem(f.toString(), module, ps.size()));
            }
        }

        for (WFGModule.Function f : WFGModule.Function.values()) {
            // if (f != WFGModule.Function.WFG2 &&
                // f != WFGModule.Function.I3) continue;
            WFGModule module = new WFGModule();
            module.setFunction(f);
            module.setEncoding(WFGModule.Encoding.DOUBLE);
            if (module.getEncoding() == WFGModule.Encoding.DOUBLE) {
                ps.add(new Problem(f.toString(), module, ps.size()));
            }
        }

        for (ZDTModule.Function f : ZDTModule.Function.values()) {
            // continue;
            ZDTModule module = new ZDTModule();
            module.setFunction(f);
            module.setEncoding(ZDTModule.Encoding.DOUBLE);
            if (module.getEncoding() == ZDTModule.Encoding.DOUBLE) {
                ps.add(new Problem(f.toString(), module, ps.size()));
            }
        }

        return ps;
    }

    private static final RuntimeException RUNTIME_EXCEEDED_EXCEPTION
        = new RuntimeException("Runtime exceeded");

    private static String dataDir = null;
    private static final String LOGFILE = "run.log";
    private static final String CONFIG_FILENAME = "config.txt";
    private static final String SCORES_FILENAME = "scores.tsv";
    private static final String SCORES_PLOT_FILENAME = "scores.pdf";
    private static final String HYPERVOLUMES_FILENAME = "hypervolumes.tsv";
    private static final String RUNTIME_FILENAME = "runtime.tsv";
    private static final String RUNTIME_PLOT_FILENAME = "runtime.pdf";

    private static final String GNUPLOT_RUNTIME = "./runtime.gp";
    private static final String GNUPLOT_SCORES = "./scores.gp";

    private static Logger logger = null;
    private static Runtime runtime = null;
    private static Test test = Test.GENERAL;

    private enum Test {
        GENERAL,
        CACHE_MUTATION_STRATEGY,
        CACHE_MUTATION_STRATEGY_RUNTIME_LIMIT,
        PARALLEL_COMPLETION,
        GENERATION_SIZE;
    }

    public static void main(String[] args) throws Exception {

        boolean skipArchiveGeneration = false;
        for (String arg : args) {
            skipArchiveGeneration |= arg.equals("skipgen");
            if (arg.equals("general")) {
                test = Test.GENERAL;
            } else if (arg.equals("cachemut")) {
                test = Test.CACHE_MUTATION_STRATEGY;
            } else if (arg.equals("cachemut_rl")) {
                test = Test.CACHE_MUTATION_STRATEGY_RUNTIME_LIMIT;
                runtimeLimit = 1_000_000_000L;
            } else if (arg.equals("parcompl")) {
                test = Test.PARALLEL_COMPLETION;
                threads = 1;
            } else if (arg.equals("gensize")) {
                test = Test.GENERATION_SIZE;
            }
        }
        final boolean genArchives = !skipArchiveGeneration;

        dataDir = "./data/"+ test.toString().toLowerCase() +"/";
        File dataDirFile = new File(dataDir);
        if (!dataDirFile.exists()) {
            dataDirFile.mkdir();
        }

        // End of initialization

        runtime = Runtime.getRuntime();
        logger = logger();
        saveConfig();

        List<Problem> ps = problems();
        List<Optimizer> os = optimizers();

        if (genArchives) writeArchives(ps, os);

        int rows = ps.size();
        int columns = os.size();

        double[][] avg_hvs = new double[rows][columns];
        double[][] min_hvs = new double[rows][columns];
        double[][] max_hvs = new double[rows][columns];
        double[] problem_max_hvs = new double[rows];
        double[] problem_min_hvs = new double[rows];

        for (int i = 0; i < rows; i++) {
            double[] hvSamples = hypervolumesForProblem(columns * REPS, ps.get(i).filename);

            problem_max_hvs[i] = Double.NEGATIVE_INFINITY;
            problem_min_hvs[i] = Double.POSITIVE_INFINITY;
            for (int j = 0; j < columns; j++) {
                double avg = 0;
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for (int k = 0; k < REPS; k++) {
                    double sample = hvSamples[j * REPS + k];
                    avg += sample;
                    min = Math.min(min, sample);
                    max = Math.max(max, sample);
                    problem_max_hvs[i] = Math.max(problem_max_hvs[i], sample);
                    problem_min_hvs[i] = Math.min(problem_min_hvs[i], sample);
                }
                avg /= REPS;

                avg_hvs[i][j] = avg;
                min_hvs[i][j] = min;
                max_hvs[i][j] = max;
            }
        }

        double[] totals = new double[columns]; // Average scores per optimizer.
        double[] min_totals = new double[columns];
        double[] max_totals = new double[columns];
        for (int i = 0; i < columns; i++) {
            min_totals[i] = Double.POSITIVE_INFINITY;
            max_totals[i] = Double.NEGATIVE_INFINITY;
        }

        double[][] avg_scores = new double[rows][columns];
        double[][] min_scores = new double[rows][columns];
        double[][] max_scores = new double[rows][columns];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                // Percentage of row max
                // double dividend = problem_max_hvs[i] - problem_min_hvs[i];
                // double min = problem_min_hvs[i];
                double dividend = problem_max_hvs[i];
                double min = 0;
                avg_scores[i][j] = (avg_hvs[i][j] - min) / dividend;
                min_scores[i][j] = (min_hvs[i][j] - min) / dividend;
                max_scores[i][j] = (max_hvs[i][j] - min) / dividend;

                totals[j] += avg_scores[i][j];
                min_totals[j] = Math.min(min_totals[j], avg_scores[i][j]);
                max_totals[j] = Math.max(max_totals[j], avg_scores[i][j]);
            }
        }

        for (int j = 0; j < columns; j++) {
            totals[j] /= rows;
        }

        writeErrorTable(dataDir+HYPERVOLUMES_FILENAME, ps, os, avg_hvs, min_hvs, max_hvs, null);
        writeErrorTable(dataDir+SCORES_FILENAME, ps, os, avg_scores, min_scores, max_scores, totals, min_totals, max_totals);

        gnuplot(dataDir+SCORES_FILENAME,
                dataDir+SCORES_PLOT_FILENAME,
                GNUPLOT_SCORES);
        gnuplot(dataDir+RUNTIME_FILENAME,
                dataDir+RUNTIME_PLOT_FILENAME,
                GNUPLOT_RUNTIME);
	}

    private static void gnuplot(String infile, String outfile, String script) throws Exception {
        final String cmd = "gnuplot -c "+script+" "+infile+" "+outfile;

        logger.info(cmd);
        Process proc = runtime.exec(cmd);
        BufferedReader errorStream =
            new BufferedReader(new InputStreamReader(proc.getErrorStream()));
        String outline = errorStream.readLine();
        while (outline != null) {
            logger.info(outline);
            outline = errorStream.readLine();
        }
        proc.waitFor();
    }

    private static Logger logger() throws Exception {
        Logger logger = Logger.getGlobal();
        FileHandler fh;
        fh = new FileHandler(dataDir+LOGFILE);
        logger.addHandler(fh);
        SimpleFormatter formatter = new SimpleFormatter();
        fh.setFormatter(formatter);
        return logger;
    }

    private static void writeArchives(List<Problem> ps, List<Optimizer> os) throws Exception {
        int rows = ps.size();
        int columns = os.size();

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        ThreadMXBean tmxb = ManagementFactory.getThreadMXBean();

        BoundCompleterModule boundCompleterModule = new BoundCompleterModule();
        boundCompleterModule.setBound(EVALUATION_LIMIT);

        // Obtain archives
        List<List<Future<IndividualSet[]>>> futures = new LinkedList<List<Future<IndividualSet[]>>>();

        int totalRuns = rows * columns * (WARMUP + REPS);
        AtomicInteger done = new AtomicInteger();
        logger.info("Starting "+totalRuns+" optimizer runs...");

        double[][] avg_times = new double[rows][columns];
        double[][] min_times = new double[rows][columns];
        double[][] max_times = new double[rows][columns];

        for (int i = 0; i < ps.size(); i++) {
            Problem p = ps.get(i);
            List<Future<IndividualSet[]>> row = new LinkedList<Future<IndividualSet[]>>();
            for (int j = 0; j < os.size(); j++) {
                Optimizer o = os.get(j);
                final int i_copy = i;
                final int j_copy = j;
                Future<IndividualSet[]> future
                    = pool.submit(new Callable<IndividualSet[]>() {
                            public IndividualSet[] call() throws Exception {
                                return optimizerRun(p, o, tmxb,
                                                    boundCompleterModule, done,
                                                    totalRuns, avg_times,
                                                    min_times, max_times,
                                                    i_copy, j_copy);
                            }
                        });
                row.add(future);
            }
            futures.add(row);
        }

        // Every row is a problem.
        IndividualSet[][][] as = new IndividualSet[rows][columns][];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                as[i][j] = futures.get(i).get(j).get();
                logger.info("Received future ["+i+","+j+"]");
            }
        }

        pool.shutdown();
        logger.info("All optimizers finished.");

        writeErrorTable(dataDir+RUNTIME_FILENAME, ps, os, avg_times, min_times, max_times, null);

        File archiveDir = new File(dataDir+"archives/");
        if (!archiveDir.exists()) {
            archiveDir.mkdir();
        }

        for (int i = 0; i < rows; i++) {
            List<IndividualSet> serialized = new LinkedList<IndividualSet>();
            for (int j = 0; j < columns; j++) {
                for (int k = 0; k < REPS; k++) {
                    serialized.add(as[i][j][k]);
                }
            }

            writeArchivesForProblem(serialized, ps.get(i).filename);
        }
    }

    public static class BoundedOpt4JTask extends Opt4JTask {
        private long startCPUTime;
        private long startTime;

        public long timeElapsed;
        public long cpuTimeElapsed;
        public int iterations;

        private final ThreadMXBean tmxb;
        public final String name;

        public BoundedOpt4JTask(ThreadMXBean tmxb, String name) {
            super(false);
            this.tmxb = tmxb;
            this.name = name;
        }

        public void execute() throws Exception {
            startCPUTime = tmxb.getCurrentThreadCpuTime();
            startTime = System.nanoTime();
            super.execute();
        }

        public void iterationComplete(int iteration) {
            long curCPUTime = tmxb.getCurrentThreadCpuTime();
            long curTime = System.nanoTime();
            this.iterations = iteration;
            cpuTimeElapsed = curCPUTime - startCPUTime;
            timeElapsed = curTime - startTime;
            super.iterationComplete(iteration);
            if (cpuTimeElapsed >= runtimeLimit) {

                throw RUNTIME_EXCEEDED_EXCEPTION;
            }
        }
    }

    private static IndividualSet[] optimizerRun(final Problem p, final Optimizer o,
                                          final ThreadMXBean tmxb,
                                          final BoundCompleterModule boundCompleterModule,
                                          final AtomicInteger done,
                                          final int totalRuns,
                                          final double[][] avg_times,
                                          double[][] min_times,
                                          double[][] max_times, int i,
                                          int j) throws Exception {
        IndividualSet[] archive_samples = new IndividualSet[REPS];
        double[] time_samples = new double[REPS];

        for (int k = 0; k < WARMUP; k++) {
            BoundedOpt4JTask task = new BoundedOpt4JTask(tmxb, o.name+"+"+p.name);
            if (test == Test.PARALLEL_COMPLETION) {
                task.init(o.module, p.module);
            } else {
                task.init(boundCompleterModule, o.module, p.module);
            }
            try {
                try {
                    task.execute();
                } catch (RuntimeException e) {
                    if (e != RUNTIME_EXCEEDED_EXCEPTION) {
                        throw e;
                    }
                }
            } catch (Exception e) {
                throw e;
            } finally {
                task.close();
            }
        }

        for (int k = 0; k < REPS; k++) {
            IndividualSet archive = null;
            int retry = 0;
            do {
                BoundedOpt4JTask task = new BoundedOpt4JTask(tmxb, o.name+"+"+p.name);

                if (test == Test.PARALLEL_COMPLETION) {
                    task.init(o.module, p.module);
                } else {
                    task.init(boundCompleterModule, o.module, p.module);
                }

                try {
                    try {
                        runtime.gc();
                        task.execute();
                    } catch (RuntimeException e) {
                        if (e != RUNTIME_EXCEEDED_EXCEPTION) {
                            throw e;
                        }
                    }

                    archive = task.getInstance(Archive.class);
                    if (archive.size() != ARCHIVE_SIZE) {
                        logger.warning(
                            o.name+ " returned "+archive.size()+ " points for "+p.name+
                            " (retry "+(retry++)+ ")");
                    }

                    double cpuSeconds = task.cpuTimeElapsed / 1.0E9;
                    double seconds = task.timeElapsed / 1.0E9;
                    logger.info("Terminated "+task.name+" after "+task.iterations+" iterations and "+cpuSeconds+"s cpu / "+seconds+"s real time.");

                    time_samples[k] = task.cpuTimeElapsed;

                    IndividualSet cleanArchive = new IndividualSet();
                    cleanArchive.addAll(archive);
                    archive_samples[k] = cleanArchive;

                    int cur = done.incrementAndGet();
                    logger.info(cur+"/"+totalRuns+" done.");

                } catch (Exception e) {
                    e.printStackTrace();
                    archive = null;
                } finally {
                    task.close();
                }

            } while (archive == null || archive.size() < 2);
        }

        double avg_time = 0.0;
        double min_time = Double.POSITIVE_INFINITY;
        double max_time = Double.NEGATIVE_INFINITY;
        for (int k = 0; k < REPS; k++) {
            double sample = time_samples[k];
            avg_time += sample;
            min_time = Math.min(min_time, sample);
            max_time = Math.max(max_time, sample);
        }
        avg_time /= REPS;

        avg_times[i][j] = avg_time;
        min_times[i][j] = min_time;
        max_times[i][j] = max_time;

        return archive_samples;
    }

    private static void writeArchivesForProblem(List<IndividualSet> archives, String problem) throws Exception {
        BufferedWriter dataStream =
            new BufferedWriter(new FileWriter(dataDir+"archives/"+problem+".pfs"));
        for (IndividualSet archive : archives) {
            for (Individual individual : archive) {
                double[] objectives = individual.getObjectives().array();
                for (double val : objectives) {
                    dataStream.write(val+" ");
                }
                dataStream.write("\n");
            }
            // Empty line to seperate input sets.
            dataStream.write("\n");
        }
        dataStream.close();
    }

    private static double[] hypervolumesForProblem(int archiveCount, String problem) throws Exception {
        Process proc = runtime.exec("./hv/hv "+dataDir+"archives/"+problem+".pfs");
        BufferedReader resultStream =
            new BufferedReader(new InputStreamReader(proc.getInputStream()));
        BufferedReader errorStream =
            new BufferedReader(new InputStreamReader(proc.getErrorStream()));

        double[] hypervolumes = new double[archiveCount];
        for (int j = 0; j < hypervolumes.length; j++) {
            String hvString = resultStream.readLine();
            try {
                hypervolumes[j] = Double.parseDouble(hvString);
                assert !Double.isNaN(hypervolumes[j]) : "Parsed "+hvString+" and got "+hypervolumes[j];
            } catch (Exception e) {
                logger.severe("j = "+j+", hvString = "+hvString+", problem = "+problem);
                String line = errorStream.readLine();
                while (line != null) {
                    logger.info(line);
                    line = errorStream.readLine();
                }
                throw e;
            }
        }

        resultStream.close();
        proc.waitFor();

        return hypervolumes;
    }

    private static void writeErrorTable(String filename,
                                        List<Problem> problems,
                                        List<Optimizer> optimizers,
                                        double[][] avg,
                                        double[][] min,
                                        double[][] max,
                                        double[] totals) throws Exception
    {
        writeErrorTable(filename, problems, optimizers, avg, min, max, totals, null, null);
    }

    private static void writeErrorTable(String filename,
                                        List<Problem> problems,
                                        List<Optimizer> optimizers,
                                        double[][] avg,
                                        double[][] min,
                                        double[][] max,
                                        double[] totals,
                                        double[] min_totals,
                                        double[] max_totals) throws Exception
    {
        FileWriter fw = new FileWriter(filename);
        BufferedWriter file =
            new BufferedWriter(fw);

        for (int i = 0; i < optimizers.size(); i++) {
            String on = optimizers.get(i).name;
            file.write("\n"+on+"\tAVG\tMIN\tMAX");
            if (totals != null) {
                file.write("\nTotal\t"+totals[i]+"\t"+min_totals[i]+"\t"+max_totals[i]);
            }
            for (int j = 0; j < problems.size(); j++) {
                String pn = problems.get(j).name;
                file.write("\n"+pn+"\t"+avg[j][i]+"\t"+min[j][i]+"\t"+max[j][i]);
            }
            file.write("\n\n");
        }

        file.close();
    }

    public static class Problem {
        public final String name;
        public final ProblemModule module;
        public final String filename;

        public Problem(String name, ProblemModule module, int index) {
            this.name = name;
            this.module = module;
            this.filename = index+"-"+name;
        }

        public String toString() {
            return name + "-" + module;
        }
    }

    public static class Optimizer {
        public final String name;
        public final OptimizerModule module;

        public Optimizer(String name, OptimizerModule module) {
            this.name = name;
            this.module = module;
        }

        public String toString() {
            return name + "-" + module;
        }
    }

}

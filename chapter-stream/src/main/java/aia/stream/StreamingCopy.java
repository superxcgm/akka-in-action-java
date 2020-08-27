package aia.stream;

import akka.actor.ActorSystem;
import akka.stream.IOResult;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.RunnableGraph;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.util.ByteString;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.concurrent.CompletionStage;

public class StreamingCopy {
    public static void main(String[] args) {
        Config config = ConfigFactory.load();
        // not used
        int maxLine = config.getInt("log-stream-processor.max-line");

        if (args.length != 2) {
            System.err.println("Provided args: input-file output-file");
            System.exit(1);
        }

        Path inputFile = Paths.get(args[0]);
        Path outputFile = Paths.get(args[1]);

        Source<ByteString, CompletionStage<IOResult>> source = FileIO.fromPath(inputFile);

        HashSet<OpenOption> options = new HashSet<>(Arrays.asList(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND));
        Sink<ByteString, CompletionStage<IOResult>> sink = FileIO.toPath(outputFile, options);

        RunnableGraph<CompletionStage<IOResult>> runnableGraph = source.to(sink);

        ActorSystem system = ActorSystem.create();
        runnableGraph.run(system).whenComplete(((ioResult, throwable) -> {
            System.out.println(ioResult.status() + ", " + ioResult.count() + " bytes read.");
            system.terminate();
        }));
    }
}

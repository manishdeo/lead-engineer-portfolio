import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.TimeUnit;

/**
 * A simple demo to illustrate the "Numbers Every Engineer Should Know" cheat sheet.
 * This program compares the time taken to read 1MB of data sequentially from memory
 * versus reading it from an SSD/HDD, highlighting the massive latency difference.
 */
public class LatencyNumbersDemo {

    private static final int ONE_MEGABYTE = 1024 * 1024;

    public static void main(String[] args) throws IOException {
        System.out.println("--- Latency Numbers Demonstration ---");

        // 1. Read 1MB from Memory
        byte[] memoryArray = new byte[ONE_MEGABYTE];
        long startTimeMem = System.nanoTime();
        for (int i = 0; i < memoryArray.length; i++) {
            byte b = memoryArray[i]; // Read operation
        }
        long endTimeMem = System.nanoTime();
        long durationMem = TimeUnit.NANOSECONDS.toMicros(endTimeMem - startTimeMem);
        System.out.printf("Time to read 1MB from memory: %d microseconds (µs)%n", durationMem);

        // 2. Read 1MB from Disk
        File tempFile = createTempFile();
        long startTimeDisk = System.nanoTime();
        try (RandomAccessFile file = new RandomAccessFile(tempFile, "r");
             FileChannel channel = file.getChannel()) {
            ByteBuffer buffer = ByteBuffer.allocate(ONE_MEGABYTE);
            channel.read(buffer);
            buffer.flip();
        }
        long endTimeDisk = System.nanoTime();
        long durationDisk = TimeUnit.NANOSECONDS.toMillis(endTimeDisk - startTimeDisk);
        System.out.printf("Time to read 1MB from disk: %d milliseconds (ms)%n", durationDisk);
        
        tempFile.delete();

        System.out.println("\nConclusion: Reading from memory is orders of magnitude faster than disk.");
    }

    private static File createTempFile() throws IOException {
        File tempFile = File.createTempFile("latency-demo", ".tmp");
        tempFile.deleteOnExit();
        try (RandomAccessFile file = new RandomAccessFile(tempFile, "rw")) {
            file.setLength(ONE_MEGABYTE); // Pre-allocate 1MB file
        }
        return tempFile;
    }
}

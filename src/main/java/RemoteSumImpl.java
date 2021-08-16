import java.io.FileNotFoundException;
import java.net.http.HttpTimeoutException;
import java.util.Random;

public class RemoteSumImpl implements RemoteSumStub {
    private static Random r = new Random();

    private int maxDurationInMillis;
    private int failureCoefficient;
    private int numFiles;

    /**
     * Constructor for configuration injection
     * @param maxDurationInMillis Maximum duration in milliseconds that the network call may take
     * @param failureCoefficient 1 call out of every failureCoeffiecient will fail with a HttpTimeoutException
     */
    public RemoteSumImpl(int maxDurationInMillis, int failureCoefficient, int numFiles) {
        this.maxDurationInMillis = maxDurationInMillis;
        this.failureCoefficient = failureCoefficient;
        this.numFiles = numFiles;
    }

    @Override
    public long getSum(int machineId, int fileId) throws HttpTimeoutException, FileNotFoundException {
        if (fileId >= numFiles) {
            throw new FileNotFoundException("This file doesn't exist");
        }
        int networkDelay = r.nextInt(maxDurationInMillis);
        try {
            // Simulate a synchronous blocking delay
            Thread.sleep(networkDelay);
        } catch (InterruptedException ie) {
            System.err.println("Call interrupted. This shouldn't have happened.");
        }
        // Roll a failureCoefficient headed dice and fail if needed
        int dieRoll = r.nextInt(failureCoefficient);
        if (dieRoll == 1) { // If failureCoefficient is 100, roughly 1 out of 100 calls will fail with this
            throw new HttpTimeoutException("Failed calling RemoteSum service.");
        }
        // Return a valid sum
        return r.nextLong();
    }
}

import java.io.FileNotFoundException;
import java.net.http.HttpTimeoutException;

/**
 * Interface representing the stub for a RemoteSum rpc call
 */
public interface RemoteSumStub {
    /**
     * Gets sum of integers in a file from a machine
     * @param machineId An integer representing the machineId
     * @param fileId An integer representing the fileName.
     * @return Returns the sum of all integers in teh file by sending the request to the machine
     * @throws HttpTimeoutException when the machine doesn't respond in reasonable amount of time.
     */
    long getSum(int machineId, int fileId) throws HttpTimeoutException, FileNotFoundException;
}

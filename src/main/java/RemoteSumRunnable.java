import java.net.http.HttpTimeoutException;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Semaphore;

public class RemoteSumRunnable implements Runnable {
    private int fileId;
    private int machineId;
    private RemoteSumStub remoteSumStubRpcCaller;
    private Semaphore semaphore;
    private Queue<Integer> failedQueue;
    private BlockingQueue<Long> resultQueue;
    private ConcurrentMap<Integer, Boolean> busyMachinesMap;

    public RemoteSumRunnable(int fileId,
                             int machineId,
                             RemoteSumStub remoteSumStubRpcCaller,
                             Semaphore availableThreads,
                             Queue<Integer> failedQueue,
                             BlockingQueue<Long> resultQueue,
                             ConcurrentMap<Integer, Boolean> busyMachinesMap) {
        this.fileId = fileId;
        this.machineId = machineId;
        this.remoteSumStubRpcCaller = remoteSumStubRpcCaller;
        this.semaphore = availableThreads;
        this.failedQueue = failedQueue;
        this.resultQueue = resultQueue;
        this.busyMachinesMap = busyMachinesMap;
    }

    @Override
    public void run() {
        try {
            long result = remoteSumStubRpcCaller.getSum(machineId, fileId);
            System.out.println("Got result " + result + " from machine " + machineId + " for file " + fileId);
            resultQueue.put(result);
        } catch (HttpTimeoutException | InterruptedException e) {
            System.err.println("Errored getting result from machine " + machineId + " for file " + fileId);
            failedQueue.offer(fileId);
        }
        finally {
            System.out.println("Releasing sempahore. Remanining capacity is " + semaphore.availablePermits());
            semaphore.release();
            busyMachinesMap.remove(machineId);
        }
    }
}

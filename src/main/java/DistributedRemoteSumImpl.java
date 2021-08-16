import java.io.FileNotFoundException;
import java.net.http.HttpTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class DistributedRemoteSumImpl implements DistributedRemoteSum {
    private int numMachines;
    private int numFiles;
    private RemoteSumStub remoteSumStubRpcCaller;
    private int degreeOfParallelism;

    private ConcurrentMap<Integer, Boolean> busyMachinesMap;
    private Lock lock = new ReentrantLock();
    private Queue<Integer> failedJobQueue = new ConcurrentLinkedQueue<>();
    private AtomicInteger currentFile = new AtomicInteger(0);

    public DistributedRemoteSumImpl(int numMachines, int numFiles, RemoteSumStub remoteSumStubRpcCaller, int degreeOfParallelism) {
        this.numMachines = numMachines;
        this.numFiles = numFiles;
        this.remoteSumStubRpcCaller = remoteSumStubRpcCaller;   // injected dependency
        this.busyMachinesMap = new ConcurrentHashMap<>();
        this.degreeOfParallelism = degreeOfParallelism;
    }

    @Override
    public long calculateSum() {
        List<Future<?>> futures = new ArrayList<>();
        final AtomicLong runningSum = new AtomicLong();
        for (int i = 0; i < degreeOfParallelism; i++) {
            futures.add(Executors.newSingleThreadExecutor().submit(() -> {
                while (true) {
                    int fileId = getNextFile();
                    if (fileId != -1) {
                        int machineId = getFreeMachine();
                        System.out.println("Got file " + fileId + " to process on machine " + machineId);
                        try {
                            long localSum = remoteSumStubRpcCaller.getSum(machineId, fileId);
                            System.out.println("Got result " + localSum + " from machine " + machineId + " for file " + fileId);
                            runningSum.addAndGet(localSum);
                        } catch (HttpTimeoutException hte) {
                            System.err.println("Errored getting result from machine " + machineId + " for file " + fileId);
                            failedJobQueue.offer(fileId);
                        } catch (FileNotFoundException fnfe) {
                            System.err.println("The file " + fileId + " was not found on machine " + machineId);
                            // TODO This shouldn't be retriable. The thread should end.
                        }
                        finally {
                            busyMachinesMap.remove(machineId);
                        }
                    } else {
                        break;
                    }
                }
            }));
        }
        for (Future f : futures) {
            try {
                f.get();
            } catch (InterruptedException | ExecutionException ie) {
                System.out.println("Interrupted during concurrent execution.");
            }
        }
        return runningSum.get();
    }

    private int getFreeMachine() {
        // TODO Iterating through a map inside a lock!!! Nooooooooooooo!
        // Other option is to keep a queue of available machines. But that would need a numMachines sized queue.
        for (int i = 0; i < numMachines; i++) {
            lock.lock();
            if (busyMachinesMap.get(i) == null) {
                busyMachinesMap.put(i, true);
                lock.unlock();
                return i;
            }
            lock.unlock();
        }
        return -1;
    }

    private int getNextFile() {
        Integer failedJob;
        if ((failedJob = failedJobQueue.poll()) != null) {
            return failedJob;
        }
        int fileToReturn = currentFile.getAndIncrement();
        if (fileToReturn < numFiles) {
            return fileToReturn;
        } else {
            return -1;
        }
    }
}


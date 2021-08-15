import java.util.Queue;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class RemoteSumManagerImpl implements RemoteSumManager {
    private int numMachines;
    private int numFiles;
    private RemoteSumStub remoteSumStubRpcCaller;
    private int degreeOfParallelism;

    private ConcurrentMap<Integer, Boolean> busyMachinesMap;
    private Lock lock = new ReentrantLock();
    private Queue<Integer> failedJobQueue = new ConcurrentLinkedQueue<>();
    private AtomicInteger currentFile = new AtomicInteger(0);
    private Semaphore availableThreads;

    public RemoteSumManagerImpl(int numMachines, int numFiles, RemoteSumStub remoteSumStubRpcCaller, int degreeOfParallelism) {
        this.numMachines = numMachines;
        this.numFiles = numFiles;
        this.remoteSumStubRpcCaller = remoteSumStubRpcCaller;   // injected dependency
        this.busyMachinesMap = new ConcurrentHashMap<>();
        this.degreeOfParallelism = degreeOfParallelism;
        this.availableThreads = new Semaphore(degreeOfParallelism); // Used for limiting the number of concurrent threads
    }

    @Override
    public long calculateSum() {
        BlockingQueue<Long> resultQueue = new LinkedBlockingQueue<>(degreeOfParallelism);
        Executors.newSingleThreadExecutor().submit(() -> {
            int fileId = -1;
            while (!failedJobQueue.isEmpty() || fileId < numFiles) {
                int machineId = getFreeMachine();
                fileId = getNextFile();
                System.out.println("Got file " + fileId + " to process on machine " + machineId);
                RemoteSumRunnable remoteSumThread = new RemoteSumRunnable(fileId,
                        machineId,
                        remoteSumStubRpcCaller,
                        availableThreads,
                        failedJobQueue,
                        resultQueue,
                        busyMachinesMap);
                try {
                    availableThreads.acquire();
                    Executors.newSingleThreadExecutor().submit(remoteSumThread);
                    System.out.println("Spun off a new thread for finding sum of file " + fileId + " on machine " + machineId);
                } catch (InterruptedException ie) {
                    System.err.println("Error occurred during semaphore acquisition. Crashing program");
                    System.exit(1);
                }
            }
        });
        final AtomicLong finalResult = new AtomicLong();
        try {
            while (currentFile.get() < numFiles || !failedJobQueue.isEmpty()) {
                System.out.println("Blocking waiting for result to appear on resultQueue");
                long localResult = resultQueue.take();
                finalResult.addAndGet(localResult);
            }
        } catch (InterruptedException ie) {
            System.err.println("Error occurred during result get");
            System.exit(1);
        }
        return finalResult.get();
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
        } else {
            return currentFile.getAndIncrement();
        }
    }
}

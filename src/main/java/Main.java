public class Main {
    public static void main(String[] args) {
        // Configuration parameters
        int maxDurationInMillis = 5000; // Max time taken for the simulated response from any machine
        int failureCoefficient = 50; // One call out of this number of calls will fail randomly with exception in the simulator
        int numMachines = 25000;
        int numFiles = 10000000;
        int degreeOfParallelism = 100;  // Number of concurrent threads that can run on client machine smoothly
        // Setup the machine behavior simulator
        RemoteSumStub remoteSumStub = new RemoteSumSimulatedImpl(maxDurationInMillis, failureCoefficient);

        // The actual sum calculation is distributed across machines and calculated in here
        RemoteSumManager remoteSumManager = new RemoteSumManagerImpl(numMachines, numFiles, remoteSumStub, degreeOfParallelism);
        long result = remoteSumManager.calculateSum();
        System.out.println("The final result is: " + result);
    }
}

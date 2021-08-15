public class Main {
    public static void main(String[] args) {
        RemoteSum remoteSum = new RemoteSumDummyImpl(5000, 50);
        RemoteSumManager remoteSumManager = new RemoteSumManagerImpl(25000, 10000000, remoteSum, 100);
        long result = remoteSumManager.calculateSum();
        System.out.println("The final result is: " + result);
    }
}

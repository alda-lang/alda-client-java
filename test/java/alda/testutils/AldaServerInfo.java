package alda.testutils;

public class AldaServerInfo {

    private String host;
    private int port;
    private int numberOfWorkers;

    public AldaServerInfo(String host, int port,  int numberOfWorkers) {
        this.host = host;
        this.port = port;
        this.numberOfWorkers = numberOfWorkers;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public int getNumberOfWorkers() {
        return numberOfWorkers;
    }
}

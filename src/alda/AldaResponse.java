package alda;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

public class AldaResponse {
  public boolean success;
  public boolean pending;
  public String signal;
  public String body;
  public AldaScore score;
  public byte[] workerAddress;
  public boolean noWorker;

  public class AldaScore {
    @SerializedName("chord-mode")
    public Boolean chordMode;
    @SerializedName("current-instruments")
    public String[] currentInstruments;
  }

  public static AldaResponse fromJson(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, AldaResponse.class);
  }
}

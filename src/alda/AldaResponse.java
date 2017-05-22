package alda;

import java.util.Map;

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

    public Map<String, String[]> nicknames;

    /**
     * Returns the current instruments if possible
     * @return null if not possible, string array with current instruments if possible.
     */
    public String[] currentInstruments() {
      if (this.currentInstruments != null &&
          this.currentInstruments.length > 0) {
        return this.currentInstruments;
      }
      return null;
    }
  }

  public static AldaResponse fromJson(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, AldaResponse.class);
  }

  /**
   * Returns an alda score corresponding to the given json
   */
  public static AldaScore fromJsonScore(String json) {
    Gson gson = new Gson();
    return gson.fromJson(json, AldaScore.class);
  }
}
